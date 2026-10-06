package com.mangadl.android.data.tracking

import android.content.Context
import android.net.Uri
import android.util.Log
import com.mangadl.android.BuildConfig
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.prefs.AppPreferences
import com.mangadl.android.data.prefs.PrefKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class TrackerSearchResult(
    val id: Int,
    val title: String,
    val totalChapters: Int = 0,
    val coverUrl: String = "",
    val tracker: String, // "AniList" or "MyAnimeList"
)

object TrackerService {
    private const val TAG = "TrackerService"
    private val httpClient: OkHttpClient by lazy { MangaDlApp.instance.httpClient }
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    const val ANILIST_GRAPHQL_URL = "https://graphql.anilist.co"
    const val MAL_API_URL = "https://api.myanimelist.net/v2"
    const val MAL_OAUTH_TOKEN_URL = "https://myanimelist.net/v1/oauth2/token"

    // ── AniList ──────────────────────────────────────────────────────────────

    /**
     * Query the authenticated AniList viewer to retrieve user ID and username.
     */
    suspend fun getAniListViewer(token: String): Result<Pair<Int, String>> = withContext(Dispatchers.IO) {
        runCatching {
            val query = """
                query {
                  Viewer {
                    id
                    name
                  }
                }
            """.trimIndent()
            val payload = JSONObject().put("query", query)
            val request = Request.Builder()
                .url(ANILIST_GRAPHQL_URL)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: error("Empty response")
            if (!response.isSuccessful) error("AniList error ${response.code}: $body")

            val json = JSONObject(body)
            val viewer = json.getJSONObject("data").getJSONObject("Viewer")
            Pair(viewer.getInt("id"), viewer.getString("name"))
        }
    }

    /**
     * Search AniList for manga by title.
     */
    suspend fun searchAniList(title: String, token: String? = null): Result<List<TrackerSearchResult>> = withContext(Dispatchers.IO) {
        runCatching {
            val query = """
                query Search(${'$'}search: String) {
                  Page(perPage: 10) {
                    media(search: ${'$'}search, type: MANGA) {
                      id
                      title {
                        romaji
                        english
                        userPreferred
                      }
                      chapters
                      coverImage {
                        medium
                        large
                      }
                    }
                  }
                }
            """.trimIndent()
            val variables = JSONObject().put("search", title)
            val payload = JSONObject().put("query", query).put("variables", variables)

            val reqBuilder = Request.Builder()
                .url(ANILIST_GRAPHQL_URL)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))

            if (!token.isNullOrEmpty()) {
                reqBuilder.header("Authorization", "Bearer $token")
            }

            val response = httpClient.newCall(reqBuilder.build()).execute()
            val body = response.body?.string() ?: error("Empty response")
            val json = JSONObject(body)
            val mediaArray = json.getJSONObject("data").getJSONObject("Page").getJSONArray("media")

            val results = mutableListOf<TrackerSearchResult>()
            for (i in 0 until mediaArray.length()) {
                val item = mediaArray.getJSONObject(i)
                val id = item.getInt("id")
                val tObj = item.getJSONObject("title")
                val displayTitle = tObj.optString("english").ifEmpty {
                    tObj.optString("userPreferred").ifEmpty { tObj.optString("romaji") }
                }
                val chapters = item.optInt("chapters", 0)
                val cover = item.optJSONObject("coverImage")?.optString("medium") ?: ""
                results.add(
                    TrackerSearchResult(
                        id = id,
                        title = displayTitle,
                        totalChapters = chapters,
                        coverUrl = cover,
                        tracker = "AniList",
                    )
                )
            }
            results
        }
    }

    /**
     * Update reading progress on AniList.
     */
    suspend fun updateAniListProgress(
        token: String,
        mediaId: Int,
        progress: Int,
        isCompleted: Boolean = false,
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val query = """
                mutation SaveMediaList(${'$'}mediaId: Int, ${'$'}progress: Int, ${'$'}status: MediaListStatus) {
                  SaveMediaListEntry(mediaId: ${'$'}mediaId, progress: ${'$'}progress, status: ${'$'}status) {
                    id
                    progress
                    status
                  }
                }
            """.trimIndent()
            val status = if (isCompleted) "COMPLETED" else "CURRENT"
            val variables = JSONObject()
                .put("mediaId", mediaId)
                .put("progress", progress)
                .put("status", status)

            val payload = JSONObject().put("query", query).put("variables", variables)
            val request = Request.Builder()
                .url(ANILIST_GRAPHQL_URL)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) error("AniList progress update failed ${response.code}: $body")
            Log.i(TAG, "Updated AniList progress for media $mediaId to chapter $progress (completed=$isCompleted)")
            true
        }
    }

    // ── MyAnimeList ──────────────────────────────────────────────────────────

    /**
     * Exchange MAL authorization code for access and refresh tokens.
     */
    suspend fun exchangeMalToken(
        code: String,
        codeVerifier: String,
        clientId: String,
        redirectUri: String,
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        runCatching {
            // First try direct exchange with MyAnimeList
            val formBody = FormBody.Builder()
                .add("client_id", clientId)
                .add("code", code)
                .add("code_verifier", codeVerifier)
                .add("grant_type", "authorization_code")
                .add("redirect_uri", redirectUri)
                .build()

            val directReq = Request.Builder()
                .url(MAL_OAUTH_TOKEN_URL)
                .post(formBody)
                .build()

            var directOk = false
            var accessToken = ""
            var refreshToken = ""

            try {
                val directResp = httpClient.newCall(directReq).execute()
                if (directResp.isSuccessful) {
                    val directJson = JSONObject(directResp.body?.string() ?: "")
                    accessToken = directJson.optString("access_token", "")
                    refreshToken = directJson.optString("refresh_token", "")
                    if (accessToken.isNotEmpty()) directOk = true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Direct MAL token exchange failed: ${e.message}, falling back to backend")
            }

            if (!directOk) {
                // Fallback to backend proxy
                val backendPayload = JSONObject()
                    .put("client_id", clientId)
                    .put("code", code)
                    .put("code_verifier", codeVerifier)
                    .put("redirect_uri", redirectUri)

                val backendReq = Request.Builder()
                    .url("${BuildConfig.BACKEND_URL.trimEnd('/')}/auth/mal/token")
                    .post(backendPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                val backendResp = httpClient.newCall(backendReq).execute()
                val body = backendResp.body?.string() ?: error("Empty response from backend")
                if (!backendResp.isSuccessful) error("Backend MAL exchange error ${backendResp.code}: $body")
                val backendJson = JSONObject(body)
                accessToken = backendJson.optString("access_token", "")
                refreshToken = backendJson.optString("refresh_token", "")
            }

            if (accessToken.isEmpty()) error("Could not obtain MAL access token")
            Pair(accessToken, refreshToken)
        }
    }

    /**
     * Fetch current MyAnimeList username.
     */
    suspend fun getMalUser(token: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("$MAL_API_URL/users/@me")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            val resp = httpClient.newCall(req).execute()
            val body = resp.body?.string() ?: error("Empty response")
            if (!resp.isSuccessful) error("MAL user error ${resp.code}: $body")
            JSONObject(body).getString("name")
        }
    }

    /**
     * Search MyAnimeList for manga by title.
     */
    suspend fun searchMal(title: String, token: String): Result<List<TrackerSearchResult>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = Uri.parse("$MAL_API_URL/manga").buildUpon()
                .appendQueryParameter("q", title.take(64))
                .appendQueryParameter("nsfw", "true")
                .appendQueryParameter("fields", "id,title,num_chapters,main_picture")
                .build()

            val req = Request.Builder()
                .url(url.toString())
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            val resp = httpClient.newCall(req).execute()
            val body = resp.body?.string() ?: error("Empty response")
            val json = JSONObject(body)
            val data = json.optJSONArray("data") ?: JSONArray()

            val results = mutableListOf<TrackerSearchResult>()
            for (i in 0 until data.length()) {
                val node = data.getJSONObject(i).getJSONObject("node")
                val id = node.getInt("id")
                val mTitle = node.getString("title")
                val chapters = node.optInt("num_chapters", 0)
                val cover = node.optJSONObject("main_picture")?.optString("medium") ?: ""
                results.add(
                    TrackerSearchResult(
                        id = id,
                        title = mTitle,
                        totalChapters = chapters,
                        coverUrl = cover,
                        tracker = "MyAnimeList",
                    )
                )
            }
            results
        }
    }

    /**
     * Update reading progress on MyAnimeList.
     */
    suspend fun updateMalProgress(
        token: String,
        malId: Int,
        progress: Int,
        isCompleted: Boolean = false,
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val status = if (isCompleted) "completed" else "reading"
            val formBody = FormBody.Builder()
                .add("status", status)
                .add("num_chapters_read", progress.toString())
                .build()

            val req = Request.Builder()
                .url("$MAL_API_URL/manga/$malId/my_list_status")
                .header("Authorization", "Bearer $token")
                .put(formBody)
                .build()

            val resp = httpClient.newCall(req).execute()
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) error("MAL progress update failed ${resp.code}: $body")
            Log.i(TAG, "Updated MAL progress for manga $malId to chapter $progress (completed=$isCompleted)")
            true
        }
    }

    // ── Auto-Tracking Synchronization ────────────────────────────────────────

    /**
     * Automatically update progress across linked trackers (AniList and MAL)
     * when a chapter is completed or progressed.
     */
    suspend fun syncChapterRead(
        context: Context,
        mangaId: String,
        chapterNumber: Float,
        isCompleted: Boolean = true,
    ) = withContext(Dispatchers.IO) {
        if (mangaId.isBlank() || chapterNumber <= 0f) return@withContext
        val trackLinks = context.getSharedPreferences("manga_dl_tracker_links", Context.MODE_PRIVATE)
        val prefs = AppPreferences.getInstance(context)

        val chInt = chapterNumber.toInt()
        val anilistMediaId = trackLinks.getInt("anilist_$mangaId", -1)
        val malMangaId = trackLinks.getInt("mal_$mangaId", -1)
        val markCompleted = isCompleted && prefs.markTrackerCompletedOnFinish.first()

        // 1. AniList sync
        if (anilistMediaId > 0) {
            val alToken = prefs.anilistToken.first()
            val alConnected = prefs.anilistConnected.first()
            if (alConnected && alToken.isNotEmpty()) {
                updateAniListProgress(alToken, anilistMediaId, chInt, markCompleted)
                    .onFailure { Log.w(TAG, "AniList auto-sync error: ${it.message}") }
            }
        }

        // 2. MAL sync
        if (malMangaId > 0) {
            val malTok = prefs.malToken.first()
            val malConnected = prefs.malConnected.first()
            if (malConnected && malTok.isNotEmpty()) {
                updateMalProgress(malTok, malMangaId, chInt, markCompleted)
                    .onFailure { Log.w(TAG, "MAL auto-sync error: ${it.message}") }
            }
        }
    }
}

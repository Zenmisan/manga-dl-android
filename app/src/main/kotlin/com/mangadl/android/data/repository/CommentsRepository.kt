package com.mangadl.android.data.repository

import com.mangadl.android.BuildConfig
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.auth.SupabaseManager
import com.mangadl.android.data.model.CommentItem
import com.mangadl.android.data.model.CommentsResponse
import com.mangadl.android.data.model.PostCommentPayload
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class CommentsRepository(
    private val baseUrl: String = BuildConfig.BACKEND_URL.trimEnd('/'),
) {
    private val client by lazy { MangaDlApp.instance.httpClient }
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private suspend fun getAuthHeader(): String? {
        return try {
            val session = SupabaseManager.client.auth.currentSessionOrNull()
            session?.accessToken?.let { "Bearer $it" }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getComments(
        provider: String,
        mangaId: String,
        chapterId: String? = null,
        limit: Int = 30,
        offset: Int = 0,
    ): Result<CommentsResponse> = withContext(Dispatchers.IO) {
        runCatching {
            var url = "$baseUrl/comments?provider=${java.net.URLEncoder.encode(provider, "UTF-8")}&manga_id=${java.net.URLEncoder.encode(mangaId, "UTF-8")}&limit=$limit&offset=$offset"
            if (!chapterId.isNullOrBlank()) {
                url += "&chapter_id=${java.net.URLEncoder.encode(chapterId, "UTF-8")}"
            }
            val requestBuilder = Request.Builder().url(url).get()
            getAuthHeader()?.let { requestBuilder.header("Authorization", it) }
            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                return@runCatching CommentsResponse()
            }
            val body = response.body?.string() ?: return@runCatching CommentsResponse()
            json.decodeFromString<CommentsResponse>(body)
        }
    }

    suspend fun postComment(
        provider: String,
        mangaId: String,
        chapterId: String? = null,
        parentId: String? = null,
        text: String,
    ): Result<CommentItem?> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = PostCommentPayload(
                provider = provider,
                mangaId = mangaId,
                chapterId = chapterId,
                parentId = parentId,
                body = text.trim(),
            )
            val jsonString = json.encodeToString(PostCommentPayload.serializer(), payload)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonString.toRequestBody(mediaType)

            val requestBuilder = Request.Builder()
                .url("$baseUrl/comments")
                .post(requestBody)
            getAuthHeader()?.let { requestBuilder.header("Authorization", it) }

            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                val err = response.body?.string() ?: ""
                throw Exception("Failed to post comment: HTTP ${response.code} $err")
            }
            val respBody = response.body?.string() ?: return@runCatching null
            json.decodeFromString<CommentItem>(respBody)
        }
    }

    suspend fun toggleLike(commentId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val requestBuilder = Request.Builder()
                .url("$baseUrl/comments/$commentId/like")
                .post("{}".toRequestBody("application/json".toMediaType()))
            getAuthHeader()?.let { requestBuilder.header("Authorization", it) }
            val response = client.newCall(requestBuilder.build()).execute()
            response.isSuccessful
        }
    }
}

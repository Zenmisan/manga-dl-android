package com.mangadl.android.ui.viewmodels

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mangadl.android.BuildConfig
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.prefs.AppPreferences
import com.mangadl.android.data.prefs.PrefKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class TrackerViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = AppPreferences.getInstance(app)
    private val httpClient = MangaDlApp.instance.httpClient

    val anilistToken: StateFlow<String> = prefs.anilistToken.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val anilistClientId: StateFlow<String> = prefs.anilistClientId.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val anilistConnected: StateFlow<Boolean> = prefs.anilistConnected.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val malToken: StateFlow<String> = prefs.malToken.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val malClientId: StateFlow<String> = prefs.malClientId.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val malConnected: StateFlow<Boolean> = prefs.malConnected.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setAnilistClientId(id: String) = viewModelScope.launch { prefs.set(PrefKeys.ANILIST_CLIENT_ID, id) }
    fun setMalClientId(id: String) = viewModelScope.launch { prefs.set(PrefKeys.MAL_CLIENT_ID, id) }

    private fun effectiveAnilistClientId() = anilistClientId.value.ifEmpty { BuildConfig.ANILIST_CLIENT_ID }
    private fun effectiveMalClientId() = malClientId.value.ifEmpty { BuildConfig.MAL_CLIENT_ID }

    /** Build the AniList OAuth URL for implicit flow. Call from UI to open in Custom Tab. */
    fun anilistAuthUrl(): String {
        val clientId = effectiveAnilistClientId().ifEmpty { return "" }
        return "https://anilist.co/api/v2/oauth/authorize" +
            "?client_id=$clientId" +
            "&response_type=token" +
            "&redirect_uri=${Uri.encode(ANILIST_REDIRECT)}"
    }

    /** Called from MainActivity when mangadl://anilist-callback#access_token=... is received. */
    fun handleAnilistCallback(fragment: String?) {
        if (fragment == null) return
        val params = fragment.split("&").associate {
            val (k, v) = it.split("=", limit = 2).let { p -> p[0] to (p.getOrElse(1) { "" }) }
            k to v
        }
        val token = params["access_token"] ?: return
        viewModelScope.launch {
            prefs.set(PrefKeys.ANILIST_TOKEN, token)
            prefs.set(PrefKeys.ANILIST_CONNECTED, true)
            Log.i(TAG, "AniList connected")
        }
    }

    fun disconnectAnilist() = viewModelScope.launch {
        prefs.set(PrefKeys.ANILIST_TOKEN, "")
        prefs.set(PrefKeys.ANILIST_CONNECTED, false)
    }

    /** MAL PKCE state — store verifier in memory for the exchange. */
    private var malCodeVerifier: String = ""

    /** Build MAL OAuth URL using PKCE (plain). */
    fun malAuthUrl(): String {
        val clientId = effectiveMalClientId().ifEmpty { return "" }
        malCodeVerifier = generatePkceVerifier()
        return "https://myanimelist.net/v1/oauth2/authorize" +
            "?response_type=code" +
            "&client_id=$clientId" +
            "&code_challenge=$malCodeVerifier" +
            "&code_challenge_method=plain" +
            "&redirect_uri=${Uri.encode(MAL_REDIRECT)}"
    }

    /** Called from MainActivity when mangadl://mal-callback?code=... is received. */
    fun handleMalCallback(uri: Uri) {
        val code = uri.getQueryParameter("code") ?: return
        val clientId = effectiveMalClientId().ifEmpty { return }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject()
                    .put("client_id", clientId)
                    .put("code", code)
                    .put("code_verifier", malCodeVerifier)
                    .put("redirect_uri", MAL_REDIRECT)
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("${BuildConfig.BACKEND_URL}/auth/mal/token")
                    .post(body)
                    .build()
                val resp = httpClient.newCall(request).execute()
                val respJson = JSONObject(resp.body?.string() ?: return@launch)
                val token = respJson.optString("access_token", "")
                val refresh = respJson.optString("refresh_token", "")
                if (token.isNotEmpty()) {
                    prefs.set(PrefKeys.MAL_TOKEN, token)
                    prefs.set(PrefKeys.MAL_REFRESH_TOKEN, refresh)
                    prefs.set(PrefKeys.MAL_CONNECTED, true)
                    Log.i(TAG, "MAL connected")
                }
            } catch (e: Exception) {
                Log.e(TAG, "MAL token exchange failed", e)
            }
        }
    }

    fun disconnectMal() = viewModelScope.launch {
        prefs.set(PrefKeys.MAL_TOKEN, "")
        prefs.set(PrefKeys.MAL_REFRESH_TOKEN, "")
        prefs.set(PrefKeys.MAL_CONNECTED, false)
    }

    // --- Per-manga link storage (SharedPreferences, dynamic keys) ---

    private val trackLinks = app.getSharedPreferences("manga_dl_tracker_links", Context.MODE_PRIVATE)

    fun isAnilistLinked(mangaId: String) = trackLinks.contains("anilist_$mangaId")
    fun isMalLinked(mangaId: String) = trackLinks.contains("mal_$mangaId")

    fun unlinkAnilist(mangaId: String) {
        trackLinks.edit().remove("anilist_$mangaId").apply()
    }

    fun unlinkMal(mangaId: String) {
        trackLinks.edit().remove("mal_$mangaId").apply()
    }

    /** Search AniList via backend, take top result, store mediaId keyed by mangaId. */
    fun linkAnilist(mangaId: String, mangaTitle: String, onResult: (success: Boolean) -> Unit) {
        val token = anilistToken.value.ifEmpty { onResult(false); return }
        viewModelScope.launch(Dispatchers.IO) {
            val ok = runCatching {
                val req = Request.Builder()
                    .url("${BuildConfig.BACKEND_URL}/auth/anilist/search?q=${Uri.encode(mangaTitle)}")
                    .header("Authorization", "Bearer $token")
                    .build()
                val resp = httpClient.newCall(req).execute()
                val arr = org.json.JSONArray(resp.body?.string() ?: error("empty"))
                val mediaId = arr.getJSONObject(0).getInt("id")
                trackLinks.edit().putInt("anilist_$mangaId", mediaId).apply()
                true
            }.getOrDefault(false)
            withContext(Dispatchers.Main) { onResult(ok) }
        }
    }

    /** Search MAL via backend, take top result, store mediaId keyed by mangaId. */
    fun linkMal(mangaId: String, mangaTitle: String, onResult: (success: Boolean) -> Unit) {
        val token = malToken.value.ifEmpty { onResult(false); return }
        viewModelScope.launch(Dispatchers.IO) {
            val ok = runCatching {
                val req = Request.Builder()
                    .url("${BuildConfig.BACKEND_URL}/auth/mal/search?q=${Uri.encode(mangaTitle)}&access_token=${Uri.encode(token)}")
                    .build()
                val resp = httpClient.newCall(req).execute()
                val arr = org.json.JSONArray(resp.body?.string() ?: error("empty"))
                val malId = arr.getJSONObject(0).getInt("id")
                trackLinks.edit().putInt("mal_$mangaId", malId).apply()
                true
            }.getOrDefault(false)
            withContext(Dispatchers.Main) { onResult(ok) }
        }
    }

    private fun generatePkceVerifier(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
        return (1..64).map { chars.random() }.joinToString("")
    }

    companion object {
        const val TAG = "TrackerViewModel"
        const val ANILIST_REDIRECT = "mangadl://anilist-callback"
        const val MAL_REDIRECT = "mangadl://mal-callback"
    }
}

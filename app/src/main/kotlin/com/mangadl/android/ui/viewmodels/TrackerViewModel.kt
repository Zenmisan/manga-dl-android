package com.mangadl.android.ui.viewmodels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.prefs.AppPreferences
import com.mangadl.android.data.prefs.PrefKeys
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.Request
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

    /** Build the AniList OAuth URL for implicit flow. Call from UI to open in Custom Tab. */
    fun anilistAuthUrl(): String {
        val clientId = anilistClientId.value.ifEmpty { return "" }
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
        val clientId = malClientId.value.ifEmpty { return "" }
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
        val clientId = malClientId.value.ifEmpty { return }
        viewModelScope.launch {
            try {
                val body = FormBody.Builder()
                    .add("client_id", clientId)
                    .add("code", code)
                    .add("code_verifier", malCodeVerifier)
                    .add("grant_type", "authorization_code")
                    .add("redirect_uri", MAL_REDIRECT)
                    .build()
                val request = Request.Builder()
                    .url("https://myanimelist.net/v1/oauth2/token")
                    .post(body)
                    .build()
                val resp = httpClient.newCall(request).execute()
                val json = JSONObject(resp.body?.string() ?: return@launch)
                val token = json.optString("access_token", "")
                val refresh = json.optString("refresh_token", "")
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

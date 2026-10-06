package com.mangadl.android.ui.viewmodels

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mangadl.android.BuildConfig
import com.mangadl.android.data.prefs.AppPreferences
import com.mangadl.android.data.prefs.PrefKeys
import com.mangadl.android.data.tracking.TrackerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrackerViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = AppPreferences.getInstance(app)

    val anilistToken: StateFlow<String> = prefs.anilistToken.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val anilistClientId: StateFlow<String> = prefs.anilistClientId.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val anilistConnected: StateFlow<Boolean> = prefs.anilistConnected.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val anilistUsername: StateFlow<String> = prefs.anilistUsername.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val malToken: StateFlow<String> = prefs.malToken.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val malClientId: StateFlow<String> = prefs.malClientId.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val malConnected: StateFlow<Boolean> = prefs.malConnected.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val malUsername: StateFlow<String> = prefs.malUsername.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val autoSyncTrackers: StateFlow<Boolean> = prefs.autoSyncTrackers.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val markCompletedOnFinish: StateFlow<Boolean> = prefs.markTrackerCompletedOnFinish.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val askBeforeScoreChange: StateFlow<Boolean> = prefs.askBeforeScoreChange.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setAnilistClientId(id: String) = viewModelScope.launch { prefs.set(PrefKeys.ANILIST_CLIENT_ID, id) }
    fun setMalClientId(id: String) = viewModelScope.launch { prefs.set(PrefKeys.MAL_CLIENT_ID, id) }
    fun setAutoSync(enabled: Boolean) = viewModelScope.launch { prefs.set(PrefKeys.AUTO_SYNC_TRACKERS, enabled) }
    fun setMarkCompletedOnFinish(v: Boolean) = viewModelScope.launch { prefs.set(PrefKeys.MARK_TRACKER_COMPLETED_ON_FINISH, v) }
    fun setAskBeforeScoreChange(v: Boolean) = viewModelScope.launch { prefs.set(PrefKeys.ASK_BEFORE_SCORE_CHANGE, v) }

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

    /**
     * Called from MainActivity when mangadl://anilist-callback#access_token=... is received.
     * Extracts token from fragment, query, or URI string.
     */
    fun handleAnilistCallback(uriOrFragment: String?) {
        if (uriOrFragment == null) return
        val raw = uriOrFragment.trim()
        val token = when {
            raw.contains("access_token=") -> {
                val paramStr = if (raw.contains("#")) raw.substringAfter("#") else raw.substringAfter("?")
                paramStr.split("&").associate {
                    val p = it.split("=", limit = 2)
                    p[0] to (p.getOrElse(1) { "" })
                }["access_token"]
            }
            else -> null
        } ?: return

        viewModelScope.launch {
            prefs.set(PrefKeys.ANILIST_TOKEN, token)
            prefs.set(PrefKeys.ANILIST_CONNECTED, true)
            Log.i(TAG, "AniList token saved")

            // Fetch username
            TrackerService.getAniListViewer(token)
                .onSuccess { (_, name) ->
                    prefs.set(PrefKeys.ANILIST_USERNAME, name)
                    Log.i(TAG, "AniList connected as $name")
                }
                .onFailure {
                    Log.w(TAG, "Failed to fetch AniList viewer profile: ${it.message}")
                }
        }
    }

    fun disconnectAnilist() = viewModelScope.launch {
        prefs.set(PrefKeys.ANILIST_TOKEN, "")
        prefs.set(PrefKeys.ANILIST_USERNAME, "")
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
            TrackerService.exchangeMalToken(
                code = code,
                codeVerifier = malCodeVerifier,
                clientId = clientId,
                redirectUri = MAL_REDIRECT,
            ).onSuccess { (accessToken, refreshToken) ->
                prefs.set(PrefKeys.MAL_TOKEN, accessToken)
                prefs.set(PrefKeys.MAL_REFRESH_TOKEN, refreshToken)
                prefs.set(PrefKeys.MAL_CONNECTED, true)
                Log.i(TAG, "MAL connected successfully")

                TrackerService.getMalUser(accessToken)
                    .onSuccess { username ->
                        prefs.set(PrefKeys.MAL_USERNAME, username)
                        Log.i(TAG, "MAL connected as $username")
                    }
                    .onFailure {
                        Log.w(TAG, "Failed to fetch MAL username: ${it.message}")
                    }
            }.onFailure { e ->
                Log.e(TAG, "MAL token exchange failed", e)
            }
        }
    }

    fun disconnectMal() = viewModelScope.launch {
        prefs.set(PrefKeys.MAL_TOKEN, "")
        prefs.set(PrefKeys.MAL_REFRESH_TOKEN, "")
        prefs.set(PrefKeys.MAL_USERNAME, "")
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

    /** Search AniList, take top result, store mediaId keyed by mangaId. */
    fun linkAnilist(mangaId: String, mangaTitle: String, onResult: (success: Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val token = anilistToken.value.ifEmpty { null }
            val ok = TrackerService.searchAniList(mangaTitle, token)
                .map { list ->
                    val top = list.firstOrNull() ?: return@map false
                    trackLinks.edit().putInt("anilist_$mangaId", top.id).apply()
                    true
                }
                .getOrDefault(false)
            withContext(Dispatchers.Main) { onResult(ok) }
        }
    }

    /** Search MAL, take top result, store mediaId keyed by mangaId. */
    fun linkMal(mangaId: String, mangaTitle: String, onResult: (success: Boolean) -> Unit) {
        val token = malToken.value.ifEmpty { onResult(false); return }
        viewModelScope.launch(Dispatchers.IO) {
            val ok = TrackerService.searchMal(mangaTitle, token)
                .map { list ->
                    val top = list.firstOrNull() ?: return@map false
                    trackLinks.edit().putInt("mal_$mangaId", top.id).apply()
                    true
                }
                .getOrDefault(false)
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

package com.mangadl.android.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mangadl.android.data.prefs.AppPreferences
import com.mangadl.android.data.prefs.PrefKeys
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = AppPreferences.getInstance(app)

    val theme: StateFlow<String> = prefs.theme.stateIn(viewModelScope, SharingStarted.Eagerly, "dark")
    val ambilight: StateFlow<Boolean> = prefs.ambilight.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val incognito: StateFlow<Boolean> = prefs.incognitoMode.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val newChapterAlerts: StateFlow<Boolean> = prefs.newChapterAlerts.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val autoDownloadNew: StateFlow<Boolean> = prefs.autoDownloadNew.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val downloadWifiOnly: StateFlow<Boolean> = prefs.downloadWifiOnly.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val backendUrl: StateFlow<String> = prefs.backendUrl.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val apiKey: StateFlow<String> = prefs.apiKey.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val readerDirection: StateFlow<String> = prefs.readerDirection.stateIn(viewModelScope, SharingStarted.Eagerly, "ltr")
    val dualPageSpread: StateFlow<String> = prefs.dualPageSpread.stateIn(viewModelScope, SharingStarted.Eagerly, "off")
    val cropBorders: StateFlow<Boolean> = prefs.cropBorders.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val tapZones: StateFlow<String> = prefs.tapZones.stateIn(viewModelScope, SharingStarted.Eagerly, "default")
    val keepScreenOn: StateFlow<Boolean> = prefs.keepScreenOn.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val showPageNumber: StateFlow<Boolean> = prefs.showPageNumber.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val fullScreen: StateFlow<Boolean> = prefs.fullScreen.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setTheme(v: String) = viewModelScope.launch { prefs.set(PrefKeys.THEME, v) }
    fun setAmbilight(v: Boolean) = viewModelScope.launch { prefs.set(PrefKeys.AMBILIGHT, v) }
    fun setIncognito(v: Boolean) = viewModelScope.launch { prefs.set(PrefKeys.INCOGNITO_MODE, v) }
    fun setNewChapterAlerts(v: Boolean) = viewModelScope.launch { prefs.set(PrefKeys.NEW_CHAPTER_ALERTS, v) }
    fun setAutoDownloadNew(v: Boolean) = viewModelScope.launch { prefs.set(PrefKeys.AUTO_DOWNLOAD_NEW, v) }
    fun setDownloadWifiOnly(v: Boolean) = viewModelScope.launch { prefs.set(PrefKeys.DOWNLOAD_WIFI_ONLY, v) }
    fun setBackendUrl(v: String) = viewModelScope.launch { prefs.set(PrefKeys.BACKEND_URL, v) }
    fun setApiKey(v: String) = viewModelScope.launch { prefs.set(PrefKeys.API_KEY, v) }
    fun setReaderDirection(v: String) = viewModelScope.launch { prefs.set(PrefKeys.READER_DIRECTION, v) }
    fun setDualPageSpread(v: String) = viewModelScope.launch { prefs.set(PrefKeys.DUAL_PAGE_SPREAD, v) }
    fun setCropBorders(v: Boolean) = viewModelScope.launch { prefs.set(PrefKeys.CROP_BORDERS, v) }
    fun setTapZones(v: String) = viewModelScope.launch { prefs.set(PrefKeys.TAP_ZONES, v) }
    fun setKeepScreenOn(v: Boolean) = viewModelScope.launch { prefs.set(PrefKeys.KEEP_SCREEN_ON, v) }
    fun setShowPageNumber(v: Boolean) = viewModelScope.launch { prefs.set(PrefKeys.SHOW_PAGE_NUMBER, v) }
    fun setFullScreen(v: Boolean) = viewModelScope.launch { prefs.set(PrefKeys.FULL_SCREEN, v) }
}

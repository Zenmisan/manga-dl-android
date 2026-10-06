package com.mangadl.android.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mangadl.android.data.backup.BackupManager
import com.mangadl.android.data.library.LibraryUpdateWorker
import com.mangadl.android.data.prefs.AppPreferences
import com.mangadl.android.data.prefs.PrefKeys
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = AppPreferences.getInstance(app)
    private val workManager = WorkManager.getInstance(app)

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
    val hapticFeedback: StateFlow<Boolean> = prefs.hapticFeedback.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val biometricLock: StateFlow<Boolean> = prefs.biometricLock.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val syncWifiOnly: StateFlow<Boolean> = prefs.syncWifiOnly.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val backgroundSyncEnabled: StateFlow<Boolean> = prefs.backgroundSyncEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val saveChaptersPublic: StateFlow<Boolean> = prefs.saveChaptersPublic.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val autoBackupWeekly: StateFlow<Boolean> = prefs.autoBackupWeekly.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val sidePadding: StateFlow<String> = prefs.sidePadding.stateIn(viewModelScope, SharingStarted.Eagerly, "0")
    val volumeKeysTurnPages: StateFlow<Boolean> = prefs.volumeKeysTurnPages.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val readerBackground: StateFlow<String> = prefs.readerBackground.stateIn(viewModelScope, SharingStarted.Eagerly, "black")
    val gridColumns: StateFlow<String> = prefs.gridColumns.stateIn(viewModelScope, SharingStarted.Eagerly, "Auto")
    val showUnreadBadges: StateFlow<Boolean> = prefs.showUnreadBadges.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val showDownloadedBadges: StateFlow<Boolean> = prefs.showDownloadedBadges.stateIn(viewModelScope, SharingStarted.Eagerly, true)

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
    fun setHapticFeedback(v: Boolean) = viewModelScope.launch { prefs.set(PrefKeys.HAPTIC_FEEDBACK, v) }
    fun setSidePadding(v: String) = viewModelScope.launch { prefs.set(PrefKeys.SIDE_PADDING, v) }
    fun setVolumeKeysTurnPages(v: Boolean) = viewModelScope.launch { prefs.set(PrefKeys.VOLUME_KEYS_TURN_PAGES, v) }
    fun setReaderBackground(v: String) = viewModelScope.launch { prefs.set(PrefKeys.READER_BACKGROUND, v) }
    fun setBiometricLock(v: Boolean) = viewModelScope.launch { prefs.set(PrefKeys.BIOMETRIC_LOCK, v) }
    fun setGridColumns(v: String) = viewModelScope.launch { prefs.set(PrefKeys.GRID_COLUMNS, v) }
    fun setShowUnreadBadges(v: Boolean) = viewModelScope.launch { prefs.set(PrefKeys.SHOW_UNREAD_BADGES, v) }
    fun setShowDownloadedBadges(v: Boolean) = viewModelScope.launch { prefs.set(PrefKeys.SHOW_DOWNLOADED_BADGES, v) }

    fun setSaveChaptersPublic(v: Boolean) = viewModelScope.launch { prefs.set(PrefKeys.SAVE_CHAPTERS_PUBLIC, v) }

    fun setSyncWifiOnly(v: Boolean) = viewModelScope.launch {
        prefs.set(PrefKeys.SYNC_WIFI_ONLY, v)
        if (backgroundSyncEnabled.value) scheduleBackgroundSync(wifiOnly = v)
    }

    fun setBackgroundSyncEnabled(v: Boolean) = viewModelScope.launch {
        prefs.set(PrefKeys.BACKGROUND_SYNC_ENABLED, v)
        if (v) {
            scheduleBackgroundSync(wifiOnly = syncWifiOnly.value)
        } else {
            workManager.cancelUniqueWork(LibraryUpdateWorker.WORK_NAME)
        }
    }

    private fun scheduleBackgroundSync(wifiOnly: Boolean) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()
        workManager.enqueueUniquePeriodicWork(
            LibraryUpdateWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<LibraryUpdateWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build(),
        )
    }

    fun setAutoBackupWeekly(v: Boolean) = viewModelScope.launch {
        prefs.set(PrefKeys.AUTO_BACKUP_WEEKLY, v)
        if (v) {
            workManager.enqueueUniquePeriodicWork(
                com.mangadl.android.data.backup.AutoBackupWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<com.mangadl.android.data.backup.AutoBackupWorker>(7, TimeUnit.DAYS).build(),
            )
        } else {
            workManager.cancelUniqueWork(com.mangadl.android.data.backup.AutoBackupWorker.WORK_NAME)
        }
    }
}

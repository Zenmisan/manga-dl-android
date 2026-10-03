package com.mangadl.android.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "manga_dl_settings")

object PrefKeys {
    // General
    val THEME = stringPreferencesKey("theme") // "dark" | "light" | "amoled"
    val ACCENT_COLOR = stringPreferencesKey("accent_color") // hex string
    val INCOGNITO_MODE = booleanPreferencesKey("incognito_mode")
    val NEW_CHAPTER_ALERTS = booleanPreferencesKey("new_chapter_alerts")
    val BACKGROUND_UPDATES = booleanPreferencesKey("background_updates")
    val DOWNLOAD_WIFI_ONLY = booleanPreferencesKey("download_wifi_only")

    // Reader
    val READER_DIRECTION = stringPreferencesKey("reader_direction") // "ltr" | "rtl" | "vertical"
    val CONTINUOUS_SCROLL = booleanPreferencesKey("continuous_scroll")
    val FULL_SCREEN = booleanPreferencesKey("full_screen")
    val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    val SHOW_PAGE_NUMBER = booleanPreferencesKey("show_page_number")
    val CROP_BORDERS = booleanPreferencesKey("crop_borders")
    val DUAL_PAGE_SPREAD = stringPreferencesKey("dual_page_spread") // "auto" | "always" | "off"
    val TAP_ZONES = stringPreferencesKey("tap_zones") // "default" | "lnav" | "edge" | "disabled"
    val SIDE_PADDING = stringPreferencesKey("side_padding") // "0".."80"

    // Library
    val UPDATE_INTERVAL = stringPreferencesKey("update_interval") // "manual" | "12h" | "24h" | "48h"
    val AUTO_DOWNLOAD_NEW = booleanPreferencesKey("auto_download_new")
    val LIBRARY_DISPLAY = stringPreferencesKey("library_display") // "grid" | "list"
    val LAST_UPDATE_CHECK = longPreferencesKey("last_update_check")

    // Trackers
    val ANILIST_CONNECTED = booleanPreferencesKey("anilist_connected")
    val MAL_CONNECTED = booleanPreferencesKey("mal_connected")

    // System
    val SYNC_WIFI_ONLY = booleanPreferencesKey("sync_wifi_only")

    // Connection
    val BACKEND_URL = stringPreferencesKey("backend_url")
    val API_KEY = stringPreferencesKey("api_key")

    // Appearance extras
    val AMBILIGHT = booleanPreferencesKey("ambilight")
}

class AppPreferences(private val context: Context) {
    val theme: Flow<String> = context.dataStore.data.map { it[PrefKeys.THEME] ?: "dark" }
    val accentColor: Flow<String> = context.dataStore.data.map { it[PrefKeys.ACCENT_COLOR] ?: "#dc2626" }
    val incognitoMode: Flow<Boolean> = context.dataStore.data.map { it[PrefKeys.INCOGNITO_MODE] ?: false }
    val newChapterAlerts: Flow<Boolean> = context.dataStore.data.map { it[PrefKeys.NEW_CHAPTER_ALERTS] ?: true }
    val backgroundUpdates: Flow<Boolean> = context.dataStore.data.map { it[PrefKeys.BACKGROUND_UPDATES] ?: true }
    val downloadWifiOnly: Flow<Boolean> = context.dataStore.data.map { it[PrefKeys.DOWNLOAD_WIFI_ONLY] ?: true }
    val readerDirection: Flow<String> = context.dataStore.data.map { it[PrefKeys.READER_DIRECTION] ?: "ltr" }
    val continuousScroll: Flow<Boolean> = context.dataStore.data.map { it[PrefKeys.CONTINUOUS_SCROLL] ?: false }
    val fullScreen: Flow<Boolean> = context.dataStore.data.map { it[PrefKeys.FULL_SCREEN] ?: true }
    val keepScreenOn: Flow<Boolean> = context.dataStore.data.map { it[PrefKeys.KEEP_SCREEN_ON] ?: true }
    val showPageNumber: Flow<Boolean> = context.dataStore.data.map { it[PrefKeys.SHOW_PAGE_NUMBER] ?: true }
    val cropBorders: Flow<Boolean> = context.dataStore.data.map { it[PrefKeys.CROP_BORDERS] ?: false }
    val dualPageSpread: Flow<String> = context.dataStore.data.map { it[PrefKeys.DUAL_PAGE_SPREAD] ?: "off" }
    val tapZones: Flow<String> = context.dataStore.data.map { it[PrefKeys.TAP_ZONES] ?: "default" }
    val sidePadding: Flow<String> = context.dataStore.data.map { it[PrefKeys.SIDE_PADDING] ?: "0" }
    val updateInterval: Flow<String> = context.dataStore.data.map { it[PrefKeys.UPDATE_INTERVAL] ?: "manual" }
    val autoDownloadNew: Flow<Boolean> = context.dataStore.data.map { it[PrefKeys.AUTO_DOWNLOAD_NEW] ?: false }
    val libraryDisplay: Flow<String> = context.dataStore.data.map { it[PrefKeys.LIBRARY_DISPLAY] ?: "grid" }
    val anilistConnected: Flow<Boolean> = context.dataStore.data.map { it[PrefKeys.ANILIST_CONNECTED] ?: false }
    val malConnected: Flow<Boolean> = context.dataStore.data.map { it[PrefKeys.MAL_CONNECTED] ?: false }
    val syncWifiOnly: Flow<Boolean> = context.dataStore.data.map { it[PrefKeys.SYNC_WIFI_ONLY] ?: true }
    val backendUrl: Flow<String> = context.dataStore.data.map { it[PrefKeys.BACKEND_URL] ?: "" }
    val apiKey: Flow<String> = context.dataStore.data.map { it[PrefKeys.API_KEY] ?: "" }
    val ambilight: Flow<Boolean> = context.dataStore.data.map { it[PrefKeys.AMBILIGHT] ?: false }

    suspend fun set(key: Preferences.Key<String>, value: String) {
        context.dataStore.edit { it[key] = value }
    }
    suspend fun set(key: Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit { it[key] = value }
    }
    suspend fun set(key: Preferences.Key<Long>, value: Long) {
        context.dataStore.edit { it[key] = value }
    }

    companion object {
        @Volatile private var INSTANCE: AppPreferences? = null
        fun getInstance(context: Context): AppPreferences =
            INSTANCE ?: synchronized(this) { AppPreferences(context.applicationContext).also { INSTANCE = it } }
    }
}

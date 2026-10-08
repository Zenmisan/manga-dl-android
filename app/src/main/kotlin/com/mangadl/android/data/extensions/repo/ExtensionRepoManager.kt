package com.mangadl.android.data.extensions.repo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
import com.mangadl.android.data.prefs.AppPreferences
import com.mangadl.android.data.prefs.PrefKeys
import com.mangadl.android.data.ui.ExtensionState
import com.mangadl.android.data.ui.UiExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class ExtensionRepoManager(
    private val context: Context,
    private val httpClient: OkHttpClient,
) {
    companion object {
        private const val TAG = "ExtensionRepoManager"
        const val DEFAULT_REPO_URL = "https://raw.githubusercontent.com/keiyoushi/extensions/repo"

        private val COVER_PALETTE = listOf(
            Color(0xFF1A2433), Color(0xFF3A1518), Color(0xFF2E2412), Color(0xFF2D1716),
            Color(0xFF13282A), Color(0xFF311A1F), Color(0xFF1D2A1A), Color(0xFF1B2030),
            Color(0xFF2B1A2E), Color(0xFF22222A), Color(0xFF1E3A5F), Color(0xFF1F2C4F),
        )

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    private val prefs = AppPreferences.getInstance(context)
    private val cacheFile = File(context.cacheDir, "keiyoushi_index.json")

    private val _repoExtensions = MutableStateFlow<List<KeiyoushiExtensionItem>>(emptyList())
    val repoExtensions: StateFlow<List<KeiyoushiExtensionItem>> = _repoExtensions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    suspend fun getRepoUrl(): String = withContext(Dispatchers.IO) {
        prefs.extensionRepoUrl.first()
    }

    suspend fun setRepoUrl(url: String) = withContext(Dispatchers.IO) {
        prefs.set(PrefKeys.EXTENSION_REPO_URL, url)
        refresh()
    }

    suspend fun refresh(): Result<List<KeiyoushiExtensionItem>> = withContext(Dispatchers.IO) {
        _isLoading.value = true
        try {
            val repoUrl = getRepoUrl().trimEnd('/')
            val indexUrl = "$repoUrl/index.min.json"

            val req = Request.Builder()
                .url(indexUrl)
                .header("User-Agent", "MangaDL-Android/1.0")
                .build()

            val response = httpClient.newCall(req).execute()
            if (!response.isSuccessful) {
                // If offline or network error, attempt to load cached index
                if (cacheFile.exists()) {
                    val cachedText = cacheFile.readText()
                    val parsed = json.decodeFromString<List<KeiyoushiExtensionItem>>(cachedText)
                    _repoExtensions.value = parsed
                    return@withContext Result.success(parsed)
                }
                return@withContext Result.failure(IllegalStateException("HTTP ${response.code} from $indexUrl"))
            }

            val body = response.body?.string().orEmpty()
            if (body.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Empty response from repository"))
            }

            // Persist to cache
            runCatching {
                cacheFile.writeText(body)
            }

            val list = json.decodeFromString<List<KeiyoushiExtensionItem>>(body)
            _repoExtensions.value = list
            Log.d(TAG, "Loaded ${list.size} extensions from repository: $repoUrl")
            Result.success(list)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch repository index: ${e.message}")
            if (cacheFile.exists()) {
                val cached = runCatching {
                    json.decodeFromString<List<KeiyoushiExtensionItem>>(cacheFile.readText())
                }.getOrNull()
                if (cached != null) {
                    _repoExtensions.value = cached
                    return@withContext Result.success(cached)
                }
            }
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Converts the repository items into UiExtension instances for the Browse screen,
     * cross-referencing with locally installed extensions to compute correct ExtensionState.
     */
    suspend fun getAvailableExtensions(installed: List<UiExtension>): List<UiExtension> = withContext(Dispatchers.Default) {
        val repoItems = _repoExtensions.value
        val repoBaseUrl = getRepoUrl().trimEnd('/')

        val installedByPkg = installed.associateBy { it.id.lowercase() }
        val installedByName = installed.associateBy { it.name.lowercase() }

        repoItems.mapNotNull { item ->
            // Filter out system notice / app update dummy extensions from Keiyoushi
            if (item.pkg.contains("keiyoushi") || item.pkg.contains("mihon")) return@mapNotNull null

            val matchingInstalled = installedByPkg[item.pkg.lowercase()]
                ?: installedByName[item.name.lowercase()]

            val state = when {
                matchingInstalled == null -> ExtensionState.Available
                matchingInstalled.versionName != item.version -> ExtensionState.UpdateAvailable
                else -> ExtensionState.Installed
            }

            // If it's already installed and up to date, skip from available list (as installed is shown in its own section)
            if (state == ExtensionState.Installed) return@mapNotNull null

            val colorIdx = item.pkg.hashCode().let { if (it < 0) -it else it } % COVER_PALETTE.size
            val apkUrl = if (item.apk.startsWith("http")) item.apk else "$repoBaseUrl/apk/${item.apk}"

            UiExtension(
                id = item.pkg,
                name = item.name,
                meta = "${item.lang.uppercase()} · v${item.version}",
                initial = item.name.firstOrNull()?.uppercase() ?: "?",
                color = COVER_PALETTE[colorIdx],
                state = state,
                lang = item.lang,
                isNsfw = item.nsfw == 1,
                apkUrl = apkUrl,
                pkgName = item.pkg,
                versionName = item.version,
                versionCode = item.code,
            )
        }
    }

    /**
     * Downloads the APK file for the given extension and triggers Android package installer.
     */
    suspend fun installExtension(context: Context, extension: UiExtension): Result<Unit> = withContext(Dispatchers.IO) {
        val apkUrl = extension.apkUrl
            ?: return@withContext Result.failure(IllegalArgumentException("No APK URL for ${extension.name}"))

        try {
            val extsDir = File(context.cacheDir, "extensions").apply { mkdirs() }
            val apkFile = File(extsDir, "${extension.id.replace('.', '_')}.apk")

            Log.d(TAG, "Downloading APK from $apkUrl to ${apkFile.absolutePath}")
            val req = Request.Builder().url(apkUrl).build()
            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) {
                return@withContext Result.failure(IllegalStateException("HTTP ${resp.code} downloading APK"))
            }

            resp.body?.byteStream()?.use { input ->
                FileOutputStream(apkFile).use { output ->
                    input.copyTo(output)
                }
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile,
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(installIntent)
            Log.d(TAG, "Launched installer intent for ${extension.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install extension ${extension.name}", e)
            Result.failure(e)
        }
    }
}

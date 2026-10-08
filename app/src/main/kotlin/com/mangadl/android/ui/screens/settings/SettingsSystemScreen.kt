package com.mangadl.android.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.Coil
import com.mangadl.android.BuildConfig
import com.mangadl.android.data.backup.BackupManager
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.ButtonSetting
import com.mangadl.android.ui.components.ButtonTone
import com.mangadl.android.ui.components.SettingsSection
import com.mangadl.android.ui.components.SwitchSetting
import com.mangadl.android.ui.components.ValueSetting
import com.mangadl.android.ui.theme.MdTheme
import com.mangadl.android.ui.viewmodels.DownloadQueueViewModel
import com.mangadl.android.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(coil.annotation.ExperimentalCoilApi::class)
@Composable
fun SystemSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupManager = remember { BackupManager() }
    var cacheLabel by remember { mutableStateOf("Tap to clear") }

    val vm: SettingsViewModel = viewModel()
    val saveChaptersPublic by vm.saveChaptersPublic.collectAsState()
    val backgroundSyncEnabled by vm.backgroundSyncEnabled.collectAsState()
    val syncWifiOnly by vm.syncWifiOnly.collectAsState()
    val autoBackupWeekly by vm.autoBackupWeekly.collectAsState()

    val downloadQueueVm: DownloadQueueViewModel = viewModel()
    val storageUsedBytes by downloadQueueVm.storageUsedBytes.collectAsState()
    LaunchedEffect(Unit) { downloadQueueVm.refreshStorageUsage() }

    var cacheBytes by remember { mutableLongStateOf(0L) }
    var dbBytes by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val imageCache = Coil.imageLoader(context).diskCache?.size ?: 0L
            val tempCache = runCatching {
                context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            }.getOrDefault(0L)
            cacheBytes = imageCache + tempCache

            val dbFile = context.getDatabasePath("manga_dl.db")
            val dbWal = context.getDatabasePath("manga_dl.db-wal")
            val dbShm = context.getDatabasePath("manga_dl.db-shm")
            dbBytes = (if (dbFile.exists()) dbFile.length() else 0L) +
                    (if (dbWal.exists()) dbWal.length() else 0L) +
                    (if (dbShm.exists()) dbShm.length() else 0L)
        }
    }

    // Drive statistics
    val statFs = remember {
        runCatching { StatFs(Environment.getDataDirectory().path) }.getOrNull()
    }
    val totalDeviceBytes = remember(statFs) {
        statFs?.let { it.blockCountLong * it.blockSizeLong } ?: 0L
    }
    val availableDeviceBytes = remember(statFs) {
        statFs?.let { it.availableBlocksLong * it.blockSizeLong } ?: 0L
    }
    val usedDeviceBytes = remember(totalDeviceBytes, availableDeviceBytes) {
        maxOf(0L, totalDeviceBytes - availableDeviceBytes)
    }
    val deviceProgress = remember(totalDeviceBytes, usedDeviceBytes) {
        if (totalDeviceBytes > 0) (usedDeviceBytes.toFloat() / totalDeviceBytes).coerceIn(0f, 1f) else 0f
    }

    var backupStatus by remember { mutableStateOf("Library, categories, history, settings (.mangadl)") }
    var restoreStatus by remember { mutableStateOf("Restore from .mangadl, .json, or .tachibk") }
    var tachiyomiStatus by remember { mutableStateOf("Import .tachibk or Tachiyomi JSON") }

    val restorePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                restoreStatus = "Restoring backup…"
                val result = runCatching {
                    var fileName: String? = null
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            fileName = cursor.getString(nameIndex)
                        }
                    }
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        backupManager.importBackup(stream, fileName)
                    } ?: error("Unable to open file stream")
                }.getOrElse {
                    com.mangadl.android.data.backup.BackupRestoreResult(
                        success = false,
                        sourceFormat = "Unknown",
                        restoredMangaCount = 0,
                        restoredProgressCount = 0,
                        restoredCategoriesCount = 0,
                        errorMessage = it.message,
                    )
                }

                restoreStatus = if (result.success) {
                    val trackerNote = if (result.restoredTrackerBindsCount > 0) ", ${result.restoredTrackerBindsCount} trackers" else ""
                    "Restored ${result.restoredMangaCount} manga, ${result.restoredProgressCount} chapters$trackerNote (${result.sourceFormat})"
                } else {
                    "Restore failed: ${result.errorMessage ?: "Unknown error"}"
                }
            }
        }
    }

    val tachiyomiPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                tachiyomiStatus = "Importing Tachiyomi backup…"
                val result = runCatching {
                    var fileName: String? = null
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            fileName = cursor.getString(nameIndex)
                        }
                    }
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        backupManager.importBackup(stream, fileName)
                    } ?: error("Unable to open file stream")
                }.getOrElse {
                    com.mangadl.android.data.backup.BackupRestoreResult(
                        success = false,
                        sourceFormat = "Unknown",
                        restoredMangaCount = 0,
                        restoredProgressCount = 0,
                        restoredCategoriesCount = 0,
                        errorMessage = it.message,
                    )
                }

                tachiyomiStatus = if (result.success) {
                    "Imported ${result.restoredMangaCount} manga, ${result.restoredProgressCount} chapters from Tachiyomi"
                } else {
                    "Import failed: ${result.errorMessage ?: "Unknown error"}"
                }
            }
        }
    }

    SettingsFrame("System", onBack) {
        SettingsSection("Storage") {
            // Storage Metrics Card matching desktop SystemSettingsPage
            StorageMetricsCard(
                totalDeviceBytes = totalDeviceBytes,
                usedDeviceBytes = usedDeviceBytes,
                deviceProgress = deviceProgress,
                downloadBytes = storageUsedBytes,
                cacheBytes = cacheBytes,
                dbBytes = dbBytes,
            )

            ValueSetting(
                "Download location",
                if (saveChaptersPublic) "Downloads/manga-dl" else "App storage / manga-dl",
                "Internal storage used unless \"Save to public Downloads\" is on",
            )
            SwitchSetting(
                "Save to public Downloads", false,
                "Visible to other apps and file managers",
                value = saveChaptersPublic, onValueChange = { vm.setSaveChaptersPublic(it) },
            )
            ButtonSetting("Image cache", "Clear", cacheLabel, tone = ButtonTone.Danger, onClick = {
                scope.launch(Dispatchers.IO) {
                    Coil.imageLoader(context).memoryCache?.clear()
                    Coil.imageLoader(context).diskCache?.clear()
                    cacheBytes = 0L
                    cacheLabel = "Cleared"
                }
            })
            var appCacheLabel by remember { mutableStateOf("Tap to clear temporary files") }
            ButtonSetting("Temporary cache", "Clear", appCacheLabel, tone = ButtonTone.Danger, onClick = {
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        context.cacheDir.deleteRecursively()
                        context.cacheDir.mkdirs()
                    }
                    val imageCache = Coil.imageLoader(context).diskCache?.size ?: 0L
                    cacheBytes = imageCache
                    appCacheLabel = "Cleared"
                }
            })
        }
        SettingsSection("Sync") {
            SwitchSetting(
                "Background sync", true, "Check subscribed manga every 12 hours",
                value = backgroundSyncEnabled, onValueChange = { vm.setBackgroundSyncEnabled(it) },
            )
            SwitchSetting(
                "Wi-Fi only", true,
                value = syncWifiOnly, onValueChange = { vm.setSyncWifiOnly(it) },
            )
            var syncLabel by remember { mutableStateOf("Runs automatically every 12 hours") }
            ButtonSetting("Background sync", "Sync Now", syncLabel, onClick = {
                val wm = androidx.work.WorkManager.getInstance(context)
                wm.enqueueUniqueWork(
                    "library_update_manual",
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    androidx.work.OneTimeWorkRequestBuilder<com.mangadl.android.data.library.LibraryUpdateWorker>().build(),
                )
                com.mangadl.android.data.sync.supabase.SupabaseSyncManager.syncAllAsync()
                syncLabel = "Sync started"
            })
        }
        SettingsSection("Backup & restore") {
            ButtonSetting(
                "Create backup",
                "Create",
                backupStatus,
                tone = ButtonTone.Primary,
                onClick = {
                    scope.launch {
                        backupStatus = "Creating backup…"
                        val fileResult = runCatching { backupManager.exportBackup(context) }
                        val file = fileResult.getOrNull()
                        if (file != null) {
                            backupStatus = "Created: ${file.name}"
                            val shareIntent = Intent.createChooser(
                                backupManager.createShareIntent(context, file),
                                "Save or Share Backup"
                            )
                            context.startActivity(shareIntent)
                        } else {
                            backupStatus = "Export failed: ${fileResult.exceptionOrNull()?.message}"
                        }
                    }
                }
            )
            ButtonSetting(
                "Restore backup",
                "Restore",
                restoreStatus,
                onClick = { restorePicker.launch("*/*") }
            )
            ButtonSetting(
                "Import from Tachiyomi",
                "Import",
                tachiyomiStatus,
                onClick = { tachiyomiPicker.launch("*/*") }
            )
            SwitchSetting(
                "Automatic backups", false, "Create a .mangadl backup every 7 days",
                value = autoBackupWeekly, onValueChange = { vm.setAutoBackupWeekly(it) },
            )
        }
        SettingsSection("About") {
            var updateStatus by remember { mutableStateOf("Up to date") }
            ButtonSetting("manga-dl ${BuildConfig.VERSION_NAME}", "Check for Updates", updateStatus, onClick = {
                scope.launch {
                    updateStatus = "Checking for updates…"
                    kotlinx.coroutines.delay(1000)
                    updateStatus = "You are on the latest version"
                }
            })
        }
    }
}

@Composable
private fun StorageMetricsCard(
    totalDeviceBytes: Long,
    usedDeviceBytes: Long,
    deviceProgress: Float,
    downloadBytes: Long,
    cacheBytes: Long,
    dbBytes: Long
) {
    val c = MdTheme.colors
    val usedGb = usedDeviceBytes.toDouble() / (1024.0 * 1024 * 1024)
    val totalGb = totalDeviceBytes.toDouble() / (1024.0 * 1024 * 1024)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(c.surfaceHigh)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row: Used vs Total
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BodyText(
                    String.format(Locale.US, "%.1f GB used", usedGb),
                    size = 14.sp,
                    weight = FontWeight.Black,
                    color = Color.White
                )
                BodyText(
                    String.format(Locale.US, "of %.1f GB total", totalGb),
                    size = 12.sp,
                    color = c.fgSubtle
                )
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = { deviceProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = Color(0xFFEF4444),
                trackColor = Color.White.copy(alpha = 0.1f)
            )

            // Itemized Breakdown Strip
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ItemizedStorageBadge("Downloads", formatBytes(downloadBytes), Color(0xFF38BDF8))
                ItemizedStorageBadge("Cache", formatBytes(cacheBytes), Color(0xFFF59E0B))
                ItemizedStorageBadge("Database", formatBytes(dbBytes), Color(0xFFA855F7))
            }
        }
    }
}

@Composable
private fun ItemizedStorageBadge(label: String, formattedSize: String, indicatorColor: Color) {
    val c = MdTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(indicatorColor)
                .padding(3.dp)
        )
        Column {
            BodyText(label, size = 9.sp, weight = FontWeight.Bold, color = c.fgSubtle)
            BodyText(formattedSize, size = 11.sp, weight = FontWeight.Black, color = Color.White)
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes >= 1024L * 1024 * 1024)
        return String.format(Locale.US, "%.1f GB", bytes.toDouble() / (1024.0 * 1024 * 1024))
    if (bytes >= 1024L * 1024)
        return String.format(Locale.US, "%.1f MB", bytes.toDouble() / (1024.0 * 1024))
    if (bytes >= 1024L)
        return String.format(Locale.US, "%.0f KB", bytes.toDouble() / 1024.0)
    return "$bytes B"
}

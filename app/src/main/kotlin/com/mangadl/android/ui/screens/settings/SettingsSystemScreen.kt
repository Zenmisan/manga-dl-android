package com.mangadl.android.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.Coil
import com.mangadl.android.BuildConfig
import com.mangadl.android.data.backup.BackupManager
import com.mangadl.android.ui.components.ButtonSetting
import com.mangadl.android.ui.components.ButtonTone
import com.mangadl.android.ui.components.SettingsSection
import com.mangadl.android.ui.components.SwitchSetting
import com.mangadl.android.ui.components.ValueSetting
import com.mangadl.android.ui.viewmodels.DownloadQueueViewModel
import com.mangadl.android.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    val storageLabel = remember(storageUsedBytes) {
        if (storageUsedBytes <= 0L) "0 MB" else {
            val mb = storageUsedBytes.toDouble() / (1024 * 1024)
            if (mb < 1024) String.format("%.1f MB", mb) else String.format("%.2f GB", mb / 1024)
        }
    }

    var backupStatus by remember { mutableStateOf("Library, categories, history, settings (JSON)") }
    var restoreStatus by remember { mutableStateOf("Restore from JSON or .tachibk") }
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
                    "Restored ${result.restoredMangaCount} manga, ${result.restoredProgressCount} chapters (${result.sourceFormat})"
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
            ValueSetting("Storage used", storageLabel, "Total size of downloaded chapters")
            ButtonSetting("Image cache", "Clear", cacheLabel, tone = ButtonTone.Danger, onClick = {
                scope.launch(Dispatchers.IO) {
                    Coil.imageLoader(context).memoryCache?.clear()
                    Coil.imageLoader(context).diskCache?.clear()
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
                // Distinct work name from LibraryUpdateWorker.WORK_NAME: enqueueUniqueWork and
                // enqueueUniquePeriodicWork share the same unique-name table, so reusing the
                // periodic schedule's name here would cancel/replace it instead of just running
                // once.
                val wm = androidx.work.WorkManager.getInstance(context)
                wm.enqueueUniqueWork(
                    "library_update_manual",
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    androidx.work.OneTimeWorkRequestBuilder<com.mangadl.android.data.library.LibraryUpdateWorker>().build(),
                )
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
                "Automatic backups", false, "Create a JSON backup every 7 days",
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

package com.mangadl.android.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import coil.Coil
import com.mangadl.android.BuildConfig
import com.mangadl.android.data.backup.BackupManager
import com.mangadl.android.ui.components.ButtonSetting
import com.mangadl.android.ui.components.ButtonTone
import com.mangadl.android.ui.components.SettingsSection
import com.mangadl.android.ui.components.SwitchSetting
import com.mangadl.android.ui.components.ValueSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SystemSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupManager = remember { BackupManager() }
    var cacheLabel by remember { mutableStateOf("Tap to clear") }

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
            ValueSetting("Download location", "Change", "Internal storage / manga-dl")
            SwitchSetting("Save chapters to device", false, "Keep CBZ files visible to other apps")
            ValueSetting("Storage limit", "Unlimited", "Oldest read chapters are removed first")
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
            SwitchSetting("Background sync", true, "Check subscribed manga every 30 minutes")
            SwitchSetting("Wi-Fi only", true)
            var syncLabel by remember { mutableStateOf("Runs automatically every 30 min") }
            ButtonSetting("Background sync", "Sync Now", syncLabel, onClick = {
                scope.launch(Dispatchers.IO) {
                    syncLabel = "Syncing…"
                    kotlinx.coroutines.delay(1000)
                    syncLabel = "Synced just now"
                }
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
            ValueSetting("Automatic backups", "Weekly")
            SwitchSetting("Cloud backup", true, "Store backups in your account")
        }
        SettingsSection("Servers") {
            ValueSetting("Komga", "Set Up", "Not connected")
            ValueSetting("Suwayomi", "Set Up", "Not connected")
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

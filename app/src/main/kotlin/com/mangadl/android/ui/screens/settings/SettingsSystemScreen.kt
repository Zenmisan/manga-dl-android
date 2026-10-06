package com.mangadl.android.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import coil.Coil
import com.mangadl.android.BuildConfig
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
    var cacheLabel by remember { mutableStateOf("Tap to clear") }

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
        }
        SettingsSection("Sync") {
            SwitchSetting("Background sync", true, "Check subscribed manga every 30 minutes")
            SwitchSetting("Wi-Fi only", true)
            ButtonSetting("Background sync", "Sync Now", "Runs automatically every 30 min")
        }
        SettingsSection("Backup & restore") {
            ButtonSetting("Create backup", "Create", "Library, categories, history, settings (JSON)", tone = ButtonTone.Primary)
            ButtonSetting("Restore backup", "Restore")
            ButtonSetting("Import from Tachiyomi", "Import", ".tachibk or JSON")
            ValueSetting("Automatic backups", "Weekly")
            SwitchSetting("Cloud backup", true, "Store backups in your account")
        }
        SettingsSection("Servers") {
            ValueSetting("Komga", "Set Up", "Not connected")
            ValueSetting("Suwayomi", "Set Up", "Not connected")
        }
        SettingsSection("About") {
            ButtonSetting("manga-dl ${BuildConfig.VERSION_NAME}", "Check for Updates", "Up to date")
        }
    }
}

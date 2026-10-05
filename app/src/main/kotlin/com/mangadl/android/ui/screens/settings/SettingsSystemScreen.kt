package com.mangadl.android.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import coil.Coil
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

    SettingsFrame("System", onBack) {
        SettingsSection("Storage") {
            ValueSetting("Download location", "Change", "Internal storage / manga-dl")
            SwitchSetting("Save chapters to device", false, "Keep CBZ files visible to other apps")
            ValueSetting("Storage limit", "[n] GB", "Oldest read chapters are removed first")
            ButtonSetting("Image cache", "Clear", "[size] used", tone = ButtonTone.Danger, onClick = {
                scope.launch(Dispatchers.IO) {
                    Coil.imageLoader(context).memoryCache?.clear()
                    Coil.imageLoader(context).diskCache?.clear()
                }
            })
        }
        SettingsSection("Sync") {
            SwitchSetting("Background sync", true, "Check subscribed manga every 30 minutes")
            SwitchSetting("Wi-Fi only", true)
            ButtonSetting("Last synced [time]", "Sync Now")
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
            ValueSetting("Suwayomi", "Edit", "Connected · [url]")
        }
        SettingsSection("About") {
            ButtonSetting("manga-dl [version]", "Check for Updates", "Up to date")
        }
    }
}

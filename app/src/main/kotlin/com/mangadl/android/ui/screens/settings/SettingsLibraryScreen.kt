package com.mangadl.android.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mangadl.android.ui.components.SegmentedSetting
import com.mangadl.android.ui.components.SettingsSection
import com.mangadl.android.ui.components.SwitchSetting
import com.mangadl.android.ui.components.ValueSetting
import com.mangadl.android.ui.viewmodels.SettingsViewModel

@Composable
fun LibrarySettingsScreen(onBack: () -> Unit, onMigrate: () -> Unit) {
    val vm: SettingsViewModel = viewModel()
    val wifiOnly by vm.downloadWifiOnly.collectAsState()
    val autoDownload by vm.autoDownloadNew.collectAsState()

    SettingsFrame("Library", onBack) {
        SettingsSection("Display") {
            SegmentedSetting("Grid columns", listOf("Auto", "2", "3", "4"), "3")
            SwitchSetting("Unread badges", true)
            SwitchSetting("Downloaded badges", true)
        }
        SettingsSection("Categories") {
            ValueSetting("Default category", "Reading")
            ValueSetting("Edit categories", "4", "Rename, reorder or delete")
        }
        SettingsSection("Updates") {
            SwitchSetting("Check on app launch", true)
            SwitchSetting("Only ongoing series", true, "Skip completed manga when checking")
            SwitchSetting(
                "Only on Wi-Fi", true,
                value = wifiOnly, onValueChange = { vm.setDownloadWifiOnly(it) },
            )
            SwitchSetting(
                "Auto-download new chapters", false,
                value = autoDownload, onValueChange = { vm.setAutoDownloadNew(it) },
            )
        }
        SettingsSection("Migration") {
            ValueSetting("Migrate manga", "", "Move titles to another source and keep progress", onMigrate)
        }
    }
}

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
    val gridColumns by vm.gridColumns.collectAsState()
    val showUnreadBadges by vm.showUnreadBadges.collectAsState()
    val showDownloadedBadges by vm.showDownloadedBadges.collectAsState()

    SettingsFrame("Library", onBack) {
        SettingsSection("Display") {
            SegmentedSetting(
                "Grid columns", listOf("Auto", "2", "3", "4"), "3",
                value = gridColumns, onValueChange = { vm.setGridColumns(it) },
            )
            SwitchSetting(
                "Unread badges", true,
                value = showUnreadBadges, onValueChange = { vm.setShowUnreadBadges(it) },
            )
            SwitchSetting(
                "Downloaded badges", true,
                value = showDownloadedBadges, onValueChange = { vm.setShowDownloadedBadges(it) },
            )
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

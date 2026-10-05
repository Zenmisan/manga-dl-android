package com.mangadl.android.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mangadl.android.ui.components.AccentSetting
import com.mangadl.android.ui.components.BackHeader
import com.mangadl.android.ui.components.ButtonSetting
import com.mangadl.android.ui.components.InputSetting
import com.mangadl.android.ui.components.Screen
import com.mangadl.android.ui.components.SegmentedSetting
import com.mangadl.android.ui.components.SettingsSection
import com.mangadl.android.ui.components.SwitchSetting
import com.mangadl.android.ui.theme.Accent
import com.mangadl.android.ui.viewmodels.SettingsViewModel

@Composable
fun GeneralSettingsScreen(onBack: () -> Unit, accent: Accent, onAccentChange: (Accent) -> Unit) {
    val vm: SettingsViewModel = viewModel()
    val theme by vm.theme.collectAsState()
    val ambilight by vm.ambilight.collectAsState()
    val incognito by vm.incognito.collectAsState()
    val newChapterAlerts by vm.newChapterAlerts.collectAsState()
    val autoDownload by vm.autoDownloadNew.collectAsState()
    val backendUrl by vm.backendUrl.collectAsState()
    val apiKey by vm.apiKey.collectAsState()

    SettingsFrame("General", onBack) {
        SettingsSection("Appearance") {
            SegmentedSetting(
                "Theme", listOf("Dark", "Light", "System"), "Dark",
                value = theme.replaceFirstChar { it.uppercaseChar() },
                onValueChange = { vm.setTheme(it.lowercase()) },
            )
            AccentSetting("Accent color", accent, onAccentChange)
            SwitchSetting(
                "Ambilight from covers", false,
                "Tint headers and the status bar with the cover colour",
                value = ambilight, onValueChange = { vm.setAmbilight(it) },
            )
        }
        SettingsSection("Connection") {
            InputSetting(
                "Backend URL", "",
                "Used for sync and backups only",
                value = backendUrl, onValueChange = { vm.setBackendUrl(it) },
            )
            InputSetting(
                "API key", "", isPassword = true,
                value = apiKey, onValueChange = { vm.setApiKey(it) },
            )
            ButtonSetting("Status: connected", "Test", "Last checked [time]")
        }
        SettingsSection("Behaviour") {
            SwitchSetting(
                "Incognito mode", false, "Pause history while reading",
                value = incognito, onValueChange = { vm.setIncognito(it) },
            )
            SwitchSetting(
                "Push notifications", true, "New chapters and finished downloads",
                value = newChapterAlerts, onValueChange = { vm.setNewChapterAlerts(it) },
            )
            SwitchSetting(
                "Auto-download", false, "Download new chapters when on Wi-Fi",
                value = autoDownload, onValueChange = { vm.setAutoDownloadNew(it) },
            )
            SwitchSetting("Haptic feedback", true, "Vibrate lightly on page turn")
            SwitchSetting("Biometric app lock", false, "Require fingerprint or face to open")
        }
    }
}

@Composable
internal fun SettingsFrame(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Screen {
        BackHeader(title, onBack, Modifier.padding(bottom = 0.dp))
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            content = content,
        )
    }
}

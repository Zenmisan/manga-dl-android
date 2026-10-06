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
fun ReaderSettingsScreen(onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel()
    val readerDir by vm.readerDirection.collectAsState()
    val dualPage by vm.dualPageSpread.collectAsState()
    val cropBorders by vm.cropBorders.collectAsState()
    val tapZones by vm.tapZones.collectAsState()
    val keepScreenOn by vm.keepScreenOn.collectAsState()
    val showPageNumber by vm.showPageNumber.collectAsState()
    val fullScreen by vm.fullScreen.collectAsState()

    SettingsFrame("Reader", onBack) {
        SettingsSection("Reading") {
            SegmentedSetting(
                "Default reading mode", listOf("LTR", "RTL", "Webtoon", "Vertical"), "LTR",
                value = when (readerDir) { "ltr" -> "LTR"; "rtl" -> "RTL"; "webtoon" -> "Webtoon"; else -> "Vertical" },
                onValueChange = { vm.setReaderDirection(when (it) { "LTR" -> "ltr"; "RTL" -> "rtl"; "Webtoon" -> "webtoon"; else -> "vertical" }) },
            )
            SwitchSetting(
                "Dual-page spread", false, "Two pages side by side in landscape",
                value = dualPage != "off",
                onValueChange = { vm.setDualPageSpread(if (it) "auto" else "off") },
            )
            SwitchSetting(
                "Crop borders", false, "Trim white margins around pages",
                value = cropBorders, onValueChange = { vm.setCropBorders(it) },
            )
            SwitchSetting(
                "Tap zones", true, "Tap left or right edge to turn pages",
                value = tapZones != "disabled",
                onValueChange = { vm.setTapZones(if (it) "default" else "disabled") },
            )
            ValueSetting("Webtoon side padding", "5%")
        }
        SettingsSection("Controls") {
            SwitchSetting("Volume keys turn pages", true)
            SwitchSetting(
                "Keep screen on", true,
                value = keepScreenOn, onValueChange = { vm.setKeepScreenOn(it) },
            )
            SwitchSetting(
                "Show page number", true,
                value = showPageNumber, onValueChange = { vm.setShowPageNumber(it) },
            )
            SwitchSetting(
                "Fullscreen", true, "Hide system bars while reading",
                value = fullScreen, onValueChange = { vm.setFullScreen(it) },
            )
        }
        SettingsSection("Display") {
            SegmentedSetting("Background", listOf("Black", "Gray", "White"), "Black")
            ValueSetting("Image filters", "Off", "Brightness, contrast, grayscale, invert, sepia")
        }
    }
}

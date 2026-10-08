package com.mangadl.android.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mangadl.android.ui.components.SegmentedSetting
import com.mangadl.android.ui.components.SettingsSection
import com.mangadl.android.ui.components.SliderSetting
import com.mangadl.android.ui.components.SwitchSetting
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
    val sidePadding by vm.sidePadding.collectAsState()
    val volumeKeysTurnPages by vm.volumeKeysTurnPages.collectAsState()
    val readerBackground by vm.readerBackground.collectAsState()
    val ambilight by vm.ambilight.collectAsState()

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
            SliderSetting(
                "Webtoon side padding (%)", 0f,
                valueRange = 0f..80f,
                valueLabel = { "${it.toInt()}%" },
                value = sidePadding.toFloatOrNull() ?: 0f,
                onValueChange = { v -> vm.setSidePadding(v.toInt().toString()) },
            )
        }
        SettingsSection("Controls") {
            SwitchSetting(
                "Volume keys turn pages", true,
                value = volumeKeysTurnPages, onValueChange = { vm.setVolumeKeysTurnPages(it) },
            )
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
            SegmentedSetting(
                "Background", listOf("Black", "Gray", "White"), "Black",
                value = when (readerBackground) { "gray" -> "Gray"; "white" -> "White"; else -> "Black" },
                onValueChange = { vm.setReaderBackground(it.lowercase()) },
            )
            SwitchSetting(
                "Ambient lighting (Ambilight)", false, "Cast soft atmospheric glow around active pages",
                value = ambilight, onValueChange = { vm.setAmbilight(it) },
            )
        }
    }
}

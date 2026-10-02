package com.mangadl.android.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.data.prefs.AppPreferences
import com.mangadl.android.data.prefs.PrefKeys
import com.mangadl.android.ui.components.MangaDlSwitch
import com.mangadl.android.ui.components.SectionLabel
import com.mangadl.android.ui.theme.AntonStyleSub
import com.mangadl.android.ui.theme.MangaDlColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsReaderScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences.getInstance(context) }
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp > 600

    val readerDirection by prefs.readerDirection.collectAsState(initial = "ltr")
    val continuousScroll by prefs.continuousScroll.collectAsState(initial = false)
    val fullScreen by prefs.fullScreen.collectAsState(initial = true)
    val keepScreenOn by prefs.keepScreenOn.collectAsState(initial = true)
    val showPageNumber by prefs.showPageNumber.collectAsState(initial = true)
    val cropBorders by prefs.cropBorders.collectAsState(initial = false)
    val tapZones by prefs.tapZones.collectAsState(initial = "default")
    val dualPageSpread by prefs.dualPageSpread.collectAsState(initial = "off")
    val sidePaddingStr by prefs.sidePadding.collectAsState(initial = "0")
    val sidePadding = sidePaddingStr.toFloatOrNull() ?: 0f

    Column(
        Modifier
            .fillMaxSize()
            .background(MangaDlColors.Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 16.dp, bottom = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MangaDlColors.TextPrimary)
            }
            Text("Reader".uppercase(), style = AntonStyleSub, color = MangaDlColors.TextPrimary)
        }

        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            // Reading mode
            item {
                SectionLabel("Reading mode", color = MangaDlColors.SectionRed, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
            }
            item {
                ReaderSegmentedRow(
                    label = "Direction",
                    options = listOf("Left to Right" to "ltr", "Right to Left" to "rtl", "Vertical" to "vertical"),
                    selected = readerDirection,
                    onSelect = { scope.launch { prefs.set(PrefKeys.READER_DIRECTION, it) } },
                )
            }
            item {
                ReaderToggleRow("Continuous scroll", checked = continuousScroll, onCheckedChange = { scope.launch { prefs.set(PrefKeys.CONTINUOUS_SCROLL, it) } })
            }

            // Display
            item {
                SectionLabel("Display", color = MangaDlColors.SectionRed, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
            }
            item {
                ReaderToggleRow("Full screen", checked = fullScreen, onCheckedChange = { scope.launch { prefs.set(PrefKeys.FULL_SCREEN, it) } })
            }
            item {
                ReaderToggleRow("Keep screen on", checked = keepScreenOn, onCheckedChange = { scope.launch { prefs.set(PrefKeys.KEEP_SCREEN_ON, it) } })
            }
            item {
                ReaderToggleRow("Show page number", checked = showPageNumber, onCheckedChange = { scope.launch { prefs.set(PrefKeys.SHOW_PAGE_NUMBER, it) } })
            }
            item {
                ReaderToggleRow("Crop borders", checked = cropBorders, onCheckedChange = { scope.launch { prefs.set(PrefKeys.CROP_BORDERS, it) } })
            }

            // Navigation
            item {
                SectionLabel("Navigation", color = MangaDlColors.SectionRed, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
            }
            item {
                ReaderSegmentedRow(
                    label = "Tap zones",
                    options = listOf("Default" to "default", "L-Nav" to "lnav", "Edge" to "edge", "Disabled" to "disabled"),
                    selected = tapZones,
                    onSelect = { scope.launch { prefs.set(PrefKeys.TAP_ZONES, it) } },
                )
            }

            // Tablet-only dual-page spread
            if (isTablet) {
                item {
                    ReaderSegmentedRow(
                        label = "Dual-page spread",
                        options = listOf("Auto" to "auto", "Always" to "always", "Off" to "off"),
                        selected = dualPageSpread,
                        onSelect = { scope.launch { prefs.set(PrefKeys.DUAL_PAGE_SPREAD, it) } },
                    )
                }
            }

            // Webtoon
            item {
                SectionLabel("Webtoon", color = MangaDlColors.SectionRed, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
            }
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Side padding", color = MangaDlColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${sidePadding.toInt()}dp",
                            color = MangaDlColors.TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = sidePadding,
                        onValueChange = { scope.launch { prefs.set(PrefKeys.SIDE_PADDING, it.toInt().toString()) } },
                        valueRange = 0f..80f,
                        colors = SliderDefaults.colors(
                            thumbColor = MangaDlColors.Primary,
                            activeTrackColor = MangaDlColors.Primary,
                            inactiveTrackColor = Color(0x33FFFFFF),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ReaderToggleRow(
    label: String,
    desc: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = MangaDlColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (desc != null) {
                Text(desc, color = MangaDlColors.TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
        MangaDlSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
}

@Composable
private fun ReaderSegmentedRow(
    label: String,
    options: List<Pair<String, String>>, // label to value
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Text(label, color = MangaDlColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x0DFFFFFF))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEach { (optLabel, optValue) ->
                val active = selected == optValue
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (active) MangaDlColors.TextPrimary else Color.Transparent)
                        .clickable { onSelect(optValue) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        optLabel,
                        color = if (active) MangaDlColors.Background else MangaDlColors.TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
}

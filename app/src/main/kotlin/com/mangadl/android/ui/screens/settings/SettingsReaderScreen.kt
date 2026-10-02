package com.mangadl.android.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.ui.components.MangaDlSwitch
import com.mangadl.android.ui.components.SectionLabel
import com.mangadl.android.ui.theme.AntonStyleSub
import com.mangadl.android.ui.theme.MangaDlColors

@Composable
fun SettingsReaderScreen(onBack: () -> Unit) {
    var continuousScroll by remember { mutableStateOf(false) }
    var fullScreen by remember { mutableStateOf(true) }
    var keepScreenOn by remember { mutableStateOf(true) }
    var showPageNumber by remember { mutableStateOf(true) }
    var volumeKeys by remember { mutableStateOf(false) }
    var selectedDirection by remember { mutableStateOf(0) }

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
            item {
                SectionLabel("Reading mode", color = MangaDlColors.SectionRed, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
            }
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text("Direction", color = MangaDlColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x0DFFFFFF))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf("LTR", "RTL", "Vertical").forEachIndexed { i, label ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (i == selectedDirection) MangaDlColors.TextPrimary else Color.Transparent)
                                    .clickable { selectedDirection = i },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    label,
                                    color = if (i == selectedDirection) MangaDlColors.Background else MangaDlColors.TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
            }
            item {
                ReaderToggleRow("Continuous scroll", checked = continuousScroll, onCheckedChange = { continuousScroll = it })
            }

            item {
                SectionLabel("Display", color = MangaDlColors.SectionRed, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
            }
            item { ReaderToggleRow("Full screen", checked = fullScreen, onCheckedChange = { fullScreen = it }) }
            item { ReaderToggleRow("Keep screen on", checked = keepScreenOn, onCheckedChange = { keepScreenOn = it }) }
            item { ReaderToggleRow("Show page number", checked = showPageNumber, onCheckedChange = { showPageNumber = it }) }

            item {
                SectionLabel("Navigation", color = MangaDlColors.SectionRed, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
            }
            item { ReaderToggleRow("Volume keys", desc = "Use volume buttons to turn pages", checked = volumeKeys, onCheckedChange = { volumeKeys = it }) }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 60.dp)
                        .clickable {}
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Tap zones", color = MangaDlColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("L/R sides", color = MangaDlColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0x80FFFFFF), modifier = Modifier.size(16.dp))
                    }
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

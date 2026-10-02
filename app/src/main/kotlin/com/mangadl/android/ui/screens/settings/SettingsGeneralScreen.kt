package com.mangadl.android.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
fun SettingsGeneralScreen(onBack: () -> Unit) {
    var newChapterAlerts by remember { mutableStateOf(true) }
    var backgroundUpdates by remember { mutableStateOf(true) }
    var wifiOnly by remember { mutableStateOf(true) }

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
            Text("General".uppercase(), style = AntonStyleSub, color = MangaDlColors.TextPrimary)
        }

        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            item {
                SectionLabel(
                    "Appearance",
                    color = MangaDlColors.SectionRed,
                    modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
                )
            }
            item {
                SettingsRowSegmented(
                    label = "Theme",
                    options = listOf("Dark", "Light", "System"),
                    selected = 0,
                    onSelect = {},
                )
            }
            item {
                SettingsRowValue(label = "Accent colour", value = "Red", onClick = {})
            }

            item {
                SectionLabel(
                    "Notifications",
                    color = MangaDlColors.SectionRed,
                    modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
                )
            }
            item {
                SettingsRowSwitch(
                    label = "New chapter alerts",
                    desc = "Notify when tracked manga updates",
                    checked = newChapterAlerts,
                    onCheckedChange = { newChapterAlerts = it },
                )
            }
            item {
                SettingsRowSwitch(
                    label = "Background updates",
                    desc = "Check for chapters in background",
                    checked = backgroundUpdates,
                    onCheckedChange = { backgroundUpdates = it },
                )
            }
            item {
                SettingsRowValue(label = "Update interval", value = "30 min", onClick = {})
            }

            item {
                SectionLabel(
                    "Network",
                    color = MangaDlColors.SectionRed,
                    modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
                )
            }
            item {
                SettingsRowSwitch(
                    label = "Download over Wi-Fi only",
                    desc = "Avoid mobile data charges",
                    checked = wifiOnly,
                    onCheckedChange = { wifiOnly = it },
                )
            }
            item {
                SettingsRowValue(label = "Download threads", value = "3", onClick = {})
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SettingsRowSwitch(
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
private fun SettingsRowValue(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MangaDlColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(value, color = MangaDlColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0x80FFFFFF), modifier = Modifier.size(16.dp))
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
}

@Composable
private fun SettingsRowSegmented(
    label: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
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
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEachIndexed { i, opt ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (i == selected) MangaDlColors.TextPrimary else Color.Transparent)
                        .clickable { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        opt,
                        color = if (i == selected) MangaDlColors.Background else MangaDlColors.TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
}

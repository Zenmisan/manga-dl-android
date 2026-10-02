package com.mangadl.android.ui.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.ui.components.MangaDlSwitch
import com.mangadl.android.ui.components.SectionLabel
import com.mangadl.android.ui.theme.MangaDlColors

@Composable
fun MoreScreen(
    onDownloads: () -> Unit,
    onSettings: () -> Unit,
    onHistory: () -> Unit,
    onStatistics: () -> Unit = {},
    onHelp: () -> Unit = {},
    onProfile: () -> Unit = {},
    onNotifications: () -> Unit = {},
    onSignIn: () -> Unit = {},
) {
    var incognito by remember { mutableStateOf(false) }
    var downloadedOnly by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(MangaDlColors.Background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item { Spacer(Modifier.height(20.dp)) }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MangaDlColors.CardBg)
                    .border(1.dp, MangaDlColors.CardBorder, RoundedCornerShape(16.dp))
                    .clickable(onClick = onProfile)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MangaDlColors.AvatarBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "U",
                        color = MangaDlColors.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("Display name", color = MangaDlColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Tap to view profile", color = MangaDlColors.TextSecondary, fontSize = 13.sp)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MangaDlColors.TextSecondary, modifier = Modifier.size(20.dp))
            }
        }

        item { Spacer(Modifier.height(20.dp)) }

        item {
            Column {
                SectionLabel("Quick toggles", color = MangaDlColors.SectionRed, modifier = Modifier.padding(bottom = 4.dp))
                ToggleRow(
                    title = "Incognito mode",
                    desc = "Hides reading activity",
                    checked = incognito,
                    onCheckedChange = { incognito = it },
                )
                ToggleRow(
                    title = "Downloaded only",
                    desc = "Show only saved chapters",
                    checked = downloadedOnly,
                    onCheckedChange = { downloadedOnly = it },
                )
            }
        }

        item { Spacer(Modifier.height(4.dp)) }

        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Color(0x14FFFFFF),
                        shape = RoundedCornerShape(0.dp),
                    )
            ) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
                Spacer(Modifier.height(8.dp))
                LinkRow(icon = Icons.Default.Download, label = "Downloads", onClick = onDownloads)
                LinkRow(icon = Icons.Default.History, label = "History", onClick = onHistory)
                LinkRow(icon = Icons.Default.Notifications, label = "Notifications", onClick = onNotifications)
                LinkRow(icon = Icons.Default.PieChart, label = "Statistics", onClick = onStatistics)
                LinkRow(icon = Icons.Default.Help, label = "Help center", onClick = onHelp)
                LinkRow(icon = Icons.Default.Settings, label = "Settings", onClick = onSettings)
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = MangaDlColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(desc, color = MangaDlColors.TextSecondary, fontSize = 12.sp)
        }
        MangaDlSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LinkRow(icon: ImageVector, label: String, badge: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xBFFFFFFF), modifier = Modifier.size(22.dp))
        Text(label, color = MangaDlColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (badge != null) {
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 22.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MangaDlColors.Primary)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(badge, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MangaDlColors.TextSecondary, modifier = Modifier.size(20.dp))
    }
}

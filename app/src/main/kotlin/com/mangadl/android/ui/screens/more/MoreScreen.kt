package com.mangadl.android.ui.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.BuildConfig
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.CountBadge
import com.mangadl.android.ui.components.Divider
import com.mangadl.android.ui.components.Eyebrow
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.components.MdSwitch
import com.mangadl.android.ui.components.SurfaceCard
import com.mangadl.android.ui.components.rememberState
import com.mangadl.android.ui.theme.MdTheme

enum class MoreDestination(val label: String, val icon: ImageVector, val badge: Int = 0) {
    Downloads("Downloads", MdIcons.Download, 3),
    Notifications("Notifications", MdIcons.Mail, 2),
    Statistics("Statistics", MdIcons.Stats),
    Import("Import local files", MdIcons.Upload),
    Backup("Backup & restore", MdIcons.Backup),
    Settings("Settings", MdIcons.Settings),
    Help("Help", MdIcons.Help),
    AllScreens("All screens (debug)", MdIcons.ListBullets),
}

@Composable
fun MoreScreen(onProfile: () -> Unit, onOpen: (MoreDestination) -> Unit) {
    val c = MdTheme.colors
    var incognito by rememberState(false)
    var downloadedOnly by rememberState(true)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        SurfaceCard(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onProfile), padding = PaddingValues(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0xFF2D1716)), contentAlignment = Alignment.Center) {
                    BodyText("U", size = 20.sp, weight = FontWeight.ExtraBold)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    BodyText("[Display name]", size = 16.sp, weight = FontWeight.ExtraBold)
                    BodyText("Tap to view profile", size = 13.sp, color = c.fg.copy(alpha = 0.65f))
                }
                Icon(MdIcons.ChevronRight, null, tint = c.fg, modifier = Modifier.size(20.dp))
            }
        }

        Column {
            Eyebrow("Quick toggles", Modifier.padding(bottom = 4.dp))
            ToggleRow("Incognito mode", "Hides reading activity", incognito) { incognito = it }
            ToggleRow("Downloaded only", "Show only saved chapters", downloadedOnly) { downloadedOnly = it }
        }

        Column {
            Divider(color = c.surfaceHigh)
            MoreDestination.entries.filter { it != MoreDestination.AllScreens || BuildConfig.DEBUG }.forEach { d ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clickable(role = Role.Button) { onOpen(d) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(d.icon, null, tint = c.fg.copy(alpha = 0.75f), modifier = Modifier.size(22.dp))
                    BodyText(d.label, Modifier.weight(1f), size = 15.sp, weight = FontWeight.SemiBold)
                    if (d.badge > 0) CountBadge(d.badge, height = 20.dp)
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            BodyText(title, size = 15.sp, weight = FontWeight.Bold)
            BodyText(subtitle, size = 12.sp, color = MdTheme.colors.fgSubtle)
        }
        MdSwitch(checked, onChange)
    }
}

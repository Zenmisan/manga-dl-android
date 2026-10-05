package com.mangadl.android.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.ui.components.BackHeader
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.Divider
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.components.Screen
import com.mangadl.android.ui.theme.MdTheme

enum class SettingsPage(val label: String, val description: String, val icon: ImageVector) {
    Account("Account", "Profile, cloud sync, sign out", MdIcons.User),
    General("General", "Theme, accent, backend, notifications", MdIcons.Settings),
    Reader("Reader", "Reading mode, tap zones, volume keys", MdIcons.Pages),
    Library("Library", "Grid, categories, auto-update", MdIcons.Library),
    Trackers("Trackers", "AniList, MAL, Kitsu and more", MdIcons.Refresh),
    System("System & backup", "Storage, sync, backup, servers", MdIcons.Backup),
}

@Composable
fun SettingsScreen(onBack: () -> Unit, onOpen: (SettingsPage) -> Unit) {
    val c = MdTheme.colors
    Screen {
        BackHeader("Settings", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
            SettingsPage.entries.forEach { page ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 68.dp)
                        .clickable(role = Role.Button) { onOpen(page) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(c.surfaceRaised), contentAlignment = Alignment.Center) {
                        Icon(page.icon, null, tint = c.fg, modifier = Modifier.size(20.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        BodyText(page.label, size = 15.sp, weight = FontWeight.Bold)
                        BodyText(page.description, size = 12.sp, color = c.fgSubtle)
                    }
                    Icon(MdIcons.ChevronRight, null, tint = c.fgFaint, modifier = Modifier.size(18.dp))
                }
                Divider()
            }
        }
        BodyText("manga-dl [version] · Android", Modifier.padding(20.dp), size = 12.sp, color = c.fgFaint)
    }
}

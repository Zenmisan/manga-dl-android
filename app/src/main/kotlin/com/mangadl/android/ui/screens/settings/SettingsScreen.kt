package com.mangadl.android.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.ui.theme.AntonStyleSub
import com.mangadl.android.ui.theme.MangaDlColors

private data class SettingsItem(
    val icon: ImageVector,
    val label: String,
    val desc: String,
    val onClick: () -> Unit,
)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onGeneral: () -> Unit,
    onReader: () -> Unit,
) {
    val items = listOf(
        SettingsItem(Icons.Default.Person, "Account", "Profile, cloud sync, sign out", {}),
        SettingsItem(Icons.Default.Tune, "General", "Theme, accent, backend, notifications", onGeneral),
        SettingsItem(Icons.Default.MenuBook, "Reader", "Reading mode, tap zones, volume keys", onReader),
        SettingsItem(Icons.Default.LocalLibrary, "Library", "Grid, categories, auto-update", {}),
        SettingsItem(Icons.Default.Refresh, "Trackers", "AniList, MAL, Kitsu and more", {}),
        SettingsItem(Icons.Default.Storage, "System & backup", "Storage, sync, backup, servers", {}),
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(MangaDlColors.Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MangaDlColors.TextPrimary)
            }
            Text(
                text = "Settings".uppercase(),
                style = AntonStyleSub,
                color = MangaDlColors.TextPrimary,
            )
        }

        LazyColumn(
            Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
        ) {
            items(items.size) { i ->
                val item = items[i]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 68.dp)
                        .clickable(onClick = item.onClick)
                        .border(
                            width = 0.dp,
                            color = Color.Transparent,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MangaDlColors.CardBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(item.icon, contentDescription = null, tint = MangaDlColors.TextPrimary, modifier = Modifier.size(20.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(item.label, color = MangaDlColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(item.desc, color = MangaDlColors.TextSecondary, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0x80FFFFFF), modifier = Modifier.size(18.dp))
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
            }
        }

        Text(
            text = "manga-dl 1.0.0 · Android",
            color = Color(0x80FFFFFF),
            fontSize = 12.sp,
            modifier = Modifier.padding(20.dp),
        )
    }
}

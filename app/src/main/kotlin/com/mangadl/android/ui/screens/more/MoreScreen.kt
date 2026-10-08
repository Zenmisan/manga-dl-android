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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mangadl.android.BuildConfig
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.CountBadge
import com.mangadl.android.ui.components.Divider
import com.mangadl.android.ui.components.Eyebrow
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.components.MdSwitch
import com.mangadl.android.ui.components.SurfaceCard
import com.mangadl.android.ui.theme.MdTheme
import com.mangadl.android.ui.viewmodels.AuthViewModel
import com.mangadl.android.ui.viewmodels.DownloadQueueViewModel
import com.mangadl.android.ui.viewmodels.SettingsViewModel
import com.mangadl.android.ui.viewmodels.UpdatesViewModel

enum class MoreDestination(val label: String, val icon: ImageVector) {
    Downloads("Downloads", MdIcons.Download),
    Notifications("Notifications", MdIcons.Mail),
    Leaderboard("Guild Leaderboard", MdIcons.Trophy),
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
    val settingsVm: SettingsViewModel = viewModel()
    val incognito by settingsVm.incognito.collectAsState()
    val downloadedOnly by settingsVm.libraryDownloadedOnly.collectAsState()

    val downloadsVm: DownloadQueueViewModel = viewModel()
    val downloads by downloadsVm.downloads.collectAsState()
    val activeDownloads = downloads.count { it.status == "queued" || it.status == "downloading" }

    val updatesVm: UpdatesViewModel = viewModel()
    val newChapters by updatesVm.newChapters.collectAsState()

    val badges = mapOf(
        MoreDestination.Downloads to activeDownloads,
        MoreDestination.Notifications to newChapters.size,
    )

    val authVm: AuthViewModel = viewModel()
    val user = authVm.currentUser
    val displayName = run {
        val meta = user?.userMetadata
        val username = try {
            (meta?.get("username") as? kotlinx.serialization.json.JsonPrimitive)?.content
        } catch (_: Exception) { null }
        val fullName = try {
            (meta?.get("full_name") as? kotlinx.serialization.json.JsonPrimitive)?.content
        } catch (_: Exception) { null }
        username ?: fullName ?: user?.email?.substringBefore('@') ?: "Guest"
    }
    val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
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
                    BodyText(initial, size = 20.sp, weight = FontWeight.ExtraBold)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    BodyText(displayName, size = 16.sp, weight = FontWeight.ExtraBold)
                    BodyText("Tap to view profile", size = 13.sp, color = c.fg.copy(alpha = 0.65f))
                }
                Icon(MdIcons.ChevronRight, null, tint = c.fg, modifier = Modifier.size(20.dp))
            }
        }

        Column {
            Eyebrow("Quick toggles", Modifier.padding(bottom = 4.dp))
            ToggleRow("Incognito mode", "Hides reading activity", incognito) { settingsVm.setIncognito(it) }
            ToggleRow("Downloaded only", "Show only saved chapters", downloadedOnly) { settingsVm.setLibraryDownloadedOnly(it) }
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
                    val badge = badges[d] ?: 0
                    if (badge > 0) CountBadge(badge, height = 20.dp)
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

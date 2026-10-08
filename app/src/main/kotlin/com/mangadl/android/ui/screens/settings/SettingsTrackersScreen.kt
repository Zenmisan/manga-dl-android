package com.mangadl.android.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.ButtonTone
import com.mangadl.android.ui.components.Divider
import com.mangadl.android.ui.components.LogoTile
import com.mangadl.android.ui.components.PillButton
import com.mangadl.android.ui.components.SettingsSection
import com.mangadl.android.ui.components.SwitchSetting
import com.mangadl.android.ui.theme.MdTheme
import com.mangadl.android.ui.viewmodels.TrackerViewModel

@Composable
fun TrackerSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm: TrackerViewModel = viewModel()
    val anilistConnected by vm.anilistConnected.collectAsState()
    val anilistUsername by vm.anilistUsername.collectAsState()
    val malConnected by vm.malConnected.collectAsState()
    val malUsername by vm.malUsername.collectAsState()
    val autoSync by vm.autoSyncTrackers.collectAsState()
    val markCompleted by vm.markCompletedOnFinish.collectAsState()
    val askBeforeScore by vm.askBeforeScoreChange.collectAsState()

    SettingsFrame("Trackers", onBack) {
        SettingsSection("Services") {
            TrackerRow(
                short = "AL",
                color = Color(0xFF0099CC),
                name = "AniList",
                connected = anilistConnected,
                username = anilistUsername,
                onConnect = {
                    val url = vm.anilistAuthUrl()
                    if (url.isNotEmpty()) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                },
                onDisconnect = { vm.disconnectAnilist() },
            )

            Divider()

            TrackerRow(
                short = "MAL",
                color = Color(0xFF2E51A2),
                name = "MyAnimeList",
                connected = malConnected,
                username = malUsername,
                onConnect = {
                    val url = vm.malAuthUrl()
                    if (url.isNotEmpty()) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                },
                onDisconnect = { vm.disconnectMal() },
            )
        }
        SettingsSection("Sync") {
            SwitchSetting(
                label = "Auto-sync progress",
                initial = autoSync,
                description = "Update trackers after each chapter",
                value = autoSync,
                onValueChange = { vm.setAutoSync(it) },
            )
            SwitchSetting(
                "Mark completed", true, "Set status to Completed on last chapter",
                value = markCompleted, onValueChange = { vm.setMarkCompletedOnFinish(it) },
            )
            SwitchSetting(
                "Ask before changing scores", false, "Confirm before overwriting a score",
                value = askBeforeScore, onValueChange = { vm.setAskBeforeScoreChange(it) },
            )
        }
    }
}

@Composable
private fun TrackerRow(
    short: String,
    color: Color,
    name: String,
    connected: Boolean,
    username: String = "",
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val c = MdTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LogoTile(short, color, anton = false)
        Column(Modifier.weight(1f)) {
            BodyText(name, size = 15.sp, weight = FontWeight.Bold)
            val subtitle = when {
                connected && username.isNotEmpty() -> "Connected as $username"
                connected -> "Connected"
                else -> "Not connected"
            }
            BodyText(
                subtitle,
                size = 12.sp,
                color = if (connected) c.successText else c.fgSubtle,
            )
        }
        PillButton(
            if (connected) "Log Out" else "Connect",
            if (connected) onDisconnect else onConnect,
            tone = if (connected) ButtonTone.Ghost else ButtonTone.Primary,
            height = 38.dp,
        )
    }
}

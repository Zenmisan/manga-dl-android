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
import com.mangadl.android.ui.components.InputSetting
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
    val anilistClientId by vm.anilistClientId.collectAsState()
    val malConnected by vm.malConnected.collectAsState()
    val malClientId by vm.malClientId.collectAsState()

    SettingsFrame("Trackers", onBack) {
        SettingsSection("Services") {
            TrackerRow(
                short = "AL",
                color = Color(0xFF0099CC),
                name = "AniList",
                connected = anilistConnected,
                onConnect = {
                    val url = vm.anilistAuthUrl()
                    if (url.isNotEmpty()) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                },
                onDisconnect = { vm.disconnectAnilist() },
            )
            if (!anilistConnected) {
                InputSetting(
                    label = "AniList Client ID",
                    initial = anilistClientId,
                    description = "Create at anilist.co/settings/developer · redirect: mangadl://anilist-callback",
                    value = anilistClientId,
                    onValueChange = { vm.setAnilistClientId(it) },
                )
            }

            Divider()

            TrackerRow(
                short = "MAL",
                color = Color(0xFF2E51A2),
                name = "MyAnimeList",
                connected = malConnected,
                onConnect = {
                    val url = vm.malAuthUrl()
                    if (url.isNotEmpty()) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                },
                onDisconnect = { vm.disconnectMal() },
            )
            if (!malConnected) {
                InputSetting(
                    label = "MAL Client ID",
                    initial = malClientId,
                    description = "Create at myanimelist.net/apiconfig · redirect: mangadl://mal-callback",
                    value = malClientId,
                    onValueChange = { vm.setMalClientId(it) },
                )
            }
        }
        SettingsSection("Sync") {
            SwitchSetting("Auto-sync progress", true, "Update trackers after each chapter")
            SwitchSetting("Mark completed", true, "Set status to Completed on last chapter")
            SwitchSetting("Ask before changing scores", false, "Confirm before overwriting a score")
        }
    }
}

@Composable
private fun TrackerRow(
    short: String,
    color: Color,
    name: String,
    connected: Boolean,
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
            BodyText(
                if (connected) "Connected" else "Not connected",
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

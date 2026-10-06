package com.mangadl.android.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.viewmodels.AuthViewModel
import com.mangadl.android.ui.viewmodels.ProfileViewModel
import com.mangadl.android.ui.components.ButtonTone
import com.mangadl.android.ui.components.CoverArt
import com.mangadl.android.ui.components.DisplayText
import com.mangadl.android.ui.components.Divider
import com.mangadl.android.ui.components.Eyebrow
import com.mangadl.android.ui.components.MdButton
import com.mangadl.android.ui.components.MdIconButton
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.theme.MdTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(onBack: () -> Unit, onEditProfile: () -> Unit) {
    val c = MdTheme.colors
    val authVm: AuthViewModel = viewModel()
    val profileVm: ProfileViewModel = viewModel()
    val library by profileVm.library.collectAsState()
    val chaptersRead by profileVm.totalChaptersRead.collectAsState()
    val streak by profileVm.streak.collectAsState()
    val user = authVm.currentUser
    val username = try {
        (user?.userMetadata?.get("username") as? kotlinx.serialization.json.JsonPrimitive)?.content
    } catch (_: Exception) { null }
        ?: user?.email?.substringBefore('@')
        ?: "Guest"
    val displayName = username
    val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val email = user?.email ?: ""
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(Modifier.fillMaxSize().background(c.bg).verticalScroll(rememberScrollState()).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().background(Color(0xFF2D1716)).statusBarsPadding().height(128.dp)) {
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    MdIconButton(MdIcons.Back, "Back", onBack)
                    MdIconButton(MdIcons.Share, "Share profile", {
                        val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Manga-DL Profile")
                            putExtra(android.content.Intent.EXTRA_TEXT, "Manga-DL: $displayName ($chaptersRead chapters read, $streak-day streak)")
                        }
                        context.startActivity(android.content.Intent.createChooser(sendIntent, "Share profile"))
                    }, iconSize = 20.dp)
                }
            }
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 20.dp, y = 40.dp)
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(c.bg)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3A1518)),
                contentAlignment = Alignment.Center,
            ) { BodyText(initial, size = 32.sp, weight = FontWeight.ExtraBold) }
        }
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    BodyText(displayName, size = 24.sp, weight = FontWeight.Black)
                    BodyText(if (email.isNotEmpty()) email else "@$username", Modifier.padding(top = 2.dp), color = c.fgSubtle)
                }
                MdButton("Edit Profile", onEditProfile, tone = ButtonTone.Ghost, height = 40.dp, shape = CircleShape, fontSize = 13.sp, horizontalPadding = 16.dp)
            }
            val bio = try {
                (user?.userMetadata?.get("bio") as? kotlinx.serialization.json.JsonPrimitive)?.content
            } catch (_: Exception) { null }
            if (!bio.isNullOrBlank()) {
                BodyText(bio, color = c.fg.copy(alpha = 0.75f), lineHeight = 21.sp)
            }
            Column {
                Divider(color = c.surfaceHigh)
                Row(Modifier.padding(vertical = 14.dp)) {
                    listOf(
                        "Chapters" to chaptersRead.toString(),
                        "Library" to library.size.toString(),
                        "Day streak" to streak.toString(),
                    ).forEach { (label, value) ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            DisplayText(value, 24.sp, uppercase = false)
                            BodyText(label, size = 12.sp, color = c.fgSubtle)
                        }
                    }
                }
                Divider(color = c.surfaceHigh)
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Eyebrow("Pinned")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val pinned = library.take(3)
                    val pinnedColors = listOf(Color(0xFF1A2433), Color(0xFF3A1518), Color(0xFF2B1A2E))
                    pinned.forEachIndexed { i, _ ->
                        CoverArt(pinnedColors[i % pinnedColors.size], Modifier.weight(1f).aspectRatio(2f / 3f), RoundedCornerShape(8.dp))
                    }
                    repeat((3 - pinned.size).coerceAtLeast(0)) {
                        CoverArt(Color(0xFF22222A), Modifier.weight(1f).aspectRatio(2f / 3f), RoundedCornerShape(8.dp))
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, c.fg.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button) {},
                        contentAlignment = Alignment.Center,
                    ) { Icon(MdIcons.Plus, "Pin a manga", tint = c.fgSubtle, modifier = Modifier.size(22.dp)) }
                }
            }
            val ch = chaptersRead
            val milestones = listOf(
                "First chapter" to (ch >= 1),
                "10 chapters" to (ch >= 10),
                "50 chapters" to (ch >= 50),
                "100 chapters" to (ch >= 100),
                "500 chapters" to (ch >= 500),
            )
            if (milestones.any { it.second }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Eyebrow("Milestones", color = c.fgSubtle)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        milestones.forEach { (label, unlocked) ->
                            ProfileMilestone(label, unlocked)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileMilestone(label: String, unlocked: Boolean) {
    val c = MdTheme.colors
    BodyText(
        label,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (unlocked) c.accentMuted else Color.Transparent)
            .border(1.dp, if (unlocked) Color.Transparent else c.fg.copy(alpha = 0.2f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        size = 12.sp,
        weight = if (unlocked) FontWeight.Bold else FontWeight.SemiBold,
        color = if (unlocked) c.accentSoft else c.fgSubtle,
    )
}

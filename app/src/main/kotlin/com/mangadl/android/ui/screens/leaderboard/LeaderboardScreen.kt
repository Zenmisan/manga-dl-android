package com.mangadl.android.ui.screens.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mangadl.android.data.gamification.HunterRank
import com.mangadl.android.data.gamification.MilestoneTier
import com.mangadl.android.data.model.LeaderboardEntry
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.DisplayText
import com.mangadl.android.ui.components.Divider
import com.mangadl.android.ui.components.Eyebrow
import com.mangadl.android.ui.components.MdIconButton
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.theme.MdTheme
import com.mangadl.android.ui.viewmodels.AuthViewModel
import com.mangadl.android.ui.viewmodels.LeaderboardPeriod
import com.mangadl.android.ui.viewmodels.LeaderboardViewModel
import java.text.NumberFormat

@Composable
fun LeaderboardScreen(
    onBack: () -> Unit,
    vm: LeaderboardViewModel = viewModel(),
    authVm: AuthViewModel = viewModel(),
) {
    val c = MdTheme.colors
    val selectedPeriod by vm.selectedPeriod.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val entries by vm.entries.collectAsState()

    val localScore by vm.localUserScore.collectAsState()
    val localHunterRank by vm.localHunterRank.collectAsState()
    val totalChapters by vm.totalChaptersRead.collectAsState()
    val streakDays by vm.streak.collectAsState()

    val user = authVm.currentUser
    val username = try {
        (user?.userMetadata?.get("username") as? kotlinx.serialization.json.JsonPrimitive)?.content
    } catch (_: Exception) { null }
        ?: user?.email?.substringBefore('@')
        ?: "Guest"

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // App Bar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MdIconButton(MdIcons.Back, "Back", onBack)
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                DisplayText("Guild Leaderboard", size = 20.sp, weight = FontWeight.ExtraBold)
                BodyText("Global Reader Hall of Fame", size = 12.sp, color = c.fgMuted)
            }
            MdIconButton(MdIcons.Refresh, "Refresh", { vm.loadLeaderboard() })
        }

        // Period filter tabs
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LeaderboardPeriod.entries.forEach { p ->
                val isSelected = selectedPeriod == p
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) c.accent else c.surface)
                        .clickable { vm.selectPeriod(p) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BodyText(
                        p.label,
                        size = 12.sp,
                        weight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else c.fgMuted,
                    )
                }
            }
        }

        // Current User Standing Card
        UserStandingBanner(
            username = username,
            score = localScore,
            rank = localHunterRank,
            chaptersRead = totalChapters,
            streak = streakDays,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Divider(color = c.surfaceHigh, modifier = Modifier.padding(top = 4.dp))

        // Leaderboard List
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.accent)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(entries, key = { index, item -> item.userId.ifBlank { "entry_$index" } }) { idx, entry ->
                    val standing = idx + 1
                    LeaderboardHunterCard(
                        standing = standing,
                        entry = entry,
                        isCurrentUser = entry.username.equals(username, ignoreCase = true)
                    )
                }
            }
        }
    }
}

@Composable
private fun UserStandingBanner(
    username: String,
    score: Int,
    rank: HunterRank,
    chaptersRead: Int,
    streak: Int,
    modifier: Modifier = Modifier,
) {
    val c = MdTheme.colors
    val numFmt = NumberFormat.getIntegerInstance()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(rank.bgColor, c.surface)
                )
            )
            .border(1.dp, rank.borderColor, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(rank.color.copy(alpha = 0.2f))
                        .border(1.5.dp, rank.color, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (rank.code == "MONARCH") MdIcons.Crown else MdIcons.Shield,
                        contentDescription = null,
                        tint = rank.color,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        BodyText(username, size = 15.sp, weight = FontWeight.ExtraBold)
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(rank.color.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            BodyText(rank.code, size = 11.sp, weight = FontWeight.Black, color = rank.color)
                        }
                    }
                    BodyText(rank.name, size = 12.sp, color = rank.color)
                }
                Column(horizontalAlignment = Alignment.End) {
                    DisplayText("${numFmt.format(score)} EXP", size = 16.sp, weight = FontWeight.ExtraBold, color = rank.color)
                    BodyText(rank.tag, size = 11.sp, color = c.fgMuted)
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatPill("Chapters", "${numFmt.format(chaptersRead)}")
                StatPill("Streak", "$streak days")
                StatPill("Tier", rank.tier.label)
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    val c = MdTheme.colors
    Column {
        BodyText(label, size = 10.sp, color = c.fgSubtle)
        BodyText(value, size = 13.sp, weight = FontWeight.Bold, color = c.fg)
    }
}

@Composable
private fun LeaderboardHunterCard(
    standing: Int,
    entry: LeaderboardEntry,
    isCurrentUser: Boolean,
) {
    val c = MdTheme.colors
    val numFmt = NumberFormat.getIntegerInstance()

    val podiumColor = when (standing) {
        1 -> Color(0xFFF59E0B) // Gold
        2 -> Color(0xFF94A3B8) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> c.surfaceHigh
    }

    val isTop3 = standing in 1..3
    val rankCode = entry.hunterRank?.rankCode ?: "E"
    val rankName = entry.hunterRank?.rankName ?: "Novice"

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrentUser) c.accentFaint else c.surface)
            .border(
                width = if (isCurrentUser || isTop3) 1.5.dp else 1.dp,
                color = if (isCurrentUser) c.accentSoft else if (isTop3) podiumColor.copy(alpha = 0.7f) else c.surfaceHigh,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Standing rank number
        Box(
            Modifier.size(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isTop3) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(podiumColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    BodyText(
                        "#$standing",
                        size = 12.sp,
                        weight = FontWeight.Black,
                        color = podiumColor,
                    )
                }
            } else {
                BodyText(
                    "#$standing",
                    size = 13.sp,
                    weight = FontWeight.Bold,
                    color = c.fgSubtle,
                )
            }
        }

        // Avatar
        val initial = (entry.displayName.ifBlank { entry.username }).firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isTop3) podiumColor.copy(alpha = 0.15f) else c.surfaceHigh),
            contentAlignment = Alignment.Center
        ) {
            BodyText(
                initial,
                size = 15.sp,
                weight = FontWeight.Bold,
                color = if (isTop3) podiumColor else c.fg,
            )
        }

        // Details
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BodyText(
                    entry.displayName.ifBlank { entry.username },
                    size = 14.sp,
                    weight = FontWeight.Bold,
                    color = if (isCurrentUser) c.accentSoft else c.fg,
                    maxLines = 1,
                )
                if (isTop3) {
                    Icon(
                        if (standing == 1) MdIcons.Crown else MdIcons.Trophy,
                        contentDescription = null,
                        tint = podiumColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BodyText(rankName, size = 11.sp, color = c.fgMuted)
                BodyText("•", size = 11.sp, color = c.fgSubtle)
                BodyText("${entry.chaptersRead} ch", size = 11.sp, color = c.fgSubtle)
            }
        }

        // EXP Score
        Column(horizontalAlignment = Alignment.End) {
            BodyText(
                "${numFmt.format(entry.score)} EXP",
                size = 13.sp,
                weight = FontWeight.ExtraBold,
                color = if (isTop3) podiumColor else c.fg,
            )
            Box(
                Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(c.surfaceHigh)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                BodyText(rankCode, size = 9.sp, weight = FontWeight.Black, color = c.fgMuted)
            }
        }
    }
}

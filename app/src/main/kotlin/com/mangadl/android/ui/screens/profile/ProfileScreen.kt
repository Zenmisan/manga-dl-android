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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mangadl.android.data.gamification.HunterRank
import com.mangadl.android.data.gamification.MILESTONES
import com.mangadl.android.data.gamification.MilestoneBadge
import com.mangadl.android.data.gamification.MilestoneCategory
import com.mangadl.android.data.gamification.MilestoneTier
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.ButtonTone
import com.mangadl.android.ui.components.CoverArt
import com.mangadl.android.ui.components.DisplayText
import com.mangadl.android.ui.components.Eyebrow
import com.mangadl.android.ui.components.MdButton
import com.mangadl.android.ui.components.MdIconButton
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.theme.MdTheme
import com.mangadl.android.ui.viewmodels.AuthViewModel
import com.mangadl.android.ui.viewmodels.ProfileViewModel
import java.text.NumberFormat

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(onBack: () -> Unit, onEditProfile: () -> Unit) {
    val c = MdTheme.colors
    val authVm: AuthViewModel = viewModel()
    val profileVm: ProfileViewModel = viewModel()

    val library by profileVm.library.collectAsState()
    val chaptersRead by profileVm.totalChaptersRead.collectAsState()
    val streak by profileVm.streak.collectAsState()
    val hunterState by profileVm.hunterState.collectAsState()

    var showMilestonesModal by remember { mutableStateOf(false) }

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

    val rank = hunterState.hunterRank
    val summary = hunterState.milestoneSummary

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        // ── Top Banner & Avatar Header ──
        Box(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2D1716))
                    .statusBarsPadding()
                    .height(132.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MdIconButton(MdIcons.Back, "Back", onBack)
                    MdIconButton(MdIcons.Share, "Share profile", {
                        val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Manga-DL Hunter Profile")
                            putExtra(
                                android.content.Intent.EXTRA_TEXT,
                                "Manga-DL Hunter: $displayName\nRank: ${rank.name} (${hunterState.readerScore} EXP)\nTitle: ${summary.currentTitle}\n$chaptersRead chapters read · $streak-day streak"
                            )
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
                    .background(Color(0xFF3A1518))
                    .border(2.dp, rank.borderColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                BodyText(initial, size = 32.sp, weight = FontWeight.ExtraBold)
            }
        }

        // ── User Identity & Standings ──
        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 50.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    BodyText(displayName, size = 24.sp, weight = FontWeight.Black)
                    BodyText(
                        if (email.isNotEmpty()) email else "@$username",
                        Modifier.padding(top = 2.dp),
                        color = c.fgSubtle,
                        size = 13.sp
                    )
                }
                MdButton(
                    "Edit Profile",
                    onEditProfile,
                    tone = ButtonTone.Ghost,
                    height = 38.dp,
                    shape = CircleShape,
                    fontSize = 12.sp,
                    horizontalPadding = 16.dp
                )
            }

            // Reader Title & Hunter Rank Badges
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Reader Title Badge
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(summary.currentTitleTier.bgColor)
                        .border(1.dp, summary.currentTitleTier.borderColor, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = MdIcons.Crown,
                        contentDescription = "Reader Title",
                        tint = summary.currentTitleTier.color,
                        modifier = Modifier.size(13.dp)
                    )
                    BodyText(
                        summary.currentTitle,
                        size = 11.sp,
                        weight = FontWeight.Black,
                        color = summary.currentTitleTier.color
                    )
                }

                // Hunter Rank Standing Badge
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(rank.bgColor)
                        .border(1.dp, rank.borderColor, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = MdIcons.Shield,
                        contentDescription = "Hunter Rank",
                        tint = rank.color,
                        modifier = Modifier.size(13.dp)
                    )
                    BodyText(
                        "${rank.name} · ${rank.tag}",
                        size = 11.sp,
                        weight = FontWeight.Black,
                        color = rank.color
                    )
                }
            }

            // Bio
            val bio = try {
                (user?.userMetadata?.get("bio") as? kotlinx.serialization.json.JsonPrimitive)?.content
            } catch (_: Exception) { null }
            if (!bio.isNullOrBlank()) {
                BodyText(bio, color = c.fg.copy(alpha = 0.8f), lineHeight = 20.sp, size = 13.sp)
            }

            // ── Guild Hunter Standing Card ──
            HunterStandingCard(
                state = hunterState,
                onViewMilestones = { showMilestonesModal = true }
            )

            // ── Pinned Showcase Badges ──
            PinnedBadgesSection(
                pinnedBadges = hunterState.pinnedBadgeObjects,
                onManagePins = { showMilestonesModal = true }
            )

            // ── Reading Highlights (Stats Grid) ──
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    icon = MdIcons.BookOpen,
                    iconTint = Color(0xFFEF4444),
                    value = NumberFormat.getIntegerInstance().format(chaptersRead),
                    label = "Chapters Read",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = MdIcons.Library,
                    iconTint = Color(0xFF38BDF8),
                    value = NumberFormat.getIntegerInstance().format(library.size),
                    label = "Series Read",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = MdIcons.Flame,
                    iconTint = Color(0xFFF59E0B),
                    value = NumberFormat.getIntegerInstance().format(streak),
                    label = "Days Active",
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Milestones & Achievements Section ──
            MilestonesCard(
                summary = summary,
                onViewAll = { showMilestonesModal = true }
            )

            // ── Recent from Library Shelf ──
            if (library.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Eyebrow("Recent from library")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val recent = library.take(4)
                        val palette = listOf(Color(0xFF1A2433), Color(0xFF3A1518), Color(0xFF2B1A2E), Color(0xFF13282A))
                        recent.forEachIndexed { i, m ->
                            CoverArt(
                                palette[i % palette.size],
                                Modifier.weight(1f).aspectRatio(2f / 3f),
                                RoundedCornerShape(8.dp),
                                imageUrl = m.coverUrl,
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Full Milestones Gallery Modal ──
    if (showMilestonesModal) {
        MilestonesGalleryModal(
            summary = summary,
            pinnedIds = hunterState.pinnedBadges,
            onTogglePin = { badgeId -> profileVm.togglePinnedBadge(badgeId) },
            onDismiss = { showMilestonesModal = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HunterStandingCard(
    state: com.mangadl.android.ui.viewmodels.HunterProfileState,
    onViewMilestones: () -> Unit
) {
    val rank = state.hunterRank
    val score = state.readerScore
    val formattedScore = NumberFormat.getIntegerInstance().format(score)
    val nextRankName = when (rank.code) {
        "E" -> "D-Rank"
        "D" -> "C-Rank"
        "C" -> "B-Rank"
        "B" -> "A-Rank"
        "A" -> "S-Rank"
        else -> "Apex Sovereign"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(rank.bgColor)
            .border(1.dp, rank.borderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header Row: Rank Icon + Title + Rank Code Pill
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(rank.color.copy(alpha = 0.2f))
                            .border(1.dp, rank.color.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = MdIcons.Shield,
                            contentDescription = rank.name,
                            tint = rank.color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        BodyText("HUNTER GUILD STANDING", size = 10.sp, weight = FontWeight.Black, color = rank.color)
                        BodyText(rank.name, size = 16.sp, weight = FontWeight.Black, color = Color.White)
                    }
                }

                // Rank Code Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(rank.color.copy(alpha = 0.25f))
                        .border(1.dp, rank.borderColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BodyText(rank.code, size = 13.sp, weight = FontWeight.Black, color = rank.color)
                }
            }

            // Reader Score / Guild EXP
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    DisplayText("$formattedScore EXP", size = 26.sp, uppercase = false)
                    BodyText("Reader EXP: (Ch × 10) + (Streak × 50) + (Library × 25)", size = 10.sp, color = Color(0xFFA1A1AA))
                }
                BodyText(
                    "${(state.rankProgress * 100).toInt()}%",
                    size = 14.sp,
                    weight = FontWeight.Black,
                    color = rank.color
                )
            }

            // Rank Progress Bar
            LinearProgressIndicator(
                progress = { state.rankProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = rank.color,
                trackColor = Color.White.copy(alpha = 0.1f),
            )

            // Progress footer
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val footerText = if (rank.code == "S" || rank.code == "MONARCH") {
                    "Apex Hunter Standing Achieved"
                } else {
                    "${NumberFormat.getIntegerInstance().format(rank.nextScore - score)} EXP to reach $nextRankName"
                }
                BodyText(footerText, size = 11.sp, color = Color(0xFFA1A1AA))

                BodyText(
                    "View Milestones →",
                    modifier = Modifier.clickable { onViewMilestones() },
                    size = 11.sp,
                    weight = FontWeight.Bold,
                    color = rank.color
                )
            }
        }
    }
}

@Composable
private fun PinnedBadgesSection(
    pinnedBadges: List<MilestoneBadge>,
    onManagePins: () -> Unit
) {
    val c = MdTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(MdIcons.Pin, contentDescription = "Pinned", tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                BodyText(
                    "PINNED SHOWCASE BADGES (${pinnedBadges.size}/4)",
                    size = 11.sp,
                    weight = FontWeight.Black,
                    color = c.fgSubtle
                )
            }
            BodyText(
                "Customize",
                modifier = Modifier.clickable { onManagePins() },
                size = 11.sp,
                weight = FontWeight.Bold,
                color = Color(0xFFEF4444)
            )
        }

        if (pinnedBadges.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .clickable { onManagePins() }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                BodyText(
                    "+ Pin up to 4 unlocked milestone badges to showcase here",
                    size = 12.sp,
                    color = c.fgSubtle
                )
            }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pinnedBadges.take(4).forEach { badge ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(c.surfaceHigh)
                            .border(1.dp, badge.tier.borderColor, RoundedCornerShape(12.dp))
                            .clickable { onManagePins() }
                            .padding(10.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(badge.color),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getMilestoneIcon(badge.iconName),
                                    contentDescription = badge.title,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            BodyText(
                                badge.title,
                                size = 11.sp,
                                weight = FontWeight.Black,
                                maxLines = 1,
                                color = Color.White
                            )
                            BodyText(
                                badge.tier.label,
                                size = 9.sp,
                                weight = FontWeight.Bold,
                                color = badge.color
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val c = MdTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(c.surfaceHigh)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(16.dp))
            DisplayText(value, size = 18.sp, uppercase = false)
            BodyText(label, size = 10.sp, weight = FontWeight.Bold, color = c.fgSubtle)
        }
    }
}

@Composable
private fun MilestonesCard(
    summary: com.mangadl.android.data.gamification.UserMilestoneSummary,
    onViewAll: () -> Unit
) {
    val c = MdTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.surfaceHigh)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header Row: Trophy + Title + Counter + "View All"
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(MdIcons.Trophy, contentDescription = "Trophy", tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                    BodyText("READER MILESTONES", size = 12.sp, weight = FontWeight.Black, color = Color.White)
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        BodyText(
                            "${summary.unlockedCount} / ${summary.totalCount}",
                            size = 10.sp,
                            weight = FontWeight.Black,
                            color = Color(0xFFF59E0B)
                        )
                    }
                }

                Row(
                    modifier = Modifier.clickable { onViewAll() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    BodyText("View All (${summary.totalCount})", size = 11.sp, weight = FontWeight.Bold, color = Color(0xFFEF4444))
                    Icon(MdIcons.ChevronRight, contentDescription = "Chevron", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                }
            }

            // Next Milestone Spotlight Card
            val next = summary.nextMilestone
            if (next != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(next.badge.color.copy(alpha = 0.2f))
                                        .border(1.dp, next.badge.color.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getMilestoneIcon(next.badge.iconName),
                                        contentDescription = next.badge.title,
                                        tint = next.badge.color,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Column {
                                    BodyText("NEXT: ${next.badge.title}", size = 12.sp, weight = FontWeight.Black, color = Color.White)
                                    BodyText(next.badge.category.label, size = 10.sp, color = c.fgSubtle)
                                }
                            }
                            BodyText("${next.percent}%", size = 12.sp, weight = FontWeight.Black, color = next.badge.color)
                        }

                        LinearProgressIndicator(
                            progress = { next.percent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = next.badge.color,
                            trackColor = Color.White.copy(alpha = 0.08f)
                        )

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            BodyText(next.badge.description, size = 10.sp, color = c.fgSubtle)
                            BodyText(
                                "${next.current} / ${next.total} (${next.remaining} left)",
                                size = 10.sp,
                                weight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Featured Honors (Unlocked Badges Grid)
            if (summary.unlocked.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BodyText("RECENTLY UNLOCKED", size = 10.sp, weight = FontWeight.Black, color = c.fgSubtle)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        summary.unlocked.takeLast(3).reversed().forEach { badge ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(badge.tier.bgColor)
                                    .border(1.dp, badge.tier.borderColor, RoundedCornerShape(10.dp))
                                    .clickable { onViewAll() }
                                    .padding(8.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(badge.color),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getMilestoneIcon(badge.iconName),
                                            contentDescription = badge.title,
                                            tint = Color.Black,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    BodyText(badge.title, size = 10.sp, weight = FontWeight.Black, maxLines = 1, color = Color.White)
                                    BodyText(badge.tier.label, size = 8.sp, weight = FontWeight.Bold, color = badge.color)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Milestones Gallery Modal
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MilestonesGalleryModal(
    summary: com.mangadl.android.data.gamification.UserMilestoneSummary,
    pinnedIds: Set<String>,
    onTogglePin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val c = MdTheme.colors
    var selectedCategory by remember { mutableStateOf<MilestoneCategory?>(null) }

    val filteredList = remember(selectedCategory) {
        if (selectedCategory == null) MILESTONES
        else MILESTONES.filter { it.category == selectedCategory }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable { onDismiss() }
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(c.surfaceHigh)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .clickable(enabled = false) {}
                    .padding(18.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Modal Header
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(MdIcons.Trophy, contentDescription = "Trophy", tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                            }
                            Column {
                                BodyText("All Reader Milestones", size = 16.sp, weight = FontWeight.Black, color = Color.White)
                                BodyText("${summary.unlockedCount} of ${summary.totalCount} milestones conquered", size = 11.sp, color = c.fgSubtle)
                            }
                        }
                        MdIconButton(MdIcons.Close, "Close", onDismiss, iconSize = 18.dp)
                    }

                    // Category Tabs Row
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CategoryTabButton(
                            label = "All (${MILESTONES.size})",
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            modifier = Modifier.weight(1f)
                        )
                        val chCounts = summary.categoryCounts[MilestoneCategory.CHAPTERS] ?: Pair(0, 15)
                        CategoryTabButton(
                            label = "Ch (${chCounts.first}/${chCounts.second})",
                            selected = selectedCategory == MilestoneCategory.CHAPTERS,
                            onClick = { selectedCategory = MilestoneCategory.CHAPTERS },
                            modifier = Modifier.weight(1f)
                        )
                        val libCounts = summary.categoryCounts[MilestoneCategory.LIBRARY] ?: Pair(0, 8)
                        CategoryTabButton(
                            label = "Lib (${libCounts.first}/${libCounts.second})",
                            selected = selectedCategory == MilestoneCategory.LIBRARY,
                            onClick = { selectedCategory = MilestoneCategory.LIBRARY },
                            modifier = Modifier.weight(1f)
                        )
                        val strkCounts = summary.categoryCounts[MilestoneCategory.STREAK] ?: Pair(0, 8)
                        CategoryTabButton(
                            label = "Streak (${strkCounts.first}/${strkCounts.second})",
                            selected = selectedCategory == MilestoneCategory.STREAK,
                            onClick = { selectedCategory = MilestoneCategory.STREAK },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Badges List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredList, key = { it.id }) { badge ->
                            val isUnlocked = summary.unlocked.any { it.id == badge.id }
                            val isPinned = badge.id in pinnedIds

                            MilestoneBadgeRow(
                                badge = badge,
                                isUnlocked = isUnlocked,
                                isPinned = isPinned,
                                onTogglePin = { onTogglePin(badge.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFFEF4444) else Color.White.copy(alpha = 0.05f))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        BodyText(
            label,
            size = 10.sp,
            weight = if (selected) FontWeight.Black else FontWeight.SemiBold,
            color = if (selected) Color.White else Color(0xFFA1A1AA),
            maxLines = 1
        )
    }
}

@Composable
private fun MilestoneBadgeRow(
    badge: MilestoneBadge,
    isUnlocked: Boolean,
    isPinned: Boolean,
    onTogglePin: () -> Unit
) {
    val c = MdTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isUnlocked) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.015f))
            .border(
                1.dp,
                if (isUnlocked) badge.tier.borderColor else Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(12.dp)
            )
            .padding(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Badge Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isUnlocked) badge.color else Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getMilestoneIcon(badge.iconName),
                        contentDescription = badge.title,
                        tint = if (isUnlocked) Color.Black else Color(0xFF71717A),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BodyText(
                            badge.title,
                            size = 12.sp,
                            weight = FontWeight.Black,
                            color = if (isUnlocked) Color.White else Color(0xFFA1A1AA)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(badge.tier.bgColor)
                                .border(1.dp, badge.tier.borderColor, RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            BodyText(badge.tier.label, size = 8.sp, weight = FontWeight.Black, color = badge.color)
                        }
                    }
                    BodyText(
                        badge.description,
                        size = 10.sp,
                        color = c.fgSubtle,
                        maxLines = 1
                    )
                    BodyText(
                        if (isUnlocked) "✓ Unlocked (${badge.threshold} ${badge.category.label})"
                        else "🔒 Needs ${badge.threshold} ${badge.category.label}",
                        size = 9.sp,
                        weight = FontWeight.Bold,
                        color = if (isUnlocked) Color(0xFF34D399) else Color(0xFF71717A)
                    )
                }
            }

            // Pin / Unpin Action (Only available if unlocked)
            if (isUnlocked) {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isPinned) Color(0xFFF59E0B).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                        .clickable { onTogglePin() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = MdIcons.Pin,
                        contentDescription = if (isPinned) "Unpin" else "Pin",
                        tint = if (isPinned) Color(0xFFF59E0B) else Color(0xFFA1A1AA),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

private fun getMilestoneIcon(iconName: String): ImageVector = when (iconName) {
    "BookOpen" -> MdIcons.BookOpen
    "Flame" -> MdIcons.Flame
    "Library" -> MdIcons.Library
    "Crown" -> MdIcons.Crown
    "Sparkles" -> MdIcons.Sparkles
    "Zap" -> MdIcons.Zap
    "Compass" -> MdIcons.Compass
    "Shield" -> MdIcons.Shield
    "Trophy" -> MdIcons.Trophy
    "Feather" -> MdIcons.Feather
    "Scroll" -> MdIcons.Scroll
    "Star" -> MdIcons.Star
    "Infinity" -> MdIcons.InfinityIcon
    else -> MdIcons.Award
}

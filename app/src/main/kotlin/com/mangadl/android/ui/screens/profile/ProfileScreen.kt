package com.mangadl.android.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mangadl.android.data.model.ReadingProgress
import com.mangadl.android.ui.components.SectionLabel
import com.mangadl.android.ui.theme.AntonStyle
import com.mangadl.android.ui.theme.MangaDlColors
import com.mangadl.android.ui.theme.MangaDlTheme
import com.mangadl.android.ui.viewmodels.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

private val AvatarDeepRed = Color(0xFF3A1518)

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    onBack: () -> Unit,
) {
    val library by viewModel.library.collectAsStateWithLifecycle()
    val recentActivity by viewModel.recentActivity.collectAsStateWithLifecycle()
    val totalChaptersRead by viewModel.totalChaptersRead.collectAsStateWithLifecycle()
    val mangaInLibrary = library.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MangaDlColors.Background),
    ) {
        // Banner + overlapping avatar
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(184.dp),
            ) {
                // Banner background
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(MangaDlColors.AvatarBg),
                )
                // Buttons inside banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MangaDlColors.TextPrimary,
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share profile",
                            tint = MangaDlColors.TextPrimary,
                        )
                    }
                }
                // Avatar overlapping bottom of banner by 44dp
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp)
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(AvatarDeepRed)
                        .border(4.dp, MangaDlColors.Background, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "U",
                        color = MangaDlColors.TextPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }

        // Name + username + edit button + bio
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Display Name",
                            color = MangaDlColors.TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            "@username",
                            color = Color(0x99FFFFFF),
                            fontSize = 14.sp,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(999.dp))
                            .clickable { }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Edit Profile",
                            color = MangaDlColors.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Text(
                    "Your reading notes go here.",
                    color = Color(0xBFFFFFFF),
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                )
            }
        }

        // 3-column stats row with dividers
        item {
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                listOf(
                    totalChaptersRead.toString() to "Chapters",
                    mangaInLibrary.toString() to "Library",
                    "1" to "Day streak",
                ).forEach { (value, label) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(value, style = AntonStyle, color = MangaDlColors.TextPrimary)
                        Text(label, color = Color(0x99FFFFFF), fontSize = 12.sp)
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
        }

        // Pinned section
        item {
            Spacer(Modifier.height(20.dp))
            SectionLabel("Pinned", color = MangaDlColors.SectionRed, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // 3 placeholder covers + 1 "add" slot
                listOf(
                    MangaDlColors.AvatarBg,
                    MangaDlColors.DetailHeaderBg,
                    Color(0xFF2E2412),
                ).forEach { bg ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg),
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+", color = Color(0x99FFFFFF), fontSize = 22.sp)
                }
            }
        }

        // Milestones section
        item {
            Spacer(Modifier.height(20.dp))
            SectionLabel("Milestones", color = Color(0x99FFFFFF), modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("First Read", "10 Chapters").forEach { milestone ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0x29DC2626))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(milestone, color = Color(0xFFFCA5A5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text("100 Chapters", color = Color(0x99FFFFFF), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Recent activity
        item {
            Spacer(Modifier.height(20.dp))
            SectionLabel("Recent activity", color = MangaDlColors.SectionRed, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        }

        if (recentActivity.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No reading activity yet",
                        color = MangaDlColors.TextSecondary,
                        fontSize = 14.sp,
                    )
                }
            }
        } else {
            items(recentActivity.size) { i ->
                ActivityRow(progress = recentActivity[i])
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun ActivityRow(progress: ReadingProgress) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MangaDlColors.CardBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = MangaDlColors.TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                progress.mangaId.split(":").lastOrNull() ?: progress.mangaId,
                color = MangaDlColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                "Ch. ${progress.chapterId.split(":").lastOrNull() ?: progress.chapterId}",
                color = MangaDlColors.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
        Text(timeAgo(progress.readAt), color = MangaDlColors.TextSecondary, fontSize = 12.sp)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(MangaDlColors.CardBorder))
}

private fun timeAgo(ms: Long): String {
    val diff = System.currentTimeMillis() - ms
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "just now"
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
        diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
        diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(ms))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
private fun ProfileScreenPreview() {
    MangaDlTheme {
        Column(Modifier.fillMaxSize().background(MangaDlColors.Background)) {
            Box(Modifier.fillMaxWidth().height(184.dp)) {
                Spacer(Modifier.fillMaxWidth().height(140.dp).background(MangaDlColors.AvatarBg))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp, 36.dp, 8.dp, 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MangaDlColors.TextPrimary, modifier = Modifier.size(24.dp))
                    Icon(Icons.Default.Share, null, tint = MangaDlColors.TextPrimary, modifier = Modifier.size(24.dp))
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp)
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(AvatarDeepRed)
                        .border(4.dp, MangaDlColors.Background, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("U", color = MangaDlColors.TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column {
                        Text("Display Name", color = MangaDlColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text("@username", color = Color(0x99FFFFFF), fontSize = 14.sp)
                    }
                    Box(
                        modifier = Modifier.height(40.dp).clip(RoundedCornerShape(999.dp)).border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(999.dp)).padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Edit Profile", color = MangaDlColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("381" to "Chapters", "42" to "Library", "7" to "Day streak").forEach { (v, l) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(v, style = AntonStyle, color = MangaDlColors.TextPrimary)
                        Text(l, color = Color(0x99FFFFFF), fontSize = 12.sp)
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
        }
    }
}

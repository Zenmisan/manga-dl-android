package com.mangadl.android.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.model.ReadingProgress
import com.mangadl.android.ui.theme.AntonStyle
import com.mangadl.android.ui.theme.MangaDlColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val db = MangaDlApp.instance.database
    val scope = rememberCoroutineScope()

    var totalChaptersRead by remember { mutableStateOf(0) }
    var mangaInLibrary by remember { mutableStateOf(0) }
    var recentActivity by remember { mutableStateOf<List<ReadingProgress>>(emptyList()) }

    LaunchedEffect(Unit) {
        db.libraryDao().getAll().collect { lib ->
            mangaInLibrary = lib.size
        }
    }
    LaunchedEffect(Unit) {
        db.progressDao().getRecent(5).collect { items ->
            recentActivity = items
        }
    }
    LaunchedEffect(Unit) {
        db.progressDao().getRecent(1000).collect { items ->
            totalChaptersRead = items.count { it.completed }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MangaDlColors.Background),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 12.dp, top = 20.dp, bottom = 8.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MangaDlColors.TextPrimary,
                    )
                }
                Text(
                    "PROFILE",
                    style = AntonStyle,
                    color = MangaDlColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            // Avatar + user info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MangaDlColors.Primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "U",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "User",
                        color = MangaDlColors.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        "Sign in to sync your library",
                        color = MangaDlColors.TextSecondary,
                        fontSize = 13.sp,
                    )
                }

                // Streak
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MangaDlColors.CardBg)
                        .border(1.dp, MangaDlColors.CardBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("🔥", fontSize = 16.sp)
                    Text(
                        "1 day streak",
                        color = MangaDlColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        item {
            // Stats cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatCard(
                    label = "Chapters Read",
                    value = totalChaptersRead.toString(),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = "Manga in Library",
                    value = mangaInLibrary.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "RECENT ACTIVITY",
                    color = MangaDlColors.SectionRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                )
            }
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
                val item = recentActivity[i]
                ActivityRow(progress = item)
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MangaDlColors.CardBg)
            .border(1.dp, MangaDlColors.CardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            value,
            color = MangaDlColors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            label,
            color = MangaDlColors.TextSecondary,
            fontSize = 12.sp,
        )
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
                Icons.Default.MenuBook,
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
        Text(
            timeAgo(progress.readAt),
            color = MangaDlColors.TextSecondary,
            fontSize = 12.sp,
        )
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MangaDlColors.CardBorder),
    )
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

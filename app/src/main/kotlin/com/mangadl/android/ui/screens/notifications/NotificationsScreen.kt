package com.mangadl.android.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.TextButton
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
import com.mangadl.android.data.model.LibraryManga
import com.mangadl.android.ui.theme.AntonStyleSub
import com.mangadl.android.ui.theme.MangaDlColors
import com.mangadl.android.ui.theme.MangaDlTheme
import com.mangadl.android.ui.viewmodels.NotificationsViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel = viewModel(),
    onBack: () -> Unit,
    onMangaClick: (provider: String, mangaId: String) -> Unit,
) {
    val allLibrary by viewModel.library.collectAsStateWithLifecycle()

    var cleared by remember { mutableStateOf(false) }
    val notifications = remember(allLibrary, cleared) {
        if (cleared) emptyList()
        else allLibrary
            .filter { it.totalChapters > it.readCount }
            .sortedByDescending { it.lastReadAt ?: it.addedAt }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MangaDlColors.Background),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 8.dp, top = 8.dp, bottom = 4.dp, end = 4.dp),
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
                "Notifications".uppercase(),
                style = AntonStyleSub,
                color = MangaDlColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            if (notifications.isNotEmpty()) {
                TextButton(onClick = { cleared = true }) {
                    Text(
                        "Mark All Read",
                        color = MangaDlColors.Primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MangaDlColors.CardBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MangaDlColors.TextSecondary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Text(
                        "All caught up",
                        color = MangaDlColors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "No new notifications",
                        color = MangaDlColors.TextSecondary,
                        fontSize = 13.sp,
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(notifications.size) { i ->
                    val manga = notifications[i]
                    NotificationRow(
                        manga = manga,
                        onClick = {
                            onMangaClick(manga.provider, manga.id.removePrefix("${manga.provider}:"))
                        },
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun NotificationRow(manga: LibraryManga, onClick: () -> Unit) {
    val newChapters = (manga.totalChapters - manga.readCount).coerceAtLeast(0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Circle icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0x29DC2626)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                tint = MangaDlColors.PrimaryLight,
                modifier = Modifier.size(20.dp),
            )
        }

        // Content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                manga.title,
                color = MangaDlColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
            )
            Text(
                "$newChapters new chapter${if (newChapters == 1) "" else "s"} available",
                color = Color(0xBFFFFFFF),
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            Text(
                timeAgo(manga.lastReadAt ?: manga.addedAt),
                color = Color(0x80FFFFFF),
                fontSize = 12.sp,
            )
        }

        // Unread dot
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(MangaDlColors.Primary),
        )
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0x0FFFFFFF)),
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

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
private fun NotificationsScreenPreview() {
    MangaDlTheme {
        Column(Modifier.fillMaxSize().background(MangaDlColors.Background)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 36.dp, bottom = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MangaDlColors.TextPrimary, modifier = Modifier.padding(8.dp))
                Text("NOTIFICATIONS", style = AntonStyleSub, color = MangaDlColors.TextPrimary, modifier = Modifier.weight(1f))
                Text("Mark All Read", color = MangaDlColors.Primary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
            }
            // Sample row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0x29DC2626)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Notifications, null, tint = MangaDlColors.PrimaryLight, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("One Piece", color = MangaDlColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("3 new chapters available", color = Color(0xBFFFFFFF), fontSize = 13.sp)
                    Text("2h ago", color = Color(0x80FFFFFF), fontSize = 12.sp)
                }
                Box(Modifier.padding(top = 6.dp).size(8.dp).clip(CircleShape).background(MangaDlColors.Primary))
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
        }
    }
}

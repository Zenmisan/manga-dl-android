package com.mangadl.android.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.model.LibraryManga
import com.mangadl.android.ui.theme.AntonStyle
import com.mangadl.android.ui.theme.MangaDlColors
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onMangaClick: (provider: String, mangaId: String) -> Unit,
) {
    val db = MangaDlApp.instance.database

    var notifications by remember { mutableStateOf<List<LibraryManga>>(emptyList()) }
    var cleared by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        db.libraryDao().getAll().collect { lib ->
            notifications = lib
                .filter { it.totalChapters > it.readCount && !cleared }
                .sortedByDescending { it.lastReadAt ?: it.addedAt }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MangaDlColors.Background),
    ) {
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
                "NOTIFICATIONS",
                style = AntonStyle,
                color = MangaDlColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            if (notifications.isNotEmpty()) {
                IconButton(
                    onClick = { cleared = true; notifications = emptyList() },
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Clear all",
                        tint = MangaDlColors.TextSecondary,
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
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MangaDlColors.TextSecondary,
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        "No new notifications",
                        color = MangaDlColors.TextSecondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "You're all caught up",
                        color = Color(0x66FFFFFF),
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
                            val mangaId = manga.id.removePrefix("${manga.provider}:")
                            onMangaClick(manga.provider, mangaId)
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
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MangaDlColors.CardBg)
                    .border(1.dp, MangaDlColors.CardBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = MangaDlColors.TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
            // Red dot indicator
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MangaDlColors.Primary)
                    .align(Alignment.TopEnd),
            )
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                manga.title,
                color = MangaDlColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
            Text(
                "$newChapters new chapter${if (newChapters == 1) "" else "s"} available",
                color = MangaDlColors.TextSecondary,
                fontSize = 12.sp,
            )
            Text(
                timeAgo(manga.lastReadAt ?: manga.addedAt),
                color = Color(0x66FFFFFF),
                fontSize = 11.sp,
            )
        }

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0x80FFFFFF),
            modifier = Modifier.size(18.dp),
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

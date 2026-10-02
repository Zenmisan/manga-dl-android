package com.mangadl.android.ui.screens.updates

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.model.LibraryManga
import com.mangadl.android.ui.theme.AntonStyle
import com.mangadl.android.ui.theme.MangaDlColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch

@Composable
fun UpdatesScreen(
    onChapterClick: (provider: String, mangaId: String, chapterId: String) -> Unit,
) {
    val db = MangaDlApp.instance.database

    val library by db.libraryDao().getAll()
        .catch { emit(emptyList()) }
        .collectAsState(initial = emptyList())

    val updatedManga = remember(library) {
        library
            .filter { it.totalChapters > it.readCount }
            .sortedByDescending { it.lastReadAt ?: it.addedAt }
    }

    var refreshing by remember { mutableStateOf(false) }

    LaunchedEffect(refreshing) {
        if (refreshing) {
            delay(1500)
            refreshing = false
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MangaDlColors.Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Updates".uppercase(),
                style = AntonStyle,
                color = MangaDlColors.TextPrimary,
            )
            IconButton(onClick = { refreshing = true }) {
                if (refreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MangaDlColors.Primary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Check for updates", tint = MangaDlColors.TextPrimary)
                }
            }
        }

        if (updatedManga.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MangaDlColors.TextSecondary,
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        "All caught up!",
                        color = MangaDlColors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "No unread chapters.",
                        color = MangaDlColors.TextSecondary,
                        fontSize = 13.sp,
                    )
                }
            }
        } else {
            Text(
                text = "Unread chapters · ${updatedManga.size} series",
                color = MangaDlColors.TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
            )

            LazyColumn(Modifier.fillMaxSize()) {
                items(updatedManga, key = { it.id }) { manga ->
                    UpdateRow(
                        manga = manga,
                        onMangaClick = { onChapterClick(manga.provider, manga.id, manga.lastReadChapterId ?: "") },
                        onDownload = {},
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateRow(
    manga: LibraryManga,
    onMangaClick: () -> Unit,
    onDownload: () -> Unit,
) {
    val unread = (manga.totalChapters - manga.readCount).coerceAtLeast(0)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onMangaClick)
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 64.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MangaDlColors.CoverPlaceholder),
        ) {
            if (manga.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = manga.coverUrl,
                    contentDescription = manga.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                manga.title,
                color = MangaDlColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "$unread chapter${if (unread != 1) "s" else ""} unread",
                color = MangaDlColors.TextSecondary,
                fontSize = 13.sp,
            )
            val timeMs = manga.lastReadAt ?: manga.addedAt
            Text(
                formatRelativeTime(timeMs),
                color = MangaDlColors.TextSubtle,
                fontSize = 12.sp,
            )
        }
        IconButton(onClick = onDownload) {
            Icon(
                Icons.Default.Download,
                contentDescription = "Download",
                tint = MangaDlColors.TextSubtle,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun formatRelativeTime(millis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - millis
    return when {
        diff < 3_600_000L -> "${diff / 60_000}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000}h ago"
        diff < 7 * 86_400_000L -> "${diff / 86_400_000}d ago"
        else -> {
            val sdf = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
            sdf.format(java.util.Date(millis))
        }
    }
}

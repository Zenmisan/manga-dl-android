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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mangadl.android.data.model.LibraryManga
import com.mangadl.android.ui.components.SectionLabel
import com.mangadl.android.ui.theme.AntonStyle
import com.mangadl.android.ui.theme.MangaDlColors
import com.mangadl.android.ui.theme.MangaDlTheme
import com.mangadl.android.ui.viewmodels.UpdatesViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

private fun dateGroup(millis: Long): String {
    val now = Calendar.getInstance()
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    return when {
        now.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR) &&
            now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) -> "TODAY"
        now.get(Calendar.DAY_OF_YEAR) - cal.get(Calendar.DAY_OF_YEAR) == 1 &&
            now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) -> "YESTERDAY"
        (now.timeInMillis - millis) < 7 * 86_400_000L -> "THIS WEEK"
        else -> SimpleDateFormat("MMMM d", Locale.getDefault()).format(Date(millis)).uppercase()
    }
}

private fun formatLastChecked(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    return when {
        diff < 60_000L -> "just now"
        diff < 3_600_000L -> "${diff / 60_000}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000}h ago"
        else -> SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(millis))
    }
}

@Composable
fun UpdatesScreen(
    viewModel: UpdatesViewModel = viewModel(),
    onChapterClick: (provider: String, mangaId: String, chapterId: String) -> Unit,
) {
    val library by viewModel.library.collectAsStateWithLifecycle()

    val updatedManga = remember(library) {
        library
            .filter { it.totalChapters > it.readCount }
            .sortedByDescending { it.lastReadAt ?: it.addedAt }
    }

    val grouped = remember(updatedManga) {
        updatedManga.groupBy { manga ->
            dateGroup(manga.lastReadAt ?: manga.addedAt)
        }.entries.toList()
    }

    var refreshing by remember { mutableStateOf(false) }
    val lastChecked = remember { System.currentTimeMillis() - 12 * 60_000L }

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
                .statusBarsPadding()
                .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "UPDATES",
                    style = AntonStyle,
                    color = MangaDlColors.TextPrimary,
                )
                Text(
                    text = "Last checked ${formatLastChecked(lastChecked)} · next check in 30 min",
                    color = MangaDlColors.TextSecondary,
                    fontSize = 12.sp,
                )
            }
            IconButton(onClick = { refreshing = true }) {
                if (refreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MangaDlColors.Primary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Check for updates",
                        tint = MangaDlColors.TextPrimary,
                    )
                }
            }
        }

        if (updatedManga.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(36.dp))
                            .background(Color(0x14FFFFFF)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MangaDlColors.TextSecondary,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                    Text("All caught up!", color = MangaDlColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("No unread chapters", color = MangaDlColors.TextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                grouped.forEach { (group, mangas) ->
                    item(key = "header_$group") {
                        SectionLabel(
                            text = group,
                            color = MangaDlColors.SectionRed,
                            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
                        )
                    }
                    items(mangas, key = { it.id }) { manga ->
                        UpdateRow(
                            manga = manga,
                            onMangaClick = {
                                onChapterClick(manga.provider, manga.id, manga.lastReadChapterId ?: "")
                            },
                            onDownload = {},
                        )
                    }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onMangaClick)
            .padding(start = 20.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
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
            val chNum = manga.totalChapters
            Text(
                "Ch. $chNum · Latest chapter",
                color = MangaDlColors.TextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onDownload) {
            Icon(
                Icons.Default.Download,
                contentDescription = "Download",
                tint = MangaDlColors.TextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
private fun UpdatesScreenPreview() {
    MangaDlTheme {
        Column(
            Modifier
                .fillMaxSize()
                .background(MangaDlColors.Background)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 32.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("UPDATES", style = AntonStyle, color = MangaDlColors.TextPrimary)
                    Text("Last checked 12m ago · next check in 30 min", color = MangaDlColors.TextSecondary, fontSize = 12.sp)
                }
                Icon(Icons.Default.Refresh, contentDescription = null, tint = MangaDlColors.TextPrimary)
            }
            SectionLabel("TODAY", color = MangaDlColors.SectionRed, modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.size(width = 44.dp, height = 64.dp).clip(RoundedCornerShape(6.dp)).background(MangaDlColors.CoverPlaceholder))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Hollow Crown", color = MangaDlColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Ch. 51 · The Unseen King", color = MangaDlColors.TextSecondary, fontSize = 13.sp)
                }
                Icon(Icons.Default.Download, contentDescription = null, tint = MangaDlColors.TextSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

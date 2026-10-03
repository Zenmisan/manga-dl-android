package com.mangadl.android.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mangadl.android.data.model.LibraryManga
import com.mangadl.android.data.model.ReadingProgress
import androidx.compose.ui.tooling.preview.Preview
import com.mangadl.android.ui.components.ConfirmDialog
import com.mangadl.android.ui.components.EmptyState
import com.mangadl.android.ui.components.PillButton
import com.mangadl.android.ui.theme.AntonStyle
import com.mangadl.android.ui.theme.MangaDlColors
import com.mangadl.android.ui.theme.MangaDlTheme
import com.mangadl.android.ui.viewmodels.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

private enum class HistoryFilter { Today, ThisWeek, Month, All }

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = viewModel(),
    onMangaClick: (provider: String, mangaId: String) -> Unit,
    onResumeReading: (provider: String, mangaId: String, chapterId: String) -> Unit = { p, m, _ -> onMangaClick(p, m) },
) {
    val allProgress by viewModel.allProgress.collectAsStateWithLifecycle()
    val library by viewModel.library.collectAsStateWithLifecycle()

    val libraryMap = remember(library) { library.associateBy { it.id } }

    var filter by remember { mutableStateOf(HistoryFilter.ThisWeek) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val history = remember(allProgress, filter) {
        val now = System.currentTimeMillis()
        val filtered = when (filter) {
            HistoryFilter.Today -> allProgress.filter { (now - it.readAt) < 86_400_000L }
            HistoryFilter.ThisWeek -> allProgress.filter { (now - it.readAt) < 7 * 86_400_000L }
            HistoryFilter.Month -> allProgress.filter { (now - it.readAt) < 30 * 86_400_000L }
            HistoryFilter.All -> allProgress
        }
        filtered.sortedByDescending { it.readAt }
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
                .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("History".uppercase(), style = AntonStyle, color = MangaDlColors.TextPrimary)
            Row {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MangaDlColors.TextPrimary)
                }
                IconButton(onClick = { showClearConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = MangaDlColors.TextPrimary)
                }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp),
        ) {
            items(HistoryFilter.entries.size) { i ->
                val f = HistoryFilter.entries[i]
                val label = when (f) {
                    HistoryFilter.ThisWeek -> "This week"
                    else -> f.name
                }
                PillButton(text = label, active = filter == f, onClick = { filter = f })
            }
        }

        if (showClearConfirm) {
            ConfirmDialog(
                title = "Clear History",
                body = "This will remove all reading history. This cannot be undone.",
                confirmLabel = "Clear",
                onConfirm = {
                    showClearConfirm = false
                    // TODO: wire actual DB clear when progressDao supports deleteAll
                },
                onDismiss = { showClearConfirm = false },
            )
        }

        if (history.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Search,
                title = "No reading history",
                subtitle = "Start reading to build history",
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(history, key = { "${it.mangaId}:${it.chapterId}:${it.readAt}" }) { progress ->
                    val manga = libraryMap[progress.mangaId]
                    HistoryRow(
                        progress = progress,
                        manga = manga,
                        onClick = {
                            val cleanMangaId = progress.mangaId.removePrefix("${progress.provider}:")
                            onMangaClick(progress.provider, cleanMangaId)
                        },
                        onResume = {
                            val cleanMangaId = progress.mangaId.removePrefix("${progress.provider}:")
                            onResumeReading(progress.provider, cleanMangaId, progress.chapterId)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    progress: ReadingProgress,
    manga: LibraryManga?,
    onClick: () -> Unit,
    onResume: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 72.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MangaDlColors.CoverPlaceholder),
        ) {
            val coverUrl = manga?.coverUrl ?: ""
            if (coverUrl.isNotBlank()) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = manga?.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = manga?.title ?: progress.mangaId,
                color = MangaDlColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Ch. ${progress.chapterId} · page ${progress.page} of ${progress.totalPages}",
                color = MangaDlColors.TextSecondary,
                fontSize = 13.sp,
            )
            Text(
                text = formatRelativeTime(progress.readAt),
                color = Color(0x8CFFFFFF),
                fontSize = 12.sp,
            )
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0x29DC2626))
                .clickable(onClick = onResume),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Resume",
                tint = MangaDlColors.SectionRed,
                modifier = Modifier.size(18.dp),
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
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(millis))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
private fun HistoryScreenPreview() {
    MangaDlTheme {
        Column(
            Modifier
                .fillMaxSize()
                .background(MangaDlColors.Background)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 32.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("HISTORY", style = AntonStyle, color = MangaDlColors.TextPrimary)
                Row {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MangaDlColors.TextPrimary)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MangaDlColors.TextPrimary)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 72.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MangaDlColors.CoverPlaceholder),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Hollow Crown", color = MangaDlColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Ch. 48 · page 14 of 38", color = MangaDlColors.TextSecondary, fontSize = 13.sp)
                    Text("2h ago", color = Color(0x8CFFFFFF), fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0x29DC2626)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MangaDlColors.SectionRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

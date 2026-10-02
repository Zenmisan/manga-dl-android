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
import coil.compose.AsyncImage
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.model.LibraryManga
import com.mangadl.android.ui.components.PillButton
import com.mangadl.android.ui.theme.AntonStyle
import com.mangadl.android.ui.theme.MangaDlColors
import kotlinx.coroutines.flow.catch
import java.text.SimpleDateFormat
import java.util.*

private enum class HistoryFilter { Today, ThisWeek, Month, All }

@Composable
fun HistoryScreen(onMangaClick: (provider: String, mangaId: String) -> Unit) {
    val db = MangaDlApp.instance.database
    val library by db.libraryDao().getAll()
        .catch { emit(emptyList()) }
        .collectAsState(initial = emptyList())

    var filter by remember { mutableStateOf(HistoryFilter.ThisWeek) }

    val history = remember(library, filter) {
        val now = System.currentTimeMillis()
        val filtered = library
            .filter { it.lastReadAt != null }
            .sortedByDescending { it.lastReadAt }

        when (filter) {
            HistoryFilter.Today -> filtered.filter { (now - (it.lastReadAt ?: 0L)) < 86_400_000L }
            HistoryFilter.ThisWeek -> filtered.filter { (now - (it.lastReadAt ?: 0L)) < 7 * 86_400_000L }
            HistoryFilter.Month -> filtered.filter { (now - (it.lastReadAt ?: 0L)) < 30 * 86_400_000L }
            HistoryFilter.All -> filtered
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
                .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("History".uppercase(), style = AntonStyle, color = MangaDlColors.TextPrimary)
            Row {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MangaDlColors.TextPrimary)
                }
                IconButton(onClick = {}) {
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

        if (history.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No reading history", color = MangaDlColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Start reading to build history", color = MangaDlColors.TextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(history, key = { it.id }) { manga ->
                    HistoryRow(manga = manga, onClick = { onMangaClick(manga.provider, manga.id) })
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(manga: LibraryManga, onClick: () -> Unit) {
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
            if (manga.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = manga.coverUrl,
                    contentDescription = manga.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = manga.title,
                color = MangaDlColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (manga.lastReadChapterId != null) "Last read chapter" else "Not started",
                color = MangaDlColors.TextSecondary,
                fontSize = 13.sp,
            )
            Text(
                text = manga.lastReadAt?.let { formatRelativeTime(it) } ?: "",
                color = Color(0x8CFFFFFF),
                fontSize = 12.sp,
            )
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0x29DC2626))
                .clickable(onClick = onClick),
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

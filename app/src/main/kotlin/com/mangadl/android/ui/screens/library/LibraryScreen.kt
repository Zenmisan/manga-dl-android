package com.mangadl.android.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.model.LibraryManga
import com.mangadl.android.ui.components.PillButton
import com.mangadl.android.ui.theme.AntonStyle
import com.mangadl.android.ui.theme.MangaDlColors
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

private enum class LibraryTab(val label: String) {
    All("All"), Reading("Reading"), PlanToRead("Plan to read"), Done("Done")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onMangaClick: (provider: String, mangaId: String) -> Unit,
    onResumeReading: (provider: String, mangaId: String, chapterId: String) -> Unit = { p, m, _ -> onMangaClick(p, m) },
) {
    val db = MangaDlApp.instance.database
    val scope = rememberCoroutineScope()
    val library by db.libraryDao().getAll()
        .catch { emit(emptyList()) }
        .collectAsState(initial = emptyList())

    var selectedTab by remember { mutableStateOf(LibraryTab.All) }
    var contextManga by remember { mutableStateOf<LibraryManga?>(null) }

    val lastRead = library.filter { it.lastReadAt != null }
        .maxByOrNull { it.lastReadAt ?: 0L }

    val filteredLibrary = when (selectedTab) {
        LibraryTab.All -> library
        LibraryTab.Reading -> library.filter { it.lastReadAt != null && it.readCount < it.totalChapters }
        LibraryTab.PlanToRead -> library.filter { it.lastReadAt == null }
        LibraryTab.Done -> library.filter { it.totalChapters > 0 && it.readCount >= it.totalChapters }
    }

    Box(Modifier.fillMaxSize()) {
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
                Text(
                    text = "Library".uppercase(),
                    style = AntonStyle,
                    color = MangaDlColors.TextPrimary,
                )
                Row {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = MangaDlColors.TextPrimary)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Tune, contentDescription = "Filter", tint = MangaDlColors.TextPrimary)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = MangaDlColors.TextPrimary)
                    }
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 14.dp),
            ) {
                items(LibraryTab.entries.size) { i ->
                    val tab = LibraryTab.entries[i]
                    val label = if (tab == LibraryTab.All) "All · ${library.size}" else tab.label
                    PillButton(
                        text = label,
                        active = selectedTab == tab,
                        onClick = { selectedTab = tab },
                    )
                }
            }

            if (lastRead != null && selectedTab == LibraryTab.All) {
                ContinueCard(
                    manga = lastRead,
                    modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp),
                    onClick = { onMangaClick(lastRead.provider, lastRead.id) },
                    onResume = {
                        val chId = lastRead.lastReadChapterId
                        if (chId != null) {
                            onResumeReading(lastRead.provider, lastRead.id, chId)
                        } else {
                            onMangaClick(lastRead.provider, lastRead.id)
                        }
                    },
                )
            }

            if (filteredLibrary.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Your library is empty",
                            color = MangaDlColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Browse sources to find manga",
                            color = MangaDlColors.TextSecondary,
                            fontSize = 13.sp,
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(filteredLibrary, key = { it.id }) { manga ->
                        MangaGridCard(
                            manga = manga,
                            onClick = { onMangaClick(manga.provider, manga.id) },
                            onLongPress = { contextManga = manga },
                        )
                    }
                }
            }
        }

        // Long-press action sheet
        if (contextManga != null) {
            val manga = contextManga!!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000))
                    .clickable { contextManga = null },
                contentAlignment = Alignment.BottomCenter,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(Color(0xFF141414))
                        .padding(bottom = 24.dp)
                        .clickable(enabled = false) {}
                ) {
                    // Drag handle
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 12.dp, bottom = 16.dp)
                            .size(36.dp, 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0x33FFFFFF))
                    )
                    // Manga title
                    Text(
                        manga.title,
                        color = MangaDlColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    ActionSheetRow(
                        icon = Icons.Default.CheckCircle,
                        label = "Mark all read",
                        tint = Color(0xFF22c55e),
                    ) {
                        scope.launch { db.libraryDao().updateReadCount(manga.id, manga.totalChapters) }
                        contextManga = null
                    }
                    ActionSheetRow(
                        icon = Icons.Default.Download,
                        label = "Download all",
                        tint = MangaDlColors.TextPrimary,
                    ) {
                        contextManga = null
                    }
                    ActionSheetRow(
                        icon = Icons.Default.Delete,
                        label = "Remove from library",
                        tint = Color(0xFFef4444),
                    ) {
                        scope.launch { db.libraryDao().delete(manga.id) }
                        contextManga = null
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionSheetRow(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, color = tint, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContinueCard(manga: LibraryManga, modifier: Modifier = Modifier, onClick: () -> Unit, onResume: () -> Unit = onClick) {
    val cardShape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(MangaDlColors.CardBg)
            .border(1.dp, MangaDlColors.CardBorder, cardShape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 96.dp)
                .clip(RoundedCornerShape(8.dp))
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

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "CONTINUE",
                color = MangaDlColors.SectionRed,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.2.sp,
            )
            Text(
                text = manga.title,
                color = MangaDlColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (manga.lastReadChapterId != null) {
                Text(
                    text = "Ch. ${manga.lastReadChapterId}",
                    color = MangaDlColors.TextSecondary,
                    fontSize = 13.sp,
                )
            }
            val readFraction = if (manga.totalChapters > 0)
                manga.readCount.toFloat() / manga.totalChapters else 0f
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x1FFFFFFF))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(readFraction.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(MangaDlColors.Primary)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MangaDlColors.Primary)
                .clickable(onClick = onResume),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Resume",
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MangaGridCard(manga: LibraryManga, onClick: () -> Unit, onLongPress: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick, onLongClick = { onLongPress() }),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
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
        Spacer(Modifier.height(6.dp))
        Text(
            text = manga.title,
            color = MangaDlColors.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

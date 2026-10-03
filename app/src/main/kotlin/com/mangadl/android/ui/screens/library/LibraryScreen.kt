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
import androidx.compose.material.icons.filled.LocalLibrary
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.mangadl.android.ui.components.ConfirmDialog
import com.mangadl.android.ui.components.PillButton
import com.mangadl.android.ui.components.rememberHapticClick
import com.mangadl.android.ui.components.rememberHapticLongPress
import com.mangadl.android.ui.theme.AntonStyle
import com.mangadl.android.ui.theme.MangaDlColors
import com.mangadl.android.ui.theme.MangaDlTheme
import com.mangadl.android.ui.viewmodels.LibraryViewModel

private enum class LibraryTab(val label: String) {
    All("All"), Reading("Reading"), PlanToRead("Plan to read"), Done("Done")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = viewModel(),
    onMangaClick: (provider: String, mangaId: String) -> Unit,
    onResumeReading: (provider: String, mangaId: String, chapterId: String) -> Unit = { p, m, _ -> onMangaClick(p, m) },
) {
    val library by viewModel.library.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(LibraryTab.All) }
    var contextManga by remember { mutableStateOf<LibraryManga?>(null) }
    var confirmRemoveManga by remember { mutableStateOf<LibraryManga?>(null) }

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
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 4.dp, top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "LIBRARY",
                    style = AntonStyle,
                    color = MangaDlColors.TextPrimary,
                )
                Row {
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MangaDlColors.TextPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Filter",
                            tint = MangaDlColors.TextPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            // Filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp),
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

            // Continue reading card
            if (lastRead != null && selectedTab == LibraryTab.All) {
                ContinueCard(
                    manga = lastRead,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 20.dp),
                    onClick = { onMangaClick(lastRead.provider, lastRead.id) },
                    onResume = {
                        val chId = lastRead.lastReadChapterId
                        if (chId != null) onResumeReading(lastRead.provider, lastRead.id, chId)
                        else onMangaClick(lastRead.provider, lastRead.id)
                    },
                )
            }

            if (filteredLibrary.isEmpty()) {
                LibraryEmptyState(
                    tab = selectedTab,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(filteredLibrary, key = { it.id }) { manga ->
                        MangaGridCard(
                            manga = manga,
                            onClick = { onMangaClick(manga.provider, manga.id) },
                            onLongPress = { contextManga = manga },
                        )
                    }
                    // Bottom padding item
                    item { Spacer(Modifier.height(16.dp)) }
                    item { Spacer(Modifier.height(16.dp)) }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }

        // Long-press action sheet
        if (contextManga != null) {
            val manga = contextManga!!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000))
                    .clickable { contextManga = null },
                contentAlignment = Alignment.BottomCenter,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(Color(0xFF141414))
                        .navigationBarsPadding()
                        .clickable(enabled = false) {}
                ) {
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 14.dp, bottom = 18.dp)
                            .size(40.dp, 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0x33FFFFFF))
                    )
                    Text(
                        manga.title,
                        color = MangaDlColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    ActionSheetRow(
                        icon = Icons.Default.CheckCircle,
                        label = "Mark all read",
                        tint = Color(0xFF22C55E),
                    ) {
                        viewModel.markAllRead(manga.id, manga.totalChapters)
                        contextManga = null
                    }
                    ActionSheetRow(
                        icon = Icons.Default.Download,
                        label = "Download all",
                        tint = MangaDlColors.TextPrimary,
                    ) { contextManga = null }
                    ActionSheetRow(
                        icon = Icons.Default.Delete,
                        label = "Remove from library",
                        tint = Color(0xFFEF4444),
                    ) {
                        confirmRemoveManga = manga
                        contextManga = null
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        confirmRemoveManga?.let { manga ->
            ConfirmDialog(
                title = "Remove from library",
                body = "\"${manga.title}\" will be removed from your library.",
                confirmLabel = "Remove",
                onConfirm = {
                    viewModel.delete(manga.id)
                    confirmRemoveManga = null
                },
                onDismiss = { confirmRemoveManga = null },
            )
        }
    }
}

@Composable
private fun LibraryEmptyState(tab: LibraryTab, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(Color(0x14FFFFFF)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.LocalLibrary,
                    contentDescription = null,
                    tint = MangaDlColors.TextSecondary,
                    modifier = Modifier.size(38.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                when (tab) {
                    LibraryTab.All -> "Your library is empty"
                    LibraryTab.Reading -> "Nothing in progress"
                    LibraryTab.PlanToRead -> "Nothing planned"
                    LibraryTab.Done -> "Nothing finished yet"
                },
                color = MangaDlColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                when (tab) {
                    LibraryTab.All -> "Browse sources to find manga to read"
                    else -> "Add manga to your library first"
                },
                color = MangaDlColors.TextSecondary,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun ActionSheetRow(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    val haptic = rememberHapticClick()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { haptic(); onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, color = tint, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ContinueCard(
    manga: LibraryManga,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onResume: () -> Unit = onClick,
) {
    val cardShape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Color(0xFF0F0F0F))
            .border(1.dp, MangaDlColors.CardBorder, cardShape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 60.dp, height = 90.dp)
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

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                "CONTINUE",
                color = MangaDlColors.SectionRed,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
            Text(
                manga.title,
                color = MangaDlColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (manga.lastReadChapterId != null) {
                Text(
                    "Ch. ${manga.lastReadChapterId}",
                    color = MangaDlColors.TextSecondary,
                    fontSize = 12.sp,
                )
            }
            val readFraction = if (manga.totalChapters > 0)
                manga.readCount.toFloat() / manga.totalChapters else 0f
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x20FFFFFF))
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
                .size(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MangaDlColors.Primary)
                .clickable(onClick = onResume),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Resume",
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MangaGridCard(manga: LibraryManga, onClick: () -> Unit, onLongPress: () -> Unit) {
    val unreadCount = (manga.totalChapters - manga.readCount).coerceAtLeast(0)
    val hapticLong = rememberHapticLongPress()

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { hapticLong(); onLongPress() },
            ),
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

            // Bottom gradient overlay for readability
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.4f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xCC000000)),
                        )
                    )
            )

            // Unread count badge (top-left)
            if (unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .padding(5.dp)
                        .sizeIn(minWidth = 22.dp, minHeight = 22.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(MangaDlColors.Primary)
                        .align(Alignment.TopStart),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }

            // Download indicator (top-right)
            Box(
                modifier = Modifier
                    .padding(5.dp)
                    .size(22.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color(0x80000000))
                    .align(Alignment.TopEnd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = "Download",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
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
            lineHeight = 15.sp,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
private fun LibraryEmptyPreview() {
    MangaDlTheme {
        Column(
            Modifier
                .fillMaxSize()
                .background(MangaDlColors.Background)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 4.dp, top = 24.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("LIBRARY", style = AntonStyle, color = MangaDlColors.TextPrimary)
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                items(LibraryTab.entries.size) { i ->
                    val tab = LibraryTab.entries[i]
                    PillButton(text = tab.label, active = i == 0, onClick = {})
                }
            }
            LibraryEmptyState(LibraryTab.All, Modifier.fillMaxSize())
        }
    }
}

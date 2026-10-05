package com.mangadl.android.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.data.ui.ContinueItem
import com.mangadl.android.data.ui.Manga
import com.mangadl.android.ui.components.Bloom
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.ButtonTone
import com.mangadl.android.ui.components.CountBadge
import com.mangadl.android.ui.components.CoverArt
import com.mangadl.android.ui.components.DisplayText
import com.mangadl.android.ui.components.Eyebrow
import com.mangadl.android.ui.components.InLibraryTag
import com.mangadl.android.ui.components.MdButton
import com.mangadl.android.ui.components.MdIconButton
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.components.PillChip
import com.mangadl.android.ui.components.ProgressBar
import com.mangadl.android.ui.components.SurfaceCard
import com.mangadl.android.ui.components.Divider
import com.mangadl.android.ui.components.SearchField
import com.mangadl.android.ui.components.TabHeader
import com.mangadl.android.ui.components.rememberState
import com.mangadl.android.ui.theme.MdTheme

@Composable
fun LibraryScreen(
    items: List<Manga>,
    continueItem: ContinueItem?,
    onOpenManga: (Manga) -> Unit,
    onResume: (Manga) -> Unit,
    onBrowse: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
    categories: List<String> = listOf("All"),
) {
    var category by rememberState(0)
    var searchActive by rememberState(false)
    var searchQuery by rememberState("")
    var showFilter by rememberState(false)
    var sortBy by rememberSaveable { androidx.compose.runtime.mutableStateOf("title_asc") }
    var filterBy by rememberSaveable { androidx.compose.runtime.mutableStateOf("all") }

    val displayItems = remember(items, sortBy, filterBy, searchQuery) {
        items
            .let { list ->
                if (searchQuery.isNotEmpty()) list.filter { it.title.contains(searchQuery, ignoreCase = true) }
                else list
            }
            .let { list ->
                when (filterBy) {
                    "unread" -> list.filter { it.unread > 0 }
                    "completed" -> list.filter { it.unread == 0 }
                    else -> list
                }
            }
            .let { list ->
                when (sortBy) {
                    "title_desc" -> list.sortedByDescending { it.title.lowercase() }
                    "unread_desc" -> list.sortedByDescending { it.unread }
                    else -> list.sortedBy { it.title.lowercase() }
                }
            }
    }

    Box(modifier.fillMaxSize()) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 600.dp
        val columns = when {
            maxWidth >= 840.dp -> 6
            wide -> 4
            else -> 3
        }
        val side = if (wide) 36.dp else 20.dp
        Column(Modifier.fillMaxSize()) {
            if (wide) {
                Row(Modifier.fillMaxWidth().padding(start = side, end = side, top = 28.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    DisplayText("Library", 40.sp, Modifier.weight(1f))
                    MdIconButton(MdIcons.Search, "Search library", { searchActive = !searchActive }, size = 48.dp, iconSize = 20.dp, background = Color.Transparent)
                    MdIconButton(MdIcons.Filter, "Sort and filter", { showFilter = true }, size = 48.dp, iconSize = 20.dp)
                }
            } else {
                TabHeader("Library") {
                    MdIconButton(MdIcons.Search, "Search library", { searchActive = !searchActive })
                    MdIconButton(MdIcons.Filter, "Sort and filter", { showFilter = true })
                    MdIconButton(MdIcons.More, "More options", {})
                }
            }
            if (searchActive) {
                SearchField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Search library…",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = side, vertical = 4.dp),
                )
            }
            if (items.isEmpty()) {
                EmptyLibrary(onBrowse, onImport)
                return@Column
            }
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(start = side, end = side, top = 4.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.forEachIndexed { i, label -> PillChip(label, i == category, { category = i }) }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(start = side, end = side, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(if (wide) 20.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (continueItem != null && !wide) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ContinueCard(continueItem, { onResume(continueItem.manga) }, Modifier.padding(bottom = 4.dp))
                    }
                }
                items(displayItems, key = { it.id }) { manga ->
                    CoverCell(manga, { onOpenManga(manga) })
                }
            }
        }
    }
    FilterSortSheet(
        visible = showFilter,
        sortBy = sortBy,
        onSortChange = { sortBy = it },
        filterBy = filterBy,
        onFilterChange = { filterBy = it },
        onDismiss = { showFilter = false },
    )
    } // Box
}

@Composable
private fun FilterSortSheet(
    visible: Boolean,
    sortBy: String,
    onSortChange: (String) -> Unit,
    filterBy: String,
    onFilterChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = MdTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(interactionSource = interactionSource, indication = null, onClick = onDismiss),
        )
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(c.sheet)
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                BodyText("Sort & Filter", size = 16.sp, weight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Eyebrow("Sort by", Modifier.padding(bottom = 4.dp))
                    listOf(
                        "title_asc" to "Title A–Z",
                        "title_desc" to "Title Z–A",
                        "unread_desc" to "Most unread",
                    ).forEach { (key, label) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (sortBy == key) c.accentFaint else Color.Transparent)
                                .clickable { onSortChange(key) }
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BodyText(label, Modifier.weight(1f), size = 14.sp,
                                weight = if (sortBy == key) FontWeight.Bold else FontWeight.Normal,
                                color = if (sortBy == key) c.accentSoft else c.fg)
                            if (sortBy == key) Icon(MdIcons.Check, null, tint = c.accentSoft, modifier = Modifier.size(16.dp))
                        }
                        Divider(color = c.surfaceHigh)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Eyebrow("Filter", Modifier.padding(bottom = 4.dp))
                    listOf(
                        "all" to "All",
                        "unread" to "Has unread",
                        "completed" to "Completed",
                    ).forEach { (key, label) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (filterBy == key) c.accentFaint else Color.Transparent)
                                .clickable { onFilterChange(key) }
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BodyText(label, Modifier.weight(1f), size = 14.sp,
                                weight = if (filterBy == key) FontWeight.Bold else FontWeight.Normal,
                                color = if (filterBy == key) c.accentSoft else c.fg)
                            if (filterBy == key) Icon(MdIcons.Check, null, tint = c.accentSoft, modifier = Modifier.size(16.dp))
                        }
                        Divider(color = c.surfaceHigh)
                    }
                }
            }
        }
    }
}

@Composable
fun CoverCell(
    manga: Manga,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showInLibraryTag: Boolean = false,
    dimInLibrary: Boolean = false,
) {
    val c = MdTheme.colors
    Column(
        modifier.clickable(role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f)) {
            CoverArt(
                if (dimInLibrary && manga.inLibrary) manga.cover.copy(alpha = 0.55f) else manga.cover,
                Modifier.fillMaxSize(),
            )
            if (showInLibraryTag && manga.inLibrary) {
                InLibraryTag(Modifier.padding(6.dp))
            } else if (manga.unread > 0) {
                CountBadge(manga.unread, Modifier.padding(6.dp))
            }
            if (manga.downloaded && !showInLibraryTag) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(MdIcons.Download, "Downloaded", tint = c.fg, modifier = Modifier.size(13.dp))
                }
            }
        }
        BodyText(manga.title, size = 12.sp, weight = FontWeight.SemiBold, lineHeight = 16.sp, maxLines = 2)
    }
}

@Composable
private fun ContinueCard(item: ContinueItem, onResume: () -> Unit, modifier: Modifier = Modifier) {
    val c = MdTheme.colors
    SurfaceCard(modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            CoverArt(item.manga.cover, Modifier.size(64.dp, 96.dp), RoundedCornerShape(8.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Eyebrow("Continue")
                BodyText(item.manga.title, size = 16.sp, weight = FontWeight.Bold)
                BodyText(item.chapter, size = 13.sp, color = c.fgSubtle)
                ProgressBar(item.progress, Modifier.padding(top = 4.dp))
            }
            MdIconButton(MdIcons.Play, "Resume ${item.manga.title}", onResume, size = 48.dp, iconSize = 20.dp, background = c.accent, tint = Color.White)
        }
    }
}

@Composable
private fun EmptyLibrary(onBrowse: () -> Unit, onImport: () -> Unit) {
    val c = MdTheme.colors
    Box(Modifier.fillMaxSize()) {
        Bloom(Modifier.offset((-100).dp, (-40).dp).size(460.dp), alpha = 0.18f)
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 48.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(c.accentMuted),
                contentAlignment = Alignment.Center,
            ) { Icon(MdIcons.Library, null, tint = c.accentLight, modifier = Modifier.size(28.dp)) }
            BodyText("Nothing here yet", size = 26.sp, weight = FontWeight.Black)
            BodyText(
                "Add manga from a source, or import CBZ, ZIP and EPUB files you already have.",
                size = 15.sp, color = c.fgMuted, lineHeight = 22.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                MdButton("Browse Sources", onBrowse, height = 48.dp, shape = RoundedCornerShape(12.dp), fontSize = 14.sp)
                MdButton("Import Files", onImport, tone = ButtonTone.Ghost, height = 48.dp, shape = RoundedCornerShape(12.dp), fontSize = 14.sp)
            }
            Column(Modifier.padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(3) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(2f / 3f)
                                    .border(1.dp, c.fg.copy(alpha = if (row == 0) 0.09f else 0.05f), RoundedCornerShape(10.dp)),
                            )
                        }
                    }
                }
            }
        }
    }
}

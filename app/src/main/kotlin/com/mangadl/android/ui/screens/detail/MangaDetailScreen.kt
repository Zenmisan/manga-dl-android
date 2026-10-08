package com.mangadl.android.ui.screens.detail

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.data.ui.Manga
import com.mangadl.android.data.ui.UiChapter
import com.mangadl.android.data.ui.UiTracker
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.CoverArt
import com.mangadl.android.ui.components.DisplayText
import com.mangadl.android.ui.components.Divider
import com.mangadl.android.ui.components.MdButton
import com.mangadl.android.ui.components.MdIconButton
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.components.VSpace
import com.mangadl.android.ui.components.rememberState
import com.mangadl.android.ui.screens.tracking.TrackingSheet
import com.mangadl.android.ui.theme.MdTheme

@Composable
fun MangaDetailScreen(
    manga: Manga,
    chapters: List<UiChapter>,
    onBack: () -> Unit,
    onResume: () -> Unit,
    onOpenChapter: (UiChapter) -> Unit,
    onToggleLibrary: () -> Unit = {},
    onWebView: () -> Unit = {},
    onDownloadChapter: (UiChapter) -> Unit = {},
    onDownloadAllChapters: () -> Unit = {},
    initiallyTracking: Boolean = false,
    genres: List<String> = emptyList(),
    trackers: List<UiTracker> = emptyList(),
    synopsis: String = "",
    authors: List<String> = emptyList(),
    resumeLabel: String = "Start Reading",
) {
    val c = MdTheme.colors
    var inLibrary by rememberState(manga.inLibrary)
    var showTracking by rememberState(initiallyTracking)
    var synopsisOpen by rememberState(false)

    var sortAscending by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var filterMode by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("all") }

    val displayChapters = androidx.compose.runtime.remember(chapters, sortAscending, filterMode) {
        chapters
            .let { list ->
                when (filterMode) {
                    "unread" -> list.filter { !it.read }
                    "downloaded" -> list.filter { it.downloaded }
                    else -> list
                }
            }
            .let { list ->
                if (sortAscending) {
                    list.sortedBy { it.number.toFloatOrNull() ?: 0f }
                } else {
                    list.sortedByDescending { it.number.toFloatOrNull() ?: 0f }
                }
            }
    }

    val config = LocalConfiguration.current
    val isTwoPane = config.orientation == Configuration.ORIENTATION_LANDSCAPE || config.screenWidthDp >= 720

    Box(Modifier.fillMaxSize().background(c.bg)) {
        if (isTwoPane) {
            Row(Modifier.fillMaxSize()) {
                // Left pane: Manga info & metadata
                Column(
                    Modifier
                        .weight(0.44f)
                        .fillMaxHeight()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MdIconButton(MdIcons.Back, "Back", onBack)
                        Row {
                            MdIconButton(MdIcons.Download, "Download chapters", onDownloadAllChapters)
                            MdIconButton(MdIcons.Globe, "Open in browser", onWebView)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CoverArt(
                            manga.cover.copy(red = manga.cover.red * 0.6f, green = manga.cover.green * 0.6f, blue = manga.cover.blue * 0.6f),
                            Modifier.size(108.dp, 160.dp).border(1.dp, c.border, RoundedCornerShape(10.dp)),
                            imageUrl = manga.coverUrl,
                        )
                        Column(Modifier.align(Alignment.Bottom), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            DisplayText(manga.title, 24.sp, lineHeight = 28.sp)
                            if (authors.isNotEmpty()) {
                                BodyText(authors.joinToString(", "), size = 13.sp, color = c.fg.copy(alpha = 0.75f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                MangaTag("Ongoing")
                                MangaTag(manga.source)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionTile(
                            if (inLibrary) "In library" else "Add to library",
                            if (inLibrary) MdIcons.BookmarkFilled else MdIcons.Bookmark,
                            active = inLibrary,
                            onClick = { inLibrary = !inLibrary; onToggleLibrary() },
                            modifier = Modifier.weight(1f),
                        )
                        ActionTile("Track", MdIcons.Track, onClick = { showTracking = true }, modifier = Modifier.weight(1f))
                        ActionTile("WebView", MdIcons.Globe, onClick = onWebView, modifier = Modifier.weight(1f))
                    }
                    MdButton(resumeLabel, onResume, Modifier.fillMaxWidth(), height = 50.dp, leadingIcon = MdIcons.Play)
                    if (synopsis.isNotBlank()) {
                        BodyText(
                            synopsis,
                            modifier = Modifier.clickable { synopsisOpen = !synopsisOpen },
                            color = c.fgMuted,
                            lineHeight = 21.sp,
                            maxLines = if (synopsisOpen) Int.MAX_VALUE else 3,
                        )
                    }
                    if (genres.isNotEmpty()) {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            genres.forEach { g ->
                                BodyText(
                                    g,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .border(1.dp, c.border, CircleShape)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    size = 12.sp,
                                    weight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }

                Box(Modifier.width(1.dp).fillMaxHeight().background(c.dividerStrong.copy(alpha = 0.15f)))

                // Right pane: Chapters list
                Column(
                    Modifier
                        .weight(0.56f)
                        .fillMaxHeight()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    Row(
                        Modifier.padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val filterLabel = if (filterMode != "all") " · ${filterMode.replaceFirstChar { it.uppercaseChar() }}" else ""
                        val sortLabel = if (sortAscending) " (Asc)" else " (Desc)"
                        BodyText(
                            "${displayChapters.size} chapters$filterLabel$sortLabel",
                            Modifier.weight(1f),
                            size = 15.sp,
                            weight = FontWeight.ExtraBold
                        )
                        MdIconButton(
                            MdIcons.Sort,
                            "Sort ${if (sortAscending) "descending" else "ascending"}",
                            { sortAscending = !sortAscending },
                            iconSize = 20.dp
                        )
                        MdIconButton(
                            MdIcons.Filter,
                            "Filter chapters ($filterMode)",
                            {
                                filterMode = when (filterMode) {
                                    "all" -> "unread"
                                    "unread" -> "downloaded"
                                    else -> "all"
                                }
                            },
                            iconSize = 20.dp,
                            tint = if (filterMode != "all") c.accentLight else c.fg
                        )
                    }
                    Divider(color = c.dividerStrong)
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(displayChapters, key = { "${it.number}_${it.title}" }) { ch ->
                            ChapterRow(ch, { onOpenChapter(ch) }, { onDownloadChapter(ch) })
                        }
                    }
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().navigationBarsPadding()) {
            item {
                Column(Modifier.fillMaxWidth().background(manga.cover).statusBarsPadding().padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 20.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MdIconButton(MdIcons.Back, "Back", onBack)
                        Row {
                            MdIconButton(MdIcons.Download, "Download chapters", onDownloadAllChapters)
                            MdIconButton(MdIcons.Globe, "Open in browser", onWebView)
                        }
                    }
                    VSpace(12.dp)
                    Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CoverArt(
                            manga.cover.copy(red = manga.cover.red * 0.6f, green = manga.cover.green * 0.6f, blue = manga.cover.blue * 0.6f),
                            Modifier.size(112.dp, 168.dp).border(1.dp, c.border, RoundedCornerShape(10.dp)),
                            imageUrl = manga.coverUrl,
                        )
                        Column(Modifier.align(Alignment.Bottom), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DisplayText(manga.title, 30.sp, lineHeight = 32.sp)
                            if (authors.isNotEmpty()) {
                                BodyText(authors.joinToString(", "), size = 13.sp, color = c.fg.copy(alpha = 0.75f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                MangaTag("Ongoing")
                                MangaTag(manga.source)
                            }
                        }
                    }
                }
            }
            item {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionTile(
                            if (inLibrary) "In library" else "Add to library",
                            if (inLibrary) MdIcons.BookmarkFilled else MdIcons.Bookmark,
                            active = inLibrary,
                            onClick = { inLibrary = !inLibrary; onToggleLibrary() },
                            modifier = Modifier.weight(1f),
                        )
                        ActionTile("Track", MdIcons.Track, onClick = { showTracking = true }, modifier = Modifier.weight(1f))
                        ActionTile("WebView", MdIcons.Globe, onClick = onWebView, modifier = Modifier.weight(1f))
                    }
                    MdButton(resumeLabel, onResume, Modifier.fillMaxWidth(), height = 52.dp, leadingIcon = MdIcons.Play)
                    if (synopsis.isNotBlank()) {
                        BodyText(
                            synopsis,
                            modifier = Modifier.clickable { synopsisOpen = !synopsisOpen },
                            color = c.fgMuted,
                            lineHeight = 21.sp,
                            maxLines = if (synopsisOpen) Int.MAX_VALUE else 3,
                        )
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        genres.forEach { g ->
                            BodyText(
                                g,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .border(1.dp, c.border, CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                size = 12.sp,
                                weight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
            item {
                Column(Modifier.padding(top = 16.dp)) {
                    Divider(color = c.dividerStrong)
                    Row(
                        Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val filterLabel = if (filterMode != "all") " · ${filterMode.replaceFirstChar { it.uppercaseChar() }}" else ""
                        val sortLabel = if (sortAscending) " (Asc)" else " (Desc)"
                        BodyText(
                            "${displayChapters.size} chapters$filterLabel$sortLabel",
                            Modifier.weight(1f),
                            size = 15.sp,
                            weight = FontWeight.ExtraBold
                        )
                        MdIconButton(
                            MdIcons.Sort,
                            "Sort ${if (sortAscending) "descending" else "ascending"}",
                            { sortAscending = !sortAscending },
                            iconSize = 20.dp
                        )
                        MdIconButton(
                            MdIcons.Filter,
                            "Filter chapters ($filterMode)",
                            {
                                filterMode = when (filterMode) {
                                    "all" -> "unread"
                                    "unread" -> "downloaded"
                                    else -> "all"
                                }
                            },
                            iconSize = 20.dp,
                            tint = if (filterMode != "all") c.accentLight else c.fg
                        )
                    }
                }
            }
            items(displayChapters, key = { "${it.number}_${it.title}" }) { ch -> ChapterRow(ch, { onOpenChapter(ch) }, { onDownloadChapter(ch) }) }
        }
    }

        TrackingSheet(visible = showTracking, onDismiss = { showTracking = false }, mangaId = manga.id, mangaTitle = manga.title)
    }
}

@Composable
private fun MangaTag(text: String) {
    BodyText(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        size = 11.sp,
        weight = FontWeight.Bold,
    )
}

@Composable
private fun ActionTile(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, active: Boolean = false) {
    val c = MdTheme.colors
    Column(
        modifier
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) c.accentMuted else c.surfaceRaised)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { if (active) selected = true },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        val tint = if (active) c.accentLight else c.fg
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        BodyText(label, size = 12.sp, weight = if (active) FontWeight.Bold else FontWeight.SemiBold, color = tint)
    }
}

@Composable
private fun ChapterRow(ch: UiChapter, onClick: () -> Unit, onDownload: () -> Unit = {}) {
    val c = MdTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f).alpha(if (ch.read) 0.45f else 1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            BodyText("Ch. ${ch.number} · ${ch.title}", weight = FontWeight.SemiBold, maxLines = 1)
            BodyText(ch.meta, size = 12.sp, color = c.fgSubtle)
        }
        if (ch.downloaded) {
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Icon(MdIcons.Check, "Downloaded", tint = c.accentLight, modifier = Modifier.size(20.dp))
            }
        } else {
            MdIconButton(MdIcons.Download, "Download chapter", onDownload, tint = c.fg.copy(alpha = 0.7f), iconSize = 20.dp)
        }
    }
}

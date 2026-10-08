package com.mangadl.android.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.data.ui.Manga
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.CoverArt
import com.mangadl.android.ui.components.InLibraryTag
import com.mangadl.android.ui.components.MdIconButton
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.components.Screen
import com.mangadl.android.ui.components.SearchField
import com.mangadl.android.ui.components.UnderlineTabs
import com.mangadl.android.ui.components.rememberState
import com.mangadl.android.ui.theme.MdTheme

@Composable
fun GlobalSearchScreen(
    onBack: () -> Unit,
    onOpenManga: (Manga) -> Unit,
    initialQuery: String = "",
    searchResults: List<Pair<String, List<Manga>>> = emptyList(),
    onSearch: (String) -> Unit = {},
) {
    val c = MdTheme.colors
    var query by rememberState(initialQuery)
    var tab by rememberState(0)
    val filterTabs = listOf("All", "Manga", "Web Novels")
    val filteredResults = remember(searchResults, tab) {
        when (tab) {
            1 -> searchResults.mapNotNull { (source, items) ->
                val mangaOnly = items.filter { !com.mangadl.android.data.extensions.ExtensionManager.isNovelSource(it.source) }
                if (mangaOnly.isNotEmpty()) source to mangaOnly else null
            }
            2 -> searchResults.mapNotNull { (source, items) ->
                val novelsOnly = items.filter { com.mangadl.android.data.extensions.ExtensionManager.isNovelSource(it.source) }
                if (novelsOnly.isNotEmpty()) source to novelsOnly else null
            }
            else -> searchResults
        }
    }

    Screen {
        Row(Modifier.padding(start = 8.dp, end = 16.dp, top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MdIconButton(MdIcons.Back, "Back", onBack)
            SearchField(
                query,
                {
                    query = it
                    onSearch(it)
                },
                "Search",
                Modifier.weight(1f),
                focused = true,
            ) {
                if (query.isNotEmpty()) {
                    MdIconButton(MdIcons.Close, "Clear search", {
                        query = ""
                        onSearch("")
                    }, size = 36.dp, iconSize = 18.dp)
                }
            }
        }
        UnderlineTabs(filterTabs, tab, { tab = it }, Modifier.padding(horizontal = 20.dp))
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)) {
            if (searchResults.isEmpty() && query.isNotBlank()) {
                // skeleton while searching
                item {
                    SearchSection("Searching…", "") {
                        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            repeat(3) { Box(Modifier.size(104.dp, 156.dp).clip(RoundedCornerShape(10.dp)).background(c.surfaceRaised)) }
                        }
                    }
                }
            } else if (filteredResults.isEmpty() && searchResults.isNotEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        BodyText("No ${filterTabs[tab].lowercase()} found for \"$query\"", color = c.fgMuted)
                    }
                }
            } else {
                items(filteredResults) { (source, results) ->
                    SearchSection(source, "${results.size} results") {
                        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(results) { m -> SearchCover(m) { onOpenManga(m) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSection(title: String, meta: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(top = 12.dp, bottom = 8.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            BodyText(title, Modifier.weight(1f), size = 15.sp, weight = FontWeight.ExtraBold)
            if (meta.isNotEmpty()) BodyText(meta, size = 12.sp, weight = FontWeight.SemiBold, color = MdTheme.colors.fgSubtle)
        }
        content()
    }
}

@Composable
private fun SearchCover(m: Manga, onClick: () -> Unit) {
    Column(Modifier.width(104.dp).clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CoverArt(m.cover, Modifier.size(104.dp, 156.dp), imageUrl = m.coverUrl) {
            if (m.inLibrary) InLibraryTag(Modifier.padding(6.dp))
        }
        BodyText(m.title, size = 12.sp, weight = FontWeight.SemiBold, lineHeight = 16.sp, maxLines = 2)
    }
}

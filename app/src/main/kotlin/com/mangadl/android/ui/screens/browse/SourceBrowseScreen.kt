package com.mangadl.android.ui.screens.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.data.ui.Manga
import com.mangadl.android.ui.components.BackHeader
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.MdButton
import com.mangadl.android.ui.components.MdIconButton
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.components.PillChip
import com.mangadl.android.ui.components.Screen
import com.mangadl.android.ui.components.rememberState
import com.mangadl.android.ui.screens.library.CoverCell
import com.mangadl.android.ui.theme.MdTheme

@Composable
fun BrowseSourceScreen(
    sourceName: String,
    items: List<Manga>,
    loading: Boolean = false,
    error: String? = null,
    onBack: () -> Unit,
    onOpenManga: (Manga) -> Unit,
) {
    val c = MdTheme.colors
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    var tab by rememberState("Popular")
    var isSearching by rememberState(false)
    var searchQuery by rememberState("")

    val displayedItems = remember(items, searchQuery) {
        if (searchQuery.isBlank()) items
        else items.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    Box(Modifier.fillMaxSize()) {
        Screen {
            if (isSearching) {
                Row(
                    Modifier.padding(start = 8.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MdIconButton(MdIcons.Back, "Close search", {
                        isSearching = false
                        searchQuery = ""
                    })
                    com.mangadl.android.ui.components.SearchField(
                        searchQuery,
                        { searchQuery = it },
                        "Filter $sourceName…",
                        Modifier.weight(1f),
                        focused = true,
                    )
                }
            } else {
                BackHeader(sourceName, onBack) {
                    MdIconButton(MdIcons.Search, "Search this source", { isSearching = true })
                    MdIconButton(MdIcons.Globe, "Open in Browser", {
                        runCatching {
                            uriHandler.openUri("https://www.google.com/search?q=" + java.net.URLEncoder.encode(sourceName, "UTF-8"))
                        }
                    })
                }
            }
            Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillChip("Popular", tab == "Popular", { tab = "Popular" })
                PillChip("Latest", tab == "Latest", { tab = "Latest" })
            }
            when {
                loading && displayedItems.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = c.accentSoft, modifier = Modifier.size(36.dp))
                }
                error != null && displayedItems.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    BodyText(error, color = c.fgSubtle, size = 14.sp)
                }
                displayedItems.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    BodyText(if (isSearching) "No matches found" else "No results", color = c.fgSubtle, size = 14.sp)
                }
                else -> LazyVerticalGrid(
                    GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(displayedItems, key = { it.id }) { m ->
                        CoverCell(m, { onOpenManga(m) }, showInLibraryTag = true, dimInLibrary = true)
                    }
                }
            }
        }
    }
}

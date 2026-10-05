package com.mangadl.android.ui.screens.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.data.ui.Manga
import com.mangadl.android.ui.components.BackHeader
import com.mangadl.android.ui.components.MdButton
import com.mangadl.android.ui.components.MdIconButton
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.components.PillChip
import com.mangadl.android.ui.components.Screen
import com.mangadl.android.ui.components.rememberState
import com.mangadl.android.ui.screens.library.CoverCell

@Composable
fun BrowseSourceScreen(sourceName: String, items: List<Manga>, onBack: () -> Unit, onOpenManga: (Manga) -> Unit) {
    var tab by rememberState("Popular")
    Box(Modifier.fillMaxSize()) {
        Screen {
            BackHeader(sourceName, onBack) {
                MdIconButton(MdIcons.Search, "Search this source", {})
                MdIconButton(MdIcons.Globe, "Open in WebView", {})
            }
            Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillChip("Popular", tab == "Popular", { tab = "Popular" })
                PillChip("Latest", tab == "Latest", { tab = "Latest" })
            }
            LazyVerticalGrid(
                GridCells.Fixed(3),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(items, key = { it.id }) { m ->
                    CoverCell(m, { onOpenManga(m) }, showInLibraryTag = true, dimInLibrary = true)
                }
            }
        }
        MdButton(
            "Filters", {},
            Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp),
            height = 52.dp, shape = RoundedCornerShape(16.dp), fontSize = 14.sp, leadingIcon = MdIcons.Filter,
        )
    }
}

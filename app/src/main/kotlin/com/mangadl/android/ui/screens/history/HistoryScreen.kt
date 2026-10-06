package com.mangadl.android.ui.screens.history

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.data.ui.HistoryItem
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.CoverArt
import com.mangadl.android.ui.components.MdIconButton
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.components.PillChip
import com.mangadl.android.ui.components.SearchField
import com.mangadl.android.ui.components.TabHeader
import com.mangadl.android.ui.components.rememberState
import com.mangadl.android.ui.theme.MdTheme

@Composable
fun HistoryScreen(
    items: List<HistoryItem>,
    onResume: (HistoryItem) -> Unit,
    onClearHistory: () -> Unit = {},
) {
    val c = MdTheme.colors
    var range by rememberState("All")
    var searchOpen by rememberState(false)
    var searchQuery by rememberState("")
    var showClearDialog by rememberState(false)

    val filteredItems = remember(items, range, searchQuery) {
        items
            .filter { h ->
                if (searchQuery.isBlank()) true
                else h.manga.title.contains(searchQuery, ignoreCase = true)
            }
            .filter { h ->
                when (range) {
                    "Today" -> h.whenText.contains("ago", ignoreCase = true) && (h.whenText.contains("m ago") || h.whenText.contains("h ago") || h.whenText.contains("Just now"))
                    "This week" -> !h.whenText.contains("d ago") || h.whenText.filter { it.isDigit() }.toIntOrNull()?.let { it <= 7 } ?: true
                    "Month" -> !h.whenText.contains("d ago") || h.whenText.filter { it.isDigit() }.toIntOrNull()?.let { it <= 30 } ?: true
                    else -> true
                }
            }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { BodyText("Clear reading history?", weight = FontWeight.Bold, size = 18.sp) },
            text = { BodyText("This will remove all progress from your history timeline. Library items and downloaded files will remain intact.", color = c.fgMuted) },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    onClearHistory()
                }) {
                    BodyText("Clear", color = c.errorText, weight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    BodyText("Cancel", color = c.fg)
                }
            },
            containerColor = c.surfaceRaised,
        )
    }

    Column(Modifier.fillMaxSize()) {
        TabHeader("History") {
            MdIconButton(MdIcons.Search, "Search history", { searchOpen = !searchOpen })
            MdIconButton(MdIcons.Trash, "Clear history", { showClearDialog = true })
        }

        if (searchOpen) {
            SearchField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                placeholder = "Search history…",
            )
        }

        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("All", "Today", "This week", "Month").forEach { PillChip(it, it == range, { range = it }, fontSize = 13.sp) }
        }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(filteredItems) { h ->
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    CoverArt(h.manga.cover, Modifier.size(48.dp, 72.dp), RoundedCornerShape(6.dp), imageUrl = h.manga.coverUrl)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        BodyText(h.manga.title, size = 15.sp, weight = FontWeight.Bold)
                        BodyText(h.where, size = 13.sp, color = c.fg.copy(alpha = 0.65f))
                        BodyText(h.whenText, size = 12.sp, color = c.fg.copy(alpha = 0.55f))
                    }
                    MdIconButton(MdIcons.Play, "Resume ${h.manga.title}", { onResume(h) }, tint = c.accentLight, background = c.accentMuted, iconSize = 18.dp)
                }
            }
        }
    }
}

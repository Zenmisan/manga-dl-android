package com.mangadl.android.ui.screens.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.extensions.ExtensionMeta
import com.mangadl.android.data.model.MangaSearchResult
import com.mangadl.android.ui.components.PillButton
import com.mangadl.android.ui.components.SectionLabel
import com.mangadl.android.ui.theme.AntonStyle
import com.mangadl.android.ui.theme.MangaDlColors
import kotlinx.coroutines.launch

private enum class BrowseTab { Sources, Extensions, Migrate }

@Composable
fun BrowseScreen(
    onMangaClick: (provider: String, mangaId: String) -> Unit,
    onSourceClick: (provider: String) -> Unit = {},
) {
    var activeTab by remember { mutableStateOf(BrowseTab.Sources) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MangaDlColors.Background)
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
            Text(
                text = "Browse".uppercase(),
                style = AntonStyle,
                color = MangaDlColors.TextPrimary,
            )
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Color(0x1AFFFFFF),
                        RoundedCornerShape(1000.dp),
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.width(14.dp))
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MangaDlColors.TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Search all sources",
                    color = MangaDlColors.TextSecondary,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 14.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .border(width = 0.dp, color = Color.Transparent)
                    .padding(bottom = 0.dp),
            ) {
                BrowseTab.entries.forEach { tab ->
                    val selected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clickable { activeTab = tab }
                            .padding(end = 24.dp),
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        Text(
                            text = tab.name,
                            color = if (selected) MangaDlColors.TextPrimary else MangaDlColors.TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                        if (selected) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(MangaDlColors.Primary)
                                    .align(Alignment.BottomStart)
                            )
                        }
                    }
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0x1AFFFFFF))
            )
        }

        when (activeTab) {
            BrowseTab.Sources -> SourcesTab(onMangaClick = onMangaClick, onSourceClick = onSourceClick)
            BrowseTab.Extensions -> ExtensionsTab()
            BrowseTab.Migrate -> MigrateTab()
        }
    }
}

@Composable
private fun SourcesTab(
    onMangaClick: (String, String) -> Unit,
    onSourceClick: (String) -> Unit,
) {
    val sources = remember { MangaDlApp.instance.extensionManager.listExtensions() }
    val lastUsed = sources.firstOrNull()

    LazyColumn(Modifier.fillMaxSize()) {
        if (lastUsed != null) {
            item {
                SectionLabel(
                    text = "Last used",
                    color = MangaDlColors.SectionRed,
                    modifier = Modifier.padding(start = 20.dp, top = 10.dp, bottom = 6.dp),
                )
            }
            item { SourceRow(src = lastUsed, onLatestClick = { onSourceClick(lastUsed.id) }) }
        }

        item {
            SectionLabel(
                text = "All sources",
                color = MangaDlColors.SectionDim,
                modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 6.dp),
            )
        }

        items(sources, key = { it.id }) { src ->
            SourceRow(src = src, onLatestClick = { onSourceClick(src.id) })
        }
    }
}

@Composable
private fun SourceRow(src: ExtensionMeta, onLatestClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onLatestClick)
            .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF2D1716)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = src.name.first().uppercaseChar().toString(),
                color = MangaDlColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = src.name,
                color = MangaDlColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${src.lang.uppercase()} · v${src.version}",
                color = MangaDlColors.TextSecondary,
                fontSize = 12.sp,
            )
        }
        Box(
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(999.dp))
                .border(1.dp, Color(0x29FFFFFF), RoundedCornerShape(999.dp))
                .clickable(onClick = onLatestClick)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Latest",
                color = MangaDlColors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clickable {},
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.PushPin,
                contentDescription = "Pin",
                tint = MangaDlColors.TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ExtensionsTab() {
    val sources = remember { MangaDlApp.instance.extensionManager.listExtensions() }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
            ) {
                item { PillButton("English", active = true, onClick = {}) }
                item { PillButton("All languages", active = false, onClick = {}) }
                item { PillButton("Show 18+", active = false, onClick = {}) }
            }
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SectionLabel("Updates pending · ${sources.size}", color = MangaDlColors.SectionRed)
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MangaDlColors.Primary)
                        .clickable {}
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Update All", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        item {
            SectionLabel(
                "Installed",
                color = MangaDlColors.SectionDim,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp),
            )
        }

        items(sources, key = { it.id }) { src ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2D1716)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = src.name.first().uppercaseChar().toString(),
                        color = MangaDlColors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(src.name, color = MangaDlColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("${src.lang.uppercase()} · v${src.version}", color = MangaDlColors.TextSecondary, fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .border(1.dp, MangaDlColors.Primary, RoundedCornerShape(999.dp))
                        .clickable {}
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Update", color = MangaDlColors.SectionRed, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun MigrateTab() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Migrate coming soon", color = MangaDlColors.TextSecondary, fontSize = 14.sp)
    }
}

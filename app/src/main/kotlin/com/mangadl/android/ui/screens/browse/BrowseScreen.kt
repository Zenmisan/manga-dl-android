package com.mangadl.android.ui.screens.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.extensions.ExtensionMeta
import com.mangadl.android.ui.components.PillButton
import com.mangadl.android.ui.components.SectionLabel
import com.mangadl.android.ui.theme.AntonStyle
import com.mangadl.android.ui.theme.MangaDlColors
import com.mangadl.android.ui.theme.MangaDlTheme

private enum class BrowseTab(val label: String) {
    Sources("Sources"), Extensions("Extensions"), Migrate("Migrate")
}

// Deterministic icon background per source name
private fun sourceIconColor(name: String): Color {
    val palette = listOf(
        Color(0xFF2D1716), Color(0xFF1A2433), Color(0xFF1A2D1A),
        Color(0xFF2D2016), Color(0xFF1D1A2D), Color(0xFF2D1A2D),
    )
    return palette[name.hashCode().and(0x7FFFFFFF) % palette.size]
}

@Composable
fun BrowseScreen(
    onMangaClick: (provider: String, mangaId: String) -> Unit = { _, _ -> },
    onSourceClick: (provider: String) -> Unit = {},
    onSearchClick: () -> Unit = {},
) {
    var activeTab by remember { mutableStateOf(BrowseTab.Sources) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MangaDlColors.Background)
    ) {
        Column(
            Modifier
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp)
        ) {
            Text(
                text = "BROWSE",
                style = AntonStyle,
                color = MangaDlColors.TextPrimary,
            )
            Spacer(Modifier.height(16.dp))

            // Tappable search bar — navigates to global search
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF111111))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                    .clickable(onClick = onSearchClick)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MangaDlColors.TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Search all sources",
                    color = MangaDlColors.TextSecondary,
                    fontSize = 15.sp,
                )
            }

            Spacer(Modifier.height(18.dp))

            // Underline tabs
            Row(Modifier.fillMaxWidth()) {
                BrowseTab.entries.forEach { tab ->
                    val selected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clickable { activeTab = tab }
                            .padding(end = 28.dp),
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        Text(
                            text = tab.label,
                            color = if (selected) MangaDlColors.TextPrimary else MangaDlColors.TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp),
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
                    .background(Color(0x14FFFFFF))
            )
        }

        when (activeTab) {
            BrowseTab.Sources -> SourcesTab(onSourceClick = onSourceClick)
            BrowseTab.Extensions -> ExtensionsTab()
            BrowseTab.Migrate -> MigrateTab()
        }
    }
}

@Composable
private fun SourcesTab(onSourceClick: (String) -> Unit) {
    val sources = remember { MangaDlApp.instance.extensionManager.listExtensions() }
    val lastUsed = sources.firstOrNull()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        if (lastUsed != null) {
            item {
                SectionLabel(
                    text = "Last used",
                    color = MangaDlColors.SectionRed,
                    modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp),
                )
            }
            item { SourceRow(src = lastUsed, onLatestClick = { onSourceClick(lastUsed.id) }) }
        }

        item {
            SectionLabel(
                text = "All sources",
                color = MangaDlColors.TextSecondary,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp),
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
            .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(sourceIconColor(src.name)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = src.name.first().uppercaseChar().toString(),
                color = MangaDlColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = src.name,
                color = MangaDlColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = buildString {
                    append(src.lang.uppercase())
                    append(" · ")
                    append(if (src.version == "0.0.0" || src.version.isBlank()) "Built-in" else "v${src.version}")
                },
                color = MangaDlColors.TextSecondary,
                fontSize = 12.sp,
            )
        }

        // Latest button
        Box(
            modifier = Modifier
                .height(34.dp)
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

        // Pin icon
        Box(
            modifier = Modifier
                .size(40.dp)
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

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 14.dp, bottom = 10.dp),
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
                    .padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SectionLabel("Updates pending · ${sources.size}", color = MangaDlColors.SectionRed)
                Box(
                    modifier = Modifier
                        .height(34.dp)
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
                color = MangaDlColors.TextSecondary,
                modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp),
            )
        }

        items(sources, key = { it.id }) { src ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(sourceIconColor(src.name)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = src.name.first().uppercaseChar().toString(),
                        color = MangaDlColors.TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(src.name, color = MangaDlColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("${src.lang.uppercase()} · v${src.version}", color = MangaDlColors.TextSecondary, fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier
                        .height(34.dp)
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Migration",
                color = MangaDlColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Move manga between sources",
                color = MangaDlColors.TextSecondary,
                fontSize = 13.sp,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
private fun BrowseScreenPreview() {
    MangaDlTheme {
        BrowseScreen()
    }
}

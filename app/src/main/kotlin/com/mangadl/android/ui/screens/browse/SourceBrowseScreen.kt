package com.mangadl.android.ui.screens.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.model.MangaSearchResult
import com.mangadl.android.ui.theme.AntonStyleSub
import com.mangadl.android.ui.theme.MangaDlColors
import com.mangadl.android.ui.theme.MangaDlTheme
import kotlinx.coroutines.launch

@Composable
fun SourceBrowseScreen(
    provider: String,
    initialQuery: String = "",
    onMangaClick: (provider: String, mangaId: String) -> Unit,
    onBack: () -> Unit,
) {
    val extensionManager = MangaDlApp.instance.extensionManager
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val source = remember(provider) { extensionManager.getExtension(provider) }
    var query by remember { mutableStateOf(initialQuery) }
    var results by remember { mutableStateOf<List<MangaSearchResult>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun doSearch(q: String) {
        scope.launch {
            loading = true
            error = null
            try {
                results = extensionManager.search(provider, q.ifBlank { "" }, 1)
            } catch (e: Exception) {
                error = e.message ?: "Search failed"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(provider) { doSearch(initialQuery) }

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
                .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MangaDlColors.TextPrimary,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = (source?.name ?: provider).uppercase(),
                    style = AntonStyleSub,
                    color = MangaDlColors.TextPrimary,
                )
                if (source != null) {
                    Text(
                        text = buildString {
                            append(source.lang.uppercase())
                            append(" · ")
                            append(if (source.version.isBlank() || source.version == "0.0.0") "Built-in" else "v${source.version}")
                        },
                        color = MangaDlColors.TextSecondary,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF111111))
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MangaDlColors.TextSecondary,
                modifier = Modifier.size(18.dp),
            )
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    color = MangaDlColors.TextPrimary,
                    fontSize = 15.sp,
                ),
                cursorBrush = SolidColor(MangaDlColors.Primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    doSearch(query)
                }),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            "Search ${source?.name ?: provider}…",
                            color = MangaDlColors.TextSecondary,
                            fontSize = 15.sp,
                        )
                    }
                    inner()
                },
            )
        }

        Box(Modifier.weight(1f)) {
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = MangaDlColors.Primary,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp,
                    )
                }
                error != null -> SourceBrowseErrorState(
                    message = error ?: "Unknown error",
                    onRetry = { doSearch(query) },
                )
                results.isEmpty() -> SourceBrowseEmptyState(hasQuery = query.isNotBlank())
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(results, key = { it.id }) { manga ->
                        BrowseResultCard(
                            manga = manga,
                            onClick = { onMangaClick(provider, manga.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceBrowseErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(36.dp))
                    .background(Color(0x14FFFFFF)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(34.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Failed to load",
                color = MangaDlColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                message,
                color = MangaDlColors.TextSecondary,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MangaDlColors.Primary)
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Retry", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun SourceBrowseEmptyState(hasQuery: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(36.dp))
                    .background(Color(0x14FFFFFF)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.SearchOff,
                    contentDescription = null,
                    tint = MangaDlColors.TextSecondary,
                    modifier = Modifier.size(34.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                if (hasQuery) "No results found" else "Search to browse",
                color = MangaDlColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (hasQuery) "Try a different search term" else "Enter a title to start searching",
                color = MangaDlColors.TextSecondary,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun BrowseResultCard(manga: MangaSearchResult, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(MangaDlColors.CoverPlaceholder),
        ) {
            AsyncImage(
                model = manga.coverUrl,
                contentDescription = manga.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Bottom gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.35f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xBB000000)),
                        )
                    )
            )
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
private fun SourceBrowseScreenPreview() {
    MangaDlTheme {
        Column(
            Modifier
                .fillMaxSize()
                .background(MangaDlColors.Background)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 12.dp, top = 24.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("←", color = MangaDlColors.TextPrimary, modifier = Modifier.padding(16.dp))
                Column {
                    Text("MANGADEX", style = AntonStyleSub, color = MangaDlColors.TextPrimary)
                    Text("EN · Built-in", color = MangaDlColors.TextSecondary, fontSize = 12.sp)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF111111))
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = MangaDlColors.TextSecondary, modifier = Modifier.size(18.dp))
                Text("Search MangaDex…", color = MangaDlColors.TextSecondary, fontSize = 15.sp)
            }
            SourceBrowseEmptyState(hasQuery = false)
        }
    }
}

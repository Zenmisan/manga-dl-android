package com.mangadl.android.ui.screens.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.model.MangaSearchResult
import com.mangadl.android.ui.theme.AntonStyle
import com.mangadl.android.ui.theme.AntonStyleSub
import com.mangadl.android.ui.theme.MangaDlColors
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
    var page by remember { mutableIntStateOf(1) }

    fun doSearch(q: String, pg: Int = 1) {
        scope.launch {
            loading = true
            error = null
            try {
                results = extensionManager.search(provider, q.ifBlank { "" }, pg)
            } catch (e: Exception) {
                error = e.message ?: "Search failed"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(provider) {
        doSearch(initialQuery)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MangaDlColors.Background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
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
                        text = "${source.lang.uppercase()} · v${source.version}",
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
                .background(MangaDlColors.CardBg)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MangaDlColors.TextSecondary,
                modifier = Modifier.size(20.dp),
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
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

        // Content
        Box(Modifier.weight(1f)) {
            when {
                loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MangaDlColors.Primary,
                )
                error != null -> Column(
                    Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Failed to load", color = MangaDlColors.TextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(error ?: "", color = MangaDlColors.TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { doSearch(query) },
                        colors = ButtonDefaults.buttonColors(containerColor = MangaDlColors.Primary),
                    ) { Text("Retry") }
                }
                results.isEmpty() && !loading -> Column(
                    Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("No results", color = MangaDlColors.TextPrimary, fontWeight = FontWeight.Bold)
                    Text("Try a different search term", color = MangaDlColors.TextSecondary, fontSize = 13.sp)
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(results, key = { it.id }) { manga ->
                        SearchResultCard(
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
private fun SearchResultCard(manga: MangaSearchResult, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = manga.coverUrl,
            contentDescription = manga.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(MangaDlColors.CardBg),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = manga.title,
            color = MangaDlColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

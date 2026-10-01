package com.mangadl.android.ui.screens.reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mangadl.android.MangaDlApp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    provider: String,
    mangaId: String,
    chapterId: String,
    onBack: () -> Unit,
) {
    val extensionManager = MangaDlApp.instance.extensionManager
    val db = MangaDlApp.instance.database
    val scope = rememberCoroutineScope()

    var pages by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showUi by remember { mutableStateOf(true) }

    LaunchedEffect(provider, chapterId) {
        loading = true
        try {
            pages = extensionManager.getPages(provider, chapterId)
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when {
            loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary,
            )
            error != null -> Column(
                modifier = Modifier.align(Alignment.Center).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Failed to load chapter", color = Color.White)
                Text(error ?: "", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onBack) { Text("Go back") }
            }
            pages.isNotEmpty() -> {
                val pagerState = rememberPagerState(pageCount = { pages.size })

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { showUi = !showUi },
                ) { pageIndex ->
                    AsyncImage(
                        model = pages[pageIndex],
                        contentDescription = "Page ${pageIndex + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                if (showUi) {
                    ReaderTopBar(
                        page = pagerState.currentPage + 1,
                        total = pages.size,
                        onBack = onBack,
                    )
                    ReaderPageIndicator(
                        page = pagerState.currentPage + 1,
                        total = pages.size,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.ReaderTopBar(page: Int, total: Int, onBack: () -> Unit) {
    TopAppBar(
        title = { Text("$page / $total", style = MaterialTheme.typography.titleMedium) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Black.copy(alpha = 0.6f),
            titleContentColor = Color.White,
        ),
        modifier = Modifier.align(Alignment.TopCenter),
    )
}

@Composable
private fun ReaderPageIndicator(page: Int, total: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.4f))
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        LinearProgressIndicator(
            progress = { if (total > 0) page.toFloat() / total else 0f },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

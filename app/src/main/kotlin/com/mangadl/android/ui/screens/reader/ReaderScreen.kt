package com.mangadl.android.ui.screens.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.model.ReadingProgress
import com.mangadl.android.ui.theme.MangaDlColors
import com.mangadl.android.ui.theme.MangaDlTheme
import kotlinx.coroutines.launch

private val ReaderBarBg = Color(0xEB080808)

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
    var bookmarked by remember { mutableStateOf(false) }

    val compositeId = "$provider:$mangaId"

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
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when {
            loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MangaDlColors.Primary,
            )
            error != null -> Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Failed to load chapter", color = Color.White, fontWeight = FontWeight.Bold)
                Text(error ?: "Unknown error", color = Color(0x80FFFFFF), fontSize = 13.sp)
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MangaDlColors.Primary)
                        .clickable { onBack() }
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Go back", color = Color.White, fontWeight = FontWeight.ExtraBold)
                }
            }
            pages.isNotEmpty() -> {
                val pagerState = rememberPagerState(pageCount = { pages.size })
                val currentPage = pagerState.currentPage

                // Zoom state — reset on page change
                var scale by remember { mutableFloatStateOf(1f) }
                var offsetX by remember { mutableFloatStateOf(0f) }
                var offsetY by remember { mutableFloatStateOf(0f) }
                val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
                    scale = (scale * zoomChange).coerceIn(1f, 5f)
                    val maxX = (scale - 1f) * 300f
                    val maxY = (scale - 1f) * 400f
                    offsetX = (offsetX + panChange.x).coerceIn(-maxX, maxX)
                    offsetY = (offsetY + panChange.y).coerceIn(-maxY, maxY)
                }

                LaunchedEffect(pagerState.currentPage) {
                    scale = 1f
                    offsetX = 0f
                    offsetY = 0f
                }

                // Save progress on page change
                LaunchedEffect(currentPage, pages.size) {
                    if (pages.isNotEmpty()) {
                        val completed = currentPage >= pages.size - 1
                        scope.launch {
                            db.progressDao().upsert(
                                ReadingProgress(
                                    mangaId = compositeId,
                                    chapterId = chapterId,
                                    provider = provider,
                                    page = currentPage + 1,
                                    totalPages = pages.size,
                                    readAt = System.currentTimeMillis(),
                                    completed = completed,
                                )
                            )
                            db.libraryDao().updateLastRead(compositeId, chapterId, System.currentTimeMillis())
                        }
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = scale <= 1f,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { showUi = !showUi },
                ) { pageIndex ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .transformable(state = transformableState, lockRotationOnZoomPan = true)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY,
                            ),
                    ) {
                        AsyncImage(
                            model = pages[pageIndex],
                            contentDescription = "Page ${pageIndex + 1}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                // ── Top bar ───────────────────────────────────────────────
                AnimatedVisibility(
                    visible = showUi,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ReaderBarBg)
                            .statusBarsPadding()
                            .height(56.dp)
                            .padding(end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                mangaId,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                            )
                            Text(
                                "Ch. ${currentPage + 1} of ${pages.size}",
                                color = Color(0x80FFFFFF),
                                fontSize = 11.sp,
                            )
                        }
                        IconButton(onClick = { bookmarked = !bookmarked }) {
                            Icon(
                                if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (bookmarked) MangaDlColors.Primary else Color.White,
                            )
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White)
                        }
                    }
                }

                // ── Bottom pill ───────────────────────────────────────────
                AnimatedVisibility(
                    visible = showUi,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(32.dp))
                                .background(ReaderBarBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = {}) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Prev Chapter", tint = Color.White)
                            }
                            Text(
                                "${currentPage + 1}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Slider(
                                value = (currentPage + 1).toFloat(),
                                onValueChange = { v ->
                                    scope.launch { pagerState.scrollToPage((v.toInt() - 1).coerceIn(0, pages.size - 1)) }
                                },
                                valueRange = 1f..pages.size.toFloat(),
                                steps = (pages.size - 2).coerceAtLeast(0),
                                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = MangaDlColors.Primary,
                                    activeTrackColor = MangaDlColors.Primary,
                                    inactiveTrackColor = Color(0x33FFFFFF),
                                ),
                            )
                            Text(
                                "${pages.size}",
                                color = Color(0x80FFFFFF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            IconButton(onClick = {}) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next Chapter", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ReaderScreenPreview() {
    MangaDlTheme {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            // Simulated page
            Box(Modifier.fillMaxSize().background(Color(0xFF1A1A2E)), contentAlignment = Alignment.Center) {
                Text("[Manga Page]", color = Color(0x40FFFFFF), fontSize = 18.sp)
            }
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(ReaderBarBg)
                    .padding(top = 24.dp)
                    .height(56.dp)
                    .padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text("hollow-crown", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("Ch. 14 of 38", color = Color(0x80FFFFFF), fontSize = 11.sp)
                }
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = Color.White)
                }
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White)
                }
            }
            // Bottom pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 32.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(ReaderBarBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = Color.White)
                    }
                    Text("14", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0x33FFFFFF))
                    ) {
                        Box(Modifier.fillMaxWidth(0.37f).fillMaxHeight().background(MangaDlColors.Primary))
                    }
                    Text("38", color = Color(0x80FFFFFF), fontSize = 13.sp)
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }
    }
}

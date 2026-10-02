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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.model.ReadingProgress
import com.mangadl.android.ui.theme.MangaDlColors
import kotlinx.coroutines.launch

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
                val progress = if (pages.isNotEmpty()) (currentPage + 1).toFloat() / pages.size else 0f

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

                // Save progress whenever the page changes
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

                AnimatedVisibility(
                    visible = showUi,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xEB080808))
                            .statusBarsPadding()
                            .height(56.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            Text(
                                "${currentPage + 1} / ${pages.size}",
                                color = Color(0xB3FFFFFF),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = {}) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White)
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showUi,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xEB080808))
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        // Page number + progress
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                "${currentPage + 1}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.width(32.dp),
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0x33FFFFFF)),
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(progress)
                                        .fillMaxHeight()
                                        .background(MangaDlColors.Primary)
                                )
                            }
                            Text(
                                "${pages.size}",
                                color = Color(0x80FFFFFF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(32.dp),
                            )
                        }

                        // Reading toolbar row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ReaderToolButton(icon = Icons.Default.SkipPrevious, label = "Prev Ch", onClick = {})
                            ReaderToolButton(icon = Icons.Default.FormatListNumbered, label = "Chapters", onClick = {})
                            ReaderToolButton(icon = Icons.Default.Settings, label = "Settings", onClick = {})
                            ReaderToolButton(icon = Icons.Default.SkipNext, label = "Next Ch", onClick = {})
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(icon, contentDescription = label, tint = Color(0xBFFFFFFF), modifier = Modifier.size(22.dp))
        Text(label, color = Color(0x80FFFFFF), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

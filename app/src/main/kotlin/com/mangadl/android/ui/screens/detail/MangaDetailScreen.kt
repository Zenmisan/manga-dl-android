package com.mangadl.android.ui.screens.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.model.Chapter
import com.mangadl.android.data.model.DownloadEntry
import com.mangadl.android.data.model.LibraryManga
import com.mangadl.android.data.model.MangaDetail
import com.mangadl.android.data.model.ReadingProgress
import com.mangadl.android.ui.theme.AntonStyle
import com.mangadl.android.ui.theme.MangaDlColors
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MangaDetailScreen(
    provider: String,
    mangaId: String,
    onReadChapter: (provider: String, mangaId: String, chapterId: String) -> Unit,
    onBack: () -> Unit,
) {
    val extensionManager = MangaDlApp.instance.extensionManager
    val db = MangaDlApp.instance.database
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    var detail by remember { mutableStateOf<MangaDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var descExpanded by remember { mutableStateOf(false) }
    var contextChapter by remember { mutableStateOf<Chapter?>(null) }

    val inLibrary by db.libraryDao().isInLibrary("$provider:$mangaId")
        .catch { emit(false) }
        .collectAsState(initial = false)

    LaunchedEffect(provider, mangaId) {
        loading = true
        try {
            detail = extensionManager.getMangaDetail(provider, mangaId)
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MangaDlColors.Background)
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
                Text("Failed to load", color = MangaDlColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(error ?: "Unknown error", color = MangaDlColors.TextSecondary, fontSize = 13.sp)
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
            detail != null -> {
                val d = detail!!
                val lastChapter = d.chapters.firstOrNull()

                LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        // Header bg
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        ) {
                            AsyncImage(
                                model = d.coverUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                MangaDlColors.DetailHeaderBg.copy(alpha = 0.7f),
                                                MangaDlColors.Background,
                                            )
                                        )
                                    )
                            )
                            // Back button
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(top = 16.dp, start = 4.dp),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MangaDlColors.TextPrimary)
                            }

                            // Cover + info
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(horizontal = 20.dp, vertical = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                AsyncImage(
                                    model = d.coverUrl,
                                    contentDescription = d.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .width(100.dp)
                                        .height(145.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                )
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(bottom = 4.dp),
                                ) {
                                    Text(
                                        d.title,
                                        style = AntonStyle,
                                        color = MangaDlColors.TextPrimary,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (d.authors.isNotEmpty()) {
                                        Text(
                                            d.authors.joinToString(", "),
                                            color = MangaDlColors.TextSecondary,
                                            fontSize = 13.sp,
                                        )
                                    }
                                    if (d.status.isNotBlank()) {
                                        Text(
                                            d.status,
                                            color = MangaDlColors.SectionRed,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        // Action buttons grid
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            ActionButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.BookmarkAdd,
                                label = if (inLibrary) "In Library" else "Add",
                                active = inLibrary,
                                onClick = {
                                    scope.launch {
                                        if (inLibrary) {
                                            db.libraryDao().delete("$provider:$mangaId")
                                        } else {
                                            db.libraryDao().upsert(
                                                LibraryManga(
                                                    id = "$provider:$mangaId",
                                                    title = d.title,
                                                    coverUrl = d.coverUrl,
                                                    provider = provider,
                                                    url = d.url,
                                                    totalChapters = d.chapters.size,
                                                )
                                            )
                                        }
                                    }
                                },
                            )
                            ActionButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Sync,
                                label = "Track",
                                active = false,
                                onClick = {},
                            )
                            ActionButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.OpenInBrowser,
                                label = "WebView",
                                active = false,
                                onClick = {},
                            )
                        }
                    }

                    item {
                        // Resume button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MangaDlColors.Primary)
                                .clickable {
                                    if (lastChapter != null) {
                                        onReadChapter(provider, mangaId, lastChapter.id)
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Text(
                                    "Resume Reading",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    if (d.description.isNotBlank()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .clickable { descExpanded = !descExpanded },
                            ) {
                                Text(
                                    text = d.description,
                                    color = MangaDlColors.TextSubtle,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    maxLines = if (descExpanded) Int.MAX_VALUE else 4,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = if (descExpanded) "Show less" else "Show more",
                                    color = MangaDlColors.SectionRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    if (d.genres.isNotEmpty()) {
                        item {
                            androidx.compose.foundation.lazy.LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                items(d.genres) { genre ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0x14FFFFFF))
                                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(20.dp))
                                            .padding(horizontal = 12.dp, vertical = 5.dp),
                                    ) {
                                        Text(genre, color = MangaDlColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "${d.chapters.size} Chapters",
                                color = MangaDlColors.TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(Icons.Default.FilterList, contentDescription = null, tint = MangaDlColors.TextSecondary, modifier = Modifier.size(20.dp))
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
                    }

                    items(d.chapters, key = { it.id }) { chapter ->
                        ChapterRow(
                            chapter = chapter,
                            onClick = { onReadChapter(provider, mangaId, chapter.id) },
                            onLongClick = { contextChapter = chapter },
                        )
                    }

                    item { Spacer(Modifier.height(32.dp)) }
                }

                // Chapter long-press action sheet
                if (contextChapter != null) {
                    val chapter = contextChapter!!
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x80000000))
                            .clickable { contextChapter = null },
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                .background(Color(0xFF141414))
                                .padding(bottom = 24.dp)
                                .clickable(enabled = false) {}
                        ) {
                            Box(
                                Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(top = 12.dp, bottom = 16.dp)
                                    .size(36.dp, 4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0x33FFFFFF))
                            )
                            Text(
                                chapter.title.ifBlank { "Chapter ${chapter.number}" },
                                color = MangaDlColors.TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            DetailActionSheetRow(
                                icon = Icons.Default.Visibility,
                                label = "Mark as Read",
                                tint = Color(0xFF22c55e),
                            ) {
                                scope.launch {
                                    db.progressDao().upsert(
                                        ReadingProgress(
                                            mangaId = "$provider:$mangaId",
                                            chapterId = chapter.id,
                                            provider = provider,
                                            page = 1,
                                            totalPages = 1,
                                            readAt = System.currentTimeMillis(),
                                            completed = true,
                                        )
                                    )
                                }
                                contextChapter = null
                            }
                            DetailActionSheetRow(
                                icon = Icons.Default.CheckCircle,
                                label = "Mark Previous Read",
                                tint = Color(0xFF3b82f6),
                            ) {
                                scope.launch {
                                    val chaptersToMark = d.chapters.filter { it.number <= chapter.number }
                                    val now = System.currentTimeMillis()
                                    chaptersToMark.forEach { ch ->
                                        db.progressDao().upsert(
                                            ReadingProgress(
                                                mangaId = "$provider:$mangaId",
                                                chapterId = ch.id,
                                                provider = provider,
                                                page = 1,
                                                totalPages = 1,
                                                readAt = now,
                                                completed = true,
                                            )
                                        )
                                    }
                                }
                                contextChapter = null
                            }
                            DetailActionSheetRow(
                                icon = Icons.Default.Download,
                                label = "Download Chapter",
                                tint = MangaDlColors.Primary,
                            ) {
                                scope.launch {
                                    db.downloadDao().upsert(
                                        DownloadEntry(
                                            id = "$provider:${chapter.id}",
                                            mangaId = "$provider:$mangaId",
                                            mangaTitle = d.title,
                                            chapterId = chapter.id,
                                            chapterTitle = chapter.title.ifBlank { "Chapter ${chapter.number}" },
                                            provider = provider,
                                        )
                                    )
                                }
                                contextChapter = null
                            }
                            DetailActionSheetRow(
                                icon = Icons.Default.Bookmark,
                                label = "Bookmark",
                                tint = Color(0xFFf59e0b),
                            ) {
                                scope.launch { snackbarHostState.showSnackbar("Bookmarks coming soon") }
                                contextChapter = null
                            }
                            DetailActionSheetRow(
                                icon = Icons.Default.ContentCopy,
                                label = "Copy Link",
                                tint = Color(0xFF9ca3af),
                            ) {
                                clipboardManager.setText(AnnotatedString("https://mangadl.app/manga/$provider/$mangaId/${chapter.id}"))
                                contextChapter = null
                            }
                            DetailActionSheetRow(
                                icon = Icons.Default.VisibilityOff,
                                label = "Hide Chapter",
                                tint = Color(0xFFef4444),
                            ) {
                                scope.launch { snackbarHostState.showSnackbar("Chapter hiding coming soon") }
                                contextChapter = null
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun DetailActionSheetRow(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, color = tint, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ActionButton(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) Color(0x26EF4444) else Color(0x0DFFFFFF))
            .border(1.dp, if (active) Color(0x33EF4444) else Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (active) MangaDlColors.SectionRed else MangaDlColors.TextSecondary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            label,
            color = if (active) MangaDlColors.SectionRed else MangaDlColors.TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChapterRow(chapter: Chapter, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = chapter.title.ifBlank { "Chapter ${chapter.number}" },
                color = MangaDlColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (chapter.publishedAt.isNotBlank()) {
                Text(chapter.publishedAt, color = MangaDlColors.TextSecondary, fontSize = 12.sp)
            }
        }
        Icon(Icons.Default.Download, contentDescription = null, tint = Color(0x60FFFFFF), modifier = Modifier.size(18.dp))
    }
    Box(Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 20.dp).background(Color(0x0FFFFFFF)))
}

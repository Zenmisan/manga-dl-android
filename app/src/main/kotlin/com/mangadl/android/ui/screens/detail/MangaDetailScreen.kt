package com.mangadl.android.ui.screens.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.model.Chapter
import com.mangadl.android.data.model.LibraryManga
import com.mangadl.android.data.model.MangaDetail
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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

    var detail by remember { mutableStateOf<MangaDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.title ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (detail != null) {
                        IconButton(onClick = {
                            scope.launch {
                                val d = detail ?: return@launch
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
                        }) {
                            Icon(
                                if (inLibrary) Icons.Default.BookmarkRemove else Icons.Default.BookmarkAdd,
                                contentDescription = if (inLibrary) "Remove from library" else "Add to library",
                            )
                        }
                    }
                },
            )
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                error != null -> Text(
                    "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
                detail != null -> DetailContent(
                    detail = detail!!,
                    onReadChapter = { chapter ->
                        onReadChapter(provider, mangaId, chapter.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun DetailContent(detail: MangaDetail, onReadChapter: (Chapter) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AsyncImage(
                    model = detail.coverUrl,
                    contentDescription = detail.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(120.dp)
                        .height(170.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(detail.title, style = MaterialTheme.typography.titleLarge)
                    if (detail.authors.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            detail.authors.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (detail.status.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        SuggestionChip(onClick = {}, label = { Text(detail.status) })
                    }
                }
            }
        }

        if (detail.description.isNotBlank()) {
            item {
                Text(
                    text = detail.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp),
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        item {
            Text(
                text = "${detail.chapters.size} Chapters",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        items(detail.chapters, key = { it.id }) { chapter ->
            ChapterRow(chapter = chapter, onClick = { onReadChapter(chapter) })
        }
    }
}

@Composable
private fun ChapterRow(chapter: Chapter, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.title.ifBlank { "Chapter ${chapter.number}" },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (chapter.publishedAt.isNotBlank()) {
                    Text(
                        text = chapter.publishedAt,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
}

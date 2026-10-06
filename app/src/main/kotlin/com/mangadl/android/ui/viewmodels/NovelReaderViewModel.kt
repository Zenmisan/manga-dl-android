package com.mangadl.android.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.db.AppDatabase
import com.mangadl.android.data.model.ReadingProgress
import com.mangadl.android.data.prefs.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jsoup.Jsoup

sealed interface NovelReaderState {
    data object Loading : NovelReaderState
    data class Success(
        val paragraphs: List<String>,
        val chapterTitle: String,
        val chapterId: String,
    ) : NovelReaderState
    data class Error(val message: String) : NovelReaderState
}

class NovelReaderViewModel(app: Application) : AndroidViewModel(app) {
    private val extMgr = MangaDlApp.instance.extensionManager
    private val db = AppDatabase.getInstance(app)
    val prefs = AppPreferences.getInstance(app)

    private val _state = MutableStateFlow<NovelReaderState>(NovelReaderState.Loading)
    val state: StateFlow<NovelReaderState> = _state

    private var currentMangaId: String = ""
    private var currentChapterId: String = ""
    private var currentProvider: String = ""

    fun loadChapter(provider: String, novelId: String, chapterId: String, chapterTitle: String) {
        currentMangaId = novelId
        currentChapterId = chapterId
        currentProvider = provider

        viewModelScope.launch(Dispatchers.IO) {
            _state.value = NovelReaderState.Loading
            runCatching {
                val raw = extMgr.getChapterText(provider, chapterId)
                parseContent(raw)
            }.onSuccess { paragraphs ->
                if (paragraphs.isEmpty()) {
                    _state.value = NovelReaderState.Error("No chapter content found")
                } else {
                    _state.value = NovelReaderState.Success(
                        paragraphs = paragraphs,
                        chapterTitle = chapterTitle,
                        chapterId = chapterId,
                    )
                }
            }.onFailure { err ->
                _state.value = NovelReaderState.Error(err.message ?: "Failed to load chapter")
            }
        }
    }

    fun saveProgress(scrollPercent: Float, chapterNumber: Float = 0f) {
        if (currentMangaId.isBlank() || currentChapterId.isBlank()) return
        val pct = (scrollPercent * 100).toInt().coerceIn(0, 100)
        val isCompleted = pct >= 95
        viewModelScope.launch(Dispatchers.IO) {
            db.progressDao().upsert(
                ReadingProgress(
                    mangaId = currentMangaId,
                    chapterId = currentChapterId,
                    provider = currentProvider,
                    page = pct,
                    totalPages = 100,
                    readAt = System.currentTimeMillis(),
                    completed = isCompleted,
                )
            )
            db.libraryDao().updateLastRead(currentMangaId, currentChapterId, System.currentTimeMillis())
            if (isCompleted) {
                val effectiveNum = if (chapterNumber > 0f) chapterNumber else {
                    Regex("""(?:chapter[_-]?|ch[_-]?|#)?([0-9]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
                        .find(currentChapterId)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
                }
                if (effectiveNum > 0f) {
                    com.mangadl.android.data.tracking.TrackerService.syncChapterRead(
                        context = MangaDlApp.instance,
                        mangaId = currentMangaId,
                        chapterNumber = effectiveNum,
                        isCompleted = true,
                    )
                }
            }
        }
    }

    fun parseContent(rawContent: String): List<String> {
        if (rawContent.isBlank()) return emptyList()

        return try {
            val doc = Jsoup.parse(rawContent)
            doc.select("script, style, iframe, .ad, .advertisement, [id*=ad-], noscript").remove()
            val container = doc.selectFirst("div.chapter-content, div.text-left, div.entry-content, article") ?: doc.body()
            val elements = container.select("p, h1, h2, h3, h4, h5, h6")
            val pList = elements.map { it.text().trim() }.filter { it.isNotBlank() }
            if (pList.isNotEmpty()) {
                pList
            } else {
                val bodyText = container.wholeText()
                bodyText.split("\n\n", "\n")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
            }
        } catch (_: Exception) {
            rawContent.split("\n\n", "\n")
                .map { it.trim() }
                .filter { it.isNotBlank() }
        }
    }
}

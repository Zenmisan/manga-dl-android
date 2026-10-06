package com.mangadl.android.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mangadl.android.MangaDlApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ReaderState {
    object Loading : ReaderState()
    data class Success(val pages: List<String>) : ReaderState()
    data class Error(val message: String) : ReaderState()
}

class ReaderViewModel(app: Application) : AndroidViewModel(app) {

    private val extMgr = MangaDlApp.instance.extensionManager
    private val db = MangaDlApp.instance.database
    private val progressDao = db.progressDao()
    private val libraryDao = db.libraryDao()

    private val _state = MutableStateFlow<ReaderState>(ReaderState.Loading)
    val state: StateFlow<ReaderState> = _state

    private var loadedChapterId = ""

    fun load(sourceId: String, chapterId: String) {
        if (chapterId == loadedChapterId && _state.value is ReaderState.Success) return
        loadedChapterId = chapterId
        viewModelScope.launch {
            _state.value = ReaderState.Loading
            runCatching {
                val rawPages = extMgr.getPages(sourceId, chapterId)
                val backendBase = com.mangadl.android.BuildConfig.BACKEND_URL.trimEnd('/')
                rawPages.map { url ->
                    when {
                        url.startsWith("http://") || url.startsWith("https://") -> url
                        url.startsWith("/api/") -> "$backendBase$url"
                        url.startsWith("/") -> "$backendBase/api$url"
                        else -> url
                    }
                }
            }.onSuccess { pages ->
                _state.value = if (pages.isEmpty()) ReaderState.Error("No pages found")
                else ReaderState.Success(pages)
            }.onFailure { e ->
                _state.value = ReaderState.Error(e.message ?: "Failed to load pages")
            }
        }
    }

    fun loadLocal(pages: List<String>) {
        loadedChapterId = "local"
        _state.value = ReaderState.Success(pages)
    }

    fun saveProgress(
        mangaId: String,
        chapterId: String,
        provider: String,
        page: Int,
        total: Int,
        chapterNumber: Float = 0f,
    ) {
        viewModelScope.launch {
            val isCompleted = page >= total - 1
            progressDao.upsert(
                com.mangadl.android.data.model.ReadingProgress(
                    mangaId = mangaId,
                    chapterId = chapterId,
                    provider = provider,
                    page = page,
                    totalPages = total,
                    completed = isCompleted,
                )
            )
            if (mangaId.isNotEmpty()) {
                libraryDao.updateLastRead(mangaId, chapterId, System.currentTimeMillis())
            }
            if (isCompleted && mangaId.isNotEmpty()) {
                val effectiveNum = if (chapterNumber > 0f) chapterNumber else {
                    Regex("""(?:chapter[_-]?|ch[_-]?|#)?([0-9]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
                        .find(chapterId)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
                }
                if (effectiveNum > 0f) {
                    com.mangadl.android.data.tracking.TrackerService.syncChapterRead(
                        context = getApplication(),
                        mangaId = mangaId,
                        chapterNumber = effectiveNum,
                        isCompleted = true,
                    )
                }
            }
        }
    }
}

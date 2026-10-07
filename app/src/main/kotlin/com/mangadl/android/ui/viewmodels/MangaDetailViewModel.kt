package com.mangadl.android.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.model.MangaDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class DetailState {
    object Loading : DetailState()
    data class Success(val detail: MangaDetail) : DetailState()
    data class Error(val message: String) : DetailState()
}

class MangaDetailViewModel(app: Application) : AndroidViewModel(app) {

    private val extMgr = MangaDlApp.instance.extensionManager
    private val db = MangaDlApp.instance.database
    private val dao = db.libraryDao()

    private val _state = MutableStateFlow<DetailState>(DetailState.Loading)
    val state: StateFlow<DetailState> = _state

    private val _inLibrary = MutableStateFlow(false)
    val inLibrary: StateFlow<Boolean> = _inLibrary

    private var currentSourceId = ""
    private var currentMangaId = ""

    fun load(sourceId: String, mangaId: String) {
        if (sourceId == currentSourceId && mangaId == currentMangaId && _state.value is DetailState.Success) return
        currentSourceId = sourceId
        currentMangaId = mangaId
        viewModelScope.launch {
            _state.value = DetailState.Loading
            _inLibrary.value = dao.getById(mangaId) != null
            runCatching {
                extMgr.getMangaDetail(sourceId, mangaId)
            }.onSuccess { detail ->
                _state.value = DetailState.Success(detail)
            }.onFailure { e ->
                _state.value = DetailState.Error(e.message ?: "Failed to load")
            }
        }
    }

    fun toggleLibrary(detail: MangaDetail) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = dao.getById(detail.id)
            if (existing != null) {
                dao.delete(existing.id)
                _inLibrary.value = false
                com.mangadl.android.data.sync.supabase.SupabaseSyncManager.syncMangaSubscriptionAsync(existing, subscribed = false)
            } else {
                val newManga = com.mangadl.android.data.model.LibraryManga(
                    id = detail.id,
                    title = detail.title,
                    coverUrl = detail.coverUrl,
                    provider = detail.provider,
                    url = detail.url,
                    totalChapters = detail.chapters.size,
                )
                dao.upsert(newManga)
                _inLibrary.value = true
                com.mangadl.android.data.sync.supabase.SupabaseSyncManager.syncMangaSubscriptionAsync(newManga, subscribed = true)
            }
        }
    }
}

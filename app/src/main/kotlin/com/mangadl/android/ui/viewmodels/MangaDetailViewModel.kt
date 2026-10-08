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

    val allCategories: StateFlow<List<com.mangadl.android.data.model.CategoryEntity>> = db.categoryDao().getAll()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _mangaCategories = MutableStateFlow<List<String>>(emptyList())
    val mangaCategories: StateFlow<List<String>> = _mangaCategories

    private var currentSourceId = ""
    private var currentMangaId = ""

    fun load(sourceId: String, mangaId: String) {
        if (sourceId == currentSourceId && mangaId == currentMangaId && _state.value is DetailState.Success) return
        currentSourceId = sourceId
        currentMangaId = mangaId
        viewModelScope.launch {
            _state.value = DetailState.Loading
            _inLibrary.value = dao.getById(mangaId) != null
            _mangaCategories.value = db.categoryDao().getCategoriesForMangaOnce(mangaId)
            runCatching {
                extMgr.getMangaDetail(sourceId, mangaId)
            }.onSuccess { detail ->
                _state.value = DetailState.Success(detail)
            }.onFailure { e ->
                _state.value = DetailState.Error(e.message ?: "Failed to load")
            }
        }
    }

    fun setCategories(categoryIds: List<String>) {
        val mangaId = currentMangaId
        if (mangaId.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            db.categoryDao().setMangaCategories(mangaId, categoryIds)
            _mangaCategories.value = categoryIds
        }
    }

    fun createCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val slug = trimmed.lowercase().replace("[^a-z0-9]+".toRegex(), "-").trim('-').ifEmpty { "shelf-" + System.currentTimeMillis() }
        viewModelScope.launch(Dispatchers.IO) {
            val existing = db.categoryDao().getAllOnce()
            val nextOrder = (existing.maxOfOrNull { it.sortOrder } ?: 0) + 1
            db.categoryDao().upsert(com.mangadl.android.data.model.CategoryEntity(id = slug, name = trimmed, sortOrder = nextOrder))
            if (currentMangaId.isNotEmpty()) {
                val updated = (_mangaCategories.value + slug).distinct()
                db.categoryDao().setMangaCategories(currentMangaId, updated)
                _mangaCategories.value = updated
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

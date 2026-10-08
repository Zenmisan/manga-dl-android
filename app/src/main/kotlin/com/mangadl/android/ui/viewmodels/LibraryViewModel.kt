package com.mangadl.android.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.model.CategoryEntity
import com.mangadl.android.data.model.LibraryManga
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel : ViewModel() {
    private val db = MangaDlApp.instance.database

    val library: StateFlow<List<LibraryManga>> = db.libraryDao().getAll()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = db.categoryDao().getAll()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val mangaCategoryMap: StateFlow<Map<String, List<String>>> = db.categoryDao().getAllMangaCategories()
        .map { list ->
            list.groupBy({ it.mangaId }, { it.categoryId })
        }
        .catch { emit(emptyMap()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _selectedCategoryId = MutableStateFlow<String?>(null) // null = "All"
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    fun createCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val slug = trimmed.lowercase().replace("[^a-z0-9]+".toRegex(), "-").trim('-').ifEmpty { "shelf-" + System.currentTimeMillis() }
        viewModelScope.launch {
            val existing = db.categoryDao().getAllOnce()
            val nextOrder = (existing.maxOfOrNull { it.sortOrder } ?: 0) + 1
            db.categoryDao().upsert(CategoryEntity(id = slug, name = trimmed, sortOrder = nextOrder))
        }
    }

    fun renameCategory(id: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val existing = db.categoryDao().getAllOnce().find { it.id == id }
            if (existing != null) {
                db.categoryDao().upsert(existing.copy(name = trimmed))
            }
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            db.categoryDao().deleteCategory(id)
            if (_selectedCategoryId.value == id) {
                _selectedCategoryId.value = null
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            val existing = db.libraryDao().getById(id)
            db.libraryDao().delete(id)
            if (existing != null) {
                com.mangadl.android.data.sync.supabase.SupabaseSyncManager.syncMangaSubscriptionAsync(existing, subscribed = false)
            }
        }
    }

    fun markAllRead(id: String, totalChapters: Int) {
        viewModelScope.launch { db.libraryDao().updateReadCount(id, totalChapters) }
    }
}


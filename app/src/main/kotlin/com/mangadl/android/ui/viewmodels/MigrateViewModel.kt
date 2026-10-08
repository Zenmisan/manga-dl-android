package com.mangadl.android.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.model.LibraryManga
import com.mangadl.android.data.model.MangaSearchResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MigrateMatch(
    val extensionId: String,
    val extensionName: String,
    val result: MangaSearchResult,
)

class MigrateViewModel : ViewModel() {

    private val db = MangaDlApp.instance.database
    private val extMgr = MangaDlApp.instance.extensionManager

    val library: StateFlow<List<LibraryManga>> = db.libraryDao().getAll()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _sourceManga = MutableStateFlow<LibraryManga?>(null)
    val sourceManga: StateFlow<LibraryManga?> = _sourceManga

    private val _matches = MutableStateFlow<List<MigrateMatch>>(emptyList())
    val matches: StateFlow<List<MigrateMatch>> = _matches

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching

    fun selectSource(manga: LibraryManga) {
        _sourceManga.value = manga
        _matches.value = emptyList()
        searchOtherSources(manga.title, manga.provider)
    }

    private fun searchOtherSources(title: String, currentProvider: String) = viewModelScope.launch {
        _searching.value = true
        val extensions = extMgr.listExtensions().filter { it.id != currentProvider }
        val jobs = extensions.map { ext ->
            async {
                try {
                    val results = extMgr.search(ext.id, title, 1)
                    results.firstOrNull()?.let { MigrateMatch(ext.id, ext.name, it) }
                } catch (e: Exception) {
                    Log.w("MigrateVM", "Search failed for ${ext.id}", e)
                    null
                }
            }
        }
        _matches.value = jobs.awaitAll().filterNotNull()
        _searching.value = false
    }

    fun migrate(
        match: MigrateMatch,
        keepRead: Boolean,
        deleteOld: Boolean,
        onDone: () -> Unit,
    ) = viewModelScope.launch {
        val src = _sourceManga.value ?: return@launch
        val newEntry = LibraryManga(
            id = match.result.id,
            title = match.result.title,
            coverUrl = match.result.coverUrl ?: "",
            provider = match.extensionId,
            url = match.result.url,
            addedAt = src.addedAt,
            readCount = if (keepRead) src.readCount else 0,
            totalChapters = src.totalChapters,
            lastReadAt = if (keepRead) src.lastReadAt else null,
            lastReadChapterId = if (keepRead) src.lastReadChapterId else null,
        )
        db.libraryDao().upsert(newEntry)
        com.mangadl.android.data.sync.supabase.SupabaseSyncManager.syncMangaSubscriptionAsync(newEntry, subscribed = true)
        if (deleteOld) {
            db.libraryDao().delete(src.id)
            com.mangadl.android.data.sync.supabase.SupabaseSyncManager.syncMangaSubscriptionAsync(src, subscribed = false)
        }
        onDone()
    }

    fun copy(match: MigrateMatch, onDone: () -> Unit) = viewModelScope.launch {
        val src = _sourceManga.value ?: return@launch
        val newEntry = LibraryManga(
            id = match.result.id,
            title = match.result.title,
            coverUrl = match.result.coverUrl ?: "",
            provider = match.extensionId,
            url = match.result.url,
            addedAt = System.currentTimeMillis(),
            readCount = src.readCount,
            totalChapters = src.totalChapters,
        )
        db.libraryDao().upsert(newEntry)
        com.mangadl.android.data.sync.supabase.SupabaseSyncManager.syncMangaSubscriptionAsync(newEntry, subscribed = true)
        onDone()
    }
}

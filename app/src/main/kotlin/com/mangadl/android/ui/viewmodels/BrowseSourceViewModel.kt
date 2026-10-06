package com.mangadl.android.ui.viewmodels

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.ui.Manga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private val COVER_PALETTE = listOf(
    Color(0xFF1A2433), Color(0xFF3A1518), Color(0xFF2E2412), Color(0xFF2D1716),
    Color(0xFF13282A), Color(0xFF311A1F), Color(0xFF1D2A1A), Color(0xFF1B2030),
    Color(0xFF2B1A2E), Color(0xFF22222A), Color(0xFF1E3A5F), Color(0xFF1F2C4F),
)

class BrowseSourceViewModel(app: Application) : AndroidViewModel(app) {

    private val extMgr = MangaDlApp.instance.extensionManager

    private val _items = MutableStateFlow<List<Manga>>(emptyList())
    val items: StateFlow<List<Manga>> = _items

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _currentTab = MutableStateFlow("Popular")
    val currentTab: StateFlow<String> = _currentTab

    private var loadedSourceId = ""
    private var currentPage = 1
    private var currentQuery = ""
    private var hasMorePages = true
    private var fetchJob: Job? = null

    fun load(sourceId: String) {
        if (sourceId == loadedSourceId && _items.value.isNotEmpty()) return
        loadedSourceId = sourceId
        currentQuery = ""
        currentPage = 1
        hasMorePages = true
        fetch(sourceId, page = 1, append = false)
    }

    fun setTab(sourceId: String, tab: String) {
        if (_currentTab.value == tab && currentQuery.isBlank()) return
        _currentTab.value = tab
        currentQuery = ""
        currentPage = 1
        hasMorePages = true
        fetch(sourceId, page = 1, append = false)
    }

    fun search(sourceId: String, query: String) {
        currentQuery = query.trim()
        currentPage = 1
        hasMorePages = true
        fetch(sourceId, page = 1, append = false)
    }

    fun loadMore(sourceId: String) {
        if (_loading.value || !hasMorePages) return
        val nextPage = currentPage + 1
        fetch(sourceId, page = nextPage, append = true)
    }

    private fun fetch(sourceId: String, page: Int, append: Boolean) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch(Dispatchers.IO) {
            _loading.value = true
            if (!append) _error.value = null
            runCatching {
                if (currentQuery.isNotBlank()) {
                    extMgr.search(sourceId, currentQuery, page)
                } else if (_currentTab.value == "Latest") {
                    extMgr.getLatest(sourceId, page)
                } else {
                    extMgr.getPopular(sourceId, page)
                }
            }.onSuccess { results ->
                val newItems = results.map { r ->
                    val idx = r.id.hashCode().let { h -> if (h < 0) -h else h } % COVER_PALETTE.size
                    Manga(
                        id = r.id,
                        title = r.title,
                        cover = COVER_PALETTE[idx],
                        coverUrl = r.coverUrl,
                        source = r.provider.ifEmpty { sourceId }
                    )
                }
                if (append) {
                    _items.value = (_items.value + newItems).distinctBy { it.id }
                } else {
                    _items.value = newItems
                }
                hasMorePages = newItems.isNotEmpty()
                currentPage = page
            }.onFailure { e ->
                if (!append) {
                    _error.value = e.message ?: "Failed to load"
                    _items.value = emptyList()
                }
            }
            _loading.value = false
        }
    }
}

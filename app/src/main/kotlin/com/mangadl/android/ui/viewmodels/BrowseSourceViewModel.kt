package com.mangadl.android.ui.viewmodels

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.ui.Manga
import kotlinx.coroutines.Dispatchers
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

    private var loadedSourceId = ""

    fun load(sourceId: String) {
        if (sourceId == loadedSourceId && _items.value.isNotEmpty()) return
        loadedSourceId = sourceId
        search(sourceId, "")
    }

    fun search(sourceId: String, query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.value = true
            _error.value = null
            runCatching {
                extMgr.search(sourceId, query.ifBlank { "a" }, 1)
            }.onSuccess { results ->
                _items.value = results.map { r ->
                    val idx = r.id.hashCode().let { h -> if (h < 0) -h else h } % COVER_PALETTE.size
                    Manga(id = r.id, title = r.title, cover = COVER_PALETTE[idx], coverUrl = r.coverUrl, source = r.provider)
                }
            }.onFailure { e ->
                _error.value = e.message ?: "Failed to load"
                _items.value = emptyList()
            }
            _loading.value = false
        }
    }
}

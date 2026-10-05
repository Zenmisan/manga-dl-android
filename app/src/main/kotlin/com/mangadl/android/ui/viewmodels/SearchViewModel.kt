package com.mangadl.android.ui.viewmodels

import android.app.Application
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.ui.Manga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private val COVER_PALETTE = listOf(
    Color(0xFF1A2433), Color(0xFF3A1518), Color(0xFF2E2412), Color(0xFF2D1716),
    Color(0xFF13282A), Color(0xFF311A1F), Color(0xFF1D2A1A), Color(0xFF1B2030),
    Color(0xFF2B1A2E), Color(0xFF22222A), Color(0xFF1E3A5F), Color(0xFF1F2C4F),
)

class SearchViewModel(app: Application) : AndroidViewModel(app) {

    private val extMgr = MangaDlApp.instance.extensionManager
    private val prefs = app.getSharedPreferences("manga_dl_sources", Context.MODE_PRIVATE)

    private val _results = MutableStateFlow<List<Pair<String, List<Manga>>>>(emptyList())
    val results: StateFlow<List<Pair<String, List<Manga>>>> = _results

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching

    fun search(query: String) {
        if (query.isBlank()) {
            _results.value = emptyList()
            return
        }
        viewModelScope.launch {
            _searching.value = true
            _results.value = emptyList()
            val disabledIds = prefs.getStringSet("disabled_sources", emptySet()) ?: emptySet()
            val enabledSources = extMgr.listExtensions().filter { it.id !in disabledIds }
            val found = enabledSources.map { src ->
                async(Dispatchers.IO) {
                    runCatching {
                        val items = extMgr.search(src.id, query, 1)
                        src.name to items.map { r ->
                            val idx = r.id.hashCode().let { if (it < 0) -it else it } % COVER_PALETTE.size
                            Manga(id = r.id, title = r.title, cover = COVER_PALETTE[idx], source = r.provider)
                        }
                    }.getOrNull()
                }
            }.awaitAll().filterNotNull().filter { it.second.isNotEmpty() }
            _results.value = found
            _searching.value = false
        }
    }
}

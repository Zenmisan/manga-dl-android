package com.mangadl.android.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.model.LibraryManga
import com.mangadl.android.data.model.ReadingProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel : ViewModel() {
    private val db = MangaDlApp.instance.database

    val allProgress: StateFlow<List<ReadingProgress>> = db.progressDao().getRecent(500)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val library: StateFlow<List<LibraryManga>> = db.libraryDao().getAll()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            db.progressDao().deleteAll()
        }
    }
}

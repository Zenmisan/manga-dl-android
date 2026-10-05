package com.mangadl.android.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.model.LibraryManga
import com.mangadl.android.data.model.ReadingProgress
import com.mangadl.android.data.ui.currentStreak
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class StatisticsViewModel : ViewModel() {
    private val db = MangaDlApp.instance.database

    val library: StateFlow<List<LibraryManga>> = db.libraryDao().getAll()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allProgress: StateFlow<List<ReadingProgress>> = db.progressDao().getRecent(1000)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalChaptersRead: StateFlow<Int> = db.progressDao().getRecent(1000)
        .catch { emit(emptyList()) }
        .map { list -> list.count { it.completed } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val streak: StateFlow<Int> = db.progressDao().getRecent(1000)
        .catch { emit(emptyList()) }
        .map { it.currentStreak() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}

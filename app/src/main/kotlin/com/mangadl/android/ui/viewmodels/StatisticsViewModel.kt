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

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

    val readingTimeHours: StateFlow<String> = totalChaptersRead
        .map { count ->
            val totalMinutes = count * 6
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "0m")

    val streak: StateFlow<Int> = db.progressDao().getRecent(1000)
        .catch { emit(emptyList()) }
        .map { it.currentStreak() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val activityHeatmap: StateFlow<List<List<Int>>> = db.progressDao().getRecent(1000)
        .catch { emit(emptyList()) }
        .map { progressList ->
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val countsByDate = progressList.groupBy { sdf.format(Date(it.readAt)) }.mapValues { it.value.size }

            val weeks = 18
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -(weeks * 7 - 1))

            val matrix = mutableListOf<List<Int>>()
            for (w in 0 until weeks) {
                val weekDays = mutableListOf<Int>()
                for (d in 0 until 7) {
                    val key = sdf.format(cal.time)
                    weekDays.add(countsByDate[key] ?: 0)
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                matrix.add(weekDays)
            }
            matrix
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), List(18) { List(7) { 0 } })
}

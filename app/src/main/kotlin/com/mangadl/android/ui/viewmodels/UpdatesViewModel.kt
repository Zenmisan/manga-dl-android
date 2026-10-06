package com.mangadl.android.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.library.LibraryUpdateWorker
import com.mangadl.android.data.model.NewChapterEntry
import com.mangadl.android.data.prefs.AppPreferences
import com.mangadl.android.data.prefs.PrefKeys
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UpdatesViewModel(app: Application) : AndroidViewModel(app) {
    private val db = MangaDlApp.instance.database
    private val workManager = WorkManager.getInstance(app)
    private val prefs = AppPreferences.getInstance(app)

    val newChapters: StateFlow<List<NewChapterEntry>> = db.updatesDao().getAll()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val lastChecked: StateFlow<String?> = prefs.lastUpdateCheck
        .map { if (it <= 0L) null else formatLastChecked(it) }
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun refresh() {
        val request = OneTimeWorkRequestBuilder<LibraryUpdateWorker>().build()
        // Distinct name from LibraryUpdateWorker.WORK_NAME: enqueueUniqueWork and
        // enqueueUniquePeriodicWork share the same unique-name table, so reusing the periodic
        // schedule's name here would cancel/replace it instead of just running once.
        workManager.enqueueUniqueWork("library_update_manual", ExistingWorkPolicy.REPLACE, request)
        viewModelScope.launch { prefs.set(PrefKeys.LAST_UPDATE_CHECK, System.currentTimeMillis()) }
    }

    private fun formatLastChecked(ms: Long): String {
        val diff = System.currentTimeMillis() - ms
        return when {
            diff < 60_000L -> "just now"
            diff < 3_600_000L -> "${diff / 60_000}m ago"
            diff < 86_400_000L -> "${diff / 3_600_000}h ago"
            else -> SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(ms))
        }
    }
}

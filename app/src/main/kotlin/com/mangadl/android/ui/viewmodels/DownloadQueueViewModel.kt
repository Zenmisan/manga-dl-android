package com.mangadl.android.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Constraints
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.download.DownloadWorker
import com.mangadl.android.data.model.DownloadEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class DownloadQueueViewModel(app: Application) : AndroidViewModel(app) {

    private val db = MangaDlApp.instance.database
    private val workManager = WorkManager.getInstance(app)

    val downloads: StateFlow<List<DownloadEntry>> = db.downloadDao().getAll()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun enqueue(
        mangaId: String,
        mangaTitle: String,
        chapterId: String,
        chapterTitle: String,
        provider: String,
    ) = viewModelScope.launch {
        val entry = DownloadEntry(
            id = UUID.randomUUID().toString(),
            mangaId = mangaId,
            mangaTitle = mangaTitle,
            chapterId = chapterId,
            chapterTitle = chapterTitle,
            provider = provider,
        )
        db.downloadDao().upsert(entry)
        triggerWorker()
    }

    fun triggerWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniqueWork(
            DownloadWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun cancel(id: String) = viewModelScope.launch {
        db.downloadDao().delete(id)
    }
}

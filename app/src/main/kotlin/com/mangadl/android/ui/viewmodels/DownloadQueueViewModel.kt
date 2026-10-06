package com.mangadl.android.ui.viewmodels

import android.app.Application
import android.content.Context
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.download.DownloadWorker
import com.mangadl.android.data.model.Chapter
import com.mangadl.android.data.model.DownloadEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class DownloadQueueViewModel(app: Application) : AndroidViewModel(app) {

    private val db = MangaDlApp.instance.database
    private val workManager = WorkManager.getInstance(app)
    private val prefs = app.getSharedPreferences("manga_dl_downloads", Context.MODE_PRIVATE)

    val downloads: StateFlow<List<DownloadEntry>> = db.downloadDao().getAll()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isQueuePaused = MutableStateFlow(prefs.getBoolean("queue_paused", false))
    val isQueuePaused: StateFlow<Boolean> = _isQueuePaused

    private val _storageUsedBytes = MutableStateFlow(0L)
    val storageUsedBytes: StateFlow<Long> = _storageUsedBytes

    init {
        refreshStorageUsage()
    }

    fun enqueue(
        mangaId: String,
        mangaTitle: String,
        chapterId: String,
        chapterTitle: String,
        provider: String,
    ) = viewModelScope.launch(Dispatchers.IO) {
        val entry = DownloadEntry(
            id = "${provider}_${mangaId}_${chapterId}".replace(Regex("[^a-zA-Z0-9_]"), "_"),
            mangaId = mangaId,
            mangaTitle = mangaTitle,
            chapterId = chapterId,
            chapterTitle = chapterTitle,
            provider = provider,
            status = "queued",
        )
        db.downloadDao().upsert(entry)
        if (!_isQueuePaused.value) {
            triggerWorker()
        }
    }

    fun enqueueBatch(
        mangaId: String,
        mangaTitle: String,
        provider: String,
        chapters: List<Chapter>,
    ) = viewModelScope.launch(Dispatchers.IO) {
        for (ch in chapters) {
            val entry = DownloadEntry(
                id = "${provider}_${mangaId}_${ch.id}".replace(Regex("[^a-zA-Z0-9_]"), "_"),
                mangaId = mangaId,
                mangaTitle = mangaTitle,
                chapterId = ch.id,
                chapterTitle = ch.title.ifBlank { "Chapter ${ch.number}" },
                provider = provider,
                status = "queued",
            )
            db.downloadDao().upsert(entry)
        }
        if (!_isQueuePaused.value) {
            triggerWorker()
        }
    }

    fun retry(id: String) = viewModelScope.launch(Dispatchers.IO) {
        db.downloadDao().updateProgress(id, "queued", 0)
        if (!_isQueuePaused.value) {
            triggerWorker()
        }
    }

    fun pause(id: String) = viewModelScope.launch(Dispatchers.IO) {
        db.downloadDao().updateStatus(id, "paused")
    }

    fun resume(id: String) = viewModelScope.launch(Dispatchers.IO) {
        db.downloadDao().updateStatus(id, "queued")
        if (!_isQueuePaused.value) {
            triggerWorker()
        }
    }

    fun remove(id: String) = viewModelScope.launch(Dispatchers.IO) {
        val entry = db.downloadDao().getById(id)
        if (entry?.filePath != null) {
            runCatching { File(entry.filePath).delete() }
        }
        db.downloadDao().delete(id)
        refreshStorageUsage()
    }

    fun toggleQueuePaused() {
        val newPaused = !_isQueuePaused.value
        prefs.edit().putBoolean("queue_paused", newPaused).apply()
        _isQueuePaused.value = newPaused
        if (!newPaused) {
            triggerWorker()
        }
    }

    fun clearAll() = viewModelScope.launch(Dispatchers.IO) {
        val all = db.downloadDao().getPending()
        all.forEach { entry ->
            if (entry.filePath != null) {
                runCatching { File(entry.filePath).delete() }
            }
        }
        db.downloadDao().deleteAll()
        refreshStorageUsage()
    }

    fun refreshStorageUsage() = viewModelScope.launch(Dispatchers.IO) {
        val baseDownloadsDir = File(
            getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: getApplication<Application>().filesDir,
            "manga-dl"
        )
        val bytes = calculateDirectorySize(baseDownloadsDir)
        _storageUsedBytes.value = bytes
    }

    private fun calculateDirectorySize(dir: File): Long {
        if (!dir.exists()) return 0L
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) calculateDirectorySize(file) else file.length()
        }
        return size
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
}

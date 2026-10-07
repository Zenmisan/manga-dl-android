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
        entry?.filePath?.let { deleteDownloadedFile(it) }
        db.downloadDao().delete(id)
        refreshStorageUsage()
    }

    /**
     * [DownloadWorker] stores either a plain filesystem path (app-private storage) or a
     * `content://` MediaStore URI (public Downloads, when "Save to public Downloads" is on) in
     * [DownloadEntry.filePath] — `File(path).delete()` silently no-ops on a content URI since it
     * isn't a real filesystem path, leaving the file orphaned in the public Downloads folder.
     */
    private fun deleteDownloadedFile(path: String) {
        if (path.startsWith("content://")) {
            runCatching {
                getApplication<Application>().contentResolver.delete(android.net.Uri.parse(path), null, null)
            }
        } else {
            runCatching { File(path).delete() }
        }
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
            entry.filePath?.let { deleteDownloadedFile(it) }
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
        val bytes = calculateDirectorySize(baseDownloadsDir) + publicDownloadsBytes()
        _storageUsedBytes.value = bytes
    }

    /**
     * Sums the size of everything this app has published under `Downloads/manga-dl/` via
     * MediaStore — the counterpart to [calculateDirectorySize] for chapters saved with "Save to
     * public Downloads" on, which live outside [calculateDirectorySize]'s app-private directory.
     */
    private fun publicDownloadsBytes(): Long {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) return 0L
        return runCatching {
            var total = 0L
            val resolver = getApplication<Application>().contentResolver
            resolver.query(
                android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                arrayOf(android.provider.MediaStore.Downloads.SIZE, android.provider.MediaStore.Downloads.RELATIVE_PATH),
                null, null, null,
            )?.use { cursor ->
                val sizeIdx = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Downloads.SIZE)
                val pathIdx = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Downloads.RELATIVE_PATH)
                while (cursor.moveToNext()) {
                    val relPath = cursor.getString(pathIdx) ?: ""
                    if (relPath.startsWith("${Environment.DIRECTORY_DOWNLOADS}/manga-dl")) {
                        total += cursor.getLong(sizeIdx)
                    }
                }
            }
            total
        }.getOrDefault(0L)
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

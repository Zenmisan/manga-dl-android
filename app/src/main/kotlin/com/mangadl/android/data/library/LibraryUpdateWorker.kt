package com.mangadl.android.data.library

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.download.DownloadWorker
import com.mangadl.android.data.model.DownloadEntry
import com.mangadl.android.data.model.NewChapterEntry
import com.mangadl.android.data.prefs.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArrayList

class LibraryUpdateWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext as MangaDlApp
        val db = app.database
        val extMgr = app.extensionManager

        val items = db.libraryDao().getAllOnce()
        if (items.isEmpty()) return@withContext Result.success()

        val prefs = AppPreferences.getInstance(applicationContext)
        val autoDownload = prefs.autoDownloadNew.first()
        val newChapterItems = CopyOnWriteArrayList<Pair<String, Int>>() // title, newCount
        val queuedAnyDownload = java.util.concurrent.atomic.AtomicBoolean(false)

        items.map { manga ->
            async {
                runCatching {
                    val detail = extMgr.getMangaDetail(manga.provider, manga.id)
                    val latestCount = detail.chapters.size
                    if (latestCount > manga.totalChapters) {
                        db.libraryDao().updateTotalChapters(manga.id, latestCount)
                        val newCount = latestCount - manga.totalChapters
                        newChapterItems.add(manga.title to newCount)

                        val newest = detail.chapters
                            .sortedByDescending { it.number }
                            .take(newCount)
                            .map { ch ->
                                NewChapterEntry(
                                    id = "${manga.id}:${ch.id}",
                                    mangaId = manga.id,
                                    mangaTitle = manga.title,
                                    coverUrl = manga.coverUrl,
                                    provider = manga.provider,
                                    chapterId = ch.id,
                                    chapterTitle = ch.title.ifBlank { "Chapter ${ch.number}" },
                                    chapterNumber = ch.number,
                                )
                            }
                        if (newest.isNotEmpty()) db.updatesDao().upsert(newest)

                        if (autoDownload && newest.isNotEmpty()) {
                            newest.forEach { ch ->
                                db.downloadDao().upsert(
                                    DownloadEntry(
                                        id = "${manga.provider}_${manga.id}_${ch.chapterId}".replace(Regex("[^a-zA-Z0-9_]"), "_"),
                                        mangaId = manga.id,
                                        mangaTitle = manga.title,
                                        chapterId = ch.chapterId,
                                        chapterTitle = ch.chapterTitle,
                                        provider = manga.provider,
                                        status = "queued",
                                    )
                                )
                            }
                            queuedAnyDownload.set(true)
                        }
                    }
                }
            }
        }.awaitAll()

        if (newChapterItems.isNotEmpty() && prefs.newChapterAlerts.first()) {
            postNotification(newChapterItems)
        }

        if (queuedAnyDownload.get()) {
            val wifiOnly = prefs.downloadWifiOnly.first()
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .build()
            WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                DownloadWorker.WORK_NAME,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<DownloadWorker>().setConstraints(constraints).build(),
            )
        }

        Result.success()
    }

    private fun postNotification(items: List<Pair<String, Int>>) {
        val ctx = applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return
        }

        val nm = NotificationManagerCompat.from(ctx)
        val title: String
        val body: String
        if (items.size == 1) {
            val (mangaTitle, count) = items.first()
            title = mangaTitle
            body = "$count new chapter${if (count > 1) "s" else ""} available"
        } else {
            title = "${items.size} series updated"
            body = items.take(3).joinToString(", ") { it.first } +
                if (items.size > 3) " and ${items.size - 3} more" else ""
        }

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIF_ID, notification)
    }

    companion object {
        const val WORK_NAME = "library_update"
        const val CHANNEL_ID = "manga_updates"
        const val CHANNEL_NAME = "Library Updates"
        private const val NOTIF_ID = 1001

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = "New chapter notifications for library titles" }
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.createNotificationChannel(channel)
            }
        }
    }
}

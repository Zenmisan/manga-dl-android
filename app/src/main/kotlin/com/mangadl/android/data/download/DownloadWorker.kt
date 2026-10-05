package com.mangadl.android.data.download

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mangadl.android.MangaDlApp
import okhttp3.Request
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DownloadWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as MangaDlApp
        val db = app.database
        val extMgr = app.extensionManager
        val httpClient = app.httpClient

        val pending = db.downloadDao().getPending()
        if (pending.isEmpty()) return Result.success()

        for (entry in pending) {
            try {
                db.downloadDao().updateProgress(entry.id, "downloading", 0)

                val pages = extMgr.getPages(entry.provider, entry.chapterId)
                if (pages.isEmpty()) {
                    db.downloadDao().updateProgress(entry.id, "failed", 0)
                    continue
                }

                val outDir = File(applicationContext.filesDir, "downloads/${entry.mangaId}")
                outDir.mkdirs()
                val cbzFile = File(outDir, "${entry.id}.cbz")

                ZipOutputStream(cbzFile.outputStream().buffered()).use { zip ->
                    pages.forEachIndexed { i, url ->
                        val bytes = try {
                            httpClient.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                                if (resp.isSuccessful) resp.body?.bytes() else null
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Page $i download failed: $url", e)
                            null
                        } ?: return@forEachIndexed

                        val ext = url.substringBefore('?').substringAfterLast('.').take(4)
                            .let { if (it.matches(Regex("[a-zA-Z]+"))) it else "jpg" }
                        zip.putNextEntry(ZipEntry(String.format("%04d.%s", i + 1, ext)))
                        zip.write(bytes)
                        zip.closeEntry()

                        db.downloadDao().updateProgress(entry.id, "downloading", i + 1)
                    }
                }

                db.downloadDao().markCompleted(entry.id, System.currentTimeMillis(), cbzFile.absolutePath)
                Log.i(TAG, "Completed: ${entry.chapterTitle} (${pages.size} pages)")
            } catch (e: Exception) {
                Log.e(TAG, "Download failed: ${entry.id}", e)
                db.downloadDao().updateProgress(entry.id, "failed", 0)
            }
        }

        return Result.success()
    }

    companion object {
        const val TAG = "DownloadWorker"
        const val WORK_NAME = "manga_download"
    }
}

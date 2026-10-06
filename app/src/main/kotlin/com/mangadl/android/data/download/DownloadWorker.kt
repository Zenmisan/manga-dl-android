package com.mangadl.android.data.download

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mangadl.android.BuildConfig
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.extensions.ExtensionManager
import com.mangadl.android.data.model.DownloadEntry
import okhttp3.Request
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DownloadWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as MangaDlApp
        val db = app.database
        val extMgr = app.extensionManager
        val httpClient = app.httpClient
        val prefs = applicationContext.getSharedPreferences("manga_dl_downloads", Context.MODE_PRIVATE)

        val pending = db.downloadDao().getPending()
        if (pending.isEmpty()) return Result.success()

        for (entry in pending) {
            if (isStopped || prefs.getBoolean("queue_paused", false)) {
                Log.d(TAG, "Queue paused or worker stopped; exiting loop.")
                break
            }

            try {
                db.downloadDao().updateProgress(entry.id, "downloading", 0)

                val safeMangaTitle = sanitizeFilename(entry.mangaTitle)
                val safeChapterTitle = sanitizeFilename(entry.chapterTitle.ifBlank { "Chapter_${entry.chapterId}" })
                val baseDownloadsDir = File(
                    applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: applicationContext.filesDir,
                    "manga-dl/$safeMangaTitle"
                )
                baseDownloadsDir.mkdirs()

                val isNovel = ExtensionManager.isNovelSource(entry.provider)

                if (isNovel) {
                    downloadNovelChapter(entry, extMgr, baseDownloadsDir, safeMangaTitle, safeChapterTitle)
                } else {
                    downloadMangaChapter(entry, extMgr, httpClient, baseDownloadsDir, safeMangaTitle, safeChapterTitle)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download failed for entry ${entry.id}", e)
                db.downloadDao().updateProgress(entry.id, "failed", 0)
            }
        }

        return Result.success()
    }

    private suspend fun downloadNovelChapter(
        entry: DownloadEntry,
        extMgr: ExtensionManager,
        baseDir: File,
        mangaTitle: String,
        chapterTitle: String,
    ) {
        val app = applicationContext as MangaDlApp
        val db = app.database

        db.downloadDao().updateTotalPages(entry.id, 1)
        val rawContent = extMgr.getChapterText(entry.provider, entry.chapterId)

        if (rawContent.isBlank()) {
            Log.w(TAG, "Empty novel chapter text for ${entry.chapterId}")
            db.downloadDao().updateProgress(entry.id, "failed", 0)
            return
        }

        val epubFile = File(baseDir, "$chapterTitle.epub")
        buildEpub(epubFile, mangaTitle, chapterTitle, rawContent, entry.id)

        db.downloadDao().updateProgress(entry.id, "downloading", 1)
        db.downloadDao().markCompleted(entry.id, System.currentTimeMillis(), epubFile.absolutePath)
        Log.i(TAG, "Novel chapter completed: ${entry.chapterTitle} -> ${epubFile.absolutePath}")
    }

    private suspend fun downloadMangaChapter(
        entry: DownloadEntry,
        extMgr: ExtensionManager,
        httpClient: okhttp3.OkHttpClient,
        baseDir: File,
        mangaTitle: String,
        chapterTitle: String,
    ) {
        val app = applicationContext as MangaDlApp
        val db = app.database

        val pages = extMgr.getPages(entry.provider, entry.chapterId)
        if (pages.isEmpty()) {
            Log.w(TAG, "No pages found for ${entry.chapterId}")
            db.downloadDao().updateProgress(entry.id, "failed", 0)
            return
        }

        db.downloadDao().updateTotalPages(entry.id, pages.size)
        val cbzFile = File(baseDir, "$chapterTitle.cbz")

        ZipOutputStream(cbzFile.outputStream().buffered()).use { zip ->
            // Write ComicInfo.xml metadata
            val comicInfo = buildComicInfoXml(mangaTitle, chapterTitle, pages.size)
            zip.putNextEntry(ZipEntry("ComicInfo.xml"))
            zip.write(comicInfo.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            pages.forEachIndexed { i, rawUrl ->
                val resolvedUrl = if (rawUrl.startsWith("/")) {
                    "${BuildConfig.BACKEND_URL.trimEnd('/')}/api$rawUrl"
                } else rawUrl

                val referer = if (resolvedUrl.startsWith("http")) {
                    resolvedUrl.split("/").take(3).joinToString("/") + "/"
                } else ""

                val req = Request.Builder()
                    .url(resolvedUrl)
                    .apply { if (referer.isNotEmpty()) addHeader("Referer", referer) }
                    .build()

                val bytes = try {
                    httpClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) resp.body?.bytes() else null
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Page $i download failed: $resolvedUrl", e)
                    null
                } ?: return@forEachIndexed

                val ext = resolvedUrl.substringBefore('?').substringAfterLast('.').take(4)
                    .let { if (it.matches(Regex("[a-zA-Z]+"))) it else "jpg" }
                zip.putNextEntry(ZipEntry(String.format("%04d.%s", i + 1, ext)))
                zip.write(bytes)
                zip.closeEntry()

                db.downloadDao().updateProgress(entry.id, "downloading", i + 1)
            }
        }

        db.downloadDao().markCompleted(entry.id, System.currentTimeMillis(), cbzFile.absolutePath)
        Log.i(TAG, "Manga chapter completed: ${entry.chapterTitle} (${pages.size} pages) -> ${cbzFile.absolutePath}")
    }

    private fun buildComicInfoXml(series: String, title: String, pageCount: Int): String =
        DownloadPackaging.buildComicInfoXml(series, title, pageCount)

    private fun buildEpub(
        outFile: File,
        bookTitle: String,
        chapterTitle: String,
        htmlOrText: String,
        chapterId: String,
    ) = DownloadPackaging.buildEpub(outFile, bookTitle, chapterTitle, htmlOrText, chapterId)

    private fun sanitizeFilename(name: String): String =
        DownloadPackaging.sanitizeFilename(name)

    companion object {
        const val TAG = "DownloadWorker"
        const val WORK_NAME = "manga_download"
    }
}

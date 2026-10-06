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

    private fun buildComicInfoXml(series: String, title: String, pageCount: Int): String {
        val chapterNum = title.filter { it.isDigit() || it == '.' }.ifBlank { "1" }
        return """<?xml version="1.0" encoding="utf-8"?>
<ComicInfo xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <Series>${escapeXml(series)}</Series>
  <Title>${escapeXml(title)}</Title>
  <Number>$chapterNum</Number>
  <PageCount>$pageCount</PageCount>
</ComicInfo>""".trimIndent()
    }

    private fun buildEpub(
        outFile: File,
        bookTitle: String,
        chapterTitle: String,
        htmlOrText: String,
        chapterId: String,
    ) {
        val formattedBody = if (htmlOrText.contains("<p>") || htmlOrText.contains("<div>")) {
            htmlOrText
        } else {
            htmlOrText.lines()
                .filter { it.isNotBlank() }
                .joinToString("\n") { "<p>${escapeXml(it.trim())}</p>" }
        }

        val chapterXhtml = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>${escapeXml(chapterTitle)}</title>
    <style type="text/css">
      body { font-family: sans-serif; line-height: 1.6; margin: 1em; }
      h1 { font-size: 1.5em; margin-bottom: 1em; }
      p { margin-bottom: 1em; text-indent: 1em; }
    </style>
  </head>
  <body>
    <h1>${escapeXml(chapterTitle)}</h1>
    $formattedBody
  </body>
</html>""".trimIndent()

        val containerXml = """<?xml version="1.0"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>""".trimIndent()

        val contentOpf = """<?xml version="1.0" encoding="utf-8"?>
<package xmlns="http://www.idpf.org/2007/opf" unique-identifier="BookID" version="2.0">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:title>${escapeXml(bookTitle)} - ${escapeXml(chapterTitle)}</dc:title>
    <dc:identifier id="BookID">$chapterId</dc:identifier>
    <dc:language>en</dc:language>
  </metadata>
  <manifest>
    <item id="chapter" href="chapter.xhtml" media-type="application/xhtml+xml"/>
  </manifest>
  <spine>
    <itemref idref="chapter"/>
  </spine>
</package>""".trimIndent()

        ZipOutputStream(outFile.outputStream().buffered()).use { zip ->
            // 1. mimetype (MUST be first and uncompressed per EPUB standard)
            val mimeBytes = "application/epub+zip".toByteArray(StandardCharsets.US_ASCII)
            val mimeEntry = ZipEntry("mimetype").apply {
                method = ZipEntry.STORED
                size = mimeBytes.size.toLong()
                compressedSize = mimeBytes.size.toLong()
                crc = CRC32().apply { update(mimeBytes) }.value
            }
            zip.putNextEntry(mimeEntry)
            zip.write(mimeBytes)
            zip.closeEntry()

            // 2. META-INF/container.xml
            zip.putNextEntry(ZipEntry("META-INF/container.xml"))
            zip.write(containerXml.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 3. OEBPS/content.opf
            zip.putNextEntry(ZipEntry("OEBPS/content.opf"))
            zip.write(contentOpf.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 4. OEBPS/chapter.xhtml
            zip.putNextEntry(ZipEntry("OEBPS/chapter.xhtml"))
            zip.write(chapterXhtml.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()
        }
    }

    private fun escapeXml(str: String): String {
        return str
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[/\\\\?%*:|\"<>]"), "-").trim().take(100)
    }

    companion object {
        const val TAG = "DownloadWorker"
        const val WORK_NAME = "manga_download"
    }
}

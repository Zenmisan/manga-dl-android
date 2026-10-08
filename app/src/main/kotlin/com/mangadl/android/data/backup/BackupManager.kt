package com.mangadl.android.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.db.AppDatabase
import com.mangadl.android.data.model.LibraryManga
import com.mangadl.android.data.model.ReadingProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupManager(
    private val db: AppDatabase = MangaDlApp.instance.database,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Export all library manga and reading progress to a formatted Manga-DL JSON backup file.
     */
    suspend fun exportBackup(context: Context): File = withContext(Dispatchers.IO) {
        val libraryEntries = db.libraryDao().getAllOnce()
        val progressEntries = db.progressDao().getRecent(10000).first()

        val backupLibrary = libraryEntries.map { m ->
            BackupMangaEntry(
                id = m.id,
                title = m.title,
                coverUrl = m.coverUrl,
                provider = m.provider,
                url = m.url,
                type = if (com.mangadl.android.data.source.SourceManager.NOVEL_SOURCE_IDS.contains(m.provider.lowercase())) "novel" else "manga",
                addedAt = m.addedAt,
                lastReadChapterId = m.lastReadChapterId,
                lastReadAt = m.lastReadAt,
                totalChapters = m.totalChapters,
                readCount = m.readCount,
            )
        }

        val backupProgress = progressEntries.map { p ->
            val num = extractChapterNumber(p.chapterId)
            BackupProgressEntry(
                mangaId = p.mangaId,
                chapterId = p.chapterId,
                provider = p.provider,
                page = p.page,
                totalPages = p.totalPages,
                readAt = p.readAt,
                completed = p.completed,
                chapterNumber = num,
            )
        }

        val backup = MangaDlBackup(
            version = "1.0",
            app = "manga-dl",
            exportedAt = System.currentTimeMillis(),
            library = backupLibrary,
            progress = backupProgress,
        )

        val jsonString = json.encodeToString(backup)
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(Date())
        val backupDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val backupFile = File(backupDir, "manga-dl-backup-$timestamp.mangadl")
        backupFile.writeText(jsonString, Charsets.UTF_8)
        backupFile
    }

    /**
     * Universal import engine:
     * Autodetects Manga-DL JSON, Tachiyomi Protobuf (.tachibk), and Tachiyomi JSON.
     * Persists restored manga into LibraryDao and progress into ProgressDao.
     */
    suspend fun importBackup(
        inputStream: InputStream,
        fileName: String? = null,
    ): BackupRestoreResult = withContext(Dispatchers.IO) {
        try {
            val rawBytes = inputStream.readBytes()
            if (rawBytes.isEmpty()) {
                return@withContext BackupRestoreResult(
                    success = false,
                    sourceFormat = "Unknown",
                    restoredMangaCount = 0,
                    restoredProgressCount = 0,
                    restoredCategoriesCount = 0,
                    errorMessage = "Empty backup file",
                )
            }

            // Check if it's Manga-DL JSON
            val isMangaDlJson = runCatching {
                if (rawBytes.size >= 2 && rawBytes[0] == 0x1f.toByte()) {
                    false
                } else {
                    val root = json.parseToJsonElement(String(rawBytes, Charsets.UTF_8)).jsonObject
                    root["app"]?.jsonPrimitive?.content == "manga-dl"
                }
            }.getOrDefault(false)

            if (isMangaDlJson) {
                val backup = json.decodeFromString<MangaDlBackup>(String(rawBytes, Charsets.UTF_8))
                var mangaCount = 0
                for (m in backup.library) {
                    db.libraryDao().upsert(
                        LibraryManga(
                            id = m.id,
                            title = m.title,
                            coverUrl = m.coverUrl,
                            provider = m.provider,
                            url = m.url,
                            addedAt = m.addedAt,
                            lastReadChapterId = m.lastReadChapterId,
                            lastReadAt = m.lastReadAt,
                            totalChapters = m.totalChapters,
                            readCount = m.readCount,
                        )
                    )
                    mangaCount++
                }

                var progressCount = 0
                for (p in backup.progress) {
                    db.progressDao().upsert(
                        ReadingProgress(
                            mangaId = p.mangaId,
                            chapterId = p.chapterId,
                            provider = p.provider,
                            page = p.page,
                            totalPages = p.totalPages,
                            readAt = p.readAt,
                            completed = p.completed,
                        )
                    )
                    progressCount++
                }

                if (mangaCount > 0 || progressCount > 0) {
                    com.mangadl.android.data.sync.supabase.SupabaseSyncManager.syncAllAsync()
                }

                return@withContext BackupRestoreResult(
                    success = true,
                    sourceFormat = "Manga-DL (.mangadl / JSON)",
                    restoredMangaCount = mangaCount,
                    restoredProgressCount = progressCount,
                    restoredCategoriesCount = backup.categories.size,
                    restoredTrackerBindsCount = backup.trackerBinds.size,
                )
            }

            // Otherwise, decode using Tachiyomi universal decoder (handles .tachibk protobuf and Tachiyomi JSON)
            val parsed = TachiyomiBackupDecoder.decode(rawBytes, fileName ?: "")
            var restoredManga = 0
            var restoredProgress = 0

            for (m in parsed.manga) {
                if (m.title.isBlank()) continue
                val provider = TachiyomiBackupDecoder.resolveProvider(m.sourceName, m.url)
                val mangaId = TachiyomiBackupDecoder.cleanMangaId(m.url, provider)

                val readChapters = m.chapters.filter { it.read || it.lastPageRead > 0 }
                val readCount = readChapters.size
                val totalChapters = m.chapters.size

                val lastRead = readChapters.maxByOrNull { it.sourceOrder }

                db.libraryDao().upsert(
                    LibraryManga(
                        id = mangaId,
                        title = m.title,
                        coverUrl = m.thumbnailUrl ?: "",
                        provider = provider,
                        url = m.url,
                        addedAt = System.currentTimeMillis(),
                        lastReadChapterId = lastRead?.let { TachiyomiBackupDecoder.cleanChapterId(it.url, it.chapterNumber) },
                        lastReadAt = if (lastRead != null) System.currentTimeMillis() else null,
                        totalChapters = totalChapters,
                        readCount = readCount,
                    )
                )
                restoredManga++

                for (ch in readChapters) {
                    val chapterId = TachiyomiBackupDecoder.cleanChapterId(ch.url, ch.chapterNumber)
                    db.progressDao().upsert(
                        ReadingProgress(
                            mangaId = mangaId,
                            chapterId = chapterId,
                            provider = provider,
                            page = ch.lastPageRead.toInt(),
                            totalPages = 0,
                            readAt = System.currentTimeMillis(),
                            completed = ch.read,
                        )
                    )
                    restoredProgress++
                }
            }

            val isBinary = rawBytes.size >= 2 && rawBytes[0] == 0x1f.toByte()
            val formatName = if (isBinary) "Tachiyomi Protobuf (.tachibk)" else "Tachiyomi JSON"

            if (restoredManga > 0 || restoredProgress > 0) {
                com.mangadl.android.data.sync.supabase.SupabaseSyncManager.syncAllAsync()
            }

            return@withContext BackupRestoreResult(
                success = true,
                sourceFormat = formatName,
                restoredMangaCount = restoredManga,
                restoredProgressCount = restoredProgress,
                restoredCategoriesCount = parsed.categories.size,
            )
        } catch (e: Exception) {
            return@withContext BackupRestoreResult(
                success = false,
                sourceFormat = "Unknown",
                restoredMangaCount = 0,
                restoredProgressCount = 0,
                restoredCategoriesCount = 0,
                errorMessage = e.message ?: "Failed to parse backup",
            )
        }
    }

    /**
     * Create an Android Share Sheet Intent for an exported backup file.
     */
    fun createShareIntent(context: Context, file: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "manga-dl Backup")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun extractChapterNumber(chapterId: String): Double {
        val match = Regex("""(?:chapter|ch)[-_ ]*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE).find(chapterId)
            ?: Regex("""(\d+(?:\.\d+)?)""").find(chapterId)
        return match?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    }
}

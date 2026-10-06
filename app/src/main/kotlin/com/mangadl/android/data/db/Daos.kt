package com.mangadl.android.data.db

import androidx.room.*
import com.mangadl.android.data.model.DownloadEntry
import com.mangadl.android.data.model.LibraryManga
import com.mangadl.android.data.model.NewChapterEntry
import com.mangadl.android.data.model.ReadingProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Query("SELECT * FROM library ORDER BY addedAt DESC")
    fun getAll(): Flow<List<LibraryManga>>

    @Query("SELECT * FROM library WHERE id = :id")
    suspend fun getById(id: String): LibraryManga?

    @Query("SELECT EXISTS(SELECT 1 FROM library WHERE id = :id)")
    fun isInLibrary(id: String): Flow<Boolean>

    @Upsert
    suspend fun upsert(manga: LibraryManga)

    @Query("DELETE FROM library WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE library SET lastReadChapterId = :chapterId, lastReadAt = :readAt WHERE id = :mangaId")
    suspend fun updateLastRead(mangaId: String, chapterId: String, readAt: Long)

    @Query("UPDATE library SET readCount = :count WHERE id = :mangaId")
    suspend fun updateReadCount(mangaId: String, count: Int)

    @Query("UPDATE library SET totalChapters = :count WHERE id = :id")
    suspend fun updateTotalChapters(id: String, count: Int)

    @Query("SELECT * FROM library")
    suspend fun getAllOnce(): List<LibraryManga>

    @Query("DELETE FROM library")
    suspend fun deleteAll()
}


@Dao
interface ProgressDao {
    @Query("SELECT * FROM reading_progress WHERE mangaId = :mangaId ORDER BY readAt DESC")
    fun getForManga(mangaId: String): Flow<List<ReadingProgress>>

    @Query("SELECT * FROM reading_progress ORDER BY readAt DESC LIMIT :limit")
    fun getRecent(limit: Int = 200): Flow<List<ReadingProgress>>

    @Query("SELECT * FROM reading_progress WHERE mangaId = :mangaId AND chapterId = :chapterId")
    suspend fun get(mangaId: String, chapterId: String): ReadingProgress?

    @Upsert
    suspend fun upsert(progress: ReadingProgress)

    @Query("SELECT COUNT(*) FROM reading_progress WHERE mangaId = :mangaId AND completed = 1")
    suspend fun countCompleted(mangaId: String): Int

    @Query("DELETE FROM reading_progress")
    suspend fun deleteAll()
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_queue ORDER BY addedAt DESC")
    fun getAll(): Flow<List<DownloadEntry>>

    @Query("SELECT * FROM download_queue WHERE status = 'queued' OR status = 'downloading' ORDER BY addedAt ASC")
    suspend fun getPending(): List<DownloadEntry>

    @Query("SELECT * FROM download_queue WHERE id = :id")
    suspend fun getById(id: String): DownloadEntry?

    @Upsert
    suspend fun upsert(entry: DownloadEntry)

    @Query("UPDATE download_queue SET status = :status, progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: String, status: String, progress: Int)

    @Query("UPDATE download_queue SET totalPages = :totalPages WHERE id = :id")
    suspend fun updateTotalPages(id: String, totalPages: Int)

    @Query("UPDATE download_queue SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE download_queue SET status = 'completed', completedAt = :completedAt, filePath = :filePath, progress = totalPages WHERE id = :id")
    suspend fun markCompleted(id: String, completedAt: Long, filePath: String)

    @Query("SELECT chapterId FROM download_queue WHERE mangaId = :mangaId AND status = 'completed'")
    fun getDownloadedChapterIds(mangaId: String): Flow<List<String>>

    @Query("DELETE FROM download_queue WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM download_queue")
    suspend fun deleteAll()
}

@Dao
interface UpdatesDao {
    @Query("SELECT * FROM new_chapters ORDER BY detectedAt DESC LIMIT 200")
    fun getAll(): Flow<List<NewChapterEntry>>

    @Upsert
    suspend fun upsert(entries: List<NewChapterEntry>)

    @Query("DELETE FROM new_chapters WHERE mangaId = :mangaId")
    suspend fun clearForManga(mangaId: String)

    @Query("DELETE FROM new_chapters")
    suspend fun deleteAll()
}

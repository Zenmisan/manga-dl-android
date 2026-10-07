package com.mangadl.android.data.sync.supabase

import android.content.Context
import android.util.Log
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.auth.SupabaseManager
import com.mangadl.android.data.model.LibraryManga
import com.mangadl.android.data.model.ReadingProgress
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object SupabaseSyncManager {
    private const val TAG = "SupabaseSync"

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val supabase get() = SupabaseManager.client
    private val db get() = MangaDlApp.instance.database
    private val libraryDao get() = db.libraryDao()
    private val progressDao get() = db.progressDao()

    fun syncAllAsync() {
        appScope.launch {
            syncAll()
        }
    }

    fun syncMangaSubscriptionAsync(manga: LibraryManga, subscribed: Boolean) {
        appScope.launch {
            syncMangaSubscription(manga, subscribed)
        }
    }

    fun syncChapterReadAsync(
        provider: String,
        mangaId: String,
        chapterId: String,
        page: Int = 0,
        totalPages: Int = 0,
        mangaTitle: String? = null,
        chapterTitle: String? = null,
        isCompleted: Boolean = true,
    ) {
        appScope.launch {
            syncChapterRead(
                provider = provider,
                mangaId = mangaId,
                chapterId = chapterId,
                page = page,
                totalPages = totalPages,
                mangaTitle = mangaTitle,
                chapterTitle = chapterTitle,
                isCompleted = isCompleted,
            )
        }
    }

    private fun currentIsoTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    /**
     * Pulls the user's library and read tracking from Supabase and merges into Room SQLite.
     * Safe to call on app startup and login.
     */
    suspend fun pullFromCloud(): Result<Unit> = withContext(Dispatchers.IO) {
        val user = supabase.auth.currentUserOrNull()
            ?: return@withContext Result.failure(IllegalStateException("No authenticated user"))
        val userId = user.id

        runCatching {
            pullLibrary(userId)
            pullReadTracking(userId)
            Log.d(TAG, "Successfully pulled cloud library and progress for user $userId")
            Unit
        }.onFailure { e ->
            Log.w(TAG, "Pull from cloud failed: ${e.message}", e)
        }
    }

    /**
     * Pushes all local library entries and completed reading progress to Supabase.
     */
    suspend fun pushToCloud(): Result<Unit> = withContext(Dispatchers.IO) {
        val user = supabase.auth.currentUserOrNull()
            ?: return@withContext Result.failure(IllegalStateException("No authenticated user"))
        val userId = user.id

        runCatching {
            pushAllLibrary(userId)
            pushAllReadTracking(userId)
            Log.d(TAG, "Successfully pushed local library and progress to cloud for user $userId")
            Unit
        }.onFailure { e ->
            Log.w(TAG, "Push to cloud failed: ${e.message}", e)
        }
    }

    /**
     * Performs a full bidirectional sync: pulls latest remote state, then pushes local changes.
     */
    suspend fun syncAll(): Result<Unit> = withContext(Dispatchers.IO) {
        val user = supabase.auth.currentUserOrNull()
            ?: return@withContext Result.failure(IllegalStateException("No authenticated user"))
        val userId = user.id

        runCatching {
            pullLibrary(userId)
            pullReadTracking(userId)
            pushAllLibrary(userId)
            pushAllReadTracking(userId)
            Log.d(TAG, "Full cloud sync completed for user $userId")
            Unit
        }.onFailure { e ->
            Log.w(TAG, "Cloud sync failed: ${e.message}", e)
        }
    }

    /**
     * Syncs a single manga library toggle (added or removed) to Supabase.
     */
    suspend fun syncMangaSubscription(manga: LibraryManga, subscribed: Boolean) = withContext(Dispatchers.IO) {
        val user = supabase.auth.currentUserOrNull() ?: return@withContext
        val userId = user.id

        runCatching {
            val recordId = "${manga.provider}:${manga.id}:$userId"
            val record = SupabaseMangaRecord(
                id = recordId,
                provider = manga.provider,
                providerMangaId = manga.id,
                title = manga.title,
                coverUrl = manga.coverUrl,
                url = manga.url,
                subscribed = subscribed,
                userId = userId,
                lastSynced = currentIsoTime(),
            )
            supabase.from("manga").upsert(record)
            Log.d(TAG, "Synced subscription for ${manga.title} (subscribed=$subscribed)")
        }.onFailure { e ->
            Log.w(TAG, "Failed to sync subscription for ${manga.title}: ${e.message}")
        }
    }

    /**
     * Syncs a completed or progress-updated chapter to Supabase `read_tracking` and `reading_progress`.
     */
    suspend fun syncChapterRead(
        provider: String,
        mangaId: String,
        chapterId: String,
        page: Int = 0,
        totalPages: Int = 0,
        mangaTitle: String? = null,
        chapterTitle: String? = null,
        isCompleted: Boolean = true,
    ) = withContext(Dispatchers.IO) {
        val user = supabase.auth.currentUserOrNull() ?: return@withContext
        val userId = user.id

        runCatching {
            val now = currentIsoTime()

            // 1. Update detailed reading_progress
            val progressRecord = SupabaseReadingProgressRecord(
                userId = userId,
                provider = provider,
                mangaId = mangaId,
                chapterId = chapterId,
                lastPage = page,
                mangaTitle = mangaTitle,
                chapterTitle = chapterTitle,
                updatedAt = now,
            )
            supabase.from("reading_progress").upsert(progressRecord)

            // 2. If chapter is completed, append to read_tracking chapter_ids array
            if (isCompleted) {
                // Fetch existing chapter_ids or start fresh
                val existing = runCatching {
                    supabase.from("read_tracking")
                        .select {
                            filter {
                                eq("user_id", userId)
                                eq("provider", provider)
                                eq("manga_id", mangaId)
                            }
                        }
                        .decodeList<SupabaseReadTrackingRecord>()
                        .firstOrNull()
                }.getOrNull()

                val currentIds = existing?.chapterIds?.toMutableSet() ?: mutableSetOf()
                currentIds.add(chapterId)

                val trackingRecord = SupabaseReadTrackingRecord(
                    userId = userId,
                    provider = provider,
                    mangaId = mangaId,
                    chapterIds = currentIds.toList(),
                    updatedAt = now,
                )
                supabase.from("read_tracking").upsert(trackingRecord)
            }
            Log.d(TAG, "Synced chapter progress: $mangaId / $chapterId")
        }.onFailure { e ->
            Log.w(TAG, "Failed to sync chapter read for $chapterId: ${e.message}")
        }
    }

    private suspend fun pullLibrary(userId: String) {
        val remoteManga = supabase.from("manga")
            .select {
                filter {
                    eq("user_id", userId)
                    eq("subscribed", true)
                }
            }
            .decodeList<SupabaseMangaRecord>()

        for (remote in remoteManga) {
            val existing = libraryDao.getById(remote.providerMangaId)
            if (existing == null) {
                libraryDao.upsert(
                    LibraryManga(
                        id = remote.providerMangaId,
                        title = remote.title,
                        coverUrl = remote.coverUrl ?: "",
                        provider = remote.provider,
                        url = remote.url,
                    )
                )
            }
        }
    }

    private suspend fun pullReadTracking(userId: String) {
        val trackingList = supabase.from("read_tracking")
            .select {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<SupabaseReadTrackingRecord>()

        for (entry in trackingList) {
            for (chId in entry.chapterIds) {
                val existing = progressDao.get(entry.mangaId, chId)
                if (existing == null || !existing.completed) {
                    progressDao.upsert(
                        ReadingProgress(
                            mangaId = entry.mangaId,
                            chapterId = chId,
                            provider = entry.provider,
                            page = existing?.page ?: 1,
                            totalPages = existing?.totalPages ?: 1,
                            completed = true,
                        )
                    )
                }
            }
        }
    }

    private suspend fun pushAllLibrary(userId: String) {
        val localList = libraryDao.getAllOnce()
        val now = currentIsoTime()
        for (local in localList) {
            val recordId = "${local.provider}:${local.id}:$userId"
            val record = SupabaseMangaRecord(
                id = recordId,
                provider = local.provider,
                providerMangaId = local.id,
                title = local.title,
                coverUrl = local.coverUrl,
                url = local.url,
                subscribed = true,
                userId = userId,
                lastSynced = now,
            )
            runCatching {
                supabase.from("manga").upsert(record)
            }
        }
    }

    private suspend fun pushAllReadTracking(userId: String) {
        val progressList = runCatching { progressDao.getRecentOnce(1000) }.getOrDefault(emptyList())
        val completedByManga = progressList.filter { it.completed }.groupBy { "${it.provider}:::${it.mangaId}" }
        val now = currentIsoTime()

        for (entry in completedByManga.entries) {
            val parts = entry.key.split(":::")
            if (parts.size != 2) continue
            val provider = parts[0]
            val mangaId = parts[1]
            val chapterIds = entry.value.map { it.chapterId }.distinct()

            val record = SupabaseReadTrackingRecord(
                userId = userId,
                provider = provider,
                mangaId = mangaId,
                chapterIds = chapterIds,
                updatedAt = now,
            )
            runCatching {
                supabase.from("read_tracking").upsert(record)
            }
        }
    }
}

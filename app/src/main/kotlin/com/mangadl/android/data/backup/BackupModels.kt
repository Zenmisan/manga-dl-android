package com.mangadl.android.data.backup

import kotlinx.serialization.Serializable

/**
 * Universal Manga-DL backup schema.
 * Represents complete library, reading history, and category associations.
 */
@Serializable
data class MangaDlBackup(
    val version: String = "1.0",
    val app: String = "manga-dl",
    val exportedAt: Long = System.currentTimeMillis(),
    val library: List<BackupMangaEntry> = emptyList(),
    val progress: List<BackupProgressEntry> = emptyList(),
    val categories: List<String> = emptyList(),
    val mangaCategories: Map<String, List<String>> = emptyMap(),
)

@Serializable
data class BackupMangaEntry(
    val id: String,
    val title: String,
    val coverUrl: String = "",
    val provider: String,
    val url: String = "",
    val addedAt: Long = System.currentTimeMillis(),
    val lastReadChapterId: String? = null,
    val lastReadAt: Long? = null,
    val totalChapters: Int = 0,
    val readCount: Int = 0,
    val categories: List<String> = emptyList(),
)

@Serializable
data class BackupProgressEntry(
    val mangaId: String,
    val chapterId: String,
    val provider: String,
    val page: Int = 0,
    val totalPages: Int = 0,
    val readAt: Long = System.currentTimeMillis(),
    val completed: Boolean = false,
)

/**
 * Outcome of a backup restore operation.
 */
data class BackupRestoreResult(
    val success: Boolean,
    val sourceFormat: String, // "Manga-DL JSON", "Tachiyomi Protobuf (.tachibk)", "Tachiyomi JSON"
    val restoredMangaCount: Int,
    val restoredProgressCount: Int,
    val restoredCategoriesCount: Int,
    val errorMessage: String? = null,
)

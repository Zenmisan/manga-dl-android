package com.mangadl.android.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
data class MangaSearchResult(
    val id: String = "",
    val title: String = "",
    val coverUrl: String = "",
    val provider: String = "",
    val url: String = "",
)

@Serializable
data class MangaDetail(
    val id: String = "",
    val title: String = "",
    val coverUrl: String = "",
    val description: String = "",
    val status: String = "",
    val genres: List<String> = emptyList(),
    val authors: List<String> = emptyList(),
    val provider: String = "",
    val url: String = "",
    val chapters: List<Chapter> = emptyList(),
)

@Serializable
data class Chapter(
    val id: String = "",
    val title: String = "",
    val number: Float = 0f,
    val publishedAt: String = "",
)

@Entity(tableName = "library")
data class LibraryManga(
    @PrimaryKey val id: String,
    val title: String,
    val coverUrl: String,
    val provider: String,
    val url: String,
    val addedAt: Long = System.currentTimeMillis(),
    val lastReadChapterId: String? = null,
    val lastReadAt: Long? = null,
    val totalChapters: Int = 0,
    val readCount: Int = 0,
)

@Entity(tableName = "reading_progress", primaryKeys = ["mangaId", "chapterId"])
data class ReadingProgress(
    val mangaId: String,
    val chapterId: String,
    val provider: String,
    val page: Int = 0,
    val totalPages: Int = 0,
    val readAt: Long = System.currentTimeMillis(),
    val completed: Boolean = false,
)

@Entity(tableName = "download_queue")
data class DownloadEntry(
    @PrimaryKey val id: String,
    val mangaId: String,
    val mangaTitle: String,
    val chapterId: String,
    val chapterTitle: String,
    val provider: String,
    val status: String = "queued",
    val progress: Int = 0,
    val totalPages: Int = 0,
    val addedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val filePath: String? = null,
)

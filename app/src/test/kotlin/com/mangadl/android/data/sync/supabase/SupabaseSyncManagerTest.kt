package com.mangadl.android.data.sync.supabase

import com.mangadl.android.data.model.LibraryManga
import com.mangadl.android.data.model.ReadingProgress
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseSyncManagerTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun testSupabaseMangaRecordSerialization() {
        val record = SupabaseMangaRecord(
            id = "mangadex:12345:user-abc",
            provider = "mangadex",
            providerMangaId = "12345",
            title = "Solo Leveling",
            coverUrl = "https://example.com/cover.jpg",
            url = "https://example.com/manga/12345",
            subscribed = true,
            userId = "user-abc",
            lastSynced = "2026-10-07T00:00:00Z",
        )

        val serialized = json.encodeToString(record)
        assertTrue(serialized.contains("\"provider_manga_id\":\"12345\""))
        assertTrue(serialized.contains("\"cover_url\":\"https://example.com/cover.jpg\""))
        assertTrue(serialized.contains("\"subscribed\":true"))

        val deserialized = json.decodeFromString<SupabaseMangaRecord>(serialized)
        assertEquals(record.id, deserialized.id)
        assertEquals(record.provider, deserialized.provider)
        assertEquals(record.providerMangaId, deserialized.providerMangaId)
        assertEquals(record.title, deserialized.title)
        assertEquals(record.subscribed, deserialized.subscribed)
    }

    @Test
    fun testSupabaseReadTrackingRecordSerialization() {
        val record = SupabaseReadTrackingRecord(
            userId = "user-abc",
            provider = "asura",
            mangaId = "solo-leveling",
            chapterIds = listOf("ch-1", "ch-2", "ch-3"),
            updatedAt = "2026-10-07T00:00:00Z",
        )

        val serialized = json.encodeToString(record)
        assertTrue(serialized.contains("\"user_id\":\"user-abc\""))
        assertTrue(serialized.contains("\"chapter_ids\":[\"ch-1\",\"ch-2\",\"ch-3\"]"))

        val deserialized = json.decodeFromString<SupabaseReadTrackingRecord>(serialized)
        assertEquals("user-abc", deserialized.userId)
        assertEquals("asura", deserialized.provider)
        assertEquals("solo-leveling", deserialized.mangaId)
        assertEquals(3, deserialized.chapterIds.size)
        assertEquals("ch-2", deserialized.chapterIds[1])
    }

    @Test
    fun testSupabaseReadingProgressRecordSerialization() {
        val record = SupabaseReadingProgressRecord(
            userId = "user-abc",
            provider = "royalroad",
            mangaId = "mother-of-learning",
            chapterId = "chapter-1",
            lastPage = 42,
            mangaTitle = "Mother of Learning",
            chapterTitle = "Chapter 1: Good Morning, Brother",
            updatedAt = "2026-10-07T00:00:00Z",
        )

        val serialized = json.encodeToString(record)
        assertTrue(serialized.contains("\"last_page\":42"))
        assertTrue(serialized.contains("\"manga_title\":\"Mother of Learning\""))

        val deserialized = json.decodeFromString<SupabaseReadingProgressRecord>(serialized)
        assertEquals(42, deserialized.lastPage)
        assertEquals("Mother of Learning", deserialized.mangaTitle)
        assertEquals("Chapter 1: Good Morning, Brother", deserialized.chapterTitle)
    }

    @Test
    fun testCompletedProgressAggregation() {
        val progressList = listOf(
            ReadingProgress("manga-1", "ch-1", "mangadex", 10, 10, completed = true),
            ReadingProgress("manga-1", "ch-2", "mangadex", 15, 15, completed = true),
            ReadingProgress("manga-1", "ch-3", "mangadex", 5, 20, completed = false), // in progress
            ReadingProgress("manga-2", "ch-10", "novelbin", 100, 100, completed = true),
        )

        val completedByManga = progressList.filter { it.completed }.groupBy { "${it.provider}:::${it.mangaId}" }
        assertEquals(2, completedByManga.size)

        val mangadexItems = completedByManga["mangadex:::manga-1"]
        assertNotNull(mangadexItems)
        assertEquals(2, mangadexItems!!.size)
        val chapterIds = mangadexItems.map { it.chapterId }
        assertEquals(listOf("ch-1", "ch-2"), chapterIds)

        val novelbinItems = completedByManga["novelbin:::manga-2"]
        assertNotNull(novelbinItems)
        assertEquals(listOf("ch-10"), novelbinItems!!.map { it.chapterId })
    }

    @Test
    fun testLocalToRemoteMangaMapping() {
        val local = LibraryManga(
            id = "re-zero",
            title = "Re:Zero Starting Life in Another World",
            coverUrl = "https://example.com/rezero.jpg",
            provider = "novelbin",
            url = "https://novelbin.me/novel-book/re-zero",
            totalChapters = 500,
            readCount = 50,
        )

        val userId = "test-user-id"
        val remoteRecord = SupabaseMangaRecord(
            id = "${local.provider}:${local.id}:$userId",
            provider = local.provider,
            providerMangaId = local.id,
            title = local.title,
            coverUrl = local.coverUrl,
            url = local.url,
            subscribed = true,
            userId = userId,
        )

        assertEquals("novelbin:re-zero:test-user-id", remoteRecord.id)
        assertEquals("novelbin", remoteRecord.provider)
        assertEquals("re-zero", remoteRecord.providerMangaId)
        assertEquals("Re:Zero Starting Life in Another World", remoteRecord.title)
        assertTrue(remoteRecord.subscribed)
    }
}

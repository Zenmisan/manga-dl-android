package com.mangadl.android.data.backup

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPOutputStream

class BackupEngineTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun testProviderResolverMappings() {
        assertEquals("asurascans", TachiyomiBackupDecoder.resolveProvider("Asura Scans"))
        assertEquals("flamecomics", TachiyomiBackupDecoder.resolveProvider("Flame Comics"))
        assertEquals("mangadex", TachiyomiBackupDecoder.resolveProvider("MangaDex"))
        assertEquals("bato", TachiyomiBackupDecoder.resolveProvider("Bato.to"))
        assertEquals("royalroad", TachiyomiBackupDecoder.resolveProvider("Royal Road"))
        assertEquals("novelbin", TachiyomiBackupDecoder.resolveProvider("NovelBin (EN)"))
        assertEquals("webtoons", TachiyomiBackupDecoder.resolveProvider("WEBTOON"))
        assertEquals("mangapill", TachiyomiBackupDecoder.resolveProvider("MangaPill"))
        assertEquals("customsource", TachiyomiBackupDecoder.resolveProvider("Custom Source!"))
    }

    @Test
    fun testMangaAndChapterIdCleaning() {
        assertEquals(
            "a9667631-b79e-4c74-8b1b-26a91c107248",
            TachiyomiBackupDecoder.cleanMangaId(
                "https://mangadex.org/title/a9667631-b79e-4c74-8b1b-26a91c107248/solo-leveling",
                "mangadex"
            )
        )
        assertEquals(
            "solo-leveling",
            TachiyomiBackupDecoder.cleanMangaId("https://asuracomics.com/manga/solo-leveling/")
        )
        assertEquals(
            "chapter-150",
            TachiyomiBackupDecoder.cleanChapterId("/manga/solo-leveling/chapter-150/")
        )
        assertEquals(
            "42.5",
            TachiyomiBackupDecoder.cleanChapterId("", 42.5f)
        )
        assertEquals(
            "10",
            TachiyomiBackupDecoder.cleanChapterId("", 10f)
        )
    }

    @Test
    fun testTachiyomiJsonDecoding() {
        val sampleJson = """
        {
          "backupCategories": [
            { "name": "Favorites", "order": 0 },
            { "name": "Reading", "order": 1 }
          ],
          "backupSources": [
            { "name": "Asura Scans", "sourceId": 12345 }
          ],
          "backupManga": [
            {
              "source": 12345,
              "url": "/manga/omniscient-reader",
              "title": "Omniscient Reader's Viewpoint",
              "artist": "Sleepy-C",
              "author": "sing N song",
              "description": "Dokja was an ordinary reader...",
              "thumbnailUrl": "https://example.com/cover.jpg",
              "categories": [1],
              "chapters": [
                {
                  "url": "/ch-1",
                  "name": "Chapter 1",
                  "read": true,
                  "lastPageRead": 25,
                  "chapterNumber": 1.0,
                  "sourceOrder": 1
                },
                {
                  "url": "/ch-2",
                  "name": "Chapter 2",
                  "read": false,
                  "lastPageRead": 0,
                  "chapterNumber": 2.0,
                  "sourceOrder": 2
                }
              ],
              "tracking": [
                {
                  "syncId": 1,
                  "mediaId": 99999,
                  "title": "Omniscient Reader",
                  "lastChapterRead": 1.0,
                  "score": 9.5,
                  "status": 1
                }
              ]
            }
          ]
        }
        """.trimIndent()

        val parsed = TachiyomiBackupDecoder.decode(sampleJson.toByteArray(Charsets.UTF_8), "test.json")
        assertEquals(1, parsed.manga.size)
        assertEquals(2, parsed.categories.size)
        assertEquals(1, parsed.sources.size)

        val m = parsed.manga[0]
        assertEquals("Omniscient Reader's Viewpoint", m.title)
        assertEquals("Asura Scans", m.sourceName)
        assertEquals(listOf("Favorites"), m.categoryNames)
        assertEquals(2, m.chapters.size)
        assertTrue(m.chapters[0].read)
        assertEquals(25L, m.chapters[0].lastPageRead)
        assertEquals(1, m.tracking.size)
        assertEquals(99999L, m.tracking[0].mediaId)
    }

    @Test
    fun testMangaDlBackupRoundTrip() {
        val original = MangaDlBackup(
            version = "1.0",
            app = "manga-dl",
            exportedAt = 1700000000000L,
            library = listOf(
                BackupMangaEntry(
                    id = "solo-leveling",
                    title = "Solo Leveling",
                    coverUrl = "https://example.com/solo.jpg",
                    provider = "asurascans",
                    url = "/manga/solo-leveling",
                    totalChapters = 200,
                    readCount = 150,
                )
            ),
            progress = listOf(
                BackupProgressEntry(
                    mangaId = "solo-leveling",
                    chapterId = "chapter-150",
                    provider = "asurascans",
                    page = 30,
                    totalPages = 30,
                    completed = true,
                )
            ),
            categories = listOf("Manhwa", "Favorites"),
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<MangaDlBackup>(encoded)

        assertEquals("manga-dl", decoded.app)
        assertEquals(1, decoded.library.size)
        assertEquals("Solo Leveling", decoded.library[0].title)
        assertEquals(1, decoded.progress.size)
        assertTrue(decoded.progress[0].completed)
        assertEquals(2, decoded.categories.size)
    }

    @Test
    fun testTachiyomiProtobufWireFormatDecoding() {
        // Construct a synthetic protobuf binary message representing BackupManga
        // Root message:
        // field 1 (BackupManga) = length-delimited bytes:
        //   field 2 (url) = string "/manga/return-of-the-mount-hua-sect"
        //   field 3 (title) = string "Return of the Mount Hua Sect"
        //   field 100 (favorite) = varint 1
        //   field 16 (BackupChapter) = length-delimited bytes:
        //     field 1 (url) = string "/ch-1"
        //     field 2 (name) = string "Episode 1"
        //     field 4 (read) = varint 1

        val chapterStream = ByteArrayOutputStream()
        writeStringField(chapterStream, 1, "/ch-1")
        writeStringField(chapterStream, 2, "Episode 1")
        writeVarintField(chapterStream, 4, 1)
        val chapterBytes = chapterStream.toByteArray()

        val mangaStream = ByteArrayOutputStream()
        writeVarintField(mangaStream, 1, 999) // source
        writeStringField(mangaStream, 2, "/manga/mount-hua")
        writeStringField(mangaStream, 3, "Return of the Mount Hua Sect")
        writeVarintField(mangaStream, 100, 1) // favorite
        writeLengthDelimitedField(mangaStream, 16, chapterBytes) // chapters
        val mangaBytes = mangaStream.toByteArray()

        val rootStream = ByteArrayOutputStream()
        writeLengthDelimitedField(rootStream, 1, mangaBytes)
        val rootBytes = rootStream.toByteArray()

        // 1. Test uncompressed protobuf
        val decoded = TachiyomiBackupDecoder.decode(rootBytes, "backup.tachibk")
        assertEquals(1, decoded.manga.size)
        val m = decoded.manga[0]
        assertEquals("Return of the Mount Hua Sect", m.title)
        assertEquals("/manga/mount-hua", m.url)
        assertEquals(1, m.chapters.size)
        assertEquals("Episode 1", m.chapters[0].name)
        assertTrue(m.chapters[0].read)

        // 2. Test gzip-compressed protobuf (standard .tachibk format)
        val gzipStream = ByteArrayOutputStream()
        GZIPOutputStream(gzipStream).use { it.write(rootBytes) }
        val gzippedBytes = gzipStream.toByteArray()

        val decodedGzip = TachiyomiBackupDecoder.decode(gzippedBytes, "backup.tachibk")
        assertEquals(1, decodedGzip.manga.size)
        assertEquals("Return of the Mount Hua Sect", decodedGzip.manga[0].title)
        assertEquals(1, decodedGzip.manga[0].chapters.size)
    }

    // --- Helper functions to build test protobuf bytes ---

    private fun writeVarint(out: ByteArrayOutputStream, value: Long) {
        var v = value
        while (v and 0x7FL.inv() != 0L) {
            out.write(((v and 0x7F) or 0x80).toInt())
            v = v ushr 7
        }
        out.write((v and 0x7F).toInt())
    }

    private fun writeVarintField(out: ByteArrayOutputStream, fieldNum: Int, value: Long) {
        val tag = (fieldNum shl 3) or 0
        writeVarint(out, tag.toLong())
        writeVarint(out, value)
    }

    private fun writeStringField(out: ByteArrayOutputStream, fieldNum: Int, value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        writeLengthDelimitedField(out, fieldNum, bytes)
    }

    private fun writeLengthDelimitedField(out: ByteArrayOutputStream, fieldNum: Int, bytes: ByteArray) {
        val tag = (fieldNum shl 3) or 2
        writeVarint(out, tag.toLong())
        writeVarint(out, bytes.size.toLong())
        out.write(bytes)
    }
}

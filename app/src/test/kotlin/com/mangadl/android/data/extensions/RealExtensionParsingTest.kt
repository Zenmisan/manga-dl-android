package com.mangadl.android.data.extensions

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class RealExtensionParsingTest {

    @Test
    fun testIsNovelSourceAll23Sources() {
        val expectedNovelSources = listOf(
            "royalroad", "novelbin", "novelfull", "freewebnovel", "novelfire", "allnovel",
            "novelphoenix", "readnovelfull", "libread", "brightnovel", "chrysanthemumgarden",
            "comrademao", "lightnoveltranslations", "bestlightnovel", "asianovel", "novelbuddy",
            "readlightnovel", "scribblehub", "lightnovelworld", "wuxiaworld", "ranobes",
            "novelsonline", "readhive"
        )

        assertEquals("Should define exactly 23 novel extension IDs", 23, ExtensionManager.NOVEL_EXTENSION_IDS.size)

        for (id in expectedNovelSources) {
            assertTrue("Expected $id to be classified as novel source", ExtensionManager.isNovelSource(id))
            assertTrue("Expected case-insensitive match for ${id.uppercase()}", ExtensionManager.isNovelSource(id.uppercase()))
        }

        // Manga sources should return false
        val mangaSources = listOf("mangadex", "asurascans", "flamecomics", "bato", "mangakakalot", "tcbscans")
        for (id in mangaSources) {
            assertFalse("Expected $id to NOT be a novel source", ExtensionManager.isNovelSource(id))
        }
    }

    @Test
    fun testParseMetaCommentAgainstBundledExtensions() {
        val extensionsDir = listOf(
            File("src/main/assets/extensions"),
            File("app/src/main/assets/extensions")
        ).firstOrNull { it.exists() }

        assertNotNull("Bundled extensions directory must exist", extensionsDir)
        val jsFiles = extensionsDir!!.listFiles { _, name -> name.endsWith(".js") } ?: emptyArray()
        assertTrue("Should have bundled extension JS files", jsFiles.isNotEmpty())

        var verifiedCount = 0
        for (file in jsFiles) {
            val script = file.readText()
            if (script.contains("// ==Extension==")) {
                val id = file.nameWithoutExtension
                val meta = ExtensionManager.parseMetaComment(id, script)
                assertNotNull("Meta comment should parse", meta)
                assertEquals(id, meta["id"])
                assertTrue("Extension ${file.name} must have a name", meta["name"]?.isNotBlank() == true)
                assertTrue("Extension ${file.name} must have a version", meta["version"]?.isNotBlank() == true)
                verifiedCount++
            }
        }

        assertTrue("Verified metadata for at least 10 bundled extensions", verifiedCount >= 10)
    }

    @Test
    fun testParseSearchResultsProduction() {
        val json = """
            [
              {
                "id": "one-piece",
                "title": "One Piece",
                "cover_url": "https://img.example.com/op.jpg",
                "provider": "mangadex",
                "url": "https://mangadex.org/title/op"
              },
              {
                "id": "solo-leveling",
                "title": "Solo Leveling",
                "coverUrl": "/manga/covers/sl.jpg",
                "provider": "asurascans",
                "url": "https://asura.gg/sl"
              }
            ]
        """.trimIndent()

        val results = ExtensionManager.parseSearchResults(json)
        assertEquals(2, results.size)

        val first = results[0]
        assertEquals("one-piece", first.id)
        assertEquals("One Piece", first.title)
        assertEquals("https://img.example.com/op.jpg", first.coverUrl)
        assertEquals("mangadex", first.provider)
        assertEquals("https://mangadex.org/title/op", first.url)

        val second = results[1]
        assertEquals("solo-leveling", second.id)
        assertEquals("Solo Leveling", second.title)
        // Relative coverUrl should be resolved
        assertTrue("Relative URL should be resolved with backend prefix", second.coverUrl.startsWith("http"))
        assertTrue("Should resolve relative path", second.coverUrl.endsWith("/api/manga/covers/sl.jpg"))

        // Null and empty handling
        assertTrue(ExtensionManager.parseSearchResults(null).isEmpty())
        assertTrue(ExtensionManager.parseSearchResults("").isEmpty())
        assertTrue(ExtensionManager.parseSearchResults("not-json").isEmpty())
    }

    @Test
    fun testParseMangaDetailProduction() {
        val json = """
            {
              "id": "frieren.10",
              "title": "Frieren: Beyond Journey's End",
              "cover_url": "https://img.example.com/frieren.jpg",
              "description": "The adventure is over but life goes on.",
              "status": "Ongoing",
              "genres": ["Adventure", "Drama", "Fantasy"],
              "authors": ["Yamada Kanehito"],
              "provider": "mangadex",
              "url": "https://mangadex.org/title/frieren",
              "chapters": [
                {
                  "id": "ch-130",
                  "title": "Chapter 130",
                  "number": 130.0,
                  "published_at": "2024-05-01"
                },
                {
                  "id": "ch-129.5",
                  "title": "Chapter 129.5: Special",
                  "number": 129.5,
                  "published_at": "2024-04-15"
                }
              ]
            }
        """.trimIndent()

        val detail = ExtensionManager.parseMangaDetail(json)
        assertEquals("frieren.10", detail.id)
        assertEquals("Frieren: Beyond Journey's End", detail.title)
        assertEquals("https://img.example.com/frieren.jpg", detail.coverUrl)
        assertEquals("Ongoing", detail.status)
        assertEquals(listOf("Adventure", "Drama", "Fantasy"), detail.genres)
        assertEquals(listOf("Yamada Kanehito"), detail.authors)
        assertEquals("mangadex", detail.provider)

        assertEquals(2, detail.chapters.size)
        assertEquals("ch-130", detail.chapters[0].id)
        assertEquals(130f, detail.chapters[0].number)
        assertEquals("ch-129.5", detail.chapters[1].id)
        assertEquals(129.5f, detail.chapters[1].number)

        // Error and null handling
        assertEquals("", ExtensionManager.parseMangaDetail(null).id)
        assertEquals("", ExtensionManager.parseMangaDetail("bad json").id)
    }

    @Test
    fun testParsePagesProduction() {
        val json = """
            [
              "https://cdn.example.com/page1.jpg",
              "/manga/asura-descramble?url=https%3A%2F%2Fcdn.example.com%2Fpage2.jpg",
              "https://cdn.example.com/page3.jpg"
            ]
        """.trimIndent()

        val pages = ExtensionManager.parsePages(json)
        assertEquals(3, pages.size)
        assertEquals("https://cdn.example.com/page1.jpg", pages[0])
        assertTrue("Page 2 relative descramble URL should be resolved", pages[1].startsWith("http"))
        assertTrue("Page 2 should contain descramble endpoint", pages[1].contains("/api/manga/asura-descramble"))
        assertEquals("https://cdn.example.com/page3.jpg", pages[2])

        // Edge cases
        assertTrue(ExtensionManager.parsePages(null).isEmpty())
        assertTrue(ExtensionManager.parsePages("").isEmpty())
        assertTrue(ExtensionManager.parsePages("invalid").isEmpty())
    }
}

package com.mangadl.android.data.parsing

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for the JSON parsing helpers mirrored from ExtensionManager:
 * parseSearchResults, parseMangaDetail, parsePages.
 *
 * These parse the JSON returned by extension JS functions.
 */
class ExtensionParsingTest {

    // Mirrors ExtensionManager.parseSearchResults
    data class SearchResult(val id: String, val title: String, val coverUrl: String, val provider: String, val url: String)

    private fun parseSearchResults(json: String?): List<SearchResult> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                SearchResult(
                    id = obj.optString("id"),
                    title = obj.optString("title"),
                    coverUrl = obj.optString("cover_url"),
                    provider = obj.optString("provider"),
                    url = obj.optString("url"),
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    // Mirrors ExtensionManager.parsePages
    private fun parsePages(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i -> arr.optString(i) }
        } catch (_: Exception) { emptyList() }
    }

    // Mirrors ExtensionManager.parseMangaDetail (simplified)
    data class Detail(val id: String, val title: String, val genres: List<String>, val chapters: Int)

    private fun parseMangaDetail(json: String?): Detail? {
        if (json.isNullOrBlank()) return null
        return try {
            val obj = JSONObject(json)
            val chaptersArr = obj.optJSONArray("chapters")
            val chapterCount = chaptersArr?.length() ?: 0
            val genres = obj.optJSONArray("genres")?.let { g ->
                (0 until g.length()).map { g.optString(it) }
            } ?: emptyList()
            Detail(
                id = obj.optString("id"),
                title = obj.optString("title"),
                genres = genres,
                chapters = chapterCount,
            )
        } catch (_: Exception) { null }
    }

    // ── Search Results ───────────────────────────────────────────────────────

    @Test
    fun `parseSearchResults returns correct list`() {
        val json = """[
          {"id":"one-piece","title":"One Piece","cover_url":"https://img.com/op.jpg","provider":"mangakatana","url":"https://mangakatana.com/manga/one-piece"},
          {"id":"naruto","title":"Naruto","cover_url":"","provider":"mangakatana","url":"https://mangakatana.com/manga/naruto"}
        ]"""
        val results = parseSearchResults(json)
        assertEquals(2, results.size)
        assertEquals("one-piece", results[0].id)
        assertEquals("One Piece", results[0].title)
        assertEquals("https://img.com/op.jpg", results[0].coverUrl)
        assertEquals("naruto", results[1].id)
    }

    @Test
    fun `parseSearchResults on empty array returns empty list`() {
        assertEquals(emptyList<SearchResult>(), parseSearchResults("[]"))
    }

    @Test
    fun `parseSearchResults on null returns empty list`() {
        assertEquals(emptyList<SearchResult>(), parseSearchResults(null))
    }

    @Test
    fun `parseSearchResults on malformed json returns empty list`() {
        assertEquals(emptyList<SearchResult>(), parseSearchResults("not json"))
    }

    @Test
    fun `parseSearchResults optString fills missing fields with empty string`() {
        val json = """[{"id":"test"}]"""
        val results = parseSearchResults(json)
        assertEquals(1, results.size)
        assertEquals("test", results[0].id)
        assertEquals("", results[0].title)
        assertEquals("", results[0].coverUrl)
    }

    // ── Pages ────────────────────────────────────────────────────────────────

    @Test
    fun `parsePages returns correct urls`() {
        val json = """["https://img.com/p1.jpg","https://img.com/p2.jpg","https://img.com/p3.jpg"]"""
        val pages = parsePages(json)
        assertEquals(3, pages.size)
        assertEquals("https://img.com/p1.jpg", pages[0])
        assertEquals("https://img.com/p3.jpg", pages[2])
    }

    @Test
    fun `parsePages on empty array returns empty list`() {
        assertEquals(emptyList<String>(), parsePages("[]"))
    }

    @Test
    fun `parsePages on null returns empty list`() {
        assertEquals(emptyList<String>(), parsePages(null))
    }

    @Test
    fun `parsePages on malformed json returns empty list`() {
        assertEquals(emptyList<String>(), parsePages("{not array}"))
    }

    // ── Manga Detail ─────────────────────────────────────────────────────────

    @Test
    fun `parseMangaDetail extracts id title genres and chapter count`() {
        val json = """{
          "id": "one-piece.20",
          "title": "One Piece",
          "genres": ["Action","Adventure","Comedy"],
          "chapters": [{"id":"ch1"},{"id":"ch2"},{"id":"ch3"}]
        }"""
        val detail = parseMangaDetail(json)
        assertNotNull(detail)
        assertEquals("one-piece.20", detail!!.id)
        assertEquals("One Piece", detail.title)
        assertEquals(listOf("Action", "Adventure", "Comedy"), detail.genres)
        assertEquals(3, detail.chapters)
    }

    @Test
    fun `parseMangaDetail with empty genres and no chapters`() {
        val json = """{"id":"x","title":"Y"}"""
        val detail = parseMangaDetail(json)
        assertNotNull(detail)
        assertEquals(emptyList<String>(), detail!!.genres)
        assertEquals(0, detail.chapters)
    }

    @Test
    fun `parseMangaDetail on null returns null`() {
        assertNull(parseMangaDetail(null))
    }
}

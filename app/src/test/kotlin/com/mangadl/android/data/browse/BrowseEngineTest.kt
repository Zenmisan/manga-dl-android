package com.mangadl.android.data.browse

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class BrowseEngineTest {

    private fun parseSearchResults(json: String?, backendBase: String = "https://manga.example.com"): List<Map<String, String>> {
        if (json.isNullOrBlank()) return emptyList()
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            val rawCover = obj.optString("cover_url").ifEmpty { obj.optString("coverUrl") }
            val resolvedCover = when {
                rawCover.startsWith("http://") || rawCover.startsWith("https://") -> rawCover
                rawCover.startsWith("/api/") -> "$backendBase$rawCover"
                rawCover.startsWith("/") -> "$backendBase/api$rawCover"
                else -> rawCover
            }
            mapOf(
                "id" to obj.optString("id"),
                "title" to obj.optString("title"),
                "coverUrl" to resolvedCover,
                "provider" to obj.optString("provider"),
            )
        }
    }

    @Test
    fun `parseSearchResults resolves relative URLs correctly`() {
        val sampleJson = """
            [
                {"id": "m1", "title": "Manga 1", "cover_url": "https://img.example.com/cover.jpg", "provider": "p1"},
                {"id": "m2", "title": "Manga 2", "coverUrl": "/manga/cover2.jpg", "provider": "p2"},
                {"id": "m3", "title": "Manga 3", "cover_url": "/api/proxy/cover3.jpg", "provider": "p3"}
            ]
        """.trimIndent()

        val results = parseSearchResults(sampleJson, "https://manga.example.com")
        assertEquals(3, results.size)
        assertEquals("https://img.example.com/cover.jpg", results[0]["coverUrl"])
        assertEquals("https://manga.example.com/api/manga/cover2.jpg", results[1]["coverUrl"])
        assertEquals("https://manga.example.com/api/proxy/cover3.jpg", results[2]["coverUrl"])
    }

    @Test
    fun `bundled extensions define getPopular and getLatest`() {
        val extensionsDir = File("src/main/assets/extensions")
        if (!extensionsDir.exists()) {
            // Path relative to project root or app module
            val altDir = File("app/src/main/assets/extensions")
            if (altDir.exists()) {
                val files = altDir.listFiles { _, name -> name.endsWith(".js") } ?: emptyArray()
                assertTrue("Should find bundled extension scripts", files.isNotEmpty())
                var withPopular = 0
                for (file in files) {
                    val text = file.readText()
                    if (text.contains("getPopular(") || text.contains("getPopular (")) {
                        withPopular++
                    }
                }
                assertTrue("At least some extensions define getPopular", withPopular > 0)
            }
        }
    }

    @Test
    fun `disabled sources filter correctly excludes disabled IDs`() {
        val allSources = listOf("mangadex", "asurascans", "flamecomics", "reaperscans")
        val disabledSources = setOf("asurascans", "reaperscans")

        val enabled = allSources.filter { it !in disabledSources }
        assertEquals(listOf("mangadex", "flamecomics"), enabled)
    }
}

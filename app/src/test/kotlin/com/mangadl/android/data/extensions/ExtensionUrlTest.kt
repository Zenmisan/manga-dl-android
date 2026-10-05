package com.mangadl.android.data.extensions

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for relative-URL resolution logic used in ExtensionManager.apiFetch.
 * Mirrors the exact logic in apiFetch: relative paths get backendUrl prepended.
 */
class ExtensionUrlTest {

    private fun resolveUrl(backendUrl: String, url: String): String {
        return if (url.startsWith("/")) backendUrl.trimEnd('/') + url else url
    }

    @Test
    fun `relative path gets backend url prepended`() {
        val result = resolveUrl("https://manga-dl.onrender.com", "/manga/proxy/html?url=foo")
        assertEquals("https://manga-dl.onrender.com/manga/proxy/html?url=foo", result)
    }

    @Test
    fun `absolute url passes through unchanged`() {
        val url = "https://mangakatana.com/manga/one-piece"
        val result = resolveUrl("https://manga-dl.onrender.com", url)
        assertEquals(url, result)
    }

    @Test
    fun `trailing slash on base url is stripped before appending`() {
        val result = resolveUrl("https://manga-dl.onrender.com/", "/manga/proxy/html")
        assertEquals("https://manga-dl.onrender.com/manga/proxy/html", result)
    }

    @Test
    fun `empty backend url with relative path gives just the path`() {
        val result = resolveUrl("", "/manga/proxy/html")
        assertEquals("/manga/proxy/html", result)
    }

    @Test
    fun `http absolute url passes through`() {
        val url = "http://localhost:8000/health"
        assertEquals(url, resolveUrl("https://remote.example.com", url))
    }
}

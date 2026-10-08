package com.mangadl.android.data.novel

import com.mangadl.android.data.extensions.ExtensionManager
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NovelReaderTest {

    @Test
    fun testIsNovelSource() {
        // Novel sources
        assertTrue(ExtensionManager.isNovelSource("royalroad"))
        assertTrue(ExtensionManager.isNovelSource("novelbin"))
        assertTrue(ExtensionManager.isNovelSource("freewebnovel"))
        assertTrue(ExtensionManager.isNovelSource("novelfull"))
        assertTrue(ExtensionManager.isNovelSource("readhive"))
        assertTrue(ExtensionManager.isNovelSource("lightnovelworld"))

        // Manga sources
        assertFalse(ExtensionManager.isNovelSource("mangadex"))
        assertFalse(ExtensionManager.isNovelSource("asurascans"))
        assertFalse(ExtensionManager.isNovelSource("tcbscans"))
        assertFalse(ExtensionManager.isNovelSource("yaoiscan"))
    }

    @Test
    fun testParseNovelHtmlContentIntoParagraphs() {
        val sampleHtml = """
            <div class="chapter-content">
                <script>showAds();</script>
                <div class="ad">Buy our premium coins!</div>
                <h1>Chapter 1: The Awakening</h1>
                <p>The dawn broke over the horizon, casting crimson light across the ruins.</p>
                <p>Linley took a deep breath, clutching the dragon ring hanging from his neck.</p>
                <div id="ad-banner">Advertisement</div>
                <p>He felt the slumbering ancient power begin to stir within his veins.</p>
                <iframe src="https://ads.tracker.com"></iframe>
            </div>
        """.trimIndent()

        val doc = Jsoup.parse(sampleHtml)
        doc.select("script, style, iframe, .ad, .advertisement, [id*=ad-], noscript").remove()
        val container = doc.selectFirst("div.chapter-content, div.text-left, div.entry-content, article") ?: doc.body()
        val elements = container.select("p, h1, h2, h3, h4, h5, h6")
        val paragraphs = elements.map { it.text().trim() }.filter { it.isNotBlank() }

        assertEquals(4, paragraphs.size)
        assertEquals("Chapter 1: The Awakening", paragraphs[0])
        assertEquals("The dawn broke over the horizon, casting crimson light across the ruins.", paragraphs[1])
        assertEquals("Linley took a deep breath, clutching the dragon ring hanging from his neck.", paragraphs[2])
        assertEquals("He felt the slumbering ancient power begin to stir within his veins.", paragraphs[3])

        // Verify no ad content leaked
        assertFalse(paragraphs.any { it.contains("Buy our premium") || it.contains("Advertisement") })
    }

    @Test
    fun testParsePlaintextNovelContent() {
        val plainText = """
            Chapter 2: The Forest Path

            The wind howled through the silver leaves of the elder wood.

            A rustle in the undergrowth warned him of an approaching beast.
        """.trimIndent()

        val doc = Jsoup.parse(plainText)
        doc.select("script, style, iframe, .ad, .advertisement, [id*=ad-], noscript").remove()
        val elements = doc.select("p, div.chapter-content, div.text-left, h1, h2, h3, h4, h5, h6")
        val paragraphs = if (elements.isNotEmpty()) {
            elements.map { it.text().trim() }.filter { it.isNotBlank() }
        } else {
            val bodyText = doc.body().wholeText()
            bodyText.split("\n\n", "\n").map { it.trim() }.filter { it.isNotBlank() }
        }

        assertEquals(3, paragraphs.size)
        assertEquals("Chapter 2: The Forest Path", paragraphs[0])
        assertEquals("The wind howled through the silver leaves of the elder wood.", paragraphs[1])
        assertEquals("A rustle in the undergrowth warned him of an approaching beast.", paragraphs[2])
    }
}

package com.mangadl.android.data.download

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class DownloadPackagingTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testBuildComicInfoXml_formattingAndEscaping() {
        val xml = DownloadPackaging.buildComicInfoXml(
            series = "Tom & Jerry <Special \"Edition'>",
            title = "Chapter 42.5: The & Big <Clash>",
            pageCount = 35
        )

        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"utf-8\"?>"))
        assertTrue(xml.contains("<Series>Tom &amp; Jerry &lt;Special &quot;Edition&apos;&gt;</Series>"))
        assertTrue(xml.contains("<Title>Chapter 42.5: The &amp; Big &lt;Clash&gt;</Title>"))
        assertTrue(xml.contains("<Number>42.5</Number>"))
        assertTrue(xml.contains("<PageCount>35</PageCount>"))
    }

    @Test
    fun testBuildComicInfoXml_fallbackChapterNumber() {
        val xml = DownloadPackaging.buildComicInfoXml(
            series = "Solo Leveling",
            title = "Prologue Special",
            pageCount = 10
        )
        assertTrue(xml.contains("<Number>1</Number>"))
    }

    @Test
    fun testBuildCbz_zipStructureAndEntries() {
        val outCbz = tempFolder.newFile("test.cbz")

        val img1 = tempFolder.newFile("page1.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val img2 = tempFolder.newFile("page2.png").apply { writeBytes(byteArrayOf(4, 5, 6)) }

        DownloadPackaging.buildCbz(
            outFile = outCbz,
            imageFiles = listOf(img1, img2),
            series = "Bleach",
            chapterTitle = "Chapter 1"
        )

        assertTrue(outCbz.exists() && outCbz.length() > 0)

        ZipFile(outCbz).use { zip ->
            val entries = zip.entries().asSequence().map { it.name }.toList()
            assertEquals(listOf("ComicInfo.xml", "0001.jpg", "0002.png"), entries)

            val comicInfoStream = zip.getInputStream(zip.getEntry("ComicInfo.xml"))
            val comicInfoContent = comicInfoStream.bufferedReader().readText()
            assertTrue(comicInfoContent.contains("<Series>Bleach</Series>"))
            assertTrue(comicInfoContent.contains("<PageCount>2</PageCount>"))

            val page1Bytes = zip.getInputStream(zip.getEntry("0001.jpg")).readBytes()
            assertArrayEquals(byteArrayOf(1, 2, 3), page1Bytes)
        }
    }

    @Test
    fun testBuildEpub_specCompliance() {
        val outEpub = tempFolder.newFile("test.epub")

        DownloadPackaging.buildEpub(
            outFile = outEpub,
            bookTitle = "Lord of the Mysteries",
            chapterTitle = "Chapter 1: Crimson",
            htmlOrText = "<p>Deep within the crimson moonlit night...</p><p>A figure stirred.</p>",
            chapterId = "lotm-ch1"
        )

        assertTrue(outEpub.exists() && outEpub.length() > 0)

        ZipFile(outEpub).use { zip ->
            val entries = zip.entries().asSequence().toList()
            assertTrue(entries.size >= 4)

            // 1. mimetype MUST be first and STORED per EPUB standard
            val firstEntry = entries[0]
            assertEquals("mimetype", firstEntry.name)
            assertEquals(ZipEntry.STORED, firstEntry.method)
            val mimeContent = zip.getInputStream(firstEntry).bufferedReader().readText()
            assertEquals("application/epub+zip", mimeContent)

            // 2. META-INF/container.xml
            val containerEntry = zip.getEntry("META-INF/container.xml")
            assertNotNull(containerEntry)
            val containerXml = zip.getInputStream(containerEntry).bufferedReader().readText()
            assertTrue(containerXml.contains("full-path=\"OEBPS/content.opf\""))

            // 3. OEBPS/content.opf
            val opfEntry = zip.getEntry("OEBPS/content.opf")
            assertNotNull(opfEntry)
            val opfContent = zip.getInputStream(opfEntry).bufferedReader().readText()
            assertTrue(opfContent.contains("<dc:title>Lord of the Mysteries - Chapter 1: Crimson</dc:title>"))
            assertTrue(opfContent.contains("<dc:identifier id=\"BookID\">lotm-ch1</dc:identifier>"))
            assertTrue(opfContent.contains("<item id=\"chapter\" href=\"chapter.xhtml\""))

            // 4. OEBPS/chapter.xhtml
            val xhtmlEntry = zip.getEntry("OEBPS/chapter.xhtml")
            assertNotNull(xhtmlEntry)
            val xhtmlContent = zip.getInputStream(xhtmlEntry).bufferedReader().readText()
            assertTrue(xhtmlContent.contains("<h1>Chapter 1: Crimson</h1>"))
            assertTrue(xhtmlContent.contains("<p>Deep within the crimson moonlit night...</p>"))
            assertTrue(xhtmlContent.contains("<p>A figure stirred.</p>"))
        }
    }

    @Test
    fun testBuildEpub_plainTextConversion() {
        val outEpub = tempFolder.newFile("plain.epub")

        val rawText = """
            Line one of the story with a <tag> & character.
            
            Line two of the story.
        """.trimIndent()

        DownloadPackaging.buildEpub(
            outFile = outEpub,
            bookTitle = "Novels",
            chapterTitle = "Ch 1",
            htmlOrText = rawText,
            chapterId = "c1"
        )

        ZipFile(outEpub).use { zip ->
            val xhtml = zip.getInputStream(zip.getEntry("OEBPS/chapter.xhtml")).bufferedReader().readText()
            assertTrue(xhtml.contains("<p>Line one of the story with a &lt;tag&gt; &amp; character.</p>"))
            assertTrue(xhtml.contains("<p>Line two of the story.</p>"))
        }
    }

    @Test
    fun testSanitizeFilename() {
        val dangerous = "Manga: Chapter/1? *special* \"quote\" <edition> | test\\end"
        val safe = DownloadPackaging.sanitizeFilename(dangerous)

        assertFalse(safe.contains("/"))
        assertFalse(safe.contains("\\"))
        assertFalse(safe.contains(":"))
        assertFalse(safe.contains("*"))
        assertFalse(safe.contains("?"))
        assertFalse(safe.contains("\""))
        assertFalse(safe.contains("<"))
        assertFalse(safe.contains(">"))
        assertFalse(safe.contains("|"))

        val veryLong = "a".repeat(150)
        assertEquals(100, DownloadPackaging.sanitizeFilename(veryLong).length)
    }
}

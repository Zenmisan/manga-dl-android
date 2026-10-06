package com.mangadl.android.data.download

import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DownloadPackaging {

    fun buildComicInfoXml(series: String, title: String, pageCount: Int): String {
        val chapterNum = title.filter { it.isDigit() || it == '.' }.ifBlank { "1" }
        return """<?xml version="1.0" encoding="utf-8"?>
<ComicInfo xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <Series>${escapeXml(series)}</Series>
  <Title>${escapeXml(title)}</Title>
  <Number>$chapterNum</Number>
  <PageCount>$pageCount</PageCount>
</ComicInfo>""".trimIndent()
    }

    fun buildEpub(
        outFile: File,
        bookTitle: String,
        chapterTitle: String,
        htmlOrText: String,
        chapterId: String,
    ) {
        val formattedBody = if (htmlOrText.contains("<p>") || htmlOrText.contains("<div>")) {
            htmlOrText
        } else {
            htmlOrText.lines()
                .filter { it.isNotBlank() }
                .joinToString("\n") { "<p>${escapeXml(it.trim())}</p>" }
        }

        val chapterXhtml = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>${escapeXml(chapterTitle)}</title>
    <style type="text/css">
      body { font-family: sans-serif; line-height: 1.6; margin: 1em; }
      h1 { font-size: 1.5em; margin-bottom: 1em; }
      p { margin-bottom: 1em; text-indent: 1em; }
    </style>
  </head>
  <body>
    <h1>${escapeXml(chapterTitle)}</h1>
    $formattedBody
  </body>
</html>""".trimIndent()

        val containerXml = """<?xml version="1.0"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>""".trimIndent()

        val contentOpf = """<?xml version="1.0" encoding="utf-8"?>
<package xmlns="http://www.idpf.org/2007/opf" unique-identifier="BookID" version="2.0">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:title>${escapeXml(bookTitle)} - ${escapeXml(chapterTitle)}</dc:title>
    <dc:identifier id="BookID">$chapterId</dc:identifier>
    <dc:language>en</dc:language>
  </metadata>
  <manifest>
    <item id="chapter" href="chapter.xhtml" media-type="application/xhtml+xml"/>
  </manifest>
  <spine>
    <itemref idref="chapter"/>
  </spine>
</package>""".trimIndent()

        ZipOutputStream(outFile.outputStream().buffered()).use { zip ->
            // 1. mimetype (MUST be first and uncompressed per EPUB standard)
            val mimeBytes = "application/epub+zip".toByteArray(StandardCharsets.US_ASCII)
            val mimeEntry = ZipEntry("mimetype").apply {
                method = ZipEntry.STORED
                size = mimeBytes.size.toLong()
                compressedSize = mimeBytes.size.toLong()
                crc = CRC32().apply { update(mimeBytes) }.value
            }
            zip.putNextEntry(mimeEntry)
            zip.write(mimeBytes)
            zip.closeEntry()

            // 2. META-INF/container.xml
            zip.putNextEntry(ZipEntry("META-INF/container.xml"))
            zip.write(containerXml.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 3. OEBPS/content.opf
            zip.putNextEntry(ZipEntry("OEBPS/content.opf"))
            zip.write(contentOpf.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 4. OEBPS/chapter.xhtml
            zip.putNextEntry(ZipEntry("OEBPS/chapter.xhtml"))
            zip.write(chapterXhtml.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()
        }
    }

    fun buildCbz(
        outFile: File,
        imageFiles: List<File>,
        series: String,
        chapterTitle: String,
    ) {
        ZipOutputStream(outFile.outputStream().buffered()).use { zip ->
            val comicInfo = buildComicInfoXml(series, chapterTitle, imageFiles.size)
            zip.putNextEntry(ZipEntry("ComicInfo.xml"))
            zip.write(comicInfo.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            imageFiles.forEachIndexed { idx, imgFile ->
                val ext = imgFile.extension.ifEmpty { "jpg" }
                val entryName = String.format("%04d.%s", idx + 1, ext)
                zip.putNextEntry(ZipEntry(entryName))
                imgFile.inputStream().buffered().use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    fun escapeXml(str: String): String {
        return str
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[/\\\\?%*:|\"<>]"), "-").trim().take(100)
    }
}

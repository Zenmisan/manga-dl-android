package com.mangadl.android.data.backup

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream

/**
 * Pure-Kotlin Tachiyomi / Mihon backup decoder.
 * Supports:
 * 1. Protocol Buffers binary (.tachibk), gzip compressed or uncompressed.
 * 2. Tachiyomi JSON backup exports.
 * 3. Provider identification and URL-to-ID normalization.
 */
object TachiyomiBackupDecoder {

    data class ParsedTachiyomiBackup(
        val manga: List<ParsedTachiyomiManga> = emptyList(),
        val categories: List<ParsedTachiyomiCategory> = emptyList(),
        val sources: List<ParsedTachiyomiSource> = emptyList(),
    )

    data class ParsedTachiyomiManga(
        val source: Long = 0,
        val sourceName: String = "",
        val url: String = "",
        val title: String = "",
        val artist: String? = null,
        val author: String? = null,
        val description: String? = null,
        val thumbnailUrl: String? = null,
        val favorite: Boolean = true,
        val chapters: List<ParsedTachiyomiChapter> = emptyList(),
        val categoryNames: List<String> = emptyList(),
        val tracking: List<ParsedTachiyomiTracking> = emptyList(),
    )

    data class ParsedTachiyomiChapter(
        val url: String = "",
        val name: String = "",
        val scanlator: String? = null,
        val read: Boolean = false,
        val bookmark: Boolean = false,
        val lastPageRead: Long = 0,
        val chapterNumber: Float = 0f,
        val sourceOrder: Long = 0,
    )

    data class ParsedTachiyomiCategory(
        val name: String = "",
        val order: Long = 0,
    )

    data class ParsedTachiyomiSource(
        val name: String = "",
        val sourceId: Long = 0,
    )

    data class ParsedTachiyomiTracking(
        val syncId: Int = 0,
        val mediaId: Long = 0,
        val title: String = "",
        val lastChapterRead: Float = 0f,
        val score: Float = 0f,
        val status: Int = 0,
        val trackingUrl: String = "",
    )

    private val PROVIDER_KEYWORDS = listOf(
        "mangadex" to "mangadex",
        "asura" to "asurascans",
        "omega" to "omegascans",
        "flame" to "flamecomics",
        "katana" to "mangakatana",
        "kakalot" to "mangakakalot",
        "manganato" to "manganato",
        "nato" to "manganato",
        "batoto" to "bato",
        "bato" to "bato",
        "pill" to "mangapill",
        "tcb" to "tcbscans",
        "mangahere" to "mangahere",
        "webtoon" to "webtoons",
        "mangaplus" to "mangaplus",
        "komga" to "komga",
        "suwayomi" to "suwayomi",
        "royalroad" to "royalroad",
        "scribblehub" to "scribblehub",
        "novelbin" to "novelbin",
        "novelfull" to "novelfull",
        "wuxiaworld" to "wuxiaworld",
        "lightnovelworld" to "lightnovelworld",
    )

    /**
     * Map a Tachiyomi source name / URL to a Manga-DL provider ID.
     */
    fun resolveProvider(sourceName: String, mangaUrl: String = ""): String {
        val s = sourceName.lowercase().trim()
        val u = mangaUrl.lowercase().trim()
        val target = "$s $u"
        for ((kw, prov) in PROVIDER_KEYWORDS) {
            if (target.contains(kw)) {
                return prov
            }
        }
        val slug = s.replace(Regex("[^a-z0-9]"), "")
        return if (slug.isNotEmpty()) slug else "tachiyomi"
    }

    /**
     * Normalize manga URL to clean ID.
     */
    fun cleanMangaId(url: String, provider: String = ""): String {
        if (url.isBlank()) return "unknown"
        var u = url.trim()
        if (u.contains("://")) {
            u = u.substringAfter("://")
            if (u.contains("/")) {
                u = "/" + u.substringAfter("/")
            }
        }
        u = u.substringBefore("?").substringBefore("#").trim('/')
        if (provider == "mangadex") {
            val uuidRegex = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", RegexOption.IGNORE_CASE)
            val match = uuidRegex.find(u)
            if (match != null) {
                return match.value.lowercase()
            }
        }
        val prefixes = listOf("manga/", "series/", "title/", "comic/")
        for (p in prefixes) {
            if (u.startsWith(p)) {
                u = u.removePrefix(p)
                break
            }
        }
        return u.trim('/').ifEmpty { "unknown" }
    }

    /**
     * Normalize chapter URL or number to clean ID.
     */
    fun cleanChapterId(url: String, chapterNumber: Float = 0f): String {
        if (url.isBlank()) {
            return if (chapterNumber > 0f) chapterNumber.toString().removeSuffix(".0") else "ch-1"
        }
        val u = url.trim().substringBefore("?").substringBefore("#").trim('/')
        if (u.contains("/")) {
            val lastSeg = u.substringAfterLast("/")
            if (lastSeg.isNotBlank()) {
                return lastSeg
            }
        }
        return u.ifEmpty { if (chapterNumber > 0f) chapterNumber.toString().removeSuffix(".0") else "ch-1" }
    }

    /**
     * Decode a Tachiyomi backup in either .tachibk (protobuf) or .json format.
     */
    fun decode(data: ByteArray, filename: String = ""): ParsedTachiyomiBackup {
        val lowerName = filename.lowercase()
        val isJson = lowerName.endsWith(".json") || (
            !isGzip(data) && data.isNotEmpty() && data[0].toInt().toChar() == '{'
        )

        return if (isJson) {
            runCatching { parseTachiyomiJson(data) }.getOrElse { decodeProtobuf(data) }
        } else {
            decodeProtobuf(data)
        }
    }

    private fun isGzip(data: ByteArray): Boolean {
        return data.size >= 2 && data[0] == 0x1f.toByte() && data[1] == 0x8b.toByte()
    }

    private fun decompressGzip(data: ByteArray): ByteArray {
        return GZIPInputStream(ByteArrayInputStream(data)).use { it.readBytes() }
    }

    /**
     * Parse Tachiyomi JSON format.
     */
    fun parseTachiyomiJson(data: ByteArray): ParsedTachiyomiBackup {
        val root = JSONObject(String(data, Charsets.UTF_8))
        val mangaArray = root.optJSONArray("backupManga")
            ?: root.optJSONArray("manga")
            ?: root.optJSONArray("library")
            ?: JSONArray()

        val catsArray = root.optJSONArray("backupCategories")
            ?: root.optJSONArray("categories")
            ?: JSONArray()

        val sourcesArray = root.optJSONArray("backupSources")
            ?: root.optJSONArray("sources")
            ?: JSONArray()

        val categories = mutableListOf<ParsedTachiyomiCategory>()
        for (i in 0 until catsArray.length()) {
            val item = catsArray.opt(i)
            if (item is JSONObject) {
                categories.add(ParsedTachiyomiCategory(item.optString("name", ""), item.optLong("order", i.toLong())))
            } else if (item != null) {
                categories.add(ParsedTachiyomiCategory(item.toString(), i.toLong()))
            }
        }

        val sources = mutableListOf<ParsedTachiyomiSource>()
        for (i in 0 until sourcesArray.length()) {
            val item = sourcesArray.opt(i)
            if (item is JSONObject) {
                val sId = item.optLong("sourceId", item.optLong("source_id", 0L))
                sources.add(ParsedTachiyomiSource(item.optString("name", ""), sId))
            }
        }

        val sourceMap = sources.filter { it.sourceId != 0L }.associate { it.sourceId to it.name }
        val catMap = categories.mapIndexed { idx, c -> (idx + 1).toLong() to c.name }.toMap()

        val mangaList = mutableListOf<ParsedTachiyomiManga>()
        for (i in 0 until mangaArray.length()) {
            val m = mangaArray.optJSONObject(i) ?: continue
            val title = m.optString("title", "")
            if (title.isBlank()) continue

            val rawCatIds = m.optJSONArray("categories") ?: m.optJSONArray("category_ids") ?: JSONArray()
            val catNames = mutableListOf<String>()
            for (j in 0 until rawCatIds.length()) {
                val cVal = rawCatIds.opt(j)
                if (cVal is String) catNames.add(cVal)
                else if (cVal is Number && catMap.containsKey(cVal.toLong())) catNames.add(catMap[cVal.toLong()]!!)
            }

            val chaptersJson = m.optJSONArray("chapters") ?: JSONArray()
            val chapters = mutableListOf<ParsedTachiyomiChapter>()
            for (j in 0 until chaptersJson.length()) {
                val ch = chaptersJson.optJSONObject(j) ?: continue
                chapters.add(
                    ParsedTachiyomiChapter(
                        url = ch.optString("url", ""),
                        name = ch.optString("name", ""),
                        scanlator = ch.optString("scanlator").ifEmpty { null },
                        read = ch.optBoolean("read", false),
                        bookmark = ch.optBoolean("bookmark", false),
                        lastPageRead = ch.optLong("lastPageRead", ch.optLong("last_page_read", 0L)),
                        chapterNumber = ch.optDouble("chapterNumber", ch.optDouble("chapter_number", 0.0)).toFloat(),
                        sourceOrder = ch.optLong("sourceOrder", ch.optLong("source_order", 0L)),
                    )
                )
            }

            val trackingJson = m.optJSONArray("tracking") ?: JSONArray()
            val tracking = mutableListOf<ParsedTachiyomiTracking>()
            for (j in 0 until trackingJson.length()) {
                val tr = trackingJson.optJSONObject(j) ?: continue
                tracking.add(
                    ParsedTachiyomiTracking(
                        syncId = tr.optInt("syncId", tr.optInt("sync_id", 0)),
                        mediaId = tr.optLong("mediaId", tr.optLong("media_id", 0L)),
                        title = tr.optString("title", ""),
                        lastChapterRead = tr.optDouble("lastChapterRead", tr.optDouble("last_chapter_read", 0.0)).toFloat(),
                        score = tr.optDouble("score", 0.0).toFloat(),
                        status = tr.optInt("status", 0),
                        trackingUrl = tr.optString("trackingUrl", tr.optString("tracking_url", "")),
                    )
                )
            }

            val sourceVal = m.optLong("source", 0L)
            val sourceName = sourceMap[sourceVal] ?: (if (sourceVal != 0L) sourceVal.toString() else "")

            mangaList.add(
                ParsedTachiyomiManga(
                    source = sourceVal,
                    sourceName = sourceName,
                    url = m.optString("url", ""),
                    title = title,
                    artist = m.optString("artist").ifEmpty { null },
                    author = m.optString("author").ifEmpty { null },
                    description = m.optString("description").ifEmpty { null },
                    thumbnailUrl = m.optString("thumbnailUrl", m.optString("thumbnail_url", "")).ifEmpty { null },
                    favorite = m.optBoolean("favorite", true),
                    chapters = chapters,
                    categoryNames = catNames,
                    tracking = tracking,
                )
            )
        }

        return ParsedTachiyomiBackup(
            manga = mangaList,
            categories = categories,
            sources = sources,
        )
    }

    /**
     * Decode binary Protocol Buffers .tachibk backup file.
     */
    fun decodeProtobuf(data: ByteArray): ParsedTachiyomiBackup {
        val payload = if (isGzip(data)) decompressGzip(data) else data
        val root = decodeProtoMessage(payload)

        val rawMangaList = getMessages(root, 1)
        val rawCatList = getMessages(root, 2)
        val rawSourceList = getMessages(root, 101)

        val categories = rawCatList.map { parseCategoryProto(it) }
        val sources = rawSourceList.map { parseSourceProto(it) }

        val catMap = categories.mapIndexed { idx, c -> (idx + 1).toLong() to c.name }.toMap()
        val sourceMap = sources.filter { it.sourceId != 0L }.associate { it.sourceId to it.name }

        val mangaList = rawMangaList.map { rawMangaBytes ->
            val mFields = decodeProtoMessage(rawMangaBytes)
            val sourceId = getLong(mFields, 1, 0L)
            val sourceName = sourceMap[sourceId] ?: ""
            val url = getString(mFields, 2)
            val title = getString(mFields, 3)
            val artist = getString(mFields, 4).ifEmpty { null }
            val author = getString(mFields, 5).ifEmpty { null }
            val description = getString(mFields, 6).ifEmpty { null }
            val thumbnailUrl = getString(mFields, 9).ifEmpty { null }
            val favorite = getBoolean(mFields, 100, true)

            // Chapters (field 16)
            val chapters = getMessages(mFields, 16).map { parseChapterProto(it) }

            // Categories (field 17: can be repeated varints or packed bytes)
            val catIds = getLongList(mFields, 17)
            val catNames = catIds.mapNotNull { catMap[it] }

            // Tracking (field 18)
            val tracking = getMessages(mFields, 18).map { parseTrackingProto(it) }

            ParsedTachiyomiManga(
                source = sourceId,
                sourceName = sourceName,
                url = url,
                title = title,
                artist = artist,
                author = author,
                description = description,
                thumbnailUrl = thumbnailUrl,
                favorite = favorite,
                chapters = chapters,
                categoryNames = catNames,
                tracking = tracking,
            )
        }

        return ParsedTachiyomiBackup(
            manga = mangaList,
            categories = categories,
            sources = sources,
        )
    }

    private fun parseCategoryProto(data: ByteArray): ParsedTachiyomiCategory {
        val f = decodeProtoMessage(data)
        return ParsedTachiyomiCategory(
            name = getString(f, 1),
            order = getLong(f, 2, 0L),
        )
    }

    private fun parseSourceProto(data: ByteArray): ParsedTachiyomiSource {
        val f = decodeProtoMessage(data)
        return ParsedTachiyomiSource(
            name = getString(f, 1),
            sourceId = getLong(f, 2, 0L),
        )
    }

    private fun parseChapterProto(data: ByteArray): ParsedTachiyomiChapter {
        val f = decodeProtoMessage(data)
        return ParsedTachiyomiChapter(
            url = getString(f, 1),
            name = getString(f, 2),
            scanlator = getString(f, 3).ifEmpty { null },
            read = getBoolean(f, 4, false),
            bookmark = getBoolean(f, 5, false),
            lastPageRead = getLong(f, 6, 0L),
            chapterNumber = getFloat(f, 9, 0f),
            sourceOrder = getLong(f, 10, 0L),
        )
    }

    private fun parseTrackingProto(data: ByteArray): ParsedTachiyomiTracking {
        val f = decodeProtoMessage(data)
        val mediaIdFrom100 = getLong(f, 100, 0L)
        val mediaIdFrom3 = getLong(f, 3, 0L)
        val mediaId = if (mediaIdFrom100 != 0L) mediaIdFrom100 else mediaIdFrom3
        return ParsedTachiyomiTracking(
            syncId = getInt(f, 1, 0),
            mediaId = mediaId,
            trackingUrl = getString(f, 4),
            title = getString(f, 5),
            lastChapterRead = getFloat(f, 6, 0f),
            score = getFloat(f, 8, 0f),
            status = getInt(f, 9, 0),
        )
    }

    // ── Low-level Protobuf Wire Decoder ──────────────────────────────────────────

    private sealed class ProtoVal {
        data class Varint(val value: Long) : ProtoVal()
        data class Fixed64(val value: Long) : ProtoVal()
        data class LengthDelimited(val bytes: ByteArray) : ProtoVal()
        data class Fixed32(val value: Int) : ProtoVal()
    }

    private fun readVarint(data: ByteArray, startPos: Int): Pair<Long, Int> {
        var result = 0L
        var shift = 0
        var pos = startPos
        while (pos < data.size) {
            val b = data[pos++].toLong()
            result = result or ((b and 0x7F) shl shift)
            if ((b and 0x80L) == 0L) {
                return Pair(result, pos)
            }
            shift += 7
            if (shift > 64) break
        }
        return Pair(result, pos)
    }

    private fun decodeProtoMessage(data: ByteArray): Map<Int, List<ProtoVal>> {
        val fields = mutableMapOf<Int, MutableList<ProtoVal>>()
        var pos = 0
        val length = data.size
        while (pos < length) {
            val (tag, nextPos) = readVarint(data, pos)
            if (nextPos <= pos) break
            pos = nextPos
            val fieldNum = (tag ushr 3).toInt()
            val wireType = (tag and 0x07L).toInt()

            when (wireType) {
                0 -> { // Varint
                    val (valLong, vPos) = readVarint(data, pos)
                    pos = vPos
                    fields.getOrPut(fieldNum) { mutableListOf() }.add(ProtoVal.Varint(valLong))
                }
                1 -> { // 64-bit
                    if (pos + 8 <= length) {
                        val buffer = ByteBuffer.wrap(data, pos, 8).order(ByteOrder.LITTLE_ENDIAN)
                        val valLong = buffer.long
                        pos += 8
                        fields.getOrPut(fieldNum) { mutableListOf() }.add(ProtoVal.Fixed64(valLong))
                    } else break
                }
                2 -> { // Length-delimited
                    val (lenLong, lPos) = readVarint(data, pos)
                    pos = lPos
                    val len = lenLong.toInt()
                    if (pos + len <= length && len >= 0) {
                        val slice = data.copyOfRange(pos, pos + len)
                        pos += len
                        fields.getOrPut(fieldNum) { mutableListOf() }.add(ProtoVal.LengthDelimited(slice))
                    } else break
                }
                5 -> { // 32-bit
                    if (pos + 4 <= length) {
                        val buffer = ByteBuffer.wrap(data, pos, 4).order(ByteOrder.LITTLE_ENDIAN)
                        val valInt = buffer.int
                        pos += 4
                        fields.getOrPut(fieldNum) { mutableListOf() }.add(ProtoVal.Fixed32(valInt))
                    } else break
                }
                else -> {
                    // Unknown or unsupported wire type (e.g. groups), abort this message safely
                    break
                }
            }
        }
        return fields
    }

    private fun getString(fields: Map<Int, List<ProtoVal>>, num: Int, default: String = ""): String {
        val vals = fields[num] ?: return default
        val first = vals.firstOrNull() ?: return default
        return when (first) {
            is ProtoVal.LengthDelimited -> String(first.bytes, Charsets.UTF_8)
            is ProtoVal.Varint -> first.value.toString()
            else -> default
        }
    }

    private fun getInt(fields: Map<Int, List<ProtoVal>>, num: Int, default: Int = 0): Int {
        val vals = fields[num] ?: return default
        val first = vals.firstOrNull() ?: return default
        return when (first) {
            is ProtoVal.Varint -> first.value.toInt()
            is ProtoVal.Fixed32 -> first.value
            is ProtoVal.Fixed64 -> first.value.toInt()
            else -> default
        }
    }

    private fun getLong(fields: Map<Int, List<ProtoVal>>, num: Int, default: Long = 0L): Long {
        val vals = fields[num] ?: return default
        val first = vals.firstOrNull() ?: return default
        return when (first) {
            is ProtoVal.Varint -> first.value
            is ProtoVal.Fixed64 -> first.value
            is ProtoVal.Fixed32 -> first.value.toLong()
            else -> default
        }
    }

    private fun getFloat(fields: Map<Int, List<ProtoVal>>, num: Int, default: Float = 0f): Float {
        val vals = fields[num] ?: return default
        val first = vals.firstOrNull() ?: return default
        return when (first) {
            is ProtoVal.Fixed32 -> Float.fromBits(first.value)
            is ProtoVal.Varint -> first.value.toFloat()
            is ProtoVal.Fixed64 -> Double.fromBits(first.value).toFloat()
            else -> default
        }
    }

    private fun getBoolean(fields: Map<Int, List<ProtoVal>>, num: Int, default: Boolean = false): Boolean {
        val vals = fields[num] ?: return default
        val first = vals.firstOrNull() ?: return default
        return when (first) {
            is ProtoVal.Varint -> first.value != 0L
            else -> default
        }
    }

    private fun getMessages(fields: Map<Int, List<ProtoVal>>, num: Int): List<ByteArray> {
        val vals = fields[num] ?: return emptyList()
        return vals.mapNotNull { if (it is ProtoVal.LengthDelimited) it.bytes else null }
    }

    private fun getLongList(fields: Map<Int, List<ProtoVal>>, num: Int): List<Long> {
        val vals = fields[num] ?: return emptyList()
        val result = mutableListOf<Long>()
        for (v in vals) {
            when (v) {
                is ProtoVal.Varint -> result.add(v.value)
                is ProtoVal.LengthDelimited -> {
                    // Packed repeated varints
                    var p = 0
                    val b = v.bytes
                    while (p < b.size) {
                        val (longVal, nextP) = readVarint(b, p)
                        if (nextP <= p) break
                        p = nextP
                        result.add(longVal)
                    }
                }
                else -> {}
            }
        }
        return result
    }
}

package com.mangadl.android.data.source.manga

import com.mangadl.android.data.model.Chapter
import com.mangadl.android.data.model.MangaDetail
import com.mangadl.android.data.model.MangaSearchResult
import com.mangadl.android.data.source.MangaSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLEncoder

class AsuraScansSource(private val client: OkHttpClient) : MangaSource {
    override val id = "asurascans"
    override val name = "Asura Scans"
    override val baseUrl = "https://asurascans.com"
    private val apiUrl = "https://api.asurascans.com/api"

    override suspend fun search(query: String, page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val offset = (page - 1) * 20
        val url = "$apiUrl/series?limit=20&offset=$offset" +
            if (query.isNotBlank()) "&search=${URLEncoder.encode(query, "UTF-8")}" else ""
        fetchApiSeries(url)
    }

    override suspend fun getPopular(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val offset = (page - 1) * 20
        val url = "$apiUrl/series?limit=20&offset=$offset&order=popular"
        fetchApiSeries(url)
    }

    override suspend fun getLatest(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val offset = (page - 1) * 20
        val url = "$apiUrl/series?limit=20&offset=$offset&order=latest"
        fetchApiSeries(url)
    }

    override suspend fun getMangaDetail(mangaId: String): MangaDetail = withContext(Dispatchers.IO) {
        // Fetch series metadata from API or HTML
        val cleanSlug = mangaId.removePrefix("comics/").removePrefix("/comics/").trimEnd('/')
        val req = Request.Builder()
            .url("$apiUrl/series/$cleanSlug")
            .header("Referer", "$baseUrl/")
            .build()

        val jsonStr = runCatching { client.newCall(req).execute().body?.string() }.getOrNull()
        if (!jsonStr.isNullOrBlank() && jsonStr.startsWith("{")) {
            val root = JSONObject(jsonStr)
            val series = root.optJSONObject("data")?.optJSONObject("series")
                ?: root.optJSONObject("series")
                ?: root.optJSONObject("data")

            if (series != null) {
                val title = series.optString("title").ifEmpty { cleanSlug }
                val cover = series.optString("cover")
                val desc = series.optString("description")
                val status = series.optString("status")

                val chapters = mutableListOf<Chapter>()
                val chsArr = series.optJSONArray("chapters")
                if (chsArr != null) {
                    for (i in 0 until chsArr.length()) {
                        val ch = chsArr.getJSONObject(i)
                        val num = ch.optDouble("number", 0.0).toFloat()
                        val chId = "$cleanSlug/chapter/${ch.optString("number").ifEmpty { num.toInt().toString() }}"
                        chapters.add(
                            Chapter(
                                id = chId,
                                title = ch.optString("title").ifEmpty { "Chapter $num" },
                                number = num,
                                publishedAt = ch.optString("published_at"),
                            )
                        )
                    }
                }

                return@withContext MangaDetail(
                    id = cleanSlug,
                    title = title,
                    coverUrl = cover,
                    description = desc,
                    status = status,
                    provider = id,
                    url = "$baseUrl/comics/$cleanSlug",
                    chapters = chapters.sortedByDescending { it.number },
                )
            }
        }

        // HTML Fallback
        val htmlReq = Request.Builder()
            .url("$baseUrl/comics/$cleanSlug")
            .header("Referer", "$baseUrl/")
            .build()
        val doc = Jsoup.parse(client.newCall(htmlReq).execute().body?.string().orEmpty(), baseUrl)

        val title = doc.selectFirst("h1, .text-xl.font-bold, .series-title")?.text().orEmpty().ifEmpty { cleanSlug }
        val cover = doc.selectFirst("img[src*='cover'], img[alt*='$title'], .series-profile-thumb img")?.attr("abs:src").orEmpty()
        val desc = doc.selectFirst(".description, [class*='synopsis'], p.text-sm")?.text().orEmpty()

        val chapters = mutableListOf<Chapter>()
        doc.select("a[href*='/chapter/']").forEach { a ->
            val href = a.attr("abs:href")
            val numStr = href.substringAfter("/chapter/").substringBefore("/").substringBefore("?")
            val num = numStr.toFloatOrNull() ?: 0f
            val chId = "$cleanSlug/chapter/$numStr"
            chapters.add(
                Chapter(
                    id = chId,
                    title = a.text().ifBlank { "Chapter $numStr" },
                    number = num,
                )
            )
        }

        MangaDetail(
            id = cleanSlug,
            title = title,
            coverUrl = cover,
            description = desc,
            provider = id,
            url = "$baseUrl/comics/$cleanSlug",
            chapters = chapters.distinctBy { it.id }.sortedByDescending { it.number },
        )
    }

    override suspend fun getPages(chapterId: String): List<String> = withContext(Dispatchers.IO) {
        val chapterUrl = if (chapterId.startsWith("http")) chapterId else "$baseUrl/comics/$chapterId"
        val req = Request.Builder()
            .url(chapterUrl)
            .header("Referer", "$baseUrl/")
            .build()

        val html = client.newCall(req).execute().body?.string().orEmpty()
        val doc = Jsoup.parse(html, baseUrl)

        val pages = mutableListOf<String>()

        // 1. Extract from Astro Island props
        val islands = doc.select("[props]")
        for (el in islands) {
            val rawProps = el.attr("props")
            if (!rawProps.contains("pages")) continue

            try {
                val json = JSONObject(rawProps)
                val pagesArr = findPagesArray(json) ?: continue
                for (i in 0 until pagesArr.length()) {
                    val p = pagesArr.optJSONObject(i) ?: continue
                    val imgUrl = p.optString("url")
                    if (imgUrl.isBlank()) continue

                    val tilesArr = p.optJSONArray("tiles")
                    val tileCols = p.optInt("tile_cols", 4)
                    val tileRows = p.optInt("tile_rows", 5)

                    if (tilesArr != null && tilesArr.length() > 0) {
                        // Append tile metadata JSON as URL fragment for on-device descrambling
                        val metaJson = JSONObject().apply {
                            put("tiles", tilesArr)
                            put("tileCols", tileCols)
                            put("tileRows", tileRows)
                        }
                        pages.add("$imgUrl#$metaJson")
                    } else {
                        pages.add(imgUrl)
                    }
                }
                if (pages.isNotEmpty()) return@withContext pages
            } catch (_: Exception) {}
        }

        // 2. Direct DOM fallback
        doc.select(".chapter-content img, [id*='reader'] img, img[alt*='Page']").forEach { img ->
            val src = img.attr("abs:src").ifEmpty { img.attr("abs:data-src") }
            if (src.isNotBlank()) pages.add(src)
        }

        pages
    }

    private fun fetchApiSeries(url: String): List<MangaSearchResult> {
        return try {
            val req = Request.Builder().url(url).header("Referer", "$baseUrl/").build()
            val jsonStr = client.newCall(req).execute().body?.string().orEmpty()
            if (jsonStr.isBlank()) return emptyList()

            val root = JSONObject(jsonStr)
            val dataArr = root.optJSONArray("data") ?: return emptyList()
            val list = mutableListOf<MangaSearchResult>()

            for (i in 0 until dataArr.length()) {
                val item = dataArr.getJSONObject(i)
                val slug = item.optString("slug").ifEmpty {
                    item.optString("public_url").removePrefix("/comics/").removeSuffix("/")
                }
                if (slug.isBlank()) continue

                list.add(
                    MangaSearchResult(
                        id = slug,
                        title = item.optString("title").ifEmpty { slug },
                        coverUrl = item.optString("cover"),
                        provider = id,
                        url = "$baseUrl/comics/$slug",
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun findPagesArray(json: JSONObject): JSONArray? {
        if (json.has("pages")) return json.optJSONArray("pages")
        if (json.has("chapter") && json.getJSONObject("chapter").has("pages")) {
            return json.getJSONObject("chapter").optJSONArray("pages")
        }
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val child = json.optJSONObject(key)
            if (child != null && child.has("pages")) {
                return child.optJSONArray("pages")
            }
        }
        return null
    }
}

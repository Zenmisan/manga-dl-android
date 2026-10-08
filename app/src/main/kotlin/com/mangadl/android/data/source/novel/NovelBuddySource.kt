package com.mangadl.android.data.source.novel

import com.mangadl.android.data.model.Chapter
import com.mangadl.android.data.model.MangaDetail
import com.mangadl.android.data.model.MangaSearchResult
import com.mangadl.android.data.source.NovelSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLEncoder

class NovelBuddySource(private val client: OkHttpClient) : NovelSource {
    override val id: String = "novelbuddy"
    override val name: String = "Novel Buddy"
    override val baseUrl: String = "https://novelbuddy.me"
    private val apiUrl: String = "https://api.novelbuddy.me/titles"

    override suspend fun search(query: String, page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$apiUrl/search?page=$page&limit=20&q=$encoded"
        val list = fetchApiItems(url)
        if (list.isNotEmpty()) list else fetchHtmlCards("$baseUrl/search?q=$encoded&page=$page")
    }

    override suspend fun getPopular(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = "$apiUrl/search?page=$page&limit=24&sort=popular"
        val list = fetchApiItems(url)
        if (list.isNotEmpty()) list else fetchHtmlCards("$baseUrl/popular?page=$page")
    }

    override suspend fun getLatest(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = "$apiUrl/search?page=$page&limit=24&sort=latest"
        val list = fetchApiItems(url)
        if (list.isNotEmpty()) list else fetchHtmlCards("$baseUrl/latest?page=$page")
    }

    override suspend fun getMangaDetail(mangaId: String): MangaDetail = withContext(Dispatchers.IO) {
        val slug = mangaId.trim('/').removePrefix("novel/").trim('/')
        val url = if (mangaId.startsWith("http")) mangaId else "$baseUrl/$slug"
        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)

        var bookId = slug
        var title = slug
        var cover = ""
        var desc = ""

        // Next.js data check
        val nextDataNode = doc.selectFirst("script#__NEXT_DATA__")
        if (nextDataNode != null) {
            runCatching {
                val json = JSONObject(nextDataNode.data())
                val pp = json.optJSONObject("props")?.optJSONObject("pageProps")
                val book = pp?.optJSONObject("initialManga")
                    ?: pp?.optJSONObject("book")
                    ?: pp?.optJSONObject("title")
                    ?: pp?.optJSONObject("novel")
                if (book != null) {
                    bookId = book.optString("id").ifEmpty { slug }
                    title = book.optString("name").ifEmpty { book.optString("title") }.ifEmpty { slug }
                    cover = book.optString("cover").ifEmpty { book.optString("image") }
                    desc = book.optString("summary").ifEmpty { book.optString("description") }
                }
            }
        }

        if (title == slug) {
            title = doc.selectFirst("h1.title, h1")?.text()?.trim().orEmpty().ifEmpty { slug }
        }
        if (cover.isEmpty()) {
            cover = doc.selectFirst("div.thumb img, img.cover, .book-info img")?.let {
                it.attr("abs:src").ifEmpty { it.attr("abs:data-src") }
            }.orEmpty()
        }
        if (desc.isEmpty()) {
            desc = doc.selectFirst("div.summary, div.description, div.synopsis")?.text().orEmpty()
        }

        val chapters = mutableListOf<Chapter>()

        // Try API chapters
        runCatching {
            val chUrl = "$apiUrl/$bookId/chapters?page=1&limit=500"
            val chReq = Request.Builder().url(chUrl).header("User-Agent", USER_AGENT).build()
            val chResp = client.newCall(chReq).execute().body?.string().orEmpty()
            if (chResp.isNotBlank()) {
                val chJson = JSONObject(chResp)
                val chArr = chJson.optJSONObject("data")?.optJSONArray("chapters")
                    ?: chJson.optJSONArray("data")
                    ?: chJson.optJSONArray("chapters")
                if (chArr != null && chArr.length() > 0) {
                    for (i in 0 until chArr.length()) {
                        val ch = chArr.getJSONObject(i)
                        val chSlug = ch.optString("slug").ifEmpty { ch.optString("id") }.ifEmpty { "${i + 1}" }
                        val chName = ch.optString("name").ifEmpty { ch.optString("title") }.ifEmpty { "Chapter ${i + 1}" }
                        val numMatch = Regex("""(?:chapter\s*|ch\.?\s*)([\d.]+)""", RegexOption.IGNORE_CASE).find(chName)
                            ?: Regex("""chapter-([\d]+)""", RegexOption.IGNORE_CASE).find(chSlug)
                        val num = ch.optDouble("number", (numMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: (i + 1).toDouble())).toFloat()
                        chapters.add(
                            Chapter(
                                id = "$slug/$chSlug",
                                title = chName,
                                number = num,
                            )
                        )
                    }
                }
            }
        }

        // Fallback: DOM chapter links
        if (chapters.isEmpty()) {
            doc.select("ul.chapter-list li a, .chapters-list li a, .list-chapter li a").forEachIndexed { i, a ->
                val href = a.attr("abs:href")
                val cSlug = href.substringAfterLast("/")
                if (cSlug.isNotEmpty() && cSlug != slug) {
                    val cName = a.text().trim().ifEmpty { "Chapter ${i + 1}" }
                    val numMatch = Regex("""(?:chapter\s*|ch\.?\s*)([\d.]+)""", RegexOption.IGNORE_CASE).find(cName)
                    val num = numMatch?.groupValues?.get(1)?.toFloatOrNull() ?: (i + 1).toFloat()
                    chapters.add(
                        Chapter(
                            id = "$slug/$cSlug",
                            title = cName,
                            number = num,
                        )
                    )
                }
            }
        }

        chapters.sortBy { it.number }

        MangaDetail(
            id = slug,
            title = title,
            coverUrl = cover,
            description = desc,
            provider = id,
            url = url,
            chapters = chapters,
        )
    }

    override suspend fun getChapterText(chapterId: String): String = withContext(Dispatchers.IO) {
        val parts = chapterId.trim('/').split('/')
        val slug = parts.firstOrNull().orEmpty()
        val chSlug = parts.drop(1).joinToString("/")
        val url = if (chapterId.startsWith("http")) chapterId else "$baseUrl/$slug/$chSlug"
        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)

        val content = doc.selectFirst("div.novel-tts-content, .chapter-content, #chapter-content, div.content")
            ?: return@withContext ""
        content.select("script, style, iframe, .ads, .hidden").remove()
        content.html()
    }

    private fun fetchApiItems(url: String): List<MangaSearchResult> {
        return try {
            val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            val resp = client.newCall(req).execute().body?.string().orEmpty()
            if (resp.isBlank()) return emptyList()
            val json = JSONObject(resp)
            val items = json.optJSONObject("data")?.optJSONArray("items")
                ?: json.optJSONArray("data")
                ?: json.optJSONArray("titles")
                ?: return emptyList()

            val list = mutableListOf<MangaSearchResult>()
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val slug = item.optString("slug").ifEmpty { item.optString("id") }
                if (slug.isNotEmpty()) {
                    val title = item.optString("name").ifEmpty { item.optString("title") }.ifEmpty { slug }
                    val cover = item.optString("cover").ifEmpty { item.optString("image") }
                    list.add(
                        MangaSearchResult(
                            id = slug,
                            title = title,
                            coverUrl = cover,
                            provider = id,
                            url = "$baseUrl/$slug",
                        )
                    )
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun fetchHtmlCards(url: String): List<MangaSearchResult> {
        return try {
            val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), baseUrl)
            doc.select(".book-item, .novel-item, .list-novel .row").mapNotNull { el ->
                val a = el.selectFirst("h3 a, .title a, a[title]") ?: return@mapNotNull null
                val href = a.attr("abs:href")
                val slug = href.removePrefix(baseUrl).trim('/')
                if (slug.isEmpty()) return@mapNotNull null
                val title = a.attr("title").ifEmpty { a.text().trim() }
                val cover = el.selectFirst("img")?.let { it.attr("abs:src").ifEmpty { it.attr("abs:data-src") } }.orEmpty()
                MangaSearchResult(
                    id = slug,
                    title = title.ifEmpty { slug },
                    coverUrl = cover,
                    provider = id,
                    url = href,
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    }
}

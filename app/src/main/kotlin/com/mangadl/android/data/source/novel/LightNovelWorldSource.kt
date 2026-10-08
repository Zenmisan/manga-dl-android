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

class LightNovelWorldSource(private val client: OkHttpClient) : NovelSource {
    override val id = "lightnovelworld"
    override val name = "LightNovelWorld"
    override val baseUrl = "https://lightnovelworld.org"

    override suspend fun search(query: String, page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val apiUrl = "$baseUrl/api/search/?q=$encoded"
        try {
            val req = Request.Builder().url(apiUrl).header("User-Agent", USER_AGENT).build()
            val resp = client.newCall(req).execute().body?.string().orEmpty()
            if (resp.isNotBlank()) {
                val json = JSONObject(resp)
                val novels = json.optJSONArray("novels")
                if (novels != null && novels.length() > 0) {
                    val list = mutableListOf<MangaSearchResult>()
                    for (i in 0 until novels.length()) {
                        val obj = novels.getJSONObject(i)
                        val slug = obj.optString("slug").trim().removePrefix("/").removeSuffix("/")
                        if (slug.isNotEmpty()) {
                            val title = obj.optString("title").ifEmpty { slug }
                            var cover = obj.optString("cover_path")
                            if (cover.startsWith("/")) cover = "$baseUrl$cover"
                            list.add(
                                MangaSearchResult(
                                    id = slug,
                                    title = title,
                                    coverUrl = cover,
                                    provider = id,
                                    url = "$baseUrl/novel/$slug",
                                )
                            )
                        }
                    }
                    if (list.isNotEmpty()) return@withContext list
                }
            }
        } catch (_: Exception) {}

        fetchHtmlList("$baseUrl/genre-all/?page=$page&order=popular")
    }

    override suspend fun getPopular(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        fetchHtmlList("$baseUrl/genre-all/?page=$page&order=popular")
    }

    override suspend fun getLatest(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        fetchHtmlList("$baseUrl/genre-all/?page=$page&order=updates")
    }

    override suspend fun getMangaDetail(mangaId: String): MangaDetail = withContext(Dispatchers.IO) {
        val slug = mangaId.removePrefix("novel/").trim('/').substringAfterLast("/")
        val url = "$baseUrl/novel/$slug"
        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)

        val title = doc.selectFirst("h1.novel-title, meta[property=og:title]")?.let {
            if (it.tagName() == "meta") it.attr("content") else it.text()
        }?.trim().orEmpty().ifEmpty { slug }

        var cover = doc.selectFirst("meta[property=og:image]")?.attr("content").orEmpty()
        if (cover.isEmpty()) {
            cover = doc.selectFirst("img.novel-cover, .novel-cover img")?.attr("abs:src").orEmpty()
        }

        val desc = doc.selectFirst("div.summary-content, .novel-summary, .summary")?.text().orEmpty()

        val chapters = mutableListOf<Chapter>()
        var pageIdx = 1
        var hasMorePages = true
        while (hasMorePages && pageIdx <= 15) {
            val chaptersUrl = "$url/chapters/?page=$pageIdx"
            val cReq = Request.Builder().url(chaptersUrl).header("User-Agent", USER_AGENT).build()
            val cDoc = runCatching {
                Jsoup.parse(client.newCall(cReq).execute().body?.string().orEmpty(), chaptersUrl)
            }.getOrNull() ?: break

            val chapterCards = cDoc.select("div.chapters-grid > div.chapter-card, #chapter-list li, .chapter-list li")
            if (chapterCards.isEmpty()) {
                hasMorePages = false
            } else {
                for (card in chapterCards) {
                    val a = card.selectFirst("a")
                    val href = a?.attr("abs:href") ?: card.attr("onclick").let { click ->
                        Regex("location\\.href=['\"]([^'\"]+)['\"]").find(click)?.groupValues?.get(1).orEmpty()
                    }
                    if (href.isNotBlank()) {
                        val cSlug = href.substringAfter("/novel/$slug/").trim('/')
                        val chTitle = card.selectFirst("h3, .chapter-title, span")?.text().orEmpty().ifEmpty { "Chapter ${chapters.size + 1}" }
                        val numMatch = Regex("""(?:chapter\s*|ch\.?\s*)([\d.]+)""", RegexOption.IGNORE_CASE).find(chTitle)
                            ?: Regex("""chapter-(\d+)""").find(cSlug)
                        val num = numMatch?.groupValues?.get(1)?.toFloatOrNull() ?: (chapters.size + 1).toFloat()
                        chapters.add(
                            Chapter(
                                id = "$slug/$cSlug",
                                title = chTitle,
                                number = num,
                            )
                        )
                    }
                }
                val nextBtn = cDoc.selectFirst("a[rel=next], .pagination .next:not(.disabled)")
                if (nextBtn == null || chapterCards.size < 20) {
                    hasMorePages = false
                } else {
                    pageIdx++
                }
            }
        }

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
        val cleanId = chapterId.trim('/')
        val url = if (cleanId.startsWith("http")) cleanId else "$baseUrl/novel/$cleanId"
        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)

        val content = doc.selectFirst("div.chapter-text, #chapter-content, .chapter-content") ?: return@withContext ""
        content.select("script, style, ins, .ads, .ad, .advertisement, .chapter-nav").remove()
        content.html()
    }

    private fun fetchHtmlList(url: String): List<MangaSearchResult> {
        return try {
            val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), baseUrl)
            doc.select("div.recommendations-grid > div.recommendation-card, .novel-item, .book-item, li.novel").mapNotNull { card ->
                val a = card.selectFirst("a") ?: return@mapNotNull null
                val href = a.attr("abs:href")
                val slug = href.substringAfter("/novel/").trim('/').substringBefore("/")
                if (slug.length < 2) return@mapNotNull null
                val title = card.selectFirst("h3, .novel-title, .title")?.text().orEmpty().ifEmpty { a.text() }
                val img = card.selectFirst("img")
                val cover = img?.attr("abs:src")?.ifEmpty { img.attr("abs:data-src") }.orEmpty()

                MangaSearchResult(
                    id = slug,
                    title = title,
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

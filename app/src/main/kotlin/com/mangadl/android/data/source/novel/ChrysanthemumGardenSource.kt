package com.mangadl.android.data.source.novel

import com.mangadl.android.data.model.Chapter
import com.mangadl.android.data.model.MangaDetail
import com.mangadl.android.data.model.MangaSearchResult
import com.mangadl.android.data.source.NovelSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.jsoup.Jsoup
import java.net.URLEncoder

class ChrysanthemumGardenSource(private val client: OkHttpClient) : NovelSource {
    override val id: String = "chrysanthemumgarden"
    override val name: String = "Chrysanthemum Garden"
    override val baseUrl: String = "https://chrysanthemumgarden.com"

    override suspend fun search(query: String, page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase()
        // Try WP JSON endpoint first
        runCatching {
            val req = Request.Builder()
                .url("$baseUrl/wp-json/cg/novels")
                .header("User-Agent", USER_AGENT)
                .build()
            val body = client.newCall(req).execute().body?.string().orEmpty()
            if (body.startsWith("[")) {
                val array = JSONArray(body)
                val results = mutableListOf<MangaSearchResult>()
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val nName = item.optString("name")
                    val nLink = item.optString("link")
                    if (nName.lowercase().contains(q)) {
                        val slug = nLink.removePrefix(baseUrl).trim('/').removePrefix("novel/").trim('/')
                        results.add(
                            MangaSearchResult(
                                id = slug,
                                title = nName,
                                coverUrl = "",
                                provider = id,
                                url = nLink.ifEmpty { "$baseUrl/novel/$slug" },
                            )
                        )
                    }
                }
                if (results.isNotEmpty()) return@withContext results
            }
        }

        // Fallback: HTML search
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$baseUrl/?s=$encoded"
        fetchArticleCards(url)
    }

    override suspend fun getPopular(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/books/page/$page/"
        fetchArticleCards(url)
    }

    override suspend fun getLatest(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/books/page/$page/"
        fetchArticleCards(url)
    }

    override suspend fun getMangaDetail(mangaId: String): MangaDetail = withContext(Dispatchers.IO) {
        val slug = mangaId.trim('/').removePrefix("novel/").trim('/')
        val url = if (mangaId.startsWith("http")) mangaId else "$baseUrl/novel/$slug"
        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)

        doc.select(".novel-raw-title").remove()
        val title = doc.selectFirst("h1.novel-title")?.text()?.trim().orEmpty().ifEmpty { slug }
        val cover = doc.selectFirst("div.novel-cover img, .novel-cover img")?.let {
            it.attr("data-breeze").ifEmpty { it.attr("abs:src") }
        }.orEmpty()

        val desc = doc.select(".entry-content p").joinToString("\n\n") { it.text().trim() }.ifEmpty {
            doc.selectFirst("div.summary, div.description")?.text().orEmpty()
        }

        val chapters = mutableListOf<Chapter>()
        doc.select(".chapter-item a, div.chapter-list a").forEachIndexed { i, a ->
            val href = a.attr("abs:href")
            val cSlug = href.removePrefix(baseUrl).trim('/')
            val cTitle = a.text().trim().ifEmpty { "Chapter ${i + 1}" }
            val numMatch = Regex("""(?:chapter\s*|ch\.?\s*)([\d.]+)""", RegexOption.IGNORE_CASE).find(cTitle)
            val num = numMatch?.groupValues?.get(1)?.toFloatOrNull() ?: (i + 1).toFloat()
            if (cSlug.isNotEmpty()) {
                chapters.add(
                    Chapter(
                        id = cSlug,
                        title = cTitle,
                        number = num,
                    )
                )
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
        val url = if (chapterId.startsWith("http")) chapterId else "$baseUrl/$chapterId"
        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)

        val content = doc.selectFirst("#novel-content, .entry-content") ?: return@withContext ""

        // Anti-scraper cleanup: remove hidden obfuscated elements used by CG
        content.select("[style*='display:none'], [style*='display: none']").remove()
        content.select("[style*='visibility:hidden'], [style*='visibility: hidden']").remove()
        content.select("[style*='font-size:0'], [style*='font-size: 0']").remove()
        content.select("[style*='width:0'], [style*='width: 0']").remove()
        content.select(".chrys-ads, .announcement, .entry-content_content, script, style, .jum, iframe").remove()
        content.select("p").filter { it.text().contains("chrysanthemumgarden", ignoreCase = true) }.forEach { it.remove() }

        content.html()
    }

    private fun fetchArticleCards(url: String): List<MangaSearchResult> {
        return try {
            val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), baseUrl)
            doc.select("article").mapNotNull { article ->
                if (article.select("div.series-genres a").text().contains("Manhua", ignoreCase = true)) return@mapNotNull null
                val titleEl = article.selectFirst("h2.novel-title > a, h2 > a") ?: return@mapNotNull null
                val name = titleEl.text().trim()
                val href = titleEl.attr("abs:href")
                val slug = href.removePrefix(baseUrl).trim('/').removePrefix("novel/").trim('/')
                if (slug.isEmpty()) return@mapNotNull null
                val cover = article.selectFirst("div.novel-cover img")?.let {
                    it.attr("data-breeze").ifEmpty { it.attr("abs:src") }
                }.orEmpty()

                MangaSearchResult(
                    id = slug,
                    title = name,
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

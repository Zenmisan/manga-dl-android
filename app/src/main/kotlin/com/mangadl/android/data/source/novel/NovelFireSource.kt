package com.mangadl.android.data.source.novel

import com.mangadl.android.data.model.Chapter
import com.mangadl.android.data.model.MangaDetail
import com.mangadl.android.data.model.MangaSearchResult
import com.mangadl.android.data.source.NovelSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLEncoder

class NovelFireSource(private val client: OkHttpClient) : NovelSource {
    override val id = "novelfire"
    override val name = "NovelFire"
    override val baseUrl = "https://novelfire.net"

    override suspend fun search(query: String, page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$baseUrl/search/?keyword=$encoded&page=$page"
        fetchHtmlList(url)
    }

    override suspend fun getPopular(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/genre-all/sort-popular/status-all/all-novel?page=$page"
        fetchHtmlList(url)
    }

    override suspend fun getLatest(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/genre-all/sort-latest-release/status-all/all-novel?page=$page"
        fetchHtmlList(url)
    }

    override suspend fun getMangaDetail(mangaId: String): MangaDetail = withContext(Dispatchers.IO) {
        val slug = mangaId.removePrefix("book/").trim('/').substringAfterLast("/")
        val url = "$baseUrl/book/$slug"
        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)

        val title = doc.selectFirst("div.novel-info h1.novel-title, h1.novel-title, h1")?.text().orEmpty().ifEmpty { slug }
        val cover = doc.selectFirst("figure.cover img, .cover img")?.let {
            it.attr("abs:src").ifEmpty { it.attr("abs:data-src") }
        }.orEmpty()
        val desc = doc.selectFirst("meta[itemprop=description]")?.attr("content")
            ?: doc.selectFirst("div.summary, div.novel-summary, div.description")?.text().orEmpty()

        val chapters = mutableListOf<Chapter>()

        // Try AJAX chapter list first
        val postId = doc.selectFirst("a#novel-report")?.attr("report-post_id").orEmpty()
        if (postId.isNotEmpty()) {
            runCatching {
                val ajaxUrl = "$baseUrl/ajax/listChapterDataAjax"
                val formBody = FormBody.Builder()
                    .add("draw", "1")
                    .add("columns[0][data]", "n_sort")
                    .add("order[0][column]", "0")
                    .add("order[0][dir]", "asc")
                    .add("start", "0")
                    .add("length", "5000")
                    .add("post_id", postId)
                    .build()
                val ajaxReq = Request.Builder()
                    .url(ajaxUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("X-Requested-With", "XMLHttpRequest")
                    .post(formBody)
                    .build()
                val ajaxResp = client.newCall(ajaxReq).execute().body?.string().orEmpty()
                if (ajaxResp.isNotBlank()) {
                    val json = JSONObject(ajaxResp)
                    val data = json.optJSONArray("data")
                    if (data != null) {
                        for (i in 0 until data.length()) {
                            val obj = data.getJSONObject(i)
                            val cTitle = obj.optString("chapter_name").ifEmpty { "Chapter ${i + 1}" }
                            val cSlug = obj.optString("chapter_slug").ifEmpty { "chapter-${i + 1}" }
                            val num = obj.optDouble("n_sort", (i + 1).toDouble()).toFloat()
                            chapters.add(
                                Chapter(
                                    id = "$slug/$cSlug",
                                    title = cTitle,
                                    number = num,
                                )
                            )
                        }
                    }
                }
            }
        }

        // Fallback: parse chapter links from DOM
        if (chapters.isEmpty()) {
            doc.select("ul.chapter-list li a, .chapters-list li a, .list-chapter li a, a[href*='/chapter-']").forEachIndexed { idx, a ->
                val href = a.attr("abs:href")
                val cSlug = href.substringAfterLast("/")
                val strong = a.selectFirst("strong.chapter-title")
                val cTitle = (strong?.text() ?: a.attr("title").ifEmpty { a.text() }).ifEmpty { "Chapter ${idx + 1}" }
                val numMatch = Regex("""chapter-(\d+)""", RegexOption.IGNORE_CASE).find(cSlug)
                    ?: Regex("""(?:chapter\s*|ch\.?\s*)([\d.]+)""", RegexOption.IGNORE_CASE).find(cTitle)
                val num = numMatch?.groupValues?.get(1)?.toFloatOrNull() ?: (idx + 1).toFloat()
                chapters.add(
                    Chapter(
                        id = "$slug/$cSlug",
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
        val cleanId = chapterId.trim('/')
        val url = if (cleanId.startsWith("http")) cleanId else "$baseUrl/book/$cleanId"
        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)

        val content = doc.selectFirst("#content-body, #chapter-content, div.chapter-content, div.chapter-text") ?: return@withContext ""
        content.select("script, style, ins, .ads, .ad, .advertisement, .chapter-nav").remove()
        content.html()
    }

    private fun fetchHtmlList(url: String): List<MangaSearchResult> {
        return try {
            val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), baseUrl)
            doc.select("li.novel-item, .novel-item").mapNotNull { item ->
                val a = item.selectFirst("h4.novel-title a, h5 a, a[title], a") ?: return@mapNotNull null
                val href = a.attr("abs:href")
                val slug = href.substringAfter("/book/").trim('/').substringBefore("/")
                if (slug.length < 2) return@mapNotNull null
                val title = (item.selectFirst("h4.novel-title, h5.novel-title, .novel-title")?.text() ?: a.attr("title").ifEmpty { a.text() }).trim()
                val img = item.selectFirst("img")
                val cover = img?.let { it.attr("abs:data-src").ifEmpty { it.attr("abs:src") } }.orEmpty()

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

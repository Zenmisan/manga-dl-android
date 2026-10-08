package com.mangadl.android.data.source.novel

import com.mangadl.android.data.model.Chapter
import com.mangadl.android.data.model.MangaDetail
import com.mangadl.android.data.model.MangaSearchResult
import com.mangadl.android.data.source.NovelSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLEncoder

open class NovelFullSource(protected val client: OkHttpClient) : NovelSource {
    override val id: String = "novelfull"
    override val name: String = "NovelFull"
    override val baseUrl: String = "https://novelfull.com"

    override suspend fun search(query: String, page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$baseUrl/search?keyword=$encoded&page=$page"
        fetchHtmlList(url)
    }

    override suspend fun getPopular(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/hot-novel?page=$page"
        fetchHtmlList(url)
    }

    override suspend fun getLatest(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/latest-release-novel?page=$page"
        fetchHtmlList(url)
    }

    override suspend fun getMangaDetail(mangaId: String): MangaDetail = withContext(Dispatchers.IO) {
        val slug = mangaId.trim('/').removeSuffix(".html").substringAfterLast("/")
        val url = if (mangaId.startsWith("http")) mangaId else "$baseUrl/$slug.html"
        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)

        val title = doc.selectFirst("h3.title, h1.title, .novel-title")?.text().orEmpty().ifEmpty { slug }
        val cover = doc.selectFirst("div.book img, .novel-cover img, .info img")?.let {
            it.attr("abs:src").ifEmpty { it.attr("abs:data-src") }
        }.orEmpty()
        val desc = doc.selectFirst("div.desc-text, .summary, .description")?.text().orEmpty()

        val chapters = mutableListOf<Chapter>()

        // Try AJAX chapter options
        val novelId = doc.selectFirst("#rating, div[data-novel-id]")?.attr("data-novel-id").orEmpty()
        if (novelId.isNotEmpty()) {
            runCatching {
                val ajaxUrl = "$baseUrl/ajax-chapter-option?novelId=$novelId"
                val ajaxReq = Request.Builder().url(ajaxUrl).header("User-Agent", USER_AGENT).build()
                val ajaxDoc = Jsoup.parse(client.newCall(ajaxReq).execute().body?.string().orEmpty(), baseUrl)
                val options = ajaxDoc.select("select > option, li[data-chapter-item] > a")
                options.forEachIndexed { i, opt ->
                    val cUrl = opt.attr("value").ifEmpty { opt.attr("href") }
                    val cSlug = cUrl.trim('/').substringAfterLast("/")
                    val cName = opt.text().ifEmpty { "Chapter ${i + 1}" }
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

        // Fallback: DOM chapter list
        if (chapters.isEmpty()) {
            doc.select(".list-chapter li a, ul.list-chapter li a").forEachIndexed { i, a ->
                val href = a.attr("abs:href")
                val cSlug = href.trim('/').substringAfterLast("/")
                val cName = a.text().ifEmpty { "Chapter ${i + 1}" }
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
        val url = if (cleanId.startsWith("http")) cleanId else "$baseUrl/$cleanId"
        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)

        val content = doc.selectFirst("#chapter-content, #chr-content, .chapter-c") ?: return@withContext ""
        content.select("script, style, ins, .ads, .ad, .advertisement, iframe").remove()
        content.html()
    }

    protected open fun fetchHtmlList(url: String): List<MangaSearchResult> {
        return try {
            val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), baseUrl)
            doc.select(".list-novel .row, div.list > div.row, #list-page .list .row").mapNotNull { row ->
                val a = row.selectFirst("h3.truyen-title > a, .novel-title > a, a[title]") ?: return@mapNotNull null
                val href = a.attr("abs:href")
                val slug = href.trim('/').removeSuffix(".html").substringAfterLast("/")
                if (slug.length < 2) return@mapNotNull null
                val title = a.text().ifEmpty { a.attr("title") }
                val img = row.selectFirst("img")
                val cover = img?.let { it.attr("abs:src").ifEmpty { it.attr("abs:data-src") } }.orEmpty()

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
        protected const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    }
}

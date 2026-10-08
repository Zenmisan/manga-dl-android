package com.mangadl.android.data.source.novel

import com.mangadl.android.data.model.Chapter
import com.mangadl.android.data.model.MangaDetail
import com.mangadl.android.data.model.MangaSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLEncoder

open class ReadNovelFullSource(client: OkHttpClient) : NovelFullSource(client) {
    override val id: String = "readnovelfull"
    override val name: String = "ReadNovelFull"
    override val baseUrl: String = "https://readnovelfull.com"

    override suspend fun search(query: String, page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$baseUrl/novel-list/search?keyword=$encoded&page=$page"
        val results = fetchHtmlList(url)
        if (results.isNotEmpty()) results else fetchHtmlList("$baseUrl/search?keyword=$encoded&page=$page")
    }

    override suspend fun getPopular(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/most-popular-novel?page=$page"
        val results = fetchHtmlList(url)
        if (results.isNotEmpty()) results else fetchHtmlList("$baseUrl/hot-novel?page=$page")
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

        val title = doc.selectFirst("div.books > div.desc > h3.title, h3.title, h1.title, .novel-title")
            ?.text().orEmpty().ifEmpty { slug }
        val cover = doc.selectFirst("div.books img, div.book img, .novel-cover img, .info img")?.let {
            it.attr("abs:src").ifEmpty { it.attr("abs:data-src") }
        }.orEmpty().replace("t-200x89", "t-300x439")
        val desc = doc.selectFirst("div.desc-text, .summary, .description")?.text().orEmpty()

        val chapters = mutableListOf<Chapter>()

        val novelId = doc.selectFirst("div#rating, div[data-novel-id]")?.attr("data-novel-id").orEmpty()
        if (novelId.isNotEmpty()) {
            runCatching {
                val archiveUrl = "$baseUrl/ajax/chapter-archive?novelId=$novelId"
                val archiveReq = Request.Builder().url(archiveUrl).header("User-Agent", USER_AGENT).build()
                val archiveDoc = Jsoup.parse(client.newCall(archiveReq).execute().body?.string().orEmpty(), baseUrl)
                val items = archiveDoc.select("ul.list-chapter li a, .panel-body ul.list-chapter li a")
                items.forEachIndexed { i, a ->
                    val href = a.attr("abs:href")
                    val cSlug = href.trim('/').removeSuffix(".html").substringAfterLast("/")
                    val cName = a.selectFirst("span")?.text()?.ifEmpty { a.text() } ?: a.text().ifEmpty { "Chapter ${i + 1}" }
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

        // Fallback: NovelFull-style AJAX or DOM
        if (chapters.isEmpty()) {
            val fallbackDetail = super.getMangaDetail(mangaId)
            return@withContext fallbackDetail.copy(
                id = slug,
                title = title,
                coverUrl = cover.ifEmpty { fallbackDetail.coverUrl },
                description = desc.ifEmpty { fallbackDetail.description },
                provider = id,
                url = url,
            )
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
        val rawHtml = super.getChapterText(chapterId)
        rawHtml.replace("[Updated from F r e e w e b n o v e l. c o m]", "")
            .replace("[Updated from Freewebnovel.com]", "")
            .replace("readnovelfull.com", "")
    }
}

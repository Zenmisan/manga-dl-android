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

class RoyalRoadSource(private val client: OkHttpClient) : NovelSource {
    override val id = "royalroad"
    override val name = "Royal Road"
    override val baseUrl = "https://www.royalroad.com"

    override suspend fun search(query: String, page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/fictions/search?title=${URLEncoder.encode(query, "UTF-8")}&page=$page"
        fetchList(url)
    }

    override suspend fun getPopular(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/fictions/best-rated?page=$page"
        fetchList(url)
    }

    override suspend fun getLatest(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/fictions/latest-updates?page=$page"
        fetchList(url)
    }

    override suspend fun getMangaDetail(mangaId: String): MangaDetail = withContext(Dispatchers.IO) {
        val url = if (mangaId.startsWith("http")) mangaId else "$baseUrl/fiction/$mangaId"
        val req = Request.Builder().url(url).build()
        val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)

        val title = doc.selectFirst("h1")?.text().orEmpty()
        val cover = doc.selectFirst(".cover-art-container img")?.attr("abs:src").orEmpty()
        val desc = doc.selectFirst(".description")?.text().orEmpty()
        val author = doc.selectFirst("h4 a")?.text().orEmpty()

        val chapters = mutableListOf<Chapter>()
        var num = 1f
        doc.select("#chapters tbody tr").forEach { tr ->
            val a = tr.selectFirst("a[href*='/chapter/']") ?: return@forEach
            val href = a.attr("abs:href")
            val cTitle = a.text()
            val chId = href.substringAfter("/chapter/").substringBefore("/")
            chapters.add(
                Chapter(
                    id = chId.ifEmpty { href },
                    title = cTitle,
                    number = num++,
                    publishedAt = tr.selectFirst("time")?.text().orEmpty(),
                )
            )
        }

        MangaDetail(
            id = mangaId,
            title = title,
            coverUrl = cover,
            description = desc,
            authors = if (author.isNotBlank()) listOf(author) else emptyList(),
            provider = id,
            url = url,
            chapters = chapters,
        )
    }

    override suspend fun getChapterText(chapterId: String): String = withContext(Dispatchers.IO) {
        val url = if (chapterId.startsWith("http")) chapterId else "$baseUrl/fiction/chapter/$chapterId"
        val req = Request.Builder().url(url).build()
        val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)

        val content = doc.selectFirst(".chapter-content") ?: return@withContext ""
        // Remove anti-scraping watermarks and scripts
        content.select("script, style, .portlet").remove()
        content.html()
    }

    private fun fetchList(url: String): List<MangaSearchResult> {
        return try {
            val req = Request.Builder().url(url).build()
            val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), baseUrl)
            doc.select(".fiction-list-item").mapNotNull { item ->
                val a = item.selectFirst("h2 a") ?: return@mapNotNull null
                val img = item.selectFirst("img")
                val href = a.attr("abs:href")
                val fId = href.substringAfter("/fiction/").substringBefore("/")

                MangaSearchResult(
                    id = fId,
                    title = a.text(),
                    coverUrl = img?.attr("abs:src").orEmpty(),
                    provider = id,
                    url = href,
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

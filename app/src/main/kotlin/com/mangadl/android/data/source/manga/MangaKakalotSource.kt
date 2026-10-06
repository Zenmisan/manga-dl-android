package com.mangadl.android.data.source.manga

import com.mangadl.android.data.model.Chapter
import com.mangadl.android.data.model.MangaDetail
import com.mangadl.android.data.model.MangaSearchResult
import com.mangadl.android.data.source.MangaSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

class MangaKakalotSource(private val client: OkHttpClient) : MangaSource {
    override val id = "mangakakalot"
    override val name = "MangaKakalot"
    override val baseUrl = "https://mangakakalot.com"

    override suspend fun search(query: String, page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val sanitized = query.trim().replace(" ", "_")
        val url = "$baseUrl/search/story/$sanitized"
        val req = Request.Builder().url(url).build()
        val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), baseUrl)

        doc.select(".story_item, .search-story-item").mapNotNull { el ->
            val a = el.selectFirst("h3 a, .item-title a") ?: return@mapNotNull null
            val img = el.selectFirst("img")
            val href = a.attr("abs:href")
            val mId = href.removePrefix(baseUrl).removePrefix("/").trim()
            MangaSearchResult(
                id = mId,
                title = a.text(),
                coverUrl = img?.attr("abs:src").orEmpty(),
                provider = id,
                url = href,
            )
        }
    }

    override suspend fun getPopular(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/manga_list?type=topview&category=all&state=all&page=$page"
        fetchList(url)
    }

    override suspend fun getLatest(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/manga_list?type=latest&category=all&state=all&page=$page"
        fetchList(url)
    }

    override suspend fun getMangaDetail(mangaId: String): MangaDetail = withContext(Dispatchers.IO) {
        val url = if (mangaId.startsWith("http")) mangaId else "$baseUrl/$mangaId"
        val req = Request.Builder().url(url).build()
        val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)

        val title = doc.selectFirst("h1, .manga-info-text h1, .story-info-right h1")?.text().orEmpty()
        val cover = doc.selectFirst(".manga-info-pic img, .info-image img")?.attr("abs:src").orEmpty()
        val desc = doc.selectFirst("#noidungm, .panel-story-info-description")?.text().orEmpty()

        val chapters = mutableListOf<Chapter>()
        doc.select(".chapter-list .row, .row-content-chapter li").forEach { row ->
            val a = row.selectFirst("a") ?: return@forEach
            val href = a.attr("abs:href")
            val cTitle = a.text()
            val num = Regex("""(?:chapter|ch\.)\s*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
                .find(cTitle)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
            chapters.add(
                Chapter(
                    id = href,
                    title = cTitle,
                    number = num,
                    publishedAt = row.selectFirst(".chapter-time, .item-time")?.text().orEmpty(),
                )
            )
        }

        MangaDetail(
            id = mangaId,
            title = title,
            coverUrl = cover,
            description = desc,
            provider = id,
            url = url,
            chapters = chapters,
        )
    }

    override suspend fun getPages(chapterId: String): List<String> = withContext(Dispatchers.IO) {
        val url = if (chapterId.startsWith("http")) chapterId else "$baseUrl/$chapterId"
        val req = Request.Builder().url(url).header("Referer", baseUrl).build()
        val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)

        doc.select(".container-chapter-reader img, .panel-read-story img").mapNotNull { img ->
            img.attr("abs:src").ifEmpty { img.attr("abs:data-src") }.takeIf { it.isNotBlank() }
        }
    }

    private fun fetchList(url: String): List<MangaSearchResult> {
        return try {
            val req = Request.Builder().url(url).build()
            val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), baseUrl)
            doc.select(".list-truyen-item-wrap").mapNotNull { el ->
                val a = el.selectFirst("h3 a") ?: return@mapNotNull null
                val img = el.selectFirst("img")
                val href = a.attr("abs:href")
                MangaSearchResult(
                    id = href.removePrefix(baseUrl).removePrefix("/"),
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

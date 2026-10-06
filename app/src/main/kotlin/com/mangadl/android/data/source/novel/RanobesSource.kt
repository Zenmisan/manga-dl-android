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
import org.jsoup.Jsoup

class RanobesSource(private val client: OkHttpClient) : NovelSource {
    override val id = "ranobes"
    override val name = "Ranobes"
    override val baseUrl = "https://ranobes.top"

    override suspend fun search(query: String, page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("do", "search")
            .add("subaction", "search")
            .add("search_start", "$page")
            .add("full_search", "0")
            .add("result_from", "1")
            .add("story", query)
            .build()

        val req = Request.Builder()
            .url("$baseUrl/index.php?do=search")
            .post(body)
            .build()

        val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), baseUrl)
        doc.select(".short-story, article.story").mapNotNull { el ->
            val a = el.selectFirst("h2.title a, .title a") ?: return@mapNotNull null
            val img = el.selectFirst("img")
            val href = a.attr("abs:href")
            val slug = href.removePrefix(baseUrl).removePrefix("/").substringBefore(".html")

            MangaSearchResult(
                id = slug,
                title = a.text(),
                coverUrl = img?.attr("abs:src").orEmpty(),
                provider = id,
                url = href,
            )
        }
    }

    override suspend fun getPopular(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = if (page <= 1) "$baseUrl/ranking/" else "$baseUrl/ranking/page/$page/"
        fetchList(url)
    }

    override suspend fun getLatest(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = if (page <= 1) baseUrl else "$baseUrl/page/$page/"
        fetchList(url)
    }

    override suspend fun getMangaDetail(mangaId: String): MangaDetail = withContext(Dispatchers.IO) {
        val url = if (mangaId.startsWith("http")) mangaId else "$baseUrl/$mangaId.html"
        val req = Request.Builder().url(url).build()
        val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)

        val title = doc.selectFirst("h1.title, .r-fullstory-title")?.text().orEmpty().ifEmpty { mangaId }
        val cover = doc.selectFirst(".poster img, .r-fullstory-poster img")?.attr("abs:src").orEmpty()
        val desc = doc.selectFirst("#fs-info, .r-fullstory-desc")?.text().orEmpty()

        val chapters = mutableListOf<Chapter>()
        var num = 1f
        doc.select(".chapters-scroll a, .chapter-item a").forEach { a ->
            val href = a.attr("abs:href")
            val cTitle = a.text()
            chapters.add(
                Chapter(
                    id = href,
                    title = cTitle,
                    number = num++,
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

    override suspend fun getChapterText(chapterId: String): String = withContext(Dispatchers.IO) {
        val url = if (chapterId.startsWith("http")) chapterId else "$baseUrl/$chapterId"
        val req = Request.Builder().url(url).build()
        val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)

        val content = doc.selectFirst("#arr-block, .text") ?: return@withContext ""
        content.select("script, style, .ads").remove()
        content.html()
    }

    private fun fetchList(url: String): List<MangaSearchResult> {
        return try {
            val req = Request.Builder().url(url).build()
            val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), baseUrl)
            doc.select(".short-story, article.story, .ranking-item").mapNotNull { el ->
                val a = el.selectFirst("h2.title a, .title a, a.name") ?: return@mapNotNull null
                val img = el.selectFirst("img")
                val href = a.attr("abs:href")
                val slug = href.removePrefix(baseUrl).removePrefix("/").substringBefore(".html")

                MangaSearchResult(
                    id = slug,
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

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

class NovelBinSource(private val client: OkHttpClient) : NovelSource {
    override val id = "novelbin"
    override val name = "NovelBin"
    override val baseUrl = "https://novelbin.me"

    override suspend fun search(query: String, page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/search?keyword=${URLEncoder.encode(query, "UTF-8")}&page=$page"
        fetchList(url)
    }

    override suspend fun getPopular(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/sort/top-view-novel?page=$page"
        fetchList(url)
    }

    override suspend fun getLatest(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/sort/latest-novel?page=$page"
        fetchList(url)
    }

    override suspend fun getMangaDetail(mangaId: String): MangaDetail = withContext(Dispatchers.IO) {
        val url = if (mangaId.startsWith("http")) mangaId else "$baseUrl/b/$mangaId"
        val req = Request.Builder().url(url).build()
        val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)

        val title = doc.selectFirst(".novel-title, h1, .title")?.text().orEmpty().ifEmpty { mangaId }
        val cover = doc.selectFirst(".book img, .novel-cover img")?.attr("abs:src").orEmpty()
        val desc = doc.selectFirst(".desc-text")?.text().orEmpty()

        val chapters = mutableListOf<Chapter>()
        var num = 1f
        doc.select(".list-chapter li a").forEach { a ->
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

        val content = doc.selectFirst("#chr-content, .chr-c") ?: return@withContext ""
        content.select("script, style, ins, .ads").remove()
        content.html()
    }

    private fun fetchList(url: String): List<MangaSearchResult> {
        return try {
            val req = Request.Builder().url(url).build()
            val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), baseUrl)
            doc.select(".list-novel .row, .col-novel-main .list .row").mapNotNull { row ->
                val a = row.selectFirst(".novel-title a") ?: return@mapNotNull null
                val img = row.selectFirst("img")
                val href = a.attr("abs:href")
                val nId = href.substringAfter("/b/").substringBefore("/").substringBefore("?")

                MangaSearchResult(
                    id = nId.ifEmpty { href },
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

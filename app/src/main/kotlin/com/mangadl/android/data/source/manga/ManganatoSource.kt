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
import org.jsoup.nodes.Document
import java.net.URLEncoder

class ManganatoSource(private val client: OkHttpClient) : MangaSource {
    override val id = "manganato"
    override val name = "Manganato"
    override val baseUrl = "https://manganato.com"
    private val readBase = "https://readmanganato.com"

    private fun fetchDoc(url: String): Document {
        val req = Request.Builder().url(url).build()
        return Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)
    }

    private fun parseCards(doc: Document): List<MangaSearchResult> {
        val seen = mutableSetOf<String>()
        return doc.select(".content-genres-item, .story_item, .list-story-item").mapNotNull { el ->
            val a = el.selectFirst("h3 a, a.genres-item-img, a[href*=readmanganato], a[href*=chapmanganato]")
                ?: return@mapNotNull null
            val href = a.attr("href")
            val mid = href.trimEnd('/').substringAfterLast('/')
            if (mid.isEmpty() || !seen.add(mid)) return@mapNotNull null
            val img = el.selectFirst("img")
            val titleEl = el.selectFirst("h3 a, .genres-item-name")
            MangaSearchResult(
                id = mid,
                title = titleEl?.text()?.trim() ?: mid,
                coverUrl = img?.attr("abs:src")?.ifEmpty { img.attr("abs:data-src") }.orEmpty(),
                provider = id,
                url = href,
            )
        }
    }

    override suspend fun search(query: String, page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val q = query.lowercase().replace(Regex("\\s+"), "_")
        val suffix = if (page > 1) "?page=$page" else ""
        parseCards(fetchDoc("$baseUrl/search/story/${URLEncoder.encode(q, "UTF-8")}$suffix"))
    }

    override suspend fun getPopular(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        parseCards(fetchDoc("$baseUrl/genre-all/$page?type=topview"))
    }

    override suspend fun getLatest(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        parseCards(fetchDoc("$baseUrl/genre-all/$page"))
    }

    override suspend fun getMangaDetail(mangaId: String): MangaDetail = withContext(Dispatchers.IO) {
        val url = "$readBase/manga-$mangaId"
        val doc = fetchDoc(url)
        val title = doc.selectFirst(".story-info-right h1, .manga-info-text h1")?.text()?.trim() ?: mangaId
        val cover = doc.selectFirst(".story-info-left img, .manga-info-pic img")
            ?.let { it.attr("abs:src").ifEmpty { it.attr("abs:data-src") } }.orEmpty()
        val desc = doc.selectFirst("#panel-story-info-description, #noidungm")
            ?.text()?.replace("Description :", "")?.trim().orEmpty()

        val chapters = doc.select(".row-content-chapter li, .chapter-list .row").mapNotNull { row ->
            val a = row.selectFirst("a") ?: return@mapNotNull null
            val href = a.attr("href")
            val chSlug = href.trimEnd('/').substringAfterLast('/')
            val num = Regex("([\\d.]+)").find(chSlug)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
            val dateEl = row.selectFirst("span[title], .chapter-time")
            Chapter(
                id = "$mangaId/$chSlug",
                title = a.text().trim(),
                number = num,
                publishedAt = dateEl?.attr("title")?.ifEmpty { dateEl.text().trim() }.orEmpty(),
            )
        }

        MangaDetail(
            id = mangaId, title = title, coverUrl = cover, description = desc,
            provider = id, url = url, chapters = chapters,
        )
    }

    override suspend fun getPages(chapterId: String): List<String> = withContext(Dispatchers.IO) {
        val chSlug = chapterId.substringAfterLast('/')
        val mangaId = chapterId.substringBeforeLast('/')
        val url = "$readBase/manga-$mangaId/$chSlug"
        val doc = fetchDoc(url)
        doc.select(".container-chapter-reader img").mapNotNull { img ->
            val src = img.attr("abs:src").ifEmpty { img.attr("abs:data-src") }
            if (src.startsWith("http")) src else null
        }
    }
}

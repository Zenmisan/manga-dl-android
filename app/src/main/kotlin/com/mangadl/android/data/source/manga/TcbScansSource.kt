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

class TcbScansSource(private val client: OkHttpClient) : MangaSource {
    override val id = "tcbscans"
    override val name = "TCB Scans"
    override val baseUrl = "https://tcbscans.me"

    private fun fetchDoc(url: String): Document {
        val req = Request.Builder().url(url).build()
        return Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)
    }

    private fun parseCards(doc: Document): List<MangaSearchResult> {
        val seen = mutableSetOf<String>()
        return doc.select("a[href*=/mangas/]").mapNotNull { a ->
            val href = a.attr("href")
            val mid = href.substringAfter("/mangas/", "").trimEnd('/')
            if (mid.isEmpty() || mid.contains('/') || !seen.add(mid)) return@mapNotNull null
            val img = a.selectFirst("img")
            val titleEl = a.selectFirst("p, span, .text-sm")
            MangaSearchResult(
                id = mid,
                title = titleEl?.text()?.trim() ?: mid.replace('-', ' '),
                coverUrl = img?.attr("abs:src")?.ifEmpty { img.attr("abs:data-src") }.orEmpty(),
                provider = id,
                url = "$baseUrl/mangas/$mid",
            )
        }
    }

    // TCB Scans is a small site — filter from the projects list rather than a real search endpoint.
    override suspend fun search(query: String, page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val all = parseCards(fetchDoc("$baseUrl/projects"))
        val q = query.lowercase()
        all.filter { it.title.lowercase().contains(q) }
    }

    override suspend fun getPopular(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        parseCards(fetchDoc("$baseUrl/projects"))
    }

    override suspend fun getLatest(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        parseCards(fetchDoc(baseUrl))
    }

    override suspend fun getMangaDetail(mangaId: String): MangaDetail = withContext(Dispatchers.IO) {
        val doc = fetchDoc("$baseUrl/mangas/$mangaId")
        val title = doc.selectFirst("h1, .text-4xl, .font-bold")?.text()?.trim() ?: mangaId
        val cover = doc.selectFirst("img[src*=cover], .rounded img")
            ?.let { it.attr("abs:src").ifEmpty { it.attr("abs:data-src") } }.orEmpty()
        val desc = doc.selectFirst(".prose, [class*=description]")?.text()?.trim().orEmpty()

        val chapters = doc.select("a[href*=/chapters/]").mapNotNull { a ->
            val chId = a.attr("href").substringAfter("/chapters/", "").trimEnd('/')
            if (chId.isEmpty()) return@mapNotNull null
            val text = a.text().trim()
            val num = Regex("([\\d.]+)").find(text)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
            Chapter(id = chId, title = text, number = num)
        }

        MangaDetail(
            id = mangaId, title = title, coverUrl = cover, description = desc,
            provider = id, url = "$baseUrl/mangas/$mangaId", chapters = chapters,
        )
    }

    override suspend fun getPages(chapterId: String): List<String> = withContext(Dispatchers.IO) {
        val doc = fetchDoc("$baseUrl/chapters/$chapterId")
        doc.select("picture img, .image-container img, .reader img").mapNotNull { img ->
            val src = img.attr("abs:src").ifEmpty { img.attr("abs:data-src") }
            if (src.startsWith("http")) src else null
        }
    }
}

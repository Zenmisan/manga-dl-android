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
import org.jsoup.nodes.Element
import java.net.URLEncoder

class MangaKatanaSource(private val client: OkHttpClient) : MangaSource {
    override val id = "mangakatana"
    override val name = "MangaKatana"
    override val baseUrl = "https://mangakatana.com"

    private fun fetchDoc(url: String): Document {
        val req = Request.Builder().url(url).build()
        return Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), baseUrl)
    }

    private fun fetchHtml(url: String): String {
        val req = Request.Builder().url(url).build()
        return client.newCall(req).execute().body?.string().orEmpty()
    }

    private fun titleCase(s: String) = s.split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

    private fun parseItems(doc: Document): List<MangaSearchResult> {
        val seen = mutableSetOf<String>()
        return doc.select("#book_list .item, .manga_list-sbs .item, .item").mapNotNull { item ->
            val a = item.selectFirst("h3.title a, .text h3 a, .title a, .text .title a, .info .title a, h3 a")
                ?: item.selectFirst("a[href*=/manga/]")
                ?: return@mapNotNull null
            val href = a.attr("href")
            if (!href.contains("/manga/")) return@mapNotNull null
            val slug = href.trimEnd('/').substringAfterLast('/')
            if (slug.isEmpty() || !seen.add(slug)) return@mapNotNull null
            val img = item.selectFirst(".media .wrap_img img, img")
            val rawTitle = a.text().trim()
            val title = (rawTitle.ifEmpty { slug.replace(Regex("\\.\\d+$"), "").replace('-', ' ') })
                .let { titleCase(it) }
            MangaSearchResult(
                id = slug,
                title = title,
                coverUrl = img?.attr("abs:data-src")?.ifEmpty { img.attr("abs:src") }.orEmpty(),
                provider = id,
                url = if (href.startsWith("http")) href else baseUrl + href,
            )
        }
    }

    override suspend fun search(query: String, page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val pageStr = if (page > 1) "/page/$page" else ""
        val doc = fetchDoc("$baseUrl$pageStr?search=${URLEncoder.encode(query, "UTF-8")}&search_by=m_name")
        parseItems(doc)
    }

    override suspend fun getPopular(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = if (page > 1) "$baseUrl/manga/page/$page" else "$baseUrl/manga"
        parseItems(fetchDoc(url))
    }

    override suspend fun getLatest(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = if (page > 1) "$baseUrl/latest/page/$page" else "$baseUrl/latest"
        parseItems(fetchDoc(url))
    }

    override suspend fun getMangaDetail(mangaId: String): MangaDetail = withContext(Dispatchers.IO) {
        val doc = fetchDoc("$baseUrl/manga/$mangaId")
        val rawTitle = doc.selectFirst("h1.heading, .info .heading, .heading, h1, .info h1")?.text()?.trim().orEmpty()
        val title = titleCase(rawTitle.ifEmpty { mangaId.replace(Regex("\\.\\d+$"), "").replace('-', ' ') })
        val cover = doc.selectFirst(".cover img, .media .wrap_img img")
            ?.let { it.attr("abs:data-src").ifEmpty { it.attr("abs:src") } }.orEmpty()
        val desc = doc.selectFirst(".summary p, .summary, .description")?.text()?.trim().orEmpty()
        val genres = doc.select(".genres a, .genre a").map { it.text().trim() }
        val authors = doc.select(".author a, .authors a").map { it.text().trim() }

        val chapters = doc.select(".chapters tr, .chapter_list tr").mapNotNull { row ->
            val a = row.selectFirst("a") ?: return@mapNotNull null
            val href = a.attr("href")
            val slug = href.trimEnd('/').substringAfterLast('/')
            val num = Regex("([\\d.]+)").find(slug)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
            Chapter(
                id = "$mangaId/$slug",
                title = a.text().trim().ifEmpty { "Chapter $num" },
                number = num,
            )
        }

        MangaDetail(
            id = mangaId, title = title, coverUrl = cover, description = desc,
            genres = genres, authors = authors, provider = id,
            url = "$baseUrl/manga/$mangaId", chapters = chapters,
        )
    }

    override suspend fun getPages(chapterId: String): List<String> = withContext(Dispatchers.IO) {
        val html = fetchHtml("$baseUrl/manga/$chapterId")
        var best: List<String> = emptyList()
        val re = Regex("""var\s+\w+\s*=\s*(\['[^']*'(?:,'[^']*')*,?]);""")
        re.findAll(html).forEach { m ->
            runCatching {
                var str = m.groupValues[1].replace("'", "\"").replace(Regex(",\\s*]"), "]")
                if (!str.endsWith("]")) str += "]"
                val arr = org.json.JSONArray(str)
                val list = (0 until arr.length()).map { arr.getString(it) }
                if (list.size > best.size && list.firstOrNull()?.startsWith("http") == true) best = list
            }
        }
        if (best.isNotEmpty()) return@withContext best

        val doc = Jsoup.parse(html, baseUrl)
        doc.select(".wrap_warpper img[data-src], #img_list img, .rd-content img").mapNotNull { img: Element ->
            img.attr("abs:data-src").ifEmpty { img.attr("abs:src") }.ifEmpty { null }
        }
    }
}

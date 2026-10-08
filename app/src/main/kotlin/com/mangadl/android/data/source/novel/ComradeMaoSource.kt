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

class ComradeMaoSource(private val client: OkHttpClient) : NovelSource {
    override val id: String = "comrademao"
    override val name: String = "Comrade Mao"
    override val baseUrl: String = "https://comrademao.com"

    override suspend fun search(query: String, page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$baseUrl/?s=$encoded&post_type=novel"
        parseBsCards(url)
    }

    override suspend fun getPopular(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/novel/?page=$page"
        parseBsCards(url)
    }

    override suspend fun getLatest(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/novel/?page=$page"
        parseBsCards(url)
    }

    override suspend fun getMangaDetail(mangaId: String): MangaDetail = withContext(Dispatchers.IO) {
        val slug = mangaId.trim('/').removePrefix("novel/").trim('/')
        val url = if (mangaId.startsWith("http")) mangaId else "$baseUrl/novel/$slug/"
        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)

        val novelInfo = doc.selectFirst("div.thumb > img")
        val title = novelInfo?.attr("title")?.replace(Regex("""\s*[–-]\s*Comrade Mao$""", RegexOption.IGNORE_CASE), "")?.trim()
            ?.ifEmpty { doc.selectFirst("h1.entry-title, h1")?.text()?.trim() }.orEmpty().ifEmpty { slug }

        val cover = novelInfo?.let { it.attr("abs:src").ifEmpty { it.attr("abs:data-src") } }.orEmpty()

        val desc = doc.select("div.wd-full p, div.entry-content p").lastOrNull()?.text()
            ?: doc.selectFirst("div.infox")?.text().orEmpty()

        val chapters = mutableListOf<Chapter>()
        val chapterElements = doc.select("li[data-num]")
        chapterElements.forEachIndexed { i, li ->
            val a = li.selectFirst("a") ?: return@forEachIndexed
            val href = a.attr("abs:href")
            val cSlug = href.removePrefix(baseUrl).trim('/')
            val cTitle = li.selectFirst(".chapternum")?.text()?.trim() ?: a.text().trim().ifEmpty { "Chapter ${i + 1}" }
            val dataNum = li.attr("data-num").toFloatOrNull()
            val numMatch = Regex("""(?:chapter\s*|ch\.?\s*)([\d.]+)""", RegexOption.IGNORE_CASE).find(cTitle)
            val num = dataNum ?: (numMatch?.groupValues?.get(1)?.toFloatOrNull() ?: (i + 1).toFloat())
            if (cSlug.isNotEmpty()) {
                chapters.add(
                    Chapter(
                        id = cSlug,
                        title = cTitle,
                        number = num,
                    )
                )
            }
        }

        // ComradeMao lists chapters descending, reverse to ascending order
        if (chapters.size > 1 && chapters.first().number > chapters.last().number) {
            chapters.reverse()
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
        val url = if (chapterId.startsWith("http")) chapterId else "$baseUrl/$chapterId"
        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)

        val content = doc.selectFirst("div[readability], div.entry-content, div.epcontent, #chapter-content")
            ?: return@withContext ""
        content.select("script, style, iframe, ins, .ads, .ad").remove()

        val html = content.html()
        html.replace(Regex("""\(end of this chapter\)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""comrademao\.com""", RegexOption.IGNORE_CASE), "")
    }

    private fun parseBsCards(url: String): List<MangaSearchResult> {
        return try {
            val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), baseUrl)
            doc.select(".bs").mapNotNull { card ->
                val a = card.selectFirst("a") ?: return@mapNotNull null
                val href = a.attr("abs:href")
                val slug = href.removePrefix(baseUrl).trim('/').removePrefix("novel/").trim('/')
                if (slug.isEmpty()) return@mapNotNull null
                val title = a.attr("title").ifEmpty { a.text() }
                    .replace(Regex("""\s*[–-]\s*Comrade Mao$""", RegexOption.IGNORE_CASE), "").trim()
                val img = card.selectFirst("img")
                val cover = img?.let { it.attr("abs:src").ifEmpty { it.attr("abs:data-src") } }.orEmpty()

                MangaSearchResult(
                    id = slug,
                    title = title.ifEmpty { slug },
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

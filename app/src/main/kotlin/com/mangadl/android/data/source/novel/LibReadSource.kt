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
import org.json.JSONObject
import org.jsoup.Jsoup

class LibReadSource(private val client: OkHttpClient) : NovelSource {
    override val id: String = "libread"
    override val name: String = "LibRead"
    override val baseUrl: String = "https://libread.com"

    override suspend fun search(query: String, page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val req = Request.Builder()
                .url("$baseUrl/search")
                .header("User-Agent", USER_AGENT)
                .header("Referer", baseUrl)
                .header("X-Requested-With", "XMLHttpRequest")
                .post(
                    FormBody.Builder()
                        .add("searchkey", query.trim())
                        .build()
                )
                .build()
            val resp = client.newCall(req).execute().body?.string().orEmpty()
            val doc = Jsoup.parse(resp, baseUrl)
            val list = parseRows(doc)
            if (list.isNotEmpty()) return@withContext list
        } catch (_: Exception) {}

        getPopular(page)
    }

    override suspend fun getPopular(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/sort/latest-release/$page"
        fetchHtmlList(url)
    }

    override suspend fun getLatest(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/sort/latest-novel/$page"
        fetchHtmlList(url)
    }

    override suspend fun getMangaDetail(mangaId: String): MangaDetail = withContext(Dispatchers.IO) {
        val slug = mangaId.removePrefix("novel/").trim('/').removeSuffix(".html")
        val url = if (mangaId.startsWith("http")) mangaId else "$baseUrl/$slug"
        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)

        val title = doc.selectFirst("h1.tit, h1.title, .novel-title")?.text().orEmpty().ifEmpty { slug }
        val cover = doc.selectFirst("picture source, div.pic img, .novel-cover img")?.let {
            it.attr("srcset").ifEmpty { it.attr("abs:src") }.ifEmpty { it.attr("abs:data-src") }
        }.orEmpty().let {
            if (it.contains(",")) it.substringAfterLast(",").trim().substringBefore(" ") else it
        }

        val desc = doc.selectFirst("div.inner, div.m-desc, div.description")?.text().orEmpty()

        val chapters = mutableListOf<Chapter>()

        // Article ID extraction
        val addEl = doc.selectFirst("a.set-case.add")
        var articleId = addEl?.attr("data-articleid").orEmpty()
        if (articleId.isEmpty()) {
            val m = Regex("""/(\d+)s\.(?:jpg|png|webp)""", RegexOption.IGNORE_CASE).find(cover)
            if (m != null) articleId = m.groupValues[1]
        }

        val acode = slug.substringBeforeLast("-").ifEmpty { slug }

        if (articleId.isNotEmpty()) {
            runCatching {
                val apiReq = Request.Builder()
                    .url("$baseUrl/api/chapterlist.php")
                    .header("User-Agent", USER_AGENT)
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("Referer", url)
                    .post(
                        FormBody.Builder()
                            .add("aid", articleId)
                            .add("acode", acode)
                            .add("cid", "1")
                            .build()
                    )
                    .build()
                val apiResp = client.newCall(apiReq).execute().body?.string().orEmpty()
                val html = if (apiResp.startsWith("{")) {
                    JSONObject(apiResp).optString("html")
                } else apiResp

                if (html.isNotBlank()) {
                    val cDoc = Jsoup.parse(html)
                    cDoc.select("option").forEachIndexed { i, opt ->
                        val cVal = opt.attr("value").substringAfterLast("/")
                        val cName = opt.text().ifEmpty { "Chapter ${i + 1}" }
                        val numMatch = Regex("""(?:chapter\s*|ch\.?\s*)([\d.]+)""", RegexOption.IGNORE_CASE).find(cName)
                        val num = numMatch?.groupValues?.get(1)?.toFloatOrNull() ?: (i + 1).toFloat()
                        chapters.add(
                            Chapter(
                                id = "$slug/$cVal",
                                title = cName,
                                number = num,
                            )
                        )
                    }
                }
            }
        }

        // Fallback: DOM chapter list
        if (chapters.isEmpty()) {
            doc.select("ul.ul-list5 li a, .chapter-list li a, .list-chapter li a").forEachIndexed { i, a ->
                val href = a.attr("abs:href")
                val cSlug = href.substringAfterLast("/")
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

        val content = doc.selectFirst("div.txt, #article, div.content, .novel-content, #chapter-content")
            ?: return@withContext ""
        content.select("script, style, iframe, ins, .ads, .ad, .advertisement").remove()

        val html = content.html()
        html.replace(Regex("""libread\.com""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""freewebnovel\.com""", RegexOption.IGNORE_CASE), "")
    }

    private fun fetchHtmlList(url: String): List<MangaSearchResult> {
        return try {
            val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), baseUrl)
            parseRows(doc)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseRows(doc: org.jsoup.nodes.Document): List<MangaSearchResult> {
        return doc.select("div.ul-list1 > div.li-row, div.li-row").mapNotNull { row ->
            val a = row.selectFirst("h3.tit > a, a[title]") ?: return@mapNotNull null
            val href = a.attr("abs:href")
            val slug = href.removePrefix(baseUrl).trim('/').removeSuffix(".html").ifEmpty { return@mapNotNull null }
            val title = a.attr("title").ifEmpty { a.text() }
            val cover = row.selectFirst("picture source, div.pic img")?.let {
                it.attr("srcset").ifEmpty { it.attr("abs:src") }.ifEmpty { it.attr("abs:data-src") }
            }.orEmpty().let {
                if (it.contains(",")) it.substringAfterLast(",").trim().substringBefore(" ") else it
            }

            MangaSearchResult(
                id = slug,
                title = title.ifEmpty { slug },
                coverUrl = cover,
                provider = id,
                url = href,
            )
        }
    }

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    }
}

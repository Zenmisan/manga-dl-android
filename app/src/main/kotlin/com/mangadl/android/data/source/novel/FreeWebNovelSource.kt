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
import java.net.URLEncoder

class FreeWebNovelSource(private val client: OkHttpClient) : NovelSource {
    override val id = "freewebnovel"
    override val name = "FreeWebNovel"
    override val baseUrl = "https://freewebnovel.com"

    override suspend fun search(query: String, page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$baseUrl/search?keyword=$encoded"
        fetchHtmlList(url)
    }

    override suspend fun getPopular(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/sort/most-popular/$page"
        fetchHtmlList(url)
    }

    override suspend fun getLatest(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/sort/latest-release/$page"
        fetchHtmlList(url)
    }

    override suspend fun getMangaDetail(mangaId: String): MangaDetail = withContext(Dispatchers.IO) {
        val slug = mangaId.removePrefix("novel/").trim('/').removeSuffix(".html")
        val url = "$baseUrl/novel/$slug.html"
        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)

        val title = doc.selectFirst("h1.tit, h1.title, .novel-title")?.text().orEmpty().ifEmpty { slug }
        val cover = doc.selectFirst("div.pic img, .novel-cover img")?.let {
            it.attr("abs:src").ifEmpty { it.attr("abs:data-src") }
        }.orEmpty()
        val desc = doc.selectFirst("div.inner, div.m-desc, div.description")?.text().orEmpty()

        val chapters = mutableListOf<Chapter>()

        // Fetch chapters via chapterlist.php
        val articleId = doc.selectFirst("a.set-case.add")?.attr("data-articleid")
            ?: doc.selectFirst("meta[name=image]")?.attr("content")?.substringAfterLast("/")?.substringBefore("s.jpg")
            ?: ""

        if (articleId.isNotEmpty()) {
            runCatching {
                val apiReq = Request.Builder()
                    .url("$baseUrl/api/chapterlist.php")
                    .header("User-Agent", USER_AGENT)
                    .header("X-Requested-With", "XMLHttpRequest")
                    .post(
                        FormBody.Builder()
                            .add("aid", articleId)
                            .add("acode", slug)
                            .add("cid", "1")
                            .build()
                    )
                    .build()
                val apiResp = client.newCall(apiReq).execute().body?.string().orEmpty()
                if (apiResp.isNotBlank()) {
                    val json = JSONObject(apiResp)
                    val html = json.optString("html")
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
        val url = if (cleanId.startsWith("http")) cleanId else "$baseUrl/novel/$cleanId"
        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)

        val content = doc.selectFirst("#chapter-content, #chr-content, div.inner, div.txt") ?: return@withContext ""
        content.select("script, style, ins, .ads, .ad, .advertisement, iframe").remove()
        content.html()
    }

    private fun fetchHtmlList(url: String): List<MangaSearchResult> {
        return try {
            val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), baseUrl)
            doc.select("div.ul-list1 > div.li-row, div.list > div.row, .col-novel-main .list .row").mapNotNull { row ->
                val a = row.selectFirst("h3.tit > a, .novel-title a") ?: return@mapNotNull null
                val href = a.attr("abs:href")
                val slug = href.substringAfter("/novel/").trim('/').removeSuffix(".html")
                if (slug.length < 2) return@mapNotNull null
                val title = a.attr("title").ifEmpty { a.text() }
                val img = row.selectFirst("img")
                val cover = img?.let { it.attr("abs:src").ifEmpty { it.attr("abs:data-src") } }.orEmpty()

                MangaSearchResult(
                    id = slug,
                    title = title,
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

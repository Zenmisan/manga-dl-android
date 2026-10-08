package com.mangadl.android.data.source.novel

import com.mangadl.android.data.model.Chapter
import com.mangadl.android.data.model.MangaDetail
import com.mangadl.android.data.model.MangaSearchResult
import com.mangadl.android.data.source.NovelSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLEncoder

class BrightNovelSource(private val client: OkHttpClient) : NovelSource {
    override val id: String = "brightnovel"
    override val name: String = "Bright Novel"
    override val baseUrl: String = "https://brightnovels.com"

    override suspend fun search(query: String, page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val apiUrl = "$baseUrl/api/search?query=$encoded"
        try {
            val req = Request.Builder().url(apiUrl).header("User-Agent", USER_AGENT).build()
            val resp = client.newCall(req).execute().body?.string().orEmpty()
            if (resp.isNotBlank()) {
                val json = JSONObject(resp)
                val seriesList = json.optJSONObject("data")?.optJSONArray("series")
                if (seriesList != null && seriesList.length() > 0) {
                    val list = mutableListOf<MangaSearchResult>()
                    for (i in 0 until seriesList.length()) {
                        val s = seriesList.getJSONObject(i)
                        val slug = s.optString("slug").trim()
                        if (slug.isNotEmpty()) {
                            val title = s.optString("title").ifEmpty { slug }
                            val cover = s.optJSONObject("cover")?.optString("url").orEmpty()
                            list.add(
                                MangaSearchResult(
                                    id = slug,
                                    title = title,
                                    coverUrl = cover,
                                    provider = id,
                                    url = "$baseUrl/series/$slug",
                                )
                            )
                        }
                    }
                    if (list.isNotEmpty()) return@withContext list
                }
            }
        } catch (_: Exception) {}

        getPopular(page)
    }

    override suspend fun getPopular(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/series?page=$page"
        parseInertiaPage(url)
    }

    override suspend fun getLatest(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/series?page=$page&order=desc"
        parseInertiaPage(url)
    }

    override suspend fun getMangaDetail(mangaId: String): MangaDetail = withContext(Dispatchers.IO) {
        val slug = mangaId.trim('/').removePrefix("series/").trim('/')
        val url = if (mangaId.startsWith("http")) mangaId else "$baseUrl/series/$slug"
        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)

        var title = slug
        var cover = ""
        var desc = ""

        val dataPage = doc.selectFirst("div#app")?.attr("data-page").orEmpty()
        if (dataPage.isNotBlank()) {
            runCatching {
                val json = JSONObject(dataPage)
                val s = json.optJSONObject("props")?.optJSONObject("series")
                if (s != null) {
                    title = s.optString("title").ifEmpty { slug }
                    cover = s.optJSONObject("cover")?.optString("url").orEmpty()
                    desc = s.optString("description")
                }
            }
        }

        if (title == slug) {
            title = doc.selectFirst("h1")?.text()?.trim().orEmpty().ifEmpty { slug }
        }
        if (cover.isEmpty()) {
            cover = doc.selectFirst("img.object-cover, img[alt*='cover'], .series-cover img")?.let {
                it.attr("abs:src").ifEmpty { it.attr("abs:data-src") }
            }.orEmpty()
        }
        if (desc.isEmpty()) {
            desc = doc.selectFirst("div.description, div.prose")?.text().orEmpty()
        }

        val chapters = mutableListOf<Chapter>()

        // Fetch free chapters from API
        runCatching {
            val chUrl = "$baseUrl/series/$slug/chapters/free?loaded=0&sort_order=desc"
            val chReq = Request.Builder().url(chUrl).header("User-Agent", USER_AGENT).build()
            val chResp = client.newCall(chReq).execute().body?.string().orEmpty()
            if (chResp.isNotBlank()) {
                val chJson = JSONObject(chResp)
                val chArr = chJson.optJSONArray("chapters")
                if (chArr != null) {
                    val rawList = mutableListOf<Chapter>()
                    for (i in 0 until chArr.length()) {
                        val chObj = chArr.getJSONObject(i)
                        if (chObj.optBoolean("is_premium", false)) continue
                        val chSlug = chObj.optString("slug")
                        val chName = chObj.optString("name").ifEmpty { "Chapter ${i + 1}" }
                        val numMatch = Regex("""(?:chapter\s*|ch\.?\s*)([\d.]+)""", RegexOption.IGNORE_CASE).find(chName)
                            ?: Regex("""chapter-([\d]+)""", RegexOption.IGNORE_CASE).find(chSlug)
                        val num = numMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
                        rawList.add(
                            Chapter(
                                id = "$slug/$chSlug",
                                title = chName,
                                number = num,
                            )
                        )
                    }
                    // Reverse to chronological order (API returns desc)
                    rawList.reverse()
                    rawList.forEachIndexed { idx, ch ->
                        chapters.add(if (ch.number == 0f) ch.copy(number = (idx + 1).toFloat()) else ch)
                    }
                }
            }
        }

        // Fallback: DOM chapter links
        if (chapters.isEmpty()) {
            doc.select("a[href*='/series/$slug/']").forEachIndexed { i, a ->
                val href = a.attr("abs:href")
                val cSlug = href.substringAfterLast("/")
                if (cSlug.isNotEmpty() && cSlug != slug) {
                    val cName = a.text().trim().ifEmpty { "Chapter ${i + 1}" }
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
        val parts = chapterId.trim('/').split('/')
        val seriesSlug = parts.firstOrNull().orEmpty()
        val chSlug = parts.drop(1).joinToString("/")
        val url = if (chapterId.startsWith("http")) chapterId else "$baseUrl/series/$seriesSlug/$chSlug"
        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)

        val dataPage = doc.selectFirst("div#app")?.attr("data-page").orEmpty()
        if (dataPage.isNotBlank()) {
            runCatching {
                val json = JSONObject(dataPage)
                val content = json.optJSONObject("props")?.optJSONObject("chapter")?.optString("content").orEmpty()
                if (content.isNotBlank()) {
                    return@withContext content
                }
            }
        }

        val content = doc.selectFirst("div.prose, #chapter-content, .content") ?: return@withContext ""
        content.select("script, style, iframe, .ads").remove()
        content.html()
    }

    private fun parseInertiaPage(url: String): List<MangaSearchResult> {
        return try {
            val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), baseUrl)
            val dataPage = doc.selectFirst("div#app")?.attr("data-page").orEmpty()
            if (dataPage.isNotBlank()) {
                val json = JSONObject(dataPage)
                val arr = json.optJSONObject("props")?.optJSONObject("seriesList")?.optJSONArray("data")
                if (arr != null && arr.length() > 0) {
                    val list = mutableListOf<MangaSearchResult>()
                    for (i in 0 until arr.length()) {
                        val s = arr.getJSONObject(i)
                        val slug = s.optString("slug").trim()
                        if (slug.isNotEmpty()) {
                            val title = s.optString("title").ifEmpty { slug }
                            val cover = s.optJSONObject("cover")?.optString("url").orEmpty()
                            list.add(
                                MangaSearchResult(
                                    id = slug,
                                    title = title,
                                    coverUrl = cover,
                                    provider = id,
                                    url = "$baseUrl/series/$slug",
                                )
                            )
                        }
                    }
                    if (list.isNotEmpty()) return list
                }
            }

            // Fallback: DOM
            doc.select("a[href*='/series/']").mapNotNull { a ->
                val href = a.attr("abs:href")
                val slug = href.removePrefix(baseUrl).trim('/').removePrefix("series/").trim('/')
                if (slug.isEmpty()) return@mapNotNull null
                val title = a.selectFirst("h3, h2, .title")?.text() ?: a.text().trim()
                val cover = a.selectFirst("img")?.attr("abs:src").orEmpty()
                MangaSearchResult(
                    id = slug,
                    title = title.ifEmpty { slug },
                    coverUrl = cover,
                    provider = id,
                    url = href,
                )
            }.distinctBy { it.id }
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    }
}

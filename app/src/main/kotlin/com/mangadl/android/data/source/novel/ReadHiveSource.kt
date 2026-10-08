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

class ReadHiveSource(private val client: OkHttpClient) : NovelSource {
    override val id: String = "readhive"
    override val name: String = "ReadHive"
    override val baseUrl: String = "https://readhive.org"

    override suspend fun search(query: String, page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val body = FormBody.Builder()
                .add("query", query.trim())
                .add("action", "search")
                .build()
            val req = Request.Builder()
                .url("$baseUrl/ajax")
                .header("User-Agent", USER_AGENT)
                .post(body)
                .build()
            val resp = client.newCall(req).execute().body?.string().orEmpty()
            if (resp.isNotBlank()) {
                val json = JSONObject(resp)
                val data = json.optJSONArray("data")
                if (data != null && data.length() > 0) {
                    val results = mutableListOf<MangaSearchResult>()
                    for (i in 0 until data.length()) {
                        val item = data.getJSONObject(i)
                        val itemUrl = item.optString("url").replace("\\", "")
                        val m = Regex("""/series/(\d+)""").find(itemUrl)
                        val novelId = m?.groupValues?.get(1) ?: item.optString("id")
                        if (novelId.isNotBlank()) {
                            val title = item.optString("title").ifEmpty { "Novel $novelId" }
                            var thumb = item.optString("thumb").replace("\\", "")
                            if (thumb.startsWith("//")) thumb = "https:$thumb"
                            else if (thumb.startsWith("/")) thumb = "$baseUrl$thumb"

                            results.add(
                                MangaSearchResult(
                                    id = novelId,
                                    title = title,
                                    coverUrl = thumb,
                                    provider = id,
                                    url = if (itemUrl.startsWith("http")) itemUrl else "$baseUrl/series/$novelId/",
                                )
                            )
                        }
                    }
                    if (results.isNotEmpty()) return@withContext results
                }
            }
        } catch (_: Exception) {}

        getPopular(page)
    }

    override suspend fun getPopular(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/page/$page/"
        parseCardList(url)
    }

    override suspend fun getLatest(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        getPopular(page)
    }

    override suspend fun getMangaDetail(mangaId: String): MangaDetail = withContext(Dispatchers.IO) {
        val cleanId = mangaId.filter { it.isDigit() }.ifEmpty { mangaId.trim('/').substringAfterLast('/') }
        val url = if (mangaId.startsWith("http")) mangaId else "$baseUrl/series/$cleanId/"
        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)

        val title = doc.selectFirst("h1, .entry-title")?.text()?.trim().orEmpty().ifEmpty { "Novel $cleanId" }
        val cover = doc.selectFirst("img.object-cover, .poster img, img[alt*='thumbnail']")?.let {
            it.attr("abs:src").ifEmpty { it.attr("abs:data-src") }
        }.orEmpty()

        val desc = doc.select("div.mb-4 > p, section div.mb-4 p, .description p")
            .joinToString("\n\n") { it.text().trim() }.ifEmpty {
                doc.selectFirst("main")?.text().orEmpty()
            }

        val chapters = mutableListOf<Chapter>()
        val seen = mutableSetOf<String>()
        val chLinks = doc.select("a[href*='/series/$cleanId/']")
        chLinks.forEach { a ->
            val href = a.attr("abs:href").trimEnd('/')
            val lastPart = href.substringAfterLast('/')
            if (lastPart.isEmpty() || lastPart == cleanId || lastPart == "series" || seen.contains(lastPart)) return@forEach
            seen.add(lastPart)

            val chSpan = a.selectFirst("div > div > span, span")
            val rawName = chSpan?.text()?.trim() ?: a.text().trim().ifEmpty { "Chapter $lastPart" }
            val chTitle = rawName.replace(Regex("""\d+\s+(?:years?|months?|weeks?|days?|hours?)\s+ago""", RegexOption.IGNORE_CASE), "").trim()
            val numMatch = Regex("""(\d+)""").find(lastPart)
            val num = numMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f

            chapters.add(
                Chapter(
                    id = "$cleanId/$lastPart",
                    title = chTitle.ifEmpty { "Chapter $lastPart" },
                    number = num,
                )
            )
        }

        chapters.sortBy { it.number }

        MangaDetail(
            id = cleanId,
            title = title,
            coverUrl = cover,
            description = desc,
            provider = id,
            url = url,
            chapters = chapters,
        )
    }

    override suspend fun getChapterText(chapterId: String): String = withContext(Dispatchers.IO) {
        val path = chapterId.trim('/')
        val url = if (path.startsWith("http")) path else "$baseUrl/series/$path"
        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)

        val content = doc.selectFirst("main > div.prose, main div[class*='prose'], div.prose, #chapter-content, .entry-content, main")
            ?: return@withContext ""
        content.select("script, style, iframe, button, noscript, .advertisement, [class*='ad-']").remove()
        content.html()
    }

    private fun parseCardList(url: String): List<MangaSearchResult> {
        return try {
            val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), baseUrl)
            val results = mutableListOf<MangaSearchResult>()
            val seen = mutableSetOf<String>()

            doc.select("a.peer, a.col-span-2, a[href*='/series/']").forEach { a ->
                val href = a.attr("abs:href")
                val m = Regex("""/series/(\d+)""").find(href) ?: return@forEach
                val id = m.groupValues[1]
                if (seen.contains(id)) return@forEach
                seen.add(id)

                val img = a.selectFirst("img")
                val rawTitle = img?.attr("alt").orEmpty()
                val title = rawTitle.replace(Regex("""\s+thumbnail$""", RegexOption.IGNORE_CASE), "").trim()
                    .ifEmpty { a.text().trim() }.ifEmpty { "Novel $id" }
                val cover = img?.let { it.attr("abs:src").ifEmpty { it.attr("abs:data-src") } }.orEmpty()

                results.add(
                    MangaSearchResult(
                        id = id,
                        title = title,
                        coverUrl = cover,
                        provider = this.id,
                        url = "$baseUrl/series/$id/",
                    )
                )
            }
            results
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    }
}

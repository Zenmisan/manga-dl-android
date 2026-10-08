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

class AsiaNovelSource(private val client: OkHttpClient) : NovelSource {
    override val id: String = "asianovel"
    override val name: String = "Asian Novel"
    override val baseUrl: String = "https://www.asianovel.net"

    override suspend fun search(query: String, page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8").replace("%20", "+")
        val url = "$baseUrl/?s=$encoded&post_type=any"
        fetchCardList(url)
    }

    override suspend fun getPopular(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/stories/page/$page/?order=desc&orderby=modified"
        fetchCardList(url)
    }

    override suspend fun getLatest(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/stories/page/$page/?order=desc&orderby=date"
        fetchCardList(url)
    }

    override suspend fun getMangaDetail(mangaId: String): MangaDetail = withContext(Dispatchers.IO) {
        val slug = mangaId.trim('/').removePrefix("story/").trim('/')
        val url = if (mangaId.startsWith("http")) mangaId else "$baseUrl/story/$slug/"
        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)

        val title = doc.selectFirst("h1.story__identity-title, h1")?.text()?.trim().orEmpty().ifEmpty { slug }
        val cover = doc.selectFirst("img.wp-post-image, .story__identity-thumbnail img")?.let {
            it.attr("abs:src").ifEmpty { it.attr("abs:data-src") }
        }.orEmpty()

        val desc = doc.selectFirst("section.story__summary")?.text()?.trim().orEmpty()

        val chapters = mutableListOf<Chapter>()
        val chLinks = doc.select("div.chapter-group > ol > li a.chapter-group__list-item-link, a.chapter-group__list-item-link")
        chLinks.forEachIndexed { idx, a ->
            val href = a.attr("abs:href")
            val cSlug = href.removePrefix(baseUrl).trim('/')
            val cTitle = a.text().trim().ifEmpty { "Chapter ${idx + 1}" }
            val numMatch = Regex("""(?:chapter|\b)\s*([\d.]+)""", RegexOption.IGNORE_CASE).find(cTitle)
                ?: Regex("""chapter-([\d]+)""", RegexOption.IGNORE_CASE).find(cSlug)
            val num = numMatch?.groupValues?.get(1)?.toFloatOrNull() ?: (idx + 1).toFloat()
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
        val rawPath = chapterId.trim('/')
        val url = if (rawPath.startsWith("http")) rawPath else "$baseUrl/$rawPath/"
        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)

        val content = doc.selectFirst("section#chapter-content > div, section#chapter-content")
            ?: return@withContext ""
        content.select("script, style, iframe, .ads, .chapter-warning").remove()
        content.html()
    }

    private fun fetchCardList(url: String): List<MangaSearchResult> {
        return try {
            val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            val doc = Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), baseUrl)
            doc.select("section.search-results__content > ul > li.card, li.card, section > ul > li").mapNotNull { card ->
                val a = card.selectFirst("a[href*='/story/'], a") ?: return@mapNotNull null
                val href = a.attr("abs:href")
                val slug = href.removePrefix(baseUrl).trim('/').removePrefix("story/").trim('/')
                if (slug.isEmpty()) return@mapNotNull null
                val titleEl = card.selectFirst("h3, h2, .title")
                val title = titleEl?.text()?.trim() ?: a.text().trim()
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

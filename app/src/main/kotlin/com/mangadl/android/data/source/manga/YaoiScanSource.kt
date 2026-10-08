package com.mangadl.android.data.source.manga

import com.mangadl.android.data.model.Chapter
import com.mangadl.android.data.model.MangaDetail
import com.mangadl.android.data.model.MangaSearchResult
import com.mangadl.android.data.source.MangaSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URLEncoder
import kotlin.math.ceil

/**
 * Madara WordPress-theme scraper, parameterized for yaoiscan.com — the web project's backend
 * runs this same theme generically off `base_url` config (`template: "madara"` in
 * `js_extensions.py`); ported here as a standalone Kotlin class per the user's "pure Kotlin,
 * no runtime templates" decision rather than carrying a substitution mechanism into Kotlin.
 * Selectors mirror `assets/extensions/madara.template.js` (being removed as part of this track).
 */
class YaoiScanSource(private val client: OkHttpClient) : MangaSource {
    override val id = "yaoiscan"
    override val name = "YaoiScan"
    override val baseUrl = "https://yaoiscan.com"
    override val nsfw = true

    @Volatile private var genreCache: List<String>? = null

    private fun fetchDoc(url: String): Document {
        val req = Request.Builder().url(url).build()
        return Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)
    }

    private fun imgSrc(el: Element?): String {
        val img = el?.selectFirst("img") ?: return ""
        return img.attr("abs:data-src").ifEmpty { img.attr("abs:data-lazy-src") }
            .ifEmpty { img.attr("abs:data-wpfc-original-src") }
            .ifEmpty { img.attr("abs:src") }
            .ifEmpty { img.attr("srcset").substringBefore(' ') }
    }

    private val cardSel = ".c-tabs-item__content, .manga-item, .page-item-detail, .c-blog-post"

    private fun parseCard(card: Element): MangaSearchResult? {
        val a = card.selectFirst(
            ".post-title a, h3.h4 a, h3 a, h5 a, a[href*=/manga/], a[href*=/series/], a[href*=/webtoon/], a[href*=/serie/]"
        ) ?: return null
        val href = a.attr("href")
        val slug = href.trimEnd('/').substringAfterLast('/')
        if (slug.isEmpty()) return null
        val titleEl = card.selectFirst(".post-title, h3.h4, h3, h5")
        val title = titleEl?.text()?.trim()?.ifEmpty { null } ?: a.text().trim()
        return MangaSearchResult(id = slug, title = title, coverUrl = imgSrc(card), provider = id, url = href)
    }

    private fun parseCards(doc: Document): List<MangaSearchResult> {
        val seen = mutableSetOf<String>()
        return doc.select(cardSel).mapNotNull { parseCard(it) }.filter { seen.add(it.id) }
    }

    private fun loadGenres(): List<String> {
        genreCache?.let { return it }
        val genres = runCatching {
            val doc = fetchDoc("$baseUrl/manga/?page=1")
            val seen = mutableSetOf<String>()
            doc.select("a[href*=manga-genre]").mapNotNull { a ->
                val href = a.attr("href").let { if (it.endsWith("/")) it else "$it/" }
                if (!href.contains("/manga-genre/") || !seen.add(href)) null else href
            }
        }.getOrDefault(emptyList())
        genreCache = genres
        return genres
    }

    // page 1: sorted listing; pages 2+: round-robin genre pages (5 genres per batch) — mirrors
    // madara.template.js's _browseByPage exactly, since this site's plain pagination is unreliable.
    private suspend fun browseByPage(orderBy: String, page: Int): List<MangaSearchResult> = coroutineScope {
        val p = if (page > 0) page else 1
        val batchSize = 5

        if (p == 1) {
            val r = parseCards(fetchDoc("$baseUrl/manga/page/1/?m_orderby=$orderBy"))
            if (r.isNotEmpty()) return@coroutineScope r
        }

        val genres = loadGenres()
        if (genres.isEmpty()) {
            return@coroutineScope parseCards(fetchDoc("$baseUrl/manga/page/$p/?m_orderby=$orderBy"))
        }

        val offset = (if (p > 1) p else 1) - 1
        val totalBatches = ceil(genres.size / batchSize.toDouble()).toInt().coerceAtLeast(1)
        val batchIdx = ((offset - 1).mod(totalBatches))
        val genrePage = (offset - 1) / totalBatches + 1

        val start = batchIdx * batchSize
        val batch = genres.drop(start).take(batchSize)
        if (batch.isEmpty()) return@coroutineScope emptyList()

        val merged = batch.map { gUrl ->
            async(Dispatchers.IO) { runCatching { parseCards(fetchDoc("${gUrl}page/$genrePage/")) }.getOrDefault(emptyList()) }
        }.awaitAll().flatten()

        val seen = mutableSetOf<String>()
        merged.filter { seen.add(it.id) }
    }

    override suspend fun search(query: String, page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val p = if (page > 0) page else 1
        parseCards(fetchDoc("$baseUrl/?s=${URLEncoder.encode(query, "UTF-8")}&post_type=wp-manga&paged=$p"))
    }

    override suspend fun getPopular(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        browseByPage("views", page)
    }

    override suspend fun getLatest(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        browseByPage("latest", page)
    }

    override suspend fun getMangaDetail(mangaId: String): MangaDetail = withContext(Dispatchers.IO) {
        val paths = listOf("/manga/", "/series/", "/webtoon/", "/serie/")
        var doc: Document? = null
        var finalUrl = ""
        for (path in paths) {
            finalUrl = "$baseUrl$path$mangaId"
            doc = runCatching { fetchDoc(finalUrl) }.getOrNull()
            if (doc?.selectFirst("h1") != null) break
        }
        val resolvedDoc = doc ?: error("Manga details page not found")

        val title = resolvedDoc.selectFirst(".post-title h1, h1")?.text()?.trim() ?: mangaId
        val cover = imgSrc(resolvedDoc.selectFirst(".summary_image"))
        val desc = resolvedDoc.selectFirst(".description-summary, .summary-content, .manga-excerpt, .post-content_item p")
            ?.text()?.trim().orEmpty()
        val genres = resolvedDoc.select(".genres-content a, a[href*=manga-genre]").map { it.text().trim() }

        val seen = mutableSetOf<String>()
        val chapters = resolvedDoc.select(".wp-manga-chapter a").mapNotNull { a ->
            val href = a.attr("href")
            val slug = href.trimEnd('/').substringAfterLast('/')
            val fullId = href.removePrefix(baseUrl)
            if (!seen.add(fullId)) return@mapNotNull null
            val num = Regex("([\\d.]+)").find(slug)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
            Chapter(id = fullId, title = a.text().trim().ifEmpty { "Chapter $num" }, number = num)
        }.sortedByDescending { it.number }

        MangaDetail(
            id = mangaId, title = title, coverUrl = cover, description = desc, genres = genres,
            provider = id, url = finalUrl, chapters = chapters,
        )
    }

    override suspend fun getPages(chapterId: String): List<String> = withContext(Dispatchers.IO) {
        val doc = fetchDoc(baseUrl + chapterId)
        doc.select(".page-break img, img.wp-manga-chapter-img").mapNotNull { img ->
            var src = img.attr("abs:src").ifEmpty { img.attr("abs:data-src") }
                .ifEmpty { img.attr("abs:data-lazy-src") }.ifEmpty { img.attr("abs:data-cdn-src") }
                .trim()
            if (src.isEmpty()) null else {
                if (src.startsWith("//")) src = "https:$src"
                src
            }
        }
    }
}

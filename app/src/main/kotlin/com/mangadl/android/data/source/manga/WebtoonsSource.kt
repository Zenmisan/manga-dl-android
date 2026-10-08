package com.mangadl.android.data.source.manga

import com.mangadl.android.data.model.Chapter
import com.mangadl.android.data.model.MangaDetail
import com.mangadl.android.data.model.MangaSearchResult
import com.mangadl.android.data.source.MangaSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLEncoder

class WebtoonsSource(private val client: OkHttpClient) : MangaSource {
    override val id = "webtoons"
    override val name = "Webtoons"
    override val baseUrl = "https://www.webtoons.com"
    private val apiBase = "https://global.apis.naver.com/webtoon/webtoon/v1.0"

    private fun fetchDoc(url: String): Document {
        val req = Request.Builder().url(url).build()
        return Jsoup.parse(client.newCall(req).execute().body?.string().orEmpty(), url)
    }

    private fun fetchJson(url: String): JSONObject {
        val req = Request.Builder().url(url).build()
        return JSONObject(client.newCall(req).execute().body?.string().orEmpty())
    }

    private fun titleListOf(data: JSONObject): List<JSONObject> {
        val result = data.optJSONObject("result")
        val arr = result?.optJSONArray("titleList")
            ?: result?.optJSONArray("titles")
            ?: data.optJSONArray("titleList")
            ?: return emptyList()
        return (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    private fun toResult(t: JSONObject): MangaSearchResult? {
        val mangaId = (if (t.has("titleId")) t.optLong("titleId") else null)?.toString()
            ?: t.optString("title_no").ifEmpty { t.optString("id") }
        if (mangaId.isEmpty()) return null
        val urlSlug = t.optString("titleNameUrlEncoding").ifEmpty { mangaId }
        return MangaSearchResult(
            id = mangaId,
            title = t.optString("title").ifEmpty { t.optString("name", mangaId) },
            coverUrl = t.optString("thumbnail").ifEmpty { t.optString("thumbnailUrl") }.ifEmpty { t.optString("squareThumbnail") },
            provider = id,
            url = "$baseUrl/en/drama/$urlSlug/list?title_no=$mangaId",
        )
    }

    private fun popularOrLatest(path: String, categorySort: String, page: Int): List<MangaSearchResult> {
        val data = fetchJson("$apiBase/titlelist/$path?webtoonLanguage=en&pageSize=20&pageNo=$page")
        var list = titleListOf(data)
        if (list.isEmpty()) {
            val data2 = fetchJson(
                "$baseUrl/api/category?webtoonType=WEBTOON&languageCode=en&sortOrder=$categorySort&pageSize=20&pageNo=$page"
            )
            list = titleListOf(data2)
        }
        return list.mapNotNull { toResult(it) }
    }

    override suspend fun search(query: String, page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val doc = fetchDoc("$baseUrl/en/search?keyword=${URLEncoder.encode(query, "UTF-8")}")
        val seen = mutableSetOf<String>()
        doc.select(".card_item, .info_area, li[class*=card]").mapNotNull { el ->
            val a = el.selectFirst("a[href*=title_no=], a[href*=/en/]") ?: return@mapNotNull null
            val href = a.attr("href")
            val mid = Regex("title_no=(\\d+)").find(href)?.groupValues?.get(1) ?: return@mapNotNull null
            if (!seen.add(mid)) return@mapNotNull null
            val img = el.selectFirst("img")
            val titleEl = el.selectFirst(".subj, .title, p.subj, .info_area strong")
            MangaSearchResult(
                id = mid,
                title = titleEl?.text()?.trim() ?: mid,
                coverUrl = img?.attr("abs:src")?.ifEmpty { img.attr("abs:data-src") }.orEmpty(),
                provider = id,
                url = href,
            )
        }
    }

    override suspend fun getPopular(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        popularOrLatest("popular", "READ_COUNT", page)
    }

    override suspend fun getLatest(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        popularOrLatest("new", "NEW_ARRIVAL", page)
    }

    override suspend fun getMangaDetail(mangaId: String): MangaDetail = withContext(Dispatchers.IO) {
        val titleNo = mangaId.substringAfterLast('/')
        val doc = fetchDoc("$baseUrl/en/action/titledetail?title_no=$titleNo")
        val title = doc.selectFirst(".subj, h1.subj, .title")?.text()?.trim() ?: mangaId
        val cover = doc.selectFirst("#content img.detail_thumbnail, .detail_info img")
            ?.let { it.attr("abs:src").ifEmpty { it.attr("abs:data-src") } }.orEmpty()
        val desc = doc.selectFirst(".summary p, .grade_area p, .detail_summary")?.text()?.trim().orEmpty()
        val genres = doc.select(".genre, .sub_genre").map { it.text().trim() }

        val chapters = doc.select("#_listUl li, .detail_lst li").mapNotNull { li ->
            val a = li.selectFirst("a") ?: return@mapNotNull null
            val epNo = Regex("episode_no=(\\d+)").find(a.attr("href"))?.groupValues?.get(1) ?: return@mapNotNull null
            val epTitleEl = li.selectFirst(".subj span, .episode_num") ?: a
            val dateEl = li.selectFirst(".date, .col_date")
            Chapter(
                id = "$titleNo/$epNo",
                title = epTitleEl.text().trim().ifEmpty { "Episode $epNo" },
                number = epNo.toFloatOrNull() ?: 0f,
                publishedAt = dateEl?.text()?.trim().orEmpty(),
            )
        }

        MangaDetail(
            id = mangaId, title = title, coverUrl = cover, description = desc, genres = genres,
            provider = id, url = "$baseUrl/en/action/titledetail?title_no=$titleNo", chapters = chapters,
        )
    }

    override suspend fun getPages(chapterId: String): List<String> = withContext(Dispatchers.IO) {
        val episodeNo = chapterId.substringAfterLast('/')
        val titleNo = chapterId.substringBeforeLast('/')
        val doc = fetchDoc("$baseUrl/en/viewer?title_no=$titleNo&episode_no=$episodeNo")
        doc.select("#_imageList img, .viewer_img img, ._images img").mapNotNull { img ->
            val src = img.attr("abs:data-url").ifEmpty { img.attr("abs:src") }.ifEmpty { img.attr("abs:data-src") }
            if (src.startsWith("http")) src else null
        }
    }
}

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
import java.net.URLEncoder

class OmegaScansSource(private val client: OkHttpClient) : MangaSource {
    override val id = "omegascans"
    override val name = "Omega Scans"
    override val baseUrl = "https://omegascans.org"
    private val apiBase = "https://api.omegascans.org"

    private fun fetchJson(path: String): JSONObject {
        val req = Request.Builder().url(apiBase + path).build()
        return JSONObject(client.newCall(req).execute().body?.string().orEmpty())
    }

    private fun toResult(item: JSONObject): MangaSearchResult {
        val slug = item.optString("series_slug").ifEmpty { item.optString("slug") }
        return MangaSearchResult(
            id = slug,
            title = item.optString("title").ifEmpty { item.optString("name", "Unknown") },
            coverUrl = item.optString("thumbnail"),
            provider = id,
            url = "$baseUrl/series/$slug",
        )
    }

    private fun queryList(path: String): List<MangaSearchResult> {
        val data = fetchJson(path).optJSONArray("data") ?: return emptyList()
        return (0 until data.length()).mapNotNull { i ->
            val item = data.getJSONObject(i)
            if (item.optString("series_type") != "Comic") null else toResult(item)
        }
    }

    override suspend fun search(query: String, page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        queryList("/query?adult=true&query_string=${URLEncoder.encode(query, "UTF-8")}&page=$page&perPage=20")
    }

    override suspend fun getPopular(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        queryList("/query?adult=true&page=$page&perPage=20&order=desc&orderBy=total_views")
    }

    override suspend fun getLatest(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        queryList("/query?adult=true&page=$page&perPage=20&order=desc&orderBy=created_at")
    }

    override suspend fun getMangaDetail(mangaId: String): MangaDetail = withContext(Dispatchers.IO) {
        val series = fetchJson("/series/$mangaId")
        val chapData = runCatching {
            fetchJson("/chapter/query?page=1&perPage=1999&series_id=${series.optInt("id")}")
        }.getOrNull()

        val chapters = (chapData?.optJSONArray("data"))?.let { arr ->
            (0 until arr.length()).map { i ->
                val item = arr.getJSONObject(i)
                val slug = item.optString("chapter_slug").ifEmpty { item.optString("slug") }
                val num = slug.removePrefix("chapter-").toFloatOrNull() ?: 0f
                Chapter(
                    id = "$mangaId/$slug",
                    title = item.optString("chapter_name").ifEmpty { item.optString("name", slug) },
                    number = num,
                    publishedAt = item.optString("created_at"),
                )
            }
        } ?: emptyList()

        val genres = series.optJSONArray("genres")?.let { arr ->
            (0 until arr.length()).map { arr.getJSONObject(it).optString("name") }
        } ?: emptyList()
        val authors = series.optJSONArray("authors")?.let { arr ->
            (0 until arr.length()).map { arr.getJSONObject(it).optString("name") }
        } ?: emptyList()

        MangaDetail(
            id = mangaId,
            title = series.optString("title").ifEmpty { series.optString("name", "Unknown") },
            coverUrl = series.optString("thumbnail"),
            description = series.optString("description").ifEmpty { series.optString("summary") },
            genres = genres,
            authors = authors,
            provider = id,
            url = "$baseUrl/series/$mangaId",
            chapters = chapters,
        )
    }

    override suspend fun getPages(chapterId: String): List<String> = withContext(Dispatchers.IO) {
        val seriesSlug = chapterId.substringBefore('/')
        val chapterSlug = chapterId.substringAfter('/')
        runCatching {
            val data = fetchJson("/chapter/$seriesSlug/$chapterSlug")
            val images = data.getJSONObject("chapter").getJSONObject("chapter_data").getJSONArray("images")
            (0 until images.length()).map { images.getString(it) }
        }.getOrDefault(emptyList())
    }
}

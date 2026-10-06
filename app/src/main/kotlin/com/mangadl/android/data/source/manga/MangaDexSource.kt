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

class MangaDexSource(private val client: OkHttpClient) : MangaSource {
    override val id = "mangadex"
    override val name = "MangaDex"
    override val baseUrl = "https://mangadex.org"
    private val apiBase = "https://api.mangadex.org"

    override suspend fun search(query: String, page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val offset = (page - 1) * 20
        val url = "$apiBase/manga?limit=20&offset=$offset&includes[]=cover_art" +
            if (query.isNotBlank()) "&title=${java.net.URLEncoder.encode(query, "UTF-8")}" else ""
        fetchMangaList(url)
    }

    override suspend fun getPopular(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val offset = (page - 1) * 20
        val url = "$apiBase/manga?limit=20&offset=$offset&includes[]=cover_art&order[followedCount]=desc"
        fetchMangaList(url)
    }

    override suspend fun getLatest(page: Int): List<MangaSearchResult> = withContext(Dispatchers.IO) {
        val offset = (page - 1) * 20
        val url = "$apiBase/manga?limit=20&offset=$offset&includes[]=cover_art&order[latestUploadedChapter]=desc"
        fetchMangaList(url)
    }

    override suspend fun getMangaDetail(mangaId: String): MangaDetail = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$apiBase/manga/$mangaId?includes[]=cover_art&includes[]=author")
            .build()
        val jsonStr = client.newCall(req).execute().body?.string().orEmpty()
        val dataObj = JSONObject(jsonStr).optJSONObject("data") ?: return@withContext MangaDetail()

        val attrs = dataObj.optJSONObject("attributes") ?: JSONObject()
        val title = attrs.optJSONObject("title")?.let {
            it.optString("en").ifEmpty { it.optString("ja-ro").ifEmpty { it.keys().next() } }
        } ?: mangaId

        val desc = attrs.optJSONObject("description")?.optString("en").orEmpty()
        val status = attrs.optString("status")

        var coverFileName = ""
        val rels = dataObj.optJSONArray("relationships")
        if (rels != null) {
            for (i in 0 until rels.length()) {
                val rel = rels.getJSONObject(i)
                if (rel.optString("type") == "cover_art") {
                    coverFileName = rel.optJSONObject("attributes")?.optString("fileName").orEmpty()
                }
            }
        }
        val coverUrl = if (coverFileName.isNotEmpty()) {
            "https://uploads.mangadex.org/covers/$mangaId/$coverFileName.512.jpg"
        } else ""

        // Fetch chapters feed
        val chapters = fetchChapters(mangaId)

        MangaDetail(
            id = mangaId,
            title = title,
            coverUrl = coverUrl,
            description = desc,
            status = status,
            provider = id,
            url = "$baseUrl/title/$mangaId",
            chapters = chapters,
        )
    }

    override suspend fun getPages(chapterId: String): List<String> = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("$apiBase/at-home/server/$chapterId").build()
        val jsonStr = client.newCall(req).execute().body?.string().orEmpty()
        val json = JSONObject(jsonStr)

        val host = json.optString("baseUrl")
        val chapterObj = json.optJSONObject("chapter") ?: return@withContext emptyList()
        val hash = chapterObj.optString("hash")
        val dataArr = chapterObj.optJSONArray("data") ?: return@withContext emptyList()

        val pages = mutableListOf<String>()
        for (i in 0 until dataArr.length()) {
            val filename = dataArr.getString(i)
            pages.add("$host/data/$hash/$filename")
        }
        pages
    }

    private fun fetchMangaList(url: String): List<MangaSearchResult> {
        val req = Request.Builder().url(url).build()
        val jsonStr = client.newCall(req).execute().body?.string().orEmpty()
        if (jsonStr.isBlank()) return emptyList()

        val dataArr = JSONObject(jsonStr).optJSONArray("data") ?: return emptyList()
        val results = mutableListOf<MangaSearchResult>()

        for (i in 0 until dataArr.length()) {
            val item = dataArr.getJSONObject(i)
            val mangaId = item.optString("id")
            val attrs = item.optJSONObject("attributes") ?: JSONObject()
            val titleObj = attrs.optJSONObject("title") ?: JSONObject()
            val title = titleObj.optString("en").ifEmpty {
                val firstKey = titleObj.keys().asSequence().firstOrNull()
                if (firstKey != null) titleObj.optString(firstKey) else "Unknown"
            }

            var coverFileName = ""
            val rels = item.optJSONArray("relationships")
            if (rels != null) {
                for (j in 0 until rels.length()) {
                    val rel = rels.getJSONObject(j)
                    if (rel.optString("type") == "cover_art") {
                        coverFileName = rel.optJSONObject("attributes")?.optString("fileName").orEmpty()
                    }
                }
            }
            val coverUrl = if (coverFileName.isNotEmpty()) {
                "https://uploads.mangadex.org/covers/$mangaId/$coverFileName.256.jpg"
            } else ""

            results.add(
                MangaSearchResult(
                    id = mangaId,
                    title = title,
                    coverUrl = coverUrl,
                    provider = id,
                    url = "$baseUrl/title/$mangaId",
                )
            )
        }
        return results
    }

    private fun fetchChapters(mangaId: String): List<Chapter> {
        val url = "$apiBase/manga/$mangaId/feed?translatedLanguage[]=en&order[chapter]=desc&limit=100"
        val req = Request.Builder().url(url).build()
        val jsonStr = client.newCall(req).execute().body?.string().orEmpty()
        val dataArr = JSONObject(jsonStr).optJSONArray("data") ?: return emptyList()

        val chapters = mutableListOf<Chapter>()
        for (i in 0 until dataArr.length()) {
            val item = dataArr.getJSONObject(i)
            val chId = item.optString("id")
            val attrs = item.optJSONObject("attributes") ?: JSONObject()
            val chNumStr = attrs.optString("chapter")
            val chNum = chNumStr.toFloatOrNull() ?: 0f
            val chTitle = attrs.optString("title")
            val displayTitle = if (chTitle.isNotBlank()) "Ch. $chNumStr - $chTitle" else "Chapter $chNumStr"

            chapters.add(
                Chapter(
                    id = chId,
                    title = displayTitle,
                    number = chNum,
                    publishedAt = attrs.optString("publishAt"),
                )
            )
        }
        return chapters
    }
}

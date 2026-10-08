package com.mangadl.android.data.source

import android.content.Context
import com.mangadl.android.data.model.Chapter
import com.mangadl.android.data.model.MangaDetail
import com.mangadl.android.data.model.MangaSearchResult
import com.mangadl.android.data.source.manga.AsuraScansSource
import com.mangadl.android.data.source.manga.MangaDexSource
import com.mangadl.android.data.source.manga.MangaKakalotSource
import com.mangadl.android.data.source.novel.NovelBinSource
import com.mangadl.android.data.source.novel.RanobesSource
import com.mangadl.android.data.source.novel.RoyalRoadSource
import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentHashMap

/**
 * Central registry and dispatcher for all native Manga and Novel sources.
 * Replaces QuickJS and DOM_SHIM execution with pure-Kotlin scrapers.
 */
class SourceManager(
    private val context: Context,
    private val client: OkHttpClient,
) {
    private val sources = ConcurrentHashMap<String, BaseSource>()

    init {
        registerBuiltInSources()
    }

    private fun registerBuiltInSources() {
        // Native Manga Sources
        registerSource(MangaDexSource(client))
        registerSource(AsuraScansSource(client))
        registerSource(MangaKakalotSource(client))
        registerSource(com.mangadl.android.data.source.manga.MangaKatanaSource(client))
        registerSource(com.mangadl.android.data.source.manga.ManganatoSource(client))
        registerSource(com.mangadl.android.data.source.manga.OmegaScansSource(client))
        registerSource(com.mangadl.android.data.source.manga.TcbScansSource(client))
        registerSource(com.mangadl.android.data.source.manga.WebtoonsSource(client))
        registerSource(com.mangadl.android.data.source.manga.YaoiScanSource(client))

        // Native Novel Sources
        registerSource(NovelBinSource(client))
        registerSource(RanobesSource(client))
        registerSource(RoyalRoadSource(client))
    }

    fun registerSource(source: BaseSource) {
        sources[source.id.lowercase()] = source
    }

    fun getSource(id: String): BaseSource? = sources[id.lowercase()]

    fun listSources(): List<BaseSource> = sources.values.toList()

    fun listMangaSources(): List<MangaSource> = sources.values.filterIsInstance<MangaSource>()

    fun listNovelSources(): List<NovelSource> = sources.values.filterIsInstance<NovelSource>()

    fun isNovelSource(sourceId: String): Boolean {
        val src = getSource(sourceId)
        return src?.isNovel ?: false
    }

    suspend fun search(sourceId: String, query: String, page: Int = 1): List<MangaSearchResult> {
        val src = getSource(sourceId) ?: return emptyList()
        return src.search(query, page)
    }

    suspend fun getPopular(sourceId: String, page: Int = 1): List<MangaSearchResult> {
        val src = getSource(sourceId) ?: return emptyList()
        return src.getPopular(page)
    }

    suspend fun getLatest(sourceId: String, page: Int = 1): List<MangaSearchResult> {
        val src = getSource(sourceId) ?: return emptyList()
        return src.getLatest(page)
    }

    suspend fun getMangaDetail(sourceId: String, mangaId: String): MangaDetail {
        val src = getSource(sourceId) ?: return MangaDetail(id = mangaId, provider = sourceId)
        return src.getMangaDetail(mangaId)
    }

    suspend fun getPages(sourceId: String, chapterId: String): List<String> {
        val src = getSource(sourceId) as? MangaSource ?: return emptyList()
        return src.getPages(chapterId)
    }

    suspend fun getChapterText(sourceId: String, chapterId: String): String {
        val src = getSource(sourceId) as? NovelSource ?: return ""
        return src.getChapterText(chapterId)
    }
}

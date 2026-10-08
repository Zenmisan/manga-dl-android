package com.mangadl.android.data.extensions

import com.mangadl.android.data.model.MangaDetail
import com.mangadl.android.data.model.MangaSearchResult
import com.mangadl.android.data.source.MangaSource
import com.mangadl.android.data.source.NovelSource
import com.mangadl.android.data.source.SourceManager

/**
 * Thin facade over [SourceManager] kept for the many existing call sites (`listExtensions()`,
 * `isNovelSource()`) that predate the pure-Kotlin source migration — every source is now a
 * native `BaseSource`, there is no more QuickJS/`assets/extensions` scraper underneath this.
 */
class ExtensionManager(
    var sourceManager: SourceManager? = null,
) {
    fun listExtensions(): List<ExtensionMeta> =
        sourceManager?.listSources()
            ?.map { ExtensionMeta(id = it.id, name = it.name, lang = it.lang, nsfw = it.nsfw) }
            ?: emptyList()

    suspend fun search(extensionId: String, query: String, page: Int = 1): List<MangaSearchResult> =
        sourceManager?.getSource(extensionId)?.search(query, page) ?: emptyList()

    suspend fun getPopular(extensionId: String, page: Int = 1): List<MangaSearchResult> =
        sourceManager?.getSource(extensionId)?.getPopular(page) ?: emptyList()

    suspend fun getLatest(extensionId: String, page: Int = 1): List<MangaSearchResult> =
        sourceManager?.getSource(extensionId)?.getLatest(page) ?: emptyList()

    suspend fun getMangaDetail(extensionId: String, mangaId: String): MangaDetail =
        sourceManager?.getSource(extensionId)?.getMangaDetail(mangaId) ?: MangaDetail(id = mangaId, provider = extensionId)

    suspend fun getPages(extensionId: String, chapterId: String): List<String> =
        (sourceManager?.getSource(extensionId) as? MangaSource)?.getPages(chapterId) ?: emptyList()

    suspend fun getChapterText(extensionId: String, chapterId: String): String =
        (sourceManager?.getSource(extensionId) as? NovelSource)?.getChapterText(chapterId) ?: ""

    companion object {
        // Static id-set check — no Context/Application instance needed, so this stays callable
        // from plain (non-Robolectric) unit tests and any call site without a live SourceManager.
        fun isNovelSource(extensionId: String): Boolean =
            SourceManager.NOVEL_SOURCE_IDS.contains(extensionId.lowercase())
    }
}

data class ExtensionMeta(
    val id: String,
    val name: String,
    val lang: String = "en",
    val nsfw: Boolean = false,
    val version: String = "1.0.0",
)

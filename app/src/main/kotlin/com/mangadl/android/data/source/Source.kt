package com.mangadl.android.data.source

import com.mangadl.android.data.model.MangaDetail
import com.mangadl.android.data.model.MangaSearchResult

interface BaseSource {
    val id: String
    val name: String
    val baseUrl: String
    val lang: String get() = "en"
    val isNovel: Boolean get() = false
    val nsfw: Boolean get() = false

    suspend fun search(query: String, page: Int = 1): List<MangaSearchResult>
    suspend fun getPopular(page: Int = 1): List<MangaSearchResult>
    suspend fun getLatest(page: Int = 1): List<MangaSearchResult>
    suspend fun getMangaDetail(mangaId: String): MangaDetail
}

interface MangaSource : BaseSource {
    override val isNovel: Boolean get() = false
    suspend fun getPages(chapterId: String): List<String>
}

interface NovelSource : BaseSource {
    override val isNovel: Boolean get() = true
    suspend fun getChapterText(chapterId: String): String
}

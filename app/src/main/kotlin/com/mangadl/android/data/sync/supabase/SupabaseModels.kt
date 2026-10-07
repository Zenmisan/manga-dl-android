package com.mangadl.android.data.sync.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseMangaRecord(
    @SerialName("id") val id: String,
    @SerialName("provider") val provider: String,
    @SerialName("provider_manga_id") val providerMangaId: String,
    @SerialName("title") val title: String,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("url") val url: String = "",
    @SerialName("subscribed") val subscribed: Boolean = true,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("last_synced") val lastSynced: String? = null,
)

@Serializable
data class SupabaseReadTrackingRecord(
    @SerialName("user_id") val userId: String,
    @SerialName("provider") val provider: String,
    @SerialName("manga_id") val mangaId: String,
    @SerialName("chapter_ids") val chapterIds: List<String> = emptyList(),
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class SupabaseReadingProgressRecord(
    @SerialName("user_id") val userId: String,
    @SerialName("provider") val provider: String,
    @SerialName("manga_id") val mangaId: String,
    @SerialName("chapter_id") val chapterId: String,
    @SerialName("last_page") val lastPage: Int = 1,
    @SerialName("manga_title") val mangaTitle: String? = null,
    @SerialName("chapter_title") val chapterTitle: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

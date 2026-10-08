package com.mangadl.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommentItem(
    val id: String,
    @SerialName("user_id") val userId: String = "",
    val username: String = "",
    @SerialName("display_name") val displayName: String = "",
    val provider: String = "",
    @SerialName("manga_id") val mangaId: String = "",
    @SerialName("chapter_id") val chapterId: String? = null,
    @SerialName("parent_id") val parentId: String? = null,
    val body: String = "",
    val likes: Int = 0,
    val liked: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String? = null,
    val replies: List<CommentItem> = emptyList(),
)

@Serializable
data class CommentsResponse(
    val comments: List<CommentItem> = emptyList(),
    val total: Int = 0,
    val offset: Int = 0,
    val limit: Int = 20,
)

@Serializable
data class PostCommentPayload(
    val provider: String,
    @SerialName("manga_id") val mangaId: String,
    @SerialName("chapter_id") val chapterId: String? = null,
    @SerialName("parent_id") val parentId: String? = null,
    val body: String,
)

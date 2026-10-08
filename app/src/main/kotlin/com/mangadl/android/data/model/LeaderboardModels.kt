package com.mangadl.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardEntry(
    @SerialName("user_id") val userId: String = "",
    val username: String = "",
    @SerialName("display_name") val displayName: String = "",
    val bio: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
    @SerialName("chapters_read") val chaptersRead: Int = 0,
    @SerialName("manga_count") val mangaCount: Int = 0,
    @SerialName("streak_days") val streakDays: Int = 0,
    val score: Int = 0,
    val rank: Int = 0,
    @SerialName("hunter_rank") val hunterRank: HunterRankPayload? = null,
)

@Serializable
data class HunterRankPayload(
    @SerialName("rank_code") val rankCode: String = "E",
    @SerialName("rank_name") val rankName: String = "E-Rank Novice",
    val tier: String = "bronze",
)

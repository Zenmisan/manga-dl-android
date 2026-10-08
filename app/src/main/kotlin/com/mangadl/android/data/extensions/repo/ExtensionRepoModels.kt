package com.mangadl.android.data.extensions.repo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KeiyoushiExtensionItem(
    @SerialName("name") val name: String,
    @SerialName("pkg") val pkg: String,
    @SerialName("apk") val apk: String,
    @SerialName("lang") val lang: String = "en",
    @SerialName("code") val code: Long = 0,
    @SerialName("version") val version: String = "1.0.0",
    @SerialName("nsfw") val nsfw: Int = 0,
    @SerialName("hasReadme") val hasReadme: Int = 0,
    @SerialName("hasChangelog") val hasChangelog: Int = 0,
    @SerialName("sources") val sources: List<KeiyoushiSourceItem> = emptyList(),
)

@Serializable
data class KeiyoushiSourceItem(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("lang") val lang: String = "en",
    @SerialName("baseUrl") val baseUrl: String = "",
)

package com.mangadl.android.data.ui

import androidx.compose.ui.graphics.Color

data class Manga(
    val id: String,
    val title: String,
    val cover: Color,
    val unread: Int = 0,
    val downloaded: Boolean = false,
    val inLibrary: Boolean = false,
    val source: String = "MangaDex",
)

data class ContinueItem(val manga: Manga, val chapter: String, val progress: Float)

data class UiChapter(
    val number: String,
    val title: String,
    val meta: String,
    val read: Boolean = false,
    val downloaded: Boolean = false,
)

data class UpdateItem(val manga: Manga, val chapter: String)
data class UpdateGroup(val label: String, val items: List<UpdateItem>)

data class HistoryItem(val manga: Manga, val where: String, val whenText: String)

enum class NoticeKind { Info, Error, Success }
data class Notice(val title: String, val body: String, val whenText: String, val unread: Boolean, val kind: NoticeKind, val icon: String)

enum class DownloadState { Downloading, Queued, Paused, Failed, Done }
data class DownloadItem(val manga: Manga, val chapter: String, val status: String, val progress: Float, val state: DownloadState)

data class UiSource(val name: String, val meta: String, val initial: String, val color: Color)

enum class ExtensionState { UpdateAvailable, Installed, Available }
data class UiExtension(val name: String, val meta: String, val initial: String, val color: Color, val state: ExtensionState)

data class UiTracker(val name: String, val short: String, val color: Color, val connected: Boolean = false)

data class LocalFile(val name: String, val meta: String, val ext: String)

package com.mangadl.android.data.ui

import androidx.compose.ui.graphics.Color
import com.mangadl.android.data.extensions.ExtensionMeta
import com.mangadl.android.data.model.Chapter
import com.mangadl.android.data.model.DownloadEntry
import com.mangadl.android.data.model.LibraryManga
import com.mangadl.android.data.model.ReadingProgress
import java.text.SimpleDateFormat
import java.util.*

private val COVER_PALETTE = listOf(
    Color(0xFF1A2433), Color(0xFF3A1518), Color(0xFF2E2412), Color(0xFF2D1716),
    Color(0xFF13282A), Color(0xFF311A1F), Color(0xFF1D2A1A), Color(0xFF1B2030),
    Color(0xFF2B1A2E), Color(0xFF22222A), Color(0xFF1E3A5F), Color(0xFF1F2C4F),
)

fun LibraryManga.toUiManga(): Manga {
    val colorIndex = id.hashCode().let { if (it < 0) -it else it } % COVER_PALETTE.size
    return Manga(
        id = id,
        title = title,
        cover = COVER_PALETTE[colorIndex],
        source = provider,
        inLibrary = true,
        unread = (totalChapters - readCount).coerceAtLeast(0),
    )
}

fun Chapter.toUiChapter(progress: ReadingProgress? = null): UiChapter {
    val meta = buildString {
        if (publishedAt.isNotEmpty()) append(publishedAt)
        if (progress != null && !progress.completed && progress.page > 0) {
            if (isNotEmpty()) append(" · ")
            append("Page ${progress.page}")
        }
    }
    return UiChapter(
        number = if (number % 1f == 0f) number.toInt().toString() else number.toString(),
        title = title.ifEmpty { "Chapter $number" },
        meta = meta,
        read = progress?.completed ?: false,
    )
}

fun ExtensionMeta.toUiSource(): UiSource {
    val colorIdx = id.hashCode().let { if (it < 0) -it else it } % COVER_PALETTE.size
    return UiSource(
        name = name,
        meta = "$lang · v$version",
        initial = name.firstOrNull()?.uppercase() ?: "?",
        color = COVER_PALETTE[colorIdx],
    )
}

fun ExtensionMeta.toUiExtension(state: ExtensionState = ExtensionState.Installed): UiExtension {
    val colorIdx = id.hashCode().let { if (it < 0) -it else it } % COVER_PALETTE.size
    return UiExtension(
        name = name,
        meta = "$lang · v$version",
        initial = name.firstOrNull()?.uppercase() ?: "?",
        color = COVER_PALETTE[colorIdx],
        state = state,
    )
}

fun List<LibraryManga>.toUpdateGroups(): List<UpdateGroup> {
    if (isEmpty()) return emptyList()
    val recent = sortedByDescending { it.lastReadAt ?: it.addedAt }
    val now = System.currentTimeMillis()
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    val todayStart = cal.timeInMillis
    val yesterdayStart = todayStart - 86_400_000L
    val groups = mutableListOf<UpdateGroup>()
    fun bucket(items: List<LibraryManga>, label: String) {
        if (items.isEmpty()) return
        groups.add(UpdateGroup(label, items.map { UpdateItem(it.toUiManga(), "Ch. ${it.readCount + 1}") }))
    }
    bucket(recent.filter { (it.lastReadAt ?: it.addedAt) >= todayStart }, "Today")
    bucket(recent.filter { val t = it.lastReadAt ?: it.addedAt; t in yesterdayStart until todayStart }, "Yesterday")
    bucket(recent.filter { (it.lastReadAt ?: it.addedAt) < yesterdayStart }, "Earlier")
    return groups
}

fun List<ReadingProgress>.toHistoryItems(library: List<LibraryManga>): List<HistoryItem> {
    val mangaMap = library.associateBy { it.id }
    return this.mapNotNull { p ->
        val manga = mangaMap[p.mangaId]?.toUiManga() ?: return@mapNotNull null
        HistoryItem(
            manga = manga,
            where = p.chapterId.substringAfterLast('/').let { if (it.isBlank()) p.chapterId else "Ch. $it" },
            whenText = formatRelativeTime(p.readAt),
        )
    }.distinctBy { it.manga.id }
}

private fun formatRelativeTime(ms: Long): String {
    val diff = System.currentTimeMillis() - ms
    return when {
        diff < 60_000L -> "Just now"
        diff < 3_600_000L -> "${diff / 60_000}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000}h ago"
        diff < 604_800_000L -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(ms))
    }
}

fun List<LibraryManga>.toNotices(): List<Notice> {
    val now = System.currentTimeMillis()
    val weekMs = 7 * 86_400_000L
    return this
        .filter { (it.totalChapters - it.readCount) > 0 }
        .sortedByDescending { it.lastReadAt ?: it.addedAt }
        .map { m ->
            val unread = m.totalChapters - m.readCount
            val age = now - (m.lastReadAt ?: m.addedAt)
            val kind = if (age < weekMs) NoticeKind.Info else NoticeKind.Success
            Notice(
                title = m.title,
                body = "$unread unread chapter${if (unread == 1) "" else "s"} available",
                whenText = formatRelativeTime(m.lastReadAt ?: m.addedAt),
                unread = age < 86_400_000L,
                kind = kind,
                icon = "up",
            )
        }
}

fun List<ReadingProgress>.currentStreak(): Int {
    if (isEmpty()) return 0
    val msPerDay = 86_400_000L
    val todayDay = System.currentTimeMillis() / msPerDay
    val activeDays = map { it.readAt / msPerDay }.toHashSet()
    if (!activeDays.contains(todayDay) && !activeDays.contains(todayDay - 1)) return 0
    var streak = 0
    var day = if (activeDays.contains(todayDay)) todayDay else todayDay - 1
    while (activeDays.contains(day)) { streak++; day-- }
    return streak
}

fun DownloadEntry.toUiDownloadItem(): DownloadItem {
    val colorIndex = mangaId.hashCode().let { if (it < 0) -it else it } % COVER_PALETTE.size
    val manga = Manga(id = mangaId, title = mangaTitle, cover = COVER_PALETTE[colorIndex], source = provider)
    return toUiDownload(manga)
}

fun DownloadEntry.toUiDownload(manga: Manga): DownloadItem {
    val state = when (status) {
        "downloading" -> DownloadState.Downloading
        "queued" -> DownloadState.Queued
        "paused" -> DownloadState.Paused
        "failed" -> DownloadState.Failed
        "done" -> DownloadState.Done
        else -> DownloadState.Queued
    }
    val fraction = if (totalPages > 0) progress.toFloat() / totalPages else 0f
    val statusText = when (state) {
        DownloadState.Downloading -> "Downloading · $progress / $totalPages"
        DownloadState.Queued -> "Queued"
        DownloadState.Paused -> "Paused"
        DownloadState.Failed -> "Failed"
        DownloadState.Done -> "Done"
    }
    return DownloadItem(
        manga = manga,
        chapter = chapterTitle,
        status = statusText,
        progress = fraction,
        state = state,
    )
}

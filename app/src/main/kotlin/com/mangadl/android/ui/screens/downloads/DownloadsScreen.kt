package com.mangadl.android.ui.screens.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.data.ui.DownloadItem
import com.mangadl.android.data.ui.DownloadState
import com.mangadl.android.ui.components.BackHeader
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.ButtonTone
import com.mangadl.android.ui.components.CoverArt
import com.mangadl.android.ui.components.Divider
import com.mangadl.android.ui.components.MdIconButton
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.components.PillButton
import com.mangadl.android.ui.components.ProgressBar
import com.mangadl.android.ui.components.Screen
import com.mangadl.android.ui.theme.MdTheme

@Composable
fun DownloadsScreen(
    items: List<DownloadItem>,
    onBack: () -> Unit,
    isPaused: Boolean = false,
    storageBytes: Long = 0L,
    onTogglePauseAll: () -> Unit = {},
    onItemAction: (DownloadItem) -> Unit = {},
) {
    val c = MdTheme.colors
    val formattedStorage = formatBytes(storageBytes)
    val storageFraction = (storageBytes.toFloat() / (5L * 1024 * 1024 * 1024)).coerceIn(0f, 1f)

    Screen {
        BackHeader("Downloads", onBack) {
            PillButton(
                if (isPaused) "Resume All" else "Pause All",
                onTogglePauseAll,
                tone = ButtonTone.Primary,
                height = 40.dp
            )
        }
        BodyText(
            "${items.size} in queue · Wi-Fi only",
            Modifier.padding(start = 20.dp, bottom = 12.dp),
            size = 13.sp,
            color = c.fg.copy(alpha = 0.65f)
        )
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items) { d ->
                DownloadCard(d, onAction = { onItemAction(d) })
            }
        }
        Column(Modifier.fillMaxWidth()) {
            Divider(color = c.surfaceHigh)
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row {
                    BodyText("Device storage used by manga-dl", Modifier.weight(1f), size = 13.sp, color = c.fg.copy(alpha = 0.7f))
                    BodyText(formattedStorage, size = 13.sp, weight = FontWeight.Bold)
                }
                ProgressBar(storageFraction, color = c.fg, height = 6.dp)
            }
        }
    }
}

@Composable
fun DownloadCard(
    d: DownloadItem,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MdTheme.colors
    val stateColor = when (d.state) {
        DownloadState.Failed -> c.errorText
        DownloadState.Done -> c.successText
        else -> c.fg.copy(alpha = 0.65f)
    }
    val barColor = when (d.state) {
        DownloadState.Failed -> c.errorBar
        DownloadState.Done -> c.success
        DownloadState.Paused -> c.fg.copy(alpha = 0.4f)
        else -> c.accent
    }
    val (icon, label) = when (d.state) {
        DownloadState.Failed -> MdIcons.Refresh to "Retry"
        DownloadState.Paused -> MdIcons.Play to "Resume"
        DownloadState.Done -> MdIcons.Close to "Remove from queue"
        else -> MdIcons.Pause to "Pause"
    }
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface)
            .border(1.dp, c.surfaceHigh, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CoverArt(d.manga.cover, Modifier.size(36.dp, 54.dp), RoundedCornerShape(6.dp), imageUrl = d.manga.coverUrl)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BodyText("${d.manga.title} · ${d.chapter}", weight = FontWeight.Bold, maxLines = 1)
            BodyText(d.status, size = 12.sp, weight = FontWeight.SemiBold, color = stateColor)
            ProgressBar(d.progress, color = barColor)
        }
        MdIconButton(icon, label, onAction, background = c.surfaceHigh, iconSize = 18.dp)
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes.toDouble() / (1024 * 1024)
    if (mb < 1024) return String.format("%.1f MB", mb)
    val gb = mb / 1024
    return String.format("%.2f GB", gb)
}

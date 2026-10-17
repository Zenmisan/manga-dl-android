package com.mangadl.android.ui.screens.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val (totalStorageBytes, freeStorageBytes) = remember {
        runCatching {
            val stat = android.os.StatFs(context.filesDir.absolutePath)
            stat.totalBytes to stat.availableBytes
        }.getOrDefault((64L * 1024 * 1024 * 1024) to (32L * 1024 * 1024 * 1024))
    }
    val formattedStorage = formatBytes(storageBytes)
    val storageFraction = if (totalStorageBytes > 0) {
        (storageBytes.toFloat() / totalStorageBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Screen {
        BackHeader("Downloads", onBack) {
            if (items.isNotEmpty()) {
                PillButton(
                    if (isPaused) "Resume All" else "Pause All",
                    onTogglePauseAll,
                    tone = ButtonTone.Primary,
                    height = 36.dp
                )
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Storage Breakdown Card matching Stitch Mobile blueprint
            com.mangadl.android.ui.components.SurfaceCard(
                Modifier.fillMaxWidth(),
                radius = 14.dp,
                background = Color.White.copy(alpha = 0.05f),
                borderColor = Color.White.copy(alpha = 0.10f),
                padding = PaddingValues(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            BodyText("STORAGE BREAKDOWN", size = 10.sp, weight = FontWeight.Black, color = c.fgSubtle)
                            BodyText("$formattedStorage Used", size = 15.sp, weight = FontWeight.Bold)
                        }
                        BodyText("${formatBytes(freeStorageBytes)} free", size = 12.sp, color = c.fgMuted)
                    }

                    // Multi-segment storage bar
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(c.surfaceHigh)
                    ) {
                        val mangaWeight = storageFraction.coerceAtLeast(0.01f)
                        val remainingWeight = (1f - mangaWeight).coerceAtLeast(0.01f)
                        Box(
                            Modifier
                                .weight(mangaWeight)
                                .fillMaxSize()
                                .background(c.accent)
                        )
                        Box(
                            Modifier
                                .weight(remainingWeight)
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.12f))
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(c.accent))
                            BodyText("Manga & Novels", size = 11.sp, color = c.fgSubtle)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)))
                            BodyText("Free Space", size = 11.sp, color = c.fgSubtle)
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BodyText(
                    if (items.isNotEmpty()) "DOWNLOADING (${items.size})" else "DOWNLOADING QUEUE",
                    size = 11.sp,
                    weight = FontWeight.Black,
                    color = c.fgSubtle
                )
                BodyText("Wi-Fi only", size = 11.sp, color = c.fgFaint)
            }
        }

        if (items.isEmpty()) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            MdIcons.Download,
                            contentDescription = null,
                            tint = c.fgMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    BodyText("No active downloads", size = 16.sp, weight = FontWeight.Bold)
                    BodyText("Downloaded chapters will appear here and can be exported as CBZ or EPUB.", size = 13.sp, color = c.fgMuted, maxLines = 2)
                }
            }
        } else {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.id }) { d ->
                    DownloadCard(d, onAction = { onItemAction(d) })
                }
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
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CoverArt(d.manga.cover, Modifier.size(36.dp, 54.dp), RoundedCornerShape(6.dp), imageUrl = d.manga.coverUrl)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BodyText("${d.manga.title} · ${d.chapter}", weight = FontWeight.Bold, maxLines = 1)
            BodyText(d.status, size = 12.sp, weight = FontWeight.SemiBold, color = stateColor)
            ProgressBar(d.progress, color = barColor, height = 5.dp)
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

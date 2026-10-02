package com.mangadl.android.ui.screens.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.model.DownloadEntry
import com.mangadl.android.ui.theme.AntonStyleSub
import com.mangadl.android.ui.theme.MangaDlColors
import kotlinx.coroutines.flow.catch

@Composable
fun DownloadsScreen(onBack: () -> Unit) {
    val db = MangaDlApp.instance.database
    val downloads by db.downloadDao().getAll()
        .catch { emit(emptyList()) }
        .collectAsState(initial = emptyList())

    val active = downloads.filter { it.status == "queued" || it.status == "downloading" }

    Column(
        Modifier
            .fillMaxSize()
            .background(MangaDlColors.Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MangaDlColors.TextPrimary)
            }
            Text(
                text = "Downloads".uppercase(),
                style = AntonStyleSub,
                color = MangaDlColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MangaDlColors.Primary)
                    .clickable {}
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Pause All", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        Text(
            text = "${active.size} in queue",
            color = MangaDlColors.TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 20.dp, bottom = 12.dp),
        )

        if (downloads.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("No downloads yet", color = MangaDlColors.TextSecondary, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(downloads, key = { it.id }) { entry ->
                    DownloadCard(entry)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = Color(0x14FFFFFF),
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp),
                )
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Device storage used by manga-dl", color = MangaDlColors.TextSecondary, fontSize = 13.sp)
                Text("—", color = MangaDlColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0x1FFFFFFF))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(0.4f)
                        .fillMaxHeight()
                        .background(MangaDlColors.TextPrimary)
                )
            }
        }
    }
}

@Composable
private fun DownloadCard(entry: DownloadEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x0DFFFFFF))
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 54.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MangaDlColors.CoverPlaceholder),
        )

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "${entry.mangaTitle} · ${entry.chapterTitle}",
                color = MangaDlColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                text = when (entry.status) {
                    "downloading" -> "Downloading · ${entry.progress} / ${entry.totalPages}"
                    "queued" -> "Queued"
                    "completed" -> "Completed"
                    "error" -> "Error"
                    else -> entry.status
                },
                color = when (entry.status) {
                    "downloading" -> MangaDlColors.SectionRed
                    "completed" -> Color(0xFF22C55E)
                    "error" -> MangaDlColors.Primary
                    else -> MangaDlColors.TextSecondary
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x1FFFFFFF))
            ) {
                val progress = if (entry.totalPages > 0) entry.progress.toFloat() / entry.totalPages else 0f
                Box(
                    Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(MangaDlColors.Primary)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0x12FFFFFF))
                .clickable {},
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (entry.status == "downloading") Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Action",
                tint = MangaDlColors.TextPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

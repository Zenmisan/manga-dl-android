package com.mangadl.android.ui.screens.updates

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.ui.components.SectionLabel
import com.mangadl.android.ui.theme.AntonStyle
import com.mangadl.android.ui.theme.MangaDlColors

@Composable
fun UpdatesScreen(
    onChapterClick: (provider: String, mangaId: String, chapterId: String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MangaDlColors.Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Updates".uppercase(),
                style = AntonStyle,
                color = MangaDlColors.TextPrimary,
            )
            IconButton(onClick = {}) {
                Icon(Icons.Default.Refresh, contentDescription = "Check for updates", tint = MangaDlColors.TextPrimary)
            }
        }

        Text(
            text = "Pull to refresh for latest chapters",
            color = MangaDlColors.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
        )

        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Box(Modifier.fillMaxSize().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No updates yet",
                            color = MangaDlColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Add manga to your library to track updates",
                            color = MangaDlColors.TextSecondary,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateGroup(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        SectionLabel(
            text = label,
            color = MangaDlColors.SectionDim,
            modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 6.dp),
        )
        content()
    }
}

@Composable
private fun UpdateRow(
    coverBg: androidx.compose.ui.graphics.Color,
    title: String,
    chapter: String,
    onDownload: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {}
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 64.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(coverBg)
        )
        Column(Modifier.weight(1f)) {
            Text(title, color = MangaDlColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(chapter, color = MangaDlColors.TextSecondary, fontSize = 13.sp)
        }
        IconButton(onClick = onDownload) {
            Icon(
                Icons.Default.Download,
                contentDescription = "Download",
                tint = MangaDlColors.TextSubtle,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

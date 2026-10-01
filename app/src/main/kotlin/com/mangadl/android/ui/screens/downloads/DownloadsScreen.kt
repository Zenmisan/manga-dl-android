package com.mangadl.android.ui.screens.downloads

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.model.DownloadEntry
import kotlinx.coroutines.flow.catch

@Composable
fun DownloadsScreen() {
    val db = MangaDlApp.instance.database
    val downloads by db.downloadDao().getAll()
        .catch { emit(emptyList()) }
        .collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize()) {
        Text(
            text = "Downloads",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp),
        )

        if (downloads.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No downloads yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn {
                items(downloads, key = { it.id }) { entry ->
                    DownloadRow(entry)
                }
            }
        }
    }
}

@Composable
private fun DownloadRow(entry: DownloadEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.mangaTitle, style = MaterialTheme.typography.titleMedium)
                Text(entry.chapterTitle, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusChip(entry.status)
        }
        if (entry.status == "downloading" && entry.totalPages > 0) {
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { entry.progress.toFloat() / entry.totalPages },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "${entry.progress}/${entry.totalPages} pages",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
}

@Composable
private fun StatusChip(status: String) {
    val color = when (status) {
        "completed" -> MaterialTheme.colorScheme.primary
        "downloading" -> MaterialTheme.colorScheme.tertiary
        "error" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = status.replaceFirstChar { it.uppercaseChar() },
        style = MaterialTheme.typography.labelMedium,
        color = color,
    )
}

package com.mangadl.android.ui.screens.sources

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.extensions.ExtensionMeta

@Composable
fun SourcesScreen() {
    val sources = remember { MangaDlApp.instance.extensionManager.listExtensions() }

    Column(Modifier.fillMaxSize()) {
        Text(
            text = "Sources",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp),
        )
        Text(
            text = "${sources.size} sources loaded",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp),
        )
        LazyColumn {
            items(sources, key = { it.id }) { src ->
                SourceRow(src)
            }
        }
    }
}

@Composable
private fun SourceRow(src: ExtensionMeta) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (src.iconUrl.isNotBlank()) {
            AsyncImage(
                model = src.iconUrl,
                contentDescription = src.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(40.dp).clip(CircleShape),
            )
        } else {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = src.name.first().uppercaseChar().toString(),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(src.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${src.lang.uppercase()} · v${src.version}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (src.nsfw) {
            SuggestionChip(onClick = {}, label = { Text("18+") })
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
}

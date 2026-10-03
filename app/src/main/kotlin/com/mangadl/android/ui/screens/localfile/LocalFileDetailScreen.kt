package com.mangadl.android.ui.screens.localfile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.compose.ui.tooling.preview.Preview
import com.mangadl.android.ui.theme.AntonStyle
import com.mangadl.android.ui.theme.MangaDlColors
import com.mangadl.android.ui.theme.MangaDlTheme
import java.io.File

@Composable
fun LocalFileDetailScreen(
    fileUri: String,
    onBack: () -> Unit,
    onReadChapter: (chapterId: String) -> Unit,
) {
    val context = LocalContext.current

    val fileName = remember(fileUri) {
        try {
            fileUri.toUri().lastPathSegment ?: fileUri.substringAfterLast("/").substringAfterLast("%2F")
        } catch (e: Exception) {
            fileUri.substringAfterLast("/")
        }
    }
    val fileExtension = remember(fileName) { fileName.substringAfterLast(".", "").uppercase() }
    val isSupported = remember(fileExtension) { fileExtension in setOf("CBZ", "CBR", "PDF") }

    val fileSize = remember(fileUri) {
        try {
            val uri = fileUri.toUri()
            if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                formatSize(file.length())
            } else {
                val cursor = context.contentResolver.query(uri, arrayOf("_size"), null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        formatSize(it.getLong(0))
                    } else "Unknown"
                } ?: "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MangaDlColors.Background),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 12.dp, top = 20.dp, bottom = 8.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MangaDlColors.TextPrimary,
                    )
                }
                Text(
                    "LOCAL FILE",
                    style = AntonStyle,
                    color = MangaDlColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            // Cover placeholder
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 100.dp, height = 140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MangaDlColors.CardBg)
                        .border(1.dp, MangaDlColors.CardBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            when (fileExtension) {
                                "PDF" -> Icons.Default.PictureAsPdf
                                else -> Icons.Default.Archive
                            },
                            contentDescription = null,
                            tint = MangaDlColors.TextSecondary,
                            modifier = Modifier.size(32.dp),
                        )
                        Text(
                            fileExtension.ifEmpty { "FILE" },
                            color = MangaDlColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        fileName.substringBeforeLast("."),
                        color = MangaDlColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    InfoRow(label = "Format", value = fileExtension.ifEmpty { "Unknown" })
                    InfoRow(label = "Size", value = fileSize)
                    InfoRow(label = "Pages", value = "Calculating...")
                }
            }
        }

        item {
            // File path
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MangaDlColors.CardBg)
                    .border(1.dp, MangaDlColors.CardBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("File Path", color = MangaDlColors.TextSecondary, fontSize = 11.sp)
                Text(
                    fileUri,
                    color = MangaDlColors.TextPrimary,
                    fontSize = 12.sp,
                    maxLines = 3,
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }

        if (!isSupported) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1AEF4444))
                        .border(1.dp, Color(0x33EF4444), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MangaDlColors.PrimaryLight,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        "Local file reading coming soon for ${fileExtension.ifEmpty { "this format" }}. Supported: CBZ, CBR, PDF.",
                        color = MangaDlColors.TextPrimary,
                        fontSize = 13.sp,
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Read button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSupported) MangaDlColors.Primary else Color(0x66DC2626))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Text(
                            "Read",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // Delete button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MangaDlColors.Primary, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MangaDlColors.Primary, modifier = Modifier.size(20.dp))
                        Text(
                            "Delete File",
                            color = MangaDlColors.Primary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = MangaDlColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.width(56.dp))
        Text(value, color = MangaDlColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes.toFloat() / (1024 * 1024))
        else -> "%.2f GB".format(bytes.toFloat() / (1024 * 1024 * 1024))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
private fun LocalFileDetailScreenPreview() {
    MangaDlTheme {
        Column(
            Modifier
                .fillMaxSize()
                .background(MangaDlColors.Background)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 4.dp, top = 8.dp, end = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = MangaDlColors.TextPrimary,
                    modifier = Modifier.padding(8.dp).size(24.dp),
                )
                Text("local_manga.cbz", style = AntonStyle, color = MangaDlColors.TextPrimary, modifier = Modifier.weight(1f))
            }
        }
    }
}

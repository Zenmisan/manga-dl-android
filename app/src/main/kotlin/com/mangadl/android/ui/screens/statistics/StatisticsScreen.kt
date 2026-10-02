package com.mangadl.android.ui.screens.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.mangadl.android.data.model.LibraryManga
import com.mangadl.android.data.model.ReadingProgress
import com.mangadl.android.ui.theme.AntonStyle
import com.mangadl.android.ui.theme.MangaDlColors
import kotlinx.coroutines.flow.catch
import java.util.*

@Composable
fun StatisticsScreen(onBack: () -> Unit) {
    val db = MangaDlApp.instance.database

    var library by remember { mutableStateOf<List<LibraryManga>>(emptyList()) }
    var allProgress by remember { mutableStateOf<List<ReadingProgress>>(emptyList()) }

    LaunchedEffect(Unit) {
        db.libraryDao().getAll()
            .catch { /* ignore */ }
            .collect { library = it }
    }
    LaunchedEffect(Unit) {
        db.progressDao().getRecent(1000)
            .catch { /* ignore */ }
            .collect { allProgress = it }
    }

    val chaptersRead = allProgress.count { it.completed }
    val pagesRead = allProgress.sumOf { it.page }
    val sourcesUsed = library.map { it.provider }.distinct().size
    val topManga = library.sortedByDescending { it.readCount }.take(5)

    // Build last-7-days activity
    val calendar = Calendar.getInstance()
    val dayLabels = List(7) { i ->
        calendar.apply { timeInMillis = System.currentTimeMillis(); add(Calendar.DAY_OF_YEAR, -(6 - i)) }
        val dayAbbrevs = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        dayAbbrevs[calendar.get(Calendar.DAY_OF_WEEK) - 1]
    }
    val dayCounts = List(7) { i ->
        val dayStart = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.DAY_OF_YEAR, -(6 - i))
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dayEnd = dayStart + 86_400_000L
        allProgress.count { it.readAt in dayStart until dayEnd }
    }
    val maxCount = dayCounts.maxOrNull()?.coerceAtLeast(1) ?: 1

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
                    "STATISTICS",
                    style = AntonStyle,
                    color = MangaDlColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // 2x2 stat tiles
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("Manga in Library", library.size.toString(), Modifier.weight(1f))
                    StatTile("Chapters Read", chaptersRead.toString(), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("Pages Read", pagesRead.toString(), Modifier.weight(1f))
                    StatTile("Sources Used", sourcesUsed.toString(), Modifier.weight(1f))
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }

        // Reading activity
        item {
            SectionHeader("READING ACTIVITY")
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                dayLabels.forEachIndexed { i, label ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val fraction = dayCounts[i].toFloat() / maxCount.toFloat()
                        val barHeight = (fraction * 48f).coerceAtLeast(4f).dp
                        val barColor = if (dayCounts[i] > 0) MangaDlColors.Primary else Color(0x33FFFFFF)
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(4.dp))
                                .background(barColor),
                        )
                        Text(
                            label,
                            color = MangaDlColors.TextSecondary,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }

        // Top manga
        item { SectionHeader("TOP MANGA") }

        if (topManga.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No manga in library", color = MangaDlColors.TextSecondary, fontSize = 14.sp)
                }
            }
        } else {
            items(topManga.size) { i ->
                val manga = topManga[i]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "${i + 1}",
                        color = MangaDlColors.Primary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.width(24.dp),
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 48.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MangaDlColors.CardBg),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(manga.title, color = MangaDlColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text("${manga.readCount} chapters read", color = MangaDlColors.TextSecondary, fontSize = 12.sp)
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(MangaDlColors.CardBorder))
            }
        }

        item { Spacer(Modifier.height(16.dp)) }

        // Library breakdown
        item { SectionHeader("LIBRARY BREAKDOWN") }

        item {
            val statuses = listOf("All" to library.size, "Reading" to 0, "Plan to read" to 0, "Done" to 0)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                statuses.forEach { (label, count) ->
                    StatusPill(label = label, count = count)
                }
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MangaDlColors.CardBg)
            .border(1.dp, MangaDlColors.CardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(value, color = MangaDlColors.TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = MangaDlColors.TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        color = MangaDlColors.SectionRed,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
    )
}

@Composable
private fun StatusPill(label: String, count: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MangaDlColors.CardBg)
            .border(1.dp, MangaDlColors.CardBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MangaDlColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(count.toString(), color = MangaDlColors.TextSecondary, fontSize = 12.sp)
    }
}

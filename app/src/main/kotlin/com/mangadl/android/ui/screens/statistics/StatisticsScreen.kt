package com.mangadl.android.ui.screens.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mangadl.android.ui.viewmodels.StatisticsViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.ui.components.BackHeader
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.DisplayText
import com.mangadl.android.ui.components.Eyebrow
import com.mangadl.android.ui.components.ProgressBar
import com.mangadl.android.ui.components.Screen
import com.mangadl.android.ui.components.SurfaceCard
import com.mangadl.android.ui.theme.MdTheme

@Composable
fun StatsScreen(onBack: () -> Unit) {
    val c = MdTheme.colors
    val vm: StatisticsViewModel = viewModel()
    val library by vm.library.collectAsState()
    val chapters by vm.totalChaptersRead.collectAsState()
    val readingTime by vm.readingTimeHours.collectAsState()
    val streak by vm.streak.collectAsState()
    val allProgress by vm.allProgress.collectAsState()
    val activityHeatmap by vm.activityHeatmap.collectAsState()

    Screen {
        BackHeader("Statistics", onBack, Modifier.padding(bottom = 0.dp))
        BodyText("Your reading habits, at a glance", Modifier.padding(start = 60.dp, bottom = 14.dp), size = 13.sp, color = c.fgSubtle)
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            val tiles = listOf(
                "Chapters read" to chapters.toString(),
                "Time reading" to readingTime,
                "Current streak" to "${streak}d",
                "In library" to library.size.toString()
            )
            tiles.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 0.dp)) {
                    row.forEach { (label, value) ->
                        SurfaceCard(Modifier.weight(1f), radius = 14.dp, background = c.surface, borderColor = c.surfaceHigh) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                BodyText(label, size = 12.sp, weight = FontWeight.SemiBold, color = c.fg.copy(alpha = 0.65f))
                                DisplayText(value, 30.sp, uppercase = false)
                            }
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Eyebrow("Activity · last 18 weeks", color = c.fgSubtle)
                Heatmap(activity = activityHeatmap)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                    BodyText("Less", size = 11.sp, color = c.fgSubtle)
                    heatShades().forEach { Box(Modifier.width(10.dp).aspectRatio(1f).clip(RoundedCornerShape(2.dp)).background(it)) }
                    BodyText("More", size = 11.sp, color = c.fgSubtle)
                }
            }
            val bySource = allProgress.groupBy { it.provider }
                .mapValues { (_, v) -> v.size }
                .entries.sortedByDescending { it.value }
            if (bySource.isNotEmpty()) {
                val maxCount = bySource.first().value.toFloat().coerceAtLeast(1f)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Eyebrow("By source", color = c.fgSubtle)
                    bySource.take(5).forEach { (provider, count) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            BodyText(provider.replaceFirstChar { it.uppercaseChar() }, Modifier.width(96.dp), size = 13.sp, weight = FontWeight.SemiBold)
                            ProgressBar(count / maxCount, Modifier.weight(1f), color = c.fg, track = c.surfaceHigh, height = 8.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun heatShades(): List<Color> {
    val c = MdTheme.colors
    return listOf(c.surfaceHigh.copy(alpha = 0.07f), c.accent.copy(alpha = 0.35f), c.accent.copy(alpha = 0.65f), c.accent)
}

@Composable
fun Heatmap(activity: List<List<Int>>, modifier: Modifier = Modifier, gap: Int = 3) {
    val shades = heatShades()
    val weeks = activity.size.coerceAtLeast(1)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap.dp)) {
        repeat(weeks) { w ->
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(gap.dp)) {
                repeat(7) { d ->
                    val count = activity.getOrNull(w)?.getOrNull(d) ?: 0
                    val shade = when {
                        count == 0 -> shades[0]
                        count in 1..2 -> shades[1]
                        count in 3..5 -> shades[2]
                        else -> shades[3]
                    }
                    Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(3.dp)).background(shade))
                }
            }
        }
    }
}

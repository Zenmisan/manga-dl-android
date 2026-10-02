package com.mangadl.android.ui.screens.tracking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.data.prefs.PrefKeys
import com.mangadl.android.data.prefs.dataStore
import com.mangadl.android.ui.theme.MangaDlColors
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val trackingStatuses = listOf("Reading", "Completed", "Plan to Read", "Dropped", "On Hold")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingSheet(
    mangaTitle: String,
    mangaId: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var anilistConnected by remember { mutableStateOf(false) }
    var malConnected by remember { mutableStateOf(false) }

    // AniList local tracking state
    var anilistStatus by remember { mutableStateOf("Reading") }
    var anilistScore by remember { mutableStateOf(0f) }
    var anilistProgress by remember { mutableStateOf(0) }
    var anilistStatusExpanded by remember { mutableStateOf(false) }

    // MAL local tracking state
    var malStatus by remember { mutableStateOf("Reading") }
    var malScore by remember { mutableStateOf(0f) }
    var malProgress by remember { mutableStateOf(0) }
    var malStatusExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        context.dataStore.data.map { it[PrefKeys.ANILIST_CONNECTED] ?: false }.collect { anilistConnected = it }
    }
    LaunchedEffect(Unit) {
        context.dataStore.data.map { it[PrefKeys.MAL_CONNECTED] ?: false }.collect { malConnected = it }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F0F0F),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x33FFFFFF)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "TRACKING",
                    color = MangaDlColors.SectionRed,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                )
                Text(
                    mangaTitle,
                    color = MangaDlColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                )
            }

            // AniList card
            TrackerCard(
                trackerName = "AniList",
                trackerInitial = "A",
                trackerColor = Color(0xFF02A9FF),
                connected = anilistConnected,
                status = anilistStatus,
                score = anilistScore,
                progress = anilistProgress,
                statusExpanded = anilistStatusExpanded,
                onToggleStatusDropdown = { anilistStatusExpanded = !anilistStatusExpanded },
                onStatusSelected = { anilistStatus = it; anilistStatusExpanded = false },
                onScoreChange = { anilistScore = it },
                onProgressChange = { anilistProgress = it },
                onConnect = {
                    scope.launch {
                        context.dataStore.updateData { prefs ->
                            prefs.toMutablePreferences().also { it[PrefKeys.ANILIST_CONNECTED] = true }
                        }
                    }
                },
            )

            // MAL card
            TrackerCard(
                trackerName = "MyAnimeList",
                trackerInitial = "M",
                trackerColor = Color(0xFF2E51A2),
                connected = malConnected,
                status = malStatus,
                score = malScore,
                progress = malProgress,
                statusExpanded = malStatusExpanded,
                onToggleStatusDropdown = { malStatusExpanded = !malStatusExpanded },
                onStatusSelected = { malStatus = it; malStatusExpanded = false },
                onScoreChange = { malScore = it },
                onProgressChange = { malProgress = it },
                onConnect = {
                    scope.launch {
                        context.dataStore.updateData { prefs ->
                            prefs.toMutablePreferences().also { it[PrefKeys.MAL_CONNECTED] = true }
                        }
                    }
                },
            )

            // Save button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MangaDlColors.Primary)
                    .clickable(onClick = onDismiss)
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Save", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TrackerCard(
    trackerName: String,
    trackerInitial: String,
    trackerColor: Color,
    connected: Boolean,
    status: String,
    score: Float,
    progress: Int,
    statusExpanded: Boolean,
    onToggleStatusDropdown: () -> Unit,
    onStatusSelected: (String) -> Unit,
    onScoreChange: (Float) -> Unit,
    onProgressChange: (Int) -> Unit,
    onConnect: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MangaDlColors.CardBg)
            .border(1.dp, MangaDlColors.CardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(trackerColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(trackerInitial, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text(
                trackerName,
                color = MangaDlColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (connected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
            }
        }

        if (!connected) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, trackerColor, RoundedCornerShape(10.dp))
                    .clickable(onClick = onConnect)
                    .padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Connect $trackerName", color = trackerColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            // Status dropdown
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Status", color = MangaDlColors.TextSecondary, fontSize = 11.sp)
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x14FFFFFF))
                            .border(1.dp, MangaDlColors.CardBorder, RoundedCornerShape(8.dp))
                            .clickable(onClick = onToggleStatusDropdown)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(status, color = MangaDlColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Icon(
                            if (statusExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MangaDlColors.TextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { onToggleStatusDropdown() },
                        modifier = Modifier.background(Color(0xFF1A1A1A)),
                    ) {
                        trackingStatuses.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s, color = MangaDlColors.TextPrimary, fontSize = 14.sp) },
                                onClick = { onStatusSelected(s) },
                            )
                        }
                    }
                }
            }

            // Score
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Score", color = MangaDlColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("${score.toInt()} / 10", color = MangaDlColors.TextPrimary, fontSize = 12.sp)
                }
                Slider(
                    value = score,
                    onValueChange = onScoreChange,
                    valueRange = 0f..10f,
                    steps = 9,
                    colors = SliderDefaults.colors(
                        thumbColor = MangaDlColors.Primary,
                        activeTrackColor = MangaDlColors.Primary,
                        inactiveTrackColor = Color(0x33FFFFFF),
                    ),
                    modifier = Modifier.height(32.dp),
                )
            }

            // Progress
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Progress (chapters)", color = MangaDlColors.TextSecondary, fontSize = 11.sp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(
                        onClick = { if (progress > 0) onProgressChange(progress - 1) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MangaDlColors.CardBg),
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = MangaDlColors.TextPrimary, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        progress.toString(),
                        color = MangaDlColors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { onProgressChange(progress + 1) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MangaDlColors.CardBg),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = MangaDlColors.TextPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

package com.mangadl.android.ui.screens.tracking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.ButtonTone
import com.mangadl.android.ui.components.DisplayText
import com.mangadl.android.ui.components.LogoTile
import com.mangadl.android.ui.components.MdButton
import com.mangadl.android.ui.components.MdIconButton
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.components.PillButton
import com.mangadl.android.ui.components.rememberState
import com.mangadl.android.ui.theme.MdTheme
import com.mangadl.android.ui.viewmodels.TrackerViewModel

private data class TrackerDef(val name: String, val short: String, val color: Color)

private val TRACKER_DEFS = listOf(
    TrackerDef("AniList", "AL", Color(0xFF0099CC)),
    TrackerDef("MyAnimeList", "MAL", Color(0xFF2E51A2)),
)

@Composable
fun TrackingSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    mangaId: String,
    mangaTitle: String,
) {
    val c = MdTheme.colors
    val vm: TrackerViewModel = viewModel()
    val anilistConnected by vm.anilistConnected.collectAsState()
    val malConnected by vm.malConnected.collectAsState()

    // track linking state in-memory so UI reflects immediately after call
    val linked = remember { mutableStateMapOf<String, Boolean>() }
    // initialize from prefs when sheet opens
    if (visible) {
        linked.getOrPut("AL") { vm.isAnilistLinked(mangaId) }
        linked.getOrPut("MAL") { vm.isMalLinked(mangaId) }
    }

    val working = remember { mutableStateMapOf<String, Boolean>() }

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            )
        }
        AnimatedVisibility(
            visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(c.sheet)
                    .border(1.dp, c.track, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).size(40.dp, 4.dp).clip(CircleShape).background(c.fg.copy(alpha = 0.25f)))
                DisplayText("Tracking", 26.sp)

                TRACKER_DEFS.forEach { tracker ->
                    val globalConnected = when (tracker.short) {
                        "AL" -> anilistConnected
                        "MAL" -> malConnected
                        else -> false
                    }
                    val isLinked = linked[tracker.short] == true
                    val isWorking = working[tracker.short] == true

                    if (isLinked) {
                        TrackedCard(
                            tracker = tracker,
                            onRemove = {
                                when (tracker.short) {
                                    "AL" -> vm.unlinkAnilist(mangaId)
                                    "MAL" -> vm.unlinkMal(mangaId)
                                }
                                linked[tracker.short] = false
                            },
                        )
                    } else {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            LogoTile(tracker.short, tracker.color, size = 36.dp, anton = false)
                            BodyText(tracker.name, Modifier.weight(1f), size = 15.sp, weight = FontWeight.SemiBold)
                            if (!globalConnected) {
                                BodyText("Not connected", size = 12.sp, color = c.fgSubtle)
                            } else {
                                PillButton(
                                    if (isWorking) "Linking…" else "Add Tracking",
                                    onClick = {
                                        if (isWorking) return@PillButton
                                        working[tracker.short] = true
                                        when (tracker.short) {
                                            "AL" -> vm.linkAnilist(mangaId, mangaTitle) { ok ->
                                                working["AL"] = false
                                                if (ok) linked["AL"] = true
                                            }
                                            "MAL" -> vm.linkMal(mangaId, mangaTitle) { ok ->
                                                working["MAL"] = false
                                                if (ok) linked["MAL"] = true
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackedCard(tracker: TrackerDef, onRemove: () -> Unit) {
    val c = MdTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .border(1.dp, c.dividerStrong, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LogoTile(tracker.short, tracker.color, size = 36.dp, anton = false)
            BodyText(tracker.name, Modifier.weight(1f), size = 15.sp, weight = FontWeight.Bold)
            MdIconButton(MdIcons.Close, "Remove tracking", onRemove, tint = c.fg.copy(alpha = 0.7f), iconSize = 18.dp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TrackField("Status", "Reading", Modifier.weight(1f))
            TrackField("Chapters", "— / —", Modifier.weight(1f))
            TrackField("Score", "—", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MdButton("Started —", {}, Modifier.weight(1f), tone = ButtonTone.Ghost, height = 44.dp, shape = RoundedCornerShape(12.dp), fontSize = 12.sp, horizontalPadding = 8.dp)
            MdButton("Finished —", {}, Modifier.weight(1f), tone = ButtonTone.Ghost, height = 44.dp, shape = RoundedCornerShape(12.dp), fontSize = 12.sp, horizontalPadding = 8.dp)
        }
    }
}

@Composable
private fun TrackField(label: String, value: String, modifier: Modifier) {
    val c = MdTheme.colors
    Column(
        modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, c.track, RoundedCornerShape(12.dp))
            .clickable(role = Role.Button) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        BodyText(label, size = 11.sp, color = c.fgSubtle)
        BodyText(value, size = 13.sp, weight = FontWeight.Bold)
    }
}

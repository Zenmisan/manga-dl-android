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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.data.ui.UiTracker
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.ButtonTone
import com.mangadl.android.ui.components.DisplayText
import com.mangadl.android.ui.components.LogoTile
import com.mangadl.android.ui.components.MdButton
import com.mangadl.android.ui.components.MdIconButton
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.components.PillButton
import com.mangadl.android.ui.theme.MdTheme

@Composable
fun TrackingSheet(visible: Boolean, onDismiss: () -> Unit, trackers: List<UiTracker>) {
    val c = MdTheme.colors
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
                val (linked, others) = trackers.partition { it.connected }
                linked.take(1).forEach { TrackedCard(it) }
                (linked.drop(1) + others).forEach { t ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LogoTile(t.short, t.color, size = 36.dp, anton = false)
                        BodyText(t.name, Modifier.weight(1f), size = 15.sp, weight = FontWeight.SemiBold)
                        PillButton("Add Tracking", {})
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackedCard(t: UiTracker) {
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
            LogoTile(t.short, t.color, size = 36.dp, anton = false)
            BodyText(t.name, Modifier.weight(1f), size = 15.sp, weight = FontWeight.Bold)
            MdIconButton(MdIcons.Close, "Remove tracking", {}, tint = c.fg.copy(alpha = 0.7f), iconSize = 18.dp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TrackField("Status", "Reading", Modifier.weight(1f))
            TrackField("Chapters", "48 / —", Modifier.weight(1f))
            TrackField("Score", "—", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MdButton("Started [date]", {}, Modifier.weight(1f), tone = ButtonTone.Ghost, height = 44.dp, shape = RoundedCornerShape(12.dp), fontSize = 12.sp, horizontalPadding = 8.dp)
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

package com.mangadl.android.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.ui.theme.MangaDlColors
import com.mangadl.android.ui.theme.SectionLabelStyle

@Composable
fun MangaDlSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) MangaDlColors.Primary else MangaDlColors.SwitchTrackOff,
        label = "switchTrack",
    )

    Box(
        modifier = modifier
            .size(width = 48.dp, height = 28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(trackColor)
            .clickable { onCheckedChange(!checked) },
    ) {
        Box(
            modifier = Modifier
                .padding(3.dp)
                .size(22.dp)
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .clip(RoundedCornerShape(11.dp))
                .background(Color.White),
        )
    }
}

@Composable
fun PillButton(
    text: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = modifier
            .height(36.dp)
            .then(
                if (active) Modifier.clip(shape).background(MangaDlColors.TextPrimary)
                else Modifier.clip(shape).background(Color.Transparent)
                    .border(1.dp, Color(0x24FFFFFF), shape)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (active) MangaDlColors.Background else MangaDlColors.TextSubtle,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
        )
    }
}

@Composable
fun SectionLabel(
    text: String,
    color: Color = MangaDlColors.SectionRed,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        style = SectionLabelStyle,
        color = color,
        modifier = modifier,
    )
}

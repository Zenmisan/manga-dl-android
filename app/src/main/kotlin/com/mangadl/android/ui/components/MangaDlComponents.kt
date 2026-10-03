package com.mangadl.android.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.ui.theme.MangaDlColors
import com.mangadl.android.ui.theme.SectionLabelStyle

@Composable
fun rememberHapticClick(): () -> Unit {
    val view = LocalView.current
    return remember(view) { { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) } }
}

@Composable
fun rememberHapticConfirm(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
        }
    }
}

@Composable
fun rememberHapticLongPress(): () -> Unit {
    val view = LocalView.current
    return remember(view) { { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) } }
}

@Composable
fun MangaDlSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = rememberHapticClick()
    val trackColor by animateColorAsState(
        targetValue = if (checked) MangaDlColors.Primary else MangaDlColors.SwitchTrackOff,
        label = "switchTrack",
    )

    Box(
        modifier = modifier
            .size(width = 48.dp, height = 28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(trackColor)
            .clickable { haptic(); onCheckedChange(!checked) },
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
    val haptic = rememberHapticClick()
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = modifier
            .height(36.dp)
            .then(
                if (active) Modifier.clip(shape).background(MangaDlColors.TextPrimary)
                else Modifier.clip(shape).background(Color.Transparent)
                    .border(1.dp, Color(0x24FFFFFF), shape)
            )
            .clickable { haptic(); onClick() }
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

// ── Shimmer / skeleton ────────────────────────────────────────────────────────

@Composable
fun SkeletonBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )
    Box(modifier.background(Color(0x1AFFFFFF).copy(alpha = alpha)))
}

@Composable
fun CoverSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(8.dp))
            .background(MangaDlColors.CoverPlaceholder.copy(alpha = alpha * 0.6f + 0.4f))
    )
}

@Composable
fun CoverSkeletonGrid(
    modifier: Modifier = Modifier,
    count: Int = 9,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        userScrollEnabled = false,
    ) {
        items(count) {
            CoverSkeleton()
        }
    }
}

// ── Empty / error states ──────────────────────────────────────────────────────

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier,
) {
    Box(modifier.padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0x14FFFFFF)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MangaDlColors.TextSecondary,
                    modifier = Modifier.size(32.dp),
                )
            }
            Text(
                title,
                color = MangaDlColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    color = MangaDlColors.TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(modifier.padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0x1FDC2626)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = MangaDlColors.Primary,
                    modifier = Modifier.size(32.dp),
                )
            }
            Text(
                "Failed to load",
                color = MangaDlColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                message,
                color = MangaDlColors.TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (onBack != null) {
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .border(1.dp, Color(0x29FFFFFF), RoundedCornerShape(999.dp))
                            .clickable(onClick = onBack)
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Go back", color = MangaDlColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (onRetry != null) {
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(MangaDlColors.Primary)
                            .clickable(onClick = onRetry)
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Try again", color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String = "Delete",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val hapticConfirm = rememberHapticConfirm()
    val hapticClick = rememberHapticClick()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = {
            Text(title, color = MangaDlColors.TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(body, color = MangaDlColors.TextSecondary)
        },
        confirmButton = {
            TextButton(onClick = { hapticConfirm(); onConfirm() }) {
                Text(confirmLabel, color = MangaDlColors.Primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = { hapticClick(); onDismiss() }) {
                Text("Cancel", color = MangaDlColors.TextSecondary)
            }
        },
    )
}

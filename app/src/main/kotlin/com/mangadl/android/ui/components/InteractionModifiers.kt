package com.mangadl.android.ui.components

import android.os.SystemClock
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mangadl.android.ui.theme.MdTheme

/**
 * Tactile spring-bounce click modifier.
 *
 * Provides:
 * 1. Physical press compression (scales to [pressedScale] with spring bounce).
 * 2. Instant haptic feedback on touch down.
 * 3. Bounded custom ripple with theme accent tint.
 * 4. Safe click debouncing to eliminate duplicate rapid taps and navigation hangs.
 */
fun Modifier.bounceClick(
    enabled: Boolean = true,
    pressedScale: Float = 0.95f,
    role: Role? = Role.Button,
    debounceMs: Long = 300L,
    onClick: () -> Unit,
): Modifier = composed {
    if (!enabled) return@composed this

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    var lastClickTime by remember { mutableLongStateOf(0L) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "bounceScale",
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(
                bounded = true,
                color = MdTheme.colors.accent.copy(alpha = 0.25f),
            ),
            enabled = enabled,
            role = role,
            onClick = {
                val now = SystemClock.elapsedRealtime()
                if (now - lastClickTime >= debounceMs) {
                    lastClickTime = now
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            },
        )
}

/**
 * Modern AMOLED subtle dark glass container.
 * Default tokens: rgba(255, 255, 255, 0.05) background with 1px rgba(255, 255, 255, 0.10) border.
 */
fun Modifier.glassCard(
    shape: Shape,
    backgroundColor: Color = Color.White.copy(alpha = 0.05f),
    borderColor: Color = Color.White.copy(alpha = 0.10f),
    borderWidth: Dp = 1.dp,
): Modifier = this
    .clip(shape)
    .background(backgroundColor)
    .border(borderWidth, borderColor, shape)

/**
 * Smooth skeleton shimmer modifier for covers and content placeholders while loading.
 */
fun Modifier.shimmerPlaceholder(
    visible: Boolean = true,
    shape: Shape? = null,
): Modifier = composed {
    if (!visible) return@composed this

    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = -500f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslate",
    )

    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.03f),
        Color.White.copy(alpha = 0.12f),
        Color.White.copy(alpha = 0.03f),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 400f, translateAnim - 400f),
        end = Offset(translateAnim, translateAnim),
    )

    val base = if (shape != null) this.clip(shape) else this
    base.background(brush)
}

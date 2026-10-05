package com.mangadl.android.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

object MangaDlColors {
    val Background = Color(0xFF050505)
    val Primary = Color(0xFFDC2626)
    val PrimaryLight = Color(0xFFEF4444)
    val TextPrimary = Color(0xFFFAFAFA)
    val TextSecondary = Color(0x99FFFFFF)
    val TextSubtle = Color(0xB8FFFFFF)
    val CardBg = Color(0x0FFFFFFF)
    val CardBorder = Color(0x1AFFFFFF)
    val CardBorderSubtle = Color(0x14FFFFFF)
    val NavBg = Color(0xF5080808)
    val SectionRed = Color(0xFFEF4444)
    val SectionDim = Color(0x99FFFFFF)
    val DetailHeaderBg = Color(0xFF1A2433)
    val SwitchTrackOff = Color(0x33FFFFFF)
    val Divider = Color(0x0FFFFFFF)
    val CoverPlaceholder = Color(0xFF0F1724)
    val AvatarBg = Color(0xFF2D1716)
}

object MdTheme {
    val colors: MdColors
        @Composable @ReadOnlyComposable
        get() = LocalMdColors.current
}

@Composable
fun MangaDlTheme(accent: Accent = Accent.Red, content: @Composable () -> Unit) {
    val colors = remember(accent) { MdColors.from(accent) }
    val scheme = darkColorScheme(
        primary = colors.accent,
        onPrimary = Color.White,
        secondary = colors.accentLight,
        background = colors.bg,
        onBackground = colors.fg,
        surface = colors.bg,
        onSurface = colors.fg,
        surfaceVariant = colors.sheet,
        onSurfaceVariant = colors.fgMuted,
        outline = colors.border,
        error = colors.errorText,
    )
    val base = MdType.body(14.sp).copy(color = colors.fg)
    MaterialTheme(colorScheme = scheme, typography = Typography) {
        CompositionLocalProvider(
            LocalMdColors provides colors,
            LocalContentColor provides colors.fg,
            LocalTextStyle provides base,
            content = content,
        )
    }
}

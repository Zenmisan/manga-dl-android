package com.mangadl.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

private val DarkColorScheme = darkColorScheme(
    primary = MangaDlColors.Primary,
    onPrimary = MangaDlColors.TextPrimary,
    primaryContainer = Color(0xFF7B0000),
    onPrimaryContainer = MangaDlColors.TextPrimary,
    background = MangaDlColors.Background,
    onBackground = MangaDlColors.TextPrimary,
    surface = Color(0xFF0D0D0D),
    onSurface = MangaDlColors.TextPrimary,
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = MangaDlColors.TextSecondary,
    outline = Color(0x1AFFFFFF),
    outlineVariant = Color(0x14FFFFFF),
    error = Color(0xFFEF4444),
)

@Composable
fun MangaDlTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content,
    )
}

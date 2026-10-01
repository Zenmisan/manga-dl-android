package com.mangadl.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Background = Color(0xFF0A0A0F)
private val Surface = Color(0xFF13131A)
private val SurfaceVariant = Color(0xFF1A1A24)
private val Primary = Color(0xFFE53935)
private val PrimaryContainer = Color(0xFF7B0000)
private val OnPrimary = Color(0xFFFFFFFF)
private val OnBackground = Color(0xFFEEEEEE)
private val OnSurface = Color(0xFFDDDDDD)
private val OnSurfaceVariant = Color(0xFF999999)
private val Outline = Color(0xFF2A2A38)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimary,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = Outline,
)

@Composable
fun MangaDlTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content,
    )
}

package com.mangadl.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class Accent(val label: String, val main: Color, val light: Color, val soft: Color) {
    Red("Red", Color(0xFFDC2626), Color(0xFFEF4444), Color(0xFFFCA5A5)),
    Blue("Blue", Color(0xFF2563EB), Color(0xFF3B82F6), Color(0xFF93C5FD)),
    Purple("Purple", Color(0xFF7C3AED), Color(0xFF8B5CF6), Color(0xFFC4B5FD)),
    Green("Green", Color(0xFF16A34A), Color(0xFF22C55E), Color(0xFF86EFAC)),
    Orange("Orange", Color(0xFFEA580C), Color(0xFFF97316), Color(0xFFFDBA74)),
    Pink("Pink", Color(0xFFDB2777), Color(0xFFEC4899), Color(0xFFF9A8D4)),
}

private val White = Color.White

@Immutable
data class MdColors(
    val accent: Color,
    val accentLight: Color,
    val accentSoft: Color,
) {
    val bg = Color(0xFF050505)
    val navBg = Color(0xFF080808)
    val sheet = Color(0xFF111111)
    val panel = Color(0xFF141414)
    val readerBg = Color.Black
    val novelBg = Color(0xFF0D0D0D)
    val paper = Color(0xFFE9E6DF)

    val fg = Color(0xFFFAFAFA)
    val fgMuted = White.copy(alpha = 0.72f)
    val fgSubtle = White.copy(alpha = 0.60f)
    val fgFaint = White.copy(alpha = 0.50f)
    val placeholder = White.copy(alpha = 0.45f)

    val surface = White.copy(alpha = 0.05f)
    val surfaceRaised = White.copy(alpha = 0.06f)
    val surfaceHigh = White.copy(alpha = 0.08f)
    val track = White.copy(alpha = 0.12f)
    val border = White.copy(alpha = 0.14f)
    val borderStrong = White.copy(alpha = 0.18f)
    val divider = White.copy(alpha = 0.06f)
    val dividerStrong = White.copy(alpha = 0.10f)
    val switchOff = White.copy(alpha = 0.20f)

    val accentMuted = accent.copy(alpha = 0.16f)
    val accentNav = accent.copy(alpha = 0.18f)
    val accentFaint = accent.copy(alpha = 0.08f)

    val success = Color(0xFF16A34A)
    val successText = Color(0xFF4ADE80)
    val successSoft = Color(0xFF16A34A).copy(alpha = 0.16f)
    val errorText = Color(0xFFF87171)
    val errorBorder = Color(0xFFEF4444)
    val errorSoft = Color(0xFFDC2626).copy(alpha = 0.18f)
    val errorBar = Color(0xFF7F1D1D)
    val danger = Color(0xFFB91C1C)

    companion object {
        fun from(accent: Accent) = MdColors(accent.main, accent.light, accent.soft)
    }
}

val LocalMdColors = staticCompositionLocalOf { MdColors.from(Accent.Red) }

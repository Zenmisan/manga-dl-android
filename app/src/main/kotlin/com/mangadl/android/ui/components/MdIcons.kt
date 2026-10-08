package com.mangadl.android.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

object MdIcons {
    val Search by lazy { stroke("Search", "M11 4a7 7 0 1 0 0 14a7 7 0 1 0 0-14z", "M20 20l-3.5-3.5") }
    val Filter by lazy { stroke("Filter", "M4 6h16M7 12h10M10 18h4") }
    val More by lazy { stroke("More", "M12 5h.01M12 12h.01M12 19h.01", width = 3f) }
    val Library by lazy { stroke("Library", "M5 4h11a3 3 0 0 1 3 3v13H8a3 3 0 0 1-3-3z", "M5 17a3 3 0 0 1 3-3h11") }
    val Updates by lazy { stroke("Updates", "M6 16v-5a6 6 0 0 1 12 0v5l2 2H4z", "M10 21h4") }
    val History by lazy { stroke("History", "M12 4a8 8 0 1 0 0 16a8 8 0 1 0 0-16z", "M12 8v4l3 2") }
    val Browse by lazy { stroke("Browse", "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18z", "M15.5 8.5l-2 5-5 2 2-5z") }
    val Menu by lazy { stroke("Menu", "M4 7h16M4 12h16M4 17h16") }
    val Back by lazy { stroke("Back", "M15 5l-7 7 7 7") }
    val ChevronRight by lazy { stroke("ChevronRight", "M9 5l7 7-7 7") }
    val ChevronDown by lazy { stroke("ChevronDown", "M6 9l6 6 6-6") }
    val ChevronUp by lazy { stroke("ChevronUp", "M6 15l6-6 6 6") }
    val Download by lazy { stroke("Download", "M12 4v11M7 10l5 5 5-5M5 20h14") }
    val Upload by lazy { stroke("Upload", "M4 20h16M12 16V4M7 9l5-5 5 5") }
    val Check by lazy { stroke("Check", "M5 12l5 5 9-10", width = 2.5f) }
    val Play by lazy { fill("Play", "M8 5l11 7-11 7z") }
    val Pause by lazy { stroke("Pause", "M8 5v14M16 5v14", width = 2.2f) }
    val Bookmark by lazy { stroke("Bookmark", "M7 4h10v16l-5-4-5 4z") }
    val BookmarkFilled by lazy { fill("BookmarkFilled", "M6 3h12v18l-6-4-6 4z") }
    val Sliders by lazy {
        stroke("Sliders", "M4 7h10M18 7h2M4 17h4M12 17h8", "M16 5a2 2 0 1 0 0 4a2 2 0 1 0 0-4z", "M10 15a2 2 0 1 0 0 4a2 2 0 1 0 0-4z")
    }
    val Settings by lazy { stroke("Settings", "M4 7h10M18 7h2M4 17h4M12 17h8M16 5v4M10 15v4") }
    val Globe by lazy { stroke("Globe", "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18z", "M3 12h18M12 3a14 14 0 0 1 0 18M12 3a14 14 0 0 0 0 18") }
    val Sort by lazy { stroke("Sort", "M7 4v16M4 17l3 3 3-3M17 20V4M14 7l3-3 3 3") }
    val Close by lazy { stroke("Close", "M6 6l12 12M18 6L6 18") }
    val Refresh by lazy { stroke("Refresh", "M20 12a8 8 0 1 1-2.3-5.7M20 4v5h-5") }
    val Trash by lazy { stroke("Trash", "M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13") }
    val Pin by lazy { stroke("Pin", "M12 17v5M8 3h8l-1 6 3 4H6l3-4z") }
    val Mail by lazy { stroke("Mail", "M4 6h16v12H4zM4 7l8 6 8-6") }
    val Stats by lazy { stroke("Stats", "M5 20V10M12 20V4M19 20v-7") }
    val Backup by lazy { stroke("Backup", "M4 6h16v4H4zM6 10v10h12V10M10 14h4") }
    val Help by lazy {
        stroke("Help", "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18z", "M9.5 9.5a2.5 2.5 0 1 1 3.5 2.3c-.6.3-1 .9-1 1.6M12 17h.01")
    }
    val User by lazy { stroke("User", "M12 4a4 4 0 1 0 0 8a4 4 0 1 0 0-8z", "M4 20a8 8 0 0 1 16 0") }
    val Track by lazy { stroke("Track", "M20 12a8 8 0 0 1-14 5.3M4 12a8 8 0 0 1 14-5.3", "M18 3v4h-4M6 21v-4h4") }
    val Share by lazy { stroke("Share", "M12 4v12M7 9l5-5 5 5M5 14v6h14v-6") }
    val Warning by lazy { stroke("Warning", "M12 4l9 16H3z", "M12 10v4M12 17h.01") }
    val ArrowUp by lazy { stroke("ArrowUp", "M12 20V6M6 12l6-6 6 6") }
    val Pages by lazy { stroke("Pages", "M6 3h12a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z", "M12 3v18") }
    val Crop by lazy { stroke("Crop", "M4 9V4h5M20 15v5h-5M4 4l6 6M20 20l-6-6") }
    val Sun by lazy {
        stroke(
            "Sun", "M12 8a4 4 0 1 0 0 8a4 4 0 1 0 0-8z",
            "M12 2v2M12 20v2M2 12h2M20 12h2M5 5l1.5 1.5M17.5 17.5L19 19M5 19l1.5-1.5M17.5 6.5L19 5",
        )
    }
    val PrevChapter by lazy { stroke("PrevChapter", "M18 6l-6 6 6 6M8 6v12") }
    val NextChapter by lazy { stroke("NextChapter", "M6 6l6 6-6 6M16 6v12") }
    val ListBullets by lazy { stroke("List", "M9 6h11M9 12h11M9 18h11M4 6h.01M4 12h.01M4 18h.01") }
    val CloudCheck by lazy { stroke("CloudCheck", "M7 18a5 5 0 0 1-.5-10A6 6 0 0 1 18 9a4.5 4.5 0 0 1-.5 9z", "M9 13l2 2 4-4") }
    val Edit by lazy { stroke("Edit", "M4 20h4L19 9l-4-4L4 16zM14 6l4 4") }
    val Plus by lazy { stroke("Plus", "M12 5v14M5 12h14") }

    // Gamification & Milestones
    val Shield by lazy { stroke("Shield", "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z") }
    val Crown by lazy { stroke("Crown", "M2 4l3 12h14l3-12-6 7-4-7-4 7-6-7z", "M3 20h18") }
    val Trophy by lazy { stroke("Trophy", "M6 9H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h2", "M18 9h2a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2h-2", "M4 22h16", "M12 15a6 6 0 0 0 6-6V3H6v6a6 6 0 0 0 6 6z", "M12 15v7") }
    val Flame by lazy { stroke("Flame", "M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.07-2.14-.22-4.05 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.15.43-2.29 1-3a2.5 2.5 0 0 0 2.5 2.5z") }
    val BookOpen by lazy { stroke("BookOpen", "M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z", "M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z") }
    val Sparkles by lazy { stroke("Sparkles", "M12 3l1.9 5.8a2 2 0 0 0 1.3 1.3L21 12l-5.8 1.9a2 2 0 0 0-1.3 1.3L12 21l-1.9-5.8a2 2 0 0 0-1.3-1.3L3 12l5.8-1.9a2 2 0 0 0 1.3-1.3L12 3z") }
    val Zap by lazy { stroke("Zap", "M13 2L3 14h9l-1 8 10-12h-9l1-8z") }
    val Star by lazy { stroke("Star", "M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z") }
    val Award by lazy { stroke("Award", "M12 15a7 7 0 1 0 0-14 7 7 0 0 0 0 14z", "M8.2 13.9L7 23l5-3 5 3-1.2-9.1") }
    val Lock by lazy { stroke("Lock", "M5 11h14v10H5z", "M8 11V7a4 4 0 0 1 8 0v4") }
    val Compass by lazy { stroke("Compass", "M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20z", "M16.2 7.8l-2.1 6.4-6.4 2.1 2.1-6.4 6.4-2.1z") }
    val Feather by lazy { stroke("Feather", "M20.2 12.2a6 6 0 0 0-8.5-8.5L5 10.5V19h8.5z", "M16 8L2 22", "M17.5 15H9") }
    val Scroll by lazy { stroke("Scroll", "M8 21h12a2 2 0 0 0 2-2v-2H10v2a2 2 0 1 1-4 0V5a2 2 0 1 0-4 0v3h4", "M19 17V5a2 2 0 0 0-2-2H4") }
    val InfinityIcon by lazy { stroke("Infinity", "M18.2 8c5.1 0 5.1 8 0 8-2.7 0-4.8-2.6-6.2-4-1.4 1.4-3.5 4-6.2 4-5.1 0-5.1-8 0-8 2.7 0 4.8 2.6 6.2 4 1.4-1.4 3.5-4 6.2-4z") }
    val Chat by lazy { stroke("Chat", "M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z") }
    val Heart by lazy { stroke("Heart", "M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z") }
    val HeartFilled by lazy { fill("HeartFilled", "M12 21.23l-1.06-1.06a5.5 5.5 0 0 1-7.78-7.78l1.06-1.06L12 5.67l7.78 5.66 1.06 1.06a5.5 5.5 0 0 1-7.78 7.78L12 21.23z") }
    val Send by lazy { stroke("Send", "M22 2L11 13", "M22 2l-7 20-4-9-9-4 20-7z") }

    private fun stroke(name: String, vararg paths: String, width: Float = 2f): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { d ->
                addPath(
                    pathData = addPathNodes(d),
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = width,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    private fun fill(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { d -> addPath(pathData = addPathNodes(d), fill = SolidColor(Color.Black)) }
        }.build()
}

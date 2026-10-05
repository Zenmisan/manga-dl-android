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

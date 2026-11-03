package com.tuned.app.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import com.tuned.app.ui.theme.Sizes

/**
 * Tuned's own icons for menus and sheets, drawn like the player's bottom row
 * (see PlayerActionIcons): 2dp rounded strokes on a 24dp grid, one weight and
 * one visual size, so they never read as stock Material glyphs. The colour
 * comes from the Icon's tint.
 */
object TunedIcons {
    val PlayNext by lazy { icon("PlayNext", "M4 7h10", "M4 12h10", "M4 17h6", "M16 13l4 3l-4 3") }
    val Queue by lazy { icon("Queue", "M3 6h13", "M3 11h13", "M3 16h7", filled = listOf("M15 14v6l5-3z")) }
    val PlaylistAdd by lazy { icon("PlaylistAdd", "M3 6h12", "M3 11h12", "M3 16h7", "M17 13v8", "M13 17h8") }
    val Mix by lazy {
        icon(
            "Mix",
            "M14 12a2 2 0 1 1-4 0a2 2 0 1 1 4 0",
            "M7.8 7.8a6 6 0 0 0 0 8.4",
            "M16.2 7.8a6 6 0 0 1 0 8.4",
            "M5 5a10 10 0 0 0 0 14",
            "M19 5a10 10 0 0 1 0 14",
        )
    }
    val Play by lazy { icon("Play", filled = listOf("M8 5v14l11-7z")) }
    val Person by lazy { icon("Person", "M16 8a4 4 0 1 1-8 0a4 4 0 1 1 8 0", "M4 20a8 6 0 0 1 16 0") }
    val Download by lazy { icon("Download", "M12 4v11", "M7.5 10.5l4.5 4.5l4.5-4.5", "M5 20h14") }
    val Downloaded by lazy { icon("Downloaded", "M5 20h14", "M7 11l3.5 3.5L17 8") }
    val Moon by lazy { icon("Moon", "M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z") }
    val Cast by lazy {
        icon(
            "Cast",
            "M3 8V6a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-6",
            "M3 12a8 8 0 0 1 8 8",
            "M3 16a4 4 0 0 1 4 4",
            "M3 20h0.01",
        )
    }
    val Info by lazy { icon("Info", "M21 12a9 9 0 1 1-18 0a9 9 0 1 1 18 0", "M12 11v5", "M12 8h0.01") }
    val OpenOut by lazy {
        icon("OpenOut", "M14 4h6v6", "M20 4l-9 9", "M18 14v4a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4")
    }
    val Copy by lazy {
        icon(
            "Copy",
            "M9 9h10a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H9a1 1 0 0 1-1-1V10a1 1 0 0 1 1-1z",
            "M5 15H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v1",
        )
    }
    val Video by lazy {
        icon("Video", "M3 7a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z", filled = listOf("M10 9v6l5-3z"))
    }
    val Refresh by lazy {
        icon("Refresh", "M20 11a8 8 0 0 0-14.5-4.5L4 8", "M4 4v4h4", "M4 13a8 8 0 0 0 14.5 4.5L20 16", "M20 20v-4h-4")
    }
    val NotForMe by lazy { icon("NotForMe", "M21 12a9 9 0 1 1-18 0a9 9 0 1 1 18 0", "M5.6 5.6l12.8 12.8") }
    val Remove by lazy { icon("Remove", "M21 12a9 9 0 1 1-18 0a9 9 0 1 1 18 0", "M8 12h8") }
    val Heart by lazy { icon("Heart", HEART) }
    val HeartFilled by lazy { icon("HeartFilled", filled = listOf(HEART)) }
    val Share by lazy { icon("Share", "M12 15V4", "M8 8l4-4l4 4", "M6 12H5a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-6a1 1 0 0 0-1-1h-1") }
    val More by lazy { icon("More", "M6 12h0.01", "M12 12h0.01", "M18 12h0.01") }
    val Back by lazy { icon("Back", "M19 12H5", "M11 6l-6 6l6 6") }
    val ChevronBack by lazy { icon("ChevronBack", "M15 5l-7 7l7 7") }
    val ChevronRight by lazy { icon("ChevronRight", "M9 5l7 7l-7 7") }
    /** The bare "i" of Apple Music's artist-info button (no ring: it sits inside a round button). */
    val InfoLetter by lazy { icon("InfoLetter", "M12 11v7", "M12 6.5h0.01") }
    val Star by lazy { icon("Star", STAR) }
    val StarFilled by lazy { icon("StarFilled", filled = listOf(STAR)) }

    private const val STAR = "M12 3.8l2.5 5.1l5.6 0.8l-4.05 3.95l0.95 5.6L12 16.6l-5 2.65l0.95-5.6L3.9 9.7l5.6-0.8z"
    private const val HEART = "M12 20s-7.5-4.6-8.8-9.4C2.3 7.4 4.2 4.5 7.3 4.5c2 0 3.6 1.1 4.7 2.8c1.1-1.7 2.7-2.8 4.7-2.8c3.1 0 5 2.9 4.1 6.1C19.5 15.4 12 20 12 20z"

    private fun icon(name: String, vararg strokes: String, filled: List<String> = emptyList()): ImageVector =
        ImageVector.Builder(name, Sizes.icon, Sizes.icon, 24f, 24f).apply {
            val ink = SolidColor(Color.Black)
            strokes.forEach {
                addPath(
                    pathData = addPathNodes(it),
                    stroke = ink,
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
            filled.forEach {
                addPath(pathData = addPathNodes(it), fill = ink, stroke = ink, strokeLineWidth = 2f, strokeLineJoin = StrokeJoin.Round)
            }
        }.build()
}

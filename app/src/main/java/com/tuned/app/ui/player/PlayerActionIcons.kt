package com.tuned.app.ui.player

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import com.tuned.app.ui.theme.Sizes

/**
 * The player's bottom-row icons, drawn as one set: 2dp rounded strokes on a
 * 24dp grid, so shuffle, lyrics, queue, sound and repeat share one weight
 * and one visual size. The colour comes from the Icon's tint.
 */
internal object PlayerActionIcons {
    val Shuffle: ImageVector by lazy {
        icon(
            "Shuffle",
            "M3 7h2.5c2.2 0 3.6 1.2 5 3.5l2 3c1.4 2.3 2.8 3.5 5 3.5H21",
            "M18 14l3 3l-3 3",
            "M3 17h2.5c1.2 0 2.2-0.4 3-1.1",
            "M14.5 8.1c0.8-0.7 1.8-1.1 3-1.1H21",
            "M18 4l3 3l-3 3",
        )
    }

    val Lyrics: ImageVector by lazy {
        icon(
            "Lyrics",
            "M5 4h14a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2h-7l-4 3.5V17H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z",
            "M7.5 8.5h9",
            "M7.5 12.5h6",
        )
    }

    val Queue: ImageVector by lazy {
        icon(
            "Queue",
            "M3 6h13",
            "M3 11h13",
            "M3 16h7",
            filled = listOf("M15 14v6l5-3z"),
        )
    }

    val Sound: ImageVector by lazy {
        icon(
            "Sound",
            "M6 3v4", "M6 11v10", "M8 9a2 2 0 1 1-4 0a2 2 0 1 1 4 0",
            "M12 3v10", "M12 17v4", "M14 15a2 2 0 1 1-4 0a2 2 0 1 1 4 0",
            "M18 3v2", "M18 9v12", "M20 7a2 2 0 1 1-4 0a2 2 0 1 1 4 0",
        )
    }

    val Repeat: ImageVector by lazy { icon("Repeat", *RepeatLoop) }

    val RepeatOne: ImageVector by lazy { icon("RepeatOne", *RepeatLoop, "M11 10.5l1.5-1v5") }

    private val RepeatLoop = arrayOf(
        "M17 3l3 3l-3 3",
        "M4 11v-1a4 4 0 0 1 4-4h12",
        "M7 21l-3-3l3-3",
        "M20 13v1a4 4 0 0 1-4 4H4",
    )

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
                addPath(
                    pathData = addPathNodes(it),
                    fill = ink,
                    stroke = ink,
                    strokeLineWidth = 2f,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()
}

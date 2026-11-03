package com.tuned.app.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.rememberMotion
import com.tuned.app.ui.theme.tunedColors
import kotlin.math.PI
import kotlin.math.cos

/** Gradient stops; enough that the curve reads as one smooth ramp. */
private const val BAR_SCRIM_STEPS = 24

/**
 * The backdrop for a top bar over scrolling content: one long, even gradient in the page colour,
 * full at the top and clear 64dp past the bar's bottom, with no solid part and no step (a solid
 * hold followed by a short fade read as a band over light pictures). It eases out on a quarter
 * cosine, so it stays mostly opaque behind the bar's title and thins gently below. Fades in when
 * [visible] turns on. It draws past the bar's bottom on purpose.
 */
@Composable
fun Modifier.barScrim(visible: Boolean, color: Color = MaterialTheme.tunedColors.bg): Modifier {
    val alpha by animateFloatAsState(if (visible) 1f else 0f, rememberMotion().base(), label = "barScrim")
    return drawBehind {
        if (alpha <= 0f) return@drawBehind
        val height = size.height + Space.x6.toPx()
        val stops = Array(BAR_SCRIM_STEPS + 1) { i ->
            val t = i / BAR_SCRIM_STEPS.toFloat()
            t to color.copy(alpha = alpha * cos(t * PI / 2).toFloat())
        }
        drawRect(
            brush = Brush.verticalGradient(colorStops = stops, startY = 0f, endY = height),
            size = Size(size.width, height),
        )
    }
}

package com.tuned.app.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Size
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The full-screen "this screen is loading" state, shared across screens
 * waiting on a first load: the Tuned flower ([LoadingSpinner]) with an
 * optional message under it.
 */
@Composable
fun ExpressiveLoadingIndicator(message: String? = null, modifier: Modifier = Modifier) {
    Box(
        modifier.semantics { progressBarRangeInfo = androidx.compose.ui.semantics.ProgressBarRangeInfo.Indeterminate },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LoadingSpinner(modifier = Modifier.size(44.dp))
            if (!message.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Compact shared loader for pagination, buttons and row-level work. */
@Composable
fun ExpressiveInlineLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 22.dp,
    strokeWidth: Dp = 2.5.dp,
) {
    LoadingSpinner(modifier = modifier.size(size), color = color, strokeWidth = strokeWidth)
}

/**
 * Tuned's loading indicator, the same everywhere: one rounded arc that turns
 * steadily while it stretches and shrinks. Its parameters mirror Material's
 * indeterminate CircularProgressIndicator so it drops in for one; [strokeWidth]
 * defaults to a tenth of the size and [trackColor] draws an optional full ring
 * behind the arc. With system animations off it holds a still arc and only
 * pulses its opacity.
 */
@Composable
fun LoadingSpinner(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = Dp.Unspecified,
    trackColor: Color = Color.Transparent,
) {
    val reduced = com.tuned.app.ui.theme.rememberMotion().reduced
    val transition = rememberInfiniteTransition(label = "spinner")
    val turn by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1_200, easing = LinearEasing)),
        label = "spinnerTurn",
    )
    // The arc's length breathes from a short dash to most of the circle and back.
    val stretch by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(750, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "spinnerStretch",
    )
    Canvas(modifier.defaultMinSize(minWidth = 24.dp, minHeight = 24.dp)) {
        val stroke = if (strokeWidth == Dp.Unspecified) size.minDimension * 0.1f else strokeWidth.toPx()
        if (reduced) {
            drawArc(color, -90f, 270f, useCenter = false, alpha = 0.55f + 0.45f * stretch, style = Stroke(stroke, cap = StrokeCap.Round), topLeft = Offset(stroke / 2, stroke / 2), size = Size(size.width - stroke, size.height - stroke))
            return@Canvas
        }
        val sweep = 28f + 232f * stretch
        val inset = Offset(stroke / 2, stroke / 2)
        val box = Size(size.width - stroke, size.height - stroke)
        if (trackColor != Color.Transparent) {
            drawArc(trackColor, 0f, 360f, useCenter = false, style = Stroke(stroke), topLeft = inset, size = box)
        }
        // The tail lags as the arc grows, so the head leads the turn.
        drawArc(color, turn - 90f - sweep * 0.5f, sweep, useCenter = false, style = Stroke(stroke, cap = StrokeCap.Round), topLeft = inset, size = box)
    }
}

/**
 * The spinner for a pull-to-refresh: the arc grows and turns with the pull,
 * then spins like [LoadingSpinner] while refreshing. [progress] is how far the
 * pull has gone, 1 at the refresh threshold.
 */
@Composable
fun PullSpinner(progress: Float, refreshing: Boolean, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    if (refreshing) {
        LoadingSpinner(modifier = modifier, color = color)
        return
    }
    Canvas(modifier.defaultMinSize(minWidth = 24.dp, minHeight = 24.dp)) {
        val p = progress.coerceIn(0f, 1f)
        val stroke = size.minDimension * 0.1f
        drawArc(
            color, -90f + p * 120f, 300f * p, useCenter = false, alpha = (p * 1.4f).coerceAtMost(1f),
            style = Stroke(stroke, cap = StrokeCap.Round),
            topLeft = Offset(stroke / 2, stroke / 2), size = Size(size.width - stroke, size.height - stroke),
        )
    }
}

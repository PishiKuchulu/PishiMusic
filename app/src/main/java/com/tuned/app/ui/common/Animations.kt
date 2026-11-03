package com.tuned.app.ui.common

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.tuned.app.ui.theme.rememberMotion

/** How far a pressed tile or button shrinks. */
private const val PRESSED_SCALE = 0.96f

/**
 * Shrinks to 96% while pressed and springs back on release (plan, motion
 * inventory #22). Uses graphicsLayer, so it never re-measures. Rows don't
 * use this: a full-width row shrinking reads as a glitch, so they keep the
 * ripple only.
 */
fun Modifier.pressScale(interactionSource: InteractionSource): Modifier = composed {
    val motion = rememberMotion()
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) PRESSED_SCALE else 1f,
        animationSpec = if (pressed) motion.fast() else motion.spring(),
        label = "pressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Swaps a skeleton for content with a crossfade (inventory #18). Both sides
 * should share geometry, as [SkeletonTrackRow] and [TrackRow] do, so the list
 * does not jump.
 */
@Composable
fun <T> LoadingCrossfade(
    state: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    val motion = rememberMotion()
    Crossfade(targetState = state, modifier = modifier, animationSpec = motion.base(), label = "loadingCrossfade") {
        content(it)
    }
}

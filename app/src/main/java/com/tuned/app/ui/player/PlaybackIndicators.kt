@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.tuned.app.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.first
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tuned.app.ui.common.ExpressiveMotion

@Composable
internal fun AnimatedPlayPauseIcon(isPlaying: Boolean, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = isPlaying,
        transitionSpec = {
            (fadeIn(tween(ExpressiveMotion.Quick)) +
                scaleIn(ExpressiveMotion.spatialSpring(), initialScale = 0.7f)) togetherWith
                (fadeOut(tween(ExpressiveMotion.Quick)) +
                    scaleOut(tween(ExpressiveMotion.Quick), targetScale = 0.7f))
        },
        label = "playPauseIcon",
        modifier = modifier,
    ) { playing ->
        Icon(
            if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = stringResource(com.tuned.app.R.string.player_play_pause_cd),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun PlayingWaveBars(
    modifier: Modifier = Modifier,
    waveColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    shape: Shape = CircleShape,
    /** False renders a still wave: the three infinite animations used to run
     * (and recompose their host row) even when nothing was playing. */
    isPlaying: Boolean = true,
) {
    var firstFraction = 0.25f
    var secondFraction = 0.95f
    var thirdFraction = 0.35f
    if (isPlaying) {
        val transition = rememberInfiniteTransition(label = "miniArtworkWave")
        val first = transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 480, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "miniWaveFirst",
        )
        val second = transition.animateFloat(
            initialValue = 0.95f,
            targetValue = 0.30f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 640, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "miniWaveSecond",
        )
        val third = transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 530, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "miniWaveThird",
        )
        firstFraction = first.value
        secondFraction = second.value
        thirdFraction = third.value
    }

    val content: @Composable () -> Unit = {
        // Fixed small badge content so it can never expand to fill the cover.
        // Outer Box is 26x22 (16+5+5, 12+5+5) — always tiny, bottom-end aligned by caller.
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .padding(horizontal = 5.dp, vertical = 5.dp)
                .size(16.dp, 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.Canvas(
                Modifier.fillMaxSize(),
            ) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas
            val barCount = 3
            val barWidth = (size.width / 5.2f).coerceAtLeast(1.5f)
            val barGap = barWidth * 0.9f
            val totalContentWidth = barCount * barWidth + (barCount - 1) * barGap
            val startX = ((size.width - totalContentWidth) / 2f).coerceAtLeast(0f)

            repeat(barCount) { index ->
                val fraction = when (index) {
                    0 -> firstFraction
                    1 -> secondFraction
                    else -> thirdFraction
                }
                val barHeight = (size.height * fraction).coerceIn(minOf(barWidth, size.height), size.height)
                drawRoundRect(
                    color = waveColor,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        x = startX + index * (barWidth + barGap),
                        y = size.height - barHeight,
                    ),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f),
                )
            }
            }
        }
    }

    if (containerColor == Color.Transparent) {
        Box(
            modifier = modifier.defaultMinSize(minWidth = 24.dp, minHeight = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    } else {
        Surface(
            shape = shape,
            color = containerColor,
            modifier = modifier.defaultMinSize(minWidth = 26.dp, minHeight = 22.dp),
        ) {
            content()
        }
    }
}

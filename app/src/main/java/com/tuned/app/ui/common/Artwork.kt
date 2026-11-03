package com.tuned.app.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tuned.app.ui.theme.AppShape
import com.tuned.app.ui.theme.Motion
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.rememberMotion
import com.tuned.app.ui.theme.tunedColors

/**
 * Artwork (square unless [height] differs) with a neutral placeholder. Decorative by default: rows and
 * tiles describe the item in their own semantics, so the image adds nothing
 * for a screen reader.
 */
@Composable
fun Artwork(
    url: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    height: Dp = size,
    shape: Shape = AppShape.xs,
    fallbackIcon: ImageVector = Icons.Filled.MusicNote,
    /** For an artist's picture: Apple Music's photo of this artist when it has one, else [url]. */
    artistName: String? = null,
) {
    val reduced = rememberMotion().reduced
    val context = LocalContext.current
    // Build the request once per (url, artist, target size): rebuilding it in
    // the composition body made every recomposition of every row allocate a
    // fresh request, and with no size() Coil decoded full-resolution covers
    // for 48dp rows.
    val targetPx = with(androidx.compose.ui.platform.LocalDensity.current) { maxOf(size, height).roundToPx() }
    val request = androidx.compose.runtime.remember(url, artistName, targetPx, reduced) {
        ImageRequest.Builder(context)
            .data(if (artistName.isNullOrBlank()) url else com.tuned.app.data.canvas.ArtistImage(artistName, url))
            .size(targetPx)
            .crossfade(if (reduced) 0 else Motion.BASE_MS)
            .build()
    }
    Box(modifier.size(width = size, height = height).clip(shape)) {
        if (url.isNullOrBlank() && artistName.isNullOrBlank()) {
            ArtworkPlaceholder(fallbackIcon, minOf(size, height))
        } else {
            SubcomposeAsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { ArtworkPlaceholder(fallbackIcon, minOf(size, height)) },
                error = { ArtworkPlaceholder(fallbackIcon, minOf(size, height)) },
            )
        }
    }
}

@Composable
private fun ArtworkPlaceholder(icon: ImageVector, size: Dp) {
    val colors = MaterialTheme.tunedColors
    Box(Modifier.fillMaxSize().background(colors.surface2), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = colors.text3, modifier = Modifier.size(size / 2))
    }
}

/**
 * Three bars that bounce while a track plays and rest when paused. 16dp wide:
 * 4dp bars with 2dp gaps. Holds still when the system removes animations.
 */
@Composable
fun PlayingIndicator(
    playing: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.tunedColors.accent,
) {
    val reduced = rememberMotion().reduced
    val transition = rememberInfiniteTransition(label = "playingIndicator")
    val phases = listOf(420, 540, 360).map { period ->
        transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(period, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "bar$period",
        )
    }
    Row(
        modifier.size(Sizes.iconInline),
        horizontalArrangement = Arrangement.spacedBy(Space.xxs),
        verticalAlignment = Alignment.Bottom,
    ) {
        phases.forEachIndexed { i, phase ->
            val rest = listOf(0.5f, 0.8f, 0.4f)[i]
            Box(
                Modifier
                    .width(Space.xs)
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleY = if (playing && !reduced) phase.value else rest
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .clip(AppShape.xs)
                    .background(color),
            )
        }
    }
}

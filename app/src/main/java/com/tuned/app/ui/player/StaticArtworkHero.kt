package com.tuned.app.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tuned.app.data.artwork.ArtworkNormalizer

/**
 * Renders static album cover art in a full-bleed top-anchored hero mode.
 *
 * Characteristics:
 * - Uses the high-resolution master image (Spotify/Apple Music/Tidal/Deezer) or upscaled YouTube art.
 * - Anchors top-aligned and crops to fill full viewport width down to the playback controls.
 * - Applies a hardware-accelerated bottom alpha gradient fade (BlendMode.DstIn) to smoothly melt
 *   the artwork into the dark scrim and fluid background behind the player controls.
 * - Hardware-accelerated alpha transition when switching between player tabs.
 * - Smooth 350ms crossfade between consecutive tracks.
 */
@Composable
fun StaticArtworkHero(
    name: String,
    artist: String,
    embeddedUrl: String?,
    resolvedUrl: String?,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    bottomFade: Float = 0.42f,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.TopCenter,
) {
    val context = LocalContext.current

    val highResUrl = resolvedUrl?.takeIf { it.isNotBlank() }
    val displayUrl = if (highResUrl != null) {
        highResUrl
    } else if (ArtworkNormalizer.isRealImage(embeddedUrl)) {
        ArtworkNormalizer.upscaleYoutubeArtwork(embeddedUrl)
    } else {
        null
    }

    if (displayUrl.isNullOrBlank()) {
        // No cover: draw a neutral placeholder instead of returning empty space
        // (the hero area used to vanish, collapsing the layout above the controls).
        Box(
            modifier = modifier.graphicsLayer { this.alpha = alpha },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(64.dp),
            )
        }
        return
    }

    val model = remember(displayUrl, context) {
        ImageRequest.Builder(context)
            .data(displayUrl)
            .crossfade(350)
            .allowHardware(true)
            .build()
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                if (alpha <= 0.001f) return@drawWithContent
                drawContent()
                if (bottomFade > 0.001f) {
                    // Eased (smootherstep) so the cover tapers off softly at both
                    // ends of the fade instead of along a straight ramp.
                    val start = 1f - bottomFade.coerceIn(0f, 1f)
                    val steps = 10
                    val stops = Array(steps + 2) { i ->
                        if (i == 0) {
                            0f to Color.Black
                        } else {
                            val t = (i - 1) / steps.toFloat()
                            val eased = t * t * t * (t * (t * 6f - 15f) + 10f)
                            (start + (1f - start) * t) to Color.Black.copy(alpha = 1f - eased)
                        }
                    }
                    drawRect(
                        brush = Brush.verticalGradient(*stops, startY = 0f, endY = size.height),
                        blendMode = BlendMode.DstIn,
                    )
                }
            },
    ) {
        AsyncImage(
            model = model,
            // The cover is the page's main visual: name it for TalkBack instead
            // of leaving it invisible (was null).
            contentDescription = listOf(name, artist).filter { it.isNotBlank() }.joinToString(" — "),
            contentScale = contentScale,
            alignment = alignment,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

package com.tuned.app.ui.common

import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.rememberMotion
import com.tuned.app.ui.theme.tunedColors

/**
 * The tinted backdrop of Home's header, shared so other pages can use the same look: the
 * [artwork] scaled up and blurred far past recognition, a vertical wash of [tint] over it, and a
 * soft light from the top-right corner. Artwork and tint fade to new values over 700ms. Blur
 * needs Android 12; older devices get the wash alone. Fills its parent; content goes on top.
 */
@Composable
fun ArtworkTintBackdrop(
    artwork: Any?,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.tunedColors
    val reduced = rememberMotion().reduced
    val animatedTint by animateColorAsState(
        targetValue = tint,
        animationSpec = if (reduced) snap() else tween(700),
        label = "backdropTint",
    )
    val strength = if (colors.isDark) 0.26f else 0.16f
    val bg = colors.bg
    Box(modifier.fillMaxSize()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Crossfade(
                targetState = artwork,
                animationSpec = if (reduced) snap() else tween(700),
                label = "backdropArt",
                modifier = Modifier.fillMaxSize(),
            ) { model ->
                if (model != null && model != "") {
                    AsyncImage(
                        model = model,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { scaleX = 1.4f; scaleY = 1.4f }
                            .blur(Space.xxl * 2, BlurredEdgeTreatment.Rectangle)
                            .graphicsLayer { alpha = 0.75f },
                    )
                }
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            0f to lerp(bg, animatedTint, strength).copy(alpha = 0.45f),
                            1f to lerp(bg, animatedTint, strength * 0.35f).copy(alpha = 0.8f),
                        ),
                    )
                    val glow = size.width * 0.8f
                    drawCircle(
                        Brush.radialGradient(
                            listOf(animatedTint.copy(alpha = 0.18f), Color.Transparent),
                            center = Offset(size.width, 0f),
                            radius = glow,
                        ),
                        radius = glow,
                        center = Offset(size.width, 0f),
                    )
                },
        )
    }
}

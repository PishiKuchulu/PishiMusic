@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.tuned.app.ui.player

import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import kotlin.math.exp
import kotlin.math.sqrt
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.Rect
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import com.tuned.app.ui.theme.Motion
import com.tuned.app.ui.theme.rememberMotion

/** The SharedTransitionLayout that hosts both the mini player and the full player. */
internal val LocalPlayerSharedScope = compositionLocalOf<SharedTransitionScope?> { null }

/** Whether full-screen artwork is on; the solid shared card and the darkening close are only used then. */
internal val LocalPlayerFullBleed = compositionLocalOf { true }

/** The AnimatedVisibility scope of whichever player (mini or full) is composing. */
internal val LocalPlayerVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

private const val NOW_PLAYING_ARTWORK = "now-playing-artwork"
private const val NOW_PLAYING_CONTAINER = "now-playing-container"
private const val NOW_PLAYING_CARD = "now-playing-card"

/*
 * Apple Music's mini player <-> Now Playing timing (iOS reverse-engineering notes
 * notes/FINDINGS.md §31, PalettePresentationAnimationController): geometry on
 * a critically damped spring (mass 3, stiffness 500) time-warped by a cubic,
 * settling in 0.8 s and ~93% done by 0.28 s; fades 0.5 s ease-in-out. Closing
 * runs the same curves in reverse.
 */
internal const val PLAYER_MORPH_MS = 800
private val PaletteCubic = CubicBezierEasing(0.1878f, 0.0023f, 0.5399f, 0.9629f)
private val PaletteOmega = sqrt(500f / 3f)
internal val PaletteEasing = Easing { u ->
    val t = 0.8f * PaletteCubic.transform(u)
    1f - (1f + PaletteOmega * t) * exp(-PaletteOmega * t)
}
// Closing starts moving on the first frame. With the opening curve's slow start the cover hung
// at full size over the page while the player around it had already faded.
private val PlayerCloseEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private const val PLAYER_CLOSE_MS = 450

internal val PlayerCloseFloat = tween<Float>(PLAYER_CLOSE_MS, easing = PlayerCloseEasing)

/** Opening runs Apple's curve; closing (the bounds shrink) runs one that moves at once. */
private fun playerMorphSpec(initial: Rect, target: Rect) =
    if (target.width < initial.width) tween<Rect>(PLAYER_CLOSE_MS, easing = PlayerCloseEasing)
    else tween<Rect>(PLAYER_MORPH_MS, easing = PaletteEasing)

internal val PlayerFadeEasing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
internal val PlayerFade = tween<Float>(500, easing = PlayerFadeEasing)

/*
 * The card stays solid through the morph: whichever side ends on top turns
 * opaque within the first ~0.18 s, and the side that leaves stays opaque until
 * the geometry is nearly done (~93% at 0.28 s), then goes in ~0.15 s. So the
 * page behind never shows through the card.
 */
internal val PlayerFadeInTop = tween<Float>(200, easing = PlayerFadeEasing)
internal val PlayerFadeOutTop = tween<Float>(120, easing = PlayerFadeEasing)

// Closing with full-screen artwork: the picture fades over the whole approach
// (~93% of the geometry is done by 0.28 s), so the card darkens into the mini
// player's own surface as it lands instead of shrinking as a bright picture.
internal val PlayerFadeOutDarken = tween<Float>(280, easing = LinearEasing)
// The mini player's content (artwork, title, controls) fades in while the card
// is still landing, crossing the player's picture as it fades out, so the bar
// is already recognisable when the card arrives.
internal val PlayerFadeInUnder = tween<Float>(200, delayMillis = 110, easing = PlayerFadeEasing)
internal val PlayerFadeOutUnder = tween<Float>(120, easing = PlayerFadeEasing)

/**
 * Marks the now-playing artwork so it travels between the mini player and
 * the full player when one opens and the other closes (plan, motion
 * inventory #6). A no-op outside the player host, in previews, or when the
 * system removes animations.
 */
@Composable
internal fun Modifier.nowPlayingArtworkBounds(cornerRadius: androidx.compose.ui.unit.Dp = 16.dp): Modifier {
    val shared = LocalPlayerSharedScope.current ?: return this
    val visibility = LocalPlayerVisibilityScope.current ?: return this
    if (rememberMotion().reduced) return this
    // Shared bounds (both sides drawn, crossfading) rather than a shared
    // element: the side being revealed is composed fresh and needs a frame or
    // two to paint its image, so the outgoing cover, already drawn, stays up
    // until then. The cover never shows empty mid-morph.
    return with(shared) {
        this@nowPlayingArtworkBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(NOW_PLAYING_ARTWORK),
            animatedVisibilityScope = visibility,
            enter = fadeIn(tween(160, delayMillis = 90)),
            exit = fadeOut(tween(160, delayMillis = 240)),
            boundsTransform = { initial, target -> playerMorphSpec(initial, target) },
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.Crop, Alignment.Center),
            // Each side clips the travelling cover to its own corners (mini 20dp, player 22dp),
            // so it lands without the corners re-rounding; one fixed 16dp clip made them snap.
            clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(cornerRadius)),
            zIndexInOverlay = 2f,
        )
    }
}

/**
 * Marks the player's container, the mini player card and the full player
 * alike, so one grows into the other (Apple Music's morph): the card's bounds
 * spring between the bar and the screen, clipped to a rounded card the whole
 * way, while each side's content fades and scales to fit. A no-op outside the
 * player host or when the system removes animations.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.nowPlayingContainerBounds(shape: Shape, fullPlayer: Boolean, darkenOnClose: Boolean = false): Modifier {
    val shared = LocalPlayerSharedScope.current ?: return this
    val visibility = LocalPlayerVisibilityScope.current ?: return this
    if (rememberMotion().reduced) return this
    return with(shared) {
        this@nowPlayingContainerBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(NOW_PLAYING_CONTAINER),
            animatedVisibilityScope = visibility,
            // Opening: the mini player's side goes almost at once. Fading over 120ms it was
            // stretched to the growing player's size, its grey card showing as a grey flash.
            enter = fadeIn(if (fullPlayer) tween(120, easing = PlayerFadeEasing) else PlayerFadeInUnder),
            exit = fadeOut(if (!fullPlayer) tween(60, easing = PlayerFadeEasing) else if (darkenOnClose) PlayerFadeOutDarken else PlayerFadeOutTop),
            boundsTransform = { initial, target -> playerMorphSpec(initial, target) },
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.FillWidth, Alignment.TopCenter),
            clipInOverlayDuringTransition = OverlayClip(shape),
            zIndexInOverlay = 1f,
        )
    }
}

/**
 * The card's solid background, shared on its own: it never fades, it only
 * changes size and corners between the bar and the screen, so the morph reads
 * as one solid card while each side's content fades over it.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.nowPlayingCardBackground(color: Color, shape: Shape): Modifier =
    // No longer shared between the bars: the travelling card flashed as a flat grey block.
    this.background(color, shape)

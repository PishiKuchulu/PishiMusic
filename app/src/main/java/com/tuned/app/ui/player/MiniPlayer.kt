package com.tuned.app.ui.player

import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.lerp
import kotlin.math.sin
import kotlin.math.PI
import com.tuned.app.ui.theme.Radius
import com.tuned.app.ui.common.pressScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.backdrop.Backdrop
import com.tuned.app.R
import com.tuned.app.playback.PlaybackChromeState
import com.tuned.app.playback.PlaybackProgressState
import com.tuned.app.ui.common.AppIconButton
import com.tuned.app.ui.theme.AppShape
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.HapticKind
import com.tuned.app.ui.theme.Keyline
import com.tuned.app.ui.theme.LiquidGlassPreset
import com.tuned.app.ui.theme.LocalLiquidGlass
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.liquidGlassChrome
import com.tuned.app.ui.theme.rememberHaptics
import com.tuned.app.ui.theme.rememberMotion
import com.tuned.app.ui.theme.tunedColors
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

/** Widest the floating chrome gets on tablets and in landscape. */
internal val ChromeMaxWidth: Dp = Sizes.tileWideWidth * 2 + Space.xxl * 2

/**
 * The floating mini player. It shares its width, shape and surface with the
 * dock under it, so the two read as one island.
 *
 * 72dp tall with 28dp corners: 56dp artwork (20dp corners) inset 8dp, title
 * and artist with a wavy progress line under them, tonal play/pause, next.
 *
 * Gestures: tap or swipe up opens the player, swipe down clears the queue,
 * swipe left/right skips.
 */
@Composable
internal fun MiniPlayer(
    state: PlaybackChromeState,
    progressState: StateFlow<PlaybackProgressState>,
    onExpand: () -> Unit,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
) {
    val track = state.current ?: return
    val colors = MaterialTheme.tunedColors
    val motion = rememberMotion()
    val haptics = rememberHaptics()
    val liquidGlass = LocalLiquidGlass.current
    // The dock's radius, so the mini player and the dock read as one family.
    val shape = AppShape.dock

    var dragging by remember { mutableStateOf(false) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    // Horizontal swipes are skip gestures: in RTL locales "next" points the
    // other way, so the raw pixel sign must be mirrored before it is read as
    // next/previous (the card's visual translation still follows the finger).
    val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
    val gestureSign = if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl) -1f else 1f
    val shownX by animateFloatAsState(dragX, if (dragging) snap() else motion.spring(), label = "miniX")
    val shownY by animateFloatAsState(dragY, if (dragging) snap() else motion.spring(), label = "miniY")
    val threshold = with(LocalDensity.current) { Sizes.miniPlayer.toPx() }

    // Follows the finger while the bar is dragged; shared by the glass plate and the card.
    val dragLayer: GraphicsLayerScope.() -> Unit = {
        translationX = shownX
        translationY = shownY.coerceAtLeast(0f)
        alpha = 1f - ((abs(shownX) + shownY.coerceAtLeast(0f)) / (threshold * 4f)).coerceIn(0f, 0.45f)
    }

    Box(
        modifier
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            .padding(start = Keyline.margin, end = Keyline.margin, bottom = bottomPadding)
            .widthIn(max = ChromeMaxWidth)
            .fillMaxWidth(),
    ) {
    // The glass sits on its own plate, outside the shared-bounds morph: inside it, the glass was
    // drawn in the transition overlay (scaled, moving) while sampling the bar's resting place,
    // which showed a grey slab and a coloured rim mid-animation. Here it stays where it samples
    // and fades with the bar, so it is live glass the whole time; the morphing card above it
    // carries only the tint and the content.
    if (backdrop != null) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer(dragLayer)
                .liquidGlassChrome(shape, liquidGlass, LiquidGlassPreset.BottomNavigation, backdrop)
                .clip(shape),
        )
    }
    Box(
        Modifier
            .fillMaxWidth()
            // The card itself grows into the full player.
            .nowPlayingContainerBounds(shape, fullPlayer = false)
            .graphicsLayer(dragLayer)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDragCancel = {
                        dragging = false
                        dragX = 0f
                        dragY = 0f
                    },
                    onDragEnd = {
                        dragging = false
                        val gestureX = dragX * gestureSign
                        when {
                            dragY > threshold -> onClose()
                            dragY < -threshold -> onExpand()
                            gestureX < -threshold -> onNext()
                            gestureX > threshold -> onPrevious()
                            else -> Unit
                        }
                        if (abs(dragX) > threshold || abs(dragY) > threshold) haptics.perform(HapticKind.Confirm)
                        dragX = 0f
                        dragY = 0f
                    },
                ) { change, amount ->
                    change.consume()
                    if (abs(dragX + amount.x) > abs(dragY + amount.y)) dragX += amount.x else dragY += amount.y
                }
            }
            .clip(shape)
            .border(Sizes.hairline, colors.line, shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onExpand),
    ) {
        // The card's background is shared with the full player, so the bar and
        // the player morph as one solid card.
        Box(Modifier.matchParentSize().nowPlayingCardBackground(com.tuned.app.ui.shell.chromeSurfaceColor(backdrop), shape))
        Row(
            Modifier
                .fillMaxWidth()
                .height(Sizes.miniPlayer)
                .padding(start = MiniArtInset, end = Space.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 52dp artwork inset 10dp: 22dp corners stay concentric with the 32dp card.
            Box(Modifier.size(MiniArtSize).clip(MiniArtShape).background(colors.surface3)) {
                // The same pinned cover as the full player, so the artwork that
                // travels between them is the image already in memory.
                val cover = rememberPinnedCover(track, androidx.hilt.navigation.compose.hiltViewModel())
                PlayerArtwork(
                    track,
                    Modifier.nowPlayingArtworkBounds(Radius.dock - MiniArtInset).fillMaxSize(),
                    // Square here; the Box around it rounds it, so the image that
                    // travels in the morph matches the full player's.
                    Dp(0f),
                    coverUrl = cover,
                    useCoverUrl = true,
                )
            }
            Spacer(Modifier.width(Space.md))
            Column(Modifier.weight(1f)) {
                // Title and artist slide up together when the track changes.
                AnimatedContent(
                    targetState = track,
                    contentKey = { it.videoId to it.title },
                    transitionSpec = {
                        (slideInVertically(motion.base()) { it / 2 } + fadeIn(motion.base()))
                            .togetherWith(slideOutVertically(motion.fast()) { -it / 2 } + fadeOut(motion.fast()))
                    },
                    label = "miniTrack",
                ) { shown ->
                    Column {
                        Text(shown.title, style = AppType.title, color = colors.text1, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(shown.artist, style = AppType.bodyS, color = colors.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(Space.xs))
                MiniPlayerProgress(progressState, playing = state.isPlaying)
            }
            Spacer(Modifier.width(Space.md))
            MiniPlayPause(
                playing = state.isPlaying,
                buffering = state.isBuffering,
                onClick = {
                    haptics.perform(HapticKind.Tick)
                    onToggle()
                },
            )
            AppIconButton(
                icon = Icons.Filled.SkipNext,
                contentDescription = stringResource(R.string.cmp_next),
                onClick = onNext,
                enabled = state.queueSize > 1,
                tint = colors.text1,
            )
        }
    }
    }
}

private val MiniArtInset = 10.dp
private val MiniArtSize = 52.dp
private val MiniArtShape = RoundedCornerShape(Radius.dock - MiniArtInset)
private val MiniPlaySize = 44.dp

/**
 * Play/pause: a light disc with a dark glyph, a circle while paused and a
 * rounded square while playing (Tuned's morph), 44dp, 14dp corners when square.
 */
@Composable
private fun MiniPlayPause(playing: Boolean, buffering: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.tunedColors
    val motion = rememberMotion()
    val playPauseLabel = stringResource(R.string.player_play_pause_cd)
    val corner by animateDpAsState(
        targetValue = if (playing) 14.dp else MiniPlaySize / 2,
        animationSpec = motion.spring(),
        label = "miniPlayCorner",
    )
    // The primary text colour as the fill, with a dark glyph: crisp, and still
    // tinted by the accent through the theme's text colour.
    val container = lerp(colors.text1, colors.accent, 0.12f)
    val glyph = colors.bg
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(MiniPlaySize)
            .pressScale(interaction)
            .clip(RoundedCornerShape(corner))
            .background(container)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = playPauseLabel
            },
        contentAlignment = Alignment.Center,
    ) {
        if (buffering) {
            com.tuned.app.ui.common.LoadingSpinner(
                modifier = Modifier.size(Sizes.iconRow),
                color = glyph,
                strokeWidth = Sizes.progressLine,
            )
        } else {
            CompositionLocalProvider(LocalContentColor provides glyph) {
                AnimatedPlayPauseIcon(playing, Modifier.size(Sizes.icon))
            }
        }
    }
}

/**
 * Wavy progress: the played part is a wave that moves while music plays and
 * flattens when paused; the rest is a straight track ending in a stop dot.
 */
@Composable
internal fun MiniPlayerProgress(
    progressState: StateFlow<PlaybackProgressState>,
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val state by progressState.collectAsStateWithLifecycle()
    val progress = if (state.durationMs > 0) (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f
    val colors = MaterialTheme.tunedColors
    val motion = rememberMotion()
    val amplitude by animateFloatAsState(if (playing && !motion.reduced) 1f else 0f, motion.base(), label = "waveAmp")
    // The phase transition only exists while playing: it used to tick for the
    // whole pause even though the amplitude had already flattened the wave.
    val phase = if (!playing || motion.reduced) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "wave")
        val p by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(WAVE_PERIOD_MS, easing = LinearEasing)),
            label = "wavePhase",
        )
        p
    }
    val active = colors.accent
    val track = colors.text3.copy(alpha = 0.55f)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(Space.sm),
    ) {
        // 2dp: a finer line than the full player's, so the bar stays calm.
        val stroke = Sizes.progressLine.toPx()
        val mid = size.height / 2f
        val gap = Space.xs.toPx()
        val wavelength = Space.xl.toPx() * 0.8f
        val amp = Space.xxs.toPx() * amplitude
        val playedEnd = size.width * progress
        if (playedEnd > stroke) {
            val path = Path()
            val shift = phase * wavelength
            var x = 0f
            path.moveTo(0f, mid + amp * sin(((x + shift) / wavelength) * 2f * PI.toFloat()))
            while (x < playedEnd) {
                x = (x + 2f).coerceAtMost(playedEnd)
                path.lineTo(x, mid + amp * sin(((x + shift) / wavelength) * 2f * PI.toFloat()))
            }
            drawPath(path, active, style = Stroke(width = stroke, cap = StrokeCap.Round))
        }
        val restStart = (playedEnd + gap).coerceAtMost(size.width)
        val dotRadius = stroke / 2f
        if (restStart < size.width - dotRadius * 2) {
            drawLine(track, Offset(restStart, mid), Offset(size.width - dotRadius * 2 - gap, mid), stroke, StrokeCap.Round)
        }
        drawCircle(active, dotRadius, Offset(size.width - dotRadius, mid))
    }
}

private const val WAVE_PERIOD_MS = 1400

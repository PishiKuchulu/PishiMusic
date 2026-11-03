package com.tuned.app.ui.player

import com.tuned.app.R
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.offset
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.tuned.app.playback.MusicPlayer
import com.tuned.app.playback.MusicPlayerState
import com.tuned.app.playback.PlayableTrack
import com.tuned.app.playback.PlaybackProgressState
import com.tuned.app.ui.theme.AppShape
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.HapticKind
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.rememberHaptics
import com.tuned.app.ui.theme.rememberMotion
import com.tuned.app.ui.theme.tunedColors
import com.tuned.app.util.ArtistHelper
import kotlinx.coroutines.flow.StateFlow

private val ScrubThickness = 10.dp
private val ScrubThicknessActive = 16.dp

/** Wavy style: the handle's height at rest and while held. */
private val WaveHandle = Space.lg + Space.xs
private val WaveHandleActive = Space.xl

/** One full wave; the wave scrolls by this much per [WavePeriodMs]. */
private val Wavelength = Space.xl + Space.xs
private const val WavePeriodMs = 1600

/** How far the bar stretches when pulled past either end. */
private val ScrubMaxPull = 24.dp

/**
 * The cover's own colour for the play control, darkened just
 * enough that a white glyph on it stays readable (3:1 or better).
 */
internal fun coverTone(cover: Color): Color {
    var c = cover.copy(alpha = 1f)
    var step = 0
    while (contrast(Color.White, c) < 3f && step < 20) {
        c = lerp(c, Color.Black, 0.08f)
        step++
    }
    return c
}

private fun contrast(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return if (la > lb) la / lb else lb / la
}

/**
 * The player's seek bar. At rest a 10dp capsule, played part white, with a
 * 12dp white dot at the play position. Under the finger it swells to 16dp and widens a little, and a time bubble
 * rides above the finger. Pulling past either end stretches the bar like a
 * rubber band (with a tick as you hit the end) and it springs back on release.
 * Tap anywhere to jump. Times use the app font with fixed-width digits; the
 * quality chip sits between them.
 */
@Composable
internal fun ScrubBar(
    progressState: StateFlow<PlaybackProgressState>,
    fallbackDurationMs: Long,
    playedColor: Color,
    qualityText: String,
    onQualityClick: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    qualityHighlight: Boolean = false,
    wavy: Boolean = false,
    playing: Boolean = false,
    /** Times and the format chip under the bar; off where the screen shows its own times (lyrics). */
    showLabels: Boolean = true,
) {
    val progress by progressState.collectAsStateWithLifecycle()
    val durationMs = if (progress.durationMs > 0L) progress.durationMs else fallbackDurationMs.coerceAtLeast(0L)
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    var pull by remember { mutableFloatStateOf(0f) }
    val pullBack = remember { Animatable(0f) }
    var lastMinute by remember { mutableIntStateOf(-1) }
    var atEdge by remember { mutableStateOf(false) }
    val active = dragFraction != null
    val fraction = dragFraction ?: if (durationMs > 0L) (progress.positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val shownMs = (fraction * durationMs).toLong()
    val motion = rememberMotion()
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val swell = if (motion.reduced) snap<Dp>() else spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium)
    val thickness by animateDpAsState(if (active) ScrubThicknessActive else ScrubThickness, swell, label = "scrubThickness")
    val widen by animateFloatAsState(
        targetValue = if (active) 1.03f else 1f,
        animationSpec = if (motion.reduced) snap() else spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium),
        label = "scrubWiden",
    )
    val maxPull = with(LocalDensity.current) { ScrubMaxPull.toPx() }
    // Rubber band: the further you pull, the less it gives.
    val shownPull = if (active) maxPull * kotlin.math.tanh(pull / maxPull) else pullBack.value
    val track = Color.White.copy(alpha = 0.30f)
    // Wavy style: the wave moves while playing and lies flat when paused or
    // held, so the exact position is easy to read while scrubbing.
    val waveAmount by animateFloatAsState(
        targetValue = if (wavy && playing && !active && !motion.reduced) 1f else 0f,
        animationSpec = if (motion.reduced) snap() else spring(dampingRatio = 1f, stiffness = Spring.StiffnessLow),
        label = "waveAmount",
    )
    val handle by animateDpAsState(if (active) WaveHandleActive else WaveHandle, swell, label = "waveHandle")
    // The phase transition only exists while the wave is actually moving; it
    // used to tick (and redraw the bar) for the whole pause and whenever the
    // plain capsule style was showing.
    val waveAnimating = wavy && playing && !motion.reduced
    var wavePhaseValue = 0f
    if (waveAnimating) {
        val animatedPhase = rememberInfiniteTransition(label = "wave").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(WavePeriodMs, easing = LinearEasing)),
            label = "wavePhase",
        )
        wavePhaseValue = animatedPhase.value
    }
    val labelColor = Color.White.copy(alpha = 0.72f)

    // The bar is laid out in a 24dp box so the title and the times sit close
    // to it; taps and drags are taken on the bar and the times row together,
    // which keeps the touch area about 48dp tall.
    Column(
        modifier
            .fillMaxWidth()
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    if (durationMs > 0L) onSeek(((offset.x / size.width).coerceIn(0f, 1f) * durationMs).toLong())
                }
            }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        scope.launch { pullBack.snapTo(0f) }
                        dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        pull = 0f
                        lastMinute = -1
                        atEdge = false
                    },
                    onDragEnd = {
                        dragFraction?.let { if (durationMs > 0L) onSeek((it * durationMs).toLong()) }
                        val from = maxPull * kotlin.math.tanh(pull / maxPull)
                        dragFraction = null
                        pull = 0f
                        scope.launch {
                            pullBack.snapTo(from)
                            pullBack.animateTo(0f, spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow))
                        }
                    },
                    onDragCancel = {
                        dragFraction = null
                        pull = 0f
                    },
                ) { change, _ ->
                    change.consume()
                    val x = change.position.x
                    val w = size.width.toFloat()
                    dragFraction = (x / w).coerceIn(0f, 1f)
                    pull = when {
                        x < 0f -> x
                        x > w -> x - w
                        else -> 0f
                    }
                    val edge = x <= 0f || x >= w
                    if (edge && !atEdge) haptics.perform(HapticKind.Confirm)
                    atEdge = edge
                    val minute = (((dragFraction ?: 0f) * durationMs) / 60_000L).toInt()
                    if (!edge && lastMinute >= 0 && minute != lastMinute) haptics.perform(HapticKind.Tick)
                    lastMinute = minute
                }
            }
    ) {
        val seekLabel = stringResource(R.string.player_seek_cd)
        val positionOf = stringResource(R.string.player_position_of)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val widthPx = constraints.maxWidth.toFloat()
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(Space.xl)
                    .semantics {
                        contentDescription = seekLabel
                        stateDescription = positionOf.format(formatPlayTime(shownMs), formatPlayTime(durationMs))
                    }
                    .graphicsLayer {
                        // Widen from the centre while held; stretch from the far end when pulled.
                        val stretch = 1f + kotlin.math.abs(shownPull) / size.width.coerceAtLeast(1f)
                        scaleX = widen * stretch
                        transformOrigin = when {
                            shownPull > 0f -> TransformOrigin(0f, 0.5f)
                            shownPull < 0f -> TransformOrigin(1f, 0.5f)
                            else -> TransformOrigin(0.5f, 0.5f)
                        }
                    }
                    .drawBehind {
                        if (wavy) {
                            drawWavyBar(fraction, track, playedColor, waveAmount, wavePhaseValue, handle)
                            return@drawBehind
                        }
                        val h = thickness.toPx()
                        val top = size.height / 2f - h / 2f
                        val radius = CornerRadius(h / 2f)
                        drawRoundRect(track, topLeft = Offset(0f, top), size = Size(size.width, h), cornerRadius = radius)
                        val played = size.width * fraction
                        if (played > 0f) {
                            // The played part is its own capsule, so its end is round, not cut.
                            drawRoundRect(playedColor, topLeft = Offset(0f, top), size = Size(played.coerceAtLeast(h), h), cornerRadius = radius)
                        }
                        // Play position dot, 2dp wider than the bar so it reads at rest and while held.
                        val dotRadius = (thickness + Space.xxs).toPx() / 2f
                        drawCircle(
                            Color.White,
                            radius = dotRadius,
                            center = Offset(played.coerceIn(dotRadius, size.width - dotRadius), size.height / 2f),
                        )
                    },
            )
            // Time bubble above the finger while scrubbing.
            if (active) {
                val bubbleX = (fraction * widthPx)
                Text(
                    formatPlayTime(shownMs),
                    style = AppType.label,
                    color = Color.Black,
                    modifier = Modifier
                        .layout { measurable, constraints ->
                            val p = measurable.measure(constraints.copy(minWidth = 0))
                            layout(p.width, p.height) {
                                val x = (bubbleX - p.width / 2f).coerceIn(0f, widthPx - p.width).toInt()
                                p.place(x, -p.height - Space.xs.roundToPx())
                            }
                        }
                        .clip(AppShape.full)
                        .background(Color.White)
                        .padding(horizontal = Space.sm, vertical = Space.xxs),
                )
            }
        }
        if (showLabels) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(formatPlayTime(shownMs), style = AppType.caption, color = if (active) Color.White else labelColor)
                Spacer(Modifier.weight(1f))
                // Drawn 8dp lower than the times, so it sits midway between the
                // bar and the play controls; the times keep their place.
                if (qualityText.isNotEmpty()) {
                    Box(Modifier.offset(y = Space.sm)) {
                        QualityChip(text = qualityText, highlight = qualityHighlight, onClick = onQualityClick)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("-" + formatPlayTime((durationMs - shownMs).coerceAtLeast(0L)), style = AppType.caption, color = labelColor)
            }
        }
    }
}

/** The audio format as a small chip ("OPUS", "Lossless"); tap for Signal path. */
@Composable
private fun QualityChip(text: String, highlight: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.tunedColors.accent
    Text(
        text,
        style = AppType.caption,
        color = if (highlight) Color.White else Color.White.copy(alpha = 0.80f),
        maxLines = 1,
        modifier = Modifier
            .clip(AppShape.full)
            .background(if (highlight) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = Space.sm, vertical = Space.xxs),
    )
}

/**
 * The wavy style: the played part is a 4dp wave up to a slim upright handle,
 * the rest a straight line with a stop dot at the end.
 */
private fun DrawScope.drawWavyBar(
    fraction: Float,
    track: Color,
    played: Color,
    amount: Float,
    phase: Float,
    handleHeight: Dp,
) {
    val stroke = Space.xs.toPx()
    val mid = size.height / 2f
    val gap = Space.xs.toPx()
    val handleWidth = Space.xs.toPx()
    val x = (size.width * fraction).coerceIn(handleWidth / 2f, size.width - handleWidth / 2f)
    val wavelength = Wavelength.toPx()
    val amp = Space.xxs.toPx() * 1.5f * amount
    val playedEnd = x - handleWidth / 2f - gap
    if (playedEnd > stroke / 2f) {
        val path = Path()
        val shift = phase * wavelength
        // Start the wave at zero height so the left end sits on the line.
        fun yAt(px: Float): Float {
            val ease = (px / wavelength).coerceAtMost(1f)
            return mid + amp * ease * sin(((px - shift) / wavelength) * 2f * PI.toFloat())
        }
        var px = stroke / 2f
        path.moveTo(px, yAt(px))
        while (px < playedEnd) {
            px = (px + 2f).coerceAtMost(playedEnd)
            path.lineTo(px, yAt(px))
        }
        drawPath(path, played, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
    val restStart = x + handleWidth / 2f + gap + stroke / 2f
    val restEnd = size.width - stroke / 2f
    if (restStart < restEnd - stroke) {
        drawLine(track, Offset(restStart, mid), Offset(restEnd - stroke - gap, mid), stroke, StrokeCap.Round)
        drawCircle(played, stroke / 2f, Offset(restEnd, mid))
    }
    val h = handleHeight.toPx()
    drawRoundRect(
        played,
        topLeft = Offset(x - handleWidth / 2f, mid - h / 2f),
        size = Size(handleWidth, h),
        cornerRadius = CornerRadius(handleWidth / 2f),
    )
}

/** "3:07", or "1:02:45" past an hour. */
internal fun formatPlayTime(ms: Long): String {
    val total = (ms / 1000L).coerceAtLeast(0L)
    val h = total / 3600L
    val m = (total % 3600L) / 60L
    val s = total % 60L
    return if (h > 0L) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}


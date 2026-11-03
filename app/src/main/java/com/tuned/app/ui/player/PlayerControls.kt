@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.tuned.app.ui.player

import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.tuned.app.playback.MusicPlayer
import com.tuned.app.playback.MusicPlayerState
import com.tuned.app.playback.isSpatialAudioCodec
import com.tuned.app.playback.qualityBadgeLabel
import com.tuned.app.playback.SignalPathReport
import com.tuned.app.playback.PlaybackProgressState
import com.tuned.app.ui.common.ExpressiveInlineLoadingIndicator
import com.tuned.app.ui.common.ExpressiveMotion
import com.tuned.app.ui.theme.LocalLiquidGlass
import com.tuned.app.ui.theme.LiquidGlassSurface
import com.tuned.app.ui.theme.liquidGlassChrome
import com.tuned.app.ui.theme.liquidGlassContainerColor
import com.tuned.app.ui.theme.isLiquidGlassBackdropSupported
import com.tuned.app.ui.theme.LocalLiquidGlassBackdrop
import com.tuned.app.ui.theme.LiquidGlassPreset
import androidx.core.graphics.scale

@Composable
internal fun PlayerProgressSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    tertiaryColor: Color = MaterialTheme.colorScheme.tertiary,
) {
    val primary = primaryColor
    val tertiary = tertiaryColor
    val inactive = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.20f else 0.12f)
    val range = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val fraction = ((value - valueRange.start) / range).coerceIn(0f, 1f)

    // Playback progress is time, not directional content: pin to LTR so the
    // Material Slider's touch mapping always matches the LTR custom drawing,
    // even in RTL locales (Arabic, …) where Slider would otherwise mirror.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier.drawBehind {
            val inset = 10.dp.toPx()
            val startX = inset
            val endX = (size.width - inset).coerceAtLeast(startX)
            val activeEndX = startX + ((endX - startX) * fraction)
            val centerY = size.height / 2f
            drawLine(
                color = inactive,
                start = androidx.compose.ui.geometry.Offset(startX, centerY),
                end = androidx.compose.ui.geometry.Offset(endX, centerY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
            if (activeEndX > startX) {
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            primary.copy(alpha = if (enabled) 1f else 0.42f),
                            tertiary.copy(alpha = if (enabled) 0.92f else 0.36f),
                        ),
                        startX = startX,
                        endX = activeEndX,
                    ),
                    start = androidx.compose.ui.geometry.Offset(startX, centerY),
                    end = androidx.compose.ui.geometry.Offset(activeEndX, centerY),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        },
        colors = SliderDefaults.colors(
            thumbColor = primary,
            activeTrackColor = Color.Transparent,
            inactiveTrackColor = Color.Transparent,
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent,
            disabledThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.34f),
            disabledActiveTrackColor = Color.Transparent,
            disabledInactiveTrackColor = Color.Transparent,
            disabledActiveTickColor = Color.Transparent,
            disabledInactiveTickColor = Color.Transparent,
        ),
    )
    }
}

@Composable
internal fun SeekBar(
    progressState: StateFlow<PlaybackProgressState>,
    isPlaying: Boolean,
    trackKey: String?,
    wavyEnabled: Boolean = true,
    onSeek: (Long) -> Unit,
    isTranslucent: Boolean = false,
    fallbackDurationMs: Long = 0L,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary,
    tertiaryColor: Color = MaterialTheme.colorScheme.tertiary,
) {
    val progress by progressState.collectAsStateWithLifecycle()
    val effectiveDurationMs = if (progress.durationMs > 0L) progress.durationMs else fallbackDurationMs.coerceAtLeast(0L)

    if (wavyEnabled) {
        WavySeekBar(
            positionMs = progress.positionMs,
            durationMs = effectiveDurationMs,
            isPlaying = isPlaying,
            onSeek = onSeek,
            isTranslucent = isTranslucent,
            trackKey = trackKey,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            tertiaryColor = tertiaryColor,
        )
        return
    }

    // Current-gesture fraction only; null = finger off, show live position.
    // Nullable (never a stale 0f) so a press without movement seeks nowhere
    // and a gesture that ends without onValueChangeFinished can't pin the bar.
    val seekInteraction = remember(trackKey) { MutableInteractionSource() }
    var isInteracting by remember(trackKey) { mutableStateOf(false) }
    LaunchedEffect(seekInteraction, trackKey) {
        var dragCount = 0
        var pressCount = 0
        seekInteraction.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> dragCount++
                is DragInteraction.Stop, is DragInteraction.Cancel -> dragCount = maxOf(0, dragCount - 1)
                is PressInteraction.Press -> pressCount++
                is PressInteraction.Release, is PressInteraction.Cancel -> pressCount = maxOf(0, pressCount - 1)
            }
            isInteracting = dragCount > 0 || pressCount > 0
        }
    }
    var dragFraction by remember(trackKey) { mutableStateOf<Float?>(null) }
    var lastSeekFraction by remember(trackKey) { mutableStateOf<Float?>(null) }
    // Heal: framework reports finger lifted or cancelled but the finished callback
    // never ran -> drop the dead value and resume live position.
    LaunchedEffect(isInteracting, trackKey) {
        if (!isInteracting) {
            delay(120L)
            dragFraction = null
            lastSeekFraction = null
        }
    }
    // Watchdog: if dragFraction was set by a tap/cancelled gesture while not interacting,
    // ensure it never permanently wedges the seekbar.
    LaunchedEffect(dragFraction, isInteracting, trackKey) {
        if (dragFraction != null && !isInteracting) {
            delay(250L)
            dragFraction = null
            lastSeekFraction = null
        }
    }

    val boundedDurationMs = effectiveDurationMs.coerceAtLeast(0L)
    val currentFraction = if (boundedDurationMs > 0L) {
        (progress.positionMs.toDouble() / boundedDurationMs.toDouble()).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val fraction = (dragFraction ?: currentFraction).coerceIn(0f, 1f)
    val shownMs = dragFraction?.let {
        (it * boundedDurationMs).toLong().coerceIn(0L, boundedDurationMs)
    } ?: progress.positionMs.coerceIn(0L, boundedDurationMs)

    val isGlassActive = LocalLiquidGlass.current &&
        isLiquidGlassBackdropSupported() &&
        LocalLiquidGlassBackdrop.current != null
    val effectivePrimary = when {
        isGlassActive -> Color.White.copy(alpha = 0.95f)
        isTranslucent -> Color.White
        else -> primaryColor
    }
    val inactiveColor = when {
        isGlassActive -> Color.White.copy(alpha = 0.16f)
        isTranslucent -> Color.White.copy(alpha = 0.20f)
        else -> effectivePrimary.copy(alpha = 0.28f)
    }
    val textColor = when {
        isGlassActive -> Color.White.copy(alpha = 0.90f)
        isTranslucent -> Color.White.copy(alpha = 0.85f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f)
    }

    // Same RTL pin as above: custom Canvas draws LTR, invisible Slider must match it.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(44.dp)) {
                val width = size.width
                val height = size.height
                val centerY = height / 2f
                val trackHeightPx = 14.dp.toPx()
                val cornerRadius = CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f)
                val thumbWidthPx = 5.dp.toPx()
                val thumbHeightPx = 38.dp.toPx()
                val thumbClearancePx = 9.dp.toPx()
                val rawThumbCenterX = fraction * width
                val thumbCenterX = if (width > thumbWidthPx) {
                    rawThumbCenterX.coerceIn(thumbWidthPx / 2f, width - thumbWidthPx / 2f)
                } else {
                    width / 2f
                }
                val activeEndX = (thumbCenterX - thumbClearancePx).coerceIn(0f, width)
                val inactiveStartX = (thumbCenterX + thumbClearancePx).coerceIn(0f, width)

                // Thick active capsule ending before the vertical thumb.
                if (activeEndX > 0f) {
                    drawRoundRect(
                        color = effectivePrimary,
                        topLeft = Offset(0f, centerY - trackHeightPx / 2f),
                        size = Size(activeEndX, trackHeightPx),
                        cornerRadius = cornerRadius,
                    )
                }

                // Thick inactive capsule starting after the vertical thumb.
                if (inactiveStartX < width) {
                    drawRoundRect(
                        color = inactiveColor,
                        topLeft = Offset(inactiveStartX, centerY - trackHeightPx / 2f),
                        size = Size(width - inactiveStartX, trackHeightPx),
                        cornerRadius = cornerRadius,
                    )
                }

                // Small endpoint marker from the reference design.
                val endpointX = width - trackHeightPx / 2f
                if (inactiveStartX < endpointX) {
                    drawCircle(
                        color = effectivePrimary.copy(alpha = 0.86f),
                        radius = 2.dp.toPx(),
                        center = Offset(endpointX, centerY),
                    )
                }

                // Tall vertical pill thumb with clear space on both sides.
                val thumbX = thumbCenterX - thumbWidthPx / 2f
                val thumbCornerRadius = CornerRadius(thumbWidthPx / 2f, thumbWidthPx / 2f)

                drawRoundRect(
                    color = effectivePrimary,
                    topLeft = Offset(thumbX, centerY - thumbHeightPx / 2f),
                    size = Size(thumbWidthPx, thumbHeightPx),
                    cornerRadius = thumbCornerRadius,
                )
            }

            // Invisible Material interaction layer: custom visuals, reliable seeking semantics.
            Slider(
                value = fraction,
                onValueChange = {
                    dragFraction = it
                    lastSeekFraction = it
                },
                onValueChangeFinished = {
                    // Commit only this gesture's value; no value = no seek.
                    val target = (lastSeekFraction ?: dragFraction)?.let {
                        (it * boundedDurationMs).toLong().coerceIn(0L, boundedDurationMs)
                    }
                    dragFraction = null
                    lastSeekFraction = null
                    if (target != null && boundedDurationMs > 0L) {
                        onSeek(target)
                    }
                },
                valueRange = 0f..1f,
                enabled = boundedDurationMs > 0L,
                interactionSource = seekInteraction,
                modifier = Modifier.fillMaxSize(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                    disabledThumbColor = Color.Transparent,
                    disabledActiveTrackColor = Color.Transparent,
                    disabledInactiveTrackColor = Color.Transparent,
                    disabledActiveTickColor = Color.Transparent,
                    disabledInactiveTickColor = Color.Transparent,
                ),
            )
        }

        Spacer(Modifier.height(2.dp))

        // Time labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatTime(shownMs),
                style = if (isTranslucent) MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium) else MaterialTheme.typography.labelMedium,
                color = textColor,
            )
            Text(
                text = "−${formatTime((boundedDurationMs - shownMs).coerceAtLeast(0))}",
                style = if (isTranslucent) MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium) else MaterialTheme.typography.labelMedium,
                color = textColor,
            )
        }
    }
    }
}

@Composable
internal fun MainControls(
    state: MusicPlayerState,
    player: MusicPlayer,
    isTranslucent: Boolean = false,
    /** When set, the play pill is filled with this colour (the cover's) and its content is white. */
    playColor: Color? = null,
) {
    val prevInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPrevPressed by prevInteraction.collectIsPressedAsState()
    val prevScale by animateFloatAsState(if (isPrevPressed) 0.85f else 1.0f, ExpressiveMotion.spatialSpring(), label = "prevScale")

    val playInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPlayPressed by playInteraction.collectIsPressedAsState()
    val playScale by animateFloatAsState(
        targetValue = if (isPlayPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "playScale",
    )

    val nextInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isNextPressed by nextInteraction.collectIsPressedAsState()
    val nextScale by animateFloatAsState(if (isNextPressed) 0.85f else 1.0f, ExpressiveMotion.spatialSpring(), label = "nextScale")

    // Adaptive transport row: fixed 58+188+58dp used to overflow narrow
    // screens (320dp class + parent padding), clipping the side buttons so
    // Next looked crushed against the edge. The pill now flexes between a
    // legible floor and its design width; prev/next never clip on any
    // screen size or Android version.
    val sideSize = if (isTranslucent) 54.dp else 58.dp
    val gapSize = if (isTranslucent) 18.dp else 16.dp
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val pillWidth = (maxWidth - sideSize * 2 - gapSize * 2)
            .coerceIn(112.dp, if (isTranslucent) 180.dp else 188.dp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(gapSize, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        LiquidGlassSurface(
            glassModifier = Modifier.liquidGlassChrome(CircleShape, LocalLiquidGlass.current, LiquidGlassPreset.PlayerControls, interactionSource = prevInteraction),
            onClick = player::previous,
            interactionSource = prevInteraction,
            shape = CircleShape,
            color = liquidGlassContainerColor(if (isTranslucent) Color.White.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.40f)),
            contentColor = if (isTranslucent) Color.White.copy(alpha = 0.94f) else MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .size(sideSize)
                .graphicsLayer {
                    scaleX = prevScale
                    scaleY = prevScale
                },
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.SkipPrevious, androidx.compose.ui.res.stringResource(com.tuned.app.R.string.desc_previous), Modifier.size(if (isTranslucent) 28.dp else 31.dp))
            }
        }
        val pillShape = RoundedCornerShape(100)
        // Single source of truth for "glass is actually refracting": the
        // container helper returns Transparent only when setting + device
        // capability + backdrop all hold. The old contentColor checked only
        // the *setting*, so setting-ON with an inactive effect (incapable
        // device, missing backdrop) rendered a white icon on the solid-white
        // fallback below — the "whole white" play button. Gating content on
        // the same active flag closes that mismatch for every path.
        val isPillGlassActive = LocalLiquidGlass.current &&
            isLiquidGlassBackdropSupported() &&
            LocalLiquidGlassBackdrop.current != null
        LiquidGlassSurface(
            glassModifier = Modifier.liquidGlassChrome(pillShape, LocalLiquidGlass.current, LiquidGlassPreset.FloatingControls, interactionSource = playInteraction),
            onClick = player::togglePlayPause,
            interactionSource = playInteraction,
            shape = pillShape,
            // Fallback matches the sibling prev/next buttons (14% white veil,
            // never solid white): on the full player's dark scrim the white
            // icon stays readable with or without refraction.
            color = if (playColor != null && !isPillGlassActive) {
                playColor
            } else {
                liquidGlassContainerColor(if (isTranslucent) Color.White.copy(alpha = 0.14f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
            },
            contentColor = if (playColor != null && !isPillGlassActive) {
                Color.White
            } else if (isPillGlassActive) {
                if (isTranslucent) Color.White else MaterialTheme.colorScheme.primary
            } else if (isTranslucent) Color.White.copy(alpha = 0.94f) else MaterialTheme.colorScheme.onPrimary,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .width(pillWidth)
                .height(if (isTranslucent) 56.dp else 60.dp)
                .graphicsLayer {
                    scaleX = playScale
                    scaleY = playScale
                },
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (state.isBuffering) {
                    ExpressiveInlineLoadingIndicator(
                        size = if (isTranslucent) 28.dp else 30.dp,
                        color = androidx.compose.material3.LocalContentColor.current,
                        strokeWidth = 3.dp,
                    )
                } else {
                    AnimatedPlayPauseIcon(state.isPlaying, Modifier.size(if (isTranslucent) 28.dp else 30.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (state.isPlaying) {
                            androidx.compose.ui.res.stringResource(com.tuned.app.R.string.cmp_pause)
                        } else {
                            androidx.compose.ui.res.stringResource(com.tuned.app.R.string.cmp_play)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.material3.LocalContentColor.current
                    )
                }
            }
        }
        LiquidGlassSurface(
            glassModifier = Modifier.liquidGlassChrome(CircleShape, LocalLiquidGlass.current, LiquidGlassPreset.PlayerControls, interactionSource = nextInteraction),
            onClick = player::next,
            interactionSource = nextInteraction,
            shape = CircleShape,
            color = liquidGlassContainerColor(if (isTranslucent) Color.White.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.40f)),
            contentColor = if (isTranslucent) Color.White.copy(alpha = 0.94f) else MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .size(sideSize)
                .graphicsLayer {
                    scaleX = nextScale
                    scaleY = nextScale
                },
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.SkipNext, androidx.compose.ui.res.stringResource(com.tuned.app.R.string.cmp_next), Modifier.size(if (isTranslucent) 28.dp else 31.dp))
            }
        }
        }
    }
}

@Composable
internal fun PlayerModeButton(
    active: Boolean,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    background: Color,
    foreground: Color,
    iconSize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.92f else 1f, ExpressiveMotion.spatialSpring(), label = "modePress",
    )
    val container by animateColorAsState(
        if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.86f)
        else liquidGlassContainerColor(background), label = "modeContainer",
    )
    val content by animateColorAsState(
        if (active) MaterialTheme.colorScheme.onPrimaryContainer else foreground, label = "modeContent",
    )
    LiquidGlassSurface(
glassModifier = Modifier.liquidGlassChrome(CircleShape, LocalLiquidGlass.current, LiquidGlassPreset.PlayerControls, interactionSource = interaction),
        onClick = onClick,
        shape = CircleShape,
        color = container,
        contentColor = content,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .semantics {
                selected = active
                stateDescription = description
            },
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, description, Modifier.size(iconSize))
            if (active) {
                Box(
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp)
                        .size(4.dp).background(content, CircleShape),
                )
            }
        }
    }
}

@Composable
internal fun PlayerUtilityControls(
    state: MusicPlayerState,
    player: MusicPlayer,
    isTranslucent: Boolean = false,
    onOpenAudioControls: () -> Unit = {},
) {
    val edgeButtonBackground = if (isTranslucent) {
        Color.White.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.40f)
    }
    val edgeButtonContent = if (isTranslucent) {
        Color.White.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.90f)
    }

    val stateOnText = androidx.compose.ui.res.stringResource(com.tuned.app.R.string.common_state_on)
    val stateOffText = androidx.compose.ui.res.stringResource(com.tuned.app.R.string.common_state_off)
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (isTranslucent) 10.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerModeButton(
            active = state.shuffleEnabled,
            description = androidx.compose.ui.res.stringResource(com.tuned.app.R.string.desc_shuffle) +
                " " + if (state.shuffleEnabled) stateOnText else stateOffText,
            icon = Icons.Filled.Shuffle,
            onClick = player::toggleShuffle,
            background = edgeButtonBackground,
            foreground = edgeButtonContent,
            iconSize = if (isTranslucent) 19.dp else 20.dp,
            modifier = Modifier.weight(1f).height(if (isTranslucent) 44.dp else 48.dp),
        )
        val isCustomAudio = state.speed != 1f || state.pitch != 1f
        PlayerModeButton(
            active = isCustomAudio,
            description = if (isCustomAudio) "Audio Controls (Active)" else "Audio & Studio Controls",
            icon = Icons.Filled.VolumeUp,
            onClick = onOpenAudioControls,
            background = edgeButtonBackground,
            foreground = edgeButtonContent,
            iconSize = if (isTranslucent) 19.dp else 20.dp,
            modifier = Modifier.weight(1f).height(if (isTranslucent) 44.dp else 48.dp),
        )
        PlayerModeButton(
            active = state.repeatMode != Player.REPEAT_MODE_OFF,
            description = when (state.repeatMode) {
                Player.REPEAT_MODE_ONE -> androidx.compose.ui.res.stringResource(com.tuned.app.R.string.queue_repeat_one)
                Player.REPEAT_MODE_ALL -> androidx.compose.ui.res.stringResource(com.tuned.app.R.string.queue_repeat_all)
                else -> androidx.compose.ui.res.stringResource(com.tuned.app.R.string.queue_repeat) + " " + stateOffText
            },
            icon = if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
            onClick = player::cycleRepeatMode,
            background = edgeButtonBackground,
            foreground = edgeButtonContent,
            iconSize = if (isTranslucent) 19.dp else 20.dp,
            modifier = Modifier.weight(1f).height(if (isTranslucent) 44.dp else 48.dp),
        )
    }
}

/** The format alone for the player's chip: "OPUS", "AAC", "Lossless"; empty when unknown. */
internal fun qualityFormatLabel(state: MusicPlayerState): String {
    val codec = state.audioCodec.orEmpty()
    return when {
        isSpatialAudioCodec(state.audioCodec) -> "Dolby Atmos"
        state.isLossless && ((state.samplingRateKHz ?: 44.1) > 48.0 || (state.bitDepth ?: 16) > 16) -> "Hi-Res Lossless"
        state.isLossless -> "Lossless"
        codec.contains("OPUS", ignoreCase = true) -> "OPUS"
        codec.contains("AAC", ignoreCase = true) || codec.contains("mp4a", ignoreCase = true) -> "AAC"
        codec.contains("MP3", ignoreCase = true) || codec.contains("mpeg", ignoreCase = true) -> "MP3"
        else -> ""
    }
}

/** Plain quality line for the player: the format name alone ("AAC", "Lossless", "Dolby Atmos"). */
internal fun qualityDisplayText(state: MusicPlayerState): String {
    val isSpatial = isSpatialAudioCodec(state.audioCodec)
    val isHiRes = state.isLossless && ((state.samplingRateKHz ?: 44.1) > 48.0 || (state.bitDepth ?: 16) > 16)
    val title = when {
        isSpatial -> "Dolby Atmos"
        isHiRes -> "Hi-Res Lossless"
        state.isLossless -> "Lossless"
        state.audioCodec?.contains("AAC", ignoreCase = true) == true -> "AAC"
        state.audioCodec?.contains("MP3", ignoreCase = true) == true -> "MP3"
        state.audioCodec?.contains("OPUS", ignoreCase = true) == true -> "Opus"
        else -> "Audio"
    }
    return title
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun AppleMusicQualityBadge(
    state: MusicPlayerState,
    signalPath: SignalPathReport,
    isTranslucent: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val isSpatial = remember(state.audioCodec) { isSpatialAudioCodec(state.audioCodec) }
    val isHiRes = state.isLossless && ((state.samplingRateKHz ?: 44.1) > 48.0 || (state.bitDepth ?: 16) > 16)
    val baseLabel = remember(state.audioCodec, state.bitrateKbps, state.samplingRateKHz, state.bitDepth, state.isLossless) {
        qualityBadgeLabel(state)
    }

    val displayTitle = when {
        isSpatial -> "Dolby Atmos"
        isHiRes -> "Hi-Res Lossless"
        state.isLossless -> "Lossless"
        state.audioCodec?.contains("AAC", ignoreCase = true) == true -> "AAC"
        state.audioCodec?.contains("MP3", ignoreCase = true) == true -> "MP3"
        state.audioCodec?.contains("OPUS", ignoreCase = true) == true -> "Opus"
        else -> "Audio"
    }

    val badgeIcon = when {
        isSpatial -> Icons.Filled.SurroundSound
        state.isLossless -> Icons.Filled.HighQuality
        else -> Icons.Filled.GraphicEq
    }

    val tintColor = when {
        signalPath.bitPerfect -> Color(0xFFE6C15A)
        signalPath.directToHal -> Color(0xFF7FD3C3)
        signalPath.clockFallbackResampled -> Color(0xFFFFB74D)
        isTranslucent -> Color.White.copy(alpha = 0.95f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    val containerBg = if (isTranslucent) {
        Color.White.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.50f)
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = containerBg,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .height(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .combinedClickable(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    },
                ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(5.dp))
                // The label often starts with the codec the title already
                // names ("AAC 131 kbps" under "AAC"); show it once.
                val detail = if (baseLabel.startsWith(displayTitle, ignoreCase = true)) {
                    baseLabel.substring(displayTitle.length).trim()
                } else {
                    baseLabel
                }
                val fullText = if (detail.isNotBlank() && !detail.equals(displayTitle, ignoreCase = true) && !detail.equals("AUDIO", ignoreCase = true)) {
                    displayTitle
                } else {
                    displayTitle
                }
                Text(
                    text = fullText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp,
                    ),
                    color = tintColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = tintColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(10.dp),
                )
            }
        }
    }
}

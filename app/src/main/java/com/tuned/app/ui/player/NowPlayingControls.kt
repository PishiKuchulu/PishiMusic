package com.tuned.app.ui.player

import com.tuned.app.ui.theme.LiquidGlassPreset
import com.tuned.app.ui.theme.LocalLiquidGlass
import com.tuned.app.ui.theme.liquidGlassChrome
import com.tuned.app.ui.theme.liquidGlassContainerColor
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.tuned.app.ui.theme.AppShape
import com.tuned.app.ui.theme.HapticKind
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.rememberHaptics
import com.tuned.app.ui.theme.rememberMotion
import com.tuned.app.ui.theme.tunedColors

/** Height of the transport row; previous and next are circles of this size. */
private val TransportSize = 64.dp

/** Fill of the previous and next circles without Liquid Glass (the player's glass tint under it). */
private val PlayerGlass = Color.White.copy(alpha = 0.14f)

/**
 * The play control's fill: the cover's own colour when it has one, Tuned's
 * accent when the cover is black and white or nearly so, so the main control
 * always stands out. Darkened until a white glyph reads at 3:1.
 */
@Composable
internal fun playFillFor(cover: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(cover.toArgb(), hsv)
    val colourful = hsv[1] >= 0.22f && hsv[2] >= 0.18f
    return coverTone(if (colourful) cover else MaterialTheme.tunedColors.accent)
}

/**
 * Previous, play/pause, next. Previous and next are glass circles; play is a
 * wide pill in [playColor] with only its icon, which swaps with a scale. Each
 * control shrinks a little under the finger and springs back.
 */
@Composable
internal fun NowPlayingTransport(
    isPlaying: Boolean,
    isBuffering: Boolean,
    playColor: Color,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    Row(
        modifier
            .fillMaxWidth()
            .height(TransportSize),
        horizontalArrangement = Arrangement.spacedBy(Space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PressableControl(
            onClick = {
                haptics.perform(HapticKind.Tick)
                onPrevious()
            },
            color = PlayerGlass,
            shape = CircleShape,
            glass = true,
            description = androidx.compose.ui.res.stringResource(com.tuned.app.R.string.desc_previous),
            modifier = Modifier.size(TransportSize),
        ) {
            Icon(Icons.Rounded.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(Space.xxl))
        }
        PressableControl(
            onClick = onToggle,
            color = playColor,
            shape = AppShape.full,
            description = if (isPlaying) {
                androidx.compose.ui.res.stringResource(com.tuned.app.R.string.cmp_pause)
            } else {
                androidx.compose.ui.res.stringResource(com.tuned.app.R.string.cmp_play)
            },
            modifier = Modifier
                .weight(1f)
                .height(TransportSize),
        ) {
            if (isBuffering) {
                com.tuned.app.ui.common.LoadingSpinner(color = Color.White, strokeWidth = Space.xxs + Sizes.hairline, modifier = Modifier.size(Sizes.icon))
            } else {
                AnimatedContent(
                    targetState = isPlaying,
                    transitionSpec = { (scaleIn(initialScale = 0.6f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut()) },
                    label = "playPause",
                ) { playing ->
                    Icon(
                        if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(Space.xxl + Space.sm),
                    )
                }
            }
        }
        PressableControl(
            onClick = {
                haptics.perform(HapticKind.Tick)
                onNext()
            },
            color = PlayerGlass,
            shape = CircleShape,
            glass = true,
            description = androidx.compose.ui.res.stringResource(com.tuned.app.R.string.cmp_next),
            modifier = Modifier.size(TransportSize),
        ) {
            Icon(Icons.Rounded.SkipNext, null, tint = Color.White, modifier = Modifier.size(Space.xxl))
        }
    }
}

@Composable
private fun PressableControl(
    onClick: () -> Unit,
    color: Color,
    shape: androidx.compose.ui.graphics.Shape,
    description: String,
    modifier: Modifier = Modifier,
    /** Liquid Glass (when it is on) under [color], sampling the player's artwork layer. */
    glass: Boolean = false,
    content: @Composable () -> Unit,
) {
    val reduced = rememberMotion().reduced
    val glassChrome = if (glass) {
        Modifier.liquidGlassChrome(shape, LocalLiquidGlass.current, LiquidGlassPreset.PlayerControls)
    } else Modifier
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = if (reduced) snap() else spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium),
        label = "controlPress",
    )
    Box(
        modifier
            // Scale only; the fill is drawn as an anti-aliased shape. Clipping
            // the scaled layer to the shape cut the pill's corners flat on
            // some GPUs mid-animation.
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(glassChrome)
            .background(if (glass) liquidGlassContainerColor(color) else color, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics {
                contentDescription = description
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) { content() }
}

/**
 * Shuffle, lyrics, queue, sound, repeat as bare icons across the width.
 * Shuffle and repeat show a dot in the accent while on.
 */
@Composable
internal fun NowPlayingActionRow(
    shuffleOn: Boolean,
    repeatMode: Int,
    soundChanged: Boolean,
    onShuffle: () -> Unit,
    onLyrics: () -> Unit,
    onQueue: () -> Unit,
    onSound: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        ActionIcon(PlayerActionIcons.Shuffle, androidx.compose.ui.res.stringResource(com.tuned.app.R.string.desc_shuffle), on = shuffleOn, onClick = onShuffle)
        ActionIcon(PlayerActionIcons.Lyrics, androidx.compose.ui.res.stringResource(com.tuned.app.R.string.player_show_lyrics_cd), on = null, onClick = onLyrics)
        ActionIcon(PlayerActionIcons.Queue, androidx.compose.ui.res.stringResource(com.tuned.app.R.string.menu_action_queue), on = null, onClick = onQueue)
        // No existing "Sound / audio controls" string resource; the label stays
        // hardcoded rather than inventing an untranslated key here.
        ActionIcon(PlayerActionIcons.Sound, "Sound", on = if (soundChanged) true else null, onClick = onSound)
        ActionIcon(
            if (repeatMode == Player.REPEAT_MODE_ONE) PlayerActionIcons.RepeatOne else PlayerActionIcons.Repeat,
            when (repeatMode) {
                Player.REPEAT_MODE_ONE -> androidx.compose.ui.res.stringResource(com.tuned.app.R.string.queue_repeat_one)
                Player.REPEAT_MODE_ALL -> androidx.compose.ui.res.stringResource(com.tuned.app.R.string.queue_repeat_all)
                else -> androidx.compose.ui.res.stringResource(com.tuned.app.R.string.desc_repeat)
            },
            on = repeatMode != Player.REPEAT_MODE_OFF,
            onClick = onRepeat,
        )
    }
}

/** [on] is null for plain actions, true or false for toggles. */
@Composable
private fun ActionIcon(icon: ImageVector, label: String, on: Boolean?, onClick: () -> Unit) {
    val accent = MaterialTheme.tunedColors.accent
    val onText = androidx.compose.ui.res.stringResource(com.tuned.app.R.string.common_state_on)
    val offText = androidx.compose.ui.res.stringResource(com.tuned.app.R.string.common_state_off)
    Box(
        Modifier
            .size(Sizes.touch)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = label
                if (on != null) stateDescription = if (on) onText else offText
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (on == false) Color.White.copy(alpha = 0.62f) else Color.White,
            modifier = Modifier.size(Sizes.icon),
        )
        if (on == true) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Space.xxs)
                    .size(Space.xs)
                    .clip(CircleShape)
                    .background(accent),
            )
        }
    }
}

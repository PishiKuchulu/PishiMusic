package com.tuned.app.ui.common

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.tuned.app.R
import com.tuned.app.ui.theme.AppShape
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.tunedColors

enum class AppButtonStyle {
    /** The one main action on a screen. Accent fill. */
    Primary,
    /** Other actions. Hairline border, no fill. */
    Secondary,
    /** Low-emphasis actions inside content. Text only. */
    Ghost,
}

/** 48dp tall text button, optionally with a leading 20dp icon. While [loading] the icon slot shows a spinner and clicks are locked out, so a slow async action cannot be fired twice by repeated taps. */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppButtonStyle = AppButtonStyle.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val colors = MaterialTheme.tunedColors
    val padding = PaddingValues(horizontal = Space.lg + Space.xs)
    val content: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                LoadingSpinner(modifier = Modifier.size(Sizes.iconRow), strokeWidth = Sizes.progressLine)
            } else if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(Sizes.iconRow))
            }
            Text(text, style = AppType.label)
        }
    }
    val effectiveEnabled = enabled && !loading
    val interaction = remember { MutableInteractionSource() }
    val sized = modifier.defaultMinSize(minHeight = Sizes.touch).pressScale(interaction)
    when (style) {
        AppButtonStyle.Primary -> Button(
            onClick = onClick,
            modifier = sized,
            interactionSource = interaction,
            enabled = effectiveEnabled,
            shape = AppShape.sm,
            contentPadding = padding,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.onAccent,
                disabledContainerColor = colors.surface2,
                disabledContentColor = colors.text3,
            ),
        ) { content() }
        AppButtonStyle.Secondary -> OutlinedButton(
            onClick = onClick,
            modifier = sized,
            interactionSource = interaction,
            enabled = effectiveEnabled,
            shape = AppShape.sm,
            contentPadding = padding,
            border = BorderStroke(Sizes.hairline, colors.line),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.text1, disabledContentColor = colors.text3),
        ) { content() }
        AppButtonStyle.Ghost -> TextButton(
            onClick = onClick,
            modifier = sized,
            interactionSource = interaction,
            enabled = effectiveEnabled,
            shape = AppShape.sm,
            contentPadding = PaddingValues(horizontal = Space.md),
            colors = ButtonDefaults.textButtonColors(contentColor = colors.accent, disabledContentColor = colors.text3),
        ) { content() }
    }
}

/**
 * Icon-only button with a 48dp touch target. [contentDescription] is
 * required: an icon button without one is invisible to a screen reader.
 */
@Composable
fun AppIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = Sizes.icon,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.tunedColors.text1,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(Sizes.touch),
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(contentColor = tint, disabledContentColor = MaterialTheme.tunedColors.text3),
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(iconSize))
    }
}

/**
 * Round play/pause button in the accent colour, the one place the full
 * radius is used for an action. [size] is 56dp in headers and 72dp in the player.
 */
@Composable
fun PlayButton(
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = Sizes.playButton,
    isBuffering: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.tunedColors
    val interaction = remember { MutableInteractionSource() }
    IconButton(
        onClick = onClick,
        modifier = modifier.size(size).pressScale(interaction),
        interactionSource = interaction,
        enabled = enabled,
        shape = AppShape.full,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = colors.accent,
            contentColor = colors.onAccent,
            disabledContainerColor = colors.surface2,
            disabledContentColor = colors.text3,
        ),
    ) {
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
            Crossfade(targetState = playing, label = "playPause") { isPlaying ->
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(if (isPlaying) R.string.cmp_pause else R.string.cmp_play),
                    modifier = Modifier.size(size / 2),
                )
            }
            if (isBuffering) {
                LoadingSpinner(
                    modifier = Modifier.size(size * 0.78f),
                    color = colors.onAccent,
                    strokeWidth = Sizes.progressLine,
                )
            }
        }
    }
}

@ComponentPreviews
@Composable
private fun ButtonsPreview() = PreviewTheme { ButtonsSample() }

/** Sample states, shared by the preview and the in-app component gallery. */
@Composable
internal fun ButtonsSample() {
    Column(Modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.md)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            AppButton("Play", onClick = {}, icon = Icons.Filled.PlayArrow)
            AppButton("Shuffle", onClick = {}, style = AppButtonStyle.Secondary, icon = Icons.Filled.Shuffle)
            AppButton("See all", onClick = {}, style = AppButtonStyle.Ghost)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            AppIconButton(Icons.Filled.Download, "Download", onClick = {})
            AppIconButton(Icons.Filled.MoreVert, "More options", onClick = {})
            Spacer(Modifier.width(Space.sm))
            PlayButton(playing = false, onClick = {})
            PlayButton(playing = true, onClick = {}, size = Sizes.playButtonPlayer)
        }
        AppButton("Disabled", onClick = {}, enabled = false)
    }
}

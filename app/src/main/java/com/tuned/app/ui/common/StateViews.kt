package com.tuned.app.ui.common

import androidx.compose.ui.unit.Dp
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.tuned.app.R
import com.tuned.app.ui.theme.AppShape
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.Keyline
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.rememberMotion
import com.tuned.app.ui.theme.tunedColors

/**
 * Shown where content would be when there is none. Says what is missing and
 * offers at most one way forward.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.tunedColors
    Column(
        modifier.fillMaxWidth().padding(horizontal = Keyline.margin, vertical = Space.x4),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = colors.text3, modifier = Modifier.size(Sizes.iconHero))
        Spacer(Modifier.height(Space.lg))
        Text(title, style = AppType.title, color = colors.text1, textAlign = TextAlign.Center)
        if (body != null) {
            Spacer(Modifier.height(Space.xs))
            Text(body, style = AppType.bodyS, color = colors.text2, textAlign = TextAlign.Center)
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Space.xl))
            AppButton(actionLabel, onClick = onAction, style = AppButtonStyle.Secondary)
        }
    }
}

/** Shown where content failed to load, with a retry in place. */
@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Filled.CloudOff,
        title = stringResource(R.string.cmp_error_title),
        // Cap the raw message: an exception dump must not become a wall of
        // text, and a polite live region makes TalkBack announce the failure
        // instead of the error appearing silently.
        body = message.take(240),
        actionLabel = stringResource(R.string.cmp_retry),
        onAction = onRetry,
        modifier = modifier.semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite },
    )
}

/**
 * A placeholder with the exact geometry of [TrackRow], so content replaces
 * it without the list jumping. Pulses gently unless animations are off.
 */
@Composable
fun SkeletonTrackRow(
    modifier: Modifier = Modifier,
    density: TrackRowDensity = TrackRowDensity.Default,
    horizontalPadding: Dp = Keyline.margin,
) {
    val colors = MaterialTheme.tunedColors
    val rowHeight = if (density == TrackRowDensity.Compact) Sizes.rowCompact else Sizes.row
    val artSize = if (density == TrackRowDensity.Compact) Sizes.artRowCompact else Sizes.artRow
    Row(
        modifier
            .fillMaxWidth()
            .height(rowHeight)
            .padding(horizontal = horizontalPadding)
            .alpha(skeletonAlpha())
            .clearAndSetSemantics { },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(artSize).clip(AppShape.xs).background(colors.surface2))
        Spacer(Modifier.width(Space.md))
        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Box(Modifier.fillMaxWidth(0.55f).height(Sizes.skeletonTitle).clip(AppShape.xs).background(colors.surface2))
            Box(Modifier.fillMaxWidth(0.35f).height(Sizes.skeletonSubtitle).clip(AppShape.xs).background(colors.surface2))
        }
    }
}

/** A placeholder with the geometry of a square [MediaTile]. */
@Composable
fun SkeletonTile(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.tunedColors
    Column(modifier.width(Sizes.tileSquare).alpha(skeletonAlpha()).clearAndSetSemantics { }) {
        Box(Modifier.size(Sizes.tileSquare).clip(AppShape.sm).background(colors.surface2))
        Spacer(Modifier.height(Space.sm))
        Box(Modifier.fillMaxWidth(0.7f).height(Sizes.skeletonTitle).clip(AppShape.xs).background(colors.surface2))
        Spacer(Modifier.height(Space.sm))
        Box(Modifier.fillMaxWidth(0.45f).height(Sizes.skeletonSubtitle).clip(AppShape.xs).background(colors.surface2))
    }
}

/** The pulse every skeleton shares (still when the system removes animations). */
@Composable
internal fun skeletonAlpha(): Float {
    val reduced = rememberMotion().reduced
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    return if (reduced) 1f else alpha
}

@ComponentPreviews
@Composable
private fun StateViewsPreview() = PreviewTheme { StateViewsSample() }

/** Sample states, shared by the preview and the in-app component gallery. */
@Composable
internal fun StateViewsSample() {
    Column {
        EmptyState(
            icon = Icons.Filled.LibraryMusic,
            title = "No playlists yet",
            body = "Make a mix from your listening, or import one from Spotify or YouTube.",
            actionLabel = "Make a mix",
            onAction = {},
        )
        ErrorState(message = "Couldn't reach YouTube Music. Check your connection.", onRetry = {})
        SkeletonTrackRow()
        SkeletonTrackRow()
        Row(Modifier.padding(Keyline.marginCompact), horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            SkeletonTile()
            SkeletonTile()
        }
    }
}

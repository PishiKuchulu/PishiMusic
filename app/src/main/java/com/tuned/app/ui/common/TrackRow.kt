package com.tuned.app.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.tuned.app.R
import com.tuned.app.ui.theme.AppShape
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.Keyline
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.rememberMotion
import com.tuned.app.ui.theme.rowIconEdgeInset
import com.tuned.app.ui.theme.tunedColors

/** Where this row's track stands relative to the player. */
enum class PlaybackMark { None, Paused, Playing }

enum class TrackRowDensity {
    /** 64dp row, 48dp artwork. Lists, search, history. */
    Default,
    /** 56dp row, 40dp artwork. Quick picks grids. */
    Compact,
}

/**
 * The one track row used everywhere a track is listed.
 *
 * Layout (Default): 16dp margin, 48dp artwork (or a 24dp index column), 12dp
 * gap, then title over subtitle with 4dp between (20 + 4 + 16 = 40dp of text
 * centred on the 48dp artwork). Text therefore starts at 76dp on every list.
 * The trailing 48dp button is inset so its 20dp icon's edge sits on the right
 * margin, matching the text column's left edge on the opposite side.
 *
 * @param index shows a track number instead of artwork (album track lists).
 * @param trailing replaces the overflow button, for a drag handle or a duration.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    artworkUrl: String? = null,
    index: Int? = null,
    playback: PlaybackMark = PlaybackMark.None,
    isDownloaded: Boolean = false,
    explicit: Boolean = false,
    isAvailable: Boolean = true,
    isSelected: Boolean = false,
    density: TrackRowDensity = TrackRowDensity.Default,
    onLongClick: (() -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    /**
     * Tap handler for an unavailable row. Without it the row is inert and the
     * user can never learn why the track cannot play; with it the row stays
     * tappable and the caller can surface the reason (snackbar/dialog). The
     * row model carries no structured reason, so the explanation text is the
     * caller's responsibility.
     */
    onUnavailableClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.tunedColors
    val rowHeight = if (density == TrackRowDensity.Compact) Sizes.rowCompact else Sizes.row
    val artSize = if (density == TrackRowDensity.Compact) Sizes.artRowCompact else Sizes.artRow
    val hasTrailing = trailing != null || onMoreClick != null

    val description = trackRowDescription(
        title = title,
        subtitle = subtitle,
        playback = playback,
        isDownloaded = isDownloaded,
        isAvailable = isAvailable,
        labels = TrackRowLabels(
            nowPlaying = stringResource(R.string.cmp_now_playing),
            downloaded = stringResource(R.string.cmp_downloaded),
            unavailable = stringResource(R.string.cmp_unavailable),
        ),
    )
    val motion = rememberMotion()
    val titleColor by animateColorAsState(
        targetValue = when {
            !isAvailable -> colors.text3
            playback != PlaybackMark.None -> colors.accent
            else -> colors.text1
        },
        animationSpec = motion.base(),
        label = "trackTitle",
    )
    val background by animateColorAsState(
        if (isSelected) colors.surface1 else Color.Transparent,
        motion.fast(),
        label = "trackSelected",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight)
            .background(background)
            .combinedClickable(
                enabled = isAvailable || onUnavailableClick != null,
                onClick = { if (isAvailable) onClick() else onUnavailableClick?.invoke() },
                onLongClick = onLongClick,
            )
            .semantics(mergeDescendants = true) {
                contentDescription = description
                selected = isSelected
            }
            .padding(start = Keyline.margin, end = if (hasTrailing) rowIconEdgeInset else Keyline.margin),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (index != null) {
            Text(
                text = index.toString(),
                style = AppType.mono,
                color = if (playback != PlaybackMark.None) colors.accent else colors.text2,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(Sizes.icon),
            )
        } else {
            Box(Modifier.size(artSize), contentAlignment = Alignment.Center) {
                Artwork(url = artworkUrl, size = artSize)
                androidx.compose.animation.AnimatedVisibility(playback != PlaybackMark.None, enter = fadeIn(motion.base()), exit = fadeOut(motion.fast())) {
                    Box(Modifier.fillMaxSize().background(colors.bg.copy(alpha = 0.6f), AppShape.xs), contentAlignment = Alignment.Center) {
                        PlayingIndicator(playing = playback == PlaybackMark.Playing)
                    }
                }
                androidx.compose.animation.AnimatedVisibility(isSelected, enter = fadeIn(motion.fast()) + scaleIn(motion.spring(), initialScale = 0.6f), exit = fadeOut(motion.fast())) {
                    Box(Modifier.fillMaxSize().background(colors.bg.copy(alpha = 0.6f), AppShape.xs), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = colors.accent, modifier = Modifier.size(Sizes.icon))
                    }
                }
            }
        }
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(title, style = AppType.body, color = titleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isDownloaded) {
                    Icon(Icons.Filled.DownloadDone, contentDescription = null, tint = colors.text2, modifier = Modifier.size(Sizes.iconInline))
                    Spacer(Modifier.width(Space.xs))
                }
                if (explicit) {
                    ExplicitTag(color = if (isAvailable) colors.text2 else colors.text3)
                    Spacer(Modifier.width(Space.xs + Space.xxs))
                }
                Text(
                    subtitle,
                    style = AppType.bodyS,
                    color = if (isAvailable) colors.text2 else colors.text3,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        when {
            trailing != null -> trailing()
            onMoreClick != null -> AppIconButton(
                icon = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.cmp_more_options),
                onClick = onMoreClick,
                iconSize = Sizes.iconRow,
                tint = colors.text2,
            )
        }
    }
}

/** Localised words the row description is built from. */
internal data class TrackRowLabels(val nowPlaying: String, val downloaded: String, val unavailable: String)

/** "Title, Subtitle, Now playing, Downloaded": one phrase a screen reader reads for the whole row. */
internal fun trackRowDescription(
    title: String,
    subtitle: String,
    playback: PlaybackMark,
    isDownloaded: Boolean,
    isAvailable: Boolean,
    labels: TrackRowLabels,
): String = buildList {
    add(title)
    if (subtitle.isNotBlank()) add(subtitle)
    if (playback != PlaybackMark.None) add(labels.nowPlaying)
    if (isDownloaded) add(labels.downloaded)
    if (!isAvailable) add(labels.unavailable)
}.joinToString(", ")

@ComponentPreviews
@Composable
private fun TrackRowPreview() = PreviewTheme { TrackRowSample() }

/** Sample states, shared by the preview and the in-app component gallery. */
@Composable
internal fun TrackRowSample() {
    Column {
        TrackRow("Odysseus", "Ludwig Göransson", onClick = {}, onMoreClick = {}, playback = PlaybackMark.Playing)
        TrackRow("all you wanna", "disctr4k", onClick = {}, onMoreClick = {}, isDownloaded = true)
        TrackRow("A title long enough that it has to be cut off at the end of the row", "Artist with a long name too", onClick = {}, onMoreClick = {})
        TrackRow("Selected track", "In multi-select", onClick = {}, isSelected = true)
        TrackRow("Not available here", "Region locked", onClick = {}, isAvailable = false, onMoreClick = {})
        TrackRow("Track 3", "3:47", onClick = {}, index = 3, onMoreClick = {})
        TrackRow(
            "Reorderable",
            "Queue",
            onClick = {},
            trailing = { AppIconButton(Icons.Filled.DragHandle, "Reorder", onClick = {}, iconSize = Sizes.iconRow) },
        )
        TrackRow("Compact", "Quick picks", onClick = {}, density = TrackRowDensity.Compact)
    }
}

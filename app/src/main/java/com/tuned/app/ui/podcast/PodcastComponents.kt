package com.tuned.app.ui.podcast

import com.tuned.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tuned.app.data.podcast.PodcastEpisode
import com.tuned.app.data.podcast.PodcastProgress
import com.tuned.app.data.podcast.PodcastShow
import com.tuned.app.ui.common.ExpressiveGroupTrackRow
import com.tuned.app.ui.common.GroupPosition

/** Square artwork for a podcast show. */
@Composable
fun PodcastShowArtwork(
    artworkUrl: String?,
    title: String,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 16,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (!artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = title,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                Icons.Filled.Mic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

/** One podcast show inside a horizontal shelf. Same tile as every other shelf. */
@Composable
fun PodcastShowCard(
    show: PodcastShow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    com.tuned.app.ui.common.MediaTile(
        title = show.title,
        subtitle = show.publisher?.takeIf { it.isNotBlank() },
        artworkUrl = show.artworkUrl,
        onClick = onClick,
        modifier = modifier,
    )
}

/** Horizontal shelf of podcast shows. */
@Composable
fun PodcastShowRow(
    shows: List<PodcastShow>,
    onShowClick: (PodcastShow) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = com.tuned.app.ui.theme.Keyline.margin),
        horizontalArrangement = Arrangement.spacedBy(com.tuned.app.ui.theme.Space.md),
    ) {
        items(shows.distinctBy { it.showId }, key = { it.showId }) { show ->
            PodcastShowCard(show = show, onClick = { onShowClick(show) })
        }
    }
}

/** One episode row with optional resume progress. */
@Composable
fun PodcastEpisodeRow(
    episode: PodcastEpisode,
    position: GroupPosition,
    isPlaying: Boolean,
    progress: PodcastProgress?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExpressiveGroupTrackRow(
        title = episode.title,
        subtitle = episodeSubtitle(episode),
        position = position,
        isPlaying = isPlaying,
        onClick = onClick,
        modifier = modifier,
        leading = {
            PodcastShowArtwork(
                artworkUrl = episode.artworkUrl,
                title = episode.title,
                modifier = Modifier.size(52.dp),
                cornerRadius = 12,
            )
        },
        trailing = {
            val fraction = progress?.fraction ?: 0f
            if (fraction > 0f) {
                Column(horizontalAlignment = Alignment.End) {
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.width(56.dp).height(4.dp),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${(fraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                IconButton(onClick = onClick) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.common_play))
                }
            }
        },
    )
}

internal fun episodeSubtitle(episode: PodcastEpisode): String =
    episode.publishedLabel?.takeIf { it.isNotBlank() }
        ?: episode.showTitle?.takeIf { it.isNotBlank() }
        ?: "Podcast"

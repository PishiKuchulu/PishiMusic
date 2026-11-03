package com.tuned.app.ui.podcast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuned.app.R
import com.tuned.app.ui.common.ExpressiveHeader
import com.tuned.app.ui.common.adaptiveContentWidth
import com.tuned.app.ui.common.groupPositionFor
import com.tuned.app.ui.common.safeDrawingBottomPadding
import com.tuned.app.ui.common.safeHorizontalContentPadding
import com.tuned.app.ui.player.LocalMiniPlayerScrollClearance
import com.tuned.app.ui.player.LocalMusicPlayer

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PodcastShowScreen(
    showId: String,
    onBack: () -> Unit = {},
    viewModel: PodcastShowViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val musicPlayer = LocalMusicPlayer.current
    val playbackState by musicPlayer.chromeState.collectAsStateWithLifecycle()

    LaunchedEffect(showId) {
        viewModel.load(showId)
    }

    Column(
        Modifier
            .fillMaxSize()
            .adaptiveContentWidth(maxWidth = 860.dp),
    ) {
        ExpressiveHeader(
            title = state.show?.title?.takeIf { it.isNotBlank() } ?: stringResource(R.string.podcasts_title),
            subtitle = state.show?.publisher?.takeIf { it.isNotBlank() },
            onBack = onBack,
        )

        val pullState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = state.isLoading && state.episodes.isNotEmpty(),
            onRefresh = viewModel::refresh,
            state = pullState,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                com.tuned.app.ui.common.RefreshIndicator(
                    state = pullState,
                    isRefreshing = state.isLoading && state.episodes.isNotEmpty(),
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            },
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().safeHorizontalContentPadding(),
            contentPadding = PaddingValues(
                top = 10.dp,
                bottom = 24.dp + LocalMiniPlayerScrollClearance.current + safeDrawingBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item(key = "show_header") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PodcastShowArtwork(
                        artworkUrl = state.show?.artworkUrl,
                        title = state.show?.title.orEmpty(),
                        modifier = Modifier.size(96.dp),
                    )
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f)) {
                        state.show?.description?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 5,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        val latest = state.episodes.firstOrNull()
                        if (latest != null) {
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { viewModel.playEpisode(latest) },
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                ) {
                                    Text(
                                        stringResource(R.string.podcast_play_latest),
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                val resume = state.resumeEpisode
                                if (resume != null && resume.videoId != latest.videoId) {
                                    Spacer(Modifier.size(8.dp))
                                    TextButton(onClick = { viewModel.playEpisode(resume) }) {
                                        Text(
                                            stringResource(R.string.podcast_resume),
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (state.isLoading && state.episodes.isEmpty()) {
                item(key = "show_loading") {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        com.tuned.app.ui.common.LoadingSpinner()
                        Spacer(Modifier.size(12.dp))
                        Text(
                            stringResource(R.string.podcasts_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else if (state.error != null && state.episodes.isEmpty()) {
                item(key = "show_error") {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.podcasts_load_fail),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            state.error.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { viewModel.load(showId, forceRefresh = true) }) {
                            Text(stringResource(R.string.ly_retry))
                        }
                    }
                }
            } else if (state.episodes.isEmpty()) {
                item(key = "show_empty") {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Filled.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            stringResource(R.string.podcasts_empty),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            } else {
                item(key = "show_episodes_header") {
                    Text(
                        stringResource(R.string.podcast_episodes),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
                itemsIndexed(
                    items = state.episodes,
                    key = { index, episode -> "episode:${episode.videoId}:$index" },
                ) { index, episode ->
                    val isPlayingThis = playbackState.isPlaying &&
                        playbackState.current?.videoId == episode.videoId
                    PodcastEpisodeRow(
                        episode = episode,
                        position = groupPositionFor(index, state.episodes.size),
                        isPlaying = isPlayingThis,
                        progress = state.progress[episode.videoId],
                        onClick = { viewModel.playEpisode(episode) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                if (state.hasMore) {
                    item(key = "show_more") {
                        TextButton(
                            onClick = viewModel::loadMore,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        ) {
                            Text(stringResource(R.string.podcast_load_more))
                        }
                    }
                }
            }
        }
        }
    }
}

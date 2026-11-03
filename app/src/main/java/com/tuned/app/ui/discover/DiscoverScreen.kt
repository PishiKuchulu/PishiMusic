package com.tuned.app.ui.discover

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tuned.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuned.app.data.generate.GeneratedTrack
import com.tuned.app.playback.toPlayableTrack
import com.tuned.app.ui.common.ArtworkImage
import com.tuned.app.ui.common.ExpressiveHeader
import com.tuned.app.ui.common.ExpressiveMotion
import com.tuned.app.ui.common.GroupGap
import com.tuned.app.ui.common.GroupPosition
import com.tuned.app.ui.common.HeaderActionIcon
import com.tuned.app.ui.common.safeDrawingBottomPadding
import com.tuned.app.ui.common.safeHorizontalContentPadding
import com.tuned.app.ui.common.adaptiveContentWidth
import com.tuned.app.ui.common.TrackContextMenuSheet
import com.tuned.app.ui.common.TrackMenuCapabilities
import com.tuned.app.ui.common.TrackMenuTarget
import com.tuned.app.ui.common.groupShape
import com.tuned.app.ui.player.LocalMiniPlayerScrollClearance
import com.tuned.app.ui.player.LocalMusicPlayer
import com.tuned.app.ui.theme.ExpressivePillShape

@Composable
private fun shimmerBrush(): Brush {
    val base = MaterialTheme.colorScheme.surfaceContainerHighest
    val highlight = androidx.compose.ui.graphics.lerp(base, MaterialTheme.colorScheme.primary, 0.22f)
    val shimmerColors = listOf(
        base.copy(alpha = 0.35f),
        highlight.copy(alpha = 0.85f),
        base.copy(alpha = 0.35f),
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -400f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2200
                -400f at 0 using LinearEasing
                -400f at 500 using LinearEasing
                1200f at 2000 using LinearEasing
                1200f at 2200
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerAnim",
    )
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 300f, translateAnim - 300f),
        end = Offset(translateAnim, translateAnim),
    )
}

@Composable
fun DiscoverScreen(
    onBack: () -> Unit = {},
    onConnectYouTube: () -> Unit = {},
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val musicPlayer = LocalMusicPlayer.current
    val playbackState by musicPlayer.chromeState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val pullState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()
    var menuTrack by remember { mutableStateOf<GeneratedTrack?>(null) }
    val playbackQueue = remember(state.tracks) { state.tracks.map(GeneratedTrack::toPlayableTrack) }
    val playFromDiscover: (GeneratedTrack) -> Unit = { track ->
        val index = state.tracks.indexOfFirst { it.key == track.key }.coerceAtLeast(0)
        musicPlayer.playDiscoverQueue(playbackQueue, index)
    }

    val shouldLoadMore by remember(listState, state.tracks.size) {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= state.tracks.size - 3
        }
    }
    LaunchedEffect(shouldLoadMore, state.tracks.size) {
        if (shouldLoadMore && !state.isLoading && !state.isLoadingMore && !state.endReached && state.tracks.isNotEmpty()) viewModel.loadMore()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .adaptiveContentWidth(maxWidth = 860.dp),
        ) {
            ExpressiveHeader(
            title = "Discover",
            subtitle = "Discover fresh tracks",
            onBack = onBack,
            actions = {
                HeaderActionIcon(Icons.Filled.BookmarkAdd, "Save as playlist", viewModel::saveAsPlaylist)
                HeaderActionIcon(Icons.Filled.Shuffle, "Shuffle Recommendations", viewModel::surpriseMe)
            },
        )

        // YouTube connection state used to be collected and never shown:
        // a disconnected user got a generic feed with no hint or way to act.
        if (!state.isYtConnected && !state.isLoading) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_connect_yt), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.settings_connect_yt_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onConnectYouTube) { Text(stringResource(R.string.cookie_login_connect)) }
            }
        }

        // Pull-to-refresh: refresh()/isRefreshing existed in the ViewModel
        // but no surface ever called or displayed them.
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            state = pullState,
            modifier = Modifier.fillMaxSize().safeHorizontalContentPadding(),
            indicator = {
                com.tuned.app.ui.common.RefreshIndicator(pullState, state.isRefreshing, Modifier.align(Alignment.TopCenter))
            },
        ) {
            Crossfade(
                targetState = state.isLoading && state.tracks.isEmpty(),
                animationSpec = tween(ExpressiveMotion.Standard, easing = FastOutSlowInEasing),
                label = "discoverState",
            ) { isLoading ->
                if (isLoading) {
                    val shimmer = shimmerBrush()
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 24.dp + LocalMiniPlayerScrollClearance.current + safeDrawingBottomPadding(),
                        ),
                        verticalArrangement = Arrangement.spacedBy(GroupGap),
                    ) {
                        items(10) { index ->
                            SkeletonCard(
                                brush = shimmer,
                                position = when (index) {
                                    0 -> GroupPosition.TOP
                                    9 -> GroupPosition.BOTTOM
                                    else -> GroupPosition.MIDDLE
                                },
                                titleWidthFraction = listOf(0.62f, 0.48f, 0.7f, 0.55f)[index % 4],
                                subtitleWidthFraction = listOf(0.4f, 0.3f, 0.45f, 0.35f)[index % 4],
                            )
                        }
                    }
                } else when {
                    state.error != null && state.tracks.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.disc_error_title), style = MaterialTheme.typography.titleMedium)
                            Text(state.error.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = viewModel::loadInitial) { Text(stringResource(R.string.ly_retry)) }
                        }
                    }
                    state.tracks.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.disc_empty_title), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.disc_empty_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    else -> LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = 24.dp + LocalMiniPlayerScrollClearance.current + safeDrawingBottomPadding()
                        ),
                        verticalArrangement = Arrangement.spacedBy(GroupGap),
                        modifier = Modifier,
                    ) {
                        itemsIndexed(
                            state.tracks,
                            key = { _, t -> t.key },
                            contentType = { _, _ -> "discover_track" },
                        ) { index, track ->
                            val position = com.tuned.app.ui.common.groupPositionFor(index, state.tracks.size)
                            // Identity by videoId when both sides carry one
                            // (GeneratedTrack.url is the pinned videoId);
                            // title+artist equality alone flagged unrelated
                            // same-name tracks as "playing".
                            val current = playbackState.current
                            val isPlayingThisSong = playbackState.isPlaying && current != null &&
                                if (track.url.isNotBlank() && !current.videoId.isNullOrBlank()) {
                                    current.videoId == track.url
                                } else {
                                    current.title.equals(track.name, ignoreCase = true) &&
                                        current.artist.equals(track.artist, ignoreCase = true)
                                }

                            DiscoverCard(
                                track = track,
                                position = position,
                                isPlaying = isPlayingThisSong,
                                onPlay = { playFromDiscover(track) },
                                onMenu = { menuTrack = track },
                                modifier = Modifier,
                            )
                        }
                        if (state.isLoadingMore) {
                            item(key = "loading_more", contentType = "loading") {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    com.tuned.app.ui.common.ExpressiveInlineLoadingIndicator()
                                }
                            }
                        }
                        if (state.loadMoreError) {
                            item(key = "loading_more_error", contentType = "error") {
                                Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                    TextButton(onClick = viewModel::retryLoadMore) {
                                        Text(stringResource(R.string.ly_retry))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        state.saveResultMessage?.let { msg ->
            LaunchedEffect(msg) {
                kotlinx.coroutines.delay(3000)
                viewModel.dismissSaveResult()
            }
            Surface(shape = ExpressivePillShape, color = MaterialTheme.colorScheme.inverseSurface, modifier = Modifier.padding(16.dp)) {
                Text(msg, color = MaterialTheme.colorScheme.inverseOnSurface, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
            }
        }
    }
    }

    menuTrack?.let { track ->
        TrackContextMenuSheet(
            target = TrackMenuTarget.Track(track.name, track.artist, track.url),
            capabilities = TrackMenuCapabilities(showCopyActions = true, showDeleteScrobble = true),
            onPlayInTuned = { playFromDiscover(track) },
            onDismiss = { menuTrack = null },
        )
    }
}

@Composable
private fun DiscoverCard(
    track: GeneratedTrack,
    position: GroupPosition,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    com.tuned.app.ui.common.ExpressiveGroupTrackRow(
        title = track.name,
        subtitle = track.artist,
        position = position,
        isPlaying = isPlaying,
        onClick = onPlay,
        modifier = modifier,
        leading = {
            Box(modifier = Modifier.size(52.dp)) {
                ArtworkImage(
                    name = track.name,
                    artist = track.artist,
                    embeddedUrl = track.artworkUrl,
                    fallbackIcon = if (isPlaying) Icons.Filled.GraphicEq else Icons.Filled.MusicNote,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                )
                if (isPlaying) {
                    com.tuned.app.ui.player.PlayingWaveBars(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(3.dp).size(24.dp, 18.dp),
                    )
                }
            }
        },
        trailing = { com.tuned.app.ui.common.OverflowMenuButton(onClick = onMenu) },
    )
}

@Composable
private fun SkeletonCard(
    brush: Brush,
    position: GroupPosition = GroupPosition.SINGLE,
    titleWidthFraction: Float = 0.6f,
    subtitleWidthFraction: Float = 0.4f,
) {
    Card(
        shape = groupShape(position),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(brush))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Box(Modifier.fillMaxWidth(titleWidthFraction).height(16.dp).clip(RoundedCornerShape(8.dp)).background(brush))
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth(subtitleWidthFraction).height(12.dp).clip(RoundedCornerShape(6.dp)).background(brush))
            }
        }
    }
}

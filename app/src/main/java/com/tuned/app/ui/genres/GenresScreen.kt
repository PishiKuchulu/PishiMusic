package com.tuned.app.ui.genres

import com.tuned.app.ui.theme.tunedColors
import androidx.compose.material.icons.filled.Category
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tuned.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuned.app.data.generate.GeneratedTrack
import com.tuned.app.ui.common.ArtworkImage
import com.tuned.app.ui.common.ExpressiveHeader
import com.tuned.app.ui.common.TrackContextMenuSheet
import com.tuned.app.ui.common.TrackMenuCapabilities
import com.tuned.app.ui.common.TrackMenuTarget
import com.tuned.app.ui.common.safeDrawingBottomPadding
import com.tuned.app.ui.common.safeHorizontalContentPadding
import com.tuned.app.ui.common.adaptiveContentWidth
import com.tuned.app.ui.player.LocalMiniPlayerScrollClearance

/**
 * Faithful port of genres.js (§5): bar-chart genre list with a period
 * filter, and a Genre Detail bottom sheet (§5.3) with Start Mix / Discover
 * More actions and an infinite-scrolling, sortable track list reusing the
 * shared track context menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenresScreen(
    onBack: () -> Unit = {},
    onNavigateToPlaylist: () -> Unit = {},
    viewModel: GenresViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.navigateToPlaylist) {
        if (state.navigateToPlaylist) {
            onNavigateToPlaylist()
            viewModel.consumeNavigateToPlaylist()
        }
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
                title = stringResource(R.string.ge_title),
                onBack = onBack,
            ) {
                // Every period as a chip in the panel, Home's chip style: one tap to switch, and
                // nothing pushed off the edge the way the old "Overall" dropdown was.
                PeriodChips(state.period, viewModel::setPeriod)
            }

            when {
                state.isLoading -> Box(
                    Modifier.fillMaxSize().safeHorizontalContentPadding(),
                    contentAlignment = Alignment.Center,
                ) { com.tuned.app.ui.common.ExpressiveLoadingIndicator() }
                state.error != null && state.stats.isEmpty() -> Box(
                    Modifier.fillMaxSize().safeHorizontalContentPadding(),
                    contentAlignment = Alignment.Center,
                ) {
                    // state.error used to be set by the ViewModel and rendered
                    // NOWHERE: a failed load looked exactly like an account
                    // with no genres.
                    com.tuned.app.ui.common.ErrorState(
                        message = state.error!!,
                        onRetry = {
                            viewModel.dismissError()
                            viewModel.retryLoad()
                        },
                    )
                }
                state.stats.isEmpty() -> Box(
                    Modifier.fillMaxSize().safeHorizontalContentPadding(),
                    contentAlignment = Alignment.Center,
                ) {
                    GenresEmpty()
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 24.dp + LocalMiniPlayerScrollClearance.current + safeDrawingBottomPadding(),
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize().safeHorizontalContentPadding(),
                ) {
                    items(state.stats.distinctBy { it.name }, key = { it.name }) { stat ->
                        GenreBarRow(
                            name = stat.name,
                            percent = stat.percentOfTop,
                            onClick = { viewModel.openDetail(stat.name) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }

    state.detailGenre?.let { genre ->
        GenreDetailSheet(
            genre = genre,
            state = state,
            onDismiss = viewModel::closeDetail,
            onLoadMore = { viewModel.loadDetailPage() },
            onSortChange = viewModel::setDetailSort,
            onRetryDetailPage = { viewModel.loadDetailPage() },
            onStartMix = { viewModel.startMix(genre) },
            onDiscoverMore = { viewModel.discoverMore(genre) },
            onShowYourTracks = { viewModel.showYourTracks(genre) },
            onExploreGenre = { viewModel.exploreGenre(it) },
        )
    }
}


@Composable
private fun PeriodChips(period: String, onChange: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = com.tuned.app.ui.theme.Keyline.margin, end = com.tuned.app.ui.theme.Keyline.margin, top = com.tuned.app.ui.theme.Space.sm, bottom = com.tuned.app.ui.theme.Space.lg),
        horizontalArrangement = Arrangement.spacedBy(com.tuned.app.ui.theme.Space.sm),
    ) {
        GENRE_PERIODS.forEach { (value, text) ->
            val selected = value == period
            Surface(
                onClick = { onChange(value) },
                shape = RoundedCornerShape(50),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.tunedColors.text1.copy(alpha = 0.10f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
                ),
            ) {
                Text(
                    text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                )
            }
        }
    }
}

/** No genres yet: a tonal mark, what's missing, and how it fills in. */
@Composable
private fun GenresEmpty() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 32.dp)) {
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Category,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.ge_empty), style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.ge_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun GenreBarRow(
    name: String,
    percent: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(targetValue = percent.coerceIn(0.03f, 1f), animationSpec = tween(600), label = "genreBar")
    Column(modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text("${(percent * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
            Box(
                Modifier
                    .fillMaxWidth(animated)
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenreDetailSheet(
    genre: String,
    state: GenresUiState,
    onDismiss: () -> Unit,
    onLoadMore: () -> Unit,
    onSortChange: (GenreDetailSort) -> Unit,
    onRetryDetailPage: () -> Unit,
    onStartMix: () -> Unit,
    onDiscoverMore: () -> Unit,
    onShowYourTracks: () -> Unit,
    onExploreGenre: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var menuTrack by remember { mutableStateOf<GeneratedTrack?>(null) }
    val listState = rememberLazyListState()

    val shouldLoadMore by remember(listState, state.detailTracks.size) {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= state.detailTracks.size - 5
        }
    }
    LaunchedEffect(shouldLoadMore, state.detailTracks.size) {
        if (shouldLoadMore && !state.detailLoading && state.detailHasMore && !state.isDiscoverMode) onLoadMore()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxSize()
                .adaptiveContentWidth(maxWidth = 640.dp)
                .align(Alignment.CenterHorizontally),
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(genre.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    if (state.isDiscoverMode) "Fresh recommendations" else "Based on your scrobbles",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.material3.Button(onClick = onStartMix, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.fr_start_mix))
                }
                if (state.isDiscoverMode) {
                    androidx.compose.material3.OutlinedButton(onClick = onShowYourTracks, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.ge_tracks))
                    }
                } else {
                    androidx.compose.material3.OutlinedButton(onClick = onDiscoverMore, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.ge_discover))
                    }
                }
            }

            // Detail sort (Popular / Newest / A–Z) existed in the ViewModel
            // with no control anywhere; wire it as a chip row.
            if (!state.isDiscoverMode) {
                com.tuned.app.ui.common.FilterChipRow(
                    options = listOf("Popular", "Newest", "A–Z"),
                    selectedIndex = state.detailSort.ordinal,
                    onSelect = { onSortChange(GenreDetailSort.entries[it]) },
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(bottom = 12.dp)) {
                items(state.detailTracks.distinctBy { it.key }, key = { it.key }) { track ->
                    GenreTrackRow(
                        track = track,
                        allTracks = state.detailTracks,
                        genreTitle = "${genre.replaceFirstChar { it.uppercase() }}",
                        onMenu = { menuTrack = track },
                        modifier = Modifier.animateItem(),
                    )
                }
                if (state.detailLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            com.tuned.app.ui.common.ExpressiveInlineLoadingIndicator()
                        }
                    }
                }
                if (state.detailError && !state.detailLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                            androidx.compose.material3.TextButton(onClick = onRetryDetailPage) {
                                Text(stringResource(R.string.cmp_retry))
                            }
                        }
                    }
                }
                if (!state.detailLoading && state.detailTracks.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.ge_no_tracks), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    menuTrack?.let { track ->
        TrackContextMenuSheet(
            target = TrackMenuTarget.Track(track.name, track.artist, track.url),
            capabilities = TrackMenuCapabilities(showCopyActions = true, showDeleteScrobble = true),
            playbackSourceLabel = "Genres",
            onDismiss = { menuTrack = null },
            onExploreGenre = onExploreGenre,
        )
    }
}

@Composable
private fun GenreTrackRow(
    track: GeneratedTrack,
    allTracks: List<GeneratedTrack>,
    genreTitle: String,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val musicPlayer = com.tuned.app.ui.player.LocalMusicPlayer.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                val queue = allTracks.map {
                    com.tuned.app.playback.PlayableTrack(it.name, it.artist, album = it.album, artworkUrl = it.artworkUrl)
                }
                val index = allTracks.indexOf(track).coerceAtLeast(0)
                musicPlayer.playQueue(queue, startIndex = index, sourceLabel = genreTitle)
            }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkImage(name = track.name, artist = track.artist, embeddedUrl = track.artworkUrl, fallbackIcon = Icons.Filled.MusicNote, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        com.tuned.app.ui.common.OverflowMenuButton(onClick = onMenu)
    }
}

package com.tuned.app.ui.feed

import com.tuned.app.ui.theme.rememberMotion
import androidx.compose.ui.unit.IntSize
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity
import com.tuned.app.ui.common.PlaybackMark
import com.tuned.app.ui.common.TrackRow
import com.tuned.app.playback.toPlayableTrack
import androidx.compose.material.icons.filled.Download
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import com.tuned.app.R
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.tuned.app.data.feed.FeedAlbum
import com.tuned.app.data.feed.FeedArtist
import com.tuned.app.data.feed.FeedQuickTile
import com.tuned.app.data.feed.FeedSpotlight
import com.tuned.app.util.ArtistHelper
import com.tuned.app.data.generate.GeneratedTrack
import com.tuned.app.data.local.HomeSection
import com.tuned.app.data.model.FriendEntry
import com.tuned.app.data.model.RecentTrack
import com.tuned.app.data.music.YouTubeMusicTrack
import com.tuned.app.data.music.YouTubePlaylistSummary
import com.tuned.app.data.music.HomeChip
import com.tuned.app.data.music.HomeShelf
import com.tuned.app.data.music.HomeShelfItem
import com.tuned.app.playback.PlayableTrack
import com.tuned.app.ui.common.ArtworkImage
import com.tuned.app.ui.common.ExpressiveHeader
import com.tuned.app.ui.common.HeaderActionIcon
import com.tuned.app.ui.common.TrackContextMenuSheet
import com.tuned.app.ui.common.TrackMiniTrayData
import com.tuned.app.ui.common.TrackMiniTraySheet
import com.tuned.app.ui.common.WindowSizeClass
import com.tuned.app.ui.common.rememberWindowSizeClass
import com.tuned.app.ui.common.TrackMenuCapabilities
import com.tuned.app.ui.common.TrackMenuTarget
import com.tuned.app.ui.common.ExpressiveInlineLoadingIndicator
import com.tuned.app.ui.common.adaptiveContentWidth
import com.tuned.app.ui.common.safeHorizontalContentPadding
import com.tuned.app.ui.navigation.ArtistAlbumNavigator
import com.tuned.app.ui.player.LocalMusicPlayer
import com.tuned.app.ui.player.PlayingWaveBars
import com.tuned.app.ui.shell.FloatingNavDefaults
import com.tuned.app.ui.podcast.PodcastShowRow
import com.tuned.app.ui.common.AppIconButton
import com.tuned.app.ui.common.pressScale
import com.tuned.app.ui.theme.AppShape
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.HapticKind
import com.tuned.app.ui.theme.Keyline
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.rememberHaptics
import com.tuned.app.ui.theme.rowIconEdgeInset
import com.tuned.app.ui.theme.tunedColors


@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenFeedPlaylist: (String) -> Unit,
    onOpenPlaylist: (Long) -> Unit = {},
    onOpenGenerator: () -> Unit = {},
    onOpenFriends: () -> Unit = {},
    onOpenDownloads: () -> Unit = {},
    onOpenFriendProfile: (username: String, displayName: String?, avatarUrl: String?) -> Unit = { _, _, _ -> },
    onOpenNewReleases: () -> Unit = {},
    onOpenPodcasts: () -> Unit = {},
    onOpenPodcastShow: (String) -> Unit = {},
    viewModel: FeedViewModel = hiltViewModel(),
    artistAlbumNavigator: ArtistAlbumNavigator = hiltViewModel<ArtistAlbumNavBridgeFeed>().navigator,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val discoveryTracks by viewModel.discoveryTracks.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) viewModel.onVisible()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val musicPlayer = LocalMusicPlayer.current
    val context = LocalContext.current
    val playbackState by musicPlayer.chromeState.collectAsStateWithLifecycle()
    var menuTrack by remember { mutableStateOf<YouTubeMusicTrack?>(null) }
    // Long-press (deep press) mini tray — 3-dot buttons keep the full sheet.
    var miniTrayTrack by remember { mutableStateOf<TrackMiniTrayData?>(null) }
    // Sections hidden via Settings → Home sections.
    val hiddenSections by viewModel.hiddenHomeSections.collectAsStateWithLifecycle()
    var showPlayground by remember { mutableStateOf(false) }
    if (showPlayground) {
        com.tuned.app.ui.playlist.PlaygroundHost(
            viewModel = hiltViewModel(),
            onDismiss = { showPlayground = false },
            onSaved = onOpenPlaylist,
        )
    }
    val podcastsEnabled by viewModel.podcastsEnabled.collectAsStateWithLifecycle()
    fun isSectionVisible(section: HomeSection) = section.id !in hiddenSections
    val snackbarHostState = remember { SnackbarHostState() }
    // Remembered: this scan over every shelf used to re-run on every
    // recomposition. feedData.friends is deliberately NOT counted — no
    // section renders it, so a friends-only feed previously reported
    // "has content" and showed a blank list instead of the empty state.
    val hasFeedContent = remember(state.feedData, state.isChipActive) {
        state.isChipActive || with(state.feedData) {
            nativeShelves.any { it.items.isNotEmpty() } ||
                quickTiles.isNotEmpty() || mixes.isNotEmpty() || topArtists.isNotEmpty() ||
                quickPicks.isNotEmpty() || jumpBackIn.isNotEmpty() || recentAlbums.isNotEmpty() ||
                heavyRotation.isNotEmpty() || ytLikedSongs.isNotEmpty() || ytRecentSongs.isNotEmpty() ||
                (becauseYouListenTo?.items?.isNotEmpty() == true) || freshFinds.isNotEmpty() ||
                spotlight != null || charts.isNotEmpty() || newReleases.isNotEmpty() ||
                podcastShows.isNotEmpty()
        }
    }
    val showSynthetic = !state.isChipActive
    val listState = rememberLazyListState()
    val shouldLoadMore by remember(listState) {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && last >= total - 3
        }
    }
    LaunchedEffect(shouldLoadMore, state.pagedShelves.size, state.chipShelves.size) {
        if (shouldLoadMore && !state.isLoading && !state.isLoadingMore && !state.isRefreshing && !state.isChipLoading) {
            viewModel.loadMoreHomeShelves()
        }
    }
    LaunchedEffect(state.error, hasFeedContent) {
        val error = state.error ?: return@LaunchedEffect
        if (hasFeedContent) {
            val result = snackbarHostState.showSnackbar(error, actionLabel = context.getString(R.string.feed_retry), withDismissAction = true)
            viewModel.dismissError()
            if (result == SnackbarResult.ActionPerformed) viewModel.refresh()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        // The header panel floats over the feed: the list fills the screen and
        // starts below the panel, so content scrolling up slides behind its
        // rounded corners instead of being cut off on a straight line.
        val density = LocalDensity.current
        var headerHeight by remember { mutableStateOf(0.dp) }
        // Scrolling down folds the chips into the header, on the same signal
        // and spring as the dock sliding away; scrolling up brings them back.
        val chipsFolded = com.tuned.app.ui.shell.LocalDockState.current.tabsCollapsed
        val headerMotion = rememberMotion()
        val foldSpec = if (headerMotion.reduced) snap<IntSize>() else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntSize.VisibilityThreshold)
        val pullState = rememberPullToRefreshState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .adaptiveContentWidth(maxWidth = 920.dp),
        ) {
            HomeHeaderPanel(
                artworkUrl = playbackState.current?.artworkUrl,
                modifier = Modifier
                    .zIndex(1f)
                    // The list keeps the full header's height as its top
                    // spacing, so folding the chips never moves the feed.
                    .onSizeChanged { if (!chipsFolded) headerHeight = with(density) { it.height.toDp() } },
            ) {
                HomeHeader(
                    greeting = stringResource(R.string.feed_home),
                    date = remember {
                        java.time.LocalDate.now().format(
                            java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d", java.util.Locale.getDefault()),
                        )
                    },
                    onOpenPlayground = { showPlayground = true },
                    onOpenDownloads = onOpenDownloads,
                    onOpenSettings = onOpenSettings,
                )
                // YouTube Music mood chips sit inside the panel; they fold
                // away while the feed scrolls down.
                if (state.homeChips.isEmpty() && state.isLoading) {
                    HomeChipsSkeleton()
                } else if (state.homeChips.isNotEmpty()) {
                    AnimatedVisibility(
                        visible = !chipsFolded,
                        enter = expandVertically(foldSpec, expandFrom = Alignment.Top) + fadeIn(headerMotion.base()),
                        exit = shrinkVertically(foldSpec, shrinkTowards = Alignment.Top) + fadeOut(headerMotion.fast()),
                    ) {
                        PinnedChipsBar(
                            chips = state.homeChips,
                            selectedChip = state.selectedChip,
                            onChipClick = viewModel::onHomeChipSelected,
                        )
                    }
                }
            }

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
                state = pullState,
                indicator = {
                    com.tuned.app.ui.common.RefreshIndicator(
                        state = pullState,
                        isRefreshing = state.isRefreshing,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = headerHeight),
                    )
                },
            ) {
                if (state.isLoading) {
                Box(Modifier.padding(top = headerHeight)) { FeedLoadingSkeleton() }
            } else if (!hasFeedContent) {
                Box(Modifier.padding(top = headerHeight)) {
                FeedEmptyState(
                    message = if (state.error != null) {
                        "Couldn't load recommendations. Try again, or search for something to play."
                    } else {
                        "Search for a song or artist to start. Home fills in as you listen."
                    },
                    onRetry = viewModel::loadFeed,
                    onOpenSearch = onOpenSearch,
                )
                }
            } else {
                val individualTopArtists = remember(state.feedData.topArtists) {
                    state.feedData.topArtists.flatMap { artist ->
                        val split = ArtistHelper.splitArtists(artist.name)
                        if (split.size <= 1) {
                            listOf(artist.copy(name = ArtistHelper.primaryArtist(artist.name)))
                        } else {
                            split.map { singleName ->
                                FeedArtist(
                                    name = singleName,
                                    browseId = if (singleName.equals(artist.name, ignoreCase = true)) artist.browseId else null,
                                    artworkUrl = artist.artworkUrl,
                                )
                            }
                        }
                    }.distinctBy { it.name.trim().lowercase() }
                }

                // YouTube sends its own "Quick picks" shelf. Its tracks fill the
                // top section, and the shelf is not repeated further down.
                val ytQuickPicksShelf = remember(state.feedData.nativeShelves) {
                    state.feedData.nativeShelves.firstOrNull { shelf ->
                        shelf.title.trim().equals("Quick picks", ignoreCase = true) &&
                            shelf.items.any { it is HomeShelfItem.Track }
                    }
                }
                // Personal picks (your plays, YouTube or Last.fm history) win over
                // YouTube's regional guest shelf.
                val personalPicks = state.feedData.hasListeningHistory || state.feedData.hasYtRecommendations || state.feedData.hasPersonalContent
                val topPicks = remember(ytQuickPicksShelf, state.feedData.quickPicks, personalPicks) {
                    (if (personalPicks) state.feedData.quickPicks
                    else ytQuickPicksShelf?.items?.mapNotNull { (it as? HomeShelfItem.Track)?.track } ?: state.feedData.quickPicks)
                        .filterNot { it.looksLikeVideo }
                }
                // Guest view hides the account-only tiles; remembered here
                // (composable scope) — inside the LazyColumn block below it
                // would not be a composable context.
                val quickTiles = remember(state.feedData.quickTiles, state.feedData.isYtConnected) {
                    if (state.feedData.isYtConnected) {
                        state.feedData.quickTiles
                    } else {
                        state.feedData.quickTiles.filter {
                            it.collection != "yt_liked" && it.collection != "yt_recent" &&
                                it.playlistId != "yt_liked" && it.playlistId != "yt_recent"
                        }
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().safeHorizontalContentPadding(),
                    contentPadding = PaddingValues(
                        bottom = FloatingNavDefaults.contentBottomPadding(),
                        top = headerHeight + Space.xl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Space.xxl),
                ) {
                    if (showSynthetic && isSectionVisible(HomeSection.QUICK_PICKS) && topPicks.isNotEmpty()) {
                        item(key = "quick_picks") {
                            HomeSectionContainer() {
                                Column {
                            val title = if (state.feedData.hasYtRecommendations) stringResource(R.string.feed_picked_you) else stringResource(R.string.feed_quick_picks)
                            val subtitle = if (state.feedData.hasYtRecommendations || state.feedData.hasListeningHistory) {
                                stringResource(R.string.feed_from_listening)
                            } else if (state.feedData.hasPersonalContent) {
                                stringResource(R.string.feed_matched_taste)
                            } else {
                                stringResource(R.string.feed_trending_worth)
                            }
                            FeedSectionHeader(
                                title = title,
                                subtitle = subtitle,
                                actionText = stringResource(R.string.feed_play_all),
                                actionIcon = Icons.Filled.PlayArrow,
                                onActionClick = { viewModel.playTracksQueue(topPicks, 0, "Quick picks") },
                                onShuffleClick = { viewModel.shuffleTracksQueue(topPicks, "Quick picks") },
                            )
                            QuickPicksRows(
                                tracks = topPicks,
                                currentPlayingVideoId = playbackState.current?.videoId,
                                isPlaying = playbackState.isPlaying,
                                onTrackClick = { index ->
                                    val track = topPicks.getOrNull(index)
                                    if (track != null) {
                                        viewModel.playTrack(track, "Quick picks")
                                    } else {
                                        viewModel.playTracksQueue(topPicks, index, "Quick picks")
                                    }
                                },
                                onMenuClick = { menuTrack = it },
                                onLongClick = { track, index ->
                                    miniTrayTrack = TrackMiniTrayData(
                                        title = track.title,
                                        artist = track.artist,
                                        album = track.album,
                                        artworkUrl = track.artworkUrl,
                                        videoId = track.videoId.takeIf(String::isNotBlank),
                                        sourceLabel = "Home",
                                        onPlay = { viewModel.playTrack(track, "Quick picks") },
                                    )
                                },
                            )
                        
                                }
                            }
                        }
                    }

                    if (showSynthetic && isSectionVisible(HomeSection.JUMP_BACK_IN) && state.feedData.jumpBackIn.isNotEmpty()) {
                        item(key = "jump_back_in") {
                            HomeSectionContainer() {
                                Column {
                            FeedSectionHeader(
                                title = stringResource(R.string.feed_jump),
                                subtitle = stringResource(R.string.feed_jump_sub),
                                actionText = stringResource(R.string.feed_play_all),
                                actionIcon = Icons.Filled.PlayArrow,
                                onActionClick = { viewModel.playRecentQueue(state.feedData.jumpBackIn, 0) },
                            )
                            FeedMediaRow(
                                content = {
                                    itemsIndexed(state.feedData.jumpBackIn, key = { index, track -> "${track.videoId}#$index" }) { index, track ->
                                        RecentTrackCard(
                                            track = track,
                                            onClick = { viewModel.playRecentQueue(state.feedData.jumpBackIn, index) },
                                            onLongClick = {
                                                miniTrayTrack = TrackMiniTrayData(
                                                    title = track.name,
                                                    artist = track.artist.displayName,
                                                    artworkUrl = track.artworkUrl,
                                                    sourceLabel = "Home",
                                                    onPlay = { viewModel.playRecentQueue(state.feedData.jumpBackIn, index) },
                                                )
                                            },
                                        )
                                    }
                                },
                            )
                        
                                }
                            }
                        }
                    }

                    if (showSynthetic && isSectionVisible(HomeSection.MIXES) && state.feedData.mixes.isNotEmpty()) {
                        item(key = "mixed_for_you") {
                            HomeSectionContainer() {
                                Column {
                            FeedSectionHeader(
                                title = stringResource(R.string.feed_mixes),
                                subtitle = stringResource(R.string.feed_mixes_sub),
                                actionText = stringResource(R.string.feed_shuffle),
                                actionIcon = Icons.Filled.Shuffle,
                                onActionClick = {
                                    state.feedData.mixes.randomOrNull()?.let(viewModel::playMix)
                                },
                            )
                            FeedMediaRow(
                                content = {
                                    items(state.feedData.mixes.distinctBy { it.seed.videoId }, key = { it.seed.videoId }) { mix ->
                                        FeedPlaylistCard(
                                            title = mix.title,
                                            subtitle = stringResource(R.string.feed_endless_radio),
                                            artworkUrl = mix.seed.artworkUrl,
                                            onClick = { viewModel.playMix(mix) },
                                            onPlayClick = { viewModel.playMix(mix) },
                                        )
                                    }
                                },
                            )
                        
                                }
                            }
                        }
                    }

                    if (showSynthetic && isSectionVisible(HomeSection.QUICK_TILES) && quickTiles.isNotEmpty()) {
                        item(key = "quick_tiles") {
                            HomeSectionContainer() {
                                Column {
                            FeedSectionHeader(title = stringResource(R.string.feed_quick_access))
                                    Spacer(modifier = Modifier.height(12.dp))
                            QuickTilesGrid(
                                tiles = quickTiles,
                                onTileClick = { tile ->
                                    when {
                                        tile.collection == "radio" -> onOpenDiscover()
                                        tile.collection == "discover_mix" -> viewModel.playDiscoverMix(tile)
                                        tile.collection == "mix" -> onOpenGenerator()
                                        tile.collection == "my_mix" -> {
                                            if (tile.playlistId != null) {
                                                viewModel.playPlaylistById(tile.playlistId, tile.title)
                                            } else {
                                                viewModel.playInfiniteRadio()
                                            }
                                        }
                                        tile.collection == "yt_liked" || tile.playlistId == "yt_liked" -> onOpenFeedPlaylist("yt_liked")
                                        tile.collection == "yt_recent" || tile.playlistId == "yt_recent" -> onOpenFeedPlaylist("yt_recent")
                                        tile.collection == "new_releases" -> onOpenNewReleases()
                                        tile.localPlaylistId != null -> onOpenPlaylist(tile.localPlaylistId)
                                        tile.playlistId != null -> onOpenFeedPlaylist(tile.playlistId)
                                        else -> viewModel.handleQuickTileClick(tile)
                                    }
                                },
                            )
                        
                                }
                            }
                        }
                    }

                    // Radio entry points break up the YouTube shelves instead of
                    // leading the page: the first screen is what you play most.
                    fun LazyListScope.radioSections() {
                        if (showSynthetic && isSectionVisible(HomeSection.HERO)) {
                        item(key = "hero") {
                            InfiniteRadioCard(
                                artworkUrl = state.feedData.quickPicks.firstOrNull { !it.artworkUrl.isNullOrBlank() }?.artworkUrl,
                                enabled = state.feedData.quickPicks.isNotEmpty(),
                                onPlay = viewModel::playInfiniteRadio,
                            )
                        }
                        }

                        if (showSynthetic && isSectionVisible(HomeSection.TASTE_STRIP)) {
                            val displayTags = state.feedData.tasteTags.ifEmpty {
                                listOf("Rock", "Indie", "Pop", "Electronic", "Hip-Hop", "R&B", "Synthwave", "Alternative")
                            }
                            item(key = "taste_strip") {
                                TasteStrip(
                                    tags = displayTags,
                                    launching = state.launchingRadio,
                                    onTagClick = { tag -> viewModel.playDiscoveryQuery(tag.displayName(), tag) },
                                )
                            }
                        }

                    }

                    // YouTube Music mood/activity chips (Relax, Energize, Workout,
                    // ...) are pinned at the top under the app bar — see the
                    // PinnedChipsBar rendered above the LazyColumn. Loading,
                    // filtering and selection logic are unchanged; only the
                    // row's position moved from between feed sections to the top.

                    if (state.isChipActive) {
                        if (state.isChipLoading) {
                            // Card-shaped shimmer placeholders that mirror the
                            // native shelf layout the chip page will render.
                            item(key = "chip_loading_skeleton") {
                                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                    com.tuned.app.ui.feed.cards.ShelfSquareSkeletonRow(
                                        cardWidth = com.tuned.app.ui.feed.cards.rememberSquareCardWidth(),
                                    )
                                    com.tuned.app.ui.feed.cards.ShelfWideSkeletonRow(
                                        cardWidth = com.tuned.app.ui.feed.cards.rememberWideCardWidth(),
                                    )
                                }
                            }
                        } else {
                            for ((shelfIndex, shelf) in state.chipShelves.withIndex()) {
                                if (shelfIndex == 2) radioSections()
                                if (shelf.items.isEmpty()) continue
                                item(key = "chip_shelf_${shelfIndex}_${shelf.title}") {
                                    HomeShelfCard(
                                        shelf = shelf,
                                        onPlayTrack = { track, title -> viewModel.playTrack(track, title) },
                                        onPlayTracksQueue = { tracks, idx, title -> viewModel.playTracksQueue(tracks, idx, title) },
                                        onShuffleTracksQueue = { tracks, title -> viewModel.shuffleTracksQueue(tracks, title) },
                                        onOpenFeedPlaylist = onOpenFeedPlaylist,
                                        onOpenAlbum = { title, artist, id -> artistAlbumNavigator.openAlbum(title, artist, id) },
                                        onTrackLongClick = { tray -> miniTrayTrack = tray },
                                    )
                                }
                            }
                        }
                    } else {
                        // Native YouTube Music shelves (exact titles, original order, original items)
                        for ((shelfIndex, shelf) in state.feedData.nativeShelves.withIndex()) {
                            if (shelfIndex == 2) radioSections()
                            if (shelf.items.isEmpty() || (shelf === ytQuickPicksShelf && !personalPicks)) continue
                            item(key = "native_shelf_${shelfIndex}_${shelf.title}") {
                                HomeShelfCard(
                                    shelf = shelf,
                                    onPlayTrack = { track, title -> viewModel.playTrack(track, title) },
                                    onPlayTracksQueue = { tracks, idx, title -> viewModel.playTracksQueue(tracks, idx, title) },
                                    onShuffleTracksQueue = { tracks, title -> viewModel.shuffleTracksQueue(tracks, title) },
                                    onOpenFeedPlaylist = onOpenFeedPlaylist,
                                    onOpenAlbum = { title, artist, id -> artistAlbumNavigator.openAlbum(title, artist, id) },
                                    onTrackLongClick = { tray -> miniTrayTrack = tray },
                                )
                            }
                        }
                    }
                    val shelvesShown = if (state.isChipActive) {
                        if (state.isChipLoading) 0 else state.chipShelves.size
                    } else state.feedData.nativeShelves.size
                    if (shelvesShown <= 2) radioSections()

                    if (showSynthetic && podcastsEnabled && state.feedData.podcastShows.isNotEmpty()) {
                        item(key = "podcast_shelf") {
                            Column {
                                FeedSectionHeader(
                                    title = state.feedData.podcastShelfTitle
                                        ?.takeIf { it.isNotBlank() }
                                        ?: stringResource(R.string.podcasts_title),
                                    actionText = stringResource(R.string.podcasts_see_all),
                                    actionIcon = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    onActionClick = onOpenPodcasts,
                                )
                                Spacer(Modifier.height(Space.md))
                                PodcastShowRow(
                                    shows = state.feedData.podcastShows,
                                    onShowClick = { show -> onOpenPodcastShow(show.showId) },
                                )
                            }
                        }
                    }

                    if (showSynthetic && discoveryTracks.isNotEmpty()) {
                        item(key = "discover_new_music") {
                            HomeSectionContainer() {
                                Column {
                                    FeedSectionHeader(
                                        title = stringResource(R.string.feed_discover),
                                        subtitle = stringResource(R.string.feed_discover_sub),
                                        actionText = stringResource(R.string.feed_see_all),
                                        actionIcon = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        onActionClick = onOpenDiscover,
                                    )
                                    FeedMediaRow(
                                        content = {
                                            itemsIndexed(discoveryTracks, key = { index, track -> "${track.videoId}#$index" }) { index, track ->
                                                val playable = track.toPlayableTrack()
                                                FeedMediaCard(
                                                    title = track.name,
                                                    subtitle = ArtistHelper.primaryArtist(track.artist),
                                                    artworkUrl = track.artworkUrl,
                                                    fallbackIcon = Icons.Filled.Explore,
                                                    badgeText = "DISCOVER",
                                                    onClick = {
                                                        val queue = discoveryTracks.map { it.toPlayableTrack() }
                                                        musicPlayer.playDiscoverQueue(queue, index)
                                                    },
                                                    onPlayClick = {
                                                        val queue = discoveryTracks.map { it.toPlayableTrack() }
                                                        musicPlayer.playDiscoverQueue(queue, index)
                                                    },
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }

                    if (showSynthetic && isSectionVisible(HomeSection.BECAUSE_YOU_LISTEN_TO)) {
                    state.feedData.becauseYouListenTo?.takeIf { it.items.isNotEmpty() }?.let { section ->
                        item(key = "because_you_listen_to") {
                            HomeSectionContainer() {
                                Column {
                            FeedSectionHeader(
                                title = section.title,
                                subtitle = section.subtitle,
                                actionText = stringResource(R.string.feed_play_all),
                                actionIcon = Icons.Filled.PlayArrow,
                                onActionClick = { viewModel.playTracksQueue(section.items, 0, section.title) },
                                onShuffleClick = { viewModel.shuffleTracksQueue(section.items, section.title) },
                            )
                            FeedMediaRow(
                                content = {
                                    itemsIndexed(section.items, key = { index, track -> "${track.videoId}#$index" }) { index, track ->
                                        FeedMediaCard(
                                            title = track.title,
                                            subtitle = ArtistHelper.primaryArtist(track.artist),
                                            artworkUrl = track.artworkUrl,
                                            fallbackIcon = Icons.Filled.MusicNote,
                                            onClick = { viewModel.playTrack(track, section.title) },
                                            onLongClick = {
                                                miniTrayTrack = TrackMiniTrayData(
                                                    title = track.title,
                                                    artist = track.artist,
                                                    album = track.album,
                                                    artworkUrl = track.artworkUrl,
                                                    videoId = track.videoId.takeIf(String::isNotBlank),
                                                    sourceLabel = "Home",
                                                    onPlay = { viewModel.playTrack(track, section.title) },
                                                )
                                            },
                                            onPlayClick = { viewModel.playTrack(track, section.title) },
                                        )
                                    }
                                },
                            )
                        
                                }
                            }
                        }
                    }
                    }

                    if (showSynthetic && isSectionVisible(HomeSection.FRESH_FINDS) && state.feedData.freshFinds.isNotEmpty()) {
                        item(key = "fresh_finds") {
                            HomeSectionContainer() {
                                Column {
                                    FeedSectionHeader(
                                        title = stringResource(R.string.feed_fresh),
                                        subtitle = stringResource(R.string.feed_fresh_sub),
                                        actionText = stringResource(R.string.feed_play_all),
                                        actionIcon = Icons.Filled.PlayArrow,
                                        onActionClick = { viewModel.playTracksQueue(state.feedData.freshFinds, 0, "Fresh Finds") },
                                        onShuffleClick = { viewModel.shuffleTracksQueue(state.feedData.freshFinds, "Fresh Finds") },
                                    )
                                    FeedMediaRow(
                                        content = {
                                            itemsIndexed(state.feedData.freshFinds, key = { index, track -> "${track.videoId}#$index" }) { index, track ->
                                                FeedMediaCard(
                                                    title = track.title,
                                                    subtitle = ArtistHelper.primaryArtist(track.artist),
                                                    artworkUrl = track.artworkUrl,
                                                    fallbackIcon = Icons.Filled.Whatshot,
                                                    badgeText = "NEW",
                                                    onLongClick = {
                                                        miniTrayTrack = TrackMiniTrayData(
                                                            title = track.title,
                                                            artist = track.artist,
                                                            album = track.album,
                                                            artworkUrl = track.artworkUrl,
                                                            videoId = track.videoId.takeIf(String::isNotBlank),
                                                            sourceLabel = "Home",
                                                            onPlay = { viewModel.playTrack(track, "Fresh Finds") },
                                                        )
                                                    },
                                                    onClick = { viewModel.playTrack(track, "Fresh Finds") },
                                                    onPlayClick = { viewModel.playTrack(track, "Fresh Finds") },
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }

                    if (showSynthetic && isSectionVisible(HomeSection.SPOTLIGHT)) {
                    state.feedData.spotlight?.let { spotlight ->
                        item(key = "spotlight_hero") {
                            SpotlightHeroCard(
                                spotlight = spotlight,
                                onPlayRadio = {
                                    viewModel.playArtistRadio(FeedArtist(ArtistHelper.primaryArtist(spotlight.artistName), spotlight.browseId, spotlight.artworkUrl))
                                },
                                onOpenArtist = {
                                    artistAlbumNavigator.openArtist(ArtistHelper.primaryArtist(spotlight.artistName), spotlight.browseId ?: "")
                                },
                            )
                        }
                    }
                    }

                    if (showSynthetic && isSectionVisible(HomeSection.TOP_ARTISTS) && individualTopArtists.isNotEmpty()) {
                        item(key = "top_artists") {
                            HomeSectionContainer() {
                                Column {
                            FeedSectionHeader(
                                    title = stringResource(R.string.feed_artists),
                                    subtitle = stringResource(R.string.feed_artists_sub),
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.padding(top = 12.dp),
                                ) {
                                    itemsIndexed(individualTopArtists, key = { index, artist -> "${artist.name}#$index" }) { index, artist ->
                                        ArtistAvatarCard(
                                            artist = artist,
                                            isTop = index < 3,
                                            onClick = {
                                                artistAlbumNavigator.openArtist(
                                                    name = ArtistHelper.primaryArtist(artist.name),
                                                    browseId = artist.browseId ?: "",
                                                )
                                            },
                                        )
                                    }
                                }
                            
                                }
                            }
                        }
                        }

                    if (showSynthetic && isSectionVisible(HomeSection.HEAVY_ROTATION) && state.feedData.heavyRotation.isNotEmpty()) {
                        item(key = "heavy_rotation") {
                            HomeSectionContainer() {
                                Column {
                            FeedSectionHeader(
                                title = stringResource(R.string.feed_favorites),
                                subtitle = stringResource(R.string.feed_favorites_sub),
                                actionText = stringResource(R.string.feed_play_all),
                                actionIcon = Icons.Filled.PlayArrow,
                                onActionClick = { viewModel.playGeneratedQueue(state.feedData.heavyRotation, 0, "Heavy Rotation") },
                            )
                            FeedMediaRow(
                                content = {
                                    itemsIndexed(state.feedData.heavyRotation, key = { index, track -> "${track.videoId}#$index" }) { index, track ->
                                        FeedMediaCard(
                                            title = track.name,
                                            subtitle = ArtistHelper.primaryArtist(track.artist),
                                            artworkUrl = track.artworkUrl,
                                            fallbackIcon = Icons.Filled.MusicNote,
                                            onClick = { viewModel.playGeneratedQueue(state.feedData.heavyRotation, index, "Heavy Rotation") },
                                            onLongClick = {
                                                miniTrayTrack = TrackMiniTrayData(
                                                    title = track.name,
                                                    artist = track.artist,
                                                    artworkUrl = track.artworkUrl,
                                                    sourceLabel = "Home",
                                                    onPlay = { viewModel.playGeneratedQueue(state.feedData.heavyRotation, index, "Heavy Rotation") },
                                                )
                                            },
                                            onPlayClick = { viewModel.playGeneratedQueue(state.feedData.heavyRotation, index, "Heavy Rotation") },
                                        )
                                    }
                                },
                            )
                        
                                }
                            }
                        }
                    }

                    if (showSynthetic && isSectionVisible(HomeSection.ALBUMS) && state.feedData.recentAlbums.isNotEmpty()) {
                        item(key = "albums_in_rotation") {
                            HomeSectionContainer() {
                                Column {
                            FeedSectionHeader(
                                title = stringResource(R.string.feed_albums),
                                subtitle = stringResource(R.string.feed_albums_sub),
                            )
                            // Occurrence-suffixed keys: two album cards can
                            // share title+artist with a null browseId, and
                            // LazyRow crashes on duplicate keys.
                            val recentAlbumKeys = remember(state.feedData.recentAlbums) {
                                val seen = mutableMapOf<String, Int>()
                                state.feedData.recentAlbums.map { album ->
                                    val base = album.browseId ?: "${album.title}|${album.artist}"
                                    val n = seen.getOrDefault(base, 0)
                                    seen[base] = n + 1
                                    if (n == 0) base else "$base#$n"
                                }
                            }
                            FeedMediaRow(
                                content = {
                                    itemsIndexed(state.feedData.recentAlbums, key = { index, _ -> recentAlbumKeys[index] }) { _, album ->
                                        FeedMediaCard(
                                            title = album.title,
                                            subtitle = ArtistHelper.primaryArtist(album.artist),
                                            artworkUrl = album.artworkUrl,
                                            fallbackIcon = Icons.Filled.Album,
                                            onClick = {
                                                artistAlbumNavigator.openAlbum(
                                                    title = album.title,
                                                    artist = ArtistHelper.primaryArtist(album.artist),
                                                    browseId = album.browseId ?: "",
                                                )
                                            },
                                        )
                                    }
                                },
                            )
                        
                                }
                            }
                        }
                    }

                    if (showSynthetic && isSectionVisible(HomeSection.CHARTS) && state.feedData.charts.isNotEmpty()) {
                        item(key = "trending_charts") {
                            HomeSectionContainer() {
                                Column {
                            FeedSectionHeader(
                                title = stringResource(R.string.feed_trending),
                                subtitle = stringResource(R.string.feed_trending_sub),
                                actionText = stringResource(R.string.feed_play_all),
                                actionIcon = Icons.Filled.PlayArrow,
                                onActionClick = { viewModel.playTracksQueue(state.feedData.charts, 0, "Top Charts") },
                                onShuffleClick = { viewModel.shuffleTracksQueue(state.feedData.charts, "Top Charts") },
                            )
                            val charts = state.feedData.charts.take(15)
                            TrackPages(count = charts.size) { index ->
                                val track = charts[index]
                                TrackRow(
                                    title = track.title,
                                    subtitle = ArtistHelper.primaryArtist(track.artist),
                                    artworkUrl = track.artworkUrl,
                                    explicit = track.explicit,
                                    onClick = { viewModel.playTrack(track, "Top Charts") },
                                    onLongClick = {
                                        miniTrayTrack = TrackMiniTrayData(
                                            title = track.title,
                                            artist = track.artist,
                                            album = track.album,
                                            artworkUrl = track.artworkUrl,
                                            videoId = track.videoId.takeIf(String::isNotBlank),
                                            sourceLabel = "Home",
                                            onPlay = { viewModel.playTrack(track, "Top Charts") },
                                        )
                                    },
                                    trailing = {
                                        Text(
                                            text = (index + 1).toString(),
                                            style = AppType.mono,
                                            color = if (index < 3) MaterialTheme.tunedColors.accent else MaterialTheme.tunedColors.text2,
                                        )
                                    },
                                )
                            }
                        
                                }
                            }
                        }
                    }

                    if (showSynthetic && isSectionVisible(HomeSection.NEW_RELEASES) && state.feedData.newReleases.isNotEmpty()) {
                        item(key = "new_releases") {
                            HomeSectionContainer() {
                                Column {
                            FeedSectionHeader(
                                title = stringResource(R.string.feed_releases),
                                subtitle = stringResource(R.string.feed_releases_sub),
                                actionText = stringResource(R.string.feed_see_all),
                                actionIcon = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                onActionClick = onOpenNewReleases,
                            )
                            val newReleaseKeys = remember(state.feedData.newReleases) {
                                val seen = mutableMapOf<String, Int>()
                                state.feedData.newReleases.map { summary ->
                                    val n = seen.getOrDefault(summary.id, 0)
                                    seen[summary.id] = n + 1
                                    if (n == 0) summary.id else "${summary.id}#$n"
                                }
                            }
                            FeedMediaRow(
                                content = {
                                    itemsIndexed(state.feedData.newReleases, key = { index, _ -> newReleaseKeys[index] }) { _, summary ->
                                        FeedMediaCard(
                                            title = summary.title,
                                            subtitle = summary.author?.let(ArtistHelper::primaryArtist) ?: "Album",
                                            artworkUrl = summary.artworkUrl,
                                            fallbackIcon = Icons.Filled.NewReleases,
                                            badgeText = "NEW",
                                            onClick = {
                                                artistAlbumNavigator.openAlbum(
                                                    title = summary.title,
                                                    artist = summary.author?.let(ArtistHelper::primaryArtist) ?: "",
                                                    browseId = summary.id,
                                                )
                                            },
                                        )
                                    }
                                },
                            )
                        
                                }
                            }
                        }
                    }

                    if (!state.isChipActive) {
                        for ((shelfIndex, shelf) in state.pagedShelves.withIndex()) {
                            if (shelf.items.isEmpty()) continue
                            item(key = "paged_shelf_${shelfIndex}_${shelf.title}") {
                                HomeShelfCard(
                                    shelf = shelf,
                                    onPlayTrack = { track, title -> viewModel.playTrack(track, title) },
                                    onPlayTracksQueue = { tracks, idx, title -> viewModel.playTracksQueue(tracks, idx, title) },
                                    onShuffleTracksQueue = { tracks, title -> viewModel.shuffleTracksQueue(tracks, title) },
                                    onOpenFeedPlaylist = onOpenFeedPlaylist,
                                    onOpenAlbum = { title, artist, id -> artistAlbumNavigator.openAlbum(title, artist, id) },
                                    onTrackLongClick = { tray -> miniTrayTrack = tray },
                                )
                            }
                        }
                    }

                    if (state.isLoadingMore) {
                        item(key = "loading_more_indicator", contentType = "loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                ExpressiveInlineLoadingIndicator()
                            }
                        }
                    }

                    if (showSynthetic) {
                        item(key = "feed_footer") {
                            FeedFooter(lastUpdatedMillis = state.feedData.lastUpdatedMillis)
                        }
                    }
                }
            }
        }
    }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
                .adaptiveContentWidth(maxWidth = 600.dp)
                .safeHorizontalContentPadding()
                .padding(bottom = FloatingNavDefaults.contentBottomPadding()),
        )
    }


    menuTrack?.let { track ->
        TrackContextMenuSheet(
            target = TrackMenuTarget.Track(track.title, track.artist, ""),
            capabilities = TrackMenuCapabilities(showCopyActions = false, showDeleteScrobble = false),
            playableTrack = PlayableTrack(
                title = track.title,
                artist = track.artist,
                album = track.album,
                artworkUrl = track.artworkUrl,
                videoId = track.videoId.takeIf(String::isNotBlank),
            ),
            playbackSourceLabel = "Home",
            onDismiss = { menuTrack = null },
        )
    }

    miniTrayTrack?.let { tray ->
        TrackMiniTraySheet(
            data = tray,
            onDismiss = { miniTrayTrack = null },
        )
    }
}

private fun String.displayName(): String =
    split(" ", "-", "_").filter { it.isNotBlank() }.joinToString(" ") {
        it.replaceFirstChar { c -> c.uppercase() }
    }

private fun relativeTime(context: android.content.Context, uts: String?): String? {
    val epoch = uts?.toLongOrNull() ?: return null
    val now = System.currentTimeMillis() / 1000
    val diff = (now - epoch).coerceAtLeast(0)
    return when {
        diff < 3600 -> context.getString(R.string.feed_ago_min, (diff / 60).coerceAtLeast(1).toInt())
        diff < 86400 -> context.getString(R.string.feed_ago_hour, (diff / 3600).toInt())
        diff < 86400 * 7 -> context.getString(R.string.feed_ago_day, (diff / 86400).toInt())
        diff < 86400 * 30 -> context.getString(R.string.feed_ago_week, (diff / (86400 * 7)).toInt())
        else -> context.getString(R.string.feed_ago_month, (diff / (86400 * 30)).toInt())
    }
}


@Composable
private fun TasteStrip(
    tags: List<String>,
    launching: String?,
    onTagClick: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        FeedSectionHeader(
            title = stringResource(R.string.feed_your_sound),
            subtitle = stringResource(R.string.feed_your_sound_sub),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 10.dp),
        ) {
            items(tags.distinctBy { it }, key = { it }) { tag ->
                val isLoading = launching?.equals(tag.displayName(), ignoreCase = true) == true
                Surface(
                    onClick = { onTagClick(tag) },
                    // While one radio launches, the others used to stay
                    // tappable-looking and swallow taps in the ViewModel
                    // guard. Disable + dim them so the state is visible.
                    enabled = launching == null,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (launching == null || isLoading) 0.85f else 0.45f),
                    modifier = Modifier,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f),
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Text(
                            tag.displayName(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (isLoading) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.feed_start_radio_cd),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Horizontally scrolling row of YouTube Music mood/activity chips
 * (e.g., Relax, Energize, Workout, Commute, Focus).
 * Placed directly above the native YTM shelves using the app's liquid-glass style.
 * Adapted from Metrolist's Home screen chip row (GPL-3.0).
 */
@Composable
fun HomeChipsRow(
    chips: List<HomeChip>,
    selectedChip: HomeChip?,
    onChipClick: (HomeChip) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (chips.isEmpty()) return
    val haptics = LocalHapticFeedback.current
    LazyRow(
        contentPadding = PaddingValues(horizontal = Keyline.margin),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(chips.distinctBy { it.title }, key = { it.title }) { chip ->
            val isSelected = chip == selectedChip
            Surface(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onChipClick(chip)
                },
                shape = CircleShape,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    // Translucent so the header panel's tint shows through.
                    MaterialTheme.tunedColors.text1.copy(alpha = 0.10f)
                },
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)
                    },
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = chip.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
    }
}

/**
 * Pinned container for the YTM chip row. Sits directly under the app bar and
 * stays visible while the feed scrolls, with a subtle gradient scrim behind
 * it so content sliding underneath stays legible. Liquid-glass styled.
 */
@Composable
private fun PinnedChipsBar(
    chips: List<HomeChip>,
    selectedChip: HomeChip?,
    onChipClick: (HomeChip) -> Unit,
) {
    HomeChipsRow(
        chips = chips,
        selectedChip = selectedChip,
        onChipClick = onChipClick,
        modifier = Modifier.padding(bottom = Space.lg),
    )
}

/**
 * Reusable surface card for a YouTube Music carousel shelf.
 * Renders each item through the card system (square / wide / featured /
 * community) chosen from the item's real YTM data, falling back to square.
 * Used for page-1 native shelves, continuation paged shelves, and chip shelves.
 */
@Composable
private fun HomeShelfCard(
    shelf: HomeShelf,
    onPlayTrack: (YouTubeMusicTrack, String) -> Unit,
    onPlayTracksQueue: (List<YouTubeMusicTrack>, Int, String) -> Unit,
    onShuffleTracksQueue: (List<YouTubeMusicTrack>, String) -> Unit,
    onOpenFeedPlaylist: (String) -> Unit,
    onOpenAlbum: (title: String, artist: String, browseId: String) -> Unit,
    onTrackLongClick: (TrackMiniTrayData) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (shelf.items.isEmpty()) return
    HomeSectionContainer(modifier) {
                                Column {
            val tracksInShelf = remember(shelf.items) {
                shelf.items.mapNotNull { (it as? HomeShelfItem.Track)?.track }
            }
            // "More" only when the shelf's destination is a playlist/album id
            // the app already knows how to open; mood/genre/chart browse ids
            // aren't supported surfaces, so the chevron stays hidden for them.
            val morePlaylistId = remember(shelf.moreBrowseId) {
                shelf.moreBrowseId
                    ?.removePrefix("VL")
                    ?.takeIf { it.startsWith("PL") || it.startsWith("MPRE") || it.startsWith("RD") || it.startsWith("OLAK") }
            }
            FeedSectionHeader(
                title = shelf.title,
                subtitle = shelfSubtitle(shelf.subtitle, shelf.strapline),
                actionText = if (morePlaylistId != null) stringResource(R.string.feed_see_all) else null,
                onActionClick = morePlaylistId?.let { { onOpenFeedPlaylist(it) } },
                onShuffleClick = if (tracksInShelf.isNotEmpty()) {
                    { onShuffleTracksQueue(tracksInShelf, shelf.title) }
                } else null,
            )
            FeedMediaRow(
                content = {
                    itemsIndexed(
                        shelf.items,
                        key = { _, item ->
                            when (item) {
                                is HomeShelfItem.Track -> "t_${item.track.videoId}"
                                is HomeShelfItem.Playlist -> "p_${item.playlist.id}"
                            }
                        },
                    ) { _, shelfItem ->
                        Box(Modifier.animateItem()) {
                            NativeShelfItemCard(
                                shelfItem = shelfItem,
                                shelfTitle = shelf.title,
                                onPlayTrack = onPlayTrack,
                                onOpenFeedPlaylist = onOpenFeedPlaylist,
                                onOpenAlbum = onOpenAlbum,
                                onTrackLongClick = onTrackLongClick,
                            )
                        }
                    }
                },
            )
        }
    }
}

/**
 * YouTube often sends the same line as both strapline and subtitle, and in
 * capitals. Show one line, in sentence case.
 */
private fun shelfSubtitle(subtitle: String?, strapline: String?): String? {
    val line = subtitle?.takeIf { it.isNotBlank() } ?: strapline?.takeIf { it.isNotBlank() } ?: return null
    val trimmed = line.trim().trimEnd('!')
    return if (trimmed.any { it.isLetter() } && trimmed == trimmed.uppercase()) {
        trimmed.lowercase().replaceFirstChar { it.titlecase() }
    } else trimmed
}

/**
 * Dispatches a single shelf item to its card variant based on the real YTM
 * data (aspect ratio, caption, community preview songs). Unknown shapes fall
 * back to the square card. Calls the same play/open/menu handlers as before.
 */
@Composable
private fun NativeShelfItemCard(
    shelfItem: HomeShelfItem,
    shelfTitle: String,
    onPlayTrack: (YouTubeMusicTrack, String) -> Unit,
    onOpenFeedPlaylist: (String) -> Unit,
    onOpenAlbum: (title: String, artist: String, browseId: String) -> Unit,
    onTrackLongClick: (TrackMiniTrayData) -> Unit,
) {
    when (val variant = com.tuned.app.ui.feed.cards.selectCardVariant(shelfItem)) {
        com.tuned.app.ui.feed.cards.HomeCardVariant.WIDE -> WideItemCard(
            shelfItem = shelfItem,
            shelfTitle = shelfTitle,
            onPlayTrack = onPlayTrack,
            onOpenFeedPlaylist = onOpenFeedPlaylist,
            onOpenAlbum = onOpenAlbum,
            onTrackLongClick = onTrackLongClick,
        )
        com.tuned.app.ui.feed.cards.HomeCardVariant.FEATURED -> FeaturedItemCard(
            shelfItem = shelfItem,
            shelfTitle = shelfTitle,
            onPlayTrack = onPlayTrack,
            onOpenFeedPlaylist = onOpenFeedPlaylist,
            onOpenAlbum = onOpenAlbum,
            onTrackLongClick = onTrackLongClick,
        )
        com.tuned.app.ui.feed.cards.HomeCardVariant.COMMUNITY -> {
            val item = shelfItem as? HomeShelfItem.Playlist ?: return
            CommunityItemCard(
                item = item,
                shelfTitle = shelfTitle,
                onPlayTrack = onPlayTrack,
                onOpenFeedPlaylist = onOpenFeedPlaylist,
                onOpenAlbum = onOpenAlbum,
                onTrackLongClick = onTrackLongClick,
            )
        }
        else -> SquareItemCard(
            shelfItem = shelfItem,
            shelfTitle = shelfTitle,
            onPlayTrack = onPlayTrack,
            onOpenFeedPlaylist = onOpenFeedPlaylist,
            onOpenAlbum = onOpenAlbum,
            onTrackLongClick = onTrackLongClick,
        )
    }
}

/** Builds the track mini-tray payload used by every card's long-press. */
private fun trackTray(track: YouTubeMusicTrack, shelfTitle: String, onPlayTrack: (YouTubeMusicTrack, String) -> Unit) =
    TrackMiniTrayData(
        title = track.title,
        artist = track.artist,
        album = track.album,
        artworkUrl = track.artworkUrl,
        videoId = track.videoId.takeIf(String::isNotBlank),
        sourceLabel = "Home",
        onPlay = { onPlayTrack(track, shelfTitle) },
    )

private fun playlistInfoLine(p: com.tuned.app.data.music.YouTubePlaylistSummary): String =
    p.author?.takeIf { it.isNotBlank() } ?: p.trackCountText.orEmpty()

private fun openPlaylistOrAlbum(
    playlist: com.tuned.app.data.music.YouTubePlaylistSummary,
    onOpenFeedPlaylist: (String) -> Unit,
    onOpenAlbum: (title: String, artist: String, browseId: String) -> Unit,
) {
    if (playlist.id.startsWith("MPRE")) {
        onOpenAlbum(playlist.title, playlist.author.orEmpty(), playlist.id)
    } else {
        onOpenFeedPlaylist(playlist.id)
    }
}

@Composable
private fun SquareItemCard(
    shelfItem: HomeShelfItem,
    shelfTitle: String,
    onPlayTrack: (YouTubeMusicTrack, String) -> Unit,
    onOpenFeedPlaylist: (String) -> Unit,
    onOpenAlbum: (title: String, artist: String, browseId: String) -> Unit,
    onTrackLongClick: (TrackMiniTrayData) -> Unit,
) {
    when (shelfItem) {
        is HomeShelfItem.Track -> {
            val track = shelfItem.track
            com.tuned.app.ui.feed.cards.ShelfSquareCard(
                title = track.title,
                infoLine = ArtistHelper.primaryArtist(track.artist),
                artworkUrl = track.artworkUrl,
                fallbackIcon = Icons.Filled.MusicNote,
                onClick = { onPlayTrack(track, shelfTitle) },
                onLongClick = { onTrackLongClick(trackTray(track, shelfTitle, onPlayTrack)) },
                onPlayClick = { onPlayTrack(track, shelfTitle) },
                cardWidth = com.tuned.app.ui.feed.cards.rememberSquareCardWidth(),
            )
        }
        is HomeShelfItem.Playlist -> {
            val p = shelfItem.playlist
            com.tuned.app.ui.feed.cards.ShelfSquareCard(
                title = p.title,
                infoLine = playlistInfoLine(p),
                artworkUrl = p.artworkUrl,
                fallbackIcon = Icons.AutoMirrored.Filled.QueueMusic,
                onClick = { openPlaylistOrAlbum(p, onOpenFeedPlaylist, onOpenAlbum) },
                cardWidth = com.tuned.app.ui.feed.cards.rememberSquareCardWidth(),
            )
        }
    }
}

@Composable
private fun WideItemCard(
    shelfItem: HomeShelfItem,
    shelfTitle: String,
    onPlayTrack: (YouTubeMusicTrack, String) -> Unit,
    onOpenFeedPlaylist: (String) -> Unit,
    onOpenAlbum: (title: String, artist: String, browseId: String) -> Unit,
    onTrackLongClick: (TrackMiniTrayData) -> Unit,
) {
    when (shelfItem) {
        is HomeShelfItem.Track -> {
            val track = shelfItem.track
            com.tuned.app.ui.feed.cards.ShelfWideCard(
                title = track.title,
                infoLine = ArtistHelper.primaryArtist(track.artist),
                artworkUrl = track.artworkUrl,
                onClick = { onPlayTrack(track, shelfTitle) },
                onLongClick = { onTrackLongClick(trackTray(track, shelfTitle, onPlayTrack)) },
                onPlayClick = { onPlayTrack(track, shelfTitle) },
                cardWidth = com.tuned.app.ui.feed.cards.rememberWideCardWidth(),
            )
        }
        is HomeShelfItem.Playlist -> {
            val p = shelfItem.playlist
            com.tuned.app.ui.feed.cards.ShelfWideCard(
                title = p.title,
                infoLine = playlistInfoLine(p),
                artworkUrl = p.artworkUrl,
                fallbackIcon = Icons.AutoMirrored.Filled.QueueMusic,
                onClick = { openPlaylistOrAlbum(p, onOpenFeedPlaylist, onOpenAlbum) },
                cardWidth = com.tuned.app.ui.feed.cards.rememberWideCardWidth(),
            )
        }
    }
}

@Composable
private fun FeaturedItemCard(
    shelfItem: HomeShelfItem,
    shelfTitle: String,
    onPlayTrack: (YouTubeMusicTrack, String) -> Unit,
    onOpenFeedPlaylist: (String) -> Unit,
    onOpenAlbum: (title: String, artist: String, browseId: String) -> Unit,
    onTrackLongClick: (TrackMiniTrayData) -> Unit,
) {
    when (shelfItem) {
        is HomeShelfItem.Track -> {
            val track = shelfItem.track
            com.tuned.app.ui.feed.cards.ShelfFeaturedCard(
                title = track.title,
                artist = ArtistHelper.primaryArtist(track.artist),
                artworkUrl = track.artworkUrl,
                caption = shelfItem.caption,
                onClick = { onPlayTrack(track, shelfTitle) },
                onLongClick = { onTrackLongClick(trackTray(track, shelfTitle, onPlayTrack)) },
                cardWidth = com.tuned.app.ui.feed.cards.rememberFeaturedCardWidth(),
            )
        }
        is HomeShelfItem.Playlist -> {
            val p = shelfItem.playlist
            com.tuned.app.ui.feed.cards.ShelfFeaturedCard(
                title = p.title,
                artist = playlistInfoLine(p),
                artworkUrl = p.artworkUrl,
                caption = shelfItem.caption,
                fallbackIcon = Icons.AutoMirrored.Filled.QueueMusic,
                onClick = { openPlaylistOrAlbum(p, onOpenFeedPlaylist, onOpenAlbum) },
                cardWidth = com.tuned.app.ui.feed.cards.rememberFeaturedCardWidth(),
            )
        }
    }
}

@Composable
private fun CommunityItemCard(
    item: HomeShelfItem.Playlist,
    shelfTitle: String,
    onPlayTrack: (YouTubeMusicTrack, String) -> Unit,
    onOpenFeedPlaylist: (String) -> Unit,
    onOpenAlbum: (title: String, artist: String, browseId: String) -> Unit,
    onTrackLongClick: (TrackMiniTrayData) -> Unit,
) {
    val p = item.playlist
    com.tuned.app.ui.common.MediaTile(
        title = p.title,
        subtitle = playlistInfoLine(p).takeIf { it.isNotBlank() },
        artworkUrl = p.artworkUrl ?: item.collageArtworkUrls.firstOrNull(),
        onClick = { openPlaylistOrAlbum(p, onOpenFeedPlaylist, onOpenAlbum) },
    )
}

/**
 * Home's shape while it loads, on Home's own spacing: Quick picks (title, subtitle, tall cards),
 * then two shelves of square tiles. The real feed lands in the same places.
 */
@Composable
internal fun FeedLoadingSkeleton() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = false,
        contentPadding = PaddingValues(top = Space.xl, bottom = FloatingNavDefaults.contentBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(Space.xxl),
    ) {
        item { SkeletonShelf(tall = true) }
        items(2) { SkeletonShelf(tall = false) }
    }
}

@Composable
private fun SkeletonShelf(tall: Boolean) {
    val colors = MaterialTheme.tunedColors
    val pulse = com.tuned.app.ui.common.skeletonAlpha()
    Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
        Column(
            Modifier.padding(horizontal = Keyline.margin).alpha(pulse),
            verticalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            Box(Modifier.size(width = Sizes.tileSquare, height = Sizes.skeletonTitle * 2).clip(AppShape.xs).background(colors.surface2))
            Box(Modifier.size(width = Sizes.tileTallWidth, height = Sizes.skeletonSubtitle).clip(AppShape.xs).background(colors.surface2))
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = Keyline.margin),
            horizontalArrangement = Arrangement.spacedBy(Space.md),
            userScrollEnabled = false,
        ) {
            items(3) {
                if (tall) {
                    Box(
                        Modifier
                            .size(width = Sizes.tileTallWidth, height = Sizes.tileTallHeight)
                            .alpha(pulse)
                            .clip(AppShape.md)
                            .background(colors.surface2),
                    )
                } else {
                    com.tuned.app.ui.common.SkeletonTile()
                }
            }
        }
    }
}

/** The header's chip row while Home loads, so the header keeps its height when chips arrive. */
@Composable
internal fun HomeChipsSkeleton() {
    val colors = MaterialTheme.tunedColors
    val pulse = com.tuned.app.ui.common.skeletonAlpha()
    LazyRow(
        modifier = Modifier.padding(bottom = Space.lg).alpha(pulse),
        contentPadding = PaddingValues(horizontal = Keyline.margin),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        userScrollEnabled = false,
    ) {
        items(listOf(Sizes.tileArtist, Sizes.tileSquare, Sizes.tileArtist, Sizes.tileSquare)) { width ->
            Box(
                Modifier
                    .size(width = width, height = Sizes.touch - Space.sm - Space.xxs)
                    .clip(AppShape.full)
                    .background(colors.text1.copy(alpha = 0.1f)),
            )
        }
    }
}

@Composable
private fun FeedFooter(lastUpdatedMillis: Long) {
    val context = LocalContext.current
    val label = remember(lastUpdatedMillis) {
        if (lastUpdatedMillis <= 0L) context.getString(R.string.feed_footer_default)
        else try {
            val time = java.time.Instant.ofEpochMilli(lastUpdatedMillis)
                .atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))
            context.getString(R.string.feed_footer_updated, time)
        } catch (_: Exception) {
            context.getString(R.string.feed_footer_default)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
        )
    }
}



@Composable
private fun QuickTilesGrid(
    tiles: List<FeedQuickTile>,
    onTileClick: (FeedQuickTile) -> Unit,
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(tiles) { tile ->
            QuickTileCard(
                tile = tile,
                onClick = { onTileClick(tile) },
            )
        }
    }
}

@Composable
private fun QuickTileCard(
    tile: FeedQuickTile,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val isYtLikedTile = tile.collection == "yt_liked" || tile.playlistId == "yt_liked"
    val isLikedTile = isYtLikedTile || tile.isLiked
    val isMixTile = tile.collection == "mix" || tile.collection == "discover_mix" || tile.title.contains("Discover Mix", ignoreCase = true)
    val isMyMixTile = tile.collection == "my_mix" || tile.title.equals("My Mix", ignoreCase = true)
    val isDiscoverTile = tile.collection == "radio" || tile.title.equals("Discover", ignoreCase = true)
    val isNewReleasesTile = tile.collection == "new_releases"
    // Built-in destinations get one plain icon on a neutral square; anything
    // else shows its own artwork. Same geometry as MediaTile.
    val icon: ImageVector? = when {
        isYtLikedTile -> Icons.Filled.ThumbUp
        isLikedTile -> Icons.Filled.Favorite
        isMixTile -> Icons.AutoMirrored.Filled.QueueMusic
        isMyMixTile -> Icons.Filled.AllInclusive
        isDiscoverTile -> Icons.Filled.Explore
        isNewReleasesTile -> Icons.Filled.NewReleases
        else -> null
    }
    val subtitle = if (isNewReleasesTile) stringResource(R.string.feed_releases_sub) else tile.subtitle ?: if (tile.actionVideoId != null) "Track" else "Playlist"
    val colors = MaterialTheme.tunedColors
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier
            .width(Sizes.tileSquare)
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        Box(
            Modifier.size(Sizes.tileSquare).clip(AppShape.sm).background(colors.surface2),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(Sizes.iconHero))
            } else {
                ArtworkImage(
                    name = tile.title,
                    artist = tile.subtitle ?: "",
                    embeddedUrl = tile.artworkUrl,
                    fallbackIcon = Icons.Filled.Album,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.height(Space.sm))
        Text(tile.title, style = AppType.body, color = colors.text1, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(Space.xxs))
        Text(subtitle, style = AppType.bodyS, color = colors.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickPicksRows(
    tracks: List<YouTubeMusicTrack>,
    currentPlayingVideoId: String?,
    isPlaying: Boolean,
    onTrackClick: (Int) -> Unit,
    onMenuClick: (YouTubeMusicTrack) -> Unit,
    onLongClick: (YouTubeMusicTrack, Int) -> Unit = { _, _ -> },
) {
    // Experiment: Apple Music style tall cards (QuickPickCard) instead of
    // pages of track rows. The menu stays on long press.
    LazyRow(
        contentPadding = PaddingValues(horizontal = Keyline.margin),
        horizontalArrangement = Arrangement.spacedBy(Space.md),
        modifier = Modifier.padding(top = Space.md),
    ) {
        itemsIndexed(tracks, key = { index, track -> track.videoId.ifBlank { "pick_$index" } }) { index, track ->
            QuickPickCard(
                track = track,
                isCurrent = track.videoId.isNotBlank() && track.videoId == currentPlayingVideoId,
                isPlaying = isPlaying,
                onClick = { onTrackClick(index) },
                onLongClick = { onLongClick(track, index) },
            )
        }
    }
}

/**
 * Track lists on Home (Quick picks, Trending): pages of three standard
 * [TrackRow]s that snap as you swipe. The next page peeks in at the edge so
 * it reads as scrollable.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackPages(
    count: Int,
    rowsPerPage: Int = 3,
    row: @Composable (index: Int) -> Unit,
) {
    val pages = (count + rowsPerPage - 1) / rowsPerPage
    val widthFraction = when (rememberWindowSizeClass()) {
        WindowSizeClass.COMPACT -> if (pages > 1) 0.88f else 1f
        WindowSizeClass.MEDIUM -> 0.5f
        WindowSizeClass.EXPANDED -> 0.34f
    }
    val listState = rememberLazyListState()
    LazyRow(
        state = listState,
        flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(listState),
        modifier = Modifier.padding(top = Space.sm),
    ) {
        items(pages) { page ->
            Column(Modifier.fillParentMaxWidth(widthFraction)) {
                for (index in page * rowsPerPage until minOf(count, (page + 1) * rowsPerPage)) row(index)
            }
        }
    }
}

@Composable
private fun FeedMediaRow(
    content: LazyListScope.() -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Keyline.margin),
        horizontalArrangement = Arrangement.spacedBy(Space.md),
        modifier = Modifier.padding(top = Space.md),
        content = content,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeedMediaCard(
    title: String,
    subtitle: String,
    artworkUrl: String?,
    fallbackIcon: ImageVector,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onPlayClick: (() -> Unit)? = null,
    badgeText: String? = null,
    cardWidth: androidx.compose.ui.unit.Dp = Sizes.tileSquare,
) {
    // One tile for every shelf. The badge / separate play affordance /
    // responsive width parameters used to be accepted here and silently
    // dropped; MediaTile renders all three now.
    com.tuned.app.ui.common.MediaTile(
        title = title,
        subtitle = subtitle.takeIf { it.isNotBlank() },
        artworkUrl = artworkUrl,
        onClick = onClick,
        onLongClick = onLongClick,
        width = cardWidth,
        badgeText = badgeText,
        onPlayClick = onPlayClick,
    )
}

@Composable
private fun FeedPlaylistCard(
    title: String,
    subtitle: String,
    artworkUrl: String?,
    onClick: () -> Unit,
    onPlayClick: (() -> Unit)? = null,
) {
    FeedMediaCard(
        title = title,
        subtitle = subtitle,
        artworkUrl = artworkUrl,
        fallbackIcon = Icons.Filled.Album,
        onClick = onClick,
        onPlayClick = onPlayClick,
        cardWidth = 156.dp,
    )
}

// Legacy card variants were consolidated into FeedMediaCard / FeedPlaylistCard
// so every shelf shares one artwork, type and play-affordance language.

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentTrackCard(
    track: RecentTrack,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val ago = remember(track.date?.uts, track.url) { relativeTime(context, track.date?.uts) }
    val primaryArtistName = remember(track.artist.displayName) { ArtistHelper.primaryArtist(track.artist.displayName) }
    com.tuned.app.ui.common.MediaTile(
        title = track.name,
        subtitle = primaryArtistName,
        artworkUrl = track.artworkUrl,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChartTrackCard(
    rank: Int,
    track: YouTubeMusicTrack,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val isTop3 = rank <= 3
    val haptics = LocalHapticFeedback.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isTop3) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isTop3) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        ),
        modifier = Modifier
            .width(288.dp)
            .height(76.dp)
            .combinedClickable(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = onLongClick?.let {
                    {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        it()
                    }
                },
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Text(
                text = String.format("%02d", rank),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                fontWeight = FontWeight.Black,
                color = if (isTop3) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.width(40.dp),
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(56.dp),
            ) {
                Box(Modifier.fillMaxSize()) {
                    ArtworkImage(
                        name = track.title,
                        artist = ArtistHelper.primaryArtist(track.artist),
                        embeddedUrl = track.artworkUrl,
                        fallbackIcon = Icons.Filled.TrendingUp,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(3.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = ArtistHelper.primaryArtist(track.artist),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun SpotlightHeroCard(
    spotlight: FeedSpotlight,
    onPlayRadio: () -> Unit,
    onOpenArtist: () -> Unit,
) {
    val primaryArtistName = remember(spotlight.artistName) { ArtistHelper.primaryArtist(spotlight.artistName) }
    FeatureCard(
        eyebrow = stringResource(R.string.feed_spotlight_eyebrow),
        title = primaryArtistName,
        subtitle = spotlight.topTrackTitle?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.feed_spotlight_sub, it) },
        artworkUrl = spotlight.artworkUrl,
        onPlay = onPlayRadio,
        onClick = onOpenArtist,
    )
}

@Composable
private fun ArtistAvatarCard(
    artist: FeedArtist,
    onClick: () -> Unit,
    isTop: Boolean = false,
) {
    val primaryArtistName = remember(artist.name) { ArtistHelper.primaryArtist(artist.name) }
    com.tuned.app.ui.common.MediaTile(
        title = primaryArtistName,
        artworkUrl = artist.artworkUrl,
        artistName = primaryArtistName,
        onClick = onClick,
        shape = com.tuned.app.ui.common.MediaTileShape.Circle,
        width = Sizes.tileArtist,
    )
}

@Composable
private fun FriendAvatarCard(
    friend: FriendEntry,
    onClick: () -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(60.dp),
        ) {
            if (!friend.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = friend.avatarUrl,
                    contentDescription = friend.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = friend.displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = friend.displayName,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

@Composable
private fun FeedSectionHeader(
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    actionIcon: ImageVector? = null,
    onActionClick: (() -> Unit)? = null,
    onShuffleClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.tunedColors
    val haptics = rememberHaptics()
    val hasAction = actionText != null && onActionClick != null
    // A text action's label ends on the margin (its 12dp inner padding sits
    // outside it); a lone shuffle icon's glyph ends on the margin.
    val endPadding = when {
        hasAction -> Keyline.margin - Space.md
        onShuffleClick != null -> rowIconEdgeInset
        else -> Keyline.margin
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Keyline.margin, end = endPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
            Text(
                title,
                style = AppType.titleL,
                color = colors.text1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = AppType.bodyS, color = colors.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (onShuffleClick != null) {
            AppIconButton(
                icon = Icons.Filled.Shuffle,
                contentDescription = stringResource(R.string.feed_shuffle_title_cd, title),
                onClick = {
                    haptics.perform(HapticKind.Confirm)
                    onShuffleClick()
                },
                iconSize = Sizes.iconRow,
                tint = colors.text2,
            )
        }
        if (hasAction) {
            Box(
                Modifier
                    .heightIn(min = Sizes.touch)
                    .clickable(role = androidx.compose.ui.semantics.Role.Button) {
                        haptics.perform(HapticKind.Confirm)
                        onActionClick?.invoke()
                    }
                    .padding(horizontal = Space.md),
                contentAlignment = Alignment.Center,
            ) {
                Text(actionText!!, style = AppType.label, color = colors.accent)
            }
        }
    }
}

@Composable
private fun FeedEmptyState(
    message: String,
    onRetry: () -> Unit,
    onOpenSearch: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeHorizontalContentPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = FloatingNavDefaults.contentBottomPadding())
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            modifier = Modifier.size(72.dp),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.feed_empty_title),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onOpenSearch,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.feed_search_music), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            Surface(
                onClick = onRetry,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
                modifier = Modifier,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Try again",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class ArtistAlbumNavBridgeFeed @javax.inject.Inject constructor(val navigator: ArtistAlbumNavigator) : androidx.lifecycle.ViewModel()

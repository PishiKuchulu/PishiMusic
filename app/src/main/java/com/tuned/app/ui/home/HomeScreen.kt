package com.tuned.app.ui.home

import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Sync
import androidx.compose.foundation.BorderStroke
import com.tuned.app.ui.theme.tunedColors
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Download
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.tuned.app.R
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.tuned.app.playback.toPlayableTrack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.text.style.TextOverflow
import com.tuned.app.ui.common.ArtworkImage
import com.tuned.app.ui.common.ExpressiveHeader
import com.tuned.app.ui.common.HeaderActionIcon
import com.tuned.app.ui.common.safeHorizontalContentPadding
import com.tuned.app.ui.common.adaptiveContentWidth
import com.tuned.app.ui.common.TrackContextMenuSheet
import com.tuned.app.ui.common.TrackMenuCapabilities
import com.tuned.app.ui.common.TrackMenuTarget
import com.tuned.app.ui.common.TrackMiniTrayData
import com.tuned.app.ui.common.TrackMiniTraySheet
import com.tuned.app.ui.theme.ArtworkShape
import com.tuned.app.ui.theme.BadgePillShape
import com.tuned.app.ui.theme.ExpressiveHeroShape
import com.tuned.app.ui.theme.ExpressivePillShape
import com.tuned.app.ui.theme.HeroInnerShape
import com.tuned.app.ui.theme.ListContainerShape
import com.tuned.app.ui.theme.NowPlayingCardShape
import com.tuned.app.ui.theme.StatPillShape
import com.tuned.app.ui.theme.TrackRowShape
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically

import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import com.tuned.app.ui.shell.FloatingNavDefaults
import coil.compose.SubcomposeAsyncImage
import com.tuned.app.data.repository.HomeAlbum
import com.tuned.app.data.repository.HomeArtistItem
import com.tuned.app.data.repository.HomeSortMode
import com.tuned.app.data.repository.HomeTrack
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

// Immutable shapes hoisted out of composition: previously each of these was
// constructed inline inside row/card composables, i.e. re-allocated for every
// row on every recomposition. Rows are the hottest path while scrolling, so
// they must not allocate.
private val ListContainerShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
private val TrackRowShape = RoundedCornerShape(18.dp)
private val NowPlayingCardShape = RoundedCornerShape(22.dp)
private val ArtworkShape = RoundedCornerShape(14.dp)
private val BadgePillShape = RoundedCornerShape(50)
private val StatPillShape = RoundedCornerShape(20.dp)
private val HeroInnerShape = RoundedCornerShape(24.dp)

/**
 * Faithful port of home.html/home.js's layout, top to bottom:
 *  1. Header row — username pill (left) + live listen timer (right)
 *  2. Stats card — big "Scrobbles" number + arrow-to-Genres, then a
 *     Tracks / Artists / Albums row
 *  3. Mix card — "List" title + sort dropdown (Recent / Most Played /
 *     Last 7 Days / Last 30 Days), then the track list itself, with the
 *     Now Playing row always pinned first when present.
 * There's no separate "Now Playing card" — that was an earlier, simplified
 * substitute; the real app renders it as the first row of the same list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenGenres: () -> Unit,
    onOpenFriends: () -> Unit,
    onOpenDownloads: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenRecap: () -> Unit = {},
    onOpenYouTubeAccount: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    artistAlbumNavigator: com.tuned.app.ui.navigation.ArtistAlbumNavigator = hiltViewModel<ArtistAlbumNavBridgeHome>().navigator,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.pollWhileActive()
        }
    }

    var menuTrack by remember { mutableStateOf<HomeTrack?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ExpressiveHeader(
                    title = stringResource(R.string.nav_you),
                    modifier = Modifier.adaptiveContentWidth(maxWidth = 860.dp),
                    actions = {
                        HeaderActionIcon(Icons.Filled.Download, "Downloads", onOpenDownloads)
                        // A plain gear, the same as Home's header.
                        HeaderActionIcon(Icons.Filled.Settings, "Settings", onOpenSettings)
                    },
                    foldContentOnScroll = true,
                ) {
                    // Who and how long, inside the panel like Home's chips.
                    val ytAccountName by viewModel.ytAccountName.collectAsStateWithLifecycle()
                    HeaderRow(
                        displayUsername = when {
                            uiState.isViewingFriend -> uiState.viewingUsername
                            uiState.username.isNotBlank() -> uiState.username
                            // No Last.fm: the connected YouTube account, then Guest.
                            uiState.isLocalStatsMode -> ytAccountName ?: "Guest"
                            else -> uiState.username
                        },
                        isViewingFriend = uiState.isViewingFriend,
                        // Friends is a Last.fm feature. Without Last.fm the pill is your YouTube
                        // account (or Guest), so it opens the YouTube account settings instead.
                        onClick = if (!uiState.isLocalStatsMode || uiState.isViewingFriend) onOpenFriends else onOpenYouTubeAccount,
                        viewModel = viewModel,
                    )
                }
            }
        },
    ) { scaffoldPadding ->
        if (uiState.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(scaffoldPadding).safeHorizontalContentPadding(),
                contentAlignment = Alignment.Center,
            ) {
                com.tuned.app.ui.common.ExpressiveLoadingIndicator(message = "Loading your listening history")
            }
            return@Scaffold
        }

        // A failed load with nothing to show is an error, not an empty
        // account: without this branch an offline first load collapsed into
        // the same "no tracks" text as a genuinely empty history.
        if (uiState.error != null && uiState.allTracks.isEmpty() && uiState.nowPlaying == null) {
            Box(
                Modifier.fillMaxSize().padding(scaffoldPadding).safeHorizontalContentPadding(),
                contentAlignment = Alignment.Center,
            ) {
                com.tuned.app.ui.common.ErrorState(
                    message = uiState.error!!,
                    onRetry = {
                        viewModel.dismissError()
                        viewModel.refresh()
                    },
                )
            }
            return@Scaffold
        }

        Box(
            modifier = Modifier.fillMaxSize().padding(scaffoldPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .adaptiveContentWidth(maxWidth = 860.dp)
                    .safeHorizontalContentPadding(),
            ) {
            val pullState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = pullState,
                indicator = {
                    com.tuned.app.ui.common.RefreshIndicator(pullState, uiState.isRefreshing, Modifier.align(Alignment.TopCenter))
                },
            ) {
                val listState = rememberLazyListState()
                // A preview: the latest songs only. The full, sortable, searchable list is the
                // History screen behind the heading's chevron.
                val rows = remember(uiState.allTracks, uiState.topTracksOverall, uiState.nowPlaying) { recentHistoryRows(uiState) }
                val playbackQueue = remember(rows) {
                    rows.mapNotNull { row ->
                        (row as? HomeRow.Track)?.track?.let { track ->
                            com.tuned.app.playback.PlayableTrack(
                                title = track.name,
                                artist = track.artist,
                                artworkUrl = track.artworkUrl,
                                videoId = track.videoId,
                            )
                        }
                    }
                }
                val playbackIndexByRow = remember(rows) {
                    var nextPlaybackIndex = 0
                    IntArray(rows.size) { rowIndex ->
                        if (rows[rowIndex] is HomeRow.Track) nextPlaybackIndex++ else -1
                    }
                }
                val musicPlayer = com.tuned.app.ui.player.LocalMusicPlayer.current
                // Long-press (deep press) on a track row opens the mini tray;
                // the 3-dot button keeps opening the full menu sheet.
                var miniTrayRowIndex by remember { mutableStateOf<Int?>(null) }

                // One scrolling page: the prompt, the stats and History scroll together, with no
                // card around the list (it was cards inside cards, and only the list scrolled).
                Box(Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            // Rows carry 8dp of their own, so artwork lands on the 16dp margin.
                            start = 8.dp,
                            end = 8.dp,
                            top = 16.dp,
                            bottom = FloatingNavDefaults.contentBottomPadding(),
                        ),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        uiState.stats?.let { stats ->
                            item(key = "stats", contentType = "stats") {
                                StatsCard(
                                    scrobbles = stats.scrobbles,
                                    trackCount = stats.trackCount,
                                    artistCount = stats.artistCount,
                                    albumCount = stats.albumCount,
                                    // Honest label: local Room aggregates are plays, not global scrobbles.
                                    headlineLabel = if (uiState.isLocalStatsMode && !uiState.isViewingFriend) "Plays" else "Scrobbles",
                                    onOpenGenres = onOpenGenres,
                                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 16.dp),
                                )
                            }
                        }
                        if (!uiState.isViewingFriend) {
                            item(key = "recap", contentType = "recap") {
                                com.tuned.app.ui.recap.RecapEntryTile(
                                    onClick = onOpenRecap,
                                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                                )
                            }
                        }
                        item(key = "historyHeader", contentType = "historyHeader") {
                            HistorySectionTitle(onClick = onOpenHistory, modifier = Modifier.padding(horizontal = 8.dp))
                        }
                        historyItems(
                            rows = rows,
                            onPlay = { rowIndex ->
                                musicPlayer.playQueue(
                                    tracks = playbackQueue,
                                    startIndex = playbackIndexByRow[rowIndex],
                                    sourceLabel = "Home",
                                )
                            },
                            onLongClick = { miniTrayRowIndex = it },
                            onMenu = { menuTrack = it },
                            onOpenAlbum = { artistAlbumNavigator.openAlbum(title = it.name, artist = it.artist, browseId = "") },
                            onOpenArtist = { artistAlbumNavigator.openArtist(name = it.name, browseId = "") },
                        )

                        // Last.fm is optional, so its prompt comes last, after your own stats and history.
                        if (uiState.isLocalStatsMode && !uiState.isViewingFriend) {
                            item(key = "lastfm", contentType = "lastfm") {
                                LocalStatsBanner(
                                    onOpenSettings = onOpenSettings,
                                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 16.dp),
                                )
                            }
                        }

                        if (rows.isEmpty()) {
                            item(key = "empty", contentType = "empty") {
                                Box(
                                    Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(stringResource(R.string.home_no_tracks), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    miniTrayRowIndex?.let { trayIndex ->
                        (rows.getOrNull(trayIndex) as? HomeRow.Track)?.let { trayRow ->
                            TrackMiniTraySheet(
                                data = TrackMiniTrayData(
                                    title = trayRow.track.name,
                                    artist = trayRow.track.artist,
                                    artworkUrl = trayRow.track.artworkUrl,
                                    sourceLabel = "Home",
                                    onPlay = {
                                        musicPlayer.playQueue(
                                            tracks = playbackQueue,
                                            startIndex = playbackIndexByRow[trayIndex],
                                            sourceLabel = "Home",
                                        )
                                    },
                                ),
                                onDismiss = { miniTrayRowIndex = null },
                            )
                        }
                    }
                }
            }
        }
    }
    }

    menuTrack?.let { track ->
        com.tuned.app.ui.common.TrackContextMenuSheet(
            target = com.tuned.app.ui.common.TrackMenuTarget.Track(track.name, track.artist, track.artworkUrl.orEmpty()),
            capabilities = com.tuned.app.ui.common.TrackMenuCapabilities(showCopyActions = true, showDeleteScrobble = true),
            playbackSourceLabel = "Home",
            onDismiss = { menuTrack = null },
        )
    }

}

private suspend fun snapshotFlowNearEnd(listState: LazyListState, onNearEnd: () -> Unit) {
    snapshotFlow {
        val info = listState.layoutInfo
        val total = info.totalItemsCount
        val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
        total > 0 && lastVisible >= total - 5
    }.collect { isNear -> if (isNear) onNearEnd() }
}

@Composable
private fun HeaderRow(
    displayUsername: String,
    isViewingFriend: Boolean,
    onClick: (() -> Unit)?,
    viewModel: HomeViewModel,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = com.tuned.app.ui.theme.Keyline.margin, end = com.tuned.app.ui.theme.Keyline.margin, bottom = com.tuned.app.ui.theme.Space.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Surface(
            onClick = onClick ?: {},
            enabled = onClick != null,
            shape = BadgePillShape,
            color = if (isViewingFriend) MaterialTheme.colorScheme.primaryContainer else HeaderChipColor(),
            border = if (isViewingFriend) null else HeaderChipBorder(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    displayUsername.ifBlank { "—" },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isViewingFriend) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                )
                if (isViewingFriend) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Filled.People,
                        contentDescription = stringResource(R.string.home_switch_profile_cd),
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        LiveListenTimer(viewModel)
    }
}

@Composable
private fun LiveListenTimer(viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listenElapsedSeconds by viewModel.listenElapsedSeconds.collectAsStateWithLifecycle()
    val totalSeconds = (uiState.stats?.timerBaseSeconds ?: 0) + listenElapsedSeconds.toLong()
    val isPlaying = uiState.nowPlaying != null

    Surface(
        shape = BadgePillShape,
        color = HeaderChipColor(),
        border = HeaderChipBorder(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
        ) {
            Icon(
                Icons.Filled.Headset,
                contentDescription = stringResource(R.string.home_listen_time_cd),
                modifier = Modifier.size(18.dp),
                tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                formatTimer(totalSeconds),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Home's chip fill: translucent, so the header panel's tint shows through. */
@Composable
private fun HeaderChipColor() = MaterialTheme.tunedColors.text1.copy(alpha = 0.10f)

@Composable
private fun HeaderChipBorder() = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f))

private fun formatTimer(totalSeconds: Long): String {
    // This is an ESTIMATED LIFETIME total (scrobble count × an average
    // track length — see HomeRepository.HomeStats.timerBaseSeconds), not a
    // session/session-elapsed timer, so it legitimately runs into weeks or
    // months for anyone with a large scrobble history — 18,838 scrobbles
    // at ~3.5 min average really is ~46 days of total listening. The raw
    // "DD:HH:MM:SS" digits made that read as a broken/runaway counter
    // instead of what it actually is; spelling out the units (matching how
    // the rest of the app writes durations elsewhere) makes the same
    // number immediately legible as "45 days" instead of a wall of colons.
    if (totalSeconds <= 0) return "--"
    val d = totalSeconds / 86400
    val h = (totalSeconds % 86400) / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return when {
        d > 0 -> "${d}d ${h}h ${m}m"
        h > 0 -> "${h}h ${m}m ${s}s"
        else -> "${m}m ${s}s"
    }
}

@Composable
fun ProfileAvatar(avatarUrl: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .border(com.tuned.app.ui.theme.Sizes.hairline, MaterialTheme.tunedColors.line, CircleShape)
            .padding(com.tuned.app.ui.theme.Space.xxs),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxSize(),
        ) {
        if (!avatarUrl.isNullOrBlank()) {
            ArtworkImage(
                name = "profile",
                artist = "avatar",
                embeddedUrl = avatarUrl,
                fallbackIcon = Icons.Filled.AccountCircle,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}
}


@Composable
private fun LocalStatsBanner(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // A prompt in the song menu's row style: tonal badge, what it does, chevron.
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth(),
        onClick = onOpenSettings,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.you_connect_lastfm), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.you_connect_lastfm_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp).size(14.dp),
            )
        }
    }
}

@Composable
private fun PodiumSection(
    artists: List<HomeArtistItem>,
    albums: List<HomeAlbum>,
    tags: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (artists.isNotEmpty()) {
            PodiumSectionTitle("Top artists")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
            ) {
                items(artists.distinctBy { "artist_${it.name.lowercase()}" }, key = { "artist_${it.name.lowercase()}" }) { artist ->
                    ArtistPodiumCard(artist)
                }
            }
        }
        if (albums.isNotEmpty()) {
            PodiumSectionTitle("Top albums")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
            ) {
                items(albums.distinctBy { "album_${it.artist.lowercase()}_${it.name.lowercase()}" }, key = { "album_${it.artist.lowercase()}_${it.name.lowercase()}" }) { album ->
                    AlbumPodiumCard(album)
                }
            }
        }
        if (tags.isNotEmpty()) {
            PodiumSectionTitle("Genres")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
            ) {
                items(tags.distinctBy { "tag_$it" }, key = { "tag_$it" }) { tag ->
                    Surface(
                        shape = BadgePillShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            tag.replaceFirstChar { c -> c.uppercase() },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumSectionTitle(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 2.dp),
    )
}

private fun formatPlays(count: Long): String =
    if (count >= 1000) "%.1fk plays".format(count / 1000.0) else "$count plays"

@Composable
private fun ArtistPodiumCard(artist: HomeArtistItem) {
    Column(
        modifier = Modifier.width(84.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            ArtworkImage(
                name = artist.name,
                artist = artist.name,
                embeddedUrl = artist.artworkUrl,
                fallbackIcon = Icons.Filled.MusicNote,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            artist.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (artist.playCount > 0) {
            Text(
                formatPlays(artist.playCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun AlbumPodiumCard(album: HomeAlbum) {
    Column(
        modifier = Modifier.width(96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(ArtworkShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            ArtworkImage(
                name = album.name,
                artist = album.artist,
                embeddedUrl = album.artworkUrl,
                fallbackIcon = Icons.Filled.MusicNote,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            album.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            album.artist,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatsCard(
    scrobbles: Long,
    trackCount: Long,
    artistCount: Long,
    albumCount: Long,
    onOpenGenres: () -> Unit,
    modifier: Modifier = Modifier,
    headlineLabel: String = "Scrobbles",
) {
    // One card, nothing nested in it: a soft accent wash from the top (the album page's), the
    // headline count in display type with the accent arrow to Genres, then songs, artists and albums.
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.22f), accent.copy(alpha = 0.04f))))
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        headlineLabel,
                        style = com.tuned.app.ui.theme.AppType.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        formatCount(rememberAnimatedCount(scrobbles)),
                        style = com.tuned.app.ui.theme.AppType.display.copy(fontSize = 48.sp, lineHeight = 52.sp),
                        color = MaterialTheme.tunedColors.text1,
                    )
                }
                FilledIconButton(
                    onClick = onOpenGenres,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = accent,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.size(52.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.home_view_genres_cd))
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                StatPill("Songs", trackCount, Modifier.weight(1f))
                StatPill("Artists", artistCount, Modifier.weight(1f))
                StatPill("Albums", albumCount, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun rememberAnimatedCount(target: Long): Long {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(target) {
        animated.animateTo(
            targetValue = target.toFloat(),
            // No overshoot: counting past the real number and back read as a glitch.
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessVeryLow,
            ),
        )
    }
    return animated.value.toLong()
}

@Composable
private fun StatPill(label: String, value: Long, modifier: Modifier = Modifier) {
    // A plain figure and its label, no pill behind it.
    Column(modifier) {
        Text(
            formatCount(rememberAnimatedCount(value)),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.tunedColors.text1,
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatCount(value: Long): String = if (value <= 0) "—" else "%,d".format(value)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MixHeader(sortMode: HomeSortMode, onSortModeChange: (HomeSortMode) -> Unit, showTitle: Boolean = true) {
    var menuOpen by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    // Real, felt tactile feedback: haptic now fires the instant the pill is
    // pressed (finger down), not only after the click completes — firing it
    // solely inside onClick meant it landed at the same moment the dropdown
    // menu opened and covered the pill, which could read as "nothing
    // happened" since the press-scale's own short spring had barely started
    // by then. A dedicated LaunchedEffect on the raw pressed state decouples
    // the haptic from whatever the click itself goes on to do.
    val pillInteractionSource = remember { MutableInteractionSource() }
    val pillPressed by pillInteractionSource.collectIsPressedAsState()
    LaunchedEffect(pillPressed) {
        if (pillPressed) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    // A more pronounced dip than the shared rememberGroupPressScale (tuned
    // for full-width group rows) — a small pill needs a bigger relative
    // shrink to actually read as a press at this size.
    val pillScale by animateFloatAsState(
        targetValue = if (pillPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "sortPillPressScale",
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (showTitle) {
            Text(
                stringResource(R.string.you_history_title),
                style = com.tuned.app.ui.theme.AppType.titleL,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
        Box {
            Surface(
                onClick = { menuOpen = true },
                shape = BadgePillShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                interactionSource = pillInteractionSource,
                modifier = Modifier
                    .heightIn(min = 34.dp)
                    .graphicsLayer {
                        scaleX = pillScale
                        scaleY = pillScale
                    },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        iconForSortMode(sortMode),
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        sortModeLabel(sortMode),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Filled.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                shape = com.tuned.app.ui.theme.MenuShape,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                // Lowered from 10dp: a heavy shadowElevation on a popup
                // paints a mostly-rectangular drop shadow around the
                // rounded card (the shadow's own corner falloff is much
                // subtler than the card's actual corner radius), which is
                // exactly what read as "squarish" — a light shadow plus a
                // bit more tonalElevation for legibility fixes that
                // without losing depth entirely.
                tonalElevation = 3.dp,
                shadowElevation = 3.dp,
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                SortOption(Icons.Filled.Schedule, "Recent", sortMode == HomeSortMode.RECENT) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSortModeChange(HomeSortMode.RECENT); menuOpen = false
                }
                SortOption(Icons.Filled.BarChart, "Most played", sortMode == HomeSortMode.MOST_PLAYED) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSortModeChange(HomeSortMode.MOST_PLAYED); menuOpen = false
                }
                SortOption(Icons.Filled.DateRange, "Last 7 days", sortMode == HomeSortMode.LAST_7_DAYS) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSortModeChange(HomeSortMode.LAST_7_DAYS); menuOpen = false
                }
                SortOption(Icons.Filled.CalendarMonth, "Last 30 days", sortMode == HomeSortMode.LAST_30_DAYS) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSortModeChange(HomeSortMode.LAST_30_DAYS); menuOpen = false
                }
                SortOption(Icons.Filled.People, "Top artists", sortMode == HomeSortMode.TOP_ARTISTS) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSortModeChange(HomeSortMode.TOP_ARTISTS); menuOpen = false
                }
                SortOption(Icons.Filled.Album, "Top albums", sortMode == HomeSortMode.TOP_ALBUMS) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSortModeChange(HomeSortMode.TOP_ALBUMS); menuOpen = false
                }
            }
        }
    }
}

private fun iconForSortMode(mode: HomeSortMode): androidx.compose.ui.graphics.vector.ImageVector = when (mode) {
    HomeSortMode.RECENT -> Icons.Filled.Schedule
    HomeSortMode.MOST_PLAYED -> Icons.Filled.BarChart
    HomeSortMode.TOP_ALBUMS -> Icons.Filled.Album
    HomeSortMode.TOP_ARTISTS -> Icons.Filled.People
    HomeSortMode.LAST_7_DAYS -> Icons.Filled.DateRange
    HomeSortMode.LAST_30_DAYS -> Icons.Filled.CalendarMonth
}

private fun sortModeLabel(mode: HomeSortMode) = when (mode) {
    HomeSortMode.RECENT -> "Recent"
    HomeSortMode.MOST_PLAYED -> "Most played"
    HomeSortMode.TOP_ALBUMS -> "Top albums"
    HomeSortMode.TOP_ARTISTS -> "Top artists"
    HomeSortMode.LAST_7_DAYS -> "Last 7 days"
    HomeSortMode.LAST_30_DAYS -> "Last 30 days"
}

@Composable
private fun SortOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = {
            if (active) Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        onClick = onClick,
        modifier = if (active) {
            Modifier
                .padding(horizontal = 6.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
        } else {
            Modifier.padding(horizontal = 6.dp).height(40.dp)
        },
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
    )
}

@Composable
private fun DateHeaderRow(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, bottom = 2.dp),
    )
}

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun TrackRow(
    track: HomeTrack,
    badge: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMenuClick: () -> Unit,
) {
    val isNowPlaying = track.isNowPlaying
    val secondaryTextColor =
        if (isNowPlaying) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        else MaterialTheme.colorScheme.onSurfaceVariant
    val cardModifier = if (isNowPlaying) {
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(NowPlayingCardShape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.70f),
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                    ),
                ),
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    } else {
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    }

    Surface(
        shape = if (isNowPlaying) NowPlayingCardShape else TrackRowShape,
        color = Color.Transparent,
        tonalElevation = if (isNowPlaying) 2.dp else 0.dp,
        modifier = cardModifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(vertical = 6.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(ArtworkShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                ArtworkImage(
                    name = track.name,
                    artist = com.tuned.app.util.ArtistHelper.primaryArtist(track.artist),
                    embeddedUrl = track.artworkUrl,
                    fallbackIcon = if (isNowPlaying) Icons.Filled.GraphicEq else Icons.Filled.MusicNote,
                    modifier = Modifier.fillMaxSize(),
                )
                if (isNowPlaying) {
                    com.tuned.app.ui.player.PlayingWaveBars(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp).size(24.dp, 18.dp),
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    track.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val displayArtist = com.tuned.app.util.ArtistHelper.primaryArtist(track.artist)
                if (displayArtist.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        displayArtist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            if (isNowPlaying) {
                val infiniteTransition = rememberInfiniteTransition(label = "nowPlayingPulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.06f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "pulseScale",
                )
                val dotAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.45f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "dotAlpha",
                )
                Surface(
                    shape = BadgePillShape,
                    color = MaterialTheme.colorScheme.primary,
                    tonalElevation = 4.dp,
                    shadowElevation = 2.dp,
                    modifier = Modifier.graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .graphicsLayer { alpha = dotAlpha }
                                .background(MaterialTheme.colorScheme.onPrimary, CircleShape),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Now Playing",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
            if (badge != null) {
                // The play count as plain muted text; a grey pill on every row was noise.
                Text(
                    badge,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            // Home-only per item 7: a small filled tonal container around
            // the overflow trigger. This composable is private to
            // HomeScreen.kt, so this doesn't touch the three-dot button on
            // Item 1 (consistency pass): the same OverflowMenuButton is now
            // used on every screen's song list, not just Home.
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.pl_options_cd), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun HomeNowPlayingBanner(
    track: HomeTrack,
    onPlay: () -> Unit,
    onStartMix: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = NowPlayingCardShape,
        color = Color.Transparent,
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(NowPlayingCardShape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.90f),
                    ),
                ),
            ),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Surface(
                    shape = BadgePillShape,
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        com.tuned.app.ui.player.PlayingWaveBars(
                            modifier = Modifier.size(14.dp, 12.dp),
                            waveColor = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "NOW PLAYING",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                IconButton(onClick = onMenuClick, modifier = Modifier.size(28.dp).minimumInteractiveComponentSize()) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.home_menu_cd),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(ArtworkShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                ) {
                    ArtworkImage(
                        name = track.name,
                        artist = track.artist,
                        embeddedUrl = track.artworkUrl,
                        fallbackIcon = Icons.Filled.GraphicEq,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onPlay,
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = BadgePillShape,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.home_play), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                androidx.compose.material3.OutlinedButton(
                    onClick = onStartMix,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = BadgePillShape,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.home_mix), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun AlbumRow(
    album: HomeAlbum,
    badge: String?,
    onClick: () -> Unit,
) {
    Surface(
        shape = TrackRowShape,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 6.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(52.dp).clip(ArtworkShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                ArtworkImage(
                    name = album.name,
                    artist = album.artist,
                    embeddedUrl = album.artworkUrl,
                    fallbackIcon = Icons.Filled.Album,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    album.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val displayArtist = com.tuned.app.util.ArtistHelper.primaryArtist(album.artist)
                if (displayArtist.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        displayArtist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun ArtistRow(
    artist: HomeArtistItem,
    badge: String?,
    onClick: () -> Unit,
) {
    Surface(
        shape = TrackRowShape,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 6.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                ArtworkImage(
                    name = artist.name,
                    artist = artist.name,
                    embeddedUrl = artist.artworkUrl,
                    fallbackIcon = Icons.Filled.People,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    artist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class ArtistAlbumNavBridgeHome @javax.inject.Inject constructor(
    val navigator: com.tuned.app.ui.navigation.ArtistAlbumNavigator
) : androidx.lifecycle.ViewModel()

/**
 * You's History preview: the playing song, then the latest distinct songs, ten at most. Local
 * (Guest) history has no timestamps, so it keeps the history's own order, then falls back to the
 * most-played list.
 */
private fun recentHistoryRows(state: HomeUiState): List<HomeRow> {
    val dated = state.allTracks.filter { it.timestampMillis != null }.sortedByDescending { it.timestampMillis }
    val source = dated.ifEmpty { state.allTracks }.ifEmpty { state.topTracksOverall }
    return buildList {
        state.nowPlaying?.let { add(HomeRow.Track(it, badge = null)) }
        source
            .filter { !it.isNowPlaying }
            .distinctBy { it.key }
            .forEach { add(HomeRow.Track(it, badge = null)) }
    }.take(HISTORY_PREVIEW_COUNT)
}

private const val HISTORY_PREVIEW_COUNT = 10

/** "History ›" on the margin: the whole line opens the full History screen. */
@Composable
private fun HistorySectionTitle(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.you_history_title),
            style = com.tuned.app.ui.theme.AppType.titleL,
            color = MaterialTheme.tunedColors.text1,
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = stringResource(R.string.you_history_open_cd),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp).size(28.dp),
        )
    }
}

/** The rows of a listening history, shared by You's preview and the History screen. */
private fun LazyListScope.historyItems(
    rows: List<HomeRow>,
    onPlay: (rowIndex: Int) -> Unit,
    onLongClick: (rowIndex: Int) -> Unit,
    onMenu: (HomeTrack) -> Unit,
    onOpenAlbum: (HomeAlbum) -> Unit,
    onOpenArtist: (com.tuned.app.data.repository.HomeArtistItem) -> Unit,
) {
    itemsIndexed(
        rows,
        key = { _, row ->
            when (row) {
                is HomeRow.DateHeader -> "date_${row.label}"
                is HomeRow.Track -> if (row.track.isNowPlaying) "nowplaying_${row.track.key}" else "track_${row.track.key}_${row.track.timestampMillis}"
                is HomeRow.Album -> "album_${row.album.artist}_${row.album.name}"
                is HomeRow.Artist -> "artist_${row.artist.name}"
            }
        },
        contentType = { _, row ->
            when (row) {
                is HomeRow.DateHeader -> "date"
                is HomeRow.Track -> "track"
                is HomeRow.Album -> "album"
                is HomeRow.Artist -> "artist"
            }
        },
    ) { rowIndex, row ->
        when (row) {
            is HomeRow.DateHeader -> DateHeaderRow(row.label)
            is HomeRow.Track -> TrackRow(
                track = row.track,
                badge = row.badge,
                onClick = { onPlay(rowIndex) },
                onLongClick = { onLongClick(rowIndex) },
                onMenuClick = { onMenu(row.track) },
            )
            is HomeRow.Album -> AlbumRow(album = row.album, badge = row.badge, onClick = { onOpenAlbum(row.album) })
            is HomeRow.Artist -> ArtistRow(artist = row.artist, badge = row.badge, onClick = { onOpenArtist(row.artist) })
        }
    }
}

/**
 * Your whole listening history: a search field and the sort menu in the header, then every row,
 * paged in as you scroll. Searching matches song, artist and album names.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    artistAlbumNavigator: com.tuned.app.ui.navigation.ArtistAlbumNavigator = hiltViewModel<ArtistAlbumNavBridgeHome>().navigator,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.pollWhileActive() }
    }
    var query by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var menuTrack by remember { mutableStateOf<HomeTrack?>(null) }
    val musicPlayer = com.tuned.app.ui.player.LocalMusicPlayer.current
    val shown = remember(rows, query) {
        val q = query.trim()
        if (q.isEmpty()) rows else rows.filter { row ->
            when (row) {
                is HomeRow.DateHeader -> false
                is HomeRow.Track -> row.track.name.contains(q, true) || row.track.artist.contains(q, true)
                is HomeRow.Album -> row.album.name.contains(q, true) || row.album.artist.contains(q, true)
                is HomeRow.Artist -> row.artist.name.contains(q, true)
            }
        }
    }
    val queue = remember(shown) {
        shown.mapNotNull { (it as? HomeRow.Track)?.track }.map {
            com.tuned.app.playback.PlayableTrack(title = it.name, artist = it.artist, artworkUrl = it.artworkUrl, videoId = it.videoId)
        }
    }
    val queueIndex = remember(shown) {
        var next = 0
        IntArray(shown.size) { if (shown[it] is HomeRow.Track) next++ else -1 }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(listState, uiState.allTracks.size) {
        snapshotFlowNearEnd(listState) { viewModel.loadNextPage() }
    }

    Column(Modifier.fillMaxSize()) {
        ExpressiveHeader(title = stringResource(R.string.you_history_title), onBack = onBack) {
            val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = com.tuned.app.ui.theme.Keyline.margin)
                    .height(com.tuned.app.ui.theme.Sizes.touch),
                decorationBox = { inner ->
                    // Home's chip style: translucent, so the header's tint shows through.
                    Row(
                        Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(HeaderChipColor())
                            .border(HeaderChipBorder(), CircleShape)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(stringResource(R.string.you_history_search_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            inner()
                        }
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }, modifier = Modifier.size(28.dp).minimumInteractiveComponentSize()) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.search_clear_cd), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                },
            )
            Box(Modifier.padding(horizontal = com.tuned.app.ui.theme.Keyline.margin)) {
                MixHeader(sortMode = uiState.sortMode, onSortModeChange = viewModel::setSortMode, showTitle = false)
            }
        }
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = 8.dp,
                end = 8.dp,
                top = 8.dp,
                bottom = 24.dp + com.tuned.app.ui.player.LocalMiniPlayerScrollClearance.current + com.tuned.app.ui.common.safeDrawingBottomPadding(),
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            historyItems(
                rows = shown,
                onPlay = { musicPlayer.playQueue(tracks = queue, startIndex = queueIndex[it].coerceAtLeast(0), sourceLabel = "History") },
                onLongClick = {},
                onMenu = { menuTrack = it },
                onOpenAlbum = { artistAlbumNavigator.openAlbum(title = it.name, artist = it.artist, browseId = "") },
                onOpenArtist = { artistAlbumNavigator.openArtist(name = it.name, browseId = "") },
            )
            if (shown.isEmpty() && !uiState.isLoading) {
                item(key = "empty") {
                    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        Text(
                            if (query.isBlank()) stringResource(R.string.home_no_tracks) else stringResource(R.string.you_history_no_match, query.trim()),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    menuTrack?.let { track ->
        com.tuned.app.ui.common.TrackContextMenuSheet(
            target = com.tuned.app.ui.common.TrackMenuTarget.Track(track.name, track.artist, track.artworkUrl.orEmpty()),
            capabilities = com.tuned.app.ui.common.TrackMenuCapabilities(showCopyActions = true, showDeleteScrobble = true),
            playbackSourceLabel = "History",
            onDismiss = { menuTrack = null },
        )
    }
}

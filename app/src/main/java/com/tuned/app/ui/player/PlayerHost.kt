@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.tuned.app.ui.player

import com.tuned.app.ui.theme.Motion
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import com.tuned.app.ui.theme.Space
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import com.tuned.app.ui.common.PredictiveBackScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import com.tuned.app.ui.theme.LocalMd3NavBar
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuned.app.playback.MusicPlayer
import com.tuned.app.playback.PlayableTrack
import com.tuned.app.playback.toGeneratedTrack
import com.tuned.app.ui.common.ExpressiveMotion
import com.tuned.app.ui.theme.LocalLiquidGlass
import com.tuned.app.ui.theme.liquidGlassSource
import com.tuned.app.ui.theme.isLiquidGlassBackdropSupported
import com.tuned.app.ui.theme.rememberLayerBackdrop
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.Dp
import com.tuned.app.ui.common.LocalUndoController
import com.tuned.app.ui.common.UndoSnackbarHost
import com.tuned.app.ui.common.rememberUndoController
import com.tuned.app.ui.shell.LocalBottomChromeHeight
import com.tuned.app.ui.shell.bottomChromeHeight
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.rememberMotion
import com.tuned.app.ui.shell.DockState
import com.tuned.app.ui.shell.LocalDockState
import com.tuned.app.ui.shell.DockFootprint
import com.tuned.app.ui.shell.MiniPlayerAboveDock
import com.tuned.app.ui.shell.LocalChromeBackdrop
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer

enum class FullPlayerTab {
    NOW_PLAYING,
    LYRICS,
    QUEUE
}

val LocalMusicPlayer = staticCompositionLocalOf<MusicPlayer> {
    error("MusicPlayer is only available inside PlayerHost")
}

val LocalAddToPlaylist = staticCompositionLocalOf<(PlayableTrack) -> Unit> {
    error("Add-to-playlist is only available inside PlayerHost")
}

/**
 * Extra list-end clearance needed while the collapsed player overlays a
 * screen. The Material 3 bar is about 86dp tall; 88dp clears it without
 * leaving a large empty band at the end of short lists.
 */
val LocalMiniPlayerScrollClearance = staticCompositionLocalOf { 0.dp }

val LocalBottomNavSlot = androidx.compose.runtime.staticCompositionLocalOf<androidx.compose.runtime.MutableState<@androidx.compose.runtime.Composable () -> Unit>> {
    error("LocalBottomNavSlot not provided")
}

/** App-wide collapsed + maximized player layered over every navigation route. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlayerHost(
    viewModel: PlayerViewModel = hiltViewModel(),
    hasBottomNavigation: Boolean = false,
    content: @Composable () -> Unit,
) {
    val state by viewModel.chromeState.collectAsStateWithLifecycle()
    val playbackState by viewModel.player.state.collectAsStateWithLifecycle()
    val activeDownloads by viewModel.activeDownloads.collectAsStateWithLifecycle()
    var expanded by rememberSaveable { mutableStateOf(false) }
    var currentTab by rememberSaveable { mutableStateOf(FullPlayerTab.NOW_PLAYING) }
    var playlistTrack by remember { mutableStateOf<PlayableTrack?>(null) }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val keepLyricsAwake = expanded && currentTab == FullPlayerTab.LYRICS && settings.keepLyricsScreenAwake
    val currentView = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(keepLyricsAwake) {
        if (keepLyricsAwake) {
            currentView.keepScreenOn = true
        }
        onDispose {
            if (keepLyricsAwake) {
                currentView.keepScreenOn = false
            }
        }
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val requestAddToPlaylist = remember(viewModel) {
        { track: PlayableTrack ->
            viewModel.prepareCustomPlaylists()
            playlistTrack = track
        }
    }
    val trackKey = state.current?.let { it.videoId ?: "${it.artist}|${it.title}" }
    LaunchedEffect(trackKey) {
        if (trackKey == null) {
            expanded = false
            currentTab = FullPlayerTab.NOW_PLAYING
        }
    }
    LaunchedEffect(expanded) {
        if (!expanded) {
            currentTab = FullPlayerTab.NOW_PLAYING
        }
    }
    LaunchedEffect(Unit) {
        viewModel.navEvents.collect {
            expanded = false
            currentTab = FullPlayerTab.NOW_PLAYING
        }
    }
    // A page opening from a link would otherwise sit hidden behind the full player.
    LaunchedEffect(Unit) {
        viewModel.pendingAppRoute.collect { route ->
            if (route != null) {
                expanded = false
                currentTab = FullPlayerTab.NOW_PLAYING
            }
        }
    }
    // "Watch video" from any song menu opens the player in video mode.
    LaunchedEffect(Unit) {
        MusicVideoMode.openRequests.collect {
            currentTab = FullPlayerTab.NOW_PLAYING
            expanded = true
        }
    }
    LaunchedEffect(Unit) {
        viewModel.mixLauncher.requests.collect {
            expanded = false
            currentTab = FullPlayerTab.NOW_PLAYING
        }
    }
    LaunchedEffect(Unit) {
        viewModel.genreExplorer.pendingGenre.collect { genre ->
            if (genre != null) {
                expanded = false
                currentTab = FullPlayerTab.NOW_PLAYING
            }
        }
    }
    val miniPlayerVisible = state.current != null && !expanded
    val motion = rememberMotion()

    val bottomNavSlot = remember { mutableStateOf<@Composable () -> Unit>({}) }
    val undo = rememberUndoController()
    LaunchedEffect(viewModel) {
        viewModel.toastMessage.collect { msg -> undo.show(msg) }
    }
    val chromeHeight = bottomChromeHeight(hasMiniPlayer = state.current != null, hasNavBar = hasBottomNavigation)
    val dockState = remember { DockState() }
    // Sibling pattern from the original player: one backdrop captures the page
    // so the floating mini player can refract it when Liquid Glass is on.
    val miniBackdropColor = MaterialTheme.colorScheme.background
    val miniBackdrop = rememberLayerBackdrop {
        drawRect(miniBackdropColor)
        drawContent()
    }
    val miniGlass = LocalLiquidGlass.current && isLiquidGlassBackdropSupported()
    // The dock slides away while a tab scrolls down and the mini player drops
    // into its place; both ride one spring, as translations only, so nothing
    // re-measures mid-animation.
    val dockHidden = hasBottomNavigation && dockState.tabsCollapsed
    val hide by animateFloatAsState(
        targetValue = if (dockHidden) 1f else 0f,
        animationSpec = if (motion.reduced) snap() else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "dockHide",
    )
    val density = LocalDensity.current
    val miniDropPx = with(density) { (MiniPlayerAboveDock - Space.md).toPx() }
    val dockDropPx = with(density) { (DockFootprint + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()).toPx() }
    val scrimColor = MaterialTheme.colorScheme.background
    LaunchedEffect(hasBottomNavigation, state.current == null) { dockState.tabsCollapsed = false }
    CompositionLocalProvider(
        LocalMusicPlayer provides viewModel.player,
        LocalAddToPlaylist provides requestAddToPlaylist,
        LocalMiniPlayerScrollClearance provides if (state.current != null) Sizes.miniPlayer else Dp(0f),
        LocalBottomNavSlot provides bottomNavSlot,
        LocalBottomChromeHeight provides chromeHeight,
        LocalUndoController provides undo,
        LocalDockState provides dockState,
    ) {
        SharedTransitionLayout(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalPlayerSharedScope provides this, LocalPlayerFullBleed provides settings.canvasFullBleed) {
        Box(Modifier.fillMaxSize()) {
            // Home recedes behind the open player, a little smaller with rounded
            // corners, and comes back as it closes.
            val recede by animateFloatAsState(
                targetValue = if (expanded && state.current != null && !motion.reduced) 1f else 0f,
                animationSpec = if (expanded) tween(PLAYER_MORPH_MS, easing = PaletteEasing) else PlayerCloseFloat,
                label = "homeRecede",
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val k = 1f - 0.06f * recede
                        scaleX = k
                        scaleY = k
                        if (recede > 0.001f) {
                            shape = RoundedCornerShape((28 * recede).dp)
                            clip = true
                        }
                    }
                    .liquidGlassSource(if (miniGlass) miniBackdrop else null),
            ) {
                content()
            }
            // Fade behind the floating chrome so scrolling content doesn't
            // fight with it. Functional, not decoration: it keeps titles legible.
            if (!expanded && (hasBottomNavigation || state.current != null)) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(chromeHeight)
                        .background(Brush.verticalGradient(0f to Color.Transparent, 0.55f to scrimColor.copy(alpha = 0.85f), 1f to scrimColor)),
                )
            }
            AnimatedVisibility(
                visible = state.current != null && !expanded,
                enter = fadeIn(PlayerFadeInUnder),
                exit = fadeOut(PlayerFadeOutUnder),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer { translationY = hide * miniDropPx },
            ) {
                CompositionLocalProvider(LocalPlayerVisibilityScope provides this) {
                    MiniPlayer(
                        state = state,
                        progressState = viewModel.progressState,
                        onExpand = { expanded = true },
                        onToggle = viewModel.player::togglePlayPause,
                        onPrevious = viewModel.player::previous,
                        onNext = viewModel.player::next,
                        onClose = viewModel.player::stopAndClear,
                        bottomPadding = if (hasBottomNavigation) MiniPlayerAboveDock else Space.md,
                        backdrop = if (miniGlass) miniBackdrop else null,
                    )
                }
            }
            if (hasBottomNavigation) {
                AnimatedVisibility(
                    visible = !expanded,
                    enter = fadeIn(motion.base()),
                    exit = fadeOut(motion.fast()),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .graphicsLayer {
                            translationY = hide * dockDropPx
                            alpha = 1f - hide * 0.6f
                        },
                ) {
                    CompositionLocalProvider(LocalChromeBackdrop provides if (miniGlass) miniBackdrop else null) {
                        bottomNavSlot.value()
                    }
                }
            }
            UndoSnackbarHost(undo, Modifier.align(Alignment.BottomCenter).padding(bottom = chromeHeight), backdrop = if (miniGlass) miniBackdrop else null)
            if (miniPlayerVisible) {
                // Sleep Timer Button
                if (!expanded) {
                    DraggableSleepTimerOverlay(
                        sleepTimerRemainingMs = playbackState.sleepTimerRemainingMs,
                        onCycleTimer = { viewModel.player.cycleSleepTimer() },
                        onCloseTimer = { viewModel.player.setSleepTimerMinutes(0) },
                    )
                }
            }
            if (activeDownloads.isNotEmpty() && !expanded) {
                val latestProgress = activeDownloads.firstOrNull()?.progressPercent ?: 0
                com.tuned.app.ui.download.DraggableDownloadOverlay(
                    activeDownloadsCount = activeDownloads.size,
                    latestProgressPercent = latestProgress,
                    onOpenDownloads = { viewModel.navigateToDownloads() },
                )
            }
            var isPlayerDragging by remember { mutableStateOf(false) }
            // The mini player's card morphs into the full player and back
            // (shared container + shared artwork, see PlayerSharedTransition).
            AnimatedVisibility(
                visible = expanded && state.current != null,
                enter = fadeIn(PlayerFadeInTop),
                exit = fadeOut(if (settings.canvasFullBleed) PlayerFadeOutDarken else PlayerFadeOutTop),
            ) {
              CompositionLocalProvider(LocalPlayerVisibilityScope provides this) {
                PredictiveBackScreen(
                    enabled = expanded && state.current != null && currentTab == FullPlayerTab.NOW_PLAYING,
                    onBack = { expanded = false },
                    backgroundColor = Color.Transparent,
                ) {
                    ExpandedPlayer(
                        viewModel = viewModel,
                        currentTab = currentTab,
                        onTabChange = { currentTab = it },
                        onRetryLyrics = viewModel::retryLyrics,
                        onCollapse = { expanded = false },
                        onOpenArtist = { artist ->
                            expanded = false
                            viewModel.openArtist(artist)
                        },
                        onOpenAlbum = {
                            val current = viewModel.player.state.value.current
                            val album = current?.album?.takeIf { it.isNotBlank() }
                            if (album != null) {
                                expanded = false
                                viewModel.openAlbum(album, current.artist)
                            }
                        },
                        onDragStatusChange = { isPlayerDragging = it },
                    )
                }
              }
            }

            playlistTrack?.let { track ->
                AddToPlaylistDialogHost(
                    viewModel = viewModel,
                    track = track,
                    onDismiss = { playlistTrack = null },
                    onAdd = { playlistIds, duplicatePlaylistIds ->
                        viewModel.addToPlaylists(playlistIds, duplicatePlaylistIds, track)
                        playlistTrack = null
                    },
                    onFindDuplicates = { playlistIds ->
                        viewModel.findDuplicatePlaylistIds(playlistIds, track)
                    },
                    onFindCachedDuplicates = { playlistIds ->
                        viewModel.findCachedDuplicatePlaylistIds(playlistIds, track)
                    },
                    onCreate = { title ->
                        viewModel.createPlaylistAndAdd(title, track)
                        playlistTrack = null
                    },
                )
            }
        }
        }
        }
    }
}

@Composable
internal fun ExpandedPlayer(
    viewModel: PlayerViewModel,
    currentTab: FullPlayerTab,
    onTabChange: (FullPlayerTab) -> Unit,
    onRetryLyrics: () -> Unit,
    onCollapse: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: () -> Unit = {},
    onDragStatusChange: (Boolean) -> Unit = {},
) {
    val state by viewModel.fullPlayerState.collectAsStateWithLifecycle()
    val lyricsState by viewModel.lyricsState.collectAsStateWithLifecycle()
    val canvas by viewModel.canvasState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val loudnessSettings by viewModel.loudnessSettings.collectAsStateWithLifecycle()
    val likedTrackKeys by viewModel.likedTrackKeys.collectAsStateWithLifecycle()
    val currentTrack = state.current
    // Normalized key: raw "title|artist".lowercase() would miss liked entries
    // whose spacing differs (e.g. trailing space from another source).
    val isLiked = currentTrack != null &&
        currentTrack.toGeneratedTrack().key in likedTrackKeys
    val lyricsOffsetMs by viewModel.liveLyricsOffsetMs.collectAsStateWithLifecycle()
    FullPlayer(
        state = state,
        progressState = viewModel.progressState,
        player = viewModel.player,
        lyricsState = lyricsState,
        wavySeekbarEnabled = settings.wavySeekbarEnabled,
        rotatingBackgroundEnabled = settings.rotatingBackgroundEnabled,
        lyricsRotatingBackgroundEnabled = settings.lyricsRotatingBackgroundEnabled,
        lyricsOffsetMs = lyricsOffsetMs,
        onSetLyricsOffsetMs = viewModel::setLyricsOffsetMs,
        lyricsFontScale = settings.lyricsFontScale,
        onSetLyricsFontScale = viewModel::setLyricsFontScale,
        lyricsButtonsAutoHideSeconds = settings.lyricsButtonsAutoHideSeconds,
        translationLanguage = settings.translationLanguage,
        canvas = canvas,
        canvasEnabled = settings.canvasEnabled,
        canvasFullBleedEnabled = settings.canvasFullBleed,
        musicVideoEnabled = settings.musicVideoEnabled,
        tuning432Enabled = settings.tuning432Enabled,
        loudnessPreampDb = if (loudnessSettings.mode != com.tuned.app.playback.LoudnessMode.OFF) {
            loudnessSettings.preampDb
        } else {
            null
        },
        currentTab = currentTab,
        onTabChange = onTabChange,
        onRetryLyrics = onRetryLyrics,
        onCollapse = onCollapse,
        onOpenArtist = onOpenArtist,
        onOpenAlbum = onOpenAlbum,
        isLiked = isLiked,
        onToggleLiked = { currentTrack?.let(viewModel::toggleLiked) },
        onDoubleTapLike = { currentTrack?.let(viewModel::like) },
        onDragStatusChange = onDragStatusChange,
        onSelectLyricsProvider = viewModel::selectLyricsProvider,
        onTryAnotherLyricsSource = viewModel::tryAnotherLyricsSource,
    )
}

@Composable
internal fun AddToPlaylistDialogHost(
    viewModel: PlayerViewModel,
    track: PlayableTrack,
    onDismiss: () -> Unit,
    onAdd: (Set<Long>, Set<Long>) -> Unit,
    onFindDuplicates: suspend (Set<Long>) -> Set<Long>,
    onFindCachedDuplicates: suspend (Set<Long>) -> Set<Long>,
    onCreate: (String) -> Unit,
) {
    val allPlaylists by viewModel.customPlaylists.collectAsStateWithLifecycle()
    // An imported file can't live in a YouTube Music playlist (no videoId).
    val playlists = remember(allPlaylists, track) {
        if (track.isImportedLocalFile()) allPlaylists.filterNot { it.remotePlaylistId != null } else allPlaylists
    }
    AddToPlaylistDialog(
        track = track,
        playlists = playlists,
        onDismiss = onDismiss,
        onAdd = onAdd,
        onFindDuplicates = onFindDuplicates,
        onFindCachedDuplicates = onFindCachedDuplicates,
        onCreate = onCreate,
    )
}

internal fun formatTime(ms: Long): String {
    val total = (ms.coerceAtLeast(0) / 1000)
    return "%d:%02d".format(total / 60, total % 60)
}

/** True for on-device local tracks (legacy copies and in-place references). */
internal fun PlayableTrack.isImportedLocalFile(): Boolean =
    com.tuned.app.data.generate.LocalAudioFormats.localReferenceOrNull(playbackUrl) != null


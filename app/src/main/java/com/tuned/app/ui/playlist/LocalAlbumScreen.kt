package com.tuned.app.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tuned.app.ui.theme.rememberLayerBackdrop
import com.tuned.app.R
import com.tuned.app.data.model.AlbumPageData
import com.tuned.app.data.playlist.PlaylistRepository
import com.tuned.app.playback.PlayableTrack
import com.tuned.app.playback.toPlayableTrack
import com.tuned.app.ui.album.AlbumDetails
import com.tuned.app.ui.album.AlbumHeader
import com.tuned.app.ui.album.AlbumTrackRow
import com.tuned.app.ui.album.isTrackPlaying
import com.tuned.app.ui.common.DetailAccentWash
import com.tuned.app.ui.common.DetailToolbar
import com.tuned.app.ui.common.LocalUndoController
import com.tuned.app.ui.common.TrackContextMenuSheet
import com.tuned.app.ui.common.TrackMenuCapabilities
import com.tuned.app.ui.common.TrackMenuTarget
import com.tuned.app.ui.common.safeDrawingBottomPadding
import com.tuned.app.ui.player.LocalMiniPlayerScrollClearance
import com.tuned.app.ui.player.LocalMusicPlayer
import com.tuned.app.ui.theme.LocalLiquidGlass
import com.tuned.app.ui.theme.LocalLiquidGlassBackdrop
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.isLiquidGlassBackdropSupported
import com.tuned.app.ui.theme.liquidGlassSource
import com.tuned.app.ui.theme.tunedColors
import com.tuned.app.util.ArtistHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class LocalAlbumViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {
    /** Null until the first read; an empty list means the songs have gone. */
    internal val albums: StateFlow<List<LocalAlbum>?> = playlistRepository.changes
        .onStart { emit(Unit) }
        .map { localAlbums(playlistRepository.getLocalSongs()?.tracks.orEmpty()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val savedKeys = MutableStateFlow(emptySet<String>())
    val saved: StateFlow<Set<String>> = savedKeys

    init {
        // "Saved" must reflect what is actually in the library (a matching
        // custom playlist), not just this process's memory: the old
        // in-memory-only set forgot every save at process death and claimed
        // success for saves that had failed.
        viewModelScope.launch { refreshSaved() }
        viewModelScope.launch { playlistRepository.changes.collect { refreshSaved() } }
    }

    private suspend fun refreshSaved() {
        val all = runCatching { playlistRepository.getAll() }.getOrDefault(emptyList())
        val custom = all.filter { it.mode == "custom" }
        if (custom.isEmpty()) return
        val localAlbums = localAlbums(playlistRepository.getLocalSongs()?.tracks.orEmpty())
        savedKeys.value = localAlbums.filter { album ->
            custom.any { saved ->
                saved.title.equals(album.title, ignoreCase = true) &&
                    saved.tracks.map { it.url }.toSet() == album.tracks.map { it.url }.toSet()
            }
        }.map { it.key }.toSet()
    }

    /** Add to library, as on a catalogue album: a playlist copy, kept even if the folder is forgotten. */
    internal fun save(album: LocalAlbum, onResult: (Boolean) -> Unit = {}) {
        if (album.key in savedKeys.value) {
            onResult(true)
            return
        }
        viewModelScope.launch {
            val ok = runCatching {
                playlistRepository.save(album.title, album.artist, "custom", album.tracks)
            }.isSuccess
            // Mark saved only after the write actually landed.
            if (ok) savedKeys.value = savedKeys.value + album.key
            onResult(ok)
        }
    }
}

/** A local album, on the same parts as a catalogue album, so it reads and moves the same. */
@Composable
fun LocalAlbumScreen(
    albumKey: String,
    onBack: () -> Unit,
    onOpenArtist: (String) -> Unit,
    viewModel: LocalAlbumViewModel = hiltViewModel(),
) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val album = albums?.firstOrNull { it.key == albumKey }
    if (album == null) {
        // Two distinct states, both labelled (the old code rendered a
        // completely blank page and relied on a side effect to leave):
        // still loading, or the album is genuinely gone (every song
        // removed / its folder forgotten) with a way back.
        Box(Modifier.fillMaxSize().background(MaterialTheme.tunedColors.bg)) {
            if (albums == null) {
                com.tuned.app.ui.common.LoadingSpinner(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(Modifier.fillMaxSize()) {
                    DetailToolbar(
                        title = "",
                        showTitle = false,
                        onBack = onBack,
                        onShare = null,
                        shareLabel = "",
                        menu = null,
                    )
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Text(
                            stringResource(R.string.alb_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.tunedColors.text2,
                        )
                    }
                }
            }
        }
        return
    }

    val player = LocalMusicPlayer.current
    val playback by player.chromeState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val undo = LocalUndoController.current
    val savedMessage = stringResource(R.string.alb_saved)
    val saveFailMessage = stringResource(R.string.alb_save_fail)
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val density = LocalDensity.current
    val colors = MaterialTheme.tunedColors
    val listState = rememberLazyListState()
    val queue = remember(album) {
        album.tracks.map { it.toPlayableTrack().copy(album = album.title, artworkUrl = it.artworkUrl ?: album.artworkUrl) }
    }
    val page = remember(album, queue) {
        AlbumPageData(
            title = album.title,
            artist = album.artist,
            artworkUrl = album.artworkUrl,
            releaseYear = album.year,
            tracks = queue,
        )
    }
    var menuTrack by remember { mutableStateOf<PlayableTrack?>(null) }
    var titleBottomPx by remember { mutableIntStateOf(Int.MAX_VALUE) }
    val toolbarPx = with(density) { (WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + Sizes.touch).roundToPx() }
    val scrolledPastTitle by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > titleBottomPx - toolbarPx
        }
    }
    val albumPlaying = playback.isPlaying && playback.current?.album.equals(album.title, ignoreCase = true)
    val play: (Int, Boolean) -> Unit = { index, shuffled ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        player.playQueue(queue, startIndex = index, sourceLabel = album.title, startShuffled = shuffled)
    }
    val openArtist: () -> Unit = { ArtistHelper.primaryArtist(album.artist).takeIf(String::isNotBlank)?.let(onOpenArtist) }
    val glassOn = LocalLiquidGlass.current && isLiquidGlassBackdropSupported()
    val pageBackdrop = rememberLayerBackdrop()

    Box(Modifier.fillMaxSize().background(colors.bg)) {
        Box(Modifier.fillMaxSize().liquidGlassSource(if (glassOn) pageBackdrop else null)) {
            DetailAccentWash(listState)
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    bottom = Space.xl + LocalMiniPlayerScrollClearance.current + safeDrawingBottomPadding(),
                ),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "header") {
                    AlbumHeader(
                        album = page,
                        apple = null,
                        notes = null,
                        onNotes = {},
                        canvas = null,
                        screenWidth = screenWidth,
                        playing = albumPlaying,
                        saved = album.key in saved,
                        saving = false,
                        onTitleBottom = { titleBottomPx = it },
                        onArtist = openArtist,
                        onShuffle = { play(0, true) },
                        onPlay = { if (albumPlaying) player.togglePlayPause() else play(0, false) },
                        onSave = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.save(album) { ok ->
                                undo?.show(if (ok) savedMessage else saveFailMessage)
                            }
                        },
                    )
                }
                itemsIndexed(queue, key = { index, track -> "track_${track.title}_$index" }, contentType = { _, _ -> "album_track" }) { index, track ->
                    AlbumTrackRow(
                        number = album.tracks[index].trackNumber ?: (index + 1),
                        track = track,
                        albumArtist = album.artist,
                        playing = isTrackPlaying(playback.isPlaying, playback.current, track),
                        onClick = { play(index, false) },
                        onMore = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuTrack = track
                        },
                    )
                }
                item(key = "details") { AlbumDetails(page, null) }
            }
        }
        CompositionLocalProvider(LocalLiquidGlassBackdrop provides if (glassOn) pageBackdrop else null) {
            DetailToolbar(
                title = album.title,
                showTitle = scrolledPastTitle,
                onBack = onBack,
                // Nothing to share or add for songs on this phone; shuffle and the artist are in the header.
                onShare = null,
                shareLabel = "",
                menu = null,
                modifier = Modifier.align(Alignment.TopCenter).zIndex(10f),
            )
        }
    }

    menuTrack?.let { track ->
        TrackContextMenuSheet(
            target = TrackMenuTarget.Track(track.title, track.artist, ""),
            capabilities = TrackMenuCapabilities(showCopyActions = true, showDeleteScrobble = false),
            playableTrack = track,
            playbackSourceLabel = album.title,
            onDismiss = { menuTrack = null },
        )
    }
}

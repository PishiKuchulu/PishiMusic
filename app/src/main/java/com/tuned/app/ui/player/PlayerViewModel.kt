@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.tuned.app.ui.player

import androidx.compose.foundation.layout.only
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuned.app.data.lyrics.LyricsRepository
import com.tuned.app.data.lyrics.LyricsResult
import com.tuned.app.data.playlist.PlaylistRepository
import com.tuned.app.data.playlist.SavedPlaylist
import com.tuned.app.data.playlist.LIKED_SONGS_MODE
import com.tuned.app.playback.MusicPlayer
import com.tuned.app.playback.PlayableTrack
import com.tuned.app.playback.toGeneratedTrack
import com.tuned.app.playback.resolve.MetadataLog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import androidx.core.graphics.scale
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
    val player: MusicPlayer,
    private val playlistRepository: PlaylistRepository,
    private val lyricsRepository: LyricsRepository,
    private val canvasRepository: com.tuned.app.data.canvas.CanvasRepository,
    private val settingsPreferences: com.tuned.app.data.local.SettingsPreferences,
    val navigator: com.tuned.app.ui.navigation.ArtistAlbumNavigator,
    val genreExplorer: com.tuned.app.ui.genres.GenreExplorer,
    val mixLauncher: com.tuned.app.ui.generate.MixLauncher,
    private val ytMusicLibraryManager: com.tuned.app.data.ytmusic.YtMusicLibraryManager,
    private val trackLikeController: com.tuned.app.data.playlist.TrackLikeController,
    private val downloadManager: com.tuned.app.data.download.TrackDownloadManager,
    private val routeNavigator: com.tuned.app.ui.navigation.AppRouteNavigator,
    private val themeRepository: com.tuned.app.data.repository.ThemeRepository,
    loudnessPrefs: com.tuned.app.playback.LoudnessPrefs,
) : ViewModel() {
    /** Loudness-normalization settings, surfaced so the signal-path sheet can show the real preamp. */
    val loudnessSettings: StateFlow<com.tuned.app.playback.LoudnessSettings> = loudnessPrefs.settings
    val navEvents = navigator.events
    val state = player.state
    val chromeState = player.chromeState
    val progressState = player.progressState
    val fullPlayerState = player.state
        .map { it.copy(positionMs = 0L, bufferedPositionMs = 0L) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, player.state.value.copy(positionMs = 0L, bufferedPositionMs = 0L))
    val settings: StateFlow<com.tuned.app.data.local.MiscSettings> = settingsPreferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.tuned.app.data.local.MiscSettings())
    val activeDownloads = downloadManager.downloads
        .map { map -> map.values.filter { !it.isFinished && it.error == null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** A page something outside the player has asked to open, such as a shared link. */
    val pendingAppRoute get() = routeNavigator.pendingRoute

    fun navigateToDownloads() {
        routeNavigator.navigateTo(com.tuned.app.ui.navigation.Screen.Downloads.route)
    }
    private val _customPlaylists = MutableStateFlow<List<SavedPlaylist>>(emptyList())
    val customPlaylists = _customPlaylists.asStateFlow()
    private var customPlaylistsLoaded = false
    val likedTrackKeys = trackLikeController.likedTrackKeys

    // Buffered: an emit with no collector on screen yet must not suspend the
    // caller's coroutine or silently vanish (the old replay=0/buffer=0 flow did both).
    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val toastMessage = _toastMessage.asSharedFlow()

    private val _liveLyricsOffsetMs = MutableStateFlow<Long?>(null)
    val liveLyricsOffsetMs: StateFlow<Long> = combine(
        _liveLyricsOffsetMs,
        settingsPreferences.settings.map { it.lyricsOffsetMs },
    ) { override, pref -> override ?: pref }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    fun openArtist(name: String, browseId: String? = null) {
        navigator.openArtist(name, browseId)
    }

    fun openAlbum(title: String, artist: String = "", browseId: String? = null) {
        navigator.openAlbum(title, artist, browseId)
    }

    fun setLyricsOffsetMs(offsetMs: Long) {
        val clamped = offsetMs.coerceIn(-10000L, 10000L)
        _liveLyricsOffsetMs.value = clamped
        viewModelScope.launch {
            settingsPreferences.setLyricsOffsetMs(clamped)
        }
    }

    fun setLyricsFontScale(scale: Float) {
        viewModelScope.launch {
            settingsPreferences.setLyricsFontScale(scale)
        }
    }

    private val _lyricsState = MutableStateFlow<LyricsUiState>(LyricsUiState.Idle)
    val lyricsState = _lyricsState.asStateFlow()

    // Type-safe track keys: the old "videoId|artist|title|flag" strings could
    // collide when a field itself contained '|' (or a null videoId met an empty
    // one), making one track inherit another's lyrics/canvas job suppression.
    private data class LyricsTrackKey(
        val videoId: String?,
        val artist: String,
        val title: String,
        val wordByWord: Boolean,
    )

    private data class CanvasTrackKey(
        val videoId: String?,
        val artist: String,
        val title: String,
        val enabled: Boolean,
        val cellular: Boolean,
    )

    private var currentTrackLyricsKey: LyricsTrackKey? = null
    private var lyricsJob: Job? = null
    private var spotLyricsOverride: Pair<com.tuned.app.data.local.LyricsProvider?, Set<com.tuned.app.data.local.LyricsProvider>> = Pair(null, emptySet())

    private val _canvasState = MutableStateFlow<com.tuned.app.data.canvas.CanvasArtwork?>(null)
    val canvasState = _canvasState.asStateFlow()
    private var currentCanvasTrackKey: CanvasTrackKey? = null
    private var canvasJob: Job? = null

    private companion object {
        /** Spinner only appears when loading actually takes time; cache
         *  hits resolve well inside this window with no flash. */
        const val LOADING_SPINNER_DELAY_MS = 250L
        /** Next-track canvas warm-up waits this long after the current track's
         *  lookup resolves. Rapid skips cancel the job inside this window, so a
         *  prefetch never spends network/decoder time on a track the user has
         *  already left behind. */
        const val CANVAS_PREFETCH_DELAY_MS = 4_000L
    }

    init {
        // Dynamic Now Playing follows the song loaded in the player even before the playback
        // service starts (a paused song restored at launch left the default accent showing).
        viewModelScope.launch {
            player.state
                .map { it.current?.title to it.current?.artist }
                .distinctUntilChanged()
                .collect { (title, artist) -> themeRepository.updatePlayerTrack(title, artist) }
        }
    }

    init {
        player.restoreSessionIfNeeded()
        viewModelScope.launch {
            try {
                playlistRepository.changes.collect {
                    if (customPlaylistsLoaded) refreshCustomPlaylists()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                // An uncaught flow error here used to kill the coroutine (and the
                // refresh chain with it) with no trace.
                android.util.Log.e("PlayerViewModel", "Playlist change stream failed", error)
            }
        }
        viewModelScope.launch {
            try {
                ytMusicLibraryManager.playlists.collect {
                    if (customPlaylistsLoaded) refreshCustomPlaylists()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                android.util.Log.e("PlayerViewModel", "Remote playlist stream failed", error)
            }
        }
        viewModelScope.launch {
            combine(
                player.chromeState.map { it.current }.distinctUntilChanged(),
                settingsPreferences.settings.map { it.wordByWordLyrics }.distinctUntilChanged(),
            ) { track, wordByWord -> track to wordByWord }.collect { (track, wordByWord) ->
                val key = track?.let { LyricsTrackKey(it.videoId, it.artist, it.title, wordByWord) }
                if (key != currentTrackLyricsKey) {
                    currentTrackLyricsKey = key
                    spotLyricsOverride = Pair(null, emptySet())
                    if (track != null) {
                        loadLyrics(track, forceRefresh = false)
                    } else {
                        lyricsJob?.cancel()
                        _lyricsState.value = LyricsUiState.Idle
                    }
                }
            }
        }
        viewModelScope.launch {
            combine(
                player.chromeState.map { it.current }.distinctUntilChanged(),
                settingsPreferences.settings.map { it.canvasEnabled to it.canvasOverCellular }.distinctUntilChanged(),
            ) { track, (enabled, cellular) -> Triple(track, enabled, cellular) }.collect { (track, enabled, cellular) ->
                val key = track?.let { CanvasTrackKey(it.videoId, it.artist, it.title, enabled, cellular) }
                if (key != currentCanvasTrackKey) {
                    currentCanvasTrackKey = key
                    canvasJob?.cancel()
                    if (track != null && enabled && track.mediaType != com.tuned.app.playback.MediaType.PODCAST_EPISODE) {
                        _canvasState.value = canvasRepository.cached(track)
                        canvasJob = viewModelScope.launch {
                            waitUntilAudible(track)
                            MetadataLog.delayed("canvas", track.videoId)
                            delay(MetadataLog.SETTLE_MS)
                            if (currentCanvasTrackKey != key) {
                                MetadataLog.skipped("canvas", track.videoId)
                                return@launch
                            }
                            MetadataLog.started("canvas", track.videoId)
                            val result = canvasRepository.canvasFor(track, cellularAllowed = cellular)
                            // A skip during the lookup cancels this job; only the
                            // winner publishes.
                            if (currentCanvasTrackKey != key) return@launch
                            _canvasState.value = result
                            // Warm the next track's cache entry while this one plays,
                            // so a skip usually lands on an in-memory hit instead of a
                            // cold network waterfall. Chained in this same job, strictly
                            // after the current lookup: the repository serializes on one
                            // mutex, so a standalone prefetch job could otherwise hold
                            // the lock inside a blocking HTTP call right when the next
                            // track needs it. The delay means rapid skips cancel before
                            // any prefetch traffic starts.
                            val upcoming = player.state.value.let { snapshot ->
                                if (snapshot.shuffleEnabled) {
                                    // Engine shuffle order is not visible here; warming
                                    // the wrong track would only spend lookups.
                                    null
                                } else {
                                    snapshot.queue.getOrNull(snapshot.currentIndex + 1)
                                }
                            }?.takeIf {
                                it.videoId != track.videoId || it.title != track.title || it.artist != track.artist
                            } ?: return@launch
                            delay(CANVAS_PREFETCH_DELAY_MS)
                            if (currentCanvasTrackKey != key) return@launch
                            // Result discarded: canvasFor caches it, which is the
                            // whole point. Never published to _canvasState here.
                            canvasRepository.canvasFor(upcoming, cellularAllowed = cellular)
                        }
                    } else {
                        _canvasState.value = null
                    }
                }
            }
        }
    }

    private suspend fun refreshCustomPlaylists() {
        try {
            _customPlaylists.value = (playlistRepository.getAll()
                .filter { it.mode == "custom" || it.mode == LIKED_SONGS_MODE }
                .sortedByDescending { it.mode == LIKED_SONGS_MODE } + ytMusicLibraryManager.playlists.value)
                .distinctBy { it.id }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            customPlaylistsLoaded = false
            android.util.Log.e("PlayerViewModel", "Couldn't refresh playlists", error)
        }
    }

    fun prepareCustomPlaylists() {
        if (customPlaylistsLoaded) return
        customPlaylistsLoaded = true
        viewModelScope.launch { refreshCustomPlaylists() }
    }

    /**
     * Holds lyrics and canvas fetches until this track is actually playing.
     * The state at subscription still describes the previous song, which is
     * not buffering, so the first value is skipped.
     */
    private suspend fun waitUntilAudible(track: PlayableTrack) {
        withTimeoutOrNull(8_000L) {
            player.state.drop(1).first { state ->
                val same = track.videoId.isNullOrBlank() || state.current?.videoId == track.videoId
                same && state.isPlaying && !state.isBuffering && state.error == null
            }
        }
    }

    fun selectLyricsProvider(provider: com.tuned.app.data.local.LyricsProvider) {
        val track = player.state.value.current ?: return
        spotLyricsOverride = Pair(provider, emptySet())
        loadLyrics(track, forceRefresh = true, overrideProvider = provider, excludedProviders = emptySet())
    }

    fun tryAnotherLyricsSource() {
        val track = player.state.value.current ?: return
        val currentSource = (_lyricsState.value as? LyricsUiState.Success)?.source
        val currentProvider = com.tuned.app.data.lyrics.providerForSource(currentSource)
        val newExcluded = if (currentProvider != null) {
            spotLyricsOverride.second + currentProvider
        } else {
            spotLyricsOverride.second
        }
        spotLyricsOverride = Pair(null, newExcluded)
        loadLyrics(track, forceRefresh = true, overrideProvider = null, excludedProviders = newExcluded)
    }

    fun loadLyrics(
        track: PlayableTrack,
        forceRefresh: Boolean = false,
        overrideProvider: com.tuned.app.data.local.LyricsProvider? = spotLyricsOverride.first,
        excludedProviders: Set<com.tuned.app.data.local.LyricsProvider> = spotLyricsOverride.second,
    ) {
        if (track.mediaType == com.tuned.app.playback.MediaType.PODCAST_EPISODE) {
            // Podcast episodes have no lyrics: skip every provider instead of
            // burning a lookup on a title that can never match.
            lyricsJob?.cancel()
            lyricsJob = null
            _lyricsState.value = LyricsUiState.Idle
            return
        }
        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch {
            // Lyrics hit other hosts (LRCLIB, Apple, SimpMusic). Starting
            // them in the same moment as youtubei/player steals connections
            // from the stream extract. Wait until this track is actually
            // playing, then fetch.
            waitUntilAudible(track)
            MetadataLog.delayed("lyrics", track.videoId)
            delay(MetadataLog.SETTLE_MS)
            MetadataLog.started("lyrics", track.videoId)
            // Hold the previous track's lyrics instead of flashing the
            // spinner on every change: cache hits resolve in milliseconds,
            // so the indicator only appears when loading actually takes time.
            val loadingIndicator = launch {
                delay(LOADING_SPINNER_DELAY_MS)
                _lyricsState.value = LyricsUiState.Loading
            }
            try {
                val durationSeconds = if (player.state.value.durationMs > 0) {
                    (player.state.value.durationMs / 1000).toInt()
                } else null

                val result = try {
                    lyricsRepository.getLyrics(
                        title = track.title,
                        artist = track.artist,
                        album = track.album,
                        durationSeconds = durationSeconds
                            ?: track.durationMs?.takeIf { it > 0 }?.let { (it / 1000).toInt() },
                        forceRefresh = forceRefresh,
                        wordByWord = settingsPreferences.settings.first().wordByWordLyrics,
                        videoId = track.videoId,
                        localLrcPath = com.tuned.app.data.generate.LocalAudioFormats
                            .importedPathOrNull(track.playbackUrl)
                            ?.let(com.tuned.app.data.generate.LocalAudioFormats::lrcPathFor)
                            // In-place tracks (content://): the sidecar text
                            // was stashed at scan time, keyed by the URL.
                            ?: com.tuned.app.data.localmusic.LocalLyricsStore
                                .pathIfExists(appContext, track.playbackUrl),
                        overrideProvider = overrideProvider,
                        excludedProviders = excludedProviders,
                        onPartialResult = { partial ->
                            withContext(Dispatchers.Main.immediate) {
                                coroutineContext.ensureActive()
                                publishLyrics(partial, wanted = track)
                            }
                        },
                    )
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    LyricsResult.Error(error.message ?: "Couldn't load lyrics")
                }
                coroutineContext.ensureActive()
                if (result is LyricsResult.Success || _lyricsState.value !is LyricsUiState.Success) {
                    publishLyrics(result, wanted = track)
                }
            } finally {
                loadingIndicator.cancel()
            }
        }
    }

    private fun publishLyrics(result: LyricsResult, wanted: PlayableTrack? = null) {
        // A superseded fetch (user skipped while it was in flight) must
        // never paint the previous song's lyrics over the new one. The job
        // cancel covers most of it; this covers the already-posted tail.
        if (wanted != null) {
            val current = player.state.value.current
            val same = if (!wanted.videoId.isNullOrBlank() || !current?.videoId.isNullOrBlank()) {
                !wanted.videoId.isNullOrBlank() && wanted.videoId == current?.videoId
            } else {
                current != null && current.title.equals(wanted.title, ignoreCase = true) &&
                    current.artist.equals(wanted.artist, ignoreCase = true)
            }
            if (!same) return
        }
        when (result) {
            is LyricsResult.Success -> {
                // Single funnel for everything the views draw, mirroring the
                // reference clients (Metrolist/SimpMusic): one provider
                // timestamp stays one drawn row. The timeline is de-overlapped
                // once so word fill, line focus and auto-scroll all read the
                // same edge-to-edge clock, then provider fragments are merged
                // into whitespace-true words so spacing and punctuation render
                // as authored. Rows are never merged or re-timed here — that
                // grouping is what broke line-by-line sync (two sung lines
                // shown as one, focus arriving late). Word-sync rows render
                // word-by-word; rows without syllables fall back to
                // line-by-line focus on the same clock.
                val lines = if (result.isSynced && result.lines.isNotEmpty() && !result.isInstrumental) {
                    com.tuned.app.ui.player.normalizeWordSpacing(
                        com.tuned.app.data.lyrics.LyricsRepository.normalizeLyricTiming(result.lines),
                    )
                } else result.lines
                _lyricsState.value = LyricsUiState.Success(
                    lines = lines,
                    isSynced = result.isSynced,
                    isWordSynced = result.isWordSynced || lines.any { it.hasSyllables },
                    plainLyrics = result.plainLyrics,
                    isInstrumental = result.isInstrumental,
                    source = result.source,
                )
            }
            is LyricsResult.Empty -> {
                _lyricsState.value = LyricsUiState.Empty
            }
            is LyricsResult.Error -> {
                _lyricsState.value = LyricsUiState.Error(result.message)
            }
        }
    }

    fun retryLyrics() {
        val track = player.state.value.current ?: return
        loadLyrics(track, forceRefresh = true)
    }

    fun addToPlaylists(
        playlistIds: Set<Long>,
        duplicatePlaylistIds: Set<Long>,
        track: PlayableTrack,
    ) {
        if (playlistIds.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                val generatedTrack = track.toGeneratedTrack()
                var remoteChanged = false
                var anyAdded = false
                playlistIds.forEach { playlistId ->
                    val allowDuplicate = playlistId in duplicatePlaylistIds
                    if (playlistId < 0L) {
                        val added = ytMusicLibraryManager.addTrack(playlistId, generatedTrack, allowDuplicate)
                        remoteChanged = remoteChanged || added
                        anyAdded = anyAdded || added
                    } else {
                        anyAdded = playlistRepository.addTrack(
                            id = playlistId,
                            track = generatedTrack,
                            allowDuplicate = allowDuplicate,
                        ) != null || anyAdded
                    }
                }
                if (remoteChanged) {
                    ytMusicLibraryManager.refresh()
                    refreshCustomPlaylists()
                }
                _toastMessage.emit(if (anyAdded) "Added to playlist" else "Couldn't add to playlist")
            }
        }
    }

    suspend fun findDuplicatePlaylistIds(playlistIds: Set<Long>, track: PlayableTrack): Set<Long> {
        val generatedTrack = track.toGeneratedTrack()
        val localDuplicates = _customPlaylists.value
            .filter { playlist ->
                playlist.id in playlistIds &&
                    playlist.remotePlaylistId == null &&
                    playlist.tracks.any {
                        it.key == generatedTrack.key ||
                            (it.name.equals(generatedTrack.name, ignoreCase = true) && it.artist.equals(generatedTrack.artist, ignoreCase = true))
                    }
            }
            .mapTo(mutableSetOf(), SavedPlaylist::id)
        val remoteIds = playlistIds.filterTo(mutableSetOf()) { it < 0L }
        return localDuplicates + ytMusicLibraryManager.findDuplicatePlaylistIds(remoteIds, generatedTrack)
    }

    suspend fun findCachedDuplicatePlaylistIds(playlistIds: Set<Long>, track: PlayableTrack): Set<Long> {
        val generatedTrack = track.toGeneratedTrack()
        val remoteIds = playlistIds.filterTo(mutableSetOf()) { it < 0L }
        return ytMusicLibraryManager.findCachedDuplicatePlaylistIds(remoteIds, generatedTrack)
    }

    fun createPlaylistAndAdd(title: String, track: PlayableTrack) {
        if (title.isBlank()) return
        viewModelScope.launch {
            runCatching {
                val playlist = playlistRepository.createCustom(title)
                playlistRepository.addTrack(playlist.id, track.toGeneratedTrack())
                _toastMessage.emit("Added to playlist")
            }
        }
    }

    fun toggleLiked(track: PlayableTrack) {
        viewModelScope.launch {
            // The like write reads Liked Songs, replaces its tracks, then
            // mirrors to Last.fm — any step can fail while a sync pass holds
            // the database. An uncaught exception in this launch takes the
            // process down, so report it like the other playlist mutations.
            val change = try {
                trackLikeController.toggle(track)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                android.util.Log.e("PlayerViewModel", "Like toggle failed", error)
                _toastMessage.tryEmit("Couldn't update Liked Songs")
                return@launch
            }
            reportYouTubeMirror(change)
        }
    }

    fun like(track: PlayableTrack) {
        viewModelScope.launch {
            val change = try {
                trackLikeController.like(track)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                android.util.Log.e("PlayerViewModel", "Like failed", error)
                _toastMessage.tryEmit("Couldn't update Liked Songs")
                return@launch
            }
            reportYouTubeMirror(change)
        }
    }

    private fun reportYouTubeMirror(change: com.tuned.app.data.playlist.LikeChange) {
        if (!change.youTubeMirrored) {
            _toastMessage.tryEmit(
                if (change.liked) "Couldn't like this song on YouTube Music"
                else "Couldn't remove the like on YouTube Music",
            )
        }
    }
}

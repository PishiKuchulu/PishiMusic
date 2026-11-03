package com.tuned.app.ui.artist

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuned.app.data.model.ArtistPageData
import com.tuned.app.data.repository.ArtistRepository
import com.tuned.app.playback.MusicPlayer
import com.tuned.app.playback.PlayableTrack
import com.tuned.app.ui.generate.MixLauncher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import com.tuned.app.data.artwork.ArtworkNormalizer
import com.tuned.app.data.canvas.CanvasArtwork
import com.tuned.app.data.canvas.CanvasRepository
import com.tuned.app.data.canvas.AppleArtistProfile
import com.tuned.app.data.canvas.AppleConcert
import com.tuned.app.data.library.FavoriteArtist
import com.tuned.app.data.library.FavoriteArtistsStore
import com.tuned.app.data.model.ArtistVideoItem
import javax.inject.Inject

import com.tuned.app.data.local.MiscSettings
import com.tuned.app.data.local.SettingsPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

sealed interface ArtistUiState {
    data object Loading : ArtistUiState
    data class Success(val data: ArtistPageData) : ArtistUiState
    data class Error(val message: String) : ArtistUiState
}

@HiltViewModel
class ArtistViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
    private val repository: ArtistRepository,
    private val musicPlayer: MusicPlayer,
    private val mixLauncher: MixLauncher,
    private val settingsPreferences: SettingsPreferences,
    private val canvasRepository: CanvasRepository,
    private val favoriteArtistsStore: FavoriteArtistsStore,
) : ViewModel() {

    val settings: StateFlow<MiscSettings> = settingsPreferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MiscSettings())

    private val _uiState = MutableStateFlow<ArtistUiState>(ArtistUiState.Loading)
    val uiState: StateFlow<ArtistUiState> = _uiState.asStateFlow()

    private val _artistCanvas = MutableStateFlow<CanvasArtwork?>(null)
    val artistCanvas: StateFlow<CanvasArtwork?> = _artistCanvas.asStateFlow()

    private val _profile = MutableStateFlow<AppleArtistProfile?>(null)
    /** Apple Music's photo, page colour and About details; null until found (or when Apple has none). */
    val profile: StateFlow<AppleArtistProfile?> = _profile.asStateFlow()

    val favorites: StateFlow<List<FavoriteArtist>> = favoriteArtistsStore.artists

    private val _profileSettled = MutableStateFlow(false)
    /** True once the Apple lookup answered, or after a short wait; the hero holds its title until then. */
    val profileSettled: StateFlow<Boolean> = _profileSettled.asStateFlow()

    private val _concerts = MutableStateFlow<List<AppleConcert>>(emptyList())
    /** Upcoming concerts from Apple Music, newest first as Apple lists them. */
    val concerts: StateFlow<List<AppleConcert>> = _concerts.asStateFlow()

    private var profileJob: Job? = null
    private var currentArtistName: String = ""
    private var currentBrowseId: String? = null
    private var loadJob: Job? = null
    private var canvasJob: Job? = null

    fun loadArtist(artistName: String, browseId: String? = null) {
        val cached = (_uiState.value as? ArtistUiState.Success)?.data
        if (artistName == currentArtistName && browseId == currentBrowseId &&
            (loadJob?.isActive == true || ArtworkNormalizer.isRealImage(cached?.artworkUrl))) {
            return
        }
        currentArtistName = artistName
        currentBrowseId = browseId

        canvasJob?.cancel()
        canvasJob = viewModelScope.launch {
            _artistCanvas.value = null
            try {
                val canvas = canvasRepository.canvasForArtist(artistName)
                coroutineContext.ensureActive()
                _artistCanvas.value = canvas
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Silently leave _artistCanvas null on network or resolution error
            }
        }

        // The hero's photo is decided before it loads; start that lookup now so it's ready.
        com.tuned.app.data.canvas.AppleArtistImages.prefetch(
            com.tuned.app.util.ArtistHelper.primaryArtist(artistName),
        )
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            _profile.value = null
            _profileSettled.value = false
            _concerts.value = emptyList()
            // Don't hold the title back for a slow network: show the name after a short wait,
            // and let a logo that arrives later cross-fade in.
            val settle = launch {
                kotlinx.coroutines.delay(PROFILE_WAIT_MS)
                _profileSettled.value = true
            }
            val profile = runCatching { canvasRepository.artistProfile(artistName) }.getOrNull()
            _profile.value = profile
            settle.cancel()
            _profileSettled.value = true
            profile?.id?.let { id -> _concerts.value = canvasRepository.artistConcerts(id) }
        }

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = ArtistUiState.Loading
            try {
                val data = repository.getArtistDetails(artistName, browseId) { initialData ->
                    coroutineContext.ensureActive()
                    _uiState.value = ArtistUiState.Success(initialData)
                }
                coroutineContext.ensureActive()
                _uiState.value = ArtistUiState.Success(data)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_uiState.value !is ArtistUiState.Success) {
                    _uiState.value = ArtistUiState.Error(e.message ?: appContext.getString(com.tuned.app.R.string.art_load_fail))
                }
            }
        }
    }

    fun playAll(startIndex: Int = 0) {
        val state = _uiState.value as? ArtistUiState.Success ?: return
        val songs = state.data.topSongs
        if (songs.isNotEmpty()) {
            musicPlayer.playQueue(songs, startIndex.coerceIn(0, songs.lastIndex), sourceLabel = state.data.name)
        }
    }

    fun playShuffle() {
        val state = _uiState.value as? ArtistUiState.Success ?: return
        val songs = state.data.topSongs
        if (songs.isNotEmpty()) {
            val shuffled = songs.shuffled()
            musicPlayer.playQueue(shuffled, 0, sourceLabel = state.data.name)
        }
    }

    fun startArtistMix() {
        val state = _uiState.value as? ArtistUiState.Success ?: return
        val artistName = state.data.name
        val topSongs = state.data.topSongs
        val firstTrack = topSongs.firstOrNull()

        viewModelScope.launch {
            val radioTracks = try {
                repository.getArtistRadio(artistName, firstTrack)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                emptyList()
            }

            val finalQueue = if (radioTracks.isNotEmpty()) {
                radioTracks
            } else if (topSongs.isNotEmpty()) {
                topSongs.shuffled()
            } else {
                emptyList()
            }

            if (finalQueue.isNotEmpty()) {
                musicPlayer.playQueue(finalQueue, startIndex = 0, sourceLabel = "$artistName Radio")
            }
        }
    }

    /** Stars or unstars the artist; returns true when it is now a favourite. */
    fun toggleFavorite(): Boolean {
        val data = (_uiState.value as? ArtistUiState.Success)?.data ?: return false
        return favoriteArtistsStore.toggle(
            name = data.name,
            browseId = data.browseId,
            artworkUrl = _profile.value?.photoUrl(600) ?: data.artworkUrl,
        )
    }

    /** Plays the artist's music videos in shelf order, starting at [video]. */
    fun playVideo(video: ArtistVideoItem) {
        val data = (_uiState.value as? ArtistUiState.Success)?.data ?: return
        val queue = data.videos.map { item ->
            PlayableTrack(
                title = item.title,
                artist = data.name,
                artworkUrl = item.thumbnailUrl,
                videoId = item.videoId,
            )
        }
        val start = data.videos.indexOf(video).coerceAtLeast(0)
        if (queue.isNotEmpty()) musicPlayer.playQueue(queue, start, sourceLabel = data.name)
    }

    private companion object {
        const val PROFILE_WAIT_MS = 1500L
    }
}

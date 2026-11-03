package com.tuned.app.ui.album

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuned.app.data.generate.GeneratedTrack
import com.tuned.app.data.model.AlbumPageData
import com.tuned.app.data.playlist.PlaylistRepository
import com.tuned.app.data.playlist.SavedPlaylist
import com.tuned.app.data.repository.AlbumRepository
import com.tuned.app.playback.MusicPlayer
import com.tuned.app.playback.PlayableTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import com.tuned.app.data.local.MiscSettings
import com.tuned.app.data.local.SettingsPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface AlbumUiState {
    data object Loading : AlbumUiState
    data class Success(val data: AlbumPageData) : AlbumUiState
    data class Error(val message: String) : AlbumUiState
}

@Immutable
data class AlbumSaveUiState(
    val isSaving: Boolean = false,
    val savedToLibrary: Boolean = false,
    val saveError: String? = null,
)

@HiltViewModel
class AlbumViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
    private val repository: AlbumRepository,
    private val playlistRepository: PlaylistRepository,
    private val musicPlayer: MusicPlayer,
    private val settingsPreferences: SettingsPreferences,
    private val canvasRepository: com.tuned.app.data.canvas.CanvasRepository,
) : ViewModel() {

    val settings: StateFlow<MiscSettings> = settingsPreferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MiscSettings())

    private val _uiState = MutableStateFlow<AlbumUiState>(AlbumUiState.Loading)
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    private val _saveUiState = MutableStateFlow(AlbumSaveUiState())
    val saveUiState: StateFlow<AlbumSaveUiState> = _saveUiState.asStateFlow()

    private val _appleInfo = MutableStateFlow<com.tuned.app.data.canvas.AppleAlbumInfo?>(null)
    /** Apple Music's notes, release details and shelves for this album, once found. */
    val appleInfo: StateFlow<com.tuned.app.data.canvas.AppleAlbumInfo?> = _appleInfo.asStateFlow()
    private var appleJob: Job? = null

    private val _albumCanvas = MutableStateFlow<com.tuned.app.data.canvas.CanvasArtwork?>(null)
    /** Apple Music's motion artwork for this album, when the Canvas setting is on and Apple has one. */
    val albumCanvas: StateFlow<com.tuned.app.data.canvas.CanvasArtwork?> = _albumCanvas.asStateFlow()

    private var currentAlbumTitle: String = ""
    private var currentArtistName: String = ""
    private var currentBrowseId: String? = null
    private var loadJob: Job? = null

    fun loadAlbum(albumTitle: String, artistName: String = "", browseId: String? = null) {
        if (albumTitle == currentAlbumTitle && artistName == currentArtistName && browseId == currentBrowseId && _uiState.value is AlbumUiState.Success) {
            return
        }
        currentAlbumTitle = albumTitle
        currentArtistName = artistName
        currentBrowseId = browseId
        _saveUiState.value = AlbumSaveUiState()
        _appleInfo.value = null
        _albumCanvas.value = null
        appleJob?.cancel()
        appleJob = null

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = AlbumUiState.Loading
            try {
                val data = repository.getAlbumDetails(albumTitle, artistName, browseId) { initialData ->
                    coroutineContext.ensureActive()
                    _uiState.value = AlbumUiState.Success(initialData)
                    viewModelScope.launch { refreshSavedState(initialData) }
                }
                coroutineContext.ensureActive()
                _uiState.value = AlbumUiState.Success(data)
                loadAppleInfo(data)
                refreshSavedState(data)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_uiState.value !is AlbumUiState.Success) {
                    _uiState.value = AlbumUiState.Error(e.message ?: appContext.getString(com.tuned.app.R.string.alb_load_fail))
                }
            }
        }
    }

    /** Looked up once the tracklist is final, so Apple's edition can be matched by track count. */
    private fun loadAppleInfo(data: AlbumPageData) {
        if (appleJob != null) return
        appleJob = viewModelScope.launch {
            launch {
                val prefs = settingsPreferences.settings.first()
                if (prefs.canvasEnabled) {
                    _albumCanvas.value = runCatching {
                        canvasRepository.canvasForAlbum(data.title, data.artist, cellularAllowed = prefs.canvasOverCellular)
                    }.getOrNull()
                }
            }
            _appleInfo.value = canvasRepository.albumInfo(data.title, data.artist, data.tracks.size.takeIf { it > 0 })
        }
    }

    /** Finds another edition of this album, then [onFound] opens it or [onMissing] says it isn't there. */
    fun openOtherVersion(title: String, artist: String, onFound: (String) -> Unit, onMissing: () -> Unit) {
        val current = (_uiState.value as? AlbumUiState.Success)?.data?.browseId.orEmpty()
        viewModelScope.launch {
            val browseId = repository.findOtherVersion(title, artist, current)
            if (browseId != null) onFound(browseId) else onMissing()
        }
    }

    /** Everything played from an album page wears the album's cover, not a track's single art. */
    private fun AlbumPageData.tracksWithCover(): List<PlayableTrack> =
        artworkUrl?.let { cover -> tracks.map { it.copy(artworkUrl = cover) } } ?: tracks

    fun playAll(startIndex: Int = 0) {
        val state = _uiState.value as? AlbumUiState.Success ?: return
        val tracks = state.data.tracksWithCover()
        if (tracks.isNotEmpty()) {
            musicPlayer.playQueue(
                tracks,
                startIndex.coerceIn(0, tracks.lastIndex),
                sourceLabel = state.data.title,
            )
        }
    }

    fun playShuffle() {
        val state = _uiState.value as? AlbumUiState.Success ?: return
        val tracks = state.data.tracksWithCover()
        if (tracks.isNotEmpty()) {
            val shuffled = tracks.shuffled()
            musicPlayer.playQueue(
                shuffled,
                0,
                sourceLabel = state.data.title,
            )
        }
    }

    /**
     * Whether [playlist] already holds this album: an exact track-identity
     * match, or any track overlap (a partial save from a progressive load).
     * Zero overlap means an unrelated user playlist that merely shares the
     * title — saving proceeds and may create a same-title sibling, exactly
     * like [PlaylistRepository.save]'s own title+mode dedupe philosophy.
     */
    private fun isAlbumCopy(playlist: SavedPlaylist, albumTracks: List<GeneratedTrack>): Boolean {
        if (albumTracks.isEmpty()) return false
        val albumKeys = albumTracks.mapTo(mutableSetOf()) { it.key }
        return playlist.tracks.any { it.key in albumKeys }
    }

    /** Reconciles the save button with the library on every load emission —
     *  a previously saved album must reopen as saved, never offer a
     *  duplicate. Yields while a save is in flight (that flow owns state). */
    private suspend fun refreshSavedState(data: AlbumPageData) {
        if (_saveUiState.value.isSaving || _saveUiState.value.savedToLibrary) return
        val title = data.title.ifBlank { return }
        val copy = runCatching { playlistRepository.findByTitle(title) }.getOrNull()
        if (copy != null && isAlbumCopy(copy, data.tracks.map { it.toGeneratedTrack() })) {
            _saveUiState.value = AlbumSaveUiState(savedToLibrary = true)
        }
    }

    /** One-tap save of the whole album to the library (issue #79) — the
     *  album equivalent of the feed playlist detail's "Save to library".
     *  Idempotent: an already-saved album just flips to saved, so a
     *  progressive tracklist can never stack same-title duplicates. */
    fun saveToLibrary() {
        val data = (_uiState.value as? AlbumUiState.Success)?.data ?: return
        if (data.tracks.isEmpty() || _saveUiState.value.isSaving || _saveUiState.value.savedToLibrary) return
        _saveUiState.value = AlbumSaveUiState(isSaving = true)
        viewModelScope.launch {
            try {
                val tracks = data.tracks.map { it.toGeneratedTrack() }
                val existing = runCatching { playlistRepository.findByTitle(data.title) }.getOrNull()
                if (existing == null || !isAlbumCopy(existing, tracks)) {
                    playlistRepository.save(
                        title = data.title.ifBlank { "Album" },
                        subtitle = data.artist.ifBlank { "YouTube Music" },
                        mode = "custom",
                        tracks = tracks,
                    )
                }
                _saveUiState.value = AlbumSaveUiState(savedToLibrary = true)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _saveUiState.value = AlbumSaveUiState(saveError = appContext.getString(com.tuned.app.R.string.alb_save_fail))
            }
        }
    }

    private fun PlayableTrack.toGeneratedTrack() = GeneratedTrack(
        name = title,
        artist = artist,
        artworkUrl = artworkUrl,
        album = album,
        url = videoId?.let { "https://music.youtube.com/watch?v=$it" }.orEmpty(),
    )
}

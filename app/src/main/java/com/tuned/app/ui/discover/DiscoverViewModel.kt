package com.tuned.app.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuned.app.data.artwork.ArtworkRepository
import com.tuned.app.data.discover.DiscoverRepository
import com.tuned.app.data.generate.GeneratedTrack
import com.tuned.app.data.naming.PlaylistNamer
import com.tuned.app.data.playlist.PlaylistRepository
import com.tuned.app.data.ytmusic.YtMusicAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import androidx.compose.runtime.Immutable

@Immutable
data class DiscoverUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val tracks: List<GeneratedTrack> = emptyList(),
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val saveResultMessage: String? = null,
    val isYtConnected: Boolean = false,
    val endReached: Boolean = false,
    /** A page fetch failed: the list keeps its content and the screen shows
     *  an inline retry row instead of the failure being silent. */
    val loadMoreError: Boolean = false,
)

/** Faithful port of discover.js (§7): infinite-scroll feed, pull-to-
 *  refresh, Surprise Me, and Save As Playlist (order-preserving snapshot
 *  of exactly what's currently rendered, with duplicate-signature
 *  detection). */
@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val repository: DiscoverRepository,
    private val playlistRepository: PlaylistRepository,
    private val artworkRepository: ArtworkRepository,
    private val ytMusicAuth: YtMusicAuthManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    /** The current feed operation. Reset-type operations (initial load,
     *  refresh, surprise) cancel each other so the last tap cannot be
     *  overwritten by an earlier, slower one; paging joins only when idle. */
    private var feedJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            ytMusicAuth.connection.collect { conn ->
                _uiState.update { it.copy(isYtConnected = conn.isConnected) }
            }
        }
        viewModelScope.launch {
            repository.feed.collect { feed ->
                if (feed.isNotEmpty() || _uiState.value.tracks.isNotEmpty()) {
                    _uiState.update {
                        it.copy(isLoading = if (feed.isNotEmpty()) false else it.isLoading, tracks = feed)
                    }
                }
            }
        }
        loadInitial()
    }

    fun loadInitial() {
        feedJob?.cancel()
        feedJob = viewModelScope.launch {
            val cached = repository.allowedCachedFeed()
            if (cached.isNotEmpty()) {
                _uiState.update { it.copy(isLoading = false, error = null, tracks = cached) }
            } else {
                _uiState.update { it.copy(isLoading = true, error = null, endReached = false) }
            }
            try {
                val needed = (INITIAL_BATCH_SIZE - cached.size).coerceAtLeast(0)
                val batch = if (needed > 0) repository.nextBatch(needed) else emptyList()
                val feed = repository.getCachedFeed()
                _uiState.update { it.copy(isLoading = false, tracks = feed) }
                artworkRepository.enrichBatch((cached + batch).take(INITIAL_BATCH_SIZE).map { it.name to it.artist })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_uiState.value.tracks.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, error = "Couldn't load recommendations") }
                }
            }
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoadingMore || _uiState.value.endReached) return
        if (_uiState.value.loadMoreError) return
        if (feedJob?.isActive == true) return
        feedJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, loadMoreError = false) }
            try {
                val more = repository.nextBatch(PAGE_SIZE)
                // An empty batch means the pool is exhausted — stop
                // auto-paging so sitting at the bottom doesn't re-trigger
                // loadMore on every size change in an endless network storm.
                _uiState.update { it.copy(isLoadingMore = false, endReached = more.isEmpty(), tracks = repository.getCachedFeed()) }
                artworkRepository.enrichBatch(more.map { it.name to it.artist })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingMore = false, loadMoreError = true) }
            }
        }
    }

    /** Manual retry from the inline error row (auto-paging stays disarmed
     *  while loadMoreError is set, so a failure cannot spin a retry storm). */
    fun retryLoadMore() {
        _uiState.update { it.copy(loadMoreError = false) }
        loadMore()
    }

    fun refresh() {
        feedJob?.cancel()
        feedJob = viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, endReached = false) }
            repository.reset()
            try {
                repository.nextBatch(INITIAL_BATCH_SIZE)
                _uiState.update { it.copy(isRefreshing = false, tracks = repository.getCachedFeed()) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isRefreshing = false, error = "Couldn't load recommendations") }
            }
        }
    }

    fun surpriseMe() {
        feedJob?.cancel()
        feedJob = viewModelScope.launch {
            repository.reset()
            _uiState.update { it.copy(isLoading = true, endReached = false) }
            try {
                repository.nextBatch(INITIAL_BATCH_SIZE)
                _uiState.update { it.copy(isLoading = false, tracks = repository.getCachedFeed()) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Couldn't load recommendations") }
            }
        }
    }

    /** §7.3 Save As Playlist: snapshots exactly what's currently rendered,
     *  no re-fetch. Order-preserving duplicate-signature guard. */
    fun saveAsPlaylist() {
        val tracks = _uiState.value.tracks
        if (tracks.isEmpty()) return
        viewModelScope.launch {
            // Every step here reads the playlist table. A failed read must
            // surface as "try again" — save() aborts rather than lose its
            // dedupe check, which used to stack a same-title sibling.
            try {
                val signature = playlistRepository.discoverSignature(tracks)
                val existing = playlistRepository.findByDiscoverSignature(signature)
                if (existing != null) {
                    _uiState.update { it.copy(saveResultMessage = "Already saved as \"${existing.title}\"") }
                    return@launch
                }
                val title = PlaylistNamer.generateUniqueName(playlistRepository.titles())
                val subtitle = PlaylistNamer.subtitleFor("discover")
                playlistRepository.save(title, subtitle, "discover", tracks, discoverSignature = signature)
                _uiState.update { it.copy(saveResultMessage = "Saved as \"$title\"") }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (e: Exception) {
                _uiState.update { it.copy(saveResultMessage = "Couldn't save playlist. Try again.") }
            }
        }
    }

    fun dismissSaveResult() = _uiState.update { it.copy(saveResultMessage = null) }
    fun dismissError() = _uiState.update { it.copy(error = null) }

    private companion object {
        const val INITIAL_BATCH_SIZE = 16
        const val PAGE_SIZE = 12
    }
}

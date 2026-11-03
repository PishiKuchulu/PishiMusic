package com.tuned.app.ui.podcast

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuned.app.data.podcast.PodcastEpisode
import com.tuned.app.data.podcast.PodcastRepository
import com.tuned.app.data.podcast.PodcastShelf
import com.tuned.app.data.podcast.toPlayableTrack
import com.tuned.app.playback.MusicPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Immutable
data class PodcastsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val shelves: List<PodcastShelf> = emptyList(),
    val error: String? = null,
    val query: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<PodcastEpisode> = emptyList(),
    /** A failed search is not "no episodes found": the screen offers retry. */
    val searchError: Boolean = false,
    val enabled: Boolean = true,
)

@HiltViewModel
class PodcastsViewModel @Inject constructor(
    private val repository: PodcastRepository,
    private val musicPlayer: MusicPlayer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PodcastsUiState())
    val uiState: StateFlow<PodcastsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            repository.enabled.collect { enabled ->
                _uiState.update { it.copy(enabled = enabled) }
                if (!enabled) {
                    loadJob?.cancel()
                    searchJob?.cancel()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            shelves = emptyList(),
                            searchResults = emptyList(),
                            isSearching = false,
                        )
                    }
                } else if (_uiState.value.shelves.isEmpty() && _uiState.value.error == null) {
                    load()
                }
            }
        }
    }

    fun load(forceRefresh: Boolean = false) {
        loadJob?.cancel()
        _uiState.update {
            it.copy(
                isLoading = it.shelves.isEmpty(),
                isRefreshing = forceRefresh,
                error = null,
            )
        }
        loadJob = viewModelScope.launch {
            try {
                val shelves = repository.home(forceRefresh)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        shelves = shelves,
                        // Empty is a real state (region/catalogue), not an
                        // error — the screen's empty branch renders it with
                        // the localized podcasts_empty string. Failures land
                        // in the catch below instead.
                        error = null,
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = error.message ?: "Couldn't load podcasts. Tap Retry.",
                    )
                }
            }
        }
    }

    fun refresh() = load(forceRefresh = true)

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(isSearching = false, searchResults = emptyList(), searchError = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            _uiState.update { it.copy(isSearching = true, searchError = false) }
            try {
                val results = repository.searchEpisodes(query)
                _uiState.update { it.copy(isSearching = false, searchResults = results, searchError = false) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.update { it.copy(isSearching = false, searchResults = emptyList(), searchError = true) }
            }
        }
    }

    fun clearQuery() = onQueryChange("")

    /** Re-runs the current search after a failure. */
    fun retrySearch() {
        val query = _uiState.value.query
        if (query.isNotBlank()) onQueryChange(query)
    }

    /** Plays one episode and keeps the visible list as the queue behind it. */
    fun playEpisode(episode: PodcastEpisode, queue: List<PodcastEpisode> = listOf(episode)) {
        viewModelScope.launch {
            val startPositionMs = withContext(Dispatchers.IO) {
                repository.resumePositionMs(episode)
            }
            val tracks = queue.map { it.toPlayableTrack() }
            val index = queue.indexOfFirst { it.videoId == episode.videoId }.coerceAtLeast(0)
            musicPlayer.playPodcastQueue(
                tracks = tracks,
                startIndex = index,
                startPositionMs = startPositionMs,
                sourceLabel = episode.showTitle?.takeIf { it.isNotBlank() } ?: "Podcasts",
            )
        }
    }

    fun playLatest(shelf: PodcastShelf) {
        val episode = shelf.episodes.firstOrNull() ?: return
        playEpisode(episode, shelf.episodes)
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 350L
    }
}

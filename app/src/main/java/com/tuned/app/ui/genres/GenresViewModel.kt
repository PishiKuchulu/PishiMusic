package com.tuned.app.ui.genres

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuned.app.data.generate.GenerateRepository
import com.tuned.app.data.generate.GeneratedTrack
import com.tuned.app.data.generate.GenreStat
import com.tuned.app.data.generate.GenresRepository
import com.tuned.app.data.naming.PlaylistNamer
import com.tuned.app.data.playlist.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Exact period options from genres.html's dropdown. */
val GENRE_PERIODS = listOf("overall" to "Overall", "7day" to "Past 7 days", "1month" to "This month", "12month" to "Last 12 months")

enum class GenreDetailSort { POPULAR, NEWEST, AZ }

@Immutable
data class GenresUiState(
    val isLoading: Boolean = true,
    val period: String = "overall",
    val stats: List<GenreStat> = emptyList(),
    val error: String? = null,
    // Detail sheet
    val detailGenre: String? = null,
    val detailTracks: List<GeneratedTrack> = emptyList(),
    val isDiscoverMode: Boolean = false,
    val detailLoading: Boolean = false,
    val detailPage: Int = 1,
    val detailSort: GenreDetailSort = GenreDetailSort.POPULAR,
    val detailHasMore: Boolean = true,
    /** A detail page fetch failed; paging pauses (but is NOT permanently
     *  closed) until the user retries. */
    val detailError: Boolean = false,
    val navigateToPlaylist: Boolean = false,
)

@HiltViewModel
class GenresViewModel @Inject constructor(
    private val genresRepository: GenresRepository,
    private val generateRepository: GenerateRepository,
    private val playlistRepository: PlaylistRepository,
    private val genreExplorer: GenreExplorer,
    private val musicPlayer: com.tuned.app.playback.MusicPlayer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GenresUiState())
    val uiState: StateFlow<GenresUiState> = _uiState.asStateFlow()

    init {
        loadGenres()
        // A genre tapped from any track's context menu app-wide (Home,
        // Discover, Playlist, Search) — see GenreExplorer's doc comment.
        // Consumed immediately so navigating back to this screen normally
        // doesn't reopen the sheet.
        genreExplorer.pendingGenre.value?.let { genre ->
            genreExplorer.consume()
            openDetail(genre)
        }
    }

    fun setPeriod(period: String) {
        if (period == _uiState.value.period) return
        _uiState.update { it.copy(period = period) }
        loadGenres()
    }

    /** Retry for the screen's error state. */
    fun retryLoad() = loadGenres()

    private fun loadGenres() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val stats = genresRepository.fetchGenreStats(_uiState.value.period)
                _uiState.update { it.copy(isLoading = false, stats = stats) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Couldn't load genres") }
            }
        }
    }

    fun openDetail(genre: String) {
        _uiState.update {
            it.copy(
                detailGenre = genre,
                detailTracks = emptyList(),
                isDiscoverMode = false,
                detailLoading = true,
                detailPage = 1,
                detailSort = GenreDetailSort.POPULAR,
                detailHasMore = true,
                detailError = false,
            )
        }
        loadDetailPage(reset = true)
    }

    fun closeDetail() {
        _uiState.update { it.copy(detailGenre = null, detailTracks = emptyList(), isDiscoverMode = false) }
    }

    fun setDetailSort(sort: GenreDetailSort) {
        if (sort == _uiState.value.detailSort) return
        _uiState.update { it.copy(detailSort = sort) }
        when (sort) {
            GenreDetailSort.AZ ->
                _uiState.update { it.copy(detailTracks = it.detailTracks.sortedBy { t -> t.name.lowercase() }) }
            // NEWEST used to be declared but never implemented; tracks carry
            // a tag year, which is the only date signal available.
            GenreDetailSort.NEWEST ->
                _uiState.update {
                    it.copy(
                        detailTracks = it.detailTracks.sortedWith(
                            compareByDescending<GeneratedTrack> { t -> t.year?.toIntOrNull() ?: 0 }
                                .thenBy { t -> t.name.lowercase() },
                        ),
                    )
                }
            // POPULAR is the repository's own order; once another sort has
            // reordered the list in place, only a refetch restores it.
            GenreDetailSort.POPULAR -> if (_uiState.value.detailGenre != null && !_uiState.value.isDiscoverMode) {
                loadDetailPage(reset = true)
            }
        }
    }

    fun showYourTracks(genre: String) {
        _uiState.update {
            it.copy(
                isDiscoverMode = false,
                detailTracks = emptyList(),
                detailLoading = true,
                detailPage = 1,
                detailHasMore = true,
                detailError = false,
            )
        }
        loadDetailPage(reset = true)
    }

    fun loadDetailPage(reset: Boolean = false) {
        val genre = _uiState.value.detailGenre ?: return
        if (_uiState.value.isDiscoverMode) return
        if (_uiState.value.detailLoading && !reset) return
        if (!reset && !_uiState.value.detailHasMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(detailLoading = true) }
            try {
                val page = if (reset) 1 else _uiState.value.detailPage
                val fetched = genresRepository.fetchGenreTracks(genre, page)
                _uiState.update { s ->
                    // The user may have opened another genre while this page
                    // was in flight; its tracks must not land in the new sheet.
                    if (s.detailGenre != genre) s
                    else {
                        val merged = if (reset) fetched else s.detailTracks + fetched
                        val ordered = when (s.detailSort) {
                            GenreDetailSort.AZ -> merged.sortedBy { it.name.lowercase() }
                            GenreDetailSort.NEWEST -> merged.sortedWith(
                                compareByDescending<GeneratedTrack> { t -> t.year?.toIntOrNull() ?: 0 }
                                    .thenBy { t -> t.name.lowercase() },
                            )
                            GenreDetailSort.POPULAR -> merged
                        }
                        s.copy(detailTracks = ordered, detailLoading = false, detailError = false, detailPage = page + 1, detailHasMore = fetched.isNotEmpty())
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // A transient failure pauses paging with a retry; it used to
                // set detailHasMore=false and kill paging for the whole sheet.
                _uiState.update { s ->
                    if (s.detailGenre != genre) s else s.copy(detailLoading = false, detailError = true)
                }
            }
        }
    }

    /** §5.3's "Start Mix" — instantly begins playback of the visible genre queue and saves to playlists. */
    fun startMix(genre: String) {
        val currentTracks = _uiState.value.detailTracks
        val isDiscover = _uiState.value.isDiscoverMode
        viewModelScope.launch {
            _uiState.update { it.copy(detailLoading = true) }
            try {
                val tracks = if (currentTracks.isNotEmpty()) {
                    currentTracks
                } else {
                    genresRepository.explorePersonalizedGenre(genre)
                }
                val playable = tracks.map { track ->
                    com.tuned.app.playback.PlayableTrack(
                        title = track.name,
                        artist = track.artist,
                        album = track.album,
                        artworkUrl = track.artworkUrl,
                    )
                }
                if (playable.isNotEmpty()) {
                    musicPlayer.playQueue(
                        playable,
                        startIndex = 0,
                        sourceLabel = "${genre.replaceFirstChar { it.uppercase() }} ${if (isDiscover) "Discovery" else "Mix"}",
                    )
                }
                val finalTracks = generateRepository.precheck(tracks).take(35).ifEmpty { tracks.take(35) }
                val title = PlaylistNamer.generateUniqueName(playlistRepository.titles())
                val subtitle = PlaylistNamer.subtitleFor("tag", tagInput = genre)
                playlistRepository.save(title, subtitle, "tag", finalTracks)
                _uiState.update { it.copy(detailGenre = null, detailLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(detailLoading = false, error = e.message) }
            }
        }
    }

    /** §5.5 "Discover More" — directly loads and displays 30-35 fresh undiscovered genre recommendations. */
    fun discoverMore(genre: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(detailLoading = true, isDiscoverMode = true) }
            try {
                val fresh = genresRepository.discoverMore(genre)
                _uiState.update { s ->
                    s.copy(
                        detailTracks = fresh.ifEmpty { s.detailTracks },
                        isDiscoverMode = true,
                        detailLoading = false,
                        detailHasMore = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(detailLoading = false, error = e.message ?: "Failed to discover genre tracks") }
            }
        }
    }

    /** §5.4 "Explore This Genre" — reachable from any track's context menu app-wide. */
    fun exploreGenre(genre: String) {
        viewModelScope.launch {
            try {
                val tracks = genresRepository.explorePersonalizedGenre(genre)
                val title = PlaylistNamer.generateUniqueName(playlistRepository.titles())
                val subtitle = PlaylistNamer.subtitleFor("tag", tagInput = genre)
                playlistRepository.save(title, subtitle, "tag", tracks)
                _uiState.update { it.copy(navigateToPlaylist = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun consumeNavigateToPlaylist() = _uiState.update { it.copy(navigateToPlaylist = false) }
    fun dismissError() = _uiState.update { it.copy(error = null) }
}

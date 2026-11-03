package com.tuned.app.ui.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuned.app.data.local.SettingsPreferences
import com.tuned.app.data.repository.HomeArtistItem
import com.tuned.app.data.repository.HomeRepository
import com.tuned.app.data.repository.HomeStats
import com.tuned.app.data.repository.HomeTrack
import com.tuned.app.playback.MusicPlayer
import com.tuned.app.playback.PlayableTrack
import com.tuned.app.ui.generate.MixLauncher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FriendProfileTab(val labelRes: Int) {
    RECENT(com.tuned.app.R.string.fr_tab_recent),
    TOP_TRACKS(com.tuned.app.R.string.fr_tab_tracks),
    TOP_ARTISTS(com.tuned.app.R.string.fr_tab_artists),
}

@Immutable
data class FriendProfileUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val username: String = "",
    val displayName: String = "",
    val avatarUrl: String? = null,
    val isPinned: Boolean = false,
    val stats: HomeStats? = null,
    val nowPlaying: HomeTrack? = null,
    val recentTracks: List<HomeTrack> = emptyList(),
    val topTracksOverall: List<HomeTrack> = emptyList(),
    val topTracks7Days: List<HomeTrack> = emptyList(),
    val topTracks30Days: List<HomeTrack> = emptyList(),
    val topArtists: List<HomeArtistItem> = emptyList(),
    val selectedTab: FriendProfileTab = FriendProfileTab.RECENT,
    val selectedPeriod: String = "overall", // "7day", "1month", "overall"
    val error: String? = null,
)

@HiltViewModel
class FriendProfileViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
    private val homeRepository: HomeRepository,
    private val settingsPreferences: SettingsPreferences,
    private val musicPlayer: MusicPlayer,
    private val mixLauncher: MixLauncher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendProfileUiState())
    val uiState: StateFlow<FriendProfileUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private var loadJob: Job? = null
    private var currentUsername: String = ""

    init {
        viewModelScope.launch {
            settingsPreferences.settings.collect { settings ->
                val pinned = currentUsername.isNotBlank() && currentUsername in settings.pinnedFriends
                _uiState.update { it.copy(isPinned = pinned) }
            }
        }
    }

    fun loadFriend(username: String, initialDisplayName: String? = null, initialAvatarUrl: String? = null) {
        if (username.isBlank()) return
        // After a failed load the same username must be retryable, not
        // swallowed by the already-loaded guard.
        if (username == currentUsername && !_uiState.value.isLoading && _uiState.value.error == null) return
        currentUsername = username

        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                username = username,
                displayName = initialDisplayName?.takeIf(String::isNotBlank) ?: username,
                avatarUrl = initialAvatarUrl,
            )
        }

        loadAllData(forceRefresh = false)
        startPolling()
    }

    fun refresh() {
        if (currentUsername.isBlank()) return
        _uiState.update { it.copy(isRefreshing = true) }
        loadAllData(forceRefresh = true)
    }

    private fun loadAllData(forceRefresh: Boolean) {
        val username = currentUsername
        // A new load supersedes the old one: cancelling also cancels the
        // per-period child loads, so friend A's results can no longer land
        // on friend B's profile.
        loadJob?.cancel()
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            val initialResult = homeRepository.fetchInitialData(username = username, forceRefresh = forceRefresh)
            initialResult.fold(
                onSuccess = { data ->
                    val (nowPlaying, recentTracks) = parseRecent(data.recent.nowPlaying, data.recent.tracks, data.topTracks)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            stats = data.stats,
                            avatarUrl = it.avatarUrl ?: data.stats.avatarUrl,
                            nowPlaying = nowPlaying,
                            recentTracks = recentTracks,
                            topTracksOverall = data.topTracks,
                            error = null,
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = err.message ?: appContext.getString(com.tuned.app.R.string.fr_load_error),
                        )
                    }
                }
            )

            // Concurrently load 7-day, 30-day top tracks and top artists
            launch {
                homeRepository.fetchTopTracksForPeriod("7day", 30, username = username).onSuccess { tracks ->
                    _uiState.update { it.copy(topTracks7Days = tracks) }
                }
            }
            launch {
                homeRepository.fetchTopTracksForPeriod("1month", 30, username = username).onSuccess { tracks ->
                    _uiState.update { it.copy(topTracks30Days = tracks) }
                }
            }
            launch {
                homeRepository.fetchTopArtistsForPeriod("overall", 30, username = username).onSuccess { artists ->
                    _uiState.update { it.copy(topArtists = artists) }
                }
            }
        }
    }

    private fun parseRecent(
        nowPlayingRaw: com.tuned.app.data.model.RecentTrack?,
        recentList: List<com.tuned.app.data.model.RecentTrack>,
        topTracks: List<HomeTrack>,
    ): Pair<HomeTrack?, List<HomeTrack>> {
        val topCountByKey = topTracks.associate { "${it.name.lowercase()}|${it.artist.lowercase()}" to it.playCount }
        val recentTracks = recentList.map { t ->
            val key = "${t.name.lowercase()}|${t.artist.displayName.lowercase()}"
            HomeTrack(
                name = t.name,
                artist = t.artist.displayName,
                artworkUrl = t.artworkUrl,
                timestampMillis = t.date?.uts?.toLongOrNull()?.times(1000),
                playCount = topCountByKey[key] ?: 0,
            )
        }
        val nowPlaying = nowPlayingRaw?.let { np ->
            HomeTrack(
                name = np.name,
                artist = np.artist.displayName,
                artworkUrl = np.artworkUrl,
                timestampMillis = System.currentTimeMillis(),
                playCount = 0,
                isNowPlaying = true,
            )
        }
        return nowPlaying to recentTracks
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch(Dispatchers.IO) {
            var consecutiveFailures = 0
            while (isActive) {
                // Back off after failures instead of hammering a failing
                // endpoint every 20s forever.
                delay(if (consecutiveFailures == 0) 20_000L else 60_000L)
                val username = currentUsername
                if (username.isNotBlank()) {
                    val result = runCatching {
                        homeRepository.fetchRecentTracks(limit = 2, username = username, forceRefresh = true)
                    }.getOrNull()
                    if (result == null) {
                        consecutiveFailures++
                    } else {
                        result.onSuccess { page ->
                            consecutiveFailures = 0
                            val (np, _) = parseRecent(page.nowPlaying, emptyList(), _uiState.value.topTracksOverall)
                            _uiState.update { state ->
                                // Keep the previous timestamp for the same
                                // track: parseRecent stamps "now", which made
                                // the row recompose on every poll tick.
                                val prev = state.nowPlaying
                                val stable = if (np != null && prev != null && np.name == prev.name && np.artist == prev.artist) {
                                    np.copy(timestampMillis = prev.timestampMillis)
                                } else np
                                if (stable == prev) state else state.copy(nowPlaying = stable)
                            }
                        }.onFailure { consecutiveFailures++ }
                    }
                }
            }
        }
    }

    fun setTab(tab: FriendProfileTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setPeriod(period: String) {
        _uiState.update { it.copy(selectedPeriod = period) }
    }

    fun togglePinned() {
        val username = currentUsername
        if (username.isNotBlank()) {
            viewModelScope.launch {
                // The settings collector in init mirrors the pinned set into
                // isPinned; flipping the state here as well raced that
                // collector and could leave the pin showing the wrong state.
                settingsPreferences.toggleFriendPinned(username)
            }
        }
    }

    fun playTrack(track: HomeTrack) {
        musicPlayer.play(
            PlayableTrack(
                title = track.name,
                artist = track.artist,
                artworkUrl = track.artworkUrl,
                videoId = track.videoId,
            ),
            sourceLabel = "${_uiState.value.displayName}'s Profile",
        )
    }

    fun playQueue(tracks: List<HomeTrack>, startIndex: Int = 0) {
        val playable = tracks.map {
            PlayableTrack(
                title = it.name,
                artist = it.artist,
                artworkUrl = it.artworkUrl,
                videoId = it.videoId,
            )
        }
        if (playable.isNotEmpty()) {
            musicPlayer.playQueue(
                tracks = playable,
                startIndex = startIndex.coerceIn(0, playable.lastIndex),
                sourceLabel = "${_uiState.value.displayName}'s Profile",
            )
        }
    }

    fun startMix(trackName: String, artistName: String) {
        mixLauncher.startMix(trackName, artistName)
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}

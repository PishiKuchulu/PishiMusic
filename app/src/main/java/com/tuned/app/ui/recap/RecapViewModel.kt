package com.tuned.app.ui.recap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuned.app.data.local.db.YearlyListenDao
import com.tuned.app.data.local.db.YearlyListenEntity
import com.tuned.app.util.ArtistHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class RecapArtist(val name: String, val minutes: Long, val artworkUrl: String?)

data class RecapState(
    val isLoading: Boolean = true,
    val year: Int = Calendar.getInstance().get(Calendar.YEAR),
    val years: List<Int> = emptyList(),
    val minutes: Long = 0L,
    val songCount: Int = 0,
    val artistCount: Int = 0,
    val topSongs: List<YearlyListenEntity> = emptyList(),
    val topArtists: List<RecapArtist> = emptyList(),
    /** Set when the year's read failed; the screen must not read it as "no listening". */
    val error: String? = null,
)

private const val TOP_COUNT = 5
private const val MS_PER_MINUTE = 60_000L

/** The sums behind a recap. Pure, so they can be checked without a database. */
internal fun buildRecap(year: Int, years: List<Int>, rows: List<YearlyListenEntity>): RecapState {
    val byArtist = rows.groupBy { ArtistHelper.primaryArtist(it.artist) }
        .map { (name, songs) ->
            RecapArtist(
                name = name,
                minutes = songs.sumOf { it.listenedMs } / MS_PER_MINUTE,
                artworkUrl = songs.maxByOrNull { it.listenedMs }?.artworkUrl,
            )
        }
        .sortedByDescending { it.minutes }
    return RecapState(
        isLoading = false,
        year = year,
        years = years,
        minutes = rows.sumOf { it.listenedMs } / MS_PER_MINUTE,
        songCount = rows.size,
        artistCount = byArtist.size,
        topSongs = rows.sortedByDescending { it.listenedMs }.take(TOP_COUNT),
        topArtists = byArtist.take(TOP_COUNT),
    )
}

@HiltViewModel
class RecapViewModel @Inject constructor(
    private val dao: YearlyListenDao,
    settingsPreferences: com.tuned.app.data.local.SettingsPreferences,
) : ViewModel() {
    /** The Fluid artwork setting: the recap's backdrop follows it, like the player's. */
    val movingBackground: StateFlow<Boolean> = settingsPreferences.settings
        .map { it.rotatingBackgroundEnabled }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _state = MutableStateFlow(RecapState())
    val state: StateFlow<RecapState> = _state.asStateFlow()

    private var showJob: kotlinx.coroutines.Job? = null

    init {
        show(_state.value.year)
    }

    fun show(year: Int) {
        // Last tap wins: the old code launched an unstored job per tap, so
        // rapid year switches could finish out of order and leave a stale
        // year on screen; it also never set loading and swallowed DAO
        // failures into a silent empty recap.
        showJob?.cancel()
        _state.value = _state.value.copy(isLoading = true, error = null, year = year)
        showJob = viewModelScope.launch {
            try {
                _state.value = buildRecap(year, dao.years(), dao.forYear(year))
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Couldn't load this year's recap.",
                )
            }
        }
    }
}

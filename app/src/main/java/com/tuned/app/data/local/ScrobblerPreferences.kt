package com.tuned.app.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class ScrobblerSettings(
    /** Master switch — MusicPlaybackService only submits anything when
     *  this is on (and a Last.fm session key exists; enabling it from
     *  Settings first collects one via the password dialog). */
    val enabled: Boolean = false,
    /** Mirrors Last.fm's own scrobble.updateNowPlaying call — shows "now
     *  playing" on your profile immediately, separate from the actual
     *  scrobble which only submits once the track has played long enough. */
    val submitNowPlaying: Boolean = true,
    /** Percent of the track that must play before it's scrobbled, same
     *  idea as Pano Scrobbler's "Scrobble delay" percent slider. Last.fm's
     *  own rule (whichever comes first) is respected on top of this: a
     *  track must also be longer than 30s, and is capped at needing at
     *  most 4 minutes of playback regardless of this percent. */
    val scrobblePercent: Int = 50,
)

@Singleton
class ScrobblerPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val ENABLED = booleanPreferencesKey("lw_scrobbler_enabled")
        val SUBMIT_NOW_PLAYING = booleanPreferencesKey("lw_scrobbler_now_playing")
        val PERCENT = intPreferencesKey("lw_scrobbler_percent")
    }

    val settings: Flow<ScrobblerSettings> = dataStore.data
        .recoverPreferences("ScrobblerPreferences")
        .map { p ->
            ScrobblerSettings(
                enabled = p.readSafely(Keys.ENABLED) ?: false,
                submitNowPlaying = p.readSafely(Keys.SUBMIT_NOW_PLAYING) ?: true,
                // Old/user-edited backups can contain values outside the Slider's
                // range. Never pass an invalid persisted value into Compose.
                scrobblePercent = (p.readSafely(Keys.PERCENT) ?: 50).coerceIn(25, 90),
            )
        }

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.ENABLED] = enabled }
    }

    suspend fun setSubmitNowPlaying(enabled: Boolean) {
        dataStore.edit { it[Keys.SUBMIT_NOW_PLAYING] = enabled }
    }

    suspend fun setScrobblePercent(percent: Int) {
        dataStore.edit { it[Keys.PERCENT] = percent.coerceIn(25, 90) }
    }
}

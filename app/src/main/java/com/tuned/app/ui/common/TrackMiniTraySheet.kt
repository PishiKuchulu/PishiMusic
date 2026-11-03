package com.tuned.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tuned.app.playback.PlayableTrack

/**
 * The menu opened by a long press on a track (Home, Search, feed playlists). It is the same
 * song menu as the ⋮ button ([TrackContextMenuSheet]), so a song has one menu everywhere.
 *
 * [onPlay] overrides the default single-track play so list screens can keep their queue
 * context (e.g. Home plays the whole visible queue from this row, Feed plays its shelf from
 * this index). When null, the menu plays the song on its own.
 */
data class TrackMiniTrayData(
    val title: String,
    val artist: String,
    val album: String? = null,
    val artworkUrl: String? = null,
    val videoId: String? = null,
    val sourceLabel: String = "Tuned",
    val onPlay: (() -> Unit)? = null,
) {
    fun toPlayable(): PlayableTrack = PlayableTrack(
        title = title,
        artist = artist,
        album = album,
        artworkUrl = artworkUrl,
        videoId = videoId?.takeIf(String::isNotBlank),
    )
}

@Composable
fun TrackMiniTraySheet(
    data: TrackMiniTrayData,
    onDismiss: () -> Unit,
) {
    val playable = remember(data) { data.toPlayable() }
    TrackContextMenuSheet(
        target = TrackMenuTarget.Track(data.title, data.artist, ""),
        capabilities = TrackMenuCapabilities(showCopyActions = true, showDeleteScrobble = false),
        playableTrack = playable,
        playbackSourceLabel = data.sourceLabel,
        onPlayInTuned = data.onPlay,
        onDismiss = onDismiss,
    )
}

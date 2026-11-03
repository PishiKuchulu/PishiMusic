package com.tuned.app.playback

import com.tuned.app.data.generate.GeneratedTrack
import com.tuned.app.data.generate.LocalAudioFormats

internal fun PlayableTrack.toGeneratedTrack() = GeneratedTrack(
    name = title,
    artist = artist,
    artworkUrl = artworkUrl,
    album = album,
    // Local songs: an on-device track keeps its path / content reference
    // so any playlist it is added to plays the original file in place
    // instead of a YouTube search.
    url = LocalAudioFormats.localReferenceOrNull(playbackUrl)
        ?: videoId?.let { "https://music.youtube.com/watch?v=$it" }.orEmpty(),
)

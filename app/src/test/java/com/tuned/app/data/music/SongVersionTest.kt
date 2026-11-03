package com.tuned.app.data.music

import com.google.common.truth.Truth.assertThat
import com.tuned.app.data.artwork.ArtworkNormalizer
import org.junit.Test

class SongVersionTest {
    private fun track(id: String, title: String, artist: String = "Tame Impala", video: Boolean? = false, seconds: Int? = 200) =
        YouTubeMusicTrack(id, title, artist, null, null, seconds, isVideo = video)

    private val video = track("vid", "Let It Happen (Official Video)", video = true, seconds = 257)

    @Test
    fun videoTitlesLoseTheirExtras() {
        assertThat(songTitleFromVideoTitle("Let It Happen (Official Video)", "Tame Impala")).isEqualTo("Let It Happen")
        assertThat(songTitleFromVideoTitle("Tame Impala - Let It Happen [Official Audio]", "Tame Impala")).isEqualTo("Let It Happen")
        assertThat(songTitleFromVideoTitle("Song (Remastered 2011)", "X")).isEqualTo("Song")
    }

    @Test
    fun theStudioSongMatchesItsOfficialVideo() {
        assertThat(isSongVersionOf(video, track("song", "Let It Happen", seconds = 467))).isFalse()
        assertThat(isSongVersionOf(video, track("song", "Let It Happen", seconds = 250))).isTrue()
    }

    @Test
    fun aLiveTakeKeepsItsOwnAudio() {
        val live = track("vid", "Let It Happen (Live at Glastonbury)", video = true)
        assertThat(isSongVersionOf(live, track("song", "Let It Happen"))).isFalse()
    }

    @Test
    fun anotherArtistOrAnotherVideoIsNotTheSong() {
        assertThat(isSongVersionOf(video, track("song", "Let It Happen", artist = "Someone Else", seconds = 250))).isFalse()
        assertThat(isSongVersionOf(video, track("other", "Let It Happen", video = true, seconds = 250))).isFalse()
        assertThat(isSongVersionOf(video, track("song", "Eventually", seconds = 250))).isFalse()
    }

    @Test
    fun theCoverPlayedFromBeatsATitleLookup() {
        val album = "https://lh3.googleusercontent.com/album=w120-h120"
        val single = "https://example.com/single.jpg"
        assertThat(ArtworkNormalizer.coverFor(album, single)).doesNotContain("example.com")
        assertThat(ArtworkNormalizer.coverFor("https://i.ytimg.com/vi/x/hq.jpg", single)).isEqualTo(single)
        assertThat(ArtworkNormalizer.coverFor(null, single)).isEqualTo(single)
    }
}

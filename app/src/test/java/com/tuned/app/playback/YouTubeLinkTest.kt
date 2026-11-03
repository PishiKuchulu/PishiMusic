package com.tuned.app.playback

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class YouTubeLinkTest {
    @Test
    fun albumLinksAreAlbums() {
        assertThat(parseYouTubeLink("https://music.youtube.com/playlist?list=OLAK5uy_abc&si=x"))
            .isEqualTo(YouTubeLink.Album("OLAK5uy_abc"))
        assertThat(parseYouTubeLink("https://music.youtube.com/browse/MPREb_abc"))
            .isEqualTo(YouTubeLink.Album("MPREb_abc"))
    }

    @Test
    fun playlistLinksArePlaylists() {
        assertThat(parseYouTubeLink("https://music.youtube.com/playlist?list=PL123"))
            .isEqualTo(YouTubeLink.Playlist("PL123"))
        assertThat(parseYouTubeLink("https://www.youtube.com/playlist?list=RDCLAK5uy_x"))
            .isEqualTo(YouTubeLink.Playlist("RDCLAK5uy_x"))
        assertThat(parseYouTubeLink("https://music.youtube.com/browse/VLPL123"))
            .isEqualTo(YouTubeLink.Playlist("PL123"))
    }

    @Test
    fun aSongInsideAListStillPlaysThatSong() {
        assertThat(parseYouTubeLink("https://music.youtube.com/watch?v=abc123&list=OLAK5uy_x"))
            .isEqualTo(YouTubeLink.Track("abc123", "OLAK5uy_x"))
        assertThat(parseYouTubeLink("https://music.youtube.com/watch?v=abc123&si=x"))
            .isEqualTo(YouTubeLink.Track("abc123", null))
    }

    @Test
    fun shortLinksAndShorts() {
        assertThat(parseYouTubeLink("https://youtu.be/abc123?si=x")).isEqualTo(YouTubeLink.Track("abc123"))
        assertThat(parseYouTubeLink("https://www.youtube.com/shorts/abc123")).isEqualTo(YouTubeLink.Track("abc123"))
    }

    @Test
    fun artistLinksAreArtists() {
        assertThat(parseYouTubeLink("https://music.youtube.com/channel/UCabc"))
            .isEqualTo(YouTubeLink.Artist("UCabc"))
    }

    @Test
    fun otherLinksAreNotOurs() {
        assertThat(parseYouTubeLink("https://music.youtube.com/")).isNull()
        assertThat(parseYouTubeLink("https://example.com/playlist?list=PL1")).isNull()
        assertThat(parseYouTubeLink("just some text")).isNull()
    }
}

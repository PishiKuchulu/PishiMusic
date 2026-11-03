package com.tuned.app.ui.playlist

import com.google.common.truth.Truth.assertThat
import com.tuned.app.data.generate.GeneratedTrack
import com.tuned.app.data.localmusic.LocalMusicNaming
import org.junit.Test

class LocalAlbumsTest {
    private fun song(name: String, album: String?, number: Int? = null, art: String? = null, artist: String = "Kanye West") =
        GeneratedTrack(name = name, artist = artist, album = album, trackNumber = number, artworkUrl = art, url = "/$name.mp3")

    @Test
    fun songsGroupByAlbumInTrackOrder() {
        val albums = localAlbums(listOf(song("B", "Yandhi", 2), song("A", "yandhi ", 1), song("C", "Other")))
        assertThat(albums.map { it.title }).containsExactly("Other", "Yandhi").inOrder()
        assertThat(albums[1].tracks.map { it.name }).containsExactly("A", "B").inOrder()
    }

    @Test
    fun missingNumbersKeepFileOrderAfterNumberedSongs() {
        val album = localAlbums(listOf(song("X", "Y"), song("Z", "Y"), song("One", "Y", 1))).single()
        assertThat(album.tracks.map { it.name }).containsExactly("One", "X", "Z").inOrder()
    }

    @Test
    fun songsWithNoAlbumStayOut() {
        assertThat(localAlbums(listOf(song("A", null), song("B", " ")))).isEmpty()
    }

    @Test
    fun coverAndArtistComeFromTheSongs() {
        val album = localAlbums(
            listOf(song("A", "Y", 1), song("B", "Y", 2, art = "cover"), song("C", "Y", 3, artist = "Guest")),
        ).single()
        assertThat(album.artworkUrl).isEqualTo("cover")
        assertThat(album.artist).isEqualTo("Kanye West")
    }

    @Test
    fun folderNamesBecomeAlbumsUnlessGeneric() {
        assertThat(LocalMusicNaming.albumFromFolder("Yandhi")).isEqualTo("Yandhi")
        assertThat(LocalMusicNaming.albumFromFolder("Music")).isNull()
        assertThat(LocalMusicNaming.albumFromFolder("download")).isNull()
        assertThat(LocalMusicNaming.albumFromFolder("  ")).isNull()
    }

    @Test
    fun trackNumbersReadBothTagForms() {
        assertThat(LocalMusicNaming.trackNumber("3/12")).isEqualTo(3)
        assertThat(LocalMusicNaming.trackNumber("7")).isEqualTo(7)
        assertThat(LocalMusicNaming.trackNumber(null)).isNull()
    }
}

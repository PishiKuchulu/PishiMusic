package com.tuned.app.ui.playlist

import com.google.common.truth.Truth.assertThat
import com.tuned.app.data.generate.GeneratedTrack
import org.junit.Test

/** Artists / folders grouping for the Local songs category tabs. */
class LocalCategoriesTest {

    private fun song(name: String, artist: String, url: String, art: String? = null) =
        GeneratedTrack(name = name, artist = artist, artworkUrl = art, url = url)

    @Test
    fun artistsGroupCaseInsensitivelyWithMostCommonSpelling() {
        val artists = localArtists(
            listOf(
                song("A", "Daft Punk", "u1"),
                song("B", "daft punk", "u2"),
                song("C", "Daft Punk", "u3"),
                song("D", "Air", "u4"),
            ),
        )
        assertThat(artists.map { it.name }).containsExactly("Air", "Daft Punk").inOrder()
        val daft = artists.first { it.key == "daft punk" }
        assertThat(daft.trackCount).isEqualTo(3)
        assertThat(daft.tracks.map { it.name }).containsExactly("A", "B", "C").inOrder()
    }

    @Test
    fun artistArtworkIsTheFirstTrackArtwork() {
        val artists = localArtists(
            listOf(
                song("A", "Air", "u1"),
                song("B", "Air", "u2", art = "art://b"),
                song("C", "Air", "u3", art = "art://c"),
            ),
        )
        assertThat(artists.single().artworkUrl).isEqualTo("art://b")
    }

    @Test
    fun folderKeyPrefersPickedFolderThenDeviceDir() {
        val saf = song("A", "X", "content://docs/1")
        val device = song("B", "X", "content://media/2")
        val legacy = song("C", "X", "/data/local_music/a.mp3")
        val folders = mapOf("content://docs/1" to "content://tree/primary%3AMusic")
        val dirs = mapOf("content://media/2" to "Music/Download/")
        assertThat(localFolderKey(saf, folders, dirs)).isEqualTo("content://tree/primary%3AMusic")
        assertThat(localFolderKey(device, folders, dirs)).isEqualTo("device:Music/Download/")
        assertThat(localFolderKey(legacy, folders, dirs)).isNull()
    }

    @Test
    fun folderGroupsSortByLabelWithUnknownLast() {
        val tracks = listOf(
            song("A", "X", "u1"),
            song("B", "X", "u2"),
            song("C", "X", "u3"),
        )
        val keys = mapOf("u1" to "device:Zeta/", "u2" to "device:Alpha/")
        val groups = localFolderGroups(
            tracks,
            keyOf = { keys[it.url] },
            labelOf = { it.removePrefix("device:").trimEnd('/') },
        )
        assertThat(groups.map { it.key }).containsExactly("device:Alpha/", "device:Zeta/", "").inOrder()
        assertThat(groups.last().tracks.map { it.name }).containsExactly("C").inOrder()
        assertThat(groups.first().trackCount).isEqualTo(1)
    }
}

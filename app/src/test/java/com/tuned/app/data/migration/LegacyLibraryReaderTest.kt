package com.tuned.app.data.migration

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class LegacyLibraryReaderTest {

    /** The tables and columns the reader uses, as in the previous app's schema 30. */
    private fun legacyDb(): SQLiteDatabase = SQLiteDatabase.create(null).apply {
        execSQL("CREATE TABLE song (id TEXT PRIMARY KEY, title TEXT, duration INTEGER, thumbnailUrl TEXT, albumId TEXT, albumName TEXT, liked INTEGER, likedDate INTEGER, totalPlayTime INTEGER, isLocal INTEGER, localPath TEXT)")
        execSQL("CREATE TABLE artist (id TEXT PRIMARY KEY, name TEXT)")
        execSQL("CREATE TABLE song_artist_map (songId TEXT, artistId TEXT, position INTEGER)")
        execSQL("CREATE TABLE playlist (id TEXT PRIMARY KEY, name TEXT, createdAt INTEGER)")
        execSQL("CREATE TABLE playlist_song_map (id INTEGER PRIMARY KEY, playlistId TEXT, songId TEXT, position INTEGER)")
        execSQL("CREATE TABLE event (id INTEGER PRIMARY KEY, songId TEXT, timestamp INTEGER, playTime INTEGER)")
        execSQL("CREATE TABLE search_history (id INTEGER PRIMARY KEY, query TEXT)")

        execSQL("INSERT INTO song VALUES ('vid1', 'Self Aware', 0, 'https://img/1', NULL, 'Self Aware', 1, 200, 600000, 0, NULL)")
        execSQL("INSERT INTO song VALUES ('vid2', 'Loser', 0, 'https://img/2', NULL, 'Loser', 1, 100, 0, 0, NULL)")
        execSQL("INSERT INTO song VALUES ('loc1', 'Home Demo', 0, NULL, NULL, NULL, 0, NULL, 30000, 1, '/storage/emulated/0/Music/demo.flac')")
        execSQL("INSERT INTO artist VALUES ('a1', 'Temper City'), ('a2', 'Tame Impala'), ('a3', 'Guest')")
        execSQL("INSERT INTO song_artist_map VALUES ('vid1', 'a1', 0), ('vid2', 'a3', 1), ('vid2', 'a2', 0)")
        execSQL("INSERT INTO playlist VALUES ('p1', 'Night drive', 1), ('p2', 'Empty one', 2)")
        execSQL("INSERT INTO playlist_song_map VALUES (1, 'p1', 'vid2', 1), (2, 'p1', 'loc1', 0)")
        execSQL("INSERT INTO event VALUES (1, 'vid1', 1000, 1), (2, 'vid1', 5000, 1)")
        execSQL("INSERT INTO search_history VALUES (1, 'tame impala'), (2, 'temper city')")
    }

    @Test
    fun readsLikedSongsInLikeOrderWithArtistsInBillingOrder() {
        val library = LegacyLibraryReader.read(legacyDb())
        assertThat(library.liked.map { it.name }).containsExactly("Loser", "Self Aware").inOrder()
        assertThat(library.liked.first().artist).isEqualTo("Tame Impala, Guest")
        assertThat(library.liked.first().url).isEqualTo("https://music.youtube.com/watch?v=vid2")
    }

    @Test
    fun dropsDurationsSavedAsArtists() {
        val db = legacyDb().apply {
            execSQL("INSERT INTO artist VALUES ('bad', '3:30')")
            execSQL("UPDATE song_artist_map SET artistId = 'bad' WHERE songId = 'vid1'")
        }
        val selfAware = LegacyLibraryReader.read(db).liked.single { it.name == "Self Aware" }
        assertThat(selfAware.artist).isEmpty()
    }

    @Test
    fun readsPlaylistsInOrderAndKeepsLocalFilesByPath() {
        val library = LegacyLibraryReader.read(legacyDb())
        assertThat(library.playlists.map { it.name }).containsExactly("Night drive", "Empty one").inOrder()
        val drive = library.playlists.first().tracks
        assertThat(drive.map { it.name }).containsExactly("Home Demo", "Loser").inOrder()
        assertThat(drive.first().url).isEqualTo("/storage/emulated/0/Music/demo.flac")
        assertThat(library.playlists[1].tracks).isEmpty()
    }

    @Test
    fun readsPlayTimeCountsAndLastPlay() {
        val plays = LegacyLibraryReader.read(legacyDb()).plays.associateBy { it.track.name }
        val selfAware = plays.getValue("Self Aware")
        assertThat(selfAware.totalPlayTimeMs).isEqualTo(600000)
        assertThat(selfAware.playCount).isEqualTo(2)
        assertThat(selfAware.lastPlayedAtMillis).isEqualTo(5000)
        assertThat(selfAware.videoId).isEqualTo("vid1")
        assertThat(plays.getValue("Home Demo").videoId).isNull()
        assertThat(plays).doesNotContainKey("Loser")
    }

    @Test
    fun readsSearchesOldestFirst() {
        assertThat(LegacyLibraryReader.read(legacyDb()).searches).containsExactly("tame impala", "temper city").inOrder()
    }

    /** Schema 7: no like dates, local files, playlist dates or play events yet. */
    @Test
    fun readsAnOldSchema() {
        val db = SQLiteDatabase.create(null).apply {
            execSQL("CREATE TABLE song (id TEXT PRIMARY KEY, title TEXT, thumbnailUrl TEXT, albumName TEXT, liked INTEGER, totalPlayTime INTEGER)")
            execSQL("CREATE TABLE artist (id TEXT PRIMARY KEY, name TEXT)")
            execSQL("CREATE TABLE song_artist_map (songId TEXT, artistId TEXT, position INTEGER)")
            execSQL("CREATE TABLE playlist (id TEXT PRIMARY KEY, name TEXT)")
            execSQL("CREATE TABLE playlist_song_map (id INTEGER PRIMARY KEY, playlistId TEXT, songId TEXT, position INTEGER)")
            execSQL("INSERT INTO song VALUES ('vid1', 'Old Song', NULL, NULL, 1, 42000)")
            execSQL("INSERT INTO artist VALUES ('a1', 'Old Artist')")
            execSQL("INSERT INTO song_artist_map VALUES ('vid1', 'a1', 0)")
            execSQL("INSERT INTO playlist VALUES ('p1', 'Old list')")
            execSQL("INSERT INTO playlist_song_map VALUES (1, 'p1', 'vid1', 0)")
        }
        val library = LegacyLibraryReader.read(db)
        assertThat(library.liked.map { it.artist }).containsExactly("Old Artist")
        assertThat(library.playlists.single().tracks.single().url).isEqualTo("https://music.youtube.com/watch?v=vid1")
        val play = library.plays.single()
        assertThat(play.totalPlayTimeMs).isEqualTo(42000)
        assertThat(play.playCount).isEqualTo(0)
    }

    @Test
    fun missingTablesGiveAnEmptyLibraryInsteadOfFailing() {
        val library = LegacyLibraryReader.read(SQLiteDatabase.create(null))
        assertThat(library.isEmpty).isTrue()
    }
}

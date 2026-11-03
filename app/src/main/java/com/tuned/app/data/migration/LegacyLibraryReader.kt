package com.tuned.app.data.migration

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.tuned.app.data.generate.GeneratedTrack

/** What Tuned brings over from the app it replaces. */
data class LegacyLibrary(
    val liked: List<GeneratedTrack>,
    val playlists: List<LegacyPlaylist>,
    val plays: List<LegacyPlays>,
    val searches: List<String>,
) {
    val isEmpty: Boolean get() = liked.isEmpty() && playlists.isEmpty() && plays.isEmpty() && searches.isEmpty()
}

data class LegacyPlaylist(val name: String, val tracks: List<GeneratedTrack>)

/** A song's listening record: total time, completed plays and the last play. */
data class LegacyPlays(
    val track: GeneratedTrack,
    val videoId: String?,
    val totalPlayTimeMs: Long,
    val playCount: Int,
    val lastPlayedAtMillis: Long,
)

/**
 * Reads the previous app's Room database (`song.db`, the InnerTune layout). Only selects,
 * never writes. Every release of that app is at schema 30, but a phone that skipped
 * updates can hold an older one: like dates arrived in 15, local files in 16,
 * playlist dates in 14 and play events in 8. Missing columns fall back to neutral
 * values, and a missing table yields nothing for its part instead of failing.
 */
object LegacyLibraryReader {

    const val DATABASE_NAME = "song.db"

    private val DURATION = Regex("""\d{1,2}(:\d{2}){1,2}""")

    fun read(db: SQLiteDatabase): LegacyLibrary {
        val artists = artistNames(db)
        val song = columns(db, "song")
        val cols = songColumns(song)
        return LegacyLibrary(
            liked = if ("liked" in song) liked(db, artists, cols, song) else emptyList(),
            playlists = playlists(db, artists, cols),
            plays = if ("totalPlayTime" in song) plays(db, artists, cols, hasEvents = columns(db, "event").contains("timestamp")) else emptyList(),
            searches = searches(db),
        )
    }

    /** The columns [table] has; empty when it doesn't exist. */
    private fun columns(db: SQLiteDatabase, table: String): Set<String> {
        val out = mutableSetOf<String>()
        query(db, "PRAGMA table_info($table)") { c -> out += c.getString(1) }
        return out
    }

    /** The song columns [track] reads, in its order, with stand-ins for missing ones. */
    private fun songColumns(song: Set<String>): String = listOf(
        "s.id",
        "s.title",
        if ("thumbnailUrl" in song) "s.thumbnailUrl" else "NULL",
        if ("albumName" in song) "s.albumName" else "NULL",
        if ("isLocal" in song) "s.isLocal" else "0",
        if ("localPath" in song) "s.localPath" else "NULL",
    ).joinToString(", ")

    /** Song id to its artists in billing order, joined as the player shows them. */
    private fun artistNames(db: SQLiteDatabase): Map<String, String> {
        val out = LinkedHashMap<String, MutableList<String>>()
        query(db, "SELECT m.songId, a.name FROM song_artist_map m JOIN artist a ON a.id = m.artistId ORDER BY m.songId, m.position") { c ->
            val name = c.getString(1)?.trim()
            // The old app sometimes saved a song's length ("3:30") as its artist; drop those.
            if (name.isNullOrBlank() || DURATION.matches(name)) return@query
            out.getOrPut(c.getString(0)) { mutableListOf() }.add(name)
        }
        return out.mapValues { (_, names) -> names.joinToString(", ") }
    }

    private fun track(c: Cursor, artists: Map<String, String>): GeneratedTrack? {
        val id = c.getString(0) ?: return null
        val title = c.getString(1)?.takeIf { it.isNotBlank() } ?: return null
        val local = c.getInt(4) != 0
        val path = c.getString(5)
        return GeneratedTrack(
            name = title,
            artist = artists[id].orEmpty(),
            artworkUrl = c.getString(2),
            // Tuned plays a local file by its absolute path, and a YouTube song by its watch link.
            url = if (local && !path.isNullOrBlank()) path else "https://music.youtube.com/watch?v=$id",
            album = c.getString(3),
        )
    }

    private fun liked(db: SQLiteDatabase, artists: Map<String, String>, cols: String, song: Set<String>): List<GeneratedTrack> {
        val out = mutableListOf<GeneratedTrack>()
        val order = if ("likedDate" in song) "s.likedDate" else "s.rowid"
        query(db, "SELECT $cols FROM song s WHERE s.liked = 1 ORDER BY $order") { c ->
            track(c, artists)?.let(out::add)
        }
        return out
    }

    private fun playlists(db: SQLiteDatabase, artists: Map<String, String>, cols: String): List<LegacyPlaylist> {
        val lists = mutableListOf<Pair<String, String>>()
        val order = if ("createdAt" in columns(db, "playlist")) "createdAt" else "rowid"
        query(db, "SELECT id, name FROM playlist ORDER BY $order") { c ->
            val name = c.getString(1)?.takeIf { it.isNotBlank() } ?: return@query
            lists += c.getString(0) to name
        }
        return lists.map { (id, name) ->
            val tracks = mutableListOf<GeneratedTrack>()
            query(
                db,
                "SELECT $cols FROM playlist_song_map m JOIN song s ON s.id = m.songId WHERE m.playlistId = ? ORDER BY m.position",
                arrayOf(id),
            ) { c -> track(c, artists)?.let(tracks::add) }
            LegacyPlaylist(name, tracks)
        }
    }

    private fun plays(db: SQLiteDatabase, artists: Map<String, String>, cols: String, hasEvents: Boolean): List<LegacyPlays> {
        val out = mutableListOf<LegacyPlays>()
        // Before play events existed there are no counts or dates, only the total time.
        val count = if (hasEvents) "(SELECT COUNT(*) FROM event e WHERE e.songId = s.id)" else "0"
        val last = if (hasEvents) "(SELECT MAX(e.timestamp) FROM event e WHERE e.songId = s.id)" else "NULL"
        query(db, "SELECT $cols, s.totalPlayTime, $count, $last FROM song s WHERE s.totalPlayTime > 0") { c ->
            val track = track(c, artists) ?: return@query
            out += LegacyPlays(
                track = track,
                videoId = if (c.getInt(4) != 0) null else c.getString(0),
                totalPlayTimeMs = c.getLong(6),
                playCount = c.getInt(7),
                lastPlayedAtMillis = if (c.isNull(8)) 0L else c.getLong(8),
            )
        }
        return out
    }

    private fun searches(db: SQLiteDatabase): List<String> {
        val out = mutableListOf<String>()
        query(db, "SELECT query FROM search_history ORDER BY id") { c ->
            c.getString(0)?.takeIf { it.isNotBlank() }?.let(out::add)
        }
        return out
    }

    private inline fun query(db: SQLiteDatabase, sql: String, args: Array<String>? = null, row: (Cursor) -> Unit) {
        val cursor = runCatching { db.rawQuery(sql, args) }.getOrNull() ?: return
        cursor.use { while (it.moveToNext()) row(it) }
    }
}

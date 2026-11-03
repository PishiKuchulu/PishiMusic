package com.tuned.app.data.repository

import com.tuned.app.data.local.db.SongPlayStatsDao
import com.tuned.app.data.local.db.SongPlayStatsEntity
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private fun trackKeyOf(title: String, artist: String) = "${title.trim()}|${artist.trim()}".lowercase()

/**
 * Local listening-behavior tracker: real accumulated listened time per track
 * plus how often it gets skipped. This is the signal LocalTasteSuggestionEngine
 * scores on, independent of any Last.fm/YouTube account. MusicPlayer calls
 * [recordListenedMs] on every track transition and [recordSkip] when a track
 * is abandoned early.
 */
@Singleton
class SongPlayStatsRepository @Inject constructor(
    private val dao: SongPlayStatsDao,
    private val yearlyDao: com.tuned.app.data.local.db.YearlyListenDao,
) {
    // recordListenedMs/recordSkip are read-modify-write cycles; rapid track
    // transitions call them concurrently and the second writer used to
    // overwrite the first one's increments (lost plays/skips). Serialize all
    // stats writes through this repository (the single writer entry point).
    private val writeMutex = kotlinx.coroutines.sync.Mutex()

    suspend fun recordListenedMs(
        title: String,
        artist: String,
        videoId: String?,
        artworkUrl: String?,
        listenedMs: Long,
        completed: Boolean,
    ) = writeMutex.withLock {
        if (title.isBlank() || artist.isBlank() || listenedMs <= 0L) return@withLock
        if (artist.equals("Podcast", ignoreCase = true)) return@withLock
        val key = trackKeyOf(title, artist)
        val existing = dao.find(key)
        dao.upsert(
            (existing ?: SongPlayStatsEntity(trackKey = key, title = title, artist = artist)).copy(
                videoId = videoId ?: existing?.videoId,
                artworkUrl = artworkUrl ?: existing?.artworkUrl,
                totalPlayTimeMs = (existing?.totalPlayTimeMs ?: 0L) + listenedMs,
                playCount = (existing?.playCount ?: 0) + if (completed) 1 else 0,
                lastPlayedAtMillis = System.currentTimeMillis(),
            ),
        )
        val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val thisYear = yearlyDao.find(year, key)
        yearlyDao.upsert(
            (thisYear ?: com.tuned.app.data.local.db.YearlyListenEntity(year = year, trackKey = key, title = title, artist = artist)).copy(
                videoId = videoId ?: thisYear?.videoId,
                artworkUrl = artworkUrl ?: thisYear?.artworkUrl,
                listenedMs = (thisYear?.listenedMs ?: 0L) + listenedMs,
                plays = (thisYear?.plays ?: 0) + if (completed) 1 else 0,
            ),
        )
    }

    suspend fun recordSkip(title: String, artist: String, videoId: String?) = writeMutex.withLock {
        if (title.isBlank() || artist.isBlank()) return@withLock
        if (artist.equals("Podcast", ignoreCase = true)) return@withLock
        val key = trackKeyOf(title, artist)
        val existing = dao.find(key)
        dao.upsert(
            (existing ?: SongPlayStatsEntity(trackKey = key, title = title, artist = artist)).copy(
                videoId = videoId ?: existing?.videoId,
                skipCount = (existing?.skipCount ?: 0) + 1,
                lastPlayedAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun mostPlayedSince(days: Int = 30, limit: Int = 100): List<SongPlayStatsEntity> =
        dao.mostPlayedSince(System.currentTimeMillis() - days * 86_400_000L, limit)

    suspend fun recentlyPlayed(limit: Int = 20): List<SongPlayStatsEntity> = dao.recentlyPlayed(limit)

    suspend fun skipCountFor(title: String, artist: String): Int =
        dao.find(trackKeyOf(title, artist))?.skipCount ?: 0
}

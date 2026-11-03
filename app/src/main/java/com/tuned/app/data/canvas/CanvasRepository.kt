package com.tuned.app.data.canvas

import java.util.Optional
import android.util.Log
import com.tuned.app.data.network.NetworkMonitor
import com.tuned.app.playback.PlayableTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Finds the looping video that belongs behind a track's or a release's cover
 * art (Spotify Canvas, Apple motion artwork, Tidal video cover, or Community
 * Canvas manifest).
 *
 * Canvases come only from Apple Music's motion artwork (the original animated
 * album covers, found through the song's album). Spotify,
 * Tidal and the community manifest are no longer asked: their loops are not
 * Apple's covers and often belong to another release.
 *
 * Waterfall lookups are serialized with a Mutex to avoid overwhelming the network
 * on rapid track skips. Results (both hits and misses) are cached in a 64-entry LRU cache.
 */
@Singleton
class CanvasRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val networkMonitor: NetworkMonitor,
) {

    private class Entry(val artwork: CanvasArtwork?, val withAlbum: Boolean)

    private val cache = object : LinkedHashMap<String, Entry>(CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>): Boolean =
            size > CACHE_SIZE
    }

    private val lock = Mutex()

    /**
     * Resolves canvas for a [track], respecting cellular data settings. Never throws.
     */
    suspend fun canvasFor(track: PlayableTrack, cellularAllowed: Boolean = true): CanvasArtwork? {
        val title = track.title.cleaned()
        val artist = track.artist.cleaned()
        if (title.isBlank() || artist.isBlank()) return null

        val album = track.album?.takeIf { it.isNotBlank() }
        val id = track.videoId?.takeIf { it.isNotBlank() } ?: "$artist-$title"

        return canvasFor(
            title = title,
            artist = artist,
            album = album,
            trackId = id,
            durationMs = track.durationMs,
            cellularAllowed = cellularAllowed,
        )
    }

    /**
     * Resolves canvas for raw title/artist/album strings.
     */
    suspend fun canvasFor(
        title: String,
        artist: String,
        album: String?,
        trackId: String? = null,
        durationMs: Long? = null,
        cellularAllowed: Boolean = true,
    ): CanvasArtwork? {
        val cleanTitle = title.cleaned()
        val cleanArtist = artist.cleaned()
        if (cleanTitle.isBlank() || cleanArtist.isBlank()) return null

        val id = trackId ?: "$cleanArtist-$cleanTitle"
        val key = "song|$id"

        val cachedAnswer = synchronized(cache) { cache[key] }
        if (cachedAnswer != null && cachedAnswer.reusable(album != null)) {
            return cachedAnswer.artwork
        }

        if (!cellularAllowed && networkMonitor.isOnCellular()) {
            Log.d(TAG, "Skipping online canvas fetch on cellular connection (cellularAllowed=false)")
            return cachedAnswer?.artwork
        }

        return resolve(key, album != null) {
            firstHit({ AppleMusicCanvas.search(okHttpClient, cleanTitle, cleanArtist, album) }) {
                it.matches(cleanTitle, cleanArtist, album)
            }
        }
    }

    /**
     * Cached canvas for [track] if already resolved in memory.
     */
    fun cached(track: PlayableTrack): CanvasArtwork? {
        val id = track.videoId?.takeIf { it.isNotBlank() } ?: "${track.artist.cleaned()}-${track.title.cleaned()}"
        val key = "song|$id"
        return synchronized(cache) { cache[key]?.artwork }
    }

    /**
     * Cached canvas for raw title/artist/id strings if already resolved in memory.
     */
    fun cached(title: String, artist: String, trackId: String? = null): CanvasArtwork? {
        val cleanTitle = title.cleaned()
        val cleanArtist = artist.cleaned()
        val fallbackId = "$cleanArtist-$cleanTitle"
        return synchronized(cache) {
            val fromId = trackId?.takeIf { it.isNotBlank() }?.let { cache["song|$it"]?.artwork }
            fromId ?: cache["song|$fallbackId"]?.artwork
        }
    }

    /**
     * Whether canvas lookup for this song has already been completed (hit or miss).
     */
    fun hasResolved(title: String, artist: String, trackId: String? = null): Boolean {
        val cleanTitle = title.cleaned()
        val cleanArtist = artist.cleaned()
        val fallbackId = "$cleanArtist-$cleanTitle"
        return synchronized(cache) {
            val fromId = trackId?.takeIf { it.isNotBlank() }?.let { cache.containsKey("song|$it") } ?: false
            fromId || cache.containsKey("song|$fallbackId")
        }
    }

    /**
     * Resolves canvas for an entire album release.
     */
    suspend fun canvasForAlbum(
        album: String,
        artist: String,
        cellularAllowed: Boolean = true,
    ): CanvasArtwork? {
        val name = album.cleaned()
        val credit = artist.cleaned()
        if (name.isBlank() || credit.isBlank()) return null

        val key = "album|$name|$credit"
        val cachedAnswer = synchronized(cache) { cache[key] }
        if (cachedAnswer != null) return cachedAnswer.artwork

        if (!cellularAllowed && networkMonitor.isOnCellular()) {
            return null
        }

        return resolve(key, withAlbum = true) {
            firstHit({ AppleMusicCanvas.searchAlbum(okHttpClient, name, credit) }) { it.matches(name, credit, name) }
        }
    }

    /**
     * Resolves Apple Music motion artwork video for an artist profile.
     */
    suspend fun canvasForArtist(
        artist: String,
        cellularAllowed: Boolean = true,
    ): CanvasArtwork? {
        val credit = artist.cleaned()
        if (credit.isBlank()) return null

        val key = "artist|$credit"
        val cachedAnswer = synchronized(cache) { cache[key] }
        if (cachedAnswer != null) return cachedAnswer.artwork

        if (!cellularAllowed && networkMonitor.isOnCellular()) {
            return null
        }

        return resolve(key, withAlbum = false) {
            firstHit({ AppleMusicCanvas.searchArtist(okHttpClient, credit) }) { artwork ->
                val want = credit.normalizeForMatch()
                val got = artwork.artist?.normalizeForMatch().orEmpty()
                got.isBlank() || got.contains(want) || want.contains(got)
            }
        }
    }

    private val profiles = java.util.concurrent.ConcurrentHashMap<String, Optional<AppleArtistProfile>>()

    /** Apple Music's photo, page colour and About details for [artist], or null. Cached per name. */
    suspend fun artistProfile(artist: String): AppleArtistProfile? {
        val credit = artist.cleaned()
        if (credit.isBlank()) return null
        val key = credit.lowercase()
        profiles[key]?.let { return it.orElse(null) }
        val result = withContext(Dispatchers.IO) {
            runCatching { AppleMusicCanvas.artistProfile(okHttpClient, credit) }
                .onFailure { Log.w(TAG, "Artist profile lookup failed for '$credit': ${it.message}") }
        }
        // A failed request is tried again next time; "Apple has no such artist" is kept.
        if (result.isSuccess) profiles[key] = Optional.ofNullable(result.getOrNull())
        return result.getOrNull()
    }

    private val albumInfos = java.util.concurrent.ConcurrentHashMap<String, Optional<AppleAlbumInfo>>()

    /** Apple Music's notes, release details and shelves for an album, or null. Cached per album. */
    suspend fun albumInfo(album: String, artist: String, trackCount: Int?): AppleAlbumInfo? {
        if (album.isBlank()) return null
        val credit = artist.cleaned()
        val key = "${album.lowercase()}|${credit.lowercase()}"
        albumInfos[key]?.let { return it.orElse(null) }
        val result = withContext(Dispatchers.IO) {
            runCatching { AppleMusicCanvas.albumInfo(okHttpClient, album, credit, trackCount) }
                .onFailure { Log.w(TAG, "Album info lookup failed for '$album': ${it.message}") }
        }
        if (result.isSuccess) albumInfos[key] = Optional.ofNullable(result.getOrNull())
        return result.getOrNull()
    }

    private val concerts = java.util.concurrent.ConcurrentHashMap<String, List<AppleConcert>>()

    /** Upcoming concerts from Apple Music's artist page, cached per artist for the session. */
    suspend fun artistConcerts(appleArtistId: String): List<AppleConcert> {
        concerts[appleArtistId]?.let { return it }
        val result = withContext(Dispatchers.IO) {
            runCatching { AppleMusicCanvas.upcomingConcerts(okHttpClient, appleArtistId) }
                .onFailure { Log.w(TAG, "Concerts lookup failed for $appleArtistId: ${it.message}") }
        }
        result.onSuccess { concerts[appleArtistId] = it }
        return result.getOrDefault(emptyList())
    }

    private suspend fun resolve(
        key: String,
        withAlbum: Boolean,
        lookUp: suspend () -> CanvasArtwork?,
    ): CanvasArtwork? = lock.withLock {
        synchronized(cache) {
            cache[key]?.let { if (it.reusable(withAlbum)) return@withLock it.artwork }
        }
        val found = withContext(Dispatchers.IO) {
            runCatching { lookUp() }
                .onFailure { Log.w(TAG, "Canvas lookup failed for '$key': ${it.message}") }
                .getOrNull()
        }
        synchronized(cache) { cache[key] = Entry(found, withAlbum) }
        found
    }

    private fun Entry.reusable(withAlbum: Boolean): Boolean =
        artwork != null || this.withAlbum || !withAlbum

    private suspend fun firstHit(
        vararg sources: suspend () -> CanvasArtwork?,
        accept: (CanvasArtwork) -> Boolean,
    ): CanvasArtwork? {
        for (source in sources) {
            val found = runCatching { source() }
                .onFailure { Log.d(TAG, "Source lookup failed: ${it.message}") }
                .getOrNull() ?: continue
            if (!accept(found)) {
                Log.d(TAG, "Rejected canvas mismatch: '${found.title}' by '${found.artist}'")
                continue
            }
            return found
        }
        Log.d(TAG, "No canvas from any provider")
        return null
    }

    private fun String.cleaned(): String = replace(NOISE, " ")
        .substringBefore(" | ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .ifBlank { this }

    private companion object {
        private const val TAG = "CanvasRepository"
        private const val CACHE_SIZE = 64

        private val NOISE = Regex(
            """\((?:from|official|lyrical|video|audio)[^)]*\)|\[[^]]*]|""" +
                """\b(?:official (?:video|audio|music video)|lyrical|full song|4k video)\b""",
            RegexOption.IGNORE_CASE,
        )
    }
}

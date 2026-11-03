package com.tuned.app.data.playlist

import com.tuned.app.data.generate.GeneratedTrack

/**
 * Supported external playlist providers for the "paste a link" importer.
 *
 * These are **public** playlist sources: the track list is read from the
 * provider's public web page, so no API key, OAuth or account is required.
 * Private playlists / "Liked Songs" would need OAuth and are not supported.
 */
enum class ExternalPlaylistSource(
    val label: String,
    val hostHint: String,
) {
    SPOTIFY("Spotify", "spotify.com"),
    APPLE_MUSIC("Apple Music", "music.apple.com"),
}

/** One row read off a provider page, before it is matched to a playable track. */
data class ExternalTrackRow(
    val title: String,
    val artist: String,
    val album: String? = null,
)

/** A parsed provider playlist — the preview payload shown before importing. */
data class ExternalPlaylistResult(
    val source: ExternalPlaylistSource,
    val title: String,
    val author: String? = null,
    val artworkUrl: String? = null,
    val rows: List<ExternalTrackRow>,
)

/** Outcome of a match pass: mirrors [CsvImportResult] so the UI reports identically. */
data class ExternalImportResult(
    val source: ExternalPlaylistSource,
    val suggestedTitle: String,
    val totalRows: Int,
    val matchedCount: Int,
    val tracks: List<GeneratedTrack>,
)

/** One Spotify playlist belonging to the connected Spotify user account. */
data class SpotifyUserPlaylist(
    val id: String,
    val name: String,
    val description: String? = null,
    val artworkUrl: String? = null,
    val trackCount: Int = 0,
)

/**
 * Link helpers shared by every provider importer.
 *
 * Accepts the shapes users actually paste or share:
 * - `https://open.spotify.com/playlist/<id>?si=...`
 * - `https://open.spotify.com/intl-de/playlist/<id>`   (locale segment)
 * - `https://spotify.link/<short>`                     (expanded by OkHttp redirects)
 * - `spotify:playlist:<id>`                            (URI form)
 * - `https://music.apple.com/us/playlist/<slug>/pl.<id>`
 * - `https://music.apple.com/us/playlist/<id>`         (no slug)
 */
object ExternalPlaylistLink {

    private val SPOTIFY_ID = Regex("""spotify\.com/(?:.+/)?playlist/([A-Za-z0-9]+)""")
    private val SPOTIFY_URI = Regex("""spotify:playlist:([A-Za-z0-9]+)""")
    private val SPOTIFY_BARE_ID = Regex("""^[A-Za-z0-9]{22}$""")
    private val APPLE_ID = Regex("""music\.apple\.com/[a-z]{2}(?:-[a-z]{2})?/playlist/(?:[^/]+/)?(pl\.[A-Za-z0-9]+)""")

    private val SPOTIFY_HOSTS = setOf("open.spotify.com", "spotify.com", "www.spotify.com", "spotify.link")
    private const val APPLE_HOST = "music.apple.com"

    /**
     * The exact host of a pasted URL, or null when it has none (bare id,
     * `spotify:` URI, scheme-less text). Host validation must go through
     * here: the old substring `contains("spotify.com")` detection accepted
     * lookalike hosts (`evilspotify.com`, `spotify.com.evil.example`) and
     * any URL merely mentioning the provider in its path or query.
     */
    private fun hostOf(value: String): String? {
        val withScheme = when {
            value.startsWith("http://", ignoreCase = true) ||
                value.startsWith("https://", ignoreCase = true) -> value
            value.contains("://") -> return null // some other scheme — not a web link we fetch
            else -> "https://$value"
        }
        return runCatching { java.net.URI(withScheme).host?.lowercase() }.getOrNull()
    }

    /** True when [raw] is a web URL whose host is exactly a Spotify host. */
    fun isSpotifyHost(raw: String): Boolean = hostOf(raw.trim()) in SPOTIFY_HOSTS

    /** True when [raw] is a web URL whose host is exactly Apple Music's. */
    fun isAppleHost(raw: String): Boolean = hostOf(raw.trim()) == APPLE_HOST

    /** Best-effort provider detection from a pasted string. */
    fun detect(raw: String): ExternalPlaylistSource? {
        val value = raw.trim()
        if (value.isEmpty()) return null
        if (value.startsWith("spotify:", ignoreCase = true) || SPOTIFY_BARE_ID.matches(value)) {
            return ExternalPlaylistSource.SPOTIFY
        }
        return when (hostOf(value)) {
            in SPOTIFY_HOSTS -> ExternalPlaylistSource.SPOTIFY
            APPLE_HOST -> ExternalPlaylistSource.APPLE_MUSIC
            else -> null
        }
    }

    /** Provider-specific playlist id, or null when the link is not a playlist. */
    fun extractId(raw: String, source: ExternalPlaylistSource): String? {
        val value = raw.trim()
        return when (source) {
            ExternalPlaylistSource.SPOTIFY -> {
                SPOTIFY_URI.find(value)?.groupValues?.getOrNull(1)?.let { return it }
                if (SPOTIFY_BARE_ID.matches(value)) return value
                // URL forms must sit on a real Spotify host, so a lookalike
                // host cannot smuggle a playlist id past the importer.
                if (!isSpotifyHost(value)) return null
                SPOTIFY_ID.find(value)?.groupValues?.getOrNull(1)
            }
            ExternalPlaylistSource.APPLE_MUSIC -> {
                if (!isAppleHost(value)) return null
                APPLE_ID.find(value)?.groupValues?.getOrNull(1)
            }
        }
    }

    /** Returns true if the link points to the user's Spotify Liked Songs collection. */
    fun isSpotifyLiked(raw: String): Boolean {
        val clean = raw.trim().lowercase()
        if (clean == "spotify:liked" || clean == "spotify:collection:tracks") return true
        return isSpotifyHost(raw) && clean.contains("collection/tracks")
    }
}

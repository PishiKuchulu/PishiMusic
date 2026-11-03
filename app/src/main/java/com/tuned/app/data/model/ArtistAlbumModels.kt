package com.tuned.app.data.model

import androidx.compose.runtime.Immutable
import com.tuned.app.playback.PlayableTrack

@Immutable
data class ArtistAlbumItem(
    val title: String,
    val browseId: String = "",
    val year: String? = null,
    val type: String? = null, // "Album", "Single", "EP"
    val artworkUrl: String? = null,
)

/** An album title without its edition: "BULLY - DELUXE" and "BULLY (Deluxe)" are both "bully". */
internal fun albumBaseTitle(title: String): String =
    title.replace(Regex("""\s*[(\[][^)\]]*[)\]]"""), "")
        .replace(Regex("""\s+[-–—]\s+.*$"""), "")
        .trim()
        .lowercase()

/**
 * Whether a shelf on an album page lists other editions of that album. YouTube translates the
 * heading, so outside English the shelf is recognised by every entry having the album's title.
 */
internal fun isOtherVersionsShelf(heading: String, albumTitle: String, itemTitles: List<String>): Boolean {
    if (heading.contains("version", ignoreCase = true)) return true
    val base = albumBaseTitle(albumTitle)
    return base.isNotEmpty() && itemTitles.isNotEmpty() && itemTitles.all { albumBaseTitle(it) == base }
}

@Immutable
data class ArtistSummaryItem(
    val name: String,
    val browseId: String = "",
    val artworkUrl: String? = null,
    val subtitle: String? = null,
)

/** A music video on an artist page: plays as a track by its [videoId]. */
@Immutable
data class ArtistVideoItem(
    val title: String,
    val videoId: String,
    val thumbnailUrl: String? = null,
    /** YouTube's subtitle without the artist, e.g. "1.2B views". */
    val subtitle: String? = null,
)

/** A playlist on an artist page ("Featured on", the artist's own playlists). */
@Immutable
data class ArtistPlaylistItem(
    val title: String,
    val playlistId: String,
    val artworkUrl: String? = null,
    val subtitle: String? = null,
)

@Immutable
data class ArtistPageData(
    val name: String,
    val browseId: String = "",
    val artworkUrl: String? = null,
    val bannerUrl: String? = null,
    val monthlyListeners: String? = null,
    val subscribers: String? = null,
    val bio: String? = null,
    val tags: List<String> = emptyList(),
    val topSongs: List<PlayableTrack> = emptyList(),
    val albums: List<ArtistAlbumItem> = emptyList(),
    val singles: List<ArtistAlbumItem> = emptyList(),
    val similarArtists: List<ArtistSummaryItem> = emptyList(),
    val fallbackArtworkUrl: String? = null,
    val videos: List<ArtistVideoItem> = emptyList(),
    val playlists: List<ArtistPlaylistItem> = emptyList(),
)

@Immutable
data class AlbumPageData(
    val title: String,
    val artist: String,
    val artistBrowseId: String? = null,
    val browseId: String = "",
    val artworkUrl: String? = null,
    val releaseYear: String? = null,
    val trackCountText: String? = null,
    val durationText: String? = null,
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val tracks: List<PlayableTrack> = emptyList(),
    val otherAlbums: List<ArtistAlbumItem> = emptyList(),
    /** YouTube's own links to other editions of this album. Search often can't find them. */
    val otherVersions: List<ArtistAlbumItem> = emptyList(),
    /** YouTube Music marks the album explicit. */
    val explicit: Boolean = false,
)

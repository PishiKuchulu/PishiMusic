package com.tuned.app.ui.playlist

import com.tuned.app.data.generate.GeneratedTrack

/**
 * Categories for the Local songs page (Songs / Albums / Artists /
 * Folders, like other local players). Albums live in LocalAlbums.kt;
 * artists and folders are grouped here. Pure functions, unit-tested.
 */

/** One artist's local songs, in playlist order. */
internal data class LocalArtist(
    /** Display name: the spelling most of the artist's songs use. */
    val name: String,
    val artworkUrl: String?,
    val tracks: List<GeneratedTrack>,
) {
    /** Case-insensitive identity, also the artist filter key. */
    val key: String get() = name.trim().lowercase()
    val trackCount: Int get() = tracks.size
}

/** Lowercase key of the placeholder artist untagged files are given. */
internal const val UNKNOWN_LOCAL_ARTIST_KEY = "unknown artist"

/**
 * Groups local songs by artist (case-insensitively), sorted by name.
 * Songs whose artist is the "Unknown artist" placeholder are NOT an
 * artist: they form one trailing bucket with no artwork borrowed from an
 * unrelated first track, and the artists tab treats it as a non-navigable
 * bucket (like unknown albums), not a fake person with a cover.
 */
internal fun localArtists(tracks: List<GeneratedTrack>): List<LocalArtist> =
    tracks.groupBy { it.artist.trim().lowercase() }
        .values
        .map { members ->
            val display = members.groupingBy { it.artist.trim() }.eachCount().maxBy { it.value }.key
            val isUnknown = display.trim().lowercase() == UNKNOWN_LOCAL_ARTIST_KEY
            LocalArtist(
                name = display,
                artworkUrl = if (isUnknown) null else members.firstNotNullOfOrNull { it.artworkUrl },
                tracks = members,
            )
        }
        .sortedWith(compareBy({ it.name.trim().lowercase() == UNKNOWN_LOCAL_ARTIST_KEY }, { it.name.lowercase() }))

/** Folder key for device-scan tracks: prefix of the unified folder key. */
internal const val DEVICE_FOLDER_PREFIX = "device:"

/** Filter key selecting the songs whose source folder is unknown. */
internal const val LOCAL_FOLDER_UNKNOWN_KEY = "local:unknown"

/** Local songs page tab ids (persisted via rememberSaveable). */
internal const val LOCAL_TAB_SONGS = "songs"
internal const val LOCAL_TAB_ALBUMS = "albums"
internal const val LOCAL_TAB_ARTISTS = "artists"
internal const val LOCAL_TAB_FOLDERS = "folders"

/**
 * The unified folder key of a track: the picked folder's tree address
 * for SAF tracks, `device:<relative path>` for device-scan tracks, null
 * when the source folder is unknown (e.g. legacy copies whose folder was
 * never recorded).
 */
internal fun localFolderKey(
    track: GeneratedTrack,
    trackFolders: Map<String, String>,
    trackDirs: Map<String, String>,
): String? = trackFolders[track.url]
    ?: trackDirs[track.url]?.let { DEVICE_FOLDER_PREFIX + it }

/** One folder's local songs, in playlist order. */
internal data class LocalFolderGroup(
    /** Unified folder key; "" for songs whose folder is unknown. */
    val key: String,
    val tracks: List<GeneratedTrack>,
) {
    val trackCount: Int get() = tracks.size
}

/**
 * Groups local songs by folder. Songs with no known folder share the ""
 * group, which always sorts last; the rest sort by their display label.
 */
internal fun localFolderGroups(
    tracks: List<GeneratedTrack>,
    keyOf: (GeneratedTrack) -> String?,
    labelOf: (String) -> String,
): List<LocalFolderGroup> =
    tracks.groupBy { keyOf(it) ?: "" }
        .map { (key, members) -> LocalFolderGroup(key, members) }
        .sortedWith(compareBy({ it.key.isEmpty() }, { labelOf(it.key).lowercase() }))

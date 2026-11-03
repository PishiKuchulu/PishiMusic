package com.tuned.app.ui.playlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tuned.app.R
import com.tuned.app.data.generate.GeneratedTrack
import com.tuned.app.ui.common.MediaTile
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.Keyline
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.tunedColors

/** Imported songs that share an album tag, in track order. */
internal data class LocalAlbum(
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val year: String?,
    val tracks: List<GeneratedTrack>,
) {
    /**
     * Identity is (album, album artist, year), not the title alone: two
     * different releases that happen to share a name ("Greatest Hits" by
     * two artists, a 1999 and a 2020 "Live") are different albums, and the
     * key is also the navigation/save identity — a title-only key merged
     * them into one album that opened and saved as a single set.
     */
    val key: String get() = listOf(title, artist, year.orEmpty())
        .joinToString("|") { it.trim().lowercase() }
}

/** Groups imported songs into albums. Songs with no album stay out, in the plain song list. */
internal fun localAlbums(tracks: List<GeneratedTrack>): List<LocalAlbum> =
    tracks.filter { !it.album.isNullOrBlank() }
        .groupBy { track ->
            val artist = track.artist.trim().lowercase()
            // Untagged files carry the "Unknown artist" placeholder; it
            // must not split (or merge) albums as if it were a real artist.
            val albumArtist = if (artist == UNKNOWN_LOCAL_ARTIST_KEY) "" else artist
            Triple(
                track.album!!.trim().lowercase(),
                albumArtist,
                track.year?.trim().orEmpty(),
            )
        }
        .values
        .map { members ->
            // A stable sort, so untagged numbers keep their file name order.
            val ordered = members.sortedWith(compareBy(nullsLast()) { it.trackNumber })
            LocalAlbum(
                title = members.first().album!!.trim(),
                artist = members.groupingBy { it.artist }.eachCount().maxBy { it.value }.key,
                artworkUrl = ordered.firstNotNullOfOrNull { it.artworkUrl },
                year = ordered.firstNotNullOfOrNull { it.year },
                tracks = ordered,
            )
        }
        .sortedBy { it.title.lowercase() }

/** The albums shelf on the Local songs page. */
@Composable
internal fun LocalAlbumsShelf(albums: List<LocalAlbum>, onOpen: (LocalAlbum) -> Unit) {
    if (albums.isEmpty()) return
    Text(
        stringResource(R.string.local_albums),
        style = AppType.titleL,
        color = MaterialTheme.tunedColors.text1,
        modifier = Modifier.padding(start = Keyline.detail, end = Keyline.detail, top = 20.dp, bottom = 12.dp),
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = Keyline.detail),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(albums, key = { it.key }) { album ->
            MediaTile(
                title = album.title,
                subtitle = album.artist,
                artworkUrl = album.artworkUrl,
                width = Sizes.tileSquare,
                onClick = { onOpen(album) },
            )
        }
    }
}

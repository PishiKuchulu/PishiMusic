package com.tuned.app.ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.tuned.app.R
import com.tuned.app.ui.theme.AppShape
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.Keyline
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.tunedColors

enum class MediaTileShape {
    /** Albums, playlists, mixes. */
    Square,
    /** Artists. Round artwork, centred text. */
    Circle,
    /** Featured items in a shelf. 16:9 artwork. */
    Wide,
}

/**
 * A tile for an album, playlist, mix or artist: artwork, then 8dp, then a
 * one-line title and an optional one-line subtitle.
 *
 * Width comes from [width] in a horizontal shelf. In a grid, pass
 * `Modifier.fillMaxWidth()` and `width = null` so the artwork fills the cell.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaTile(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    artworkUrl: String? = null,
    shape: MediaTileShape = MediaTileShape.Square,
    width: Dp? = if (shape == MediaTileShape.Wide) Sizes.tileWideWidth else Sizes.tileSquare,
    onLongClick: (() -> Unit)? = null,
    /** For artist tiles: loads Apple Music's photo of this artist when it has one. */
    artistName: String? = null,
    /** Small pill over the artwork's top-start corner (e.g. "NEW"). Callers
     *  in the feed used to pass this into card wrappers that silently dropped
     *  it; the tile renders it now. */
    badgeText: String? = null,
    /** Separate play affordance over the artwork's bottom-end corner, for
     *  tiles whose tap opens a page instead of playing. */
    onPlayClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.tunedColors
    val centred = shape == MediaTileShape.Circle
    val sized = if (width != null) modifier.width(width) else modifier
    // Keyed by identity: an unkeyed source kept the previous item's press
    // state when the lazy shelf recycled this tile for another item.
    val interaction = remember(title, artworkUrl) { MutableInteractionSource() }
    Column(
        modifier = sized
            .pressScale(interaction)
            .combinedClickable(interactionSource = interaction, indication = null, onClick = onClick, onLongClick = onLongClick)
            .semantics(mergeDescendants = true) {
                contentDescription = listOfNotNull(title, subtitle).joinToString(", ")
                role = Role.Button
            },
        horizontalAlignment = if (centred) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val artHeight = if (shape == MediaTileShape.Wide) maxWidth * 9 / 16 else maxWidth
            val artWidth = maxWidth
            Box {
                Artwork(
                    url = artworkUrl,
                    size = artWidth,
                    shape = if (shape == MediaTileShape.Circle) AppShape.full else AppShape.sm,
                    fallbackIcon = if (shape == MediaTileShape.Circle) Icons.Filled.Person else Icons.Filled.Album,
                    height = artHeight,
                    artistName = artistName,
                )
                if (badgeText != null) {
                    Surface(
                        shape = AppShape.full,
                        color = colors.accent,
                        modifier = Modifier.align(Alignment.TopStart).padding(Space.sm),
                    ) {
                        Text(
                            badgeText,
                            style = AppType.caption,
                            color = colors.onAccent,
                            modifier = Modifier.padding(horizontal = Space.sm, vertical = Space.xxs),
                        )
                    }
                }
                val playDescription = stringResource(R.string.cmp_play)
                if (onPlayClick != null) {
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(Space.sm)
                            .size(Sizes.touch - Space.sm)
                            .clip(AppShape.full)
                            .background(colors.accent)
                            .clickable(role = Role.Button, onClick = onPlayClick)
                            .semantics { contentDescription = playDescription },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(Sizes.iconRow))
                    }
                }
            }
        }
        Spacer(Modifier.height(Space.sm))
        Text(
            title,
            style = AppType.body,
            color = colors.text1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (centred) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        if (subtitle != null) {
            Spacer(Modifier.height(Space.xxs))
            Text(
                subtitle,
                style = AppType.bodyS,
                color = colors.text2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (centred) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Horizontal shelf of tiles. The first tile starts on the screen margin and
 * scrolled tiles run to the screen edge.
 */
@Composable
fun <T> MediaShelf(
    items: List<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    tile: @Composable (T) -> Unit,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Keyline.margin),
        horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        items(items, key = key) { tile(it) }
    }
}

@ComponentPreviews
@Composable
private fun MediaTilePreview() = PreviewTheme { MediaTileSample() }

/** Sample states, shared by the preview and the in-app component gallery. */
@Composable
internal fun MediaTileSample() {
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg), modifier = Modifier.padding(vertical = Space.lg)) {
        MediaShelf(items = listOf("Hazy Monsoon", "Crimson Canyon", "Restless Cosmos"), key = { it }) {
            MediaTile(it, onClick = {}, subtitle = "25 songs")
        }
        MediaShelf(items = listOf("Karunesh", "Puhf", "optic core"), key = { it }) {
            MediaTile(it, onClick = {}, shape = MediaTileShape.Circle)
        }
        MediaShelf(items = listOf("New this week"), key = { it }) {
            MediaTile(it, onClick = {}, subtitle = "Fresh releases from artists you follow", shape = MediaTileShape.Wide)
        }
        Row(Modifier.padding(horizontal = Keyline.marginCompact), horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            MediaTile("Grid cell", onClick = {}, subtitle = "Fills its column", width = null, modifier = Modifier.weight(1f))
            MediaTile("Second cell", onClick = {}, subtitle = "Same width", width = null, modifier = Modifier.weight(1f))
        }
    }
}

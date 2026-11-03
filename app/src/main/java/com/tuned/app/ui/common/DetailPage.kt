package com.tuned.app.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import com.tuned.app.ui.player.PlayingWaveBars
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.tuned.app.R
import com.tuned.app.data.artwork.ArtworkNormalizer
import com.tuned.app.data.model.ArtistAlbumItem
import com.tuned.app.ui.common.ArtworkImage
import com.tuned.app.ui.theme.AppShape
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.Keyline
import com.tuned.app.ui.theme.LiquidGlassPreset
import com.tuned.app.ui.theme.LocalLiquidGlass
import com.tuned.app.ui.theme.LocalLiquidGlassBackdrop
import com.tuned.app.ui.theme.MenuShape
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.glassPageTint
import com.tuned.app.ui.theme.isLiquidGlassBackdropSupported
import com.tuned.app.ui.theme.liquidGlassChrome
import com.tuned.app.ui.theme.tunedColors

// Parts shared by the artist and album pages (Apple Music's detail-page layout in Tuned's look):
// the floating toolbar, the round side buttons, "Title ›" section headers, tile shelves and
// release tiles.

/** 48dp circle in the text colour at 10%, like the dock's selected tab. */
@Composable
internal fun DetailSideButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    ink: Color,
    tint: Color = ink,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(Sizes.touch)
            .glassSurface(AppShape.full, fill = ink.copy(alpha = 0.1f)),
        shape = AppShape.full,
        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent, contentColor = tint),
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(Sizes.icon))
    }
}

/**
 * Toolbar over the page: back on the left, a capsule with share and more (Apple's two) on the right, both on
 * a frosted surface so they read over the photo or cover. Once the title scrolls away the bar fills
 * in and shows it in the middle.
 */
@Composable
internal fun DetailToolbar(
    title: String,
    showTitle: Boolean,
    onBack: () -> Unit,
    /** Null leaves the capsule out: a page with nothing to share or add, such as a local album. */
    onShare: (() -> Unit)?,
    shareLabel: String,
    menu: (@Composable (dismiss: () -> Unit) -> Unit)?,
    modifier: Modifier = Modifier,
    /** The capsule's first button: Share by default; playlists put Search there. */
    actionIcon: ImageVector = TunedIcons.Share,
    actionTint: Color = MaterialTheme.tunedColors.text1,
) {
    val colors = MaterialTheme.tunedColors
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .barScrim(visible = showTitle)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = Space.lg, vertical = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassCircleButton(TunedIcons.ChevronBack, stringResource(R.string.pd_back_cd), onBack)
        Row(Modifier.weight(1f).padding(horizontal = Space.sm), horizontalArrangement = Arrangement.Center) {
            AnimatedVisibility(visible = showTitle, enter = fadeIn(), exit = fadeOut()) {
                Text(title, style = AppType.title, color = colors.text1, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (onShare == null && menu == null) {
            // Keeps the title centred where the capsule would have been.
            Spacer(Modifier.width(Sizes.touch))
            return@Row
        }
        // Share and menu are independent: hiding one must not hide the other
        // (previously a page with share-but-no-menu lost both).
        Row(
            modifier = Modifier.glassSurface(AppShape.full, fill = glassFill(), hairline = true),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onShare != null) {
                AppIconButton(actionIcon, shareLabel, onShare, iconSize = Sizes.iconRow, tint = actionTint)
            }
            if (menu != null) {
                Box {
                    AppIconButton(TunedIcons.More, stringResource(R.string.art_more_cd), { menuOpen = true }, iconSize = Sizes.iconRow)
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        shape = MenuShape,
                        containerColor = colors.surface2,
                        // Clear of the capsule, like the app's other menus.
                        offset = DpOffset(0.dp, Space.sm),
                        modifier = Modifier.widthIn(min = Sizes.tileTallWidth),
                    ) {
                        menu { menuOpen = false }
                    }
                }
            }
        }
    }
}

/** A menu row: icon, then the label, 48dp tall. */
@Composable
internal fun DetailMenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.tunedColors
    DropdownMenuItem(
        text = { Text(label, style = AppType.body, color = colors.text1) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = colors.text2, modifier = Modifier.size(Sizes.iconRow)) },
        modifier = Modifier.heightIn(min = Sizes.touch),
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = Space.lg),
    )
}

/** The frosted fill under the toolbar buttons. */
@Composable
internal fun glassFill(): Color = MaterialTheme.tunedColors.surface2.copy(alpha = 0.78f)

/**
 * A floating control's surface. With Liquid Glass on (and a layer behind to sample, from
 * [LocalLiquidGlassBackdrop]) it is glass under the shared page tint, like every other glass
 * control; otherwise [fill], with the hairline when asked.
 */
@Composable
internal fun Modifier.glassSurface(
    shape: Shape,
    fill: Color,
    hairline: Boolean = false,
): Modifier {
    val backdrop = LocalLiquidGlassBackdrop.current
    val glass = LocalLiquidGlass.current && isLiquidGlassBackdropSupported() && backdrop != null
    if (glass) {
        return this
            .liquidGlassChrome(shape, enabled = true, preset = LiquidGlassPreset.FloatingControls, backdrop = backdrop)
            .background(glassPageTint(), shape)
    }
    val colors = MaterialTheme.tunedColors
    return this
        .clip(shape)
        .background(fill)
        .then(if (hairline) Modifier.border(Sizes.hairline, colors.line, shape) else Modifier)
}

@Composable
internal fun GlassCircleButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    val colors = MaterialTheme.tunedColors
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(Sizes.touch)
            .glassSurface(AppShape.full, fill = glassFill(), hairline = true),
        shape = AppShape.full,
        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent, contentColor = colors.text1),
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(Sizes.iconRow))
    }
}

// ---------------------------------------------------------------------------------------------
// Sections
// ---------------------------------------------------------------------------------------------

/** "Title ›": the chevron follows the title and the pair opens the full list. */
@Composable
internal fun DetailSectionHeader(title: String, onSeeAll: (() -> Unit)?) {
    val colors = MaterialTheme.tunedColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Space.xl + Space.xs, bottom = Space.md)
            .padding(horizontal = Keyline.detail),
    ) {
        Row(
            modifier = Modifier
                .clip(AppShape.xs)
                .then(if (onSeeAll != null) Modifier.clickable(onClick = onSeeAll) else Modifier)
                .semantics { heading() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = AppType.titleL,
                color = colors.text1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (onSeeAll != null) {
                Spacer(Modifier.width(Space.xs))
                Icon(TunedIcons.ChevronRight, contentDescription = null, tint = colors.text2, modifier = Modifier.size(Sizes.iconRow))
            }
        }
    }
}

/** A section header over a horizontal row of tiles on the 20dp margin. */
internal fun LazyListScope.detailShelf(
    key: String,
    title: @Composable () -> String,
    onSeeAll: (() -> Unit)?,
    tiles: LazyListScope.() -> Unit,
) {
    item(key = key) {
        Column {
            DetailSectionHeader(title = title(), onSeeAll = onSeeAll)
            LazyRow(
                contentPadding = PaddingValues(horizontal = Keyline.detail),
                horizontalArrangement = Arrangement.spacedBy(Space.md),
                content = tiles,
            )
        }
    }
}

/** Cover, title and year. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ReleaseTile(item: ArtistAlbumItem, artist: String, width: Dp?, onClick: () -> Unit) {
    val colors = MaterialTheme.tunedColors
    val interaction = remember { MutableInteractionSource() }
    val subtitle = item.year ?: releaseType(item)
    Column(
        modifier = (if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .pressScale(interaction)
            .combinedClickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "${item.title}, $subtitle"
                role = Role.Button
            },
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(AppShape.sm)) {
            ArtworkImage(
                name = item.title,
                artist = artist,
                embeddedUrl = item.artworkUrl,
                fallbackIcon = Icons.Filled.Album,
                modifier = Modifier.fillMaxSize(),
                // The artist's own channel gives the right cover; a lookup by title can
                // land on another artist's record ("Woman" by Justice found Micah Justice).
                useFixedUrl = ArtworkNormalizer.isRealImage(item.artworkUrl),
                fixedUrl = item.artworkUrl,
            )
        }
        Spacer(Modifier.height(Space.sm))
        Text(item.title, style = AppType.body, color = colors.text1, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = AppType.bodyS, color = colors.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun releaseType(item: ArtistAlbumItem): String = when (item.type) {
    "Single" -> stringResource(R.string.art_type_single)
    "EP" -> stringResource(R.string.art_type_ep)
    else -> stringResource(R.string.art_type_album)
}
// ---------------------------------------------------------------------------------------------
// Album and playlist header and rows
// ---------------------------------------------------------------------------------------------

/** Cover inset from each screen edge (Apple Music: side = min(W - 136, 385)). */
private val DetailCoverInset: Dp = Space.x6 + Space.xs

/** Apple's cap on the cover's side. */
private val DetailCoverMax: Dp = Sizes.collectionArtMedium + Sizes.tileSquare

/** Width of the Play pill between the two round buttons (Apple: up to 160). */
internal val DetailPlayPillWidth: Dp = Sizes.tileSquare + Space.lg

/** Top of a detail header: the toolbar under the status bar (48dp and 4dp above and below), then 20dp. */
@Composable
internal fun detailHeaderTop(): Dp =
    WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + Sizes.touch + Space.xs * 2 + Space.lg + Space.xs

/** The cover's side on an album or playlist page. */
internal fun detailCoverSide(screenWidth: Dp): Dp =
    (screenWidth - DetailCoverInset * 2).coerceIn(Sizes.collectionArt, DetailCoverMax)

/** The accent wash behind the top of the page: strong at the top, gone by its end. */
private const val WASH_TOP_ALPHA = 0.22f
private const val WASH_MID_ALPHA = 0.08f
private val WashHeight: Dp = Sizes.collectionArtMedium + Sizes.tileSquare

/** Tuned's soft accent wash behind an album or playlist header; it scrolls away with [listState]'s first item. */
@Composable
internal fun DetailAccentWash(listState: LazyListState) {
    val colors = MaterialTheme.tunedColors
    Box(
        Modifier
            .fillMaxWidth()
            .height(WashHeight)
            .graphicsLayer {
                translationY = if (listState.firstVisibleItemIndex == 0) -listState.firstVisibleItemScrollOffset.toFloat() else -size.height
            }
            .background(
                Brush.verticalGradient(
                    listOf(colors.accent.copy(alpha = WASH_TOP_ALPHA), colors.accent.copy(alpha = WASH_MID_ALPHA), Color.Transparent),
                ),
            ),
    )
}

/** Apple's Play pill: text colour fill, page colour glyph and label. */
@Composable
internal fun DetailPlayPill(playing: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.tunedColors
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = AppShape.full,
        color = if (enabled) colors.text1 else colors.text1.copy(alpha = 0.3f),
        contentColor = colors.bg,
        interactionSource = interaction,
        modifier = Modifier
            .size(width = DetailPlayPillWidth, height = Sizes.touch)
            .pressScale(interaction),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (playing) Icons.Filled.Pause else TunedIcons.Play,
                contentDescription = null,
                modifier = Modifier.size(Sizes.iconRow),
            )
            Spacer(Modifier.width(Space.sm))
            Text(
                text = stringResource(if (playing) R.string.cmp_pause else R.string.cmp_play),
                style = AppType.bodyStrong,
            )
        }
    }
}

/**
 * A playlist row as Apple Music draws it: 48dp cover (wave bars over it while it plays), title,
 * a second line, ⋯; 64dp tall on the 20dp margin, no card, no separator. [trailing] goes before
 * ⋯ (a drag handle while reordering).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DetailTrackRow(
    title: String,
    subtitle: String,
    artworkName: String,
    artworkArtist: String,
    artworkUrl: String?,
    playing: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    explicit: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.tunedColors
    Row(
        modifier = modifier
            .fillMaxWidth()
            // 64dp: 16dp between covers, so the list breathes (Apple's playlist rows are ~60).
            .heightIn(min = Sizes.row)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = Keyline.detail, end = Keyline.detail - Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(Sizes.artRow).clip(AppShape.xs)) {
            ArtworkImage(
                name = artworkName,
                artist = artworkArtist,
                embeddedUrl = artworkUrl,
                fallbackIcon = Icons.Filled.MusicNote,
                modifier = Modifier.fillMaxSize(),
            )
            if (playing) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                    PlayingWaveBars(containerColor = Color.Transparent, waveColor = Color.White)
                }
            }
        }
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
            Text(
                text = title,
                style = AppType.body,
                color = if (playing) colors.accent else colors.text1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank() || explicit) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (explicit) {
                        ExplicitTag()
                        Spacer(Modifier.width(Space.xs + Space.xxs))
                    }
                    Text(text = subtitle, style = AppType.bodyS, color = colors.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        trailing?.invoke()
        AppIconButton(
            icon = TunedIcons.More,
            contentDescription = stringResource(R.string.dl_options_cd),
            onClick = onMore,
            iconSize = Sizes.iconRow,
            tint = colors.text2,
        )
    }
}

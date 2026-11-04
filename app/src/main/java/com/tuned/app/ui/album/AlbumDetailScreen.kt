package com.tuned.app.ui.album

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tuned.app.ui.common.EdgeToEdgeDialogWindow
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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuned.app.R
import com.tuned.app.data.canvas.AppleAlbumInfo
import com.tuned.app.data.canvas.AppleShelf
import com.tuned.app.data.canvas.CanvasArtwork
import com.tuned.app.data.model.AlbumPageData
import com.tuned.app.playback.PlayableTrack
import com.tuned.app.ui.common.ArtworkImage
import com.tuned.app.ui.common.ExplicitTag
import com.tuned.app.ui.theme.ExplicitMark
import com.tuned.app.ui.common.TrackContextMenuSheet
import com.tuned.app.ui.common.TrackMenuCapabilities
import com.tuned.app.ui.common.TrackMenuTarget
import com.tuned.app.ui.common.safeDrawingBottomPadding
import com.tuned.app.ui.common.AppIconButton
import com.tuned.app.ui.common.DetailMenuItem
import com.tuned.app.ui.common.detailHeaderTop
import com.tuned.app.ui.common.detailCoverSide
import com.tuned.app.ui.common.DetailTrackRow
import com.tuned.app.ui.common.DetailPlayPillWidth
import com.tuned.app.ui.common.DetailPlayPill
import com.tuned.app.ui.common.DetailAccentWash
import com.tuned.app.ui.common.DetailSideButton
import com.tuned.app.ui.common.DetailToolbar
import com.tuned.app.ui.common.LocalUndoController
import com.tuned.app.ui.common.MediaTile
import com.tuned.app.ui.common.MediaTileShape
import com.tuned.app.ui.common.ReleaseTile
import com.tuned.app.ui.common.SkeletonTrackRow
import com.tuned.app.ui.common.TrackRowDensity
import com.tuned.app.ui.common.TunedIcons
import com.tuned.app.ui.common.detailShelf
import com.tuned.app.ui.common.pressScale
import com.tuned.app.ui.common.skeletonAlpha
import com.tuned.app.ui.player.LocalMiniPlayerScrollClearance
import com.tuned.app.ui.player.LocalMusicPlayer
import com.tuned.app.ui.player.PlayingWaveBars
import com.tuned.app.ui.player.CanvasVideoPlayer
import com.tuned.app.ui.theme.AppShape
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.Keyline
import com.tuned.app.ui.theme.LocalLiquidGlass
import com.tuned.app.ui.theme.LocalLiquidGlassBackdrop
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.isLiquidGlassBackdropSupported
import com.tuned.app.ui.theme.liquidGlassSource
import com.tuned.app.ui.theme.rememberLayerBackdrop
import com.tuned.app.ui.theme.tunedColors
import com.tuned.app.util.ArtistHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Shelf tiles as a share of the screen width, the same as the artist page's. */
private const val RELEASE_TILE_SHARE = 0.4f

/** Tall motion artwork's width over height (Apple's 3:4 clip shown at 0.72). */
private const val CANVAS_TALL_ASPECT = 0.72f

/** The canvas hero's fade: solid for the top half, gone at its bottom edge (Apple's stops). */
private val CanvasFade = arrayOf(
    0f to Color.White,
    0.5f to Color.White,
    0.7f to Color.White.copy(alpha = 0.8f),
    0.84f to Color.White.copy(alpha = 0.4f),
    0.94f to Color.White.copy(alpha = 0.12f),
    1f to Color.Transparent,
)

/** Lines of notes shown before "More" (Apple: 2). */
private const val NOTES_LINES = 2

/** The notes pop-up's maximum height, as a share of the screen. */
private const val NOTES_DIALOG_HEIGHT = 0.7f
private const val VIDEO_TILE_SHARE = 0.72f


/**
 * Album page in Apple Music's order (iOS 27 `ContainerDetail`): cover, title, artist, a
 * genre · year line, Shuffle · Play · Add, the notes, the tracklist, the release details
 * under it (date, length, ℗, label), then Apple's shelves: music videos, more by the artist,
 * other versions, you might also like. Notes, details and shelves come from Apple Music's
 * catalog ([AppleAlbumInfo]); the rest from YouTube Music. Drawn with the artist page's parts.
 */
@Composable
fun AlbumDetailScreen(
    albumTitle: String,
    artistName: String = "",
    browseId: String? = null,
    onBack: () -> Unit,
    onOpenArtist: (name: String, browseId: String?) -> Unit = { _, _ -> },
    onOpenAlbum: (title: String, artist: String, browseId: String?) -> Unit = { _, _, _ -> },
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveUiState.collectAsStateWithLifecycle()
    val apple by viewModel.appleInfo.collectAsStateWithLifecycle()
    val albumCanvas by viewModel.albumCanvas.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val canvas = albumCanvas?.takeIf { settings.canvasEnabled }
    val musicPlayer = LocalMusicPlayer.current
    val playbackState by musicPlayer.chromeState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val colors = MaterialTheme.tunedColors
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val undo = LocalUndoController.current
    val versionMissing = stringResource(R.string.alb_version_missing)
    val pinnedMessage = stringResource(R.string.pin_added)
    val unpinnedMessage = stringResource(R.string.pin_removed)
    val pinsViewModel: com.tuned.app.ui.common.PinsViewModel = hiltViewModel()
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val density = LocalDensity.current

    LaunchedEffect(albumTitle, artistName, browseId) {
        viewModel.loadAlbum(albumTitle, artistName, browseId)
    }

    var selectedTrackMenu by remember { mutableStateOf<PlayableTrack?>(null) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val data = (uiState as? AlbumUiState.Success)?.data
    val title = data?.title ?: albumTitle
    val artist = data?.artist?.takeIf { it.isNotBlank() } ?: artistName
    // Apple's editorial notes when it has them, else YouTube's description.
    val notes = remember(apple?.notes, data?.description) {
        apple?.notes?.let { AnnotatedString.fromHtml(it.replace("\n", "<br>")) }
            ?: data?.description?.trim()?.takeIf { it.isNotBlank() }?.let(::AnnotatedString)
    }

    // The bar shows the title once the header's title has gone under it.
    var titleBottomPx by remember { mutableIntStateOf(Int.MAX_VALUE) }
    val toolbarPx = with(density) { (WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + Sizes.touch).roundToPx() }
    val scrolledPastTitle by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > titleBottomPx - toolbarPx
        }
    }

    val isAlbumPlaying = data != null && playbackState.isPlaying &&
        (playbackState.sourceLabel.contains(data.title, ignoreCase = true) ||
            playbackState.current?.album.equals(data.title, ignoreCase = true))

    val shareLink = remember(data, albumTitle) {
        data?.browseId?.takeIf { it.isNotBlank() }?.let { "https://music.youtube.com/browse/$it" }
            ?: "https://music.youtube.com/search?q=${android.net.Uri.encode("$title $artist".trim())}"
    }
    val shareAlbum: () -> Unit = {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, shareLink)
        }
        context.startActivity(Intent.createChooser(send, null))
    }
    val savedMessage = stringResource(R.string.alb_saved)
    val copiedMessage = stringResource(R.string.art_link_copied)
    LaunchedEffect(saveState.saveError) { saveState.saveError?.let { undo?.show(it) } }
    val playOrPause: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (isAlbumPlaying) musicPlayer.togglePlayPause() else viewModel.playAll()
    }
    val openArtist: () -> Unit = {
        val names = ArtistHelper.splitArtists(artist)
        if (names.isNotEmpty()) onOpenArtist(names.first(), if (names.size == 1) data?.artistBrowseId else null)
    }

    // Liquid Glass: the page is the layer the toolbar refracts, as on the artist page.
    val glassOn = LocalLiquidGlass.current && isLiquidGlassBackdropSupported()
    val pageBackdrop = rememberLayerBackdrop()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        Box(Modifier.fillMaxSize().liquidGlassSource(if (glassOn) pageBackdrop else null)) {
            // Tuned's soft accent wash behind the header; it scrolls away with the cover. A
            // canvas album has its clip there instead.
            if (canvas == null) DetailAccentWash(listState)
            when (val state = uiState) {
                is AlbumUiState.Loading -> AlbumLoading(title = albumTitle.takeUnless { browseId != null && it == "Album" }.orEmpty(), artist = artistName, screenWidth = screenWidth)
                is AlbumUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(Space.xl)) {
                            Text(
                                text = state.message,
                                style = AppType.body,
                                color = colors.text2,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(Space.lg))
                            Button(onClick = { viewModel.loadAlbum(albumTitle, artistName, browseId) }) {
                                Text(stringResource(R.string.ly_retry))
                            }
                        }
                    }
                }
                is AlbumUiState.Success -> {
                    val album = state.data
                    // Shelf-kind filtering used to run inside the LazyColumn
                    // builder on every recomposition (including playback
                    // ticks); it depends only on the album + Apple data.
                    val shelfKinds = remember(album, apple) {
                        val shelves = apple?.shelves.orEmpty()
                        AppleShelf.Kind.entries.filter { kind ->
                            shelves.any { it.kind == kind } ||
                                (kind == AppleShelf.Kind.MoreByArtist && album.otherAlbums.isNotEmpty()) ||
                                (kind == AppleShelf.Kind.OtherVersions && album.otherVersions.isNotEmpty())
                        }
                    }
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            bottom = Space.xl + LocalMiniPlayerScrollClearance.current + safeDrawingBottomPadding(),
                        ),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item(key = "header") {
                            AlbumHeader(
                                album = album,
                                apple = apple,
                                notes = notes,
                                onNotes = { showAbout = true },
                                canvas = canvas,
                                screenWidth = screenWidth,
                                playing = isAlbumPlaying,
                                saved = saveState.savedToLibrary,
                                saving = saveState.isSaving,
                                onTitleBottom = { titleBottomPx = it },
                                onArtist = openArtist,
                                onShuffle = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.playShuffle()
                                },
                                onPlay = playOrPause,
                                onSave = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.saveToLibrary()
                                    undo?.show(savedMessage)
                                },
                            )
                        }

                        if (album.tracks.isEmpty()) {
                            item(key = "tracks_empty") {
                                Text(
                                    text = stringResource(R.string.alb_empty),
                                    style = AppType.body,
                                    color = colors.text2,
                                    modifier = Modifier.padding(horizontal = Keyline.detail, vertical = Space.lg),
                                )
                            }
                        }
                        itemsIndexed(
                            album.tracks,
                            key = { index, track -> "track_${track.videoId ?: track.title}_$index" },
                            contentType = { _, _ -> "album_track" },
                        ) { index, track ->
                            AlbumTrackRow(
                                number = index + 1,
                                track = track,
                                albumArtist = album.artist,
                                playing = isTrackPlaying(playbackState.isPlaying, playbackState.current, track),
                                onClick = { viewModel.playAll(startIndex = index) },
                                onMore = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedTrackMenu = track
                                },
                            )
                        }

                        if (album.tracks.isNotEmpty()) {
                            item(key = "details") { AlbumDetails(album, apple) }
                        }

                        // Apple's shelves in Apple's order. "More by" prefers YouTube's own list,
                        // which opens straight to each record.
                        val appleShelves = apple?.shelves.orEmpty()
                        val tileWidth = (screenWidth * RELEASE_TILE_SHARE).coerceAtMost(Sizes.tileTallWidth)
                        val kinds = shelfKinds
                        kinds.forEach { kind ->
                            val shelf = appleShelves.firstOrNull { it.kind == kind }
                            when {
                                kind == AppleShelf.Kind.MoreByArtist && album.otherAlbums.isNotEmpty() -> detailShelf(
                                    key = "more_by",
                                    title = { stringResource(R.string.alb_more_by, ArtistHelper.primaryArtist(album.artist)) },
                                    onSeeAll = openArtist,
                                ) {
                                    // Sources can list the same release twice; show it once, and key by position
                                    // too so a repeat can never crash the list.
                                    val releases = album.otherAlbums.distinctBy { it.browseId.ifBlank { it.title } }
                                    itemsIndexed(releases, key = { i, it -> "more_${i}_${it.browseId.ifBlank { it.title }}" }) { _, item ->
                                        ReleaseTile(
                                            item = item,
                                            artist = album.artist,
                                            width = tileWidth,
                                            onClick = { onOpenAlbum(item.title, album.artist, item.browseId) },
                                        )
                                    }
                                }
                                // YouTube's list links straight to each edition, and search often can't find them.
                                kind == AppleShelf.Kind.OtherVersions && album.otherVersions.isNotEmpty() -> detailShelf(
                                    key = "other_versions",
                                    title = { stringResource(R.string.alb_other_versions) },
                                    onSeeAll = null,
                                ) {
                                    itemsIndexed(album.otherVersions, key = { i, it -> "version_${i}_${it.browseId}" }) { _, item ->
                                        ReleaseTile(
                                            // YouTube leaves the year off these entries; Apple usually has it.
                                            item = item.copy(
                                                year = item.year ?: shelf?.items?.firstOrNull { it.title.equals(item.title, ignoreCase = true) }?.year,
                                            ),
                                            artist = album.artist,
                                            width = tileWidth,
                                            onClick = { onOpenAlbum(item.title, album.artist, item.browseId) },
                                        )
                                    }
                                }
                                shelf != null -> detailShelf(
                                    key = "apple_${kind.name}",
                                    title = {
                                        when (kind) {
                                            AppleShelf.Kind.MusicVideos -> stringResource(R.string.art_music_videos)
                                            AppleShelf.Kind.MoreByArtist -> stringResource(R.string.alb_more_by, ArtistHelper.primaryArtist(album.artist))
                                            AppleShelf.Kind.OtherVersions -> stringResource(R.string.alb_other_versions)
                                            AppleShelf.Kind.YouMightAlsoLike -> stringResource(R.string.alb_you_might_like)
                                        }
                                    },
                                    onSeeAll = if (kind == AppleShelf.Kind.MoreByArtist) openArtist else null,
                                ) {
                                    // Apple can repeat an entry (Dawn FM lists "Alternate World" twice): show it once,
                                    // and key by position too so a repeat can never crash the list.
                                    val entries = shelf.items.distinctBy { listOf(it.title, it.artist, it.year, it.isVideo) }
                                    itemsIndexed(entries, key = { i, it -> "${kind.name}_${i}_${it.title}_${it.artist}_${it.year}" }) { _, item ->
                                        val itemArtist = item.artist ?: album.artist
                                        if (item.isVideo) {
                                            MediaTile(
                                                title = item.title,
                                                subtitle = item.artist,
                                                artworkUrl = item.artworkUrl(),
                                                shape = MediaTileShape.Wide,
                                                width = (screenWidth * VIDEO_TILE_SHARE).coerceAtMost(Sizes.tileTallWidth + Sizes.tileSquare),
                                                onClick = { musicPlayer.play(PlayableTrack(title = item.title, artist = itemArtist), sourceLabel = album.title) },
                                            )
                                        } else {
                                            MediaTile(
                                                title = item.title,
                                                subtitle = if (kind == AppleShelf.Kind.YouMightAlsoLike) item.artist else item.year ?: item.artist,
                                                artworkUrl = item.artworkUrl(),
                                                width = tileWidth,
                                                onClick = {
                                                    if (kind == AppleShelf.Kind.OtherVersions) {
                                                        viewModel.openOtherVersion(
                                                            title = item.title,
                                                            artist = itemArtist,
                                                            onFound = { id -> onOpenAlbum(item.title, itemArtist, id) },
                                                            onMissing = { undo?.show(versionMissing) },
                                                        )
                                                    } else {
                                                        onOpenAlbum(item.title, itemArtist, null)
                                                    }
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        CompositionLocalProvider(LocalLiquidGlassBackdrop provides if (glassOn) pageBackdrop else null) {
            DetailToolbar(
                title = title,
                showTitle = scrolledPastTitle,
                onBack = onBack,
                onShare = shareAlbum,
                shareLabel = stringResource(R.string.alb_share),
                menu = { dismiss ->
                    val albumPin = com.tuned.app.data.library.PinnedItem(
                        kind = com.tuned.app.data.library.PinKind.ALBUM,
                        title = title,
                        artist = artist,
                        artworkUrl = data?.artworkUrl,
                        browseId = data?.browseId?.takeIf(String::isNotBlank) ?: browseId,
                    )
                    DetailMenuItem(Icons.Rounded.PushPin, stringResource(if (pinsViewModel.isPinned(albumPin)) R.string.pin_remove else R.string.pin_add)) {
                        dismiss()
                        val now = pinsViewModel.toggle(albumPin)
                        undo?.show(if (now) pinnedMessage else unpinnedMessage)
                    }
                    DetailMenuItem(Icons.Rounded.Shuffle, stringResource(R.string.alb_shuffle_cd)) { dismiss(); viewModel.playShuffle() }
                    if (artist.isNotBlank()) {
                        DetailMenuItem(Icons.Rounded.Person, stringResource(R.string.alb_go_to_artist)) { dismiss(); openArtist() }
                    }
                    DetailMenuItem(TunedIcons.Copy, stringResource(R.string.art_copy_link)) {
                        dismiss()
                        clipboard.setText(AnnotatedString(shareLink))
                        undo?.show(copiedMessage)
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(10f),
            )
        }
    }

    // Apple's album notes pop-up, opened from the notes' "More".
    if (showAbout && notes != null) {
        AlbumNotesDialog(title = title, artist = artist, notes = notes, onDismiss = { showAbout = false })
    }

    selectedTrackMenu?.let { track ->
        val safeArtist = track.artist.takeIf { it.isNotBlank() && !ArtistHelper.isPlayCountOrStat(it) } ?: artist
        val safeTrack = if (track.artist != safeArtist) track.copy(artist = safeArtist) else track
        TrackContextMenuSheet(
            target = TrackMenuTarget.Track(safeTrack.title, safeTrack.artist, ""),
            capabilities = TrackMenuCapabilities(showCopyActions = true, showDeleteScrobble = false),
            playableTrack = safeTrack,
            playbackSourceLabel = albumTitle,
            onDismiss = { selectedTrackMenu = null },
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Header
// ---------------------------------------------------------------------------------------------

/**
 * Cover, title, artist (accent, opens the artist), genre · year, then the three controls and the
 * album notes. Spacing from iOS 27's `ContainerDetail` header: 22 cover to title, 3 title to
 * artist, about 20 to the controls.
 */
@Composable
internal fun AlbumHeader(
    album: AlbumPageData,
    apple: AppleAlbumInfo?,
    notes: AnnotatedString?,
    onNotes: () -> Unit,
    screenWidth: Dp,
    playing: Boolean,
    saved: Boolean,
    saving: Boolean,
    canvas: CanvasArtwork?,
    onTitleBottom: (Int) -> Unit,
    onArtist: () -> Unit,
    onShuffle: () -> Unit,
    onPlay: () -> Unit,
    onSave: () -> Unit,
) {
    val colors = MaterialTheme.tunedColors
    val canPlay = album.tracks.isNotEmpty()
    // Apple Music's motion artwork replaces the cover: the tall clip when there is one.
    val canvasUrl = canvas?.let { it.tallUrl ?: it.url }?.takeIf { it.isNotBlank() }
    val heroAspect = if (canvas?.tallUrl != null) CANVAS_TALL_ASPECT else 1f
    val heroHeight = screenWidth / heroAspect
    var contentTop by remember { mutableIntStateOf(0) }
    Box(Modifier.fillMaxWidth()) {
        if (canvasUrl != null) {
            AlbumCanvasHero(
                videoUrl = canvasUrl,
                fallbackUrl = canvas?.fallbackUrl,
                album = album,
                aspect = heroAspect,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Over a canvas, the title block starts on the clip's faded lower part.
                .padding(top = if (canvasUrl != null) (heroHeight - Sizes.tileSquare).coerceAtLeast(detailHeaderTop()) else detailHeaderTop())
                .onGloballyPositioned { contentTop = it.positionInParent().y.toInt() },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (canvasUrl == null) {
                Box(
                    Modifier
                        .size(detailCoverSide(screenWidth))
                        .shadow(Space.md, AppShape.md, clip = false)
                        .clip(AppShape.md),
                ) {
                    ArtworkImage(
                        name = album.title,
                        artist = album.artist,
                        embeddedUrl = album.artworkUrl,
                        fallbackIcon = Icons.Filled.Album,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Spacer(Modifier.height(Space.xl))
            }
            // As in Apple Music, the explicit mark follows the album title.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = Keyline.detail + Space.xs)
                    .onGloballyPositioned { onTitleBottom((contentTop + it.positionInParent().y + it.size.height).toInt()) },
            ) {
                Text(
                    text = album.title,
                    style = AppType.titleL,
                    color = colors.text1,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (album.explicit) {
                    Spacer(Modifier.width(Space.sm))
                    ExplicitTag(color = colors.text1.copy(alpha = 0.65f), fontSize = ExplicitMark.textLarge)
                }
            }
            if (album.artist.isNotBlank()) {
                Spacer(Modifier.height(Space.xxs))
                Text(
                    text = album.artist,
                    style = AppType.title,
                    // Apple: the accent on a plain page, white over artwork.
                    color = if (canvasUrl != null) colors.text1 else colors.accent,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(horizontal = Keyline.detail + Space.xs)
                        .clip(AppShape.xs)
                        .clickable(onClick = onArtist),
                )
            }
            val meta = (album.releaseYear ?: apple?.releaseDate?.take(4)).orEmpty()
            if (meta.isNotBlank()) {
                Spacer(Modifier.height(Space.xs))
                Text(text = meta, style = AppType.caption, color = colors.text2, maxLines = 1)
            }

            Spacer(Modifier.height(Space.lg + Space.xs))
            Row(
                horizontalArrangement = Arrangement.spacedBy(Space.md + Space.xxs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DetailSideButton(Icons.Rounded.Shuffle, stringResource(R.string.alb_shuffle_cd), onShuffle, ink = colors.text1)
                DetailPlayPill(playing = playing, enabled = canPlay, onClick = onPlay)
                DetailSideButton(
                    icon = if (saved) Icons.Rounded.Check else Icons.Rounded.Add,
                    contentDescription = stringResource(if (saved) R.string.alb_saved_cd else R.string.alb_save_cd),
                    onClick = { if (!saved && !saving && canPlay) onSave() },
                    ink = colors.text1,
                )
            }

            if (notes != null) {
                Spacer(Modifier.height(Space.xl))
                AlbumNotes(notes, onMore = onNotes)
            }
            Spacer(Modifier.height(Space.lg))
        }
    }
}

/**
 * The album's motion artwork at full width from the top of the screen, as Apple Music
 * show it: the cover underneath until the clip starts, solid for the top half, then fading out
 * so the title block sits on its lower part.
 */
@Composable
private fun AlbumCanvasHero(videoUrl: String, fallbackUrl: String?, album: AlbumPageData, aspect: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(Brush.verticalGradient(colorStops = CanvasFade), blendMode = BlendMode.DstIn)
            },
    ) {
        ArtworkImage(
            name = album.title,
            artist = album.artist,
            embeddedUrl = album.artworkUrl,
            fallbackIcon = Icons.Filled.Album,
            modifier = Modifier.fillMaxSize(),
        )
        CanvasVideoPlayer(
            videoUrl = videoUrl,
            isPlaying = true,
            enableScrim = false,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * The album notes as Apple Music shows them: two lines, the second fading out just before a bold
 * "More" at its end. A tap opens the About page with the whole text.
 */
@Composable
private fun AlbumNotes(text: AnnotatedString, onMore: () -> Unit) {
    val colors = MaterialTheme.tunedColors
    val more = stringResource(R.string.alb_more)
    val measurer = rememberTextMeasurer()
    var layout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    val overflows = layout?.hasVisualOverflow == true
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Keyline.detail)
            .clip(AppShape.xs)
            .clickable(enabled = overflows, onClick = onMore),
    ) {
        val moreWidth = remember(more) { measurer.measure(more, AppType.bodyStrong).size.width.toFloat() }
        Text(
            text = text,
            style = AppType.body,
            color = colors.text2,
            maxLines = NOTES_LINES,
            overflow = TextOverflow.Clip,
            onTextLayout = { layout = it },
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val result = layout ?: return@drawWithContent
                    if (!result.hasVisualOverflow) return@drawWithContent
                    // Clear the end of the last line for "More", with a short fade before it.
                    val top = result.getLineTop(NOTES_LINES - 1)
                    val clearFrom = size.width - moreWidth - Space.xs.toPx()
                    val fadeFrom = (clearFrom - Space.xxl.toPx()).coerceAtLeast(0f)
                    drawRect(
                        brush = Brush.horizontalGradient(0f to Color.Transparent, 1f to Color.Black, startX = fadeFrom, endX = clearFrom),
                        topLeft = Offset(fadeFrom, top),
                        size = Size(clearFrom - fadeFrom, size.height - top),
                        blendMode = BlendMode.DstOut,
                    )
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(clearFrom, top),
                        size = Size(size.width - clearFrom, size.height - top),
                        blendMode = BlendMode.DstOut,
                    )
                },
        )
        if (overflows) {
            Text(
                text = more,
                style = AppType.bodyStrong,
                color = colors.text1,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}

/**
 * Apple Music's album notes pop-up: a card over the blurred, dimmed page with the album title
 * and artist centred on top and the whole notes below, scrolling inside the card with soft
 * edges. A tap outside or Back closes it.
 */
@Composable
private fun AlbumNotesDialog(title: String, artist: String, notes: AnnotatedString, onDismiss: () -> Unit) {
    val colors = MaterialTheme.tunedColors
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val scroll = rememberScrollState()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        EdgeToEdgeDialogWindow()
        Box(
            Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = Keyline.detail)
                    .fillMaxWidth()
                    .heightIn(max = screenHeight * NOTES_DIALOG_HEIGHT)
                    .clip(AppShape.xl)
                    .background(colors.surface2)
                    .border(Sizes.hairline, colors.line, AppShape.xl)
                    // Taps inside the card don't close it.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = Space.xl, end = Space.xl, top = Space.xl, bottom = Space.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = title, style = AppType.title, color = colors.text1, textAlign = TextAlign.Center)
                    if (artist.isNotBlank()) {
                        Text(text = artist, style = AppType.bodyS, color = colors.text2, textAlign = TextAlign.Center)
                    }
                }
                Text(
                    text = notes,
                    style = AppType.body,
                    color = colors.text1,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            // Soft edges where the text is cut by the card.
                            val fade = Space.xl.toPx()
                            if (scroll.value > 0) {
                                drawRect(
                                    brush = Brush.verticalGradient(0f to Color.Black, 1f to Color.Transparent, startY = 0f, endY = fade),
                                    size = Size(size.width, fade),
                                    blendMode = BlendMode.DstOut,
                                )
                            }
                            if (scroll.value < scroll.maxValue) {
                                drawRect(
                                    brush = Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black, startY = size.height - fade, endY = size.height),
                                    topLeft = Offset(0f, size.height - fade),
                                    size = Size(size.width, fade),
                                    blendMode = BlendMode.DstOut,
                                )
                            }
                        }
                        .verticalScroll(scroll)
                        .padding(start = Space.xl, end = Space.xl, bottom = Space.xl),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Tracklist and details
// ---------------------------------------------------------------------------------------------

/**
 * An album row as Apple draws it: the number in a column (a wave while it plays), the title,
 * the artist only when it differs from the album's, and ⋯. 56dp tall, no cover, no separators.
 */
@OptIn(ExperimentalFoundationApi::class)
/**
 * The artist line under an album track, only when it adds someone. An album credited to
 * "Kanye West & Ye" with tracks by "Kanye West" says nothing new on every row.
 */
internal fun trackArtistLine(trackArtist: String, albumArtist: String): String? {
    if (trackArtist.isBlank() || ArtistHelper.isPlayCountOrStat(trackArtist)) return null
    val credited = ArtistHelper.splitArtists(albumArtist).map { it.lowercase() }
    return trackArtist.takeUnless { ArtistHelper.splitArtists(it).all { name -> name.lowercase() in credited } }
}

@Composable
internal fun AlbumTrackRow(
    number: Int,
    track: PlayableTrack,
    albumArtist: String,
    playing: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit,
) {
    val colors = MaterialTheme.tunedColors
    val featured = trackArtistLine(track.artist, albumArtist)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.rowCompact)
            .combinedClickable(onClick = onClick, onLongClick = onMore)
            .padding(start = Keyline.detail, end = Keyline.detail - Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(Space.xxl), contentAlignment = Alignment.CenterStart) {
            if (playing) {
                PlayingWaveBars(
                    modifier = Modifier.size(width = Sizes.iconRow, height = Sizes.iconInline),
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    waveColor = colors.accent,
                )
            } else {
                Text(text = number.toString(), style = AppType.body, color = colors.text2)
            }
        }
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = track.title,
                    style = AppType.body,
                    color = if (playing) colors.accent else colors.text1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (track.explicit) {
                    Spacer(Modifier.width(Space.xs + Space.xxs))
                    ExplicitTag()
                }
            }
            if (featured != null) {
                Text(text = featured, style = AppType.bodyS, color = colors.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        AppIconButton(
            icon = TunedIcons.More,
            contentDescription = stringResource(R.string.dl_options_cd),
            onClick = onMore,
            iconSize = Sizes.iconRow,
            tint = colors.text2,
        )
    }
}

/**
 * Under the tracklist, one fact per line: the release date, "12 songs", "38 minutes", the ℗ line,
 * then the record label. Date, ℗ and label come from Apple; YouTube only has the year.
 */
@Composable
internal fun AlbumDetails(album: AlbumPageData, apple: AppleAlbumInfo?) {
    val colors = MaterialTheme.tunedColors
    val songs = pluralStringResource(R.plurals.alb_song_count, album.tracks.size, album.tracks.size)
    val length = album.durationText?.takeIf { it.isNotBlank() }
    val date = remember(apple?.releaseDate) {
        apple?.releaseDate?.let { raw ->
            runCatching { LocalDate.parse(raw).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)) }.getOrNull()
        }
    } ?: album.releaseYear?.takeIf { it.isNotBlank() }
    // Running text: proportional digits. bodyS has fixed-width ones, which spread "2007" apart.
    val detailStyle = AppType.bodyS.copy(fontFeatureSettings = null)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Keyline.detail)
            .padding(top = Space.lg + Space.xs, bottom = Space.xs),
    ) {
        date?.let { Text(text = it, style = detailStyle, color = colors.text2) }
        Text(text = songs, style = detailStyle, color = colors.text2)
        length?.let { Text(text = it, style = detailStyle, color = colors.text2) }
        apple?.copyright?.let { Text(text = it, style = detailStyle, color = colors.text2) }
        apple?.recordLabel?.let { label ->
            Spacer(Modifier.height(Space.md))
            Text(text = stringResource(R.string.alb_record_label), style = AppType.caption, color = colors.text2)
            Text(text = label, style = detailStyle, color = colors.text1)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Loading
// ---------------------------------------------------------------------------------------------

/** The page's own shape while it loads: cover, the known title and artist, controls, rows. */
@Composable
private fun AlbumLoading(title: String, artist: String, screenWidth: Dp) {
    val colors = MaterialTheme.tunedColors
    val pulse = skeletonAlpha()
    LazyColumn(userScrollEnabled = false, modifier = Modifier.fillMaxSize()) {
        item(key = "header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = detailHeaderTop()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(detailCoverSide(screenWidth))
                        .alpha(pulse)
                        .clip(AppShape.md)
                        .background(colors.surface2),
                )
                Spacer(Modifier.height(Space.xl))
                Text(
                    text = title,
                    style = AppType.titleL,
                    color = colors.text1,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = Keyline.detail + Space.xs),
                )
                if (artist.isNotBlank()) {
                    Spacer(Modifier.height(Space.xxs))
                    Text(text = artist, style = AppType.title, color = colors.accent, maxLines = 1)
                }
                Spacer(Modifier.height(Space.lg + Space.xs + Space.xl))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Space.md + Space.xxs),
                    modifier = Modifier.alpha(pulse),
                ) {
                    Box(Modifier.size(Sizes.touch).clip(AppShape.full).background(colors.surface2))
                    Box(Modifier.size(width = DetailPlayPillWidth, height = Sizes.touch).clip(AppShape.full).background(colors.surface2))
                    Box(Modifier.size(Sizes.touch).clip(AppShape.full).background(colors.surface2))
                }
                Spacer(Modifier.height(Space.lg))
            }
        }
        items(6) { SkeletonTrackRow(density = TrackRowDensity.Compact, horizontalPadding = Keyline.detail) }
    }
}

internal fun isTrackPlaying(isPlaying: Boolean, current: PlayableTrack?, track: PlayableTrack): Boolean {
    if (!isPlaying || current == null) return false
    if (current.videoId != null && track.videoId != null) return current.videoId == track.videoId
    return current.title.equals(track.title, ignoreCase = true)
}

package com.tuned.app.ui.artist

import com.tuned.app.ui.theme.glassPageTint
import androidx.compose.ui.graphics.lerp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Shape
import com.tuned.app.ui.theme.LiquidGlassPreset
import com.tuned.app.ui.theme.LocalLiquidGlass
import com.tuned.app.ui.theme.LocalLiquidGlassBackdrop
import com.tuned.app.ui.theme.isLiquidGlassBackdropSupported
import com.tuned.app.ui.theme.liquidGlassChrome
import com.tuned.app.ui.theme.liquidGlassSource
import com.tuned.app.ui.theme.rememberLayerBackdrop
import com.tuned.app.ui.common.skeletonAlpha
import com.tuned.app.ui.common.ExplicitTag
import com.tuned.app.ui.common.TrackRowDensity
import com.tuned.app.ui.common.SkeletonTrackRow
import androidx.compose.ui.draw.alpha
import com.tuned.app.ui.common.barScrim
import com.tuned.app.ui.common.DetailMenuItem
import com.tuned.app.ui.common.DetailSectionHeader
import com.tuned.app.ui.common.DetailSideButton
import com.tuned.app.ui.common.DetailToolbar
import com.tuned.app.ui.common.GlassCircleButton
import com.tuned.app.ui.common.ReleaseTile
import com.tuned.app.ui.common.detailShelf
import com.tuned.app.ui.common.glassFill
import com.tuned.app.ui.common.glassSurface
import com.tuned.app.ui.common.releaseType
import com.tuned.app.data.canvas.ArtistImage
import com.tuned.app.data.canvas.AppleMusicCanvas
import com.tuned.app.data.canvas.AppleConcert
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.luminance
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.Crossfade
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tuned.app.R
import com.tuned.app.data.artwork.ArtworkNormalizer
import com.tuned.app.data.canvas.AppleArtistProfile
import com.tuned.app.data.model.ArtistAlbumItem
import com.tuned.app.data.model.ArtistPageData
import com.tuned.app.playback.PlayableTrack
import com.tuned.app.ui.common.ArtworkImage
import com.tuned.app.ui.common.EdgeToEdgeDialogWindow
import com.tuned.app.ui.common.TrackContextMenuSheet
import com.tuned.app.ui.common.TrackMenuCapabilities
import com.tuned.app.ui.common.TrackMenuTarget
import com.tuned.app.ui.common.safeDrawingBottomPadding
import com.tuned.app.ui.common.AppIconButton
import com.tuned.app.ui.common.LocalUndoController
import com.tuned.app.ui.common.MediaTile
import com.tuned.app.ui.common.MediaTileShape
import com.tuned.app.ui.common.TunedIcons
import com.tuned.app.ui.common.pressScale
import com.tuned.app.ui.genres.GenreExplorer
import com.tuned.app.ui.player.CanvasVideoPlayer
import com.tuned.app.ui.player.LocalMiniPlayerScrollClearance
import com.tuned.app.ui.player.LocalMusicPlayer
import com.tuned.app.ui.player.PlayingWaveBars
import com.tuned.app.ui.theme.AppShape
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.Keyline
import com.tuned.app.ui.theme.MenuShape
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.rememberMotion
import com.tuned.app.ui.theme.tunedColors
import com.tuned.app.util.ArtistHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Top songs shown on the page; the rest are behind "Top songs ›". */
private const val TOP_SONGS_PREVIEW = 5

/** Shelf tile widths as a share of the screen width, measured on Apple Music (iOS 27, 402pt). */
private const val RELEASE_TILE_SHARE = 0.4f
private const val VIDEO_TILE_SHARE = 0.72f
private const val ARTIST_TILE_SHARE = 0.3f


/** Where the artist name sits over the photo, as a share of the photo's height. */
private const val HERO_NAME_TOP = 0.86f

/** The lists behind a section's chevron, and the About page behind ⓘ. */
private enum class ArtistSubPage { TopSongs, Albums, Singles, Videos, Playlists, About }

/**
 * Artist page laid out like Apple Music's (iOS 27): photo hero with the name and
 * ⓘ / Play / ☆, latest release, top songs, albums, music videos, singles and EPs,
 * playlists, similar artists, about. Each section's chevron opens its full list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artistName: String,
    browseId: String? = null,
    onBack: () -> Unit,
    onOpenAlbum: (title: String, artist: String, browseId: String?) -> Unit = { _, _, _ -> },
    onOpenArtist: (name: String, browseId: String?) -> Unit = { _, _ -> },
    onOpenPlaylist: (playlistId: String) -> Unit = {},
    viewModel: ArtistViewModel = hiltViewModel(),
    genreBridge: ArtistDetailGenreBridge = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val artistCanvas by viewModel.artistCanvas.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val musicPlayer = LocalMusicPlayer.current
    val playbackState by musicPlayer.chromeState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val colors = MaterialTheme.tunedColors
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val undo = LocalUndoController.current
    val density = LocalDensity.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    LaunchedEffect(artistName, browseId) {
        viewModel.loadArtist(artistName, browseId)
    }

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var selectedTrackMenu by remember { mutableStateOf<PlayableTrack?>(null) }
    var showHeaderVideo by rememberSaveable(artistName, settings.canvasEnabled) { mutableStateOf(settings.canvasEnabled) }
    var showFullscreenMotion by rememberSaveable { mutableStateOf(false) }
    var subPage by rememberSaveable { mutableStateOf<ArtistSubPage?>(null) }
    // Kept while the sub-page slides out, so it doesn't go blank mid-animation.
    var shownSubPage by remember { mutableStateOf<ArtistSubPage?>(null) }
    if (subPage != null) shownSubPage = subPage
    BackHandler(enabled = subPage != null) { subPage = null }

    val listState = rememberLazyListState()
    // Square clip for the square hero; the tall one for the full-screen view.
    val motionVideoUrl = artistCanvas?.let { it.url.takeIf(String::isNotBlank) ?: it.wideUrl ?: it.tallUrl }?.takeIf { it.isNotBlank() }
    val fullscreenVideoUrl = artistCanvas?.tallUrl?.takeIf { it.isNotBlank() } ?: motionVideoUrl
    val data = (uiState as? ArtistUiState.Success)?.data
    val displayName = ArtistHelper.primaryArtist(data?.name ?: artistName)
    val isFavorite = favorites.any { stored ->
        // Browse id decides when both sides have one (same-name artists
        // are different artists); the name is the fallback.
        val storedId = stored.browseId
        val thisId = data?.browseId ?: browseId
        if (storedId.isNotBlank() && !thisId.isNullOrBlank()) storedId == thisId
        else stored.name.equals(data?.name ?: artistName, ignoreCase = true)
    }
    val isArtistPlaying = data != null && playbackState.isPlaying &&
        (playbackState.sourceLabel == data.name || playbackState.current?.artist.equals(data.name, ignoreCase = true))

    // One photo, picked before it loads (AppleArtistImages): Apple Music's when Apple has this
    // artist, else YouTube's. Loading YouTube's first and swapping showed the wrong one for a second.
    val youtubePhotos = remember(data) {
        listOfNotNull(data?.artworkUrl, data?.bannerUrl, data?.fallbackArtworkUrl)
            .filter(ArtworkNormalizer::isRealImage)
            .distinct()
    }
    var photoIndex by remember(youtubePhotos) { mutableIntStateOf(0) }
    val photoUrl: Any? = if (youtubePhotos.isNotEmpty() || profile?.photoTemplate != null) {
        ArtistImage(displayName, youtubePhotos.getOrNull(photoIndex))
    } else null
    val profileSettled by viewModel.profileSettled.collectAsStateWithLifecycle()
    val concerts by viewModel.concerts.collectAsStateWithLifecycle()
    val openConcerts: (String?) -> Unit = { url ->
        (url ?: profile?.id?.let(AppleMusicCanvas::concertsPageUrl))?.let { link ->
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(link))) }
        }
    }

    val scrolledPastName by remember(screenWidth) {
        val threshold: Float = with(density) { (screenWidth * HERO_NAME_TOP).toPx() }
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > threshold }
    }

    val shareLink = remember(data, artistName) {
        data?.browseId?.takeIf { it.isNotBlank() }?.let { "https://music.youtube.com/channel/$it" }
            ?: "https://music.youtube.com/search?q=${android.net.Uri.encode(displayName)}"
    }
    val shareArtist: () -> Unit = {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, displayName)
            putExtra(Intent.EXTRA_TEXT, shareLink)
        }
        context.startActivity(Intent.createChooser(send, null))
    }
    val favoriteMessage = stringResource(R.string.art_favorited)
    val unfavoriteMessage = stringResource(R.string.art_unfavorited)
    val copiedMessage = stringResource(R.string.art_link_copied)
    val pinnedMessage = stringResource(R.string.pin_added)
    val unpinnedMessage = stringResource(R.string.pin_removed)
    val pinsViewModel: com.tuned.app.ui.common.PinsViewModel = hiltViewModel()
    val toggleFavorite: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val nowFavorite = viewModel.toggleFavorite()
        undo?.show(if (nowFavorite) favoriteMessage else unfavoriteMessage)
    }
    val playOrPause: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (isArtistPlaying) musicPlayer.togglePlayPause() else viewModel.playAll()
    }

    // Liquid Glass: the page is the layer the toolbar refracts. The hero's buttons sit inside the
    // page, so they sample the photo behind them instead (never the layer they are drawn into).
    val glassOn = LocalLiquidGlass.current && isLiquidGlassBackdropSupported()
    val pageBackdrop = rememberLayerBackdrop()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        Box(Modifier.fillMaxSize().liquidGlassSource(if (glassOn) pageBackdrop else null)) {
        when (val state = uiState) {
            is ArtistUiState.Loading -> {
                // The page's own shape while it loads: the photo (Apple's is often ready already)
                // and the name where they will stay, then placeholders where the content lands.
                val loadingName = ArtistHelper.primaryArtist(artistName)
                LazyColumn(userScrollEnabled = false, modifier = Modifier.fillMaxSize()) {
                    item(key = "hero") {
                        ArtistHero(
                            name = loadingName,
                            photo = ArtistImage(loadingName, null),
                            onPhotoError = {},
                            logoUrl = profile?.logoUrl,
                            titleSettled = profileSettled,
                            hasConcerts = false,
                            onConcerts = {},
                            videoUrl = null,
                            scrollOffset = { 0 },
                            onOpenVideo = {},
                            controls = {
                                // Loading skeleton: the controls are drawn
                                // where they will live, but must not look
                                // tappable before there is anything to play
                                // or star — a transparent overlay eats taps.
                                Box {
                                    HeroControls(
                                        playing = false,
                                        canPlay = false,
                                        favorite = isFavorite,
                                        onInfo = {},
                                        onPlay = {},
                                        onFavorite = {},
                                    )
                                    Box(
                                        Modifier
                                            .matchParentSize()
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                            ) {},
                                    )
                                }
                            },
                        )
                    }
                    item(key = "latest_release") { LatestReleaseSkeleton() }
                    item(key = "top_songs_header") {
                        Box(
                            Modifier
                                .padding(start = Keyline.detail, top = Space.xl + Space.xs, bottom = Space.md)
                                .size(width = Sizes.tileSquare, height = Sizes.skeletonTitle * 2)
                                .alpha(skeletonAlpha())
                                .clip(AppShape.xs)
                                .background(colors.surface2),
                        )
                    }
                    items(TOP_SONGS_PREVIEW) {
                        SkeletonTrackRow(density = TrackRowDensity.Compact, horizontalPadding = Keyline.detail)
                    }
                }
            }
            is ArtistUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(Space.xl)) {
                        Text(
                            text = state.message,
                            style = AppType.body,
                            color = colors.text2,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(Space.lg))
                        Button(onClick = { viewModel.loadArtist(artistName, browseId) }) {
                            Text(stringResource(R.string.ly_retry))
                        }
                    }
                }
            }
            is ArtistUiState.Success -> {
                val page = state.data
                val releaseYears = remember(page) { releaseYears(page) }
                val openRelease: (ArtistAlbumItem) -> Unit = { item -> onOpenAlbum(item.title, displayName, item.browseId) }
                val latest = remember(page) { latestRelease(page.albums + page.singles) }
                val about = rememberAboutFacts(page, profile)
                val bio = remember(page, profile) { profile?.bio?.let(::readableBio) ?: page.bio?.let(::readableBio) }

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        bottom = Space.xl + LocalMiniPlayerScrollClearance.current + safeDrawingBottomPadding(),
                    ),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(key = "hero") {
                        val controls: @Composable () -> Unit = {
                            HeroControls(
                                playing = isArtistPlaying,
                                canPlay = page.topSongs.isNotEmpty(),
                                favorite = isFavorite,
                                onInfo = { subPage = ArtistSubPage.About },
                                onPlay = playOrPause,
                                onFavorite = toggleFavorite,
                            )
                        }
                        if (photoUrl != null) {
                            ArtistHero(
                                name = displayName,
                                photo = photoUrl,
                                onPhotoError = { photoIndex++ },
                                logoUrl = profile?.logoUrl,
                                titleSettled = profileSettled,
                                hasConcerts = concerts.isNotEmpty(),
                                onConcerts = { openConcerts(null) },
                                videoUrl = motionVideoUrl?.takeIf { showHeaderVideo },
                                scrollOffset = {
                                    if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset else 0
                                },
                                onOpenVideo = { showFullscreenMotion = true },
                                controls = controls,
                            )
                        } else {
                            CompactArtistHeader(name = displayName, controls = controls)
                        }
                    }

                    if (latest != null) {
                        item(key = "latest_release") {
                            LatestReleaseCard(item = latest, artist = displayName, onClick = { openRelease(latest) })
                        }
                    }

                    if (page.topSongs.isNotEmpty()) {
                        item(key = "top_songs_header") {
                            DetailSectionHeader(
                                title = stringResource(R.string.art_top_songs),
                                onSeeAll = if (page.topSongs.size > TOP_SONGS_PREVIEW) ({ subPage = ArtistSubPage.TopSongs }) else null,
                            )
                        }
                        val preview = page.topSongs.take(TOP_SONGS_PREVIEW)
                        itemsIndexed(
                            preview,
                            key = { index, track -> "song_${track.videoId ?: track.title}_$index" },
                            contentType = { _, _ -> "artist_song" },
                        ) { index, track ->
                            ArtistSongRow(
                                track = track,
                                subtitle = songSubtitle(track, releaseYears, stringResource(R.string.art_type_single)),
                                playing = isTrackPlaying(playbackState.isPlaying, playbackState.current, track),
                                onClick = { viewModel.playAll(startIndex = index) },
                                onMore = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedTrackMenu = track
                                },
                            )
                        }
                    }

                    if (page.albums.isNotEmpty()) {
                        detailShelf(
                            key = "albums",
                            title = { stringResource(R.string.art_albums) },
                            onSeeAll = { subPage = ArtistSubPage.Albums },
                        ) {
                            items(page.albums.distinctBy { it.browseId.ifBlank { it.title } }, key = { "album_${it.browseId.ifBlank { it.title }}" }) { item ->
                                ReleaseTile(item, displayName, width = screenWidth.share(RELEASE_TILE_SHARE), onClick = { openRelease(item) })
                            }
                        }
                    }

                    if (page.videos.isNotEmpty()) {
                        detailShelf(
                            key = "videos",
                            title = { stringResource(R.string.art_music_videos) },
                            onSeeAll = { subPage = ArtistSubPage.Videos },
                        ) {
                            items(page.videos.distinctBy { "video_${it.videoId}" }, key = { "video_${it.videoId}" }) { video ->
                                MediaTile(
                                    title = video.title,
                                    subtitle = video.subtitle,
                                    artworkUrl = video.thumbnailUrl,
                                    shape = MediaTileShape.Wide,
                                    width = screenWidth.share(VIDEO_TILE_SHARE, max = Sizes.tileTallWidth + Sizes.tileSquare),
                                    onClick = { viewModel.playVideo(video) },
                                )
                            }
                        }
                    }

                    if (concerts.isNotEmpty()) {
                        item(key = "concerts") {
                            DetailSectionHeader(
                                title = stringResource(R.string.art_upcoming_concerts),
                                onSeeAll = { openConcerts(null) },
                            )
                            concerts.take(3).forEach { concert ->
                                ConcertRow(concert, onClick = { openConcerts(concert.url) })
                            }
                        }
                    }

                    if (page.singles.isNotEmpty()) {
                        detailShelf(
                            key = "singles",
                            title = { stringResource(R.string.art_singles_eps) },
                            onSeeAll = { subPage = ArtistSubPage.Singles },
                        ) {
                            items(page.singles.distinctBy { it.browseId.ifBlank { it.title } }, key = { "single_${it.browseId.ifBlank { it.title }}" }) { item ->
                                ReleaseTile(item, displayName, width = screenWidth.share(RELEASE_TILE_SHARE), onClick = { openRelease(item) })
                            }
                        }
                    }

                    if (page.playlists.isNotEmpty()) {
                        detailShelf(
                            key = "playlists",
                            title = { stringResource(R.string.art_playlists) },
                            onSeeAll = { subPage = ArtistSubPage.Playlists },
                        ) {
                            items(page.playlists.distinctBy { "playlist_${it.playlistId}" }, key = { "playlist_${it.playlistId}" }) { playlist ->
                                MediaTile(
                                    title = playlist.title,
                                    subtitle = playlist.subtitle,
                                    artworkUrl = playlist.artworkUrl,
                                    width = screenWidth.share(RELEASE_TILE_SHARE),
                                    onClick = { onOpenPlaylist(playlist.playlistId) },
                                )
                            }
                        }
                    }

                    val similar = individualArtists(page)
                    if (similar.isNotEmpty()) {
                        detailShelf(key = "similar", title = { stringResource(R.string.art_fans_like) }, onSeeAll = null) {
                            items(similar.distinctBy { it.browseId.ifBlank { it.name } }, key = { "artist_${it.browseId.ifBlank { it.name }}" }) { artist ->
                                val name = ArtistHelper.primaryArtist(artist.name)
                                MediaTile(
                                    title = name,
                                    artworkUrl = artist.artworkUrl,
                                    artistName = name,
                                    shape = MediaTileShape.Circle,
                                    width = screenWidth.share(ARTIST_TILE_SHARE, max = Sizes.tileSquare),
                                    onClick = { onOpenArtist(name, artist.browseId) },
                                )
                            }
                        }
                    }

                    if (bio != null || about.isNotEmpty()) {
                        item(key = "about") {
                            DetailSectionHeader(
                                title = stringResource(R.string.art_about_artist, displayName),
                                onSeeAll = { subPage = ArtistSubPage.About },
                            )
                            AboutCard(bio = bio, facts = about, onClick = { subPage = ArtistSubPage.About })
                        }
                    }
                }
            }
        }
        }

        CompositionLocalProvider(LocalLiquidGlassBackdrop provides if (glassOn) pageBackdrop else null) {
        DetailToolbar(
            title = displayName,
            showTitle = scrolledPastName,
            onBack = onBack,
            onShare = shareArtist,
            shareLabel = stringResource(R.string.art_share),
            menu = { dismiss ->
                val artistPin = com.tuned.app.data.library.PinnedItem(
                    kind = com.tuned.app.data.library.PinKind.ARTIST,
                    title = displayName,
                    artworkUrl = data?.artworkUrl,
                    browseId = browseId,
                )
                DetailMenuItem(Icons.Rounded.PushPin, stringResource(if (pinsViewModel.isPinned(artistPin)) R.string.pin_remove else R.string.pin_add)) {
                    dismiss()
                    val now = pinsViewModel.toggle(artistPin)
                    undo?.show(if (now) pinnedMessage else unpinnedMessage)
                }
                DetailMenuItem(Icons.Rounded.Shuffle, stringResource(R.string.art_shuffle_cd)) { dismiss(); viewModel.playShuffle() }
                DetailMenuItem(TunedIcons.Mix, stringResource(R.string.art_radio_cd)) { dismiss(); viewModel.startArtistMix() }
                // Photo or the artist's video in the header (only when Apple has a video).
                if (motionVideoUrl != null) {
                    DetailMenuItem(
                        if (showHeaderVideo) Icons.Filled.Image else Icons.Filled.SmartDisplay,
                        stringResource(if (showHeaderVideo) R.string.art_show_photo else R.string.art_show_video),
                    ) { dismiss(); showHeaderVideo = !showHeaderVideo }
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

        AnimatedVisibility(
            visible = subPage != null && data != null,
            enter = if (subPage == ArtistSubPage.About) fadeIn(rememberMotion().slow()) else slideInHorizontally { it } + fadeIn(),
            exit = if (shownSubPage == ArtistSubPage.About) fadeOut(rememberMotion().slow()) else slideOutHorizontally { it } + fadeOut(),
            modifier = Modifier.zIndex(20f),
        ) {
            val page = data ?: return@AnimatedVisibility
            ArtistSubPageScreen(
                page = shownSubPage ?: return@AnimatedVisibility,
                data = page,
                displayName = displayName,
                profile = profile,
                photoUrl = photoUrl,
                screenWidth = screenWidth,
                playbackPlaying = playbackState.isPlaying,
                playbackCurrent = playbackState.current,
                onBack = { subPage = null },
                onPlaySong = { index -> viewModel.playAll(startIndex = index) },
                onSongMenu = { selectedTrackMenu = it },
                onOpenRelease = { item -> onOpenAlbum(item.title, displayName, item.browseId) },
                onPlayVideo = viewModel::playVideo,
                onOpenPlaylist = onOpenPlaylist,
                onTag = { genreBridge.genreExplorer.explore(it) },
            )
        }
    }

    if (showFullscreenMotion && motionVideoUrl != null) {
        Dialog(
            onDismissRequest = { showFullscreenMotion = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            EdgeToEdgeDialogWindow()
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                CanvasVideoPlayer(
                    videoUrl = fullscreenVideoUrl ?: motionVideoUrl,
                    isPlaying = true,
                    enableScrim = false,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    Modifier.padding(
                        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + Space.xs,
                        start = Space.lg,
                    ),
                ) {
                    GlassCircleButton(
                        icon = TunedIcons.ChevronBack,
                        contentDescription = stringResource(R.string.pd_back_cd),
                        onClick = { showFullscreenMotion = false },
                    )
                }
                // Functional fade so the name stays readable over any clip.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                        .padding(bottom = safeDrawingBottomPadding())
                        .padding(horizontal = Keyline.detail, vertical = Space.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = displayName,
                        style = AppType.display,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(Space.lg + Space.xs))
                    HeroControls(
                        playing = isArtistPlaying,
                        canPlay = data?.topSongs?.isNotEmpty() == true,
                        favorite = isFavorite,
                        onInfo = {
                            showFullscreenMotion = false
                            subPage = ArtistSubPage.About
                        },
                        onPlay = playOrPause,
                        onFavorite = toggleFavorite,
                        onDark = true,
                    )
                }
            }
        }
    }

    selectedTrackMenu?.let { track ->
        TrackContextMenuSheet(
            target = TrackMenuTarget.Track(track.title, track.artist, ""),
            capabilities = TrackMenuCapabilities(showCopyActions = true, showDeleteScrobble = false),
            playableTrack = track,
            playbackSourceLabel = artistName,
            onDismiss = { selectedTrackMenu = null },
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Header
// ---------------------------------------------------------------------------------------------

/**
 * Apple Music's artist header: the photo at full width, sharp at the top and fading into the
 * page, with the name and the controls centred over its lower part. The artist's own Apple
 * Music video plays over the photo when there is one.
 */
@Composable
private fun ArtistHero(
    name: String,
    photo: Any,
    onPhotoError: () -> Unit,
    logoUrl: String?,
    titleSettled: Boolean,
    hasConcerts: Boolean,
    onConcerts: () -> Unit,
    videoUrl: String?,
    scrollOffset: () -> Int,
    onOpenVideo: () -> Unit,
    controls: @Composable () -> Unit,
) {
    val colors = MaterialTheme.tunedColors
    val context = LocalContext.current
    val fadeMask = remember { Brush.verticalGradient(colorStops = easedFadeStops(from = 0.62f, to = 1f)) }
    val request = remember(photo) { ImageRequest.Builder(context).data(photo).crossfade(400).build() }
    val glassOn = LocalLiquidGlass.current && isLiquidGlassBackdropSupported()
    val photoBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val side = maxWidth
        Box(
            modifier = Modifier
                // Recorded as shown (moved and faded), so the glass over it matches what is behind.
                .liquidGlassSource(if (glassOn) photoBackdrop else null)
                .fillMaxWidth()
                .height(side)
                .graphicsLayer {
                    translationY = scrollOffset() * 0.4f
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    drawRect(fadeMask, blendMode = BlendMode.DstIn)
                }
                .then(if (videoUrl != null) Modifier.clickable(onClick = onOpenVideo) else Modifier),
        ) {
            AsyncImage(
                model = request,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize(),
                onError = { onPhotoError() },
            )
            if (videoUrl != null) {
                CanvasVideoPlayer(
                    videoUrl = videoUrl,
                    isPlaying = true,
                    enableScrim = false,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            StatusBarShade(side)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                // The title slot ends where a one-line name would, so the buttons stay put.
                .padding(top = side * HERO_NAME_TOP - (Sizes.tileSquare - Sizes.touch), bottom = Space.xl + Space.xs)
                .padding(horizontal = Keyline.detail),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CompositionLocalProvider(LocalLiquidGlassBackdrop provides if (glassOn) photoBackdrop else null) {
                ArtistTitleSlot(
                    name = name,
                    logoUrl = logoUrl,
                    settled = titleSettled,
                    hasConcerts = hasConcerts,
                    onConcerts = onConcerts,
                )
                Spacer(Modifier.height(Space.lg + Space.xs))
                controls()
            }
        }
    }
}

/** Functional shade under the status bar so the clock and the toolbar stay readable. */
@Composable
private fun StatusBarShade(photoHeight: Dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(photoHeight * 0.25f)
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent))),
    )
}

/**
 * Apple Music's title slot: a fixed height with the artist's wordmark (Apple's logo for the
 * artist, dark theme only since most are drawn light) or the name at its bottom, and the
 * "Upcoming concerts" pill just above it. Nothing shows until the logo lookup answers, so the
 * name never flashes before the logo.
 */
@Composable
private fun ArtistTitleSlot(
    name: String,
    logoUrl: String?,
    settled: Boolean,
    hasConcerts: Boolean,
    onConcerts: () -> Unit,
) {
    val colors = MaterialTheme.tunedColors
    val dark = colors.bg.luminance() < 0.5f
    var logoFailed by remember(logoUrl) { mutableStateOf(false) }
    val state = when {
        !settled -> TitleState.Waiting
        dark && logoUrl != null && !logoFailed -> TitleState.Logo
        else -> TitleState.Name
    }
    var titleHeight by remember { mutableIntStateOf(0) }
    val gap = with(LocalDensity.current) { Space.md.roundToPx() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Sizes.tileSquare),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Crossfade(targetState = state, animationSpec = rememberMotion().base(), label = "artistTitle") { shown ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                when (shown) {
                    TitleState.Waiting -> Unit
                    TitleState.Logo -> AsyncImage(
                        model = logoUrl,
                        contentDescription = name,
                        contentScale = ContentScale.Fit,
                        onError = { logoFailed = true },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .heightIn(max = Sizes.tileSquare)
                            .onSizeChanged { titleHeight = it.height },
                    )
                    TitleState.Name -> Text(
                        text = name,
                        style = AppType.display,
                        color = colors.text1,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.onSizeChanged { titleHeight = it.height },
                    )
                }
            }
        }
        // Floats over the title without taking layout space, so its arrival moves nothing.
        AnimatedVisibility(
            visible = hasConcerts && state != TitleState.Waiting,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.offset { IntOffset(0, -(titleHeight + gap)) },
        ) {
            ConcertsPill(onClick = onConcerts)
        }
    }
}

private enum class TitleState { Waiting, Logo, Name }

/** Apple Music's "Upcoming concerts" pill over the artist name. */
@Composable
private fun ConcertsPill(onClick: () -> Unit) {
    val colors = MaterialTheme.tunedColors
    Row(
        modifier = Modifier
            .glassSurface(AppShape.full, fill = glassFill(), hairline = true)
            .clickable(onClick = onClick)
            .padding(horizontal = Space.md, vertical = Space.xs + Space.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.xs + Space.xxs),
    ) {
        Icon(Icons.Rounded.ConfirmationNumber, contentDescription = null, tint = colors.text1, modifier = Modifier.size(Sizes.iconInline))
        Text(stringResource(R.string.art_upcoming_concerts), style = AppType.caption, color = colors.text1)
    }
}

/** A concert: a date tile (month over day), then the city, then the venue from Apple's line. */
@Composable
private fun ConcertRow(concert: AppleConcert, onClick: () -> Unit) {
    val colors = MaterialTheme.tunedColors
    val local = remember(concert) { concert.date?.atZone(concert.zone ?: java.time.ZoneId.systemDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Keyline.detail, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.lg),
    ) {
        Column(
            modifier = Modifier
                .size(Sizes.touch + Space.xs)
                .clip(AppShape.md)
                .background(colors.surface1),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                local?.format(DateTimeFormatter.ofPattern("MMM")).orEmpty(),
                style = AppType.caption,
                color = colors.accent,
            )
            Text(local?.dayOfMonth?.toString().orEmpty(), style = AppType.title, color = colors.text1)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
            Text(concert.city, style = AppType.bodyStrong, color = colors.text1, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (concert.subtitle.isNotBlank()) {
                Text(concert.subtitle.substringBefore('·').trim(), style = AppType.bodyS, color = colors.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Header for an artist without a photo: the name on the toolbar's line, the controls under it. */
@Composable
private fun CompactArtistHeader(name: String, controls: @Composable () -> Unit) {
    val colors = MaterialTheme.tunedColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = Space.xs, bottom = Space.xl + Space.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Sizes.touch)
                // Clear of the back circle and the share/more capsule.
                .padding(horizontal = Sizes.touch * 2 + Space.lg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name,
                style = AppType.headline,
                color = colors.text1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(Space.md))
        controls()
    }
}

/** ⓘ, a large Play, and ☆ (favourite), centred with 24dp between them (Apple's ControlsStack). */
@Composable
private fun HeroControls(
    playing: Boolean,
    canPlay: Boolean,
    favorite: Boolean,
    onInfo: () -> Unit,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onDark: Boolean = false,
) {
    val colors = MaterialTheme.tunedColors
    val ink = if (onDark) Color.White else colors.text1
    val paper = if (onDark) Color.Black else colors.bg
    Row(
        horizontalArrangement = Arrangement.spacedBy(Space.xl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DetailSideButton(TunedIcons.InfoLetter, stringResource(R.string.art_info_cd), onInfo, ink = ink)
        val interaction = remember { MutableInteractionSource() }
        IconButton(
            onClick = onPlay,
            enabled = canPlay,
            interactionSource = interaction,
            modifier = Modifier
                .size(Sizes.playButtonPlayer)
                .pressScale(interaction),
            shape = AppShape.full,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = ink,
                contentColor = paper,
                disabledContainerColor = ink.copy(alpha = 0.3f),
                disabledContentColor = paper,
            ),
        ) {
            Icon(
                imageVector = if (playing) Icons.Filled.Pause else TunedIcons.Play,
                contentDescription = stringResource(if (playing) R.string.cmp_pause else R.string.art_play_cd),
                modifier = Modifier.size(Sizes.icon + Space.md),
            )
        }
        DetailSideButton(
            icon = if (favorite) TunedIcons.StarFilled else TunedIcons.Star,
            contentDescription = stringResource(if (favorite) R.string.art_unfavorite_cd else R.string.art_favorite_cd),
            onClick = onFavorite,
            ink = ink,
            tint = if (favorite) colors.accent else ink,
        )
    }
}


/** A top-songs row: 48dp cover, title, the album (or "Single") and ⋯, no separators. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArtistSongRow(
    track: PlayableTrack,
    subtitle: String,
    playing: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit,
) {
    val colors = MaterialTheme.tunedColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.rowCompact)
            .combinedClickable(onClick = onClick, onLongClick = onMore)
            .padding(start = Keyline.detail, end = Keyline.detail - Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(Sizes.artRow).clip(AppShape.xs)) {
            ArtworkImage(
                name = track.title,
                artist = ArtistHelper.primaryArtist(track.artist),
                embeddedUrl = track.artworkUrl,
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
                text = track.title,
                style = AppType.body,
                color = if (playing) colors.accent else colors.text1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (track.explicit) {
                    ExplicitTag()
                    Spacer(Modifier.width(Space.xs + Space.xxs))
                }
                Text(text = subtitle, style = AppType.bodyS, color = colors.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
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


/** [LatestReleaseCard]'s shape while the page loads. */
@Composable
private fun LatestReleaseSkeleton() {
    val colors = MaterialTheme.tunedColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Keyline.detail)
            .alpha(skeletonAlpha())
            .clip(AppShape.lg)
            .background(colors.surface1)
            .padding(Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(Sizes.avatarHero).clip(AppShape.sm).background(colors.surface2))
        Spacer(Modifier.width(Space.lg))
        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Box(Modifier.fillMaxWidth(0.25f).height(Sizes.skeletonSubtitle).clip(AppShape.xs).background(colors.surface2))
            Box(Modifier.fillMaxWidth(0.7f).height(Sizes.skeletonTitle).clip(AppShape.xs).background(colors.surface2))
            Box(Modifier.fillMaxWidth(0.35f).height(Sizes.skeletonSubtitle).clip(AppShape.xs).background(colors.surface2))
        }
    }
}

/** Apple Music's latest-release card: cover, year, title and what it is. */
@Composable
private fun LatestReleaseCard(item: ArtistAlbumItem, artist: String, onClick: () -> Unit) {
    val colors = MaterialTheme.tunedColors
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Keyline.detail)
            .pressScale(interaction)
            .clip(AppShape.lg)
            .background(colors.surface1)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(Sizes.avatarHero).clip(AppShape.sm)) {
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
        Spacer(Modifier.width(Space.lg))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
            item.year?.let { Text(it, style = AppType.caption, color = colors.text2, maxLines = 1) }
            Text(item.title, style = AppType.title, color = colors.text1, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(releaseType(item), style = AppType.bodyS, color = colors.text2, maxLines = 1)
        }
        Icon(TunedIcons.ChevronRight, contentDescription = null, tint = colors.text3, modifier = Modifier.size(Sizes.iconRow))
    }
}

/** One fact under the bio: "Hometown / Sheffield, England". */
private data class AboutFact(val label: String, val value: String)

@Composable
private fun rememberAboutFacts(page: ArtistPageData, profile: AppleArtistProfile?): List<AboutFact> {
    val hometown = stringResource(R.string.art_hometown)
    val born = stringResource(R.string.art_born)
    val formed = stringResource(R.string.art_formed)
    val genre = stringResource(R.string.art_genre)
    val listeners = stringResource(R.string.art_listeners)
    return remember(page, profile) {
        buildList {
            profile?.origin?.let { add(AboutFact(hometown, it)) }
            profile?.bornOrFormed?.let { add(AboutFact(if (profile.isGroup == false) born else formed, readableDate(it))) }
            (profile?.genres?.firstOrNull() ?: page.tags.firstOrNull()?.replaceFirstChar { it.titlecase() })
                ?.let { add(AboutFact(genre, it)) }
            (page.monthlyListeners ?: page.subscribers)?.trim()?.takeIf { it.isNotEmpty() }
                ?.let { add(AboutFact(listeners, it.substringBefore(' '))) }
        }
    }
}

/** About card: four lines of the bio, then up to three facts in columns. */
@Composable
private fun AboutCard(bio: String?, facts: List<AboutFact>, onClick: () -> Unit) {
    val colors = MaterialTheme.tunedColors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Keyline.detail)
            .clip(AppShape.lg)
            .background(colors.surface1)
            .clickable(onClick = onClick)
            .padding(Space.lg),
    ) {
        if (bio != null) {
            Text(bioText(bio), style = AppType.body, color = colors.text1, maxLines = 4, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(Space.sm))
            Text(stringResource(R.string.art_read_more), style = AppType.label, color = colors.text1)
        }
        if (facts.isNotEmpty()) {
            if (bio != null) Spacer(Modifier.height(Space.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.lg)) {
                facts.take(3).forEach { fact ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
                        Text(fact.label, style = AppType.caption, color = colors.text2, maxLines = 1)
                        Text(fact.value, style = AppType.bodyStrong, color = colors.text1, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Sub-pages
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArtistSubPageScreen(
    page: ArtistSubPage,
    data: ArtistPageData,
    displayName: String,
    profile: AppleArtistProfile?,
    photoUrl: Any?,
    screenWidth: Dp,
    playbackPlaying: Boolean,
    playbackCurrent: PlayableTrack?,
    onBack: () -> Unit,
    onPlaySong: (Int) -> Unit,
    onSongMenu: (PlayableTrack) -> Unit,
    onOpenRelease: (ArtistAlbumItem) -> Unit,
    onPlayVideo: (com.tuned.app.data.model.ArtistVideoItem) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onTag: (String) -> Unit,
) {
    val colors = MaterialTheme.tunedColors
    val bottom = Space.xl + LocalMiniPlayerScrollClearance.current + safeDrawingBottomPadding()
    // Below the bar (4 + 48 + 4) and clear of its 24dp fade, so the back button never crowds the first row.
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + Space.xs + Sizes.touch + Space.xs + Space.xl
    val title = when (page) {
        ArtistSubPage.TopSongs -> stringResource(R.string.art_top_songs)
        ArtistSubPage.Albums -> stringResource(R.string.art_albums)
        ArtistSubPage.Singles -> stringResource(R.string.art_singles_eps)
        ArtistSubPage.Videos -> stringResource(R.string.art_music_videos)
        ArtistSubPage.Playlists -> stringResource(R.string.art_playlists)
        ArtistSubPage.About -> stringResource(R.string.art_about_artist, displayName)
    }
    val gridPadding = PaddingValues(start = Keyline.detail, end = Keyline.detail, top = top, bottom = bottom)
    val aboutListState = rememberLazyListState()
    // Over the About photo the bar stays clear; it fills in once the photo has scrolled away.
    val aboutPastPhoto by remember { derivedStateOf { aboutListState.firstVisibleItemIndex > 0 } }

    val glassOn = LocalLiquidGlass.current && isLiquidGlassBackdropSupported()
    val subBackdrop = rememberLayerBackdrop()

    Box(Modifier.fillMaxSize().background(colors.bg)) {
        Box(Modifier.fillMaxSize().liquidGlassSource(if (glassOn) subBackdrop else null)) {
        when (page) {
            ArtistSubPage.TopSongs -> {
                val years = remember(data) { releaseYears(data) }
                LazyColumn(contentPadding = PaddingValues(top = top, bottom = bottom)) {
                    itemsIndexed(data.topSongs, key = { index, track -> "all_${track.videoId ?: track.title}_$index" }) { index, track ->
                        ArtistSongRow(
                            track = track,
                            subtitle = songSubtitle(track, years, stringResource(R.string.art_type_single)),
                            playing = isTrackPlaying(playbackPlaying, playbackCurrent, track),
                            onClick = { onPlaySong(index) },
                            onMore = { onSongMenu(track) },
                        )
                    }
                }
            }
            ArtistSubPage.Albums, ArtistSubPage.Singles -> {
                val releases = if (page == ArtistSubPage.Albums) data.albums else data.singles
                LazyVerticalGrid(
                    columns = GridCells.Fixed(Keyline.gridColumns),
                    contentPadding = gridPadding,
                    horizontalArrangement = Arrangement.spacedBy(Space.md),
                    verticalArrangement = Arrangement.spacedBy(Space.xl),
                ) {
                    items(releases.distinctBy { it.browseId.ifBlank { it.title } }, key = { it.browseId.ifBlank { it.title } }) { item ->
                        ReleaseTile(item, displayName, width = null, onClick = { onOpenRelease(item) })
                    }
                }
            }
            ArtistSubPage.Videos -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed((Keyline.gridColumns - 1).coerceAtLeast(1)),
                    contentPadding = gridPadding,
                    horizontalArrangement = Arrangement.spacedBy(Space.md),
                    verticalArrangement = Arrangement.spacedBy(Space.xl),
                ) {
                    items(data.videos.distinctBy { it.videoId }, key = { it.videoId }) { video ->
                        MediaTile(
                            title = video.title,
                            subtitle = video.subtitle,
                            artworkUrl = video.thumbnailUrl,
                            shape = MediaTileShape.Wide,
                            width = null,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onPlayVideo(video) },
                        )
                    }
                }
            }
            ArtistSubPage.Playlists -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(Keyline.gridColumns),
                    contentPadding = gridPadding,
                    horizontalArrangement = Arrangement.spacedBy(Space.md),
                    verticalArrangement = Arrangement.spacedBy(Space.xl),
                ) {
                    items(data.playlists.distinctBy { it.playlistId }, key = { it.playlistId }) { playlist ->
                        MediaTile(
                            title = playlist.title,
                            subtitle = playlist.subtitle,
                            artworkUrl = playlist.artworkUrl,
                            width = null,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onOpenPlaylist(playlist.playlistId) },
                        )
                    }
                }
            }
            ArtistSubPage.About -> {
                ArtistAboutPage(
                    displayName = displayName,
                    photoUrl = photoUrl,
                    bio = remember(data, profile) { profile?.bio?.let(::readableBio) ?: data.bio?.let(::readableBio) },
                    profile = profile,
                    tags = data.tags,
                    listState = aboutListState,
                    topInset = top,
                    bottomInset = bottom,
                    onTag = onTag,
                )
            }
        }

        }

        // The sub-page's own bar: back and its title, on the page colour.
        CompositionLocalProvider(LocalLiquidGlassBackdrop provides if (glassOn) subBackdrop else null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .barScrim(visible = page != ArtistSubPage.About || photoUrl == null || aboutPastPhoto)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = Space.lg, vertical = Space.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassCircleButton(TunedIcons.ChevronBack, stringResource(R.string.pd_back_cd), onBack)
            if (page != ArtistSubPage.About || aboutPastPhoto) {
                Text(
                    if (page == ArtistSubPage.About) displayName else title,
                    style = AppType.title,
                    color = colors.text1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = Sizes.touch),
                )
            }
        }
        }
    }
}

/**
 * The page behind ⓘ, laid out as Apple Music's: the photo with a long fade and the name over it,
 * From and Formed side by side, the genres as pills, then "About" and the editorial bio.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArtistAboutPage(
    displayName: String,
    photoUrl: Any?,
    bio: String?,
    profile: AppleArtistProfile?,
    tags: List<String>,
    listState: LazyListState,
    topInset: Dp,
    bottomInset: Dp,
    onTag: (String) -> Unit,
) {
    val colors = MaterialTheme.tunedColors
    val from = stringResource(R.string.art_hometown)
    val born = stringResource(R.string.art_born)
    val formed = stringResource(R.string.art_formed)
    val facts = remember(profile) {
        listOfNotNull(
            profile?.origin?.let { from to it },
            profile?.bornOrFormed?.let { (if (profile.isGroup == false) born else formed) to readableDate(it) },
        )
    }
    val genres = remember(profile, tags) {
        profile?.genres.orEmpty().filterNot { it.equals("Music", ignoreCase = true) }
            .ifEmpty { tags.map { tag -> tag.replaceFirstChar { it.titlecase() } } }
            .distinctBy { it.lowercase() }
            .take(8)
    }
    // The hero's fade and shade, so going in and out of ⓘ only changes what's under the photo.
    val fade = remember { Brush.verticalGradient(colorStops = easedFadeStops(from = 0.62f, to = 1f)) }

    LazyColumn(state = listState, contentPadding = PaddingValues(bottom = bottomInset)) {
        item(key = "about_header") {
            Box(
                Modifier
                    .fillMaxWidth()
                    .then(if (photoUrl != null) Modifier.aspectRatio(1f) else Modifier.padding(top = topInset)),
            ) {
                if (photoUrl != null) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                drawRect(fade, blendMode = BlendMode.DstIn)
                            },
                    )
                    BoxWithConstraints(Modifier.fillMaxWidth()) { StatusBarShade(maxWidth) }
                }
                Text(
                    displayName,
                    style = AppType.display,
                    color = colors.text1,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = Keyline.detail, vertical = Space.sm),
                )
            }
        }
        if (facts.isNotEmpty()) {
            item(key = "about_facts") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Keyline.detail)
                        .padding(top = Space.lg),
                ) {
                    facts.forEach { (label, value) ->
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
                            Text(label, style = AppType.bodyS, color = colors.text2)
                            Text(value, style = AppType.bodyStrong, color = colors.text1)
                        }
                    }
                    if (facts.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        if (genres.isNotEmpty()) {
            item(key = "about_genres") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Keyline.detail)
                        .padding(top = Space.lg),
                    verticalArrangement = Arrangement.spacedBy(Space.sm),
                ) {
                    Text(stringResource(R.string.art_genre), style = AppType.bodyS, color = colors.text2)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Space.sm),
                        verticalArrangement = Arrangement.spacedBy(Space.sm),
                    ) {
                        genres.forEach { genre ->
                            Text(
                                genre,
                                style = AppType.label,
                                color = colors.text1,
                                modifier = Modifier
                                    .clip(AppShape.full)
                                    .background(colors.text1.copy(alpha = 0.1f))
                                    .clickable { onTag(genre) }
                                    .padding(horizontal = Space.md + Space.xxs, vertical = Space.xs + Space.xxs),
                            )
                        }
                    }
                }
            }
        }
        if (bio != null) {
            item(key = "about_bio") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Keyline.detail)
                        .padding(top = Space.xl),
                    verticalArrangement = Arrangement.spacedBy(Space.sm),
                ) {
                    Text(stringResource(R.string.art_about), style = AppType.titleL, color = colors.text1)
                    Text(
                        bioText(bio),
                        style = AppType.body.copy(lineHeight = AppType.body.lineHeight * 1.2f),
                        color = colors.text2,
                    )
                }
            }
        }
    }
}

/** Apple's bios carry light HTML (<i>Album</i>, <br>); render it instead of showing the tags. */
private fun bioText(bio: String): AnnotatedString = AnnotatedString.fromHtml(bio.replace("\n", "<br>"))


// ---------------------------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------------------------

@dagger.hilt.android.lifecycle.HiltViewModel
class ArtistDetailGenreBridge @javax.inject.Inject constructor(
    val genreExplorer: GenreExplorer,
) : androidx.lifecycle.ViewModel()

/** A share of the screen width for a shelf tile, capped so tablets don't get giant covers. */
private fun Dp.share(fraction: Float, max: Dp = Sizes.tileTallWidth): Dp = (this * fraction).coerceAtMost(max)

private fun isTrackPlaying(isPlaying: Boolean, current: PlayableTrack?, track: PlayableTrack): Boolean =
    isPlaying && current?.title.equals(track.title, ignoreCase = true) &&
        current?.artist.equals(track.artist, ignoreCase = true)

/** Release title (lowercase) to year, from the page's own albums and singles. */
private fun releaseYears(page: ArtistPageData): Map<String, String> =
    (page.albums + page.singles)
        .mapNotNull { item -> item.year?.let { item.title.lowercase().trim() to it } }
        .toMap()

/** Similar artists split into one tile per name ("A & B" becomes two). */
private fun individualArtists(page: ArtistPageData) =
    page.similarArtists.flatMap { item ->
        val split = ArtistHelper.splitArtists(item.name)
        if (split.size <= 1) listOf(item)
        else split.map { singleName -> item.copy(name = singleName, browseId = "") }
    }.distinctBy { it.name.lowercase().trim() }

/** One fact: the album ("AM"), "Single" for a single, else the year. */
private fun songSubtitle(track: PlayableTrack, years: Map<String, String>, singleLabel: String): String {
    val title = track.title.trim()
    val album = track.album?.trim()?.takeIf { it.isNotEmpty() }
    val isSingle = album != null && album.equals(title, ignoreCase = true)
    val year = years[(album ?: title).lowercase()]
    return when {
        album != null && !isSingle -> album
        isSingle -> singleLabel
        else -> year.orEmpty()
    }
}

/** "1965-08-30" as "30 Aug 1965" in the user's locale; anything else as given. */
private fun readableDate(raw: String): String =
    runCatching { LocalDate.parse(raw).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) }
        .getOrDefault(raw)

/** The bio without bare links (YouTube descriptions often end in a tracking URL). */
private fun readableBio(bio: String): String? =
    bio.lines()
        .map { line -> line.replace(Regex("""https?://\S+"""), "").trim() }
        .filter { it.isNotEmpty() }
        .joinToString("\n")
        .takeIf { it.isNotBlank() }

/** The newest release by year; ties keep YouTube Music's order. */
private fun latestRelease(items: List<ArtistAlbumItem>): ArtistAlbumItem? =
    items.filter { it.year?.toIntOrNull() != null }.maxByOrNull { it.year!!.toInt() }

/** Mask stops: opaque down to [from], then a smootherstep fade to clear at [to]. */
private fun easedFadeStops(from: Float, to: Float, steps: Int = 12): Array<Pair<Float, Color>> =
    Array(steps + 2) { i ->
        if (i == 0) return@Array 0f to Color.Black
        val t = (i - 1) / steps.toFloat()
        val eased = t * t * t * (t * (t * 6f - 15f) + 10f)
        (from + (to - from) * t) to Color.Black.copy(alpha = 1f - eased)
    }

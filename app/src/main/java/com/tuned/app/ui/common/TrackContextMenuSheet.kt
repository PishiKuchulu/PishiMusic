package com.tuned.app.ui.common

import android.annotation.SuppressLint

import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tuned.app.R
import com.tuned.app.data.jam.JamRepository
import com.tuned.app.playback.PlayableTrack
import com.tuned.app.ui.common.LocalUndoController
import com.tuned.app.ui.generate.MixLauncher
import com.tuned.app.ui.navigation.ArtistAlbumNavigator
import com.tuned.app.ui.player.LocalAddToPlaylist
import com.tuned.app.ui.player.LocalMusicPlayer
import com.tuned.app.ui.player.PlayerCastMenuRow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Which optional rows this instance of the sheet should show — matches the
 *  reference's per-screen menu variance (§1.7 / §6.5 / home.js's reduced
 *  menu): Home and Discover omit Copy/Delete Scrobble; Playlist/Search/
 *  Genre Detail include everything. */
data class TrackMenuCapabilities(
    val showCopyActions: Boolean = true,
    val showDeleteScrobble: Boolean = true,
)

sealed interface TrackMenuTarget {
    data class Track(val name: String, val artist: String, val url: String) : TrackMenuTarget
    data class Artist(val name: String, val url: String) : TrackMenuTarget
    data class Album(val name: String, val artist: String, val url: String) : TrackMenuTarget
}

@HiltViewModel
class ArtistAlbumMenuViewModel @Inject constructor(
    private val navigator: ArtistAlbumNavigator,
) : ViewModel() {
    fun openArtist(name: String, browseId: String? = null) {
        navigator.openArtist(name, browseId)
    }

    fun openAlbum(title: String, artist: String = "", browseId: String? = null) {
        navigator.openAlbum(title, artist, browseId)
    }
}

/** Lets the menus pin things to Library. */
@HiltViewModel
class PinsViewModel @Inject constructor(
    private val store: com.tuned.app.data.library.PinsStore,
) : ViewModel() {
    val pins = store.pins
    fun isPinned(item: com.tuned.app.data.library.PinnedItem) = store.isPinned(item)
    fun toggle(item: com.tuned.app.data.library.PinnedItem) = store.toggle(item)
}

/** Thin bridge so TrackContextMenuSheet can reach the MixLauncher singleton
 *  the same way it already reaches GenreRowViewModel — every caller gets
 *  "Start Mix with this Song" working for free, with no per-screen wiring. */
@HiltViewModel
class StartMixMenuViewModel @Inject constructor(private val mixLauncher: MixLauncher) : ViewModel() {
    fun startMix(trackName: String, artistName: String, videoId: String? = null) {
        mixLauncher.startMix(trackName, artistName, videoId)
    }
}

enum class TrackDownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
}

@HiltViewModel
class DownloadMenuViewModel @Inject constructor(
    private val downloadManager: com.tuned.app.data.download.TrackDownloadManager,
    settingsPreferences: com.tuned.app.data.local.SettingsPreferences,
    private val routeNavigator: com.tuned.app.ui.navigation.AppRouteNavigator,
) : ViewModel() {
    val activeDownloads = downloadManager.downloads

    /** Mirrors Settings -> Audio & Streaming -> Download Quality so the
     *  3-dot menu label always shows what will actually be downloaded. */
    val downloadQuality = settingsPreferences.settings
        .map { it.downloadQuality }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), 27)

    suspend fun checkStatus(title: String, artist: String): TrackDownloadStatus {
        if (downloadManager.isDownloading(title, artist)) return TrackDownloadStatus.DOWNLOADING
        if (downloadManager.isTrackDownloaded(title, artist)) return TrackDownloadStatus.DOWNLOADED
        return TrackDownloadStatus.NOT_DOWNLOADED
    }

    fun download(
        title: String,
        artist: String,
        album: String? = null,
        artworkUrl: String? = null,
        videoId: String? = null,
        durationMs: Long? = null,
    ) {
        downloadManager.downloadTrack(
            title = title,
            artist = artist,
            album = album,
            artworkUrl = artworkUrl,
            videoId = videoId,
            durationMs = durationMs,
        )
        routeNavigator.navigateTo(com.tuned.app.ui.navigation.Screen.Downloads.route)
    }

    fun openDownloads() {
        routeNavigator.navigateTo(com.tuned.app.ui.navigation.Screen.Downloads.route)
    }
}

/** Short label for the configured download tier — must stay in sync with the
 *  Download Quality tiers in SettingsScreen (28/27/7/6/5/4/-1). */
fun downloadLabelForQuality(context: android.content.Context, quality: Int): String {
    val tier = when (quality) {
        28 -> context.getString(R.string.menu_quality_atmos)
        27, 7 -> context.getString(R.string.menu_quality_hires)
        6 -> context.getString(R.string.menu_quality_cd)
        5 -> context.getString(R.string.menu_quality_standard)
        4 -> context.getString(R.string.menu_quality_saver)
        -1 -> context.getString(R.string.menu_quality_youtube)
        else -> context.getString(R.string.menu_quality_hires)
    }
    return context.getString(R.string.menu_download_tier, tier)
}

/** The configured download tier on its own ("Hi-Res"), for the menu's value column. */
fun downloadTierForQuality(context: android.content.Context, quality: Int): String =
    downloadLabelForQuality(context, quality).let { label ->
        Regex("\\((.+)\\)").find(label)?.groupValues?.get(1) ?: label
    }

@HiltViewModel
class RecommendationExclusionMenuViewModel @Inject constructor(
    private val discoverRepository: com.tuned.app.data.discover.DiscoverRepository,
) : ViewModel() {
    fun exclude(trackName: String, artistName: String) {
        viewModelScope.launch {
            discoverRepository.excludeFromRecommendations(trackName, artistName)
        }
    }
}

/** Same idea, for the Genre row — every caller (Home, Discover, Playlist,
 *  Search) gets "tap the genre to open it in Genres" for free, without
 *  each of them needing to pass onExploreGenre + a NavController down
 *  through their own screen. */
@HiltViewModel
class ExploreGenreMenuViewModel @Inject constructor(private val genreExplorer: com.tuned.app.ui.genres.GenreExplorer) : ViewModel() {
    fun explore(genre: String) {
        genreExplorer.explore(genre)
    }
}

/** music.youtube.com rather than youtube.com: if YouTube Music is installed
 *  it's registered as that domain's Android App Link target, so a plain
 *  ACTION_VIEW opens the app directly — no explicit package targeting (and
 *  the manifest <queries> visibility declaration that would need) required.
 *  Falls back to the YouTube Music website when the app isn't installed. */
private fun openUrl(context: Context, url: String) {
    if (url.isBlank()) return
    try {
        val targetUri = if (url.startsWith("http://") || url.startsWith("https://")) {
            android.net.Uri.parse(url)
        } else {
            android.net.Uri.parse("https://$url")
        }
        context.startActivity(Intent(Intent.ACTION_VIEW, targetUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        android.util.Log.w("TrackMenu", "Couldn't open link: $url", e)
    }
}

private fun buildLastFmUrl(target: TrackMenuTarget): String {
    return when (target) {
        is TrackMenuTarget.Track -> if (target.url.isNotBlank() && !target.url.startsWith("/")) target.url else try {
            "https://www.last.fm/music/${android.net.Uri.encode(target.artist)}/_/${android.net.Uri.encode(target.name)}"
        } catch (e: Exception) { "" }
        is TrackMenuTarget.Artist -> if (target.url.isNotBlank()) target.url else try {
            "https://www.last.fm/music/${android.net.Uri.encode(target.name)}"
        } catch (e: Exception) { "" }
        is TrackMenuTarget.Album -> if (target.url.isNotBlank()) target.url else try {
            "https://www.last.fm/music/${android.net.Uri.encode(target.artist)}/${android.net.Uri.encode(target.name)}"
        } catch (e: Exception) { "" }
    }
}

/**
 * Faithful port of the shared track/artist/album 3-dot menu used across
 * Home, Playlist, Search, Discover, and Genre Detail (§1.7 / §6.5). One
 * component, capability-gated per screen rather than duplicated per screen.
 *
 * Layout, top to bottom: which song it is (cover, title, artist, genre pill),
 * the accent "Start a mix" pill, three quick cards (Play next, Add to queue,
 * Add to playlist), then grouped sections: Go to, Playback, About, and the
 * removal actions in the danger colour. It opens at 80% of the screen and the
 * first upward scroll grows it to full height; the sleep timer opens inside
 * the same sheet.
 *
 * Every surface reads the live app accent (MaterialTheme.colorScheme), so
 * switching accent (a preset, Monochrome, wallpaper Dynamic Color, or
 * Dynamic Now Playing) restyles this popup the same way it restyles the rest
 * of the app.
 *
 * [onStartMix] defaults to routing through MixLauncher (which
 * GenerateViewModel and MainShell both listen to) rather than requiring
 * every call site to wire it. A caller can still pass its own [onStartMix]
 * to override that default if a screen ever needs different behavior.
 *
 * [onExploreGenre] / [onDeleteScrobble] remain caller-supplied: those need
 * screen-specific navigation / API side effects this component doesn't own.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
// The sleep-timer toast is formatted inside the onSet callback with the
// runtime minute count; it cannot be resolved during composition.
@SuppressLint("LocalContextGetResourceValueCall")
fun TrackContextMenuSheet(
    target: TrackMenuTarget,
    capabilities: TrackMenuCapabilities,
    playableTrack: PlayableTrack? = null,
    onDismiss: () -> Unit,
    playbackSourceLabel: String = "Tuned",
    onPlayInTuned: (() -> Unit)? = null,
    onStartMix: ((trackName: String, artistName: String) -> Unit)? = null,
    onExploreGenre: ((genre: String) -> Unit)? = null,
    onDeleteScrobble: ((trackName: String, artistName: String) -> Unit)? = null,
    onRefreshArtwork: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onRemoveFromQueue: (() -> Unit)? = null,
    genreResolverViewModel: GenreRowViewModel = hiltViewModel(),
    startMixViewModel: StartMixMenuViewModel = hiltViewModel(),
    pinsViewModel: PinsViewModel = hiltViewModel(),
    exploreGenreViewModel: ExploreGenreMenuViewModel = hiltViewModel(),
    downloadViewModel: DownloadMenuViewModel = hiltViewModel(),
    exclusionViewModel: RecommendationExclusionMenuViewModel = hiltViewModel(),
    artistAlbumViewModel: ArtistAlbumMenuViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val musicPlayer = LocalMusicPlayer.current
    val addToPlaylist = LocalAddToPlaylist.current
    var showDetailsSheet by remember { mutableStateOf(false) }
    var showJamSheet by remember { mutableStateOf(false) }
    var resolvedGenre by remember(target) { mutableStateOf<String?>(null) }
    var resolvingGenre by remember(target) { mutableStateOf(false) }
    val activeDownloads by downloadViewModel.activeDownloads.collectAsStateWithLifecycle()
    val downloadQuality by downloadViewModel.downloadQuality.collectAsStateWithLifecycle()
    var isDownloaded by remember(target) { mutableStateOf(false) }

    LaunchedEffect(target, activeDownloads) {
        if (target is TrackMenuTarget.Track) {
            isDownloaded = runCatching {
                downloadViewModel.checkStatus(target.name, target.artist)
            }.getOrDefault(TrackDownloadStatus.NOT_DOWNLOADED) == TrackDownloadStatus.DOWNLOADED
        }
    }

    LaunchedEffect(target) {
        if (target is TrackMenuTarget.Track) {
            resolvingGenre = true
            resolvedGenre = runCatching { genreResolverViewModel.resolve(target.name, target.artist) }.getOrNull()
            resolvingGenre = false
        }
    }

    fun exploreGenre(genre: String) {
        if (onExploreGenre != null) onExploreGenre(genre)
        else exploreGenreViewModel.explore(genre)
    }

    if (showDetailsSheet && target is TrackMenuTarget.Track) {
        val playable = playableTrack ?: PlayableTrack(title = target.name, artist = target.artist)
        TrackDetailsSheet(
            title = target.name,
            artist = target.artist,
            album = playable.album,
            artworkUrl = playable.artworkUrl,
            videoId = playable.videoId,
            durationMs = playable.durationMs,
            onDismiss = {
                showDetailsSheet = false
                onDismiss()
            },
            onPlayTrack = {
                onPlayInTuned?.invoke() ?: musicPlayer.play(playable, sourceLabel = playbackSourceLabel)
            },
        )
        return
    }

    if (showJamSheet) {
        com.tuned.app.ui.jam.JamSheet(
            onDismiss = {
                showJamSheet = false
                onDismiss()
            },
        )
        return
    }

    val playerState by musicPlayer.state.collectAsStateWithLifecycle()
    var page by remember(target) { mutableStateOf(MenuPage.Main) }
    val undo = LocalUndoController.current
    fun notify(message: String) {
        if (undo != null) undo.show(message)
        else android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Picking an action slides the sheet down first, then closes it, instead
    // of removing it at once.
    val closeScope = rememberCoroutineScope()
    fun close() {
        closeScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }
    // One fixed height (the Signal path sheet's): the list scrolls inside it. It used to open at 80%
    // and spend the first swipe growing to full height, so the first scroll moved the sheet, not the list.
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.85f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        // The background behind is smoothly blurred by EdgeToEdgeDialogWindow.
        // A translucent/transparent container lets the soft blur shine through
        // while the individual action cards float with clean contrast on top.
        containerColor = blurSheetContainerColor(),
        scrimColor = Color.Black.copy(alpha = 0.32f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        dragHandle = {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp),
            ) {}
        },
    ) {
        EdgeToEdgeDialogWindow(visible = sheetState.targetValue != androidx.compose.material3.SheetValue.Hidden)
        // The sleep timer opens inside the same sheet instead of a dialog on top.
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                val forward = targetState == MenuPage.SleepTimer
                (slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(180))) togetherWith
                    (slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { if (forward) -it / 3 else it / 3 } + fadeOut(tween(120)))
            },
            label = "menuPage",
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveContentWidth(maxWidth = 600.dp)
                .align(Alignment.CenterHorizontally)
                .heightIn(max = maxHeight),
        ) { shown ->
            val pageScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fadeTopEdge(pageScroll)
                    .sheetScrollGuard()
                    .padding(horizontal = 14.dp)
                    // No stretch at the ends: the list stops where it stops.
                    .verticalScroll(pageScroll, overscrollEffect = null)
                    .padding(bottom = 24.dp + safeDrawingBottomPadding()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (shown) {
                    MenuPage.SleepTimer -> SleepTimerPage(
                        remainingMs = playerState.sleepTimerRemainingMs,
                        onBack = { page = MenuPage.Main },
                        onSet = { minutes ->
                            musicPlayer.setSleepTimerMinutes(minutes)
                            if (minutes > 0) notify(context.getString(R.string.menu_timer_set, minutes))
                            close()
                        },
                    )
                    MenuPage.Main -> when (target) {
                        is TrackMenuTarget.Track -> {
                            val t = target
                            val playable = playableTrack ?: PlayableTrack(title = t.name, artist = t.artist)
                            val isCurrent = playerState.current?.let {
                                it.title.equals(t.name, ignoreCase = true) && it.artist.equals(t.artist, ignoreCase = true)
                            } == true
                            val isLocalFile = com.tuned.app.data.generate.LocalAudioFormats.localReferenceOrNull(playable.playbackUrl) != null
                            val downloadKey = com.tuned.app.data.download.TrackDownloadManager.makeDownloadKey(t.name, t.artist)
                            val isDownloading = activeDownloads[downloadKey]?.let { !it.isFinished } == true
                            val playingNextMessage = stringResource(R.string.menu_playing_next)
                            val addedToQueueMessage = stringResource(R.string.menu_added_queue)
                            val pinAddedMessage = stringResource(R.string.pin_added)
                            val pinRemovedMessage = stringResource(R.string.pin_removed)
                            val copiedMessage = stringResource(R.string.menu_copied)
                            val wontRecommendMessage = stringResource(R.string.menu_wont_recommend)

                            // Which song this menu is for.
                            SongMenuHeader(
                                title = t.name,
                                artist = t.artist,
                                artworkUrl = playable.artworkUrl,
                                genre = resolvedGenre?.takeIf { !resolvingGenre && it.isNotBlank() },
                                onGenre = { genre -> exploreGenre(genre); close() },
                            )
                            StartMixCard {
                                if (onStartMix != null) onStartMix(t.name, t.artist)
                                else startMixViewModel.startMix(t.name, t.artist, playableTrack?.videoId)
                                close()
                            }
                            QuickActionsRow(
                                onJam = { showJamSheet = true },
                                onPlayNext = {
                                    musicPlayer.playNext(playable)
                                    notify(playingNextMessage)
                                    close()
                                },
                                onAddToQueue = {
                                    musicPlayer.addToQueue(playable)
                                    notify(addedToQueueMessage)
                                    close()
                                },
                                onAddToPlaylist = { addToPlaylist(playable); close() },
                            )

                            // Go to
                            MenuGroup(
                                buildList {
                                    for (art in com.tuned.app.util.ArtistHelper.splitArtists(t.artist)) {
                                        add { pos ->
                                            MenuActionRow(Icons.Filled.Person, stringResource(R.string.menu_go_artist, art), position = pos) {
                                                artistAlbumViewModel.openArtist(art)
                                                close()
                                            }
                                        }
                                    }
                                    val album = playable.album
                                    if (!album.isNullOrBlank()) {
                                        add { pos ->
                                            MenuActionRow(Icons.Filled.Album, stringResource(R.string.menu_go_album, album), position = pos) {
                                                artistAlbumViewModel.openAlbum(album, playable.artist.ifBlank { t.artist })
                                                close()
                                            }
                                        }
                                    }
                                },
                            )

                            // Playback
                            MenuGroup(
                                buildList {
                                    if (!isCurrent) {
                                        add { pos ->
                                            MenuActionRow(Icons.Filled.PlayCircle, stringResource(R.string.menu_quick_play), position = pos) {
                                                onPlayInTuned?.invoke() ?: musicPlayer.play(playable, sourceLabel = playbackSourceLabel)
                                                close()
                                            }
                                        }
                                    }
                                    // The file is already on the device; no streaming download.
                                    if (!isLocalFile) add { pos ->
                                        when {
                                            isDownloaded -> MenuActionRow(
                                                Icons.Filled.CheckCircle,
                                                stringResource(R.string.menu_downloaded),
                                                position = pos,
                                                chevron = true,
                                            ) {
                                                downloadViewModel.openDownloads()
                                                close()
                                            }
                                            isDownloading -> MenuActionRow(
                                                Icons.Filled.Download,
                                                stringResource(R.string.menu_downloading),
                                                position = pos,
                                                loading = true,
                                            ) {
                                                downloadViewModel.openDownloads()
                                                close()
                                            }
                                            else -> MenuActionRow(
                                                Icons.Filled.Download,
                                                stringResource(R.string.menu_download),
                                                position = pos,
                                                value = downloadTierForQuality(context, downloadQuality),
                                            ) {
                                                downloadViewModel.download(
                                                    title = t.name,
                                                    artist = t.artist,
                                                    album = playable.album,
                                                    artworkUrl = playable.artworkUrl,
                                                    videoId = playable.videoId,
                                                    durationMs = playable.durationMs,
                                                )
                                                close()
                                            }
                                        }
                                    }
                                    add { pos ->
                                        MenuActionRow(
                                            Icons.Filled.Timer,
                                            stringResource(R.string.menu_sleep_timer),
                                            position = pos,
                                            value = playerState.sleepTimerRemainingMs?.let { sleepTimerLeft(context, it) },
                                            chevron = true,
                                        ) { page = MenuPage.SleepTimer }
                                    }
                                    add { pos ->
                                        Card(
                                            shape = groupShape(pos),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            PlayerCastMenuRow(musicPlayer)
                                        }
                                    }
                                },
                            )

                            // About this song
                            MenuGroup(
                                buildList {
                                    if (playableTrack != null) {
                                        val pin = com.tuned.app.data.library.PinnedItem(
                                            kind = com.tuned.app.data.library.PinKind.SONG,
                                            title = playableTrack.title,
                                            artist = playableTrack.artist,
                                            artworkUrl = playableTrack.artworkUrl,
                                            videoId = playableTrack.videoId,
                                        )
                                        add { pos ->
                                            MenuActionRow(
                                                Icons.Filled.PushPin,
                                                stringResource(if (pinsViewModel.isPinned(pin)) R.string.pin_remove else R.string.pin_add),
                                                position = pos,
                                            ) {
                                                val now = pinsViewModel.toggle(pin)
                                                notify(if (now) pinAddedMessage else pinRemovedMessage)
                                                onDismiss()
                                            }
                                        }
                                    }
                                    add { pos ->
                                        MenuActionRow(Icons.Filled.Info, stringResource(R.string.menu_details_specs), position = pos, chevron = true) {
                                            showDetailsSheet = true
                                        }
                                    }
                                    add { pos ->
                                        MenuActionRow(Icons.Filled.Language, stringResource(R.string.menu_open_lastfm), position = pos) {
                                            openUrl(context, buildLastFmUrl(target))
                                            close()
                                        }
                                    }
                                    if (capabilities.showCopyActions) {
                                        add { pos ->
                                            MenuActionRow(Icons.Filled.ContentCopy, stringResource(R.string.menu_copy_song), position = pos) {
                                                clipboard.setText(AnnotatedString("${t.name} - ${t.artist}"))
                                                notify(copiedMessage)
                                                close()
                                            }
                                        }
                                    }
                                    if (onRefreshArtwork != null) {
                                        add { pos ->
                                            MenuActionRow(Icons.Filled.Refresh, stringResource(R.string.menu_refresh_artwork), position = pos) {
                                                onRefreshArtwork()
                                                close()
                                            }
                                        }
                                    }
                                },
                            )

                            // Taking it away
                            MenuGroup(
                                buildList {
                                    if (onRemoveFromQueue != null) {
                                        add { pos ->
                                            MenuActionRow(Icons.Filled.Delete, stringResource(R.string.menu_remove_from_queue), danger = true, position = pos) {
                                                onRemoveFromQueue()
                                                close()
                                            }
                                        }
                                    }
                                    if (onRemoveFromPlaylist != null) {
                                        add { pos ->
                                            MenuActionRow(Icons.Filled.Delete, stringResource(R.string.menu_remove_from_playlist), danger = true, position = pos) {
                                                onRemoveFromPlaylist()
                                                close()
                                            }
                                        }
                                    }
                                    add { pos ->
                                        MenuActionRow(Icons.Filled.ThumbDown, stringResource(R.string.menu_dont_recommend), danger = true, position = pos) {
                                            exclusionViewModel.exclude(t.name, t.artist)
                                            notify(wontRecommendMessage)
                                            close()
                                        }
                                    }
                                },
                            )
                        }
                        is TrackMenuTarget.Artist -> MenuGroup(
                            listOf(
                                { pos ->
                                    MenuActionRow(Icons.Filled.Person, stringResource(R.string.menu_view_artist), position = pos) {
                                        artistAlbumViewModel.openArtist(target.name)
                                        close()
                                    }
                                },
                                { pos ->
                                    MenuActionRow(Icons.Filled.Language, stringResource(R.string.menu_open_lastfm), position = pos) {
                                        openUrl(context, buildLastFmUrl(target))
                                        close()
                                    }
                                },
                            ),
                        )
                        is TrackMenuTarget.Album -> MenuGroup(
                            buildList {
                                add { pos ->
                                    MenuActionRow(Icons.Filled.Album, stringResource(R.string.menu_view_album), position = pos) {
                                        artistAlbumViewModel.openAlbum(target.name, target.artist)
                                        close()
                                    }
                                }
                                if (target.artist.isNotBlank()) {
                                    add { pos ->
                                        MenuActionRow(Icons.Filled.Person, stringResource(R.string.menu_view_artist_name, target.artist), position = pos) {
                                            artistAlbumViewModel.openArtist(target.artist)
                                            close()
                                        }
                                    }
                                }
                                add { pos ->
                                    MenuActionRow(Icons.Filled.Language, stringResource(R.string.menu_open_lastfm), position = pos) {
                                        openUrl(context, buildLastFmUrl(target))
                                        close()
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

private enum class MenuPage { Main, SleepTimer }

/** A group of rows in Tuned's grouped shape; nothing is drawn when it is empty. */
@Composable
private fun MenuGroup(rows: List<@Composable (GroupPosition) -> Unit>) {
    if (rows.isEmpty()) return
    ExpressiveGroup(rowCount = rows.size) { index, position -> rows[index](position) }
}

/**
 * Which song the menu is for: the cover, the title in bold, the artist, and the
 * genre as a small pill that opens the genre.
 */
@Composable
private fun SongMenuHeader(
    title: String,
    artist: String,
    artworkUrl: String?,
    genre: String?,
    onGenre: (String) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkImage(
            name = title,
            artist = artist,
            embeddedUrl = artworkUrl,
            fallbackIcon = Icons.Filled.MusicNote,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (genre != null) {
            Spacer(Modifier.width(10.dp))
            // A tag, so it stays quieter than the title: small, muted, no accent.
            Surface(
                onClick = { onGenre(genre) },
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                    genre,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
    }
}

/** Sleep timer choices, inside the song menu, in the same grouped style. */
@Composable
private fun SleepTimerPage(
    remainingMs: Long?,
    onBack: () -> Unit,
    onSet: (minutes: Int) -> Unit,
) {
    val context = LocalContext.current
    var custom by rememberSaveable { mutableStateOf(20) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.menu_back))
        }
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.menu_sleep_timer), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (remainingMs != null) {
                Text(sleepTimerLeft(context, remainingMs), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
    val choices = listOf(15, 30, 45, 60)
    ExpressiveGroup(rowCount = choices.size) { index, position ->
        val minutes = choices[index]
        MenuActionRow(
            Icons.Filled.Timer,
            if (minutes == 60) stringResource(R.string.menu_timer_hour) else stringResource(R.string.menu_timer_min, minutes),
            position = position,
        ) { onSet(minutes) }
    }
    // Custom length: a stepper, so no keyboard opens over the sheet.
    Card(
        shape = RoundedCornerShape(GroupOuterRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.menu_timer_custom), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = { custom = (custom - 5).coerceAtLeast(5) }, enabled = custom > 5) {
                Icon(Icons.Filled.Remove, stringResource(R.string.menu_timer_less))
            }
            Text(
                stringResource(R.string.menu_timer_min, custom),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(64.dp),
            )
            IconButton(onClick = { custom = (custom + 5).coerceAtMost(240) }, enabled = custom < 240) {
                Icon(Icons.Filled.Add, stringResource(R.string.menu_timer_more))
            }
            Spacer(Modifier.width(4.dp))
            Surface(
                onClick = { onSet(custom) },
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary,
            ) {
                Text(
                    stringResource(R.string.menu_timer_start),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
    if (remainingMs != null) {
        ExpressiveGroup(rowCount = 1) { _, position ->
            MenuActionRow(Icons.Filled.TimerOff, stringResource(R.string.menu_timer_turn_off), danger = true, position = position) { onSet(0) }
        }
    }
}

/** "23 min left" or "1 h 5 min left". */
private fun sleepTimerLeft(context: Context, remainingMs: Long): String {
    val minutes = ((remainingMs + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
    val text = if (minutes >= 60) context.getString(R.string.menu_timer_h_min, minutes / 60, minutes % 60)
    else context.getString(R.string.menu_timer_min, minutes)
    return context.getString(R.string.menu_timer_left, text)
}

/**
 * "Start Mix with this Song" (§6) — a featured card rather than a plain
 * text row, so it reads as the primary action in the sheet. Colored with
 * the app's live accent (MaterialTheme.colorScheme.primary), which already
 * reflects whichever source is currently driving the theme — a manual
 * preset, Monochrome, wallpaper Dynamic Color, or (when active) the
 * Dynamic Now Playing Theme's extracted artwork palette. There's no
 * separate "which accent source" branch to maintain here: reading
 * colorScheme.primary is inherently correct for all of them since that's
 * exactly the value ThemeRepository recomputes for whichever source is
 * currently active.
 */
@Composable
private fun StartMixCard(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "startMixScale",
    )

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        // 0dp deliberately — see ModeCard/SettingsToggleCard for why a
        // nonzero tonalElevation would blend a second tinted layer on top
        // of containerColor here.
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(22.dp),
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Shuffle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Text(
                stringResource(R.string.menu_start_mix),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The three things people do most, directly below Start Mix (Jam joins when enabled). */
@Composable
private fun QuickActionsRow(
    onJam: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
) {
    // Equal heights, so a label that wraps to two lines keeps the cards even.
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (JamRepository.isEnabled) {
            QuickActionCard(Icons.Filled.Group, stringResource(R.string.menu_quick_jam), Modifier.weight(1f), onJam)
        }
        QuickActionCard(Icons.AutoMirrored.Filled.PlaylistPlay, stringResource(R.string.menu_quick_play_next), Modifier.weight(1f), onPlayNext)
        QuickActionCard(Icons.Filled.QueueMusic, stringResource(R.string.menu_add_queue), Modifier.weight(1f), onAddToQueue)
        QuickActionCard(Icons.Filled.PlaylistAdd, stringResource(R.string.menu_quick_add_playlist), Modifier.weight(1f), onAddToPlaylist)
    }
}


@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberGroupPressScale(interactionSource)
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(22.dp),
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        // Icons pinned to the same top line in every card; the label is centred in the space below,
        // so a one-line label ("Play next") and a two-line one ("Add to queue") share their middle.
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** A rounded, elevated-feeling row (real Card, not a flat clickable Row) —
 *  each menu action is one row in the shared ExpressiveGroup surface (see
 *  the call site), so the whole set of actions reads as one continuous
 *  premium container instead of separate floating rows — same language as
 *  Settings/Generator's grouped lists. An optional [value] sits on the right
 *  (the download tier, the sleep timer's time left), with a chevron when the
 *  row opens something. */
@Composable
private fun MenuActionRow(
    icon: ImageVector,
    label: String,
    danger: Boolean = false,
    position: GroupPosition = GroupPosition.SINGLE,
    value: String? = null,
    chevron: Boolean = false,
    loading: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberGroupPressScale(interactionSource)
    val contentColor = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val badgeColor = if (danger) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    val badgeContentColor = if (danger) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = groupShape(position),
        interactionSource = interactionSource,
        modifier = Modifier.fillMaxWidth().graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(badgeColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = badgeContentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            when {
                loading -> ExpressiveInlineLoadingIndicator(
                    modifier = Modifier.padding(start = 8.dp),
                    size = 14.dp,
                    strokeWidth = 2.dp,
                )
                value != null -> Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            if (chevron) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp).size(14.dp),
                )
            }
        }
    }
}

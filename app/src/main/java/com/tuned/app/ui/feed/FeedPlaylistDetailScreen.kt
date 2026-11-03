package com.tuned.app.ui.feed

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuned.app.R
import com.tuned.app.data.music.YouTubeMusicTrack
import com.tuned.app.data.music.YouTubePlaylistResult
import com.tuned.app.playback.PlayableTrack
import com.tuned.app.ui.common.ArtworkImage
import com.tuned.app.ui.common.TrackContextMenuSheet
import com.tuned.app.ui.common.TrackMenuCapabilities
import com.tuned.app.ui.common.TrackMenuTarget
import com.tuned.app.ui.common.TrackMiniTrayData
import com.tuned.app.ui.common.TrackMiniTraySheet
import com.tuned.app.ui.common.safeDrawingBottomPadding
import com.tuned.app.ui.common.DetailAccentWash
import com.tuned.app.ui.common.DetailMenuItem
import com.tuned.app.ui.common.DetailPlayPill
import com.tuned.app.ui.common.DetailPlayPillWidth
import com.tuned.app.ui.common.DetailSideButton
import com.tuned.app.ui.common.DetailToolbar
import com.tuned.app.ui.common.DetailTrackRow
import com.tuned.app.ui.common.LocalUndoController
import com.tuned.app.ui.common.SkeletonTrackRow
import com.tuned.app.ui.common.TrackRowDensity
import com.tuned.app.ui.common.TunedIcons
import com.tuned.app.ui.common.detailCoverSide
import com.tuned.app.ui.common.detailHeaderTop
import com.tuned.app.ui.common.skeletonAlpha
import com.tuned.app.ui.player.LocalMiniPlayerScrollClearance
import com.tuned.app.ui.player.LocalMusicPlayer
import com.tuned.app.ui.theme.AppShape
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.Keyline
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.tunedColors

/**
 * A YouTube Music playlist, laid out like the album page (Apple Music's order, Tuned's parts):
 * cover, title, owner, song count, Shuffle · Play · Add, the songs with their covers, then the
 * total length.
 */
@Composable
fun FeedPlaylistDetailScreen(
    playlistId: String,
    onBack: () -> Unit,
    viewModel: FeedPlaylistDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = MaterialTheme.tunedColors
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val undo = LocalUndoController.current
    val density = LocalDensity.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    LaunchedEffect(playlistId) {
        viewModel.load(playlistId)
    }

    val listState = rememberLazyListState()
    val success = state as? FeedPlaylistDetailUiState.Success
    val playlist = success?.playlist
    val title = playlist?.title?.takeIf(String::isNotBlank) ?: stringResource(R.string.fpd_playlist)

    var titleBottomPx by remember { mutableIntStateOf(Int.MAX_VALUE) }
    val toolbarPx = with(density) { (WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + Sizes.touch).roundToPx() }
    val scrolledPastTitle by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > titleBottomPx - toolbarPx }
    }

    // Synthetic collections (liked/recent) have no public YouTube playlist
    // to link to; a raw-concatenated id could also inject extra parameters.
    val isSynthetic = playlistId == "yt_liked" || playlistId == "yt_recent"
    val shareLink = "https://music.youtube.com/playlist?list=${android.net.Uri.encode(playlistId)}"
    val sharePlaylist: () -> Unit = {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, shareLink)
        }
        context.startActivity(Intent.createChooser(send, null))
    }
    val copiedMessage = stringResource(R.string.art_link_copied)
    val savedMessage = stringResource(R.string.alb_saved)
    LaunchedEffect(success?.saveError) { success?.saveError?.let { undo?.show(it) } }
    // "Saved" is shown only when the state actually reaches savedToLibrary —
    // it used to fire unconditionally at tap time, even when the ViewModel
    // rejected or failed the save (and then contradicted the error message).
    LaunchedEffect(success?.savedToLibrary) { if (success?.savedToLibrary == true) undo?.show(savedMessage) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        DetailAccentWash(listState)
        when (val current = state) {
            FeedPlaylistDetailUiState.Loading -> PlaylistLoading(title = title, screenWidth = screenWidth)
            is FeedPlaylistDetailUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(Space.xl)) {
                        Text(text = current.message, style = AppType.body, color = colors.text2, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(Space.lg))
                        Button(onClick = { viewModel.load(playlistId) }) { Text(stringResource(R.string.fpd_retry)) }
                    }
                }
            }
            is FeedPlaylistDetailUiState.Success -> PlaylistContent(
                playlist = current.playlist,
                listState = listState,
                screenWidth = screenWidth,
                isLoadingMore = current.isLoadingMore,
                incomplete = current.loadError != null,
                saved = current.savedToLibrary,
                saving = current.isSaving,
                onRetry = { viewModel.load(playlistId, force = true) },
                onPlay = viewModel::playFrom,
                onShuffle = viewModel::shuffle,
                onSave = { viewModel.saveToLibrary() },
                onTitleBottom = { titleBottomPx = it },
            )
        }

        DetailToolbar(
            title = title,
            showTitle = scrolledPastTitle,
            onBack = onBack,
            onShare = if (isSynthetic) null else sharePlaylist,
            shareLabel = stringResource(R.string.fpd_share),
            menu = { dismiss ->
                DetailMenuItem(Icons.Rounded.Shuffle, stringResource(R.string.alb_shuffle_cd)) { dismiss(); viewModel.shuffle() }
                if (!isSynthetic) {
                    DetailMenuItem(TunedIcons.Copy, stringResource(R.string.art_copy_link)) {
                        dismiss()
                        clipboard.setText(AnnotatedString(shareLink))
                        undo?.show(copiedMessage)
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(10f),
        )
    }
}

@Composable
private fun PlaylistContent(
    playlist: YouTubePlaylistResult,
    listState: LazyListState,
    screenWidth: Dp,
    isLoadingMore: Boolean,
    incomplete: Boolean,
    saved: Boolean,
    saving: Boolean,
    onRetry: () -> Unit,
    onPlay: (Int) -> Unit,
    onShuffle: () -> Unit,
    onSave: () -> Unit,
    onTitleBottom: (Int) -> Unit,
) {
    val colors = MaterialTheme.tunedColors
    val musicPlayer = LocalMusicPlayer.current
    val playback by musicPlayer.chromeState.collectAsStateWithLifecycle()
    val sourceLabel = playlist.title.ifBlank { "Feed mix" }
    val playlistPlaying = playback.isPlaying && playback.sourceLabel == sourceLabel
    var menuTrack by remember { mutableStateOf<YouTubeMusicTrack?>(null) }
    var trayIndex by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = Space.xl + LocalMiniPlayerScrollClearance.current + safeDrawingBottomPadding(),
        ),
    ) {
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
                        .shadow(Space.md, AppShape.md, clip = false)
                        .clip(AppShape.md),
                ) {
                    ArtworkImage(
                        name = playlist.title,
                        artist = playlist.author.orEmpty(),
                        embeddedUrl = playlist.artworkUrl ?: playlist.tracks.firstOrNull()?.artworkUrl,
                        fallbackIcon = if (playlist.id == "yt_liked") Icons.Filled.Favorite else Icons.Filled.MusicNote,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Spacer(Modifier.height(Space.xl))
                Text(
                    text = playlist.title.ifBlank { stringResource(R.string.fpd_playlist) },
                    style = AppType.titleL,
                    color = colors.text1,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(horizontal = Keyline.detail + Space.xs)
                        .onGloballyPositioned { onTitleBottom((it.positionInParent().y + it.size.height).toInt()) },
                )
                playlist.author?.takeIf(String::isNotBlank)?.let { author ->
                    Spacer(Modifier.height(Space.xxs))
                    Text(text = author, style = AppType.title, color = colors.accent, textAlign = TextAlign.Center, maxLines = 1)
                }
                val count = playlist.tracks.size.takeIf { it > 0 } ?: playlist.trackCount
                if (count > 0) {
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        text = pluralStringResource(R.plurals.alb_song_count, count, count),
                        style = AppType.caption.copy(fontFeatureSettings = null),
                        color = colors.text2,
                    )
                }
                Spacer(Modifier.height(Space.lg + Space.xs))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Space.md + Space.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DetailSideButton(Icons.Rounded.Shuffle, stringResource(R.string.alb_shuffle_cd), onShuffle, ink = colors.text1)
                    DetailPlayPill(
                        playing = playlistPlaying,
                        enabled = playlist.tracks.isNotEmpty(),
                        onClick = { if (playlistPlaying) musicPlayer.togglePlayPause() else onPlay(0) },
                    )
                    DetailSideButton(
                        icon = if (saved) Icons.Rounded.Check else Icons.Rounded.Add,
                        contentDescription = stringResource(if (saved) R.string.alb_saved_cd else R.string.alb_save_cd),
                        onClick = { if (!saved && !saving && playlist.tracks.isNotEmpty()) onSave() },
                        ink = colors.text1,
                    )
                }
                if (isLoadingMore || incomplete) {
                    Spacer(Modifier.height(Space.md))
                    Text(
                        text = stringResource(if (incomplete) R.string.fpd_incomplete else R.string.fpd_loading_more),
                        style = AppType.bodyS,
                        color = colors.text2,
                        modifier = Modifier
                            .clip(AppShape.xs)
                            .then(
                                if (incomplete) {
                                    Modifier
                                        .minimumInteractiveComponentSize()
                                        .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onRetry)
                                } else Modifier,
                            ),
                    )
                }
                Spacer(Modifier.height(Space.lg))
            }
        }

        itemsIndexed(
            items = playlist.tracks,
            key = { index, track -> "${track.videoId}:$index" },
            contentType = { _, _ -> "playlist_track" },
        ) { index, track ->
            DetailTrackRow(
                title = track.title,
                subtitle = track.artist,
                artworkName = track.title,
                artworkArtist = track.artist,
                artworkUrl = track.artworkUrl,
                playing = playback.isPlaying && playback.current?.let { it.videoId == track.videoId || it.title.equals(track.title, ignoreCase = true) } == true,
                onClick = { onPlay(index) },
                onLongClick = { trayIndex = index },
                onMore = { menuTrack = track },
                explicit = track.explicit,
            )
        }

        if (playlist.tracks.isNotEmpty()) {
            item(key = "details") {
                val seconds = playlist.tracks.sumOf { it.durationSeconds ?: 0 }
                val songs = pluralStringResource(R.plurals.alb_song_count, playlist.tracks.size, playlist.tracks.size)
                Column(Modifier.padding(start = Keyline.detail, end = Keyline.detail, top = Space.lg + Space.xs)) {
                    Text(text = songs, style = AppType.bodyS.copy(fontFeatureSettings = null), color = colors.text2)
                    if (seconds > 0) {
                        Text(text = playlistLength(seconds), style = AppType.bodyS.copy(fontFeatureSettings = null), color = colors.text2)
                    }
                }
            }
        }
    }

    menuTrack?.let { track ->
        TrackContextMenuSheet(
            target = TrackMenuTarget.Track(track.title, track.artist, ""),
            capabilities = TrackMenuCapabilities(showCopyActions = true, showDeleteScrobble = false),
            playableTrack = track.toPlayable(),
            playbackSourceLabel = sourceLabel,
            onDismiss = { menuTrack = null },
        )
    }
    trayIndex?.let { index ->
        playlist.tracks.getOrNull(index)?.let { track ->
            TrackMiniTraySheet(
                data = TrackMiniTrayData(
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    artworkUrl = track.artworkUrl,
                    videoId = track.videoId.takeIf(String::isNotBlank),
                    sourceLabel = sourceLabel,
                    onPlay = { onPlay(index) },
                ),
                onDismiss = { trayIndex = null },
            )
        }
    }
}

/** The page's shape while it loads: cover, the title when known, controls, rows. */
@Composable
private fun PlaylistLoading(title: String, screenWidth: Dp) {
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
                Box(Modifier.size(detailCoverSide(screenWidth)).alpha(pulse).clip(AppShape.md).background(colors.surface2))
                Spacer(Modifier.height(Space.xl))
                Text(text = title, style = AppType.titleL, color = colors.text1, textAlign = TextAlign.Center, maxLines = 2)
                Spacer(Modifier.height(Space.lg + Space.xs + Space.xl))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.md + Space.xxs), modifier = Modifier.alpha(pulse)) {
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

private fun YouTubeMusicTrack.toPlayable() = PlayableTrack(
    title = title,
    artist = artist,
    album = album,
    artworkUrl = artworkUrl,
    videoId = videoId.takeIf(String::isNotBlank),
)

/** "1 hr 12 min" or "38 minutes", like the album footer. */
@Composable
private fun playlistLength(seconds: Int): String {
    val minutes = (seconds + 30) / 60
    return if (minutes >= 60) {
        stringResource(R.string.fpd_length_hours, minutes / 60, minutes % 60)
    } else {
        pluralStringResource(R.plurals.fpd_minutes, minutes, minutes)
    }
}

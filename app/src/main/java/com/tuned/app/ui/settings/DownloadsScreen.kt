@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.tuned.app.ui.settings

import com.tuned.app.R
import com.tuned.app.ui.common.PredictiveBackScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuned.app.data.download.DownloadProgress
import com.tuned.app.data.local.db.DownloadedTrackEntity
import com.tuned.app.ui.common.ArtworkImage
import com.tuned.app.ui.common.ExpressiveHeader
import com.tuned.app.ui.common.GroupPosition
import com.tuned.app.ui.common.adaptiveContentWidth
import com.tuned.app.ui.common.groupPositionFor
import com.tuned.app.ui.common.groupShape
import com.tuned.app.ui.common.safeDrawingBottomPadding
import com.tuned.app.ui.player.LocalMiniPlayerScrollClearance
import com.tuned.app.ui.player.LocalMusicPlayer
import com.tuned.app.ui.player.PlayingWaveBars
import com.tuned.app.ui.shell.FloatingNavDefaults
import com.tuned.app.ui.theme.ArtworkShape
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.AnimatedContent
import com.tuned.app.ui.common.detailHeaderTop
import com.tuned.app.ui.theme.iconEdgeInset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.AppShape
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.Keyline
import com.tuned.app.ui.theme.tunedColors
import com.tuned.app.ui.common.DetailPlayPill
import com.tuned.app.ui.common.DetailPlayPillWidth
import com.tuned.app.ui.common.detailCoverSide
import com.tuned.app.ui.common.AppIconButton
import com.tuned.app.ui.common.TunedIcons
import androidx.compose.foundation.layout.heightIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** "Oct 25" this year, "Oct 25, 2025" before it. */
private fun formatDate(millis: Long): String {
    val cal = java.util.Calendar.getInstance()
    val thisYear = cal.get(java.util.Calendar.YEAR)
    cal.timeInMillis = millis
    val pattern = if (cal.get(java.util.Calendar.YEAR) == thisYear) "MMM d" else "MMM d, yyyy"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
}

/** "1 song", "3 songs". */
// Song counts use the pl_song_count plurals resource (this helper used
// to hand-roll English pluralization: "1 song" / "N songs").

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes.toDouble() / (1024 * 1024)
    return if (mb >= 1000) "%.1f GB".format(mb / 1024) else "%.1f MB".format(mb)
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

enum class DownloadTab(val label: String) {
    SONGS("Songs"),
    ARTISTS("Artists"),
    ALBUMS("Albums"),
}

// Sort option definitions were moved to DownloadsViewModel.kt so the
// persisted sort state and its stable ids live together. Kept here as
// comments to preserve the original code for upstream review.
// enum class SongSortOption(val label: String) {
//     RECENT("Recently Added"),
//     OLDEST("Oldest First"),
//     TITLE_AZ("Title (A-Z)"),
//     ARTIST_AZ("Artist (A-Z)"),
//     SIZE_DESC("File Size"),
// }
//
// enum class ArtistSortOption(val label: String) {
//     NAME_AZ("Name (A-Z)"),
//     MOST_SONGS("Most Songs"),
//     STORAGE_SIZE("Storage Size"),
// }
//
// enum class AlbumSortOption(val label: String) {
//     TITLE_AZ("Title (A-Z)"),
//     ARTIST_AZ("Artist (A-Z)"),
//     MOST_TRACKS("Most Tracks"),
//     RECENT("Recently Added"),
// }

/** OpenDocumentTree that also requests a persistable grant, so the chosen
 *  download folder survives reboots (takePersistableUriPermission in the
 *  result callback completes the handshake). */
private class PersistableOpenDocumentTree : ActivityResultContracts.OpenDocumentTree() {
    override fun createIntent(context: android.content.Context, input: android.net.Uri?): android.content.Intent =
        super.createIntent(context, input).addFlags(
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
        )
}

sealed interface DownloadSubView {
    data object Root : DownloadSubView
    data class ArtistDetail(val artistName: String) : DownloadSubView
    data class AlbumDetail(val albumTitle: String, val artistName: String) : DownloadSubView
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onBack: (() -> Unit)? = null,
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val tracks by viewModel.downloadedTracks.collectAsStateWithLifecycle()
    val artists by viewModel.downloadedArtists.collectAsStateWithLifecycle()
    val albums by viewModel.downloadedAlbums.collectAsStateWithLifecycle()
    val totalBytes by viewModel.totalBytes.collectAsStateWithLifecycle()
    val activeDownloadsMap by viewModel.activeDownloads.collectAsStateWithLifecycle()
    val downloadLyrics by viewModel.downloadLyrics.collectAsStateWithLifecycle()
    val songSort by viewModel.songSort.collectAsStateWithLifecycle()
    val artistSort by viewModel.artistSort.collectAsStateWithLifecycle()
    val albumSort by viewModel.albumSort.collectAsStateWithLifecycle()
    val downloadFolder by viewModel.downloadFolder.collectAsStateWithLifecycle()
    val downloadStructure by viewModel.downloadStructure.collectAsStateWithLifecycle()
    val useAlbumArtistForFolders by viewModel.useAlbumArtistForFolders.collectAsStateWithLifecycle()
    val primaryArtistOnly by viewModel.primaryArtistOnly.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val activeDownloads = activeDownloadsMap.values.filter { !it.isFinished && it.error == null }

    val haptic = LocalHapticFeedback.current
    val musicPlayer = LocalMusicPlayer.current
    val playbackState by musicPlayer.chromeState.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(DownloadTab.SONGS) }
    var subViewStack by remember { mutableStateOf(listOf<DownloadSubView>(DownloadSubView.Root)) }
    val currentSubView = subViewStack.last()

    var searchQuery by remember { mutableStateOf("") }
    // Replaced by the DataStore-backed ViewModel state above so these values
    // survive navigation. Original screen-local state kept for upstream review:
    // var songSort by remember { mutableStateOf(SongSortOption.RECENT) }
    // var artistSort by remember { mutableStateOf(ArtistSortOption.NAME_AZ) }
    // var albumSort by remember { mutableStateOf(AlbumSortOption.TITLE_AZ) }
    var filterLosslessOnly by remember { mutableStateOf(false) }
    var filterLyricsOnly by remember { mutableStateOf(false) }

    var showClearAllConfirm by remember { mutableStateOf(false) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }
    var trackToDelete by remember { mutableStateOf<DownloadedTrackEntity?>(null) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showOrganizationDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val downloadTreeUri by viewModel.downloadTreeUri.collectAsStateWithLifecycle()
    val hasCustomLocation = downloadTreeUri.isNotBlank()
    // Human-readable custom label; null when on the default location or the
    // grant was lost (SD card removed / permission revoked).
    val customLocationLabel = remember(context, downloadTreeUri) {
        com.tuned.app.data.download.SafTreeFiles.describeLocation(context, downloadTreeUri)
    }
    val customLocationStale = hasCustomLocation && customLocationLabel == null
    val storageNoun = customLocationLabel ?: "Music/$downloadFolder"
    val locationPicker =
        rememberLauncherForActivityResult(PersistableOpenDocumentTree()) { uri: android.net.Uri? ->
            if (uri != null) {
                if (com.tuned.app.data.download.SafTreeFiles.takePersistedAccess(context, uri)) {
                    viewModel.setDownloadTreeUri(uri.toString())
                }
            }
        }

    fun navigateTo(subView: DownloadSubView) {
        subViewStack = subViewStack + subView
    }
    val pageMotion = com.tuned.app.ui.theme.rememberMotion()

    fun popSubView(): Boolean {
        return if (subViewStack.size > 1) {
            subViewStack = subViewStack.dropLast(1)
            true
        } else {
            false
        }
    }

    val totalSizeText = formatBytes(totalBytes ?: 0L)

    PredictiveBackScreen(
        enabled = subViewStack.size > 1,
        onBack = { popSubView() },
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .adaptiveContentWidth(maxWidth = 860.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
        // Artist and album pages push like any other screen (NavGraph's shared X axis): in from
        // the end, the old page drifting back and fading; back reverses it. The header goes with
        // its page.
        AnimatedContent(
            targetState = subViewStack,
            transitionSpec = {
                val forward = targetState.size > initialState.size
                if (forward) {
                    (slideInHorizontally(pageMotion.base()) { it * 3 / 10 } + fadeIn(pageMotion.base()))
                        .togetherWith(slideOutHorizontally(pageMotion.base()) { -it / 10 } + fadeOut(pageMotion.fast()))
                } else {
                    (slideInHorizontally(pageMotion.base()) { -it / 10 } + fadeIn(pageMotion.base()))
                        .togetherWith(slideOutHorizontally(pageMotion.base()) { it * 3 / 10 } + fadeOut(pageMotion.fast()))
                }
            },
            contentKey = { it.last() },
            label = "downloadsPage",
            modifier = Modifier.fillMaxSize(),
        ) { stack ->
        val currentSubView = stack.last()
        Column(Modifier.fillMaxSize()) {
            // Header configuration depending on current view depth
            val headerBack: (() -> Unit)? = when {
                subViewStack.size > 1 -> { { popSubView() } }
                onBack != null -> onBack
                else -> null
            }

            val headerTitle = when (currentSubView) {
                is DownloadSubView.Root -> "Downloads"
                // The page below names the artist or album under its cover, as the album page does.
                is DownloadSubView.ArtistDetail -> ""
                is DownloadSubView.AlbumDetail -> ""
            }

            if (currentSubView is DownloadSubView.Root) ExpressiveHeader(
                title = headerTitle,
                onBack = headerBack,
                actions = {
                    if (activeDownloads.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.cancelAllDownloads()
                            },
                            colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp),
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(com.tuned.app.R.string.dl_cancel_all, activeDownloads.size), style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    Box {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showOptionsMenu = true
                            },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(com.tuned.app.R.string.dl_options_cd))
                        }
                        DropdownMenu(shape = com.tuned.app.ui.theme.MenuShape, containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh, 
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false },
                        ) {
                            if (activeDownloads.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(com.tuned.app.R.string.dl_cancel_all_confirm, activeDownloads.size), color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showOptionsMenu = false
                                        viewModel.cancelAllDownloads()
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(stringResource(com.tuned.app.R.string.dl_lyrics_row))
                                        Switch(
                                            checked = downloadLyrics,
                                            onCheckedChange = { viewModel.setDownloadLyrics(it) },
                                        )
                                    }
                                },
                                leadingIcon = { Icon(Icons.Filled.FormatQuote, contentDescription = null) },
                                onClick = { viewModel.setDownloadLyrics(!downloadLyrics) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(com.tuned.app.R.string.dl_open_manager)) },
                                leadingIcon = { Icon(Icons.Filled.FolderOpen, contentDescription = null) },
                                onClick = {
                                    showOptionsMenu = false
                                    viewModel.openInFileManager()
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(stringResource(com.tuned.app.R.string.dl_folder))
                                        Text(
                                            "Music/$downloadFolder",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                },
                                leadingIcon = { Icon(Icons.Filled.FolderOpen, contentDescription = null) },
                                onClick = {
                                    showOptionsMenu = false
                                    showFolderDialog = true
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(stringResource(com.tuned.app.R.string.dl_location))
                                        Text(
                                            customLocationLabel
                                                ?: if (customLocationStale) {
                                                    stringResource(com.tuned.app.R.string.dl_location_unavailable_short)
                                                } else {
                                                    stringResource(
                                                        com.tuned.app.R.string.dl_location_default,
                                                        downloadFolder,
                                                    )
                                                },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (customLocationStale) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                        )
                                    }
                                },
                                leadingIcon = { Icon(Icons.Filled.Folder, contentDescription = null) },
                                onClick = {
                                    showOptionsMenu = false
                                    showLocationDialog = true
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(stringResource(com.tuned.app.R.string.dl_org))
                                        Text(
                                            if (downloadStructure == com.tuned.app.data.local.DownloadFolderStructure.FLAT) {
                                                stringResource(
                                                    com.tuned.app.R.string.dl_menu_flat,
                                                    downloadFolder,
                                                )
                                            } else {
                                                stringResource(
                                                    com.tuned.app.R.string.dl_menu_structured,
                                                    stringResource(downloadStructure.shortLabelRes),
                                                    downloadFolder,
                                                )
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                },
                                leadingIcon = { Icon(Icons.Filled.Folder, contentDescription = null) },
                                onClick = {
                                    showOptionsMenu = false
                                    showOrganizationDialog = true
                                },
                            )
                            if (tracks.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(com.tuned.app.R.string.dl_clear_history_only)) },
                                    leadingIcon = { Icon(Icons.Filled.DeleteSweep, contentDescription = null) },
                                    onClick = {
                                        showOptionsMenu = false
                                        showClearHistoryConfirm = true
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(com.tuned.app.R.string.dl_delete_all_files), color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showOptionsMenu = false
                                        showClearAllConfirm = true
                                    },
                                )
                            }
                        }
                    }
                },
            )

            // Offline Mode Banner
            if (!isOnline) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudOff,
                            contentDescription = stringResource(com.tuned.app.R.string.dl_offline_cd),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(com.tuned.app.R.string.dl_offline_banner),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }

            // Route content depending on subViewStack depth
            when (currentSubView) {
                is DownloadSubView.Root -> {
                    if (tracks.isEmpty() && activeDownloads.isEmpty()) {
                        EmptyDownloadsState()
                    } else {
                        // Segmented tabs: Songs | Artists | Albums
                        DownloadTabBar(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it },
                            songCount = tracks.size,
                            artistCount = artists.size,
                            albumCount = albums.size,
                        )

                        // Search and filter controls
                        DownloadFilterControls(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            selectedTab = selectedTab,
                            songSort = songSort,
                            // Previously: onSongSortChange = { songSort = it },
                            onSongSortChange = viewModel::setSongSort,
                            artistSort = artistSort,
                            // Previously: onArtistSortChange = { artistSort = it },
                            onArtistSortChange = viewModel::setArtistSort,
                            albumSort = albumSort,
                            // Previously: onAlbumSortChange = { albumSort = it },
                            onAlbumSortChange = viewModel::setAlbumSort,
                            filterLossless = filterLosslessOnly,
                            onToggleLossless = { filterLosslessOnly = !filterLosslessOnly },
                            filterLyrics = filterLyricsOnly,
                            onToggleLyrics = { filterLyricsOnly = !filterLyricsOnly },
                        )

                        // Tabs swap with the dock's fade-through (MainShell's "tabs" transition).
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                (fadeIn(pageMotion.base()) + androidx.compose.animation.slideInVertically(pageMotion.base()) { it / 100 })
                                    .togetherWith(fadeOut(pageMotion.fast()))
                            },
                            label = "downloadsTab",
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        ) { selectedTab ->
                        when (selectedTab) {
                            DownloadTab.SONGS -> {
                                val filteredTracks = remember(tracks, searchQuery, songSort, filterLosslessOnly, filterLyricsOnly) {
                                    tracks
                                        .filter {
                                            if (searchQuery.isBlank()) true
                                            else it.title.contains(searchQuery, ignoreCase = true) ||
                                                it.artist.contains(searchQuery, ignoreCase = true) ||
                                                it.album.contains(searchQuery, ignoreCase = true)
                                        }
                                        .filter { if (filterLosslessOnly) it.isLossless else true }
                                        .filter { if (filterLyricsOnly) it.hasLyrics else true }
                                        .let { list ->
                                            when (songSort) {
                                                SongSortOption.RECENT -> list.sortedByDescending { it.downloadedAtMillis }
                                                SongSortOption.OLDEST -> list.sortedBy { it.downloadedAtMillis }
                                                SongSortOption.TITLE_AZ -> list.sortedBy { it.title.lowercase() }
                                                SongSortOption.ARTIST_AZ -> list.sortedBy { it.artist.lowercase() }
                                                SongSortOption.SIZE_DESC -> list.sortedByDescending { it.fileSizeBytes }
                                            }
                                        }
                                }

                                DownloadedSongsList(
                                    tracks = filteredTracks,
                                    activeDownloads = activeDownloads,
                                    playbackState = playbackState,
                                    totalSizeText = totalSizeText,
                                    downloadFolder = downloadFolder,
                                    downloadLocationLabel = customLocationLabel,
                                    downloadStructure = downloadStructure,
                                    onPlayTrack = { track -> viewModel.playTrack(track, filteredTracks) },
                                    onPlayNext = { viewModel.playNext(it) },
                                    onAddToQueue = { viewModel.addToQueue(it) },
                                    onPlayAll = { shuffled -> viewModel.playTracks(filteredTracks, startShuffled = shuffled) },
                                    onSelectArtist = { navigateTo(DownloadSubView.ArtistDetail(it)) },
                                    onSelectAlbum = { alb, art -> navigateTo(DownloadSubView.AlbumDetail(alb, art)) },
                                    onDeleteTrack = { trackToDelete = it },
                                    onCancelDownload = { viewModel.cancelDownload(it) },
                                    onOpenFileManager = { viewModel.openInFileManager() },
                                    onChangeFolder = { showFolderDialog = true },
                                )
                            }
                            DownloadTab.ARTISTS -> {
                                val filteredArtists = remember(artists, searchQuery, artistSort) {
                                    artists
                                        .filter {
                                            if (searchQuery.isBlank()) true
                                            else it.name.contains(searchQuery, ignoreCase = true)
                                        }
                                        .let { list ->
                                            when (artistSort) {
                                                ArtistSortOption.NAME_AZ -> list.sortedBy { it.name.lowercase() }
                                                ArtistSortOption.MOST_SONGS -> list.sortedByDescending { it.tracks.size }
                                                ArtistSortOption.STORAGE_SIZE -> list.sortedByDescending { it.totalSizeBytes }
                                            }
                                        }
                                }

                                DownloadedArtistsList(
                                    artists = filteredArtists,
                                    onSelectArtist = { navigateTo(DownloadSubView.ArtistDetail(it.name)) },
                                    onPlayAll = { viewModel.playAll(startShuffled = false) },
                                    onShuffleAll = { viewModel.playAll(startShuffled = true) },
                                )
                            }
                            DownloadTab.ALBUMS -> {
                                val filteredAlbums = remember(albums, searchQuery, albumSort) {
                                    albums
                                        .filter {
                                            if (searchQuery.isBlank()) true
                                            else it.title.contains(searchQuery, ignoreCase = true) ||
                                                it.artist.contains(searchQuery, ignoreCase = true)
                                        }
                                        .let { list ->
                                            when (albumSort) {
                                                AlbumSortOption.TITLE_AZ -> list.sortedBy { it.title.lowercase() }
                                                AlbumSortOption.ARTIST_AZ -> list.sortedBy { it.artist.lowercase() }
                                                AlbumSortOption.MOST_TRACKS -> list.sortedByDescending { it.tracks.size }
                                                AlbumSortOption.RECENT -> list.sortedByDescending { it.tracks.maxOfOrNull { t -> t.downloadedAtMillis } ?: 0L }
                                            }
                                        }
                                }

                                DownloadedAlbumsList(
                                    albums = filteredAlbums,
                                    onSelectAlbum = { navigateTo(DownloadSubView.AlbumDetail(it.title, it.artist)) },
                                    onPlayAll = { viewModel.playAll(startShuffled = false) },
                                    onShuffleAll = { viewModel.playAll(startShuffled = true) },
                                )
                            }
                        }
                        }
                    }
                }
                is DownloadSubView.ArtistDetail -> {
                    val artistTracks = remember(tracks, currentSubView.artistName) {
                        tracks.filter {
                            it.artist.equals(currentSubView.artistName, ignoreCase = true) ||
                                it.artist.contains(currentSubView.artistName, ignoreCase = true)
                        }.sortedByDescending { it.downloadedAtMillis }
                    }
                    val artistAlbums = remember(albums, currentSubView.artistName) {
                        albums.filter {
                            it.artist.equals(currentSubView.artistName, ignoreCase = true) ||
                                it.artist.contains(currentSubView.artistName, ignoreCase = true) ||
                                currentSubView.artistName.contains(it.artist, ignoreCase = true)
                        }
                    }

                    if (artistTracks.isEmpty() && tracks.isNotEmpty()) {
                        LaunchedEffect(Unit) {
                            popSubView()
                        }
                    }

                    ArtistDetailView(
                        artistName = currentSubView.artistName,
                        tracks = artistTracks,
                        albums = artistAlbums,
                        playbackState = playbackState,
                        onPlayTrack = { track -> viewModel.playTrack(track, artistTracks) },
                        onPlayNext = { viewModel.playNext(it) },
                        onAddToQueue = { viewModel.addToQueue(it) },
                        onPlayAll = { shuffled -> viewModel.playTracks(artistTracks, startShuffled = shuffled) },
                        onSelectArtist = { navigateTo(DownloadSubView.ArtistDetail(it)) },
                        onSelectAlbum = { alb -> navigateTo(DownloadSubView.AlbumDetail(alb, currentSubView.artistName)) },
                        onDeleteTrack = { trackToDelete = it },
                        onBack = { popSubView() },
                    )
                }
                is DownloadSubView.AlbumDetail -> {
                    val albumTracks = remember(tracks, currentSubView.albumTitle, currentSubView.artistName) {
                        tracks.filter {
                            it.album.trim().equals(currentSubView.albumTitle.trim(), ignoreCase = true) &&
                                (currentSubView.artistName.isBlank() ||
                                    it.artist.equals(currentSubView.artistName, ignoreCase = true) ||
                                    it.artist.contains(currentSubView.artistName, ignoreCase = true) ||
                                    currentSubView.artistName.contains(it.artist, ignoreCase = true))
                        }
                    }

                    if (albumTracks.isEmpty() && tracks.isNotEmpty()) {
                        LaunchedEffect(Unit) {
                            popSubView()
                        }
                    }

                    AlbumDetailView(
                        albumTitle = currentSubView.albumTitle,
                        artistName = currentSubView.artistName,
                        tracks = albumTracks,
                        playbackState = playbackState,
                        onPlayTrack = { track -> viewModel.playTrack(track, albumTracks) },
                        onPlayNext = { viewModel.playNext(it) },
                        onAddToQueue = { viewModel.addToQueue(it) },
                        onPlayAll = { shuffled -> viewModel.playTracks(albumTracks, startShuffled = shuffled) },
                        onSelectArtist = { navigateTo(DownloadSubView.ArtistDetail(it)) },
                        onSelectAlbum = { alb, art -> navigateTo(DownloadSubView.AlbumDetail(alb, art)) },
                        onDeleteTrack = { trackToDelete = it },
                        onBack = { popSubView() },
                    )
                }
            }
                }
        }
}
    }

    // Delete single track dialog
    trackToDelete?.let { track ->
        AlertDialog(
            onDismissRequest = { trackToDelete = null },
            title = { Text(stringResource(com.tuned.app.R.string.dl_delete_title, track.title)) },
            text = {
                Text(
                    stringResource(com.tuned.app.R.string.dl_delete_text),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTrack(track)
                        trackToDelete = null
                    },
                ) {
                    Text(stringResource(com.tuned.app.R.string.dl_delete_file), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            viewModel.deleteHistoryRecordOnly(track)
                            trackToDelete = null
                        },
                    ) {
                        Text(stringResource(com.tuned.app.R.string.dl_history_only))
                    }
                    TextButton(onClick = { trackToDelete = null }) { Text(stringResource(com.tuned.app.R.string.common_cancel)) }
                }
            },
        )
    }

    // Clear history only confirm
    if (showClearHistoryConfirm) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirm = false },
            title = { Text(stringResource(com.tuned.app.R.string.dl_clear_history_title)) },
            text = { Text(stringResource(com.tuned.app.R.string.dl_clear_history, storageNoun)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistoryOnly()
                        showClearHistoryConfirm = false
                    },
                ) {
                    Text(stringResource(com.tuned.app.R.string.dl_clear_history_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryConfirm = false }) { Text(stringResource(com.tuned.app.R.string.common_cancel)) }
            },
        )
    }

    // Clear all files confirm
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text(stringResource(com.tuned.app.R.string.dl_delete_all_title)) },
            text = { Text(stringResource(com.tuned.app.R.string.dl_delete_all, tracks.size, storageNoun, totalSizeText)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAll()
                        showClearAllConfirm = false
                    },
                ) {
                    Text(stringResource(com.tuned.app.R.string.dl_delete_all_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) { Text(stringResource(com.tuned.app.R.string.common_cancel)) }
            },
        )
    }

    if (showFolderDialog) {
        DownloadFolderDialog(
            currentFolder = downloadFolder,
            onDismiss = { showFolderDialog = false },
            onConfirm = { name ->
                viewModel.setDownloadFolder(name)
                showFolderDialog = false
            },
        )
    }

    if (showLocationDialog) {
        DownloadLocationDialog(
            currentDescription = customLocationLabel
                ?: stringResource(
                    com.tuned.app.R.string.dl_location_default,
                    downloadFolder,
                ),
            isCustom = hasCustomLocation,
            isUsable = !customLocationStale,
            onChoose = {
                showLocationDialog = false
                locationPicker.launch(
                    runCatching { android.net.Uri.parse(downloadTreeUri) }.getOrNull(),
                )
            },
            onReset = {
                viewModel.clearDownloadTreeUri()
                showLocationDialog = false
            },
            onDismiss = { showLocationDialog = false },
        )
    }

    if (showOrganizationDialog) {
        FolderOrganizationDialog(
            currentStructure = downloadStructure,
            useAlbumArtist = useAlbumArtistForFolders,
            primaryOnly = primaryArtistOnly,
            downloadFolder = downloadFolder,
            onDismiss = { showOrganizationDialog = false },
            onStructureChange = { viewModel.setDownloadStructure(it) },
            onUseAlbumArtistChange = { viewModel.setUseAlbumArtistForFolders(it) },
            onPrimaryOnlyChange = { viewModel.setPrimaryArtistOnly(it) },
        )
    }
}
}

@Composable
private fun DownloadLocationDialog(
    currentDescription: String,
    isCustom: Boolean,
    isUsable: Boolean,
    onChoose: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(com.tuned.app.R.string.dl_location)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(com.tuned.app.R.string.dl_location_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    currentDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCustom && !isUsable) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
                if (isCustom && !isUsable) {
                    Text(
                        stringResource(com.tuned.app.R.string.dl_location_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onChoose) {
                Text(stringResource(com.tuned.app.R.string.dl_location_choose))
            }
        },
        dismissButton = {
            Row {
                if (isCustom) {
                    TextButton(onClick = onReset) {
                        Text(stringResource(com.tuned.app.R.string.dl_location_reset))
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(com.tuned.app.R.string.common_cancel)) }
            }
        },
    )
}

@Composable
private fun DownloadFolderDialog(
    currentFolder: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var input by remember(currentFolder) { mutableStateOf(currentFolder) }
    val sanitized = remember(input) {
        com.tuned.app.data.local.sanitizeDownloadFolderName(input.ifBlank { currentFolder })
    }
    val isChanged = sanitized != currentFolder
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(com.tuned.app.R.string.dl_folder)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(com.tuned.app.R.string.dl_folder_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text(stringResource(com.tuned.app.R.string.dl_folder_name)) },
                    prefix = { Text("Music/") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (isChanged) {
                    Text(
                        stringResource(com.tuned.app.R.string.dl_folder_will_save, sanitized),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(input) },
                enabled = input.isNotBlank() && isChanged,
            ) { Text(stringResource(com.tuned.app.R.string.common_save)) }
        },
        dismissButton = {
            Row {
                if (currentFolder != com.tuned.app.data.local.DEFAULT_DOWNLOAD_FOLDER) {
                    TextButton(onClick = { onConfirm(com.tuned.app.data.local.DEFAULT_DOWNLOAD_FOLDER) }) {
                        Text(stringResource(com.tuned.app.R.string.common_reset))
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(com.tuned.app.R.string.common_cancel)) }
            }
        },
    )
}

@Composable
private fun FolderOrganizationDialog(
    currentStructure: com.tuned.app.data.local.DownloadFolderStructure,
    useAlbumArtist: Boolean,
    primaryOnly: Boolean,
    downloadFolder: String,
    onDismiss: () -> Unit,
    onStructureChange: (com.tuned.app.data.local.DownloadFolderStructure) -> Unit,
    onUseAlbumArtistChange: (Boolean) -> Unit,
    onPrimaryOnlyChange: (Boolean) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(com.tuned.app.R.string.dl_org)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    stringResource(com.tuned.app.R.string.dl_org_desc, downloadFolder),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                com.tuned.app.data.local.DownloadFolderStructure.entries.forEach { structure ->
                    val selected = structure == currentStructure
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onStructureChange(structure) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected, onClick = { onStructureChange(structure) })
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(structure.titleRes),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            )
                            Text(
                                structure.example.replace("<dir>", downloadFolder),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(com.tuned.app.R.string.dl_use_album_artist), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            stringResource(com.tuned.app.R.string.dl_use_album_artist_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = useAlbumArtist, onCheckedChange = onUseAlbumArtistChange)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(com.tuned.app.R.string.dl_primary_only), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            stringResource(com.tuned.app.R.string.dl_primary_only_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = primaryOnly, onCheckedChange = onPrimaryOnlyChange)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(com.tuned.app.R.string.common_done)) }
        },
    )
}

@Composable
private fun DownloadTabBar(
    selectedTab: DownloadTab,
    onTabSelected: (DownloadTab) -> Unit,
    songCount: Int,
    artistCount: Int,
    albumCount: Int,
) {
    val colors = MaterialTheme.tunedColors
    // Home's chips: neutral pills, the selected one filled in the text colour. The count sits
    // muted after the name, so the lists below need no "Songs (1)" heading.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = Keyline.margin, end = Keyline.margin, top = Space.lg, bottom = Space.md),
        horizontalArrangement = Arrangement.spacedBy(Space.sm + Space.xxs),
    ) {
        listOf(
            Triple(DownloadTab.SONGS, "Songs", songCount),
            Triple(DownloadTab.ARTISTS, "Artists", artistCount),
            Triple(DownloadTab.ALBUMS, "Albums", albumCount),
        ).forEach { (tab, label, count) ->
            val selected = tab == selectedTab
            Surface(
                onClick = { onTabSelected(tab) },
                shape = AppShape.full,
                color = if (selected) colors.text1 else colors.text1.copy(alpha = 0.08f),
                contentColor = if (selected) colors.bg else colors.text1,
            ) {
                Row(
                    Modifier.padding(horizontal = Space.lg, vertical = Space.sm + Space.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, style = AppType.bodyStrong)
                    Spacer(Modifier.width(Space.xs + Space.xxs))
                    Text(
                        "$count",
                        style = AppType.bodyStrong,
                        color = if (selected) colors.bg.copy(alpha = 0.6f) else colors.text2,
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadFilterControls(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedTab: DownloadTab,
    songSort: SongSortOption,
    onSongSortChange: (SongSortOption) -> Unit,
    artistSort: ArtistSortOption,
    onArtistSortChange: (ArtistSortOption) -> Unit,
    albumSort: AlbumSortOption,
    onAlbumSortChange: (AlbumSortOption) -> Unit,
    filterLossless: Boolean,
    onToggleLossless: () -> Unit,
    filterLyrics: Boolean,
    onToggleLyrics: () -> Unit,
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Keyline.margin, end = iconEdgeInset, bottom = Space.sm),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Search bar + Sort menu button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        when (selectedTab) {
                            DownloadTab.SONGS -> "Search downloads"
                            DownloadTab.ARTISTS -> "Search artists"
                            DownloadTab.ALBUMS -> "Search albums"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                    )
                },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = stringResource(com.tuned.app.R.string.search_cd), modifier = Modifier.size(20.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(com.tuned.app.R.string.pd_clear_search_cd), modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                // A borderless pill on the chips' wash.
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.tunedColors.text1.copy(alpha = 0.08f),
                    unfocusedContainerColor = MaterialTheme.tunedColors.text1.copy(alpha = 0.08f),
                    focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
            )

            // Sort dropdown button
            Box {
                // A plain icon, the same as Library's sort.
                IconButton(onClick = { sortMenuExpanded = true }) {
                    Icon(
                        Icons.AutoMirrored.Filled.Sort,
                        contentDescription = stringResource(com.tuned.app.R.string.pl_sort_label),
                    )
                }

                DropdownMenu(shape = com.tuned.app.ui.theme.MenuShape, containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh, 
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                ) {
                    when (selectedTab) {
                        DownloadTab.SONGS -> {
                            // The filters live here with the sort instead of a second chip row.
                            DropdownMenuItem(
                                text = { Text(stringResource(com.tuned.app.R.string.dl_filter_lossless)) },
                                trailingIcon = { if (filterLossless) Icon(Icons.Filled.Check, contentDescription = null) },
                                onClick = onToggleLossless,
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(com.tuned.app.R.string.dl_filter_lyrics)) },
                                trailingIcon = { if (filterLyrics) Icon(Icons.Filled.Check, contentDescription = null) },
                                onClick = onToggleLyrics,
                            )
                            androidx.compose.material3.HorizontalDivider(Modifier.padding(vertical = Space.xs))
                            SongSortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.label,
                                            fontWeight = if (songSort == option) FontWeight.Bold else FontWeight.Normal,
                                            color = if (songSort == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        )
                                    },
                                    onClick = {
                                        onSongSortChange(option)
                                        sortMenuExpanded = false
                                    },
                                )
                            }
                        }
                        DownloadTab.ARTISTS -> {
                            ArtistSortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.label,
                                            fontWeight = if (artistSort == option) FontWeight.Bold else FontWeight.Normal,
                                            color = if (artistSort == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        )
                                    },
                                    onClick = {
                                        onArtistSortChange(option)
                                        sortMenuExpanded = false
                                    },
                                )
                            }
                        }
                        DownloadTab.ALBUMS -> {
                            AlbumSortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.label,
                                            fontWeight = if (albumSort == option) FontWeight.Bold else FontWeight.Normal,
                                            color = if (albumSort == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        )
                                    },
                                    onClick = {
                                        onAlbumSortChange(option)
                                        sortMenuExpanded = false
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

/** Play and Shuffle under the controls of every tab, the album page's pair. */
@Composable
private fun DownloadPlayRow(onPlay: () -> Unit, onShuffle: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Keyline.margin, end = Keyline.margin, top = Space.sm, bottom = Space.lg),
        horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        Box(Modifier.weight(1f)) {
            DownloadPill(Icons.Filled.PlayArrow, stringResource(com.tuned.app.R.string.dl_play_cd), filled = true) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onPlay()
            }
        }
        Box(Modifier.weight(1f)) {
            DownloadPill(Icons.Filled.Shuffle, stringResource(com.tuned.app.R.string.feed_shuffle), filled = false) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onShuffle()
            }
        }
    }
}

/** A full-width pill: Play in the text colour, Shuffle on the 10% wash. */
@Composable
private fun DownloadPill(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, filled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.tunedColors
    Surface(
        onClick = onClick,
        shape = AppShape.full,
        color = if (filled) colors.text1 else colors.text1.copy(alpha = 0.1f),
        contentColor = if (filled) colors.bg else colors.text1,
        modifier = Modifier
            .fillMaxWidth()
            .height(Sizes.touch),
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(Sizes.iconRow))
            Spacer(Modifier.width(Space.sm))
            Text(label, style = AppType.bodyStrong)
        }
    }
}

/** An artist or album in a list: cover, name, one muted line. No card; the whole row opens it. */
@Composable
private fun DownloadCollectionRow(
    title: String,
    subtitle: String,
    artworkName: String,
    artworkArtist: String,
    artworkUrl: String?,
    round: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.tunedColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Keyline.margin, vertical = Space.sm + Space.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkImage(
            name = artworkName,
            artist = artworkArtist,
            embeddedUrl = artworkUrl,
            fallbackIcon = if (round) Icons.Filled.Person else Icons.Filled.Album,
            modifier = Modifier
                .size(Sizes.artRow)
                .clip(if (round) CircleShape else DownloadArtShape),
        )
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
            Text(title, style = AppType.body, color = colors.text1, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = AppType.bodyS, color = colors.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.text2,
            modifier = Modifier.size(Sizes.iconRow),
        )
    }
}

/** Row covers use the shared row shape (10dp, like Library's). */
private val DownloadArtShape = ArtworkShape

@Composable
private fun downloadListBottom(): androidx.compose.ui.unit.Dp =
    Space.xl + com.tuned.app.ui.player.LocalMiniPlayerScrollClearance.current + com.tuned.app.ui.common.safeDrawingBottomPadding()

@Composable
private fun DownloadsEmptyResult(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = Space.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.tunedColors.text2, modifier = Modifier.size(Sizes.iconHero))
        Text(text, style = AppType.body, color = MaterialTheme.tunedColors.text2)
    }
}

@Composable
private fun DownloadedSongsList(
    tracks: List<DownloadedTrackEntity>,
    activeDownloads: List<DownloadProgress>,
    playbackState: com.tuned.app.playback.PlaybackChromeState,
    totalSizeText: String,
    downloadFolder: String,
    downloadLocationLabel: String? = null,
    downloadStructure: com.tuned.app.data.local.DownloadFolderStructure,
    onPlayTrack: (DownloadedTrackEntity) -> Unit,
    onPlayNext: (DownloadedTrackEntity) -> Unit,
    onAddToQueue: (DownloadedTrackEntity) -> Unit,
    onPlayAll: (Boolean) -> Unit,
    onSelectArtist: (String) -> Unit,
    onSelectAlbum: (String, String) -> Unit,
    onDeleteTrack: (DownloadedTrackEntity) -> Unit,
    onCancelDownload: (String) -> Unit,
    onOpenFileManager: () -> Unit,
    onChangeFolder: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        contentPadding = PaddingValues(bottom = downloadListBottom()),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (activeDownloads.isNotEmpty()) {
            item { DownloadSectionTitle("Downloading") }
            items(activeDownloads, key = { it.key }) { download ->
                Box(Modifier.padding(horizontal = Keyline.margin, vertical = Space.xs)) {
                    ActiveDownloadCard(
                        download = download,
                        onCancel = { onCancelDownload(download.key) },
                    )
                }
            }
        }

        if (tracks.isNotEmpty()) {
            item { DownloadPlayRow(onPlay = { onPlayAll(false) }, onShuffle = { onPlayAll(true) }) }
            items(tracks, key = { it.id }) { track ->
                Box(Modifier.animateItem()) {
                    DownloadedTrackCard(
                        track = track,
                        isPlaying = isPlayingTrack(playbackState, track),
                        position = GroupPosition.SINGLE,
                        onPlay = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPlayTrack(track)
                        },
                        onPlayNext = { onPlayNext(track) },
                        onAddToQueue = { onAddToQueue(track) },
                        onSelectArtist = { onSelectArtist(track.artist) },
                        onSelectAlbum = { if (track.album.isNotBlank()) onSelectAlbum(track.album, track.artist) },
                        onDelete = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDeleteTrack(track)
                        },
                    )
                }
            }
        } else {
            item { DownloadsEmptyResult(Icons.Filled.Search, "No songs match") }
        }
    }
}

@Composable
private fun DownloadedArtistsList(
    artists: List<DownloadedArtist>,
    onSelectArtist: (DownloadedArtist) -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = downloadListBottom()),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (artists.isNotEmpty()) {
            item { DownloadPlayRow(onPlay = onPlayAll, onShuffle = onShuffleAll) }
            items(artists, key = { it.name }) { artist ->
                DownloadCollectionRow(
                    title = artist.name,
                    subtitle = pluralStringResource(R.plurals.pl_song_count, artist.tracks.size, artist.tracks.size),
                    artworkName = artist.name,
                    artworkArtist = artist.name,
                    artworkUrl = artist.artworkUrl,
                    round = true,
                    onClick = { onSelectArtist(artist) },
                )
            }
        } else {
            item { DownloadsEmptyResult(Icons.Filled.Person, "No artists match") }
        }
    }
}

@Composable
private fun DownloadedAlbumsList(
    albums: List<DownloadedAlbum>,
    onSelectAlbum: (DownloadedAlbum) -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = downloadListBottom()),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (albums.isNotEmpty()) {
            item { DownloadPlayRow(onPlay = onPlayAll, onShuffle = onShuffleAll) }
            items(albums.distinctBy { "${it.title}_${it.artist}" }, key = { "${it.title}_${it.artist}" }) { album ->
                DownloadCollectionRow(
                    title = album.title,
                    subtitle = album.artist,
                    artworkName = album.title,
                    artworkArtist = album.artist,
                    artworkUrl = album.artworkUrl,
                    round = false,
                    onClick = { onSelectAlbum(album) },
                )
            }
        } else {
            item { DownloadsEmptyResult(Icons.Filled.Album, "No albums match") }
        }
    }
}

/**
 * The top of a downloaded artist or album, built like the album page (AlbumHeader): no card, a
 * centred cover (round for an artist), the name, the artist in the accent, a muted line of
 * counts, then Apple's pair of Play and Shuffle pills.
 */
@Composable
private fun DownloadHero(
    title: String,
    artworkName: String,
    artworkArtist: String,
    artworkUrl: String?,
    round: Boolean,
    byline: String?,
    meta: String,
    playing: Boolean,
    onByline: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
) {
    val colors = MaterialTheme.tunedColors
    val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
    val coverShape = if (round) CircleShape else AppShape.md
    val coverSide = if (round) Sizes.tileSquare + Space.xxl else detailCoverSide(screenWidth)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = detailHeaderTop(), bottom = Space.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(coverSide)
                .shadow(Space.md, coverShape, clip = false)
                .clip(coverShape),
        ) {
            ArtworkImage(
                name = artworkName,
                artist = artworkArtist,
                embeddedUrl = artworkUrl,
                fallbackIcon = if (round) Icons.Filled.Person else Icons.Filled.Album,
                modifier = Modifier.fillMaxSize(),
            )
        }
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
        if (!byline.isNullOrBlank()) {
            Spacer(Modifier.height(Space.xxs))
            Text(
                text = byline,
                style = AppType.title,
                color = colors.accent,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = Keyline.detail + Space.xs)
                    .clip(AppShape.xs)
                    .clickable(onClick = onByline),
            )
        }
        Spacer(Modifier.height(Space.xs))
        Text(text = meta, style = AppType.caption, color = colors.text2, maxLines = 1)
        Spacer(Modifier.height(Space.lg + Space.xs))
        Row(
            horizontalArrangement = Arrangement.spacedBy(Space.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DetailPlayPill(playing = playing, enabled = true, onClick = onPlay)
            DownloadShufflePill(onClick = onShuffle)
        }
    }
}

/** Shuffle beside the Play pill, the same size, on the side buttons' 10% wash. */
@Composable
private fun DownloadShufflePill(onClick: () -> Unit) {
    val colors = MaterialTheme.tunedColors
    Surface(
        onClick = onClick,
        shape = AppShape.full,
        color = colors.text1.copy(alpha = 0.1f),
        contentColor = colors.text1,
        modifier = Modifier.size(width = DetailPlayPillWidth, height = Sizes.touch),
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(Sizes.iconRow))
            Spacer(Modifier.width(Space.sm))
            Text(stringResource(com.tuned.app.R.string.feed_shuffle), style = AppType.bodyStrong)
        }
    }
}

/** A section title on these pages, in the album page's size. */
@Composable
private fun DownloadSectionTitle(text: String) {
    Text(
        text = text,
        style = AppType.title,
        color = MaterialTheme.tunedColors.text1,
        modifier = Modifier.padding(start = Keyline.detail, end = Keyline.detail, top = Space.lg, bottom = Space.xs),
    )
}

private fun isPlayingTrack(state: com.tuned.app.playback.PlaybackChromeState, track: DownloadedTrackEntity): Boolean =
    state.isPlaying &&
        state.current?.title.equals(track.title, ignoreCase = true) &&
        state.current?.artist.equals(track.artist, ignoreCase = true)

/**
 * The frame of a downloaded artist or album page, as on the album page: the accent wash behind
 * the top, the list, and a frosted back button floating under the status bar instead of a header.
 */
@Composable
private fun DownloadDetailFrame(
    onBack: () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
        com.tuned.app.ui.common.DetailAccentWash(listState)
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = downloadListBottom()),
            modifier = Modifier.fillMaxSize(),
            content = content,
        )
        Box(
            Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = Keyline.detail - Space.xs, top = Space.xs),
        ) {
            com.tuned.app.ui.common.DetailSideButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(com.tuned.app.R.string.cmp_back),
                onClick = onBack,
                ink = MaterialTheme.tunedColors.text1,
            )
        }
    }
}

@Composable
private fun ArtistDetailView(
    artistName: String,
    tracks: List<DownloadedTrackEntity>,
    albums: List<DownloadedAlbum>,
    playbackState: com.tuned.app.playback.PlaybackChromeState,
    onPlayTrack: (DownloadedTrackEntity) -> Unit,
    onPlayNext: (DownloadedTrackEntity) -> Unit,
    onAddToQueue: (DownloadedTrackEntity) -> Unit,
    onPlayAll: (Boolean) -> Unit,
    onSelectArtist: (String) -> Unit = {},
    onSelectAlbum: (String) -> Unit,
    onDeleteTrack: (DownloadedTrackEntity) -> Unit,
    onBack: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val colors = MaterialTheme.tunedColors
    val totalSize = tracks.sumOf { it.fileSizeBytes }
    val latestArtwork = tracks.firstOrNull { !it.artworkUrl.isNullOrBlank() }?.artworkUrl

    DownloadDetailFrame(onBack = onBack) {
        item {
            DownloadHero(
                title = artistName,
                artworkName = artistName,
                artworkArtist = artistName,
                artworkUrl = latestArtwork,
                round = true,
                byline = null,
                meta = formatBytes(totalSize),
                playing = tracks.any { isPlayingTrack(playbackState, it) },
                onByline = {},
                onPlay = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPlayAll(false)
                },
                onShuffle = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPlayAll(true)
                },
            )
        }

        if (albums.isNotEmpty()) {
            item { DownloadSectionTitle("Albums") }
            item {
                // Plain tiles, cover and two lines, like Home's shelves: no card behind them.
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Space.md),
                    contentPadding = PaddingValues(horizontal = Keyline.detail),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(albums.distinctBy { it.title }, key = { it.title }) { album ->
                        Column(
                            Modifier
                                .width(Sizes.tileSquare)
                                .clip(AppShape.sm)
                                .clickable { onSelectAlbum(album.title) },
                        ) {
                            ArtworkImage(
                                name = album.title,
                                artist = album.artist,
                                embeddedUrl = album.artworkUrl,
                                fallbackIcon = Icons.Filled.Album,
                                modifier = Modifier
                                    .size(Sizes.tileSquare)
                                    .clip(AppShape.sm),
                            )
                            Spacer(Modifier.height(Space.sm))
                            Text(
                                text = album.title,
                                style = AppType.bodyStrong,
                                color = colors.text1,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = pluralStringResource(R.plurals.pl_song_count, album.tracks.size, album.tracks.size),
                                style = AppType.bodyS,
                                color = colors.text2,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }

        item { DownloadSectionTitle("Songs") }
        items(tracks, key = { it.id }) { track ->
            DownloadedTrackCard(
                track = track,
                isPlaying = isPlayingTrack(playbackState, track),
                position = GroupPosition.SINGLE,
                onPlay = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPlayTrack(track)
                },
                onPlayNext = { onPlayNext(track) },
                onAddToQueue = { onAddToQueue(track) },
                onSelectArtist = { onSelectArtist(track.artist) },
                onSelectAlbum = { if (track.album.isNotBlank()) onSelectAlbum(track.album) },
                onDelete = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDeleteTrack(track)
                },
            )
        }
    }
}

@Composable
private fun AlbumDetailView(
    albumTitle: String,
    artistName: String,
    tracks: List<DownloadedTrackEntity>,
    playbackState: com.tuned.app.playback.PlaybackChromeState,
    onPlayTrack: (DownloadedTrackEntity) -> Unit,
    onPlayNext: (DownloadedTrackEntity) -> Unit,
    onAddToQueue: (DownloadedTrackEntity) -> Unit,
    onPlayAll: (Boolean) -> Unit,
    onSelectArtist: (String) -> Unit,
    onSelectAlbum: (String, String) -> Unit = { _, _ -> },
    onDeleteTrack: (DownloadedTrackEntity) -> Unit,
    onBack: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val totalSize = tracks.sumOf { it.fileSizeBytes }
    val latestArtwork = tracks.firstOrNull { !it.artworkUrl.isNullOrBlank() }?.artworkUrl

    DownloadDetailFrame(onBack = onBack) {
        item {
            DownloadHero(
                title = albumTitle,
                artworkName = albumTitle,
                artworkArtist = artistName,
                artworkUrl = latestArtwork,
                round = false,
                byline = artistName,
                meta = formatBytes(totalSize),
                playing = tracks.any { isPlayingTrack(playbackState, it) },
                onByline = { onSelectArtist(artistName) },
                onPlay = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPlayAll(false)
                },
                onShuffle = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPlayAll(true)
                },
            )
        }

        itemsIndexed(
            items = tracks,
            key = { _, item -> item.id },
        ) { index, track ->
            DownloadedTrackCard(
                track = track,
                trackNumber = index + 1,
                isPlaying = isPlayingTrack(playbackState, track),
                position = GroupPosition.SINGLE,
                onPlay = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPlayTrack(track)
                },
                onPlayNext = { onPlayNext(track) },
                onAddToQueue = { onAddToQueue(track) },
                onSelectArtist = { onSelectArtist(track.artist) },
                onSelectAlbum = { if (track.album.isNotBlank()) onSelectAlbum(track.album, track.artist) },
                onDelete = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDeleteTrack(track)
                },
            )
        }
        // Under the tracklist, as Apple has it: how much is here, then what it takes up.
        item {
            Text(
                text = pluralStringResource(R.plurals.pl_song_count, tracks.size, tracks.size),
                style = AppType.bodyS,
                color = MaterialTheme.tunedColors.text2,
                modifier = Modifier.padding(start = Keyline.detail, end = Keyline.detail, top = Space.lg),
            )
        }
    }
}

@Composable
private fun ActiveDownloadCard(
    download: DownloadProgress,
    onCancel: () -> Unit,
) {
    val progressFloat by animateFloatAsState(
        targetValue = download.progressPercent / 100f,
        label = "downloadProgress",
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = download.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (download.isWaitingForConnection) {
                            "Waiting for a connection"
                        } else {
                            "${download.progressPercent}%"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(com.tuned.app.R.string.dl_cancel_dl_cd), tint = MaterialTheme.colorScheme.error)
                }
            }

            LinearProgressIndicator(
                progress = { progressFloat },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
            )
        }
    }
}

@Composable
private fun DownloadedTrackCard(
    track: DownloadedTrackEntity,
    isPlaying: Boolean,
    position: GroupPosition,
    trackNumber: Int? = null,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onSelectArtist: () -> Unit,
    onSelectAlbum: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val colors = MaterialTheme.tunedColors
    // No card behind the row: songs sit on the page like Library's playlists. Album rows are
    // numbered with no cover, like the album page's tracklist.
    Box(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Sizes.rowCompact)
                .combinedClickable(onClick = onPlay, onLongClick = { menuExpanded = true })
                // The album page's margin on its tracklist, the page margin everywhere else.
                .padding(
                    start = if (trackNumber != null) Keyline.detail else Keyline.margin,
                    end = iconEdgeInset,
                    top = Space.sm + Space.xxs,
                    bottom = Space.sm + Space.xxs,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (trackNumber != null) {
                Box(Modifier.width(Space.xxl), contentAlignment = Alignment.CenterStart) {
                    if (isPlaying) {
                        PlayingWaveBars(
                            modifier = Modifier.size(width = Sizes.iconRow, height = Sizes.iconInline),
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                            waveColor = colors.accent,
                        )
                    } else {
                        Text(text = "$trackNumber", style = AppType.body, color = colors.text2)
                    }
                }
            } else {
                Box(Modifier.size(Sizes.artRow)) {
                    ArtworkImage(
                        name = track.title,
                        artist = track.artist,
                        embeddedUrl = track.artworkUrl,
                        fallbackIcon = Icons.Filled.MusicNote,
                        modifier = Modifier.fillMaxSize().clip(DownloadArtShape),
                    )
                    if (isPlaying) {
                        PlayingWaveBars(
                            Modifier.align(Alignment.BottomEnd).padding(3.dp).size(22.dp, 16.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.width(Space.md))

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
                Text(
                    text = track.title,
                    style = AppType.body,
                    color = if (isPlaying) colors.accent else colors.text1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // One muted fact per line: the artist on a song list, the length on an album's
                // tracklist (the page names the rest). The format is in the menu.
                val detail = if (trackNumber == null) {
                    track.artist
                } else {
                    track.durationMs.takeIf { it > 0 }?.let { formatDuration(it) }.orEmpty()
                }
                if (detail.isNotBlank()) {
                    Text(text = detail, style = AppType.bodyS, color = colors.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            // More options menu
            Box {
                AppIconButton(
                    icon = Icons.Filled.MoreVert,
                    contentDescription = stringResource(com.tuned.app.R.string.dl_song_options_cd),
                    onClick = { menuExpanded = true },
                    iconSize = Sizes.iconRow,
                    tint = colors.text2,
                )

                DropdownMenu(shape = com.tuned.app.ui.theme.MenuShape, containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh, 
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    // The file's format, which the row no longer shows.
                    Text(
                        text = track.formatBadge,
                        style = AppType.bodyS,
                        color = colors.text2,
                        modifier = Modifier.padding(horizontal = Space.lg, vertical = Space.sm),
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(com.tuned.app.R.string.dl_play_cd)) },
                        leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onPlay()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(com.tuned.app.R.string.dl_play_next)) },
                        leadingIcon = { Icon(Icons.Filled.PlaylistAdd, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onPlayNext()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(com.tuned.app.R.string.dl_add_queue)) },
                        leadingIcon = { Icon(Icons.Filled.QueueMusic, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onAddToQueue()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(com.tuned.app.R.string.dl_view_artist, track.artist)) },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onSelectArtist()
                        },
                    )
                    if (track.album.isNotBlank()) {
                        DropdownMenuItem(
                            text = { Text(stringResource(com.tuned.app.R.string.dl_view_album, track.album)) },
                            leadingIcon = { Icon(Icons.Filled.Album, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onSelectAlbum()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(com.tuned.app.R.string.common_delete), color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDownloadsState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(88.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp),
                    )
                }
            }
            Text(
                stringResource(com.tuned.app.R.string.dl_empty_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(com.tuned.app.R.string.dl_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

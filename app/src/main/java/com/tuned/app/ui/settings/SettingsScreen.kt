package com.tuned.app.ui.settings

import com.tuned.app.ui.common.sheetScrollGuard
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Widgets
import com.tuned.app.BuildConfig
import com.tuned.app.data.jam.JamRepository
import com.tuned.app.ui.jam.JamSheet
import com.tuned.app.ui.jam.JamViewModel
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalFocusManager
import com.tuned.app.ui.common.HeaderActionIcon
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.graphics.Shape
import kotlinx.coroutines.delay
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import com.tuned.app.util.BatteryOptimizationHelper
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Waves
import com.tuned.app.ui.player.LyricsOffsetDialog
import com.tuned.app.ui.player.LyricsSizeDialog
import androidx.compose.material.icons.filled.FormatSize
import kotlin.math.roundToInt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.SpeakerPhone
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.blur
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.tuned.app.data.local.AppLanguage
import com.tuned.app.data.local.nativeDisplayName
import com.tuned.app.data.lyrics.translationLanguageName
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import java.util.Locale
import com.tuned.app.ui.player.LocalMiniPlayerScrollClearance
import com.tuned.app.R
import com.tuned.app.data.update.AppUpdateManager
import com.tuned.app.data.local.AccentMode
import com.tuned.app.data.local.ThemeMode
import com.tuned.app.playback.ClarityPresets
import com.tuned.app.playback.LoudnessMode
import com.tuned.app.ui.common.ExpressiveHeader
import com.tuned.app.ui.common.safeDrawingBottomPadding
import com.tuned.app.ui.common.safeHorizontalContentPadding
import com.tuned.app.ui.common.adaptiveContentWidth
import com.tuned.app.ui.theme.ExpressivePillShape

data class ToggleDetailInfo(
    val title: String,
    val subtitle: String,
    val checked: Boolean,
)

val LocalToggleDetailViewer = staticCompositionLocalOf<((ToggleDetailInfo) -> Unit)?> { null }

/**
 * Determines the target LazyColumn item index (section) for a searchable setting.
 */
internal fun getTargetSectionIndex(settingId: String?): Int {
    if (settingId == null) return 0
    val entry = SettingsSearchIndex.allEntries.firstOrNull { it.id == settingId } ?: return 0
    return when (entry.parentTab) {
        SettingsTab.AUDIO -> if (entry.section == "Output & Loudness") 1 else 0
        SettingsTab.APPEARANCE -> when (entry.section) {
            "Accent Color" -> 1
            "Experimental & Features", "Experimental & Player" -> 2
            "Canvas" -> 3
            else -> 0
        }
        SettingsTab.YOUTUBE -> if (entry.section == "Liked Songs") 1 else 0
        SettingsTab.SPOTIFY -> 0
        SettingsTab.LIBRARY -> when (entry.section) {
            "Content" -> 1
            "Imports", "Imports & Addons", "Modules & Addons" -> 2
            else -> 0
        }
        SettingsTab.DATA_BACKUP -> if (entry.section == "Data Management") 1 else 0
        else -> 0
    }
}

enum class SettingsTab(
    @androidx.annotation.StringRes val titleRes: Int,
    @androidx.annotation.StringRes val subtitleRes: Int,
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    AUDIO(
        titleRes = R.string.set_cat_audio,
        subtitleRes = R.string.set_cat_audio_sub,
        title = "Audio",
        subtitle = "Quality, equalizer and output",
        icon = Icons.Filled.GraphicEq,
    ),
    APPEARANCE(
        titleRes = R.string.set_cat_appear,
        subtitleRes = R.string.set_cat_appear_sub,
        title = "Appearance",
        subtitle = "Theme, colours, artwork and lyrics",
        icon = Icons.Filled.Palette,
    ),
    YOUTUBE(
        titleRes = R.string.set_cat_yt,
        subtitleRes = R.string.set_cat_yt_sub,
        title = "YouTube Music",
        subtitle = "Account, channels and library sync",
        icon = Icons.Filled.CloudSync,
    ),
    SPOTIFY(
        titleRes = R.string.set_cat_spotify,
        subtitleRes = R.string.set_cat_spotify_sub,
        title = "Spotify",
        subtitle = "Canvas, playlists and liked songs",
        icon = Icons.Filled.PlayCircle,
    ),
    LAST_FM(
        titleRes = R.string.set_cat_lfm,
        subtitleRes = R.string.set_cat_lfm_sub,
        title = "Last.fm",
        subtitle = "Optional",
        icon = Icons.Filled.Album,
    ),
    LIBRARY(
        titleRes = R.string.set_cat_lib,
        subtitleRes = R.string.set_cat_lib_sub,
        title = "Library",
        subtitle = "Home layout, imports and downloads",
        icon = Icons.Filled.QueueMusic,
    ),
    DATA_BACKUP(
        titleRes = R.string.set_cat_data,
        subtitleRes = R.string.set_cat_data_sub,
        title = "Data",
        subtitle = "Backup, cache and reset",
        icon = Icons.Filled.Backup,
    ),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onLoggedOut: () -> Unit = {},
    onOpenDownloads: () -> Unit = {},
    onOpenModules: () -> Unit = {},
    onOpenHomeSections: () -> Unit = {},
    onOpenExcludedSongs: () -> Unit = {},
    onOpenYouTubeImport: () -> Unit = {},
    onOpenYouTubeLogin: () -> Unit = {},
    onOpenExternalImport: () -> Unit = {},
    onOpenSpotifyLogin: () -> Unit = {},
    onOpenSpotifyTokenImport: () -> Unit = {},
    /** Open on this setting's tab with it highlighted, as a search result does (e.g. "youtube.account"). */
    initialSettingId: String? = null,
    viewModel: SettingsViewModel = hiltViewModel(),
    jamViewModel: JamViewModel = hiltViewModel(),
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val jamUiState by jamViewModel.uiState.collectAsStateWithLifecycle()
    val avatarUrl by viewModel.avatarUrl.collectAsStateWithLifecycle()
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val misc by viewModel.misc.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadCount by viewModel.downloadCount.collectAsStateWithLifecycle()
    val downloadTotalBytes by viewModel.downloadTotalBytes.collectAsStateWithLifecycle()
    val ytConnection by viewModel.ytConnection.collectAsStateWithLifecycle()
    val ytSyncEnabled by viewModel.ytSyncEnabled.collectAsStateWithLifecycle()
    val ytHistorySyncEnabled by viewModel.ytHistorySyncEnabled.collectAsStateWithLifecycle()
    val ytSyncState by viewModel.ytSyncState.collectAsStateWithLifecycle()
    val ytLastSyncAt by viewModel.ytLastSyncAt.collectAsStateWithLifecycle()
    val syncedPlaylistIds by viewModel.syncedPlaylistIds.collectAsStateWithLifecycle()
    val allPlaylists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val ytAccountPlaylists by viewModel.ytAccountPlaylists.collectAsStateWithLifecycle()
    val hiddenYtLibraryPlaylistIds by viewModel.hiddenYtLibraryPlaylistIds.collectAsStateWithLifecycle()
    val ytChannels by viewModel.ytChannels.collectAsStateWithLifecycle()
    val ytChannelsLoading by viewModel.ytChannelsLoading.collectAsStateWithLifecycle()
    val spotifyConnected by viewModel.spotifyConnected.collectAsStateWithLifecycle()
    val eq by viewModel.equalizer.collectAsStateWithLifecycle()
    val usbExclusiveEnabled by viewModel.usbExclusiveEnabled.collectAsStateWithLifecycle()
    val internalDirectEnabled by viewModel.internalDirectEnabled.collectAsStateWithLifecycle()
    val forceSoftwareVolume by viewModel.forceSoftwareVolume.collectAsStateWithLifecycle()
    val internalDirectAvailability by viewModel.internalDirectAvailability.collectAsStateWithLifecycle()
    val loudness by viewModel.loudness.collectAsStateWithLifecycle()
    val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()
    var showUpdateSheet by remember { mutableStateOf(false) }
    if (showUpdateSheet) {
        com.tuned.app.ui.common.UpdateSheet(viewModel.appUpdateManager) { showUpdateSheet = false }
    }
    val discordPresenceEnabled by viewModel.discordPresenceEnabled.collectAsStateWithLifecycle()
    val scrobbler by viewModel.scrobbler.collectAsStateWithLifecycle()
    val isLastFmConnected by viewModel.isLastFmConnected.collectAsStateWithLifecycle()
    val hasApiKey by viewModel.hasApiKey.collectAsStateWithLifecycle()
    val lastFmAuthUrl by viewModel.lastFmAuthUrl.collectAsStateWithLifecycle()
    val lastFmConnecting by viewModel.lastFmConnecting.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Last.fm web auth (Settings → Integrations): open the auth URL in Custom
    // Tabs the moment SettingsViewModel produces one.
    androidx.compose.runtime.LaunchedEffect(lastFmAuthUrl) {
        lastFmAuthUrl?.let { url ->
            runCatching {
                androidx.browser.customtabs.CustomTabsIntent.Builder().build()
                    .launchUrl(context, android.net.Uri.parse(url))
            }.onFailure {
                viewModel.showToast(context.getString(R.string.toast_no_browser_lfm))
                viewModel.cancelLastFmConnect()
            }
        }
    }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showDownloadQualityDialog by remember { mutableStateOf(false) }
    var showEqSheet by remember { mutableStateOf(false) }
    var showAutoEq by remember { mutableStateOf(false) }
    var showLyricsProviderDialog by remember { mutableStateOf(false) }
    var showLyricsOffsetDialog by remember { mutableStateOf(false) }
    var showLyricsSizeDialog by remember { mutableStateOf(false) }
    var showLyricsAutoHideDialog by remember { mutableStateOf(false) }
    var showTranslationLanguageDialog by remember { mutableStateOf(false) }
    var showLoudnessDialog by remember { mutableStateOf(false) }
    var showScrobblePercentDialog by remember { mutableStateOf(false) }
    var showClarityPresetDialog by remember { mutableStateOf(false) }
    var showSyncPlaylistsSheet by remember { mutableStateOf(false) }
    var showYtLibraryVisibilitySheet by remember { mutableStateOf(false) }
    var showYtChannelSheet by remember { mutableStateOf(false) }
    var showYtDisconnectConfirm by remember { mutableStateOf(false) }
    var showSpotifyDisconnectConfirm by remember { mutableStateOf(false) }
    var showCookieLoginDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showJamSheet by remember { mutableStateOf(false) }
    var showComponentGallery by remember { mutableStateOf(false) }
    var activeToggleDetail by remember { mutableStateOf<ToggleDetailInfo?>(null) }
    val currentLanguage = remember(misc.appLanguageTag) { AppLanguage.fromTag(misc.appLanguageTag) }

    // Sends the user to Android's own Notification Listener access screen
    // — the one permission this feature needs that the app can never grant

    // "*/*" rather than "application/json": many document providers (Drive,
    // Downloads, some file managers) report a .json file as
    // application/octet-stream or text/plain, and GetContent's mime filter
    // hides anything that doesn't match — the user's own backup file would
    // silently not show up. Real validation happens right after the file is
    // read (stagePendingRestore), so being permissive here is safe.
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.handleRestorePicked(uri)
        }
    }

    val csvPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.handleCsvPicked(uri)
        }
    }

    var showYouTubeImportSheet by remember { mutableStateOf(false) }

    // Lets the user pick exactly where the backup file is saved (SAF), so
    // it's guaranteed to be somewhere restoreLauncher's picker can browse
    // back to later — unlike a silent write into app-private storage.
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.exportBackup(uri, appVersionName(context))
    }

    CompositionLocalProvider(
        LocalToggleDetailViewer provides { activeToggleDetail = it },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .adaptiveContentWidth(maxWidth = 760.dp),
        ) {
        var activeTab by remember { mutableStateOf<SettingsTab?>(null) }
        var isSearchBarVisible by rememberSaveable { mutableStateOf(false) }
        var highlightedSettingId by remember { mutableStateOf<String?>(null) }
        val focusManager = LocalFocusManager.current
        LaunchedEffect(initialSettingId) {
            val entry = initialSettingId?.let { id -> SettingsSearchIndex.allEntries.firstOrNull { it.id == id } }
            if (entry != null) {
                activeTab = entry.parentTab
                highlightedSettingId = entry.id
            }
        }

        val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
        val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

        LaunchedEffect(highlightedSettingId) {
            if (highlightedSettingId != null) {
                delay(3200L)
                highlightedSettingId = null
            }
        }

        BackHandler(enabled = searchQuery.isNotBlank() || isSearchBarVisible || activeTab != null) {
            if (searchQuery.isNotBlank() || isSearchBarVisible) {
                viewModel.clearSearch()
                isSearchBarVisible = false
            } else if (activeTab != null) {
                activeTab = null
            }
        }

        // Pages inside Settings use the dock's fade-through like Settings itself (MainShell's
        // "tabs" transition), with the header part of the page.
        val pageMotion = com.tuned.app.ui.theme.rememberMotion()
        AnimatedContent(
            targetState = activeTab,
            transitionSpec = {
                (fadeIn(pageMotion.base()) + androidx.compose.animation.slideInVertically(pageMotion.base()) { it / 100 })
                    .togetherWith(fadeOut(pageMotion.fast()))
            },
            label = "SettingsTabTransition",
            modifier = Modifier.fillMaxSize(),
        ) { page ->
        Column(Modifier.fillMaxSize()) {
        ExpressiveHeader(
            title = if (searchQuery.isNotBlank()) stringResource(R.string.settings_search_title) else (page?.title ?: stringResource(R.string.settings)),
            onBack = {
                if (searchQuery.isNotBlank() || isSearchBarVisible) {
                    viewModel.clearSearch()
                    isSearchBarVisible = false
                } else if (activeTab != null) {
                    activeTab = null
                } else {
                    onBack()
                }
            },
            actions = {
                HeaderActionIcon(
                    icon = if (isSearchBarVisible || searchQuery.isNotBlank()) Icons.Filled.Close else Icons.Filled.Search,
                    contentDescription = if (isSearchBarVisible || searchQuery.isNotBlank()) stringResource(R.string.settings_search_clear) else stringResource(R.string.settings_search_title),
                    onClick = {
                        if (isSearchBarVisible || searchQuery.isNotBlank()) {
                            viewModel.clearSearch()
                            isSearchBarVisible = false
                        } else {
                            isSearchBarVisible = true
                        }
                    },
                )
            },
        )

        AnimatedVisibility(
            visible = isSearchBarVisible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            SettingsSearchBar(
                query = searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                onClear = {
                    if (searchQuery.isNotBlank()) {
                        viewModel.clearSearch()
                    } else {
                        isSearchBarVisible = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }

        if (searchQuery.isNotBlank()) {
            SettingsSearchResultsList(
                query = searchQuery,
                results = searchResults,
                onResultClick = { entry ->
                    focusManager.clearFocus()
                    viewModel.clearSearch()
                    isSearchBarVisible = false
                    activeTab = entry.parentTab
                    highlightedSettingId = entry.id
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        } else {
            val tab = page
            val listState = remember(tab) { LazyListState() }

            LaunchedEffect(highlightedSettingId, tab) {
                if (highlightedSettingId != null && tab != null) {
                    val targetIndex = getTargetSectionIndex(highlightedSettingId)
                    if (targetIndex > 0) {
                        delay(80L)
                        listState.animateScrollToItem(targetIndex)
                    } else if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0) {
                        delay(80L)
                        listState.animateScrollToItem(0)
                    }
                }
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 32.dp + LocalMiniPlayerScrollClearance.current + safeDrawingBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxSize().safeHorizontalContentPadding(),
            ) {
                when (tab) {
                    null -> {
                        // General first (how the app plays and looks), then accounts with YouTube
                        // Music first and Last.fm last now that Last.fm is optional, then app-level items.
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SectionLabel(stringResource(R.string.settings_label_general))
                                SettingsGroup(rowCount = 3) { index, position ->
                                    when (index) {
                                        0 -> SettingsActionCard(
                                            icon = Icons.Filled.GraphicEq,
                                            iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            title = stringResource(R.string.set_cat_audio),
                                            subtitle = stringResource(R.string.set_cat_audio_sub),
                                            onClick = { activeTab = SettingsTab.AUDIO },
                                            position = position,
                                        )
                                        1 -> SettingsActionCard(
                                            icon = Icons.Filled.Palette,
                                            iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            title = stringResource(R.string.set_cat_appear),
                                            subtitle = stringResource(R.string.set_cat_appear_sub),
                                            onClick = { activeTab = SettingsTab.APPEARANCE },
                                            position = position,
                                        )
                                        else -> SettingsActionCard(
                                            icon = Icons.Filled.QueueMusic,
                                            iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            title = stringResource(R.string.set_cat_lib),
                                            subtitle = run {
                                                val count = downloadCount
                                                if (count > 0) "$count downloaded" else "Home layout, imports and downloads"
                                            },
                                            onClick = { activeTab = SettingsTab.LIBRARY },
                                            position = position,
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SectionLabel(stringResource(R.string.settings_label_accounts))
                                SettingsGroup(rowCount = 3) { index, position ->
                                    when (index) {
                                        0 -> SettingsActionCard(
                                            icon = Icons.Filled.CloudSync,
                                            iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            title = stringResource(R.string.set_cat_yt),
                                            subtitle = if (ytConnection.isConnected) {
                                                "Signed in as ${ytConnection.accountName}"
                                            } else {
                                                "Account, channels and library sync"
                                            },
                                            onClick = { activeTab = SettingsTab.YOUTUBE },
                                            position = position,
                                        )
                                        1 -> SettingsActionCard(
                                            icon = Icons.Filled.PlayCircle,
                                            iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            title = stringResource(R.string.set_cat_spotify),
                                            subtitle = if (spotifyConnected) {
                                                "Connected"
                                            } else {
                                                "Canvas, playlists and liked songs"
                                            },
                                            onClick = { activeTab = SettingsTab.SPOTIFY },
                                            position = position,
                                        )
                                        else -> SettingsActionCard(
                                            icon = Icons.Filled.Album,
                                            iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            title = stringResource(R.string.set_cat_lfm),
                                            subtitle = if (isLastFmConnected) "Scrobbling as ${session.username}" else "Optional",
                                            onClick = { activeTab = SettingsTab.LAST_FM },
                                            position = position,
                                        )
                                    }
                                }
                            }
                        }

                        if (JamRepository.isEnabled) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    SectionLabel(stringResource(R.string.settings_label_listen_together))
                                    SettingsGroup(rowCount = 1) { _, position ->
                                        SettingsActionCard(
                                            icon = Icons.Filled.Group,
                                            iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            title = stringResource(R.string.settings_jam_title),
                                            subtitle = if (jamUiState.isConnected) {
                                                "${jamUiState.participants.size} listening"
                                            } else {
                                                stringResource(R.string.settings_jam_sub)
                                            },
                                            onClick = { showJamSheet = true },
                                            position = position,
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SectionLabel(stringResource(R.string.settings_label_app))
                                SettingsGroup(rowCount = if (AppUpdateManager.isEnabled) 3 else 2) { index, position ->
                                    when (if (AppUpdateManager.isEnabled) index else index + 1) {
                                        0 -> CheckUpdatesRow(
                                            info = updateInfo,
                                            position = position,
                                            isHighlighted = false,
                                            onOpen = { showUpdateSheet = true },
                                            onCheck = viewModel::checkForUpdates,
                                        )
                                        1 -> SettingsActionCard(
                                            icon = Icons.Filled.Extension,
                                            iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            title = stringResource(R.string.settings_modules_title),
                                            subtitle = stringResource(R.string.settings_modules_sub),
                                            onClick = onOpenModules,
                                            position = position,
                                            isHighlighted = (highlightedSettingId == "services.modules" || highlightedSettingId == "library.modules"),
                                        )
                                        else -> SettingsActionCard(
                                            icon = Icons.Filled.Backup,
                                            iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            title = stringResource(R.string.set_cat_data),
                                            subtitle = stringResource(R.string.set_cat_data_sub),
                                            onClick = { activeTab = SettingsTab.DATA_BACKUP },
                                            position = position,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    SettingsTab.AUDIO -> {
                        item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel(stringResource(R.string.settings_section_audio))
                    val qualitySubtitle = when (misc.losslessQuality) {
                        28 -> "Dolby Atmos"
                        27 -> "Max"
                        7 -> "Hi-Res"
                        6 -> "CD"
                        5 -> "Standard"
                        4 -> "Data saver"
                        -1 -> "YouTube Music"
                        else -> "Max"
                    }
                    val downloadQualitySubtitle = when (misc.downloadQuality) {
                        28 -> "Dolby Atmos"
                        27 -> "Hi-Res"
                        7 -> "Hi-Res"
                        6 -> "CD"
                        5 -> "Standard"
                        4 -> "Data saver"
                        -1 -> "YouTube Music"
                        else -> "Hi-Res"
                    }

                    val isIgnored = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
                    val totalAudioRows = if (misc.crossfadeEnabled) {
                        if (isIgnored) 7 else 8
                    } else {
                        if (isIgnored) 6 else 7
                    }
                    SettingsGroup(rowCount = totalAudioRows) { index, position ->
                        when (index) {
                            0 -> SettingsActionCard(
                                icon = Icons.Filled.HighQuality,
                                iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                title = stringResource(R.string.settings_streaming_quality),
                                subtitle = if (misc.losslessQuality == -1) "Plays YouTube's own stream" else qualitySubtitle,
                                onClick = { showQualityDialog = true },
                                position = position,
                                isHighlighted = (highlightedSettingId == "audio.streaming_quality"),
                            )
                            1 -> SettingsActionCard(
                                icon = Icons.Filled.CloudDownload,
                                iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                title = stringResource(R.string.settings_download_quality),
                                subtitle = downloadQualitySubtitle,
                                onClick = { showDownloadQualityDialog = true },
                                position = position,
                                isHighlighted = (highlightedSettingId == "audio.download_quality"),
                            )
                            2 -> SettingsToggleCard(
                                icon = Icons.Filled.HighQuality,
                                iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                title = stringResource(R.string.dl_filter_lossless),
                                subtitle = if (misc.preferLosslessStreaming) {
                                    "On"
                                } else {
                                    "Off"
                                },
                                checked = misc.preferLosslessStreaming,
                                onCheckedChange = viewModel::setPreferLosslessStreaming,
                                position = position,
                                isHighlighted = (highlightedSettingId == "audio.prefer_lossless"),
                            )
                            3 -> SettingsToggleCard(
                                icon = Icons.Filled.GraphicEq,
                                iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
                                iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                                title = stringResource(R.string.settings_song_transitions),
                                subtitle = when {
                                    misc.crossfadeEnabled && misc.isBitPerfectEnabled -> stringResource(R.string.settings_transitions_bitperfect)
                                    misc.crossfadeEnabled && misc.autoMixEnabled -> stringResource(R.string.settings_automix)
                                    misc.crossfadeEnabled -> stringResource(R.string.settings_transitions_crossfade_seconds, misc.crossfadeSeconds)
                                    else -> stringResource(R.string.settings_transitions_off)
                                },
                                checked = misc.crossfadeEnabled,
                                onCheckedChange = viewModel::setCrossfadeEnabled,
                                position = position,
                                isHighlighted = (highlightedSettingId == "audio.crossfade"),
                            )
                            4 -> if (misc.crossfadeEnabled) {
                                CrossfadeDurationRow(
                                    seconds = misc.crossfadeSeconds,
                                    onSecondsChange = viewModel::setCrossfadeSeconds,
                                    autoMix = misc.autoMixEnabled,
                                    onAutoMixChange = viewModel::setAutoMixEnabled,
                                    position = position,
                                    isHighlighted = (highlightedSettingId == "audio.crossfade_duration"),
                                )
                            } else {
                                SettingsToggleCard(
                                    icon = Icons.Filled.Lyrics,
                                    iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                    iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    title = stringResource(R.string.settings_keep_lyrics_screen_awake),
                                    subtitle = stringResource(R.string.settings_keep_lyrics_screen_awake_desc),
                                    checked = misc.keepLyricsScreenAwake,
                                    onCheckedChange = viewModel::setKeepLyricsScreenAwake,
                                    position = position,
                                    isHighlighted = (highlightedSettingId == "audio.keep_lyrics_screen_awake"),
                                )
                            }
                            5 -> if (misc.crossfadeEnabled) {
                                SettingsToggleCard(
                                    icon = Icons.Filled.Lyrics,
                                    iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                    iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    title = stringResource(R.string.settings_keep_lyrics_screen_awake),
                                    subtitle = stringResource(R.string.settings_keep_lyrics_screen_awake_desc),
                                    checked = misc.keepLyricsScreenAwake,
                                    onCheckedChange = viewModel::setKeepLyricsScreenAwake,
                                    position = position,
                                    isHighlighted = (highlightedSettingId == "audio.keep_lyrics_screen_awake"),
                                )
                            } else {
                                SettingsToggleCard(
                                    icon = Icons.Filled.Lyrics,
                                    iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                    iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    title = stringResource(R.string.settings_download_lyrics),
                                    subtitle = if (misc.downloadLyrics) {
                                        "On"
                                    } else {
                                        "Off"
                                    },
                                    checked = misc.downloadLyrics,
                                    onCheckedChange = viewModel::setDownloadLyrics,
                                    position = position,
                                    isHighlighted = (highlightedSettingId == "audio.download_lyrics"),
                                )
                            }
                            6 -> if (misc.crossfadeEnabled) {
                                SettingsToggleCard(
                                    icon = Icons.Filled.Lyrics,
                                    iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                    iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    title = stringResource(R.string.settings_download_lyrics),
                                    subtitle = if (misc.downloadLyrics) {
                                        "On"
                                    } else {
                                        "Off"
                                    },
                                    checked = misc.downloadLyrics,
                                    onCheckedChange = viewModel::setDownloadLyrics,
                                    position = position,
                                    isHighlighted = (highlightedSettingId == "audio.download_lyrics"),
                                )
                            } else if (!isIgnored) {
                                SettingsActionCard(
                                    icon = Icons.Filled.Bolt,
                                    iconContainer = MaterialTheme.colorScheme.errorContainer,
                                    iconTint = MaterialTheme.colorScheme.onErrorContainer,
                                    title = stringResource(R.string.settings_battery_title),
                                    subtitle = stringResource(R.string.set_battery_restricted),
                                    onClick = { BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context) },
                                    position = position,
                                    isHighlighted = (highlightedSettingId == "audio.battery_optimization"),
                                )
                            }
                            7 -> if (!isIgnored) {
                                SettingsActionCard(
                                    icon = Icons.Filled.Bolt,
                                    iconContainer = MaterialTheme.colorScheme.errorContainer,
                                    iconTint = MaterialTheme.colorScheme.onErrorContainer,
                                    title = stringResource(R.string.settings_battery_title),
                                    subtitle = stringResource(R.string.set_battery_restricted),
                                    onClick = { BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context) },
                                    position = position,
                                    isHighlighted = (highlightedSettingId == "audio.battery_optimization"),
                                )
                            }
                        }
                    }
                }
            }

                        item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel(stringResource(R.string.settings_section_bitperfect))
                    AdvancedAudioWarning()
                    SettingsGroup(rowCount = 5) { index, position ->
                        when (index) {
                            0 -> SettingsToggleCard(
                                icon = Icons.Filled.Tune,
                                iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                title = stringResource(R.string.settings_bit_perfect),
                                subtitle = if (misc.isBitPerfectEnabled) {
                                    stringResource(R.string.settings_bit_perfect_on_detail)
                                } else {
                                    stringResource(R.string.settings_bit_perfect_off_detail)
                                },
                                checked = misc.isBitPerfectEnabled,
                                onCheckedChange = viewModel::setBitPerfectEnabled,
                                position = position,
                                isHighlighted = (highlightedSettingId == "audio.bit_perfect"),
                            )
                            1 -> SettingsToggleCard(
                                icon = Icons.Filled.Usb,
                                iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                title = stringResource(R.string.set_usb_title),
                                subtitle = if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
                                    stringResource(R.string.set_usb_old)
                                } else if (usbExclusiveEnabled || misc.isBitPerfectEnabled) {
                                    stringResource(R.string.set_usb_on)
                                } else {
                                    stringResource(R.string.set_usb_off)
                                },
                                checked = usbExclusiveEnabled,
                                onCheckedChange = viewModel::setUsbExclusiveEnabled,
                                position = position,
                                isHighlighted = (highlightedSettingId == "audio.usb_exclusive"),
                            )
                            2 -> {
                                // Probe what this output accepts only once the row is on screen.
                                LaunchedEffect(Unit) { viewModel.refreshInternalDirectAvailability() }
                                SettingsToggleCard(
                                icon = Icons.Filled.SpeakerPhone,
                                iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
                                iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                                title = stringResource(R.string.settings_internal_direct),
                                subtitle = when {
                                    !internalDirectEnabled -> stringResource(R.string.settings_internal_direct_off)
                                    internalDirectAvailability.available ->
                                        stringResource(R.string.settings_internal_direct_on)
                                    else -> stringResource(
                                        R.string.settings_internal_direct_inactive,
                                        stringResource(com.tuned.app.playback.internalFailureDetail(internalDirectAvailability.reason)),
                                    )
                                },
                                checked = internalDirectEnabled,
                                // Greyed out only when this device can never bypass the mixer.
                                // A route- or format-specific refusal must not lock the toggle:
                                // turning it off used to grey it out until the app restarted.
                                enabled = internalDirectEnabled || internalDirectAvailability.canEnable,
                                onCheckedChange = viewModel::setInternalDirectEnabled,
                                position = position,
                                isHighlighted = (highlightedSettingId == "audio.internal_direct"),
                            )
                            }
                            3 -> SettingsToggleCard(
                                icon = Icons.Filled.VolumeUp,
                                iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                title = stringResource(R.string.settings_force_sw_volume),
                                subtitle = if (forceSoftwareVolume) {
                                    stringResource(R.string.settings_force_sw_volume_on)
                                } else {
                                    stringResource(R.string.settings_force_sw_volume_off)
                                },
                                checked = forceSoftwareVolume,
                                onCheckedChange = viewModel::setForceSoftwareVolume,
                                position = position,
                                isHighlighted = (highlightedSettingId == "audio.force_software_volume"),
                            )
                            4 -> SettingsToggleCard(
                                icon = Icons.Filled.GraphicEq,
                                iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                title = stringResource(R.string.settings_tuning_432),
                                subtitle = if (misc.tuning432Enabled) {
                                    stringResource(R.string.settings_tuning_432_on)
                                } else {
                                    stringResource(R.string.settings_tuning_432_off)
                                },
                                checked = misc.tuning432Enabled,
                                onCheckedChange = viewModel::setTuning432Enabled,
                                position = position,
                                isHighlighted = (highlightedSettingId == "audio.tuning_432"),
                            )
                        }
                    }
                }
            }

                        item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel(stringResource(R.string.settings_label_output))
                    val clarityPreset = remember(misc.clarityPreset) { ClarityPresets.fromIndex(misc.clarityPreset) }
                    val loudnessSubtitle = when (loudness.mode) {
                        LoudnessMode.TRACK -> "Per song"
                        LoudnessMode.ALBUM -> "Per album"
                        else -> "Off"
                    }
                    SettingsGroup(rowCount = 7) { index, position ->
                        when (index) {
                            0 -> SettingsActionCard(
                                icon = Icons.Filled.VolumeUp,
                                iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                title = stringResource(R.string.set_loud_title),
                                subtitle = loudnessSubtitle,
                                onClick = { showLoudnessDialog = true },
                                position = position,
                                isHighlighted = (highlightedSettingId == "audio.loudness_normalization"),
                            )
                            1 -> SettingsActionCard(
                                icon = Icons.Filled.GraphicEq,
                                iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
                                iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                                title = stringResource(R.string.settings_equalizer),
                                subtitle = if (misc.isBitPerfectEnabled && eq.enabled) {
                                    stringResource(R.string.set_eq_on_preset, eq.presetName)
                                } else if (eq.enabled) {
                                    stringResource(R.string.set_eq_on_band, eq.presetName)
                                } else {
                                    "Off"
                                },
                                onClick = { showEqSheet = true },
                                position = position,
                                isHighlighted = (highlightedSettingId == "audio.equalizer"),
                            )
                            2 -> SettingsToggleCard(
                                icon = Icons.Filled.VolumeUp,
                                iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
                                iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                                title = stringResource(R.string.set_sys_fx_title),
                                subtitle = if (misc.systemEffectsMode) {
                                    stringResource(R.string.set_sys_fx_on)
                                } else {
                                    stringResource(R.string.set_sys_fx_off)
                                },
                                checked = misc.systemEffectsMode,
                                onCheckedChange = viewModel::setSystemEffectsMode,
                                position = position,
                                isHighlighted = (highlightedSettingId == "audio.system_audio_effects"),
                            )
                            3 -> SettingsToggleCard(
                                icon = Icons.Filled.Waves,
                                iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                title = stringResource(R.string.settings_studio_clarity),
                                subtitle = if (misc.isStudioMasterClarityEnabled) {
                                    stringResource(R.string.settings_studio_clarity_on)
                                } else {
                                    stringResource(R.string.settings_studio_clarity_off)
                                },
                                checked = misc.isStudioMasterClarityEnabled,
                                onCheckedChange = viewModel::setStudioMasterClarity,
                                position = position,
                                isHighlighted = (highlightedSettingId == "audio.studio_clarity"),
                            )
                            4 -> SettingsActionCard(
                                icon = Icons.Filled.Tune,
                                iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
                                iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                                title = stringResource(R.string.set_clarity_title),
                                subtitle = stringResource(
                                    R.string.set_clarity_row,
                                    clarityPresetName(clarityPreset),
                                    clarityPresetDescription(clarityPreset),
                                ),
                                onClick = { showClarityPresetDialog = true },
                                position = position,
                                isHighlighted = (highlightedSettingId == "audio.clarity_preset"),
                            )
                            5 -> SettingsToggleCard(
                                icon = Icons.Filled.GraphicEq,
                                iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                title = stringResource(R.string.set_clarity_bypass),
                                subtitle = if (misc.clarityAtmosBypass) {
                                    "On"
                                } else {
                                    "Off"
                                },
                                checked = misc.clarityAtmosBypass,
                                onCheckedChange = viewModel::setClarityAtmosBypass,
                                position = position,
                                isHighlighted = (highlightedSettingId == "audio.clarity_spatial_bypass"),
                            )
                            6 -> SettingsActionCard(
                                icon = Icons.Filled.Headphones,
                                iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                title = stringResource(R.string.autoeq_title),
                                subtitle = com.tuned.app.ui.settings.autoeq.autoEqSummary(),
                                onClick = { showAutoEq = true },
                                position = position,
                                isHighlighted = (highlightedSettingId == "audio.autoeq"),
                            )
                        }
                    }
                }
            }
                    }

                    SettingsTab.APPEARANCE -> {
                        item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel(stringResource(R.string.settings_section_language))
                    SettingsGroup(rowCount = 1) { _, position ->
                        SettingsActionCard(
                            icon = Icons.Filled.Language,
                            iconContainer = MaterialTheme.colorScheme.primaryContainer,
                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                            title = stringResource(R.string.settings_language_title),
                            subtitle = currentLanguage.nativeDisplayName(),
                            onClick = { showLanguageDialog = true },
                            position = position,
                            isHighlighted = (highlightedSettingId == "about.language"),
                        )
                    }
                }
            }
                        item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel(stringResource(R.string.settings_section_appearance))
                    SettingsGroup(rowCount = 9) { index, position ->
                        when (index) {
                            0 -> ThemeModeSelectorCard(
                                currentThemeMode = theme?.themeMode ?: ThemeMode.SYSTEM,
                                onSelectThemeMode = viewModel::setThemeMode,
                                position = position,
                                isHighlighted = (highlightedSettingId == "appearance.theme_mode"),
                            )
                            1 -> SettingsToggleCard(
                                icon = Icons.Filled.Contrast,
                                iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
                                iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                                title = stringResource(R.string.settings_amoled),
                                subtitle = stringResource(R.string.settings_amoled_sub),
                                checked = theme?.amoled ?: false,
                                enabled = theme?.themeMode != ThemeMode.LIGHT,
                                onCheckedChange = viewModel::setAmoled,
                                position = position,
                                isHighlighted = (highlightedSettingId == "appearance.amoled"),
                            )
                            2 -> SettingsToggleCard(
                                icon = Icons.Filled.Palette,
                                iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                title = stringResource(R.string.settings_dynamic_color),
                                subtitle = stringResource(R.string.settings_dynamic_color_sub),
                                checked = theme?.mode == AccentMode.DYNAMIC,
                                onCheckedChange = { enabled ->
                                    viewModel.setAccentMode(if (enabled) AccentMode.DYNAMIC else AccentMode.MANUAL)
                                },
                                position = position,
                                isHighlighted = (highlightedSettingId == "appearance.dynamic_color"),
                            )
                            3 -> SettingsToggleCard(
                                icon = Icons.Filled.BubbleChart,
                                iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                title = stringResource(R.string.settings_liquid_glass),
                                subtitle = stringResource(R.string.settings_liquid_glass_sub),
                                checked = theme?.liquidGlass ?: false,
                                onCheckedChange = viewModel::setLiquidGlass,
                                position = position,
                                isHighlighted = (highlightedSettingId == "appearance.liquid_glass"),
                            )
                            4 -> SettingsToggleCard(
                                icon = Icons.Filled.VisibilityOff,
                                iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                title = stringResource(R.string.settings_hide_nav_tabs),
                                subtitle = stringResource(R.string.settings_hide_nav_tabs_sub),
                                checked = theme?.hideNavTabs ?: false,
                                onCheckedChange = viewModel::setHideNavTabs,
                                position = position,
                                isHighlighted = (highlightedSettingId == "appearance.hide_nav_tabs"),
                            )
                            5 -> SettingsToggleCard(
                                icon = Icons.Filled.Widgets,
                                iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                title = stringResource(R.string.settings_md3_nav_bar),
                                subtitle = stringResource(R.string.settings_md3_nav_bar_sub),
                                checked = theme?.md3NavBar ?: false,
                                onCheckedChange = viewModel::setMd3NavBar,
                                position = position,
                                isHighlighted = (highlightedSettingId == "appearance.md3_nav_bar"),
                            )
                            6 -> SettingsToggleCard(
                                icon = Icons.Filled.Album,
                                iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
                                iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                                title = stringResource(R.string.settings_dynamic_now_playing),
                                subtitle = stringResource(R.string.settings_dynamic_now_playing_sub),
                                checked = misc.dynamicNowPlayingEnabled,
                                onCheckedChange = viewModel::setDynamicNowPlaying,
                                position = position,
                                isHighlighted = (highlightedSettingId == "appearance.dynamic_now_playing"),
                            )
                            7 -> SettingsToggleCard(
                                icon = Icons.Filled.TextFields,
                                iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                title = stringResource(R.string.settings_app_font),
                                subtitle = stringResource(R.string.settings_app_font_sub),
                                checked = misc.useCustomFont,
                                onCheckedChange = viewModel::setUseCustomFont,
                                position = position,
                                isHighlighted = (highlightedSettingId == "appearance.app_font"),
                            )
                            8 -> SettingsActionCard(
                                icon = Icons.Filled.Dashboard,
                                iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                title = stringResource(R.string.settings_home_sections),
                                subtitle = run {
                                    val total = com.tuned.app.data.local.HomeSection.entries.size
                                    val visible = total - misc.hiddenHomeSections.size
                                    stringResource(R.string.home_sections_visible, visible, total)
                                },
                                onClick = onOpenHomeSections,
                                position = position,
                                isHighlighted = (highlightedSettingId == "appearance.home_sections"),
                            )
                        }
                    }
                }
            }

                        item {
                Column {
                    SectionLabel(stringResource(R.string.settings_section_accent))
                    Spacer(Modifier.height(10.dp))
                    Card(
                        shape = CardOuterShape,
                        colors = CardDefaults.cardColors(
                            containerColor = if (highlightedSettingId == "appearance.accent_preset") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else androidx.compose.ui.graphics.Color.Transparent
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .settingHighlightGlow(
                                isHighlighted = (highlightedSettingId == "appearance.accent_preset"),
                                shape = CardOuterShape,
                            ),
                    ) {
                        // No card fill: the swatches sit on the page like the rows above.
                        Column(Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                            AccentPresetGrid(
                                currentMode = theme?.mode ?: AccentMode.MANUAL,
                                selectedHex = theme?.accentColorHex,
                                onPickPreset = { hex ->
                                    viewModel.setManualAccent(Color(android.graphics.Color.parseColor(hex)))
                                },
                                onPickMono = {
                                    viewModel.setAccentMode(AccentMode.MONOCHROME)
                                },
                                onPickCustom = viewModel::openColorWheel,
                            )
                        }
                    }
                }
            }

                        item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel(stringResource(R.string.settings_canvas_enabled))
                    val canvasRowCount = if (misc.canvasEnabled) 3 else 1
                    SettingsGroup(rowCount = canvasRowCount) { index, position ->
                        when (index) {
                            0 -> SettingsToggleCard(
                                icon = Icons.Filled.SmartDisplay,
                                iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                title = stringResource(R.string.settings_canvas_enabled),
                                subtitle = stringResource(R.string.settings_canvas_enabled_sub),
                                checked = misc.canvasEnabled,
                                onCheckedChange = viewModel::setCanvasEnabled,
                                position = position,
                                isHighlighted = (highlightedSettingId == "appearance.canvas_enabled"),
                            )
                            1 -> SettingsToggleCard(
                                icon = Icons.Filled.Visibility,
                                iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                title = stringResource(R.string.settings_canvas_full_bleed),
                                subtitle = stringResource(R.string.settings_canvas_full_bleed_sub),
                                checked = misc.canvasFullBleed,
                                onCheckedChange = viewModel::setCanvasFullBleed,
                                position = position,
                                isHighlighted = (highlightedSettingId == "appearance.canvas_full_bleed"),
                            )
                            2 -> SettingsToggleCard(
                                icon = Icons.Filled.CloudDownload,
                                iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
                                iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                                title = stringResource(R.string.settings_canvas_cellular),
                                subtitle = stringResource(R.string.settings_canvas_cellular_sub),
                                checked = misc.canvasOverCellular,
                                onCheckedChange = viewModel::setCanvasOverCellular,
                                position = position,
                                isHighlighted = (highlightedSettingId == "appearance.canvas_cellular"),
                            )
                        }
                    }
                }
            }

                        item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel(stringResource(R.string.settings_section_experimental))
                    SettingsGroup(rowCount = if (BuildConfig.DEBUG) 12 else 10) { index, position ->
                        when (index) {
                            0 -> SettingsToggleCard(
                                icon = Icons.Filled.Lyrics,
                                iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
                                iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                                title = "Word-by-word lyrics",
                                subtitle = if (misc.wordByWordLyrics) {
                                    "Each word lights up as it's sung"
                                } else {
                                    "One line at a time"
                                },
                                checked = misc.wordByWordLyrics,
                                onCheckedChange = viewModel::setWordByWordLyrics,
                                position = position,
                                isHighlighted = (highlightedSettingId == "appearance.word_by_word_lyrics"),
                            )
                            1 -> SettingsToggleCard(
                                icon = Icons.Filled.Waves,
                                iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                title = stringResource(R.string.settings_wavy_seekbar),
                                subtitle = if (misc.wavySeekbarEnabled) {
                                    stringResource(R.string.settings_wavy_seekbar_on)
                                } else {
                                    stringResource(R.string.settings_wavy_seekbar_off)
                                },
                                checked = misc.wavySeekbarEnabled,
                                onCheckedChange = viewModel::setWavySeekbarEnabled,
                                position = position,
                                isHighlighted = (highlightedSettingId == "appearance.wavy_seekbar"),
                            )
                            2 -> SettingsToggleCard(
                                icon = Icons.Filled.RestartAlt,
                                iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                title = stringResource(R.string.settings_rotating_background),
                                subtitle = if (misc.rotatingBackgroundEnabled) {
                                    stringResource(R.string.settings_rotating_background_on)
                                } else {
                                    stringResource(R.string.settings_rotating_background_off)
                                },
                                checked = misc.rotatingBackgroundEnabled,
                                onCheckedChange = viewModel::setRotatingBackgroundEnabled,
                                position = position,
                                isHighlighted = (highlightedSettingId == "appearance.rotating_background"),
                            )
                            3 -> SettingsToggleCard(
                                icon = Icons.Filled.RestartAlt,
                                iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                title = stringResource(R.string.settings_lyrics_rotating_background),
                                subtitle = if (misc.lyricsRotatingBackgroundEnabled) {
                                    stringResource(R.string.settings_lyrics_rotating_background_on)
                                } else {
                                    stringResource(R.string.settings_lyrics_rotating_background_off)
                                },
                                checked = misc.lyricsRotatingBackgroundEnabled,
                                onCheckedChange = viewModel::setLyricsRotatingBackgroundEnabled,
                                position = position,
                                isHighlighted = (highlightedSettingId == "appearance.lyrics_rotating_background"),
                            )
                            4 -> SettingsActionCard(
                                icon = Icons.Filled.Lyrics,
                                iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
                                iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                                title = stringResource(R.string.settings_lyrics_provider),
                                subtitle = misc.lyricsProvider.title,
                                onClick = { showLyricsProviderDialog = true },
                                position = position,
                                isHighlighted = (highlightedSettingId == "appearance.lyrics_provider"),
                            )
                            5 -> SettingsActionCard(
                                icon = Icons.Filled.Timer,
                                iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                title = stringResource(R.string.set_lyrics_offset_title),
                                subtitle = if (misc.lyricsOffsetMs == 0L) {
                                    stringResource(R.string.set_offset_off)
                                } else {
                                    stringResource(
                                        R.string.set_offset_value,
                                        if (misc.lyricsOffsetMs > 0) "+" else "",
                                        misc.lyricsOffsetMs,
                                    )
                                },
                                onClick = { showLyricsOffsetDialog = true },
                                position = position,
                                isHighlighted = (highlightedSettingId == "appearance.lyrics_offset"),
                            )
                            6 -> SettingsActionCard(
                                icon = Icons.Filled.FormatSize,
                                iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                title = stringResource(R.string.set_lyrics_size_title),
                                subtitle = stringResource(
                                    R.string.set_size_value,
                                    (misc.lyricsFontScale * 100).roundToInt(),
                                    when {
                                        misc.lyricsFontScale < 0.85f -> stringResource(R.string.set_size_small)
                                        misc.lyricsFontScale in 0.85f..1.15f -> stringResource(R.string.set_size_standard)
                                        misc.lyricsFontScale in 1.16f..1.35f -> stringResource(R.string.set_size_large)
                                        else -> stringResource(R.string.set_size_xlarge)
                                    },
                                ),
                                onClick = { showLyricsSizeDialog = true },
                                position = position,
                                isHighlighted = (highlightedSettingId == "appearance.lyrics_size"),
                            )
                            7 -> SettingsActionCard(
                                icon = Icons.Filled.VisibilityOff,
                                iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                title = stringResource(R.string.set_lyrics_autohide_title),
                                subtitle = if (misc.lyricsButtonsAutoHideSeconds == 0) {
                                    stringResource(R.string.set_lyrics_autohide_never)
                                } else {
                                    stringResource(
                                        R.string.set_lyrics_autohide_value,
                                        misc.lyricsButtonsAutoHideSeconds,
                                    )
                                },
                                onClick = { showLyricsAutoHideDialog = true },
                                position = position,
                                isHighlighted = (highlightedSettingId == "appearance.lyrics_autohide"),
                            )
                            8 -> SettingsActionCard(
                                icon = Icons.Filled.Translate,
                                iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                title = stringResource(R.string.set_translation_language_title),
                                subtitle = if (misc.translationLanguage.isBlank()) {
                                    stringResource(R.string.set_translation_language_follows)
                                } else {
                                    translationLanguageName(misc.translationLanguage, Locale.getDefault())
                                },
                                onClick = { showTranslationLanguageDialog = true },
                                position = position,
                                isHighlighted = (highlightedSettingId == "appearance.translation_language"),
                            )
                            9 -> SettingsToggleCard(
                                icon = Icons.Filled.SmartDisplay,
                                iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                title = stringResource(R.string.settings_music_video),
                                subtitle = if (misc.musicVideoEnabled) {
                                    stringResource(R.string.settings_music_video_on)
                                } else {
                                    stringResource(R.string.settings_music_video_off)
                                },
                                checked = misc.musicVideoEnabled,
                                onCheckedChange = viewModel::setMusicVideoEnabled,
                                position = position,
                                isHighlighted = (highlightedSettingId == "appearance.music_video"),
                            )
                            10 -> SettingsToggleCard(
                                icon = Icons.Filled.GridOn,
                                iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                title = "Layout grid",
                                subtitle = "Debug only",
                                checked = theme?.designGrid ?: false,
                                onCheckedChange = viewModel::setDesignGrid,
                                position = position,
                            )
                            11 -> SettingsActionCard(
                                icon = Icons.Filled.Widgets,
                                iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                title = "Component gallery",
                                subtitle = "Debug only",
                                onClick = { showComponentGallery = true },
                                position = position,
                            )
                        }
                    }
                }
            }

                    }

                    SettingsTab.YOUTUBE -> {
                        item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel(stringResource(R.string.settings_section_youtube))
                    val ytConnected = ytConnection.isConnected
                    val syncSubtitle = when (val sync = ytSyncState) {
                        is com.tuned.app.data.ytmusic.YtSyncState.Running ->
                            "Syncing ${sync.current} of ${sync.total}"
                        is com.tuned.app.data.ytmusic.YtSyncState.Completed ->
                            "Synced ${relativeTime(sync.atMillis)}"
                        is com.tuned.app.data.ytmusic.YtSyncState.Failed ->
                            "Last sync failed"
                        else ->
                            if (!ytConnected) "Sign in first"
                            else if (ytSyncEnabled) lastSyncLabel(ytLastSyncAt) ?: "Chosen playlists stay in sync with your account"
                            else "Keep your YouTube Music playlists and Tuned the same"
                    }
                    // Display name of the active channel: the picked entry when
                    // the channel list was loaded, otherwise the stored name.
                    val currentChannelName = ytChannels.firstOrNull {
                        it.channelId == ytConnection.onBehalfOfUser &&
                            it.authUserIndex == ytConnection.authUserIndex &&
                            it.pageId == ytConnection.pageId
                    }?.accountName ?: ytConnection.accountName
                    val ytRowCount = if (ytConnected) 7 else 3
                    SettingsGroup(rowCount = ytRowCount) { index, position ->
                        if (!ytConnected) {
                            when (index) {
                                0 -> SettingsActionCard(
                                    icon = Icons.Filled.CloudSync,
                                    iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    title = stringResource(R.string.settings_connect_yt),
                                    subtitle = stringResource(R.string.settings_connect_yt_sub),
                                    onClick = onOpenYouTubeLogin,
                                    position = position,
                                    isHighlighted = (highlightedSettingId == "youtube.account"),
                                )
                                1 -> SettingsActionCard(
                                    icon = Icons.Filled.Key,
                                    iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                    iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    title = stringResource(R.string.settings_cookie_login),
                                    subtitle = stringResource(R.string.settings_cookie_login_sub),
                                    onClick = { showCookieLoginDialog = true },
                                    position = position,
                                    isHighlighted = (highlightedSettingId == "youtube.cookie_login"),
                                )
                                2 -> SettingsToggleCard(
                                    icon = Icons.Filled.CloudSync,
                                    iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                    iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    title = stringResource(R.string.settings_yt_sync),
                                    subtitle = syncSubtitle,
                                    checked = false,
                                    onCheckedChange = viewModel::setYtSyncEnabled,
                                    position = position,
                                    isHighlighted = (highlightedSettingId == "youtube.sync"),
                                )
                            }
                        } else {
                            when (index) {
                                0 -> YouTubeAccountRow(
                                    accountName = ytConnection.accountName,
                                    channelHandle = ytConnection.channelHandle,
                                    onDisconnect = { showYtDisconnectConfirm = true },
                                    position = position,
                                    isHighlighted = (highlightedSettingId == "youtube.account"),
                                )
                                1 -> SettingsToggleCard(
                                    icon = Icons.Filled.CloudSync,
                                    iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                    iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    title = stringResource(R.string.settings_yt_sync),
                                    subtitle = syncSubtitle,
                                    checked = ytSyncEnabled,
                                    onCheckedChange = viewModel::setYtSyncEnabled,
                                    position = position,
                                    isHighlighted = (highlightedSettingId == "youtube.sync"),
                                )
                                2 -> SettingsActionCard(
                                    icon = Icons.Filled.SwitchAccount,
                                    iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                    iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    title = stringResource(R.string.set_yt_channel_row),
                                    subtitle = currentChannelName.ifBlank { stringResource(R.string.set_channel_default) },
                                    onClick = {
                                        viewModel.loadYtChannels()
                                        showYtChannelSheet = true
                                    },
                                    position = position,
                                    isHighlighted = (highlightedSettingId == "youtube.channel"),
                                )
                            3 -> if (ytConnected) {
                                val selectedCount = syncedPlaylistIds?.size ?: allPlaylists.size
                                val syncCountText = if (syncedPlaylistIds == null || selectedCount == allPlaylists.size) {
                                    stringResource(R.string.set_sync_all, allPlaylists.size)
                                } else {
                                    stringResource(R.string.set_sync_partial, selectedCount, allPlaylists.size)
                                }
                                SettingsActionCard(
                                    icon = Icons.Filled.FormatListBulleted,
                                    iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
                                    iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    title = stringResource(R.string.settings_select_playlists),
                                    subtitle = syncCountText,
                                    onClick = { showSyncPlaylistsSheet = true },
                                    position = position,
                                    isHighlighted = (highlightedSettingId == "youtube.select_playlists"),
                                )
                            } else {
                                SettingsActionCard(
                                    icon = Icons.Filled.QueueMusic,
                                    iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
                                    iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    title = stringResource(R.string.set_import_yt),
                                    subtitle = stringResource(R.string.set_import_yt_sub),
                                    onClick = onOpenYouTubeImport,
                                    position = position,
                                    isHighlighted = (highlightedSettingId == "youtube.import"),
                                )
                            }
                            4 -> if (ytConnected) {
                                val shownCount = ytAccountPlaylists.count { it.id !in hiddenYtLibraryPlaylistIds }
                                val visibilitySubtitle = if (shownCount == ytAccountPlaylists.size) {
                                    stringResource(R.string.set_yt_shown_all, ytAccountPlaylists.size)
                                } else {
                                    stringResource(R.string.set_yt_shown_partial, shownCount, ytAccountPlaylists.size)
                                }
                                SettingsActionCard(
                                    icon = Icons.Filled.Visibility,
                                    iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                    iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    title = stringResource(R.string.settings_yt_shown),
                                    subtitle = visibilitySubtitle,
                                    onClick = { showYtLibraryVisibilitySheet = true },
                                    position = position,
                                    isHighlighted = (highlightedSettingId == "youtube.shown_playlists"),
                                )
                            }
                            5 -> SettingsActionCard(
                                icon = Icons.Filled.QueueMusic,
                                iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                title = stringResource(R.string.settings_make_local),
                                subtitle = stringResource(R.string.settings_make_local_sub),
                                onClick = onOpenYouTubeImport,
                                position = position,
                                isHighlighted = (highlightedSettingId == "youtube.import"),
                            )
                            6 -> if (ytConnected) {
                                SettingsToggleCard(
                                    icon = Icons.Filled.History,
                                    iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
                                    iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    title = stringResource(R.string.settings_yt_history),
                                    subtitle = if (ytHistorySyncEnabled) {
                                        stringResource(R.string.set_yt_hist_on)
                                    } else {
                                        stringResource(R.string.set_yt_hist_off)
                                    },
                                    checked = ytHistorySyncEnabled,
                                    onCheckedChange = viewModel::setYtHistorySyncEnabled,
                                    position = position,
                                    isHighlighted = (highlightedSettingId == "youtube.history"),
                                )
                            }
                        }
                    }
                }
            }
        }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SectionLabel(stringResource(R.string.settings_label_liked))
                                val likedLocalSync by viewModel.likedSongsLocalSync.collectAsStateWithLifecycle()
                                val likedYouTubeSync by viewModel.likedSongsYouTubeSync.collectAsStateWithLifecycle()
                                SettingsGroup(rowCount = 2) { index, position ->
                                    when (index) {
                                        0 -> SettingsToggleCard(
                                            icon = Icons.Filled.Favorite,
                                            iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            title = "Liked songs in Tuned",
                                            subtitle = "Hearts save songs to a Liked Songs playlist",
                                            checked = likedLocalSync,
                                            onCheckedChange = viewModel::setLikedSongsLocalSync,
                                            position = position,
                                            isHighlighted = (highlightedSettingId == "liked.local"),
                                        )
                                        else -> SettingsToggleCard(
                                            icon = Icons.Filled.CloudSync,
                                            iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                            iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            title = "Like on YouTube Music too",
                                            subtitle = if (ytConnection.isConnected) {
                                                "Liking a song here likes it there"
                                            } else {
                                                "Sign in to YouTube Music first"
                                            },
                                            checked = likedYouTubeSync,
                                            onCheckedChange = viewModel::setLikedSongsYouTubeSync,
                                            position = position,
                                            isHighlighted = (highlightedSettingId == "liked.youtube"),
                                        )
                                    }
                                }
                            }
                        }

        }

                    SettingsTab.SPOTIFY -> {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SectionLabel(stringResource(R.string.settings_label_spotify))
                                SettingsGroup(rowCount = 2) { index, position ->
                                    when (index) {
                                        0 -> if (spotifyConnected) {
                                            SpotifyAccountRow(
                                                onDisconnect = { showSpotifyDisconnectConfirm = true },
                                                onClick = onOpenSpotifyLogin,
                                                position = position,
                                                isHighlighted = (highlightedSettingId == "spotify.account"),
                                            )
                                        } else {
                                            SettingsActionCard(
                                                icon = Icons.Filled.PlayCircle,
                                                iconContainer = MaterialTheme.colorScheme.surfaceVariant,
                                                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                title = "Sign in to Spotify",
                                                subtitle = "To import your playlists and liked songs",
                                                onClick = onOpenSpotifyLogin,
                                                position = position,
                                                isHighlighted = (highlightedSettingId == "spotify.account"),
                                            )
                                        }
                                        1 -> SettingsActionCard(
                                            icon = Icons.Filled.QueueMusic,
                                            iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                            iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            title = "Your Spotify playlists",
                                            subtitle = "Bring in your playlists and liked songs",
                                            onClick = onOpenExternalImport,
                                            position = position,
                                            isHighlighted = (highlightedSettingId == "spotify.import"),
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SectionLabel(stringResource(R.string.settings_label_advanced))
                                SettingsGroup(rowCount = 1) { _, position ->
                                    SettingsActionCard(
                                        icon = Icons.Filled.Key,
                                        iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
                                        iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        title = "Sign in with a cookie",
                                        subtitle = "Paste the sp_dc cookie from your browser",
                                        onClick = onOpenSpotifyTokenImport,
                                        position = position,
                                        isHighlighted = (highlightedSettingId == "spotify.token"),
                                    )
                                }
                            }
                        }
                    }

                    SettingsTab.LAST_FM -> {
                        item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel(stringResource(R.string.settings_label_scrobbling))
                    // Last.fm is optional here — never a gate. Connected:
                    // global scrobbles + stats sync. Disconnected: Stats and
                    // recommendations run local-first from Room.
                    LastFmIntegrationCard(
                        isConnected = isLastFmConnected,
                        username = session.username,
                        avatarUrl = avatarUrl,
                        connecting = lastFmConnecting,
                        awaitingApproval = lastFmAuthUrl != null,
                        hasApiKey = hasApiKey,
                        isHighlighted = (highlightedSettingId in listOf("lastfm.connect", "lastfm.api_credentials")),
                        onConnect = { viewModel.beginLastFmConnect() },
                        onCancel = viewModel::cancelLastFmConnect,
                        onDisconnect = viewModel::disconnectLastFm,
                        onSaveKeys = viewModel::saveApiCredentials,
                        onRemoveKey = viewModel::clearApiKey,
                        onOpenCreateKeyPage = {
                            runCatching {
                                androidx.browser.customtabs.CustomTabsIntent.Builder().build()
                                    .launchUrl(context, android.net.Uri.parse(LAST_FM_CREATE_KEY_URL))
                            }.onFailure {
                                viewModel.showToast(context.getString(R.string.toast_no_browser_open_lfm))
                            }
                        },
                    )
                    SettingsGroup(rowCount = 3) { index, position ->
                        when (index) {
                            0 -> SettingsToggleCard(
                                icon = Icons.Filled.History,
                                iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                title = stringResource(R.string.settings_scrobble),
                                subtitle = stringResource(R.string.you_connect_lastfm_sub),
                                checked = scrobbler.enabled,
                                onCheckedChange = viewModel::setScrobblerEnabled,
                                position = position,
                                isHighlighted = (highlightedSettingId == "lastfm.scrobbler"),
                            )
                            1 -> SettingsToggleCard(
                                icon = Icons.Filled.PlayCircle,
                                iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                title = stringResource(R.string.settings_now_playing),
                                subtitle = stringResource(R.string.settings_now_playing_sub),
                                checked = scrobbler.submitNowPlaying,
                                onCheckedChange = viewModel::setSubmitNowPlaying,
                                position = position,
                                enabled = scrobbler.enabled,
                                isHighlighted = (highlightedSettingId == "lastfm.now_playing"),
                            )
                            else -> SettingsActionCard(
                                icon = Icons.Filled.Timer,
                                iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
                                iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                                title = "Scrobble threshold",
                                subtitle = "${scrobbler.scrobblePercent}%",
                                onClick = { if (scrobbler.enabled) showScrobblePercentDialog = true },
                                position = position,
                                isHighlighted = (highlightedSettingId == "lastfm.scrobble_percent"),
                            )
                        }
                    }
                    // Discord presence, next to Last.fm because both are "tell
                    // other services what I'm listening to" integrations and
                    // neither is required for playback. Unlike Last.fm this
                    // needs no account in Tuned — it publishes as the
                    // Tuned application through the Discord app itself, so
                    // the copy says exactly that.
                    SettingsToggleCard(
                        icon = Icons.Filled.Public,
                        iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                        iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                        title = stringResource(R.string.set_discord_title),
                        subtitle = if (discordPresenceEnabled) {
                            stringResource(R.string.set_discord_on)
                        } else {
                            stringResource(R.string.set_discord_off)
                        },
                        checked = discordPresenceEnabled,
                        onCheckedChange = viewModel::setDiscordPresenceEnabled,
                        isHighlighted = (highlightedSettingId == "discord.presence"),
                    )
                }
            }
                    }



                    SettingsTab.LIBRARY -> {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SectionLabel(stringResource(R.string.settings_label_home))
                                SettingsGroup(rowCount = 2) { index, position ->
                                    when (index) {
                                        0 -> SettingsActionCard(
                                            icon = Icons.Filled.Dashboard,
                                            iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            title = stringResource(R.string.settings_home_sections),
                                            subtitle = run {
                                                val total = com.tuned.app.data.local.HomeSection.entries.size
                                                val visible = total - misc.hiddenHomeSections.size
                                                stringResource(R.string.home_sections_visible, visible, total)
                                            },
                                            onClick = onOpenHomeSections,
                                            position = position,
                                            isHighlighted = (highlightedSettingId == "library.home_sections"),
                                        )
                                        else -> SettingsActionCard(
                                            icon = Icons.Filled.RestartAlt,
                                            iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                            iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            title = stringResource(R.string.settings_excluded_songs),
                                            subtitle = stringResource(R.string.set_excl_count, state.recommendationExclusionCount),
                                            onClick = onOpenExcludedSongs,
                                            position = position,
                                            isHighlighted = (highlightedSettingId == "library.excluded_songs"),
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SectionLabel(stringResource(R.string.settings_section_content))
                                SettingsGroup(rowCount = 1) { _, position ->
                                    SettingsToggleCard(
                                        icon = Icons.Filled.Mic,
                                        iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
                                        iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        title = stringResource(R.string.settings_podcasts),
                                        subtitle = if (misc.podcastsEnabled) {
                                            stringResource(R.string.settings_podcasts_sub_on)
                                        } else {
                                            stringResource(R.string.settings_podcasts_sub_off)
                                        },
                                        checked = misc.podcastsEnabled,
                                        onCheckedChange = viewModel::setPodcastsEnabled,
                                        position = position,
                                        isHighlighted = (highlightedSettingId == "library.podcasts"),
                                    )
                                }
                            }
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SectionLabel(stringResource(R.string.settings_section_imports))
                                SettingsGroup(rowCount = 3) { index, position ->
                                    when (index) {
                                        0 -> SettingsActionCard(
                                            icon = Icons.Filled.QueueMusic,
                                            iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            title = stringResource(R.string.set_import_yt),
                                            subtitle = stringResource(R.string.set_import_yt_sub),
                                            onClick = onOpenYouTubeImport,
                                            position = position,
                                            isHighlighted = (highlightedSettingId == "library.import_yt"),
                                        )
                                        1 -> SettingsActionCard(
                                            icon = Icons.Filled.QueueMusic,
                                            iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                            iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            title = stringResource(R.string.set_import_spotify),
                                            subtitle = stringResource(R.string.set_import_spotify_sub),
                                            onClick = onOpenExternalImport,
                                            position = position,
                                            isHighlighted = (highlightedSettingId == "library.import_external"),
                                        )
                                        else -> SettingsActionCard(
                                            icon = Icons.Filled.FileDownload,
                                            iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
                                            iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                                            title = stringResource(R.string.settings_import_file),
                                            subtitle = stringResource(R.string.settings_import_file_sub),
                                            onClick = {
                                                runCatching {
                                                    csvPickerLauncher.launch(arrayOf("text/*", "text/csv", "application/csv", "audio/x-mpegurl", "application/x-mpegurl", "application/vnd.apple.mpegurl", "*/*"))
                                                }.onFailure { viewModel.showToast(context.getString(R.string.toast_no_picker)) }
                                            },
                                            position = position,
                                            isHighlighted = (highlightedSettingId == "library.import_file"),
                                        )
                                    }
                                }
                            }
                        }



                    }

                    SettingsTab.DATA_BACKUP -> {
                        item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel(stringResource(R.string.settings_section_backup))
                    SettingsGroup(rowCount = 2) { index, position ->
                        when (index) {
                            0 -> SettingsActionCard(
                                icon = Icons.Filled.Backup,
                                iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                title = stringResource(R.string.settings_backup),
                                subtitle = stringResource(R.string.settings_backup_sub),
                                onClick = {
                                    runCatching { backupLauncher.launch("tuned-backup-${System.currentTimeMillis()}.json") }
                                        .onFailure { viewModel.showToast(context.getString(R.string.toast_no_picker)) }
                                },
                                position = position,
                                isHighlighted = (highlightedSettingId == "backup.create"),
                            )
                            1 -> SettingsActionCard(
                                icon = Icons.Filled.CloudDownload,
                                iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                title = stringResource(R.string.settings_restore),
                                subtitle = stringResource(R.string.settings_restore_sub),
                                onClick = {
                                    runCatching { restoreLauncher.launch(arrayOf("*/*")) }
                                        .onFailure { viewModel.showToast(context.getString(R.string.toast_no_picker)) }
                                },
                                position = position,
                                isHighlighted = (highlightedSettingId == "backup.restore"),
                            )
                        }
                    }
                }
            }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SectionLabel(stringResource(R.string.settings_section_data))
                                SettingsGroup(rowCount = 1) { index, position ->
                                    when (index) {
                                        0 -> SettingsActionCard(
                                            icon = Icons.Filled.Delete,
                                            iconContainer = MaterialTheme.colorScheme.errorContainer,
                                            iconTint = MaterialTheme.colorScheme.onErrorContainer,
                                            title = stringResource(R.string.settings_clear_all),
                                            subtitle = stringResource(R.string.settings_clear_all_sub),
                                            danger = true,
                                            onClick = viewModel::requestClearAllData,
                                            position = position,
                                            isHighlighted = (highlightedSettingId == "backup.clear_all"),
                                        )
                                    }
                                }
                            }
                        }
                        // Updates and bug-report logs.
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SectionLabel(stringResource(R.string.settings_section_about))
                                SettingsGroup(rowCount = if (AppUpdateManager.isEnabled) 2 else 1) { index, position ->
                                    when (if (AppUpdateManager.isEnabled) index else index + 1) {
                                        0 -> CheckUpdatesRow(
                                            info = updateInfo,
                                            position = position,
                                            isHighlighted = (highlightedSettingId == "about.check_updates"),
                                            onOpen = { showUpdateSheet = true },
                                            onCheck = viewModel::checkForUpdates,
                                        )
                                        else -> SettingsActionCard(
                                            icon = Icons.Filled.Code,
                                            iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                            iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            title = stringResource(R.string.settings_diagnostics),
                                            subtitle = stringResource(R.string.settings_diagnostics_sub),
                                            onClick = { viewModel.exportDiagnostics() },
                                            position = position,
                                            isHighlighted = (highlightedSettingId == "about.diagnostics"),
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
        }
    }
    }

    // -- Custom color wheel dialog (par 8.4) --
    if (state.showColorWheel) {
        ColorWheelSheet(onDismiss = viewModel::dismissColorWheel, onApply = viewModel::applyCustomColor)
    }

    // -- Open-source licences --

    // -- Clear-all-data confirm --
    if (state.showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissClearAllConfirm,
            title = { Text(stringResource(R.string.dialog_clear_all_title)) },
            text = { Text(stringResource(R.string.dialog_clear_all_text)) },
            confirmButton = { TextButton(onClick = { viewModel.confirmClearAllData(onLoggedOut) }) { Text(stringResource(R.string.dialog_clear_all_confirm)) } },
            dismissButton = { TextButton(onClick = viewModel::dismissClearAllConfirm) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    // -- YouTube Music disconnect confirm --
    if (showYtDisconnectConfirm) {
        AlertDialog(
            onDismissRequest = { showYtDisconnectConfirm = false },
            title = { Text(stringResource(R.string.dialog_disconnect_yt_title)) },
            text = { Text(stringResource(R.string.dialog_disconnect_yt_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showYtDisconnectConfirm = false
                    viewModel.disconnectYouTube()
                }) { Text(stringResource(R.string.common_disconnect)) }
            },
            dismissButton = {
                TextButton(onClick = { showYtDisconnectConfirm = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    // -- Spotify disconnect confirm --
    if (showSpotifyDisconnectConfirm) {
        AlertDialog(
            onDismissRequest = { showSpotifyDisconnectConfirm = false },
            title = { Text(stringResource(R.string.spotify_signout_title)) },
            text = { Text(stringResource(R.string.spotify_signout_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showSpotifyDisconnectConfirm = false
                    viewModel.disconnectSpotify()
                    runCatching {
                        val cm = android.webkit.CookieManager.getInstance()
                        for (domain in listOf("https://spotify.com", "https://accounts.spotify.com", "https://open.spotify.com")) {
                            val cookies = cm.getCookie(domain) ?: continue
                            for (cookie in cookies.split(";")) {
                                val name = cookie.substringBefore("=").trim()
                                if (name.isNotEmpty()) {
                                    cm.setCookie(domain, "$name=; Expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/")
                                }
                            }
                        }
                        cm.flush()
                    }
                }) { Text(stringResource(R.string.common_disconnect)) }
            },
            dismissButton = {
                TextButton(onClick = { showSpotifyDisconnectConfirm = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    if (showCookieLoginDialog) {
        CookieLoginDialog(
            onDismiss = { showCookieLoginDialog = false },
            onConnect = { cookies ->
                viewModel.connectWithCookies(cookies) { success, _ ->
                    if (success) showCookieLoginDialog = false
                }
            },
        )
    }

    // -- Restore confirm --
    if (state.showRestoreConfirm) {        val isPlaylistMirror = state.pendingRestoreKind == PendingRestoreKind.PLAYLIST_MIRROR
        AlertDialog(
            onDismissRequest = viewModel::dismissRestoreConfirm,
            title = { Text(if (isPlaylistMirror) "Add playlists from this file?" else "Restore this backup?") },
            text = {
                Text(
                    if (isPlaylistMirror) {
                        "Adds the playlists from this backup to yours and turns sync back on."
                    } else {
                        "Replaces your playlists and settings with the ones in this backup."
                    },
                )
            },
            confirmButton = { TextButton(onClick = { viewModel.confirmRestore(onBack) }) { Text(if (isPlaylistMirror) stringResource(R.string.common_sync) else stringResource(R.string.common_restore)) } },
            dismissButton = { TextButton(onClick = viewModel::dismissRestoreConfirm) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    // -- App language picker (Settings -> Language) --
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.settings_language_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    AppLanguage.SELECTABLE.forEach { language ->
                        val selected = language == currentLanguage
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    viewModel.setAppLanguage(language)
                                    showLanguageDialog = false
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    language.nativeDisplayName(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                )
                                if (language == AppLanguage.SYSTEM) {
                                    Text(
                                        stringResource(R.string.settings_language_system_sub),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (selected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = stringResource(R.string.common_selected),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.common_done))
                }
            },
        )
    }

    // -- Enable Scrobbling password dialog --
    if (state.showSessionKeyDialog) {
        var password by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = viewModel::dismissSessionKeyDialog,
            title = { Text(stringResource(R.string.set_scrobble_title)) },
            text = {
                Column {
                    Text(
                        "Last.fm needs your password once to start scrobbling without a browser. It goes straight to Last.fm and isn't saved.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(16.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.set_scrobble_pass)) },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        isError = state.sessionKeyError != null,
                        supportingText = state.sessionKeyError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.submitPassword(password) },
                    enabled = !state.sessionKeyLoading,
                ) {
                    if (state.sessionKeyLoading) {
                        com.tuned.app.ui.common.ExpressiveInlineLoadingIndicator(size = 18.dp)
                    } else {
                        Text(stringResource(R.string.set_scrobble_enable))
                    }
                }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissSessionKeyDialog) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    if (showAutoEq) {
        com.tuned.app.ui.settings.autoeq.AutoEqDialog(onDismiss = { showAutoEq = false })
    }

    if (showComponentGallery) {
        com.tuned.app.ui.common.ComponentGallery(onDismiss = { showComponentGallery = false })
    }

    if (showJamSheet) {
        JamSheet(
            viewModel = jamViewModel,
            onDismiss = { showJamSheet = false },
        )
    }

    // -- Experimental 15-band equalizer --
    if (showEqSheet) {
        EqualizerSheet(
            eq = eq,
            onDismiss = { showEqSheet = false },
            onSetEnabled = viewModel::setEqualizerEnabled,
            onPickPreset = viewModel::applyEqPreset,
            onBandPreview = viewModel::previewEqBandGain,
            onBandChange = viewModel::setEqBandGain,
        )
    }



    // -- Lyrics provider picker: preferred source first, automatic
    // fallback to the rest when it returns nothing --
    if (showLyricsProviderDialog) {
        LyricsProviderDialog(
            current = misc.lyricsProvider,
            onSelect = {
                viewModel.setLyricsProvider(it)
                showLyricsProviderDialog = false
            },
            onDismiss = { showLyricsProviderDialog = false },
        )
    }

    // -- Lyrics sync offset stepper: shifts highlight/focus only --
    if (showLyricsOffsetDialog) {
        LyricsOffsetDialog(
            currentMs = misc.lyricsOffsetMs,
            onSelect = { viewModel.setLyricsOffsetMs(it) },
            onDismiss = { showLyricsOffsetDialog = false },
        )
    }

    // -- Lyrics text size dialog: scales font size with live preview --
    if (showLyricsSizeDialog) {
        LyricsSizeDialog(
            currentScale = misc.lyricsFontScale,
            onSelect = { viewModel.setLyricsFontScale(it) },
            onDismiss = { showLyricsSizeDialog = false },
        )
    }

    // -- Floating lyrics buttons: idle time before they fade out --
    if (showLyricsAutoHideDialog) {
        LyricsAutoHideDialog(
            currentSeconds = misc.lyricsButtonsAutoHideSeconds,
            onSelect = {
                viewModel.setLyricsButtonsAutoHideSeconds(it)
                showLyricsAutoHideDialog = false
            },
            onDismiss = { showLyricsAutoHideDialog = false },
        )
    }

    // -- Language the lyrics translate button turns songs into --
    if (showTranslationLanguageDialog) {
        TranslationLanguageDialog(
            current = misc.translationLanguage,
            onSelect = {
                viewModel.setTranslationLanguage(it)
                showTranslationLanguageDialog = false
            },
            onDismiss = { showTranslationLanguageDialog = false },
        )
    }

    if (showLoudnessDialog) {
        LoudnessModeDialog(
            current = loudness.mode,
            onSelect = {
                viewModel.setLoudnessMode(it)
                showLoudnessDialog = false
            },
            preampDb = loudness.preampDb,
            onPreampChange = viewModel::setLoudnessPreamp,
            onDismiss = { showLoudnessDialog = false },
        )
    }

    if (showScrobblePercentDialog) {
        ScrobblePercentDialog(
            currentPercent = scrobbler.scrobblePercent,
            onPercentChange = viewModel::setScrobblePercent,
            onDismiss = { showScrobblePercentDialog = false },
        )
    }

    if (showClarityPresetDialog) {
        ClarityPresetDialog(
            currentIndex = misc.clarityPreset,
            onSelect = {
                viewModel.setClarityPreset(it)
                showClarityPresetDialog = false
            },
            onDismiss = { showClarityPresetDialog = false },
        )
    }

    // -- Selective Playlist Sync sheet --
    if (showSyncPlaylistsSheet) {
        SyncPlaylistsSheet(
            playlists = allPlaylists,
            syncedIds = syncedPlaylistIds,
            onToggleSync = viewModel::togglePlaylistSync,
            onSelectAll = viewModel::selectAllPlaylistsForSync,
            onDismiss = { showSyncPlaylistsSheet = false },
        )
    }

    if (showYtLibraryVisibilitySheet) {
        YouTubeLibraryVisibilitySheet(
            playlists = ytAccountPlaylists,
            hiddenIds = hiddenYtLibraryPlaylistIds,
            onSetVisible = viewModel::setYtLibraryPlaylistVisible,
            onSetAllVisible = viewModel::setAllYtLibraryPlaylistsVisible,
            onDismiss = { showYtLibraryVisibilitySheet = false },
        )
    }

    if (showYtChannelSheet) {
        YouTubeChannelSheet(
            channels = ytChannels,
            isLoading = ytChannelsLoading,
            selectedChannelId = ytConnection.onBehalfOfUser,
            selectedAuthUser = ytConnection.authUserIndex,
            selectedPageId = ytConnection.pageId,
            onReload = viewModel::loadYtChannels,
            onSelect = {
                viewModel.selectYtChannel(it)
                showYtChannelSheet = false
            },
            onDismiss = { showYtChannelSheet = false },
        )
    }

    if (showQualityDialog) {
        val tiers = listOf(
            Triple(28, "Dolby Atmos", "Spatial audio" to ""),
            Triple(27, "Max", "FLAC up to 24-bit / 192 kHz" to ""),
            Triple(7, "Hi-Res", "FLAC 24-bit / 96 kHz" to ""),
            Triple(6, "CD", "FLAC 16-bit / 44.1 kHz" to ""),
            Triple(5, "Standard", "320 kbps" to ""),
            Triple(4, "Data saver", "96 kbps AAC" to ""),
            Triple(-1, "YouTube Music", "128 to 256 kbps, as YouTube streams it" to ""),
        )
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

        ModalBottomSheet(
            onDismissRequest = { showQualityDialog = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .adaptiveContentWidth(maxWidth = 640.dp)
                    .align(Alignment.CenterHorizontally)
                    .sheetScrollGuard()
                    // No stretch at the ends, and the fling's leftover never moves the sheet.
                    .verticalScroll(rememberScrollState(), overscrollEffect = null)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp + safeDrawingBottomPadding()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.HighQuality,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "Streaming quality",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Text(
                    "If a song isn't available at this level, the next one down plays.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    tiers.forEach { (qualityId, title, meta) ->
                        val (subtitle, badge) = meta
                        val isSelected = misc.losslessQuality == qualityId
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.setLosslessQuality(qualityId)
                                showQualityDialog = false
                            },
                            // A plain row; the chosen one gets a soft tint and the check.
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else Color.Transparent,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = stringResource(R.string.set_selected_cd),
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(18.dp),
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

    if (showDownloadQualityDialog) {
        val downloadTiers = listOf(
            Triple(28, "Dolby Atmos", "Spatial audio" to ""),
            Triple(27, "Max", "FLAC up to 24-bit / 192 kHz" to ""),
            Triple(7, "Hi-Res", "FLAC 24-bit / 96 kHz" to ""),
            Triple(6, "CD", "FLAC 16-bit / 44.1 kHz" to ""),
            Triple(5, "Standard", "320 kbps" to ""),
            Triple(4, "Data saver", "96 kbps AAC" to ""),
            Triple(-1, "YouTube Music", "128 to 256 kbps, as YouTube streams it" to ""),
        )
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

        ModalBottomSheet(
            onDismissRequest = { showDownloadQualityDialog = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .adaptiveContentWidth(maxWidth = 640.dp)
                    .align(Alignment.CenterHorizontally)
                    .sheetScrollGuard()
                    // No stretch at the ends, and the fling's leftover never moves the sheet.
                    .verticalScroll(rememberScrollState(), overscrollEffect = null)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp + safeDrawingBottomPadding()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.CloudDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "Download quality",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Text(
                    "If a song isn't available at this level, the next one down is saved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    downloadTiers.forEach { (qualityId, title, meta) ->
                        val (subtitle, badge) = meta
                        val isSelected = misc.downloadQuality == qualityId
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.setDownloadQuality(qualityId)
                                showDownloadQualityDialog = false
                            },
                            // A plain row; the chosen one gets a soft tint and the check.
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else Color.Transparent,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = stringResource(R.string.set_selected_cd),
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(18.dp),
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

    if (showYouTubeImportSheet) {
        YouTubeImportSheet(
            onDismiss = { showYouTubeImportSheet = false },
            innerTube = viewModel.innerTube,
            importManager = viewModel.playlistImportManager,
            onImportSuccess = { saved ->
                viewModel.showToast(context.getString(R.string.toast_imported, saved.title, saved.tracks.size))
            },
        )
    }

    state.toastMessage?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(3000)
            viewModel.dismissToast()
        }
        Box(
            Modifier
                .fillMaxSize()
                .safeHorizontalContentPadding()
                .padding(
                    bottom = 24.dp +
                        LocalMiniPlayerScrollClearance.current +
                        safeDrawingBottomPadding(),
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(shape = ExpressivePillShape, color = MaterialTheme.colorScheme.inverseSurface, tonalElevation = 6.dp) {
                Text(msg, color = MaterialTheme.colorScheme.inverseOnSurface, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
            }
        }
    }

    if (activeToggleDetail != null) {
        val detail = activeToggleDetail!!
        Dialog(
            onDismissRequest = { activeToggleDetail = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .blur(16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { activeToggleDetail = null },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .widthIn(max = 420.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (detail.checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.size(44.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        imageVector = if (detail.checked) Icons.Filled.Check else Icons.Filled.Info,
                                        contentDescription = null,
                                        tint = if (detail.checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (detail.checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            ) {
                                Text(
                                    text = if (detail.checked) "On" else "Off",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (detail.checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = detail.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = detail.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp,
                        )
                        Spacer(Modifier.height(20.dp))
                        FilledTonalButton(
                            onClick = { activeToggleDetail = null },
                            modifier = Modifier.align(Alignment.End),
                            shape = CircleShape,
                        ) {
                            Text(stringResource(R.string.common_close), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

internal fun appVersionName(context: android.content.Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "4.3.1"
} catch (error: Exception) {
    "4.3.1"
} catch (error: LinkageError) {
    "4.3.1"
}

/** OEM Settings/browser components are optional and occasionally broken on
 * custom ROMs. Never let an external activity failure escape a click event. */
internal fun startActivitySafely(context: android.content.Context, intent: Intent): Boolean {
    val safeIntent = Intent(intent).apply {
        if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
        context.startActivity(safeIntent)
        true
    } catch (_: Exception) {
        false
    } catch (_: LinkageError) {
        false
    }
}

/** The update row: opens the update when there is one, otherwise checks for one. */
@Composable
private fun CheckUpdatesRow(
    info: com.tuned.app.data.update.UpdateInfo,
    position: GroupPosition,
    isHighlighted: Boolean,
    onOpen: () -> Unit,
    onCheck: () -> Unit,
) {
    val context = LocalContext.current
    SettingsActionCard(
        icon = Icons.Filled.CloudDownload,
        iconContainer = MaterialTheme.colorScheme.primaryContainer,
        iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
        title = stringResource(R.string.settings_check_updates),
        subtitle = when {
            info.isChecking -> stringResource(R.string.update_checking)
            info.isUpdateAvailable -> stringResource(R.string.update_new, info.latestVersion)
            else -> stringResource(R.string.update_installed, appVersionName(context))
        },
        onClick = { if (info.isUpdateAvailable) onOpen() else onCheck() },
        position = position,
        isHighlighted = isHighlighted,
    )
}

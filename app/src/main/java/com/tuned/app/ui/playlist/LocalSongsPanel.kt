package com.tuned.app.ui.playlist

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tuned.app.R
import com.tuned.app.data.localmusic.LocalMusicLibrary
import com.tuned.app.data.localmusic.LocalMusicLibrary.ImportState
import com.tuned.app.ui.common.AppButton
import com.tuned.app.ui.common.AppButtonStyle
import com.tuned.app.ui.common.AppIconButton
import com.tuned.app.ui.theme.AppShape
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.tunedColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Local songs: folder import controls shown at the top of the "Local songs" playlist. */
@HiltViewModel
class LocalSongsViewModel @Inject constructor(
    private val library: LocalMusicLibrary,
) : ViewModel() {
    val importState: StateFlow<ImportState> = library.state
    val folders: StateFlow<List<Uri>> = library.folders

    private val _storageBytes = MutableStateFlow(0L)
    val storageBytes: StateFlow<Long> = _storageBytes.asStateFlow()

    fun refreshStorage() {
        viewModelScope.launch(Dispatchers.IO) { _storageBytes.value = library.storageBytes() }
    }

    val trackFolders: StateFlow<Map<String, String>> = library.trackFolders
    val trackDirs: StateFlow<Map<String, String>> = library.trackDirs

    /** Opening the Local section quietly refreshes the device scan. */
    fun refreshOnOpen() = library.refreshDeviceInBackground()

    init {
        library.fillMissingTrackFolders()
    }

    fun addFolder(uri: Uri) = library.addFolderAndImport(uri)
    fun scanDevice() = library.scanDevice()
    fun requiredAudioPermission(): String = library.requiredAudioPermission()
    fun hasDeviceScanPermission(): Boolean = library.hasDeviceScanPermission()
    fun rescan() = library.rescanAll()
    fun forget(uri: Uri) = library.forgetFolder(uri)
    fun acknowledge() = library.acknowledgeReport()
    fun label(uri: Uri): String = library.folderLabel(uri)
}

/**
 * The Local songs folders, in the page's own row style: one row per folder with its song count,
 * then Add folder and Rescan as text actions. Before any folder there is one line and a button.
 */
@Composable
fun LocalSongsPanel(
    trackCount: Int,
    viewModel: LocalSongsViewModel = hiltViewModel(),
) {
    val colors = MaterialTheme.tunedColors
    val state by viewModel.importState.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val trackFolders by viewModel.trackFolders.collectAsStateWithLifecycle()
    val storageBytes by viewModel.storageBytes.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) viewModel.addFolder(uri)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.scanDevice()
    }
    fun scanDevice() {
        if (viewModel.hasDeviceScanPermission()) viewModel.scanDevice()
        else permissionLauncher.launch(viewModel.requiredAudioPermission())
    }
    val running = state is ImportState.Scanning || state is ImportState.Importing
    // Forgetting a folder releases its grant and deletes every reference
    // row in one tap with no way back — confirm first, like playlist delete.
    var pendingForget by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) { viewModel.refreshOnOpen() }
    LaunchedEffect(state is ImportState.Finished, trackCount) { viewModel.refreshStorage() }

    Column(Modifier.fillMaxWidth()) {
        if (folders.isEmpty()) {
            Text(stringResource(R.string.local_empty_hint), style = AppType.bodyS, color = colors.text2)
            Spacer(Modifier.height(Space.md))
            AppButton(
                text = stringResource(R.string.local_scan_device),
                onClick = { scanDevice() },
                icon = Icons.Filled.Refresh,
                enabled = !running,
            )
            Spacer(Modifier.height(Space.sm))
            AppButton(
                text = stringResource(R.string.local_choose_folder),
                onClick = { picker.launch(null) },
                style = AppButtonStyle.Ghost,
                icon = Icons.Filled.CreateNewFolder,
                enabled = !running,
            )
        } else {
            val counts = remember(trackFolders) { trackFolders.values.groupingBy { it }.eachCount() }
            folders.forEach { folder ->
                FolderRow(
                    name = viewModel.label(folder),
                    songs = counts[folder.toString()],
                    enabled = !running,
                    onForget = { pendingForget = folder },
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                AppButton(
                    text = stringResource(R.string.local_add_folder),
                    onClick = { picker.launch(null) },
                    style = AppButtonStyle.Ghost,
                    icon = Icons.Filled.CreateNewFolder,
                    enabled = !running,
                    // The text lines up with the folder names' tile above.
                    modifier = Modifier.offset(x = -Space.md),
                )
                AppButton(
                    text = stringResource(R.string.local_rescan),
                    onClick = viewModel::rescan,
                    style = AppButtonStyle.Ghost,
                    icon = Icons.Filled.Refresh,
                    enabled = !running,
                    modifier = Modifier.offset(x = -Space.md),
                )
                AppButton(
                    text = stringResource(R.string.local_scan_device),
                    onClick = { scanDevice() },
                    style = AppButtonStyle.Ghost,
                    icon = Icons.Filled.Refresh,
                    enabled = !running,
                    modifier = Modifier.offset(x = -Space.md),
                )
                Spacer(Modifier.weight(1f))
                if (storageBytes > 0L) {
                    Text(
                        stringResource(R.string.local_storage_short, formatMegabytes(storageBytes)),
                        style = AppType.caption,
                        color = colors.text3,
                    )
                }
            }
        }
        ImportStatus(state, onDismiss = viewModel::acknowledge)
    }

    pendingForget?.let { folder ->
        val counts = trackFolders.values.groupingBy { it }.eachCount()
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingForget = null },
            title = { Text(stringResource(R.string.local_forget_folder)) },
            text = {
                val songs = counts[folder.toString()]
                Text(
                    if (songs != null) {
                        viewModel.label(folder) + " — " + pluralStringResource(R.plurals.alb_song_count, songs, songs)
                    } else {
                        viewModel.label(folder)
                    },
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        pendingForget = null
                        viewModel.forget(folder)
                    },
                ) { Text(stringResource(R.string.local_forget_folder)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pendingForget = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun FolderRow(name: String, songs: Int?, enabled: Boolean, onForget: () -> Unit) {
    val colors = MaterialTheme.tunedColors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = Sizes.rowCompact),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(Sizes.artRowCompact).background(colors.surface1, AppShape.sm),
        ) {
            Icon(Icons.Filled.Folder, contentDescription = null, tint = colors.text2, modifier = Modifier.size(Sizes.iconRow))
        }
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
            Text(name, style = AppType.body, color = colors.text1, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (songs != null) {
                Text(pluralStringResource(R.plurals.alb_song_count, songs, songs), style = AppType.bodyS, color = colors.text2)
            }
        }
        AppIconButton(
            icon = Icons.Filled.Close,
            contentDescription = stringResource(R.string.local_forget_folder),
            onClick = onForget,
            enabled = enabled,
            iconSize = Sizes.iconRow,
            tint = colors.text3,
            modifier = Modifier.offset(x = Space.md),
        )
    }
}

/** Scanning, copying, then what the import did: shown only while there is something to say. */
@Composable
private fun ImportStatus(state: ImportState, onDismiss: () -> Unit) {
    val colors = MaterialTheme.tunedColors
    when (state) {
        is ImportState.Scanning -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = Space.sm),
        ) {
            com.tuned.app.ui.common.LoadingSpinner(modifier = Modifier.size(Sizes.iconRow))
            Spacer(Modifier.width(Space.sm))
            Text(stringResource(R.string.local_scanning, state.filesFound), style = AppType.bodyS, color = colors.text2)
        }
        is ImportState.Importing -> Column(Modifier.padding(top = Space.sm), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            val fraction = if (state.total > 0) state.done.toFloat() / state.total else 0f
            LinearProgressIndicator(
                progress = { fraction },
                color = colors.accent,
                trackColor = colors.surface2,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(R.string.local_copying, state.done, state.total, state.currentName),
                style = AppType.bodyS,
                color = colors.text2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        is ImportState.Finished -> {
            val r = state.report
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = Space.xs)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
                    Text(stringResource(R.string.local_report_summary, r.added, r.updated, r.unchanged), style = AppType.bodyS, color = colors.text2)
                    if (r.unsupported.isNotEmpty()) {
                        val total = r.unsupported.values.sum()
                        Text(
                            pluralStringResource(
                                R.plurals.local_report_unsupported,
                                total,
                                total,
                                r.unsupported.keys.sorted().joinToString(", ") { ".$it" },
                            ),
                            style = AppType.bodyS,
                            color = colors.text3,
                        )
                    }
                    if (r.failed > 0) {
                        Text(pluralStringResource(R.plurals.local_report_failed, r.failed, r.failed), style = AppType.bodyS, color = colors.danger)
                    }
                    if (r.unreachableFolders > 0) {
                        Text(
                            pluralStringResource(R.plurals.local_report_unreachable, r.unreachableFolders, r.unreachableFolders),
                            style = AppType.bodyS,
                            color = colors.danger,
                        )
                    }
                }
                AppIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.common_close),
                    onClick = onDismiss,
                    iconSize = Sizes.iconInline,
                    tint = colors.text3,
                    modifier = Modifier.offset(x = Space.md),
                )
            }
        }
        ImportState.Idle -> Unit
    }
}

private fun formatMegabytes(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) "%.1f GB".format(mb / 1024.0) else "%.0f MB".format(mb)
}

/** Category tabs for the Local songs page, in the same chips as the folder picker. */
@Composable
fun LocalTabRow(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FolderChip(stringResource(R.string.local_tab_songs), selected == LOCAL_TAB_SONGS, asTab = true) { onSelect(LOCAL_TAB_SONGS) }
        FolderChip(stringResource(R.string.local_albums), selected == LOCAL_TAB_ALBUMS, asTab = true) { onSelect(LOCAL_TAB_ALBUMS) }
        FolderChip(stringResource(R.string.search_tab_artists), selected == LOCAL_TAB_ARTISTS, asTab = true) { onSelect(LOCAL_TAB_ARTISTS) }
        FolderChip(stringResource(R.string.local_tab_folders), selected == LOCAL_TAB_FOLDERS, asTab = true) { onSelect(LOCAL_TAB_FOLDERS) }
    }
}

/**
 * Picks which folder's songs the Local songs list shows. Only worth showing with two or
 * more folders, and it uses the same chips as the Downloads tabs.
 */
@Composable
fun LocalFolderChips(
    selected: String?,
    onSelect: (String?) -> Unit,
    viewModel: LocalSongsViewModel = hiltViewModel(),
) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    if (folders.size < 2) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FolderChip(stringResource(R.string.local_all_folders), selected == null) { onSelect(null) }
        folders.forEach { folder ->
            FolderChip(viewModel.label(folder), selected == folder.toString()) { onSelect(folder.toString()) }
        }
    }
}

@Composable
private fun FolderChip(label: String, selected: Boolean, asTab: Boolean = false, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        // Category tabs must announce as tabs, not filter chips, to
        // assistive technology; folder filter chips keep chip semantics.
        modifier = if (asTab) {
            Modifier.semantics {
                role = androidx.compose.ui.semantics.Role.Tab
                this.selected = selected
            }
        } else {
            Modifier
        },
        shape = RoundedCornerShape(50),
        border = null,
        label = { Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.onSurface,
            selectedLabelColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

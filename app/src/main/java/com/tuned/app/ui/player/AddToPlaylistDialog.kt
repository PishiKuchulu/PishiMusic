@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.tuned.app.ui.player

import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.border
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.tuned.app.ui.common.adaptiveContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tuned.app.data.playlist.SavedPlaylist
import com.tuned.app.data.playlist.LIKED_SONGS_MODE
import com.tuned.app.playback.PlayableTrack
import com.tuned.app.playback.toGeneratedTrack
import com.tuned.app.ui.common.ExpressiveInlineLoadingIndicator
import com.tuned.app.ui.common.ExpressiveMotion
import com.tuned.app.ui.common.PlaylistCover

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddToPlaylistDialog(
    track: PlayableTrack,
    playlists: List<SavedPlaylist>,
    onDismiss: () -> Unit,
    onAdd: (Set<Long>, Set<Long>) -> Unit,
    onFindDuplicates: suspend (Set<Long>) -> Set<Long>,
    onFindCachedDuplicates: suspend (Set<Long>) -> Set<Long>,
    onCreate: (String) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sanitizedPlaylists = remember(playlists) { playlists.distinctBy { it.id } }
    var newPlaylistName by androidx.compose.runtime.saveable.rememberSaveable(track) { mutableStateOf("") }
    var selectedPlaylistIds by remember(track) { mutableStateOf(emptySet<Long>()) }
    var duplicateConfirmation by remember(track) { mutableStateOf<Set<Long>?>(null) }
    var duplicatePlaylistIds by remember(track) { mutableStateOf(emptySet<Long>()) }
    var knownDuplicatePlaylistIds by remember(track) { mutableStateOf(emptySet<Long>()) }
    var isCheckingDuplicates by remember(track) { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val duplicateCheckFailedMessage = stringResource(com.tuned.app.R.string.toast_action_fail, "check for duplicates")
    val trackKey = remember(track.title, track.artist) { track.toGeneratedTrack().key }
    /** Playlists already containing this track, computed once per list change. */
    val playlistsContainingTrack = remember(sanitizedPlaylists, trackKey, track.title, track.artist) {
        sanitizedPlaylists
            .filter { playlist ->
                playlist.tracks.any {
                    it.key == trackKey ||
                        (it.name.equals(track.title, ignoreCase = true) && it.artist.equals(track.artist, ignoreCase = true))
                }
            }
            .mapTo(mutableSetOf(), SavedPlaylist::id)
    }
    val selectedPlaylists = sanitizedPlaylists.filter { it.id in selectedPlaylistIds }
    val playlistListMaxHeight = (LocalConfiguration.current.screenHeightDp.dp - 360.dp)
        .coerceIn(180.dp, 410.dp)

    LaunchedEffect(sanitizedPlaylists.map(SavedPlaylist::id)) {
        selectedPlaylistIds = selectedPlaylistIds.intersect(sanitizedPlaylists.mapTo(mutableSetOf(), SavedPlaylist::id))
    }

    LaunchedEffect(trackKey, sanitizedPlaylists.map(SavedPlaylist::id)) {
        runCatching {
            val remoteIds = sanitizedPlaylists
                .filterTo(mutableListOf()) { it.remotePlaylistId != null }
                .mapTo(mutableSetOf(), SavedPlaylist::id)
            knownDuplicatePlaylistIds = knownDuplicatePlaylistIds + onFindCachedDuplicates(remoteIds)
        }
    }

    fun requestAdd(playlistIds: Set<Long>) {
        if (playlistIds.isEmpty() || isCheckingDuplicates) return
        val immediateDuplicates = playlistIds.filter { id ->
            id in knownDuplicatePlaylistIds ||
                sanitizedPlaylists.firstOrNull { it.id == id }?.tracks?.any {
                    it.key == trackKey ||
                        (it.name.equals(track.title, ignoreCase = true) && it.artist.equals(track.artist, ignoreCase = true))
                } == true
        }.toSet()
        if (immediateDuplicates.isNotEmpty()) {
            duplicatePlaylistIds = emptySet()
            duplicateConfirmation = immediateDuplicates
            return
        }
        scope.launch {
            runCatching {
                isCheckingDuplicates = true
                val duplicates = onFindDuplicates(playlistIds)
                isCheckingDuplicates = false
                knownDuplicatePlaylistIds = (knownDuplicatePlaylistIds - playlistIds) + duplicates
                if (duplicates.isEmpty()) {
                    onAdd(playlistIds, emptySet())
                } else {
                    duplicatePlaylistIds = emptySet()
                    duplicateConfirmation = duplicates
                }
            }.onFailure {
                isCheckingDuplicates = false
                // The add still proceeds (a failed duplicate check must not block
                // the user), but no longer silently: the old path added with no
                // sign anything had gone wrong, so a duplicate could land while
                // the user believed the check had passed.
                android.widget.Toast.makeText(
                    context,
                    duplicateCheckFailedMessage,
                    android.widget.Toast.LENGTH_LONG,
                ).show()
                onAdd(playlistIds, emptySet())
            }
        }
    }

    duplicateConfirmation?.let { duplicates ->
        val duplicatePlaylists = sanitizedPlaylists.filter { it.id in duplicates }
        if (duplicatePlaylists.size == 1) {
            val playlist = duplicatePlaylists.first()
            AlertDialog(
                onDismissRequest = { duplicateConfirmation = null },
                title = { Text(stringResource(com.tuned.app.R.string.add_dup_single_title)) },
                text = {
                    Text(
                        stringResource(com.tuned.app.R.string.add_dup_single_text, track.title, track.artist, playlist.title),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onAdd(selectedPlaylistIds, setOf(playlist.id))
                            duplicateConfirmation = null
                        },
                    ) {
                        Text(stringResource(com.tuned.app.R.string.add_dup_add_anyway))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { duplicateConfirmation = null }) {
                        Text(stringResource(com.tuned.app.R.string.add_dup_leave))
                    }
                },
            )
            return
        }

        val missingCount = selectedPlaylistIds.size - duplicates.size
        AlertDialog(
            onDismissRequest = { duplicateConfirmation = null },
            title = {
                Text(stringResource(com.tuned.app.R.string.add_dup_multi_title, duplicates.size))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(com.tuned.app.R.string.add_dup_multi_text),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        itemsIndexed(
                            duplicatePlaylists,
                            key = { index, playlist -> "${playlist.id}_${playlist.remotePlaylistId ?: ""}_$index" },
                        ) { _, playlist ->
                            val addAgain = playlist.id in duplicatePlaylistIds
                            val canAddAnother = playlist.mode != LIKED_SONGS_MODE
                            Surface(
                                onClick = {
                                    duplicatePlaylistIds = if (addAgain) {
                                        duplicatePlaylistIds - playlist.id
                                    } else {
                                        duplicatePlaylistIds + playlist.id
                                    }
                                },
                                enabled = canAddAnother,
                                shape = RoundedCornerShape(14.dp),
                                color = if (addAgain) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            playlist.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            if (canAddAnother) "Add another copy" else "Already liked",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Checkbox(checked = addAgain, onCheckedChange = null, enabled = canAddAnother)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAdd(selectedPlaylistIds, duplicatePlaylistIds)
                        duplicateConfirmation = null
                    },
                ) {
                    Text(
                        when {
                            duplicatePlaylistIds.isNotEmpty() -> stringResource(com.tuned.app.R.string.add_dup_add_anyway)
                            missingCount > 0 -> stringResource(com.tuned.app.R.string.add_dup_add_missing)
                            else -> stringResource(com.tuned.app.R.string.add_dup_leave)
                        },
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { duplicateConfirmation = null }) {
                    Text(stringResource(com.tuned.app.R.string.add_dup_leave))
                }
            },
        )
        return
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        // Opaque sheet: player controls / quality pill behind must not bleed through.
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        scrimColor = Color.Black.copy(alpha = 0.55f),
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
        var showCreateDialog by remember { mutableStateOf(false) }
        
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text(stringResource(com.tuned.app.R.string.add_create_title)) },
                text = {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text(stringResource(com.tuned.app.R.string.pl_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = newPlaylistName.isNotBlank(),
                        onClick = {
                            val cleanName = newPlaylistName.trim()
                            val existingPlaylist = sanitizedPlaylists.firstOrNull {
                                it.mode == "custom" && it.title.equals(cleanName, ignoreCase = true)
                            }
                            if (existingPlaylist == null) {
                                onCreate(cleanName)
                            } else {
                                selectedPlaylistIds = setOf(existingPlaylist.id)
                                requestAdd(setOf(existingPlaylist.id))
                            }
                            showCreateDialog = false
                        },
                    ) {
                        Text(stringResource(com.tuned.app.R.string.add_create_and_add))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) { Text(stringResource(com.tuned.app.R.string.common_cancel)) }
                },
            )
        }

        com.tuned.app.ui.common.EdgeToEdgeDialogWindow(visible = sheetState.targetValue != androidx.compose.material3.SheetValue.Hidden)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveContentWidth(maxWidth = 640.dp)
                .align(Alignment.CenterHorizontally)
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(52.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.size(27.dp))
                    }
                }
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(
                        "Add to playlist",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (selectedPlaylistIds.isEmpty()) "Choose one or more playlists" else "${selectedPlaylistIds.size} selected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                androidx.compose.material3.IconButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(com.tuned.app.R.string.player_create_cd))
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        track.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (sanitizedPlaylists.isEmpty()) {
                Text(
                    "No playlists yet. Create your first playlist below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = playlistListMaxHeight),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    itemsIndexed(
                        sanitizedPlaylists,
                        key = { index, playlist -> "${playlist.id}_${playlist.remotePlaylistId ?: ""}_$index" },
                    ) { _, playlist ->
                        val selected = playlist.id in selectedPlaylistIds
                        // Membership sets are precomputed once per playlist list
                        // (playlistTrackKeys above); the row used to rescan every
                        // track of the playlist on each recomposition.
                        val alreadyAdded = playlist.id in knownDuplicatePlaylistIds ||
                            playlist.id in playlistsContainingTrack
                        val trackCount = playlist.remoteTrackCount
                            ?: playlist.tracks.size.takeIf { playlist.remotePlaylistId == null }
                        val countLabel = when (trackCount) {
                            null -> "Track count unavailable"
                            1 -> "1 track"
                            else -> "$trackCount tracks"
                        }
                        val rowColor by animateColorAsState(
                            targetValue = if (selected) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            animationSpec = tween(ExpressiveMotion.Quick),
                            label = "playlistSelectionColor",
                        )
                        Surface(
                            onClick = {
                                selectedPlaylistIds = if (selected) {
                                    selectedPlaylistIds - playlist.id
                                } else {
                                    selectedPlaylistIds + playlist.id
                                }
                            },
                            shape = RoundedCornerShape(18.dp),
                            color = rowColor,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                PlaylistCover(
                                    playlist = playlist,
                                    modifier = Modifier.size(52.dp),
                                    cornerRadius = 14.dp,
                                )
                                Column(Modifier.weight(1f).padding(horizontal = 13.dp)) {
                                    Text(
                                        playlist.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        when {
                                            isCheckingDuplicates && selected -> "Checking…"
                                            alreadyAdded -> "Already added"
                                            else -> countLabel
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (alreadyAdded) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Checkbox(checked = selected, onCheckedChange = null)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(com.tuned.app.R.string.common_cancel)) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { requestAdd(selectedPlaylistIds) },
                    enabled = selectedPlaylistIds.isNotEmpty() && !isCheckingDuplicates,
                ) {
                    if (isCheckingDuplicates) {
                        ExpressiveInlineLoadingIndicator(
                            size = 18.dp,
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(Icons.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.size(19.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            selectedPlaylistIds.isEmpty() -> "Add"
                            isCheckingDuplicates -> "Checking"
                            else -> "Add to ${selectedPlaylists.size}"
                        },
                    )
                }
            }
        }
    }
}

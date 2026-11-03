package com.tuned.app.ui.settings

import androidx.compose.material3.OutlinedTextField
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tuned.app.R
import com.tuned.app.ui.theme.ExpressivePillShape

@Composable
internal fun YouTubeAccountRow(
    accountName: String,
    channelHandle: String?,
    onDisconnect: () -> Unit,
    position: GroupPosition,
    isHighlighted: Boolean = false,
) {
    val shape = groupShape(position)
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else androidx.compose.ui.graphics.Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .settingHighlightGlow(isHighlighted = isHighlighted, shape = shape),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(
                Icons.Filled.SmartDisplay,
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "YouTube Music Account",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    accountName.ifBlank { "Connected" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                if (!channelHandle.isNullOrBlank()) {
                    Text(
                        channelHandle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalIconButton(
                onClick = onDisconnect,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Icon(Icons.Filled.Logout, contentDescription = stringResource(R.string.set_disconnect_cd))
            }
        }
    }
}

@Composable
internal fun SpotifyAccountRow(
    onDisconnect: () -> Unit,
    onClick: () -> Unit,
    position: GroupPosition,
    isHighlighted: Boolean = false,
) {
    val shape = groupShape(position)
    Card(
        onClick = onClick,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else androidx.compose.ui.graphics.Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .settingHighlightGlow(isHighlighted = isHighlighted, shape = shape),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(
                Icons.Filled.PlayCircle,
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Spotify Account",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Connected (sp_dc)",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    "Tap to manage session",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalIconButton(
                onClick = onDisconnect,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Icon(Icons.Filled.Logout, contentDescription = stringResource(R.string.set_disconnect_cd))
            }
        }
    }
}

internal fun lastSyncLabel(lastSyncAtMillis: Long): String? {
    if (lastSyncAtMillis <= 0L) return null
    return "Synced ${relativeTime(lastSyncAtMillis)}"
}

internal fun relativeTime(timestampMillis: Long): String {
    if (timestampMillis <= 0L) return "never"
    val delta = System.currentTimeMillis() - timestampMillis
    val minutes = delta / 60_000L
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 60 * 24 -> "${minutes / 60}h ago"
        else -> "${minutes / (60 * 24)}d ago"
    }
}

internal const val LAST_FM_CREATE_KEY_URL = "https://www.last.fm/api/account/create"

@Composable
internal fun LastFmIntegrationCard(
    isConnected: Boolean,
    username: String,
    avatarUrl: String?,
    connecting: Boolean,
    awaitingApproval: Boolean,
    hasApiKey: Boolean,
    onConnect: () -> Unit,
    onCancel: () -> Unit,
    onDisconnect: () -> Unit,
    onSaveKeys: (String, String) -> Unit,
    onRemoveKey: () -> Unit,
    onOpenCreateKeyPage: () -> Unit,
    isHighlighted: Boolean = false,
) {
    var showDisconnectConfirm by remember { mutableStateOf(false) }
    // No shared key exists, so the form starts open until a key is saved.
    var showKeyForm by remember(hasApiKey) { mutableStateOf(!hasApiKey) }
    var keyInput by remember { mutableStateOf("") }
    var secretInput by remember { mutableStateOf("") }

    Card(
        shape = CardOuterShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else androidx.compose.ui.graphics.Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .settingHighlightGlow(isHighlighted = isHighlighted, shape = CardOuterShape),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = if (isConnected) 14.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isConnected) {
                    if (avatarUrl != null) {
                        coil.compose.AsyncImage(
                            model = avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        IconBadge(
                            Icons.Filled.CloudSync,
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                } else {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (isConnected && username.isNotBlank()) username.take(1).uppercase() else "L",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Last.fm",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (isConnected && username.isNotBlank()) username else "Not connected",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    if (!isConnected) {
                        Text(
                            "Stats use local listening until you connect",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (isConnected) {
                    Spacer(Modifier.width(8.dp))
                    FilledTonalIconButton(
                        onClick = { showDisconnectConfirm = true },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) {
                        Icon(Icons.Filled.Logout, contentDescription = stringResource(R.string.set_disconnect_cd))
                    }
                }
            }
            if (!isConnected) {
                when {
                    awaitingApproval || connecting -> {
                        com.tuned.app.ui.common.ExpressiveLoadingIndicator(
                            message = if (connecting) "Connecting to Last.fm…" else "Waiting for approval in the browser…",
                        )
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = onCancel) { Text(stringResource(R.string.common_cancel)) }
                    }
                    else -> {
                        Button(
                            onClick = onConnect,
                            enabled = hasApiKey,
                            shape = ExpressivePillShape,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.set_lfm_connect)) }
                        Text(
                            if (hasApiKey) "Approve in your browser. You can disconnect anytime — Stats keep working locally."
                            else "Add your API key below first, then connect.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (!isConnected) {
                // ── Bring-your-own-key (required, no shared key): Last.fm
                //    rate-limits per API key, so each person adds their own key
                //    here before connecting.
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.set_lfm_key_label),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            if (hasApiKey) stringResource(R.string.set_lfm_key_saved)
                            else stringResource(R.string.set_lfm_key_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { showKeyForm = !showKeyForm }) {
                        Text(if (showKeyForm) "Hide" else if (hasApiKey) "Change" else "Add key")
                    }
                }
                if (showKeyForm) {
                    androidx.compose.material3.OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it.trim() },
                        label = { Text(stringResource(R.string.set_lfm_apikey)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = secretInput,
                        onValueChange = { secretInput = it.trim() },
                        label = { Text(stringResource(R.string.set_lfm_secret)) },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                onSaveKeys(keyInput, secretInput)
                                keyInput = ""
                                secretInput = ""
                                showKeyForm = false
                            },
                            enabled = keyInput.length >= 16 && secretInput.length >= 16,
                            shape = ExpressivePillShape,
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(R.string.set_lfm_save)) }
                        if (hasApiKey) {
                            OutlinedButton(
                                onClick = onRemoveKey,
                                shape = ExpressivePillShape,
                            ) { Text(stringResource(R.string.set_lfm_remove)) }
                        }
                    }
                    TextButton(onClick = onOpenCreateKeyPage) {
                        Text(stringResource(R.string.set_lfm_get))
                    }
                } else {
                    TextButton(onClick = onOpenCreateKeyPage) {
                        Text(stringResource(R.string.set_lfm_how))
                    }
                }
            }
            }
        }
    }

    if (showDisconnectConfirm) {
        AlertDialog(
            onDismissRequest = { showDisconnectConfirm = false },
            title = { Text(stringResource(R.string.set_lfm_disc_title)) },
            text = { Text(stringResource(R.string.set_lfm_disc_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showDisconnectConfirm = false
                    onDisconnect()
                }) { Text(stringResource(R.string.set_lfm_disc_btn)) }
            },
            dismissButton = { TextButton(onClick = { showDisconnectConfirm = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

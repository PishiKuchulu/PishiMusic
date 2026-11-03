package com.tuned.app.ui.playlist

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tuned.app.R
import com.tuned.app.data.generate.GeneratedTrack
import kotlinx.coroutines.launch

/** The Playground sheet, wired up: Library and the Home header both open it. */
@Composable
internal fun PlaygroundHost(
    viewModel: PlaylistViewModel,
    onDismiss: () -> Unit,
    onSaved: (Long) -> Unit = {},
) {
    val musicPlayer = com.tuned.app.ui.player.LocalMusicPlayer.current
    val subtitle = androidx.compose.ui.res.stringResource(com.tuned.app.R.string.playground_title)
    PlaylistPlaygroundSheet(
        create = viewModel::createFromIdea,
        onPlay = { tracks ->
            onDismiss()
            musicPlayer.playQueue(
                tracks.map { track ->
                    com.tuned.app.playback.PlayableTrack(
                        title = track.name,
                        artist = track.artist,
                        album = track.album,
                        artworkUrl = track.artworkUrl,
                        videoId = Regex("v=([A-Za-z0-9_-]{11})").find(track.url)?.groupValues?.get(1),
                    )
                },
                sourceLabel = subtitle,
            )
        },
        onSave = { idea, tracks ->
            onDismiss()
            viewModel.savePlayground(idea, tracks, subtitle, onSaved)
        },
        onDismiss = onDismiss,
    )
}

/**
 * Turns an idea into a playlist. [create] builds the list, [onPlay] and [onSave] act on
 * the result. The list comes from YouTube Music searches for the idea, not a language
 * model, so the sheet says the results can surprise.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaylistPlaygroundSheet(
    create: suspend (String) -> List<GeneratedTrack>,
    onPlay: (List<GeneratedTrack>) -> Unit,
    onSave: (String, List<GeneratedTrack>) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var idea by rememberSaveable { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    var tracks by remember { mutableStateOf<List<GeneratedTrack>?>(null) }

    fun run() {
        if (idea.isBlank() || working) return
        working = true
        scope.launch {
            tracks = runCatching { create(idea.trim()) }.getOrDefault(emptyList())
            working = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.playground_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                value = idea,
                onValueChange = { idea = it },
                placeholder = { Text(stringResource(R.string.playground_hint)) },
                singleLine = true,
                enabled = !working,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { run() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (working) Modifier.playgroundShimmer() else Modifier),
            )
            val result = tracks
            when {
                working -> Text(
                    stringResource(R.string.playground_working),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                result == null -> Button(
                    onClick = { run() },
                    enabled = idea.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.playground_create)) }
                result.isEmpty() -> {
                    Text(
                        stringResource(R.string.playground_nothing),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = { run() }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.playground_again))
                    }
                }
                else -> {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 340.dp)) {
                        itemsIndexed(result, key = { index, track -> "$index:${track.key}" }) { _, track ->
                            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Text(
                                    track.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    track.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onPlay(result) }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.playground_play))
                        }
                        OutlinedButton(onClick = { onSave(idea.trim(), result) }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.playground_save))
                        }
                    }
                    TextButton(onClick = { run() }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.playground_again))
                    }
                }
            }
            Text(
                stringResource(R.string.playground_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A band of light sweeping across while the playlist is being made. */
@Composable
private fun Modifier.playgroundShimmer(): Modifier {
    val highlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    val sweep by rememberInfiniteTransition(label = "playgroundShimmer").animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(1_300, easing = LinearEasing), RepeatMode.Restart),
        label = "playgroundShimmerBand",
    )
    return drawWithContent {
        drawContent()
        val x = sweep * size.width
        drawRect(
            Brush.horizontalGradient(
                colors = listOf(androidx.compose.ui.graphics.Color.Transparent, highlight, androidx.compose.ui.graphics.Color.Transparent),
                startX = x - size.width * 0.3f,
                endX = x + size.width * 0.3f,
            ),
        )
    }
}

package com.tuned.app.ui.player

import com.tuned.app.ui.common.blurSheetContainerColor
import com.tuned.app.ui.common.fadeTopEdge
import com.tuned.app.ui.common.sheetScrollGuard
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tuned.app.R
import androidx.compose.ui.semantics.selected
import com.tuned.app.data.local.LyricsProvider
import com.tuned.app.ui.common.EdgeToEdgeDialogWindow
import com.tuned.app.ui.common.ExpressiveGroup
import com.tuned.app.ui.common.adaptiveContentWidth
import com.tuned.app.ui.common.groupShape
import com.tuned.app.ui.common.safeDrawingBottomPadding

/** The source pickers, grouped by what they give you, so 18 sources can be scanned. */
private val SourceGroups: List<Pair<Int, List<LyricsProvider>>> = listOf(
    R.string.ly_group_auto to listOf(LyricsProvider.AUTO),
    R.string.ly_group_word to listOf(
        LyricsProvider.APPLE_LYRICS,
        LyricsProvider.APPLE_MUSIC,
        LyricsProvider.LRC_RED,
        LyricsProvider.BINI_LYRICS,
        LyricsProvider.LYRICS_PLUS,
        LyricsProvider.BETTER_LYRICS,
        LyricsProvider.BETTER_LYRICS_PORTATO,
        LyricsProvider.KUGOU,
    ),
    R.string.ly_group_line to listOf(
        LyricsProvider.SPOTIFY,
        LyricsProvider.MUSIXMATCH,
        LyricsProvider.SIMP_MUSIC,
        LyricsProvider.LRCLIB,
        LyricsProvider.UNISON,
        LyricsProvider.MEGALOBIZ,
        LyricsProvider.YOUTUBE_CAPTIONS,
    ),
    R.string.ly_group_plain to listOf(
        LyricsProvider.YOUTUBE_MUSIC,
        LyricsProvider.GENIUS,
    ),
)

/**
 * Where the lyrics come from, as a sheet in the song menu's style: the source
 * in use with a "Try another" pill, then every source in grouped cards. Picking
 * a source applies it and closes the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSourceDialog(
    currentSource: String?,
    currentProvider: LyricsProvider?,
    onSelectProvider: (LyricsProvider) -> Unit,
    onTryAnotherSource: () -> Unit,
    onDismiss: () -> Unit,
) {
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.8f
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // Slide down first, then act, so a choice never makes the sheet vanish at once.
    fun closeThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { action() }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
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
        val sheetScroll = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveContentWidth(maxWidth = 600.dp)
                .align(Alignment.CenterHorizontally)
                .heightIn(max = maxHeight)
                .fadeTopEdge(sheetScroll)
                .sheetScrollGuard()
                .padding(horizontal = 14.dp)
                // No stretch at the ends: the list stops where it stops.
                .verticalScroll(sheetScroll, overscrollEffect = null)
                .padding(bottom = 24.dp + safeDrawingBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.ly_source_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 4.dp),
            )
            CurrentSourceCard(
                source = currentSource?.substringBefore(" (")?.takeIf { it.isNotBlank() },
                onTryAnother = { closeThen(onTryAnotherSource) },
            )
            SourceGroups.forEach { (label, providers) ->
                Text(
                    stringResource(label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp, top = 8.dp),
                )
                ExpressiveGroup(rowCount = providers.size) { index, position ->
                    val provider = providers[index]
                    SourceRow(
                        provider = provider,
                        selected = provider == currentProvider,
                        shape = groupShape(position),
                        onClick = { closeThen { onSelectProvider(provider) } },
                    )
                }
            }
        }
    }
}

/** The source in use, with a pill to look for the same song elsewhere. */
@Composable
private fun CurrentSourceCard(source: String?, onTryAnother: () -> Unit) {
    Card(
        shape = groupShape(com.tuned.app.ui.common.GroupPosition.SINGLE),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Lyrics, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.ly_source_now_showing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    source ?: stringResource(R.string.ly_source_none),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            Surface(
                onClick = onTryAnother,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary,
            ) {
                Row(
                    Modifier.padding(start = 12.dp, end = 14.dp, top = 9.dp, bottom = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.ly_try_another_source),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

/** One source: its name, what it gives you in one line, and a check when chosen. */
@Composable
private fun SourceRow(
    provider: LyricsProvider,
    selected: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val isSelected = selected
    Card(
        onClick = onClick,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        // Selection was visual-only (a check icon with a null description):
        // announce it in the row's semantics for TalkBack.
        modifier = Modifier
            .fillMaxWidth()
            .androidx.compose.ui.semantics.semantics {
                this.selected = isSelected
            },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    provider.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    provider.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (selected) {
                Spacer(Modifier.width(10.dp))
                Icon(Icons.Filled.Check, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
        }
    }
}

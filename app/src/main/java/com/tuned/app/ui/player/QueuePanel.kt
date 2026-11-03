@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.tuned.app.ui.player

import com.tuned.app.R
import com.tuned.app.ui.theme.tunedColors
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.Radius
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.AppShape
import com.tuned.app.ui.common.PlayingIndicator
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tuned.app.playback.MusicPlayer
import com.tuned.app.playback.MusicPlayerState
import com.tuned.app.ui.common.TrackContextMenuSheet
import com.tuned.app.ui.common.TrackMenuCapabilities
import com.tuned.app.ui.common.TrackMenuTarget
import com.tuned.app.ui.theme.LocalLiquidGlass
import com.tuned.app.ui.theme.LiquidGlassSurface
import com.tuned.app.ui.theme.liquidGlassChrome
import kotlin.time.Duration.Companion.seconds

internal var isQueueLockedState by mutableStateOf(false)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun QueuePanel(state: MusicPlayerState, player: MusicPlayer, modifier: Modifier = Modifier) {
    val isQueueLocked = isQueueLockedState
    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var menuIndex by remember { mutableStateOf<Int?>(null) }

    // Stable keys so animateItem() can animate moves instead of treating
    // every shifted row as a new item. Duplicates get occurrence suffixes.
    val queueKeys = remember(state.queue) {
        val counts = mutableMapOf<String, Int>()
        state.queue.map { item ->
            val base = item.videoId ?: item.playbackUrl
                ?: "${item.artist}|${item.title}|${item.album}"
            val n = counts.getOrDefault(base, 0)
            counts[base] = n + 1
            "$base#$n"
        }
    }
    val currentQueue by rememberUpdatedState(state.queue)
    val currentQueueKeys by rememberUpdatedState(queueKeys)
    // Abort a stale drag if the queue itself changes underneath us
    // (track ended, clear-upcoming, fresh playQueue, …).
    LaunchedEffect(state.queue.size) {
        if (draggingIndex >= state.queue.size) {
            draggingIndex = -1
            dragOffsetY = 0f
        }
    }
    // Follow the now-playing song in long queues: auto-scroll on every track
    // change (and on first open), unless the user is dragging or has manually
    // scrolled within the last few seconds. The eye button still jumps on demand.
    var userScrollHoldUntil by remember { mutableLongStateOf(0L) }
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) userScrollHoldUntil = System.currentTimeMillis() + 3_000L
    }
    LaunchedEffect(state.currentIndex, state.queue.size) {
        if (draggingIndex != -1 || state.queue.isEmpty()) return@LaunchedEffect
        if (System.currentTimeMillis() < userScrollHoldUntil) return@LaunchedEffect
        val target = state.currentIndex.coerceIn(0, state.queue.size - 1)
        runCatching { listState.animateScrollToItem(target) }
    }
    // True upcoming play order (shuffle/repeat aware). Under shuffle the list
    // below stays in playlist order, so without this strip you can never tell
    // what plays next — and Play-next adds look "lost".
    val shuffleUpcoming = remember(state.queue, state.currentIndex, state.shuffleEnabled, state.repeatMode) {
        if (state.shuffleEnabled) runCatching { player.peekUpcomingIndices(3) }.getOrDefault(emptyList())
        else emptyList()
    }.filter { it in state.queue.indices && it != state.currentIndex }

    val colors = MaterialTheme.tunedColors
    Column(modifier) {
        // Header: what is left, then the two modes that change what is next.
        val upcomingCount = (state.queue.size - state.currentIndex - 1).coerceAtLeast(0)
        var headerMenu by remember { mutableStateOf(false) }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = Space.sm, bottom = Space.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.queue_up_next), style = AppType.titleL, color = Color.White)
                Text(
                    if (state.isEndlessQueue) {
                        "Keeps going with similar songs"
                    } else {
                        // One fact: how many songs come after this one.
                        when (upcomingCount) {
                            0 -> "Nothing after this song"
                            1 -> "1 more song"
                            else -> "$upcomingCount more songs"
                        }
                    },
                    style = AppType.bodyS,
                    color = Color.White.copy(alpha = 0.70f),
                )
            }
            Box {
                IconButton(onClick = { headerMenu = true }, modifier = Modifier.size(Sizes.touch)) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.queue_options_cd), tint = Color.White)
                }
                androidx.compose.material3.DropdownMenu(shape = com.tuned.app.ui.theme.MenuShape, containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh, expanded = headerMenu, onDismissRequest = { headerMenu = false }) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(stringResource(R.string.queue_jump)) },
                        onClick = {
                            headerMenu = false
                            userScrollHoldUntil = 0L
                            scope.launch { listState.animateScrollToItem(state.currentIndex.coerceAtLeast(0)) }
                        },
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(if (isQueueLocked) "Unlock order" else "Lock order") },
                        onClick = {
                            headerMenu = false
                            isQueueLockedState = !isQueueLockedState
                        },
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(stringResource(R.string.queue_clear)) },
                        onClick = {
                            headerMenu = false
                            player.clearUpcoming()
                        },
                    )
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = Space.md),
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            QueueModeChip(Icons.Rounded.Shuffle, stringResource(R.string.queue_shuffle), on = state.shuffleEnabled, onClick = player::toggleShuffle)
            QueueModeChip(
                if (state.repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                stringResource(
                    when (state.repeatMode) {
                        androidx.media3.common.Player.REPEAT_MODE_ONE -> R.string.queue_repeat_one
                        androidx.media3.common.Player.REPEAT_MODE_ALL -> R.string.queue_repeat_all
                        else -> R.string.queue_repeat
                    },
                ),
                on = state.repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF,
                onClick = player::cycleRepeatMode,
            )
            // Apple's AutoPlay: similar songs follow when the queue runs out.
            QueueModeChip(Icons.Rounded.AllInclusive, stringResource(R.string.queue_autoplay), on = state.autoPlayEnabled, onClick = player::toggleAutoPlay)
        }
        if (shuffleUpcoming.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    "Up next in shuffle order",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.60f),
                    fontWeight = FontWeight.Bold,
                )
                shuffleUpcoming.forEachIndexed { order, queueIndex ->
                    val item = state.queue[queueIndex]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { player.seekToQueueItem(queueIndex) }
                            .padding(horizontal = 4.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${order + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(20.dp),
                        )
                        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text(
                                item.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.92f),
                            )
                            Text(
                                item.artist,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.60f),
                            )
                        }
                        Text(
                            "#${queueIndex + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.40f),
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(state.queue, key = { index, _ -> queueKeys.getOrNull(index) ?: index.toString() }) { index, item ->
                val currentIndex by rememberUpdatedState(index)
                val isCurrent = index == state.currentIndex
                val isDragging = index == draggingIndex
                val stableKey = queueKeys.getOrNull(index) ?: index.toString()
                val dismissState = androidx.compose.material3.rememberSwipeToDismissBoxState(
                    confirmValueChange = { dismissValue ->
                        if (dismissValue != androidx.compose.material3.SwipeToDismissBoxValue.Settled) {
                            // Resolve the index at swipe time by stable key: the
                            // composition-time `index` can be stale if the queue
                            // shifted between composition and the gesture, which
                            // removed the wrong item (audit #12).
                            val resolvedIndex = currentQueueKeys.indexOf(stableKey).takeIf { it >= 0 } ?: currentIndex
                            if (dismissValue == androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd) {
                                // Play next: move it right after the playing song; the row stays.
                                val playing = state.currentIndex
                                if (resolvedIndex >= 0 && playing >= 0 && resolvedIndex != playing + 1) {
                                    player.moveQueueItem(resolvedIndex, if (resolvedIndex > playing) playing + 1 else playing)
                                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                }
                                false
                            } else {
                                if (resolvedIndex >= 0) player.removeQueueItem(resolvedIndex)
                                true
                            }
                        } else {
                            false
                        }
                    }
                )
                androidx.compose.material3.SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = !isQueueLocked && !isCurrent,
                    // The playing track cannot be swipe-removed either: removal was
                    // instant, unconfirmed and had no undo in this panel.
                    enableDismissFromEndToStart = !isQueueLocked && !isCurrent,
                    modifier = Modifier.animateItem(),
                    backgroundContent = {
                        // Only drawn while a swipe is in progress. Right: play
                        // next (accent). Left: remove (danger).
                        val dir = dismissState.dismissDirection
                        if (dir != androidx.compose.material3.SwipeToDismissBoxValue.Settled) {
                            val playNext = dir == androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clip(AppShape.md)
                                    .background(if (playNext) colors.accent else colors.danger)
                                    .padding(horizontal = Space.xl),
                                contentAlignment = if (playNext) Alignment.CenterStart else Alignment.CenterEnd,
                            ) {
                                Icon(
                                    if (playNext) Icons.AutoMirrored.Rounded.PlaylistPlay else Icons.Rounded.Delete,
                                    contentDescription = if (playNext) "Play next" else stringResource(com.tuned.app.R.string.player_delete_cd),
                                    tint = Color.White,
                                )
                            }
                        }
                    },
                    content = {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(Sizes.row)
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer {
                                translationY = if (isDragging) dragOffsetY else 0f
                                shadowElevation = if (isDragging) 18f else 0f
                                val s = if (isDragging) 1.025f else 1f
                                scaleX = s
                                scaleY = s
                            }
                            .clip(AppShape.md)
                            .background(if (isDragging) colors.surface2 else Color.Transparent)
                            .combinedClickable(
                                onClick = { player.seekToQueueItem(currentQueueKeys.indexOf(stableKey).takeIf { it >= 0 } ?: currentIndex) },
                                onLongClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuIndex = currentQueueKeys.indexOf(stableKey).takeIf { it >= 0 } ?: currentIndex
                                },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(Sizes.artRow).clip(AppShape.sm)) {
                            PlayerArtwork(item, Modifier.fillMaxSize(), Radius.sm)
                            if (isCurrent) {
                                Box(
                                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
                                    contentAlignment = Alignment.Center,
                                ) { PlayingIndicator(playing = state.isPlaying) }
                            }
                        }
                        Column(Modifier.weight(1f).padding(horizontal = Space.md)) {
                            Text(
                                item.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = AppType.body,
                                color = if (isCurrent) colors.accent else Color.White,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (item.explicit) {
                                    com.tuned.app.ui.common.ExplicitTag(color = Color.White.copy(alpha = 0.62f))
                                    Spacer(Modifier.width(Space.xs + Space.xxs))
                                }
                                Text(
                                    item.artist,
                                    style = AppType.bodyS,
                                    color = Color.White.copy(alpha = 0.62f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (!isQueueLocked) {
                            Icon(
                                Icons.Rounded.DragHandle,
                                stringResource(com.tuned.app.R.string.queue_drag_hint),
                                tint = Color.White.copy(alpha = if (isDragging) 1f else 0.5f),
                                modifier = Modifier
                                    .size(Sizes.touch)
                                    .padding(Space.md)
                                    .pointerInput(stableKey) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                val actualIndex = currentQueueKeys.indexOf(stableKey).takeIf { it >= 0 } ?: currentIndex
                                                draggingIndex = actualIndex
                                                dragOffsetY = 0f
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                            onDragEnd = {
                                                draggingIndex = -1
                                                dragOffsetY = 0f
                                            },
                                            onDragCancel = {
                                                draggingIndex = -1
                                                dragOffsetY = 0f
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val source = draggingIndex
                                                if (source < 0) return@detectDragGesturesAfterLongPress
                                                dragOffsetY += dragAmount.y
                                                val layoutInfo = listState.layoutInfo
                                                val draggedInfo = layoutInfo.visibleItemsInfo
                                                    .firstOrNull { it.index == source }
                                                    ?: return@detectDragGesturesAfterLongPress
                                                val draggedCenter = draggedInfo.offset + draggedInfo.size / 2 + dragOffsetY.toInt()
                                                val target = layoutInfo.visibleItemsInfo.firstOrNull { info ->
                                                    info.index != source &&
                                                        info.index in currentQueue.indices &&
                                                        draggedCenter in info.offset..(info.offset + info.size)
                                                }?.index
                                                if (target != null && target != source) {
                                                    player.moveQueueItem(source, target)
                                                    // Keep the row glued under the finger across
                                                    // the layout shift caused by the move.
                                                    val targetInfo = layoutInfo.visibleItemsInfo
                                                        .firstOrNull { it.index == target }
                                                    if (targetInfo != null) {
                                                        dragOffsetY += (draggedInfo.offset - targetInfo.offset).toFloat()
                                                    }
                                                    draggingIndex = target
                                                    haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                                                }
                                                // Edge auto-scroll while dragging.
                                                val viewportStart = layoutInfo.viewportStartOffset
                                                val viewportEnd = layoutInfo.viewportEndOffset
                                                val edgeZone = 180
                                                when {
                                                    draggedCenter < viewportStart + edgeZone ->
                                                        scope.launch { listState.scrollBy(-28f) }
                                                    draggedCenter > viewportEnd - edgeZone ->
                                                        scope.launch { listState.scrollBy(28f) }
                                                }
                                            },
                                        )
                                    },
                            )
                        }
                    }
                    },
                )
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }

    menuIndex?.let { index ->
        val itemTrack = state.queue.getOrNull(index)
        if (itemTrack != null) {
            com.tuned.app.ui.common.TrackContextMenuSheet(
                target = com.tuned.app.ui.common.TrackMenuTarget.Track(itemTrack.title, itemTrack.artist, itemTrack.album ?: ""),
                capabilities = com.tuned.app.ui.common.TrackMenuCapabilities(showCopyActions = true),
                playableTrack = itemTrack,
                onDismiss = { menuIndex = null },
                onRemoveFromQueue = if (isQueueLocked) null else ({ player.removeQueueItem(index) })
            )
        } else {
            menuIndex = null
        }
    }
}

/** A mode toggle above the queue: accent fill when on. */
@Composable
private fun QueueModeChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, on: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.tunedColors
    Row(
        Modifier
            .height(Space.xxl + Space.xs)
            .clip(AppShape.full)
            .background(if (on) colors.accent else Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = Space.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(Sizes.iconRow))
        Text(label, style = AppType.label, color = Color.White)
    }
}

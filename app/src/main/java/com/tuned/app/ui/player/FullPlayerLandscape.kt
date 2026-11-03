@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.tuned.app.ui.player

import com.tuned.app.R
import androidx.compose.ui.res.stringResource
import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuned.app.playback.MusicPlayer
import com.tuned.app.playback.MusicPlayerState
import com.tuned.app.playback.PlaybackProgressState
import com.tuned.app.playback.PlayableTrack
import com.tuned.app.ui.common.ExpressiveMotion
import com.tuned.app.ui.theme.LocalLiquidGlass
import com.tuned.app.ui.theme.LiquidGlassSurface
import com.tuned.app.ui.theme.liquidGlassChrome
import com.tuned.app.ui.theme.liquidGlassContainerColor
import com.tuned.app.ui.theme.LiquidGlassPreset
import kotlin.math.abs

@Composable
internal fun FullPlayerLandscape(
    track: PlayableTrack,
    state: MusicPlayerState,
    player: MusicPlayer,
    progressState: StateFlow<PlaybackProgressState>?,
    wavySeekbarEnabled: Boolean,
    showSleeveCanvas: Boolean,
    canvas: com.tuned.app.data.canvas.CanvasArtwork?,
    ambientColor: Color,
    ambientCompanion: Color,
    ambientDeep: Color,
    isLiked: Boolean,
    onToggleLiked: () -> Unit,
    onDoubleTapLike: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: () -> Unit = {},
    onTabChange: (FullPlayerTab) -> Unit,
    shownDismissY: Float,
    swipeThreshold: Float,
    onOpenSignalPath: () -> Unit = {},
    onOpenAudioControls: () -> Unit = {},
) {
    var isArtworkDragging by remember { mutableStateOf(false) }
    var artworkDragX by remember { mutableFloatStateOf(0f) }
    val shownArtworkX by animateFloatAsState(
        targetValue = artworkDragX,
        animationSpec = if (isArtworkDragging) snap() else androidx.compose.animation.core.spring(dampingRatio = 1f, stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
        label = "landscapeArtworkDragX",
    )
    var seekOverlaySeconds by remember { mutableIntStateOf(5) }
    var seekOverlayDirection by remember { mutableStateOf<SeekDirection?>(null) }
    var lastTapSide by remember { mutableStateOf<SeekDirection?>(null) }
    var lastTapTimestamp by remember { mutableLongStateOf(0L) }
    var lastLikeTapTimestamp by remember { mutableLongStateOf(0L) }
    var seekResetJob by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var canvasAspect by remember { mutableFloatStateOf(1f) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left Column: Large Album Artwork & Canvas with gestures
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            val artworkSize = (minOf(maxWidth, maxHeight) - 16.dp)
                .coerceAtLeast(0.dp)
                .coerceAtMost(480.dp)

            val glowAlpha by animateFloatAsState(
                targetValue = if (state.isPlaying) 0.65f else 0.35f,
                animationSpec = tween(600),
                label = "landscapeArtworkGlowAlpha",
            )
            Box(
                modifier = Modifier
                    .size(artworkSize + 36.dp)
                    .graphicsLayer {
                        translationX = shownArtworkX * 0.7f
                        alpha = glowAlpha
                    }
                    .background(
                        Brush.radialGradient(
                            0.0f to ambientColor.copy(alpha = 0.50f),
                            0.50f to ambientCompanion.copy(alpha = 0.22f),
                            1.0f to Color.Transparent,
                        ),
                        shape = CircleShape,
                    ),
            )

            val artworkPlayingScale by animateFloatAsState(
                targetValue = if (state.isPlaying) 1.0f else 0.92f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "landscapeArtworkPlayingScale",
            )

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.88f),
                tonalElevation = 6.dp,
                shadowElevation = if (state.isPlaying) 28.dp else 12.dp,
                modifier = Modifier
                    .size(artworkSize)
                    .graphicsLayer {
                        scaleX = artworkPlayingScale
                        scaleY = artworkPlayingScale
                        translationX = shownArtworkX
                    }
                    .pointerInput(track.videoId, track.title) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var isDrag = false
                            val touchSlop = viewConfiguration.touchSlop
                            val initialX = down.position.x
                            val initialY = down.position.y

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                if (!change.pressed) {
                                    if (isDrag) {
                                        isArtworkDragging = false
                                        when {
                                            artworkDragX < -swipeThreshold -> player.next()
                                            artworkDragX > swipeThreshold -> player.previous()
                                        }
                                        artworkDragX = 0f
                                    } else {
                                        val now = SystemClock.elapsedRealtime()
                                        val side = when {
                                            initialX < size.width * 0.34f -> SeekDirection.REWIND
                                            initialX > size.width * 0.66f -> SeekDirection.FORWARD
                                            else -> null
                                        }

                                        if (side == null) {
                                            lastTapSide = null
                                            lastTapTimestamp = 0L
                                            if (lastLikeTapTimestamp != 0L && now - lastLikeTapTimestamp < 450L) {
                                                lastLikeTapTimestamp = 0L
                                                onDoubleTapLike()
                                            } else {
                                                lastLikeTapTimestamp = now
                                            }
                                        } else {
                                            lastLikeTapTimestamp = 0L
                                            if (lastTapSide != side) {
                                                seekResetJob?.cancel()
                                                seekOverlayDirection = null
                                                lastTapSide = side
                                                lastTapTimestamp = now
                                            } else if (now - lastTapTimestamp < 450L) {
                                                val newSeconds = if (seekOverlayDirection == side) seekOverlaySeconds + 5 else 5
                                                seekOverlaySeconds = newSeconds
                                                seekOverlayDirection = side
                                                lastTapTimestamp = now
                                                val deltaMs = if (side == SeekDirection.FORWARD) 5_000L else -5_000L
                                                val newPos = (player.state.value.positionMs + deltaMs).coerceIn(0L, player.state.value.durationMs.coerceAtLeast(0L))
                                                player.seekTo(newPos)

                                                seekResetJob?.cancel()
                                                seekResetJob = coroutineScope.launch {
                                                    delay(700L)
                                                    seekOverlayDirection = null
                                                    lastTapSide = null
                                                }
                                            } else {
                                                lastTapTimestamp = now
                                                lastTapSide = side
                                                seekOverlayDirection = null
                                            }
                                        }
                                    }
                                    break
                                }

                                if (change.isConsumed) {
                                    isArtworkDragging = false
                                    artworkDragX = 0f
                                    lastLikeTapTimestamp = 0L
                                    lastTapTimestamp = 0L
                                    lastTapSide = null
                                    break
                                }

                                val dx = change.position.x - initialX
                                val dy = change.position.y - initialY
                                if (!isDrag) {
                                    if (kotlin.math.abs(dx) > touchSlop && kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                                        isDrag = true
                                        isArtworkDragging = true
                                        lastLikeTapTimestamp = 0L
                                        lastTapTimestamp = 0L
                                        lastTapSide = null
                                        change.consume()
                                    }
                                } else {
                                    change.consume()
                                    artworkDragX = dx
                                }
                            }
                        }
                    },
            ) {
                Box(Modifier.fillMaxSize()) {
                    PlayerArtwork(
                        track = track,
                        modifier = Modifier.fillMaxSize(),
                        corner = 24.dp,
                        canvas = if (showSleeveCanvas) canvas else null,
                        isPlaying = state.isPlaying,
                        pausedForTransition = shownDismissY > 1f,
                        onAspectRatioChanged = { canvasAspect = it },
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = seekOverlayDirection == SeekDirection.REWIND,
                enter = fadeIn(tween(100)) + scaleIn(ExpressiveMotion.spatialSpring(), initialScale = 0.88f),
                exit = fadeOut(tween(200)),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(0.42f)
                    .height(130.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp, topEnd = 72.dp, bottomEnd = 72.dp))
                        .background(Color.Black.copy(alpha = 0.58f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.22f),
                            modifier = Modifier.size(52.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.FastRewind,
                                    contentDescription = stringResource(R.string.player_rewind_cd),
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "-${seekOverlaySeconds}s",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                        )
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = seekOverlayDirection == SeekDirection.FORWARD,
                enter = fadeIn(tween(100)) + scaleIn(ExpressiveMotion.spatialSpring(), initialScale = 0.88f),
                exit = fadeOut(tween(200)),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth(0.42f)
                    .height(130.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp, topStart = 72.dp, bottomStart = 72.dp))
                        .background(Color.Black.copy(alpha = 0.58f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.22f),
                            modifier = Modifier.size(52.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.FastForward,
                                    contentDescription = stringResource(R.string.player_forward_cd),
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "+${seekOverlaySeconds}s",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                        )
                    }
                }
            }
        }

        // Right Column: Controls & Metadata
        Column(
            modifier = Modifier
                .weight(1.15f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE),
                    ) {
                        Text(
                            track.title,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                letterSpacing = (-0.35).sp,
                                fontWeight = FontWeight.ExtraBold,
                            ),
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false,
                        )
                        if (track.explicit) {
                            Spacer(Modifier.width(com.tuned.app.ui.theme.Space.sm))
                            com.tuned.app.ui.common.ExplicitTag(
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = com.tuned.app.ui.theme.ExplicitMark.textLarge,
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    val splitArtists = remember(track.artist) {
                        com.tuned.app.util.ArtistHelper.splitArtists(track.artist)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE),
                    ) {
                        splitArtists.forEachIndexed { index, artName ->
                            Text(
                                text = artName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                                color = Color.White.copy(alpha = 0.72f),
                                modifier = Modifier
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null,
                                    ) { onOpenArtist(artName) },
                            )
                            if (index < splitArtists.lastIndex) {
                                Text(
                                    text = ", ",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Normal,
                                    ),
                                    color = Color.White.copy(alpha = 0.50f),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.width(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val likeInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    val isLikePressed by likeInteraction.collectIsPressedAsState()
                    val likeScale by animateFloatAsState(
                        targetValue = if (isLikePressed) 0.78f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                        label = "landscapeLikeScale",
                    )
                    LiquidGlassSurface(
                        glassModifier = Modifier.liquidGlassChrome(CircleShape, LocalLiquidGlass.current, LiquidGlassPreset.PlayerControls, interactionSource = likeInteraction),
                        onClick = onToggleLiked,
                        interactionSource = likeInteraction,
                        shape = CircleShape,
                        color = if (isLiked) Color.White.copy(alpha = 0.92f) else liquidGlassContainerColor(Color.White.copy(alpha = 0.14f)),
                        contentColor = if (isLiked) {
                            Color(0xFFE91E63)
                        } else {
                            Color.White.copy(alpha = 0.85f)
                        },
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .size(50.dp)
                            .graphicsLayer {
                                scaleX = likeScale
                                scaleY = likeScale
                            },
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isLiked) {
                                    stringResource(R.string.art_unfavorite_cd)
                                } else {
                                    stringResource(R.string.art_favorite_cd)
                                },
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }

                    val lyricsInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    val isLyricsPressed by lyricsInteraction.collectIsPressedAsState()
                    val lyricsScale by animateFloatAsState(
                        targetValue = if (isLyricsPressed) 0.82f else 1.0f,
                        animationSpec = ExpressiveMotion.spatialSpring(),
                        label = "landscapeLyricsScale",
                    )
                    LiquidGlassSurface(
                        glassModifier = Modifier.liquidGlassChrome(CircleShape, LocalLiquidGlass.current, LiquidGlassPreset.PlayerControls, interactionSource = lyricsInteraction),
                        onClick = { onTabChange(FullPlayerTab.LYRICS) },
                        interactionSource = lyricsInteraction,
                        shape = CircleShape,
                        color = liquidGlassContainerColor(Color.White.copy(alpha = 0.14f)),
                        contentColor = Color.White.copy(alpha = 0.90f),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .size(50.dp)
                            .graphicsLayer {
                                scaleX = lyricsScale
                                scaleY = lyricsScale
                            },
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.FormatQuote,
                                contentDescription = stringResource(R.string.player_show_lyrics_cd),
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            SeekBar(
                progressState = progressState ?: player.progressState,
                isPlaying = state.isPlaying,
                trackKey = track.videoId ?: "${track.artist}|${track.title}",
                wavyEnabled = wavySeekbarEnabled,
                onSeek = player::seekTo,
                isTranslucent = LocalLiquidGlass.current,
                fallbackDurationMs = track.durationMs ?: state.durationMs,
                primaryColor = ambientColor,
                secondaryColor = ambientCompanion,
                tertiaryColor = ambientDeep,
            )

            Spacer(Modifier.height(14.dp))

            MainControls(state, player, isTranslucent = LocalLiquidGlass.current)

            Spacer(Modifier.height(10.dp))

            val signalPath by player.signalPath.collectAsStateWithLifecycle()
            AppleMusicQualityBadge(
                state = state,
                signalPath = signalPath,
                isTranslucent = LocalLiquidGlass.current,
                onClick = onOpenSignalPath,
            )

            Spacer(Modifier.height(14.dp))

            PlayerUtilityControls(
                state = state,
                player = player,
                isTranslucent = LocalLiquidGlass.current,
                onOpenAudioControls = onOpenAudioControls,
            )
        }
    }
}

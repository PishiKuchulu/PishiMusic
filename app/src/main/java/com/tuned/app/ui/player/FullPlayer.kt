@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.tuned.app.ui.player

import androidx.compose.ui.graphics.RectangleShape
import kotlin.math.roundToInt
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.SystemClock
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.tuned.app.ui.common.PredictiveBackScreen
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.graphics.lerp
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import com.tuned.app.ui.common.adaptiveContentWidth
import com.tuned.app.ui.common.isTabletOrWideScreen
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.tuned.app.playback.MusicPlayer
import com.tuned.app.playback.MusicPlayerState
import com.tuned.app.playback.PlaybackProgressState
import com.tuned.app.data.artwork.ArtworkNormalizer
import com.tuned.app.playback.PlayableTrack
import com.tuned.app.ui.common.ArtworkImage
import com.tuned.app.ui.common.ArtworkViewModel
import com.tuned.app.ui.common.ExpressiveMotion
import com.tuned.app.ui.common.TrackContextMenuSheet
import com.tuned.app.ui.common.TrackMenuCapabilities
import com.tuned.app.ui.common.TrackMenuTarget
import com.tuned.app.ui.theme.LocalLiquidGlass
import com.tuned.app.ui.theme.LiquidGlassSurface
import com.tuned.app.ui.theme.liquidGlassChrome
import com.tuned.app.ui.theme.liquidGlassContainerColor
import com.tuned.app.ui.theme.liquidGlassSource
import com.tuned.app.ui.theme.isLiquidGlassBackdropSupported
import com.tuned.app.ui.theme.LocalLiquidGlassBackdrop
import com.tuned.app.ui.theme.LocalLiquidGlassOverlayBackdrop
import com.tuned.app.ui.theme.LiquidGlassPreset
import com.tuned.app.ui.theme.rememberLayerBackdrop
import kotlin.time.Duration.Companion.seconds
import kotlin.math.abs

internal enum class SeekDirection { REWIND, FORWARD }

/**
 * Dynamic ambient lyrics background overlay.
 * Derives colors strictly from the track cover-art palette (ambientColor, ambientCompanion, ambientDeep).
 * Slowly modulates the atmospheric orbs line-by-line as lyrics advance, ensuring:
 *  1. Micro-shifts: consecutive lines maintain cohesive color harmony with very subtle, gentle differentiation.
 *  2. Zero visibility issues: controlled luminance and dark base keep synchronized text crisp and clear.
 *  3. Performance: progressState is sampled via derivedStateOf so recomposition only occurs when the active line changes.
 */
@Composable
internal fun LyricsDynamicAmbientBackdrop(
    isLyricsTab: Boolean,
    lyricsState: LyricsUiState,
    progressState: StateFlow<PlaybackProgressState>,
    lyricsOffsetMs: Long,
    ambientColor: Color,
    ambientCompanion: Color,
    ambientDeep: Color,
    bgWidth: Float,
    bgHeight: Float,
    bgMaxDimension: Float,
    modifier: Modifier = Modifier,
) {
    val lyricsBackdropAlpha by animateFloatAsState(
        targetValue = if (isLyricsTab) 1f else 0f,
        animationSpec = tween(350),
        label = "lyricsBackdropAlpha",
    )

    if (lyricsBackdropAlpha <= 0.001f) return

    val progress by progressState.collectAsStateWithLifecycle()
    val lines = (lyricsState as? LyricsUiState.Success)?.lines.orEmpty()
    val activeLine by remember(lines) {
        derivedStateOf {
            if (lines.isNotEmpty()) {
                activeLineIndex(lines, progress.positionMs + lyricsOffsetMs).coerceAtLeast(0)
            } else {
                0
            }
        }
    }

    // Micro-modulation strictly bounded within the cover art's palette tone:
    // angle advances gently per line index:
    val lineAngle = activeLine * 0.35f
    val shiftRatio1 = sin(lineAngle) * 0.05f + 0.06f
    val shiftRatio2 = cos(lineAngle * 0.70f) * 0.05f + 0.06f
    val shiftRatio3 = sin(lineAngle * 0.50f + 1f) * 0.04f + 0.05f

    val targetColor1 = lerp(ambientColor, ambientCompanion, shiftRatio1)
    val targetColor2 = lerp(ambientCompanion, ambientDeep, shiftRatio2)
    val targetColor3 = lerp(ambientDeep, ambientColor, shiftRatio3)

    val animatedColor1 by animateColorAsState(
        targetValue = targetColor1,
        animationSpec = tween(1200, easing = LinearOutSlowInEasing),
        label = "lyricAmbientColor1",
    )
    val animatedColor2 by animateColorAsState(
        targetValue = targetColor2,
        animationSpec = tween(1200, easing = LinearOutSlowInEasing),
        label = "lyricAmbientColor2",
    )
    val animatedColor3 by animateColorAsState(
        targetValue = targetColor3,
        animationSpec = tween(1200, easing = LinearOutSlowInEasing),
        label = "lyricAmbientColor3",
    )

    // Gentle spatial drift of the orbs (subtle 2% to 4% offset)
    val targetCenterX1 = 0.25f + sin(lineAngle * 0.45f) * 0.04f
    val targetCenterY1 = 0.22f + cos(lineAngle * 0.45f) * 0.03f
    val targetCenterX2 = 0.86f - cos(lineAngle * 0.55f) * 0.04f
    val targetCenterY2 = 0.67f + sin(lineAngle * 0.55f) * 0.03f

    val animCenterX1 by animateFloatAsState(targetCenterX1, tween(1400, easing = LinearOutSlowInEasing), label = "lyricCenterX1")
    val animCenterY1 by animateFloatAsState(targetCenterY1, tween(1400, easing = LinearOutSlowInEasing), label = "lyricCenterY1")
    val animCenterX2 by animateFloatAsState(targetCenterX2, tween(1400, easing = LinearOutSlowInEasing), label = "lyricCenterX2")
    val animCenterY2 by animateFloatAsState(targetCenterY2, tween(1400, easing = LinearOutSlowInEasing), label = "lyricCenterY2")

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = lyricsBackdropAlpha }
    ) {
        // Dark scrim underlay to protect contrast and eliminate visibility issues
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.36f)))

        // Primary cover art ambient orb (subtle line-by-line micro-shift)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        0f to animatedColor1.copy(alpha = 0.50f),
                        0.48f to animatedColor1.copy(alpha = 0.18f),
                        1f to Color.Transparent,
                        center = Offset(bgWidth * animCenterX1, bgHeight * animCenterY1),
                        radius = bgMaxDimension * 0.85f,
                    )
                )
        )
        // Secondary cover art companion orb
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        0f to animatedColor2.copy(alpha = 0.44f),
                        0.50f to animatedColor2.copy(alpha = 0.15f),
                        1f to Color.Transparent,
                        center = Offset(bgWidth * animCenterX2, bgHeight * animCenterY2),
                        radius = bgMaxDimension * 0.78f,
                    )
                )
        )
        // Deep anchor orb
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        0f to animatedColor3.copy(alpha = 0.36f),
                        0.55f to animatedColor3.copy(alpha = 0.12f),
                        1f to Color.Transparent,
                        center = Offset(bgWidth * 0.18f, bgHeight * 0.84f),
                        radius = bgMaxDimension * 0.70f,
                    )
                )
        )
    }
}

@Composable
internal fun FullPlayer(
    state: MusicPlayerState,
    progressState: StateFlow<PlaybackProgressState>,
    player: MusicPlayer,
    lyricsState: LyricsUiState,
    wavySeekbarEnabled: Boolean = true,
    rotatingBackgroundEnabled: Boolean = true,
    lyricsRotatingBackgroundEnabled: Boolean = true,
    lyricsOffsetMs: Long = 0L,
    onSetLyricsOffsetMs: ((Long) -> Unit)? = null,
    lyricsFontScale: Float = 1.0f,
    onSetLyricsFontScale: ((Float) -> Unit)? = null,
    /** 0 = floating lyrics buttons never auto-hide; 2..10 = idle seconds. */
    lyricsButtonsAutoHideSeconds: Int = 5,
    /** Language the lyric translate button targets; blank = app language. */
    translationLanguage: String = "",
    canvas: com.tuned.app.data.canvas.CanvasArtwork? = null,
    canvasEnabled: Boolean = true,
    canvasFullBleedEnabled: Boolean = true,
    /** The Music Video setting: shows the Song | Video switch in the top bar. */
    musicVideoEnabled: Boolean = false,
    /** 432Hz tuning on/off, shown in the signal-path sheet's DSP stage. */
    tuning432Enabled: Boolean = false,
    /** Loudness-normalization preamp (dB) when active; null hides the preamp row in the signal path. */
    loudnessPreampDb: Float? = null,
    currentTab: FullPlayerTab,
    onTabChange: (FullPlayerTab) -> Unit,
    onRetryLyrics: () -> Unit,
    onCollapse: () -> Unit,
    onOpenArtist: (String) -> Unit = {},
    onOpenAlbum: () -> Unit = {},
    isLiked: Boolean = false,
    onToggleLiked: () -> Unit = {},
    onDoubleTapLike: () -> Unit = {},
    onDragStatusChange: (Boolean) -> Unit = {},
    onSelectLyricsProvider: ((com.tuned.app.data.local.LyricsProvider) -> Unit)? = null,
    onTryAnotherLyricsSource: (() -> Unit)? = null,
) {
    val track = state.current ?: return
    var showLyricsOffsetDialog by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isTablet = isTabletOrWideScreen()
    val isTabletLandscape = isLandscape && (isTablet || configuration.screenWidthDp >= 600)
    val isCanvasActive = canvasEnabled && canvas != null
    val showFullBleed = canvasFullBleedEnabled
    val showSleeveCanvas = isCanvasActive && (!showFullBleed || isTabletLandscape)
    // activeCanvas is the single gate every render branch below keys off, so it also
    // carries the Animated Album Canvas toggle. The ViewModel nulls canvasState
    // asynchronously, so without this a disabled toggle still paints one frame of
    // motion artwork over the cover.
    val activeCanvas = remember(canvas, showFullBleed, canvasEnabled, isTabletLandscape) {
        if (!canvasEnabled) {
            null
        } else {
            val tall = canvas?.tallUrl
            if (showFullBleed && !isTabletLandscape && canvas != null && !tall.isNullOrBlank()) {
                canvas.copy(url = tall)
            } else {
                canvas
            }
        }
    }
    var canvasAspect by remember(activeCanvas?.url) { mutableFloatStateOf(0f) }
    // How much of the hero the canvas currently covers — its own fade-in alpha
    // times the presentation alpha. The static hero fades out in step with it,
    // so the cover never shows through the video (its bottom fade, its 320 ms
    // reveal, the repaint window on a track change) and the two never fight for
    // the same pixels. A disposed canvas reports 0, so the hero returns the
    // moment a track without a canvas starts.
    var canvasCover by remember { mutableFloatStateOf(0f) }
    var lyricsFullscreen by remember(currentTab) { mutableStateOf(false) }
    var lyricsControlsVisible by remember(currentTab) { mutableStateOf(true) }
    val view = LocalView.current
    LyricsFullscreenEffect(view, lyricsFullscreen)
    var showTrackMenu by remember(track.videoId, track.title) { mutableStateOf(false) }
    // Music video in place of the artwork ("Watch video"), and its full-screen view.
    val musicVideoOn by MusicVideoMode.on.collectAsStateWithLifecycle()
    var musicVideoFullscreen by remember { mutableStateOf(false) }
    // Held here, above the tabs: lyrics and the queue leave the video running, so coming back
    // shows it at once, and the page stays dimmed on every tab while it plays.
    val musicVideoSession = if (musicVideoEnabled && musicVideoOn && !musicVideoFullscreen) {
        rememberMusicVideoSession(track, player)
    } else {
        null
    }
    val musicVideoCover by animateFloatAsState(
        targetValue = if (musicVideoSession?.showing == true) 1f else 0f,
        animationSpec = tween(350),
        label = "musicVideoCover",
    )
    val musicVideoUi = MusicVideoUi(
        session = musicVideoSession,
        cover = musicVideoCover,
        onFullscreen = { musicVideoFullscreen = true },
    )
    var showSignalPath by remember { mutableStateOf(false) }
    var showAudioControls by remember { mutableStateOf(false) }
    val signalPath by player.signalPath.collectAsStateWithLifecycle()
    var dismissDragY by remember(track.videoId, track.title) { mutableFloatStateOf(0f) }
    var isDismissDragging by remember { mutableStateOf(false) }
    LaunchedEffect(isDismissDragging) { onDragStatusChange(isDismissDragging) }
    val shownDismissY by animateFloatAsState(
        targetValue = dismissDragY,
        animationSpec = if (isDismissDragging) snap() else ExpressiveMotion.spatialSpring(),
        label = "fullPlayerDismissY",
    )
    val swipeThreshold = with(LocalDensity.current) { 88.dp.toPx() }

    val ambient = rememberPlayerAmbient(track)
    val resolvedAmbientUrl = ambient.resolvedArtworkUrl
    val ambientColor = ambient.primary
    val ambientCompanion = ambient.companion
    val ambientDeep = ambient.deep

    fun Modifier.playerVerticalSwipe(enabled: Boolean): Modifier = if (!enabled) this else pointerInput(track.videoId, track.title, currentTab) {
        detectVerticalDragGestures(
            onDragStart = { isDismissDragging = true },
            onDragCancel = {
                isDismissDragging = false
                dismissDragY = 0f
            },
            onDragEnd = {
                isDismissDragging = false
                when {
                    currentTab != FullPlayerTab.NOW_PLAYING && dismissDragY > swipeThreshold -> {
                        onTabChange(FullPlayerTab.NOW_PLAYING)
                        dismissDragY = 0f
                    }
                    currentTab == FullPlayerTab.NOW_PLAYING && dismissDragY < -swipeThreshold -> {
                        onTabChange(FullPlayerTab.QUEUE)
                        dismissDragY = 0f
                    }
                    // Closing keeps the drag offset, so the player carries on
                    // down from where the finger let go instead of snapping back
                    // up first.
                    currentTab == FullPlayerTab.NOW_PLAYING && dismissDragY > swipeThreshold -> onCollapse()
                    else -> dismissDragY = 0f
                }
            },
        ) { change, amount ->
            val updatedDrag = if (currentTab != FullPlayerTab.NOW_PLAYING) {
                (dismissDragY + amount).coerceAtLeast(0f)
            } else {
                dismissDragY + amount
            }
            if (updatedDrag != dismissDragY) change.consume()
            dismissDragY = updatedDrag
        }
    }

    // Unconditional remember keeps composition stable; usage gated by glass flag.
    // Single backdrop, sibling source below — never nest glass inside its own capture.
    val playerBackdrop = rememberLayerBackdrop()
    val fullGlass = LocalLiquidGlass.current && isLiquidGlassBackdropSupported()
    CompositionLocalProvider(
        LocalLiquidGlassBackdrop provides if (fullGlass) playerBackdrop else null,
        LocalLiquidGlassOverlayBackdrop provides if (fullGlass) playerBackdrop else null,
    ) {
    // With full-screen artwork the player keeps the card's 28dp corners at all
    // times (like the phone's own screen corners), so the edge-to-edge picture
    // never snaps between round and square when the morph starts or ends.
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = if (showFullBleed) RoundedCornerShape(28.dp) else RectangleShape,
        modifier = Modifier
            .fillMaxSize()
            // A layout offset, not a draw translation, so the morph back into
            // the mini player starts from wherever the drag left the card.
            .offset { IntOffset(0, shownDismissY.coerceAtLeast(0f).roundToInt()) }
            .nowPlayingContainerBounds(RoundedCornerShape(28.dp), fullPlayer = true, darkenOnClose = showFullBleed)
            .graphicsLayer {
                val playerHeight = size.height.coerceAtLeast(1f)
                alpha = (1f - shownDismissY.coerceAtLeast(0f) / (playerHeight * 1.5f)).coerceIn(0.72f, 1f)
            }
            .playerVerticalSwipe(enabled = currentTab == FullPlayerTab.NOW_PLAYING),
    ) {
        CompositionLocalProvider(LocalMusicVideo provides musicVideoUi) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val bgWidth = constraints.maxWidth.toFloat()
            val bgHeight = constraints.maxHeight.toFloat()
            val bgMaxDimension = maxOf(bgWidth, bgHeight, 1f)
            var heroBottomPx by remember { mutableFloatStateOf(0f) }

            val isLyricsTab = currentTab == FullPlayerTab.LYRICS
            val showLyricsFluid = lyricsRotatingBackgroundEnabled && rotatingBackgroundEnabled
            // Drives the Now Playing <-> Lyrics backdrop hand-over: the fluid
            // layer fades out while the full-bleed cover backdrop fades in when
            // lyrics rotating background is disabled. When enabled, the fluid
            // background stays active across tabs.
            val lyricsVeil by animateFloatAsState(
                targetValue = if (isLyricsTab && !showLyricsFluid) 1f else 0f,
                animationSpec = tween(350),
                label = "lyricsVeil",
            )
            FullPlayerBackdrop(
                track = track,
                state = state,
                currentTab = currentTab,
                lyricsState = lyricsState,
                progressState = progressState,
                lyricsOffsetMs = lyricsOffsetMs,
                rotatingBackgroundEnabled = rotatingBackgroundEnabled,
                lyricsRotatingBackgroundEnabled = lyricsRotatingBackgroundEnabled,
                isLyricsTab = isLyricsTab,
                showLyricsFluid = showLyricsFluid,
                lyricsVeil = lyricsVeil,
                ambientColor = ambientColor,
                ambientCompanion = ambientCompanion,
                ambientDeep = ambientDeep,
                bgWidth = bgWidth,
                bgHeight = bgHeight,
                bgMaxDimension = bgMaxDimension,
                showFullBleed = showFullBleed,
                isTabletLandscape = isTabletLandscape,
                activeCanvas = activeCanvas,
                canvasAspect = canvasAspect,
                onCanvasAspectChanged = { canvasAspect = it },
                canvasCover = canvasCover,
                onCanvasCoverChanged = { canvasCover = it },
                heroBottomPx = heroBottomPx,
                resolvedAmbientUrl = resolvedAmbientUrl,
                pausedForTransition = shownDismissY > 1f || currentTab != FullPlayerTab.NOW_PLAYING,
                glassSource = if (fullGlass) playerBackdrop else null,
                modifier = Modifier.matchParentSize(),
            )
            // While the music video plays the page goes near black, so the video reads as a screen.
            if (musicVideoCover > 0f) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = musicVideoCover * MUSIC_VIDEO_DIM)))
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // ── Header: slimmer, calmer, premium ─────────────────────
                val showHeader = !lyricsFullscreen && (currentTab != FullPlayerTab.LYRICS || lyricsControlsVisible || showLyricsOffsetDialog || showTrackMenu)
                AnimatedVisibility(
                    visible = showHeader,
                    enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                    exit = fadeOut(tween(200)) + shrinkVertically(tween(200)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isTabletLandscape) {
                                    Modifier.widthIn(max = 1200.dp).fillMaxWidth()
                                } else {
                                    Modifier.adaptiveContentWidth(maxWidth = 640.dp)
                                }
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        FullPlayerTopBar(
                            currentTab = currentTab,
                            track = track,
                            sourceLabel = state.sourceLabel,
                            isTabletLandscape = isTabletLandscape,
                            // Timing moved to the lyrics toolbar; the header stays back · title · menu.
                            showLyricsOffsetButton = false,
                            lyricsOffsetMs = lyricsOffsetMs,
                            onBackOrCollapse = {
                                if (currentTab != FullPlayerTab.NOW_PLAYING) onTabChange(FullPlayerTab.NOW_PLAYING) else onCollapse()
                            },
                            onOpenLyricsOffset = { showLyricsOffsetDialog = true },
                            onOpenMenu = { showTrackMenu = true },
                            videoSelected = if (musicVideoEnabled && currentTab == FullPlayerTab.NOW_PLAYING) musicVideoOn else null,
                            onSelectVideo = { video -> if (video) MusicVideoMode.watch() else MusicVideoMode.stop() },
                            modifier = Modifier.playerVerticalSwipe(enabled = currentTab != FullPlayerTab.NOW_PLAYING),
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }

                FullPlayerTabContent(
                    currentTab = currentTab,
                    onTabChange = onTabChange,
                    track = track,
                    state = state,
                    player = player,
                    lyricsState = lyricsState,
                    progressState = progressState,
                    wavySeekbarEnabled = wavySeekbarEnabled,
                    lyricsOffsetMs = lyricsOffsetMs,
                    lyricsFontScale = lyricsFontScale,
                    onSetLyricsFontScale = onSetLyricsFontScale,
                    lyricsButtonsAutoHideSeconds = lyricsButtonsAutoHideSeconds,
                    translationLanguage = translationLanguage,
                    onRetryLyrics = onRetryLyrics,
                    onSelectLyricsProvider = onSelectLyricsProvider,
                    onTryAnotherLyricsSource = onTryAnotherLyricsSource,
                    ambientColor = ambientColor,
                    ambientCompanion = ambientCompanion,
                    ambientDeep = ambientDeep,
                    resolvedAmbientUrl = resolvedAmbientUrl,
                    isTabletLandscape = isTabletLandscape,
                    showFullBleed = showFullBleed,
                    showSleeveCanvas = showSleeveCanvas,
                    activeCanvas = activeCanvas,
                    canvas = canvas,
                    isLiked = isLiked,
                    onToggleLiked = onToggleLiked,
                    onDoubleTapLike = onDoubleTapLike,
                    onOpenArtist = onOpenArtist,
                    onOpenAlbum = onOpenAlbum,
                    shownDismissY = shownDismissY,
                    swipeThreshold = swipeThreshold,
                    signalPath = signalPath,
                    onHeroBottomChanged = { if (it != heroBottomPx) heroBottomPx = it },
                    onCanvasAspectChanged = { canvasAspect = it },
                    lyricsFullscreen = lyricsFullscreen,
                    onLyricsFullscreenChange = { lyricsFullscreen = it },
                    lyricsControlsVisible = lyricsControlsVisible,
                    onLyricsControlsVisibleChange = { lyricsControlsVisible = it },
                    onOpenLyricsOffset = { showLyricsOffsetDialog = true },
                    onOpenSignalPath = { showSignalPath = true },
                    onOpenAudioControls = { showAudioControls = true },
                    modifier = Modifier.weight(1f),
                )
                state.error?.takeUnless { lyricsFullscreen }?.let { message ->
                    PlayerErrorBanner(message = message, onRetry = player::retry, onDismiss = player::clearError)
                }
            }
        }
        }
    }
    if (showTrackMenu) {
        TrackContextMenuSheet(
            target = TrackMenuTarget.Track(track.title, track.artist, ""),
            capabilities = TrackMenuCapabilities(
                showCopyActions = true,
                showDeleteScrobble = false,
            ),
            playableTrack = track,
            onDismiss = { showTrackMenu = false },
            onPlayInTuned = { player.play(track, sourceLabel = state.sourceLabel) },
        )
    }
    if (musicVideoFullscreen) {
        MusicVideoFullscreen(
            track = track,
            musicPlayer = player,
            onDismiss = { musicVideoFullscreen = false },
        )
    }
    if (showLyricsOffsetDialog && onSetLyricsOffsetMs != null) {
        LyricsOffsetDialog(
            currentMs = lyricsOffsetMs,
            onSelect = onSetLyricsOffsetMs,
            onDismiss = { showLyricsOffsetDialog = false },
        )
    }
    if (showSignalPath) {
        SignalPathSheet(
            player = player,
            signalPath = signalPath,
            onDismiss = { showSignalPath = false },
            tuning432Enabled = tuning432Enabled,
            loudnessPreampDb = loudnessPreampDb,
        )
    }
    if (showAudioControls) {
        AudioControlsSheet(
            player = player,
            state = state,
            onDismiss = { showAudioControls = false },
        )
    }
}
}

@Composable
internal fun PlayerArtwork(
    track: PlayableTrack,
    modifier: Modifier,
    corner: androidx.compose.ui.unit.Dp,
    // One decode size for the mini player, the full player and the queue, so
    // each side of the morph reuses the cover already in memory instead of
    // reloading it and fading in from a dark placeholder.
    decodeSizePx: Int? = PLAYER_COVER_PX,
    canvas: com.tuned.app.data.canvas.CanvasArtwork? = null,
    isPlaying: Boolean = false,
    pausedForTransition: Boolean = false,
    transformations: List<coil.transform.Transformation> = emptyList(),
    onAspectRatioChanged: (Float) -> Unit = {},
    /** The player's pinned cover for this track; null while it is being chosen. */
    coverUrl: String? = null,
    useCoverUrl: Boolean = false,
) {
    // The sleeve cover and its canvas are stacked in the same bounds: the
    // cover is the placeholder while the video loads, then hands the pixels
    // over as the canvas's own alpha climbs. Without this the static art
    // stays at full opacity underneath the video and shows through wherever
    // the canvas is transparent.
    var sleeveCover by remember(canvas) { mutableFloatStateOf(0f) }
    Box(modifier.clip(RoundedCornerShape(corner)).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
        ArtworkImage(
            name = track.title,
            artist = track.artist,
            embeddedUrl = track.artworkUrl,
            fallbackIcon = Icons.Filled.MusicNote,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha =
                        if (sleeveCover >= 0.95f) 0f else (1f - sleeveCover).coerceIn(0f, 1f)
                },
            decodeSizePx = decodeSizePx,
            transformations = transformations,
            useFixedUrl = useCoverUrl,
            fixedUrl = coverUrl,
        )
        if (canvas != null) {
            CanvasArtworkPlayer(
                canvas = canvas,
                isPlaying = isPlaying,
                pausedForTransition = pausedForTransition,
                onAspectRatioChanged = onAspectRatioChanged,
                onCoverChanged = { sleeveCover = it },
                contentMode = CanvasContentMode.CROP,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Decode size shared by every now-playing cover (see PlayerArtwork). */
internal const val PLAYER_COVER_PX = 1024

/** How dark the page goes behind a playing music video. */
private const val MUSIC_VIDEO_DIM = 0.82f

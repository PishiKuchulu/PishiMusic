@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.tuned.app.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.layout
import com.tuned.app.playback.isSpatialAudioCodec
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.Space
import com.tuned.app.playback.SignalPathReport
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.runtime.produceState
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

/**
 * The full player's backdrop: the fluid (or blurred) artwork, the ambient
 * colour wash, contrast scrims, the full-bleed cover and Canvas video, and the
 * blurred-cover veil the lyrics tab fades in.
 */
@Composable
internal fun FullPlayerBackdrop(
    track: PlayableTrack,
    state: MusicPlayerState,
    currentTab: FullPlayerTab,
    lyricsState: LyricsUiState,
    progressState: StateFlow<PlaybackProgressState>,
    lyricsOffsetMs: Long,
    rotatingBackgroundEnabled: Boolean,
    lyricsRotatingBackgroundEnabled: Boolean,
    isLyricsTab: Boolean,
    showLyricsFluid: Boolean,
    lyricsVeil: Float,
    ambientColor: Color,
    ambientCompanion: Color,
    ambientDeep: Color,
    bgWidth: Float,
    bgHeight: Float,
    bgMaxDimension: Float,
    showFullBleed: Boolean,
    isTabletLandscape: Boolean,
    activeCanvas: com.tuned.app.data.canvas.CanvasArtwork?,
    canvasAspect: Float,
    onCanvasAspectChanged: (Float) -> Unit,
    canvasCover: Float,
    onCanvasCoverChanged: (Float) -> Unit,
    heroBottomPx: Float,
    resolvedAmbientUrl: String?,
    pausedForTransition: Boolean,
    glassSource: com.kyant.backdrop.backdrops.LayerBackdrop?,
    modifier: Modifier = Modifier,
) {
    Box(modifier.liquidGlassSource(glassSource)) {
        FluidArtworkBackground(
            track = track,
            coverUrl = resolvedAmbientUrl,
            isPlaying = state.isPlaying,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = (1f - lyricsVeil).coerceIn(0f, 1f) },
            extraBlur = isLyricsTab,
            rotatingBackgroundEnabled = if (isLyricsTab) showLyricsFluid else rotatingBackgroundEnabled,
            lyricsRotatingBackgroundEnabled = lyricsRotatingBackgroundEnabled,
            fallback = {
                val staticBlurTransform = remember(currentTab) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        listOf(
                            BlurTransformation(
                                radius = if (currentTab == FullPlayerTab.LYRICS) 25 else 18,
                                maxDimension = 100,
                            )
                        )
                    } else {
                        emptyList()
                    }
                }
                PlayerArtwork(
                    track = track,
                    coverUrl = resolvedAmbientUrl,
                    useCoverUrl = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.35f
                            scaleY = 1.35f
                            alpha = 0.9f
                        }
                        .then(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                // Low-RAM / incapable devices get a shallow blur:
                                // the 36dp full-screen blur is the fallback path's
                                // most expensive draw (isFluidSupported already
                                // encodes the low-RAM gate used by the fluid path).
                                val backdropBlur = if (isFluidSupported()) {
                                    if (currentTab == FullPlayerTab.LYRICS) 36.dp else 20.dp
                                } else {
                                    12.dp
                                }
                                Modifier.blur(backdropBlur)
                            } else {
                                Modifier
                            }
                        ),
                    corner = 0.dp,
                    decodeSizePx = 200,
                    transformations = staticBlurTransform,
                )
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (isLyricsTab) 0.40f else 0.52f)))
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                0f to ambientColor.copy(alpha = 0.58f),
                                0.45f to ambientColor.copy(alpha = 0.22f),
                                1f to Color.Transparent,
                                center = androidx.compose.ui.geometry.Offset(bgWidth * 0.25f, bgHeight * 0.20f),
                                radius = bgMaxDimension * 0.85f,
                            )
                        )
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                0f to ambientCompanion.copy(alpha = 0.52f),
                                0.50f to ambientCompanion.copy(alpha = 0.20f),
                                1f to Color.Transparent,
                                center = androidx.compose.ui.geometry.Offset(bgWidth * 0.88f, bgHeight * 0.65f),
                                radius = bgMaxDimension * 0.78f,
                            )
                        )
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                0f to ambientDeep.copy(alpha = 0.42f),
                                0.55f to ambientDeep.copy(alpha = 0.14f),
                                1f to Color.Transparent,
                                center = androidx.compose.ui.geometry.Offset(bgWidth * 0.15f, bgHeight * 0.82f),
                                radius = bgMaxDimension * 0.70f,
                            )
                        )
                )
            }
        )
        // Dynamic ambient lyrics gradient overlay: subtly shifts line-by-line strictly
        // using the album cover art palette, preserving zero text visibility issues.
        LyricsDynamicAmbientBackdrop(
            isLyricsTab = isLyricsTab,
            lyricsState = lyricsState,
            progressState = progressState,
            lyricsOffsetMs = lyricsOffsetMs,
            ambientColor = ambientColor,
            ambientCompanion = ambientCompanion,
            ambientDeep = ambientDeep,
            bgWidth = bgWidth,
            bgHeight = bgHeight,
            bgMaxDimension = bgMaxDimension,
        )
        // Contrast scrim gradient (ensures text & controls are clear while preserving vibrant colors)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.00f to Color.Black.copy(alpha = 0.35f),
                        0.28f to Color.Black.copy(alpha = 0.15f),
                        0.65f to Color.Black.copy(alpha = 0.40f),
                        1.00f to Color.Black.copy(alpha = 0.72f),
                    )
                )
        )
        // Subtle edge vignette
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        0f to Color.Transparent,
                        0.65f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.30f),
                        center = androidx.compose.ui.geometry.Offset(
                            bgWidth * 0.50f,
                            bgHeight * 0.40f,
                        ),
                        radius = bgMaxDimension * 0.80f,
                    ),
                ),
        )
        if (showFullBleed && !isTabletLandscape) {
            val isTallCanvas = remember(activeCanvas, canvasAspect) {
                if (activeCanvas == null) false
                else if (canvasAspect > 0f) {
                    // True vertical video (9:16 is ~0.56, 3:4 is 0.75; <= 0.72 is tall vertical)
                    canvasAspect <= 0.72f
                } else {
                    // Spotify Canvas is natively 9:16 vertical by specification
                    activeCanvas.source == com.tuned.app.data.canvas.CanvasSource.SPOTIFY
                }
            }

            val heroHeight = if (heroBottomPx > 0f) {
                with(LocalDensity.current) { heroBottomPx.toDp() }
            } else {
                with(LocalDensity.current) { (bgHeight * 0.54f).toDp() }
            }
            val heroAlpha by animateFloatAsState(
                targetValue = if (currentTab == FullPlayerTab.NOW_PLAYING) 1f else 0f,
                animationSpec = tween(280, easing = FastOutSlowInEasing),
                label = "heroAlpha",
            )
            // Static cover first, canvas on top: the hero is the
            // placeholder while the video loads, and hands the pixels
            // over as the canvas's own alpha climbs to full.
            val heroCover =
                if (canvasCover >= 0.95f) 0f else (1f - canvasCover).coerceIn(0f, 1f)
            // A playing music video takes the artwork's place.
            val videoCover = LocalMusicVideo.current?.cover ?: 0f
            StaticArtworkHero(
                name = track.title,
                artist = track.artist,
                embeddedUrl = null,
                resolvedUrl = resolvedAmbientUrl,
                bottomFade = 0.2f,
                alpha = heroAlpha * heroCover * (1f - videoCover),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .height(heroHeight),
            )
            if (activeCanvas != null) {
                CanvasArtworkPlayer(
                    canvas = activeCanvas,
                    isPlaying = state.isPlaying,
                    contentMode = CanvasContentMode.CROP,
                    alignPortraitTop = true,
                    bottomFade = if (isTallCanvas) 0f else 0.2f,
                    presentationAlpha = { heroAlpha * (1f - videoCover) },
                    onCoverChanged = onCanvasCoverChanged,
                    onAspectRatioChanged = onCanvasAspectChanged,
                    pausedForTransition = pausedForTransition,
                    modifier = if (isTallCanvas) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                            .align(Alignment.TopStart)
                            .fillMaxWidth()
                            .height(heroHeight)
                    },
                )

                // For true full-bleed canvas: a subtle contrast gradient scrim so top header and
                // bottom playback controls/title remain 100% legible without blurring the video
                if (isTallCanvas) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = canvasCover * heroAlpha * (1f - videoCover) }
                            .background(
                                Brush.verticalGradient(
                                    0.00f to Color.Black.copy(alpha = 0.32f),
                                    0.12f to Color.Transparent,
                                    0.55f to Color.Transparent,
                                    0.78f to Color.Black.copy(alpha = 0.36f),
                                    1.00f to Color.Black.copy(alpha = 0.72f),
                                )
                            )
                    )
                }
            }
        }

        // Lyrics tab backdrop: instead of stacking ever more dim over
        // the fluid, the artwork itself takes over — a full-bleed
        // blurred cover under a soft scrim, so the backdrop keeps the
        // album's colours and composition (a touch darker than the
        // reference progression for a calmer read).
        // Keeps the top bar readable on any cover.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(HeaderFadeHeight)
                .background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.45f), 1f to Color.Transparent)),
        )
        if (lyricsVeil > 0.01f) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = lyricsVeil }) {
                PlayerArtwork(
                    track = track,
                    coverUrl = resolvedAmbientUrl,
                    useCoverUrl = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.35f
                            scaleY = 1.35f
                        }
                        .then(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                Modifier.blur(36.dp)
                            } else {
                                Modifier
                            }
                        ),
                    corner = 0.dp,
                    decodeSizePx = 200,
                    transformations = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        listOf(BlurTransformation(radius = 25, maxDimension = 100))
                    } else {
                        emptyList()
                    },
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.00f to Color.Black.copy(alpha = 0.37f),
                                0.35f to Color.Black.copy(alpha = 0.47f),
                                0.70f to Color.Black.copy(alpha = 0.64f),
                                1.00f to Color.Black.copy(alpha = 0.67f),
                            )
                        )
                )
            }
        }
    }
}

/** Top bar: back or collapse, the tab title over its context line, the lyrics offset button (lyrics tab) and the song menu. */
@Composable
internal fun FullPlayerTopBar(
    currentTab: FullPlayerTab,
    track: PlayableTrack,
    sourceLabel: String,
    isTabletLandscape: Boolean,
    showLyricsOffsetButton: Boolean,
    lyricsOffsetMs: Long,
    onBackOrCollapse: () -> Unit,
    onOpenLyricsOffset: () -> Unit,
    onOpenMenu: () -> Unit,
    /** Song | Video switch state, YouTube Music style; null hides it (setting off, other tabs). */
    videoSelected: Boolean? = null,
    onSelectVideo: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = if (isTabletLandscape) 32.dp else Space.xl)
            .then(modifier),
    ) {
        // FullPlayer sits on a dark blurred-artwork scrim in both light and
        // dark mode, so foreground must use the white overlay palette —
        // never MaterialTheme onSurface (near-black in light mode).
        LiquidGlassSurface(
            onClick = onBackOrCollapse,
            shape = CircleShape,
            color = Color.Transparent,
            contentColor = Color.White.copy(alpha = 0.94f),
            glassModifier = Modifier,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(Sizes.touch),
        ) {
            Icon(
                if (currentTab != FullPlayerTab.NOW_PLAYING) Icons.Filled.ArrowBack else Icons.Filled.ExpandMore,
                if (currentTab != FullPlayerTab.NOW_PLAYING) "Back to player" else "Minimize player",
                modifier = Modifier.size(26.dp),
            )
        }
        if (videoSelected != null) {
            SongVideoSwitch(
                videoSelected = videoSelected,
                onSelect = onSelectVideo,
                modifier = Modifier.align(Alignment.Center),
            )
        } else Column(
            // Just clear of the buttons on each side (two on the right when the lyrics offset
            // button shows); a longer name scrolls instead of being cut.
            Modifier
                .align(Alignment.Center)
                .padding(horizontal = if (showLyricsOffsetButton) Sizes.touch * 2 else Sizes.touch + Space.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                when (currentTab) {
                    FullPlayerTab.NOW_PLAYING -> "Playing from"
                    FullPlayerTab.LYRICS -> track.artist
                    FullPlayerTab.QUEUE -> "Playing from"
                },
                style = AppType.caption,
                color = Color.White.copy(alpha = 0.70f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (currentTab == FullPlayerTab.LYRICS) {
                    // The artist is on the line above, so "(feat. …)" only crowds the title.
                    track.title.replace(FEAT_SUFFIX, "").trim().ifBlank { track.title }
                } else {
                    sourceLabel.takeIf { it.isNotBlank() } ?: "Tuned"
                },
                style = AppType.label,
                color = Color.White,
                maxLines = 1,
                // The lyrics header keeps a still title ending in "…"; elsewhere a long name
                // scrolls and fades at its edges instead of showing cut letters.
                overflow = if (currentTab == FullPlayerTab.LYRICS) TextOverflow.Ellipsis else TextOverflow.Clip,
                modifier = if (currentTab == FullPlayerTab.LYRICS) Modifier
                else Modifier.marqueeEdgeFade().basicMarquee(iterations = Int.MAX_VALUE),
            )
        }
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (showLyricsOffsetButton) {
                LiquidGlassSurface(
                    onClick = onOpenLyricsOffset,
                    shape = CircleShape,
                    color = if (lyricsOffsetMs != 0L) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    else Color.Transparent,
                    contentColor = if (lyricsOffsetMs != 0L) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.94f),
                    glassModifier = Modifier,
                    modifier = Modifier.size(Sizes.touch),
                ) {
                    Icon(
                        Icons.Filled.Timer,
                        contentDescription = stringResource(com.tuned.app.R.string.player_lyrics_offset_cd),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            if (currentTab != FullPlayerTab.QUEUE) {
            LiquidGlassSurface(
                onClick = onOpenMenu,
                shape = CircleShape,
                color = Color.Transparent,
                contentColor = Color.White.copy(alpha = 0.94f),
                glassModifier = Modifier,
                modifier = Modifier.size(Sizes.touch),
            ) {
                Icon(
                    Icons.Filled.MoreVert,
                    stringResource(com.tuned.app.R.string.pd_song_options_cd),
                    modifier = Modifier.size(22.dp),
                )
            }
            }
        }
    }
}

/**
 * The cover on Now playing (the sleeve, when full-bleed artwork is off) and
 * its gestures: swipe sideways to skip, double-tap the middle to like,
 * double-tap an edge to seek 5s with an overlay that counts up.
 */
@Composable
internal fun NowPlayingArtworkStage(
    track: PlayableTrack,
    coverUrl: String?,
    state: MusicPlayerState,
    player: MusicPlayer,
    sleeveAlpha: Float,
    showSleeveCanvas: Boolean,
    canvas: com.tuned.app.data.canvas.CanvasArtwork?,
    ambientColor: Color,
    ambientCompanion: Color,
    pausedForTransition: Boolean,
    swipeThreshold: Float,
    onDoubleTapLike: () -> Unit,
    onHeroBottomChanged: (Float) -> Unit,
    onCanvasAspectChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val autoMixTransition by player.autoMixTransition.collectAsStateWithLifecycle()
    val autoMixProgress = rememberAutoMixProgress(autoMixTransition)
    // The cover on screen when a blend starts, which can differ from the track's own artwork.
    var settledCover by remember { mutableStateOf(coverUrl) }
    // Was a state write during composition; same update, now in an effect.
    LaunchedEffect(coverUrl, autoMixTransition) {
        if (autoMixTransition == null && coverUrl != null && settledCover != coverUrl) settledCover = coverUrl
    }
    var isArtworkDragging by remember(track.videoId, track.title) { mutableStateOf(false) }
    var artworkDragX by remember(track.videoId, track.title) { mutableFloatStateOf(0f) }
    var seekOverlayDirection by remember(track.videoId, track.title) { mutableStateOf<SeekDirection?>(null) }
    var seekOverlaySeconds by remember(track.videoId, track.title) { mutableIntStateOf(0) }
    var lastTapTimestamp by remember(track.videoId, track.title) { mutableLongStateOf(0L) }
    var lastTapSide by remember(track.videoId, track.title) { mutableStateOf<SeekDirection?>(null) }
    var lastLikeTapTimestamp by remember(track.videoId, track.title) { mutableLongStateOf(0L) }
    var seekResetJob by remember(track.videoId, track.title) { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()
    // A plain slide: the cover follows the finger at half speed and settles back with no
    // overshoot. (It used to tilt with the drag and bounce back on a bouncy spring.)
    val shownArtworkX by animateFloatAsState(
        targetValue = artworkDragX * 0.5f,
        animationSpec = if (isArtworkDragging) snap() else spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow),
        label = "fullPlayerArtworkX",
    )
    BoxWithConstraints(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                val rootPos = coordinates.positionInRoot()
                val bottom = rootPos.y + coordinates.size.height
                if (bottom > 0f) onHeroBottomChanged(bottom)
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
                                    // The center third owns Like only. Clear any pending
                                    // side sequence so it can never complete a seek.
                                    lastTapSide = null
                                    lastTapTimestamp = 0L
                                    if (lastLikeTapTimestamp != 0L && now - lastLikeTapTimestamp < 450L) {
                                        lastLikeTapTimestamp = 0L
                                        onDoubleTapLike()
                                    } else {
                                        lastLikeTapTimestamp = now
                                    }
                                } else {
                                    // Preserve the existing edge double-tap seek behavior.
                                    // An edge tap cannot complete a center Like sequence.
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
        contentAlignment = Alignment.Center,
    ) {
        // The cover spans the column's width, so its edges sit on the same
        // 24dp margin as the title, and is centred in the space between the
        // top bar and the title (on short screens the height limits it).
        val artworkSize = minOf(maxWidth, maxHeight - Space.lg)
            .coerceAtLeast(0.dp)
            .coerceAtMost(SleeveMaxSize)

        // A playing music video takes the sleeve's place.
        val musicVideo = LocalMusicVideo.current
        val shownSleeveAlpha = sleeveAlpha * (1f - (musicVideo?.cover ?: 0f))
        if (shownSleeveAlpha > 0.001f) {
            val playerMorphing = LocalPlayerVisibilityScope.current?.transition?.isRunning == true
            val artworkPlayingScale by animateFloatAsState(
                // Full size while the player opens or closes: the morph travels to the cover's
                // real bounds, so a paused cover at 94% jumped at both ends of it.
                targetValue = if (state.isPlaying || playerMorphing) 1.0f else 0.94f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "artworkPlayingScale",
            )

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.88f * shownSleeveAlpha),
                tonalElevation = 6.dp * shownSleeveAlpha,
                shadowElevation = if (state.isPlaying) 28.dp * shownSleeveAlpha else 12.dp * shownSleeveAlpha,
                modifier = Modifier
                    .nowPlayingArtworkBounds(22.dp)
                    .size(artworkSize)
                    .graphicsLayer {
                        scaleX = artworkPlayingScale
                        scaleY = artworkPlayingScale
                        translationX = shownArtworkX
                        alpha = shownSleeveAlpha * (1f - (kotlin.math.abs(shownArtworkX) / size.width).coerceIn(0f, 0.35f))
                    },
            ) {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().autoMixIncomingCover(autoMixProgress)) {
                    PlayerArtwork(
                        track = track,
                        coverUrl = coverUrl,
                        useCoverUrl = true,
                        modifier = Modifier.fillMaxSize(),
                        // The sleeve Surface rounds it; the image itself stays square
                        // so it matches the mini player's in the morph.
                        corner = 0.dp,
                        canvas = if (showSleeveCanvas) canvas else null,
                        isPlaying = state.isPlaying,
                        pausedForTransition = pausedForTransition,
                        onAspectRatioChanged = onCanvasAspectChanged,
                    )
                    }
                    AutoMixArtworkBlend(autoMixTransition, settledCover.takeIf { it != coverUrl }, autoMixProgress)
                }
            }
        }

        musicVideo?.session?.let { session ->
            MusicVideoStage(
                session = session,
                onFullscreen = musicVideo.onFullscreen,
                modifier = Modifier.fillMaxSize(),
            )
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
                                contentDescription = stringResource(com.tuned.app.R.string.player_seek_back_cd),
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
                                contentDescription = stringResource(com.tuned.app.R.string.player_seek_fwd_cd),
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
}

/** Title, artists (each a link), and the like button. */
@Composable
internal fun NowPlayingTrackInfo(
    track: PlayableTrack,
    isLiked: Boolean,
    onToggleLiked: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: () -> Unit = {},
) {
    val haptics = com.tuned.app.ui.theme.rememberHaptics()
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            // The explicit chip rides in the marquee, right after the title's last word.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    // Tapping the title opens the song's album.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenAlbum,
                    )
                    .marqueeEdgeFade(20.dp)
                    .basicMarquee(iterations = Int.MAX_VALUE),
            ) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        letterSpacing = (-0.35).sp,
                        fontWeight = FontWeight.ExtraBold,
                        // Drop the font's spacing under the title so the artist sits close.
                        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.LastLineBottom),
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
            val splitArtists = remember(track.artist) {
                com.tuned.app.util.ArtistHelper.splitArtists(track.artist)
            }
            // One line that scrolls like the title, each name still its own tap target.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .marqueeEdgeFade(20.dp)
                    .basicMarquee(iterations = Int.MAX_VALUE),
            ) {
                splitArtists.forEachIndexed { index, artName ->
                    Text(
                        text = artName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.FirstLineTop),
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
                                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.FirstLineTop),
                            ),
                            color = Color.White.copy(alpha = 0.50f),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                label = "likeScale",
            )
            LiquidGlassSurface(
                glassModifier = Modifier.liquidGlassChrome(CircleShape, LocalLiquidGlass.current, LiquidGlassPreset.PlayerControls, interactionSource = likeInteraction),
                onClick = {
                    haptics.perform(com.tuned.app.ui.theme.HapticKind.Confirm)
                    onToggleLiked()
                },
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
                    .size(46.dp)
                    .graphicsLayer {
                        scaleX = likeScale
                        scaleY = likeScale
                    },
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isLiked) "Unlike song" else "Like song",
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

/** Playback error card: tap to retry, close to dismiss. */
@Composable
internal fun PlayerErrorBanner(message: String, onRetry: () -> Unit, onDismiss: () -> Unit) {
    Surface(
        onClick = onRetry,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.ErrorOutline, null, modifier = Modifier.size(21.dp))
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(com.tuned.app.R.string.fpd_retry),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(Icons.Filled.Close, stringResource(com.tuned.app.R.string.gen_dismiss), modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * One cover per track for the whole player (full-screen cover, sleeve, lyrics
 * backdrop, moving background and colours), chosen once and then kept, so the
 * picture and its colours load a single time. A cover already known is used
 * at once; otherwise the album cover lookup gets a moment (0.8s when YouTube
 * Music's square art can stand in, longer when only a video thumbnail exists,
 * which is never shown for songs). See [ArtworkNormalizer.coverFor].
 */
@Composable
internal fun rememberPinnedCover(track: PlayableTrack, artworkViewModel: ArtworkViewModel): String? {
    val key = remember(track.title, track.artist) { ArtworkNormalizer.cacheKey(track.title, track.artist) }
    val embedded = track.artworkUrl
    val local = embedded?.startsWith("file://") == true
    val known = artworkViewModel.resolved.value[key]
    val initial = if (local || known != null || ArtworkNormalizer.isYouTubeMusicCover(embedded)) ArtworkNormalizer.coverFor(embedded, known) else null
    // Keyed on the track: a new track starts from its own choice, never the last one's.
    var pinned by remember(key, embedded) { mutableStateOf(initial) }
    LaunchedEffect(key, embedded) {
        if (pinned != null || local || known != null) return@LaunchedEffect
        artworkViewModel.resolve(track.title, track.artist)
        val squareStandIn = ArtworkNormalizer.coverFor(embedded, null)
        val lookup = artworkViewModel.resolved.map { it[key] }.filterNotNull()
        val resolved = withTimeoutOrNull(if (squareStandIn != null) 800L else 10_000L) { lookup.first() }
        pinned = ArtworkNormalizer.coverFor(embedded, resolved)
    }
    return pinned
}

/** Colours taken from the cover for the player's ambient wash, and the pinned cover URL. */
internal data class PlayerAmbient(
    val resolvedArtworkUrl: String?,
    val primary: Color,
    val companion: Color,
    val deep: Color,
)

@Composable
internal fun rememberPlayerAmbient(track: PlayableTrack): PlayerAmbient {
    // Dominant cover-art color for the ambient background glow. Extracted
    // once per track through the shared Coil loader (normally a cache hit)
    // with Palette; any failure leaves the standard surface gradient.
    val context = LocalContext.current
    val artworkViewModel: ArtworkViewModel = hiltViewModel()
    val resolvedAmbientUrl = rememberPinnedCover(track, artworkViewModel)

    // Start from the colours found the last time this track was open: the player is rebuilt each
    // time it opens, and waiting for the extraction again made the play pill jump from the
    // default to the cover's colour a moment (up to 1-2s) after opening.
    val ambientKey = ambientKeyOf(track)
    val known = remember(ambientKey) { synchronized(ambientCache) { ambientCache[ambientKey] } }
    // Not keyed to the track: the old colours hold until the new ones are found, so a song
    // change crossfades cover to cover instead of dipping through the default accent.
    var ambientPrimary by remember { mutableStateOf(known?.primary) }
    var ambientSecondary by remember { mutableStateOf(known?.companion) }
    var ambientTertiary by remember { mutableStateOf(known?.deep) }
    // Keyed to the track too: songs on one album share a cover URL, and a new song must still
    // get its colours without the player being reopened.
    LaunchedEffect(ambientKey, resolvedAmbientUrl) {
        known?.let {
            ambientPrimary = it.primary
            ambientSecondary = it.companion
            ambientTertiary = it.deep
        }
        val url = resolvedAmbientUrl?.takeIf { it.isNotBlank() }
        if (url == null) {
            if (known == null) {
                ambientPrimary = null
                ambientSecondary = null
                ambientTertiary = null
            }
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false)
                    .size(128)
                    .build()
                val bitmap = ((context.imageLoader.execute(request) as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
                    ?: return@runCatching
                val palette = Palette.from(bitmap).clearFilters().generate()
                // A "vibrant" swatch can be a few tinted pixels on an otherwise
                // grey cover; only use one that covers a real part of the image.
                val dominant = palette.dominantSwatch
                val minPopulation = (dominant?.population ?: 0) / 10
                fun Palette.Swatch?.real() = this?.takeIf { it.population >= minPopulation }
                val p = palette.vibrantSwatch.real() ?: dominant ?: palette.mutedSwatch
                val s = palette.lightVibrantSwatch.real() ?: palette.darkVibrantSwatch.real() ?: palette.mutedSwatch ?: dominant
                val t = palette.darkVibrantSwatch.real() ?: palette.darkMutedSwatch ?: dominant
                // Black-and-white covers get a neutral grey backdrop, not a tint.
                val grey = isBlackAndWhite(bitmap)
                fun pick(swatch: Palette.Swatch) = Color(swatch.rgb).let { if (grey) it.desaturated() else it }
                if (p != null) ambientPrimary = pick(p)
                if (s != null) ambientSecondary = pick(s)
                if (t != null) ambientTertiary = pick(t)
                synchronized(ambientCache) {
                    ambientCache[ambientKey] = AmbientSwatches(ambientPrimary, ambientSecondary, ambientTertiary)
                }
            }
        }
    }
    val ambientColor by animateColorAsState(
        targetValue = ambientPrimary ?: MaterialTheme.colorScheme.primary,
        animationSpec = tween(700),
        label = "playerAmbientColor",
    )
    val ambientCompanion by animateColorAsState(
        targetValue = ambientSecondary ?: androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.tertiary, ambientColor, 0.42f),
        animationSpec = tween(700),
        label = "playerAmbientCompanion",
    )
    val ambientDeep by animateColorAsState(
        targetValue = ambientTertiary ?: androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.secondary, ambientColor, 0.25f),
        animationSpec = tween(700),
        label = "playerAmbientDeep",
    )

    return PlayerAmbient(resolvedAmbientUrl, ambientColor, ambientCompanion, ambientDeep)
}

/** Cover colours found for a track (see rememberPlayerAmbient). */
private data class AmbientSwatches(val primary: Color?, val companion: Color?, val deep: Color?)

private fun ambientKeyOf(track: PlayableTrack): String =
    track.videoId?.takeIf { it.isNotBlank() } ?: "${track.title}|${track.artist}"

/** The last 32 tracks' cover colours, newest last. */
private val ambientCache = object : LinkedHashMap<String, AmbientSwatches>(32, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, AmbientSwatches>?) = size > 32
}

/** The same brightness with no colour. */
private fun Color.desaturated(): Color {
    val l = 0.299f * red + 0.587f * green + 0.114f * blue
    return Color(l, l, l, alpha)
}

/** Now playing on phones: the cover, then title, scrub bar with quality, transport and the utility row. */
@Composable
internal fun NowPlayingColumn(
    track: PlayableTrack,
    state: MusicPlayerState,
    player: MusicPlayer,
    progressState: StateFlow<PlaybackProgressState>,
    wavySeekbarEnabled: Boolean,
    resolvedAmbientUrl: String?,
    activeCanvas: com.tuned.app.data.canvas.CanvasArtwork?,
    showFullBleed: Boolean,
    showSleeveCanvas: Boolean,
    canvas: com.tuned.app.data.canvas.CanvasArtwork?,
    ambientColor: Color,
    ambientCompanion: Color,
    ambientDeep: Color,
    pausedForTransition: Boolean,
    swipeThreshold: Float,
    onDoubleTapLike: () -> Unit,
    onHeroBottomChanged: (Float) -> Unit,
    onCanvasAspectChanged: (Float) -> Unit,
    isLiked: Boolean,
    onToggleLiked: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: () -> Unit = {},
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    signalPath: SignalPathReport,
    onOpenSignalPath: () -> Unit,
    onOpenAudioControls: () -> Unit,
) {
    // The current layout, tightened: one 24dp margin, quality under the
    // scrub bar instead of in its own row, the play pill in the cover's colour.
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = Space.xl)
            .padding(bottom = Space.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val hasArtwork = !track.artworkUrl.isNullOrBlank() || !resolvedAmbientUrl.isNullOrBlank() || activeCanvas != null
        val sleeveAlpha by animateFloatAsState(
            targetValue = if (showFullBleed && hasArtwork) 0f else 1f,
            animationSpec = tween(350),
            label = "sleeveAlpha",
        )
        NowPlayingArtworkStage(
            track = track,
            coverUrl = resolvedAmbientUrl,
            state = state,
            player = player,
            sleeveAlpha = sleeveAlpha,
            showSleeveCanvas = showSleeveCanvas,
            canvas = canvas,
            ambientColor = ambientColor,
            ambientCompanion = ambientCompanion,
            pausedForTransition = pausedForTransition,
            swipeThreshold = swipeThreshold,
            onDoubleTapLike = onDoubleTapLike,
            onHeroBottomChanged = onHeroBottomChanged,
            onCanvasAspectChanged = onCanvasAspectChanged,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
        NowPlayingTrackInfo(
            track = track,
            isLiked = isLiked,
            onToggleLiked = onToggleLiked,
            onOpenArtist = onOpenArtist,
            onOpenAlbum = onOpenAlbum,
        )
        // The bar, times and format chip sit centred between the title and the
        // transport: 16dp above and 28dp below (the times row's own height
        // already adds space on that side).
        Spacer(Modifier.height(Space.lg))
        ScrubBar(
            progressState = progressState,
            fallbackDurationMs = track.durationMs ?: state.durationMs,
            playedColor = Color.White,
            qualityText = qualityFormatLabel(state),
            onQualityClick = onOpenSignalPath,
            onSeek = player::seekTo,
            qualityHighlight = state.isLossless || isSpatialAudioCodec(state.audioCodec),
            wavy = wavySeekbarEnabled,
            playing = state.isPlaying,
        )
        Spacer(Modifier.height(Space.xl + Space.xs))
        NowPlayingTransport(
            isPlaying = state.isPlaying,
            isBuffering = state.isBuffering,
            playColor = playFillFor(ambientColor),
            onPrevious = player::previous,
            onToggle = player::togglePlayPause,
            onNext = player::next,
        )
        Spacer(Modifier.height(Space.lg))
        NowPlayingActionRow(
            shuffleOn = state.shuffleEnabled,
            repeatMode = state.repeatMode,
            soundChanged = state.speed != 1f || state.pitch != 1f,
            onShuffle = player::toggleShuffle,
            onLyrics = onOpenLyrics,
            onQueue = onOpenQueue,
            onSound = onOpenAudioControls,
            onRepeat = player::cycleRepeatMode,
        )
    }
}

/** The Signal path dialog, with the USB DAC and direct-output state it needs. */
@Composable
internal fun SignalPathSheet(
    player: MusicPlayer,
    signalPath: SignalPathReport,
    onDismiss: () -> Unit,
    tuning432Enabled: Boolean = false,
    loudnessPreampDb: Float? = null,
) {
    val usbDac by player.usbDacState.collectAsStateWithLifecycle()
    val dac = usbDac.dac
    val internalAvailability by player.internalDirectAvailability.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { player.refreshInternalDirectAvailability() }
    SignalPathDialog(
        report = signalPath,
        needsUsbPermission = dac != null && dac.hasUsbPeripheral && !dac.usbPermissionGranted,
        onRequestUsbAccess = player::requestUsbPermission,
        onDismiss = onDismiss,
        internalAvailability = internalAvailability,
        internalEnabled = player.isInternalDirectPrefEnabled,
        internalUnderruns = if (signalPath.internalDirectActive) player.internalDirectUnderruns() else null,
        usbDiagnostic = if (dac != null) player::usbDiagnosticReport else null,
        tuning432Enabled = tuning432Enabled,
        loudnessPreampDb = loudnessPreampDb,
    )
}

/** Hides the system bars while lyrics are fullscreen and restores them after. */
@Composable
internal fun LyricsFullscreenEffect(view: android.view.View, lyricsFullscreen: Boolean) {
    DisposableEffect(view, lyricsFullscreen) {
        val fullscreenActive = lyricsFullscreen
        val activity = generateSequence(view.context) { (it as? android.content.ContextWrapper)?.baseContext }
            .filterIsInstance<android.app.Activity>().firstOrNull()
        val controller = activity?.window?.let { WindowCompat.getInsetsController(it, view) }
        val previousBehavior = controller?.systemBarsBehavior
        if (fullscreenActive) {
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (fullscreenActive) {
                controller?.show(WindowInsetsCompat.Type.systemBars())
                previousBehavior?.let { controller?.systemBarsBehavior = it }
            }
        }
    }

}

/**
 * The player's three tabs with the crossfade between them: lyrics, queue, and
 * Now playing (the phone column, or the side-by-side tablet landscape layout).
 */
@Composable
internal fun FullPlayerTabContent(
    currentTab: FullPlayerTab,
    onTabChange: (FullPlayerTab) -> Unit,
    track: PlayableTrack,
    state: MusicPlayerState,
    player: MusicPlayer,
    lyricsState: LyricsUiState,
    progressState: StateFlow<PlaybackProgressState>,
    wavySeekbarEnabled: Boolean,
    lyricsOffsetMs: Long,
    lyricsFontScale: Float,
    onSetLyricsFontScale: ((Float) -> Unit)?,
    lyricsButtonsAutoHideSeconds: Int,
    translationLanguage: String,
    onRetryLyrics: () -> Unit,
    onSelectLyricsProvider: ((com.tuned.app.data.local.LyricsProvider) -> Unit)?,
    onTryAnotherLyricsSource: (() -> Unit)?,
    ambientColor: Color,
    ambientCompanion: Color,
    ambientDeep: Color,
    resolvedAmbientUrl: String?,
    isTabletLandscape: Boolean,
    showFullBleed: Boolean,
    showSleeveCanvas: Boolean,
    activeCanvas: com.tuned.app.data.canvas.CanvasArtwork?,
    canvas: com.tuned.app.data.canvas.CanvasArtwork?,
    isLiked: Boolean,
    onToggleLiked: () -> Unit,
    onDoubleTapLike: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: () -> Unit = {},
    shownDismissY: Float,
    swipeThreshold: Float,
    signalPath: SignalPathReport,
    onHeroBottomChanged: (Float) -> Unit,
    onCanvasAspectChanged: (Float) -> Unit,
    lyricsFullscreen: Boolean,
    onLyricsFullscreenChange: (Boolean) -> Unit,
    lyricsControlsVisible: Boolean,
    onLyricsControlsVisibleChange: (Boolean) -> Unit,
    onOpenLyricsOffset: () -> Unit,
    onOpenSignalPath: () -> Unit,
    onOpenAudioControls: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = currentTab,
        modifier = modifier
            .then(
                if (isTabletLandscape) {
                    Modifier.widthIn(max = 1200.dp).fillMaxWidth()
                } else {
                    Modifier.adaptiveContentWidth(maxWidth = 680.dp)
                }
            ),
        transitionSpec = {
            // Matched fade durations keep both layers visible
            // throughout the crossfade so the background never
            // flashes through between cover and canvas/lyrics.
            val dur = 280
            if (targetState != FullPlayerTab.NOW_PLAYING) {
                (slideInVertically(tween(dur, easing = FastOutSlowInEasing)) { it / 12 } +
                    fadeIn(tween(dur))) togetherWith
                    (slideOutVertically(tween(dur, easing = FastOutSlowInEasing)) { -it / 12 } +
                        fadeOut(tween(dur)))
            } else {
                (slideInVertically(tween(dur, easing = FastOutSlowInEasing)) { -it / 12 } +
                    fadeIn(tween(dur))) togetherWith
                    (slideOutVertically(tween(dur, easing = FastOutSlowInEasing)) { it / 12 } +
                        fadeOut(tween(dur)))
            }
        },
        label = "playerTabContent",
    ) { tab ->
        when (tab) {
            FullPlayerTab.LYRICS -> {
                PredictiveBackScreen(
                    enabled = currentTab == FullPlayerTab.LYRICS,
                    backgroundColor = Color.Transparent,
                    onBack = {
                        if (lyricsFullscreen) {
                            onLyricsFullscreenChange(false)
                        } else {
                            onTabChange(FullPlayerTab.NOW_PLAYING)
                        }
                    },
                ) {
                    ModernLyricsPanel(
                        state = state,
                        player = player,
                        lyricsState = lyricsState,
                        progressState = progressState,
                        wavySeekbarEnabled = wavySeekbarEnabled,
                        lyricsOffsetMs = lyricsOffsetMs,
                        lyricsFontScale = lyricsFontScale,
                        onLyricsFontScaleChange = onSetLyricsFontScale ?: {},
                        controlsAutoHideSeconds = lyricsButtonsAutoHideSeconds,
                        controlsVisibleState = lyricsControlsVisible,
                        onControlsVisibleChange = onLyricsControlsVisibleChange,
                        translationLanguage = translationLanguage,
                        onRetry = onRetryLyrics,
                        onSelectLyricsProvider = onSelectLyricsProvider,
                        onTryAnotherLyricsSource = onTryAnotherLyricsSource,
                        onToggleFullscreen = { onLyricsFullscreenChange(!lyricsFullscreen) },
                        isFullscreen = lyricsFullscreen,
                        onOpenLyricsOffset = onOpenLyricsOffset,
                        primaryColor = ambientColor,
                        secondaryColor = ambientCompanion,
                        tertiaryColor = ambientDeep,
                        resolvedArtworkUrl = resolvedAmbientUrl,
                        modifier = Modifier
                            .fillMaxSize()
                            .adaptiveContentWidth(maxWidth = if (isTabletLandscape) 960.dp else 720.dp),
                    )
                }
            }

            FullPlayerTab.QUEUE -> {
                PredictiveBackScreen(
                    enabled = currentTab == FullPlayerTab.QUEUE,
                    backgroundColor = Color.Transparent,
                    onBack = { onTabChange(FullPlayerTab.NOW_PLAYING) },
                ) {
                    QueuePanel(
                        state,
                        player,
                        Modifier
                            .fillMaxSize()
                            .adaptiveContentWidth(maxWidth = if (isTabletLandscape) 960.dp else 720.dp)
                            .padding(horizontal = Space.xl),
                    )
                }
            }

            FullPlayerTab.NOW_PLAYING -> {
                if (isTabletLandscape) {
                    FullPlayerLandscape(
                        track = track,
                        state = state,
                        player = player,
                        progressState = progressState,
                        wavySeekbarEnabled = wavySeekbarEnabled,
                        showSleeveCanvas = showSleeveCanvas,
                        canvas = activeCanvas ?: canvas,
                        ambientColor = ambientColor,
                        ambientCompanion = ambientCompanion,
                        ambientDeep = ambientDeep,
                        isLiked = isLiked,
                        onToggleLiked = onToggleLiked,
                        onDoubleTapLike = onDoubleTapLike,
                        onOpenArtist = onOpenArtist,
                        onOpenAlbum = onOpenAlbum,
                        onTabChange = onTabChange,
                        shownDismissY = shownDismissY,
                        swipeThreshold = swipeThreshold,
                        onOpenSignalPath = onOpenSignalPath,
                        onOpenAudioControls = onOpenAudioControls,
                    )
                } else {
                    NowPlayingColumn(
                        track = track,
                        state = state,
                        player = player,
                        progressState = progressState,
                        wavySeekbarEnabled = wavySeekbarEnabled,
                        resolvedAmbientUrl = resolvedAmbientUrl,
                        activeCanvas = activeCanvas,
                        showFullBleed = showFullBleed,
                        showSleeveCanvas = showSleeveCanvas,
                        canvas = canvas,
                        ambientColor = ambientColor,
                        ambientCompanion = ambientCompanion,
                        ambientDeep = ambientDeep,
                        pausedForTransition = shownDismissY > 1f || currentTab != FullPlayerTab.NOW_PLAYING,
                        swipeThreshold = swipeThreshold,
                        onDoubleTapLike = onDoubleTapLike,
                        onHeroBottomChanged = onHeroBottomChanged,
                        onCanvasAspectChanged = onCanvasAspectChanged,
                        isLiked = isLiked,
                        onToggleLiked = onToggleLiked,
                        onOpenArtist = onOpenArtist,
                        onOpenAlbum = onOpenAlbum,
                        onOpenLyrics = { onTabChange(FullPlayerTab.LYRICS) },
                        onOpenQueue = { onTabChange(FullPlayerTab.QUEUE) },
                        signalPath = signalPath,
                        onOpenSignalPath = onOpenSignalPath,
                        onOpenAudioControls = onOpenAudioControls,
                    )
                }
            }
        }
    }
}

/** Height of the dark fade behind the full player's top bar. */
private val HeaderFadeHeight = 160.dp

/** Largest the sleeve gets on wide portrait screens. */
private val SleeveMaxSize = 420.dp

/**
 * Fades the right [edge] of a scrolling (marquee) text, only when it fills the width it
 * was given: a short name that doesn't scroll keeps its first and last letters solid.
 */
@Composable
internal fun Modifier.marqueeEdgeFade(edge: androidx.compose.ui.unit.Dp = 16.dp): Modifier {
    var overflowing by remember { mutableStateOf(false) }
    return this
        .layout { measurable, constraints ->
            // Compare the text's natural width with the room it has: a full-width slot alone
            // doesn't mean the text scrolls.
            overflowing = constraints.hasBoundedWidth &&
                measurable.maxIntrinsicWidth(constraints.maxHeight) > constraints.maxWidth
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        }
        .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            if (!overflowing) return@drawWithContent
            // Only the trailing edge fades: the title rests at its start, so a fade on the
            // left made its first letter look washed out.
            val w = edge.toPx().coerceAtMost(size.width / 3f)
            drawRect(
                brush = Brush.horizontalGradient(0f to Color.Black, 1f to Color.Transparent, startX = size.width - w, endX = size.width),
                topLeft = androidx.compose.ui.geometry.Offset(size.width - w, 0f),
                size = androidx.compose.ui.geometry.Size(w, size.height),
                blendMode = androidx.compose.ui.graphics.BlendMode.DstIn,
            )
        }
}

/** " (feat. X)", " [ft. X]", " (with X)" at the end of a title. */
private val FEAT_SUFFIX = Regex("""\s*[(\[](?:feat\.?|ft\.?|featuring|with)\s[^)\]]*[)\]]""", RegexOption.IGNORE_CASE)

package com.tuned.app.ui.player

import android.app.Activity
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.ui.geometry.Offset
import com.tuned.app.ui.common.pressScale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.tuned.app.R
import com.tuned.app.playback.MusicPlayer
import com.tuned.app.playback.PlayableTrack
import com.tuned.app.ui.common.AppIconButton
import com.tuned.app.ui.theme.AppShape
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.Space
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the full player shows the song's music video in place of its artwork. "Watch video"
 * turns it on from anywhere ([watch]); [openRequests] asks the player host to open the player.
 * It stays on across songs until switched off; a song without a video shows its artwork.
 */
object MusicVideoMode {
    private val _on = MutableStateFlow(false)
    val on: StateFlow<Boolean> = _on.asStateFlow()

    private val _openRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val openRequests: SharedFlow<Unit> = _openRequests.asSharedFlow()

    fun watch() {
        _on.value = true
        _openRequests.tryEmit(Unit)
    }

    fun stop() {
        _on.value = false
    }
}

/**
 * What the full player tells its artwork stage and backdrop: the running video ([session], null
 * when video mode is off or full screen), how far it has replaced the artwork ([cover], 0 to 1),
 * and what a tap on it does.
 */
internal class MusicVideoUi(
    val session: MusicVideoSession?,
    val cover: Float,
    val onFullscreen: () -> Unit,
)

/**
 * The music video's own player and whether a frame of it is showing. The full player keeps it
 * above its tabs, so opening lyrics or the queue doesn't tear it down: coming back shows the same
 * running video at once, not the artwork until a new player's first frame.
 */
internal class MusicVideoSession(
    val player: ExoPlayer,
    val showing: Boolean,
    val failed: Boolean,
)

@Composable
internal fun rememberMusicVideoSession(
    track: PlayableTrack,
    musicPlayer: MusicPlayer,
    viewModel: SyncedVideoViewModel = hiltViewModel(),
): MusicVideoSession {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Not keyed by the song: the video player reports false when it loads a new stream and true
    // on its first frame. Resetting here on a song change could miss that pair when the stream
    // stays the same, leaving the flag wrong.
    var rendered by remember { mutableStateOf(false) }
    // A mid-playback stream error (expired URL, network drop) used to be invisible:
    // the stage kept the last frame and "showing" stayed true.
    var streamFailed by remember { mutableStateOf(false) }

    LaunchedEffect(track.title, track.artist, track.videoId) {
        viewModel.loadVideo(track.title, track.artist, track.videoId)
    }

    val videoPlayer = rememberSyncedVideoPlayer(
        uiState,
        musicPlayer,
        onRendered = { rendered = it },
        onError = { streamFailed = it },
    )
    return MusicVideoSession(
        player = videoPlayer,
        showing = uiState is SyncedVideoUiState.Ready && rendered && !streamFailed,
        failed = uiState is SyncedVideoUiState.Error || streamFailed,
    )
}

internal val LocalMusicVideo = compositionLocalOf<MusicVideoUi?> { null }

/** The video's own width over height, from its first frames (pixel aspect included); null until known. */
@Composable
private fun rememberVideoAspect(player: ExoPlayer): Float? {
    var aspect by remember(player) { mutableStateOf<Float?>(null) }
    DisposableEffect(player) {
        fun read(size: VideoSize) {
            if (size.width > 0 && size.height > 0) {
                aspect = size.width * size.pixelWidthHeightRatio / size.height
            }
        }
        read(player.videoSize)
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) = read(videoSize)
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    return aspect
}

/** The largest size of [aspect] that fits in [maxWidth] x [maxHeight]: never stretched or cropped. */
private fun fitted(aspect: Float, maxWidth: Dp, maxHeight: Dp): Pair<Dp, Dp> {
    val byWidthHeight = maxWidth / aspect
    return if (byWidthHeight <= maxHeight) maxWidth to byWidthHeight else maxHeight * aspect to maxHeight
}

@Composable
private fun VideoSurface(player: ExoPlayer, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextureView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                player.setVideoTextureView(this)
            }
        },
    )
}

/**
 * The music video inside the full player, where the artwork sits, at the video's own shape
 * (never stretched or cropped), muted and kept on the song's clock. A video as wide as the
 * screen runs edge to edge with soft top and bottom edges; a narrower one gets rounded corners.
 * A full-screen button sits on its corner; the top bar's [SongVideoSwitch] goes back to the
 * artwork. The player itself is [session]'s, which outlives this stage.
 */
@Composable
internal fun MusicVideoStage(
    session: MusicVideoSession,
    onFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val videoPlayer = session.player
    val aspect = rememberVideoAspect(videoPlayer) ?: DEFAULT_ASPECT
    // Starts at the session's answer, so a stage that comes back over a running video shows it
    // straight away instead of fading in again.
    val alpha by animateFloatAsState(if (session.showing) 1f else 0f, tween(VIDEO_FADE_MS), label = "musicVideoFade")

    if (session.failed) return
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        // The stage sits inside the player's 24dp margin; a wide video reaches past it to the
        // screen's edges, a narrower one stays inside with rounded corners.
        val edgeWidth = maxWidth + Space.xl * 2
        val (width, height) = fitted(aspect, edgeWidth, maxHeight)
        val edgeToEdge = width >= edgeWidth
        Box(
            Modifier
                .requiredSize(width, height)
                .alpha(alpha)
                .then(if (edgeToEdge) Modifier else Modifier.clip(AppShape.lg))
                .background(Color.Black)
                // A tap on the video opens it full screen.
                .clickable(onClickLabel = stringResource(R.string.video_enter_fullscreen), onClick = onFullscreen),
        ) {
            VideoSurface(videoPlayer, Modifier.fillMaxSize())
            // A soft shade in the corner keeps the full-screen icon readable over any frame.
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(Sizes.touch + Space.xl)
                    .background(Brush.radialGradient(colorStops = CornerShade, center = Offset.Infinite)),
            )
            Icon(
                Icons.Rounded.Fullscreen,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = if (edgeToEdge) Space.xl + Space.xs else Space.md, bottom = Space.md)
                    .size(Sizes.icon),
            )
        }
    }
}

/**
 * The music video full screen: black, the video at its own shape in the middle (landscape videos
 * turn the screen), system bars hidden. A tap shows the controls for a few seconds: close and the
 * song on top, the player's own scrub bar and transport at the bottom. A double tap on either
 * half skips 10 seconds.
 */
@Composable
internal fun MusicVideoFullscreen(
    track: PlayableTrack,
    musicPlayer: MusicPlayer,
    onDismiss: () -> Unit,
    viewModel: SyncedVideoViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state by musicPlayer.state.collectAsStateWithLifecycle()
    var rendered by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var controlsNudge by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(track.title, track.artist, track.videoId) {
        viewModel.loadVideo(track.title, track.artist, track.videoId)
    }
    LaunchedEffect(controlsVisible, controlsNudge, state.isPlaying) {
        if (controlsVisible && state.isPlaying) {
            delay(CONTROLS_HIDE_MS)
            controlsVisible = false
        }
    }
    val videoPlayer = rememberSyncedVideoPlayer(
        uiState,
        musicPlayer,
        onRendered = { rendered = it },
        onError = { if (it) rendered = false },
    )
    val aspect = rememberVideoAspect(videoPlayer)
    val alpha by animateFloatAsState(if (rendered) 1f else 0f, tween(VIDEO_FADE_MS), label = "fullscreenVideoFade")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        ImmersiveWindow(landscape = (aspect ?: DEFAULT_ASPECT) > 1f)
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { controlsVisible = !controlsVisible },
                        onDoubleTap = { offset ->
                            val forward = offset.x > size.width / 2
                            val target = (musicPlayer.currentPositionMs() + if (forward) SKIP_MS else -SKIP_MS).coerceAtLeast(0L)
                            musicPlayer.seekTo(target)
                            controlsNudge += 1f
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            val (width, height) = fitted(aspect ?: DEFAULT_ASPECT, maxWidth, maxHeight)
            VideoSurface(videoPlayer, Modifier.requiredSize(width, height).alpha(alpha))

            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(tween(CONTROLS_FADE_MS)),
                exit = fadeOut(tween(CONTROLS_FADE_MS)),
            ) {
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colorStops = ControlsScrim))) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(horizontal = Space.lg, vertical = Space.sm),
                    ) {
                        AppIconButton(
                            icon = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.video_exit_fullscreen),
                            onClick = onDismiss,
                            tint = Color.White,
                        )
                        Spacer(Modifier.width(Space.sm))
                        Column(Modifier.weight(1f)) {
                            Text(track.title, style = AppType.bodyStrong, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, style = AppType.bodyS, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    // Previous, play/pause, next in the middle of the video, YouTube style.
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Space.xxl),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.Center),
                    ) {
                        FullscreenButton(Icons.Rounded.SkipPrevious, stringResource(R.string.cmp_previous)) {
                            musicPlayer.previous(); controlsNudge += 1f
                        }
                        FullscreenButton(
                            icon = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            label = stringResource(if (state.isPlaying) R.string.cmp_pause else R.string.cmp_play),
                            primary = true,
                        ) {
                            musicPlayer.togglePlayPause(); controlsNudge += 1f
                        }
                        FullscreenButton(Icons.Rounded.SkipNext, stringResource(R.string.cmp_next)) {
                            musicPlayer.next(); controlsNudge += 1f
                        }
                    }
                    ScrubBar(
                        progressState = musicPlayer.progressState,
                        fallbackDurationMs = track.durationMs ?: state.durationMs,
                        playedColor = Color.White,
                        qualityText = "",
                        onQualityClick = {},
                        onSeek = { musicPlayer.seekTo(it); controlsNudge += 1f },
                        playing = state.isPlaying,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(horizontal = Space.xl, vertical = Space.lg),
                    )
                }
            }
        }
    }
}

/** A round full-screen control: white glass, or solid white with a dark glyph for play. */
@Composable
private fun FullscreenButton(icon: ImageVector, label: String, primary: Boolean = false, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(if (primary) Sizes.playButtonPlayer else Sizes.playButton)
            .pressScale(interaction)
            .clip(AppShape.full)
            .background(if (primary) Color.White else Color.White.copy(alpha = TRANSPORT_FILL))
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (primary) Color.Black else Color.White,
            modifier = Modifier.size(if (primary) Sizes.iconHero - Space.sm else Sizes.icon + Space.xs),
        )
    }
}

/** Hides the system bars and, for a landscape video, turns the screen while the view is open. */
@Composable
private fun ImmersiveWindow(landscape: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, landscape) {
        val window = (view.parent as? DialogWindowProvider)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        val activity = generateSequence(view.context) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>().firstOrNull()
        val previous = activity?.requestedOrientation
        if (landscape) activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            if (activity != null && previous != null) activity.requestedOrientation = previous
        }
    }
}

/**
 * The player's song / video switch, as YouTube Music has it, drawn as two icons: a translucent
 * pill with a white thumb that slides under the chosen one. White in both themes, since the
 * player sits on a dark scrim.
 */
@Composable
internal fun SongVideoSwitch(
    videoSelected: Boolean,
    onSelect: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumb by animateFloatAsState(
        targetValue = if (videoSelected) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "songVideoThumb",
    )
    Box(
        modifier
            .height(SWITCH_HEIGHT)
            .clip(AppShape.full)
            .background(Color.White.copy(alpha = SWITCH_TRACK_ALPHA))
            .padding(Space.xxs),
    ) {
        Row {
            SwitchSide(Icons.Rounded.MusicNote, stringResource(R.string.video_switch_song), selected = !videoSelected, thumb = 1f - thumb) { onSelect(false) }
            SwitchSide(Icons.Rounded.SmartDisplay, stringResource(R.string.video_switch_video), selected = videoSelected, thumb = thumb) { onSelect(true) }
        }
    }
}

@Composable
private fun SwitchSide(icon: ImageVector, label: String, selected: Boolean, thumb: Float, onClick: () -> Unit) {
    Box(
        Modifier
            .width(SWITCH_SIDE_WIDTH)
            .fillMaxHeight()
            .clip(AppShape.full)
            .background(Color.White.copy(alpha = thumb))
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics {
                this.selected = selected
                contentDescription = label
            },
        contentAlignment = Alignment.Center,
    ) {
        // Dark on the white thumb, white on the track; crossfades with the thumb.
        Icon(icon, contentDescription = null, tint = lerp(Color.White, Color.Black, thumb), modifier = Modifier.size(Sizes.iconRow))
    }
}

/** Until the first frame reports its size. */
private const val DEFAULT_ASPECT = 16f / 9f
private const val VIDEO_FADE_MS = 300
private const val CONTROLS_FADE_MS = 200
private const val CONTROLS_HIDE_MS = 3_500L
private const val SKIP_MS = 10_000L

/** The shade behind the full-screen icon in the video's corner. */
private val CornerShade = arrayOf(
    0f to Color.Black.copy(alpha = 0.5f),
    1f to Color.Transparent,
)

/** Previous and next in full screen: white glass. */
private const val TRANSPORT_FILL = 0.22f

/** Behind the full-screen controls: dark at the top and bottom, clear in the middle. */
private val ControlsScrim = arrayOf(
    0f to Color.Black.copy(alpha = 0.6f),
    0.2f to Color.Transparent,
    0.65f to Color.Transparent,
    1f to Color.Black.copy(alpha = 0.75f),
)

private val SWITCH_HEIGHT = Sizes.chip + Space.xs
private val SWITCH_SIDE_WIDTH = Sizes.touch + Space.xs
private const val SWITCH_TRACK_ALPHA = 0.14f

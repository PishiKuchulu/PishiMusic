package com.tuned.app.ui.player

import android.app.Activity
import android.content.ContextWrapper
import android.util.Log
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.tuned.app.R
import com.tuned.app.data.music.YouTubeVideoStream
import com.tuned.app.data.video.SyncedVideoRepository
import com.tuned.app.playback.MusicPlayer
import com.tuned.app.playback.PlayableTrack
import com.tuned.app.ui.common.EdgeToEdgeDialogWindow
import com.tuned.app.ui.common.ExpressiveLoadingIndicator
import com.tuned.app.ui.common.adaptiveContentWidth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

private const val TAG = "SyncedVideoPlayer"

/** A hard re-sync aims this far ahead of the audio, about what the seek takes to land. */
private const val HARD_SEEK_LEAD_MS = 250L

/** After a hard re-sync, how long to leave the video alone before measuring drift again. */
private const val HARD_SEEK_SETTLE_MS = 1_000L

sealed interface SyncedVideoUiState {
    data object Loading : SyncedVideoUiState
    data class Ready(
        val bundle: com.tuned.app.data.music.SyncedVideoStreamBundle,
        val activeStream: YouTubeVideoStream,
        val selectedHeight: Int? = null,
    ) : SyncedVideoUiState {
        val stream: YouTubeVideoStream get() = activeStream
        val isAuto: Boolean get() = selectedHeight == null || selectedHeight <= 0
        val currentQualityLabel: String
            get() = if (isAuto) "Auto" else activeStream.displayQuality
    }
    data class Error(val message: String) : SyncedVideoUiState
}

@HiltViewModel
class SyncedVideoViewModel @Inject constructor(
    private val videoRepository: SyncedVideoRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<SyncedVideoUiState>(SyncedVideoUiState.Loading)
    val uiState: StateFlow<SyncedVideoUiState> = _uiState.asStateFlow()

    private var currentKey: String? = null

    fun loadVideo(title: String, artist: String, videoId: String?) {
        val key = "$title|$artist|$videoId"
        if (currentKey == key && _uiState.value is SyncedVideoUiState.Ready) return
        currentKey = key

        viewModelScope.launch {
            _uiState.value = SyncedVideoUiState.Loading
            videoRepository.resolveVideo(title, artist, videoId)
                .onSuccess { bundle ->
                    _uiState.value = SyncedVideoUiState.Ready(
                        bundle = bundle,
                        activeStream = bundle.defaultStream,
                        selectedHeight = null,
                    )
                }
                .onFailure { error ->
                    _uiState.value = SyncedVideoUiState.Error(
                        error.message ?: "Unable to resolve video stream"
                    )
                }
        }
    }

    fun selectQuality(height: Int?) {
        val current = _uiState.value as? SyncedVideoUiState.Ready ?: return
        val targetStream = if (height == null || height <= 0) {
            current.bundle.defaultStream
        } else {
            current.bundle.availableStreams.firstOrNull { (it.height ?: 0) == height }
                ?: current.bundle.availableStreams.minByOrNull { abs((it.height ?: 0) - height) }
                ?: current.bundle.defaultStream
        }
        _uiState.value = current.copy(
            activeStream = targetStream,
            selectedHeight = height,
        )
    }

    fun retry(title: String, artist: String, videoId: String?) {
        currentKey = null
        loadVideo(title, artist, videoId)
    }
}

/**
 * The muted video player behind every synced music video: loads [uiState]'s stream and keeps it on
 * [musicPlayer]'s clock (audio is the master). [onRendered] reports whether a frame is on screen.
 * Used by the full player's video mode ([MusicVideoStage]) and its full-screen view ([MusicVideoFullscreen]).
 */
@OptIn(UnstableApi::class)
@Composable
internal fun rememberSyncedVideoPlayer(
    uiState: SyncedVideoUiState,
    musicPlayer: MusicPlayer,
    onRendered: (Boolean) -> Unit,
    onError: (Boolean) -> Unit = {},
): ExoPlayer {
    val context = LocalContext.current
    val playerState by musicPlayer.state.collectAsStateWithLifecycle()
    // The listener below lives as long as the player, across songs; it must call the caller's
    // latest callback, or a new song's "rendered" never reaches the screen and its video stays hidden.
    val currentOnRendered by rememberUpdatedState(onRendered)
    val currentOnError by rememberUpdatedState(onError)
    // ExoPlayer dedicated strictly for video rendering
    val videoPlayer = remember(context) {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 2_000,
                /* maxBufferMs = */ 8_000,
                /* bufferForPlaybackMs = */ 1_000,
                /* bufferForPlaybackAfterRebufferMs = */ 2_000,
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(
                mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                    "Origin" to "https://www.youtube.com",
                    "Referer" to "https://www.youtube.com",
                )
            )
        val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setLoadControl(loadControl)
            .build()
            .apply {
                volume = 0f
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                    .build()
            }
    }

    DisposableEffect(videoPlayer) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                currentOnError(false)
                currentOnRendered(true)
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                // Publish the failure: the stage used to keep reporting "rendered"
                // and show a dead frame as if the video were playing. Log only the
                // error code — the exception message can embed the signed stream URL.
                currentOnRendered(false)
                currentOnError(true)
                Log.w(TAG, "Video player error: ${error.errorCodeName}")
            }
        }
        videoPlayer.addListener(listener)
        onDispose {
            videoPlayer.removeListener(listener)
            videoPlayer.release()
        }
    }

    // Load stream into video player when ready or when switching quality
    LaunchedEffect(uiState) {
        val state = uiState
        if (state is SyncedVideoUiState.Ready) {
            val stream = state.activeStream
            val currentMediaUri = videoPlayer.currentMediaItem?.localConfiguration?.uri?.toString()
            if (currentMediaUri == stream.url) return@LaunchedEffect

            currentOnRendered(false)
            val currentPos = videoPlayer.currentPosition.takeIf { it > 0 } ?: musicPlayer.currentPositionMs()
            val mediaItem = MediaItem.Builder()
                .setUri(stream.url)
                .apply {
                    stream.mimeType?.let { setMimeType(it) }
                }
                .build()
            videoPlayer.setMediaItem(mediaItem)
            videoPlayer.prepare()
            videoPlayer.seekTo(currentPos)
            videoPlayer.playWhenReady = playerState.isPlaying
        }
    }

    // Exact millisecond synchronization loop (PLL)
    LaunchedEffect(videoPlayer, playerState.isPlaying, uiState) {
        if (uiState !is SyncedVideoUiState.Ready) return@LaunchedEffect
        while (isActive) {
            // Paused: nothing to sync. The old loop kept polling positions at
            // 20 Hz for the whole pause; park it and just hold the video paused.
            if (!playerState.isPlaying) {
                if (videoPlayer.playWhenReady) videoPlayer.playWhenReady = false
                delay(250L)
                continue
            }
            val audioPos = musicPlayer.currentPositionMs()
            val videoPos = videoPlayer.currentPosition
            val driftMs = videoPos - audioPos
            val audioSpeed = playerState.speed

            // Play / Pause sync
            if (videoPlayer.playWhenReady != playerState.isPlaying) {
                videoPlayer.playWhenReady = playerState.isPlaying
            }

            // Only correct a video that is playing. While it buffers its position stands still, so
            // the drift looks large; seeking then restarts the buffering, and the old loop did that
            // every 50ms, so the first frame never came.
            if (videoPlayer.playbackState != Player.STATE_READY) {
                delay(50L)
                continue
            }

            // Drift compensation
            if (abs(driftMs) > 150L) {
                // Hard seek for significant drift or user seeks, aimed a little ahead of the
                // audio to cover the time the seek itself takes; then give it time to land.
                videoPlayer.seekTo(audioPos + HARD_SEEK_LEAD_MS)
                if (videoPlayer.playbackParameters.speed != audioSpeed) {
                    videoPlayer.playbackParameters = PlaybackParameters(audioSpeed)
                }
                delay(HARD_SEEK_SETTLE_MS)
                continue
            } else if (abs(driftMs) > 40L) {
                // Micro-phase adjustment for sub-millisecond convergence without stutters
                val speedCorrection = if (driftMs > 0) 0.95f else 1.05f
                videoPlayer.playbackParameters = PlaybackParameters(audioSpeed * speedCorrection)
            } else {
                // Exact sync achieved
                if (videoPlayer.playbackParameters.speed != audioSpeed) {
                    videoPlayer.playbackParameters = PlaybackParameters(audioSpeed)
                }
            }

            delay(50L) // 20 times per second high-precision clock
        }
    }
    return videoPlayer
}

package com.tuned.app.ui.player

import androidx.compose.material.icons.outlined.Timer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Share
import com.tuned.app.ui.player.lyricsshare.ShareLyricsDialog
import com.tuned.app.ui.player.lyricsshare.TrackInfo
import com.tuned.app.ui.player.lyricsshare.buildLyricsSnapshot
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.material3.Slider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import com.tuned.app.R
import com.tuned.app.ui.theme.LocalLiquidGlass
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.mutableIntStateOf
import com.tuned.app.ui.theme.LyricsFace
import com.tuned.app.ui.theme.LiquidGlassPreset
import com.tuned.app.ui.theme.LiquidGlassSurface
import com.tuned.app.ui.theme.liquidGlassChrome
import com.tuned.app.ui.theme.liquidGlassContainerColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuned.app.data.local.LyricsProvider
import com.tuned.app.data.lyrics.LyricLine
import com.tuned.app.data.lyrics.LyricSyllable
import com.tuned.app.data.lyrics.LyricsTranslation
import com.tuned.app.data.lyrics.isRtlText
import com.tuned.app.data.lyrics.providerForSource
import com.tuned.app.data.lyrics.translationLanguageName
import com.tuned.app.data.lyrics.translationWireTarget
import com.tuned.app.playback.MusicPlayer
import com.tuned.app.playback.MusicPlayerState
import com.tuned.app.playback.PlaybackProgressState
import com.tuned.app.ui.common.ExpressiveInlineLoadingIndicator
import com.tuned.app.ui.common.ExpressiveMotion
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/** Which secondary script the floating buttons currently draw under each
 *  line: none ([Original]), the pronunciation ([Romanized]) or a translation
 *  ([Translated]). At most one mode is shown at a time. */
private enum class LyricsDisplayMode { Original, Romanized, Translated }

/*
 * Apple Music's lyric motion (values from docs/plans/full-player-plan.md §4b,
 * taken from Apple Music's own defaults; written here from that spec, not
 * copied from any renderer).
 */

/** White strength of every line that is not being sung (Apple's deselected white, α0.175). */
private const val LYRIC_INACTIVE_ALPHA = 0.175f

/** Apple's duet layout: each voice's lines take 85% of the width, on its own side. */
private const val LYRIC_DUET_WIDTH = 0.85f

/** How lyric text aligns in the current line: start, or end for a duet's second singer. */
private val LocalLyricAlign = androidx.compose.runtime.staticCompositionLocalOf { TextAlign.Start }

/** The unsung part of the line being sung, before the wipe reaches it (Apple: α0.35). */
private const val LYRIC_UNSUNG_ALPHA = 0.35f

/** A word glows only when it is held: sung for at least this long, and slowly
 *  (this long per letter or more), like a sustained note. */
private const val LYRIC_HELD_MIN_MS = 1000L
private const val LYRIC_HELD_MS_PER_CHAR = 200L

/** While the user scrolls by hand every line shows at this strength (Apple: α0.4). */
private const val LYRIC_BROWSE_ALPHA = 0.4f

/** Auto-follow comes back this long after the finger leaves the list. */
private const val LYRIC_BROWSE_HOLD_MS = 3000L

/** Blur on lines that are not being sung. */
private const val LYRIC_BLUR_DP = 4.5f

/** How far the list must scroll to put [item] in the middle of the screen. */
private fun LazyListState.distanceToCentre(item: LazyListItemInfo): Float {
    val centre = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    return (item.offset + item.size / 2 - centre).toFloat()
}

/** Each line's own position spring and the delay between lines: Apple's
 *  lineChangeSpringTimingParameters, mass 1, stiffness 150, damping 30 (overdamped). */
private const val LYRIC_LINE_STIFFNESS = 150f
private const val LYRIC_LINE_DAMPING = 30f
private const val LYRIC_RIPPLE_STAGGER_MS = 50L

/** One line change: how far the list jumped, when, and which line was on top. */
private class LyricRipple(val shiftPx: Float, val startNanos: Long, val firstIndex: Int)

/** A ripple is dropped once every line has settled. */
private const val LYRIC_RIPPLE_LIFETIME_NS = 2_000_000_000L

/**
 * Where line [index] sits relative to its laid-out place: the sum of every
 * ripple still settling. Each line holds its old place for its stagger, then
 * follows Apple's overdamped spring (m 1, k 150, c 30; ζ ≈ 1.22), released from
 * rest: x(t) = shift · (r₂e^(r₁t) − r₁e^(r₂t)) / (r₂ − r₁). It leaves a touch
 * slower than a critical spring and lands with a longer, softer tail.
 */
private fun rippleOffset(ripples: List<LyricRipple>, nowNanos: Long, index: Int): Float {
    if (ripples.isEmpty()) return 0f
    val omega = kotlin.math.sqrt(LYRIC_LINE_STIFFNESS)
    val zeta = LYRIC_LINE_DAMPING / (2f * omega)
    val root = omega * kotlin.math.sqrt(zeta * zeta - 1f)
    val r1 = -zeta * omega + root
    val r2 = -zeta * omega - root
    var offset = 0f
    for (r in ripples) {
        val order = (index - r.firstIndex).coerceIn(0, 12)
        val t = (nowNanos - r.startNanos) / 1_000_000_000f - order * LYRIC_RIPPLE_STAGGER_MS / 1000f
        offset += if (t <= 0f) r.shiftPx
        else r.shiftPx * (r2 * kotlin.math.exp(r1 * t) - r1 * kotlin.math.exp(r2 * t)) / (r2 - r1)
    }
    return offset
}

/** Focus change curve: cubic-bezier(0.33, 0, 0.67, 1). */
private val LYRIC_FOCUS_EASE = CubicBezierEasing(0.33f, 0f, 0.67f, 1f)

/** Glide curve for the follow scroll and row scale: quick departure, long
 *  soft landing — closer to a platform sheet than to a linear scroll. */
private val LYRIC_EASE = CubicBezierEasing(0.41f, 0f, 0.12f, 0.99f)

/** Follow-scroll duration: small corrections stay crisp, whole-verse jumps
 *  glide. Gap (px between the row and the focus point) picks the point inside
 *  the band; the band itself is what keeps motion calm. */
private const val LYRIC_SCROLL_MIN_MS = 350f
private const val LYRIC_SCROLL_MAX_MS = 500f

/** Sub-line reveal: slightly unhurried in, clipped and quick out. */
private const val LYRIC_SUBLINE_OPEN_MS = 460
private const val LYRIC_SUBLINE_CLOSE_MS = 260

@Composable
fun ModernLyricsPanel(
    state: MusicPlayerState,
    player: MusicPlayer,
    lyricsState: LyricsUiState,
    progressState: StateFlow<PlaybackProgressState>? = null,
    wavySeekbarEnabled: Boolean = true,
    onToggleFullscreen: (() -> Unit)? = null,
    isFullscreen: Boolean = false,
    onRetry: () -> Unit = {},
    onOpenLyricsOffset: (() -> Unit)? = null,
    lyricsFontScale: Float = 1.0f,
    onLyricsFontScaleChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
    /** Manual sync correction (ms, + = lyrics earlier). Applies to lyric
     *  focus/highlight only — the seekbar below keeps true position. */
    lyricsOffsetMs: Long = 0L,
    controlsAutoHideSeconds: Int = 5,
    /** Controls auto-hide visibility state (hoisted or internally managed). */
    controlsVisibleState: Boolean? = null,
    onControlsVisibleChange: ((Boolean) -> Unit)? = null,
    /** Language the translate button targets; blank = follow the app language. */
    translationLanguage: String = "",
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary,
    tertiaryColor: Color = MaterialTheme.colorScheme.tertiary,
    onSelectLyricsProvider: ((LyricsProvider) -> Unit)? = null,
    onTryAnotherLyricsSource: (() -> Unit)? = null,
    resolvedArtworkUrl: String? = null,
) {
    state.current ?: return

    // The screen stays on while lyrics are open, like a lyrics sheet on a
    // music stand; it sleeps normally again once they close.
    val hostView = LocalView.current
    DisposableEffect(hostView) {
        hostView.keepScreenOn = true
        onDispose { hostView.keepScreenOn = false }
    }

    var showShareDialog by rememberSaveable { mutableStateOf(false) }
    val canShareLyrics = lyricsState is LyricsUiState.Success &&
        !lyricsState.isInstrumental &&
        (lyricsState.lines.any { it.text.isNotBlank() } || !lyricsState.plainLyrics.isNullOrBlank())

    // High-frequency progress is collected ONLY in the leaf hosts below
    // (lyrics list + controls). Collecting it here would recompose the whole
    // panel incl. AnimatedContent + badge ~16x/sec. Other players isolate it.
    val progressFlow = progressState ?: player.progressState
    val progressInitial = PlaybackProgressState(positionMs = state.positionMs, durationMs = state.durationMs)
    var effectiveFontScale by remember(lyricsFontScale) { mutableFloatStateOf(lyricsFontScale) }

    // Floating buttons over the lyrics stay only while the user is active:
    // hide after the configured idle time and bring them back on the first tap.
    var internalControlsVisible by remember { mutableStateOf(true) }
    val controlsVisible = controlsVisibleState ?: internalControlsVisible
    val setControlsVisible: (Boolean) -> Unit = { visible ->
        internalControlsVisible = visible
        onControlsVisibleChange?.invoke(visible)
    }
    var fontSliderOpen by rememberSaveable { mutableStateOf(false) }
    var singSliderOpen by rememberSaveable { mutableStateOf(false) }
    var sourceDialogOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(controlsVisible, controlsAutoHideSeconds, fontSliderOpen, singSliderOpen, sourceDialogOpen, isFullscreen) {
        if (controlsVisible && controlsAutoHideSeconds > 0 && !fontSliderOpen && !singSliderOpen && !sourceDialogOpen) {
            delay(controlsAutoHideSeconds * 1000L)
            setControlsVisible(false)
        }
    }
    // Entering or leaving lyrics fullscreen starts a fresh visible period.
    LaunchedEffect(isFullscreen) { setControlsVisible(true) }

    // On-demand lyric translation / romanization. Work is batched and cached
    // per track and language inside [LyricsTranslation]; the panel owns only
    // the shown mode, the loading flags and one shared reveal that grows all
    // sub-lines in and out together with the list.
    val translationContext = LocalContext.current
    val translationScope = rememberCoroutineScope()
    val translationTrackKey = state.current?.let { track ->
        track.videoId ?: "${track.artist}|${track.title}"
    }.orEmpty()
    val translatableLines = (lyricsState as? LyricsUiState.Success)
        ?.takeIf { it.isSynced && it.lines.isNotEmpty() && !it.isInstrumental }
        ?.lines

    var displayMode by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(LyricsDisplayMode.Original) }
    var translationReady by remember { mutableStateOf<List<String?>?>(null) }
    var romanizationReady by remember { mutableStateOf<List<String?>?>(null) }
    var translationLoading by remember { mutableStateOf(false) }
    var romanizationLoading by remember { mutableStateOf(false) }
    var translationJob by remember { mutableStateOf<Job?>(null) }
    var romanizationJob by remember { mutableStateOf<Job?>(null) }
    var visibleSubLines by remember { mutableStateOf<List<String?>?>(null) }
    val subReveal = remember { Animatable(0f) }
    val targetLanguage = remember(translationLanguage) { translationWireTarget(translationLanguage) }

    // A new track, newly fetched lyrics or a different target language all
    // invalidate whatever was computed for the old ones.
    LaunchedEffect(translationTrackKey, translationLanguage, translatableLines) {
        translationJob?.cancel()
        romanizationJob?.cancel()
        translationJob = null
        romanizationJob = null
        translationLoading = false
        romanizationLoading = false
        translationReady = null
        romanizationReady = null
        displayMode = LyricsDisplayMode.Original
        visibleSubLines = null
        subReveal.snapTo(0f)
    }

    // A script switch closes the old sub-lines first, then grows the new ones
    // in, so a Romanized -> Translated flip reads as one motion instead of a
    // text swap.
    LaunchedEffect(displayMode) {
        if (displayMode == LyricsDisplayMode.Original) {
            if (subReveal.value > 0f || visibleSubLines != null) {
                subReveal.animateTo(0f, tween(LYRIC_SUBLINE_CLOSE_MS, easing = FastOutSlowInEasing))
                visibleSubLines = null
            }
        } else {
            if (subReveal.value > 0f) {
                subReveal.animateTo(0f, tween(LYRIC_SUBLINE_CLOSE_MS, easing = FastOutSlowInEasing))
            }
            visibleSubLines = when (displayMode) {
                LyricsDisplayMode.Romanized -> romanizationReady
                LyricsDisplayMode.Translated -> translationReady
                LyricsDisplayMode.Original -> null
            }
            subReveal.animateTo(1f, tween(LYRIC_SUBLINE_OPEN_MS, easing = FastOutSlowInEasing))
        }
    }

    fun showLyricsToast(message: String) {
        android.widget.Toast.makeText(translationContext, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    fun cancelTranslationLoad() {
        translationJob?.cancel()
        translationJob = null
        translationLoading = false
    }

    fun cancelRomanizationLoad() {
        romanizationJob?.cancel()
        romanizationJob = null
        romanizationLoading = false
    }

    val onToggleTranslation: () -> Unit = {
        val lines = translatableLines
        when {
            lines == null -> Unit
            displayMode == LyricsDisplayMode.Translated -> {
                // Turning the mode off abandons the other mode's in-flight
                // load too: the user has moved on from both scripts.
                cancelRomanizationLoad()
                displayMode = LyricsDisplayMode.Original
            }
            translationReady != null -> {
                cancelRomanizationLoad()
                displayMode = LyricsDisplayMode.Translated
            }
            !translationLoading -> {
                cancelRomanizationLoad()
                translationLoading = true
                translationJob = translationScope.launch {
                    try {
                        when (val result = LyricsTranslation.translate(translationTrackKey, lines, targetLanguage)) {
                            is LyricsTranslation.Result.Translated -> {
                                translationReady = result.subLines
                                displayMode = LyricsDisplayMode.Translated
                            }
                            is LyricsTranslation.Result.SameLanguage -> showLyricsToast(
                                translationContext.getString(
                                    R.string.ly_toast_already_language,
                                    translationLanguageName(result.language, Locale.getDefault()),
                                ),
                            )
                            LyricsTranslation.Result.Unavailable -> showLyricsToast(
                                translationContext.getString(R.string.ly_toast_translation_unavailable),
                            )
                        }
                    } finally {
                        if (translationJob === coroutineContext[Job]) {
                            translationLoading = false
                            translationJob = null
                        }
                    }
                }
            }
        }
    }

    val onToggleRomanization: () -> Unit = {
        val lines = translatableLines
        when {
            lines == null -> Unit
            displayMode == LyricsDisplayMode.Romanized -> {
                cancelTranslationLoad()
                displayMode = LyricsDisplayMode.Original
            }
            romanizationReady != null -> {
                cancelTranslationLoad()
                displayMode = LyricsDisplayMode.Romanized
            }
            !romanizationLoading -> {
                cancelTranslationLoad()
                romanizationLoading = true
                romanizationJob = translationScope.launch {
                    try {
                        when (val result = LyricsTranslation.romanize(translationTrackKey, lines)) {
                            is LyricsTranslation.RomanizationResult.Romanized -> {
                                romanizationReady = result.subLines
                                displayMode = LyricsDisplayMode.Romanized
                            }
                            LyricsTranslation.RomanizationResult.AlreadyRomanized -> showLyricsToast(
                                translationContext.getString(R.string.ly_toast_already_romanized),
                            )
                            LyricsTranslation.RomanizationResult.Unavailable -> showLyricsToast(
                                translationContext.getString(R.string.ly_toast_romanization_unavailable),
                            )
                        }
                    } finally {
                        if (romanizationJob === coroutineContext[Job]) {
                            romanizationLoading = false
                            romanizationJob = null
                        }
                    }
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AnimatedContent(
                targetState = lyricsState,
                transitionSpec = {
                    (fadeIn(tween(ExpressiveMotion.Quick)) +
                        androidx.compose.animation.scaleIn(ExpressiveMotion.spatialSpring(), initialScale = 0.96f)) togetherWith
                        (fadeOut(tween(ExpressiveMotion.Quick)) +
                            androidx.compose.animation.scaleOut(tween(ExpressiveMotion.Quick), targetScale = 0.96f))
                },
                label = "modernLyricsStateContent",
                modifier = Modifier.fillMaxSize(),
            ) { targetState ->
                when (targetState) {
                    is LyricsUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                ExpressiveInlineLoadingIndicator(
                                    size = 42.dp,
                                    strokeWidth = 3.5.dp,
                                    color = Color.White,
                                )
                                Text(
                                    "Finding lyrics…",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.70f),
                                )
                            }
                        }
                    }

                    is LyricsUiState.Empty, is LyricsUiState.Error -> {
                        ModernEmptyLyricsView(
                            isInstrumental = false,
                            onRetry = onRetry,
                        )
                    }

                    is LyricsUiState.Success -> {
                        if (targetState.isInstrumental) {
                            ModernEmptyLyricsView(
                                isInstrumental = true,
                                onRetry = onRetry,
                            )
                        } else if (targetState.isSynced && targetState.lines.isNotEmpty()) {
                            // Reference parity: one provider timestamp stays one
                            // drawn row — long rows wrap visually via Text
                            // soft-wrap on their own timestamp, never sliced
                            // into fabricated timed chunks.
                            val isWordSynced = targetState.isWordSynced ||
                                remember(targetState.lines) { targetState.lines.any { it.hasSyllables } }
                            val isOverallRtl = remember(targetState.lines) {
                                val meaningful = targetState.lines.filter { it.text.isNotBlank() && it.text != "♪" }
                                if (meaningful.isEmpty()) false
                                else meaningful.count { it.isRtl } > meaningful.size / 2
                            }
                            // Apple Music word-sync rows run full-sentence wide, so
                            // they keep a compact size while other providers use
                            // the standard tier. Sizes stay moderate so wrapped
                            // rows clear the edges at the focus zoom.
                            val isAppleMusic = remember(targetState.source) {
                                targetState.source?.contains("Apple Music", ignoreCase = true) == true
                            }
                            // Shared style instances drawn by every row below.
                            val currentTextStyle = LocalTextStyle.current
                            // Apple Music's lyric type: one size for every
                            // provider, bold, tight tracking, set in Inter (the
                            // open face closest to SF Pro).
                            // Apple's lyric line: largeTitle bold, 34pt.
                            val karaokeNormalStyle = remember(currentTextStyle, effectiveFontScale) {
                                val baseSize = 34f * effectiveFontScale
                                currentTextStyle.copy(
                                    fontFamily = LyricsFace,
                                    fontSize = baseSize.sp,
                                    lineHeight = (baseSize * 1.25f).sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5f * effectiveFontScale).sp,
                                    textMotion = TextMotion.Animated,
                                )
                            }
                            // Background vocals: smaller, under the main line.
                            // Apple's background vocals: title2, 22pt.
                            val karaokeAccompanimentStyle = remember(currentTextStyle, effectiveFontScale) {
                                val baseSize = 22f * effectiveFontScale
                                currentTextStyle.copy(
                                    fontFamily = LyricsFace,
                                    fontSize = baseSize.sp,
                                    lineHeight = (baseSize * 1.25f).sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = (-0.3f * effectiveFontScale).sp,
                                    textMotion = TextMotion.Animated,
                                )
                            }
                            // Sub-lines (translation / romanization) draw
                            // smaller and lighter than the lyric they follow.
                            val subLineStyle = remember(currentTextStyle, effectiveFontScale) {
                                currentTextStyle.copy(
                                    fontFamily = LyricsFace,
                                    fontSize = (18f * effectiveFontScale).sp,
                                    lineHeight = (24f * effectiveFontScale).sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textMotion = TextMotion.Animated,
                                )
                            }

                            val layoutDirection = if (isOverallRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                            // Short provider badge: makes it visible why words
                            // animate (word-sync) or just scroll (line-sync).
                            val wordSyncLabel = stringResource(R.string.ly_word_sync)
                            val lineSyncLabel = stringResource(R.string.ly_line_sync)
                            val providerFallback = stringResource(R.string.ly_provider_fallback)
                            val syncLabel = remember(targetState.source, isWordSynced) {
                                val provider = targetState.source
                                    ?.substringBefore(" (")
                                    ?.takeIf { it.isNotBlank() } ?: providerFallback
                                provider
                            }
                            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = 8.dp),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 6.dp),
                                        horizontalArrangement = Arrangement.Center,
                                    ) {
                                        // Tap to try another lyrics source.
                                        val canSwitchSource = onSelectLyricsProvider != null || onTryAnotherLyricsSource != null
                                        Surface(
                                            onClick = { sourceDialogOpen = true },
                                            enabled = canSwitchSource,
                                            shape = CircleShape,
                                            // Glass and tint on the same capsule: a clickable Surface grows to
                                            // 48dp for touch but paints its colour only on the smaller pill,
                                            // which showed a dark pill floating inside the glass.
                                            modifier = Modifier
                                                .liquidGlassChrome(CircleShape, LocalLiquidGlass.current, LiquidGlassPreset.FloatingControls)
                                                .background(liquidGlassContainerColor(Color.White.copy(alpha = 0.16f)), CircleShape),
                                            color = Color.Transparent,
                                            contentColor = Color.White.copy(alpha = 0.92f),
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(start = 12.dp, end = if (canSwitchSource) 8.dp else 12.dp, top = 5.dp, bottom = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    text = syncLabel,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                    ),
                                                )
                                                if (canSwitchSource) {
                                                    Icon(
                                                        Icons.Filled.ArrowDropDown,
                                                        contentDescription = stringResource(R.string.ly_source_cd),
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    SyncedLyricsProgressHost(
                                        lines = targetState.lines,
                                        isOverallRtl = isOverallRtl,
                                        normalStyle = karaokeNormalStyle,
                                        accompanimentStyle = karaokeAccompanimentStyle,
                                        subLineStyle = subLineStyle,
                                        subLines = visibleSubLines,
                                        subReveal = { subReveal.value },
                                        progressFlow = progressFlow,
                                        progressInitial = progressInitial,
                                        state = state,
                                        player = player,
                                        lyricsOffsetMs = lyricsOffsetMs,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                    )
                                }
                            }
                        } else if (!targetState.plainLyrics.isNullOrBlank()) {
                            ModernPlainLyricsView(
                                plainLyrics = targetState.plainLyrics,
                                lyricsFontScale = effectiveFontScale,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            ModernEmptyLyricsView(
                                isInstrumental = false,
                                onRetry = onRetry,
                            )
                        }
                    }

                    is LyricsUiState.Idle -> {
                        Box(Modifier.fillMaxSize())
                    }
                }
            }
            // Any tap while the buttons are hidden brings them back — and
            // only that: it must not also seek to the lyric line underneath.
            if (!controlsVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) { detectTapGestures { setControlsVisible(true) } },
                )
            }
        }

        ModernLyricsControlsHost(
            state = state,
            progressFlow = progressFlow,
            progressInitial = progressInitial,
            player = player,
            wavySeekbarEnabled = wavySeekbarEnabled,
            onToggleFullscreen = onToggleFullscreen,
            isFullscreen = isFullscreen,
            lyricsOffsetMs = lyricsOffsetMs,
            onOpenLyricsOffset = onOpenLyricsOffset,
            controlsVisible = controlsVisible,
            displayMode = displayMode,
            translationLoading = translationLoading,
            romanizationLoading = romanizationLoading,
            translationAvailable = translatableLines != null,
            onToggleTranslation = onToggleTranslation,
            onToggleRomanization = onToggleRomanization,
            showFontSlider = fontSliderOpen,
            onShowFontSliderChange = { fontSliderOpen = it },
            vocalsAvailable = state.vocalsAvailable,
            vocalLevel = state.vocalLevel,
            onVocalLevelChange = player::setVocalLevel,
            showSingSlider = singSliderOpen,
            onShowSingSliderChange = { singSliderOpen = it },
            onOpenLyricsSource = if (onSelectLyricsProvider != null || onTryAnotherLyricsSource != null) {
                { sourceDialogOpen = true }
            } else null,
            lyricsFontScale = effectiveFontScale,
            onLyricsFontScaleChange = { newScale ->
                effectiveFontScale = newScale
                onLyricsFontScaleChange(newScale)
            },
            onLyricsFontScalePreview = { previewScale ->
                effectiveFontScale = previewScale
            },
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            tertiaryColor = tertiaryColor,
            canShareLyrics = canShareLyrics,
            onShareLyrics = { showShareDialog = true },
            lyricsOffsetActive = lyricsOffsetMs != 0L,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp, top = 6.dp),
        )

        if (sourceDialogOpen) {
            val currentSource = (lyricsState as? LyricsUiState.Success)?.source
            val currentProvider = remember(currentSource) { providerForSource(currentSource) }
            LyricsSourceDialog(
                currentSource = currentSource,
                currentProvider = currentProvider,
                onSelectProvider = { provider ->
                    sourceDialogOpen = false
                    onSelectLyricsProvider?.invoke(provider)
                },
                onTryAnotherSource = {
                    sourceDialogOpen = false
                    onTryAnotherLyricsSource?.invoke()
                },
                onDismiss = { sourceDialogOpen = false },
            )
        }
    }

    if (showShareDialog) {
        val currentTrack = state.current
        if (currentTrack != null) {
            val trackInfo = remember(currentTrack, resolvedArtworkUrl) {
                TrackInfo(
                    title = currentTrack.title,
                    artist = currentTrack.artist,
                    artworkUrl = currentTrack.artworkUrl,
                    resolvedArtworkUrl = resolvedArtworkUrl,
                )
            }
            val activeIndex = remember(lyricsState, lyricsOffsetMs) {
                val lines = (lyricsState as? LyricsUiState.Success)?.lines.orEmpty()
                if (lines.isNotEmpty()) {
                    activeLineIndex(lines, progressFlow.value.positionMs + lyricsOffsetMs)
                } else {
                    -1
                }
            }
            val snapshot = remember(lyricsState, trackInfo) {
                buildLyricsSnapshot(lyricsState, trackInfo, activeIndex)
            }
            if (snapshot != null) {
                ShareLyricsDialog(
                    snapshot = snapshot,
                    onDismiss = { showShareDialog = false },
                    primaryColor = primaryColor,
                )
            }
        }
    }
}

/**
 * Smoothly melts the top and bottom edges of the lyrics viewport using a hardware-accelerated
 * offscreen alpha gradient mask (BlendMode.DstIn). Eliminates abrupt hard-edge clipping as
 * lyrics scroll down toward the bottom player controls.
 */
private fun Modifier.lyricsFadingEdge(
    topFadeDp: androidx.compose.ui.unit.Dp = 36.dp,
    bottomFadeDp: androidx.compose.ui.unit.Dp = 110.dp,
): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val topFadePx = topFadeDp.toPx()
        val bottomFadePx = bottomFadeDp.toPx()
        val height = size.height
        if (height > topFadePx + bottomFadePx) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    (topFadePx / height) to Color.Black,
                    ((height - bottomFadePx) / height) to Color.Black,
                    1f to Color.Transparent,
                    startY = 0f,
                    endY = height,
                ),
                blendMode = BlendMode.DstIn,
            )
        }
    }

/**
 * Reference lyrics sync list (Metrolist/SimpMusic parity, no gradle canvas).
 *
 * Sync contract, identical for word-by-word and line-by-line rows:
 * - One provider timestamp = one drawn row. Rows are never merged, split,
 *   or re-timed here; [LyricLine.timeMs] is the single source of truth.
 * - The active row is the last row with `timeMs <= effectivePosition`
 *   (binary search, same as the classic view and the reference clients'
 *   `findCurrentLineIndex`). A row's end is implied by the next row's start,
 *   never stored, so overlapping focus windows are impossible by
 *   construction (the old `maxOf(naturalEnd, nextStart)` mapping kept two
 *   rows lit around every boundary).
 * - Word fill reads each syllable's own `[timeMs, timeMs + durationMs)`
 *   window (already de-overlapped upstream by `normalizeLyricTiming`). A
 *   zero-duration syllable holds until the next syllable starts, matching
 *   the reference `findActiveLineIndices` end-time fallback.
 * - Position is the raw playhead plus the manual offset on every tick — no
 *   time quantization. Reference clients sample per frame (8ms /
 *   withFrameNanos); the 60ms progress ticker feeding this list is already
 *   the coarsest step word fills can tolerate (the old 120ms bucketing
 *   visibly stalled syllables 100-200ms long).
 * - Long rows wrap visually via [Text] soft-wrap on their own timestamp.
 *   They are never sliced into fabricated timed chunks: the old
 *   proportional-width slicing invented timestamps the vocals never sang,
 *   which is why line-by-line drifted and word-by-word stalled.
 */
@Composable
private fun ModernSyncedLyricsList(
    lines: List<LyricLine>,
    isOverallRtl: Boolean,
    normalStyle: TextStyle,
    accompanimentStyle: TextStyle,
    currentPositionMs: () -> Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    lyricsOffsetMs: Long = 0L,
    /** Aligned sub-line per row index ([LyricsTranslation] output), or null. */
    subLines: List<String?>? = null,
    /** Shared 0..1 reveal for every sub-line; read per frame, not recomposed. */
    subReveal: () -> Float = { 0f },
    subLineStyle: TextStyle? = null,
) {
    val listState = rememberLazyListState()
    val currentPositionMsState by rememberUpdatedState(currentPositionMs)

    val activeIndex by remember(lines) {
        derivedStateOf { activeLineIndex(lines, currentPositionMsState()) }
    }

    // Browsing: from the moment a finger moves the list until 3s after it
    // lets go. Lines lose their blur and dim evenly so any line can be read
    // and tapped; auto-follow waits until browsing ends.
    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    var browsing by remember { mutableStateOf(false) }
    LaunchedEffect(isDragged) {
        if (isDragged) {
            browsing = true
        } else if (browsing) {
            delay(LYRIC_BROWSE_HOLD_MS)
            browsing = false
        }
    }

    // Apple Music's ripple: the list jumps to the new line at once, and every
    // visible line then springs from where it was to where it now is, each
    // one starting 50ms after the line above it, so the verse ripples upward
    // instead of sliding as one block.
    //
    // The jump and each line's offset must land in the same frame, or the
    // lines flash at their new place for one frame first. So the jump is
    // dispatched inside a frame callback together with the ripple record,
    // and every line derives its offset from that record and the frame clock
    // in its draw layer, with no per-line coroutine that would start a
    // frame late.
    val density = LocalDensity.current
    // The intro's interlude dots are an extra list item ahead of the first line.
    val leadingItems = if ((lines.firstOrNull()?.timeMs ?: 0L) >= LYRIC_INTERLUDE_MIN_MS) 1 else 0
    val ripples = remember { mutableStateListOf<LyricRipple>() }
    var frameNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(activeIndex, browsing) {
        if (browsing || activeIndex !in lines.indices) return@LaunchedEffect
        runCatching {
            val activeItem = activeIndex + leadingItems
            val target = listState.layoutInfo.visibleItemsInfo.find { it.index == activeItem }
            if (target != null) {
                val delta = listState.distanceToCentre(target)
                if (kotlin.math.abs(delta) >= 1f) {
                    withFrameNanos { now ->
                        val firstIndex = listState.firstVisibleItemIndex - leadingItems
                        listState.dispatchRawDelta(delta)
                        frameNanos = now
                        ripples.add(LyricRipple(delta, now, firstIndex))
                    }
                }
            } else {
                // Far away (a seek): no ripple, just go there.
                ripples.clear()
                listState.scrollToItem(index = activeItem)
                listState.layoutInfo.visibleItemsInfo.find { it.index == activeItem }?.let {
                    listState.dispatchRawDelta(listState.distanceToCentre(it))
                }
            }
        }
    }
    // The frame clock runs only while a ripple is settling.
    LaunchedEffect(ripples.isNotEmpty()) {
        while (ripples.isNotEmpty()) {
            withFrameNanos { now ->
                frameNanos = now
                ripples.removeAll { now - it.startNanos > LYRIC_RIPPLE_LIFETIME_NS }
            }
        }
    }

    val isDuet = remember(lines) { lines.any { it.vocalist != 0 } }
    // Room below the last lines, so they can reach the centre too.
    val viewportHeightPx by remember { derivedStateOf { listState.layoutInfo.viewportSize.height } }
    val bottomPadding = maxOf(130.dp, with(density) { (viewportHeightPx / 2).toDp() })
    LazyColumn(
        state = listState,
        modifier = modifier.lyricsFadingEdge(topFadeDp = 36.dp, bottomFadeDp = 110.dp),
        contentPadding = PaddingValues(top = 40.dp, bottom = bottomPadding, start = 12.dp, end = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // A long intro before the first line gets Apple's interlude dots at the top.
        lines.firstOrNull()?.let { first ->
            if (first.timeMs >= LYRIC_INTERLUDE_MIN_MS) {
                item(key = "interlude_intro") {
                    InterludeDots(
                        startMs = 0L,
                        endMs = first.timeMs,
                        currentPositionMs = currentPositionMs,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
        }
        itemsIndexed(lines, key = { index, line -> "$index:${line.timeMs}" }) { index, line ->
            val isActive = index == activeIndex
            // Duets: the second singer's lines sit on the right, each voice in 85% of the width.
            val secondVoice = isDuet && line.vocalist == 1
            val lineAlign = if (secondVoice) TextAlign.End else TextAlign.Start
            val isBgRow = line.syllables.isNotEmpty() && line.syllables.all { it.isBackground }
            val isLineRtl = remember(line, isOverallRtl) {
                line.isRtl || (isOverallRtl && (line.text.isBlank() || line.text == "♪"))
            }


            // Light: the sung line in full white, every other line dim. While
            // browsing the others brighten so they can be read (and lose their
            // blur below); the sung line stays white so it is still easy to find.
            // Dimming every line, the sung one too, made the screen go dark mid-scroll.
            // Before the first line nothing is lit: every line stays dim (Apple keeps the intro
            // on the interlude dots), instead of the whole song showing in full white.
            val alphaTarget = when {
                isActive -> 1f
                browsing -> LYRIC_BROWSE_ALPHA
                else -> LYRIC_INACTIVE_ALPHA
            }
            val alpha by animateFloatAsState(
                targetValue = alphaTarget,
                animationSpec = tween(320, easing = LYRIC_FOCUS_EASE),
                label = "modernLyricAlpha_$index",
            )
            // Depth: inactive lines blurred; a line that becomes active clears
            // 0.15s after it lights up. No blur while browsing.
            val blur = remember { Animatable(0f) }
            LaunchedEffect(isActive, browsing) {
                val target = if (browsing || isActive) 0f else LYRIC_BLUR_DP
                if (isActive && blur.value > 0f) delay(150)
                blur.animateTo(target, tween(300, easing = LYRIC_FOCUS_EASE))
            }
            // Inactive lines sit at 0.98; a line under the finger shrinks to
            // 0.95 with a soft platter behind it, and release seeks there.
            val press = remember { MutableInteractionSource() }
            val pressed by press.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = when {
                    pressed -> 0.95f
                    isActive -> 1f
                    else -> 0.98f
                },
                animationSpec = spring(dampingRatio = 1f, stiffness = LYRIC_LINE_STIFFNESS),
                label = "modernLyricScale_$index",
            )
            val platter by animateFloatAsState(
                targetValue = if (pressed) 0.08f else 0f,
                animationSpec = tween(120),
                label = "modernLyricPlatter_$index",
            )

            val lineLayoutDirection = if (isLineRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides lineLayoutDirection, LocalLyricAlign provides lineAlign) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(if (isLineRtl != secondVoice) 1f else 0f, 0.5f)
                            scaleX = scale
                            scaleY = scale
                            this.translationY = rippleOffset(ripples, frameNanos, index)
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                val r = blur.value * density.density
                                renderEffect = if (r > 0.3f) BlurEffect(r, r) else null
                            }
                            this.alpha = alpha * if (isBgRow) (if (isActive) 0.85f else 0.55f) else 1f
                        }
                        .background(Color.White.copy(alpha = platter), RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = press,
                            indication = null,
                        ) {
                            onSeek(line.timeMs)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    val fontStyle = if (isBgRow) accompanimentStyle else normalStyle
                    // Background vocals ride under the lead line on their own smaller line,
                    // Apple's way. Unflagged ones written as a trailing "(…)" count too.
                    val parts = remember(line) { line.withParenthesisedBackground().splitLeadAndBackground() }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(if (isDuet) LYRIC_DUET_WIDTH else 1f)
                            .align(if (secondVoice) Alignment.TopEnd else Alignment.TopStart),
                    ) {
                        if (!isBgRow && parts != null) {
                            ModernWordByWordLine(
                                line = parts.first,
                                currentPositionMs = currentPositionMs,
                                isActive = isActive,
                                activeColor = Color.White,
                                inactiveColor = Color.White.copy(alpha = LYRIC_UNSUNG_ALPHA),
                                fontStyle = normalStyle,
                                isRtl = isLineRtl,
                            )
                            BackgroundVocalLine(
                                line = parts.second,
                                currentPositionMs = currentPositionMs,
                                isLineActive = isActive,
                                style = accompanimentStyle,
                                isRtl = isLineRtl,
                            )
                        } else ModernWordByWordLine(
                            line = line,
                            currentPositionMs = currentPositionMs,
                            isActive = isActive,
                            activeColor = Color.White,
                            inactiveColor = Color.White.copy(alpha = LYRIC_UNSUNG_ALPHA),
                            fontStyle = fontStyle,
                            isRtl = isLineRtl,
                            modifier = Modifier.padding(start = if (isBgRow) 14.dp else 0.dp),
                        )
                        // Translation / romanization copy under the lyric.
                        // The engine returns null for lines that have no
                        // distinct sub-line, so no per-row text comparison
                        // is needed here.
                        val subLine = subLines?.getOrNull(index)
                        if (subLine != null && subLineStyle != null) {
                            LyricSubLine(
                                text = subLine,
                                style = subLineStyle,
                                reveal = subReveal,
                            )
                        }
                        // A long instrumental gap after this line: Apple's three dots.
                        val next = lines.getOrNull(index + 1)
                        if (next != null) {
                            val endOfLine = remember(line) { line.endMs() }
                            if (next.timeMs - endOfLine >= LYRIC_INTERLUDE_MIN_MS) {
                                InterludeDots(
                                    startMs = endOfLine,
                                    endMs = next.timeMs,
                                    currentPositionMs = currentPositionMs,
                                    // Same space above the dots as between them and the next line.
                                    topGap = 22.dp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Translated or romanized copy of a line, drawn underneath it. Height grows
 * from zero with the panel's shared reveal so the list reflows softly as
 * sub-lines arrive, while the text itself rides a slightly shorter tuck and
 * an alpha ramp (alpha = reveal², so the tail of the motion is a fade rather
 * than a bare height snap). Reads [reveal] per frame in layout and draw
 * phases; the panel animates one value for the whole list.
 */
@Composable
private fun LyricSubLine(
    text: String,
    style: TextStyle,
    reveal: () -> Float,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val height = (placeable.height * reveal()).roundToInt().coerceIn(0, placeable.height)
                layout(placeable.width, height) { placeable.place(0, 0) }
            },
    ) {
        Text(
            text = text,
            style = style,
            color = Color.White.copy(alpha = 0.78f),
            textAlign = LocalLyricAlign.current,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
                .graphicsLayer {
                    val open = reveal()
                    alpha = open * open
                    translationY = -size.height * (1f - open) * 0.6f
                },
        )
    }
}

@Composable
private fun ModernWordByWordLine(
    line: LyricLine,
    currentPositionMs: () -> Long,
    isActive: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    fontStyle: TextStyle,
    isRtl: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val lineLayoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides lineLayoutDirection) {
        if (!line.hasSyllables || !isActive) {
            val animatedColor by animateColorAsState(
                targetValue = activeColor,
                animationSpec = tween(240, easing = FastOutSlowInEasing),
                label = "lineColor",
            )
            Column(
                modifier = modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
            ) {
                Box(Modifier.fillMaxWidth()) {
                    Text(
                        text = line.text.ifBlank { "♪" },
                        style = fontStyle,
                        color = animatedColor,
                        textAlign = LocalLyricAlign.current,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (!line.transliteration.isNullOrBlank() && isActive) {
                    Text(
                        text = line.transliteration,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.2.sp,
                        ),
                        color = activeColor.copy(alpha = 0.72f),
                        textAlign = LocalLyricAlign.current,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 3.dp),
                    )
                }
            }
            return@CompositionLocalProvider
        }

        val displayTexts = remember(line) { resolveSyllableDisplayTexts(line) }
        val fullText = remember(line, displayTexts) {
            line.syllables.indices.joinToString("") { displayTexts.getOrElse(it) { line.syllables[it].text } }
        }
        // Char range of each syllable inside [fullText].
        val charRanges = remember(line, displayTexts) {
            var offset = 0
            line.syllables.indices.map { i ->
                val start = offset
                offset += displayTexts.getOrElse(i) { line.syllables[i].text }.length
                start to offset
            }
        }
        var layout by remember(fullText) { mutableStateOf<TextLayoutResult?>(null) }
        val maskPaint = remember {
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN)
            }
        }
        val clearPaint = remember {
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
            }
        }

        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
        ) {
            // Karaoke fill: dim base text + bright copy clipped to the sung
            // extent with a smooth feathered gradient wipe.
            Box(modifier = Modifier.fillMaxWidth()) {
                // Held words glow, nothing else does (see HeldWordGlow).
                HeldWordGlow(
                    line = line,
                    fullText = fullText,
                    charRanges = charRanges,
                    layout = { layout },
                    currentPositionMs = currentPositionMs,
                    style = fontStyle,
                )
                Text(
                    text = fullText,
                    style = fontStyle,
                    color = inactiveColor,
                    textAlign = LocalLyricAlign.current,
                    onTextLayout = { layout = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawWithContent {
                            val l = layout
                            if (l == null) drawContent()
                            else drawWithSyllableLift(l, line, charRanges, currentPositionMs(), 2.dp.toPx(), isRtl)
                        },
                )
                Text(
                    text = fullText,
                    style = fontStyle,
                    color = activeColor,
                    textAlign = LocalLyricAlign.current,
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawWithContent {
                            val l = layout ?: return@drawWithContent
                            val textLength = fullText.length
                            if (textLength == 0) return@drawWithContent
                            val pos = currentPositionMs()
                            var fill = 0f
                            var sylIdx = -1
                            var sylP = 0f
                            for (i in line.syllables.indices) {
                                val syl = line.syllables[i]
                                if (pos < syl.timeMs) break
                                val (cs, ce) = charRanges[i]
                                val linear = if (syl.durationMs <= 0L) 1f
                                else ((pos - syl.timeMs).toFloat() / syl.durationMs).coerceIn(0f, 1f)
                                // Mild ease-out: a sung syllable attacks fast and
                                // sustains, so a linear wipe reads as lagging.
                                val p = 1f - Math.pow((1f - linear).toDouble(), 1.4).toFloat()
                                fill = cs + (ce - cs) * p
                                sylIdx = i
                                sylP = p
                                if (p < 1f) break
                            }
                            if (fill <= 0f) return@drawWithContent
                            val liftPx = 2.dp.toPx()
                            if (fill >= textLength) {
                                drawWithSyllableLift(l, line, charRanges, pos, liftPx, isRtl)
                                return@drawWithContent
                            }
                            // Interpolate across the syllable's pixel extent, not
                            // per char: in Indic/complex scripts offsets inside a
                            // grapheme cluster (matras, conjuncts) report stalled
                            // or backward x, which froze/jumped the wipe.
                            val (sCs, sCe) = charRanges[sylIdx]
                            val startIdx = sCs.coerceIn(0, textLength - 1)
                            val lastIdx = (sCe - 1).coerceIn(startIdx, textLength - 1)
                            val sameLine = l.getLineForOffset(startIdx) == l.getLineForOffset(lastIdx)
                            val lineIdx: Int
                            val x: Float
                            // Apple's karaoke edge: a 30pt soft gradient, half each
                            // side of the sung point (lineProgressionGradientFeather).
                            val feather = 15.dp.toPx()
                            if (sameLine) {
                                lineIdx = l.getLineForOffset(startIdx)
                                val xs = l.getHorizontalPosition(startIdx, true)
                                val xe = if (sCe < textLength && l.getLineForOffset(sCe) == lineIdx) {
                                    l.getHorizontalPosition(sCe, true)
                                } else if (isRtl) l.getLineLeft(lineIdx) else l.getLineRight(lineIdx)
                                x = xs + (xe - xs) * sylP
                            } else {
                                val charIdx = fill.toInt().coerceIn(0, textLength - 1)
                                val frac = fill - charIdx
                                lineIdx = l.getLineForOffset(charIdx)
                                val x0 = l.getHorizontalPosition(charIdx, true)
                                val x1 = if (charIdx + 1 < textLength && l.getLineForOffset(charIdx + 1) == lineIdx) {
                                    l.getHorizontalPosition(charIdx + 1, true)
                                } else if (isRtl) l.getLineLeft(lineIdx) else l.getLineRight(lineIdx)
                                x = x0 + (x1 - x0) * frac
                            }
                            // Lifted glyphs rise above the row; widen the mask so
                            // the unsung part of a rising syllable stays masked.
                            // Rising syllables lift above the row, so the mask reaches
                            // up too, but only on the first row: on later rows it
                            // would cut the descenders of the row above.
                            val top = if (lineIdx == 0) l.getLineTop(lineIdx) - liftPx * 1.5f else l.getLineTop(lineIdx)
                            val bottom = l.getLineBottom(lineIdx)

                            val canvas = drawContext.canvas.nativeCanvas
                            val checkpoint = canvas.saveLayer(0f, 0f, size.width, size.height, null)
                            drawWithSyllableLift(l, line, charRanges, pos, liftPx, isRtl)

                            if (isRtl) {
                                val xStart = (x - feather).coerceAtLeast(0f)
                                val xEnd = (x + feather).coerceAtMost(size.width)
                                if (xStart > 0f) {
                                    canvas.drawRect(0f, top, xStart, bottom, clearPaint)
                                }
                                if (xEnd > xStart) {
                                    maskPaint.shader = android.graphics.LinearGradient(
                                        xStart, 0f, xEnd, 0f,
                                        android.graphics.Color.TRANSPARENT,
                                        android.graphics.Color.WHITE,
                                        android.graphics.Shader.TileMode.CLAMP,
                                    )
                                    canvas.drawRect(xStart, top, xEnd, bottom, maskPaint)
                                }
                            } else {
                                val xStart = (x - feather).coerceAtLeast(0f)
                                val xEnd = (x + feather).coerceAtMost(size.width)
                                if (xEnd < size.width) {
                                    canvas.drawRect(xEnd, top, size.width, bottom, clearPaint)
                                }
                                if (xEnd > xStart) {
                                    maskPaint.shader = android.graphics.LinearGradient(
                                        xStart, 0f, xEnd, 0f,
                                        android.graphics.Color.WHITE,
                                        android.graphics.Color.TRANSPARENT,
                                        android.graphics.Shader.TileMode.CLAMP,
                                    )
                                    canvas.drawRect(xStart, top, xEnd, bottom, maskPaint)
                                }
                            }
                            if (bottom < size.height) {
                                canvas.drawRect(0f, bottom, size.width, size.height, clearPaint)
                            }
                            canvas.restoreToCount(checkpoint)
                        },
                )
            }

            if (!line.transliteration.isNullOrBlank()) {
                Text(
                    text = line.transliteration,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.2.sp,
                    ),
                    color = activeColor.copy(alpha = 0.76f),
                    textAlign = LocalLyricAlign.current,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }
        }
    }
}

/**
 * Apple Music's held-word glow: only a word that is sustained (sung for 1s or
 * more, and slowly) gets a soft white glow, strongest in the middle of the
 * hold and gone at both ends. Ordinary lines never glow.
 *
 * Drawn as a blurred copy of the line laid out exactly like it (same text,
 * style and width), clipped to the held word, so the halo sits on the glyphs
 * in every script. Below API 31 blur is unavailable and nothing is drawn.
 */
@Composable
private fun HeldWordGlow(
    line: LyricLine,
    fullText: String,
    charRanges: List<Pair<Int, Int>>,
    layout: () -> TextLayoutResult?,
    currentPositionMs: () -> Long,
    style: TextStyle,
) {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return
    val held = remember(line) {
        line.syllables.indices.filter { i ->
            val syl = line.syllables[i]
            val letters = syl.text.trim().length.coerceAtLeast(1)
            syl.durationMs >= LYRIC_HELD_MIN_MS && syl.durationMs / letters >= LYRIC_HELD_MS_PER_CHAR
        }
    }
    if (held.isEmpty()) return
    Text(
        text = fullText,
        style = style,
        color = Color.White,
        textAlign = LocalLyricAlign.current,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val pos = currentPositionMs()
                val syl = held.firstOrNull { i ->
                    val s = line.syllables[i]
                    pos >= s.timeMs && pos < s.timeMs + s.durationMs
                }?.let { line.syllables[it] }
                alpha = if (syl == null) 0f else {
                    val p = (pos - syl.timeMs).toFloat() / syl.durationMs
                    // Apple's glowRange tops out at 0.4.
                    0.4f * kotlin.math.sin(Math.PI.toFloat() * p)
                }
            }
            .blur(6.dp, BlurredEdgeTreatment.Unbounded)
            .drawWithContent {
                val l = layout() ?: return@drawWithContent
                val pos = currentPositionMs()
                val i = held.firstOrNull { idx ->
                    val s = line.syllables[idx]
                    pos >= s.timeMs && pos < s.timeMs + s.durationMs
                } ?: return@drawWithContent
                val (cs, ce) = charRanges[i]
                if (ce <= cs || ce > fullText.length) return@drawWithContent
                val box = l.getPathForRange(cs, ce).getBounds()
                clipRect(box.left - 6f, box.top - 6f, box.right + 6f, box.bottom + 6f) {
                    this@drawWithContent.drawContent()
                }
            },
    )
}

@Composable
private fun ModernPlainLyricsView(
    plainLyrics: String,
    lyricsFontScale: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val isRtl = remember(plainLyrics) { isRtlText(plainLyrics) }
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        val scrollState = rememberScrollState()
        Column(
            modifier = modifier
                .lyricsFadingEdge(topFadeDp = 24.dp, bottomFadeDp = 100.dp)
                .verticalScroll(scrollState)
                .padding(top = 24.dp, bottom = 90.dp, start = 16.dp, end = 16.dp),
        ) {
            Row(
                modifier = Modifier.padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Filled.SyncDisabled,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White.copy(alpha = 0.90f),
                )
                Text(
                    stringResource(R.string.ly_not_synced),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.70f),
                )
            }

            Text(
                text = plainLyrics,
                style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = (28f * lyricsFontScale).sp,
lineHeight = (46f * lyricsFontScale).sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.1.sp,
                ),
                textAlign = TextAlign.Start,
                color = Color.White.copy(alpha = 0.94f),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ModernEmptyLyricsView(
    isInstrumental: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Icon(
                imageVector = if (isInstrumental) Icons.Filled.MusicOff else Icons.Filled.Lyrics,
                contentDescription = null,
                modifier = Modifier.size(42.dp),
                tint = Color.White.copy(alpha = 0.90f),
            )

            Text(
                text = if (isInstrumental) stringResource(R.string.ly_instrumental) else stringResource(R.string.ly_none),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Text(
                text = if (isInstrumental) {
                    stringResource(R.string.ly_no_vocals)
                } else {
                    stringResource(R.string.ly_no_synced)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.70f),
                textAlign = TextAlign.Center,
            )

            if (!isInstrumental) {
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = onRetry) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.ly_retry))
                }
            }
        }
    }
}

/**
 * Leaf host: collects progress ONLY for the lyrics list, so badge +
 * AnimatedContent above never recompose on ticks. Reference-client parity
 * (Metrolist/SimpMusic): the raw playhead position drives focus every tick
 * with no time quantization — word fills step at syllable granularity
 * (>=50ms), so throttling would visibly stall the karaoke highlight.
 */
@Composable
private fun SyncedLyricsProgressHost(
    lines: List<com.tuned.app.data.lyrics.LyricLine>,
    isOverallRtl: Boolean,
    normalStyle: TextStyle,
    accompanimentStyle: TextStyle,
    progressFlow: StateFlow<PlaybackProgressState>,
    progressInitial: PlaybackProgressState,
    state: MusicPlayerState,
    player: MusicPlayer,
    lyricsOffsetMs: Long = 0L,
    subLines: List<String?>? = null,
    subReveal: () -> Float = { 0f },
    subLineStyle: TextStyle? = null,
    modifier: Modifier = Modifier,
) {
    val progress by progressFlow.collectAsStateWithLifecycle(initialValue = progressInitial)
    val latestSampleMs by rememberUpdatedState(progress.positionMs)
    val offsetState by rememberUpdatedState(lyricsOffsetMs)
    val framePositionMs = remember { mutableLongStateOf(progress.positionMs) }
    // Vsync clock (90/120Hz): advances by real frame time and is steered
    // toward the ExoPlayer playhead (sampled every ~60ms). Never runs
    // backwards on normal drift; snaps instantly on seeks/track changes.
    LaunchedEffect(state.isPlaying) {
        if (!state.isPlaying) {
            snapshotFlow { latestSampleMs }.collect { framePositionMs.longValue = it }
            return@LaunchedEffect
        }
        var lastSample = latestSampleMs
        var sampleFrameNanos = -1L
        var lastFrameNanos = -1L
        var display = latestSampleMs.toDouble()
        while (true) {
            withFrameNanos { now ->
                val sample = latestSampleMs
                if (sample != lastSample || sampleFrameNanos < 0L) {
                    lastSample = sample
                    sampleFrameNanos = now
                }
                val target = sample + ((now - sampleFrameNanos) / 1_000_000.0).coerceAtMost(250.0)
                val frameDt = if (lastFrameNanos < 0L) 0.0 else (now - lastFrameNanos) / 1_000_000.0
                lastFrameNanos = now
                val predicted = display + frameDt
                val err = target - predicted
                display = if (kotlin.math.abs(err) > 300.0) {
                    target
                } else {
                    maxOf(display, predicted + err * 0.2)
                }
                framePositionMs.longValue = display.toLong()
            }
        }
    }
    val currentPosition: () -> Long = remember {
        { framePositionMs.longValue + offsetState }
    }
    ModernSyncedLyricsList(
        lines = lines,
        isOverallRtl = isOverallRtl,
        normalStyle = normalStyle,
        accompanimentStyle = accompanimentStyle,
        currentPositionMs = currentPosition,
        isPlaying = state.isPlaying,
        onSeek = { lineTimeMs ->
            // Inverse of the highlight shift: tap targets audio time. Clamp to
            // the track's duration too: the old code only floored at 0, so a
            // line timestamp past the end (bad provider data + positive offset)
            // sought beyond the track.
            val target = (lineTimeMs - lyricsOffsetMs).coerceAtLeast(0L)
            val duration = state.durationMs
            player.seekTo(if (duration > 0L) target.coerceAtMost(duration) else target)
        },
        lyricsOffsetMs = lyricsOffsetMs,
        subLines = subLines,
        subReveal = subReveal,
        subLineStyle = subLineStyle,
        modifier = modifier,
    )
}

/**
 * Leaf host for transport controls: owns the 60ms seekbar clock so the panel
 * above stays static. No behavior change, strictly fewer recompositions.
 */
@Composable
private fun ModernLyricsControlsHost(
    state: MusicPlayerState,
    progressFlow: StateFlow<PlaybackProgressState>,
    progressInitial: PlaybackProgressState,
    player: MusicPlayer,
    wavySeekbarEnabled: Boolean = true,
    onToggleFullscreen: (() -> Unit)? = null,
    isFullscreen: Boolean = false,
    lyricsOffsetMs: Long = 0L,
    onOpenLyricsOffset: (() -> Unit)? = null,
    controlsVisible: Boolean = true,
    displayMode: LyricsDisplayMode = LyricsDisplayMode.Original,
    translationLoading: Boolean = false,
    romanizationLoading: Boolean = false,
    translationAvailable: Boolean = false,
    onToggleTranslation: () -> Unit = {},
    onToggleRomanization: () -> Unit = {},
    showFontSlider: Boolean = false,
    onShowFontSliderChange: (Boolean) -> Unit = {},
    vocalsAvailable: Boolean = false,
    vocalLevel: Float = 0f,
    onVocalLevelChange: (Float) -> Unit = {},
    showSingSlider: Boolean = false,
    onShowSingSliderChange: (Boolean) -> Unit = {},
    onOpenLyricsSource: (() -> Unit)? = null,
    lyricsFontScale: Float = 1f,
    onLyricsFontScaleChange: (Float) -> Unit = {},
    onLyricsFontScalePreview: (Float) -> Unit = {},
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary,
    tertiaryColor: Color = MaterialTheme.colorScheme.tertiary,
    canShareLyrics: Boolean = false,
    onShareLyrics: () -> Unit = {},
    lyricsOffsetActive: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val progress by progressFlow.collectAsStateWithLifecycle(initialValue = progressInitial)
    ModernLyricsControls(
        state = state,
        currentPositionMs = progress.positionMs,
        totalDurationMs = if (progress.durationMs > 0) progress.durationMs else state.durationMs,
        player = player,
        wavySeekbarEnabled = wavySeekbarEnabled,
        onToggleFullscreen = onToggleFullscreen,
        isFullscreen = isFullscreen,
        lyricsOffsetMs = lyricsOffsetMs,
        onOpenLyricsOffset = onOpenLyricsOffset,
        controlsVisible = controlsVisible,
        displayMode = displayMode,
        translationLoading = translationLoading,
        romanizationLoading = romanizationLoading,
        translationAvailable = translationAvailable,
        onToggleTranslation = onToggleTranslation,
        onToggleRomanization = onToggleRomanization,
        showFontSlider = showFontSlider,
        onShowFontSliderChange = onShowFontSliderChange,
        vocalsAvailable = vocalsAvailable,
        vocalLevel = vocalLevel,
        onVocalLevelChange = onVocalLevelChange,
        showSingSlider = showSingSlider,
        onShowSingSliderChange = onShowSingSliderChange,
        onOpenLyricsSource = onOpenLyricsSource,
        lyricsFontScale = lyricsFontScale,
        onLyricsFontScaleChange = onLyricsFontScaleChange,
        onLyricsFontScalePreview = onLyricsFontScalePreview,
        primaryColor = primaryColor,
        secondaryColor = secondaryColor,
        tertiaryColor = tertiaryColor,
        canShareLyrics = canShareLyrics,
        onShareLyrics = onShareLyrics,
        lyricsOffsetActive = lyricsOffsetActive,
        modifier = modifier,
    )
}

/** One tool in the lyrics tool pill: a 44dp icon, accent when on, a spinner while loading. */
@Composable
private fun TranslatePronounceButton(
    displayMode: LyricsDisplayMode,
    loading: Boolean,
    enabled: Boolean,
    onToggleTranslation: () -> Unit,
    onToggleRomanization: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        LyricsToolButton(
            icon = Icons.Filled.Translate,
            description = stringResource(R.string.ly_translate_pronounce_cd),
            active = displayMode != LyricsDisplayMode.Original,
            loading = loading,
            enabled = enabled,
            onClick = { open = true },
        )
        androidx.compose.material3.DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            @Composable
            fun option(label: Int, mode: LyricsDisplayMode, choose: () -> Unit) {
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(stringResource(label)) },
                    trailingIcon = if (displayMode == mode) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else null,
                    onClick = {
                        open = false
                        if (displayMode != mode) choose()
                    },
                )
            }
            option(R.string.ly_mode_original, LyricsDisplayMode.Original) {
                // Turning off is the active mode's own toggle.
                if (displayMode == LyricsDisplayMode.Translated) onToggleTranslation() else onToggleRomanization()
            }
            option(R.string.ly_mode_translation, LyricsDisplayMode.Translated, onToggleTranslation)
            option(R.string.ly_mode_pronunciation, LyricsDisplayMode.Romanized, onToggleRomanization)
        }
    }
}

@Composable
private fun LyricsToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    active: Boolean = false,
    loading: Boolean = false,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 1f,
        animationSpec = ExpressiveMotion.spatialSpring(),
        label = "lyricsToolScale",
    )
    val accent = MaterialTheme.colorScheme.primary
    IconButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(if (active) accent.copy(alpha = 0.24f) else Color.Transparent),
    ) {
        if (loading) {
            ExpressiveInlineLoadingIndicator(size = 20.dp, strokeWidth = 2.2.dp, color = Color.White)
        } else {
            Icon(
                icon,
                contentDescription = description,
                modifier = Modifier.size(22.dp),
                tint = when {
                    active -> accent
                    enabled -> Color.White.copy(alpha = 0.92f)
                    else -> Color.White.copy(alpha = 0.38f)
                },
            )
        }
    }
}

@Composable
private fun ModernLyricsControls(
    state: MusicPlayerState,
    currentPositionMs: Long,
    totalDurationMs: Long,
    player: MusicPlayer,
    wavySeekbarEnabled: Boolean = true,
    onToggleFullscreen: (() -> Unit)? = null,
    isFullscreen: Boolean = false,
    lyricsOffsetMs: Long = 0L,
    onOpenLyricsOffset: (() -> Unit)? = null,
    controlsVisible: Boolean = true,
    displayMode: LyricsDisplayMode = LyricsDisplayMode.Original,
    translationLoading: Boolean = false,
    romanizationLoading: Boolean = false,
    translationAvailable: Boolean = false,
    onToggleTranslation: () -> Unit = {},
    onToggleRomanization: () -> Unit = {},
    showFontSlider: Boolean = false,
    onShowFontSliderChange: (Boolean) -> Unit = {},
    vocalsAvailable: Boolean = false,
    vocalLevel: Float = 0f,
    onVocalLevelChange: (Float) -> Unit = {},
    showSingSlider: Boolean = false,
    onShowSingSliderChange: (Boolean) -> Unit = {},
    onOpenLyricsSource: (() -> Unit)? = null,
    lyricsFontScale: Float = 1f,
    onLyricsFontScaleChange: (Float) -> Unit = {},
    onLyricsFontScalePreview: (Float) -> Unit = {},
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary,
    tertiaryColor: Color = MaterialTheme.colorScheme.tertiary,
    canShareLyrics: Boolean = false,
    onShareLyrics: () -> Unit = {},
    lyricsOffsetActive: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var sliderPosition by remember(lyricsFontScale) { mutableFloatStateOf(lyricsFontScale) }
    var isDragging by remember { mutableStateOf(false) }
    val currentDisplayScale = if (isDragging) sliderPosition else lyricsFontScale
    val buttonsAlpha by animateFloatAsState(
        targetValue = if (controlsVisible) 1f else 0f,
        animationSpec = tween(150),
        label = "lyricsButtonsAlpha",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Apple's Sing: turn the vocals down with a slider.
        AnimatedVisibility(
            visible = showSingSlider && vocalsAvailable,
            enter = fadeIn(tween(150)) + expandVertically(tween(200)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(200)),
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.ly_vocals),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.90f),
                    )
                    // Right is full vocals, as on Apple's slider; left takes them out.
                    androidx.compose.material3.Slider(
                        value = 1f - vocalLevel,
                        onValueChange = { onVocalLevelChange(1f - it) },
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = showFontSlider,
            enter = fadeIn(tween(150)) + expandVertically(tween(200)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(200)),
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.35f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.lyrics_size_percent, (currentDisplayScale * 100).roundToInt()),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.90f),
                        )
                        if (currentDisplayScale != 1.0f) {
                            TextButton(
                                onClick = {
                                    sliderPosition = 1.0f
                                    isDragging = false
                                    onLyricsFontScalePreview(1.0f)
                                    onLyricsFontScaleChange(1.0f)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(26.dp),
                            ) {
                                Text(
                                    "Reset",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "A",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.70f),
                        )
                        Slider(
                            value = currentDisplayScale,
                            onValueChange = {
                                isDragging = true
                                sliderPosition = it
                                onLyricsFontScalePreview(it)
                            },
                            onValueChangeFinished = {
                                isDragging = false
                                onLyricsFontScaleChange(sliderPosition)
                            },
                            valueRange = 0.7f..1.5f,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "A",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.95f),
                        )
                    }
                }
            }
        }

        // The tools as one floating pill, in the dock's shape. The lyrics
        // source moved to the pill at the top of the lyrics.
        val isCustomFont = lyricsFontScale != 1.0f
        val pillShape = RoundedCornerShape(50)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = buttonsAlpha }
                .padding(bottom = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .clip(pillShape)
                    .liquidGlassChrome(pillShape, LocalLiquidGlass.current, LiquidGlassPreset.FloatingControls)
                    .background(liquidGlassContainerColor(Color.White.copy(alpha = 0.12f)))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Apple's Translate & Pronounce: one button, a menu of the three ways to read.
                TranslatePronounceButton(
                    displayMode = displayMode,
                    loading = translationLoading || romanizationLoading,
                    enabled = controlsVisible && translationAvailable,
                    onToggleTranslation = onToggleTranslation,
                    onToggleRomanization = onToggleRomanization,
                )
                if (vocalsAvailable) {
                    LyricsToolButton(
                        icon = Icons.Filled.Mic,
                        description = stringResource(R.string.ly_sing_cd),
                        active = showSingSlider || vocalLevel > 0f,
                        enabled = controlsVisible,
                        onClick = { onShowSingSliderChange(!showSingSlider) },
                    )
                }
                if (canShareLyrics) {
                    LyricsToolButton(
                        icon = Icons.Filled.Share,
                        description = stringResource(R.string.lyshare_button_cd),
                        enabled = controlsVisible,
                        onClick = onShareLyrics,
                    )
                }
                // Lyrics timing lives with the other lyrics tools, not in the header.
                if (onOpenLyricsOffset != null) {
                    LyricsToolButton(
                        icon = Icons.Outlined.Timer,
                        description = stringResource(R.string.player_lyrics_offset_cd),
                        active = lyricsOffsetActive,
                        enabled = controlsVisible,
                        onClick = onOpenLyricsOffset,
                    )
                }
                LyricsToolButton(
                    icon = Icons.Filled.FormatSize,
                    description = stringResource(R.string.ly_size_cd),
                    active = showFontSlider || isCustomFont,
                    enabled = controlsVisible,
                    onClick = { onShowFontSliderChange(!showFontSlider) },
                )
                if (onToggleFullscreen != null) {
                    LyricsToolButton(
                        icon = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                        description = if (isFullscreen) "Exit fullscreen lyrics" else "Fullscreen lyrics",
                        enabled = controlsVisible,
                        onClick = onToggleFullscreen,
                    )
                }
            }
        }

        if (isFullscreen) return@Column

        // Current-gesture value only; null = finger off, show live position.
        // Keyed by track so a previous song's drag can never leak into this
        // one, and nullable so a press without movement seeks nowhere while a
        // gesture that ends without onValueChangeFinished can't pin the bar.
        val lyricsTrackKey = state.current?.let { it.videoId ?: "${it.artist}|${it.title}" }
        val seekInteraction = remember(lyricsTrackKey) { MutableInteractionSource() }
        var isInteracting by remember(lyricsTrackKey) { mutableStateOf(false) }
        LaunchedEffect(seekInteraction, lyricsTrackKey) {
            var dragCount = 0
            var pressCount = 0
            seekInteraction.interactions.collect { interaction ->
                when (interaction) {
                    is DragInteraction.Start -> dragCount++
                    is DragInteraction.Stop, is DragInteraction.Cancel -> dragCount = maxOf(0, dragCount - 1)
                    is PressInteraction.Press -> pressCount++
                    is PressInteraction.Release, is PressInteraction.Cancel -> pressCount = maxOf(0, pressCount - 1)
                }
                isInteracting = dragCount > 0 || pressCount > 0
            }
        }
        var dragValue by remember(lyricsTrackKey) { mutableStateOf<Float?>(null) }
        var lastSeekValue by remember(lyricsTrackKey) { mutableStateOf<Float?>(null) }
        LaunchedEffect(isInteracting, lyricsTrackKey) {
            if (!isInteracting) {
                delay(120L)
                dragValue = null
                lastSeekValue = null
            }
        }
        LaunchedEffect(dragValue, isInteracting, lyricsTrackKey) {
            if (dragValue != null && !isInteracting) {
                delay(250L)
                dragValue = null
                lastSeekValue = null
            }
        }
        val end = totalDurationMs.coerceAtLeast(1).toFloat()
        val shown = (dragValue ?: currentPositionMs.coerceIn(0, totalDurationMs.coerceAtLeast(0)).toFloat())
            .coerceIn(0f, end)

        // The same scrub bar as the player, following the same Wavy seek bar
        // setting, so the bar does not change when switching to lyrics.
        ScrubBar(
            progressState = player.progressState,
            fallbackDurationMs = totalDurationMs,
            playedColor = Color.White,
            qualityText = "",
            onQualityClick = {},
            onSeek = player::seekTo,
            wavy = wavySeekbarEnabled,
            playing = state.isPlaying,
            showLabels = false,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                formatTime(shown.toLong()),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val prevInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = player::previous,
                    interactionSource = prevInteraction,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .liquidGlassChrome(CircleShape, LocalLiquidGlass.current, LiquidGlassPreset.FloatingControls, interactionSource = prevInteraction)
                        .background(liquidGlassContainerColor(Color.White.copy(alpha = 0.14f))),
                ) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        "Previous",
                        Modifier.size(24.dp),
                        tint = Color.White.copy(alpha = 0.94f),
                    )
                }

                val playInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = player::togglePlayPause,
                    interactionSource = playInteraction,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .liquidGlassChrome(CircleShape, LocalLiquidGlass.current, LiquidGlassPreset.FloatingControls, interactionSource = playInteraction)
                        .background(liquidGlassContainerColor(Color.White.copy(alpha = 0.18f))),
                ) {
                    if (state.isBuffering) {
                        ExpressiveInlineLoadingIndicator(
                            size = 24.dp,
                            color = Color.White,
                            strokeWidth = 2.5.dp,
                        )
                    } else {
                        AnimatedPlayPauseIcon(state.isPlaying, Modifier.size(28.dp))
                    }
                }

                val nextInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = player::next,
                    interactionSource = nextInteraction,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .liquidGlassChrome(CircleShape, LocalLiquidGlass.current, LiquidGlassPreset.FloatingControls, interactionSource = nextInteraction)
                        .background(liquidGlassContainerColor(Color.White.copy(alpha = 0.14f))),
                ) {
                    Icon(
                        Icons.Filled.SkipNext,
                        "Next",
                        Modifier.size(24.dp),
                        tint = Color.White.copy(alpha = 0.94f),
                    )
                }
            }

            Text(
                "−${formatTime((totalDurationMs - shown.toLong()).coerceAtLeast(0))}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

/**
 * Lift envelope (0..1) for a syllable: rises as it starts being sung, holds
 * while sung, then settles back after it ends, so a soft wave travels across
 * the line. Purely visual; derived from the same timestamps as the wipe.
 */
private fun syllableLift(syl: LyricSyllable, pos: Long): Float {
    // Apple Music: a syllable rises 2pt as it is sung and stays up, so the sung
    // part of the line sits slightly higher. It never drops back (no bounce).
    if (pos < syl.timeMs) return 0f
    val dur = syl.durationMs.coerceIn(140L, 420L)
    val rise = ((pos - syl.timeMs).toFloat() / dur).coerceIn(0f, 1f)
    val inv = 1f - rise
    return 1f - inv * inv * inv
}

/**
 * Draws the text content with each currently-lifting syllable translated up
 * by up to [liftPx]. Syllables wrapping across visual lines are not lifted.
 */
private fun ContentDrawScope.drawWithSyllableLift(
    l: TextLayoutResult,
    line: LyricLine,
    charRanges: List<Pair<Int, Int>>,
    pos: Long,
    liftPx: Float,
    isRtl: Boolean,
) {
    val textLength = l.layoutInput.text.length
    var rects: ArrayList<FloatArray>? = null
    for (i in line.syllables.indices) {
        val syl = line.syllables[i]
        val lift = syllableLift(syl, pos)
        val grow = syllableEmphasis(syl, pos)
        if (lift <= 0.001f && grow <= 0.001f) continue
        val (cs, ce) = charRanges.getOrNull(i) ?: continue
        if (ce <= cs || cs >= textLength) continue
        val last = (ce - 1).coerceAtMost(textLength - 1)
        val li = l.getLineForOffset(cs)
        if (li != l.getLineForOffset(last)) continue
        val top = l.getLineTop(li)
        val bottom = l.getLineBottom(li)
        fun xAt(off: Int): Float = if (off < textLength && l.getLineForOffset(off) == li) {
            l.getHorizontalPosition(off, true)
        } else if (isRtl) l.getLineLeft(li) else l.getLineRight(li)
        if (rects == null) rects = ArrayList(8)
        if (grow <= 0.001f) {
            val xs = xAt(cs)
            val xe = xAt(ce)
            rects += floatArrayOf(minOf(xs, xe), top, maxOf(xs, xe), bottom, -lift * liftPx, 1f)
            continue
        }
        // A held word swells letter by letter: a wave crosses the word over the note, each
        // letter growing about its own centre as the wave passes, so the word keeps its width.
        val letters = (cs until minOf(ce, textLength)).filter { !l.layoutInput.text[it].isWhitespace() }
        val n = letters.size.coerceAtLeast(1)
        val p = ((pos - syl.timeMs).toFloat() / syl.durationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
        val center = p * n - 0.5f
        for ((k, off) in letters.withIndex()) {
            val d = k - center
            val wave = kotlin.math.exp(-(d * d) / 2f)
            // Letters the wave has passed stay a little larger until the word ends.
            val held = if (k < center) 0.35f else 0f
            val scale = 1f + (LYRIC_EMPHASIS_MAX - 1f) * grow * maxOf(wave, held)
            val xs = xAt(off)
            val xe = xAt(off + 1)
            rects += floatArrayOf(minOf(xs, xe), top, maxOf(xs, xe), bottom, -lift * liftPx, scale)
        }
    }
    if (rects == null) {
        drawContent()
        return
    }
    val canvas = drawContext.canvas.nativeCanvas
    canvas.save()
    for (r in rects) canvas.clipOutRect(r[0], r[1], r[2], r[3])
    drawContent()
    canvas.restore()
    for (r in rects) {
        canvas.save()
        canvas.translate(0f, r[4])
        // A held word grows from its baseline centre; clipping after the scale keeps
        // the whole enlarged word.
        if (r[5] > 1.0005f) canvas.scale(r[5], r[5], (r[0] + r[2]) / 2f, r[3])
        canvas.clipRect(r[0], r[1], r[2], r[3])
        drawContent()
        canvas.restore()
    }
}

/** Apple's emphasizingScaleRange: a held word grows up to 14% while it's sung. */
private const val LYRIC_EMPHASIS_MAX = 1.14f

/** How long a held word takes to settle back to normal size after it ends. */
private const val LYRIC_EMPHASIS_RELEASE_MS = 300f

/**
 * How far a syllable is into its emphasis, 0 to 1. Only held words (long, slowly sung, the
 * same rule as the glow) grow: they swell over the note and ease back after it ends.
 */
private fun syllableEmphasis(syl: LyricSyllable, pos: Long): Float {
    val letters = syl.text.trim().length.coerceAtLeast(1)
    if (syl.durationMs < LYRIC_HELD_MIN_MS || syl.durationMs / letters < LYRIC_HELD_MS_PER_CHAR) return 0f
    if (pos < syl.timeMs) return 0f
    val end = syl.timeMs + syl.durationMs
    if (pos <= end) {
        val p = (pos - syl.timeMs).toFloat() / syl.durationMs
        return 1f - (1f - p) * (1f - p)
    }
    val t = ((pos - end) / LYRIC_EMPHASIS_RELEASE_MS).coerceIn(0f, 1f)
    val k = 1f - t
    return k * k * (3f - 2f * k)
}

/**
 * Background vocals as Apple draws them: a smaller line under the lead, scaled to 0.9 and dim
 * until its first word starts, then growing to full size and lighting up with its own wipe.
 * While the row isn't the one being sung it stays small and dim like the rest of the row.
 */
@Composable
private fun BackgroundVocalLine(
    line: LyricLine,
    currentPositionMs: () -> Long,
    isLineActive: Boolean,
    style: TextStyle,
    isRtl: Boolean,
) {
    val startMs = line.syllables.firstOrNull()?.timeMs ?: line.timeMs
    val endMs = line.syllables.lastOrNull()?.let { it.timeMs + it.durationMs } ?: (line.timeMs + line.durationMs)
    val singing by remember(line, isLineActive) {
        derivedStateOf { isLineActive && currentPositionMs() in (startMs - 100)..(endMs + 400) }
    }
    val scale by animateFloatAsState(
        targetValue = if (singing) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 1f, stiffness = LYRIC_LINE_STIFFNESS),
        label = "bgVocalScale",
    )
    val bright by animateFloatAsState(
        targetValue = if (singing) 1f else 0f,
        animationSpec = tween(250, easing = LYRIC_FOCUS_EASE),
        label = "bgVocalBright",
    )
    Box(
        Modifier
            .fillMaxWidth()
            // Apple's backgroundVocalsTopSpacing.
            .padding(top = 15.dp)
            .graphicsLayer {
                transformOrigin = TransformOrigin(if (isRtl) 1f else 0f, 0f)
                scaleX = scale
                scaleY = scale
                alpha = 0.55f + 0.45f * bright
            },
    ) {
        ModernWordByWordLine(
            line = line,
            currentPositionMs = currentPositionMs,
            isActive = singing,
            activeColor = Color.White.copy(alpha = 0.85f),
            inactiveColor = Color.White.copy(alpha = 0.35f),
            fontStyle = style,
            isRtl = isRtl,
        )
    }
}

/**
 * Marks a trailing parenthesised phrase ("self(Old self)") as background vocals when the
 * provider sent it unflagged, and drops its outer parentheses. Lines that already have
 * flagged background syllables, or no lead words before the "(", are left alone.
 */
internal fun LyricLine.withParenthesisedBackground(): LyricLine {
    if (!hasSyllables || syllables.any { it.isBackground }) return this
    val open = syllables.indexOfFirst { it.text.contains('(') }
    if (open < 0 || !syllables.last().text.trimEnd().endsWith(")")) return this
    val openSyl = syllables[open]
    val before = openSyl.text.substringBefore('(')
    // Leads must exist before the parenthesis, inside this syllable or earlier ones.
    if (open == 0 && before.isBlank()) return this
    val out = ArrayList<LyricSyllable>(syllables.size + 1)
    for (i in 0 until open) out += syllables[i]
    if (before.isNotBlank()) {
        // "self(Old": the lead part keeps the syllable's start, the rest is background.
        out += openSyl.copy(text = before.trimEnd(), durationMs = openSyl.durationMs / 2)
        out += openSyl.copy(
            text = openSyl.text.substringAfter('('),
            timeMs = openSyl.timeMs + openSyl.durationMs / 2,
            durationMs = openSyl.durationMs - openSyl.durationMs / 2,
            isBackground = true,
            appendToPrevious = false,
        )
    } else {
        out += openSyl.copy(text = openSyl.text.replaceFirst("(", ""), isBackground = true)
    }
    for (i in open + 1 until syllables.size) out += syllables[i].copy(isBackground = true)
    val last = out.last()
    out[out.lastIndex] = last.copy(text = last.text.trimEnd().removeSuffix(")"))
    return copy(syllables = out.filter { it.text.isNotBlank() })
}

/** The lead words and the background words as two lines, or null when there's only one kind. */
internal fun LyricLine.splitLeadAndBackground(): Pair<LyricLine, LyricLine>? {
    val leads = syllables.filter { !it.isBackground }
    val backs = syllables.filter { it.isBackground }
    if (leads.isEmpty() || backs.isEmpty()) return null
    fun textOf(list: List<LyricSyllable>) = list.joinToString("") { s ->
        if (s.appendToPrevious) s.text.trim() else " " + s.text.trim()
    }.trim()
    val lead = copy(text = textOf(leads), syllables = leads)
    val back = LyricLine(
        timeMs = backs.first().timeMs,
        durationMs = backs.last().let { it.timeMs + it.durationMs } - backs.first().timeMs,
        text = textOf(backs),
        syllables = backs.map { it.copy(isBackground = false) },
    )
    return lead to back
}

/**
 * Animated pixel scroll for [LazyListState], which only ships instant
 * [LazyListState.scrollBy] and indexed [LazyListState.animateScrollToItem].
 * A cubic-bezier frame loop so the active-line follow glides rather than
 * jumping. Callers already guard with runCatching. Mirrors the classic
 * view's helper (same package, file-private there so duplicated here).
 */
private suspend fun LazyListState.animateScrollBy(pixels: Float, durationMs: Float = 560f) {
    if (pixels == 0f) return
    var consumed = 0f
    var startNanos = -1L
    var done = false
    while (!done) {
        val target = withFrameNanos { now ->
            if (startNanos < 0L) startNanos = now
            val t = ((now - startNanos) / (durationMs * 1_000_000f)).coerceIn(0f, 1f)
            done = t >= 1f
            pixels * LYRIC_EASE.transform(t)
        }
        val delta = target - consumed
        val scrolled = scrollBy(delta)
        consumed += scrolled
        if (scrolled == 0f && delta != 0f) {
            break
        }
    }
}

/** Follow-scroll duration by gap: short nudges stay crisp, whole-verse jumps
 *  glide, bounded to the calm 350–500ms band. */
private fun lyricScrollDurationMs(deltaPx: Float): Float =
    (LYRIC_SCROLL_MIN_MS + kotlin.math.abs(deltaPx) * 0.35f)
        .coerceAtMost(LYRIC_SCROLL_MAX_MS)

internal fun activeLineIndex(lines: List<LyricLine>, positionMs: Long): Int {
    var low = 0
    var high = lines.size
    while (low < high) {
        val middle = (low + high) ushr 1
        if (lines[middle].timeMs <= positionMs) low = middle + 1 else high = middle
    }
    return low - 1
}

/**
 * Guarantees proper spacing between words across all providers.
 * If provider syllable tokens stripped whitespace, restores spaces using line.text as ground truth.
 */
internal fun resolveSyllableDisplayTexts(line: LyricLine): List<String> {
    val syllables = line.syllables
    if (syllables.isEmpty()) return emptyList()

    val fullText = line.text
    if (fullText.contains(' ') || fullText.contains('\u00A0')) {
        val result = mutableListOf<String>()
        var cursor = 0

        for (i in syllables.indices) {
            val syl = syllables[i]
            val cleanSyl = syl.text.trim()
            if (cleanSyl.isEmpty()) {
                result.add(syl.text)
                continue
            }

            val idx = fullText.indexOf(cleanSyl, startIndex = cursor, ignoreCase = true)
            if (idx >= 0) {
                val endIdx = idx + cleanSyl.length
                val hasTrailingSpace = endIdx < fullText.length && (fullText[endIdx] == ' ' || fullText[endIdx] == '\u00A0')
                if (hasTrailingSpace && i < syllables.size - 1) {
                    result.add("$cleanSyl ")
                    cursor = endIdx + 1
                } else {
                    result.add(cleanSyl)
                    cursor = endIdx
                }
            } else {
                if (i < syllables.size - 1) {
                    result.add("$cleanSyl ")
                } else {
                    result.add(cleanSyl)
                }
            }
        }
        return result
    }

    return syllables.map { it.text }
}

/** A gap at least this long between lines gets interlude dots. */
private const val LYRIC_INTERLUDE_MIN_MS = 4500L

/** Where a line's singing ends: its last syllable's end, or its own duration. */
private fun LyricLine.endMs(): Long =
    syllables.maxOfOrNull { it.timeMs + it.durationMs } ?: (timeMs + durationMs)

/**
 * Apple's instrumental-break dots: three 12dp dots, 8dp apart, in a 40dp slot. They open when
 * the gap starts, light up one after another as it passes, and breathe gently. In the last
 * half second they swell, then shrink and fade as the next line comes in. Outside the gap the
 * slot takes no space.
 */
@Composable
private fun InterludeDots(
    startMs: Long,
    endMs: Long,
    currentPositionMs: () -> Long,
    modifier: Modifier = Modifier,
    topGap: androidx.compose.ui.unit.Dp = 0.dp,
) {
    val showing by remember(startMs, endMs) {
        derivedStateOf { currentPositionMs() in (startMs + 200)..endMs }
    }
    val open by animateFloatAsState(
        targetValue = if (showing) 1f else 0f,
        animationSpec = spring(dampingRatio = 1f, stiffness = LYRIC_LINE_STIFFNESS),
        label = "interludeOpen",
    )
    if (open <= 0.001f) return
    Box(
        modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val h = (placeable.height * open).roundToInt()
                layout(placeable.width, h) { placeable.place(0, 0) }
            }
            .padding(top = topGap)
            .height(24.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        androidx.compose.foundation.Canvas(Modifier.size(width = 52.dp, height = 24.dp)) {
            val pos = currentPositionMs()
            val span = (endMs - startMs).coerceAtLeast(1L)
            val f = ((pos - startMs).toFloat() / span).coerceIn(0f, 1f)
            val toEnd = (endMs - pos).toFloat()
            // Breathing: a slow swell across the whole group.
            val breath = 1f + 0.06f * kotlin.math.sin(2f * Math.PI.toFloat() * (pos - startMs) / 1600f)
            // Last 500ms: swell to 1.15, then shrink to nothing.
            val outro = when {
                toEnd > 500f -> 1f
                toEnd > 250f -> 1f + 0.15f * (1f - (toEnd - 250f) / 250f)
                else -> 1.15f * (toEnd / 250f).coerceIn(0f, 1f)
            }
            val groupScale = breath * outro * open
            val r = 6.dp.toPx()
            val gap = 8.dp.toPx()
            val cy = size.height / 2f
            val pivotX = r * 3 + gap
            for (i in 0 until 3) {
                val lit = (f * 3f - i).coerceIn(0f, 1f)
                val cx = r + i * (2 * r + gap)
                val x = pivotX + (cx - pivotX) * groupScale
                drawCircle(
                    color = Color.White.copy(alpha = (0.18f + 0.82f * lit) * open),
                    radius = r * groupScale,
                    center = androidx.compose.ui.geometry.Offset(x, cy),
                )
            }
        }
    }
}

@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.tuned.app.ui.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuned.app.R
import com.tuned.app.ui.common.adaptiveContentWidth
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.ExpressivePillShape
import com.tuned.app.ui.theme.Motion
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.rememberMotion
import com.tuned.app.ui.theme.tunedColors
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The welcome screen: the Tuned lockup (flower mark beside the wordmark) on
 * Tuned's soft accent wash, then two ways in.
 *
 * Intro, about 1.6s: the flower turns in large at the centre, settles to its
 * lockup size beside the wordmark while the letters slide out from behind it,
 * then the actions rise in. After that the mark keeps a slow
 * turn. With animations off in the system, everything is shown at once and
 * the mark stays still.
 *
 * Last.fm is not part of this path. It lives in Settings and is optional.
 */
@Composable
fun LoginScreen(
    onLoginWithYouTube: () -> Unit,
    onContinueAsGuest: () -> Unit,
    onRestoreBackupAndSignIn: ((String) -> Unit)? = null,
    onDismissError: (() -> Unit)? = null,
    errorMessage: String? = null,
    isBusy: Boolean = false,
    onOpenDownloads: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.tunedColors
    val accent = MaterialTheme.colorScheme.primary
    val motion = rememberMotion()
    val restoreScope = androidx.compose.runtime.rememberCoroutineScope()
    var restoreReadError by remember { mutableStateOf<String?>(null) }
    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null && onRestoreBackupAndSignIn != null) {
            // Was a synchronous readText() on the main thread in this
            // callback — a large backup froze the login screen.
            restoreScope.launch {
                val content = runCatching {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                            ?: error("Could not read this backup")
                    }
                }
                content.onSuccess {
                    restoreReadError = null
                    onRestoreBackupAndSignIn(it)
                }.onFailure {
                    restoreReadError = it.message ?: "Could not read this backup"
                }
            }
        }
    }

    // 0 → 1 per stage. Reduced motion starts every stage at its end.
    val start = if (motion.reduced) 1f else 0f
    val markIn = remember { Animatable(start) }
    val settle = remember { Animatable(start) }
    val actionsIn = remember { Animatable(start) }
    LaunchedEffect(motion.reduced) {
        if (motion.reduced) return@LaunchedEffect
        markIn.animateTo(1f, tween(620, easing = Motion.EmphasizedDecelerate))
        val s = async { settle.animateTo(1f, tween(640, easing = Motion.Emphasized)) }
        delay(360)
        launch { actionsIn.animateTo(1f, tween(520, easing = Motion.EmphasizedDecelerate)) }
        s.await()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
            .background(
                // Tuned's soft accent wash from the top edge.
                Brush.verticalGradient(
                    0f to accent.copy(alpha = 0.22f),
                    0.55f to Color.Transparent,
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .adaptiveContentWidth(maxWidth = 480.dp)
                .align(Alignment.Center)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = Space.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Lockup(
                    markColor = colors.text1,
                    textColor = colors.text1,
                    markIn = markIn.value,
                    settle = settle.value,
                    spin = !motion.reduced,
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Space.xl)
                    .graphicsLayer {
                        alpha = actionsIn.value
                        translationY = (1f - actionsIn.value) * 32.dp.toPx()
                    },
            ) {
                Button(
                    onClick = onLoginWithYouTube,
                    enabled = !isBusy,
                    shape = ExpressivePillShape,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Icon(Icons.Filled.PlayCircle, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(Space.sm))
                    Text(stringResource(R.string.login_yt), style = AppType.bodyStrong)
                }
                Spacer(Modifier.height(Space.md))
                FilledTonalButton(
                    onClick = onContinueAsGuest,
                    enabled = !isBusy,
                    shape = ExpressivePillShape,
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = colors.surface2, contentColor = colors.text1),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Text(stringResource(R.string.login_guest), style = AppType.bodyStrong)
                }
                Spacer(Modifier.height(Space.md))
                Text(
                    stringResource(R.string.login_guest_desc),
                    style = AppType.bodyS,
                    color = colors.text2,
                    textAlign = TextAlign.Center,
                )

                if (onRestoreBackupAndSignIn != null || onOpenDownloads != null) {
                    Spacer(Modifier.height(Space.lg))
                    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                        if (onRestoreBackupAndSignIn != null) {
                            TextButton(
                                onClick = { restoreBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                                enabled = !isBusy,
                            ) {
                                Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(Space.xs + Space.xxs))
                                Text(stringResource(R.string.login_restore), style = AppType.label)
                            }
                        }
                        if (onOpenDownloads != null) {
                            TextButton(onClick = onOpenDownloads) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(Space.xs + Space.xxs))
                                Text(stringResource(R.string.login_downloads), style = AppType.label)
                            }
                        }
                    }
                }

                val visibleError = errorMessage ?: restoreReadError
                if (visibleError != null) {
                    Spacer(Modifier.height(Space.md))
                    Text(visibleError, style = AppType.bodyS, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    if (onDismissError != null) {
                        TextButton(onClick = {
                            restoreReadError = null
                            onDismissError()
                        }) { Text(stringResource(R.string.gen_dismiss), style = AppType.label) }
                    }
                }
            }
        }
    }
}

private val WordSize = 52.sp
// Google Sans Flex: x-height 0.51 em, cap height 0.72 em.
private const val XHeightEm = 0.51f
private const val CapHeightEm = 0.72f
// The flower fills 100 of the drawable's 108 units.
private const val MarkInset = 108f / 100f
private const val MarkIntroScale = 2.4f

/**
 * The wordmark face: Google Sans Flex at its heaviest and fairly wide, with the
 * display optical size and square corners, as in the Tuned lockup.
 */
private val WordmarkFamily = FontFamily(
    Font(
        R.font.google_sans_flex,
        weight = FontWeight.Black,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(1000),
            FontVariation.width(120f),
            FontVariation.Setting("opsz", 144f),
            FontVariation.Setting("ROND", 0f),
        ),
    ),
)

/**
 * Flower mark beside lowercase "tuned". The mark is as tall as the capitals and
 * sits on the baseline, with a gap of a third of the x-height. [markIn] turns the mark
 * in at [MarkIntroScale] centred on the lockup; [settle] shrinks it into place
 * and lets each letter slide out from behind it, staggered.
 */
@Composable
private fun Lockup(
    markColor: Color,
    textColor: Color,
    markIn: Float,
    settle: Float,
    spin: Boolean,
) {
    val density = LocalDensity.current
    var lockupWidthPx by remember { mutableIntStateOf(0) }
    val turn by if (spin) {
        rememberInfiniteTransition(label = "markTurn").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart),
            label = "markTurnAngle",
        )
    } else {
        remember { mutableStateOf(0f) }
    }
    val name = stringResource(R.string.set_about_title)
    val wordmark = name.lowercase()
    val wordStyle = TextStyle(
        fontFamily = WordmarkFamily,
        fontWeight = FontWeight.Black,
        fontSize = WordSize,
        lineHeight = WordSize,
        letterSpacing = 0.01.em,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )
    val xHeight = with(density) { (WordSize.toPx() * XHeightEm).toDp() }
    // As tall as the capitals (the top of the t), sitting on the baseline.
    val markSize = with(density) { (WordSize.toPx() * CapHeightEm * MarkInset).toDp() }
    val markGap = xHeight * 0.36f

    // Never wider than the screen: measure at full size, then scale the whole lockup down to fit.
    var availablePx by remember { mutableIntStateOf(0) }
    val fit = if (availablePx > 0 && lockupWidthPx > availablePx) availablePx.toFloat() / lockupWidthPx else 1f
    Box(
        Modifier
            .fillMaxWidth()
            .onSizeChanged { availablePx = it.width },
        contentAlignment = Alignment.Center,
    ) {
    Row(
        modifier = Modifier
            .wrapContentWidth(unbounded = true)
            .onSizeChanged { lockupWidthPx = it.width }
            .graphicsLayer {
                scaleX = fit
                scaleY = fit
            }
            .semantics { contentDescription = name },
    ) {
        // Offset that puts the mark at the lockup's centre before it settles.
        val centreShift = with(density) { (lockupWidthPx / 2f) - (markSize.toPx() / 2f) }
        Image(
            painter = painterResource(R.drawable.ic_tuned_mark),
            contentDescription = null,
            colorFilter = ColorFilter.tint(markColor),
            modifier = Modifier
                // The flower's bottom edge sits on the baseline.
                .alignBy { (it.measuredHeight * (1f - (MarkInset - 1f) / 2f / MarkInset)).toInt() }
                .size(markSize)
                .graphicsLayer {
                    val scale = MarkIntroScale + (1f - MarkIntroScale) * settle
                    scaleX = scale * (0.6f + 0.4f * markIn)
                    scaleY = scaleX
                    alpha = markIn
                    rotationZ = (1f - markIn) * -120f + turn
                    translationX = centreShift * (1f - settle)
                },
        )
        Spacer(Modifier.width(markGap))
        Row(Modifier.alignByBaseline()) {
            wordmark.forEachIndexed { i, ch ->
                // Each letter starts a little later, so the word unrolls from the mark.
                val delayFrac = i * 0.08f
                val t = ((settle - delayFrac) / (1f - delayFrac)).coerceIn(0f, 1f)
                Text(
                    ch.toString(),
                    style = wordStyle,
                    color = textColor,
                    modifier = Modifier
                        .alignByBaseline()
                        .graphicsLayer {
                            alpha = t
                            translationX = (1f - t) * -(markGap.toPx() + 24.dp.toPx())
                        },
                )
            }
        }
    }
    }
}

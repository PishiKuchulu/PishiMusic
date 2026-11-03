package com.tuned.app.ui.player

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tuned.app.R
import com.tuned.app.playback.AutoMixTransition
import com.tuned.app.ui.common.ArtworkImage
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * The blend's progress for drawing: 1 outside a blend. A blend starts at exactly 0, with
 * no easing into it, or the new cover would flash before the old one covers it.
 */
@Composable
internal fun rememberAutoMixProgress(transition: AutoMixTransition?): () -> Float {
    val blending = transition != null
    val smoothed = remember(blending) { Animatable(if (blending) 0f else 1f) }
    LaunchedEffect(transition?.progress) {
        transition?.let { smoothed.animateTo(it.progress, tween(AUTOMIX_SMOOTH_MS, easing = LinearEasing)) }
    }
    return { smoothed.value }
}

/** Apple's artwork blend: how strong the effect is at this point in the transition. */
internal fun autoMixIntensity(progress: Float): Float = min(min(progress, 1f - progress) * 4.8f, 0.6f)

/** The song overlap is long; the covers only need the middle of it to change over. */
internal fun autoMixVisualProgress(progress: Float): Float {
    val p = ((progress - AUTOMIX_VISUAL_FROM) / (AUTOMIX_VISUAL_TO - AUTOMIX_VISUAL_FROM)).coerceIn(0f, 1f)
    return p * p * (3f - 2f * p)
}

/**
 * Melts a cover: bends it with a slow flowing distortion and blurs it, by [amount] from
 * 0 to 1. Both covers get this, so there is no edge or shape anywhere, just one image
 * losing its form and another finding it. Below Android 13 it is blur alone, and below
 * Android 12 there's no effect and the covers simply crossfade.
 */
private fun meltEffect(amount: Float, time: Float, widthPx: Float, heightPx: Float, blurPx: Float): androidx.compose.ui.graphics.RenderEffect? {
    if (amount <= 0.001f || Build.VERSION.SDK_INT < Build.VERSION_CODES.S || widthPx <= 0f) return null
    val radius = (amount * blurPx).coerceAtLeast(0.01f)
    val blur = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return blur.asComposeRenderEffect()
    // Compiled by the GPU driver at run time, so a driver that refuses it only loses the bend.
    val shader = runCatching {
        RuntimeShader(MELT_SHADER).apply {
            setFloatUniform("size", widthPx, heightPx)
            setFloatUniform("amount", amount)
            setFloatUniform("time", time)
        }
    }.getOrNull() ?: return blur.asComposeRenderEffect()
    return RenderEffect.createChainEffect(
        blur,
        RenderEffect.createRuntimeShaderEffect(shader, "content"),
    ).asComposeRenderEffect()
}

/** The new cover, underneath: it starts melted and slightly large, and settles into place. */
internal fun Modifier.autoMixIncomingCover(progress: () -> Float): Modifier = graphicsLayer {
    val p = progress()
    if (p >= 1f) {
        renderEffect = null
        scaleX = 1f
        scaleY = 1f
    } else {
        val shown = autoMixVisualProgress(p)
        val scale = 1f + (1f - shown) * AUTOMIX_SCALE
        scaleX = scale
        scaleY = scale
        renderEffect = meltEffect(1f - shown, p * AUTOMIX_FLOW_SPEED, size.width, size.height, AUTOMIX_BLUR_DP.dp.toPx())
    }
}

/** The old cover on top of the new one: it melts away, drifting outward, then dissolves. */
@Composable
internal fun BoxScope.AutoMixArtworkBlend(transition: AutoMixTransition?, outgoingCover: String?, progress: () -> Float) {
    if (transition == null) return
    val cover = outgoingCover ?: transition.fromArtwork
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                val p = progress()
                val gone = autoMixVisualProgress(p)
                alpha = 1f - gone
                val scale = 1f + gone * AUTOMIX_SCALE
                scaleX = scale
                scaleY = scale
                renderEffect = meltEffect(gone, p * AUTOMIX_FLOW_SPEED, size.width, size.height, AUTOMIX_BLUR_DP.dp.toPx())
            },
    ) {
        ArtworkImage(
            name = "",
            artist = "",
            embeddedUrl = cover,
            fallbackIcon = Icons.Filled.MusicNote,
            modifier = Modifier.fillMaxSize(),
            decodeSizePx = PLAYER_COVER_PX,
            useFixedUrl = true,
            fixedUrl = cover,
        )
    }
    AnimatedVisibility(
        visible = transition.progress < AUTOMIX_LABEL_UNTIL,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
    ) {
        AutoMixLabel()
    }
}

/** A small infinity sign with light running along it, and the name. */
@Composable
private fun AutoMixLabel() {
    val run by rememberInfiniteTransition(label = "autoMixRun").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(AUTOMIX_RUN_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "autoMixRunAlong",
    )
    Row(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.38f), RoundedCornerShape(50))
            .padding(start = 10.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(Modifier.size(width = 20.dp, height = 10.dp)) {
            val path = Path()
            val midX = size.width / 2f
            val midY = size.height / 2f
            for (step in 0..AUTOMIX_INFINITY_STEPS) {
                val t = 2.0 * PI * step / AUTOMIX_INFINITY_STEPS
                val d = 1.0 + sin(t) * sin(t)
                val x = midX + (midX * cos(t) / d).toFloat()
                val y = midY + (size.height * sin(t) * cos(t) / d).toFloat()
                if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            val width = 1.5.dp.toPx()
            drawPath(path, Color.White.copy(alpha = 0.35f), style = Stroke(width))
            val measure = PathMeasure().apply { setPath(path, false) }
            val total = measure.length
            val start = run * total
            val end = start + total * 0.28f
            val lit = Path()
            measure.getSegment(start, min(end, total), lit, true)
            if (end > total) measure.getSegment(0f, end - total, lit, true)
            drawPath(lit, Color.White, style = Stroke(width, cap = StrokeCap.Round))
        }
        Text(
            text = stringResource(R.string.settings_automix),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
    }
}

/**
 * Layered sine waves folded through each other, which reads as slow liquid rather than a
 * ripple. Samples stay inside the image, so its edges never pull in transparency.
 */
private const val MELT_SHADER = """
uniform shader content;
uniform float2 size;
uniform float amount;
uniform float time;

float2 flow(float2 p, float t) {
    float a = sin(p.y * 5.1 + t + 1.8 * sin(p.x * 3.7 - t * 0.7));
    float b = sin(p.x * 4.3 - t * 0.8 + 1.8 * sin(p.y * 3.1 + t * 0.6));
    return float2(a, b);
}

half4 main(float2 coord) {
    float2 uv = coord / size;
    float2 bend = flow(uv, time) + 0.5 * flow(uv * 2.3 + 1.7, time * 1.3);
    float2 at = clamp(uv + bend * amount * 0.09, float2(0.0), float2(1.0));
    return content.eval(at * size);
}
"""

private const val AUTOMIX_SMOOTH_MS = 90
private const val AUTOMIX_VISUAL_FROM = 0.12f
private const val AUTOMIX_VISUAL_TO = 0.82f
private const val AUTOMIX_SCALE = 0.05f
private const val AUTOMIX_BLUR_DP = 26
private const val AUTOMIX_FLOW_SPEED = 5f
private const val AUTOMIX_LABEL_UNTIL = 0.92f
private const val AUTOMIX_RUN_MS = 1_800
private const val AUTOMIX_INFINITY_STEPS = 48

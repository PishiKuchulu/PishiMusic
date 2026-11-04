package com.tuned.app.ui.player

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.tuned.app.playback.PlayableTrack
import com.tuned.app.data.artwork.ArtworkNormalizer
import com.tuned.app.ui.common.ArtworkViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.intellij.lang.annotations.Language
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

@Language("AGSL")
internal val FLUID_AGSL = """
    uniform shader curArt;
    uniform shader prevArt;
    uniform float2 curArtSize;
    uniform float2 prevArtSize;
    uniform float fade;

    uniform float4 sides;
    uniform float4 angles;
    uniform float2 centre0;
    uniform float2 centre1;
    uniform float2 centre2;
    uniform float2 centre3;

    uniform float2 twistA;
    uniform float2 twistB;
    uniform float twistRadius;
    uniform float twistAngle;

    uniform float saturation;
    uniform float brightness;
    uniform float contrast;

    float2 twist(float2 coord, float2 offset, float radius, float angle) {
        float2 d = coord - offset;
        float dist = length(d);
        if (dist < radius) {
            float ratio = (radius - dist) / radius;
            float a = ratio * ratio * angle;
            float s = sin(a);
            float c = cos(a);
            d = float2(d.x * c - d.y * s, d.x * s + d.y * c);
        }
        return d + offset;
    }

    float2 localOf(float2 p, float2 centre, float angle, float side) {
        float2 d = p - centre;
        float c = cos(-angle);
        float s = sin(-angle);
        return float2(d.x * c - d.y * s, d.x * s + d.y * c) / side + 0.5;
    }

    bool covers(float2 uv) {
        return uv.x >= 0.0 && uv.x <= 1.0 && uv.y >= 0.0 && uv.y <= 1.0;
    }

    half4 main(float2 fragCoord) {
        float2 p = twist(fragCoord, twistA, twistRadius, twistAngle);
        p = twist(p, twistB, twistRadius, -twistAngle);

        float2 uv = clamp(localOf(p, centre0, angles.x, sides.x), 0.0, 1.0);
        float2 u1 = localOf(p, centre1, angles.y, sides.y);
        float2 u2 = localOf(p, centre2, angles.z, sides.z);
        float2 u3 = localOf(p, centre3, angles.w, sides.w);
        if (covers(u1)) uv = u1;
        if (covers(u2)) uv = u2;
        if (covers(u3)) uv = u3;

        float3 cur = float3(curArt.eval(uv * curArtSize).rgb);
        float3 prv = float3(prevArt.eval(uv * prevArtSize).rgb);
        float3 c = mix(prv, cur, fade);

        // Vibrance: dull colours get lifted most and vivid ones least, so a muted cover
        // gains colour without a bright one going neon.
        float mid = (c.r + c.g + c.b) / 3.0;
        float chroma = max(c.r, max(c.g, c.b)) - min(c.r, min(c.g, c.b));
        c = float3(mid) + (c - float3(mid)) * mix(saturation, 1.0 + (saturation - 1.0) * 0.45, clamp(chroma, 0.0, 1.0));
        c *= brightness;
        c = c * (1.0 + contrast) - float3(0.5 * contrast);
        return half4(half3(clamp(c, 0.0, 1.0)), 1.0);
    }
""".trimIndent()

internal data class FluidCopy(
    val centreX: Float,
    val centreY: Float,
    val side: Float,
    val angle: Float,
)

internal fun fluidCopies(width: Float, height: Float, seconds: Float, seed: Int): List<FluidCopy> {
    val longest = maxOf(width, height)
    val t = seconds * FLOW_SPEED
    val cx = width / 2f
    val cy = height / 2f
    val orbit = { rate: Float, radius: Float, phase: Float ->
        val a = rate * t + phase
        cx + width * radius * cos(a) to cy + width * radius * sin(a)
    }
    val (x3, y3) = orbit(ORBIT_RATE_MID, ORBIT_RADIUS_MID, startPhase(seed, 4))
    val (x4, y4) = orbit(ORBIT_RATE_SMALL, ORBIT_RADIUS_SMALL, startPhase(seed, 5))
    return listOf(
        FluidCopy(cx, cy, longest * SQRT_2, startPhase(seed, 0) + ROTATION[0] * t),
        FluidCopy(width / 2.5f, height / 2.5f, longest * 0.8f, startPhase(seed, 1) + ROTATION[1] * t),
        FluidCopy(x3, y3, longest * 0.5f, startPhase(seed, 2) + ROTATION[2] * t),
        FluidCopy(x4, y4, longest * 0.25f, startPhase(seed, 3) + ROTATION[3] * t),
    )
}

private fun startPhase(seed: Int, index: Int): Float =
    (((seed ushr (index * 5)) and 0x1F) / 32f) * TWO_PI

private const val TWO_PI = 6.2831855f
private val SQRT_2 = sqrt(2f)

private val ROTATION = floatArrayOf(0.06f, -0.12f, 0.06f, -0.08f)

private const val ORBIT_RADIUS_MID = 0.25f
private const val ORBIT_RADIUS_SMALL = 0.10f
private const val ORBIT_RATE_MID = 0.045f
private const val ORBIT_RATE_SMALL = 0.09f
private const val FLOW_SPEED = 2.5f

private const val SATURATION = 1.9f
private const val BRIGHTNESS = 0.52f
private const val LYRICS_BRIGHTNESS = 0.44f
private const val CONTRAST = 0.3f

/**
 * Average brightness the graded backdrop aims for, measured on Borderline (The Slow Rush) at
 * the base values above. A dark cover (Dracula's night photo) used to grade to black: the
 * contrast step subtracts 0.15, which wiped out everything under ~0.12 after the brightness
 * factor. Its factor is now raised until it reaches the same level; brighter covers keep the
 * base factor.
 */
private const val TARGET_LUMA = 0.11f
private const val LYRICS_TARGET_LUMA = 0.086f
/** The factor never goes past this multiple of the base, so a near-black cover stays dark. */
private const val MAX_BRIGHTNESS_GAIN = 2.5f

private const val TWIST_ANGLE = 1.6f
private const val TWIST_REACH = 1.0f

// Less blur keeps the cover's colours as separate pools instead of one muddy average.
private const val BLUR_FRACTION = 0.13f
private const val BLUR_SIGMA_MIN = 60f
private const val BLUR_SIGMA_MAX = 360f

private const val TEXTURE_PX = 256
private const val TEXTURE_CACHE = 3
private const val CROSSFADE_MS = 1400
private const val APPEAR_MS = 400

/** The last few fluid textures by cover (and blur mode), so reopening the player starts on them.
 * Byte-sized (was: 4 entries of unbounded size — a 256px texture is ~256 KB, but the
 * cache key admits any cover the loader returns). */
private val fluidTextureCache = object : android.util.LruCache<String, Bitmap>(4 * 1024 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
}
private const val FRAME_SECONDS = 1f / 30f

private val textureCache = LinkedHashMap<String, Bitmap>()

private suspend fun loadTexture(context: Context, url: String, preBlur: Boolean = false): Bitmap? = withContext(Dispatchers.IO) {
    val cacheKey = if (preBlur) "$url-preblurred" else url
    synchronized(textureCache) {
        textureCache.remove(cacheKey)?.also { textureCache[cacheKey] = it }
    }?.let { return@withContext it }

    val request = ImageRequest.Builder(context)
        .data(url)
        .size(TEXTURE_PX)
        .allowHardware(false)
        .build()
    // The app-wide Coil singleton, not a fresh ImageLoader per texture load.
    val loader = context.imageLoader
    val result = runCatching { loader.execute(request) }.getOrNull()
    var bitmap = ((result as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap ?: return@withContext null

    if (preBlur) {
        bitmap = FastBlur.blurArtwork(bitmap, radius = 18, maxDimension = 64)
    }

    synchronized(textureCache) {
        textureCache[cacheKey] = bitmap
        while (textureCache.size > TEXTURE_CACHE) {
            textureCache.remove(textureCache.keys.first())
        }
    }
    bitmap
}

private val blackAndWhiteCache = java.util.WeakHashMap<Bitmap, Boolean>()

/**
 * True when [bitmap] is essentially black and white: its mean chroma (spread
 * between the RGB channels, 0..1) is under 0.06. Unlike HSV saturation this
 * stays low for near-black pixels with a faint tint. Cached per bitmap.
 */
internal fun isBlackAndWhite(bitmap: Bitmap): Boolean = synchronized(blackAndWhiteCache) {
    blackAndWhiteCache.getOrPut(bitmap) {
        var sum = 0f
        var n = 0
        val step = maxOf(1, minOf(bitmap.width, bitmap.height) / 32)
        for (y in 0 until bitmap.height step step) for (x in 0 until bitmap.width step step) {
            val c = bitmap.getPixel(x, y)
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            sum += (maxOf(r, g, b) - minOf(r, g, b)) / 255f
            n++
        }
        n > 0 && sum / n < 0.06f
    }
}

private val brightnessCache = java.util.WeakHashMap<Bitmap, MutableMap<Float, Float>>()

/**
 * The brightness factor for [bitmap]: [base], or more when the cover is dark enough that the
 * shader's grade (saturation, brightness, contrast) would average under [target]. Found by
 * bisection on a 16x16 sample with the shader's own maths; cached per bitmap and base.
 */
internal fun backdropBrightness(bitmap: Bitmap, base: Float, target: Float): Float = synchronized(brightnessCache) {
    brightnessCache.getOrPut(bitmap) { mutableMapOf() }.getOrPut(base) {
        val sample = Bitmap.createScaledBitmap(bitmap, 16, 16, true)
        val pixels = IntArray(256).also { sample.getPixels(it, 0, 16, 0, 0, 16, 16) }
        if (sample !== bitmap) sample.recycle()
        val saturation = if (isBlackAndWhite(bitmap)) 0f else SATURATION
        fun graded(factor: Float): Float {
            var total = 0f
            for (c in pixels) {
                val r = ((c shr 16) and 0xFF) / 255f
                val g = ((c shr 8) and 0xFF) / 255f
                val b = (c and 0xFF) / 255f
                val mid = (r + g + b) / 3f
                val chroma = (maxOf(r, g, b) - minOf(r, g, b)).coerceIn(0f, 1f)
                // The same vibrance as the shader.
                val vivid = saturation + (1f + (saturation - 1f) * 0.45f - saturation) * chroma
                fun grade(x: Float) = ((mid + (x - mid) * vivid) * factor * (1f + CONTRAST) - 0.5f * CONTRAST).coerceIn(0f, 1f)
                total += 0.299f * grade(r) + 0.587f * grade(g) + 0.114f * grade(b)
            }
            return total / pixels.size
        }
        if (graded(base) >= target) return@getOrPut base
        var low = base
        var high = base * MAX_BRIGHTNESS_GAIN
        repeat(12) {
            val mid = (low + high) / 2f
            if (graded(mid) < target) low = mid else high = mid
        }
        high
    }
}

internal fun fluidBlurSigma(width: Float, height: Float, extraBlur: Boolean = false): Float =
    (minOf(width, height) * (if (extraBlur) 0.50f else BLUR_FRACTION)).coerceIn(
        if (extraBlur) 280f else BLUR_SIGMA_MIN,
        if (extraBlur) 720f else BLUR_SIGMA_MAX
    )

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class AgslFluidRenderer {
    private val shader = RuntimeShader(FLUID_AGSL)
    private val paint = Paint()

    fun render(
        canvas: android.graphics.Canvas,
        width: Float,
        height: Float,
        seconds: Float,
        seed: Int,
        current: Bitmap,
        previous: Bitmap,
        fade: Float,
        extraBlur: Boolean = false,
    ) {
        val copies = fluidCopies(width, height, seconds, seed)
        val curShader = BitmapShader(current, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val prevShader = BitmapShader(previous, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        shader.setInputShader("curArt", curShader)
        shader.setInputShader("prevArt", prevShader)
        shader.setFloatUniform("curArtSize", current.width.toFloat(), current.height.toFloat())
        shader.setFloatUniform("prevArtSize", previous.width.toFloat(), previous.height.toFloat())
        shader.setFloatUniform("fade", fade)
        shader.setFloatUniform("sides", copies[0].side, copies[1].side, copies[2].side, copies[3].side)
        shader.setFloatUniform("angles", copies[0].angle, copies[1].angle, copies[2].angle, copies[3].angle)
        shader.setFloatUniform("centre0", copies[0].centreX, copies[0].centreY)
        shader.setFloatUniform("centre1", copies[1].centreX, copies[1].centreY)
        shader.setFloatUniform("centre2", copies[2].centreX, copies[2].centreY)
        shader.setFloatUniform("centre3", copies[3].centreX, copies[3].centreY)
        shader.setFloatUniform("twistA", width * 0.25f, height)
        shader.setFloatUniform("twistB", width * 0.75f, 0f)
        shader.setFloatUniform("twistRadius", (maxOf(width, height) + minOf(width, height)) / 2f * TWIST_REACH)
        shader.setFloatUniform("twistAngle", TWIST_ANGLE)
        // Black-and-white covers stay grey: boosting saturation turned their
        // faint colour cast (a cool tint in a grey photo) into a strong one.
        shader.setFloatUniform("saturation", if (isBlackAndWhite(current)) 0f else SATURATION)
        // Per cover, so dark covers don't grade to black (see backdropBrightness).
        val base = if (extraBlur) LYRICS_BRIGHTNESS else BRIGHTNESS
        val target = if (extraBlur) LYRICS_TARGET_LUMA else TARGET_LUMA
        val curBrightness = backdropBrightness(current, base, target)
        val prevBrightness = backdropBrightness(previous, base, target)
        shader.setFloatUniform("brightness", prevBrightness + (curBrightness - prevBrightness) * fade)
        shader.setFloatUniform("contrast", CONTRAST)
        paint.shader = shader
        canvas.drawRect(0f, 0f, width, height, paint)
    }
}

/**
 * Checks whether this device can render the fluid background.
 * Requires:
 *  - Android 12+ (API >= 31) because RenderEffect and AGSL (API 33+) are unsupported on API <= 30
 *    (where Modifier.blur() is an explicit no-op in Jetpack Compose).
 *  - Hardware acceleration enabled on the current View.
 *  - Not a low-RAM device where background blurs are unsupported or disabled by the OS/GPU.
 */
@Composable
fun isFluidSupported(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return false
    }

    val context = LocalContext.current
    val view = LocalView.current

    if (view.isInEditMode) return false
    if (!view.isHardwareAccelerated) return false

    val activityManager = remember(context) {
        runCatching { context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager }.getOrNull()
    }
    if (activityManager?.isLowRamDevice == true) {
        return false
    }

    return true
}

@Composable
fun FluidArtworkBackground(
    track: PlayableTrack,
    coverUrl: String?,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    extraBlur: Boolean = false,
    rotatingBackgroundEnabled: Boolean = true,
    lyricsRotatingBackgroundEnabled: Boolean = true,
    artworkViewModel: ArtworkViewModel = hiltViewModel(),
    fallback: @Composable () -> Unit,
) {
    val fluidSupported = isFluidSupported()
    val isEnabled = if (extraBlur) lyricsRotatingBackgroundEnabled && rotatingBackgroundEnabled else rotatingBackgroundEnabled
    if (!isEnabled || !fluidSupported) {
        Box(modifier) {
            fallback()
        }
        return
    }

    val context = LocalContext.current
    // The player's pinned cover (square album art; see ArtworkNormalizer.coverFor).
    val artworkUrl = coverUrl

    val agslRenderer = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { AgslFluidRenderer() }.getOrNull()
        } else {
            null
        }
    }

    // Start from the texture loaded the last time this cover was shown. The player is rebuilt
    // on every open, and starting empty showed the lighter blurred fallback (a grey flash) until
    // the fluid background had loaded and faded in.
    val cacheKey = artworkUrl?.let { "$it|${agslRenderer == null}" }
    val cached = remember(cacheKey) { cacheKey?.let { fluidTextureCache.get(it) } }
    var current by remember { mutableStateOf<Bitmap?>(cached) }
    var previous by remember { mutableStateOf<Bitmap?>(null) }
    val crossfade = remember { Animatable(1f) }
    val appear = remember { Animatable(if (cached != null) 1f else 0f) }
    val seed = remember(track.videoId, track.title) {
        (track.videoId.hashCode() * 31) xor track.title.hashCode()
    }


    LaunchedEffect(artworkUrl, agslRenderer == null) {
        val url = artworkUrl?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        val bitmap = cacheKey?.let { fluidTextureCache.get(it) }
            ?: loadTexture(context, url, preBlur = (agslRenderer == null))?.also { loaded ->
                cacheKey?.let { fluidTextureCache.put(it, loaded) }
            }
            ?: return@LaunchedEffect
        val showing = current
        current = bitmap
        if (showing != null && showing !== bitmap) {
            previous = showing
            crossfade.snapTo(0f)
            crossfade.animateTo(1f, tween(CROSSFADE_MS))
            previous = null
        }
    }

    LaunchedEffect(current != null) {
        if (current != null) appear.animateTo(1f, tween(APPEAR_MS))
    }

    var seconds by remember { mutableFloatStateOf(0f) }
    val shouldPauseLoop = extraBlur && !lyricsRotatingBackgroundEnabled
    // The 30 fps render loop only runs while the track is playing: a paused
    // player used to keep redrawing the full-screen blur indefinitely.
    LaunchedEffect(!shouldPauseLoop, isPlaying) {
        if (shouldPauseLoop || !isPlaying) return@LaunchedEffect
        var last = 0L
        var pending = 0f
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) pending += ((now - last) / 1_000_000_000f).coerceIn(0f, 0.1f)
                last = now
            }
            if (pending >= FRAME_SECONDS) {
                seconds += pending
                pending = 0f
            }
        }
    }

    Box(modifier) {
        // While the moving background loads, show the dark page colour, not the blurred
        // fallback: that fallback is lighter than the finished background, so a fresh open
        // flashed grey and then dimmed for a second as the moving background faded in.
        if (appear.value < 1f || current == null) {
            Box(Modifier.fillMaxSize().background(Color(0xFF0B0B0C)))
        }

        if (current != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = appear.value
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val sigma = fluidBlurSigma(size.width, size.height, extraBlur)
                            // A haunted GPU driver must cost us the blur, never the process.
                            renderEffect = runCatching {
                                RenderEffect
                                    .createBlurEffect(sigma, sigma, Shader.TileMode.CLAMP)
                                    .asComposeRenderEffect()
                            }.getOrNull()
                        }
                    }
                    .drawBehind {
                        val cur = current ?: return@drawBehind
                        val prv = previous ?: cur

                        if (agslRenderer != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            drawIntoCanvas { canvas ->
                                agslRenderer.render(
                                    canvas = canvas.nativeCanvas,
                                    width = size.width,
                                    height = size.height,
                                    seconds = seconds,
                                    seed = seed,
                                    current = cur,
                                    previous = prv,
                                    fade = crossfade.value,
                                    extraBlur = extraBlur,
                                )
                            }
                        } else {
                            val copies = fluidCopies(size.width, size.height, seconds, seed)
                            val curImg = cur.asImageBitmap()
                            for (copy in copies) {
                                rotate(
                                    degrees = Math.toDegrees(copy.angle.toDouble()).toFloat(),
                                    pivot = Offset(copy.centreX, copy.copy(angle = copy.angle).centreY)
                                ) {
                                    drawImage(
                                        image = curImg,
                                        dstOffset = IntOffset(
                                            (copy.centreX - copy.side / 2f).toInt(),
                                            (copy.centreY - copy.side / 2f).toInt()
                                        ),
                                        dstSize = IntSize(copy.side.toInt(), copy.side.toInt())
                                    )
                                }
                            }
                        }
                    }
            )
            BackdropGrain(Modifier.fillMaxSize())
        }
    }
}

private val grainTile: Bitmap by lazy {
    val random = Random(7)
    val pixels = IntArray(GRAIN_TILE_PX * GRAIN_TILE_PX) {
        val shade = random.nextInt(256)
        (0xFF shl 24) or (shade shl 16) or (shade shl 8) or shade
    }
    Bitmap.createBitmap(pixels, GRAIN_TILE_PX, GRAIN_TILE_PX, Bitmap.Config.ARGB_8888)
}

/**
 * A faint fixed grain over the backdrop. Soft dark gradients band on 8-bit screens, and the
 * grain breaks the bands up; it has to sit on top because the blur would wipe it out.
 */
@Composable
private fun BackdropGrain(modifier: Modifier = Modifier) {
    val brush = remember {
        androidx.compose.ui.graphics.ShaderBrush(
            androidx.compose.ui.graphics.ImageShader(
                grainTile.asImageBitmap(),
                androidx.compose.ui.graphics.TileMode.Repeated,
                androidx.compose.ui.graphics.TileMode.Repeated,
            ),
        )
    }
    Box(
        modifier.drawBehind {
            drawRect(brush, alpha = GRAIN_STRENGTH, blendMode = androidx.compose.ui.graphics.BlendMode.Overlay)
        },
    )
}

private const val GRAIN_TILE_PX = 128
private const val GRAIN_STRENGTH = 0.07f

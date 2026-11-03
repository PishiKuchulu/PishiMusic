package com.tuned.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.LruCache
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.tuned.app.R
import java.io.File
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Draws what RemoteViews cannot, at device density and at the size each view
 * shows it, so the launcher never scales it:
 * - the card: one flat colour taken from the cover (see [coverHue]), or
 *   Tuned's surface when there is no cover;
 * - text in Google Sans Flex (the launcher can't load the app's font);
 * - the cover thumbnail with exact corners;
 * - the progress line.
 */
internal object WidgetRender {

    /** One AppType role: size in sp plus the variable-font axes. */
    data class TextRole(val sizeSp: Float, val weight: Int, val width: Float, val opsz: Float)

    // Bold Google Sans Flex, as the player uses it; slightly wide, never stretched.
    val TitleS = TextRole(15f, 700, 106f, 18f)
    val ArtistS = TextRole(13f, 500, 100f, 14f)
    val TitleM = TextRole(19f, 740, 108f, 22f)
    val ArtistM = TextRole(14f, 500, 100f, 16f)
    val TitleL = TextRole(22f, 740, 108f, 26f)
    val ArtistL = TextRole(15f, 500, 100f, 16f)
    val Message = TextRole(15f, 620, 104f, 18f)
    /** The reference look: a bold title over a lighter artist line. */
    val RefTitle = TextRole(18f, 720, 104f, 20f)
    val RefArtist = TextRole(15f, 480, 100f, 16f)


    private var baseTypeface: Typeface? = null
    private val textCache = LruCache<String, Bitmap>(24)

    private fun typeface(context: Context): Typeface? =
        baseTypeface ?: runCatching { ResourcesCompat.getFont(context, R.font.google_sans_flex) }
            .getOrNull()
            ?.also { baseTypeface = it }

    /**
     * [text] in up to [maxLines] lines within [maxWidthPx], cut only as a last resort:
     * first the full text at the role's size; then without a trailing "(…)", "[…]" or
     * " - …" part; then the same at 92% and 85% of the size. Only if none fits is the
     * shortest version ellipsized. The bitmap is only as wide as the text.
     */
    fun text(context: Context, text: String, role: TextRole, color: Int, maxWidthPx: Int, maxLines: Int = 1, centered: Boolean = false): Bitmap? = runCatching {
        if (maxWidthPx <= 0) return null
        val key = "$text|$role|$color|$maxWidthPx|$maxLines|$centered"
        textCache.get(key)?.takeIf { !it.isRecycled }?.let { return it }
        val metrics = context.resources.displayMetrics
        val baseSize = role.sizeSp * metrics.density * context.resources.configuration.fontScale
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            typeface = typeface(context)
            this.color = color
        }
        // Optional: if font axes aren't available the text still draws in the base face.
        runCatching { paint.fontVariationSettings = "'wght' ${role.weight}, 'wdth' ${role.width}, 'opsz' ${role.opsz}" }

        val versions = listOf(text, shortTitle(text)).distinct()
        var chosen: StaticLayout? = null
        outer@ for (factor in floatArrayOf(1f, 0.92f, 0.85f)) {
            paint.textSize = baseSize * factor
            for (version in versions) {
                val l = layout(version, paint, maxWidthPx, maxLines, centered, ellipsize = false)
                if (l.lineCount <= maxLines) { chosen = l; break@outer }
            }
        }
        val layout = chosen ?: run {
            paint.textSize = baseSize * 0.85f
            layout(versions.last(), paint, maxWidthPx, maxLines, centered, ellipsize = true)
        }
        var width = 1f
        for (i in 0 until layout.lineCount) width = max(width, layout.getLineWidth(i))
        val bitmapWidth = ceil(width).toInt().coerceIn(1, maxWidthPx)
        val bitmap = Bitmap.createBitmap(bitmapWidth, max(1, layout.height), Bitmap.Config.ARGB_8888)
        bitmap.density = metrics.densityDpi
        val canvas = Canvas(bitmap)
        // Centred lines were laid out across the full width; shift so the block fits the bitmap.
        if (centered && layout.width > bitmapWidth) canvas.translate(-(layout.width - bitmapWidth) / 2f, 0f)
        layout.draw(canvas)
        textCache.put(key, bitmap)
        bitmap
    }.getOrNull()

    /** Whether [text] fits on one line at [role]'s size (or slightly smaller) within [maxWidthPx]. */
    fun fitsOneLine(context: Context, text: String, role: TextRole, maxWidthPx: Int): Boolean = runCatching {
        val metrics = context.resources.displayMetrics
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = typeface(context) }
        runCatching { paint.fontVariationSettings = "'wght' ${role.weight}, 'wdth' ${role.width}, 'opsz' ${role.opsz}" }
        paint.textSize = role.sizeSp * metrics.density * context.resources.configuration.fontScale * 0.85f
        listOf(text, shortTitle(text)).any { paint.measureText(it) <= maxWidthPx }
    }.getOrDefault(true)

    /** The title without a trailing "(feat. …)", "[…]" or " - Remastered"-style part. */
    private fun shortTitle(text: String): String {
        val trimmed = text.replace(Regex("""\s*[(\[][^()\[\]]*[)\]]\s*$"""), "")
            .replace(Regex("""\s+-\s+[^-]+$"""), "")
            .trim()
        return trimmed.ifBlank { text }
    }

    private fun layout(text: String, paint: TextPaint, maxWidthPx: Int, maxLines: Int, centered: Boolean, ellipsize: Boolean): StaticLayout {
        val natural = ceil(paint.measureText(text)).toInt().coerceIn(1, maxWidthPx)
        val width = if (maxLines > 1 || centered) maxWidthPx else natural
        val builder = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(if (centered) android.text.Layout.Alignment.ALIGN_CENTER else android.text.Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .setBreakStrategy(android.graphics.text.LineBreaker.BREAK_STRATEGY_BALANCED)
        if (ellipsize) builder.setMaxLines(maxLines).setEllipsize(TextUtils.TruncateAt.END)
        return builder.build()
    }

    // ---- the cover source, kept in memory so a mid-write file never blanks the widget ----

    private var lastCoverKey: String? = null
    private var lastCover: Bitmap? = null

    /** The saved cover; the last good decode if the file is missing or being rewritten. */
    @Synchronized
    fun cover(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return lastCover
        val file = File(path)
        val key = "$path|${file.lastModified()}|${file.length()}"
        if (key == lastCoverKey && lastCover?.isRecycled == false) return lastCover
        val decoded = runCatching { if (file.exists()) BitmapFactory.decodeFile(path) else null }.getOrNull()
        if (decoded != null) {
            lastCover = decoded
            lastCoverKey = key
        }
        return decoded ?: lastCover
    }

    /** Forgets the cached cover (nothing has ever played, or data was cleared). */
    @Synchronized
    fun forgetCover() {
        lastCover = null
        lastCoverKey = null
    }

    // ---- card ----

    /**
     * A card in [fill] with a hairline in [line]. With [wash], a soft wash of that colour
     * from the top (22% fading to 4%), as on the home screen's header card.
     */
    fun plainCard(widthPx: Int, heightPx: Int, radiusPx: Float, fill: Int, line: Int, density: Float, wash: Int? = null): Bitmap? = runCatching {
        val out = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fill }
        val bounds = RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())
        canvas.drawRoundRect(bounds, radiusPx, radiusPx, paint)
        if (wash != null) {
            val top = (wash and 0x00FFFFFF) or (56 shl 24)
            val bottom = (wash and 0x00FFFFFF) or (10 shl 24)
            canvas.drawRoundRect(bounds, radiusPx, radiusPx, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = android.graphics.LinearGradient(0f, 0f, 0f, heightPx.toFloat(), top, bottom, android.graphics.Shader.TileMode.CLAMP)
            })
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = density
        paint.color = line
        val inset = density / 2f
        canvas.drawRoundRect(RectF(inset, inset, widthPx - inset, heightPx - inset), radiusPx - inset, radiusPx - inset, paint)
        out
    }.getOrNull()

    // ---- cover thumbnail ----

    /** [src] centre-cropped to a [sizePx] square with [radiusPx] corners. */
    fun thumb(src: Bitmap, sizePx: Int, radiusPx: Float): Bitmap? = runCatching {
        if (sizePx <= 0 || src.isRecycled) return null
        val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val scale = sizePx / min(src.width, src.height).toFloat()
        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate((sizePx - src.width * scale) / 2f, (sizePx - src.height * scale) / 2f)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply { setLocalMatrix(matrix) }
        }
        Canvas(out).drawRoundRect(RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()), radiusPx, radiusPx, paint)
        out
    }.getOrNull()

    // ---- the reference look: colour from the cover's hue ----

    /** Card colour, the light tint for secondary text, and the logo disc, all from one cover. */
    data class Hue(val card: Int, val tint: Int, val logo: Int)

    private val hueCache = LruCache<String, Hue>(8)

    /**
     * The cover's main hue by area (the swatch covering the most pixels among those
     * with real colour; a grey cover stays grey): a rich mid-dark card, a light tint
     * of the same hue for the artist line, and a pale disc for the flower.
     */
    fun coverHue(key: String, src: Bitmap): Hue = hueCache.get(key) ?: runCatching {
        val p = androidx.palette.graphics.Palette.from(src).maximumColorCount(24).generate()
        val coloured = p.swatches.filter { it.hsl[1] >= 0.18f && it.hsl[2] in 0.08f..0.92f }.maxByOrNull { it.population }
        // A cover with little real colour (mostly black, white or grey) gets a neutral card,
        // not a tint of whatever small coloured patch it has. Count actual pixels: the
        // palette's swatch populations overweight small saturated areas.
        if (coloured == null || colourfulShare(src) < 0.20f) {
            return@runCatching Hue(card = 0xFF1E1E21.toInt(), tint = 0xFFB4B4B8.toInt(), logo = 0xFFB4B4B8.toInt())
        }
        val hsl = coloured.hsl
        fun tone(sat: Float, light: Float) = androidx.core.graphics.ColorUtils.HSLToColor(
            floatArrayOf(hsl[0], hsl[1].coerceIn(sat * 0.6f, sat), light),
        )
        // A rich mid tone, bright enough to read as the cover's colour, dark enough for white text.
        Hue(card = tone(0.62f, 0.32f), tint = tone(0.75f, 0.80f), logo = tone(0.6f, 0.84f))
    }.getOrDefault(Hue(0xFF1C1C1E.toInt(), 0xFFA7A7A9.toInt(), 0xFFD0D0D0.toInt())).also { hueCache.put(key, it) }

    /** The share of the cover's pixels (sampled on a 32x32 grid) that carry real colour. */
    private fun colourfulShare(src: Bitmap): Float = runCatching {
        val small = Bitmap.createScaledBitmap(src, 32, 32, true)
        val hsl = FloatArray(3)
        var count = 0
        for (y in 0 until 32) for (x in 0 until 32) {
            androidx.core.graphics.ColorUtils.colorToHSL(small.getPixel(x, y), hsl)
            if (hsl[1] >= 0.25f && hsl[2] in 0.12f..0.90f) count++
        }
        if (small !== src) small.recycle()
        count / 1024f
    }.getOrDefault(1f)

    /**
     * The cover filling [widthPx] x [heightPx] (centre-cropped) with [radiusPx] corners,
     * darkened toward the bottom so light text there reads on any cover.
     */
    fun fullCover(src: Bitmap, widthPx: Int, heightPx: Int, radiusPx: Float): Bitmap? = runCatching {
        val out = coverRect(src, widthPx, heightPx, floatArrayOf(radiusPx, radiusPx, radiusPx, radiusPx, radiusPx, radiusPx, radiusPx, radiusPx))
        val canvas = Canvas(out)
        val path = android.graphics.Path().apply {
            addRoundRect(RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat()), radiusPx, radiusPx, android.graphics.Path.Direction.CW)
        }
        canvas.clipPath(path)
        val fade = Paint().apply {
            shader = android.graphics.LinearGradient(
                0f, heightPx * 0.45f, 0f, heightPx.toFloat(),
                android.graphics.Color.argb(0, 0, 0, 0), android.graphics.Color.argb(185, 0, 0, 0),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, heightPx * 0.45f, widthPx.toFloat(), heightPx.toFloat(), fade)
        out
    }.getOrNull()

    /** The cover filling the left of the Wide widget: rounded on the left, square on the right. */
    fun leftCover(src: Bitmap, widthPx: Int, heightPx: Int, radiusPx: Float): Bitmap? = runCatching {
        coverRect(src, widthPx, heightPx, floatArrayOf(radiusPx, radiusPx, 0f, 0f, 0f, 0f, radiusPx, radiusPx))
    }.getOrNull()

    private fun coverRect(src: Bitmap, widthPx: Int, heightPx: Int, radii: FloatArray): Bitmap {
        val out = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val scale = max(widthPx / src.width.toFloat(), heightPx / src.height.toFloat())
        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate((widthPx - src.width * scale) / 2f, (heightPx - src.height * scale) / 2f)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply { setLocalMatrix(matrix) }
        }
        val path = android.graphics.Path().apply {
            addRoundRect(RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat()), radii, android.graphics.Path.Direction.CW)
        }
        Canvas(out).drawPath(path, paint)
        return out
    }
}

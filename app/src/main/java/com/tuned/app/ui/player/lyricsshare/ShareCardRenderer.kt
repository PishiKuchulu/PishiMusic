package com.tuned.app.ui.player.lyricsshare

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.tuned.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

object ShareCardRenderer {

    const val BASE_WIDTH: Int = 1080

    const val LYRIC_LINE_SPACING_MULTIPLIER: Float = 1.06f
    const val LYRIC_BLOCK_GAP_FACTOR: Float = 0.34f

    const val BASE_LYRIC_FONT_SIZE: Float = 62f
    const val MIN_LYRIC_FONT_SIZE: Float = 40f

    const val BASE_TITLE_FONT_SIZE: Float = 36f
    const val BASE_ARTIST_FONT_SIZE: Float = 30f

    /**
     * Renders the card into an immutable software [Bitmap] with automatic OOM retry.
     */
    suspend fun renderCard(
        context: Context,
        spec: ShareCardSpec,
    ): Bitmap = withContext(Dispatchers.Default) {
        try {
            renderInternal(context, spec)
        } catch (oom: OutOfMemoryError) {
            System.gc()
            val retrySpec = spec.copy(scaleFactor = spec.scaleFactor * 0.75f)
            renderInternal(context, retrySpec)
        }
    }

    fun getTypeface(context: Context, weight: Int): Typeface {
        return try {
            val base = ResourcesCompat.getFont(context, R.font.google_sans_flex)
            if (base != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Typeface.create(base, weight, false)
            } else {
                base ?: Typeface.create(Typeface.DEFAULT, if (weight >= 600) Typeface.BOLD else Typeface.NORMAL)
            }
        } catch (_: Throwable) {
            Typeface.create(Typeface.DEFAULT, if (weight >= 600) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    fun wrapTextToWidth(
        text: CharSequence,
        paint: TextPaint,
        maxWidth: Int,
    ): CharSequence {
        if (maxWidth <= 0) return text
        val rawLines = text.split("\n")
        val result = mutableListOf<String>()

        for (rawLine in rawLines) {
            if (paint.measureText(rawLine) <= maxWidth) {
                result.add(rawLine)
            } else {
                val words = rawLine.split(" ")
                if (words.size <= 1) {
                    result.add(rawLine)
                } else {
                    var currentLine = StringBuilder()
                    for (word in words) {
                        val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
                        if (paint.measureText(candidate) <= maxWidth) {
                            currentLine = StringBuilder(candidate)
                        } else {
                            if (currentLine.isNotEmpty()) {
                                result.add(currentLine.toString())
                                currentLine = StringBuilder(word)
                            } else {
                                result.add(word)
                            }
                        }
                    }
                    if (currentLine.isNotEmpty()) {
                        result.add(currentLine.toString())
                    }
                }
            }
        }
        return result.joinToString("\n")
    }

    fun createStaticLayout(
        text: CharSequence,
        paint: TextPaint,
        width: Int,
        isRtl: Boolean,
        spacingMult: Float = LYRIC_LINE_SPACING_MULTIPLIER,
    ): StaticLayout {
        val alignment = if (isRtl) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL
        val safeWidth = max(width, 10)
        val wrappedText = wrapTextToWidth(text, paint, safeWidth)
        return try {
            StaticLayout.Builder.obtain(wrappedText, 0, wrappedText.length, paint, safeWidth).let { builder ->
                builder.setAlignment(alignment)
                    .setLineSpacing(0f, spacingMult)
                    .setIncludePad(true) // Crucial for Hindi/Devanagari matras, Arabic marks, and ascenders
                    .build()
            }
        } catch (_: Throwable) {
            @Suppress("DEPRECATION")
            StaticLayout(wrappedText, 0, wrappedText.length, paint, safeWidth, alignment, spacingMult, 0f, true)
        }
    }

    // ---- Lyric story card --------------------------------------------------------------------
    //
    // Always a 9:16 story. The blurred cover (or a deep gradient from the chosen colour) fills
    // it; one frosted panel sits a little above centre: the lyrics in big tight bold type, then
    // a deeper strip with the cover, title and artist, and the flower with "tuned" on the right.

    private data class Plan(
        val lyricLayouts: List<StaticLayout>,
        val lyricsHeight: Int,
        val blockGap: Float,
        val panelHeight: Int,
        val stripHeight: Int,
        val titleLayout: StaticLayout,
        val artistLayout: StaticLayout,
        val brandWidth: Float,
    )

    private class Metrics(scale: Float, width: Int) {
        val margin = (150 * scale).roundToInt()
        // Instagram and others cover the top and bottom of a story with their own UI.
        val safeY = (230 * scale).roundToInt()
        val pad = (48 * scale).roundToInt()
        val panelWidth = width - margin * 2
        val textWidth = panelWidth - pad * 2
        val radius = 44f * scale
        val thumb = (112 * scale).roundToInt()
        val thumbRadius = 12f * scale
        val stripPadY = (34 * scale).roundToInt()
        val gap = (22 * scale).roundToInt()
        val brandSize = 32f * scale
    }

    private fun renderInternal(
        context: Context,
        spec: ShareCardSpec,
    ): Bitmap {
        val scale = spec.scaleFactor
        val width = (spec.targetWidth * scale).roundToInt().coerceAtLeast(320)
        val height = (width * 16.0f / 9.0f).roundToInt()
        val m = Metrics(scale, width)

        val plan = plan(context, spec, m, scale, height - m.safeY * 2)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawBackground(canvas, spec, width, height)

        // Optically centred: a touch above the middle.
        val panelTop = ((height - plan.panelHeight) * 0.46f).coerceAtLeast(m.safeY.toFloat() * 0.5f)
        drawPanel(canvas, bitmap, context, spec, m, plan, panelTop, scale)
        return bitmap
    }

    private fun brandPaint(context: Context, size: Float, color: Int) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getTypeface(context, 800)
        textSize = size
        this.color = color
        // Optional: the unit tests' Android stub lacks font axes; the bold weight still applies.
        // Lighter and narrower than the welcome lockup: at strip size the heavy cut fills in.
        runCatching { fontVariationSettings = "'wght' 820, 'wdth' 108, 'opsz' 32, 'ROND' 0" }
    }

    /** The app's own Google Sans Flex axes (see AppType), so the card reads like Tuned. */
    private fun TextPaint.flex(weight: Int, width: Int, round: Int, opsz: Int) = apply {
        // Optional: the unit tests' Android stub lacks font axes; the weight still applies.
        runCatching { fontVariationSettings = "'wght' $weight, 'wdth' $width, 'ROND' $round, 'opsz' $opsz" }
    }

    private fun stripLayout(text: String, paint: TextPaint, width: Int, rtl: Boolean): StaticLayout {
        val align = if (rtl) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL
        val w = max(width, 10)
        return runCatching {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, w)
                .setAlignment(align)
                .setLineSpacing(0f, 1.04f)
                .setIncludePad(false)
                .setMaxLines(2)
                .setEllipsize(android.text.TextUtils.TruncateAt.END)
                .build()
        }.getOrElse { createStaticLayout(text, paint, w, rtl, 1.04f) }
    }

    private fun plan(context: Context, spec: ShareCardSpec, m: Metrics, scale: Float, maxPanel: Int): Plan {
        val bold = getTypeface(context, 760)
        // The same face as the lyrics screen (LyricsFace: Inter Bold).
        val lyricFace = runCatching { ResourcesCompat.getFont(context, R.font.inter_bold) }.getOrNull() ?: bold
        val medium = getTypeface(context, 500)
        val c = spec.derivedColors

        val brand = brandPaint(context, m.brandSize, c.brandTextColor)
        val brandWidth = m.brandSize + m.brandSize * 0.24f + brand.measureText("tuned")
        val stripTextWidth = (m.textWidth - m.thumb - m.gap - brandWidth - m.gap).roundToInt().coerceAtLeast(120)

        val titleLayout = stripLayout(
            spec.title,
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = bold; textSize = BASE_TITLE_FONT_SIZE * scale; color = lyricInk(c) }.flex(700, 112, 32, 22),
            stripTextWidth, spec.isRtl,
        )
        val artistLayout = stripLayout(
            spec.artist,
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = medium; textSize = BASE_ARTIST_FONT_SIZE * scale; color = artistInk(c) }.flex(500, 104, 20, 15),
            stripTextWidth, spec.isRtl,
        )
        val stripText = titleLayout.height + (4 * scale).roundToInt() + artistLayout.height
        val stripHeight = m.stripPadY * 2 + max(m.thumb, stripText)

        // Largest lyric size whose panel fits between the story's safe areas.
        var size = BASE_LYRIC_FONT_SIZE * scale
        val floor = MIN_LYRIC_FONT_SIZE * scale
        while (true) {
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = lyricFace
                textSize = size
                color = lyricInk(c)
                // The lyrics screen's slightly tight tracking (-0.5sp at 34sp).
                runCatching { letterSpacing = -0.015f }
            }
            val layouts = spec.lines.map { createStaticLayout(it.text, paint, m.textWidth, it.isRtl) }
            val blockGap = size * LYRIC_BLOCK_GAP_FACTOR
            val lyricsHeight = (layouts.sumOf { it.height.toDouble() } + (layouts.size - 1).coerceAtLeast(0) * blockGap).roundToInt()
            val panelHeight = m.pad + lyricsHeight + m.pad + stripHeight
            val plan = Plan(layouts, lyricsHeight, blockGap, panelHeight, stripHeight, titleLayout, artistLayout, brandWidth)
            if (panelHeight <= maxPanel || size <= floor) return plan
            size -= 2f * scale
        }
    }

    /** The artist line at about 62% white on dark backgrounds, as on Apple's strip. */
    private fun artistInk(c: DerivedCardColors) = if (c.isDarkBackground) AndroidColor.argb(158, 255, 255, 255) else c.secondaryTextColor

    /** Pure white on dark backgrounds: the derived tint turns greys pink. */
    private fun lyricInk(c: DerivedCardColors) = if (c.isDarkBackground) AndroidColor.WHITE else c.lyricsTextColor

    /**
     * The canvas, as on Apple's stickers: a smooth gradient of large soft colour fields. The
     * cover option takes them from the cover's main colours; a colour option turns that one
     * colour a little either way round the wheel. Light colours keep a plain light gradient
     * so dark text reads on them.
     */
    private fun drawBackground(canvas: Canvas, spec: ShareCardSpec, width: Int, height: Int) {
        val w = width.toFloat()
        val h = height.toFloat()
        val tones = when {
            spec.isCoverBackground -> coverTones(spec.coverBitmap)
            spec.derivedColors.isDarkBackground -> {
                val c = spec.swatchColor
                listOf(shiftHue(c, -18f), c, shiftHue(c, 22f)).map(::deepTone)
            }
            else -> null
        }
        if (tones == null) {
            val base = spec.swatchColor
            val top = blend(shiftHue(base, -14f), AndroidColor.WHITE, 0.14f)
            val bottom = blend(shiftHue(base, 18f), AndroidColor.BLACK, 0.16f)
            canvas.drawRect(0f, 0f, w, h, Paint().apply {
                shader = LinearGradient(0f, 0f, w * 0.4f, h, top, bottom, Shader.TileMode.CLAMP)
            })
            return
        }
        canvas.drawColor(blend(tones.last(), AndroidColor.BLACK, 0.55f))
        // Three soft fields: upper left, right, lower left.
        val fields = listOf(
            Triple(0.10f, 0.10f, 1.00f),
            Triple(1.00f, 0.48f, 0.90f),
            Triple(0.15f, 0.95f, 1.05f),
        )
        for ((i, f) in fields.withIndex()) {
            val c = tones[i % tones.size]
            canvas.drawRect(0f, 0f, w, h, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = android.graphics.RadialGradient(
                    w * f.first, h * f.second, w * f.third,
                    // Eased falloff, so no field shows a ring or a bright core.
                    intArrayOf(withAlpha(c, 235), withAlpha(c, 190), withAlpha(c, 110), withAlpha(c, 40), withAlpha(c, 0)),
                    floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f), Shader.TileMode.CLAMP,
                )
            })
        }
        // Deeper towards the bottom, so the strip always reads.
        canvas.drawRect(0f, 0f, w, h, Paint().apply {
            shader = LinearGradient(0f, h * 0.45f, 0f, h, AndroidColor.TRANSPARENT, AndroidColor.argb(110, 0, 0, 0), Shader.TileMode.CLAMP)
        })
    }

    /**
     * Up to three rich tones from the cover. A cover with little colour (black and white
     * photos) gets deep charcoals instead of muddy greys.
     */
    private fun coverTones(cover: Bitmap?): List<Int> {
        val mono = listOf(0xFF3A3A3F.toInt(), 0xFF26262A.toInt(), 0xFF2F2F34.toInt())
        if (cover == null || cover.isRecycled) return mono
        val palette = runCatching { androidx.palette.graphics.Palette.from(cover).maximumColorCount(16).generate() }.getOrNull() ?: return mono
        val swatches = palette.swatches.sortedByDescending { it.population }
        val total = swatches.sumOf { it.population }.coerceAtLeast(1)
        val hsv = FloatArray(3)
        val colourful = swatches.filter { AndroidColor.colorToHSV(it.rgb, hsv); hsv[1] > 0.22f && hsv[2] > 0.15f }
        if (colourful.sumOf { it.population } < total * 0.12f) return mono
        val picks = listOfNotNull(palette.vibrantSwatch, palette.darkVibrantSwatch, palette.mutedSwatch, palette.darkMutedSwatch)
            .map { it.rgb }
            .ifEmpty { colourful.map { it.rgb } }
            .distinct()
            .take(3)
        return (if (picks.size >= 3) picks else picks + picks.map { shiftHue(it, 20f) }).take(3).map(::deepTone)
    }

    /** Saturated and mid-dark, so white text always reads on it. Greys stay grey. */
    private fun deepTone(color: Int): Int {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(color, hsv)
        if (hsv[1] > 0.12f) hsv[1] = hsv[1].coerceIn(0.45f, 0.85f)
        hsv[2] = hsv[2].coerceIn(0.18f, 0.58f)
        return AndroidColor.HSVToColor(hsv)
    }

    private fun withAlpha(c: Int, a: Int) = AndroidColor.argb(a, AndroidColor.red(c), AndroidColor.green(c), AndroidColor.blue(c))

    private fun shiftHue(color: Int, degrees: Float): Int {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(color, hsv)
        if (hsv[1] < 0.08f) return color // greys have no hue to turn
        hsv[0] = (hsv[0] + degrees + 360f) % 360f
        return AndroidColor.HSVToColor(hsv)
    }

    /** A soft blur of [region] of [source]: scaled far down, then back up with filtering. */
    private fun blurredRegion(source: Bitmap, region: Rect, scale: Float): Bitmap? = runCatching {
        val part = Bitmap.createBitmap(source, region.left, region.top, region.width(), region.height())
        val factor = (14 * scale).coerceAtLeast(4f)
        val tiny = Bitmap.createScaledBitmap(part, (part.width / factor).roundToInt().coerceAtLeast(2), (part.height / factor).roundToInt().coerceAtLeast(2), true)
        val mid = Bitmap.createScaledBitmap(tiny, (part.width / 3).coerceAtLeast(2), (part.height / 3).coerceAtLeast(2), true)
        Bitmap.createScaledBitmap(mid, part.width, part.height, true)
    }.getOrNull()

    private fun drawPanel(canvas: Canvas, bitmap: Bitmap, context: Context, spec: ShareCardSpec, m: Metrics, plan: Plan, top: Float, scale: Float) {
        val c = spec.derivedColors
        val left = m.margin.toFloat()
        val right = left + m.panelWidth
        val bottom = top + plan.panelHeight
        val panel = RectF(left, top, right, bottom)
        val clip = android.graphics.Path().apply { addRoundRect(panel, m.radius, m.radius, android.graphics.Path.Direction.CW) }

        // Glass: the background behind the panel, blurred and lightened. No shadow: on colour
        // it reads as a smudge, and the frost alone lifts the panel.
        val region = Rect(left.roundToInt(), top.roundToInt().coerceAtLeast(0), right.roundToInt(), bottom.roundToInt().coerceAtMost(bitmap.height))
        val glass = blurredRegion(bitmap, region, scale)
        canvas.save()
        canvas.clipPath(clip)
        glass?.let { canvas.drawBitmap(it, region.left.toFloat(), region.top.toFloat(), Paint(Paint.FILTER_BITMAP_FLAG)) }
        canvas.drawColor(if (c.isDarkBackground) AndroidColor.argb(38, 255, 255, 255) else AndroidColor.argb(90, 255, 255, 255))
        // A faint sheen from the top edge.
        canvas.drawRect(panel, Paint().apply {
            shader = LinearGradient(0f, top, 0f, top + plan.panelHeight * 0.5f, AndroidColor.argb(26, 255, 255, 255), AndroidColor.TRANSPARENT, Shader.TileMode.CLAMP)
        })
        val stripTop = bottom - plan.stripHeight
        canvas.drawRect(left, stripTop, right, bottom, Paint().apply { color = AndroidColor.argb(if (c.isDarkBackground) 30 else 12, 0, 0, 0) })
        canvas.restore()

        val textLeft = left + m.pad
        var y = top + m.pad
        for ((i, layout) in plan.lyricLayouts.withIndex()) {
            canvas.save()
            canvas.translate(textLeft, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height + if (i < plan.lyricLayouts.lastIndex) plan.blockGap else 0f
        }

        // The strip: cover, title and artist centred on it, the brand at the far end.
        val midY = stripTop + plan.stripHeight / 2f
        drawCover(canvas, spec.coverBitmap, RectF(textLeft, midY - m.thumb / 2f, textLeft + m.thumb, midY + m.thumb / 2f), m.thumbRadius, c)
        val x = textLeft + m.thumb + m.gap
        val textBlock = plan.titleLayout.height + 4 * scale + plan.artistLayout.height
        var ty = midY - textBlock / 2f
        canvas.save(); canvas.translate(x, ty); plan.titleLayout.draw(canvas); canvas.restore()
        ty += plan.titleLayout.height + 4 * scale
        canvas.save(); canvas.translate(x, ty); plan.artistLayout.draw(canvas); canvas.restore()
        drawBrand(canvas, context, right - m.pad - plan.brandWidth, midY, m.brandSize, c.brandTextColor)
    }

    /** The flower beside lowercase "tuned", where Apple puts its logo, centred on [midY]. */
    private fun drawBrand(canvas: Canvas, context: Context, x: Float, midY: Float, size: Float, color: Int) {
        val paint = brandPaint(context, size, color)
        val cap = size * 0.72f
        val baseline = midY + cap / 2f
        val mark = size.roundToInt()
        drawLogoMark(canvas, context, x + mark / 2f, midY, mark, color)
        canvas.drawText("tuned", x + mark + size * 0.24f, baseline, paint)
    }

    private fun drawCover(canvas: Canvas, cover: Bitmap?, rect: RectF, radius: Float, c: DerivedCardColors) {
        if (cover != null && !cover.isRecycled) {
            val rounded = getRoundedBitmap(cover, radius, rect.width().roundToInt(), rect.height().roundToInt())
            canvas.drawBitmap(rounded, rect.left, rect.top, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        } else {
            canvas.drawRoundRect(rect, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.dividerColor })
        }
    }

    private fun blend(a: Int, b: Int, t: Float): Int = AndroidColor.rgb(
        (AndroidColor.red(a) + (AndroidColor.red(b) - AndroidColor.red(a)) * t).roundToInt(),
        (AndroidColor.green(a) + (AndroidColor.green(b) - AndroidColor.green(a)) * t).roundToInt(),
        (AndroidColor.blue(a) + (AndroidColor.blue(b) - AndroidColor.blue(a)) * t).roundToInt(),
    )

    private fun drawLogoMark(
        canvas: Canvas,
        context: Context,
        centerX: Float,
        centerY: Float,
        size: Int,
        tintColor: Int,
    ) {
        val drawable = runCatching {
            ContextCompat.getDrawable(context, R.drawable.ic_tuned_mark)
        }.getOrNull() ?: return
        val half = size / 2
        drawable.setBounds((centerX - half).toInt(), (centerY - half).toInt(), (centerX + half).toInt(), (centerY + half).toInt())
        drawable.colorFilter = PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
        drawable.draw(canvas)
    }

    private fun getRoundedBitmap(source: Bitmap, cornerRadius: Float, targetW: Int, targetH: Int): Bitmap {
        val output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = RectF(0f, 0f, targetW.toFloat(), targetH.toFloat())

        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        val srcRect = Rect(0, 0, source.width, source.height)
        val dstRect = Rect(0, 0, targetW, targetH)
        canvas.drawBitmap(source, srcRect, dstRect, paint)
        return output
    }
}

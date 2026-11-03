package com.tuned.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import android.view.View
import android.widget.RemoteViews
import com.tuned.app.R
import java.io.File

/**
 * Builds the four now-playing widgets ([WidgetStyle]), each a fixed size:
 * - Full cover (2x2): the cover edge to edge, title and artist on a fade.
 * - Small (2x2): cover top-left, the flower top-right, title and artist below.
 * - Medium (4x2): cover on the left; title, artist and controls centred on
 *   the right.
 * - Wide (4x2): the cover across the left edge to edge; title, artist, album
 *   and controls centred on the right.
 *
 * The card takes its colour from the cover's main hue (a rich mid-dark tone,
 * with a light tint of it for the artist line); with no cover it is Tuned's
 * surface in the user's theme ([WidgetPalette]). One rule: this NEVER throws.
 */
internal object WidgetViews {

    /**
     * The widget follows Tuned's own playback only. [usableSession] is a live
     * Tuned session; otherwise [hasLastTrack] shows the last song with Play
     * resuming it.
     */
    internal data class Resolved(
        val snapshot: WidgetSnapshot,
        val hasLastTrack: Boolean,
        val usableSession: Boolean,
    )

    internal fun resolve(context: Context): Resolved {
        val snapshot = WidgetSnapshot.read(context)
        val ownTrack = snapshot.sourcePackage.isEmpty() || snapshot.sourcePackage == context.packageName
        // The saved flag outlives a killed process; the in-process session token does not.
        val usable = snapshot.hasSession && ownTrack && ActiveMediaSessionHolder.ownToken != null
        return Resolved(snapshot, hasLastTrack = ownTrack && snapshot.title.isNotBlank(), usableSession = usable)
    }

    private fun layoutFor(style: WidgetStyle) = when (style) {
        WidgetStyle.FULL_COVER -> R.layout.widget_style_full_cover
        WidgetStyle.SMALL -> R.layout.widget_style_small
        WidgetStyle.MEDIUM -> R.layout.widget_style_medium
        WidgetStyle.WIDE -> R.layout.widget_style_wide
    }

    /** The widget's portrait size in dp (min width, max height), with a per-style fallback. */
    private fun sizeDp(context: Context, appWidgetId: Int, style: WidgetStyle): Pair<Int, Int> {
        val (w, h) = runCatching {
            val o = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
            o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) to o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        }.getOrDefault(0 to 0)
        val wide = style == WidgetStyle.MEDIUM || style == WidgetStyle.WIDE
        return (if (w > 0) w else if (wide) 360 else 170) to (if (h > 0) h else 180)
    }

    fun build(context: Context, appWidgetId: Int, style: WidgetStyle, progressOverride: Float? = null): RemoteViews {
        val views = RemoteViews(context.packageName, layoutFor(style))
        val (cellW, cellH) = sizeDp(context, appWidgetId, style)
        // The 2x2 widgets stay square; the 4x2 ones fill their whole cell, like the launcher's
        // own widgets. A fixed 2.1:1 card left a band empty on launchers with taller cells.
        val (w, h) = when (style) {
            WidgetStyle.FULL_COVER, WidgetStyle.SMALL -> minOf(cellW, cellH).let { it to it }
            WidgetStyle.MEDIUM, WidgetStyle.WIDE -> cellW to cellH
        }
        val d = context.resources.displayMetrics.density
        val padX = ((cellW - w) / 2f * d).toInt()
        val padY = ((cellH - h) / 2f * d).toInt()
        views.setViewPadding(R.id.widget_root, padX, padY, padX, padY)
        runCatching { bind(context, views, style, w, h, resolve(context), WidgetPalette.current(context)) }
        return views
    }

    // Byte-sized, not count-sized: 8 entries of a large widget bitmap can be
    // tens of MB. Cap the cache at ~8 MB of bitmap data instead.
    private val bitmapCache = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    private fun cached(key: String, make: () -> Bitmap?): Bitmap? =
        bitmapCache.get(key)?.takeIf { !it.isRecycled } ?: make()?.also { bitmapCache.put(key, it) }

    private fun bind(
        context: Context,
        views: RemoteViews,
        style: WidgetStyle,
        widthDp: Int,
        heightDp: Int,
        resolved: Resolved,
        palette: WidgetPalette,
    ) {
        val d = context.resources.displayMetrics.density
        fun px(dp: Float) = (dp * d).toInt()
        val cardRadius = 28f * d
        val wPx = px(widthDp.toFloat())
        val hPx = px(heightDp.toFloat())
        views.setOnClickPendingIntent(R.id.widget_root, WidgetActions.openAppPending(context))

        val snapshot = resolved.snapshot
        val live = resolved.usableSession
        if (!live && !resolved.hasLastTrack) {
            WidgetRender.forgetCover()
            WidgetRender.plainCard(wPx, hPx, cardRadius, palette.card, palette.line, d)?.let { views.setImageViewBitmap(R.id.widget_bg, it) }
            views.setViewVisibility(R.id.widget_empty_group, View.VISIBLE)
            views.setViewVisibility(R.id.widget_content_group, View.GONE)
            val message = context.getString(R.string.widget_empty)
            WidgetRender.text(context, message, WidgetRender.Message, palette.text1, px(widthDp - 32f), maxLines = 2)
                ?.let { views.setImageViewBitmap(R.id.widget_empty_text, it) }
            views.setContentDescription(R.id.widget_empty_text, message)
            return
        }
        views.setViewVisibility(R.id.widget_empty_group, View.GONE)
        views.setViewVisibility(R.id.widget_content_group, View.VISIBLE)

        val source = WidgetRender.cover(snapshot.artPath)
        val artKey = "${snapshot.artPath}|${File(snapshot.artPath.orEmpty()).lastModified()}"
        val hue = source?.let { WidgetRender.coverHue(artKey, it) }
        // Colours: from the cover's hue, or Tuned's theme when there is no cover.
        val card = hue?.card ?: palette.card
        val titleInk = if (hue != null) 0xFFFFFFFF.toInt() else palette.text1
        // On the full cover the artist line is soft white, so no tint fights the photo.
        val artistInk = if (style == WidgetStyle.FULL_COVER && hue != null) 0xDDFFFFFF.toInt() else hue?.tint ?: palette.text2
        val glyphInk = if (hue != null) 0xFFFFFFFF.toInt() else palette.text1

        // The card, and the cover where each style puts it.
        when (style) {
            WidgetStyle.FULL_COVER -> {
                val bg = source?.let { src -> cached("full|$artKey|$wPx|$hPx") { WidgetRender.fullCover(src, wPx, hPx, cardRadius) } }
                    ?: WidgetRender.plainCard(wPx, hPx, cardRadius, card, palette.line, d)
                bg?.let { views.setImageViewBitmap(R.id.widget_bg, it) }
            }
            // The cover's light tint washes down from the top, as on the home header.
            else -> WidgetRender.plainCard(wPx, hPx, cardRadius, card, card, d, wash = hue?.tint)?.let { views.setImageViewBitmap(R.id.widget_bg, it) }
        }
        val artW: Int
        val artH: Int
        val textDp: Float
        when (style) {
            WidgetStyle.FULL_COVER -> { artW = 0; artH = 0; textDp = widthDp - 32f }
            WidgetStyle.SMALL -> {
                // As big as fits above the two text lines.
                val side = minOf(widthDp - 32f, heightDp - 32f - 44f - 8f).coerceAtLeast(48f)
                artW = px(side); artH = artW
                textDp = widthDp - 32f
            }
            WidgetStyle.MEDIUM -> {
                // Square, but never so wide the text loses its half of the card.
                val side = minOf(heightDp - 32f, (widthDp - 16f) * 0.45f)
                artW = px(side); artH = artW
                textDp = widthDp - 16f - side - 32f
            }
            WidgetStyle.WIDE -> {
                // The cover fills the left edge to edge: square when the card is wide enough,
                // cropped narrower on a tall cell so the text keeps over half of the card.
                val coverDp = minOf(heightDp.toFloat(), widthDp * 0.45f)
                artW = px(coverDp); artH = hPx
                textDp = widthDp - coverDp - 32f
            }
        }
        if (artW > 0) {
            val art = when (style) {
                WidgetStyle.WIDE -> source?.let { src -> cached("left|$artKey|$artW|$artH") { WidgetRender.leftCover(src, artW, artH, cardRadius) } }
                // 12dp corners: concentric with the 28dp card at the 16dp inset.
                else -> source?.let { src -> cached("thumb|$artKey|$artW") { WidgetRender.thumb(src, artW, 12f * d) } }
            } ?: WidgetRender.plainCard(artW, artH, 12f * d, palette.line, palette.line, d)
            art?.let { views.setImageViewBitmap(R.id.widget_art, it) }
        }

        val title = snapshot.title.ifBlank { context.getString(R.string.widget_unknown_track) }
        val artist = snapshot.artist.ifBlank { context.getString(R.string.widget_unknown_artist) }
        val textPx = px(textDp.coerceAtLeast(40f))
        // Medium and Wide centre their text, and give long titles a second line.
        val centred = style == WidgetStyle.MEDIUM || style == WidgetStyle.WIDE
        // Full cover has room above its text, so long titles get a second line there too.
        // Two title lines on Medium and Full cover. Wide gets a second line only when the title
        // needs it, and then drops the album line so the block stays centred.
        val wideNeedsTwo = style == WidgetStyle.WIDE && !WidgetRender.fitsOneLine(context, title, WidgetRender.RefTitle, textPx)
        val titleLines = if (style == WidgetStyle.MEDIUM || style == WidgetStyle.FULL_COVER || wideNeedsTwo) 2 else 1
        WidgetRender.text(context, title, WidgetRender.RefTitle, titleInk, textPx, maxLines = titleLines, centered = centred)
            ?.let { views.setImageViewBitmap(R.id.widget_title, it) }
        WidgetRender.text(context, artist, WidgetRender.RefArtist, artistInk, textPx, centered = centred)
            ?.let { views.setImageViewBitmap(R.id.widget_artist, it) }
        views.setContentDescription(R.id.widget_title, title)
        views.setContentDescription(R.id.widget_artist, artist)
        if (style == WidgetStyle.WIDE) {
            // A single's album usually repeats the title; show it only when it adds something.
            val album = snapshot.album.takeUnless { it.trim().equals(title.trim(), ignoreCase = true) || wideNeedsTwo }.orEmpty()
            views.setViewVisibility(R.id.widget_album, if (album.isBlank()) View.GONE else View.VISIBLE)
            if (album.isNotBlank()) {
                WidgetRender.text(context, album, WidgetRender.RefArtist, artistInk, textPx, centered = true)?.let { views.setImageViewBitmap(R.id.widget_album, it) }
                views.setContentDescription(R.id.widget_album, album)
            }
        }


        // The flower appears on the Small widget only, in the artist line's tint.
        if (style == WidgetStyle.SMALL) views.setInt(R.id.widget_logo, "setColorFilter", artistInk)

        // Controls: plain white glyphs, Play-Pause larger between them.
        val playing = live && snapshot.isPlaying
        views.setImageViewResource(R.id.widget_play, if (playing) R.drawable.ic_widget_pause else R.drawable.ic_widget_play)
        views.setContentDescription(R.id.widget_play, context.getString(if (playing) R.string.widget_pause else R.string.widget_play))
        for (id in intArrayOf(R.id.widget_play, R.id.widget_prev, R.id.widget_next)) views.setInt(id, "setColorFilter", glyphInk)
        val playAction = if (live) {
            WidgetActions.togglePending(context, NowPlayingWidgetReceiver::class.java)
        } else {
            WidgetActions.resumePending(context)
        }
        views.setOnClickPendingIntent(R.id.widget_play, playAction)
        views.setOnClickPendingIntent(R.id.widget_next, WidgetActions.nextPending(context, NowPlayingWidgetReceiver::class.java))
        views.setOnClickPendingIntent(R.id.widget_prev, WidgetActions.prevPending(context, NowPlayingWidgetReceiver::class.java))
    }
}

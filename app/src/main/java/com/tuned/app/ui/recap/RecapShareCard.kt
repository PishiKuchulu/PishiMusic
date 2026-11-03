package com.tuned.app.ui.recap

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.FileProvider
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.tuned.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val CARD_WIDTH = 1080
private const val CARD_HEIGHT = 1920
private const val MARGIN = 90f

/** Draws the recap as a 9:16 story, coloured from the top song's cover, and opens the share sheet. */
internal suspend fun shareRecap(context: Context, state: RecapState) {
    val file = withContext(Dispatchers.IO) {
        val cover = state.topSongs.firstOrNull()?.artworkUrl?.let { loadCover(context, it) }
        val card = drawCard(context, state, cover)
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        File(dir, "tuned-recap-${state.year}.png").also { target ->
            target.outputStream().use { card.compress(Bitmap.CompressFormat.PNG, 100, it) }
            card.recycle()
        }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, null))
}

private suspend fun loadCover(context: Context, url: String): Bitmap? {
    val request = ImageRequest.Builder(context).data(url).size(720).allowHardware(false).build()
    // The app's single Coil loader (with its memory/disk caches), not a
    // fresh ImageLoader per share — a new instance bypassed both caches
    // and re-downloaded the cover every time.
    val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
    return ((result as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
}

private fun drawCard(context: Context, state: RecapState, cover: Bitmap?): Bitmap {
    val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val tint = cover?.let { Palette.from(it).generate().getDarkVibrantColor(Color.rgb(32, 32, 36)) } ?: Color.rgb(32, 32, 36)
    canvas.drawRect(
        0f,
        0f,
        CARD_WIDTH.toFloat(),
        CARD_HEIGHT.toFloat(),
        Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, CARD_HEIGHT.toFloat(), tint, Color.rgb(11, 11, 12), Shader.TileMode.CLAMP)
        },
    )
    val heavy = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    fun paint(size: Float, alpha: Int = 255, face: Typeface = heavy) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        this.alpha = alpha
        textSize = size
        typeface = face
    }
    fun fitted(text: String, p: Paint): Int = p.breakText(text, true, CARD_WIDTH - MARGIN * 2, null)
    var y = 170f
    canvas.drawText(context.getString(R.string.recap_title, state.year), MARGIN, y, paint(56f))
    val minutes = String.format(java.util.Locale.getDefault(), "%,d", state.minutes)
    val number = paint(minOf(300f, (CARD_WIDTH - MARGIN * 2) / (minutes.length * 0.62f)))
    y += number.textSize + 20f
    canvas.drawText(minutes, MARGIN, y, number)
    y += 72f
    canvas.drawText(context.getString(R.string.recap_minutes_label), MARGIN, y, paint(50f, 220, Typeface.DEFAULT))
    val song = state.topSongs.firstOrNull()
    if (song != null) {
        y += 90f
        canvas.drawText(context.getString(R.string.recap_top_song), MARGIN, y, paint(40f, 190, Typeface.DEFAULT))
        y += 36f
        if (cover != null) {
            val size = 560f
            val target = RectF(MARGIN, y, MARGIN + size, y + size)
            val rounded = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = android.graphics.BitmapShader(cover, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).also {
                    it.setLocalMatrix(android.graphics.Matrix().apply {
                        setRectToRect(RectF(Rect(0, 0, cover.width, cover.height)), target, android.graphics.Matrix.ScaleToFit.FILL)
                    })
                }
            }
            canvas.drawRoundRect(target, 44f, 44f, rounded)
            y += size
        }
        y += 76f
        val title = paint(58f)
        canvas.drawText(song.title, 0, fitted(song.title, title), MARGIN, y, title)
        y += 58f
        val by = paint(42f, 200, Typeface.DEFAULT)
        canvas.drawText(song.artist, 0, fitted(song.artist, by), MARGIN, y, by)
    }
    state.topArtists.firstOrNull()?.let { artist ->
        y += 96f
        canvas.drawText(context.getString(R.string.recap_top_artist), MARGIN, y, paint(40f, 190, Typeface.DEFAULT))
        y += 66f
        val name = paint(58f)
        canvas.drawText(artist.name, 0, fitted(artist.name, name), MARGIN, y, name)
    }
    val mark = paint(46f)
    canvas.drawText("tuned", CARD_WIDTH - MARGIN - mark.measureText("tuned"), CARD_HEIGHT - 90f, mark)
    return bitmap
}

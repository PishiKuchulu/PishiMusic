package com.tuned.app.ui.feed

import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.compose.AsyncImagePainter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import android.graphics.drawable.BitmapDrawable
import com.tuned.app.data.music.YouTubeMusicTrack
import com.tuned.app.util.ArtistHelper
import com.tuned.app.ui.common.PlayingIndicator
import com.tuned.app.ui.common.pressScale
import com.tuned.app.ui.theme.AppShape
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.tunedColors

/**
 * A tall Quick picks card in the style of Apple Music's tiles. The
 * artwork sits at the top at its real proportions; below it the artwork's bottom rows are stretched
 * down to the end of the card and blurred, so the cover carries on into the
 * card instead of stopping at a hard edge. Title and artist sit on that
 * extension.
 *
 * The extension stretches the last rows of the image (an edge extend), it
 * does not mirror the artwork.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun QuickPickCard(
    track: YouTubeMusicTrack,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.tunedColors
    val context = LocalContext.current
    // Software bitmap so the cover's bottom edge can be read for the text
    // colour — but downsampled to the card's real size: a full-resolution
    // software decode per card was the most expensive image load on Home.
    val targetPx = with(androidx.compose.ui.platform.LocalDensity.current) { Sizes.tileTallWidth.roundToPx() }
    val request = remember(track.artworkUrl, targetPx) {
        ImageRequest.Builder(context).data(track.artworkUrl).allowHardware(false).size(targetPx).build()
    }
    val painter = rememberAsyncImagePainter(request)
    // No shade over the cover: the text colour follows the cover instead,
    // dark on light covers (white, cream), white on everything else.
    val painterState = painter.state
    // The pixel loop runs off the main thread; it used to execute inside
    // remember{} during composition.
    val lightBottom by androidx.compose.runtime.produceState(false, painterState) {
        val bitmap = ((painterState as? AsyncImagePainter.State.Success)?.result?.drawable as? BitmapDrawable)?.bitmap
        value = bitmap != null && kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            bottomEdgeLuminance(bitmap) > LIGHT_EDGE
        }
    }
    val loaded = painterState is AsyncImagePainter.State.Success
    val textColor = when {
        !loaded -> colors.text1
        lightBottom -> Color.Black.copy(alpha = 0.87f)
        else -> Color.White
    }
    val subTextColor = when {
        !loaded -> colors.text2
        lightBottom -> Color.Black.copy(alpha = 0.6f)
        else -> Color.White.copy(alpha = 0.75f)
    }
    val interaction = remember { MutableInteractionSource() }
    val artist = remember(track.artist) { ArtistHelper.primaryArtist(track.artist) }
    // The artwork keeps its real proportions: a square cover fills a square,
    // a 16:9 video thumbnail shows whole at 16:9. Portrait images (rare) are
    // cropped to a square. Square until the image has loaded.
    val intrinsic = painter.intrinsicSize
    val artRatio = if (intrinsic.isSpecified && intrinsic.height > 0f) (intrinsic.width / intrinsic.height).coerceAtLeast(1f) else 1f
    Box(
        modifier
            .width(Sizes.tileTallWidth)
            .height(Sizes.tileTallHeight)
            .pressScale(interaction)
            .clip(AppShape.md)
            .background(colors.surface2)
            .border(Sizes.hairline, Color.White.copy(alpha = 0.12f), AppShape.md)
            .combinedClickable(interactionSource = interaction, indication = null, onClick = onClick, onLongClick = onLongClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "${track.title}, $artist"
                role = Role.Button
            },
    ) {
        // The extension: the artwork's bottom strip stretched to the card's
        // end, then blurred. Drawn with the same centre crop as the sharp
        // artwork above so the two meet without a seam.
        Canvas(
            Modifier
                .fillMaxSize()
                .blur(Space.xl, BlurredEdgeTreatment.Rectangle),
        ) {
            val width = size.width
            val artHeight = width / artRatio
            // Same placement as the sharp image: full width, real height
            // (portrait images centre-cropped to a square).
            val rawRatio = if (intrinsic.isSpecified && intrinsic.height > 0f) intrinsic.width / intrinsic.height else 1f
            val drawSize = Size(width, width / rawRatio)
            val cropOffset = Offset(0f, (artHeight - drawSize.height) / 2f)
            val strip = artHeight * STRIP_FRACTION
            val stripTop = artHeight - strip
            // The whole cover goes into the blurred layer too, so the blur only
            // mixes the cover's own colours; blurring the strip alone pulled the
            // card's dark background in and left a grey band where they meet.
            clipRect(bottom = artHeight) {
                translate(cropOffset.x, cropOffset.y) {
                    with(painter) { draw(drawSize) }
                }
            }
            clipRect(top = stripTop) {
                withTransform({ scale(1f, (size.height - stripTop) / strip, pivot = Offset(0f, stripTop)) }) {
                    clipRect(bottom = artHeight) {
                        translate(cropOffset.x, cropOffset.y) {
                            with(painter) { draw(drawSize) }
                        }
                    }
                }
            }
        }
        // The sharp artwork. Its lower quarter fades out so it melts
        // into the extension instead of ending on a line.
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(artRatio)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.verticalGradient(0.72f to Color.Black, 1f to Color.Transparent),
                        blendMode = BlendMode.DstIn,
                    )
                },
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.xxs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                if (isCurrent) PlayingIndicator(playing = isPlaying, color = textColor, modifier = Modifier.size(Sizes.iconInline))
                Text(track.title, style = AppType.bodyStrong, color = textColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(artist, style = AppType.bodyS, color = subTextColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Bottom-edge luminance above which the card switches to dark text. */
private const val LIGHT_EDGE = 0.5f

/** Average luminance of the bottom 6% of [bitmap], sampled every few pixels. */
private fun bottomEdgeLuminance(bitmap: android.graphics.Bitmap): Float {
    val w = bitmap.width
    val h = bitmap.height
    if (w < 4 || h < 4) return 0f
    var sum = 0f
    var n = 0
    for (y in (h * 0.94f).toInt() until h step 2) for (x in 0 until w step 4) {
        sum += Color(bitmap.getPixel(x, y)).luminance()
        n++
    }
    return if (n == 0) 0f else sum / n
}

/** Share of the artwork's height, from the bottom, that is stretched into the extension. */
private const val STRIP_FRACTION = 0.06f

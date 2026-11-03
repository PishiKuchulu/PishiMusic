/*
 * FeaturedCard: big square cover with gradient scrim, title+artist at top,
 * 3-dot menu top-right, optional italic caption at the bottom.
 * Used for YTM's big featured cards ("Your daily discover").
 */
package com.tuned.app.ui.feed.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuned.app.ui.common.ArtworkImage

/**
 * Large featured square card: full-bleed cover, dark gradient scrim at the
 * bottom, title + artist overlaid at the top, a 3-dot affordance at the top
 * right, and an optional italic caption (YTM "Because you liked X") at the
 * bottom. Sized to snap in a horizontal pager-style row.
 */
@Composable
fun ShelfFeaturedCard(
    title: String,
    artist: String,
    artworkUrl: String?,
    caption: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
    fallbackIcon: ImageVector = Icons.Filled.MusicNote,
    cardWidth: Dp = com.tuned.app.ui.theme.Sizes.tileSquare,
) {
    com.tuned.app.ui.common.MediaTile(
        title = title,
        // The caption (e.g. "Because you liked X") used to be accepted and
        // dropped; it rides along as a second subtitle line.
        subtitle = listOfNotNull(
            artist.takeIf { it.isNotBlank() },
            caption?.takeIf { it.isNotBlank() },
        ).joinToString(" · ").takeIf { it.isNotBlank() },
        artworkUrl = artworkUrl,
        onClick = onClick,
        onLongClick = onLongClick ?: onMenuClick,
        modifier = modifier,
        width = cardWidth,
    )
}

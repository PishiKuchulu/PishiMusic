/*
 * SquareCard: square cover + title + info line. Default native shelf card.
 */
package com.tuned.app.ui.feed.cards

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tuned.app.R

/**
 * Square cover + title (1-2 lines) + info line. The default native shelf card —
 * used for songs, albums and playlists when no richer shape is provided.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShelfSquareCard(
    title: String,
    infoLine: String,
    artworkUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onPlayClick: (() -> Unit)? = null,
    fallbackIcon: ImageVector = Icons.Filled.MusicNote,
    cardWidth: Dp = com.tuned.app.ui.theme.Sizes.tileSquare,
) {
    // Same tile as every other shelf on Home.
    com.tuned.app.ui.common.MediaTile(
        title = title,
        subtitle = infoLine.takeIf { it.isNotBlank() },
        artworkUrl = artworkUrl,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        width = cardWidth,
        onPlayClick = onPlayClick,
    )
}

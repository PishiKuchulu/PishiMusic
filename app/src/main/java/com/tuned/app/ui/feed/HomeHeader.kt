package com.tuned.app.ui.feed

import androidx.compose.material.icons.filled.Settings
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.animation.Crossfade
import android.os.Build
import com.tuned.app.ui.theme.rememberMotion
import com.tuned.app.ui.theme.Radius
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.snap
import androidx.compose.animation.animateColorAsState
import com.tuned.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.tuned.app.ui.common.AppIconButton
import com.tuned.app.ui.common.ArtworkTintBackdrop
import com.tuned.app.ui.common.Artwork
import com.tuned.app.ui.common.PlayButton
import com.tuned.app.ui.theme.AppShape
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.Keyline
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.iconEdgeInset
import com.tuned.app.ui.theme.tunedColors

/**
 * Home's header: the page title ("Home"), an optional line under it, and
 * three actions whose glyphs end on the right margin: new mix, downloads,
 * and settings.
 */
@Composable
fun HomeHeader(
    greeting: String,
    date: String,
    onOpenPlayground: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.tunedColors
    Row(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(start = Keyline.margin, end = iconEdgeInset, top = Space.lg, bottom = Space.md)
            .height(Sizes.touch),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
            Text(
                greeting,
                style = AppType.display,
                color = colors.text1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            if (date.isNotBlank()) Text(date, style = AppType.bodyS, color = colors.text2)
        }
        AppIconButton(Icons.Filled.AutoAwesome, stringResource(R.string.playground_title), onClick = onOpenPlayground)
        AppIconButton(Icons.Filled.Download, "Downloads", onClick = onOpenDownloads)
        AppIconButton(Icons.Filled.Settings, "Settings", onClick = onOpenSettings)
    }
}

/**
 * The panel behind Home's title and chips. It takes its colour from the
 * accent, which follows the playing track's artwork, so the top of Home
 * changes with the music: a vertical wash of that colour, a soft light from
 * the top-right corner behind the settings button, and 28dp bottom corners. When the
 * track changes, the colour fades to the new one over 700ms.
 */
@Composable
fun HomeHeaderPanel(
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.tunedColors
    Box(
        modifier
            .fillMaxWidth()
            .clip(HeaderPanelShape)
            .background(colors.bg),
    ) {
        ArtworkTintBackdrop(artwork = artworkUrl, tint = colors.accent, modifier = Modifier.matchParentSize())
        Column(Modifier.fillMaxWidth(), content = content)
    }
}

private val HeaderPanelShape = RoundedCornerShape(bottomStart = Radius.xl, bottomEnd = Radius.xl)

/**
 * The Infinite Radio card: artwork from your quick picks fills a 200dp card
 * with 28dp corners; a dark fade from the bottom keeps the text readable; the
 * accent play button sits on the right edge of the text block.
 */
/** A section on Home: no card behind it, so content sits on the screen margin. */
@Composable
fun HomeSectionContainer(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxWidth()) { content() }
}

@Composable
fun InfiniteRadioCard(
    artworkUrl: String?,
    enabled: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FeatureCard(
        eyebrow = stringResource(R.string.feed_radio_eyebrow),
        title = stringResource(R.string.feed_radio_title),
        subtitle = stringResource(R.string.feed_radio_sub),
        artworkUrl = artworkUrl,
        onPlay = if (enabled) onPlay else null,
        modifier = modifier,
    )
}

/**
 * The one large card style on Home (Infinite Radio, the artist spotlight):
 * full-bleed artwork, a fade that keeps the text readable, an accent eyebrow,
 * a display title, and the accent play button on the right.
 */
@Composable
fun FeatureCard(
    eyebrow: String,
    title: String,
    subtitle: String?,
    artworkUrl: String?,
    onPlay: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.tunedColors
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = Keyline.margin)
            .height(Sizes.heroHeight)
            .clip(AppShape.xl)
            .background(colors.surface2)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        if (!artworkUrl.isNullOrBlank()) {
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
                Artwork(url = artworkUrl, size = maxWidth, height = maxHeight, shape = AppShape.xl)
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to colors.bg.copy(alpha = 0.05f),
                        0.45f to colors.bg.copy(alpha = 0.35f),
                        1f to colors.bg.copy(alpha = 0.92f),
                    ),
                ),
        )
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = Space.xl, end = Space.lg, bottom = Space.lg),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(eyebrow, style = AppType.caption, color = colors.accent)
                Text(
                    title,
                    style = AppType.display,
                    color = colors.text1,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, style = AppType.bodyS, color = colors.text2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.size(Space.md))
            // Always rendered: removing the button when radio is disabled
            // shifted the whole text block. Disabled state explains itself.
            PlayButton(playing = false, onClick = onPlay ?: {}, enabled = onPlay != null)
        }
    }
}

@Preview(widthDp = 360, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeHeaderPreview() = com.tuned.app.ui.common.PreviewTheme {
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        HomeHeader("Home", "", {}, {}, {})
        InfiniteRadioCard(artworkUrl = null, enabled = true, onPlay = {})
    }
}

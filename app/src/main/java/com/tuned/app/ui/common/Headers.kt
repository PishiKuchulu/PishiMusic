package com.tuned.app.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.tuned.app.R
import com.tuned.app.ui.theme.AppShape
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.Keyline
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.WidthClass
import com.tuned.app.ui.theme.currentWidthClass
import com.tuned.app.ui.theme.iconEdgeInset
import com.tuned.app.ui.theme.rememberMotion
import com.tuned.app.ui.theme.tunedColors

/**
 * Section title on the screen margin with an optional text action whose
 * label ends on the right margin. 32dp above and 12dp below by default.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(top = Space.xxl, bottom = Space.md),
) {
    val colors = MaterialTheme.tunedColors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding)
            .padding(start = Keyline.margin, end = if (actionLabel != null) Keyline.margin - Space.md else Keyline.margin),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = AppType.titleL,
            color = colors.text1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (actionLabel != null && onAction != null) {
            Box(
                Modifier
                    .heightIn(min = Sizes.touch)
                    .clickable(onClick = onAction)
                    .padding(horizontal = Space.md),
                contentAlignment = Alignment.Center,
            ) {
                Text(actionLabel, style = AppType.label, color = colors.accent)
            }
        }
    }
}

/** True once the large [ScreenTitle] (the list's first item) has scrolled under the bar. */
@Composable
fun rememberTitleCollapsed(listState: LazyListState): Boolean {
    val collapsed by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }
    return collapsed
}

/**
 * 64dp top bar. The small title fades in once [collapsed] (the large
 * [ScreenTitle] has scrolled away), and a hairline appears under the bar.
 * Icon glyphs on both ends sit on the screen margin.
 */
@Composable
fun ScreenTopBar(
    title: String,
    collapsed: Boolean,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = MaterialTheme.tunedColors
    val motion = rememberMotion()
    val titleAlpha by animateFloatAsState(if (collapsed) 1f else 0f, motion.fast(), label = "barTitle")
    Column(modifier.fillMaxWidth().barScrim(visible = collapsed)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(Sizes.topBar)
                .padding(start = if (onBack != null) iconEdgeInset else Keyline.margin, end = iconEdgeInset),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                AppIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cmp_back), onClick = onBack)
                Spacer(Modifier.width(Space.xs))
            }
            Text(
                title,
                style = AppType.title,
                color = colors.text1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // Invisible (alpha 0) until collapsed, but present in the
                // semantics tree the whole time — TalkBack announced the
                // title twice (bar + large title). Clear it while hidden.
                modifier = Modifier
                    .weight(1f)
                    .alpha(titleAlpha)
                    .then(if (collapsed) Modifier else Modifier.clearAndSetSemantics {}),
            )
            actions()
        }
    }
}

/** Large title, the first item of a screen's list. Collapses into [ScreenTopBar]. */
@Composable
fun ScreenTitle(title: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    val colors = MaterialTheme.tunedColors
    Column(
        modifier
            .fillMaxWidth()
            .padding(start = Keyline.margin, end = Keyline.margin, top = Space.sm, bottom = Space.lg),
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Text(title, style = AppType.headline, color = colors.text1, modifier = Modifier.semantics { heading() })
        if (subtitle != null) Text(subtitle, style = AppType.mono, color = colors.text2)
    }
}

/**
 * Header for an album or playlist. Artwork centred (200dp, 240dp on wider
 * screens); title and metadata on the margin; secondary actions on the left
 * with glyphs on the margin, and the play button ending on the right margin.
 */
@Composable
fun CollectionHeader(
    title: String,
    meta: String,
    playing: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    artworkUrl: String? = null,
    subtitle: String? = null,
    onShuffle: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    onMore: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.tunedColors
    val artSize = if (currentWidthClass() == WidthClass.Compact) Sizes.collectionArt else Sizes.collectionArtMedium
    Column(modifier.fillMaxWidth().padding(top = Space.sm)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Artwork(url = artworkUrl, size = artSize, shape = AppShape.lg)
        }
        Spacer(Modifier.height(Space.xl))
        Column(Modifier.padding(horizontal = Keyline.margin), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(title, style = AppType.headline, color = colors.text1, modifier = Modifier.semantics { heading() })
            if (subtitle != null) Text(subtitle, style = AppType.body, color = colors.text2)
            Text(meta, style = AppType.mono, color = colors.text2)
        }
        Spacer(Modifier.height(Space.md))
        Row(
            Modifier.fillMaxWidth().padding(start = iconEdgeInset, end = Keyline.margin),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onShuffle != null) AppIconButton(Icons.Filled.Shuffle, stringResource(R.string.cmp_shuffle), onClick = onShuffle, tint = colors.text2)
            if (onDownload != null) AppIconButton(Icons.Filled.Download, stringResource(R.string.cmp_download), onClick = onDownload, tint = colors.text2)
            if (onMore != null) AppIconButton(Icons.Filled.MoreVert, stringResource(R.string.cmp_more_options), onClick = onMore, tint = colors.text2)
            Spacer(Modifier.weight(1f))
            PlayButton(playing = playing, onClick = onPlay)
        }
    }
}

@ComponentPreviews
@Composable
private fun HeadersPreview() = PreviewTheme { HeadersSample() }

/** Sample states, shared by the preview and the in-app component gallery. */
@Composable
internal fun HeadersSample() {
    Column {
        ScreenTopBar(title = "Library", collapsed = false, actions = {
            AppIconButton(Icons.Filled.Search, "Search", onClick = {})
        })
        ScreenTopBar(title = "Hazy Monsoon", collapsed = true, onBack = {}, actions = {
            AppIconButton(Icons.Filled.MoreVert, "More options", onClick = {})
        })
        ScreenTitle("Library", subtitle = "11 playlists")
        SectionHeader("Jump back in", actionLabel = "See all", onAction = {})
        SectionHeader("Recently added", contentPadding = PaddingValues(bottom = Space.md))
    }
}

@ComponentPreviews
@Composable
private fun CollectionHeaderPreview() = PreviewTheme { CollectionHeaderSample() }

/** Sample states, shared by the preview and the in-app component gallery. */
@Composable
internal fun CollectionHeaderSample() {
    CollectionHeader(
        title = "Hazy Monsoon",
        subtitle = "Mix from your listening",
        meta = "21 songs",
        playing = false,
        onPlay = {},
        onShuffle = {},
        onDownload = {},
        onMore = {},
    )
}

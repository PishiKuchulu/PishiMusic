package com.tuned.app.ui.common

import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.tuned.app.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuned.app.ui.common.AppIconButton
import com.tuned.app.ui.common.ArtworkTintBackdrop
import com.tuned.app.ui.player.LocalMusicPlayer
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.Keyline
import com.tuned.app.ui.theme.Radius
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.iconEdgeInset
import com.tuned.app.ui.theme.tunedColors

private val HeaderShape = RoundedCornerShape(bottomStart = Radius.xl, bottomEnd = Radius.xl)

/**
 * The header at the top of every main screen, in Home's style (`HomeHeader` + `HomeHeaderPanel`):
 * a full-bleed panel washed in the accent and the playing song's artwork, with 28dp bottom
 * corners, running up behind the status bar.
 *
 * Inside: the title on the left margin (display size on a tab root; a smaller title after a
 * back arrow on a pushed screen) and plain icon [actions] whose glyphs end on the right margin,
 * like Home's. [subtitle] sits on its own line under the title. [content] goes below that, inside
 * the panel, the way Home holds its chips (Search puts its field and filters there).
 */
@Composable
fun ExpressiveHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    /** Fold [content] away while the page scrolls down, like Home's chips (tab roots only). */
    foldContentOnScroll: Boolean = false,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val colors = MaterialTheme.tunedColors
    val chrome by LocalMusicPlayer.current.chromeState.collectAsStateWithLifecycle()
    Box(
        modifier
            .fillMaxWidth()
            .zIndex(1f)
            .clip(HeaderShape)
            .background(colors.bg),
    ) {
        ArtworkTintBackdrop(artwork = chrome.current?.artworkUrl, tint = colors.accent, modifier = Modifier.matchParentSize())
        Column(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        // A back arrow's glyph sits on the margin, like the trailing icons.
                        start = if (onBack != null) iconEdgeInset else Keyline.margin,
                        end = iconEdgeInset,
                        top = Space.lg,
                        // Home's spacing when the content can fold: md under the title, so the
                        // folded header keeps a margin.
                        bottom = when {
                            subtitle == null && content == null -> Space.lg
                            foldContentOnScroll -> Space.md
                            else -> Space.xs
                        },
                    )
                    .height(Sizes.touch),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    AppIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cmp_back), onClick = onBack)
                    Spacer(Modifier.width(Space.xs))
                }
                Text(
                    title,
                    style = if (onBack != null) AppType.headline else AppType.display,
                    color = colors.text1,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                actions()
            }
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = AppType.bodyS,
                    color = colors.text2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(
                        start = if (onBack != null) iconEdgeInset + Sizes.touch + Space.xs else Keyline.margin,
                        end = Keyline.margin,
                        bottom = if (content == null) Space.lg else Space.md,
                    ),
                )
            }
            if (content != null) {
                if (foldContentOnScroll) {
                    // Same signal and spring as Home's chips: the dock's scroll state.
                    val folded = com.tuned.app.ui.shell.LocalDockState.current.tabsCollapsed
                    val motion = com.tuned.app.ui.theme.rememberMotion()
                    val spec = if (motion.reduced) androidx.compose.animation.core.snap<androidx.compose.ui.unit.IntSize>() else androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
                        visibilityThreshold = androidx.compose.ui.unit.IntSize.VisibilityThreshold,
                    )
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !folded,
                        enter = androidx.compose.animation.expandVertically(spec, expandFrom = Alignment.Top) + androidx.compose.animation.fadeIn(motion.base()),
                        exit = androidx.compose.animation.shrinkVertically(spec, shrinkTowards = Alignment.Top) + androidx.compose.animation.fadeOut(motion.fast()),
                    ) {
                        Column(Modifier.fillMaxWidth()) { content() }
                    }
                } else {
                    content()
                }
            }
        }
    }
}

/**
 * A trailing header action: a plain icon whose glyph ends on the margin, the same as Home's
 * header buttons (no tonal circle behind it, since the panel's wash already sets it off).
 */
@Composable
fun HeaderActionIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    // Non-null label, enforced by the type: the nullable version funnelled
    // through orEmpty() and silently produced nameless icon buttons,
    // defeating AppIconButton's required-label contract.
    AppIconButton(icon, contentDescription, onClick = onClick)
}

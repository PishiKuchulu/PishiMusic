package com.tuned.app.ui.common

import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import com.tuned.app.ui.theme.rememberMotion
import com.tuned.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.animateFloatAsState
import com.tuned.app.ui.common.rememberGroupPressScale
import com.tuned.app.ui.common.GroupOuterRadius
import com.tuned.app.ui.common.GroupInnerRadius
import com.tuned.app.ui.common.GroupGap
import androidx.compose.ui.graphics.graphicsLayer
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.palette.graphics.Palette
import androidx.compose.ui.unit.Dp
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.tuned.app.ui.player.isBlackAndWhite
import com.tuned.app.ui.theme.AppShape
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.Keyline
import com.tuned.app.ui.theme.Radius
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.tunedColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Ink for anything drawn on a [CoverSheet]: the theme's own text, line and fill roles. */
object SheetInk {
    val text1: Color @Composable get() = MaterialTheme.tunedColors.text1
    val text2: Color @Composable get() = MaterialTheme.tunedColors.text2
    val text3: Color @Composable get() = MaterialTheme.tunedColors.text3
    val line: Color @Composable get() = MaterialTheme.tunedColors.line
    val fill: Color @Composable get() = MaterialTheme.tunedColors.surface2
    val fillPressed: Color @Composable get() = MaterialTheme.tunedColors.text1.copy(alpha = 0.14f)
}

/**
 * A bottom sheet on Tuned's sheet surface, with an optional [cover] that sits over the
 * sheet's top edge, centred. Content uses [SheetInk] colours.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverSheet(
    onDismiss: () -> Unit,
    cover: (@Composable () -> Unit)? = null,
    /** Share of the screen the panel opens at; scrolling the content grows it to full height. */
    openFraction: Float = 0.8f,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tint = MaterialTheme.tunedColors.surface1
    var expanded by remember { mutableStateOf(false) }
    val motion = rememberMotion()
    val fraction by animateFloatAsState(if (expanded) 1f else openFraction, motion.spring(), label = "sheetHeight")
    val screen = LocalConfiguration.current.screenHeightDp.dp
    // The first upward scroll grows the sheet to full height before the content moves.
    val growFirst = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!expanded && available.y < 0f) {
                    expanded = true
                    return available
                }
                return Offset.Zero
            }
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        // The sheet itself is clear; the tinted panel below is drawn by hand so
        // the cover can sit over its top edge.
        shape = RectangleShape,
        containerColor = Color.Transparent,
        contentColor = SheetInk.text1,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        dragHandle = null,
    ) {
        val overlap = if (cover != null) Sizes.sheetArt / 2 else Dp(0f)
        // Kept below the status bar when the sheet is fully open.
        Box(Modifier.fillMaxWidth().statusBarsPadding()) {
            Column(
                Modifier
                    .padding(top = overlap)
                    .fillMaxWidth()
                    .heightIn(max = screen * fraction - overlap)
                    .nestedScroll(growFirst)
                    .clip(RoundedCornerShape(topStart = Radius.xl, topEnd = Radius.xl))
                    .background(tint)
                    .navigationBarsPadding()
                    .padding(bottom = Space.sm),
            ) {
                if (cover != null) {
                    Spacer(Modifier.height(overlap + Space.lg))
                } else {
                    Box(Modifier.fillMaxWidth().padding(top = Space.md, bottom = Space.sm), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(Sizes.handleWidth, Sizes.handleHeight).clip(AppShape.full).background(SheetInk.line))
                    }
                }
                Box(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth()) { content() }
                    if (cover != null) {
                        // Content scrolling up softens out under the cover instead
                        // of stopping on a hard line.
                        Box(
                            Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .height(Space.lg)
                                .background(Brush.verticalGradient(listOf(tint, tint.copy(alpha = 0f)))),
                        )
                    }
                }
            }
            if (cover != null) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .size(Sizes.sheetArt)
                        .clip(AppShape.lg),
                ) { cover() }
            }
        }
    }
}

/** Centred title and subtitle under the sheet's cover. */
@Composable
fun SheetTitle(title: String, subtitle: String?) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Keyline.margin),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.xxs),
    ) {
        Text(title, style = AppType.titleL, color = SheetInk.text1, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        if (!subtitle.isNullOrBlank()) {
            Text(subtitle, style = AppType.body, color = SheetInk.text2, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

/** One of the big rounded actions at the top of a sheet: icon over a short label. */
data class SheetAction(val icon: ImageVector, val label: String, val active: Boolean = false, val onClick: () -> Unit)

/** A row of equal, rounded action buttons on the margin. */
@Composable
fun SheetActionRow(actions: List<SheetAction>) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Keyline.margin),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        actions.forEach { action ->
            val interaction = remember { MutableInteractionSource() }
            Column(
                Modifier
                    .weight(1f)
                    .height(Sizes.sheetAction)
                    .pressScale(interaction)
                    .clip(AppShape.md)
                    .background(SheetInk.fill)
                    .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = action.onClick),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // The accent marks one thing only: an action that is on (a liked song).
                val ink = if (action.active) MaterialTheme.tunedColors.accent else SheetInk.text1
                Icon(action.icon, contentDescription = null, tint = ink, modifier = Modifier.size(Sizes.icon))
                Spacer(Modifier.height(Space.xs))
                Text(action.label, style = AppType.label, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/**
 * A list row on a tinted sheet: a 24dp icon on the margin, the label, and an
 * optional value, spinner or chevron on the right. 56dp tall.
 */
@Composable
fun SheetRow(
    icon: ImageVector?,
    label: String,
    value: String? = null,
    loading: Boolean = false,
    chevron: Boolean = false,
    dim: Boolean = false,
    onClick: (() -> Unit)?,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.rowCompact)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = Keyline.margin),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = SheetInk.text2, modifier = Modifier.size(Sizes.icon))
            Spacer(Modifier.width(Space.lg))
        }
        Text(
            label,
            style = AppType.body,
            color = if (dim) SheetInk.text2 else SheetInk.text1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        when {
            loading -> {
                Spacer(Modifier.width(Space.md))
                com.tuned.app.ui.common.LoadingSpinner(color = SheetInk.text2, strokeWidth = Space.xxs, modifier = Modifier.size(Sizes.iconInline))
            }
            value != null -> {
                Spacer(Modifier.width(Space.md))
                Text(value, style = AppType.bodyS, color = SheetInk.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (chevron) {
            Spacer(Modifier.width(Space.xs))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = SheetInk.text3, modifier = Modifier.size(Sizes.iconRow))
        }
    }
}

/** Groups of rows split by an inset hairline; no card behind any row. */
@Composable
fun SheetGroups(groups: List<List<@Composable () -> Unit>>) {
    val shown = groups.filter { it.isNotEmpty() }
    shown.forEachIndexed { index, rows ->
        if (index > 0) {
            Box(
                Modifier
                    .padding(start = Keyline.margin + Sizes.icon + Space.lg, top = Space.xs, bottom = Space.xs)
                    .fillMaxWidth()
                    .height(Sizes.hairline)
                    .background(SheetInk.line),
            )
        }
        rows.forEach { it() }
    }
}

/** The genre as a small pill under the title; tap to explore it. */
@Composable
fun SheetPill(text: String, onClick: (() -> Unit)?) {
    val interaction = remember { MutableInteractionSource() }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = AppType.label,
            color = SheetInk.text1,
            maxLines = 1,
            modifier = Modifier
                .pressScale(interaction)
                .clip(AppShape.full)
                .background(SheetInk.fill)
                .then(if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick) else Modifier)
                .padding(horizontal = Space.md, vertical = Space.xs + Space.xxs),
        )
    }
}

/**
 * A card that leads somewhere real (the artist, the album): its picture on the
 * left (round for people, square for records), a small caption and the name
 * in heavy type.
 */
@Composable
fun SheetLinkCard(
    caption: String,
    name: String,
    round: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    image: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberGroupPressScale(interaction)
    Row(
        modifier
            .height(Sizes.sheetAction)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(AppShape.md)
            .background(SheetInk.fill)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(Sizes.artRow)
                .clip(if (round) AppShape.full else AppShape.xs)
                .background(SheetInk.fill),
            contentAlignment = Alignment.Center,
        ) { image() }
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
            Text(caption, style = AppType.caption, color = SheetInk.text2, maxLines = 1)
            Text(name, style = AppType.bodyStrong, color = SheetInk.text1, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** A sub-page header inside a sheet: back arrow on the margin and the page title. */
@Composable
fun SheetSubHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(Sizes.rowCompact)
            .padding(horizontal = Keyline.margin - (Sizes.touch - Sizes.icon) / 2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconButton(TunedIcons.Back, stringResource(R.string.menu_back), onBack, tint = SheetInk.text1)
        Spacer(Modifier.width(Space.sm))
        Text(title, style = AppType.title, color = SheetInk.text1)
    }
    SheetDivider()
}

/** A full-width hairline between parts of a sheet. */
@Composable
fun SheetDivider() {
    Box(Modifier.padding(vertical = Space.xs).fillMaxWidth().height(Sizes.hairline).background(SheetInk.line))
}

/**
 * The song's first spec under the title, in mono ("OPUS"). One fact, so no
 * separators. Tapping it opens the genre when there is one.
 */
@Composable
fun SheetSpecLine(parts: List<String>, onGenre: (() -> Unit)?) {
    Text(
        parts.firstOrNull().orEmpty(),
        style = AppType.mono,
        color = SheetInk.text2,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Keyline.margin)
            .then(if (onGenre != null) Modifier.clickable(role = Role.Button, onClick = onGenre) else Modifier),
    )
}

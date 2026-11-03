package com.tuned.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Layout tokens for Tuned. Every spacing, size and corner radius in UI code
 * comes from here; tools/check_design_tokens.py fails the build when raw dp
 * values grow outside ui/theme. Values sit on a 4dp grid (2dp only for
 * optical nudges). See docs/plans/redesign-plan.md section 3.
 */
object Space {
    /** Icon-to-badge nudge, hairline offsets. */
    val xxs: Dp = 2.dp
    /** Between paired text lines, inside chips. */
    val xs: Dp = 4.dp
    /** Icon to label, tight stacks. */
    val sm: Dp = 8.dp
    /** Gap between row content, grid gutter. */
    val md: Dp = 12.dp
    /** Compact screen margin, card padding. */
    val lg: Dp = 16.dp
    /** Between groups inside a section; screen margin on medium widths. */
    val xl: Dp = 24.dp
    /** Between sections. */
    val xxl: Dp = 32.dp
    /** Above a screen title. */
    val x3: Dp = 40.dp
    /** Empty-state padding. */
    val x4: Dp = 48.dp
    /** Hero spacing in the player. */
    val x6: Dp = 64.dp
}

/** Window width classes used by [Keyline]. Matches Material's breakpoints. */
enum class WidthClass { Compact, Medium, Expanded }

@Composable
@ReadOnlyComposable
fun currentWidthClass(): WidthClass {
    val width = LocalConfiguration.current.screenWidthDp
    return when {
        width >= 840 -> WidthClass.Expanded
        width >= 600 -> WidthClass.Medium
        else -> WidthClass.Compact
    }
}

/**
 * Horizontal alignment lines. List text always starts at [textStart]
 * (margin + row artwork + gap), so section headers, dividers and empty states
 * align to either the screen margin or the text column and nothing else.
 */
object Keyline {
    val marginCompact: Dp = Space.lg
    val marginMedium: Dp = Space.xl
    val marginExpanded: Dp = Space.xxl

    /** Apple Music's page margin, used on the artist and album pages (20pt on iPhone). */
    val detail: Dp = 20.dp

    /** Where row text starts on compact screens: 16 + 48 + 12 = 76dp. */
    val textStart: Dp = marginCompact + Sizes.artRow + Space.md

    /** Content stops growing past this width on medium screens. */
    val maxContentMedium: Dp = 720.dp
    /** Two-pane layouts stop growing past this width. */
    val maxContentExpanded: Dp = 1040.dp

    val margin: Dp
        @Composable @ReadOnlyComposable
        get() = when (currentWidthClass()) {
            WidthClass.Compact -> marginCompact
            WidthClass.Medium -> marginMedium
            WidthClass.Expanded -> marginExpanded
        }

    val gridColumns: Int
        @Composable @ReadOnlyComposable
        get() = when (currentWidthClass()) {
            WidthClass.Compact -> 2
            WidthClass.Medium -> 3
            WidthClass.Expanded -> 4
        }

    val gridGutter: Dp
        @Composable @ReadOnlyComposable
        get() = if (currentWidthClass() == WidthClass.Compact) Space.md else Space.lg
}

/** Fixed component sizes. Named Sizes to avoid clashing with geometry.Size. */
object Sizes {
    /** Minimum touch target for anything tappable. */
    val touch: Dp = 48.dp

    val icon: Dp = 24.dp
    val iconRow: Dp = 20.dp
    val iconInline: Dp = 16.dp

    val artRow: Dp = 48.dp
    val artRowCompact: Dp = 40.dp
    val artQueueCurrent: Dp = 56.dp

    val tileSquare: Dp = 144.dp
    /** Artist circles in a shelf. */
    val tileArtist: Dp = 112.dp
    val tileWideWidth: Dp = 264.dp
    val tileWideHeight: Dp = 148.dp
    /** Tall artwork cards (Quick picks): width from Apple Music's Top Picks on Android, height trimmed to 280dp. */
    val tileTallWidth: Dp = 220.dp
    val tileTallHeight: Dp = 280.dp
    /** Home's Infinite Radio card. */
    val heroHeight: Dp = 200.dp

    val avatarHeader: Dp = 32.dp
    val avatarRow: Dp = 64.dp
    val avatarHero: Dp = 96.dp

    val topBar: Dp = 64.dp
    /** Floating mini player: 56dp artwork inset 8dp. */
    val miniPlayer: Dp = 72.dp
    val bottomBar: Dp = 64.dp
    /** Tab row inside the floating dock. */
    val dockTabs: Dp = 56.dp
    /** The dock floats on a soft shadow. */
    val dockElevation: Dp = 12.dp
    /** Tonal lift of the dock's surface when Liquid Glass is off. */
    val dockTonal: Dp = 6.dp
    /** Play button inside the mini player. */
    val miniPlayButton: Dp = 40.dp

    val row: Dp = 64.dp
    val rowCompact: Dp = 56.dp
    val rowTall: Dp = 72.dp

    val playButton: Dp = 56.dp
    val playButtonPlayer: Dp = 72.dp

    /** Visible chip height; the touch target still grows to [touch]. */
    val chip: Dp = 32.dp
    /** Empty and error state icons. */
    val iconHero: Dp = 48.dp

    /** Collection header artwork (album, playlist). */
    val collectionArt: Dp = 200.dp
    val collectionArtMedium: Dp = 240.dp

    /** Bottom sheet drag handle. */
    /** Cover in a cover-tinted sheet's header, sitting over the sheet's top edge. */
    val sheetArt: Dp = 112.dp

    /** A large action button in a sheet (icon over label). */
    val sheetAction: Dp = 64.dp


    val handleWidth: Dp = 32.dp
    val handleHeight: Dp = 4.dp

    /** Skeleton placeholder bars standing in for a title and a subtitle. */
    val skeletonTitle: Dp = 12.dp
    val skeletonSubtitle: Dp = 8.dp

    /** 1dp separators and outlines. */
    val hairline: Dp = 1.dp

    /** Playback progress along the mini player's edge. */
    val progressLine: Dp = 2.dp
}

/**
 * Padding that puts an icon's visible edge on the screen margin while its
 * touch target stays [Sizes.touch]. A 24dp icon in a 48dp target has 12dp of
 * slack on each side, so the container sits 4dp in from the edge.
 */
val iconEdgeInset: Dp
    @Composable @ReadOnlyComposable
    get() = Keyline.margin - (Sizes.touch - Sizes.icon) / 2

/** Same as [iconEdgeInset] for 20dp row icons. */
val rowIconEdgeInset: Dp
    @Composable @ReadOnlyComposable
    get() = Keyline.margin - (Sizes.touch - Sizes.iconRow) / 2

/** Corner radii. Four steps plus full; nothing else. */
object Radius {
    /** Row artwork, badges. */
    val xs: Dp = 4.dp
    /** Tiles, chips, inputs, buttons. */
    val sm: Dp = 8.dp
    /** Sheet top corners, cards. */
    val md: Dp = 12.dp
    /** Player artwork, dialogs. */
    val lg: Dp = 16.dp
    /** Hero cards and the floating dock: the deliberately soft, bold surfaces. */
    val xl: Dp = 28.dp
    /** Tuned's original floating dock. */
    val dock: Dp = 32.dp
}

/** The explicit "E" chip: small beside rows, larger beside a page title. */
object ExplicitMark {
    val text = 9.5.sp
    val textLarge = 11.sp
}

/** Shapes built from [Radius]. */
object AppShape {
    val xs = RoundedCornerShape(Radius.xs)
    val sm = RoundedCornerShape(Radius.sm)
    val md = RoundedCornerShape(Radius.md)
    val lg = RoundedCornerShape(Radius.lg)
    val xl = RoundedCornerShape(Radius.xl)
    val dock = RoundedCornerShape(Radius.dock)
    /** Avatars, switches and the play button only. */
    val full = RoundedCornerShape(percent = 50)
    val sheet = RoundedCornerShape(topStart = Radius.md, topEnd = Radius.md)
}

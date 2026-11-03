package com.tuned.app.ui.shell

import com.tuned.app.ui.theme.glassPageTint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.kyant.backdrop.Backdrop
import com.tuned.app.ui.theme.AppShape
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.HapticKind
import com.tuned.app.ui.theme.Keyline
import com.tuned.app.ui.theme.LiquidGlassPreset
import com.tuned.app.ui.theme.LocalLiquidGlass
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.liquidGlassChrome
import com.tuned.app.ui.theme.isLiquidGlassBackdropSupported
import com.tuned.app.ui.theme.rememberHaptics
import com.tuned.app.ui.theme.rememberMotion
import com.tuned.app.ui.theme.tunedColors

/**
 * Total height of the bottom chrome (dock, floating mini player, their gaps
 * and the gesture inset). Scrolling content pads its end by this so the last
 * item clears the chrome. Computed once by PlayerHost; it does not change
 * when the dock hides on scroll, so lists never jump.
 */
val LocalBottomChromeHeight = compositionLocalOf { Dp(0f) }

/** Whether the dock is hidden. Set by scrolling in the tabs. */
@Stable
class DockState {
    var tabsCollapsed by mutableStateOf(false)
}

val LocalDockState = compositionLocalOf { DockState() }

/**
 * Hides the dock while content scrolls down and brings it back on any scroll
 * up. Attach to the container around the tab content; it reads the scroll of
 * whatever list is inside. 24dp of hysteresis stops it flickering.
 */
class DockScrollConnection(private val state: DockState, private val hysteresisPx: Float) : NestedScrollConnection {
    private var travel = 0f

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        val dy = consumed.y
        if (dy == 0f) return Offset.Zero
        travel = if ((travel < 0) == (dy < 0)) travel + dy else dy
        when {
            travel < -hysteresisPx -> state.tabsCollapsed = true
            travel > hysteresisPx -> state.tabsCollapsed = false
        }
        return Offset.Zero
    }

    override suspend fun onPostFling(consumed: androidx.compose.ui.unit.Velocity, available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
        // A fast fling barely moves `travel` before it ends, so the dock could
        // be left in the opposite state of the final scroll direction.
        when {
            available.y < -800f -> state.tabsCollapsed = true
            available.y > 800f -> state.tabsCollapsed = false
        }
        return super.onPostFling(consumed, available)
    }
}

@Composable
fun rememberDockScrollConnection(state: DockState): DockScrollConnection {
    val hysteresis = with(LocalDensity.current) { Space.xl.toPx() }
    return remember(state, hysteresis) { DockScrollConnection(state, hysteresis) }
}

/** One destination in the dock. */
data class NavDestination(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

/**
 * Gap between the dock and the gesture bar. The mini player has to sit 4dp
 * higher than the original 12dp gap gave, to cover the top edge of a tile row
 * Home leaves showing above it at rest; with the tighter 6dp mini player gap
 * the dock takes up the difference (18dp).
 */
val DockBottomGap: Dp = Space.lg + Space.xxs

/** Space the dock takes above the gesture inset: 12dp above, the 56dp dock, 18dp below. */
val DockFootprint: Dp = Space.md + Sizes.dockTabs + DockBottomGap

/** Gap between the floating mini player and the dock: tight, so they read as one island. */
val MiniPlayerGap: Dp = Space.xs + Space.xxs

/** Mini player's bottom padding above the gesture inset when the dock is shown. */
val MiniPlayerAboveDock: Dp = DockBottomGap + Sizes.dockTabs + MiniPlayerGap

/** Height reserved for the chrome: gesture inset, dock and mini player when present. */
@Composable
fun bottomChromeHeight(hasMiniPlayer: Boolean, hasNavBar: Boolean): Dp {
    var height = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    height += if (hasNavBar) DockFootprint else Space.md
    if (hasMiniPlayer) height += Sizes.miniPlayer + MiniPlayerGap
    return height
}

/**
 * Backdrop the floating chrome samples when Liquid Glass is on. PlayerHost
 * provides one for both the dock and the mini player, so the two glass
 * surfaces refract the same layer and look identical.
 */
val LocalChromeBackdrop = compositionLocalOf<Backdrop?> { null }

/**
 * Surface colour of the dock and the mini player. Under Liquid Glass it is the
 * shared page tint over the glass, the same dark material as every other glass control.
 */
@Composable
fun chromeSurfaceColor(backdrop: Backdrop?): Color {
    val colors = MaterialTheme.tunedColors
    val glass = LocalLiquidGlass.current && isLiquidGlassBackdropSupported() && backdrop != null
    return if (glass) glassPageTint() else colors.surface2
}

/**
 * Tuned's floating dock, refined: a compact pill centred under the mini
 * player, only as wide as its tabs. The selected tab is a lighter neutral pill
 * with its icon and label; the rest are icons. Switching tabs grows the new
 * pill and shrinks the old one on the same spring, so the dock's width
 * changes smoothly instead of jumping.
 */
@Composable
fun FloatingDock(
    destinations: List<NavDestination>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = LocalChromeBackdrop.current,
) {
    val colors = MaterialTheme.tunedColors
    val liquidGlass = LocalLiquidGlass.current
    val haptics = rememberHaptics()
    val shape = AppShape.full
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            .padding(start = Keyline.margin, end = Keyline.margin, top = Space.md, bottom = DockBottomGap),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .liquidGlassChrome(shape, liquidGlass, LiquidGlassPreset.BottomNavigation, backdrop)
                .clip(shape)
                .background(chromeSurfaceColor(backdrop))
                .border(Sizes.hairline, colors.line, shape)
                .height(Sizes.dockTabs)
                .padding(horizontal = Space.xs)
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xxs),
        ) {
            destinations.forEachIndexed { index, destination ->
                val selected = index == selectedIndex
                DockItem(
                    destination = destination,
                    selected = selected,
                    onClick = {
                        if (!selected) haptics.perform(HapticKind.Tick)
                        onSelect(index)
                    },
                )
            }
        }
    }
}

@Composable
private fun DockItem(
    destination: NavDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.tunedColors
    val motion = rememberMotion()
    val pill by animateColorAsState(
        targetValue = if (selected) colors.text1.copy(alpha = 0.12f) else colors.text1.copy(alpha = 0f),
        animationSpec = motion.base(),
        label = "dockPill",
    )
    val content by animateColorAsState(
        targetValue = if (selected) colors.text1 else colors.text2,
        animationSpec = motion.base(),
        label = "dockContent",
    )
    // Critically damped: the pill opens quickly and settles without overshoot.
    val pop = if (motion.reduced) snap() else spring<androidx.compose.ui.unit.IntSize>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    Row(
        modifier = Modifier
            .height(Sizes.touch)
            .clip(AppShape.full)
            .background(pill)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = androidx.compose.material3.ripple(), onClick = onClick)
            .semantics {
                role = Role.Tab
                this.selected = selected
            }
            .padding(horizontal = Space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (selected) destination.selectedIcon else destination.icon,
            contentDescription = destination.label,
            tint = content,
            modifier = Modifier.size(Sizes.icon),
        )
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(motion.base()) + expandHorizontally(pop, expandFrom = Alignment.Start),
            exit = fadeOut(motion.fast()) + shrinkHorizontally(pop, shrinkTowards = Alignment.Start),
        ) {
            Text(
                text = destination.label,
                style = AppType.label,
                color = content,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(start = Space.sm),
            )
        }
    }
}

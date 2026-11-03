package com.tuned.app.ui.common

import androidx.compose.foundation.ScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tuned.app.ui.theme.tunedColors

/**
 * Surface of the blurred-backdrop sheets (song menu, Signal path, Lyrics source): the sheet's own
 * page colour, slightly see-through so the blur still reads at the edges, and solid enough that the
 * screen behind never shows through the gaps between cards.
 */
@Composable
fun blurSheetContainerColor(): Color = MaterialTheme.tunedColors.surface1.copy(alpha = 0.94f)

/**
 * Sits between a sheet's scrolling body and the sheet. Material's sheet takes whatever a fling has
 * left when the list hits an end: at the bottom it kicked the sheet up and let it spring back, at
 * the top it dragged the sheet down (sometimes closing it). Here a fling's leftover speed never
 * reaches the sheet, and neither does pushing up past the bottom; only a finger pulling down from the top moves it,
 * so swipe-to-dismiss works as before. Put it on the viewport, before `verticalScroll`.
 */
@Composable
fun Modifier.sheetScrollGuard(): Modifier {
    val guard = remember {
        object : NestedScrollConnection {
            // True once the finger has pulled the sheet down in this gesture; its release fling then
            // goes to the sheet, which settles open or closes.
            private var fingerMovedSheet = false

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) {
                    // Fling steps pass untouched: the sheet ignores them, and the list's fling then
                    // stops at once. Eating them kept a dead fling running for about a second after
                    // a drag-to-close, and the sheet sat half-closed until it ended.
                    return Offset.Zero
                }
                if (available.y > 0f) {
                    fingerMovedSheet = true
                    return Offset.Zero
                }
                return available
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val passToSheet = fingerMovedSheet
                fingerMovedSheet = false
                return if (passToSheet) Velocity.Zero else available
            }
        }
    }
    return nestedScroll(guard)
}

/**
 * Fades the top [height] of a scrolling sheet body once it has scrolled, so rows slide out under
 * the handle instead of being sliced by a hard edge. Put it on the viewport, before `verticalScroll`.
 */
fun Modifier.fadeTopEdge(scroll: ScrollState, height: Dp = 24.dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val h = height.toPx()
        val strength = (scroll.value / h).coerceIn(0f, 1f)
        if (strength > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 1f - strength),
                    1f to Color.Black,
                    endY = h,
                ),
                size = Size(size.width, h),
                blendMode = BlendMode.DstIn,
            )
        }
    }

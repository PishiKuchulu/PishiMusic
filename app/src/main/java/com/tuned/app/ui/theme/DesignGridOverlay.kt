package com.tuned.app.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Debug overlay for checking alignment on a device. Draws:
 *  - a 4dp grid (faint) with every 8dp line slightly stronger,
 *  - the screen margins (16dp on compact widths) in cyan,
 *  - the row text keyline at 76dp in magenta.
 * Touches pass through; it never consumes input.
 */
@Composable
fun DesignGridOverlay(modifier: Modifier = Modifier) {
    val margin = Keyline.margin
    val textStart = margin + Sizes.artRow + Space.md
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(modifier.fillMaxSize()) {
        val step = 4.dp.toPx()
        val hair = 1f
        val minor = Color(0x14FFFFFF)
        val major = Color(0x26FFFFFF)

        var i = 0
        var x = 0f
        while (x <= size.width) {
            drawLine(if (i % 2 == 0) major else minor, Offset(x, 0f), Offset(x, size.height), hair)
            x += step; i++
        }
        i = 0
        var y = 0f
        while (y <= size.height) {
            drawLine(if (i % 2 == 0) major else minor, Offset(0f, y), Offset(size.width, y), hair)
            y += step; i++
        }

        fun vertical(dpFromStart: Float, color: Color) {
            val px = if (rtl) size.width - dpFromStart else dpFromStart
            drawLine(color, Offset(px, 0f), Offset(px, size.height), 2f)
        }
        val marginPx = margin.toPx()
        vertical(marginPx, Color(0xCC00E5FF))
        vertical(size.width - marginPx, Color(0xCC00E5FF))
        vertical(textStart.toPx(), Color(0xCCFF2BD6))
    }
}

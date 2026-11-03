package com.tuned.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/**
 * Colour roles for Tuned. Components read roles, never hex values. Neutrals
 * are fixed; only [accent] changes (manual pick, artwork or wallpaper), and
 * it is adjusted until it meets contrast against the background.
 */
@Immutable
data class TunedColors(
    val bg: Color,
    /** Rows on press, sheets. */
    val surface1: Color,
    /** Inputs, chips. */
    val surface2: Color,
    /** Raised controls on surface2. */
    val surface3: Color,
    /** 1dp hairlines and borders. */
    val line: Color,
    /** Primary text. */
    val text1: Color,
    /** Secondary text; 4.5:1 or better on bg, surface1 and surface2. */
    val text2: Color,
    /** Captions and placeholders; 4.5:1 or better on bg, surface1 and surface2. */
    val text3: Color,
    val accent: Color,
    val onAccent: Color,
    /** Tinted fill for selected rows and badges; text1 reads on it. */
    val accentContainer: Color,
    val danger: Color,
    val isDark: Boolean,
)

object TunedPalette {
    /** Tuned's default accent. */
    const val DEFAULT_ACCENT_HEX = "#FF6A3D"

    private const val TEXT_CONTRAST = 4.5f
    private const val UI_CONTRAST = 3f

    fun dark(accentSeed: Color, amoled: Boolean = false, monochrome: Boolean = false): TunedColors {
        val bg = if (amoled) Color(0xFF000000) else Color(0xFF0B0B0C)
        val surface1 = if (amoled) Color(0xFF0E0E0F) else Color(0xFF141415)
        val surface2 = if (amoled) Color(0xFF161618) else Color(0xFF1C1C1E)
        val surface3 = if (amoled) Color(0xFF1E1E20) else Color(0xFF242426)
        val text1 = Color(0xFFEBEBEB)
        val seed = if (monochrome) text1 else accentSeed
        val accent = ensureContrast(seed, against = listOf(bg to TEXT_CONTRAST, surface2 to UI_CONTRAST), toward = Color.White)
        return TunedColors(
            bg = bg,
            surface1 = surface1,
            surface2 = surface2,
            surface3 = surface3,
            line = Color(0xFF262628),
            text1 = text1,
            text2 = Color(0xFFA7A7A9),
            text3 = Color(0xFF8A8A8C),
            accent = accent,
            onAccent = readableOn(accent),
            accentContainer = lerp(bg, accent, 0.20f),
            danger = Color(0xFFFF6B5E),
            isDark = true,
        )
    }

    fun light(accentSeed: Color, monochrome: Boolean = false): TunedColors {
        val bg = Color(0xFFFAFAF8)
        val surface2 = Color(0xFFF1F1EE)
        val text1 = Color(0xFF121212)
        val seed = if (monochrome) text1 else accentSeed
        val accent = ensureContrast(seed, against = listOf(bg to TEXT_CONTRAST, surface2 to UI_CONTRAST), toward = Color.Black)
        return TunedColors(
            bg = bg,
            surface1 = Color(0xFFFFFFFF),
            surface2 = surface2,
            surface3 = Color(0xFFE8E8E4),
            line = Color(0xFFE2E2DE),
            text1 = text1,
            text2 = Color(0xFF5F5F5E),
            text3 = Color(0xFF6B6B6A),
            accent = accent,
            onAccent = readableOn(accent),
            accentContainer = lerp(bg, accent, 0.14f),
            danger = Color(0xFFC62A1E),
            isDark = false,
        )
    }

    fun dark(accentHex: String, amoled: Boolean = false, monochrome: Boolean = false): TunedColors =
        dark(parseHex(accentHex), amoled, monochrome)

    fun light(accentHex: String, monochrome: Boolean = false): TunedColors =
        light(parseHex(accentHex), monochrome)

    /**
     * Material's roles filled from Tuned roles, so existing Material components
     * render with the same palette. Secondary and tertiary containers are
     * neutral on purpose: only the accent carries colour.
     */
    fun toMaterial(c: TunedColors): ColorScheme {
        val errorContainer = lerp(c.bg, c.danger, if (c.isDark) 0.20f else 0.14f)
        return if (c.isDark) {
            darkColorScheme(
                primary = c.accent, onPrimary = c.onAccent,
                primaryContainer = c.accentContainer, onPrimaryContainer = c.text1,
                inversePrimary = ensureContrast(c.accent, listOf(Color(0xFFFAFAF8) to TEXT_CONTRAST), Color.Black),
                secondary = c.text2, onSecondary = c.bg,
                secondaryContainer = c.surface2, onSecondaryContainer = c.text1,
                tertiary = c.accent, onTertiary = c.onAccent,
                tertiaryContainer = c.surface2, onTertiaryContainer = c.text1,
                background = c.bg, onBackground = c.text1,
                surface = c.bg, onSurface = c.text1,
                surfaceVariant = c.surface2, onSurfaceVariant = c.text2,
                surfaceTint = c.bg,
                inverseSurface = c.text1, inverseOnSurface = c.bg,
                error = c.danger, onError = c.bg,
                errorContainer = errorContainer, onErrorContainer = c.text1,
                outline = c.text3, outlineVariant = c.line,
                scrim = Color.Black,
                surfaceBright = c.surface3, surfaceDim = c.bg,
                surfaceContainerLowest = c.bg, surfaceContainerLow = c.surface1,
                surfaceContainer = c.surface1, surfaceContainerHigh = c.surface2,
                surfaceContainerHighest = c.surface3,
            )
        } else {
            lightColorScheme(
                primary = c.accent, onPrimary = c.onAccent,
                primaryContainer = c.accentContainer, onPrimaryContainer = c.text1,
                inversePrimary = ensureContrast(c.accent, listOf(Color(0xFF0B0B0C) to TEXT_CONTRAST), Color.White),
                secondary = c.text2, onSecondary = c.surface1,
                secondaryContainer = c.surface2, onSecondaryContainer = c.text1,
                tertiary = c.accent, onTertiary = c.onAccent,
                tertiaryContainer = c.surface2, onTertiaryContainer = c.text1,
                background = c.bg, onBackground = c.text1,
                surface = c.bg, onSurface = c.text1,
                surfaceVariant = c.surface2, onSurfaceVariant = c.text2,
                surfaceTint = c.bg,
                inverseSurface = c.text1, inverseOnSurface = c.bg,
                error = c.danger, onError = c.surface1,
                errorContainer = errorContainer, onErrorContainer = c.text1,
                outline = c.text3, outlineVariant = c.line,
                scrim = Color.Black,
                surfaceBright = c.surface1, surfaceDim = c.surface3,
                surfaceContainerLowest = c.surface1, surfaceContainerLow = c.bg,
                surfaceContainer = c.surface2, surfaceContainerHigh = c.surface3,
                surfaceContainerHighest = c.surface3,
            )
        }
    }

    /** Reads Tuned roles back out of a scheme built by [toMaterial]. */
    fun fromMaterial(s: ColorScheme, isDark: Boolean): TunedColors = TunedColors(
        bg = s.background,
        surface1 = if (isDark) s.surfaceContainer else s.surfaceContainerLowest,
        surface2 = if (isDark) s.surfaceContainerHigh else s.surfaceContainer,
        surface3 = s.surfaceContainerHighest,
        line = s.outlineVariant,
        text1 = s.onSurface,
        text2 = s.onSurfaceVariant,
        text3 = s.outline,
        accent = s.primary,
        onAccent = s.onPrimary,
        accentContainer = s.primaryContainer,
        danger = s.error,
        isDark = isDark,
    )

    /** WCAG 2.x contrast ratio between two opaque colours. */
    fun contrast(a: Color, b: Color): Float {
        val la = a.luminance() + 0.05f
        val lb = b.luminance() + 0.05f
        return if (la > lb) la / lb else lb / la
    }

    /**
     * Blends [color] toward [toward] in 4% steps until it meets every
     * (background, ratio) pair. Each step blends from the original colour, not
     * the previous step: repeated small blends from pure black round back to
     * black at 8 bits per channel and never move. At 100% the result is
     * [toward] itself, so the loop always ends on a readable colour.
     */
    fun ensureContrast(color: Color, against: List<Pair<Color, Float>>, toward: Color): Color {
        val start = color.copy(alpha = 1f)
        for (step in 0..25) {
            val c = lerp(start, toward, step * 0.04f)
            if (against.all { (bg, ratio) -> contrast(c, bg) >= ratio }) return c
        }
        return toward
    }

    /**
     * Black or white, whichever reads better on [color]. Pure black and white
     * guarantee at least 4.58:1 against any colour; an off-black would not
     * (a mid-grey accent gets only 4.36:1 with #0B0B0C).
     */
    fun readableOn(color: Color): Color =
        if (contrast(Color.Black, color) >= contrast(Color.White, color)) Color.Black else Color.White

    fun parseHex(hex: String): Color {
        val clean = hex.removePrefix("#")
        val argb = when (clean.length) {
            6 -> clean.toLongOrNull(16)?.or(0xFF000000)
            8 -> clean.toLongOrNull(16)
            else -> null
        } ?: return parseHex(DEFAULT_ACCENT_HEX)
        return Color(argb.toInt())
    }
}

/** Tuned roles for the current theme. */
val MaterialTheme.tunedColors: TunedColors
    @Composable @ReadOnlyComposable
    get() = TunedPalette.fromMaterial(colorScheme, colorScheme.background.luminance() < 0.5f)

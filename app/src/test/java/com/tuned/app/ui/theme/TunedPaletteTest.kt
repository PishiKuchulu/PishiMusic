package com.tuned.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

class TunedPaletteTest {

    /** Accent seeds that are hard to keep readable: near-black, near-white, saturated yellow, grey. */
    private val seeds = listOf(
        TunedPalette.DEFAULT_ACCENT_HEX, "#000000", "#101010", "#FFFFFF", "#FFFF00",
        "#808080", "#0000FF", "#E03030", "#00FF7F", "#4B0082",
    )

    private fun palettes(seed: String) = listOf(
        TunedPalette.dark(seed),
        TunedPalette.dark(seed, amoled = true),
        TunedPalette.light(seed),
    )

    @Test
    fun textRolesMeetAaOnEveryNeutralSurface() {
        for (c in palettes(TunedPalette.DEFAULT_ACCENT_HEX)) {
            for (text in listOf(c.text1, c.text2, c.text3)) {
                for (surface in listOf(c.bg, c.surface1, c.surface2)) {
                    assertThat(TunedPalette.contrast(text, surface)).isAtLeast(4.5f)
                }
            }
        }
    }

    @Test
    fun anyAccentSeedIsReadableAgainstTheBackground() {
        for (seed in seeds) {
            for (c in palettes(seed)) {
                val label = "seed $seed, dark=${c.isDark}, bg=${c.bg}, accent=${c.accent}"
                assertWithMessage(label).that(TunedPalette.contrast(c.accent, c.bg)).isAtLeast(4.5f)
                assertWithMessage(label).that(TunedPalette.contrast(c.accent, c.surface2)).isAtLeast(3f)
                assertWithMessage(label).that(TunedPalette.contrast(c.onAccent, c.accent)).isAtLeast(4.5f)
            }
        }
    }

    @Test
    fun textStaysReadableOnTheAccentContainer() {
        for (seed in seeds) {
            for (c in palettes(seed)) {
                assertThat(TunedPalette.contrast(c.text1, c.accentContainer)).isAtLeast(4.5f)
            }
        }
    }

    @Test
    fun readableAccentSeedIsLeftAlone() {
        val dark = TunedPalette.dark(TunedPalette.DEFAULT_ACCENT_HEX)
        assertThat(dark.accent).isEqualTo(TunedPalette.parseHex(TunedPalette.DEFAULT_ACCENT_HEX))
    }

    @Test
    fun materialSchemeRoundTripsToTheSameRoles() {
        for (c in palettes(TunedPalette.DEFAULT_ACCENT_HEX)) {
            val back = TunedPalette.fromMaterial(TunedPalette.toMaterial(c), c.isDark)
            assertThat(back).isEqualTo(c.copy(isDark = c.isDark))
        }
    }

    @Test
    fun badHexFallsBackToTheDefaultAccent() {
        assertThat(TunedPalette.parseHex("not a colour"))
            .isEqualTo(TunedPalette.parseHex(TunedPalette.DEFAULT_ACCENT_HEX))
        assertThat(TunedPalette.parseHex("#00FF00")).isEqualTo(Color(0xFF00FF00))
    }
}

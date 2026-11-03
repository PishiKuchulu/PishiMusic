package com.tuned.app.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import com.tuned.app.data.local.ThemeMode
import com.tuned.app.data.repository.ThemeUiState
import com.tuned.app.ui.theme.TunedPalette

/**
 * The widget's colours, taken from the same theme the app draws with: the
 * dark and light schemes [ThemeUiState] builds from the accent mode (manual,
 * monochrome, dynamic, now playing) and AMOLED, picked by Tuned's theme mode.
 *
 * Saved as plain ints next to the widget snapshot, so a cold widget update
 * (no app process, no repository) still draws in the user's colours.
 */
internal data class WidgetPalette(
    val card: Int,
    val line: Int,
    val text1: Int,
    val text2: Int,
    val text3: Int,
    val accent: Int,
    /** The mini player's tonal play button: text1 mixed 28% toward the accent. */
    val playContainer: Int,
    val playGlyph: Int,
    val isDark: Boolean = true,
) {
    companion object {
        private const val STORE = "tuned_widget_palette"
        private const val KEY_DARK = "dark_"
        private const val KEY_LIGHT = "light_"
        private const val KEY_MODE = "mode"

        /** Tuned's default dark palette, for before the app has ever saved one. */
        private val fallbackDark: WidgetPalette by lazy {
            from(TunedPalette.toMaterial(TunedPalette.dark(TunedPalette.DEFAULT_ACCENT_HEX, monochrome = true)), isDark = true)
        }
        private val fallbackLight: WidgetPalette by lazy {
            from(TunedPalette.toMaterial(TunedPalette.light(TunedPalette.DEFAULT_ACCENT_HEX, monochrome = true)), isDark = false)
        }

        private fun from(scheme: androidx.compose.material3.ColorScheme, isDark: Boolean): WidgetPalette {
            val c = TunedPalette.fromMaterial(scheme, isDark)
            return WidgetPalette(
                card = c.surface2.toArgb(),
                line = c.line.toArgb(),
                text1 = c.text1.toArgb(),
                text2 = c.text2.toArgb(),
                text3 = c.text3.toArgb(),
                accent = c.accent.toArgb(),
                playContainer = lerp(c.text1, c.accent, 0.28f).toArgb(),
                playGlyph = c.bg.toArgb(),
                isDark = isDark,
            )
        }

        /** Saves both palettes and the theme mode from the app's live theme. */
        fun save(context: Context, ui: ThemeUiState) {
            runCatching {
                val edit = context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit()
                from(ui.darkColorScheme, isDark = true).write(edit, KEY_DARK)
                from(ui.lightColorScheme, isDark = false).write(edit, KEY_LIGHT)
                edit.putString(KEY_MODE, ui.themeMode.storageValue).apply()
            }
        }

        /** The palette to draw with now, following Tuned's System / Light / Dark setting. */
        fun current(context: Context): WidgetPalette {
            val prefs = runCatching { context.getSharedPreferences(STORE, Context.MODE_PRIVATE) }.getOrNull()
            val mode = ThemeMode.fromStorage(prefs?.getString(KEY_MODE, null))
            val dark = when (mode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
            }
            val prefix = if (dark) KEY_DARK else KEY_LIGHT
            return prefs?.let { read(it, prefix)?.copy(isDark = dark) } ?: if (dark) fallbackDark else fallbackLight
        }

        private fun read(p: android.content.SharedPreferences, prefix: String): WidgetPalette? {
            if (!p.contains(prefix + "card")) return null
            fun i(name: String) = p.getInt(prefix + name, 0)
            return WidgetPalette(
                card = i("card"), line = i("line"), text1 = i("text1"), text2 = i("text2"), text3 = i("text3"),
                accent = i("accent"), playContainer = i("playContainer"), playGlyph = i("playGlyph"),
            )
        }
    }

    private fun write(edit: android.content.SharedPreferences.Editor, prefix: String) {
        edit.putInt(prefix + "card", card).putInt(prefix + "line", line)
            .putInt(prefix + "text1", text1).putInt(prefix + "text2", text2).putInt(prefix + "text3", text3)
            .putInt(prefix + "accent", accent).putInt(prefix + "playContainer", playContainer)
            .putInt(prefix + "playGlyph", playGlyph)
    }
}

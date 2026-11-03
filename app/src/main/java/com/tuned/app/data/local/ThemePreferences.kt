package com.tuned.app.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tuned.app.ui.theme.TunedPalette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class AccentMode(val storageValue: String) {
    MANUAL("manual"),
    DYNAMIC("dynamic"),
    MONOCHROME("monochrome");

    companion object {
        /** Nothing stored yet means a fresh install: Tuned starts monochrome. */
        fun fromStorage(value: String?): AccentMode =
            entries.firstOrNull { it.storageValue == value } ?: MONOCHROME
    }
}

enum class ThemeMode(val storageValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}

data class ThemePrefs(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: String = TunedPalette.DEFAULT_ACCENT_HEX,
    val accentMode: AccentMode = AccentMode.MONOCHROME,
    val amoled: Boolean = false,
    /** iOS-style liquid-glass materials. Off by default; Tuned keeps blur to the bottom bar and player. */
    val liquidGlass: Boolean = false,
    /** Optional Material 3 full-width navigation bar instead of the default floating dock. */
    val md3NavBar: Boolean = false,
    /** Whether bottom navigation tabs are completely hidden. */
    val hideNavTabs: Boolean = false,
    /** Debug builds only: draws the 4dp grid and keylines over the app. */
    val designGrid: Boolean = false,
)

@Singleton
class ThemePreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("lw_themeMode")
        val ACCENT_COLOR = stringPreferencesKey("lw_accent")
        val ACCENT_MODE = stringPreferencesKey("lw_accentMode")
        val AMOLED = booleanPreferencesKey("lw_amoled")
        val LIQUID_GLASS = booleanPreferencesKey("lw_liquidGlass")
        val MD3_NAV_BAR = booleanPreferencesKey("lw_md3NavBar")
        val HIDE_NAV_TABS = booleanPreferencesKey("lw_hideNavTabs")
        val DESIGN_GRID = booleanPreferencesKey("tuned_design_grid")
    }

    val prefs: Flow<ThemePrefs> = dataStore.data
        .recoverPreferences("ThemePreferences")
        .map { p ->
            ThemePrefs(
                themeMode = ThemeMode.fromStorage(p.readSafely(Keys.THEME_MODE)),
                accentColor = p.readSafely(Keys.ACCENT_COLOR).withoutSignatureLime(),
                accentMode = AccentMode.fromStorage(p.readSafely(Keys.ACCENT_MODE)),
                amoled = p.readSafely(Keys.AMOLED) ?: false,
                liquidGlass = p.readSafely(Keys.LIQUID_GLASS) ?: false,
                md3NavBar = p.readSafely(Keys.MD3_NAV_BAR) ?: false,
                hideNavTabs = p.readSafely(Keys.HIDE_NAV_TABS) ?: false,
                designGrid = p.readSafely(Keys.DESIGN_GRID) ?: false,
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.storageValue }
    }

    suspend fun setManualAccent(color: String) {
        dataStore.edit {
            it[Keys.ACCENT_COLOR] = color
            it[Keys.ACCENT_MODE] = AccentMode.MANUAL.storageValue
        }
    }

    suspend fun setMode(mode: AccentMode) {
        dataStore.edit {
            it[Keys.ACCENT_MODE] = mode.storageValue
        }
    }

    suspend fun setAmoled(enabled: Boolean) {
        dataStore.edit { it[Keys.AMOLED] = enabled }
    }

    suspend fun setLiquidGlass(enabled: Boolean) {
        dataStore.edit { it[Keys.LIQUID_GLASS] = enabled }
    }

    suspend fun setMd3NavBar(enabled: Boolean) {
        dataStore.edit { it[Keys.MD3_NAV_BAR] = enabled }
    }

    suspend fun setDesignGrid(enabled: Boolean) {
        dataStore.edit { it[Keys.DESIGN_GRID] = enabled }
    }

    suspend fun setHideNavTabs(enabled: Boolean) {
        dataStore.edit { it[Keys.HIDE_NAV_TABS] = enabled }
    }
}

/**
 * Tuned's removed Signature Theme saved its lime (#C9FB00) as the manual
 * accent. Reading that value back as Tuned's default retires it without a
 * data migration; any other stored accent is kept.
 */
private const val SIGNATURE_LIME = "#C9FB00"

private fun String?.withoutSignatureLime(): String =
    this?.takeUnless { it.equals(SIGNATURE_LIME, ignoreCase = true) } ?: TunedPalette.DEFAULT_ACCENT_HEX

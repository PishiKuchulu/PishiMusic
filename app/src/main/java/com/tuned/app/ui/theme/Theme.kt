package com.tuned.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.tuned.app.data.local.AccentMode
import com.tuned.app.data.repository.ThemeUiState

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.staticCompositionLocalOf
import com.tuned.app.data.local.ThemeMode
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

/** Shared composition local indicating whether the active theme is dark mode. */
val LocalIsDarkTheme = staticCompositionLocalOf { true }
/** Shared composition local indicating whether bottom navigation tabs are hidden. */
val LocalHideNavTabs = staticCompositionLocalOf { false }

/**
 * Wraps the whole app. Supports System Default, Light, and Dark modes.
 * Kyant0 Backdrop captures underlying content into a hardware-accelerated layer
 * shared app-wide; screens add their own sibling sources for local content
 * (nav bar, player, headers).
 */
@Composable
fun TunedTheme(
    themeState: ThemeUiState,
    content: @Composable () -> Unit,
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeState.themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    // Dynamic Color ON (S+): use the live wallpaper Monet scheme instead of the
    // default manual accent or monochrome. System dynamic auto-tracks wallpaper
    // changes, so no manual refresh is needed. Now-playing artwork wins when it
    // is active; pre-S falls back to the repository's wallpaper-seeded scheme.
    val context = LocalContext.current
    val useSystemDynamic = themeState.mode == AccentMode.DYNAMIC &&
        !themeState.isNowPlayingThemed &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    // The wallpaper only supplies the accent; neutrals stay Tuned's so every
    // accent source produces the same surfaces and text contrast.
    val systemDark = if (useSystemDynamic) {
        TunedPalette.toMaterial(TunedPalette.dark(dynamicDarkColorScheme(context).primary, themeState.amoled))
    } else null
    val systemLight = if (useSystemDynamic) {
        TunedPalette.toMaterial(TunedPalette.light(dynamicLightColorScheme(context).primary))
    } else null
    val activeColorScheme = when {
        systemDark != null && isDark -> systemDark
        systemLight != null && !isDark -> systemLight
        isDark -> themeState.darkColorScheme
        else -> themeState.lightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            runCatching {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    window.isStatusBarContrastEnforced = false
                    window.isNavigationBarContrastEnforced = false
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = activeColorScheme.withAnimatedAccent(),
        typography = if (themeState.useCustomFont) TunedTypography else SystemTypography,
        shapes = TunedShapes,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            SideEffect { AppType.useTunedFont = themeState.useCustomFont }
            CompositionLocalProvider(
                LocalLiquidGlass provides themeState.liquidGlass,
                LocalMd3NavBar provides themeState.md3NavBar,
                LocalHideNavTabs provides themeState.hideNavTabs,
                LocalIsDarkTheme provides isDark,
            ) {
                // The Box is always present so toggling the overlay never
                // rebuilds the app tree (which would reset navigation state).
                Box(Modifier.fillMaxSize()) {
                    content()
                    if (themeState.designGrid) DesignGridOverlay()
                }
            }
        }
    }
}

/**
 * Crossfades the accent roles when the accent changes (a new track's
 * artwork, a wallpaper change) instead of jumping (plan, motion inventory
 * #17). Neutral roles switch instantly; they only change with light/dark.
 */
@Composable
private fun ColorScheme.withAnimatedAccent(): ColorScheme {
    val spec = rememberMotion().slow<Color>()
    val primary by animateColorAsState(primary, spec, label = "primary")
    val onPrimary by animateColorAsState(onPrimary, spec, label = "onPrimary")
    val primaryContainer by animateColorAsState(primaryContainer, spec, label = "primaryContainer")
    val tertiary by animateColorAsState(tertiary, spec, label = "tertiary")
    val onTertiary by animateColorAsState(onTertiary, spec, label = "onTertiary")
    val inversePrimary by animateColorAsState(inversePrimary, spec, label = "inversePrimary")
    return copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        inversePrimary = inversePrimary,
    )
}

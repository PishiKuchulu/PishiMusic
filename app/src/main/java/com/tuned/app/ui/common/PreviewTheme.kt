package com.tuned.app.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.tuned.app.ui.theme.TunedShapes
import com.tuned.app.ui.theme.TunedTypography
import com.tuned.app.ui.theme.TunedPalette

/**
 * The Tuned theme without any app state, for previews. Real screens use
 * TunedTheme, which builds the same palette from the user's settings.
 */
@Composable
internal fun PreviewTheme(dark: Boolean = true, content: @Composable () -> Unit) {
    val colors = if (dark) TunedPalette.dark(TunedPalette.DEFAULT_ACCENT_HEX) else TunedPalette.light(TunedPalette.DEFAULT_ACCENT_HEX)
    MaterialTheme(
        colorScheme = TunedPalette.toMaterial(colors),
        typography = TunedTypography,
        shapes = TunedShapes,
    ) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box { content() }
        }
    }
}

/**
 * The preview matrix every component is checked against: dark and light,
 * the smallest common phone width, a large font scale and a tablet width.
 */
@Preview(name = "Dark 360", widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Light 360", widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark 360 font 1.3", widthDp = 360, fontScale = 1.3f, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Dark 600", widthDp = 600, uiMode = Configuration.UI_MODE_NIGHT_YES)
internal annotation class ComponentPreviews

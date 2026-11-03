package com.tuned.app.ui.settings

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import com.tuned.app.R
import com.tuned.app.data.local.AccentMode
import com.tuned.app.ui.common.adaptiveContentWidth
import com.tuned.app.ui.theme.ExpressivePillShape

internal data class AccentPreset(val name: String, val hex: String)

internal val ACCENT_PRESETS = listOf(
    AccentPreset("Signal", "#FF6A3D"),
    AccentPreset("Crimson", "#E03030"),
    AccentPreset("Violet", "#7C4DFF"),
    AccentPreset("Ocean", "#2196C6"),
    AccentPreset("Sage", "#6B9E6B"),
    AccentPreset("Amber", "#E0A030"),
    AccentPreset("Rose", "#E0507A"),
)

@Composable
internal fun AccentPresetGrid(
    currentMode: AccentMode,
    selectedHex: String?,
    onPickPreset: (String) -> Unit,
    onPickMono: () -> Unit,
    onPickCustom: () -> Unit,
) {
    // A preset only reads as "selected" while the user is actually in
    // manual mode — Dynamic/Monochrome shouldn't light up whichever preset
    // happens to hex-match by coincidence.
    fun isPresetSelected(hex: String) =
        currentMode == AccentMode.MANUAL && selectedHex?.equals(hex, ignoreCase = true) == true
    val customSelected = currentMode == AccentMode.MANUAL &&
        selectedHex != null &&
        ACCENT_PRESETS.none { it.hex.equals(selectedHex, ignoreCase = true) }
    val monoSelected = currentMode == AccentMode.MONOCHROME

    // One unified 4-column tile grid — six color presets plus Mono and
    // Custom as tiles of their own, not a separate row of pill buttons.
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
        ACCENT_PRESETS.take(4).forEach { preset ->
            ColorTile(
                label = preset.name,
                selected = isPresetSelected(preset.hex),
                modifier = Modifier.weight(1f),
                onClick = { onPickPreset(preset.hex) },
            ) {
                PaletteTilePreview(preset.hex)
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
        ACCENT_PRESETS.drop(4).forEach { preset ->
            ColorTile(
                label = preset.name,
                selected = isPresetSelected(preset.hex),
                modifier = Modifier.weight(1f),
                onClick = { onPickPreset(preset.hex) },
            ) {
                PaletteTilePreview(preset.hex)
            }
        }
        ColorTile(
            label = "Mono",
            selected = monoSelected,
            modifier = Modifier.weight(1f),
            onClick = onPickMono,
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Contrast,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        ColorTile(
            label = stringResource(R.string.set_eq_custom),
            selected = customSelected,
            modifier = Modifier.weight(1f),
            onClick = onPickCustom,
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.sweepGradient(
                            listOf(Color(0xFFE03030), Color(0xFFE0A030), Color(0xFF6B9E6B), Color(0xFF2196C6), Color(0xFF7C4DFF), Color(0xFFE03030)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Colorize,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

/**
 * Four coordinated shades derived from one preset hex via HSV — Dark,
 * Medium, Light, and a punchier Accent tone — used to render a real
 * multi-tone palette preview inside each Accent tile instead of one flat
 * swatch. Order returned: [dark, medium, light, accent].
 *
 * Tuned for a muted, Material You / Monet feel rather than raw HSV
 * vibrance: saturation is capped well below 100% even for the "accent"
 * shade (Monet's HCT-derived tonal palettes rarely reach full chroma —
 * that's what read as neon here), and the value range is narrower so
 * "dark" and "light" stay closer to the preset's own character instead of
 * swinging to near-black/near-white extremes.
 */
internal fun accentShades(hex: String): List<Color> {
    val argb = android.graphics.Color.parseColor(hex)
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(argb, hsv)
    val (h, s, v) = hsv
    val mutedBase = s * 0.72f // the single biggest lever for "less neon"

    fun shade(saturation: Float, value: Float): Color {
        val arr = floatArrayOf(h, saturation.coerceIn(0f, 0.82f), value.coerceIn(0.2f, 0.92f))
        return Color(android.graphics.Color.HSVToColor(arr))
    }

    return listOf(
        shade(mutedBase.coerceAtLeast(0.42f), v * 0.62f), // dark
        shade(mutedBase, v * 0.80f), // medium — closest to the preset's own tone
        shade((mutedBase * 0.55f), (v + (1f - v) * 0.45f).coerceAtLeast(0.78f)), // light
        shade((mutedBase * 1.15f), (v * 0.95f)), // accent — a touch richer, never maxed out
    )
}

/** Renders a preset's four shades as a 2x2 block grid filling the tile,
 *  so selecting a preset previews its whole coordinated palette rather
 *  than one flat color. */
@Composable
internal fun PaletteTilePreview(hex: String) {
    val shades = remember(hex) { accentShades(hex) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.weight(1f).fillMaxWidth()) {
            Box(Modifier.weight(1f).fillMaxHeight().background(shades[0]))
            Box(Modifier.weight(1f).fillMaxHeight().background(shades[1]))
        }
        Row(Modifier.weight(1f).fillMaxWidth()) {
            Box(Modifier.weight(1f).fillMaxHeight().background(shades[2]))
            Box(Modifier.weight(1f).fillMaxHeight().background(shades[3]))
        }
    }
}

/**
 * One expressive accent tile: a real elevated square swatch (Modifier.shadow
 * — a true drop shadow, not Card's tonal-elevation color blend, which would
 * otherwise wash out the exact color a swatch is supposed to preview),
 * a spring scale/elevation lift on selection, a genuine ripple on tap, and
 * an animated check badge. [content] draws the tile's fill — a flat color
 * for presets, an icon-on-surface treatment for Mono/Custom.
 */
@Composable
internal fun ColorTile(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable (Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else if (selected) 1.04f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "tileScale",
    )
    val elevation by animateDpAsState(
        targetValue = if (selected) 8.dp else 2.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tileElevation",
    )
    val borderColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "tileBorder",
    )
    val tileShape = RoundedCornerShape(20.dp)

    Column(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                // Real shadow, not tonal elevation — keeps every tile's
                // color a true, undistorted preview of the accent it
                // represents (see GenerateScreen's ModeCard fix for why
                // Card's own elevation param is the wrong tool for this).
                .shadow(elevation = elevation, shape = tileShape, clip = false)
                .clip(tileShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick,
                )
                .border(2.5.dp, borderColor, tileShape),
            contentAlignment = Alignment.Center,
        ) {
            content(selected)
            androidx.compose.animation.AnimatedVisibility(
                visible = selected,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(),
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
            ) {
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ColorWheelSheet(onDismiss: () -> Unit, onApply: (Color) -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    var hue by remember { mutableStateOf(4f) }
    var saturation by remember { mutableStateOf(0.75f) }
    var lightness by remember { mutableStateOf(0.5f) }
    val previewColor = Color.hsl(hue, saturation, lightness)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveContentWidth(maxWidth = 560.dp)
                .align(Alignment.CenterHorizontally)
                .padding(20.dp),
        ) {
            Text(stringResource(R.string.set_color_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(previewColor),
            )
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.set_hue), style = MaterialTheme.typography.labelLarge)
            Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f)
            Text(stringResource(R.string.set_sat), style = MaterialTheme.typography.labelLarge)
            Slider(value = saturation, onValueChange = { saturation = it }, valueRange = 0f..1f)
            Text(stringResource(R.string.set_light), style = MaterialTheme.typography.labelLarge)
            Slider(value = lightness, onValueChange = { lightness = it }, valueRange = 0.15f..0.85f)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDismiss, shape = ExpressivePillShape, modifier = Modifier.weight(1f).height(48.dp)) { Text(stringResource(R.string.common_cancel)) }
                Button(onClick = { onApply(previewColor) }, shape = ExpressivePillShape, modifier = Modifier.weight(1f).height(48.dp)) { Text(stringResource(R.string.common_apply)) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// -- Experimental 15-band equalizer (Settings → Experimental → Equalizer) --

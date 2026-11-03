package com.tuned.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuned.app.R
import com.tuned.app.playback.MusicPlayer
import com.tuned.app.playback.MusicPlayerState
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private const val MIN_RATE = 0.50f
private const val MAX_RATE = 1.50f

/** Rounds to 0.01 and catches normal speed, which is hard to land on by hand. */
private fun snapRate(value: Float): Float {
    val rounded = ((value * 100).roundToInt() / 100f).coerceIn(MIN_RATE, MAX_RATE)
    return if (abs(rounded - 1f) <= 0.015f) 1f else rounded
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AudioControlsSheet(
    player: MusicPlayer,
    state: MusicPlayerState,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = LocalHapticFeedback.current
    // Link state is derived from the engine state until the user overrides it:
    // the old remember(initial) snapshot went stale when speed/pitch changed
    // from anywhere else (presets, another surface) while the sheet was open.
    var linkOverride by remember { mutableStateOf<Boolean?>(null) }
    val isLinked = linkOverride ?: (state.speed == state.pitch)
    var currentSpeed by remember(state.speed) { mutableFloatStateOf(state.speed) }
    var currentPitch by remember(state.pitch) { mutableFloatStateOf(state.pitch) }
    // Speed and pitch change the samples, so Bit-Perfect turns them off. The
    // engine exposes that gate under the legacy name `vocalsAvailable` — it is
    // NOT about vocals; it is exactly "not bit-perfect".
    val rateAvailable = state.vocalsAvailable

    var showEqualizer by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var showAutoEq by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    if (showAutoEq) {
        com.tuned.app.ui.settings.autoeq.AutoEqDialog(onDismiss = { showAutoEq = false })
    }
    if (showEqualizer) {
        val settings: com.tuned.app.ui.settings.SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
        val eq by settings.equalizer.collectAsStateWithLifecycle()
        com.tuned.app.ui.settings.EqualizerSheet(
            eq = eq,
            onDismiss = { showEqualizer = false },
            onSetEnabled = settings::setEqualizerEnabled,
            onPickPreset = settings::applyEqPreset,
            onBandPreview = settings::previewEqBandGain,
            onBandChange = settings::setEqBandGain,
        )
    }

    fun apply(speed: Float, pitch: Float) {
        currentSpeed = speed
        currentPitch = pitch
        player.setPlaybackParameters(speed, pitch)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp),
            ) {}
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.audio_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_close))
                }
            }

            SheetCard {
                val castVolume by player.castVolume.collectAsStateWithLifecycle()
                Text(
                    text = stringResource(if (castVolume != null) R.string.audio_cast_volume else R.string.audio_volume),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                val cast = castVolume
                if (cast != null) {
                    PlainSlider(value = cast, range = 0f..1f, enabled = true, onChange = player::setCastVolume)
                } else {
                    VolumeSlider(
                        isTranslucent = false,
                        primaryColor = MaterialTheme.colorScheme.primary,
                        fullWidth = true,
                    )
                }
            }

            SheetCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.audio_speed_pitch),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    SheetChip(
                        label = stringResource(if (isLinked) R.string.audio_linked else R.string.audio_separate),
                        selected = false,
                        enabled = rateAvailable,
                        leadingIcon = {
                            Icon(
                                imageVector = if (isLinked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                        },
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val nowLinked = !isLinked
                            linkOverride = nowLinked
                            if (nowLinked) apply(currentSpeed, currentSpeed)
                        },
                    )
                }

                if (!rateAvailable) {
                    Text(
                        text = stringResource(R.string.audio_bit_perfect_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                RateSlider(
                    label = stringResource(if (isLinked) R.string.audio_speed_pitch else R.string.audio_speed),
                    value = currentSpeed,
                    enabled = rateAvailable,
                    onChange = { if (isLinked) apply(it, it) else apply(it, currentPitch) },
                )
                if (!isLinked) {
                    RateSlider(
                        label = stringResource(R.string.audio_pitch),
                        value = currentPitch,
                        enabled = rateAvailable,
                        onChange = { apply(currentSpeed, it) },
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        R.string.speed_normal to 1f,
                        R.string.speed_slowed to 0.85f,
                        R.string.speed_deep_slowed to 0.75f,
                        R.string.speed_nightcore to 1.15f,
                        R.string.speed_sped_up to 1.25f,
                    ).forEach { (label, rate) ->
                        SheetChip(
                            label = stringResource(label),
                            selected = currentSpeed == rate && currentPitch == rate,
                            enabled = rateAvailable,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                linkOverride = true
                                apply(rate, rate)
                            },
                        )
                    }
                }
            }

            // The equaliser and AutoEQ run together in the audio engine: headphone correction
            // first, your own taste on top.
            SheetCard {
                SheetLink(stringResource(R.string.settings_equalizer), stringResource(R.string.audio_eq_sub)) { showEqualizer = true }
                SheetLink(stringResource(R.string.autoeq_title), stringResource(R.string.audio_autoeq_sub)) { showAutoEq = true }
            }

            // Apple's Music Haptics; phones with no vibrator don't get the toggle.
            if (player.canVibrate) {
                SheetCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.music_haptics),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                stringResource(R.string.music_haptics_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = state.musicHapticsEnabled,
                            onCheckedChange = { player.toggleMusicHaptics() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun SheetChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    FilterChip(
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        shape = RoundedCornerShape(50),
        leadingIcon = leadingIcon,
        label = {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            labelColor = MaterialTheme.colorScheme.onSurface,
            iconColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = null,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RateSlider(
    label: String,
    value: Float,
    enabled: Boolean,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = String.format(Locale.US, "%.2fx", value),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        PlainSlider(value = value, range = MIN_RATE..MAX_RATE, enabled = enabled, onChange = { onChange(snapRate(it)) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlainSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onChange: (Float) -> Unit,
) {
    val trackColors = SliderDefaults.colors(
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )
    Slider(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        valueRange = range,
        colors = trackColors,
        thumb = {
            Box(
                Modifier
                    .size(18.dp)
                    .background(
                        if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        CircleShape,
                    ),
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                modifier = Modifier.height(6.dp),
                enabled = enabled,
                colors = trackColors,
                thumbTrackGapSize = 0.dp,
                trackInsideCornerSize = 3.dp,
                drawStopIndicator = null,
            )
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

/** A row that opens another screen, in the Settings list style. */
@Composable
private fun SheetLink(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

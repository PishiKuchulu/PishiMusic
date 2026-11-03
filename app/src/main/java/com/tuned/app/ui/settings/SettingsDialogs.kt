package com.tuned.app.ui.settings

import com.tuned.app.ui.common.sheetScrollGuard
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import com.tuned.app.data.local.LyricsProvider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.tuned.app.data.lyrics.TRANSLATION_LANGUAGES
import com.tuned.app.data.lyrics.translationLanguageName
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import java.util.Locale
import kotlin.math.roundToInt
import com.tuned.app.R
import com.tuned.app.playback.ClarityPresets
import com.tuned.app.playback.LoudnessMode

/**
 * The Settings pickers' bottom sheet (the quality sheet's look): title, one muted line, then the
 * choices as plain rows or pills. No radio buttons, no Done button; picking closes it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsChoiceSheet(
    title: String,
    description: String?,
    onDismiss: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = {
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(36.dp, 4.dp),
            ) {}
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .sheetScrollGuard()
                .verticalScroll(rememberScrollState(), overscrollEffect = null)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp + com.tuned.app.ui.common.safeDrawingBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            content()
        }
    }
}

/** A choice in [SettingsChoiceSheet]: a plain row; the chosen one gets the soft tint and a check. */
@Composable
internal fun SettingsChoiceRow(title: String, subtitle: String?, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else androidx.compose.ui.graphics.Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (selected) {
                androidx.compose.foundation.layout.Box(
                    Modifier
                        .size(28.dp, 28.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = stringResource(R.string.set_selected_cd),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp, 18.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun LyricsProviderDialog(
    current: LyricsProvider,
    onSelect: (LyricsProvider) -> Unit,
    onDismiss: () -> Unit,
) {
    SettingsChoiceSheet(
        title = stringResource(R.string.settings_lyrics_provider),
        description = stringResource(R.string.settings_lyrics_provider_desc),
        onDismiss = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            LyricsProvider.entries.forEach { provider ->
                SettingsChoiceRow(provider.title, provider.subtitle, provider == current) {
                    onSelect(provider)
                    onDismiss()
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun LyricsAutoHideDialog(
    currentSeconds: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    SettingsChoiceSheet(
        title = stringResource(R.string.set_lyrics_autohide_title),
        description = stringResource(R.string.set_lyrics_autohide_desc),
        onDismiss = onDismiss,
    ) {
        SettingsChoiceRow(stringResource(R.string.set_lyrics_autohide_never), null, currentSeconds == 0) {
            onSelect(0)
            onDismiss()
        }
        // The delays as one row of pills rather than nine stacked rows.
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (seconds in 2..10) {
                val selected = currentSeconds == seconds
                androidx.compose.material3.Surface(
                    onClick = {
                        onSelect(seconds)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(50),
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                ) {
                    Text(
                        "${seconds} s",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun TranslationLanguageDialog(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val locale = remember { Locale.getDefault() }
    val languages = remember { TRANSLATION_LANGUAGES }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_translation_language_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    stringResource(R.string.set_translation_language_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                SettingsRadioOption(
                    label = stringResource(R.string.set_translation_language_app),
                    selected = current.isBlank(),
                    onClick = { onSelect("") },
                )
                for (language in languages) {
                    SettingsRadioOption(
                        label = translationLanguageName(language.code, locale),
                        selected = current.equals(language.code, ignoreCase = true),
                        onClick = { onSelect(language.code) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_done)) }
        },
    )
}

internal fun loudnessModeTitle(context: android.content.Context, mode: LoudnessMode): String = when (mode) {
    LoudnessMode.TRACK -> context.getString(R.string.set_loud_track)
    LoudnessMode.ALBUM -> context.getString(R.string.set_loud_album)
    else -> context.getString(R.string.set_loud_off)
}

internal fun loudnessModeSubtitle(context: android.content.Context, mode: LoudnessMode): String = when (mode) {
    LoudnessMode.TRACK -> context.getString(R.string.set_loud_track_sub)
    LoudnessMode.ALBUM -> context.getString(R.string.set_loud_album_sub)
    else -> context.getString(R.string.set_loud_off_sub)
}

@Composable
internal fun clarityPresetName(preset: com.tuned.app.playback.ClarityPreset): String = stringResource(
    when (preset.key) {
        "speaker" -> R.string.set_preset_speaker
        "headphone" -> R.string.set_preset_headphone
        "dac" -> R.string.set_preset_dac
        else -> R.string.set_preset_reference
    },
)

@Composable
internal fun clarityPresetDescription(preset: com.tuned.app.playback.ClarityPreset): String = stringResource(
    when (preset.key) {
        "speaker" -> R.string.set_preset_speaker_desc
        "headphone" -> R.string.set_preset_headphone_desc
        "dac" -> R.string.set_preset_dac_desc
        else -> R.string.set_preset_reference_desc
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LoudnessModeDialog(
    current: LoudnessMode,
    onSelect: (LoudnessMode) -> Unit,
    preampDb: Float,
    onPreampChange: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_loud_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    stringResource(R.string.set_loud_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                LoudnessMode.entries.forEach { mode ->
                    val selected = mode == current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(mode) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected, onClick = { onSelect(mode) })
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                loudnessModeTitle(LocalContext.current, mode),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            )
                            Text(
                                loudnessModeSubtitle(LocalContext.current, mode),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.autoeq_preamp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        String.format(Locale.US, "%+.1f dB", preampDb),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Slider(
                    value = preampDb,
                    onValueChange = onPreampChange,
                    valueRange = -12f..12f,
                    steps = 47,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_done)) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScrobblePercentDialog(
    currentPercent: Int,
    onPercentChange: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sliderState = remember { androidx.compose.runtime.mutableFloatStateOf(currentPercent.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scrobble threshold") },
        text = {
            Column {
                Text(
                    "How much of a track must play before it is scrobbled to Last.fm.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                Slider(
                    value = sliderState.floatValue,
                    onValueChange = { value ->
                        sliderState.floatValue = value
                        onPercentChange(value.roundToInt())
                    },
                    valueRange = 25f..90f,
                    steps = 64,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "${sliderState.floatValue.roundToInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_done)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClarityPresetDialog(
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_clarity_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "Tunes the Studio Master Clarity chain to the output. Reference is the unmodified shipping curve.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                ClarityPresets.ALL.forEach { preset ->
                    val selected = preset.index == currentIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(preset.index) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected, onClick = { onSelect(preset.index) })
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                clarityPresetName(preset),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            )
                            Text(
                                clarityPresetDescription(preset),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_done)) }
        },
    )
}

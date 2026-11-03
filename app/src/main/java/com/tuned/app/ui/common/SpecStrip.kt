package com.tuned.app.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.tuned.app.R
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.tunedColors
import java.util.Locale

/**
 * What the audio actually is, as one fact in mono: the codec ("FLAC"), or the
 * first known part when the codec is missing. Tapping it opens the signal path,
 * which has the rest.
 */
@Composable
fun SpecStrip(
    codec: String?,
    modifier: Modifier = Modifier,
    bitDepth: Int? = null,
    sampleRateHz: Int? = null,
    bitrateKbps: Int? = null,
    output: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val text = formatSpec(codec, bitDepth, sampleRateHz, bitrateKbps, output)
    if (text.isEmpty()) return
    val label = stringResource(R.string.cmp_audio_details)
    Box(
        modifier
            .heightIn(min = Sizes.touch)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClickLabel = label, onClick = onClick) else Modifier)
            .padding(horizontal = Space.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = AppType.mono, color = MaterialTheme.tunedColors.text2)
    }
}

/**
 * The first known part, in order: codec, bit depth, sample rate, bitrate
 * (lossy audio only), output. Sample rates read the way people say them:
 * 44100 is "44.1 kHz", 96000 is "96 kHz".
 */
internal fun formatSpec(
    codec: String?,
    bitDepth: Int?,
    sampleRateHz: Int?,
    bitrateKbps: Int?,
    output: String?,
): String = buildList {
    codec?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it.uppercase(Locale.ROOT)) }
    if (bitDepth != null && bitDepth > 0) add("$bitDepth-bit")
    if (sampleRateHz != null && sampleRateHz > 0) add(formatSampleRate(sampleRateHz))
    if (bitDepth == null && bitrateKbps != null && bitrateKbps > 0) add("$bitrateKbps kbps")
    output?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
}.firstOrNull().orEmpty()

internal fun formatSampleRate(hz: Int): String {
    val khz = hz / 1000.0
    val text = if (hz % 1000 == 0) (hz / 1000).toString() else String.format(Locale.ROOT, "%.1f", khz)
    return "$text kHz"
}

@ComponentPreviews
@Composable
private fun SpecStripPreview() = PreviewTheme { SpecStripSample() }

/** Sample states, shared by the preview and the in-app component gallery. */
@Composable
internal fun SpecStripSample() {
    Column(Modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        SpecStrip(codec = "flac", bitDepth = 24, sampleRateHz = 96_000, output = "USB DAC", onClick = {})
        SpecStrip(codec = "FLAC", bitDepth = 16, sampleRateHz = 44_100)
        SpecStrip(codec = "Opus", bitrateKbps = 256, sampleRateHz = 48_000, output = "Bluetooth")
    }
}

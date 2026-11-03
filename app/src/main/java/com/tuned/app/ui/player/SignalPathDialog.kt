package com.tuned.app.ui.player

import com.tuned.app.ui.common.blurSheetContainerColor
import com.tuned.app.ui.common.fadeTopEdge
import com.tuned.app.ui.common.sheetScrollGuard
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import com.tuned.app.ui.common.EdgeToEdgeDialogWindow
import com.tuned.app.ui.common.ExpressiveGroup
import com.tuned.app.ui.common.GroupPosition
import com.tuned.app.ui.common.adaptiveContentWidth
import com.tuned.app.ui.common.groupShape
import com.tuned.app.ui.common.safeDrawingBottomPadding
import com.tuned.app.ui.theme.AppType
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tuned.app.R
import com.tuned.app.playback.InternalDirectAvailability
import com.tuned.app.playback.InternalDirectStream
import com.tuned.app.playback.InternalRoute
import com.tuned.app.playback.SignalPathReport
import com.tuned.app.playback.internalFailureDetail
import com.tuned.app.ui.theme.LiquidGlassPreset
import com.tuned.app.ui.theme.LocalLiquidGlass
import com.tuned.app.ui.theme.LocalLiquidGlassOverlayBackdrop
import java.util.Locale

/**
 * Signal path, opened from the format chip under the scrub bar. A sheet in the
 * song menu's style: what is happening in one sentence first, then the chain
 * as one grouped card (tap a stage for its details), the two scores with what
 * they mean, and the raw engineering detail folded under "Technical details".
 * Every row is a measured check; the bit-perfect verdict appears only when all
 * of them pass. Drift refreshes live while playing (1 Hz report ticks).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignalPathDialog(
    report: SignalPathReport,
    needsUsbPermission: Boolean,
    onRequestUsbAccess: () -> Unit,
    onDismiss: () -> Unit,
    internalAvailability: InternalDirectAvailability? = null,
    internalEnabled: Boolean = false,
    internalUnderruns: Long? = null,
    usbDiagnostic: (() -> String)? = null,
    /** 432Hz tuning state (from settings), shown in the DSP stage; the report itself carries no tuning field. */
    tuning432Enabled: Boolean = false,
    /** Real loudness-normalization preamp (dB) when normalization is active; null = no preamp row is shown. */
    loudnessPreampDb: Float? = null,
) {
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.85f
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = blurSheetContainerColor(),
        scrimColor = Color.Black.copy(alpha = 0.32f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
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
        EdgeToEdgeDialogWindow(visible = sheetState.targetValue != androidx.compose.material3.SheetValue.Hidden)
        val sheetScroll = rememberScrollState()
        val khz = { hz: Int -> (hz / 1000.0).let { if (it % 1.0 == 0.0) "${it.toInt()} kHz" else "$it kHz" } }
        val srcRate = khz(report.sourceRateHz ?: report.appRateHz.takeIf { it > 0 } ?: 44100)
        // Null when the stream doesn't report one (Opus and other lossy codecs have no bit depth);
        // then no depth is shown rather than a made-up 16-bit.
        val srcBits = report.sourceBitDepth
        val srcCodec = codecName(report.sourceLabel)
        val dspRate = khz(report.appRateHz)
        val outRate = khz(report.outputRateHz)
        val deviceName = report.dacName ?: if (report.internalDirectActive) "Internal Hi-Res DAC" else "Android default output"
        var openStage by remember { mutableStateOf<Int?>(null) }
        var technicalOpen by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveContentWidth(maxWidth = 600.dp)
                .align(Alignment.CenterHorizontally)
                .heightIn(max = maxHeight)
                .fadeTopEdge(sheetScroll)
                .sheetScrollGuard()
                .padding(horizontal = 14.dp)
                // No stretch at the ends: the list stops where it stops.
                .verticalScroll(sheetScroll, overscrollEffect = null)
                .padding(bottom = 24.dp + safeDrawingBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 2.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.signal_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                VerdictPill(report = report)
            }

            // 1. What is happening, in one sentence.
            Card(
                shape = groupShape(GroupPosition.SINGLE),
                colors = CardDefaults.cardColors(
                    containerColor = if (report.bitPerfect) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (srcBits != null) {
                            stringResource(R.string.signal_summary, srcCodec, srcRate, srcBits, outputName(report.outputEngineName), outRate, report.outputBits)
                        } else {
                            stringResource(R.string.signal_summary_no_depth, srcCodec, srcRate, outputName(report.outputEngineName), outRate, report.outputBits)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val verdict = asSentences(verdictText(report))
                    if (verdict.isNotBlank()) {
                        Text(
                            verdict,
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                report.bitPerfect -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }

            // 2. The chain, one grouped card; tap a stage for its details.
            val stages: List<SignalStage> = listOf(
                SignalStage(Icons.Filled.MusicNote, stringResource(R.string.sig_node_source), srcCodec) {
                    StageDetail(stringResource(R.string.sig_detail_format), srcCodec)
                    StageDetail(stringResource(R.string.sig_detail_rate), srcRate)
                    if (srcBits != null) StageDetail(stringResource(R.string.sig_detail_depth), "$srcBits-bit")
                },
                SignalStage(Icons.Filled.GraphicEq, stringResource(R.string.sig_node_dsp), if (report.dspScore == "—") stringResource(R.string.sig_dsp_off) else stringResource(R.string.sig_dsp_on)) {
                    StageDetail(stringResource(R.string.sig_detail_rate), dspRate)
                    StageDetail(stringResource(R.string.sig_detail_chain), report.dspSummary)
                    StageDetail(stringResource(R.string.sig_eq), if (report.dspScore == "—") stringResource(R.string.sig_eq_off) else stringResource(R.string.sig_eq_mode))
                    StageDetail(
                        stringResource(R.string.settings_tuning_432),
                        stringResource(if (tuning432Enabled) R.string.sig_dsp_on else R.string.sig_dsp_off),
                    )
                    StageDetail(
                        stringResource(R.string.sig_crossfade),
                        if (report.checks.any { it.detailRes == R.string.signal_detail_dsp_crossfade }) stringResource(R.string.sig_xfade_active) else stringResource(R.string.sig_xfade_off),
                    )
                },
                SignalStage(Icons.Filled.VolumeUp, stringResource(R.string.sig_node_volume), report.volumeMode) {
                    StageDetail(stringResource(R.string.sig_detail_level), report.volumeDetail)
                    // No preamp row unless loudness normalization is actually applying one:
                    // the report carries no preamp measurement, and a fixed "0.0 dB" was fabricated.
                    if (loudnessPreampDb != null) {
                        StageDetail(
                            stringResource(R.string.sig_preamp),
                            String.format(Locale.ROOT, "%+.1f dB", loudnessPreampDb),
                        )
                    }
                },
                SignalStage(Icons.Filled.Speaker, stringResource(R.string.sig_node_output), outRate) {
                    StageDetail(stringResource(R.string.sig_detail_engine), report.outputEngineName)
                    StageDetail(stringResource(R.string.sig_detail_rate), outRate)
                    StageDetail(stringResource(R.string.sig_detail_depth), "${report.outputBits}-bit")
                    StageDetail(stringResource(R.string.sig_latency), "${report.outputLatencyMs} ms")
                },
                SignalStage(if (report.dacName != null) Icons.Filled.Headphones else Icons.Filled.PhoneAndroid, stringResource(R.string.sig_node_device), deviceName) {
                    StageDetail(stringResource(R.string.sig_detail_device), deviceName)
                    StageDetail(stringResource(R.string.sig_detail_rate), outRate)
                    StageDetail(stringResource(R.string.sig_detail_depth), "${report.outputBits}-bit")
                },
            )
            ExpressiveGroup(rowCount = stages.size) { index, position ->
                StageRow(
                    stage = stages[index],
                    position = position,
                    open = openStage == index,
                    onClick = { openStage = if (openStage == index) null else index },
                )
            }

            // 3. The fidelity score, out of 100, with what it means. (There was a second "Processing"
            // score, but it was a fixed 21 whenever any processing ran; the Processing row says that.)
            ScoreCard(
                label = stringResource(R.string.sig_score_fidelity),
                value = "${report.fidelityScore}",
                meaning = stringResource(R.string.sig_score_fidelity_meaning),
                highlight = report.bitPerfect,
                modifier = Modifier.fillMaxWidth(),
            )

            if (needsUsbPermission) {
                Surface(
                    onClick = onRequestUsbAccess,
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(R.string.signal_grant_usb),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 14.dp),
                    )
                }
            }

            // 4. Engineering detail, folded away.
            Card(
                onClick = { technicalOpen = !technicalOpen },
                shape = groupShape(GroupPosition.SINGLE),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.sig_technical),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            if (technicalOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    androidx.compose.animation.AnimatedVisibility(visible = technicalOpen) {
                        Column(Modifier.padding(top = 8.dp)) {
                            val measuring = stringResource(R.string.signal_measuring)
                            HealthRow(
                                stringResource(R.string.signal_drift),
                                report.driftPpm?.let { String.format(Locale.ROOT, "%+.1f PPM", it) }
                                    ?: if (report.isPlaying) measuring else "—",
                            )
                            val streamState = if (report.isPlaying) stringResource(R.string.signal_playing) else stringResource(R.string.signal_idle)
                            // Exclusive usbdevfs never uses AudioTrack. Gold is a stricter
                            // verdict (clock + unity/FU + DSP off); do not call exclusive
                            // output "Shared AudioTrack" just because a metadata check failed.
                            val routeTrack = when {
                                report.internalDirectActive -> stringResource(R.string.signal_internal_track)
                                report.usbExclusiveActive || report.bitPerfect -> stringResource(R.string.signal_direct_track)
                                else -> stringResource(R.string.signal_shared_track)
                            }
                            HealthRow(
                                stringResource(R.string.signal_stream),
                                streamState,
                            )
                            HealthRow(stringResource(R.string.signal_glitches), report.glitchCount.toString())
                            // No buffer-count row: the report carries no measured buffer size, and the
                            // old row derived one from a 5%-of-rate formula. Latency is the report's own
                            // figure (the old row doubled it, contradicting the Output stage above).
                            HealthRow(stringResource(R.string.sig_latency), "${report.outputLatencyMs} ms")
                            HealthRow(stringResource(R.string.sig_flags), report.outputFlags.joinToString(" "), mono = true)
                            if (report.usbExclusiveActive) {
                                HealthRow(stringResource(R.string.signal_usb_interface), stringResource(R.string.signal_usb_interface_value))
                                HealthRow(stringResource(R.string.signal_android_mixer), stringResource(R.string.signal_android_mixer_bypassed))
                                val epRate = if (report.outputRateHz > 0) report.outputRateHz else report.appRateHz
                                HealthRow(
                                    stringResource(R.string.signal_internal_endpoint),
                                    if (epRate > 0) stringResource(R.string.signal_internal_endpoint_value, epRate, "${report.outputBits}-bit PCM") else "—",
                                )
                            } else {
                                val session = report.internalSession
                                val route = session?.route ?: internalAvailability?.route ?: InternalRoute.UNKNOWN
                                val routeName = session?.routeName ?: internalAvailability?.routeName.orEmpty()
                                HealthRow(
                                    stringResource(R.string.signal_internal_route),
                                    routeName.ifBlank { routeLabel(route) },
                                )
                                HealthRow(
                                    stringResource(R.string.signal_internal_status),
                                    when {
                                        report.internalDirectActive -> stringResource(R.string.internal_reason_active)
                                        !internalEnabled -> stringResource(R.string.signal_internal_off)
                                        internalAvailability != null -> stringResource(internalFailureDetail(internalAvailability.reason))
                                        else -> "—"
                                    },
                                )
                                val probe = internalAvailability?.probe
                                HealthRow(
                                    stringResource(R.string.signal_internal_endpoint),
                                    when {
                                        session != null -> stringResource(
                                            R.string.signal_internal_endpoint_value,
                                            session.deviceRateHz,
                                            InternalDirectStream.labelOf(session.deviceEncoding),
                                        )
                                        probe != null && probe.usable -> stringResource(
                                            R.string.signal_internal_endpoint_value,
                                            probe.sampleRateHz,
                                            InternalDirectStream.labelOf(probe.encoding),
                                        )
                                        probe != null && internalAvailability != null ->
                                            stringResource(internalFailureDetail(internalAvailability.reason))
                                        else -> stringResource(R.string.signal_internal_not_checked)
                                    },
                                )
                                if (internalUnderruns != null) {
                                    HealthRow(stringResource(R.string.signal_internal_underruns), internalUnderruns.toString())
                                }
                            }
                            if (usbDiagnostic != null) {
                                val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
                                val copiedLabel = stringResource(R.string.signal_usb_diag_copied)
                                var copied by remember { mutableStateOf(false) }
                                TextButton(
                                    onClick = {
                                        // The raw diagnostic can carry device serials and long hex
                                        // identifiers; share a redacted copy, never the verbatim dump.
                                        clipboard.setText(androidx.compose.ui.text.AnnotatedString(redactDiagnostic(usbDiagnostic())))
                                        copied = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(if (copied) copiedLabel else stringResource(R.string.signal_usb_diag_copy))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One stage of the chain: its icon, name, the key value, and its details. */
private class SignalStage(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val value: String,
    val details: @Composable () -> Unit,
)

@Composable
private fun StageRow(stage: SignalStage, position: GroupPosition, open: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = groupShape(position),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(stage.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(14.dp))
                Text(stage.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(12.dp))
                Text(
                    stage.value,
                    style = AppType.mono,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            androidx.compose.animation.AnimatedVisibility(visible = open) {
                // Labels start under the stage title (40dp badge + 14dp gap); values end on the same
                // edge as the stage's own value.
                Column(Modifier.padding(start = 54.dp, top = 8.dp)) { stage.details() }
            }
        }
    }
}

@Composable
private fun StageDetail(label: String, value: String) = HealthRow(label, value)

/** A score with what it means, as a grouped card. */
@Composable
private fun ScoreCard(label: String, value: String, meaning: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Card(
        shape = groupShape(GroupPosition.SINGLE),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier,
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    "/100",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp).alignByBaseline(),
                )
            }
            Text(meaning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** "OPUS" → "Opus": codecs that are names, not acronyms, in their usual spelling. */
private fun codecName(label: String): String = when (label.trim().lowercase()) {
    "opus" -> "Opus"
    "vorbis" -> "Vorbis"
    else -> label
}

/** The output engine as a listener would say it, for the one-line summary. */
private fun outputName(engine: String): String = when (engine) {
    "Android AudioTrack" -> "Android's mixer"
    "Android Bit-Perfect Output" -> "Android's bit-perfect mode"
    "USB Exclusive Output" -> "USB exclusive mode"
    "Direct HAL (AAudio MMAP)" -> "the direct hardware path"
    else -> engine
}

/** Splits "x — y" into two sentences, with a capital after the full stop and one at the end. */
private fun asSentences(text: String): String {
    if (text.isBlank()) return text
    val joined = text.split(" — ", " - ").joinToString(". ") { part ->
        part.trim().replaceFirstChar { it.uppercase() }
    }
    return if (joined.last() in ".!?") joined else "$joined."
}

/** Verdict line: confirmation when perfect, otherwise the first failing check. */
@Composable
private fun verdictText(report: SignalPathReport): String {
    if (report.bitPerfect) return stringResource(R.string.signal_verdict_ok)
    if (report.directToHal && report.parametricEqActive) return stringResource(R.string.signal_verdict_direct_eq)
    if (report.directToHal) return stringResource(R.string.signal_verdict_direct_hal)
    val failing = report.checks.firstOrNull { !it.passed } ?: return ""
    return stringResource(failing.labelRes) +
        "\n" +
        stringResource(failing.detailRes, *failing.detailArgs.toTypedArray()).substringBefore('\n')
}

@Composable
private fun VerdictPill(report: SignalPathReport) {
    val bg = when {
        report.bitPerfect -> MaterialTheme.colorScheme.primary
        report.directToHal -> MaterialTheme.colorScheme.tertiary
        report.clockFallbackResampled -> Color(0xFFE65100).copy(alpha = 0.25f)
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val fg = when {
        report.bitPerfect -> MaterialTheme.colorScheme.onPrimary
        report.directToHal -> MaterialTheme.colorScheme.onTertiary
        report.clockFallbackResampled -> Color(0xFFFFB74D)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val text = when {
        report.bitPerfect -> stringResource(R.string.signal_bit_perfect)
        report.directToHal && report.parametricEqActive -> stringResource(R.string.signal_direct_eq)
        report.directToHal -> stringResource(R.string.signal_direct_hal)
        report.clockFallbackResampled && !report.internalDirectActive -> stringResource(R.string.signal_resampled_dac)
        else -> stringResource(R.string.signal_check_path)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = fg,
        )
    }
}

@Composable
private fun HealthRow(label: String, value: String, mono: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            value,
            style = if (mono) AppType.mono else MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun routeLabel(route: InternalRoute): String = stringResource(
    when (route) {
        InternalRoute.SPEAKER -> R.string.route_speaker
        InternalRoute.WIRED -> R.string.route_wired
        InternalRoute.BLUETOOTH -> R.string.route_bluetooth
        InternalRoute.USB -> R.string.route_usb
        InternalRoute.OTHER -> R.string.route_other
        InternalRoute.UNKNOWN -> R.string.route_unknown
    },
)

/** Masks URLs, long hex identifiers and serial-looking tokens before a diagnostic leaves the app. */
private fun redactDiagnostic(raw: String): String =
    raw
        .replace(Regex("https?://\\S+"), "<url>")
        .replace(Regex("\\b[0-9a-fA-F]{16,}\\b"), "<id>")
        .replace(Regex("(?i)(serial|sn)\\s*[:=]\\s*\\S+"), "$1: <id>")

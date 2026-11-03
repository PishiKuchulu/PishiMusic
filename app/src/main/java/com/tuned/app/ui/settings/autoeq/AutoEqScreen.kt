package com.tuned.app.ui.settings.autoeq

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.PaddingValues
import com.tuned.app.ui.theme.tunedColors
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.Keyline
import com.tuned.app.ui.theme.AppType
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuned.app.R
import com.tuned.app.data.autoeq.AutoEqMeasurement
import com.tuned.app.data.autoeq.AutoEqState
import com.tuned.app.data.autoeq.AutoEqTargets
import com.tuned.app.data.autoeq.PeqFilter
import com.tuned.app.data.autoeq.PeqFilterType
import com.tuned.app.data.autoeq.PeqMath
import com.tuned.app.data.autoeq.PeqSettings
import java.util.Locale
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.roundToInt

/** Full-screen AutoEQ editor, opened from Settings → Audio. */
@Composable
fun AutoEqDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            AutoEqScreen(onBack = onDismiss)
        }
    }
}

/** One-line status for the Settings row. */
@Composable
fun autoEqSummary(viewModel: AutoEqViewModel = hiltViewModel()): String {
    val enabled by viewModel.enabled.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    return when {
        !enabled -> stringResource(R.string.autoeq_row_off)
        state.isCustom -> stringResource(R.string.autoeq_row_custom, state.entry?.name ?: stringResource(R.string.autoeq_manual))
        state.entry != null -> stringResource(R.string.autoeq_row_on, state.entry!!.name)
        else -> stringResource(R.string.autoeq_row_off)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoEqScreen(onBack: () -> Unit, viewModel: AutoEqViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val enabled by viewModel.enabled.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val measurement by viewModel.measurement.collectAsStateWithLifecycle()
    val device by viewModel.currentDevice.collectAsStateWithLifecycle()
    val deviceProfiles by viewModel.deviceProfiles.collectAsStateWithLifecycle()
    val deviceLabels by viewModel.deviceLabels.collectAsStateWithLifecycle()
    val autoPerDevice by viewModel.autoPerDevice.collectAsStateWithLifecycle()
    var searchOpen by remember { mutableStateOf(false) }
    var expandedFilter by remember { mutableStateOf(-1) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { res -> Toast.makeText(context, context.getString(res), Toast.LENGTH_SHORT).show() }
    }

    // The shared header (Home's panel) with the on/off switch as its action; sections below are
    // plain titled blocks on the page, no cards.
    Column(Modifier.fillMaxSize()) {
    com.tuned.app.ui.common.ExpressiveHeader(
        title = stringResource(R.string.autoeq_title),
        onBack = onBack,
        actions = {
            Switch(checked = enabled, onCheckedChange = viewModel::setEnabled)
            Spacer(Modifier.width(Space.md))
        },
    )
    if (busy) Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) { com.tuned.app.ui.common.LoadingSpinner(Modifier.size(28.dp)) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(top = Space.sm, bottom = Space.xl + com.tuned.app.ui.player.LocalMiniPlayerScrollClearance.current),
        verticalArrangement = Arrangement.spacedBy(Space.sm),
    ) {

        item { ProfileCard(state, onSearch = { searchOpen = true }, viewModel = viewModel) }

        item {
            SectionCard(stringResource(R.string.autoeq_curve_title)) {
                ResponseChart(state = state, measurement = measurement, targetDelta = viewModel.targetDelta(state))
            }
        }

        if (state.base != null) {
            item { TargetCard(state = state, viewModel = viewModel) }
        }

        item {
            SectionCard(stringResource(R.string.autoeq_filters_title)) {
                Text(
                    stringResource(R.string.autoeq_filters_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                PreampRow(state.settings, viewModel)
                if (state.settings.filters.isEmpty()) {
                    TextButton(onClick = { viewModel.startManual() }) { Text(stringResource(R.string.autoeq_manual_start)) }
                }
            }
        }
        itemsIndexed(state.settings.filters) { index, filter ->
            FilterRow(
                index = index,
                filter = filter,
                expanded = expandedFilter == index,
                onToggleExpanded = { expandedFilter = if (expandedFilter == index) -1 else index },
                onChange = { viewModel.updateFilter(index, it) },
                onDelete = {
                    expandedFilter = -1
                    viewModel.removeFilter(index)
                },
            )
        }
        if (state.settings.filters.isNotEmpty() && state.settings.filters.size < PeqSettings.MAX_FILTERS) {
            item {
                OutlinedButton(onClick = { viewModel.addFilter() }, modifier = Modifier.padding(horizontal = Keyline.margin)) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.autoeq_add_filter))
                }
            }
        }

        item {
            SectionCard(stringResource(R.string.autoeq_devices_title)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.autoeq_devices_auto),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = autoPerDevice, onCheckedChange = viewModel::setAutoPerDevice)
                }
                val current = device
                if (current != null) {
                    FilledTonalButton(onClick = { viewModel.saveForCurrentDevice() }, enabled = state.settings.filters.isNotEmpty()) {
                        Icon(Icons.Filled.Headphones, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.autoeq_devices_save, current.label), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                deviceProfiles.forEach { (key, saved) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(deviceLabels[key] ?: key, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                profileLabel(saved),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { viewModel.forgetDevice(key) }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.autoeq_devices_forget))
                        }
                    }
                }
            }
        }

        item {
            Text(
                stringResource(R.string.autoeq_attribution, state.entry?.subtitle ?: "—"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Keyline.margin, vertical = Space.sm),
            )
        }
    }
    }

    if (searchOpen) {
        SearchSheet(viewModel = viewModel, onDismiss = { searchOpen = false })
    }
}

@Composable
private fun profileLabel(state: AutoEqState): String {
    val name = state.entry?.name ?: stringResource(R.string.autoeq_manual)
    return name
}

/** A section on the page: its title in the Settings section style, then the content. No card. */
@Composable
private fun SectionCard(title: String? = null, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = Keyline.margin, end = Keyline.margin, top = Space.lg),
    ) {
        if (title != null) {
            Text(title, style = AppType.titleL, color = MaterialTheme.tunedColors.text1)
            Spacer(Modifier.height(Space.md))
        }
        content()
    }
}

@Composable
private fun ProfileCard(state: AutoEqState, onSearch: () -> Unit, viewModel: AutoEqViewModel) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Headphones, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    state.entry?.name ?: if (state.settings.filters.isNotEmpty()) {
                        stringResource(R.string.autoeq_manual)
                    } else {
                        stringResource(R.string.autoeq_no_profile)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                state.entry?.let {
                    Text(it.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (state.isCustom) {
                    Text(
                        stringResource(R.string.autoeq_custom_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSearch) {
                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.autoeq_choose))
            }
            if (state.isCustom && state.base != null) {
                OutlinedButton(onClick = { viewModel.resetToProfile() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.autoeq_reset))
                }
            }
        }
    }
}

@Composable
private fun TargetCard(state: AutoEqState, viewModel: AutoEqViewModel) {
    SectionCard(stringResource(R.string.autoeq_target_title)) {
        Text(
            stringResource(R.string.autoeq_target_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        var menuOpen by remember { mutableStateOf(false) }
        val currentName = if (state.targetId == AutoEqTargets.DEFAULT_TARGET_ID) {
            stringResource(R.string.autoeq_target_default)
        } else {
            viewModel.targets.firstOrNull { it.id == state.targetId }?.name ?: state.targetId
        }
        Box {
            OutlinedButton(onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                Text(currentName, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Filled.ExpandMore, contentDescription = null)
            }
            DropdownMenu(shape = com.tuned.app.ui.theme.MenuShape, containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh, expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.autoeq_target_default)) },
                    onClick = {
                        menuOpen = false
                        viewModel.setTarget(AutoEqTargets.DEFAULT_TARGET_ID)
                    },
                )
                viewModel.targets.forEach { target ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (target.family == "in") "${target.name} ${stringResource(R.string.autoeq_target_iem_suffix)}" else target.name,
                            )
                        },
                        onClick = {
                            menuOpen = false
                            viewModel.setTarget(target.id)
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        AdjustSlider(
            label = stringResource(R.string.autoeq_bass),
            value = state.bassDb,
            range = -6.0..12.0,
            step = 0.5,
            format = { "%+.1f dB".format(Locale.ROOT, it) },
            onCommit = { viewModel.setBass(it) },
        )
        AdjustSlider(
            label = stringResource(R.string.autoeq_tilt),
            value = state.tiltDbPerOctave,
            range = -1.0..1.0,
            step = 0.1,
            format = { "%+.1f dB/oct".format(Locale.ROOT, it) },
            onCommit = { viewModel.setTilt(it) },
        )
    }
}

@Composable
private fun AdjustSlider(
    label: String,
    value: Double,
    range: ClosedFloatingPointRange<Double>,
    step: Double,
    format: (Double) -> String,
    onCommit: (Double) -> Unit,
) {
    var local by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(format(local.toDouble()), style = MaterialTheme.typography.labelLarge)
    }
    Slider(
        value = local,
        onValueChange = { local = (PeqMath.roundTo(it.toDouble(), step)).toFloat() },
        onValueChangeFinished = { onCommit(local.toDouble()) },
        valueRange = range.start.toFloat()..range.endInclusive.toFloat(),
    )
}

@Composable
private fun PreampRow(settings: PeqSettings, viewModel: AutoEqViewModel) {
    var local by remember(settings.preampDb) { mutableFloatStateOf(settings.preampDb.toFloat()) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.autoeq_preamp), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text("%+.1f dB".format(Locale.ROOT, local), style = MaterialTheme.typography.labelLarge)
        TextButton(onClick = { viewModel.autoPreamp() }) { Text(stringResource(R.string.autoeq_preamp_auto)) }
    }
    Slider(
        value = local,
        onValueChange = { local = PeqMath.roundTo(it.toDouble(), 0.1).toFloat() },
        onValueChangeFinished = { viewModel.setPreamp(local.toDouble()) },
        valueRange = -24f..6f,
    )
}

@Composable
private fun FilterRow(
    index: Int,
    filter: PeqFilter,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onChange: (PeqFilter) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.tunedColors
    // A plain row: number, frequency, then the gain (or the type for filters without gain) on
    // its own line. Type and Q are in the expanded part.
    Column(Modifier.fillMaxWidth().padding(horizontal = Keyline.margin)) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onToggleExpanded)
                    .padding(vertical = Space.sm),
            ) {
                Text("${index + 1}", style = AppType.body, color = colors.text2, modifier = Modifier.width(Space.xxl))
                Column(Modifier.weight(1f)) {
                    Text(
                        formatHz(filter.frequencyHz),
                        style = AppType.body,
                        color = if (filter.enabled) colors.text1 else colors.text2,
                    )
                    Text(
                        if (filter.type.hasGain) "%+.1f dB".format(Locale.ROOT, filter.gainDb) else filter.type.code,
                        style = AppType.bodyS,
                        color = colors.text2,
                    )
                }
                Switch(checked = filter.enabled, onCheckedChange = { onChange(filter.copy(enabled = it)) })
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            if (expanded) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    PeqFilterType.entries.forEach { type ->
                        val selected = type == filter.type
                        TextButton(onClick = { onChange(filter.copy(type = type)) }) {
                            Text(
                                type.code,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                LogSlider(
                    label = stringResource(R.string.autoeq_frequency),
                    value = filter.frequencyHz,
                    min = 20.0,
                    max = 20_000.0,
                    format = { formatHz(it) },
                    onCommit = { onChange(filter.copy(frequencyHz = PeqMath.roundTo(it, 1.0))) },
                )
                if (filter.type.hasGain) {
                    var gain by remember(filter.gainDb) { mutableFloatStateOf(filter.gainDb.toFloat()) }
                    Row {
                        Text(stringResource(R.string.autoeq_gain), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Text("%+.1f dB".format(Locale.ROOT, gain), style = MaterialTheme.typography.labelMedium)
                    }
                    Slider(
                        value = gain,
                        onValueChange = { gain = PeqMath.roundTo(it.toDouble(), 0.1).toFloat() },
                        onValueChangeFinished = { onChange(filter.copy(gainDb = gain.toDouble())) },
                        valueRange = -20f..20f,
                    )
                }
                LogSlider(
                    label = stringResource(R.string.autoeq_q),
                    value = filter.q,
                    min = 0.1,
                    max = 10.0,
                    format = { "%.2f".format(Locale.ROOT, it) },
                    onCommit = { onChange(filter.copy(q = PeqMath.roundTo(it, 0.01))) },
                )
                TextButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.autoeq_remove_filter))
                }
            }
        }
    }
}

@Composable
private fun LogSlider(
    label: String,
    value: Double,
    min: Double,
    max: Double,
    format: (Double) -> String,
    onCommit: (Double) -> Unit,
) {
    val lnMin = ln(min)
    val lnMax = ln(max)
    var position by remember(value) {
        mutableFloatStateOf(((ln(value.coerceIn(min, max)) - lnMin) / (lnMax - lnMin)).toFloat())
    }
    val current = kotlin.math.exp(lnMin + position * (lnMax - lnMin))
    Row {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(format(current), style = MaterialTheme.typography.labelMedium)
    }
    Slider(
        value = position,
        onValueChange = { position = it },
        onValueChangeFinished = { onCommit(current) },
    )
}

private fun formatHz(hz: Double): String =
    if (hz >= 1000) "%.2f kHz".format(Locale.ROOT, hz / 1000.0).replace(".00", "") else "${hz.roundToInt()} Hz"

/**
 * Correction curve (always) plus, when the lab published its measurement:
 * raw response, chosen target and the corrected result.
 */
@Composable
private fun ResponseChart(state: AutoEqState, measurement: AutoEqMeasurement?, targetDelta: List<Double>?) {
    val eqColor = MaterialTheme.colorScheme.primary
    val rawColor = MaterialTheme.colorScheme.outline
    val targetColor = MaterialTheme.colorScheme.tertiary
    val resultColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val grid = PeqMath.LOG_GRID

    val eqCurve = remember(state.settings) {
        grid.map { f -> PeqMath.filtersDb(state.settings.filters, f) }
    }
    val series = remember(state.settings, measurement, targetDelta) {
        val m = measurement ?: return@remember null
        val freqs = m.frequencies
        val delta = targetDelta?.takeIf { state.targetId != AutoEqTargets.DEFAULT_TARGET_ID }
        val target = freqs.indices.map { i ->
            val t = m.target[i]
            if (t.isNaN()) Double.NaN else t + (delta?.let { interpolate(grid, it, freqs[i]) } ?: 0.0) +
                PeqMath.bassShelfDb(state.bassDb, freqs[i]) + PeqMath.tiltDb(state.tiltDbPerOctave, freqs[i])
        }
        val corrected = freqs.indices.map { i -> m.raw[i] + PeqMath.filtersDb(state.settings.filters, freqs[i]) }
        Triple(m.raw.toList(), target, corrected)
    }

    Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
        val values = buildList {
            addAll(eqCurve)
            series?.let { (raw, target, corrected) ->
                addAll(raw)
                addAll(target.filter { !it.isNaN() })
                addAll(corrected)
            }
        }.filter { it.isFinite() }
        val top = ((values.maxOrNull() ?: 6.0).coerceAtLeast(6.0) + 2.0).coerceAtMost(30.0)
        val bottom = ((values.minOrNull() ?: -6.0).coerceAtMost(-6.0) - 2.0).coerceAtLeast(-30.0)
        val logMin = log10(20.0)
        val logMax = log10(20_000.0)
        fun x(f: Double) = ((log10(f) - logMin) / (logMax - logMin) * size.width).toFloat()
        fun y(db: Double) = ((top - db) / (top - bottom) * size.height).toFloat()

        listOf(100.0, 1000.0, 10_000.0).forEach { f ->
            drawLine(gridColor, Offset(x(f), 0f), Offset(x(f), size.height), strokeWidth = 1f)
        }
        drawLine(gridColor, Offset(0f, y(0.0)), Offset(size.width, y(0.0)), strokeWidth = 1.5f)

        fun drawSeries(freqs: DoubleArray, values: List<Double>, color: Color, width: Float, dashed: Boolean = false) {
            val path = Path()
            var started = false
            freqs.indices.forEach { i ->
                val v = values.getOrNull(i) ?: return@forEach
                if (!v.isFinite() || freqs[i] < 20.0 || freqs[i] > 20_000.0) return@forEach
                val px = x(freqs[i])
                val py = y(v)
                if (!started) {
                    path.moveTo(px, py)
                    started = true
                } else {
                    path.lineTo(px, py)
                }
            }
            drawPath(
                path,
                color,
                style = Stroke(width = width, pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(10f, 8f)) else null),
            )
        }

        series?.let { (raw, target, corrected) ->
            val freqs = measurement!!.frequencies
            drawSeries(freqs, raw, rawColor, 2f)
            drawSeries(freqs, target, targetColor, 2.5f, dashed = true)
            drawSeries(freqs, corrected, resultColor, 2.5f)
        }
        drawSeries(grid, eqCurve, eqColor, 4f)
    }
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Legend(stringResource(R.string.autoeq_legend_eq), eqColor)
        if (series != null) {
            Legend(stringResource(R.string.autoeq_legend_raw), rawColor)
            Legend(stringResource(R.string.autoeq_legend_target), targetColor)
            Legend(stringResource(R.string.autoeq_legend_result), resultColor)
        }
    }
}

@Composable
private fun Legend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(10.dp)) { drawCircle(color) }
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun interpolate(grid: DoubleArray, values: List<Double>, f: Double): Double {
    if (values.isEmpty()) return 0.0
    if (f <= grid.first()) return values.first()
    if (f >= grid.last()) return values.last()
    val position = ln(f / grid.first()) / ln(2.0) * 12.0
    val i = position.toInt().coerceIn(0, values.size - 2)
    val t = position - i
    return values[i] + (values[i + 1] - values[i]) * t
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchSheet(viewModel: AutoEqViewModel, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val indexState by viewModel.indexState.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadIndex() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(stringResource(R.string.autoeq_search_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.autoeq_search_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            when (indexState) {
                AutoEqViewModel.IndexState.LOADING -> Row(verticalAlignment = Alignment.CenterVertically) {
                    com.tuned.app.ui.common.LoadingSpinner(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.autoeq_search_loading), style = MaterialTheme.typography.bodySmall)
                }
                AutoEqViewModel.IndexState.ERROR -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.autoeq_search_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { viewModel.loadIndex(force = true) }) { Text(stringResource(R.string.autoeq_retry)) }
                }
                else -> Unit
            }
            if (busy) Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) { com.tuned.app.ui.common.LoadingSpinner(Modifier.size(28.dp)) }
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 520.dp)) {
                items(results, key = { it.encodedPath }) { entry ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !busy) { viewModel.select(entry, onDone = onDismiss) }
                            .padding(vertical = 10.dp),
                    ) {
                        Text(entry.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(entry.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (query.isNotBlank() && results.isEmpty() && indexState == AutoEqViewModel.IndexState.READY) {
                Text(
                    stringResource(R.string.autoeq_search_empty),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

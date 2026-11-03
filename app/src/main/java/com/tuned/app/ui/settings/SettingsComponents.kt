package com.tuned.app.ui.settings

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Shape
import kotlinx.coroutines.delay
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import kotlin.math.roundToInt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tuned.app.R
import com.tuned.app.data.local.ThemeMode
import com.tuned.app.ui.common.ConnectedButtonGroup
import com.tuned.app.ui.common.ConnectedButtonItem
import com.tuned.app.ui.theme.ExpressivePillShape
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode

// -- Expressive shape scale used only within this screen --
internal val CardOuterShape = RoundedCornerShape(28.dp)

internal val IconBadgeShape = RoundedCornerShape(14.dp)

/** Where a row sits within a visually-connected group of settings rows —
 *  drives per-row corner radii so a multi-row group reads as one premium
 *  surface split into rows, not a stack of separate cards (see groupShape
 *  below and the GROUP_GAP spacing used between rows in a group's Column). */
internal enum class GroupPosition { SINGLE, TOP, MIDDLE, BOTTOM }

internal val GROUP_OUTER_RADIUS = 28.dp

internal val GROUP_INNER_RADIUS = 6.dp

// Rows sit on the page with no card, so there is no seam between them.
internal val GROUP_GAP = 0.dp

internal fun groupShape(position: GroupPosition): RoundedCornerShape = when (position) {
    GroupPosition.SINGLE -> RoundedCornerShape(GROUP_OUTER_RADIUS)
    GroupPosition.TOP -> RoundedCornerShape(
        topStart = GROUP_OUTER_RADIUS, topEnd = GROUP_OUTER_RADIUS,
        bottomStart = GROUP_INNER_RADIUS, bottomEnd = GROUP_INNER_RADIUS,
    )
    GroupPosition.MIDDLE -> RoundedCornerShape(GROUP_INNER_RADIUS)
    GroupPosition.BOTTOM -> RoundedCornerShape(
        topStart = GROUP_INNER_RADIUS, topEnd = GROUP_INNER_RADIUS,
        bottomStart = GROUP_OUTER_RADIUS, bottomEnd = GROUP_OUTER_RADIUS,
    )
}

/** Wraps a fixed list of settings rows and assigns each one its
 *  GroupPosition automatically — SINGLE for a lone row, TOP/BOTTOM for the
 *  ends of a longer group, MIDDLE for everything between. Rows are stacked
 *  with a tiny GROUP_GAP rather than normal item spacing, so the group
 *  reads as one connected surface with rows peeking through a hairline gap
 *  rather than a list of separate cards. */
@Composable
internal fun SettingsGroup(rowCount: Int, content: @Composable (index: Int, position: GroupPosition) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(GROUP_GAP)) {
        for (i in 0 until rowCount) {
            val position = when {
                rowCount == 1 -> GroupPosition.SINGLE
                i == 0 -> GroupPosition.TOP
                i == rowCount - 1 -> GroupPosition.BOTTOM
                else -> GroupPosition.MIDDLE
            }
            content(i, position)
        }
    }
}

/**
 * Attaches a BringIntoViewRequester and renders an expressive pulsing highlight glow
 * with outer colored shadow halo, accent border, and auto-scroll when isHighlighted is true.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
internal fun Modifier.settingHighlightGlow(
    isHighlighted: Boolean,
    shape: Shape = CardOuterShape,
): Modifier = composed {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val primaryColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            delay(160L)
            bringIntoViewRequester.bringIntoView()
        }
    }

    if (!isHighlighted) {
        return@composed this.bringIntoViewRequester(bringIntoViewRequester)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "settingHighlightGlow")
    val glowProgress by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowProgress",
    )

    this
        .bringIntoViewRequester(bringIntoViewRequester)
        .shadow(
            elevation = (7 * glowProgress).dp,
            shape = shape,
            ambientColor = primaryColor.copy(alpha = 0.40f * glowProgress),
            spotColor = primaryColor.copy(alpha = 0.55f * glowProgress),
        )
        .border(
            width = 2.5.dp,
            color = primaryColor.copy(alpha = glowProgress),
            shape = shape,
        )
}

/**
 * Faithful port of settings.js (par 8): Last.fm account management, appearance
 * (AMOLED / Dynamic Color / Monochrome / accent presets / custom color
 * wheel), iTunes/ListenBrainz artwork toggles, data management (clear
 * recommendation exclusions, clear all data), backup & restore, and app info.
 *
 * Visuals only: restyled into a Material 3 Expressive presentation
 * (larger touch targets, per-row cards, tonal icon badges, spring-based
 * press feedback). Every setting, callback, and piece of state below is
 * unchanged from the original implementation.
 */

/** Small tap-scale used across the row-style cards on this screen for a
 *  softer, springier press response than the plain ripple alone gives. */
@Composable
internal fun rememberPressScale(interactionSource: MutableInteractionSource): Float {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    return scale
}

@Composable
internal fun SectionLabel(text: String) {
    // A real section title (white, bold, on the row's start line) instead of a small accent
    // label: the accent stays for things you tap.
    Text(
        text = text,
        style = com.tuned.app.ui.theme.AppType.titleL,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun IconBadge(icon: ImageVector, container: Color, tint: Color, modifier: Modifier = Modifier) {
    // One badge colour for every row: callers passed secondary/tertiary containers too, and in
    // the dark theme those sit at the page's own shade, so some badges vanished. Danger keeps red.
    val isDanger = container == MaterialTheme.colorScheme.errorContainer
    val badge = if (isDanger) container else MaterialTheme.colorScheme.primaryContainer
    val glyph = if (isDanger) tint else MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        modifier
            .size(44.dp)
            .clip(IconBadgeShape)
            .background(badge),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = glyph, modifier = Modifier.size(22.dp))
    }
}

@Composable
internal fun IconBadge(painter: Painter, container: Color, tint: Color, modifier: Modifier = Modifier) {
    // One badge colour for every row: callers passed secondary/tertiary containers too, and in
    // the dark theme those sit at the page's own shade, so some badges vanished. Danger keeps red.
    val isDanger = container == MaterialTheme.colorScheme.errorContainer
    val badge = if (isDanger) container else MaterialTheme.colorScheme.primaryContainer
    val glyph = if (isDanger) tint else MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        modifier
            .size(44.dp)
            .clip(IconBadgeShape)
            .background(badge),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter, contentDescription = null, tint = glyph, modifier = Modifier.size(22.dp))
    }
}

@Composable
internal fun ThemeModeSelectorCard(
    currentThemeMode: ThemeMode,
    onSelectThemeMode: (ThemeMode) -> Unit,
    position: GroupPosition = GroupPosition.SINGLE,
    isHighlighted: Boolean = false,
) {
    val shape = groupShape(position)

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .settingHighlightGlow(isHighlighted = isHighlighted, shape = shape),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    Icons.Filled.BrightnessAuto,
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.settings_theme_mode),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        stringResource(R.string.settings_theme_mode_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            val items = listOf(
                ConnectedButtonItem(stringResource(R.string.theme_mode_system), Icons.Filled.BrightnessAuto),
                ConnectedButtonItem(stringResource(R.string.theme_mode_light), Icons.Filled.LightMode),
                ConnectedButtonItem(stringResource(R.string.theme_mode_dark), Icons.Filled.DarkMode),
            )
            val selectedIdx = when (currentThemeMode) {
                ThemeMode.SYSTEM -> 0
                ThemeMode.LIGHT -> 1
                ThemeMode.DARK -> 2
            }
            ConnectedButtonGroup(
                items = items,
                selectedIndex = selectedIdx,
                onSelect = { idx ->
                    val mode = when (idx) {
                        0 -> ThemeMode.SYSTEM
                        1 -> ThemeMode.LIGHT
                        else -> ThemeMode.DARK
                    }
                    onSelectThemeMode(mode)
                },
            )
        }
    }
}

@Composable
internal fun SettingsToggleCard(
    painter: Painter,
    iconContainer: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    position: GroupPosition = GroupPosition.SINGLE,
    enabled: Boolean = true,
    isHighlighted: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)
    val shape = groupShape(position)
    val haptic = LocalHapticFeedback.current
    val detailViewer = LocalToggleDetailViewer.current

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.Transparent,
            disabledContainerColor = Color.Transparent,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val s = if (enabled) scale else 1f
                scaleX = s
                scaleY = s
            }
            .settingHighlightGlow(isHighlighted = isHighlighted, shape = shape)
            .clip(shape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                enabled = enabled,
                onClick = { onCheckedChange(!checked) },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    detailViewer?.invoke(ToggleDetailInfo(title, subtitle, checked))
                },
            ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(
                painter,
                if (enabled) iconContainer else iconContainer.copy(alpha = 0.5f),
                if (enabled) iconTint else iconTint.copy(alpha = 0.5f),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = if (enabled) onCheckedChange else null,
                thumbContent = if (checked) {
                    {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                } else null,
            )
        }
    }
}

@Composable
internal fun SettingsToggleCard(
    icon: ImageVector,
    iconContainer: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    position: GroupPosition = GroupPosition.SINGLE,
    enabled: Boolean = true,
    isHighlighted: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)
    val shape = groupShape(position)
    val haptic = LocalHapticFeedback.current
    val detailViewer = LocalToggleDetailViewer.current

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.Transparent,
            disabledContainerColor = Color.Transparent,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val s = if (enabled) scale else 1f
                scaleX = s
                scaleY = s
            }
            .settingHighlightGlow(isHighlighted = isHighlighted, shape = shape)
            .clip(shape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                enabled = enabled,
                onClick = { onCheckedChange(!checked) },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    detailViewer?.invoke(ToggleDetailInfo(title, subtitle, checked))
                },
            ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(
                icon,
                if (enabled) iconContainer else iconContainer.copy(alpha = 0.5f),
                if (enabled) iconTint else iconTint.copy(alpha = 0.5f),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = if (enabled) onCheckedChange else null,
                thumbContent = if (checked) {
                    {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                } else null,
            )
        }
    }
}

@Composable
internal fun CrossfadeDurationRow(
    seconds: Int,
    onSecondsChange: (Int) -> Unit,
    autoMix: Boolean = false,
    onAutoMixChange: (Boolean) -> Unit = {},
    position: GroupPosition = GroupPosition.SINGLE,
    isHighlighted: Boolean = false,
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var sliderValue by remember { mutableStateOf(seconds.coerceIn(1, 12).toFloat()) }
    var isDragging by remember { mutableStateOf(false) }
    LaunchedEffect(seconds) {
        if (!isDragging) sliderValue = seconds.coerceIn(1, 12).toFloat()
    }
    val shape = groupShape(position)

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .settingHighlightGlow(isHighlighted = isHighlighted, shape = shape),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            // Crossfade blends for a set time; AutoMix picks the moment and length from the music.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                androidx.compose.material3.FilterChip(
                    selected = !autoMix,
                    onClick = { onAutoMixChange(false) },
                    label = { Text(stringResource(R.string.settings_crossfade)) },
                )
                androidx.compose.material3.FilterChip(
                    selected = autoMix,
                    onClick = { onAutoMixChange(true) },
                    label = { Text(stringResource(R.string.settings_automix)) },
                )
            }
            if (autoMix) {
                Text(
                    stringResource(R.string.settings_automix_detail),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    Icons.Filled.Tune,
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.set_xfade_dur), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(
                        "${sliderValue.roundToInt()}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        "${sliderValue.roundToInt()}s",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            Slider(
                value = sliderValue,
                onValueChange = { value ->
                    isDragging = true
                    val rounded = value.roundToInt().coerceIn(1, 12)
                    if (rounded.toFloat() != sliderValue) {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        sliderValue = rounded.toFloat()
                        onSecondsChange(rounded)
                    }
                },
                onValueChangeFinished = {
                    isDragging = false
                },
                valueRange = 1f..12f,
                steps = 10,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.set_xfade_1), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text(stringResource(R.string.set_xfade_6), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text(stringResource(R.string.set_xfade_12), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
internal fun SettingsActionCard(
    icon: ImageVector,
    iconContainer: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    danger: Boolean = false,
    onClick: () -> Unit,
    position: GroupPosition = GroupPosition.SINGLE,
    isHighlighted: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)
    val titleColor = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val shape = groupShape(position)

    Card(
        onClick = onClick,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .settingHighlightGlow(isHighlighted = isHighlighted, shape = shape),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(icon, iconContainer, iconTint)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = titleColor, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ==========================================
// SETTINGS SEARCH COMPOSABLES
// ==========================================


@Composable
internal fun SettingsRadioOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
        )
    }
}

/**
 * Above the bit-perfect and USB settings: they send audio past Android's own volume handling,
 * straight to a DAC or a pair of USB earphones, so a wrong setting reaches someone's ears.
 */
@Composable
internal fun AdvancedAudioWarning() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(com.tuned.app.ui.theme.AppShape.lg)
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f))
            .padding(com.tuned.app.ui.theme.Space.lg),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Rounded.WarningAmber,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(com.tuned.app.ui.theme.Sizes.iconRow),
        )
        Spacer(Modifier.width(com.tuned.app.ui.theme.Space.md))
        Column(verticalArrangement = Arrangement.spacedBy(com.tuned.app.ui.theme.Space.xxs)) {
            Text(
                text = stringResource(R.string.settings_bitperfect_warning_title),
                style = com.tuned.app.ui.theme.AppType.bodyStrong,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = stringResource(R.string.settings_bitperfect_warning_body),
                style = com.tuned.app.ui.theme.AppType.bodyS,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
            )
        }
    }
}

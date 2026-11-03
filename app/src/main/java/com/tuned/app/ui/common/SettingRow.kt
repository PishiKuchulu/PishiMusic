package com.tuned.app.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.Keyline
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.tunedColors

/**
 * A settings row: 24dp icon on the margin, text at 56dp (16 + 24 + 16),
 * 56dp tall for one line and 72dp with a subtitle. No card per row; groups
 * are separated by [SectionHeader]s instead.
 *
 * Pass [checked] for a switch row (the whole row toggles), or [onClick] for a
 * row that opens something (a chevron shows unless [value] is given).
 */
@Composable
fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    value: String? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    // A switch with no callback renders as a live-looking but dead control;
    // fail loudly at composition instead of shipping a decorative toggle.
    require(checked == null || onCheckedChange != null || onClick != null) {
        "SettingRow with checked= requires onCheckedChange or onClick"
    }
    val colors = MaterialTheme.tunedColors
    val interaction = when {
        checked != null && onCheckedChange != null ->
            Modifier.toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
        onClick != null -> Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        else -> Modifier
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (subtitle == null) Sizes.rowCompact else Sizes.rowTall)
            .then(interaction)
            .padding(horizontal = Keyline.margin, vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (enabled) colors.text2 else colors.text3, modifier = Modifier.size(Sizes.icon))
            Spacer(Modifier.width(Space.lg))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(title, style = AppType.body, color = if (enabled) colors.text1 else colors.text3)
            if (subtitle != null) Text(subtitle, style = AppType.bodyS, color = colors.text2)
        }
        when {
            checked != null -> {
                Spacer(Modifier.width(Space.lg))
                Switch(
                    checked = checked,
                    onCheckedChange = null,
                    enabled = enabled,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = colors.accent,
                        checkedThumbColor = colors.onAccent,
                        uncheckedTrackColor = colors.surface2,
                        uncheckedThumbColor = colors.text3,
                        uncheckedBorderColor = colors.line,
                    ),
                )
            }
            value != null -> {
                Spacer(Modifier.width(Space.lg))
                Text(
                    value,
                    style = AppType.bodyS,
                    color = colors.text2,
                    modifier = Modifier.semantics { stateDescription = value },
                )
            }
            onClick != null -> {
                Spacer(Modifier.width(Space.lg))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.text3, modifier = Modifier.size(Sizes.iconRow))
            }
        }
    }
}

@ComponentPreviews
@Composable
private fun SettingRowPreview() = PreviewTheme { SettingRowSample() }

/** Sample states, shared by the preview and the in-app component gallery. */
@Composable
internal fun SettingRowSample() {
    Column {
        SettingRow("Language", icon = Icons.Filled.Language, value = "English", onClick = {})
        SettingRow("Equalizer", icon = Icons.Filled.GraphicEq, subtitle = "Harman 2019 target", onClick = {})
        SettingRow("Liquid Glass", icon = Icons.Filled.BubbleChart, subtitle = "Blur behind the bottom bar and player", checked = false, onCheckedChange = {})
        SettingRow("Crossfade", subtitle = "Overlap the end of one track with the start of the next", checked = true, onCheckedChange = {})
        SettingRow("Not available", subtitle = "Needs a USB DAC", checked = false, onCheckedChange = {}, enabled = false)
    }
}

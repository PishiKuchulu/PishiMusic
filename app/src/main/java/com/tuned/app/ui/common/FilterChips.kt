package com.tuned.app.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.tuned.app.R
import com.tuned.app.ui.theme.AppShape
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.Keyline
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.rememberMotion
import com.tuned.app.ui.theme.tunedColors

/**
 * A single-select row of filter chips that scrolls horizontally. Chips are
 * 32dp tall to look light, while each touch target is still 48dp. The first
 * chip starts on the screen margin.
 */
@Composable
fun FilterChipRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth().selectableGroup(),
        contentPadding = PaddingValues(horizontal = Keyline.margin),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(options, key = { _, label -> label }) { index, label ->
            FilterChip(label = label, selected = index == selectedIndex, onClick = { onSelect(index) })
        }
    }
}

@Composable
fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.tunedColors
    val motion = rememberMotion()
    val container by animateColorAsState(if (selected) colors.accent else colors.surface2, motion.fast(), label = "chipBg")
    val content by animateColorAsState(if (selected) colors.onAccent else colors.text1, motion.fast(), label = "chipFg")
    Box(
        modifier
            .minimumInteractiveComponentSize()
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .height(Sizes.chip)
                .clip(AppShape.sm)
                .background(container)
                .padding(horizontal = Space.md),
            contentAlignment = Alignment.Center,
        ) {
            // Selection is not colour-only: a check glyph gives colour-blind
            // users (and TalkBack, via cmp_selected) a second signal.
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                if (selected) {
                    androidx.compose.material3.Icon(
                        androidx.compose.material.icons.Icons.Filled.Check,
                        contentDescription = stringResource(R.string.cmp_selected),
                        tint = content,
                        modifier = Modifier.size(Sizes.iconInline),
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.size(Space.xs))
                }
                Text(label, style = AppType.label, color = content)
            }
        }
    }
}

@ComponentPreviews
@Composable
private fun FilterChipsPreview() = PreviewTheme { FilterChipsSample() }

/** Sample states, shared by the preview and the in-app component gallery. */
@Composable
internal fun FilterChipsSample() {
    Column(Modifier.padding(vertical = Space.lg)) {
        FilterChipRow(
            options = listOf("Playlists", "Albums", "Artists", "Liked", "Downloaded", "Local", "Podcasts"),
            selectedIndex = 1,
            onSelect = {},
        )
    }
}

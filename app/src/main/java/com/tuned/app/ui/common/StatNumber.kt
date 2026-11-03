package com.tuned.app.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.rememberMotion
import com.tuned.app.ui.theme.tunedColors
import java.text.NumberFormat
import java.util.Locale

/**
 * A large statistic with a label underneath. When the value changes, only
 * the digits that changed roll: up when the number grows, down when it
 * shrinks. Digits are tabular, so nothing shifts sideways while rolling.
 */
@Composable
fun StatNumber(
    value: Long,
    label: String,
    modifier: Modifier = Modifier,
    style: TextStyle = AppType.monoL,
    locale: Locale = Locale.getDefault(),
) {
    val colors = MaterialTheme.tunedColors
    val motion = rememberMotion()
    // Last composed value, kept outside snapshot state: writing state during
    // composition would schedule another composition for no reason.
    val last = remember { LongArray(1) { value } }
    val growing = value >= last[0]
    SideEffect { last[0] = value }
    val formatted = formatStat(value, locale)

    Column(
        modifier.clearAndSetSemantics { contentDescription = "$formatted $label" },
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Row {
            // Key digits by their position from the right so that 999 -> 1,000
            // rolls the existing columns instead of re-laying the whole number.
            formatted.forEachIndexed { i, char ->
                val fromRight = formatted.length - i
                key(fromRight) {
                    AnimatedContent(
                        targetState = char,
                        transitionSpec = {
                            if (motion.reduced) {
                                ContentTransform(fadeIn(snap()), fadeOut(snap()))
                            } else if (growing) {
                                (slideInVertically(motion.base()) { it } + fadeIn(motion.base())) togetherWith
                                    (slideOutVertically(motion.base()) { -it } + fadeOut(motion.base()))
                            } else {
                                (slideInVertically(motion.base()) { -it } + fadeIn(motion.base())) togetherWith
                                    (slideOutVertically(motion.base()) { it } + fadeOut(motion.base()))
                            }
                        },
                        label = "digit$fromRight",
                    ) { c ->
                        Text(c.toString(), style = style, color = colors.text1)
                    }
                }
            }
        }
        Text(label, style = AppType.caption, color = colors.text2)
    }
}

/** Grouped for the reader's locale: 19588 is "19,588" in English, "19.588" in German. */
internal fun formatStat(value: Long, locale: Locale): String =
    NumberFormat.getIntegerInstance(locale).format(value)

@ComponentPreviews
@Composable
private fun StatNumberPreview() = PreviewTheme { StatNumberSample() }

/** Sample states, shared by the preview and the in-app component gallery. */
@Composable
internal fun StatNumberSample() {
    Row(Modifier.padding(Space.lg), horizontalArrangement = Arrangement.spacedBy(Space.xl)) {
        StatNumber(19_588, "Scrobbles")
        StatNumber(5_415, "Artists")
        StatNumber(6_093, "Albums")
    }
}

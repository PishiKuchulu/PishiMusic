package com.tuned.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.tuned.app.R
import com.tuned.app.ui.theme.AppShape
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.ExplicitMark
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.tunedColors

/**
 * The explicit mark in Tuned's chip language, like the player's format chip: a bold "E" in the
 * app's font on a full pill, tinted from the colour of the text it sits beside.
 */
@Composable
fun ExplicitTag(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.tunedColors.text2,
    fontSize: TextUnit = ExplicitMark.text,
) {
    val label = stringResource(R.string.cmp_explicit)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .semantics { contentDescription = label }
            .background(color.copy(alpha = 0.16f), AppShape.full)
            .padding(horizontal = Space.xs + Space.xxs / 2, vertical = Space.xxs / 2),
    ) {
        Text(
            text = "E",
            color = color,
            style = AppType.caption.copy(
                fontSize = fontSize,
                lineHeight = fontSize,
                fontWeight = FontWeight.Bold,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        )
    }
}

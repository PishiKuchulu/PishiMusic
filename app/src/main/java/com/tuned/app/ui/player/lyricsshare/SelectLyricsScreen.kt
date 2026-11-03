package com.tuned.app.ui.player.lyricsshare

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuned.app.R
import androidx.compose.material.icons.rounded.Close
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import coil.compose.AsyncImage
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.LyricsFace
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.tuned.app.ui.theme.tunedColors

@Composable
fun SelectLyricsScreen(
    snapshot: LyricsSnapshot,
    selectedRange: IntRange,
    onSelectionChange: (IntRange) -> Unit,
    onContinue: () -> Unit,
    onClose: () -> Unit,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val colors = MaterialTheme.tunedColors

    // Bring the starting selection into view.
    LaunchedEffect(Unit) {
        if (!selectedRange.isEmpty()) listState.animateScrollToItem((selectedRange.first - 1).coerceAtLeast(0))
    }

    val selectedCount = if (selectedRange.isEmpty()) 0 else (selectedRange.last - selectedRange.first + 1)
    val characters = if (selectedRange.isEmpty()) 0 else snapshot.lines.slice(selectedRange).sumOf { it.text.length }
    val maxLinesCapMessage = stringResource(R.string.lyshare_max_lines_reached)
    // Selected lines take the song's colour, as Apple Music tints them; text flips to stay readable.
    val selectedFill = primaryColor
    val selectedText = if (primaryColor.luminance() > 0.5f) Color(0xFF121212) else Color.White

    Box(modifier.fillMaxSize().background(colors.bg)) {
        Column(Modifier.fillMaxSize()) {
            // Header: the cover, how many lines, how many characters, and close.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = snapshot.trackInfo.resolvedArtworkUrl ?: snapshot.trackInfo.artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(colors.surface3),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (selectedCount == 1) {
                            stringResource(R.string.lyshare_line_selected_single)
                        } else {
                            stringResource(R.string.lyshare_lines_selected, selectedCount)
                        },
                        style = AppType.title,
                        color = colors.text1,
                    )
                    Text(
                        text = pluralStringResource(R.plurals.lyshare_characters, characters, characters),
                        style = AppType.bodyS,
                        color = colors.text2,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(colors.surface2), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.lyshare_close_cd),
                            tint = colors.text2,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            // Every line is a tile; the selected ones are filled with the song's colour.
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(snapshot.lines, key = { index, _ -> index }) { index, line ->
                    val isSelected = index in selectedRange
                    val fill by animateColorAsState(if (isSelected) selectedFill else colors.surface2, tween(160), label = "lyricTileFill")
                    val ink by animateColorAsState(if (isSelected) selectedText else colors.text1, tween(160), label = "lyricTileInk")
                    val accessibilityLabel = if (isSelected) {
                        stringResource(R.string.lyshare_accessibility_line_selected, index + 1, snapshot.lines.size)
                    } else {
                        stringResource(R.string.lyshare_accessibility_line_unselected, index + 1, snapshot.lines.size)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(fill)
                            .clickable {
                                val next = updateContiguousSelection(
                                    currentRange = selectedRange,
                                    tappedIndex = index,
                                    totalLines = snapshot.lines.size,
                                    maxLines = 5,
                                    onCapReached = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        Toast.makeText(context, maxLinesCapMessage, Toast.LENGTH_SHORT).show()
                                    },
                                )
                                onSelectionChange(next)
                            }
                            .semantics {
                                role = Role.Checkbox
                                selected = isSelected
                                contentDescription = "$accessibilityLabel: ${line.text}"
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = line.text,
                            // The lyrics screen's face, so lines look as they do there.
                            style = TextStyle(
                                fontFamily = LyricsFace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                lineHeight = 24.sp,
                                letterSpacing = (-0.3).sp,
                            ),
                            color = ink,
                            textAlign = if (line.isRtl) TextAlign.End else TextAlign.Start,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        // The list fades under the button.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(120.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, colors.bg.copy(alpha = 0.92f), colors.bg))),
        )

        Button(
            onClick = onContinue,
            enabled = selectedCount > 0,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.text1,
                contentColor = colors.bg,
                disabledContainerColor = colors.surface2,
                disabledContentColor = colors.text3,
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(text = stringResource(R.string.lyshare_continue), style = AppType.bodyStrong)
        }
    }
}

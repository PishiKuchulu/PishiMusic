package com.tuned.app.ui.recap

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuned.app.R
import com.tuned.app.data.local.db.YearlyListenEntity
import com.tuned.app.playback.PlayableTrack
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import com.tuned.app.ui.common.barScrim
import com.tuned.app.ui.common.AppButton
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.player.LocalMiniPlayerScrollClearance
import com.tuned.app.ui.common.safeHorizontalContentPadding
import com.tuned.app.ui.common.safeDrawingBottomPadding
import com.tuned.app.ui.common.adaptiveContentWidth
import com.tuned.app.ui.common.ExpressiveHeader
import androidx.compose.material3.Surface
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import com.tuned.app.ui.common.AppButtonStyle
import com.tuned.app.ui.common.Artwork
import com.tuned.app.ui.common.EdgeToEdgeDialogWindow
import com.tuned.app.ui.common.GlassCircleButton
import com.tuned.app.ui.common.TunedIcons
import com.tuned.app.ui.player.FluidArtworkBackground
import com.tuned.app.ui.player.LocalMusicPlayer
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.Keyline
import com.tuned.app.ui.theme.tunedColors
import kotlinx.coroutines.launch

/** Type size that lets the minutes fill the line: short numbers huge, long ones still fitting. */
internal fun recapNumberSp(widthDp: Float, digits: Int): Float =
    (widthDp / (digits.coerceAtLeast(1) * RECAP_DIGIT_WIDTH_EM)).coerceIn(RECAP_NUMBER_MIN_SP, RECAP_NUMBER_MAX_SP)

private const val RECAP_DIGIT_WIDTH_EM = 0.72f
private const val RECAP_NUMBER_MIN_SP = 56f
private const val RECAP_NUMBER_MAX_SP = 148f

private fun YearlyListenEntity.toPlayable() =
    PlayableTrack(title = title, artist = artist, artworkUrl = artworkUrl, videoId = videoId)

private fun YearlyListenEntity.minutes(): Int = (listenedMs / 60_000L).toInt()

/**
 * A year of listening as a chart: the minutes large at the top, then the top songs and the top
 * artists on big rank numbers, number one bright and the rest dimmed.
 */
@Composable
fun RecapScreen(
    onBack: () -> Unit,
    onOpenArtist: (String) -> Unit,
    viewModel: RecapViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val player = LocalMusicPlayer.current
    val colors = MaterialTheme.tunedColors
    val playFrom: (Int) -> Unit = { index ->
        player.playQueue(state.topSongs.map { it.toPlayable() }, startIndex = index, sourceLabel = "${state.year}")
    }
    // Under a minute reads as "0 minutes": not worth a place on the chart.
    val artists = remember(state.topArtists) { state.topArtists.filter { it.minutes >= 1 } }
    val listState = rememberLazyListState()
    val scrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
    }

    Box(Modifier.fillMaxSize().background(colors.bg)) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = Keyline.detail,
                end = Keyline.detail,
                bottom = Space.xl + LocalMiniPlayerScrollClearance.current + safeDrawingBottomPadding(),
            ),
            modifier = Modifier.fillMaxSize().adaptiveContentWidth(maxWidth = 680.dp),
        ) {
            item(key = "header") { ChartHeader(state, onSelectYear = viewModel::show) }
            if (state.error != null) {
                item(key = "recap_error") {
                    Column(Modifier.padding(top = Space.lg)) {
                        Text(state.error.orEmpty(), style = AppType.body, color = colors.danger)
                        androidx.compose.material3.TextButton(onClick = { viewModel.show(state.year) }) {
                            Text(stringResource(R.string.ly_retry))
                        }
                    }
                }
            }
            if (state.topSongs.isNotEmpty()) {
                item(key = "songsLabel") { ChartTitle(stringResource(R.string.recap_top_songs)) }
                itemsIndexed(state.topSongs, key = { _, song -> "song_${song.trackKey}" }) { index, song ->
                    ChartRow(
                        rank = index + 1,
                        title = song.title,
                        subtitle = song.artist,
                        minutes = song.minutes(),
                        onClick = { playFrom(index) },
                    ) { Artwork(url = song.artworkUrl, size = 56.dp, shape = RoundedCornerShape(10.dp)) }
                }
            }
            if (artists.isNotEmpty()) {
                item(key = "artistsLabel") { ChartTitle(stringResource(R.string.recap_top_artists)) }
                itemsIndexed(artists, key = { _, artist -> "artist_${artist.name}" }) { index, artist ->
                    ChartRow(
                        rank = index + 1,
                        title = artist.name,
                        subtitle = null,
                        minutes = artist.minutes.toInt(),
                        onClick = { onOpenArtist(artist.name) },
                    ) { Artwork(url = artist.artworkUrl, size = 56.dp, artistName = artist.name, shape = CircleShape) }
                }
            }
        }
        // Once the page scrolls, the page colour rises behind the back button so text never runs under it.
        Row(
            Modifier
                .fillMaxWidth()
                .barScrim(visible = scrolled)
                .statusBarsPadding()
                .padding(horizontal = Keyline.detail, vertical = Space.sm),
        ) {
            GlassCircleButton(TunedIcons.ChevronBack, stringResource(R.string.common_back), onBack)
            Spacer(Modifier.weight(1f))
            if (state.songCount > 0) {
                // The share card (RecapShareCard.shareRecap) was fully built
                // but had no caller — this button is its way in.
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                GlassCircleButton(Icons.Filled.Share, stringResource(R.string.common_share)) {
                    scope.launch { shareRecap(context, state) }
                }
            }
        }
    }
}

/** The year, the minutes as large as the line allows, and what they were spread across. */
@Composable
private fun ChartHeader(state: RecapState, onSelectYear: (Int) -> Unit) {
    val colors = MaterialTheme.tunedColors
    val number = remember(state.year) { Animatable(0f) }
    LaunchedEffect(state.year, state.minutes) {
        number.animateTo(state.minutes.toFloat(), tween(COUNT_UP_MS, easing = FastOutSlowInEasing))
    }
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 72.dp, bottom = Space.sm)) {
        Text(state.year.toString(), style = AppType.title, color = colors.text2)
        if (state.songCount == 0 && !state.isLoading) {
            Spacer(Modifier.height(Space.sm))
            Text(stringResource(R.string.recap_empty), style = AppType.body, color = colors.text2)
            return@Column
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val digits = String.format(java.util.Locale.getDefault(), "%,d", number.value.toLong())
            val finalLength = String.format(java.util.Locale.getDefault(), "%,d", state.minutes).length + 4
            val size = recapNumberSp(maxWidth.value, finalLength).sp
            Text(
                stringResource(R.string.recap_chart_minutes, digits),
                style = AppType.display.copy(fontSize = size, lineHeight = size * 1.05f),
                color = colors.text1,
                maxLines = 1,
            )
        }
        Text(
            stringResource(
                R.string.recap_chart_spread,
                pluralStringResource(R.plurals.recap_songs, state.songCount, state.songCount),
                pluralStringResource(R.plurals.recap_artists, state.artistCount, state.artistCount),
            ),
            style = AppType.body,
            color = colors.text2,
        )
        if (state.years.size > 1) {
            Spacer(Modifier.height(Space.lg))
            YearChips(state.years, state.year, onSelectYear)
        }
    }
}

@Composable
private fun ChartTitle(text: String) {
    Text(
        text,
        style = AppType.titleL,
        color = MaterialTheme.tunedColors.text1,
        modifier = Modifier.padding(top = Space.xxl, bottom = Space.sm),
    )
}

/** Rank large on the left (number one bright), the picture, the name, then the minutes. */
@Composable
private fun ChartRow(
    rank: Int,
    title: String,
    subtitle: String?,
    minutes: Int,
    onClick: () -> Unit,
    picture: @Composable () -> Unit,
) {
    val colors = MaterialTheme.tunedColors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            rank.toString(),
            style = AppType.display.copy(fontSize = 32.sp, lineHeight = 34.sp),
            color = if (rank == 1) colors.text1 else colors.text3,
            modifier = Modifier.width(44.dp),
        )
        picture()
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Text(title, style = AppType.bodyStrong, color = colors.text1, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = AppType.bodyS, color = colors.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                pluralStringResource(R.plurals.recap_artist_minutes, minutes, minutes),
                style = AppType.caption,
                color = colors.text3,
            )
        }
    }
}

/** The years with listening, when there is more than one. */
@Composable
private fun YearChips(years: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        years.forEach { year ->
            val on = year == selected
            Surface(
                onClick = { onSelect(year) },
                shape = RoundedCornerShape(50),
                color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.tunedColors.text1.copy(alpha = 0.10f),
            ) {
                Text(
                    year.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                )
            }
        }
    }
}

/** The way into the recap from the You tab: the top song's cover, the year and the minutes. */
@Composable
fun RecapEntryTile(onClick: () -> Unit, modifier: Modifier = Modifier, viewModel: RecapViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.tunedColors
    if (state.songCount == 0) return
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        viewModel.show(state.year)
        onPauseOrDispose { }
    }
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.22f), Color.Transparent)))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(url = state.topSongs.firstOrNull()?.artworkUrl, size = 76.dp, shape = RoundedCornerShape(16.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.recap_title, state.year), style = AppType.titleL, color = colors.text1, maxLines = 1)
            Text(
                pluralStringResource(R.plurals.recap_artist_minutes, state.minutes.toInt(), state.minutes.toInt()),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.text2,
            )
        }
        androidx.compose.material3.Icon(
            TunedIcons.ChevronRight,
            contentDescription = null,
            tint = colors.text2,
        )
    }
}

private const val COUNT_UP_MS = 900

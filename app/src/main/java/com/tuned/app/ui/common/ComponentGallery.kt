package com.tuned.app.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tuned.app.ui.theme.tunedColors

/**
 * Debug-only screen that shows every shared component in its sample states,
 * using the device's real theme, fonts and width. Turn on the layout grid in
 * Settings to check alignment against the keylines.
 */
@Composable
fun ComponentGallery(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val listState = rememberLazyListState()
        val collapsed = rememberTitleCollapsed(listState)
        val sections: List<Pair<String, @Composable () -> Unit>> = listOf(
            "Track rows" to { TrackRowSample() },
            "Swipe actions" to { SwipeRowSample() },
            "Tiles and shelves" to { MediaTileSample() },
            "Headers" to { HeadersSample() },
            "Collection header" to { CollectionHeaderSample() },
            "Buttons" to { ButtonsSample() },
            "Filter chips" to { FilterChipsSample() },
            "Audio spec" to { SpecStripSample() },
            "Statistics" to { StatNumberSample() },
            "Settings rows" to { SettingRowSample() },
            "Sheet" to { AppSheetSample() },
            "Empty, error and loading" to { StateViewsSample() },
            "Undo" to { UndoSnackbarSample() },
        )
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.tunedColors.bg) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                ScreenTopBar(title = "Components", collapsed = collapsed, actions = {
                    AppIconButton(Icons.Filled.Close, "Close", onClick = onDismiss)
                })
                LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                    item { ScreenTitle("Components", subtitle = "${sections.size} groups") }
                    sections.forEach { (title, sample) ->
                        item(key = title) {
                            SectionHeader(title)
                            sample()
                        }
                    }
                }
            }
        }
    }
}

package com.tuned.app.ui.common

import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.tuned.app.ui.theme.AppShape
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.Keyline
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.tunedColors

/**
 * Bottom sheet with Tuned's geometry: 12dp top corners, a 32x4dp handle 12dp
 * from the top, an optional title on the margin, and content that clears the
 * gesture bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.tunedColors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier.windowInsetsPadding(WindowInsets.statusBars),
        sheetState = sheetState,
        shape = AppShape.sheet,
        containerColor = colors.surface1,
        contentColor = colors.text1,
        dragHandle = { SheetHandle() },
        contentWindowInsets = { WindowInsets(0) },
    ) {
        SheetBody(title = title, content = content)
    }
}

@Composable
private fun SheetHandle() {
    Box(
        Modifier
            .padding(vertical = Space.md)
            .size(width = Sizes.handleWidth, height = Sizes.handleHeight)
            .clip(AppShape.full)
            .background(MaterialTheme.tunedColors.line),
    )
}

@Composable
private fun SheetBody(title: String?, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = Space.sm)) {
        if (title != null) {
            Text(
                title,
                style = AppType.title,
                color = MaterialTheme.tunedColors.text1,
                modifier = Modifier
                    .padding(start = Keyline.margin, end = Keyline.margin, bottom = Space.sm)
                    .semantics { heading() },
            )
        }
        content()
    }
}

/** The sheet's static contents, since a modal sheet cannot render in a preview. */
@ComponentPreviews
@Composable
private fun AppSheetPreview() = PreviewTheme { AppSheetSample() }

/** Sample states, shared by the preview and the in-app component gallery. */
@Composable
internal fun AppSheetSample() {
    Column(
        Modifier.clip(AppShape.sheet).background(MaterialTheme.tunedColors.surface1),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SheetHandle()
        SheetBody(title = "Odysseus") {
            SettingRow("Add to queue", icon = Icons.AutoMirrored.Filled.QueueMusic, onClick = {})
            SettingRow("Add to playlist", icon = Icons.AutoMirrored.Filled.PlaylistAdd, onClick = {})
            SettingRow("Go to album", icon = Icons.Filled.Album, onClick = {})
        }
    }
}

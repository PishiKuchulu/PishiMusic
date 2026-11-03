package com.tuned.app.ui.common

import com.tuned.app.ui.theme.glassPageTint
import com.tuned.app.ui.theme.Backdrop
import com.tuned.app.ui.theme.LiquidGlassPreset
import com.tuned.app.ui.theme.LocalLiquidGlass
import com.tuned.app.ui.theme.isLiquidGlassBackdropSupported
import com.tuned.app.ui.theme.liquidGlassChrome
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.tuned.app.ui.theme.AppShape
import com.tuned.app.ui.theme.AppType
import com.tuned.app.ui.theme.Keyline
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.tunedColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * App-wide feedback with an optional undo. Reversible actions run at once and
 * call [show]; only data loss asks for confirmation first. A newer message
 * replaces an older one, and the older action is kept (not undone).
 */
@Stable
class UndoController internal constructor(
    val hostState: SnackbarHostState,
    private val scope: CoroutineScope,
) {
    fun show(message: String, undoLabel: String? = null, onUndo: (() -> Unit)? = null) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            val result = hostState.showSnackbar(
                message = message,
                actionLabel = if (onUndo != null) undoLabel else null,
                // ~4s (Short) is too little to notice and reach Undo after a
                // deletion; undoable messages get Long, plain notices stay Short.
                duration = if (onUndo != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) onUndo?.invoke()
        }
    }
}

@Composable
fun rememberUndoController(): UndoController {
    val hostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    return remember(hostState, scope) { UndoController(hostState, scope) }
}

/** Provided once by the app shell; null in previews and isolated screens. */
val LocalUndoController = staticCompositionLocalOf<UndoController?> { null }

/**
 * Place above the bottom bar. Messages sit on the screen margins. With Liquid Glass on, pass
 * the layer the dock and mini player sample, so the message is the same glass as they are.
 */
@Composable
fun UndoSnackbarHost(controller: UndoController, modifier: Modifier = Modifier, backdrop: Backdrop? = null) {
    SnackbarHost(controller.hostState, modifier) { data -> UndoSnackbar(data, backdrop) }
}

@Composable
private fun UndoSnackbar(data: SnackbarData, backdrop: Backdrop?) {
    UndoSnackbarContent(
        message = data.visuals.message,
        actionLabel = data.visuals.actionLabel,
        onAction = data::performAction,
        backdrop = backdrop,
    )
}

@Composable
private fun UndoSnackbarContent(message: String, actionLabel: String?, onAction: () -> Unit, backdrop: Backdrop? = null) {
    val colors = MaterialTheme.tunedColors
    val glass = LocalLiquidGlass.current && isLiquidGlassBackdropSupported() && backdrop != null
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Keyline.margin, vertical = Space.sm)
            .then(
                if (glass && backdrop != null) {
                    Modifier
                        .liquidGlassChrome(AppShape.sm, enabled = true, preset = LiquidGlassPreset.MiniPlayer, backdrop = backdrop)
                        .background(glassPageTint(), AppShape.sm)
                } else {
                    Modifier.clip(AppShape.sm).background(colors.surface3)
                },
            )
            .heightIn(min = Sizes.touch)
            .padding(start = Space.lg)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(message, style = AppType.body, color = colors.text1, modifier = Modifier.weight(1f).padding(vertical = Space.md))
        if (actionLabel != null) {
            Spacer(Modifier.width(Space.sm))
            Box(
                Modifier
                    .heightIn(min = Sizes.touch)
                    .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onAction)
                    .padding(horizontal = Space.lg),
                contentAlignment = Alignment.Center,
            ) {
                Text(actionLabel, style = AppType.label, color = colors.accent)
            }
        } else {
            Spacer(Modifier.width(Space.lg))
        }
    }
}

@ComponentPreviews
@Composable
private fun UndoSnackbarPreview() = PreviewTheme { UndoSnackbarSample() }

/** Sample states, shared by the preview and the in-app component gallery. */
@Composable
internal fun UndoSnackbarSample() {
    Column {
        UndoSnackbarContent("Removed “Odysseus” from Hazy Monsoon", actionLabel = "Undo", onAction = {})
        UndoSnackbarContent("Added 21 tracks to the queue", actionLabel = null, onAction = {})
    }
}

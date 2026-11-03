package com.tuned.app.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import com.tuned.app.R
import com.tuned.app.ui.theme.HapticKind
import com.tuned.app.ui.theme.Keyline
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.rememberHaptics
import com.tuned.app.ui.theme.rememberMotion
import com.tuned.app.ui.theme.tunedColors

/**
 * Wraps a row with two swipe actions. Swipe right adds to the queue and the
 * row springs back. Swipe left removes and the row stays gone, so the caller
 * removes the item and offers undo. A haptic marks the point where releasing
 * triggers the action.
 *
 * Every swipe action must also exist in the row's context menu: gestures are
 * shortcuts, never the only way.
 */
@Composable
fun SwipeRow(
    modifier: Modifier = Modifier,
    onAddToQueue: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val addToQueue by rememberUpdatedState(onAddToQueue)
    val remove by rememberUpdatedState(onRemove)
    val haptics = rememberHaptics()
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { addToQueue?.invoke(); false }
                SwipeToDismissBoxValue.EndToStart -> { remove?.invoke(); remove != null }
                SwipeToDismissBoxValue.Settled -> true
            }
        },
    )
    LaunchedEffect(state.targetValue) {
        if (state.targetValue != SwipeToDismissBoxValue.Settled) haptics.perform(HapticKind.Threshold)
    }
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = onAddToQueue != null,
        enableDismissFromEndToStart = onRemove != null,
        backgroundContent = {
            val direction = state.dismissDirection
            if (direction != SwipeToDismissBoxValue.Settled) {
                val armed = state.targetValue != SwipeToDismissBoxValue.Settled
                SwipeBackground(isRemove = direction == SwipeToDismissBoxValue.EndToStart, armed = armed)
            }
        },
    ) {
        // TalkBack users cannot swipe: expose both actions as custom
        // accessibility actions on the row itself (the context menu is not
        // always present in every list that uses this row).
        val queueLabel = stringResource(R.string.cmp_add_to_queue)
        val removeLabel = stringResource(R.string.cmp_remove)
        Box(
            Modifier
                .background(MaterialTheme.tunedColors.bg)
                .semantics {
                    customActions = listOfNotNull(
                        if (onAddToQueue != null) CustomAccessibilityAction(queueLabel) { addToQueue?.invoke(); true } else null,
                        if (onRemove != null) CustomAccessibilityAction(removeLabel) { remove?.invoke(); true } else null,
                    )
                },
        ) { content() }
    }
}

@Composable
private fun SwipeBackground(isRemove: Boolean, armed: Boolean = true) {
    val colors = MaterialTheme.tunedColors
    val motion = rememberMotion()
    val iconScale by animateFloatAsState(if (armed) 1f else 0.6f, motion.spring(), label = "swipeIcon")
    Box(
        Modifier
            .fillMaxSize()
            .background(if (isRemove) colors.danger else colors.accent)
            .padding(horizontal = Keyline.margin),
        contentAlignment = if (isRemove) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Icon(
            imageVector = if (isRemove) Icons.Filled.DeleteOutline else Icons.AutoMirrored.Filled.PlaylistAdd,
            contentDescription = stringResource(if (isRemove) R.string.cmp_remove else R.string.cmp_add_to_queue),
            tint = if (isRemove) colors.bg else colors.onAccent,
            modifier = Modifier.size(Sizes.icon).graphicsLayer { scaleX = iconScale; scaleY = iconScale },
        )
    }
}

@ComponentPreviews
@Composable
private fun SwipeRowPreview() = PreviewTheme { SwipeRowSample() }

/** Sample states, shared by the preview and the in-app component gallery. */
@Composable
internal fun SwipeRowSample() {
    Column {
        SwipeRow(onAddToQueue = {}, onRemove = {}) {
            TrackRow("Swipe me", "Right to queue, left to remove", onClick = {}, onMoreClick = {})
        }
        Box(Modifier.size(Sizes.row)) { SwipeBackground(isRemove = false) }
        Box(Modifier.size(Sizes.row)) { SwipeBackground(isRemove = true) }
    }
}

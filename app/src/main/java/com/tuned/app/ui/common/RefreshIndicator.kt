package com.tuned.app.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Pull-to-refresh with Tuned's spinner: a small round chip that slides down
 * with the pull, its arc growing as it goes, and spins while refreshing. Put
 * it in PullToRefreshBox's indicator slot, aligned to the top centre.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshIndicator(state: PullToRefreshState, isRefreshing: Boolean, modifier: Modifier = Modifier) {
    val pulled = state.distanceFraction
    if (!isRefreshing && pulled <= 0f) return
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 3.dp,
        modifier = modifier.semantics {
            if (isRefreshing) progressBarRangeInfo = androidx.compose.ui.semantics.ProgressBarRangeInfo.Indeterminate
        }
            .size(40.dp)
            .graphicsLayer {
                // Hidden above the top edge at rest, 24dp down at the threshold.
                val travel = if (isRefreshing) 1f else pulled.coerceAtMost(1.25f)
                translationY = travel * 64.dp.toPx() - 40.dp.toPx()
            },
    ) {
        PullSpinner(progress = pulled, refreshing = isRefreshing, modifier = Modifier.padding(10.dp))
    }
}

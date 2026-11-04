package com.tuned.app.ui.player

import com.tuned.app.ui.common.SheetInk
import com.tuned.app.ui.theme.tunedColors
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.Sizes
import com.tuned.app.ui.theme.Keyline
import com.tuned.app.ui.theme.AppType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.outlined.Cast
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tuned.app.R
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import com.tuned.app.playback.MusicPlayer

@Composable
internal fun PlayerCastMenuRow(player: MusicPlayer) {
    val context = LocalContext.current
    val castUnavailableMessage = stringResource(R.string.cast_unavailable)
    // State, not a remember() snapshot: a transient init failure used to stick
    // for the row's whole life. Tapping the row now retries initialization.
    var available by remember(player) { mutableStateOf(player.initializeCast()) }
    var routeButton by remember { mutableStateOf<MediaRouteButton?>(null) }
    Row(
        Modifier.fillMaxWidth().clickable {
            if (!available) available = player.initializeCast()
            if (available) routeButton?.performClick()
            else Toast.makeText(context, castUnavailableMessage, Toast.LENGTH_SHORT).show()
        }.padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val badge = Modifier.size(40.dp).clip(RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
        if (available) {
            AndroidView(
                modifier = badge,
                factory = { viewContext ->
                    MediaRouteButton(viewContext).apply {
                        background = null
                        contentDescription = viewContext.getString(R.string.player_cast_cd)
                        CastButtonFactory.setUpMediaRouteButton(viewContext, this)
                        routeButton = this
                    }
                },
            )
        } else {
            Icon(Icons.Filled.Cast, null, badge.padding(10.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(16.dp))
        Text(stringResource(R.string.cast_title), style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * The system cast picker without a visible button: each time [requests] goes
 * up, the picker opens (or a short note says casting is unavailable). Used by
 * the Cast tile in the song menu.
 */
@Composable
internal fun PlayerCastLauncher(player: MusicPlayer, requests: Int) {
    val context = LocalContext.current
    val castUnavailableMessage = stringResource(R.string.cast_unavailable)
    var available by remember(player) { mutableStateOf(player.initializeCast()) }
    var routeButton by remember { mutableStateOf<MediaRouteButton?>(null) }
    if (available) {
        AndroidView(
            modifier = Modifier.size(Sizes.hairline).alpha(0f),
            factory = { viewContext ->
                MediaRouteButton(viewContext).apply {
                    background = null
                    CastButtonFactory.setUpMediaRouteButton(viewContext, this)
                    routeButton = this
                }
            },
        )
    }
    LaunchedEffect(requests) {
        if (requests == 0) return@LaunchedEffect
        // Retry init on each new request instead of caching a first failure.
        if (!available) available = player.initializeCast()
        if (available) routeButton?.performClick()
        else Toast.makeText(context, castUnavailableMessage, Toast.LENGTH_SHORT).show()
    }
}

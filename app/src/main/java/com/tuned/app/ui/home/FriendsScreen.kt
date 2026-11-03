package com.tuned.app.ui.home

import com.tuned.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tuned.app.ui.common.ArtworkImage
import com.tuned.app.ui.common.ErrorState
import com.tuned.app.ui.common.GroupGap
import com.tuned.app.ui.common.adaptiveContentWidth
import com.tuned.app.ui.common.ExpressiveGroupTrackRow
import com.tuned.app.ui.common.ExpressiveHeader
import com.tuned.app.ui.common.groupPositionFor
import com.tuned.app.ui.player.LocalMiniPlayerScrollClearance
import com.tuned.app.ui.common.safeDrawingBottomPadding
import com.tuned.app.ui.common.safeHorizontalContentPadding
private val FriendsContainerShape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

/**
 * A real pushed screen now, not a Dialog/ModalBottomSheet — see the long
 * comment trail this replaced in HomeScreen.kt's old FriendsSheet for the
 * full history. Every other pushed screen in this app (Settings, Search,
 * Discover, Genres) already renders correctly edge-to-edge
 * using this exact Scaffold-less ExpressiveHeader + Column pattern, so
 * Friends now just follows the same proven structure instead of being a
 * one-off overlay fighting Compose's Dialog/BottomSheet sizing quirks.
 *
 * Shares HomeViewModel with the Home tab (see NavGraph.kt — scoped to
 * MainShell's own back stack entry) rather than owning its own state, so
 * switching to a friend's profile here is immediately reflected on Home
 * once you navigate back.
 */
@Composable
fun FriendsScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    onOpenFriendProfile: (username: String, displayName: String?, avatarUrl: String?) -> Unit = { _, _, _ -> },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Loads the friends list the first time this screen is reached, same
    // trigger openFriendsSheet() used to provide via the old sheet's open
    // action — now driven by navigation arriving here instead.
    LaunchedEffect(Unit) {
        viewModel.openFriendsSheet()
    }

    val friends = uiState.sortedFriends
    val pinnedFriends = uiState.pinnedFriends
    val isLoading = uiState.isLoadingFriends
    val friendsError = uiState.friendsError
    val isViewingFriend = uiState.isViewingFriend

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .adaptiveContentWidth(maxWidth = 760.dp),
        ) {
        ExpressiveHeader(
            title = "Friends",
            subtitle = if (friends.isNotEmpty()) "Long-press a friend to pin" else null,
            onBack = onBack,
        )

        androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp))

        // Wrapped in the app's own rounded "list container" surface, same
        // idea as Home's own track list — but using a shape rounded ONLY
        // at the top (matching Home's own local override, not the theme
        // default's all-four-corners version): rounding the BOTTOM
        // corners too, on a box that's flush with the true screen edge,
        // clips those corners right at the physical edge and leaves a
        // small curved sliver of the plain screen background showing
        // through in both bottom corners — exactly the persistent "gap
        // near the nav gesture area" this was. Flush + square at the
        // bottom is what actually reads as genuinely edge-to-edge; the
        // "container's end" is instead made visible by real bottom
        // padding around the last row (below), not a corner cutout.
        Box(
            Modifier
                .fillMaxSize()
                .clip(FriendsContainerShape)
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            // Lazy: a long friends list used to be composed in full inside a
            // scrolling Column. Keys are usernames, so pin/reorder keeps each
            // row's state attached to the friend, not the position.
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .safeHorizontalContentPadding(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 12.dp,
                    bottom = 24.dp + LocalMiniPlayerScrollClearance.current + safeDrawingBottomPadding(),
                ),
            ) {
            when {
                isLoading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        com.tuned.app.ui.common.ExpressiveLoadingIndicator(message = "Loading friends")
                    }
                }
                // A failed fetch is an error with a retry, not "no friends":
                // an outage or rate-limit used to render exactly like an
                // account with zero friends.
                friendsError && friends.isEmpty() -> item(key = "error") {
                    ErrorState(
                        message = stringResource(R.string.fr_load_error),
                        onRetry = { viewModel.openFriendsSheet(forceRefresh = true) },
                    )
                }
                friends.isEmpty() -> item(key = "empty") {
                    Text(
                        "No friends found on this Last.fm account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                else -> {
                    val rowCount = friends.size + if (isViewingFriend) 1 else 0
                    items(
                        count = rowCount,
                        key = { index ->
                            if (isViewingFriend && index == 0) "self" else friends[index - if (isViewingFriend) 1 else 0].name
                        },
                    ) { index ->
                        val position = groupPositionFor(index, rowCount)
                        Box(Modifier.padding(bottom = if (index < rowCount - 1) GroupGap else 0.dp)) {
                        if (isViewingFriend && index == 0) {
                            ExpressiveGroupTrackRow(
                                title = "Back to my profile",
                                subtitle = "View your own data again",
                                position = position,
                                onClick = {
                                    viewModel.returnToOwnProfile()
                                    onBack()
                                },
                                leading = {
                                    Box(
                                        Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                },
                            )
                        } else {
                            val friend = friends[index - if (isViewingFriend) 1 else 0]
                            val isPinned = friend.name in pinnedFriends
                            ExpressiveGroupTrackRow(
                                title = friend.displayName,
                                subtitle = "@${friend.name}",
                                position = position,
                                onClick = {
                                    onOpenFriendProfile(friend.name, friend.displayName, friend.avatarUrl)
                                },
                                onLongClick = { viewModel.toggleFriendPinned(friend.name) },
                                leading = {
                                    if (!friend.avatarUrl.isNullOrBlank()) {
                                        ArtworkImage(
                                            name = "friend",
                                            artist = friend.name,
                                            embeddedUrl = friend.avatarUrl,
                                            fallbackIcon = Icons.Filled.AccountCircle,
                                            modifier = Modifier.size(44.dp).clip(CircleShape),
                                        )
                                    } else {
                                        Box(
                                            Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                },
                                trailing = if (isPinned) {
                                    { Icon(Icons.Filled.PushPin, contentDescription = stringResource(R.string.pl_pinned_cd), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
                                } else null,
                            )
                        }
                        }
                    }
                }
            }
        }
        }
    }
}
}

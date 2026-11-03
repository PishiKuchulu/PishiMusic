package com.tuned.app.ui.shell

import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Home
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.NavigationBarItem
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import android.view.HapticFeedbackConstants
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalView
import android.graphics.Bitmap
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.core.graphics.scale
import com.tuned.app.ui.theme.drawInteractiveGlass
import com.tuned.app.ui.theme.liquidGlass
import com.tuned.app.ui.theme.rememberGlassInteraction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.nio.IntBuffer
import kotlin.time.Duration.Companion.seconds
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuned.app.ui.common.ExpressiveMotion
import com.tuned.app.ui.common.PredictiveBackScreen
import com.tuned.app.ui.common.adaptiveContentWidth
import com.tuned.app.ui.feed.FeedScreen
import com.tuned.app.ui.home.HomeScreen
import com.tuned.app.ui.player.LocalMiniPlayerScrollClearance
import com.tuned.app.ui.playlist.PlaylistScreen
import androidx.compose.foundation.shape.CornerBasedShape
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import kotlin.math.sign
import com.tuned.app.ui.theme.LocalIsDarkTheme
import com.tuned.app.ui.theme.LocalLiquidGlass
import com.tuned.app.ui.theme.LocalMd3NavBar
import com.tuned.app.ui.theme.LocalHideNavTabs
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.Orientation
import com.tuned.app.ui.theme.LiquidGlassPreset
import com.tuned.app.ui.theme.liquidGlassChrome
import com.tuned.app.ui.theme.liquidGlassContainerColor
import com.tuned.app.ui.theme.LayerBackdrop
import com.tuned.app.ui.theme.SquircleShape
import com.tuned.app.ui.theme.rememberLayerBackdrop
import com.tuned.app.ui.theme.isLiquidGlassBackdropSupported
import com.tuned.app.ui.theme.liquidGlassSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.res.stringResource
import com.tuned.app.ui.search.SearchScreen
import com.tuned.app.ui.theme.Keyline
import com.tuned.app.ui.theme.Space
import com.tuned.app.ui.theme.rememberMotion
import androidx.compose.ui.input.nestedscroll.nestedScroll

/** Thin bridge exposing AppUpdateManager's live update state to MainShell */
@HiltViewModel
class MainShellViewModel @Inject constructor(
    val appUpdateManager: com.tuned.app.data.update.AppUpdateManager,
    val ytAuthManager: com.tuned.app.data.ytmusic.YtMusicAuthManager,
    val settingsPreferences: com.tuned.app.data.local.SettingsPreferences,
) : ViewModel() {
    val updateInfo = appUpdateManager.updateInfo
    val ytConnection = ytAuthManager.connection
    val settings = settingsPreferences.settings

    fun dismissUpdate(version: String) {
        appUpdateManager.dismissUpdate(version)
    }

    fun openUpdate(context: android.content.Context) {
        appUpdateManager.openUpdate(context)
    }

}

private enum class MainTab(val labelRes: Int, val icon: ImageVector, val selectedIcon: ImageVector) {
    // Rounded glyphs to match Google Sans Flex: outlined when idle, filled
    // when selected. Library is a record; You (listening stats) a podium chart.
    HOME(com.tuned.app.R.string.nav_home, Icons.Outlined.Home, Icons.Rounded.Home),
    SEARCH(com.tuned.app.R.string.nav_search, Icons.Rounded.Search, Icons.Rounded.Search),
    LIBRARY(com.tuned.app.R.string.nav_library, Icons.Outlined.Album, Icons.Rounded.Album),
    YOU(com.tuned.app.R.string.nav_you, Icons.Outlined.Leaderboard, Icons.Rounded.Leaderboard),
}

/** Shared with any screen hosted inside [MainShell] so their scrolling
 *  lists know how much bottom content padding to reserve — the nav
 *  overlays content (it's not a Scaffold bottomBar reserving space), so
 *  each screen leaves this much room for its last item to clear it. */
object FloatingNavDefaults {
    val ContentBottomPadding = 112.dp

    /**
     * Full bottom clearance for edge-to-edge scrolling content: the floating
     * dock's visual height + margins ([ContentBottomPadding]) PLUS the live
     * navigation-bar (gesture area) inset. Screens that let their list draw
     * beneath the transparent gesture area must use this instead of the raw
     * constant, otherwise the last row hides behind the dock/gesture bar.
     */
    @Composable
    fun contentBottomPadding(): Dp = LocalBottomChromeHeight.current + Space.lg
}



@Composable
fun MainShell(
    onOpenSettings: () -> Unit,
    onOpenArtist: (name: String, browseId: String?) -> Unit,
    onOpenAlbum: (title: String, artist: String, browseId: String?) -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenGenres: () -> Unit,
    onOpenHistory: () -> Unit = {},
    onOpenRecap: () -> Unit = {},
    onOpenYouTubeAccount: () -> Unit = {},
    onOpenFriends: () -> Unit,
    onOpenFriendProfile: (username: String, displayName: String?, avatarUrl: String?) -> Unit = { _, _, _ -> },
    onOpenFeedPlaylist: (String) -> Unit,
    onOpenPlaylist: (Long) -> Unit = {},
    onOpenGenerator: () -> Unit = {},
    onOpenNewReleases: () -> Unit = {},
    onOpenDownloads: () -> Unit = {},
    onOpenPodcasts: () -> Unit = {},
    onOpenPodcastShow: (String) -> Unit = {},
    onOpenYouTubeLogin: () -> Unit = {},
    mainShellViewModel: MainShellViewModel = hiltViewModel(),
) {
    val tabs = MainTab.entries
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    val openSearch = { selectedTab = MainTab.SEARCH }
    // Keeps each tab's saveable UI state (scroll positions, fields) while
    // another tab is showing, so switching back lands where you left.
    val tabStateHolder = rememberSaveableStateHolder()
    val motion = rememberMotion()
    val dockState = LocalDockState.current
    val dockScroll = rememberDockScrollConnection(dockState)
    LaunchedEffect(selectedTab) { dockState.tabsCollapsed = false }
    val updateInfo by mainShellViewModel.updateInfo.collectAsStateWithLifecycle()
    val showUpdateBanner = updateInfo.isUpdateAvailable && !updateInfo.isDismissed
    var showUpdateSheet by rememberSaveable { mutableStateOf(false) }
    if (showUpdateSheet) {
        com.tuned.app.ui.common.UpdateSheet(mainShellViewModel.appUpdateManager) { showUpdateSheet = false }
    }

    var tabDrag by remember { mutableFloatStateOf(0f) }
    val tabSwipeDistance = with(LocalDensity.current) { TabSwipeDistance.toPx() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val backgroundColor = MaterialTheme.colorScheme.background
    // Unconditional remember keeps composition stable; usage gated below.
    val navigationBackdrop = rememberLayerBackdrop {
        drawRect(backgroundColor)
        drawContent()
    }
    val navGlass = isLiquidGlassBackdropSupported()

    Box(Modifier.fillMaxSize()) {
        BackHandler(enabled = selectedTab != MainTab.HOME) { selectedTab = MainTab.HOME }
        // Fade through (plan, motion inventory #2): the old tab fades out
        // quickly, the new one fades in and rises 8dp. With the dock hidden, a sideways
        // swipe changes tab; shelves and row swipe actions get the drag first, so they
        // never fight it.
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                (fadeIn(motion.base()) + slideInVertically(motion.base()) { it / 100 })
                    .togetherWith(fadeOut(motion.fast()))
            },
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(dockScroll)
                .draggable(
                    state = rememberDraggableState { tabDrag += it },
                    orientation = Orientation.Horizontal,
                    enabled = LocalHideNavTabs.current,
                    onDragStarted = { tabDrag = 0f },
                    onDragStopped = { velocity ->
                        val step = tabSwipeStep(tabDrag, velocity, tabSwipeDistance, rtl)
                        selectedTab = tabs[(selectedTab.ordinal + step).coerceIn(0, tabs.lastIndex)]
                    },
                )
                .liquidGlassSource(if (navGlass) navigationBackdrop else null),
            label = "tabs",
        ) { tab ->
            tabStateHolder.SaveableStateProvider(tab.name) {
                when (tab) {
                    MainTab.HOME -> FeedScreen(
                        onOpenSettings = onOpenSettings,
                        onOpenSearch = openSearch,
                        onOpenDiscover = onOpenDiscover,
                        onOpenPlaylist = onOpenPlaylist,
                        onOpenFeedPlaylist = onOpenFeedPlaylist,
                        onOpenGenerator = onOpenGenerator,
                        onOpenFriends = onOpenFriends,
                        onOpenFriendProfile = onOpenFriendProfile,
                        onOpenNewReleases = onOpenNewReleases,
                        onOpenDownloads = onOpenDownloads,
                        onOpenPodcasts = onOpenPodcasts,
                        onOpenPodcastShow = onOpenPodcastShow,
                    )
                    MainTab.SEARCH -> SearchScreen(
                        embedded = true,
                        onOpenArtist = onOpenArtist,
                        onOpenAlbum = onOpenAlbum,
                        onOpenPlaylist = onOpenFeedPlaylist,
                    )
                    MainTab.LIBRARY -> PlaylistScreen(onOpenPlaylist = onOpenPlaylist, onOpenGenerator = onOpenGenerator)
                    MainTab.YOU -> HomeScreen(
                        onOpenSettings = onOpenSettings,
                        onOpenSearch = openSearch,
                        onOpenDiscover = onOpenDiscover,
                        onOpenGenres = onOpenGenres,
                        onOpenHistory = onOpenHistory,
                        onOpenRecap = onOpenRecap,
                        onOpenYouTubeAccount = onOpenYouTubeAccount,
                        onOpenFriends = onOpenFriends,
                        onOpenDownloads = onOpenDownloads,
                    )
                }
            }
        }

        // App update prompt banner (only shown on app open when an update is available and not dismissed)
        AnimatedVisibility(
            visible = showUpdateBanner,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .adaptiveContentWidth(maxWidth = 600.dp)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .zIndex(10f),
        ) {
            UpdatePromptCard(
                version = updateInfo.latestVersion,
                onUpdate = { showUpdateSheet = true },
                onDismiss = { mainShellViewModel.dismissUpdate(updateInfo.latestVersion) },
            )
        }

        val bottomNavSlot = com.tuned.app.ui.player.LocalBottomNavSlot.current
        val chromeHeight = LocalBottomChromeHeight.current
        val tabLabels = tabs.map { stringResource(it.labelRes) }
        val destinations = remember(tabLabels) {
            tabs.mapIndexed { index, tab -> NavDestination(tabLabels[index], tab.icon, tab.selectedIcon) }
        }
        val currentTab by rememberUpdatedState(selectedTab)
        DisposableEffect(bottomNavSlot, destinations) {
            bottomNavSlot.value = {
                FloatingDock(
                    destinations = destinations,
                    selectedIndex = currentTab.ordinal,
                    onSelect = { selectedTab = tabs[it] },
                )
            }
            onDispose { bottomNavSlot.value = {} }
        }
    }
}

private val TabSwipeDistance = 72.dp
private const val TAB_SWIPE_VELOCITY = 800f

/** Which way a finished sideways swipe moves between tabs: -1, 0 or 1. */
internal fun tabSwipeStep(dragPx: Float, velocity: Float, distancePx: Float, rtl: Boolean): Int {
    val forward = when {
        dragPx <= -distancePx || velocity <= -TAB_SWIPE_VELOCITY -> 1
        dragPx >= distancePx || velocity >= TAB_SWIPE_VELOCITY -> -1
        else -> 0
    }
    return if (rtl) -forward else forward
}

@Composable
private fun UpdatePromptCard(
    version: String,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.CloudDownload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = androidx.compose.ui.res.stringResource(com.tuned.app.R.string.update_available),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = androidx.compose.ui.res.stringResource(com.tuned.app.R.string.update_new, version),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                )
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onUpdate),
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(com.tuned.app.R.string.update),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = androidx.compose.ui.res.stringResource(com.tuned.app.R.string.dismiss_update),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

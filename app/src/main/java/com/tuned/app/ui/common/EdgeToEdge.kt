package com.tuned.app.ui.common

import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import android.view.ViewParent
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

/** Keeps interactive content clear of side cutouts while its parent remains full-bleed. */
@Composable
fun Modifier.safeHorizontalContentPadding(): Modifier =
    windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))

/** Bottom clearance covering gesture navigation, three-button navigation, and cutouts. */
@Composable
fun safeDrawingBottomPadding(): Dp =
    WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()

/**
 * Ensures a Dialog or ModalBottomSheet window properly renders edge-to-edge
 * with transparent navigation and status bars, disabled contrast scrims,
 * and hardware blur-behind on supported Android versions (API 31+).
 */
@Composable
fun EdgeToEdgeDialogWindow(visible: Boolean = true) {
    val view = LocalView.current
    // The screen behind blurs in as the sheet rises and clears as it slides
    // away, instead of switching on and off in one frame.
    var blurTarget by remember { mutableStateOf<View?>(null) }
    val blur = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        // Rises fast at the start (LinearOutSlowIn), so the screen is already soft while the sheet
        // is still coming up; a slow-start curve left it sharp until the sheet had arrived.
        blur.animateTo(
            if (visible) DIALOG_BLUR_RADIUS else 0f,
            if (visible) tween(240, easing = LinearOutSlowInEasing) else tween(260),
        )
    }
    // Read in composition, so every animation frame recomposes and re-applies it.
    val r = blur.value
    val target = blurTarget
    SideEffect {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                target?.setRenderEffect(if (r < 0.5f) null else RenderEffect.createBlurEffect(r, r, Shader.TileMode.CLAMP))
            }
        }
    }
    DisposableEffect(view) {
        var dialogWindow: Window? = null
        var isDialog = false

        var current: ViewParent? = view.parent
        while (current != null) {
            if (current is DialogWindowProvider) {
                dialogWindow = current.window
                isDialog = true
                break
            }
            // NOTE: android.app.Dialog extends neither Context nor ViewParent,
            // so `is Dialog` checks are provably dead (compiler error) and a
            // Dialog never appears in a view-parent chain either. Dialog
            // windows are found via DialogWindowProvider above.
            current = current.parent
        }

        var activity: Activity? = null
        var actCtx: Context? = view.context
        while (actCtx is ContextWrapper) {
            if (actCtx is Activity) {
                activity = actCtx
                break
            }
            actCtx = actCtx.baseContext
        }
        if (activity == null) {
            var rootCtx: Context? = view.rootView.context
            while (rootCtx is ContextWrapper) {
                if (rootCtx is Activity) {
                    activity = rootCtx
                    break
                }
                rootCtx = rootCtx.baseContext
            }
        }

        val actDecor = activity?.window?.decorView
        if (dialogWindow == null && view.rootView != actDecor) {
            isDialog = true
        }

        val targetWindow = dialogWindow ?: activity?.window
        targetWindow?.let { w ->
            runCatching {
                WindowCompat.setDecorFitsSystemWindows(w, false)
                w.navigationBarColor = android.graphics.Color.TRANSPARENT
                w.statusBarColor = android.graphics.Color.TRANSPARENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    w.isNavigationBarContrastEnforced = false
                    w.isStatusBarContrastEnforced = false
                }
                if (isDialog) {
                    w.setDimAmount(0.28f)
                }
            }
        }

        var blurredView: View? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && activity != null) {
            val bgView = activity.findViewById<View>(android.R.id.content) ?: actDecor
            if (bgView != null && bgView != view.rootView) {
                blurredView = bgView
                blurTarget = bgView
            }
        }

        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                runCatching {
                    blurredView?.setRenderEffect(null)
                }
                blurTarget = null
            }
        }
    }
}

/** Blur on the screen behind a sheet or dialog. */
private const val DIALOG_BLUR_RADIUS = 36f

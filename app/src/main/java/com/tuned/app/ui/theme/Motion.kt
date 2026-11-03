package com.tuned.app.ui.theme

import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * Motion tokens. Four timings cover the app; each falls back to an instant
 * change when the system "Remove animations" setting is on.
 */
@Immutable
class Motion(
    /** True when the system "Remove animations" setting is on. */
    val reduced: Boolean,
) {
    /** 120ms. Press states, chip selection. */
    fun <T> fast(): FiniteAnimationSpec<T> =
        if (reduced) snap() else tween(FAST_MS, easing = Standard)

    /** 220ms. Sheets opening, content swaps. */
    fun <T> base(): FiniteAnimationSpec<T> =
        if (reduced) snap() else tween(BASE_MS, easing = EmphasizedDecelerate)

    /** 360ms. Player expand, shared elements. */
    fun <T> slow(): FiniteAnimationSpec<T> =
        if (reduced) snap() else tween(SLOW_MS, easing = Emphasized)

    /** Drags and swipes settling. Critically damped, no overshoot. */
    fun <T> spring(): FiniteAnimationSpec<T> =
        if (reduced) snap() else spring(dampingRatio = 1f, stiffness = 400f)

    companion object {
        const val FAST_MS = 120
        const val BASE_MS = 220
        const val SLOW_MS = 360

        val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
        // Single-curve approximation of Material's two-segment emphasized easing.
        val Emphasized = CubicBezierEasing(0.3f, 0f, 0f, 1f)
    }
}

/** Motion tokens that respect the system animation scale. */
@Composable
fun rememberMotion(): Motion {
    val context = LocalContext.current
    return remember(context) {
        val scale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        Motion(reduced = scale == 0f)
    }
}

/** The four kinds of haptic feedback Tuned uses. Everything routes through these. */
enum class HapticKind {
    /** Slider steps and picker detents. */
    Tick,
    /** Like, add to queue, download started. */
    Confirm,
    /** A blocked action. */
    Reject,
    /** A swipe crosses its action point. */
    Threshold,
}

@Immutable
class Haptics(private val view: View) {
    fun perform(kind: HapticKind) {
        view.performHapticFeedback(constantFor(kind))
    }

    private fun constantFor(kind: HapticKind): Int = when (kind) {
        HapticKind.Tick ->
            if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.CLOCK_TICK
        HapticKind.Confirm ->
            if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CONTEXT_CLICK
        HapticKind.Reject ->
            if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
        HapticKind.Threshold ->
            if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE else HapticFeedbackConstants.CONTEXT_CLICK
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}

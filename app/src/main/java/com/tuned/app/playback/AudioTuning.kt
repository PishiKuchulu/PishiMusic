package com.tuned.app.playback

import kotlin.math.log2

/**
 * Global 432Hz tuning mode: every playback pitch is scaled so A4 sounds at
 * 432 Hz instead of the conventional 440 Hz.
 *
 * The transformation itself is performed by Media3's Sonic stage inside the
 * PCM16 delegate sink (the same stage that already runs user speed/pitch —
 * see NativeProcessingAudioSink.tryConfigureNativePath). Sonic applies the
 * pitch factor while preserving tempo and duration, so this object only
 * owns the math and the single composition rule: the tuning factor is
 * multiplied into the USER's pitch exactly once, at the sink boundary,
 * from the user's requested parameters — never from already-effective
 * parameters — so it cannot be applied twice.
 */
object AudioTuning {

    /** Conventional tuning reference the feature retunes FROM. */
    const val REFERENCE_A4_HZ = 440.0

    /** Tuning reference applied while the mode is on. */
    const val TARGET_A4_HZ = 432.0

    /** Output frequency = input frequency × PITCH_FACTOR (0.981818…). */
    const val PITCH_FACTOR: Float = (TARGET_A4_HZ / REFERENCE_A4_HZ).toFloat()

    /** The shift in cents: 1200 × log2(432/440) ≈ -31.77 cents. */
    val SHIFT_CENTS: Double = 1200.0 * log2(TARGET_A4_HZ / REFERENCE_A4_HZ)

    /**
     * The pitch to hand to the audio sink for a user's requested [userPitch]:
     * the user's own pitch factor with the tuning factor composed in while
     * the mode is on; the user's pitch untouched while it is off, so OFF is
     * a true bypass of the original playback parameters.
     */
    fun effectivePitch(userPitch: Float, enabled: Boolean): Float =
        if (enabled) userPitch * PITCH_FACTOR else userPitch

    /** True when the effective pitch differs from unity and Sonic must run. */
    fun isShifted(userPitch: Float, enabled: Boolean): Boolean =
        effectivePitch(userPitch, enabled) != 1f

    /** What a tuning toggle must arm in the sink so the audible route can
     *  carry the new effective pitch. Pure decision, unit-tested; the sink
     *  supplies the live state and performs the action on its render thread. */
    enum class TuningRouteAction {
        /** Nothing to reroute: the current route already carries the
         *  effective parameters, or the toggle is on the way out (routes
         *  restore through adoption / the next configure, the sink's
         *  standing "only on the way in" discipline). */
        NONE,

        /** Reroute the open Android-path stream (float delegate or internal
         *  direct) onto the Sonic-carrying PCM16 route. */
        REROUTE_ANDROID_PATH,

        /** Leave the bit-exact exclusive USB stream at a buffer boundary;
         *  it has no processing stage at all. */
        LEAVE_EXCLUSIVE_USB,
    }

    fun routeActionOnToggle(
        enabling: Boolean,
        usbExclusiveActive: Boolean,
        currentlyEffectivelyShifted: Boolean,
    ): TuningRouteAction = when {
        !enabling -> TuningRouteAction.NONE
        usbExclusiveActive -> TuningRouteAction.LEAVE_EXCLUSIVE_USB
        !currentlyEffectivelyShifted -> TuningRouteAction.REROUTE_ANDROID_PATH
        else -> TuningRouteAction.NONE
    }
}

package com.tuned.app.playback

import com.google.common.truth.Truth.assertThat
import kotlin.math.pow
import org.junit.Test

/** 432Hz tuning composition: the factor, the OFF bypass, single application. */
class AudioTuningTest {

    @Test
    fun factorIsExactly432Over440() {
        assertThat(AudioTuning.PITCH_FACTOR).isWithin(1e-7f).of((432.0 / 440.0).toFloat())
        assertThat(AudioTuning.PITCH_FACTOR).isWithin(1e-6f).of(0.9818182f)
    }

    @Test
    fun shiftIsAboutMinus31Point77Cents() {
        assertThat(AudioTuning.SHIFT_CENTS).isWithin(0.01).of(-31.77)
        // A 440 Hz tone retuned by the factor lands on 432 Hz.
        assertThat(440.0 * AudioTuning.PITCH_FACTOR).isWithin(0.01).of(432.0)
        // And the cents figure agrees with the factor: 2^(cents/1200) == factor.
        assertThat(2.0.pow(AudioTuning.SHIFT_CENTS / 1200.0))
            .isWithin(1e-6)
            .of(AudioTuning.PITCH_FACTOR.toDouble())
    }

    @Test
    fun offIsATrueBypass() {
        for (pitch in listOf(0.5f, 0.9f, 1f, 1.1f, 2f)) {
            assertThat(AudioTuning.effectivePitch(pitch, enabled = false)).isEqualTo(pitch)
            assertThat(AudioTuning.isShifted(pitch, enabled = false)).isEqualTo(pitch != 1f)
        }
    }

    @Test
    fun onComposesTheFactorOntoTheUserPitchOnce() {
        assertThat(AudioTuning.effectivePitch(1f, enabled = true))
            .isWithin(1e-7f).of(AudioTuning.PITCH_FACTOR)
        // A user's own pitch setting is scaled, not replaced.
        assertThat(AudioTuning.effectivePitch(1.1f, enabled = true))
            .isWithin(1e-6f).of(1.1f * AudioTuning.PITCH_FACTOR)
        assertThat(AudioTuning.isShifted(1f, enabled = true)).isTrue()
    }

    @Test
    fun pitchCompensatingTheFactorCancelsTheTuning() {
        // A user pitch of 440/432 cancels the tuning (to float precision).
        val compensating = (440.0 / 432.0).toFloat()
        assertThat(AudioTuning.effectivePitch(compensating, enabled = true))
            .isWithin(1e-6f).of(1f)
    }

    @Test
    fun repeatedTogglesNeverDrift() {
        // ON -> OFF -> ON -> OFF (cycle 0 is ON): the composition always
        // starts from the user's own pitch, so the effective pitch is
        // identical after every ON and exactly the user's after every OFF —
        // never factor^2, ^3...
        val userPitch = 1.05f
        repeat(4) { cycle ->
            val enabled = cycle % 2 == 0
            val expected = if (enabled) userPitch * AudioTuning.PITCH_FACTOR else userPitch
            assertThat(AudioTuning.effectivePitch(userPitch, enabled)).isEqualTo(expected)
        }
    }

    @Test
    fun routeActionOnToggleTruthTable() {
        // Enabling onto an open exclusive stream: leave it (no processing there).
        assertThat(
            AudioTuning.routeActionOnToggle(
                enabling = true, usbExclusiveActive = true, currentlyEffectivelyShifted = false,
            ),
        ).isEqualTo(AudioTuning.TuningRouteAction.LEAVE_EXCLUSIVE_USB)
        // Enabling at unity on an Android route: reroute onto the Sonic route.
        assertThat(
            AudioTuning.routeActionOnToggle(
                enabling = true, usbExclusiveActive = false, currentlyEffectivelyShifted = false,
            ),
        ).isEqualTo(AudioTuning.TuningRouteAction.REROUTE_ANDROID_PATH)
        // Enabling while already shifted (user tempo): the route already
        // carries Sonic; only the parameters change.
        assertThat(
            AudioTuning.routeActionOnToggle(
                enabling = true, usbExclusiveActive = false, currentlyEffectivelyShifted = true,
            ),
        ).isEqualTo(AudioTuning.TuningRouteAction.NONE)
        // Disabling never forces a reroute, from any state ("only on the way in").
        for (exclusive in listOf(false, true)) {
            for (shifted in listOf(false, true)) {
                assertThat(
                    AudioTuning.routeActionOnToggle(
                        enabling = false, usbExclusiveActive = exclusive, currentlyEffectivelyShifted = shifted,
                    ),
                ).isEqualTo(AudioTuning.TuningRouteAction.NONE)
            }
        }
    }
}

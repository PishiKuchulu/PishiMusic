package com.tuned.app.playback

import com.google.common.truth.Truth.assertThat
import com.tuned.app.playback.analysis.OnsetEnvelope
import com.tuned.app.playback.analysis.TempoEstimate
import com.tuned.app.playback.analysis.beatDriftMs
import com.tuned.app.playback.analysis.beatLockSpeed
import com.tuned.app.playback.analysis.blendBassCut
import com.tuned.app.playback.analysis.estimateTempo
import com.tuned.app.playback.analysis.planBeatMatch
import com.tuned.app.playback.analysis.wholeBarsMs
import org.junit.Test
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class BeatMatchTest {
    private val rate = 44_100

    /** Mono kicks at [bpm], the first [offsetMs] in, for [seconds]. */
    private fun envelope(bpm: Double, offsetMs: Int = 0, seconds: Int = 30): FloatArray {
        val onset = OnsetEnvelope(rate)
        val period = rate * 60.0 / bpm
        val offset = rate * offsetMs / 1000
        val values = ArrayList<Float>()
        for (i in 0 until rate * seconds) {
            val into = (i - offset).takeIf { it >= 0 }?.let { (it % period) } ?: Double.MAX_VALUE
            val sample = if (into < rate / 10) 0.8 * exp(-into / (rate / 40.0)) * sin(2 * PI * 60 * into / rate) else 0.0
            val value = onset.feed(sample)
            if (!value.isNaN()) values += value
        }
        return values.toFloatArray()
    }

    private fun tempo(bpm: Double, phaseMs: Double = 0.0, confidence: Float = 0.5f) =
        TempoEstimate(bpm, 60_000.0 / bpm, phaseMs, confidence)

    @Test
    fun findsTheTempoOfASteadyBeat() {
        for (bpm in listOf(90.0, 120.0, 128.0, 140.0)) {
            assertThat(estimateTempo(envelope(bpm))!!.bpm).isWithin(1.0).of(bpm)
        }
    }

    @Test
    fun findsWhereTheBeatFalls() {
        val found = estimateTempo(envelope(120.0, offsetMs = 200))!!
        val off = (found.phaseMs - 200.0).mod(found.periodMs)
        assertThat(minOf(off, found.periodMs - off)).isLessThan(40.0)
    }

    @Test
    fun aBusyMixStillGivesTheRightTempo() {
        // Kicks at 120 with noise and an off-beat hat, closer to a real song.
        val onset = OnsetEnvelope(rate)
        val random = java.util.Random(3)
        val values = ArrayList<Float>()
        for (i in 0 until rate * 20) {
            val kick = (i % (rate / 2)).toDouble()
            val hat = ((i + rate / 4) % (rate / 2)).toDouble()
            var sample = 0.02 * (random.nextDouble() * 2 - 1)
            if (kick < rate / 10) sample += 0.8 * exp(-kick / (rate / 40.0)) * sin(2 * PI * 60 * kick / rate)
            if (hat < rate / 40) sample += 0.2 * exp(-hat / (rate / 200.0)) * (random.nextDouble() * 2 - 1)
            val value = onset.feed(sample)
            if (!value.isNaN()) values += value
        }
        val found = estimateTempo(values.toFloatArray())!!
        assertThat(found.bpm).isWithin(2.0).of(120.0)
        assertThat(found.confidence).isGreaterThan(0.1f)
    }

    @Test
    fun tooLittleAudioGivesNoTempo() {
        assertThat(estimateTempo(FloatArray(100))).isNull()
    }

    @Test
    fun silenceGivesNoTempo() {
        assertThat(estimateTempo(FloatArray(3000))).isNull()
    }

    @Test
    fun closeTemposMatchByStretchingTheIncomingSong() {
        val match = planBeatMatch(tempo(120.0), tempo(125.0), 200_000L, 8_000L)!!
        assertThat(match.speed).isWithin(0.001f).of(0.96f)
    }

    @Test
    fun halfAndDoubleTimeCountAsTheSameTempo() {
        assertThat(planBeatMatch(tempo(140.0), tempo(70.0), 0L, 8_000L)!!.speed).isWithin(0.001f).of(1f)
        assertThat(planBeatMatch(tempo(70.0), tempo(140.0), 0L, 8_000L)!!.speed).isWithin(0.001f).of(1f)
    }

    @Test
    fun farApartTemposDoNotMatch() {
        assertThat(planBeatMatch(tempo(100.0), tempo(125.0), 0L, 8_000L)).isNull()
    }

    @Test
    fun aVagueBeatDoesNotMatch() {
        assertThat(planBeatMatch(tempo(120.0, confidence = 0.05f), tempo(120.0), 0L, 8_000L)).isNull()
        assertThat(planBeatMatch(null, tempo(120.0), 0L, 8_000L)).isNull()
    }

    @Test
    fun theStartLandsTheBeatsTogetherAndNeverInThePast() {
        val outgoing = tempo(120.0, phaseMs = 130.0)
        val incoming = tempo(120.0, phaseMs = 90.0)
        val match = planBeatMatch(outgoing, incoming, 200_123L, 8_000L)!!
        assertThat(match.startAtMs).isAtLeast(200_123L)
        assertThat(match.startAtMs).isLessThan(200_123L + 501L)
        // Starting then, the incoming song's first beat arrives on an outgoing beat.
        val drift = beatDriftMs(match.startAtMs, 0L, outgoing, incoming, match.speed)
        assertThat(kotlin.math.abs(drift)).isLessThan(2.0)
    }

    @Test
    fun blendsAreWholeBars() {
        // 120 BPM: a bar is 2 s.
        assertThat(wholeBarsMs(8_000L, 500.0)).isEqualTo(8_000L)
        assertThat(wholeBarsMs(9_500L, 500.0)).isEqualTo(8_000L)
        assertThat(wholeBarsMs(1_000L, 500.0)).isEqualTo(2_000L)
    }

    @Test
    fun driftIsSignedAndTheShortWayRound() {
        val beat = tempo(120.0)
        assertThat(beatDriftMs(10_050L, 10_000L, beat, beat, 1f)).isWithin(0.01).of(50.0)
        assertThat(beatDriftMs(10_000L, 10_050L, beat, beat, 1f)).isWithin(0.01).of(-50.0)
    }

    @Test
    fun beatLockTrimsGentlyTowardsTheBeat() {
        assertThat(beatLockSpeed(1f, 30.0)).isGreaterThan(1f)
        assertThat(beatLockSpeed(1f, -30.0)).isLessThan(1f)
        assertThat(beatLockSpeed(1f, 400.0)).isWithin(0.0001f).of(1.02f)
    }

    @Test
    fun matchedSongsPlayTogetherAtFullLevelInTheMiddle() {
        val (outgoing, incoming) = com.tuned.app.playback.analysis.beatMatchedGains(0.5f)
        assertThat(outgoing).isEqualTo(1f)
        assertThat(incoming).isEqualTo(1f)
        assertThat(com.tuned.app.playback.analysis.beatMatchedGains(0f)).isEqualTo(1f to 0f)
        assertThat(com.tuned.app.playback.analysis.beatMatchedGains(1f)).isEqualTo(0f to 1f)
    }

    @Test
    fun aBeatlessIntroLayersInsteadOfFading() {
        val clear = tempo(116.0, confidence = 0.18f)
        val beatless = tempo(125.0, confidence = 0.04f)
        fun layer(a: TempoEstimate?, b: TempoEstimate?) = com.tuned.app.playback.analysis.canLayerWithoutMatching(a, b)
        assertThat(layer(clear, beatless)).isTrue()
        assertThat(layer(beatless, clear)).isTrue()
        // Two clear beats at different tempos would clash, and an unread song might.
        assertThat(layer(clear, tempo(90.0, confidence = 0.18f))).isFalse()
        assertThat(layer(clear, null)).isFalse()
    }

    @Test
    fun theBassLinesSwapInTheMiddle() {
        assertThat(blendBassCut(0f, incoming = true)).isEqualTo(1f)
        assertThat(blendBassCut(0f, incoming = false)).isEqualTo(0f)
        assertThat(blendBassCut(1f, incoming = true)).isEqualTo(0f)
        assertThat(blendBassCut(1f, incoming = false)).isEqualTo(1f)
        assertThat(blendBassCut(0.5f, incoming = true)).isWithin(0.01f).of(0.5f)
    }
}

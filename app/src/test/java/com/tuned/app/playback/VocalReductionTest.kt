package com.tuned.app.playback

import androidx.media3.common.C
import com.google.common.truth.Truth.assertThat
import com.tuned.app.playback.analysis.LoudnessMonitor
import com.tuned.app.playback.analysis.PcmInputStage
import com.tuned.app.playback.analysis.VocalControl
import com.tuned.app.playback.analysis.VocalReducer
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class VocalReductionTest {
    private val rate = 44_100

    private fun tone(hz: Double, i: Int) = 0.5 * sin(2 * PI * hz * i / rate)

    /** Peak of the output over the last half of a [seconds] run, after the ramp has settled. */
    private fun peak(seconds: Int = 1, level: Double, l: (Int) -> Double, r: (Int) -> Double): Pair<Double, Double> {
        val reducer = VocalReducer(rate)
        val n = rate * seconds
        var pl = 0.0
        var pr = 0.0
        for (i in 0 until n) {
            reducer.process(l(i), r(i), level)
            if (i > n / 2) {
                pl = maxOf(pl, abs(reducer.left))
                pr = maxOf(pr, abs(reducer.right))
            }
        }
        return pl to pr
    }

    @Test
    fun centredVocalsDropOut() {
        val (l, r) = peak(level = 1.0, l = { tone(1000.0, it) }, r = { tone(1000.0, it) })
        assertThat(l).isLessThan(0.12)
        assertThat(r).isLessThan(0.12)
    }

    @Test
    fun centredBassStays() {
        val (l, _) = peak(level = 1.0, l = { tone(50.0, it) }, r = { tone(50.0, it) })
        assertThat(l).isGreaterThan(0.4)
    }

    @Test
    fun hardPannedAudioStays() {
        // Only on the left: half of it reads as centre, so it dips but stays audible.
        val (l, _) = peak(level = 1.0, l = { tone(1000.0, it) }, r = { 0.0 })
        assertThat(l).isGreaterThan(0.2)
    }

    @Test
    fun oppositePhaseAudioIsUntouched() {
        val (l, r) = peak(level = 1.0, l = { tone(1000.0, it) }, r = { -tone(1000.0, it) })
        assertThat(l).isWithin(0.01).of(0.5)
        assertThat(r).isWithin(0.01).of(0.5)
    }

    /** Level of [hz] in the settled second half of a two second run. */
    private fun toneLevel(hz: Double, level: Double, l: (Int) -> Double, r: (Int) -> Double): Double {
        val reducer = VocalReducer(rate)
        val n = rate * 2
        var re = 0.0
        var im = 0.0
        for (i in 0 until n) {
            reducer.process(l(i), r(i), level)
            if (i >= n / 2) {
                re += reducer.left * kotlin.math.cos(2 * PI * hz * i / rate)
                im += reducer.left * sin(2 * PI * hz * i / rate)
            }
        }
        return 2 * kotlin.math.sqrt(re * re + im * im) / (n - n / 2)
    }

    @Test
    fun aStereoInstrumentSurvivesWhileTheCentredVocalGoes() {
        val left = { i: Int -> tone(1000.0, i) + tone(3000.0, i) }
        val right = { i: Int -> tone(1000.0, i) }
        assertThat(toneLevel(1000.0, 1.0, left, right)).isLessThan(0.12)
        assertThat(toneLevel(3000.0, 1.0, left, right)).isGreaterThan(0.25)
    }

    @Test
    fun fastSyllablesDropToo() {
        // Rap-like: a 180 Hz voice with ten harmonics, 150 ms on and 150 ms off.
        val voice = { i: Int ->
            if ((i * 1000L / rate) % 300 < 150) (1..10).sumOf { 0.05 * sin(2 * PI * 180.0 * it * i / rate) } else 0.0
        }
        val reducer = VocalReducer(rate)
        var input = 0.0
        var output = 0.0
        for (i in 0 until rate * 3) {
            val v = voice(i)
            reducer.process(v, v, 1.0)
            if (i > rate) {
                input += v * v
                output += reducer.left * reducer.left
            }
        }
        assertThat(output / input).isLessThan(0.25)
    }

    @Test
    fun cymbalsAboveTheVocalRangeStay() {
        assertThat(toneLevel(14_000.0, 1.0, { tone(14_000.0, it) }, { tone(14_000.0, it) })).isGreaterThan(0.45)
    }

    @Test
    fun centredDrumHitsStay() {
        // A short noise burst every half second, the same on both sides.
        val random = java.util.Random(7)
        val burst = DoubleArray(rate / 50) { 0.5 * (random.nextDouble() * 2 - 1) }
        val hit = { i: Int -> (i % (rate / 2)).let { if (it < burst.size) burst[it] else 0.0 } }
        val reducer = VocalReducer(rate)
        var input = 0.0
        var output = 0.0
        val n = rate * 3
        for (i in 0 until n) {
            val v = hit(i)
            reducer.process(v, v, 1.0)
            if (i > rate) {
                input += v * v
                output += reducer.left * reducer.left
            }
        }
        assertThat(output / input).isGreaterThan(0.5)
    }

    @Test
    fun aLevelOfZeroAfterUseReturnsToTheDrySignal() {
        val reducer = VocalReducer(rate)
        repeat(rate) { reducer.process(tone(1000.0, it), tone(1000.0, it), 1.0) }
        repeat(rate / 2) { reducer.process(tone(1000.0, it), tone(1000.0, it), 0.0) }
        assertThat(reducer.isIdle).isTrue()
        reducer.process(0.25, -0.5, 0.0)
        assertThat(reducer.left).isEqualTo(0.25)
        assertThat(reducer.right).isEqualTo(-0.5)
    }

    private fun pcm16(vararg samples: Short): ByteBuffer =
        ByteBuffer.allocate(samples.size * 2).order(ByteOrder.nativeOrder()).apply {
            samples.forEach { putShort(it) }
            flip()
        }

    @Test
    fun levelZeroLeavesAudioBitIdentical() {
        val stage = PcmInputStage(LoudnessMonitor(), VocalControl())
        stage.configure(rate, 2, C.ENCODING_PCM_16BIT)
        val buffer = pcm16(1000, 1000, -2000, 500, 32767, -32768)
        val before = ByteArray(buffer.remaining()).also { buffer.duplicate().get(it) }
        stage.process(buffer)
        val after = ByteArray(buffer.remaining()).also { buffer.duplicate().get(it) }
        assertThat(after).isEqualTo(before)
        assertThat(buffer.position()).isEqualTo(0)
    }

    @Test
    fun stageMeasuresLoudness() {
        val monitor = LoudnessMonitor()
        val stage = PcmInputStage(monitor, VocalControl())
        stage.configure(rate, 2, C.ENCODING_PCM_FLOAT)
        // One second of stereo float at half scale.
        val buffer = ByteBuffer.allocate(rate * 2 * 4).order(ByteOrder.nativeOrder())
        repeat(rate * 2) { buffer.putFloat(0.5f) }
        buffer.flip()
        stage.process(buffer)
        assertThat(monitor.recentDb()!!).isWithin(0.1f).of(-6.02f)
    }

    @Test
    fun anUnreadableFormatPassesUntouched() {
        val monitor = LoudnessMonitor()
        val stage = PcmInputStage(monitor, VocalControl().apply { level = 1f })
        stage.configure(0, 0, C.ENCODING_INVALID)
        val buffer = pcm16(1000, 1000)
        val before = ByteArray(buffer.remaining()).also { buffer.duplicate().get(it) }
        stage.process(buffer)
        val after = ByteArray(buffer.remaining()).also { buffer.duplicate().get(it) }
        assertThat(after).isEqualTo(before)
        assertThat(monitor.recentDb()).isNull()
    }
}

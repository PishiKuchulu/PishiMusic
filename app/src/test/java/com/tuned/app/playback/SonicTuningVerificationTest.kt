package com.tuned.app.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.SonicAudioProcessor
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Issue D verification: the REAL production pitch stage — Media3's
 * SonicAudioProcessor (pure Java in media3-common 1.2.1, no native code),
 * driven with the exact factor the app composes in AudioTuning — measured
 * on synthetic PCM. This is the same processor class, in the same PCM16
 * configuration, that DefaultAudioSink's chain uses for playback
 * parameters inside Tuned's PCM16 delegate sink.
 *
 * Frequency is estimated from a Hann-windowed FFT peak with parabolic
 * interpolation over the steady-state middle of the output (Sonic's
 * start/end transients excluded). At 44 100 Hz with a 16 384-point FFT the
 * bin width is ≈2.69 Hz and interpolation brings the estimation error well
 * under ±1 Hz for a pure tone, so the ±3 Hz assertion window is dominated
 * by the DSP, not the measurement. A double application of the factor
 * would land at ≈424.2 Hz — far outside the window — so Test B also
 * proves single application.
 */
@RunWith(RobolectricTestRunner::class)
class SonicTuningVerificationTest {

    private val sampleRate = 44_100
    private val channels = 2

    /** Runs [frames] of per-channel sine waves through Sonic at [pitch]. */
    private fun runSonic(freqsHz: DoubleArray, frames: Int, pitch: Float): Array<DoubleArray> {
        // The generated input, kept so the bypass case can return it.
        val input = Array(channels) { ch ->
            DoubleArray(frames) { i -> sin(2.0 * PI * freqsHz[ch] * i / sampleRate) * 0.5 }
        }
        val processor = SonicAudioProcessor()
        val format = AudioFormat(sampleRate, channels, C.ENCODING_PCM_16BIT)
        val outputFormat = processor.configure(format)
        assertThat(outputFormat.sampleRate).isEqualTo(sampleRate)
        assertThat(outputFormat.channelCount).isEqualTo(channels)
        processor.setSpeed(1f)
        processor.setPitch(pitch)
        processor.flush()
        // The chain contract (DefaultAudioProcessorChain): an inactive
        // processor is bypassed entirely and buffers pass through untouched.
        // At pitch 1.0 SonicAudioProcessor is inactive by design — that
        // bypass IS the production OFF behavior, so assert on the input.
        if (!processor.isActive) return input

        val output = Array(channels) { mutableListOf<Short>() }
        fun drain() {
            while (true) {
                val out = processor.output
                if (!out.hasRemaining()) break
                val dup = out.duplicate().order(ByteOrder.nativeOrder())
                while (dup.remaining() >= 2 * channels) {
                    for (ch in 0 until channels) output[ch].add(dup.getShort())
                }
                out.position(out.limit())
            }
        }

        val chunkFrames = 1024
        var written = 0
        while (written < frames) {
            val n = minOf(chunkFrames, frames - written)
            val buffer = ByteBuffer.allocateDirect(n * channels * 2).order(ByteOrder.nativeOrder())
            for (i in 0 until n) {
                for (ch in 0 until channels) {
                    val sample = (input[ch][written + i] * Short.MAX_VALUE).toInt()
                    buffer.putShort(sample.toShort())
                }
            }
            buffer.flip()
            processor.queueInput(buffer)
            drain()
            written += n
        }
        processor.queueEndOfStream()
        var guard = 0
        while (!processor.isEnded && guard++ < 10_000) drain()
        drain()
        return Array(channels) { ch -> DoubleArray(output[ch].size) { output[ch][it].toDouble() / Short.MAX_VALUE } }
    }

    /** Hann-windowed FFT peak with parabolic interpolation, steady-state middle only. */
    private fun estimateFrequencyHz(samples: DoubleArray, sampleRate: Int): Double {
        val fftSize = 16_384
        val skip = samples.size / 4
        val segment = samples.copyOfRange(skip, minOf(samples.size, skip + fftSize))
        val re = DoubleArray(fftSize)
        val im = DoubleArray(fftSize)
        for (i in segment.indices) {
            val window = 0.5 - 0.5 * kotlin.math.cos(2.0 * PI * i / (segment.size - 1))
            re[i] = segment[i] * window
        }
        fft(re, im)
        var peak = 1
        var peakMag = 0.0
        for (k in 1 until fftSize / 2) {
            val mag = re[k] * re[k] + im[k] * im[k]
            if (mag > peakMag) {
                peakMag = mag
                peak = k
            }
        }
        val alpha = kotlin.math.sqrt(re[peak - 1] * re[peak - 1] + im[peak - 1] * im[peak - 1])
        val beta = kotlin.math.sqrt(peakMag)
        val gamma = kotlin.math.sqrt(re[peak + 1] * re[peak + 1] + im[peak + 1] * im[peak + 1])
        val shift = 0.5 * (alpha - gamma) / (alpha - 2.0 * beta + gamma)
        return (peak + shift) * sampleRate / fftSize
    }

    private fun fft(re: DoubleArray, im: DoubleArray) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                re[i] = re[j].also { re[j] = re[i] }
                im[i] = im[j].also { im[j] = im[i] }
            }
        }
        var len = 2
        while (len <= n) {
            val angle = -2.0 * PI / len
            val wr = kotlin.math.cos(angle)
            val wi = kotlin.math.sin(angle)
            for (i in 0 until n step len) {
                var cwr = 1.0
                var cwi = 0.0
                for (k in 0 until len / 2) {
                    val ur = re[i + k]
                    val ui = im[i + k]
                    val vr = re[i + k + len / 2] * cwr - im[i + k + len / 2] * cwi
                    val vi = re[i + k + len / 2] * cwi + im[i + k + len / 2] * cwr
                    re[i + k] = ur + vr
                    im[i + k] = ui + vi
                    re[i + k + len / 2] = ur - vr
                    im[i + k + len / 2] = ui - vi
                    val nwr = cwr * wr - cwi * wi
                    cwi = cwr * wi + cwi * wr
                    cwr = nwr
                }
            }
            len = len shl 1
        }
    }

    @Test
    fun testA_originalModeKeeps440() {
        val inputFrames = sampleRate * 2
        val out = runSonic(doubleArrayOf(440.0, 440.0), inputFrames, pitch = 1f)
        assertThat(estimateFrequencyHz(out[0], sampleRate)).isWithin(3.0).of(440.0)
        // Passthrough: no samples lost or invented beyond a frame or two.
        assertThat(out[0].size).isWithin(4).of(inputFrames)
    }

    @Test
    fun testB_tuningModeProduces432() {
        val inputFrames = sampleRate * 2
        val out = runSonic(doubleArrayOf(440.0, 440.0), inputFrames, pitch = AudioTuning.PITCH_FACTOR)
        assertThat(estimateFrequencyHz(out[0], sampleRate)).isWithin(3.0).of(432.0)
        assertThat(estimateFrequencyHz(out[1], sampleRate)).isWithin(3.0).of(432.0)
    }

    @Test
    fun testC_durationIsPreserved() {
        val inputFrames = sampleRate * 2
        val out = runSonic(doubleArrayOf(440.0, 440.0), inputFrames, pitch = AudioTuning.PITCH_FACTOR)
        val drift = abs(out[0].size - inputFrames).toDouble() / inputFrames
        assertThat(drift).isLessThan(0.02)
    }

    @Test
    fun testD_stereoChannelsStayIndependentAndClean() {
        val inputFrames = sampleRate * 2
        val out = runSonic(doubleArrayOf(440.0, 660.0), inputFrames, pitch = AudioTuning.PITCH_FACTOR)
        // Each channel carries its own tone, retuned by the same factor —
        // any channel mixing would drag both estimates to the same value.
        assertThat(estimateFrequencyHz(out[0], sampleRate)).isWithin(3.0).of(432.0)
        assertThat(estimateFrequencyHz(out[1], sampleRate)).isWithin(4.0).of(648.0)
        // No clipping: the 0.5-amplitude input must stay within full scale.
        assertThat(out[0].maxOf { abs(it) }).isLessThan(0.99)
        assertThat(out[1].maxOf { abs(it) }).isLessThan(0.99)
    }
}

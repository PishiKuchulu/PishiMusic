package com.tuned.app.playback.analysis

import androidx.media3.common.C
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A rolling loudness history for one player, for AutoMix: one reading per
 * [WINDOW_MS] over the last [HISTORY_MS]. Readings are in dBFS; silence is
 * kept so a fade-out shows as falling values.
 */
class LoudnessMonitor {
    /** Music Haptics: when upcoming beats will be heard, as System.nanoTime() values. */
    val beats = java.util.concurrent.ConcurrentLinkedQueue<Long>()
    /** How much bass to take out while blending into another song, 0 to 1. */
    @Volatile var bassCut: Float = 0f
    private val lock = Any()
    private val onsets = FloatArray(ONSET_HISTORY_MS / ONSET_HOP_MS)
    private var onsetCount = 0
    private var onsetNext = 0
    private var lastOnsetMs = 0.0
    private val readings = FloatArray((HISTORY_MS / WINDOW_MS).toInt())
    private var count = 0
    private var next = 0

    fun add(db: Float) = synchronized(lock) {
        readings[next] = db
        next = (next + 1) % readings.size
        if (count < readings.size) count++
    }

    fun clear() = synchronized(lock) {
        count = 0
        next = 0
    }

    /** Records one envelope value, at [mediaMs] on the song's clock. A jump starts over: it's a seek or a new song. */
    fun addOnset(value: Float, mediaMs: Double) = synchronized(lock) {
        if (onsetCount > 0 && kotlin.math.abs(mediaMs - lastOnsetMs - ONSET_HOP_MS) > ONSET_GAP_MS) onsetCount = 0
        onsets[onsetNext] = value
        onsetNext = (onsetNext + 1) % onsets.size
        if (onsetCount < onsets.size) onsetCount++
        lastOnsetMs = mediaMs
    }

    fun clearOnsets() = synchronized(lock) { onsetCount = 0 }

    /** The beat of what has just been playing, or null with too little to go on. */
    fun tempo(): TempoEstimate? {
        val (recent, startMs) = synchronized(lock) {
            val copy = FloatArray(onsetCount) { onsets[(onsetNext - onsetCount + it + onsets.size) % onsets.size] }
            copy to lastOnsetMs - (onsetCount - 1) * ONSET_HOP_MS
        }
        return estimateTempo(recent, startMs)
    }

    /** Average loudness of the last [ms], or null with less than that recorded. */
    fun recentDb(ms: Long = 1_000L): Float? = synchronized(lock) {
        val n = (ms / WINDOW_MS).toInt().coerceAtLeast(1)
        if (count < n) return null
        energyMean((0 until n).map { readings[(next - 1 - it + readings.size) % readings.size] })
    }

    /**
     * The song's usual level over the whole history, ignoring near-silence, or
     * null before [MIN_TYPICAL_MS] of sound. This is what an outro is measured against.
     */
    fun typicalDb(): Float? = synchronized(lock) {
        val sound = (0 until count).map { readings[it] }.filter { it > SILENCE_DB }
        if (sound.size * WINDOW_MS < MIN_TYPICAL_MS) return null
        energyMean(sound)
    }

    private fun energyMean(dbs: List<Float>): Float {
        val power = dbs.sumOf { Math.pow(10.0, it / 10.0) } / dbs.size
        return (10 * log10(power.coerceAtLeast(1e-12))).toFloat()
    }

    companion object {
        const val WINDOW_MS = 100L
        const val HISTORY_MS = 30_000L
        const val MIN_TYPICAL_MS = 8_000L
        const val SILENCE_DB = -60f
        const val ONSET_HISTORY_MS = 30_000
        const val ONSET_GAP_MS = 100.0
    }
}

/** How much of the vocals to take out, 0 (none) to 1 (as much as possible); shared by both players. */
class VocalControl {
    @Volatile var level: Float = 0f
    /** Music Haptics is on, so the stage should look for beats. */
    @Volatile var beatsEnabled: Boolean = false
}

/**
 * Finds beats for Music Haptics: a bass-band energy jump well above the recent average.
 * Feed it samples; [feed] returns true on the sample where a beat starts. Beats are at
 * least [MIN_GAP_MS] apart, faster than anyone can feel separately.
 */
class BeatDetector(samplesPerSecond: Int) {
    private val lowPass = 1.0 - exp(-2.0 * PI * BASS_HZ / samplesPerSecond.coerceAtLeast(1))
    private val hopSamples = (samplesPerSecond / 100).coerceAtLeast(1) // 10 ms
    private val minGapHops = MIN_GAP_MS / 10
    private var low1 = 0.0
    private var low2 = 0.0
    private var energy = 0.0
    private var count = 0
    private var average = 0.0
    private var hopsSinceBeat = minGapHops

    fun feed(sample: Double): Boolean {
        low1 += lowPass * (sample - low1)
        low2 += lowPass * (low1 - low2)
        energy += low2 * low2
        if (++count < hopSamples) return false
        val hop = energy / count
        energy = 0.0
        count = 0
        hopsSinceBeat++
        val isBeat = hop > MIN_ENERGY && hop > average * JUMP && hopsSinceBeat >= minGapHops
        average += AVERAGE_RATE * (hop - average)
        if (isBeat) hopsSinceBeat = 0
        return isBeat
    }

    private companion object {
        const val BASS_HZ = 150.0
        const val MIN_GAP_MS = 180
        const val JUMP = 1.6
        const val MIN_ENERGY = 1e-4
        const val AVERAGE_RATE = 0.01 // about a second at one hop per 10 ms
    }
}

/**
 * Vocal reduction for one stereo stream, decided per frequency over short frames so the
 * rest of the song survives. A frequency is only cut when it is:
 *  - in the centre: left and right match. Stereo instruments and reverb are left alone.
 *  - in the vocal range: bass below it and cymbals above it stay.
 *  - sustained or harmonic: drum hits are short and spread across frequencies, so they stay.
 * Only the centre part of a frequency is removed. It can't tell a voice from a centred,
 * harmonic instrument such as a keyboard, so those dip too.
 *
 * The frames add a delay of about 90 ms while it is on, so it blends in from the dry
 * signal once the first frame is ready, and back out when the level returns to 0.
 * The cut stops short of full removal: going deeper leaves a hollow ghost of the voice.
 */
class VocalReducer(sampleRateHz: Int) {
    private val rate = sampleRateHz.coerceAtLeast(1)
    private val size = Integer.highestOneBit((rate * FRAME_SECONDS).toInt().coerceAtLeast(512) - 1) * 2
    private val hop = size / 4
    private val bins = size / 2 + 1
    private val window = DoubleArray(size) { 0.5 - 0.5 * cos(2 * PI * it / size) }
    private val overlapGain = 1.0 / 1.5 // what a Hann window squared sums to at a quarter hop
    private val cosTable = DoubleArray(size / 2) { cos(2 * PI * it / size) }
    private val sinTable = DoubleArray(size / 2) { sin(2 * PI * it / size) }
    private val bitReverse = IntArray(size).also { table ->
        val bits = Integer.numberOfTrailingZeros(size)
        for (i in 0 until size) table[i] = Integer.reverse(i) ushr (32 - bits)
    }

    private val historyL = DoubleArray(size)
    private val historyR = DoubleArray(size)
    private val outL = DoubleArray(size)
    private val outR = DoubleArray(size)
    private val reL = DoubleArray(size)
    private val imL = DoubleArray(size)
    private val reR = DoubleArray(size)
    private val imR = DoubleArray(size)
    private val centreHistory = Array(TIME_FRAMES) { DoubleArray(bins) }
    private val centreNow = DoubleArray(bins)
    private val previousCut = DoubleArray(bins)
    private val cutNow = DoubleArray(bins)
    private val sortBuffer = DoubleArray(maxOf(TIME_FRAMES, 2 * FREQ_RADIUS + 1))
    private val binWeight = DoubleArray(bins) { vocalRangeWeight(it * rate.toDouble() / size) }
    private val toneWeight = DoubleArray(bins) { toneAt(it * rate.toDouble() / size) }
    private val mixStep = 1.0 / (rate * BLEND_SECONDS)

    private var historyIndex = 0
    private var position = 0
    private var sinceFrame = 0
    private var warmup = size
    private var mix = 0.0
    private var level = 0.0
    private var target = 0.0

    var left = 0.0
        private set
    var right = 0.0
        private set

    /** Processes one frame; read the result from [left] and [right]. */
    fun process(l: Double, r: Double, target: Double) {
        this.target = target
        if (isIdle && target <= 0.0) {
            left = l
            right = r
            return
        }
        val wetL = outL[position]
        val wetR = outR[position]
        outL[position] = 0.0
        outR[position] = 0.0
        historyL[position] = l
        historyR[position] = r
        position = (position + 1) % size
        if (++sinceFrame == hop) {
            sinceFrame = 0
            runFrame()
        }
        if (warmup > 0) {
            warmup--
        } else {
            mix = (mix + if (target > 0.0) mixStep else -mixStep).coerceIn(0.0, 1.0)
            // Back at dry: the next use starts clean instead of playing stale audio.
            if (mix == 0.0 && target <= 0.0) reset()
        }
        left = l + mix * (wetL - l)
        right = r + mix * (wetR - r)
    }

    val isIdle: Boolean get() = mix == 0.0 && warmup == size

    /** Drops all held audio, for a seek or a stop. */
    fun reset() {
        historyL.fill(0.0)
        historyR.fill(0.0)
        outL.fill(0.0)
        outR.fill(0.0)
        centreHistory.forEach { it.fill(0.0) }
        previousCut.fill(0.0)
        position = 0
        sinceFrame = 0
        historyIndex = 0
        warmup = size
        mix = 0.0
        level = 0.0
    }

    private fun runFrame() {
        for (i in 0 until size) {
            val at = (position + i) % size
            reL[i] = historyL[at] * window[i]
            imL[i] = 0.0
            reR[i] = historyR[at] * window[i]
            imR[i] = 0.0
        }
        fft(reL, imL, inverse = false)
        fft(reR, imR, inverse = false)

        level += (target - level).coerceIn(-LEVEL_STEP, LEVEL_STEP)
        val past = centreHistory[historyIndex]
        for (k in 0 until bins) {
            val cRe = (reL[k] + reR[k]) / 2
            val cIm = (imL[k] + imR[k]) / 2
            centreNow[k] = sqrt(cRe * cRe + cIm * cIm)
            past[k] = centreNow[k]
        }
        historyIndex = (historyIndex + 1) % TIME_FRAMES

        for (k in 0 until bins) {
            val weight = binWeight[k]
            var cut = 0.0
            if (weight > 0.0 && level > 0.0) {
                val power = reL[k] * reL[k] + imL[k] * imL[k] + reR[k] * reR[k] + imR[k] * imR[k]
                val match = if (power > 1e-18) 2 * (reL[k] * reR[k] + imL[k] * imR[k]) / power else 0.0
                val centred = smoothStep(CENTRE_FROM, CENTRE_TO, match)
                if (centred > 0.0) {
                    val sustained = medianOverTime(k)
                    val spread = medianOverFrequency(k)
                    val now = centreNow[k]
                    val total = sustained * sustained + spread * spread
                    val held = if (total > 1e-18) sustained * sustained / total else 0.0
                    // Fast syllables aren't held, but a voiced one still stands out as
                    // harmonic peaks. Noise bursts like drums and consonants don't.
                    val peakTotal = now * now + spread * spread
                    val peak = if (peakTotal > 1e-18) smoothStep(PEAK_FROM, PEAK_TO, now * now / peakTotal) else 0.0
                    cut = MAX_CUT * level * weight * centred * maxOf(held, peak)
                }
            }
            cutNow[k] = cut
        }
        // Smoothed across time and frequency: single bins switching on and off are
        // what makes the watery, chirping sound.
        for (k in 0 until bins) {
            val around = (cutNow[maxOf(k - 1, 0)] + cutNow[k] + cutNow[minOf(k + 1, bins - 1)]) / 3
            val cut = CUT_HOLD * previousCut[k] + (1 - CUT_HOLD) * around
            previousCut[k] = cut
            val cRe = (reL[k] + reR[k]) / 2 * cut
            val cIm = (imL[k] + imR[k]) / 2 * cut
            reL[k] -= cRe
            imL[k] -= cIm
            reR[k] -= cRe
            imR[k] -= cIm
            // Taking the voice out tilts the mix towards the low-mids; lean it back.
            val tone = 1.0 + level * (toneWeight[k] - 1.0)
            reL[k] *= tone
            imL[k] *= tone
            reR[k] *= tone
            imR[k] *= tone
            if (k in 1 until size / 2) {
                val m = size - k
                reL[m] = reL[k]
                imL[m] = -imL[k]
                reR[m] = reR[k]
                imR[m] = -imR[k]
            }
        }

        fft(reL, imL, inverse = true)
        fft(reR, imR, inverse = true)
        for (i in 0 until size) {
            val at = (position + i) % size
            outL[at] += reL[i] * window[i] * overlapGain
            outR[at] += reR[i] * window[i] * overlapGain
        }
    }

    private fun medianOverTime(k: Int): Double {
        for (t in 0 until TIME_FRAMES) sortBuffer[t] = centreHistory[t][k]
        return median(TIME_FRAMES)
    }

    private fun medianOverFrequency(k: Int): Double {
        var n = 0
        for (j in maxOf(0, k - FREQ_RADIUS)..minOf(bins - 1, k + FREQ_RADIUS)) sortBuffer[n++] = centreNow[j]
        return median(n)
    }

    private fun median(n: Int): Double {
        for (i in 1 until n) {
            val value = sortBuffer[i]
            var j = i - 1
            while (j >= 0 && sortBuffer[j] > value) {
                sortBuffer[j + 1] = sortBuffer[j]
                j--
            }
            sortBuffer[j + 1] = value
        }
        return sortBuffer[n / 2]
    }

    private fun fft(re: DoubleArray, im: DoubleArray, inverse: Boolean) {
        for (i in 0 until size) {
            val j = bitReverse[i]
            if (j > i) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        var span = 2
        while (span <= size) {
            val half = span / 2
            val step = size / span
            var start = 0
            while (start < size) {
                for (i in 0 until half) {
                    val c = cosTable[i * step]
                    val s = if (inverse) sinTable[i * step] else -sinTable[i * step]
                    val a = start + i
                    val b = a + half
                    val tr = re[b] * c - im[b] * s
                    val ti = re[b] * s + im[b] * c
                    re[b] = re[a] - tr
                    im[b] = im[a] - ti
                    re[a] += tr
                    im[a] += ti
                }
                start += span
            }
            span *= 2
        }
        if (inverse) {
            for (i in 0 until size) {
                re[i] /= size
                im[i] /= size
            }
        }
    }

    private fun smoothStep(from: Double, to: Double, value: Double): Double {
        val x = ((value - from) / (to - from)).coerceIn(0.0, 1.0)
        return x * x * (3 - 2 * x)
    }

    /** Full-level tone: a dip through the muddy low-mids and a lift for presence. */
    private fun toneAt(hz: Double): Double = when {
        hz <= TONE_DIP_START_HZ -> 1.0
        hz < TONE_DIP_CENTRE_HZ -> 1.0 + (TONE_DIP - 1.0) * (hz - TONE_DIP_START_HZ) / (TONE_DIP_CENTRE_HZ - TONE_DIP_START_HZ)
        hz < TONE_DIP_END_HZ -> TONE_DIP + (1.0 - TONE_DIP) * (hz - TONE_DIP_CENTRE_HZ) / (TONE_DIP_END_HZ - TONE_DIP_CENTRE_HZ)
        hz <= TONE_LIFT_START_HZ -> 1.0
        hz < TONE_LIFT_FULL_HZ -> 1.0 + (TONE_LIFT - 1.0) * (hz - TONE_LIFT_START_HZ) / (TONE_LIFT_FULL_HZ - TONE_LIFT_START_HZ)
        else -> TONE_LIFT
    }

    private fun vocalRangeWeight(hz: Double): Double = when {
        hz <= LOW_START_HZ -> 0.0
        hz < LOW_FULL_HZ -> (hz - LOW_START_HZ) / (LOW_FULL_HZ - LOW_START_HZ)
        hz <= HIGH_FULL_HZ -> 1.0
        hz < HIGH_END_HZ -> (HIGH_END_HZ - hz) / (HIGH_END_HZ - HIGH_FULL_HZ)
        else -> 0.0
    }

    private companion object {
        // Rounded up to a power of two: 4096 samples at 44.1 and 48 kHz. Shorter frames can't
        // separate a voice's harmonics, which is heard as phasing.
        const val FRAME_SECONDS = 0.085
        const val BLEND_SECONDS = 0.02
        const val LEVEL_STEP = 0.25
        const val MAX_CUT = 0.95
        const val CUT_HOLD = 0.5
        const val TIME_FRAMES = 5
        const val FREQ_RADIUS = 12
        const val PEAK_FROM = 0.4
        const val PEAK_TO = 0.7
        const val CENTRE_FROM = 0.45
        const val CENTRE_TO = 0.85
        const val LOW_START_HZ = 90.0
        const val LOW_FULL_HZ = 180.0
        const val HIGH_FULL_HZ = 4_500.0
        const val HIGH_END_HZ = 9_000.0
        const val TONE_DIP = 0.79 // -2 dB
        const val TONE_DIP_START_HZ = 120.0
        const val TONE_DIP_CENTRE_HZ = 250.0
        const val TONE_DIP_END_HZ = 500.0
        const val TONE_LIFT = 1.41 // +3 dB
        const val TONE_LIFT_START_HZ = 3_000.0
        const val TONE_LIFT_FULL_HZ = 7_000.0
    }
}

/** Takes the bass out of one stereo stream by [process]'s target amount, ramping so it never clicks. */
class BassCut(sampleRateHz: Int) {
    private val lowPass = 1.0 - exp(-2.0 * PI * CUTOFF_HZ / sampleRateHz.coerceAtLeast(1))
    private val ramp = 1.0 / (sampleRateHz.coerceAtLeast(1) * RAMP_SECONDS)
    private var low1L = 0.0
    private var low2L = 0.0
    private var low1R = 0.0
    private var low2R = 0.0
    private var current = 0.0

    var left = 0.0
        private set
    var right = 0.0
        private set

    fun process(l: Double, r: Double, target: Double) {
        current += (target - current).coerceIn(-ramp, ramp)
        low1L += lowPass * (l - low1L)
        low2L += lowPass * (low1L - low2L)
        low1R += lowPass * (r - low1R)
        low2R += lowPass * (low1R - low2R)
        left = l - current * low2L
        right = r - current * low2R
    }

    val isIdle: Boolean get() = current == 0.0

    private companion object {
        const val CUTOFF_HZ = 160.0
        const val RAMP_SECONDS = 0.2
    }
}

/**
 * What every decoded buffer passes through on its way into the sink: measures loudness
 * for [loudness] and applies [vocals], in place. It runs at the sink's input because the
 * native float path skips Media3's own AudioProcessor chain. Formats it can't read pass
 * untouched, and a read-only buffer is measured but not changed.
 */
class PcmInputStage(private val loudness: LoudnessMonitor, private val vocals: VocalControl) {
    private var encoding = C.ENCODING_INVALID
    private var channels = 0
    private var samplesPerWindow = 0
    private var reducer: VocalReducer? = null
    private var bassCut: BassCut? = null
    private var onsetEnvelope: OnsetEnvelope? = null
    private var beatDetector: BeatDetector? = null
    private var samplesPerSecond = 0
    private var sumSquares = 0.0
    private var samples = 0

    fun configure(sampleRateHz: Int, channelCount: Int, pcmEncoding: Int) {
        encoding = pcmEncoding
        channels = channelCount
        samplesPerWindow = (sampleRateHz * channelCount * LoudnessMonitor.WINDOW_MS / 1000).toInt()
        val stereo = channelCount == 2 && sampleRateHz > 0
        reducer = if (stereo) VocalReducer(sampleRateHz) else null
        bassCut = if (stereo) BassCut(sampleRateHz) else null
        samplesPerSecond = sampleRateHz * channelCount
        beatDetector = if (samplesPerSecond > 0) BeatDetector(samplesPerSecond) else null
        onsetEnvelope = if (samplesPerSecond > 0) OnsetEnvelope(samplesPerSecond) else null
        reset()
    }

    fun reset() {
        sumSquares = 0.0
        samples = 0
        reducer?.reset()
        loudness.beats.clear()
        loudness.clearOnsets()
    }

    /**
     * Runs one decoded buffer. [presentationTimeUs] and [sinkPositionUs] (what is being
     * heard now, on the same clock; negative when unknown) time the beats it finds:
     * this audio plays later than it is decoded.
     */
    fun process(buffer: ByteBuffer, presentationTimeUs: Long = 0L, sinkPositionUs: Long = -1L) {
        val bytes = when (encoding) {
            C.ENCODING_PCM_16BIT -> 2
            C.ENCODING_PCM_24BIT -> 3
            C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 4
            else -> return
        }
        if (channels <= 0 || samplesPerWindow <= 0) return
        val view = buffer.duplicate().order(ByteOrder.nativeOrder())
        val target = vocals.level.toDouble().coerceIn(0.0, 1.0)
        val stereo = reducer?.takeIf { !buffer.isReadOnly && (target > 0.0 || !it.isIdle) }
        val cutTarget = loudness.bassCut.toDouble().coerceIn(0.0, 1.0)
        val cut = bassCut?.takeIf { !buffer.isReadOnly && (cutTarget > 0.0 || !it.isIdle) }
        var at = view.position()
        val end = view.limit()
        val detector = beatDetector?.takeIf { vocals.beatsEnabled }
        var fed = 0L
        fun tap(value: Double) {
            val onset = onsetEnvelope?.feed(value)
            if (onset != null && !onset.isNaN()) {
                loudness.addOnset(onset, (presentationTimeUs + fed * 1_000_000L / samplesPerSecond) / 1000.0)
            }
            if (detector != null && detector.feed(value)) {
                val beatUs = presentationTimeUs + fed * 1_000_000L / samplesPerSecond
                val leadUs = if (sinkPositionUs >= 0L) (beatUs - sinkPositionUs).coerceIn(0L, MAX_BEAT_LEAD_US) else DEFAULT_BEAT_LEAD_US
                loudness.beats.add(System.nanoTime() + leadUs * 1_000L)
            }
            fed++
        }
        if (stereo != null || cut != null) {
            while (at + 2 * bytes <= end) {
                var l = read(view, at)
                var r = read(view, at + bytes)
                if (stereo != null) {
                    stereo.process(l, r, target)
                    l = stereo.left
                    r = stereo.right
                }
                if (cut != null) {
                    cut.process(l, r, cutTarget)
                    l = cut.left
                    r = cut.right
                }
                write(view, at, l)
                write(view, at + bytes, r)
                measure(l)
                measure(r)
                tap(l)
                tap(r)
                at += 2 * bytes
            }
        } else {
            while (at + bytes <= end) {
                val value = read(view, at)
                measure(value)
                tap(value)
                at += bytes
            }
        }
    }

    private companion object {
        const val MAX_BEAT_LEAD_US = 2_000_000L
        const val DEFAULT_BEAT_LEAD_US = 200_000L
    }

    private fun read(view: ByteBuffer, at: Int): Double = when (encoding) {
        C.ENCODING_PCM_16BIT -> view.getShort(at) / 32768.0
        C.ENCODING_PCM_FLOAT -> view.getFloat(at).toDouble()
        C.ENCODING_PCM_32BIT -> view.getInt(at) / 2147483648.0
        else -> {
            val little = view.order() == ByteOrder.LITTLE_ENDIAN
            val b0 = view.get(at).toInt() and 0xFF
            val b1 = view.get(at + 1).toInt() and 0xFF
            val b2 = view.get(at + 2).toInt() and 0xFF
            val raw = if (little) b0 or (b1 shl 8) or (b2 shl 16) else b2 or (b1 shl 8) or (b0 shl 16)
            ((raw shl 8) shr 8) / 8388608.0
        }
    }

    private fun write(view: ByteBuffer, at: Int, value: Double) {
        val v = value.coerceIn(-1.0, 1.0)
        when (encoding) {
            C.ENCODING_PCM_16BIT -> view.putShort(at, (v * 32767.0).toInt().toShort())
            C.ENCODING_PCM_FLOAT -> view.putFloat(at, value.toFloat())
            C.ENCODING_PCM_32BIT -> view.putInt(at, (v * 2147483647.0).toInt())
            else -> {
                val raw = (v * 8388607.0).toInt()
                val little = view.order() == ByteOrder.LITTLE_ENDIAN
                view.put(at, (if (little) raw else raw shr 16).toByte())
                view.put(at + 1, (raw shr 8).toByte())
                view.put(at + 2, (if (little) raw shr 16 else raw).toByte())
            }
        }
    }

    private fun measure(value: Double) {
        sumSquares += value * value
        if (++samples >= samplesPerWindow) {
            val rms = sqrt(sumSquares / samples)
            loudness.add((20 * log10(rms.coerceAtLeast(1e-6))).toFloat())
            sumSquares = 0.0
            samples = 0
        }
    }
}

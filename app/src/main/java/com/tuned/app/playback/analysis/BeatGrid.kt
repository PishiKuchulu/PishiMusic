package com.tuned.app.playback.analysis

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.PI

/** One envelope value per this many milliseconds of audio. */
const val ONSET_HOP_MS = 10

/** How regular a song's beat must look before AutoMix will match to it, 0 to 1. */
const val BEAT_MIN_CONFIDENCE = 0.10f

/** The furthest AutoMix will bend a song's speed; beyond this it can be heard. */
const val BEAT_MAX_STRETCH = 0.10

private const val MIN_ENVELOPE_MS = 8_000
private const val MIN_BPM = 60.0
private const val MAX_BPM = 180.0
private const val PREFERRED_BPM = 120.0
private const val SMOOTH_HOPS = 2

/** Beat correction: this much drift asks for the full speed trim. */
private const val BEAT_LOCK_WINDOW_MS = 1_500.0
private const val BEAT_LOCK_MAX_TRIM = 0.02

/** A song's beat: its tempo, and where one beat falls on the song's own clock. */
data class TempoEstimate(
    val bpm: Double,
    val periodMs: Double,
    /** Media time of a beat. */
    val phaseMs: Double,
    /** How regular the beat is, 0 to 1. */
    val confidence: Float,
)

/** How one song should be started against another so their beats line up. */
data class BeatMatch(
    /** Speed for the incoming song, with its pitch left alone. */
    val speed: Float,
    /** Where in the outgoing song to start the incoming one. */
    val startAtMs: Long,
    /** The blend length, in whole bars. */
    val overlapMs: Long,
)

/**
 * Reduces audio to how sharply its energy rises every [ONSET_HOP_MS]: peaks where
 * something hits. Bass carries most of the weight, because that's where the kick is;
 * a little of the full signal covers songs without one. [feed] takes interleaved samples.
 */
class OnsetEnvelope(samplesPerSecond: Int) {
    private val lowPass = 1.0 - exp(-2.0 * PI * BASS_HZ / samplesPerSecond.coerceAtLeast(1))
    private val hopSamples = (samplesPerSecond * ONSET_HOP_MS / 1000).coerceAtLeast(1)
    private var low1 = 0.0
    private var low2 = 0.0
    private var energy = 0.0
    private var count = 0
    private var previous = Double.NaN

    /** The next envelope value when this sample completes a hop, otherwise NaN. */
    fun feed(sample: Double): Float {
        low1 += lowPass * (sample - low1)
        low2 += lowPass * (low1 - low2)
        energy += low2 * low2 + FULL_BAND_WEIGHT * sample * sample
        if (++count < hopSamples) return Float.NaN
        val level = ln(energy / count + ENERGY_FLOOR)
        energy = 0.0
        count = 0
        val rise = if (previous.isNaN()) 0.0 else (level - previous).coerceAtLeast(0.0)
        previous = level
        return rise.toFloat()
    }

    private companion object {
        const val BASS_HZ = 150.0
        const val FULL_BAND_WEIGHT = 0.25
        const val ENERGY_FLOOR = 1e-7
    }
}

/**
 * Finds the tempo and beat positions in an onset envelope, or null when there isn't
 * enough of it. [startMs] is the media time of the envelope's first value.
 */
fun estimateTempo(envelope: FloatArray, startMs: Double = 0.0): TempoEstimate? {
    if (envelope.size * ONSET_HOP_MS < MIN_ENVELOPE_MS) return null
    // Smoothed over about 50 ms: a single hop of bass carries its own wave as ripple,
    // and that buries the beat when correlating.
    val smooth = DoubleArray(envelope.size) { i ->
        var sum = 0.0
        var weight = 0.0
        for (j in -SMOOTH_HOPS..SMOOTH_HOPS) {
            val value = envelope.getOrNull(i + j) ?: continue
            val w = (SMOOTH_HOPS + 1 - kotlin.math.abs(j)).toDouble()
            sum += value * w
            weight += w
        }
        sum / weight
    }
    val mean = smooth.sum() / smooth.size
    val signal = DoubleArray(smooth.size) { smooth[it] - mean }
    val power = signal.sumOf { it * it }
    if (power <= 1e-9) return null

    fun correlation(lag: Int): Double {
        var sum = 0.0
        for (i in 0 until signal.size - lag) sum += signal[i] * signal[i + lag]
        return sum / power
    }

    val minLag = (60_000.0 / MAX_BPM / ONSET_HOP_MS).toInt()
    val maxLag = (60_000.0 / MIN_BPM / ONSET_HOP_MS).toInt()
    var bestLag = 0
    var bestScore = Double.NEGATIVE_INFINITY
    var bestRaw = 0.0
    for (lag in minLag..maxLag) {
        val raw = correlation(lag)
        // A real beat also repeats at two and four beats out. A false tempo at 3:2 of the
        // real one doesn't, which is what tells them apart.
        val support = raw + 0.5 * correlation(lag * 2) + 0.25 * correlation(lag * 4)
        // Half and double tempo correlate too; lean towards the range people count in.
        val octaves = ln((60_000.0 / (lag * ONSET_HOP_MS)) / PREFERRED_BPM) / ln(2.0)
        val score = support * exp(-0.5 * octaves * octaves)
        if (score > bestScore) {
            bestScore = score
            bestLag = lag
            bestRaw = raw
        }
    }
    if (bestLag == 0 || bestRaw <= 0.0) return null

    // A peak four beats out pins the period down four times finer, which is what keeps
    // two songs together across a whole blend.
    var period = bestLag.toDouble()
    val far = bestLag * 4
    if (far + 8 < signal.size / 2) {
        var farBest = far
        var farScore = Double.NEGATIVE_INFINITY
        for (lag in far - 8..far + 8) {
            val raw = correlation(lag)
            if (raw > farScore) {
                farScore = raw
                farBest = lag
            }
        }
        period = farBest / 4.0
    }

    var bestOffset = 0
    var bestSum = Double.NEGATIVE_INFINITY
    for (offset in 0 until period.toInt().coerceAtLeast(1)) {
        var sum = 0.0
        var at = offset.toDouble()
        while (at < signal.size) {
            sum += signal[at.toInt()]
            at += period
        }
        if (sum > bestSum) {
            bestSum = sum
            bestOffset = offset
        }
    }
    val periodMs = period * ONSET_HOP_MS
    return TempoEstimate(
        bpm = 60_000.0 / periodMs,
        periodMs = periodMs,
        phaseMs = startMs + bestOffset * ONSET_HOP_MS,
        confidence = bestRaw.toFloat().coerceIn(0f, 1f),
    )
}

/**
 * How to bring [incoming] in over [outgoing], or null when their tempos are too far apart
 * or either beat is too vague: then a plain blend sounds better than a forced one. Half
 * and double time count as the same tempo. The incoming song starts from its beginning,
 * on the first outgoing position at or after [positionMs] that lines the two grids up.
 */
fun planBeatMatch(
    outgoing: TempoEstimate?,
    incoming: TempoEstimate?,
    positionMs: Long,
    plannedOverlapMs: Long,
): BeatMatch? {
    if (outgoing == null || incoming == null) return null
    if (outgoing.confidence < BEAT_MIN_CONFIDENCE || incoming.confidence < BEAT_MIN_CONFIDENCE) return null
    val speed = listOf(0.5, 1.0, 2.0)
        .map { outgoing.bpm / (incoming.bpm * it) }
        .minByOrNull { abs(it - 1.0) }
        ?: return null
    if (abs(speed - 1.0) > BEAT_MAX_STRETCH) return null

    val beat = outgoing.periodMs
    val wanted = outgoing.phaseMs - incoming.phaseMs / speed
    val fromNow = (wanted - positionMs).mod(beat)
    return BeatMatch(
        speed = speed.toFloat(),
        startAtMs = positionMs + fromNow.toLong(),
        overlapMs = wholeBarsMs(plannedOverlapMs, beat),
    )
}

/** [plannedMs] rounded down to whole four-beat bars, at least one. */
fun wholeBarsMs(plannedMs: Long, beatMs: Double): Long {
    val bar = beatMs * 4
    val bars = (plannedMs / bar).toInt().coerceAtLeast(1)
    return (bars * bar).toLong()
}

/** How far the incoming song's beat lags the outgoing one's, in ms; negative when it leads. */
fun beatDriftMs(
    outgoingPositionMs: Long,
    incomingPositionMs: Long,
    outgoing: TempoEstimate,
    incoming: TempoEstimate,
    speed: Float,
): Double {
    val beat = outgoing.periodMs
    val heard = (outgoingPositionMs - outgoing.phaseMs).mod(beat)
    val arriving = ((incomingPositionMs - incoming.phaseMs) / speed).mod(beat)
    val drift = (heard - arriving).mod(beat)
    return if (drift > beat / 2) drift - beat else drift
}

/** The speed that closes [driftMs] without being heard. */
fun beatLockSpeed(baseSpeed: Float, driftMs: Double): Float {
    val trim = (driftMs / BEAT_LOCK_WINDOW_MS).coerceIn(-BEAT_LOCK_MAX_TRIM, BEAT_LOCK_MAX_TRIM)
    return (baseSpeed * (1.0 + trim)).toFloat()
}

/**
 * True when one side of a blend has no beat to speak of, such as an atmospheric intro or a
 * long ring-out. Nothing can clash then, so the two can overlap at full level even though
 * there's no beat to match. Unknown tempos don't count: those might still clash.
 */
fun canLayerWithoutMatching(outgoing: TempoEstimate?, incoming: TempoEstimate?): Boolean {
    if (outgoing == null || incoming == null) return false
    return outgoing.confidence < BEAT_MIN_CONFIDENCE || incoming.confidence < BEAT_MIN_CONFIDENCE
}

/**
 * Levels for a blend that can't clash, as (outgoing, incoming): it can play at full level
 * like a DJ mix. The new song comes up first, and the old one only leaves once the new one
 * is there. A plain fade dips in the middle instead, which is right for songs that would
 * clash but sounds like two separate fades when they wouldn't.
 */
fun beatMatchedGains(progress: Float): Pair<Float, Float> {
    fun rise(from: Float, to: Float): Float {
        val x = ((progress - from) / (to - from)).coerceIn(0f, 1f)
        return x * x * (3f - 2f * x)
    }
    return (1f - rise(0.55f, 1f)) to rise(0f, 0.45f)
}

/**
 * How much bass to take out of one side of a blend, 0 to 1. Two bass lines at once turn
 * to mud, so the incoming song comes in without its bass and the two swap in the middle.
 */
fun blendBassCut(progress: Float, incoming: Boolean): Float {
    val swap = ((progress - BASS_SWAP_FROM) / (BASS_SWAP_TO - BASS_SWAP_FROM)).coerceIn(0f, 1f)
    return if (incoming) 1f - swap else swap
}

private const val BASS_SWAP_FROM = 0.4f
private const val BASS_SWAP_TO = 0.6f

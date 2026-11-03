package com.tuned.app.playback

import com.google.common.truth.Truth.assertThat
import com.tuned.app.playback.analysis.LoudnessMonitor
import org.junit.Test

class AutoMixTest {

    private fun input(
        durationMs: Long = 240_000L,
        positionMs: Long = 100_000L,
        recentDb: Float? = -14f,
        typicalDb: Float? = -14f,
        nextContinuesAlbum: Boolean = false,
    ) = AutoMixInput(durationMs, positionMs, recentDb, typicalDb, nextContinuesAlbum)

    @Test
    fun albumTracksPlayStraightOn() {
        assertThat(autoMixFadeMs(input(nextContinuesAlbum = true))).isEqualTo(0L)
    }

    @Test
    fun aSteadySongGetsTheStandardBlend() {
        assertThat(autoMixFadeMs(input())).isEqualTo(AUTOMIX_FADE_MS)
    }

    @Test
    fun shortSongsGetAShorterBlend() {
        assertThat(autoMixFadeMs(input(durationMs = 120_000L, positionMs = 60_000L))).isEqualTo(AUTOMIX_SHORT_FADE_MS)
    }

    @Test
    fun anOutroStartsTheBlendWhereItFades() {
        // 10 s left and 12 dB under its usual level: blend over those 10 s.
        val fade = autoMixFadeMs(input(positionMs = 230_000L, recentDb = -26f, typicalDb = -14f))
        assertThat(fade).isEqualTo(10_000L)
    }

    @Test
    fun aQuietPassageEarlierInTheSongIsNotAnOutro() {
        val fade = autoMixFadeMs(input(positionMs = 120_000L, recentDb = -30f, typicalDb = -14f))
        assertThat(fade).isEqualTo(AUTOMIX_FADE_MS)
    }

    @Test
    fun aVeryLateOutroStillGetsTheMinimumBlend() {
        val fade = autoMixFadeMs(input(positionMs = 239_000L, recentDb = -30f, typicalDb = -14f))
        assertThat(fade).isEqualTo(AUTOMIX_MIN_FADE_MS)
    }

    @Test
    fun withoutLoudnessItFallsBackToTheStandardBlend() {
        assertThat(autoMixFadeMs(input(positionMs = 230_000L, recentDb = null, typicalDb = null))).isEqualTo(AUTOMIX_FADE_MS)
    }

    @Test
    fun loudnessMonitorTracksRecentAndTypicalLevels() {
        val monitor = LoudnessMonitor()
        assertThat(monitor.recentDb()).isNull()
        repeat(100) { monitor.add(-12f) } // 10 s of steady music
        assertThat(monitor.typicalDb()!!).isWithin(0.01f).of(-12f)
        repeat(10) { monitor.add(-40f) } // then a second of fade
        assertThat(monitor.recentDb()!!).isWithin(0.01f).of(-40f)
        // The fade barely moves the usual level, so it reads as a drop.
        assertThat(monitor.typicalDb()!!).isGreaterThan(-13f)
    }

    @Test
    fun silenceDoesNotCountTowardsTheUsualLevel() {
        val monitor = LoudnessMonitor()
        repeat(200) { monitor.add(-90f) }
        assertThat(monitor.typicalDb()).isNull()
    }
}

class BeatDetectorTest {
    private val rate = 44_100

    /** A 60 Hz kick every [everyMs] for [seconds]; returns how many beats were found. */
    private fun beats(everyMs: Int, seconds: Int = 4): Int {
        val detector = com.tuned.app.playback.analysis.BeatDetector(rate)
        val period = rate * everyMs / 1000
        val kick = rate / 20 // 50 ms long
        var found = 0
        for (i in 0 until rate * seconds) {
            val into = i % period
            val value = if (into < kick) 0.8 * kotlin.math.sin(2 * Math.PI * 60 * into / rate) else 0.0
            if (detector.feed(value)) found++
        }
        return found
    }

    @Test
    fun findsEachKick() {
        // 120 BPM for 4 s is 8 kicks.
        assertThat(beats(everyMs = 500)).isIn(7..8)
    }

    @Test
    fun neverFiresFasterThanTheMinimumGap() {
        // Kicks every 100 ms can't all register: at most one per 180 ms.
        assertThat(beats(everyMs = 100, seconds = 2)).isAtMost(12)
    }

    @Test
    fun silenceHasNoBeats() {
        val detector = com.tuned.app.playback.analysis.BeatDetector(rate)
        var found = 0
        repeat(rate * 2) { if (detector.feed(0.0)) found++ }
        assertThat(found).isEqualTo(0)
    }
}

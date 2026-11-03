package com.tuned.app.ui.common

import com.google.common.truth.Truth.assertThat
import java.util.Locale
import org.junit.Test

class ComponentFormattingTest {

    @Test
    fun sampleRatesReadTheWayPeopleSayThem() {
        assertThat(formatSampleRate(44_100)).isEqualTo("44.1 kHz")
        assertThat(formatSampleRate(48_000)).isEqualTo("48 kHz")
        assertThat(formatSampleRate(88_200)).isEqualTo("88.2 kHz")
        assertThat(formatSampleRate(96_000)).isEqualTo("96 kHz")
        assertThat(formatSampleRate(192_000)).isEqualTo("192 kHz")
    }

    @Test
    fun specStripShowsOneKnownPart() {
        assertThat(formatSpec("flac", 24, 96_000, null, "USB DAC")).isEqualTo("FLAC")
        assertThat(formatSpec(null, 16, 44_100, null, null)).isEqualTo("16-bit")
        assertThat(formatSpec(null, null, 44_100, null, null)).isEqualTo("44.1 kHz")
        assertThat(formatSpec(null, null, null, null, null)).isEmpty()
        assertThat(formatSpec("  ", 0, 0, 0, " ")).isEmpty()
    }

    @Test
    fun bitrateOnlyShowsForLossyAudio() {
        assertThat(formatSpec(null, null, null, 256, "Bluetooth")).isEqualTo("256 kbps")
        assertThat(formatSpec(null, 24, null, 2_300, "USB DAC")).isEqualTo("24-bit")
    }

    @Test
    fun specStripIgnoresTheDeviceLocale() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertThat(formatSampleRate(44_100)).isEqualTo("44.1 kHz")
        } finally {
            Locale.setDefault(saved)
        }
    }

    @Test
    fun statsGroupDigitsForTheReadersLocale() {
        assertThat(formatStat(19_588, Locale.US)).isEqualTo("19,588")
        assertThat(formatStat(19_588, Locale.GERMANY)).isEqualTo("19.588")
        assertThat(formatStat(7, Locale.US)).isEqualTo("7")
    }

    private val labels = TrackRowLabels(nowPlaying = "Now playing", downloaded = "Downloaded", unavailable = "Unavailable")

    @Test
    fun trackRowReadsAsOnePhrase() {
        assertThat(trackRowDescription("Odysseus", "Ludwig Göransson", PlaybackMark.None, false, true, labels))
            .isEqualTo("Odysseus, Ludwig Göransson")
    }

    @Test
    fun trackRowStatesAreSpokenInOrder() {
        assertThat(trackRowDescription("Odysseus", "Ludwig Göransson", PlaybackMark.Paused, true, false, labels))
            .isEqualTo("Odysseus, Ludwig Göransson, Now playing, Downloaded, Unavailable")
    }

    @Test
    fun blankSubtitleIsLeftOut() {
        assertThat(trackRowDescription("Intro", " ", PlaybackMark.None, false, true, labels)).isEqualTo("Intro")
    }
}

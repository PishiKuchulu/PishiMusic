package com.tuned.app.ui.recap

import com.google.common.truth.Truth.assertThat
import com.tuned.app.data.local.db.YearlyListenEntity
import org.junit.Test

class RecapTest {
    private fun row(title: String, artist: String, minutes: Long) = YearlyListenEntity(
        year = 2026,
        trackKey = "$title|$artist",
        title = title,
        artist = artist,
        listenedMs = minutes * 60_000L,
    )

    @Test
    fun addsUpTheYear() {
        val recap = buildRecap(2026, listOf(2026), listOf(row("A", "One", 10), row("B", "One", 5), row("C", "Two", 20)))
        assertThat(recap.minutes).isEqualTo(35)
        assertThat(recap.songCount).isEqualTo(3)
        assertThat(recap.artistCount).isEqualTo(2)
    }

    @Test
    fun ranksSongsAndArtistsByTimeListened() {
        val recap = buildRecap(2026, listOf(2026), listOf(row("A", "One", 10), row("B", "One", 15), row("C", "Two", 20)))
        assertThat(recap.topSongs.map { it.title }).containsExactly("C", "B", "A").inOrder()
        assertThat(recap.topArtists.map { it.name }).containsExactly("One", "Two").inOrder()
    }

    @Test
    fun keepsOnlyTheTopFive() {
        val rows = (1..8).map { row("Song $it", "Artist $it", it.toLong()) }
        val recap = buildRecap(2026, listOf(2026), rows)
        assertThat(recap.topSongs).hasSize(5)
        assertThat(recap.topArtists).hasSize(5)
        assertThat(recap.songCount).isEqualTo(8)
    }

    @Test
    fun anEmptyYearIsEmpty() {
        val recap = buildRecap(2026, emptyList(), emptyList())
        assertThat(recap.minutes).isEqualTo(0)
        assertThat(recap.topSongs).isEmpty()
        assertThat(recap.isLoading).isFalse()
    }
}

class RecapNumberSizeTest {
    @Test
    fun shortNumbersRunLargeAndLongOnesStillFit() {
        val width = 360f
        val small = recapNumberSp(width, 2)
        val large = recapNumberSp(width, 7)
        assertThat(small).isEqualTo(148f)
        assertThat(large).isLessThan(small)
        assertThat(large * 7 * 0.72f).isAtMost(width + 1f)
    }

    @Test
    fun neverSmallerThanTheFloor() {
        assertThat(recapNumberSp(100f, 12)).isEqualTo(56f)
    }
}

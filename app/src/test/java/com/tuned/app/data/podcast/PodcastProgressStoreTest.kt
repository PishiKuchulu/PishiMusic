package com.tuned.app.data.podcast

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

/**
 * Resume rules: a tap-through (< 3 s) is not a listen, a finished episode
 * (> 95 %) does not resume, and everything in between resumes where it
 * stopped. (The resume floor was lowered from 10 s to 3 s so short episodes
 * and previews resume too; the continue-listening surfacing floor stays
 * 10 s — see PodcastProgressStore.MIN_CONTINUE_POSITION_MS.)
 */
class PodcastProgressStoreTest {

    private val dao: PodcastProgressDao = mockk(relaxed = true)

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun store() = PodcastProgressStore(dao)

    @Test
    fun resumePositionMs_whenNoSavedRow_returnsZero() = runTest {
        coEvery { dao.get("video") } returns null

        assertThat(store().resumePositionMs("video", 60_000L)).isEqualTo(0L)
    }

    @Test
    fun resumePositionMs_whenSavedPositionIsUnderThreeSeconds_returnsZero() = runTest {
        coEvery { dao.get("video") } returns PodcastProgressEntity(
            videoId = "video",
            positionMs = 2_000L,
            durationMs = 60_000L,
        )

        assertThat(store().resumePositionMs("video", 60_000L)).isEqualTo(0L)
    }

    @Test
    fun resumePositionMs_whenSavedPositionIsAboveResumeFloor_returnsIt() = runTest {
        coEvery { dao.get("video") } returns PodcastProgressEntity(
            videoId = "video",
            positionMs = 9_000L,
            durationMs = 60_000L,
        )

        assertThat(store().resumePositionMs("video", 60_000L)).isEqualTo(9_000L)
    }

    @Test
    fun resumePositionMs_whenEpisodeWasCompleted_returnsZero() = runTest {
        coEvery { dao.get("video") } returns PodcastProgressEntity(
            videoId = "video",
            positionMs = 30_000L,
            durationMs = 60_000L,
            completed = true,
        )

        assertThat(store().resumePositionMs("video", 60_000L)).isEqualTo(0L)
    }

    @Test
    fun resumePositionMs_whenSavedPositionIsPastNinetyFivePercent_returnsZero() = runTest {
        coEvery { dao.get("video") } returns PodcastProgressEntity(
            videoId = "video",
            positionMs = 58_000L,
            durationMs = 60_000L,
        )

        assertThat(store().resumePositionMs("video", 60_000L)).isEqualTo(0L)
    }
    @Test
    fun resumePositionMs_whenSavedPositionIsInRange_returnsIt() = runTest {
        coEvery { dao.get("video") } returns PodcastProgressEntity(
            videoId = "video",
            positionMs = 25_000L,
            durationMs = 60_000L,
        )

        assertThat(store().resumePositionMs("video", 60_000L)).isEqualTo(25_000L)
    }

    @Test
    fun save_whenPositionIsUnderThreeSeconds_doesNotWriteARow() = runTest {
        store().save(
            videoId = "video",
            showId = "MPSPPLabc",
            positionMs = 2_000L,
            durationMs = 60_000L,
            completed = false,
        )

        coVerify(exactly = 0) { dao.upsert(any()) }
    }

    @Test
    fun save_whenPositionIsAboveResumeFloor_writesARow() = runTest {
        store().save(
            videoId = "video",
            showId = "MPSPPLabc",
            positionMs = 4_000L,
            durationMs = 60_000L,
            completed = false,
        )

        coVerify(exactly = 1) { dao.upsert(any()) }
    }

    @Test
    fun save_whenPositionIsPastNinetyFivePercent_marksTheEpisodeCompleted() = runTest {
        val entity = slot<PodcastProgressEntity>()
        coEvery { dao.upsert(capture(entity)) } returns Unit

        store().save(
            videoId = "video",
            showId = "MPSPPLabc",
            positionMs = 59_000L,
            durationMs = 60_000L,
            completed = false,
        )

        coVerify(exactly = 1) { dao.upsert(any()) }
        assertThat(entity.captured.completed).isTrue()
        assertThat(entity.captured.positionMs).isEqualTo(59_000L)
        assertThat(entity.captured.showId).isEqualTo("MPSPPLabc")
    }

    @Test
    fun save_whenPositionIsInRange_storesThePositionUncompleted() = runTest {
        val entity = slot<PodcastProgressEntity>()
        coEvery { dao.upsert(capture(entity)) } returns Unit

        store().save(
            videoId = "video",
            showId = "MPSPPLabc",
            positionMs = 30_000L,
            durationMs = 60_000L,
            completed = false,
        )

        coVerify(exactly = 1) { dao.upsert(any()) }
        assertThat(entity.captured.completed).isFalse()
        assertThat(entity.captured.positionMs).isEqualTo(30_000L)
    }
}

package com.tuned.app.ui.album

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TrackArtistLineTest {
    @Test
    fun sameArtistIsHidden() {
        assertThat(trackArtistLine("Justice", "justice")).isNull()
    }

    @Test
    fun oneOfTheAlbumsCreditedArtistsIsHidden() {
        assertThat(trackArtistLine("Kanye West", "Kanye West & Ye")).isNull()
    }

    @Test
    fun aGuestStillShows() {
        assertThat(trackArtistLine("Kanye West & CeeLo Green", "Kanye West & Ye")).isEqualTo("Kanye West & CeeLo Green")
    }

    @Test
    fun blankShowsNothing() {
        assertThat(trackArtistLine("", "Kanye West")).isNull()
    }
}

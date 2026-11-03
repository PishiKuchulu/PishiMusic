package com.tuned.app.data.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OtherVersionsShelfTest {
    @Test
    fun editionsShareTheBaseTitle() {
        assertThat(albumBaseTitle("BULLY - DELUXE")).isEqualTo("bully")
        assertThat(albumBaseTitle("Bully (Deluxe)")).isEqualTo("bully")
        assertThat(albumBaseTitle("BULLY")).isEqualTo("bully")
    }

    @Test
    fun englishHeadingIsEnough() {
        assertThat(isOtherVersionsShelf("Other versions", "BULLY", listOf("Anything"))).isTrue()
    }

    @Test
    fun translatedHeadingIsRecognisedByTheTitles() {
        assertThat(isOtherVersionsShelf("他のバージョン", "BULLY", listOf("BULLY - DELUXE"))).isTrue()
    }

    @Test
    fun otherShelvesAreLeftAlone() {
        assertThat(isOtherVersionsShelf("Releases for you", "BULLY", listOf("BULLY", "Alfredo 2"))).isFalse()
        assertThat(isOtherVersionsShelf("Releases for you", "BULLY", emptyList())).isFalse()
    }
}

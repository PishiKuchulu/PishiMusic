package com.tuned.app.ui.shell

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TabSwipeStepTest {
    @Test
    fun swipingLeftGoesToTheNextTab() {
        assertThat(tabSwipeStep(-200f, 0f, 150f, rtl = false)).isEqualTo(1)
    }

    @Test
    fun swipingRightGoesBack() {
        assertThat(tabSwipeStep(200f, 0f, 150f, rtl = false)).isEqualTo(-1)
    }

    @Test
    fun aFastFlickCountsEvenWhenShort() {
        assertThat(tabSwipeStep(-20f, -1500f, 150f, rtl = false)).isEqualTo(1)
    }

    @Test
    fun aSmallSlowDragStaysPut() {
        assertThat(tabSwipeStep(-40f, -100f, 150f, rtl = false)).isEqualTo(0)
    }

    @Test
    fun rightToLeftLayoutsFlip() {
        assertThat(tabSwipeStep(-200f, 0f, 150f, rtl = true)).isEqualTo(-1)
    }
}

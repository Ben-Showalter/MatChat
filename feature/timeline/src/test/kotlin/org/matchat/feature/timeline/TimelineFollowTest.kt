package org.matchat.feature.timeline

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** [followAfterScroll] — new messages land flush with the bottom unless the
 *  user has scrolled up (UX-SPEC S9). */
class TimelineFollowTest {

    @Test
    fun `scrolling up away from the newest message stops following`() {
        assertFalse(followAfterScroll(following = true, dy = -40, canScrollDown = true))
    }

    @Test
    fun `scrolling back down to the end follows again`() {
        assertTrue(followAfterScroll(following = false, dy = 40, canScrollDown = false))
    }

    @Test
    fun `scrolling down but not yet at the end still does not follow`() {
        assertFalse(followAfterScroll(following = false, dy = 40, canScrollDown = true))
    }

    @Test
    fun `a layout pass leaves following on even though new rows sit below`() {
        assertTrue(followAfterScroll(following = true, dy = 0, canScrollDown = true))
    }

    @Test
    fun `a layout pass leaves following off while scrolled up`() {
        assertFalse(followAfterScroll(following = false, dy = 0, canScrollDown = false))
    }
}

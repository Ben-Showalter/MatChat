package org.matchat.core.ui.softkey

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure unit coverage for [mirroredLabels] (Phase 6, UI improvement plan) —
 *  no Fragment/Robolectric harness needed. */
class SoftkeyMirrorTest {

    @Test
    fun `unswapped keeps each label at its own position`() {
        val (l, c, r) = mirroredLabels("Options", "Select", "Back", swapped = false)
        assertEquals("Options", l)
        assertEquals("Select", c)
        assertEquals("Back", r)
    }

    @Test
    fun `swapped exchanges left and right, center stays put`() {
        val (l, c, r) = mirroredLabels("Options", "Select", "Back", swapped = true)
        assertEquals("Back", l)
        assertEquals("Select", c)
        assertEquals("Options", r)
    }

    @Test
    fun `a blank label stays blank after swapping position`() {
        val (l, c, r) = mirroredLabels("", "Select", "Back", swapped = true)
        assertEquals("Back", l)
        assertEquals("Select", c)
        assertEquals("", r)
    }
}

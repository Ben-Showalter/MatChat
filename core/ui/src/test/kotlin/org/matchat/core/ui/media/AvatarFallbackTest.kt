package org.matchat.core.ui.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** Pure logic, no Android API — no Robolectric needed (unlike KeyMapTest). */
class AvatarFallbackTest {

    @Test
    fun `the same user id always gets the same color`() {
        assertEquals(AvatarFallback.colorFor("@wayne:example.org"), AvatarFallback.colorFor("@wayne:example.org"))
    }

    @Test
    fun `different user ids can get different colors`() {
        // Not a strict guarantee (a hash can collide), but these two should
        // land in different buckets — a canary against an accidental
        // constant-color bug.
        assertNotEquals(AvatarFallback.colorFor("@wayne:example.org"), AvatarFallback.colorFor("@merv:example.org"))
    }

    @Test
    fun `initial is the first letter, uppercased`() {
        assertEquals("W", AvatarFallback.initial("wayne"))
        assertEquals("W", AvatarFallback.initial("Wayne"))
    }

    @Test
    fun `initial strips a leading raw-Matrix-ID at sign`() {
        assertEquals("W", AvatarFallback.initial("@wayne:example.org"))
    }

    @Test
    fun `initial falls back to a question mark for nothing usable`() {
        assertEquals("?", AvatarFallback.initial(""))
        assertEquals("?", AvatarFallback.initial("@"))
        assertEquals("?", AvatarFallback.initial("   "))
    }
}

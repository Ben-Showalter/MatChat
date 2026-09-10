package org.matchat.core.matrix.internal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * [PinnedEventsContent.withEvent] only — the list-editing half needs no
 * Android API. [PinnedEventsContent.toJson] (org.json.JSONObject) isn't
 * covered here: android.jar's org.json classes are stubs outside
 * Robolectric, which this module doesn't pull in (same untested-since gap
 * as core/rtc's CallMembership, which builds JSON the same way).
 */
class PinnedEventsContentTest {

    @Test
    fun `pinning adds the event once`() {
        assertEquals(listOf("a", "b"), PinnedEventsContent.withEvent(listOf("a"), "b", pinned = true))
    }

    @Test
    fun `pinning an already-pinned event is a no-op`() {
        assertEquals(listOf("a", "b"), PinnedEventsContent.withEvent(listOf("a", "b"), "b", pinned = true))
    }

    @Test
    fun `unpinning removes the event`() {
        assertEquals(listOf("a"), PinnedEventsContent.withEvent(listOf("a", "b"), "b", pinned = false))
    }

    @Test
    fun `unpinning an event not present is a no-op`() {
        assertEquals(listOf("a"), PinnedEventsContent.withEvent(listOf("a"), "z", pinned = false))
    }
}

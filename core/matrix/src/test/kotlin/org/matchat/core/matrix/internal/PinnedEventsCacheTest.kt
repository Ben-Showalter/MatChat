package org.matchat.core.matrix.internal

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * [PinnedEventsCache] is a plain `object` — JVM-process-global state, not
 * reset between tests — so each test below uses its own distinct room id to
 * stay independent of test execution order.
 */
class PinnedEventsCacheTest {

    @Test
    fun `get returns null when nothing has been cached for the room yet`() {
        assertNull(PinnedEventsCache.get("!room-a:example.org"))
    }

    @Test
    fun `put then get returns what was put`() {
        val roomId = "!room-b:example.org"
        PinnedEventsCache.put(roomId, setOf("a", "b"))
        assertEquals(setOf("a", "b"), PinnedEventsCache.get(roomId))
    }

    @Test
    fun `updatesFor emits only values published after collection starts`() = runTest {
        val roomId = "!room-c:example.org"
        PinnedEventsCache.put(roomId, setOf("before")) // must not replay to a fresh subscriber

        PinnedEventsCache.updatesFor(roomId).test {
            PinnedEventsCache.put(roomId, setOf("after"))
            assertEquals(setOf("after"), awaitItem())
        }
    }

    @Test
    fun `updatesFor ignores a different room's put`() = runTest {
        val roomId = "!room-d:example.org"
        val otherRoomId = "!room-e:example.org"

        PinnedEventsCache.updatesFor(roomId).test {
            PinnedEventsCache.put(otherRoomId, setOf("x"))
            PinnedEventsCache.put(roomId, setOf("y"))
            assertEquals(setOf("y"), awaitItem())
        }
    }

    /** Regression test for the "2nd pin replaces the first" bug:
     *  [RustRoomTimeline.setPinned] used to build its write baseline from a
     *  cold SDK read alone, which lags the server's echo of the *previous*
     *  write — pin A, then pin B before that echo lands, and the cold read
     *  comes back missing A, so the write replaces the pinned list with [B]
     *  alone. The fix prefers [PinnedEventsCache.get] (put synchronously
     *  right after every successful write, no round trip needed) over that
     *  cold read. This reproduces the fix's exact expression —
     *  `(PinnedEventsCache.get(roomId) ?: staleColdRead).toList()` — against
     *  [PinnedEventsContent.withEvent], the same two already-unit-tested
     *  pieces [RustRoomTimeline.setPinned] combines; a `Room`/`RoomInfo`
     *  mock would be needed to exercise setPinned itself, which this module
     *  deliberately doesn't do (see this file's own class doc). */
    @Test
    fun `write baseline prefers the cache over a colder value, so two pins issued close together both stick`() {
        val roomId = "!room-f:example.org"
        // Pin "a" completes; its cache entry lands (PinnedEventsCache.put,
        // exactly as setPinned does after every successful write).
        PinnedEventsCache.put(roomId, setOf("a"))

        // A cold SDK read that hasn't caught up with that write yet — the
        // race this fixes. Deliberately empty, standing in for
        // fetchPinnedIds(r) returning stale room state.
        val staleColdRead = emptySet<String>()
        val current = (PinnedEventsCache.get(roomId) ?: staleColdRead).toList()
        val next = PinnedEventsContent.withEvent(current, "b", pinned = true)

        assertEquals(listOf("a", "b"), next)
    }
}

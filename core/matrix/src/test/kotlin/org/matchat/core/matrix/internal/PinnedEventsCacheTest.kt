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
}

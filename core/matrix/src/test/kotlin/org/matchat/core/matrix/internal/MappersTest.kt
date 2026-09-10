package org.matchat.core.matrix.internal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Phase 7 (UI improvement plan): the timeline used to show every sender as
 * their raw Matrix ID (a confirmed bug, `event.sender` used directly).
 * [Mappers.resolveSenderName] is the pure part of the fix — the part that
 * doesn't need a live SDK/homeserver (AGENTS.md §6) — RustRoomTimeline
 * supplies the member-list map this reads from (extended in the Avatars
 * round to carry an avatarUrl alongside displayName; resolveSenderName only
 * reads the name half).
 */
class MappersTest {

    @Test
    fun `a known member resolves to their display name`() {
        val members = mapOf("@wayne:example.org" to MemberInfo("Wayne", avatarUrl = null))
        assertEquals("Wayne", Mappers.resolveSenderName("@wayne:example.org", members))
    }

    @Test
    fun `an unknown sender falls back to the raw Matrix ID, never blank`() {
        assertEquals("@ray:example.org", Mappers.resolveSenderName("@ray:example.org", emptyMap()))
    }

    @Test
    fun `a member with no display name set falls back to the raw Matrix ID`() {
        // fetchMembers (RustRoomTimeline) omits a member with neither field set
        // rather than storing an empty string, so this is the same case as
        // "unknown".
        val members = mapOf("@other:example.org" to MemberInfo("Other", avatarUrl = null))
        assertEquals("@wayne:example.org", Mappers.resolveSenderName("@wayne:example.org", members))
    }
}

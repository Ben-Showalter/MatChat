package org.matchat.feature.timeline

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.matchat.core.model.ReactionSummary

/** [resolveReactionKey]/[normalizeReactionKey] — the fix for "reacting with
 *  an emoji already on the message adds a duplicate chip instead of
 *  bumping the count" (a Unicode variation-selector mismatch between the
 *  picker's own literal and whatever key is already in use). */
class ReactionKeyTest {

    @Test
    fun `no existing reaction keeps the chosen literal`() {
        assertEquals("👍", resolveReactionKey(emptyList(), "👍"))
    }

    @Test
    fun `an exact existing match is reused as-is`() {
        val existing = listOf(ReactionSummary("👍", 1, reactedByMe = false))
        assertEquals("👍", resolveReactionKey(existing, "👍"))
    }

    @Test
    fun `a variation-selector mismatch still resolves to the existing key`() {
        // The message already has the VS16 form; the picker's own literal
        // (in REACTION_CHOICES) happens to be the bare form here.
        val existing = listOf(ReactionSummary("❤️", 1, reactedByMe = false))
        assertEquals("❤️", resolveReactionKey(existing, "❤"))
    }

    @Test
    fun `a different emoji is never matched`() {
        val existing = listOf(ReactionSummary("👎", 1, reactedByMe = false))
        assertEquals("👍", resolveReactionKey(existing, "👍"))
    }

    @Test
    fun `normalize strips variation selectors`() {
        assertEquals("❤", normalizeReactionKey("❤️"))
        assertEquals("A", normalizeReactionKey("A︎"))
        assertEquals("👍", normalizeReactionKey("👍"))
    }
}

package org.matchat.client.sync

import org.junit.Assert.assertEquals
import org.junit.Test
import org.matchat.client.sync.NotificationDecision.Action

class NotificationDecisionTest {

    private fun decide(
        prev: Int,
        now: Int,
        previewChanged: Boolean = false,
        showing: Boolean = true,
        enabled: Boolean = true,
    ) = NotificationDecision.decide(prev, now, previewChanged, showing, enabled)

    @Test
    fun `a new unread message alerts`() {
        assertEquals(Action.ALERT, decide(prev = 0, now = 1))
        assertEquals(Action.ALERT, decide(prev = 1, now = 2, previewChanged = true))
    }

    @Test
    fun `the text arriving after the count updates silently`() {
        assertEquals(Action.UPDATE_SILENTLY, decide(prev = 1, now = 1, previewChanged = true))
    }

    @Test
    fun `a dismissed notification is not brought back`() {
        assertEquals(Action.NONE, decide(prev = 1, now = 1, previewChanged = true, showing = false))
    }

    @Test
    fun `nothing changed does nothing`() {
        assertEquals(Action.NONE, decide(prev = 1, now = 1))
        assertEquals(Action.NONE, decide(prev = 0, now = 0, previewChanged = true))
    }

    @Test
    fun `reading the room cancels`() {
        assertEquals(Action.CANCEL, decide(prev = 2, now = 0))
    }

    @Test
    fun `with notifications off it only ever cancels`() {
        assertEquals(Action.NONE, decide(prev = 0, now = 1, enabled = false))
        assertEquals(Action.NONE, decide(prev = 1, now = 1, previewChanged = true, enabled = false))
        assertEquals(Action.CANCEL, decide(prev = 1, now = 0, enabled = false))
    }
}

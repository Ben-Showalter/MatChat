package org.matchat.core.ui.softkey

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.matchat.core.model.SyncState
import org.matchat.core.ui.R

/** Pure unit coverage for [connectionDotColorAttr] (Online indicator round) —
 *  no Fragment/Robolectric harness needed, same convention as
 *  SoftkeyMirrorTest's coverage of mirroredLabels. */
class ConnectionDotColorTest {

    @Test
    fun `SYNCING (the ordinary connected state here) is green`() {
        assertEquals(R.attr.colorEncrypted, connectionDotColorAttr(SyncState.SYNCING))
    }

    @Test
    fun `OFFLINE and ERROR are both red`() {
        assertEquals(R.attr.colorError, connectionDotColorAttr(SyncState.OFFLINE))
        assertEquals(R.attr.colorError, connectionDotColorAttr(SyncState.ERROR))
    }

    @Test
    fun `IDLE (no active session) hides the dot rather than picking a color`() {
        assertNull(connectionDotColorAttr(SyncState.IDLE))
    }

    @Test
    fun `connected and not-connected never resolve to the same color`() {
        assertNotEquals(connectionDotColorAttr(SyncState.SYNCING), connectionDotColorAttr(SyncState.OFFLINE))
    }
}

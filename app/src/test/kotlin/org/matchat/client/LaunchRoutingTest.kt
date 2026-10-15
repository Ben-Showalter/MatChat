package org.matchat.client

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Opening MatChat lands on the room list unless it came from a notification
 *  (LaunchRouting.kt). */
class LaunchRoutingTest {

    private fun goHome(
        userLeft: Boolean = true,
        launchedExternal: Boolean = false,
        screenOnWhenStopped: Boolean = true,
        fromNotification: Boolean = false,
        inCall: Boolean = false,
        hasSession: Boolean = true,
    ) = shouldGoHome(userLeft, launchedExternal, screenOnWhenStopped, fromNotification, inCall, hasSession)

    @Test
    fun `leaving with Home or End and opening the app again goes to the room list`() {
        assertTrue(goHome())
    }

    @Test
    fun `a notification tap opens its own room instead`() {
        assertFalse(goHome(fromNotification = true))
    }

    @Test
    fun `returning from a picker or settings page MatChat opened stays put`() {
        assertFalse(goHome(launchedExternal = true))
    }

    @Test
    fun `closing and opening the flip stays put`() {
        assertFalse(goHome(userLeft = false, screenOnWhenStopped = false))
        assertFalse(goHome(screenOnWhenStopped = false))
    }

    @Test
    fun `being interrupted without the user leaving stays put`() {
        assertFalse(goHome(userLeft = false))
    }

    @Test
    fun `an ongoing call keeps its screen`() {
        assertFalse(goHome(inCall = true))
    }

    @Test
    fun `without a session there is no room list to go to`() {
        assertFalse(goHome(hasSession = false))
    }
}

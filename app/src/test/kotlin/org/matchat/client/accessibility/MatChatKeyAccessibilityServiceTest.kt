package org.matchat.client.accessibility

import android.view.KeyEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure unit coverage for [isInterceptedSoftkey] — a plain Int compare
 * against a real [KeyEvent] keycode constant, so no Android runtime/
 * Robolectric is needed here (this module's existing convention, per
 * SilentLeadInSoundTest: only the pure, non-Android parts of a class get a
 * JVM unit test). The service's actual key interception
 * (FLAG_REQUEST_FILTER_KEY_EVENTS, granted only once a user enables system
 * Accessibility access) and MainActivity.handleAccessibilityKeyEvent's
 * delegation into the already-tested KeyMap/LogicalKeyReceiver path can't be
 * verified without a real device/emulator and a real (or simulated) T9 IME
 * — see the manual QA checklist in this round's PR description.
 */
class MatChatKeyAccessibilityServiceTest {

    @Test
    fun `the right softkey is intercepted`() {
        assertTrue(isInterceptedSoftkey(KeyEvent.KEYCODE_SOFT_RIGHT))
    }

    @Test
    fun `the left softkey is never intercepted`() {
        assertFalse(isInterceptedSoftkey(KeyEvent.KEYCODE_SOFT_LEFT))
    }

    @Test
    fun `MENU is never intercepted`() {
        assertFalse(isInterceptedSoftkey(KeyEvent.KEYCODE_MENU))
    }

    @Test
    fun `the dedicated BACK key is never intercepted`() {
        assertFalse(isInterceptedSoftkey(KeyEvent.KEYCODE_BACK))
    }

    @Test
    fun `digits, D-pad, and CENTER are never intercepted`() {
        assertFalse(isInterceptedSoftkey(KeyEvent.KEYCODE_1))
        assertFalse(isInterceptedSoftkey(KeyEvent.KEYCODE_DPAD_UP))
        assertFalse(isInterceptedSoftkey(KeyEvent.KEYCODE_DPAD_CENTER))
        assertFalse(isInterceptedSoftkey(KeyEvent.KEYCODE_ENTER))
    }
}

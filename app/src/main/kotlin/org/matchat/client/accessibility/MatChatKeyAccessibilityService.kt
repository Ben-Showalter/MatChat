package org.matchat.client.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import org.matchat.client.MainActivity

/**
 * Optional, user-enabled workaround (Settings > Advanced > "Softkey helper",
 * which links to system Accessibility settings) for a confirmed device
 * conflict, diagnosed with the user via adb logcat: on some hardware, the
 * system's own predictive-text ("T9word") keyboard consumes
 * KEYCODE_SOFT_RIGHT before it ever reaches MainActivity.dispatchKeyEvent,
 * but ONLY while an EditText/IME is active — outside of composing, the
 * normal dispatch path already works fine. (A separate app, TurboText, also
 * runs its own accessibility-based key logger on the diagnosed device, but
 * its own log output showed it observing, not consuming, the key — a red
 * herring; the T9 IME itself is the actual consumer.)
 *
 * A service requesting FLAG_REQUEST_FILTER_KEY_EVENTS receives hardware key
 * events earlier in the platform's input pipeline than IME processing does
 * — that's the documented purpose of the flag — so it gets a chance to
 * claim the key before that IME can swallow it.
 *
 * Scope is deliberately as narrow as possible, per explicit direction: this
 * service claims ONLY the physical right softkey (KEYCODE_SOFT_RIGHT) — not
 * SOFT_LEFT, not MENU, not the dedicated BACK key. Every other key —
 * including all of T9's own digit/D-pad/CENTER input, and the left softkey
 * — returns false immediately from [onKeyEvent], completely untouched, and
 * continues through the normal platform pipeline exactly as if this service
 * didn't exist.
 *
 * A claimed key is handed to [MainActivity.handleExternalSoftkey], which
 * runs it through the exact same [org.matchat.core.ui.key.KeyMap] +
 * LogicalKeyReceiver path `dispatchKeyEvent` already uses — no duplicated
 * or divergent key-handling logic.
 *
 * No canRetrieveWindowContent, no screen reading, ever
 * (key_accessibility_service_config.xml). Entirely inert unless the user
 * explicitly enables it in system Accessibility settings — nothing about
 * normal dispatchKeyEvent handling changes if they don't, on this device or
 * any other.
 */
class MatChatKeyAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        serviceInfo = serviceInfo?.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        if (!isInterceptedSoftkey(event.keyCode)) return false
        return MainActivity.handleExternalSoftkey(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) = Unit

    override fun onInterrupt() = Unit
}

/** True only for the physical right softkey. A top-level, pure function
 *  (not a private method on the service) so it's unit-testable without an
 *  Android runtime — a plain Int compare against a real [KeyEvent] keycode
 *  constant needs no Robolectric, unlike constructing/inspecting a live
 *  [KeyEvent] instance would. */
internal fun isInterceptedSoftkey(keyCode: Int): Boolean = keyCode == KeyEvent.KEYCODE_SOFT_RIGHT

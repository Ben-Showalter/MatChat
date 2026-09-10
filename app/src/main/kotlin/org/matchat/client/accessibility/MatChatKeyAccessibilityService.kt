package org.matchat.client.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import org.matchat.client.MainActivity

/**
 * Optional, user-enabled workaround (Settings > Accessibility > MatChat softkeys)
 * for a confirmed device conflict, diagnosed with the user via adb logcat: on
 * this hardware, the system's own predictive-text ("T9word") keyboard consumes
 * KEYCODE_SOFT_RIGHT (and presumably the other softkey-ish codes) before it ever
 * reaches MainActivity.dispatchKeyEvent, but ONLY while an EditText/IME is
 * active — outside of composing, the normal dispatch path already works fine.
 * (TurboTextKeyService, a separate accessibility-based key *logger* on this
 * device, was a red herring: its own log line showed it observing, not
 * consuming, the key. The T9 IME itself is the actual consumer.)
 *
 * A service requesting FLAG_REQUEST_FILTER_KEY_EVENTS receives hardware key
 * events earlier in the platform's input pipeline than IME processing does —
 * that's the documented purpose of the flag — so it gets a chance to claim the
 * four softkey-ish codes before that IME can swallow them.
 *
 * Scope is deliberately as narrow as possible:
 * - Only KEYCODE_SOFT_LEFT/SOFT_RIGHT/MENU/BACK are ever inspected here — every
 *   other code (all of T9's own digit/DPAD/CENTER input) returns false
 *   immediately, completely untouched, so text entry is unaffected whether or
 *   not this service is enabled.
 * - A claimed key is handed to [MainActivity.handleExternalSoftkey], which runs
 *   through the exact same KeyMap + LogicalKeyReceiver path dispatchKeyEvent
 *   uses — no duplicated or divergent key-handling logic.
 * - No canRetrieveWindowContent, no screen reading, ever
 *   (key_accessibility_service_config.xml).
 *
 * Entirely inert unless the user explicitly enables it in system Accessibility
 * settings — nothing about normal dispatchKeyEvent handling changes if they
 * don't, on this device or any other.
 */
class MatChatKeyAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        serviceInfo = serviceInfo?.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        if (event.keyCode !in SOFTKEY_CODES) return false
        return MainActivity.handleExternalSoftkey(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) = Unit

    override fun onInterrupt() = Unit

    private companion object {
        // Exactly the codes KeyMap's per-device table recognizes as one of the
        // two softkey positions or the dedicated Back key — nothing else.
        val SOFTKEY_CODES = setOf(
            KeyEvent.KEYCODE_SOFT_LEFT,
            KeyEvent.KEYCODE_SOFT_RIGHT,
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_BACK,
        )
    }
}

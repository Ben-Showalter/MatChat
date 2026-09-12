package org.matchat.client.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
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
 * normal dispatch path already works fine.
 *
 * UNRESOLVED as of the latest on-device report (right softkey still doesn't
 * open Options with this service enabled): a separate app, TurboText, also
 * runs its own accessibility-based key service (com.turbotext.app /
 * TurboTextKeyService) on the diagnosed device. An earlier capture judged it
 * a "red herring" — its own log output appeared to show it observing, not
 * consuming, the key. A fresh capture then showed that same service
 * actively reacting to KEYCODE_SOFT_RIGHT, gated on which app is in the
 * foreground; but disabling TurboText's accessibility service entirely and
 * retesting did NOT fix the symptom either, so it is not (solely) the
 * cause. Neither test result should be treated as settled — there was, and
 * still is, no logging anywhere in this class or MainActivity's key-dispatch
 * path to actually confirm what this service itself sees or returns. That
 * logging now exists (SOFTKEY_LOG_TAG = "MatChatSoftkey" in MainActivity,
 * and below) specifically so the next on-device logcat capture can show,
 * unambiguously, whether onKeyEvent below is even being invoked at all —
 * needed before guessing at an actual fix again. One other platform
 * behavior worth ruling out on the next pass: since Android 13, a sideloaded
 * app's Accessibility toggle (this app always sideloads on this device
 * class — ADR 0004, no Play Store) can be silently blocked by the OS's
 * "restricted settings" protection until the user explicitly allows it
 * (device Settings > Apps > MatChat > overflow menu > "Allow restricted
 * settings," then re-enable Accessibility) — the toggle can visually read
 * "on" while the service was never actually granted the flag below.
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
        // Diagnostic only (see this class's doc comment): proves the service
        // actually connected and was granted the flag — if this never logs on a
        // device where the user believes they enabled it, the OS never actually
        // started it (e.g. Android 13+'s "restricted settings" silently blocking
        // a sideloaded app's toggle).
        Log.d(LOG_TAG, "onServiceConnected: flags=${serviceInfo?.flags}")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        if (!isInterceptedSoftkey(event.keyCode)) return false
        val consumed = MainActivity.handleExternalSoftkey(event)
        Log.d(LOG_TAG, "onKeyEvent: keyCode=${event.keyCode} consumed=$consumed")
        return consumed
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) = Unit

    override fun onInterrupt() = Unit

    private companion object {
        // Same tag MainActivity's dispatchKeyEvent/handleAccessibilityKeyEvent
        // log under, so one logcat filter shows the whole path in order.
        const val LOG_TAG = "MatChatSoftkey"
    }
}

/** True only for the physical right softkey. A top-level, pure function
 *  (not a private method on the service) so it's unit-testable without an
 *  Android runtime — a plain Int compare against a real [KeyEvent] keycode
 *  constant needs no Robolectric, unlike constructing/inspecting a live
 *  [KeyEvent] instance would. */
internal fun isInterceptedSoftkey(keyCode: Int): Boolean = keyCode == KeyEvent.KEYCODE_SOFT_RIGHT

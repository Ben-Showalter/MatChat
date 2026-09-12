package org.matchat.core.ui.key

import android.view.KeyEvent

/**
 * The single translation from raw [KeyEvent] codes to [LogicalKey]. Softkey
 * codes vary by device — KEYCODE_SOFT_LEFT/SOFT_RIGHT are frequently never
 * dispatched, and the keys arrive as KEYCODE_MENU / KEYCODE_BACK or an OEM
 * private code (PLAN.md risk table, AGENTS.md §4). This table is per-device data,
 * established by the M0 key-logger spike on real hardware. Nothing outside this
 * file reads a keycode *to decide what it logically means* — the one narrow
 * exception is `MatChatKeyAccessibilityService` (app/accessibility, docs/adr/0007
 * addendum), which reads a raw keycode only to decide whether to intercept it
 * at all (a plain equality check against KEYCODE_SOFT_RIGHT), before handing
 * any actually-claimed event straight back into [map] for the real
 * translation — it never derives a [LogicalKey] itself.
 *
 * Long-press (`#`, `*`) is resolved by the caller tracking down/up time; [map]
 * handles the single-press codes and the D-pad.
 */
object KeyMap {

    /** Returns the logical key for a key-DOWN event, or null to fall through.
     *  [swapped] (Settings > Advanced > "Swap Left/Right keys", Phase 6 of the
     *  UI improvement plan; docs/adr/0007) flips SOFT_LEFT/SOFT_RIGHT at the
     *  end, after the normal per-device keycode table below — a device whose
     *  hardware softkeys are physically reversed still maps LEFT-position to
     *  Options semantically everywhere past this function, just via the
     *  opposite raw keycode.
     *
     *  KEYCODE_BACK is handled separately, first, and NEVER swapped: on
     *  hardware with a dedicated Back key (distinct from the two labeled
     *  positional softkeys), that key must always mean Back — it isn't one
     *  of the two things the swap preference is about. A confirmed bug had
     *  it swapping to Options along with the positional keys, since the
     *  per-device table below originally folded BACK into the same
     *  LogicalKey as the right softkey position (true before the swap
     *  preference existed, since they meant the same thing; no longer true
     *  once "the right position" and "always Back" can disagree). */
    fun map(event: KeyEvent, swapped: Boolean = false): LogicalKey? {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) return LogicalKey.SOFT_RIGHT
        val logical = mapRaw(event) ?: return null
        return if (swapped) swapSoftkeys(logical) else logical
    }

    private fun swapSoftkeys(key: LogicalKey): LogicalKey = when (key) {
        LogicalKey.SOFT_LEFT -> LogicalKey.SOFT_RIGHT
        LogicalKey.SOFT_RIGHT -> LogicalKey.SOFT_LEFT
        else -> key
    }

    private fun mapRaw(event: KeyEvent): LogicalKey? = when (event.keyCode) {
        KeyEvent.KEYCODE_DPAD_UP -> LogicalKey.UP
        KeyEvent.KEYCODE_DPAD_DOWN -> LogicalKey.DOWN
        KeyEvent.KEYCODE_DPAD_LEFT -> LogicalKey.LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> LogicalKey.RIGHT
        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> LogicalKey.CENTER

        // LEFT softkey = Options. Most AOSP flips deliver it as MENU; some as the
        // legacy SOFT_LEFT. Add a SKU's OEM code here, never in a feature module.
        KeyEvent.KEYCODE_SOFT_LEFT, KeyEvent.KEYCODE_MENU -> LogicalKey.SOFT_LEFT

        // RIGHT softkey position. KEYCODE_BACK is NOT handled here — see map()'s
        // doc comment: it's a separate, never-swapped, always-Back case.
        KeyEvent.KEYCODE_SOFT_RIGHT -> LogicalKey.SOFT_RIGHT

        // Hardware call keys (docs/VOICE.md §6). Present on these feature phones;
        // only the call screens act on them, elsewhere they fall through.
        KeyEvent.KEYCODE_CALL -> LogicalKey.CALL
        KeyEvent.KEYCODE_ENDCALL -> LogicalKey.END

        KeyEvent.KEYCODE_0 -> LogicalKey.DIGIT_0
        KeyEvent.KEYCODE_1 -> LogicalKey.DIGIT_1
        KeyEvent.KEYCODE_2 -> LogicalKey.DIGIT_2
        KeyEvent.KEYCODE_3 -> LogicalKey.DIGIT_3
        KeyEvent.KEYCODE_4 -> LogicalKey.DIGIT_4
        KeyEvent.KEYCODE_5 -> LogicalKey.DIGIT_5
        KeyEvent.KEYCODE_6 -> LogicalKey.DIGIT_6
        KeyEvent.KEYCODE_7 -> LogicalKey.DIGIT_7
        KeyEvent.KEYCODE_8 -> LogicalKey.DIGIT_8
        KeyEvent.KEYCODE_9 -> LogicalKey.DIGIT_9

        else -> null
    }

    /** Codes that a long-press turns into a hold action. Unlike `#`/`*`
     *  (absent from [mapRaw], so a short press of those does nothing
     *  anywhere), CENTER/ENTER are also mapped in [mapRaw] to plain CENTER —
     *  a long press therefore dispatches both, in order: the initial
     *  (non-long) DOWN fires plain CENTER first, then this fires CENTER_HOLD
     *  once the OS's long-press threshold passes. CENTER stays instant
     *  everywhere by default; only a screen that specifically opts into
     *  treating CENTER_HOLD as its real "confirm" (and CENTER itself as a
     *  no-op in that context) sees hold-to-confirm behavior. */
    fun holdKey(keyCode: Int): LogicalKey? = when (keyCode) {
        KeyEvent.KEYCODE_POUND -> LogicalKey.HASH_HOLD
        KeyEvent.KEYCODE_STAR -> LogicalKey.STAR_HOLD
        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> LogicalKey.CENTER_HOLD
        else -> null
    }
}

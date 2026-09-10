package org.matchat.core.ui.softkey

import org.matchat.core.ui.key.LogicalKey

/**
 * An optional, narrow exception to the rule that UP/DOWN/LEFT/RIGHT always
 * go to the platform's own focus search (`MainActivity.dispatchKeyEvent`) —
 * a screen implements this only when it wants to intercept one of those
 * keys itself instead (docs/adr/0007's softkey-swap exception is the same
 * shape of narrow, explicit deviation; this is documented here rather than
 * a new ADR file, matching this session's own precedent for a
 * similarly-scoped change).
 *
 * Currently used only for D-pad RIGHT as a shortcut to Pinned messages
 * (TimelineFragment, Pinned messages quick-access round) — UP/DOWN/LEFT are
 * untouched by this interface's existence; `MainActivity` only consults it
 * for RIGHT, and only when the current screen implements it.
 */
interface DirectionalKeyReceiver {
    /** Return true to consume [key] (skip platform focus search for it). */
    fun onDirectionalKey(key: LogicalKey): Boolean
}

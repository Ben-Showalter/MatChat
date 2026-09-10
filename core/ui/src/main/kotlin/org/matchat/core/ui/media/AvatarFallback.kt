package org.matchat.core.ui.media

/**
 * Element-style no-avatar fallback: a deterministic color per user id, and
 * the display name's first letter. [colorFor] deliberately isn't routed
 * through the app's `?attr` theme system — these colors exist purely to
 * tell people apart at a glance, the same across every theme, not to match
 * chrome (a narrow, explicit exception to "everything goes through
 * `?attr`", documented here rather than scattered comments at each call
 * site).
 */
object AvatarFallback {

    /** A small fixed palette, not theme-driven (see class doc) — picked for
     *  reasonable legibility of a white letter on top, in both themes. */
    private val PALETTE = intArrayOf(
        0xFF368BD6.toInt(), // blue
        0xFFAC3BA8.toInt(), // fuchsia
        0xFF03B381.toInt(), // green
        0xFFE64F7A.toInt(), // rose
        0xFFFF812D.toInt(), // orange
        0xFF2DC2C5.toInt(), // teal
        0xFF5C56F5.toInt(), // purple
        0xFF74D12C.toInt(), // lime
    )

    /** Deterministic: the same [userId] always gets the same color, on any
     *  device, any session — a plain sum-of-code-units hash, the same
     *  well-known approach Element itself uses. */
    fun colorFor(userId: String): Int = PALETTE[userId.sumOf { it.code } % PALETTE.size]

    /** The letter shown in the fallback circle: [name]'s first letter/digit,
     *  uppercased, ignoring a leading "@" (the raw-Matrix-ID fallback name
     *  shape) — "?" if there's nothing usable. */
    fun initial(name: String): String {
        val first = name.trim().removePrefix("@").firstOrNull { it.isLetterOrDigit() } ?: return "?"
        return first.uppercaseChar().toString()
    }
}

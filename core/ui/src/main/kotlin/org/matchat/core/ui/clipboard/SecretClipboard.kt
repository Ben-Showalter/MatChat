package org.matchat.core.ui.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build

/**
 * Reading a pasted setup link (UX-SPEC S3/S4, Flip-DumbPhoneGuide "Setup
 * codes"): the text is read once and the clipboard is cleared straight away,
 * since a setup link can hold a password or recovery key. Never logged.
 */
object SecretClipboard {

    /** The clipboard's text, or null when it holds none; the clipboard is cleared either way. */
    fun take(context: Context): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        val text = clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                clipboard.clearPrimaryClip()
            } else {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }
        return text?.takeIf { it.isNotBlank() }
    }
}

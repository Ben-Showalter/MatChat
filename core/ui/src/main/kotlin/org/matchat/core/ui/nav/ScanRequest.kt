package org.matchat.core.ui.nav

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import org.matchat.core.model.setup.ScanKind

/**
 * How a scanned QR code (S31, docs/adr/0010) gets back to the screen that
 * asked for it: the scan screen accepts only a QR code of the asked-for
 * [ScanKind], then hands its text to the caller through the Fragment Result
 * API and closes. The text
 * lives only in memory — the caller uses it at once and keeps no copy, and it
 * is never logged (it can hold a password or recovery key).
 */
object ScanRequest {

    private const val KEY_TEXT = "text"

    private fun requestKey(kind: ScanKind) = "matchat.scan.${kind.name}"

    /** Called by the scan screen with the accepted text. */
    fun deliver(fragment: Fragment, kind: ScanKind, text: String) {
        fragment.parentFragmentManager.setFragmentResult(requestKey(kind), Bundle().apply { putString(KEY_TEXT, text) })
    }

    /** A caller listens for [kind] while its view exists; [onText] gets the scanned text. */
    fun listen(fragment: Fragment, owner: LifecycleOwner, kind: ScanKind, onText: (String) -> Unit) {
        fragment.parentFragmentManager.setFragmentResultListener(requestKey(kind), owner) { _, bundle ->
            bundle.getString(KEY_TEXT)?.let(onText)
        }
    }
}

package org.matchat.feature.settings

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import dagger.hilt.android.AndroidEntryPoint
import org.matchat.core.ui.focus.FocusEngine
import org.matchat.core.ui.softkey.SoftkeyFragment

/** S14 Help — key hints. LEFT is blank; the list is the whole screen. Which
 *  physical key does what (help_softkeys/help_back) depends on
 *  Settings > Advanced > "Swap Left/Right keys" (docs/adr/0007) — read once
 *  here at bind time, same as [SoftkeyFragment] itself does when it renders
 *  the softkey bar, since a fresh Fragment instance is created each time
 *  this screen is (re)opened. */
@AndroidEntryPoint
class HelpFragment : SoftkeyFragment() {

    override val contentLayoutId: Int = R.layout.fragment_help
    override val leftLabel: CharSequence get() = getString(org.matchat.core.ui.R.string.softkey_blank)
    override val centerLabel: CharSequence get() = getString(org.matchat.core.ui.R.string.softkey_select)

    override fun onContentViewCreated(content: View) {
        setTitle(getString(R.string.help_title))
        val swapped = userPreferences.softkeysSwapped.value
        content.findViewById<TextView>(R.id.help_softkeys).setText(
            if (swapped) R.string.help_softkeys_swapped else R.string.help_softkeys,
        )
        content.findViewById<TextView>(R.id.help_back).setText(
            if (swapped) R.string.help_back_swapped else R.string.help_back,
        )
        val list = (content as ViewGroup).getChildAt(0) as ViewGroup
        FocusEngine.requestInitialFocus(list.getChildAt(0))
    }
}

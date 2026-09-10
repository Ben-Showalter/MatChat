package org.matchat.feature.newchat

import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.matchat.core.ui.focus.FocusEngine
import org.matchat.core.ui.nav.Navigator
import org.matchat.core.ui.softkey.SoftkeyFragment
import org.matchat.core.ui.theme.themeColor
import org.matchat.core.ui.theme.themeDimenPx
import org.matchat.feature.newchat.databinding.FragmentNewChatBinding
import org.matchat.core.ui.R as UiR

/** S20 New message. LEFT is blank; each row is a focus stop. */
@AndroidEntryPoint
class NewChatFragment : SoftkeyFragment() {

    override val contentLayoutId: Int = R.layout.fragment_new_chat
    override val leftLabel: CharSequence get() = getString(org.matchat.core.ui.R.string.softkey_blank)
    override val centerLabel: CharSequence get() = getString(org.matchat.core.ui.R.string.softkey_select)

    private val viewModel: NewChatViewModel by viewModels()
    private var binding: FragmentNewChatBinding? = null
    private val navigator: Navigator get() = requireActivity() as Navigator

    override fun onContentViewCreated(content: View) {
        val b = FragmentNewChatBinding.bind(content)
        binding = b
        setTitle(getString(R.string.newchat_title))

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch { viewModel.navEvents.collect(::navigate) }
            }
        }
    }

    private fun render(state: NewChatState) {
        val container = binding?.newchatContainer ?: return
        container.removeAllViews()

        if (state.contacts.isNotEmpty()) {
            container.addView(header(getString(R.string.newchat_section_contacts)))
            state.contacts.forEach { container.addView(contactRow(it)) }
        }
        if (state.recents.isNotEmpty()) {
            container.addView(header(getString(R.string.newchat_section_recent)))
            state.recents.forEach { container.addView(contactRow(it)) }
        }
        if (!state.isLoading && !state.hasSavedContacts) {
            container.addView(header(getString(R.string.newchat_empty)))
        }
        val typeRow = actionRow(getString(R.string.newchat_type_address)) {
            viewModel.onAction(NewChatAction.TypeAnAddress)
        }
        container.addView(typeRow)

        if (container.findFocus() == null) {
            FocusEngine.requestInitialFocus(container.getChildAt(0)?.takeIf { it.isFocusable } ?: typeRow)
        }
    }

    private fun navigate(nav: NewChatNav) {
        when (nav) {
            is NewChatNav.OpenRoom -> navigator.toRoom(nav.roomId)
            NewChatNav.TypeAddress -> navigator.toTypeAddress()
        }
    }

    private fun header(text: String): TextView =
        TextView(requireContext()).apply {
            this.text = text.uppercase()
            setTextSize(TypedValue.COMPLEX_UNIT_PX, requireContext().themeDimenPx(UiR.attr.textSizeMeta))
            setTextColor(requireContext().themeColor(UiR.attr.colorTextSecondary))
            isFocusable = false
            val pad = resources.getDimensionPixelSize(UiR.dimen.content_pad)
            setPadding(0, pad, 0, 2)
        }

    private fun contactRow(row: ContactRow): View =
        actionRow(row.primary, row.secondary) { viewModel.onAction(NewChatAction.Select(row.address)) }

    private fun actionRow(primary: String, secondary: String = "", onClick: () -> Unit): View {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            isFocusable = true
            isFocusableInTouchMode = false
            minimumHeight = resources.getDimensionPixelSize(UiR.dimen.row_min_height_compact)
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(UiR.drawable.focus_selector)
            setOnClickListener { onClick() }
        }
        layout.addView(
            TextView(requireContext()).apply {
                text = primary
                setTextSize(TypedValue.COMPLEX_UNIT_PX, requireContext().themeDimenPx(UiR.attr.textSizeBody))
                isDuplicateParentStateEnabled = true // inherit row focus → text flips
                setTextColor(requireContext().themeColor(UiR.attr.colorTextOnFocus))
            },
        )
        if (secondary.isNotEmpty()) {
            layout.addView(
                TextView(requireContext()).apply {
                    text = secondary
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, requireContext().themeDimenPx(UiR.attr.textSizeMeta))
                    isDuplicateParentStateEnabled = true
                    setTextColor(requireContext().themeColor(UiR.attr.colorTextMetaOnFocus))
                },
            )
        }
        return layout
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}

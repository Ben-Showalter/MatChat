package org.matchat.feature.settings

import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.matchat.core.ui.focus.FocusEngine
import org.matchat.core.ui.softkey.SoftkeyFragment
import org.matchat.feature.settings.databinding.FragmentAdvancedBinding

/** Settings > Advanced (Phase 6, UI improvement plan; docs/adr/0007): a
 *  narrow, explicit exception to "LEFT=Options/RIGHT=Back, always" for a
 *  device whose hardware softkeys are physically reversed. One focusable
 *  toggle row — CENTER (or a tap) selects it; render() is the only place
 *  that decides its checkmark (AGENTS.md §3), same shape as ThemeFragment. */
@AndroidEntryPoint
class AdvancedFragment : SoftkeyFragment() {

    override val contentLayoutId: Int = R.layout.fragment_advanced
    override val leftLabel: CharSequence get() = getString(org.matchat.core.ui.R.string.softkey_blank)
    override val centerLabel: CharSequence get() = getString(org.matchat.core.ui.R.string.softkey_select)

    private val viewModel: AdvancedViewModel by viewModels()
    private var binding: FragmentAdvancedBinding? = null

    override fun onContentViewCreated(content: View) {
        val b = FragmentAdvancedBinding.bind(content)
        binding = b
        setTitle(getString(R.string.advanced_title))

        b.advancedSwapSoftkeys.setOnClickListener {
            viewModel.onAction(AdvancedAction.ToggleSoftkeysSwapped)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
        FocusEngine.requestInitialFocus(b.advancedSwapSoftkeys)
    }

    private fun render(state: AdvancedState) {
        val b = binding ?: return
        val label = getString(R.string.advanced_swap_softkeys)
        b.advancedSwapSoftkeys.text =
            if (state.softkeysSwapped) getString(R.string.theme_row_selected_format, label) else label
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}

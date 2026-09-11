package org.matchat.feature.settings

import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.matchat.core.ui.focus.FocusEngine
import org.matchat.core.ui.prefs.TextSizePreference
import org.matchat.core.ui.softkey.SoftkeyFragment
import org.matchat.feature.settings.databinding.FragmentTextSizeBinding

/** Settings > Text size (UX-SPEC §S16): Normal (the default set) / Small /
 *  Large, three inline focusable rows — same shape as ThemeFragment's
 *  Appearance section (few enough states that a MenuSheet picker isn't
 *  warranted the way Accent color's 16 choices need one). Also cycled from
 *  any screen by holding * (MainActivity). */
@AndroidEntryPoint
class TextSizeFragment : SoftkeyFragment() {

    override val contentLayoutId: Int = R.layout.fragment_text_size
    override val leftLabel: CharSequence get() = getString(org.matchat.core.ui.R.string.softkey_blank)
    override val centerLabel: CharSequence get() = getString(org.matchat.core.ui.R.string.softkey_select)

    private val viewModel: TextSizeViewModel by viewModels()
    private var binding: FragmentTextSizeBinding? = null

    override fun onContentViewCreated(content: View) {
        val b = FragmentTextSizeBinding.bind(content)
        binding = b
        setTitle(getString(R.string.settings_text_size))

        b.textSizeNormal.setOnClickListener { viewModel.onAction(TextSizeAction.SelectNormal) }
        b.textSizeSmall.setOnClickListener { viewModel.onAction(TextSizeAction.SelectSmall) }
        b.textSizeLarge.setOnClickListener { viewModel.onAction(TextSizeAction.SelectLarge) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
        FocusEngine.requestInitialFocus(b.textSizeNormal)
    }

    private fun render(state: TextSizeState) {
        val b = binding ?: return
        b.textSizeNormal.text = labelFor(R.string.text_size_normal, state.size == TextSizePreference.NORMAL)
        b.textSizeSmall.text = labelFor(R.string.text_size_small, state.size == TextSizePreference.SMALL)
        b.textSizeLarge.text = labelFor(R.string.text_size_large, state.size == TextSizePreference.LARGE)
    }

    private fun labelFor(labelRes: Int, selected: Boolean): CharSequence {
        val label = getString(labelRes)
        return if (selected) getString(R.string.theme_row_selected_format, label) else label
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}

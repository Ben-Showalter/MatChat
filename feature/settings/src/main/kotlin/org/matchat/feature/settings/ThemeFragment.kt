package org.matchat.feature.settings

import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.matchat.core.ui.focus.FocusEngine
import org.matchat.core.ui.prefs.AccentColor
import org.matchat.core.ui.prefs.ThemeMode
import org.matchat.core.ui.softkey.SoftkeyFragment
import org.matchat.feature.settings.databinding.FragmentThemeBinding

/** S24 Theme: light/dark, then a list of accent colors. Both lists are plain
 *  focusable rows — CENTER (or a tap) selects one; render() is the only
 *  place that decides which row carries the checkmark (AGENTS.md §3). */
@AndroidEntryPoint
class ThemeFragment : SoftkeyFragment() {

    override val contentLayoutId: Int = R.layout.fragment_theme
    override val leftLabel: CharSequence get() = getString(org.matchat.core.ui.R.string.softkey_blank)
    override val centerLabel: CharSequence get() = getString(org.matchat.core.ui.R.string.softkey_select)

    private val viewModel: ThemeViewModel by viewModels()
    private var binding: FragmentThemeBinding? = null

    override fun onContentViewCreated(content: View) {
        val b = FragmentThemeBinding.bind(content)
        binding = b
        setTitle(getString(R.string.theme_title))

        b.themeLight.setOnClickListener { viewModel.onAction(ThemeAction.SelectLight) }
        b.themeDark.setOnClickListener { viewModel.onAction(ThemeAction.SelectDark) }
        b.themeAccentGreen.setOnClickListener {
            viewModel.onAction(ThemeAction.SelectAccent(AccentColor.GREEN))
        }
        b.themeAccentAmber.setOnClickListener {
            viewModel.onAction(ThemeAction.SelectAccent(AccentColor.AMBER))
        }
        b.themeAccentBlue.setOnClickListener {
            viewModel.onAction(ThemeAction.SelectAccent(AccentColor.BLUE))
        }
        b.themeAccentPlum.setOnClickListener {
            viewModel.onAction(ThemeAction.SelectAccent(AccentColor.PLUM))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
        FocusEngine.requestInitialFocus(b.themeLight)
    }

    private fun render(state: ThemeState) {
        val b = binding ?: return
        b.themeLight.text = labelFor(R.string.theme_light, state.mode == ThemeMode.LIGHT)
        b.themeDark.text = labelFor(R.string.theme_dark, state.mode == ThemeMode.DARK)
        b.themeAccentGreen.text = labelFor(R.string.theme_accent_green, state.accent == AccentColor.GREEN)
        b.themeAccentAmber.text = labelFor(R.string.theme_accent_amber, state.accent == AccentColor.AMBER)
        b.themeAccentBlue.text = labelFor(R.string.theme_accent_blue, state.accent == AccentColor.BLUE)
        b.themeAccentPlum.text = labelFor(R.string.theme_accent_plum, state.accent == AccentColor.PLUM)
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

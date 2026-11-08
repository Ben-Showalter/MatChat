package org.matchat.feature.onboarding

import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.matchat.core.model.setup.ScanKind
import org.matchat.core.ui.clipboard.SecretClipboard
import org.matchat.core.ui.focus.FocusEngine
import org.matchat.core.ui.nav.Navigator
import org.matchat.core.ui.nav.ScanRequest
import org.matchat.core.ui.softkey.SoftkeyFragment
import org.matchat.feature.onboarding.databinding.FragmentSetupCodeBinding

/** S4 "Sign in with setup code" (UX-SPEC §S4): Scan QR setup code · Paste
 *  setup link · No, help me set it up. LEFT is blank — the rows are the choices. */
@AndroidEntryPoint
class SetupCodeFragment : SoftkeyFragment() {

    override val contentLayoutId: Int = R.layout.fragment_setup_code
    override val leftLabel: CharSequence get() = getString(org.matchat.core.ui.R.string.softkey_blank)
    override val centerLabel: CharSequence get() = getString(org.matchat.core.ui.R.string.softkey_select)

    private val viewModel: SetupCodeViewModel by viewModels()
    private val navigator: Navigator get() = requireActivity() as Navigator
    private var binding: FragmentSetupCodeBinding? = null

    override fun onContentViewCreated(content: View) {
        val b = FragmentSetupCodeBinding.bind(content)
        binding = b
        setTitle(getString(R.string.setup_code_title))
        b.setupCodeScan.setOnClickListener { viewModel.onAction(SetupCodeAction.Scan) }
        b.setupCodePaste.setOnClickListener {
            viewModel.onAction(SetupCodeAction.Paste(SecretClipboard.take(requireContext())))
        }
        b.setupCodeHelp.setOnClickListener { viewModel.onAction(SetupCodeAction.Help) }
        ScanRequest.listen(this, viewLifecycleOwner, ScanKind.SETUP_CODE) {
            viewModel.onAction(SetupCodeAction.Scanned(it))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch { viewModel.navEvents.collect(::navigate) }
            }
        }
        FocusEngine.requestInitialFocus(b.setupCodeScan)
    }

    private fun render(state: SetupCodeState) {
        val b = binding ?: return
        b.setupCodeProblem.isVisible = state.problem != null
        b.setupCodeProblem.text = when (state.problem) {
            SetupProblem.NOT_A_SETUP_CODE -> getString(R.string.setup_not_a_setup_code)
            SetupProblem.TOO_NEW -> getString(R.string.setup_too_new)
            SetupProblem.NOTHING_TO_PASTE -> getString(R.string.setup_nothing_to_paste)
            null -> ""
        }
    }

    private fun navigate(nav: SetupCodeNav) {
        when (nav) {
            SetupCodeNav.OpenScanner -> navigator.toScan(ScanKind.SETUP_CODE)
            SetupCodeNav.OpenSignIn -> navigator.toSignIn()
            SetupCodeNav.OpenHelp -> navigator.toSetupHelp()
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}

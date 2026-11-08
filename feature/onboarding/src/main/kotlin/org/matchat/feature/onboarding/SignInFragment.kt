package org.matchat.feature.onboarding

import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.matchat.core.model.ErrorText
import org.matchat.core.model.setup.ScanKind
import org.matchat.core.ui.clipboard.SecretClipboard
import org.matchat.core.ui.focus.FocusEngine
import org.matchat.core.ui.menu.MenuItem
import org.matchat.core.ui.menu.MenuSheet
import org.matchat.core.ui.nav.Navigator
import org.matchat.core.ui.nav.ScanRequest
import org.matchat.core.ui.softkey.SoftkeyFragment
import org.matchat.feature.onboarding.databinding.FragmentSignInBinding

/** S3 Sign in (password), with S32 "Setup code found" in place of the form
 *  while a scanned or pasted setup code waits to be used. */
@AndroidEntryPoint
class SignInFragment : SoftkeyFragment() {

    override val contentLayoutId: Int = R.layout.fragment_sign_in
    override val leftLabel: CharSequence get() = getString(org.matchat.core.ui.R.string.softkey_options)
    override val centerLabel: CharSequence get() = getString(org.matchat.core.ui.R.string.softkey_select)

    private val viewModel: SignInViewModel by viewModels()
    private var binding: FragmentSignInBinding? = null
    private val navigator: Navigator get() = requireActivity() as Navigator

    // The last FormFill applied, so render copies a setup code into the form once.
    private var appliedFill = 0

    override fun onContentViewCreated(content: View) {
        val b = FragmentSignInBinding.bind(content)
        binding = b
        setTitle(getString(R.string.signin_title))
        b.togglePassword.setOnClickListener {
            viewModel.onAction(SignInAction.ToggleShowPassword)
        }
        b.signInButton.setOnClickListener { submit() }
        b.moreOptions.setOnClickListener { onOptions() }
        b.setupFoundSignIn.setOnClickListener { viewModel.onAction(SignInAction.ConfirmSetupCode) }
        b.setupFoundFill.setOnClickListener { viewModel.onAction(SignInAction.FillFromSetupCode) }
        b.setupFoundCancel.setOnClickListener { viewModel.onAction(SignInAction.CancelSetupCode) }
        ScanRequest.listen(this, viewLifecycleOwner, ScanKind.SETUP_CODE) {
            viewModel.onAction(SignInAction.SetupCodeReceived(it))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch { viewModel.navEvents.collect(::navigate) }
            }
        }
        FocusEngine.requestInitialFocus(b.username)
    }

    private fun submit() {
        val b = binding ?: return
        viewModel.onAction(
            SignInAction.Submit(
                username = b.username.text.toString(),
                password = b.password.text.toString(),
                homeserver = b.homeserverField.text.toString(),
            ),
        )
    }

    private fun navigate(nav: SignInNav) {
        when (nav) {
            SignInNav.Success -> navigator.toRoomListRoot()
            SignInNav.RecoveryKeyFailed -> {
                navigator.toRoomListRoot()
                navigator.toVerification()
                Toast.makeText(requireContext(), R.string.setup_recovery_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun render(state: SignInState) {
        val b = binding ?: return
        renderSetupCode(b, state)
        // Pre-fill the homeserver once; when pinned it is read-only (grey, locked).
        if (b.homeserverField.text.isNullOrEmpty()) {
            b.homeserverField.setText(state.homeserver)
        }
        b.homeserverField.isEnabled = !state.homeserverPinned
        b.homeserverField.isFocusable = !state.homeserverPinned
        // The field is a textVisiblePassword so the device keypad lets digits
        // through (see fragment_sign_in.xml); masking is a transformation driven
        // by state, so Show/Hide stays a pure function of showPassword.
        b.password.transformationMethod = if (state.showPassword) {
            HideReturnsTransformationMethod.getInstance()
        } else {
            PasswordTransformationMethod.getInstance()
        }
        b.password.setSelection(b.password.text?.length ?: 0)
        b.togglePassword.setText(
            if (state.showPassword) R.string.signin_hide_password else R.string.signin_show_password,
        )
        b.signInButton.isEnabled = !state.isSubmitting
        b.error.isVisible = state.error != null || state.setupProblem != null || state.recoveryKeyPending
        b.error.text = when {
            state.error != null -> messageFor(state.error)
            state.setupProblem != null -> getString(setupProblemText(state.setupProblem))
            state.recoveryKeyPending -> getString(R.string.setup_recovery_pending)
            else -> ""
        }
    }

    /** S32 in place of the form while a setup code is shown, and a one-time
     *  copy of a code's server/username into the form. */
    private fun renderSetupCode(b: FragmentSignInBinding, state: SignInState) {
        val found = state.found
        val wasShowing = b.setupFound.isVisible
        b.signInForm.isVisible = found == null
        b.setupFound.isVisible = found != null
        if (found != null) {
            val notIncluded = getString(R.string.setup_found_not_included)
            b.setupFoundDetails.text = getString(
                R.string.setup_found_details,
                found.server,
                found.user ?: notIncluded,
                if (found.hasPassword) getString(R.string.setup_found_hidden) else notIncluded,
                if (found.hasRecoveryKey) getString(R.string.setup_found_included) else notIncluded,
            )
            b.setupFoundProblem.isVisible = !found.canSignIn
            b.setupFoundProblem.text = when {
                found.blockedServer != null ->
                    getString(R.string.setup_found_blocked_server, found.blockedServer, found.server)
                else -> getString(R.string.setup_found_incomplete)
            }
            b.setupFoundSignIn.isVisible = found.canSignIn
            b.setupFoundFill.isVisible = !found.isComplete && found.blockedServer == null
            if (!wasShowing) {
                FocusEngine.requestInitialFocus(if (found.canSignIn) b.setupFoundSignIn else b.setupFoundCancel)
            }
        } else if (wasShowing) {
            FocusEngine.requestInitialFocus(b.username)
        }
        val fill = state.fill
        if (fill != null && fill.id != appliedFill) {
            appliedFill = fill.id
            fill.server?.let { if (!state.homeserverPinned) b.homeserverField.setText(it) }
            fill.user?.let { b.username.setText(it) }
            FocusEngine.requestInitialFocus(if (fill.user == null) b.username else b.password)
        }
    }

    private fun setupProblemText(problem: SetupProblem): Int = when (problem) {
        SetupProblem.NOT_A_SETUP_CODE -> R.string.setup_not_a_setup_code
        SetupProblem.TOO_NEW -> R.string.setup_too_new
        SetupProblem.NOTHING_TO_PASTE -> R.string.setup_nothing_to_paste
    }

    private fun messageFor(error: ErrorText): CharSequence = when (error.key) {
        ErrorText.Key.NETWORK -> getString(R.string.signin_error_network)
        ErrorText.Key.BAD_CREDENTIALS -> getString(R.string.signin_error_credentials)
        else -> getString(R.string.signin_error_generic, error.args.firstOrNull().orEmpty())
    }

    /** The Options softkey, or the "More options" row — the route while a
     *  text box is focused, since the T9 keyboard takes the softkeys then
     *  (UX-SPEC S3). Before sign-in: check for a newer app version, or Help. */
    override fun onOptions(): Boolean {
        val items = listOf(
            MenuItem(OPT_SCAN, getString(R.string.setup_scan)),
            MenuItem(OPT_PASTE, getString(R.string.setup_paste)),
            MenuItem(OPT_UPDATE, getString(R.string.signin_opt_update)),
            MenuItem(OPT_HELP, getString(R.string.signin_opt_help)),
        )
        MenuSheet.show(requireContext(), items) { selected ->
            when (selected.id) {
                OPT_SCAN -> navigator.toScan(ScanKind.SETUP_CODE)
                OPT_PASTE -> viewModel.onAction(SignInAction.SetupCodeReceived(SecretClipboard.take(requireContext())))
                OPT_UPDATE -> navigator.toUpdate()
                OPT_HELP -> navigator.toHelp()
            }
        }
        return true
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private companion object {
        const val OPT_SCAN = "scan"
        const val OPT_PASTE = "paste"
        const val OPT_UPDATE = "update"
        const val OPT_HELP = "help"
    }
}

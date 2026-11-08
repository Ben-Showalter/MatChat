package org.matchat.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.matchat.core.matrix.MatrixAuth
import org.matchat.core.matrix.MatrixSession
import org.matchat.core.model.ErrorText
import org.matchat.core.model.setup.SetupCode
import org.matchat.core.model.setup.SetupCodeParser
import org.matchat.core.policy.PolicyProvider
import javax.inject.Inject

/**
 * S3 Sign in, plus S32 "Setup code found": a scanned or pasted setup code is
 * shown first (password and recovery key hidden) and used only on "Sign in"
 * (Flip-DumbPhoneGuide "Setup codes"). A recovery key from the code is applied
 * right after sign-in, so encrypted history is readable at once. The code and
 * key are held only here, in memory, and dropped once used; never logged.
 */
@HiltViewModel
class SignInViewModel @Inject constructor(
    private val auth: MatrixAuth,
    private val session: MatrixSession,
    policyProvider: PolicyProvider,
    pendingSetupCode: PendingSetupCode,
) : ViewModel() {

    private val pinned = policyProvider.policy.value.pinnedHomeserver

    private val _state = MutableStateFlow(
        SignInState(
            homeserver = pinned ?: DEFAULT_HOMESERVER,
            homeserverPinned = pinned != null,
        ),
    )
    val state: StateFlow<SignInState> = _state.asStateFlow()

    private val navChannel = Channel<SignInNav>(Channel.BUFFERED)
    val navEvents: Flow<SignInNav> = navChannel.receiveAsFlow()

    // The setup code on screen (S32), and a recovery key waiting for sign-in.
    // A confirmed code is kept until sign-in succeeds, so a failure goes back
    // to S32 with the error and "Sign in" retries without scanning again.
    private var code: SetupCode? = null
    private var confirmed: SetupFound? = null
    private var pendingRecoveryKey: String? = null
    private var fillCount = 0

    init {
        // A code chosen on S4 ("Sign in with setup code") arrives here.
        pendingSetupCode.take()?.let { onSetupCode(it) }
    }

    fun onAction(action: SignInAction) {
        when (action) {
            is SignInAction.Submit ->
                submit(action.username.trim(), action.password, action.homeserver.trim())
            SignInAction.DismissError -> _state.update { it.copy(error = null, setupProblem = null) }
            SignInAction.ToggleShowPassword ->
                _state.update { it.copy(showPassword = !it.showPassword) }
            is SignInAction.SetupCodeReceived -> onSetupCode(action.text)
            SignInAction.ConfirmSetupCode -> confirmSetupCode()
            SignInAction.FillFromSetupCode -> fillFromSetupCode()
            SignInAction.CancelSetupCode -> {
                code = null
                confirmed = null
                pendingRecoveryKey = null
                _state.update { it.copy(found = null, recoveryKeyPending = false) }
            }
        }
    }

    private fun onSetupCode(text: String?) {
        if (text.isNullOrBlank()) {
            _state.update { it.copy(setupProblem = SetupProblem.NOTHING_TO_PASTE) }
            return
        }
        when (val parsed = SetupCodeParser.parse(text)) {
            is SetupCodeParser.Result.Ok -> show(parsed.code)
            SetupCodeParser.Result.TooNew -> _state.update { it.copy(setupProblem = SetupProblem.TOO_NEW) }
            SetupCodeParser.Result.NotASetupCode ->
                _state.update { it.copy(setupProblem = SetupProblem.NOT_A_SETUP_CODE) }
        }
    }

    private fun show(setup: SetupCode) {
        code = setup
        val codeServer = setup.server
        val blocked = codeServer.takeIf { pinned != null && it != null && host(it) != host(pinned) }
        _state.update {
            it.copy(
                found = SetupFound(
                    server = if (pinned != null) pinned else codeServer ?: it.homeserver,
                    user = setup.user,
                    hasPassword = setup.password != null,
                    hasRecoveryKey = setup.recoveryKey != null,
                    blockedServer = blocked,
                ),
                setupProblem = null,
                error = null,
            )
        }
    }

    private fun confirmSetupCode() {
        val setup = code ?: return
        val found = _state.value.found ?: return
        if (!found.canSignIn || _state.value.isSubmitting) return
        pendingRecoveryKey = setup.recoveryKey
        confirmed = found
        submit(setup.user.orEmpty(), setup.password.orEmpty(), found.server)
    }

    /** Copy what the code has into the form; keep its recovery key for after sign-in. */
    private fun fillFromSetupCode() {
        val setup = code ?: return
        val found = _state.value.found ?: return
        if (found.blockedServer != null) return
        pendingRecoveryKey = setup.recoveryKey
        code = null
        confirmed = null
        fillCount++
        _state.update {
            it.copy(
                found = null,
                fill = FormFill(fillCount, if (pinned == null) setup.server else null, setup.user),
                recoveryKeyPending = pendingRecoveryKey != null,
            )
        }
    }

    private fun submit(username: String, password: String, homeserver: String) {
        val server = if (state.value.homeserverPinned) state.value.homeserver else homeserver
        if (username.isEmpty() || password.isEmpty() || server.isEmpty()) {
            _state.update { it.copy(error = ErrorText(ErrorText.Key.BAD_CREDENTIALS)) }
            return
        }
        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val result = auth.signIn(username, password, server)
            result.fold(
                onSuccess = { navChannel.send(afterSignIn()) },
                onFailure = { cause ->
                    // From S32: stay there with the error, so "Sign in" retries.
                    _state.update {
                        it.copy(isSubmitting = false, error = mapError(cause), found = confirmed ?: it.found)
                    }
                },
            )
        }
    }

    /** Applies a setup code's recovery key, if there was one, then says where to go. */
    private suspend fun afterSignIn(): SignInNav {
        val key = pendingRecoveryKey
        pendingRecoveryKey = null
        code = null
        confirmed = null
        val recovered = key == null || session.recoverEncryption(key).isSuccess
        _state.update { it.copy(isSubmitting = false, recoveryKeyPending = false, found = null) }
        return if (recovered) SignInNav.Success else SignInNav.RecoveryKeyFailed
    }

    private fun mapError(cause: Throwable): ErrorText {
        val message = cause.message.orEmpty()
        return when {
            cause is java.io.IOException -> ErrorText(ErrorText.Key.NETWORK, retryable = true)
            // The phone refused MatChat its own data folder (EACCES): no
            // network or password problem — say how to fix it on the phone.
            listOf("os error 13", "EACCES").any { message.contains(it, ignoreCase = true) } ->
                ErrorText(ErrorText.Key.STORAGE_UNAVAILABLE)
            // Clear auth rejections read as a wrong username/password.
            listOf("forbidden", "unauthorized", "password", "m_forbidden", "invalid")
                .any { message.contains(it, ignoreCase = true) } ->
                ErrorText(ErrorText.Key.BAD_CREDENTIALS)
            // Anything else (bad server, TLS/cert, unreachable) is shown verbatim
            // so the real cause is visible (AGENTS.md — no silent failure states).
            else -> ErrorText(ErrorText.Key.SIGN_IN_FAILED, args = listOf(message.take(MAX_MSG)))
        }
    }

    /** "https://Matrix.Example.org/" and "matrix.example.org" are the same server. */
    private fun host(server: String): String =
        server.trim().lowercase().removePrefix("https://").removePrefix("http://").trimEnd('/')

    private companion object {
        // Homeserver assumed from the sign-in mockup (PLAN.md §12 open question 1).
        const val DEFAULT_HOMESERVER = "chats.carpathianserver.org"
        const val MAX_MSG = 200
    }
}

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
import kotlinx.coroutines.launch
import org.matchat.core.model.setup.SetupCodeParser
import javax.inject.Inject

/**
 * S4 "Do you have a QR setup code ready?" (Flip-DumbPhoneGuide "Setup codes"):
 * asked before the camera opens, so nobody lands in a viewfinder with nothing
 * to point it at. A usable code goes on to S3, which shows it (S32) before
 * anything is used.
 */
@HiltViewModel
class SetupCodeViewModel @Inject constructor(
    private val pending: PendingSetupCode,
) : ViewModel() {

    private val _state = MutableStateFlow(SetupCodeState())
    val state: StateFlow<SetupCodeState> = _state.asStateFlow()

    private val navChannel = Channel<SetupCodeNav>(Channel.BUFFERED)
    val navEvents: Flow<SetupCodeNav> = navChannel.receiveAsFlow()

    fun onAction(action: SetupCodeAction) {
        when (action) {
            SetupCodeAction.Scan -> send(SetupCodeNav.OpenScanner)
            SetupCodeAction.Help -> send(SetupCodeNav.OpenHelp)
            is SetupCodeAction.Scanned -> accept(action.text)
            is SetupCodeAction.Paste -> accept(action.text)
        }
    }

    private fun accept(text: String?) {
        val problem = when {
            text.isNullOrBlank() -> SetupProblem.NOTHING_TO_PASTE
            else -> when (SetupCodeParser.parse(text)) {
                is SetupCodeParser.Result.Ok -> null
                SetupCodeParser.Result.TooNew -> SetupProblem.TOO_NEW
                SetupCodeParser.Result.NotASetupCode -> SetupProblem.NOT_A_SETUP_CODE
            }
        }
        _state.value = SetupCodeState(problem)
        if (problem == null && text != null) {
            pending.put(text)
            send(SetupCodeNav.OpenSignIn)
        }
    }

    private fun send(nav: SetupCodeNav) {
        viewModelScope.launch { navChannel.send(nav) }
    }
}

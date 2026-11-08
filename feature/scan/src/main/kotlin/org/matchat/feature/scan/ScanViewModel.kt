package org.matchat.feature.scan

import androidx.lifecycle.SavedStateHandle
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
import org.matchat.core.model.setup.ScanKind
import org.matchat.core.model.setup.ScannedValue
import org.matchat.core.model.setup.SetupCodeParser
import javax.inject.Inject

/**
 * S31 Scan: decides whether a decoded QR code is what the caller asked for
 * ([ScanKind]). A match closes the screen with the value; anything else gets a
 * plain message and scanning goes on. Never logs or keeps the decoded text —
 * only a hash of the last rejected one, so the same wrong code held in front of
 * the camera doesn't re-announce itself every frame.
 */
@HiltViewModel
class ScanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val kind: ScanKind =
        savedStateHandle.get<String>("kind")?.let { name -> ScanKind.entries.firstOrNull { it.name == name } }
            ?: ScanKind.SETUP_CODE

    private val _state = MutableStateFlow(ScanState())
    val state: StateFlow<ScanState> = _state.asStateFlow()

    private val navChannel = Channel<ScanNav>(Channel.BUFFERED)
    val navEvents: Flow<ScanNav> = navChannel.receiveAsFlow()

    private var lastRejected: Int? = null
    private var accepted = false

    fun onAction(action: ScanAction) {
        when (action) {
            is ScanAction.Decoded -> onDecoded(action.text)
            ScanAction.ScanAgain -> scanAgain()
            ScanAction.CameraStarted -> _state.value = ScanState(ScanStatus.POINT)
            ScanAction.CameraFailed -> _state.value = ScanState(ScanStatus.NO_CAMERA)
            ScanAction.PermissionDenied -> _state.value = ScanState(ScanStatus.NO_PERMISSION)
        }
    }

    private fun onDecoded(text: String) {
        if (accepted) return
        val value = accept(text)
        if (value != null) {
            accepted = true
            viewModelScope.launch { navChannel.send(ScanNav.Accepted(value)) }
            return
        }
        val hash = text.hashCode()
        if (hash == lastRejected) return
        lastRejected = hash
        _state.value = ScanState(rejection(text))
    }

    /** The value to hand back, or null if [text] isn't what was asked for. */
    private fun accept(text: String): String? = when (kind) {
        ScanKind.SETUP_CODE -> text.takeIf { SetupCodeParser.parse(it) is SetupCodeParser.Result.Ok }
        ScanKind.RECOVERY_KEY -> ScannedValue.recoveryKey(text)
        ScanKind.ADDRESS -> ScannedValue.address(text)
    }

    private fun rejection(text: String): ScanStatus = when {
        SetupCodeParser.parse(text) == SetupCodeParser.Result.TooNew -> ScanStatus.TOO_NEW
        kind == ScanKind.RECOVERY_KEY -> ScanStatus.NOT_A_RECOVERY_KEY
        kind == ScanKind.ADDRESS -> ScanStatus.NOT_AN_ADDRESS
        else -> ScanStatus.NOT_A_SETUP_CODE
    }

    /** "Scan again": show the next rejection even if it's the same code. */
    private fun scanAgain() {
        lastRejected = null
        if (_state.value.canRetry) {
            _state.value = ScanState(ScanStatus.STARTING)
            viewModelScope.launch { navChannel.send(ScanNav.RestartCamera) }
        } else {
            _state.value = ScanState(ScanStatus.POINT)
        }
    }
}

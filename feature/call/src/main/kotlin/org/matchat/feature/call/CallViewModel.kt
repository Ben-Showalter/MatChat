package org.matchat.feature.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.matchat.core.model.RoomId
import org.matchat.core.rtc.CallController
import org.matchat.core.rtc.CallSession
import javax.inject.Inject

/** Drives the call screens off the single [CallController] session. */
@HiltViewModel
class CallViewModel @Inject constructor(
    private val calls: CallController,
) : ViewModel() {

    val session: StateFlow<CallSession> =
        calls.session.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), CallSession())

    private var placed = false

    /** Start an outgoing call once (guards against re-placing on recreate). */
    fun placeOnce(roomId: RoomId, peerName: String?) {
        if (placed) return
        placed = true
        viewModelScope.launch { calls.placeCall(roomId, peerName) }
    }

    fun answer() = viewModelScope.launch { calls.answer() }
    fun hangup() = viewModelScope.launch { calls.hangup() }
    fun toggleMute() = calls.toggleMute()
    fun toggleSpeaker() = calls.toggleSpeaker()

    private companion object { const val STOP_MS = 5_000L }
}

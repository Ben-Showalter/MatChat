package org.matchat.core.rtc

import kotlinx.coroutines.flow.Flow
import org.matchat.core.model.RoomId

/**
 * The app-facing voice-call contract (docs/VOICE.md, ADR 0006). One call at a
 * time. Signalling (m.call.member membership + the ring notification) rides the
 * Matrix SDK via :core:matrix; media rides [AudioTransport]. The :feature:call
 * screens observe [session] and drive it with CALL/END.
 */
interface CallController {
    /** The single live call state (IDLE when there is no call). */
    val session: Flow<CallSession>

    /** Place a call in [roomId]: publish our m.call.member, then join media. */
    suspend fun placeCall(roomId: RoomId, peerName: String?)

    /** Answer the current incoming (RINGING) call. */
    suspend fun answer()

    /** Hang up / decline / cancel — publishes a leave and tears down media. */
    suspend fun hangup()

    fun toggleMute()
    fun toggleSpeaker()

    /** Present a ringing incoming call to the UI (e.g. from the sync service when
     *  it sees a fresh RING notification, docs/VOICE.md §5). */
    fun onIncomingCall(incoming: IncomingCall)
}

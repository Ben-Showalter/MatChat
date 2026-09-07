package org.matchat.core.rtc

import org.matchat.core.model.RoomId
import org.matchat.core.model.UserId

/** Where a call is in its lifecycle (docs/VOICE.md). */
enum class CallPhase {
    IDLE,
    /** We are placing a call and publishing our membership. */
    DIALING,
    /** An incoming call is ringing (a RING notification within its lifetime). */
    RINGING,
    /** Joining the media transport (LiveKit). */
    CONNECTING,
    /** In the call with live audio. */
    CONNECTED,
    /** The call has ended (hung up, declined, or failed). */
    ENDED,
}

/**
 * The single observable call state. v1 is SFU-trusted, never end-to-end
 * encrypted — [endToEndEncrypted] is always false and the UI must say so
 * (docs/VOICE.md §4). [audioAvailable] is false until the LiveKit transport is
 * configured and connected; the scaffold reports false so the UI is honest that
 * signalling works but media does not yet.
 */
data class CallSession(
    val phase: CallPhase = CallPhase.IDLE,
    val roomId: RoomId? = null,
    val peerName: String? = null,
    val participants: List<UserId> = emptyList(),
    val endToEndEncrypted: Boolean = false,
    val audioAvailable: Boolean = false,
    val micMuted: Boolean = false,
    val speakerOn: Boolean = false,
    /** Present when [phase] is ENDED after a failure, for the UI to explain. */
    val error: CallError? = null,
) {
    val isActive: Boolean get() = phase != CallPhase.IDLE && phase != CallPhase.ENDED
}

enum class CallError { NO_TRANSPORT, TOKEN_FAILED, SIGNALLING_FAILED, PEER_GONE }

/** An incoming call surfaced to the ring pipeline (docs/VOICE.md §5). */
data class IncomingCall(
    val roomId: RoomId,
    val callerName: String,
)

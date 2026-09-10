package org.matchat.core.rtc.internal

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.matchat.core.matrix.MatrixSession
import org.matchat.core.model.RoomId
import org.matchat.core.rtc.AudioTransport
import org.matchat.core.rtc.CallController
import org.matchat.core.rtc.CallError
import org.matchat.core.rtc.CallPhase
import org.matchat.core.rtc.CallSession
import org.matchat.core.rtc.IncomingCall
import org.matchat.core.rtc.RtcConfig
import org.matchat.core.rtc.TokenService

/**
 * MatrixRTC signalling on top of :core:matrix primitives, with media delegated to
 * [AudioTransport]. Publishing/observing m.call.member is real (spike 1);
 * [AudioTransport] is stubbed until the SFU exists, so calls reach CONNECTED with
 * audio reported unavailable rather than pretending to carry sound.
 */
@Singleton
internal class MatrixRtcCallController @Inject constructor(
    private val matrix: MatrixSession,
    private val transport: AudioTransport,
    private val tokenService: TokenService,
    private val config: RtcConfig,
) : CallController {

    private val _session = MutableStateFlow(CallSession())
    override val session: Flow<CallSession> = _session

    override suspend fun placeCall(roomId: RoomId, peerName: String?) {
        _session.value = CallSession(phase = CallPhase.DIALING, roomId = roomId, peerName = peerName)
        if (!publishMembership(roomId)) {
            end(roomId, CallError.SIGNALLING_FAILED)
            return
        }
        connectMedia(roomId, peerName)
    }

    override suspend fun answer() {
        val current = _session.value
        val roomId = current.roomId ?: return
        if (!publishMembership(roomId)) {
            end(roomId, CallError.SIGNALLING_FAILED)
            return
        }
        connectMedia(roomId, current.peerName)
    }

    override suspend fun hangup() {
        val current = _session.value
        transport.disconnect()
        current.roomId?.let { roomId ->
            val uid = matrix.ownUserId()?.value
            if (uid != null) {
                matrix.sendStateEvent(
                    roomId,
                    CallMembership.EVENT_TYPE,
                    CallMembership.stateKey(uid),
                    CallMembership.leaveContent(),
                )
            }
        }
        _session.update { it.copy(phase = CallPhase.ENDED) }
    }

    override fun toggleMute() {
        val muted = !_session.value.micMuted
        transport.setMicMuted(muted)
        _session.update { it.copy(micMuted = muted) }
    }

    override fun toggleSpeaker() {
        val on = !_session.value.speakerOn
        transport.setSpeakerOn(on)
        _session.update { it.copy(speakerOn = on) }
    }

    override fun onIncomingCall(incoming: IncomingCall) {
        // Ignore a ring while already in a call (the SFU handles multi-party; a
        // second ring for the same room is noise).
        if (_session.value.isActive) return
        _session.value = CallSession(
            phase = CallPhase.RINGING,
            roomId = incoming.roomId,
            peerName = incoming.callerName,
        )
    }

    private suspend fun publishMembership(roomId: RoomId): Boolean {
        val uid = matrix.ownUserId()?.value ?: return false
        val content = CallMembership.joinContent(uid, DEVICE_ID, roomId.value, config)
        return matrix.sendStateEvent(roomId, CallMembership.EVENT_TYPE, CallMembership.stateKey(uid), content)
            .isSuccess
    }

    private suspend fun connectMedia(roomId: RoomId, peerName: String?) {
        _session.value = CallSession(phase = CallPhase.CONNECTING, roomId = roomId, peerName = peerName)
        val audioUp = if (config.isConfigured) {
            tokenService.fetchToken(roomId.value)?.let { transport.connect(it) } ?: false
        } else {
            false
        }
        _session.value = CallSession(
            phase = CallPhase.CONNECTED,
            roomId = roomId,
            peerName = peerName,
            audioAvailable = audioUp,
            error = if (audioUp) null else CallError.NO_TRANSPORT,
        )
    }

    private fun end(roomId: RoomId, error: CallError) {
        _session.value = CallSession(phase = CallPhase.ENDED, roomId = roomId, error = error)
    }

    private companion object {
        // Device-scoped membership is a spike-2 item; a placeholder until then.
        const val DEVICE_ID = "MATCHAT"
    }
}

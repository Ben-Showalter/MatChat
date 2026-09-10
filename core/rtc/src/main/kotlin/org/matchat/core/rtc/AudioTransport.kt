package org.matchat.core.rtc

import kotlinx.coroutines.flow.Flow

/** Everything the media transport needs to join a LiveKit room. */
data class TransportConfig(
    val livekitUrl: String,
    val token: String,
)

/**
 * The media side of a call — audio to/from the LiveKit SFU. Deliberately an
 * interface: spike 2 (real two-way audio to Element) needs a running SFU +
 * lk-jwt-service, so the LiveKit-backed implementation lands later. Until then a
 * stub reports "no transport", which keeps the call UI and signalling testable
 * without a server (docs/VOICE.md §7).
 */
interface AudioTransport {
    val connected: Flow<Boolean>

    /** Join the media room. Returns false when audio is unavailable (e.g. no
     *  server configured yet, or the connection failed). */
    suspend fun connect(config: TransportConfig): Boolean

    fun disconnect()

    fun setMicMuted(muted: Boolean)

    /** Route to the loudspeaker (true) or the earpiece (false) — earpiece is the
     *  default on these phones; `*` toggles speaker (docs/VOICE.md §6). */
    fun setSpeakerOn(on: Boolean)
}

/**
 * Issues LiveKit access tokens from lk-jwt-service (MSC4195). Interface for the
 * same reason as [AudioTransport]. [RtcConfig] carries the endpoint; when it is
 * blank the fetch fails fast and the call ends with [CallError.NO_TRANSPORT].
 */
interface TokenService {
    suspend fun fetchToken(roomId: String): TransportConfig?
}

/**
 * Server endpoints for the call transport, provided by :app (like MatrixDevConfig
 * provides SDK knobs). Blank until the SFU + lk-jwt-service are deployed; the
 * controller treats blank as "audio not available yet".
 */
data class RtcConfig(
    val livekitUrl: String = "",
    val tokenEndpoint: String = "",
) {
    val isConfigured: Boolean get() = livekitUrl.isNotBlank() && tokenEndpoint.isNotBlank()
}

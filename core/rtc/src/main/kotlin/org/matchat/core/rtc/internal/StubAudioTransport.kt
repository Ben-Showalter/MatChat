package org.matchat.core.rtc.internal

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.matchat.core.rtc.AudioTransport
import org.matchat.core.rtc.TokenService
import org.matchat.core.rtc.TransportConfig
import javax.inject.Inject

/**
 * Placeholder media transport until the LiveKit Android SDK is wired against a
 * real SFU (spike 2, docs/VOICE.md §7). It never connects, so a call reaches the
 * in-call screen with audio reported unavailable — signalling is exercised, media
 * is honestly absent. Replaced by a LiveKit-backed impl once the server exists.
 */
internal class StubAudioTransport @Inject constructor() : AudioTransport {

    private val _connected = MutableStateFlow(false)
    override val connected: Flow<Boolean> = _connected

    override suspend fun connect(config: TransportConfig): Boolean {
        Log.i(TAG, "stub transport: would join ${config.livekitUrl} — LiveKit not wired yet")
        return false
    }

    override fun disconnect() { _connected.value = false }
    override fun setMicMuted(muted: Boolean) = Unit
    override fun setSpeakerOn(on: Boolean) = Unit

    private companion object { const val TAG = "StubAudioTransport" }
}

/** Placeholder token service. Real impl POSTs to lk-jwt-service /get_token. */
internal class StubTokenService @Inject constructor() : TokenService {
    override suspend fun fetchToken(roomId: String): TransportConfig? = null
}

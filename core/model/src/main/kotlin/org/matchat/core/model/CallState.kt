package org.matchat.core.model

/**
 * Whether a room currently has an active MatrixRTC call and who is in it
 * (docs/VOICE.md, ADR 0006). Derived from the SDK's parsed `m.call.member`
 * membership — MatChat never parses those state events itself; the RTC protocol
 * logic in :core:rtc builds on this and the raw-send primitives.
 */
data class CallState(
    val hasActiveCall: Boolean,
    val participantIds: List<UserId>,
) {
    companion object {
        val NONE = CallState(hasActiveCall = false, participantIds = emptyList())
    }
}

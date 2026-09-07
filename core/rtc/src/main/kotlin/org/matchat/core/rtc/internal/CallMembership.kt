package org.matchat.core.rtc.internal

import org.json.JSONArray
import org.json.JSONObject
import org.matchat.core.rtc.RtcConfig

/**
 * Builds the `m.call.member` state-event content (MSC4143) that MatrixRTC rides
 * on. This is the interop surface with Element and MUST be validated in spike 2
 * against a real Element X client — the shape below follows the MSC but is not
 * yet confirmed on the wire, so it is deliberately isolated here.
 *
 * Leaving a call is an empty content object to the same state key.
 */
internal object CallMembership {

    const val EVENT_TYPE = "m.call.member"

    /** State key for our membership. The newer MSC uses `_<userId>_<deviceId>`;
     *  we use the user id until spike 2 pins the device-scoped format. */
    fun stateKey(userId: String): String = userId

    /** Our active membership content, announcing a LiveKit focus. */
    fun joinContent(userId: String, deviceId: String, roomId: String, config: RtcConfig): String {
        val focus = JSONObject()
            .put("type", "livekit")
            .put("livekit_service_url", config.livekitUrl)
            .put("livekit_alias", roomId)
        return JSONObject()
            .put("application", "m.call")
            .put("call_id", "")
            .put("scope", "m.room")
            .put("device_id", deviceId)
            .put("focus_active", JSONObject().put("type", "livekit").put("focus_selection", "oldest_membership"))
            .put("foci_preferred", JSONArray().put(focus))
            .put("member", JSONObject().put("user_id", userId))
            .toString()
    }

    /** Empty content = we have left the call. */
    fun leaveContent(): String = "{}"
}

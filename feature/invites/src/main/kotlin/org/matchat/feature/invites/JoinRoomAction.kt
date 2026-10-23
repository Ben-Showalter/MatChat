package org.matchat.feature.invites

import org.matchat.core.model.RoomId

sealed interface JoinRoomAction {
    /** CENTER on the action button — Join, Ask to join or Open, per state. */
    data object Activate : JoinRoomAction
}

sealed interface JoinRoomNav {
    data class OpenRoom(val roomId: RoomId) : JoinRoomNav
}

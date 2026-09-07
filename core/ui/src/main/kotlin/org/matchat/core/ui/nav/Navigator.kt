package org.matchat.core.ui.nav

import org.matchat.core.model.EventId
import org.matchat.core.model.RoomId
import org.matchat.core.model.UserId

/**
 * Cross-screen navigation. Features depend on this interface, never on each
 * other (AGENTS.md §2); :app implements it once over Jetpack Navigation. A
 * Fragment collects its ViewModel's one-shot nav events and calls these.
 */
interface Navigator {
    fun toSignIn()
    /** After a successful sign-in: room list becomes the root, onboarding is popped. */
    fun toRoomListRoot()
    /** After sign-out: welcome becomes the root, everything else is cleared. */
    fun toWelcomeRoot()
    fun toRoom(roomId: RoomId)
    /** Full-screen image viewer for a timeline image (D-pad pan, * / # zoom). */
    fun toImageViewer(eventId: EventId)
    /** Room info + basic room edits (S12). */
    fun toRoomInfo(roomId: RoomId)
    /** Message info (S11): metadata for one event, with a link to the sender. */
    fun toMessageInfo(eventId: EventId, senderId: UserId, timestampEpochMs: Long)
    /** A sender's profile, reached from message info. */
    fun toProfile(userId: UserId)
    fun toInvites()
    fun toInvite(roomId: RoomId)
    fun toNewChat()
    fun toTypeAddress()
    fun toVerification()
    fun toSettings()
    fun toPolicy()
    fun toHelp()
    fun back()
}

package org.matchat.client.sync

/**
 * What to do with a room's message notification on each room-list update.
 * Pure, so every branch is unit-tested.
 *
 * Why a silent update exists: the SDK often raises a room's unread count a
 * moment before it updates the room's latest message, and an encrypted message
 * is only readable a moment after it arrives. Alerting on the count alone left
 * the notification showing the previous message, or just "1 new message".
 */
object NotificationDecision {
    enum class Action {
        /** A new message: post with sound, vibration and LED. */
        ALERT,

        /** Same unread count, but the latest message's text changed: replace
         *  the showing notification's text without alerting again. */
        UPDATE_SILENTLY,
        CANCEL,
        NONE,
    }

    /**
     * @param showing MatChat's notification for the room is still up — a
     *   notification the user dismissed is never brought back by an update.
     */
    fun decide(
        prevUnread: Int,
        nowUnread: Int,
        previewChanged: Boolean,
        showing: Boolean,
        enabled: Boolean,
    ): Action = when {
        nowUnread == 0 && prevUnread > 0 -> Action.CANCEL
        !enabled -> Action.NONE
        nowUnread > prevUnread -> Action.ALERT
        nowUnread > 0 && previewChanged && showing -> Action.UPDATE_SILENTLY
        else -> Action.NONE
    }
}

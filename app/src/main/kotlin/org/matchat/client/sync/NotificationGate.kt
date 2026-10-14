package org.matchat.client.sync

import org.matchat.core.model.RoomSummary

/**
 * When to post a room's message notification. Pure, so every branch is
 * unit-tested; [MessageNotifications] holds the per-room [RoomState] and acts
 * on the [Action].
 *
 * A notification waits until its text is readable. The SDK often raises a
 * room's unread count a moment before it updates the room's latest message,
 * and an encrypted message is only readable a moment after it arrives —
 * alerting on the count alone showed the previous message, or just
 * "1 new message". So a rise in the count starts a *pending* alert, posted
 * once the latest message is newer than the last one accounted for, not our
 * own, and decrypted. If that hasn't happened within [DECRYPT_WAIT_MS] it is
 * posted without the text, so a message that can't be decrypted (missing
 * keys) still alerts.
 */
object NotificationGate {

    /** How long a notification waits for its text before posting without it
     *  (user's call: 10 s). */
    const val DECRYPT_WAIT_MS = 10_000L

    /**
     * @param accountedTs timestamp of the newest message already notified,
     *   seeded, or sent by us — a latest message at or before it is not new.
     * @param pendingSinceMs when the current pending alert started (the
     *   caller's clock), or null when nothing is waiting.
     */
    data class RoomState(
        val unread: Int,
        val accountedTs: Long,
        val pendingSinceMs: Long? = null,
    )

    enum class Action {
        NONE,

        /** A new message is waiting for its text. */
        HOLD,

        /** Post with the latest message's text (or media label). */
        POST_WITH_TEXT,

        /** Waited [DECRYPT_WAIT_MS] for the text: post with the count only. */
        POST_GENERIC,

        /** The room was read: remove its notification. */
        CANCEL,
    }

    data class Result(val action: Action, val state: RoomState)

    /** The baseline for a room seen on the first pass after start: its
     *  existing unread history never alerts. */
    fun seed(room: RoomSummary) = RoomState(room.unreadCount, room.lastActivityEpochMs ?: 0L)

    /**
     * @param prev the room's state from the previous pass; null for a room
     *   that wasn't in the list then (e.g. just joined), whose messages are
     *   all new.
     * @param nowMs the caller's monotonic clock, compared with
     *   [RoomState.pendingSinceMs].
     */
    fun decide(prev: RoomState?, room: RoomSummary, nowMs: Long, enabled: Boolean): Result {
        val p = prev ?: RoomState(unread = 0, accountedTs = 0L)
        val unread = room.unreadCount
        val ts = room.lastActivityEpochMs ?: 0L
        val accounted = if (room.lastMessageIsOwn) maxOf(p.accountedTs, ts) else p.accountedTs

        if (unread == 0) {
            val action = if (p.unread > 0 || p.pendingSinceMs != null) Action.CANCEL else Action.NONE
            return Result(action, RoomState(0, maxOf(accounted, ts)))
        }
        if (!enabled) return Result(Action.NONE, RoomState(unread, accounted))

        // A further rise while one is waiting keeps the original deadline.
        val since = when {
            p.pendingSinceMs != null -> p.pendingSinceMs
            unread > p.unread -> nowMs
            else -> return Result(Action.NONE, RoomState(unread, accounted))
        }

        val ready = ts > accounted &&
            !room.lastMessageIsOwn &&
            !room.lastMessageUndecrypted &&
            (room.lastMessage != null || room.lastMessageMedia != null)
        return when {
            ready -> Result(Action.POST_WITH_TEXT, RoomState(unread, ts))
            nowMs - since >= DECRYPT_WAIT_MS ->
                Result(Action.POST_GENERIC, RoomState(unread, maxOf(accounted, ts)))
            else -> Result(Action.HOLD, RoomState(unread, accounted, since))
        }
    }
}

package org.matchat.core.model

/**
 * One row in a room timeline (S9). The SDK produces these already paginated and
 * deduped; we never merge or reorder events ourselves (AGENTS.md §0).
 */
sealed interface TimelineItem {
    data class Message(
        val eventId: EventId,
        val sender: UserId,
        val senderName: String,
        val body: String,
        val timestampEpochMs: Long,
        val isOwn: Boolean,
        val sendState: SendState,
        /** True when another member has a read receipt on this (own) message. */
        val isRead: Boolean = false,
        /** An `mxc://` URI for the sender's avatar, or null for none set. */
        val senderAvatarUrl: String? = null,
        /** Who (besides the sender) has a read receipt on this message — the
         *  "seen by" avatar row, shown only for own messages (UX-SPEC S9). */
        val seenBy: List<SeenBy> = emptyList(),
    ) : TimelineItem

    /**
     * A media message (image / video / audio / voice / file). The bytes are not
     * carried here — the UI asks the session to download them by [eventId], which
     * keeps encryption keys inside :core:matrix (AGENTS.md §0/§2).
     */
    data class Media(
        val eventId: EventId,
        val sender: UserId,
        val senderName: String,
        val body: String,
        val timestampEpochMs: Long,
        val isOwn: Boolean,
        val sendState: SendState,
        val kind: MediaKind,
        val filename: String,
        val caption: String?,
        val mimeType: String?,
        val sizeBytes: Long?,
        val durationMs: Long?,
        val isRead: Boolean = false,
        val senderAvatarUrl: String? = null,
        val seenBy: List<SeenBy> = emptyList(),
    ) : TimelineItem

    data class DaySeparator(val label: String) : TimelineItem

    /** Rendered as a distinct, non-scary row with a "Fix encryption" action → S6. */
    data class UnableToDecrypt(
        val eventId: EventId,
        val sender: UserId,
    ) : TimelineItem

    data class StateChange(val text: String) : TimelineItem
}

/** One member who has read a message, for the per-message "seen by" avatar
 *  row (UX-SPEC S9). [avatarUrl] is an `mxc://` URI, or null for none set. */
data class SeenBy(val userId: UserId, val avatarUrl: String?)

enum class MediaKind { IMAGE, VIDEO, AUDIO, VOICE, FILE }

enum class SendState { SENDING, SENT, FAILED }

enum class SyncState { IDLE, SYNCING, OFFLINE, ERROR }

package org.matchat.feature.timeline

import org.matchat.core.model.ErrorText
import org.matchat.core.model.EventId
import org.matchat.core.model.ReactionSummary
import org.matchat.core.model.SeenBy
import org.matchat.core.model.SendState

/** A display-ready timeline row (S9). Formatting is done in the ViewModel. */
sealed interface TimelineRow {
    val stableId: String

    data class Message(
        val eventId: EventId,
        /** Sender name only when it changes from the previous row (UX-SPEC S9). */
        val senderName: String?,
        val body: String,
        val time: String,
        val isOwn: Boolean,
        val sendGlyph: String,
        /** Full sender id and send time, carried for the Message info screen (S11). */
        val senderId: String,
        val timestampEpochMs: Long,
        /** An `mxc://` URI, or null for no avatar set — shown next to
         *  [senderName] when that's non-null (Avatars round). */
        val senderAvatarUrl: String? = null,
        /** Who's read this (own) message besides the sender — rendered as a
         *  short avatar row under the bubble; always empty for a received
         *  message (UX-SPEC S9). */
        val seenBy: List<SeenBy> = emptyList(),
        /** Reaction chips shown below the bubble (Reactions round); empty
         *  hides the row entirely. */
        val reactions: List<ReactionSummary> = emptyList(),
        /** True when pinned — TimelineAdapter prefixes the time text with
         *  📌, the same compact idiom sendGlyph already uses (Pinned
         *  messages round). */
        val isPinned: Boolean = false,
    ) : TimelineRow {
        override val stableId: String get() = eventId.value
    }

    /** An inline image (downloaded by eventId on bind). */
    data class Image(
        val eventId: EventId,
        val senderName: String?,
        val caption: String?,
        val time: String,
        val isOwn: Boolean,
        val sendGlyph: String,
        val senderAvatarUrl: String? = null,
        val seenBy: List<SeenBy> = emptyList(),
        val reactions: List<ReactionSummary> = emptyList(),
        val isPinned: Boolean = false,
    ) : TimelineRow {
        override val stableId: String get() = "img:${eventId.value}"
    }

    /** A file / video / audio / voice attachment; CENTER downloads and opens/plays. */
    data class Attachment(
        val eventId: EventId,
        val senderName: String?,
        val glyph: String,
        val label: String,
        val sub: String?,
        val time: String,
        val isOwn: Boolean,
        val mimeType: String?,
        val play: Boolean, // true = audio/voice (play in-app), false = open externally
        val isPinned: Boolean = false,
    ) : TimelineRow {
        override val stableId: String get() = "att:${eventId.value}"
    }

    data class DaySeparator(val label: String) : TimelineRow {
        override val stableId: String get() = "day:$label"
    }

    data class UnableToDecrypt(val eventId: EventId) : TimelineRow {
        override val stableId: String get() = "utd:${eventId.value}"
    }

    data class State(val text: String) : TimelineRow {
        override val stableId: String get() = "state:$text"
    }
}

/**
 * Everything the timeline shows (S9). The unencrypted warning band, loading of
 * earlier messages, empty and error are all fields here (AGENTS.md §3, G4).
 */
data class TimelineState(
    val title: String = "",
    val rows: List<TimelineRow> = emptyList(),
    val isEncrypted: Boolean = true,
    val isLoadingEarlier: Boolean = false,
    val isComposeFocused: Boolean = false,
    /** "Alice is typing…" / "Several people are typing…", or null when nobody is. */
    val typingText: String? = null,
    val error: ErrorText? = null,
    /** Count of pinned messages in this room — drives the pinned-messages
     *  band's visibility/text and whether the RIGHT-key shortcut does
     *  anything (Pinned messages quick-access round). */
    val pinnedCount: Int = 0,
) {
    val isEmpty: Boolean get() = rows.isEmpty() && !isLoadingEarlier
    val showUnencryptedBand: Boolean get() = !isEncrypted

    companion object {
        fun glyph(state: SendState): String = when (state) {
            SendState.SENDING -> "○"
            SendState.SENT -> "✓"
            SendState.FAILED -> "!"
        }
    }
}

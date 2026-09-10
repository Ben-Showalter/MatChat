package org.matchat.core.matrix.internal

import org.matchat.core.model.EventId
import org.matchat.core.model.MediaKind
import org.matchat.core.model.RoomId
import org.matchat.core.model.RoomSummary
import org.matchat.core.model.SeenBy
import org.matchat.core.model.SendState
import org.matchat.core.model.TimelineItem
import org.matchat.core.model.UserId
import org.matrix.rustcomponents.sdk.EventOrTransactionId
import org.matrix.rustcomponents.sdk.MediaSource
import org.matrix.rustcomponents.sdk.MessageType
import org.matrix.rustcomponents.sdk.MsgLikeKind
import org.matrix.rustcomponents.sdk.Room
import org.matrix.rustcomponents.sdk.TimelineItemContent
import org.matrix.rustcomponents.sdk.TimelineItem as RustTimelineItem

/** A room member's already-synced name/avatar (RustRoomTimeline.fetchMembers,
 *  RustMatrixSession.roomMembers) — either field may be null when the server
 *  hasn't published one. */
internal data class MemberInfo(val displayName: String?, val avatarUrl: String?)

/**
 * SDK types -> :core:model types. The only mapping layer; if the SDK changes
 * shape on upgrade, this file is what breaks, by design (ARCHITECTURE.md).
 */
internal object Mappers {

    suspend fun toRoomSummary(room: Room): RoomSummary {
        val info = runCatching { room.roomInfo() }.getOrNull()
        val encrypted = runCatching { room.isEncrypted() }.getOrDefault(true)
        return RoomSummary(
            id = RoomId(room.id()),
            name = info?.displayName ?: room.displayName() ?: room.id(),
            // Last-message preview needs the latest-event API — a follow-up.
            lastMessage = null,
            lastActivityEpochMs = null,
            unreadCount = (info?.numUnreadMessages ?: 0uL).toInt(),
            isEncrypted = encrypted,
            avatarUrl = info?.avatarUrl ?: runCatching { room.avatarUrl() }.getOrNull(),
        )
    }

    /**
     * Maps one SDK timeline item to a domain [TimelineItem], or null for items we
     * do not render (state changes, virtual dividers). Only text messages are
     * mapped in this step.
     *
     * EventTimelineItem is a uniffi Record, so its fields are properties. The body
     * path is content -> MsgLike.content.kind -> Message.content.body.
     *
     * [members] is the room's already-synced member list (userId -> name/avatar;
     * RustRoomTimeline fetches it once via fetchMembers(), no per-event network
     * round trip) — resolveSenderName falls back to the raw Matrix ID for a
     * sender not yet in it (e.g. joined mid-conversation before the next
     * member-list refresh), never a blank name; a missing avatar is simply null.
     */
    fun toTimelineItem(item: RustTimelineItem, members: Map<String, MemberInfo>): TimelineItem? {
        val event = item.asEvent() ?: return null
        val msgLike = event.content as? TimelineItemContent.MsgLike ?: return null
        val messageKind = msgLike.content.kind as? MsgLikeKind.Message ?: return null
        val eventId = eventIdOf(event.eventOrTransactionId)
        // "Seen by" (Avatars round): every read-receipt holder other than the
        // sender — already a full user-id list on the SDK side (Map<String,
        // Receipt>), not just a count; readByOther is kept as its own bool for
        // isRead's cheap single-glyph check rather than reading seenBy.isEmpty()
        // in the hot render path.
        val seenBy = runCatching {
            event.readReceipts.keys
                .filter { it != event.sender }
                .map { SeenBy(UserId(it), members[it]?.avatarUrl) }
        }.getOrDefault(emptyList())
        val readByOther = seenBy.isNotEmpty()
        val senderAvatarUrl = members[event.sender]?.avatarUrl

        val media = mediaOf(messageKind.content.msgType, eventId, event, readByOther, members, senderAvatarUrl, seenBy)
        if (media != null) return media

        return TimelineItem.Message(
            eventId = EventId(eventId),
            sender = UserId(event.sender),
            senderName = resolveSenderName(event.sender, members),
            body = messageKind.content.body,
            timestampEpochMs = event.timestamp.toLong(),
            isOwn = event.isOwn,
            sendState = SendState.SENT,
            isRead = readByOther,
            senderAvatarUrl = senderAvatarUrl,
            seenBy = seenBy,
        )
    }

    /** The raw Matrix ID (`@user:server`) is always a safe fallback — never blank,
     *  never a network call — for a sender [members] doesn't (yet) know. */
    internal fun resolveSenderName(rawSenderId: String, members: Map<String, MemberInfo>): String =
        members[rawSenderId]?.displayName ?: rawSenderId

    /** Media messages (image/video/audio/voice/file). Registers the MediaSource
     *  so the download-by-id path can reach it. Returns null for text-like types. */
    @Suppress("CyclomaticComplexMethod", "LongParameterList")
    private fun mediaOf(
        type: MessageType,
        eventId: String,
        event: org.matrix.rustcomponents.sdk.EventTimelineItem,
        isRead: Boolean,
        members: Map<String, MemberInfo>,
        senderAvatarUrl: String?,
        seenBy: List<SeenBy>,
    ): TimelineItem.Media? {
        val (kind, source, filename, caption, mime, size, durationMs) = when (type) {
            is MessageType.Image -> Media6(
                MediaKind.IMAGE, type.content.source, type.content.filename, type.content.caption,
                type.content.info?.mimetype, type.content.info?.size?.toLong(), null,
            )
            is MessageType.Video -> Media6(
                MediaKind.VIDEO, type.content.source, type.content.filename, type.content.caption,
                type.content.info?.mimetype, type.content.info?.size?.toLong(), null,
            )
            is MessageType.Audio -> Media6(
                if (type.content.voice != null) MediaKind.VOICE else MediaKind.AUDIO,
                type.content.source, type.content.filename, type.content.caption,
                type.content.info?.mimetype, type.content.info?.size?.toLong(),
                runCatching {
                    type.content.info?.duration?.toMillis() ?: type.content.audio?.duration?.toMillis()
                }.getOrNull(),
            )
            is MessageType.File -> Media6(
                MediaKind.FILE, type.content.source, type.content.filename, type.content.caption,
                type.content.info?.mimetype, type.content.info?.size?.toLong(), null,
            )
            else -> return null
        }
        MediaRegistry.put(eventId, source)
        return TimelineItem.Media(
            eventId = EventId(eventId),
            sender = UserId(event.sender),
            senderName = resolveSenderName(event.sender, members),
            body = caption ?: filename,
            timestampEpochMs = event.timestamp.toLong(),
            isOwn = event.isOwn,
            sendState = SendState.SENT,
            kind = kind,
            filename = filename,
            caption = caption,
            mimeType = mime,
            sizeBytes = size,
            durationMs = durationMs,
            isRead = isRead,
            senderAvatarUrl = senderAvatarUrl,
            seenBy = seenBy,
        )
    }

    /** Small carrier so the media `when` can destructure its columns (a data
     *  class provides component1..7 automatically). */
    private data class Media6(
        val kind: MediaKind,
        val source: MediaSource,
        val filename: String,
        val caption: String?,
        val mime: String?,
        val size: Long?,
        val durationMs: Long?,
    )

    private fun eventIdOf(id: EventOrTransactionId): String =
        (id as? EventOrTransactionId.EventId)?.eventId.orEmpty()
}

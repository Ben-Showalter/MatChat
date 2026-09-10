package org.matchat.core.model

/** A member of a room, mapped from the SDK for the Room Info screen (S12). */
data class RoomMemberSummary(
    val userId: UserId,
    val displayName: String?,
    val membership: Membership,
    val isSelf: Boolean,
    /** An `mxc://` URI, or null for no avatar set. */
    val avatarUrl: String? = null,
) {
    /** Best display label: the name if the server has one, else the address. */
    val label: String get() = displayName?.takeIf { it.isNotBlank() } ?: userId.value
}

enum class Membership { JOINED, INVITED, LEFT, BANNED, KNOCKING, OTHER }

/** Read-only room details for the Room Info header (S12). */
data class RoomDetails(
    val id: RoomId,
    val name: String,
    val topic: String?,
    val isEncrypted: Boolean,
    val isDirect: Boolean,
    val memberCount: Int,
)

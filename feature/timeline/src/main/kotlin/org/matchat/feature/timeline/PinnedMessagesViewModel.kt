package org.matchat.feature.timeline

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.matchat.core.matrix.MatrixSession
import org.matchat.core.model.RoomId
import org.matchat.core.model.TimelineItem
import org.matchat.core.model.format.RelativeTime
import javax.inject.Inject

/**
 * Room Info > Pinned messages (Pinned messages round). Reuses [RoomInfoRow]/
 * [RoomInfoState] and RoomInfoAdapter rather than a new row type + adapter —
 * each pinned message becomes a Field row (CENTER-activatable), filtered
 * straight out of the room's own already-mapped timeline (RoomTimeline
 * .items), not a separate SDK call. Opens a **second** live timeline for the
 * same room alongside the one TimelineFragment may already have open — this
 * app doesn't cache/share RoomTimeline instances per room anywhere, so this
 * matches existing precedent (e.g. RoomInfoFragment's own session.roomMembers
 * call), not a new pattern.
 */
@HiltViewModel
class PinnedMessagesViewModel @Inject constructor(
    session: MatrixSession,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val roomId = RoomId(requireNotNull(savedStateHandle["roomId"]))
    private val timeline = session.timeline(roomId)

    val state: StateFlow<RoomInfoState> =
        timeline.items
            .map { RoomInfoState(rows = it.toPinnedRows()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), RoomInfoState())

    /** CENTER on a pinned row — jump-to-message inside the room isn't wired
     *  anywhere in this app yet (no screen has that capability today), so
     *  this opens the room at its normal position. A flagged scope cut, not
     *  a silent gap. */
    fun roomId(): RoomId = roomId

    private fun List<TimelineItem>.toPinnedRows(): List<RoomInfoRow> = mapNotNull { item ->
        when {
            item is TimelineItem.Message && item.isPinned -> RoomInfoRow.Field(
                key = item.eventId.value,
                label = "${item.senderName} · ${RelativeTime.clockTime(item.timestampEpochMs)}",
                value = item.body,
            )
            item is TimelineItem.Media && item.isPinned -> RoomInfoRow.Field(
                key = item.eventId.value,
                label = "${item.senderName} · ${RelativeTime.clockTime(item.timestampEpochMs)}",
                value = item.caption ?: item.filename,
            )
            else -> null
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

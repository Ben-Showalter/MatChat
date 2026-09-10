package org.matchat.core.matrix.internal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

/**
 * Root cause of the pin-persistence bug: [RustMatrixSession.timeline] builds a
 * brand-new [RustRoomTimeline] on every call, so [TimelineViewModel],
 * [PinnedMessagesViewModel] and [MessageInfoViewModel] each hold an independent
 * instance for the same room, each with its own private, in-memory pinnedIds
 * seeded from its own cold `room.roomInfo()` read. A pin made through one
 * instance's [RustRoomTimeline.setPinned] therefore never reaches a sibling or
 * future instance for the same room — no error, because nothing failed.
 *
 * This is a small, process-lifetime, in-memory cache internal to :core:matrix
 * (same "stateful singleton, no DI" shape as [MediaRegistry]) so every
 * [RustRoomTimeline] for a given room shares one truth, live.
 */
internal object PinnedEventsCache {
    private val state = MutableStateFlow<Map<String, Set<String>>>(emptyMap())

    /** Null means nothing has been cached for this room yet this process. */
    fun get(roomId: String): Set<String>? = state.value[roomId]

    fun put(roomId: String, ids: Set<String>) {
        state.value = state.value + (roomId to ids)
    }

    /** Emits only values published *after* collection starts — a fresh
     *  subscriber gets its own seed via [get], not a replay of this. */
    fun updatesFor(roomId: String): Flow<Set<String>> =
        state.map { it[roomId] }
            .drop(1)
            .filterNotNull()
            .distinctUntilChanged()
}

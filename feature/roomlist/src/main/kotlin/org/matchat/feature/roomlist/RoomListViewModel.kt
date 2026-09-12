package org.matchat.feature.roomlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.matchat.core.matrix.Draft
import org.matchat.core.matrix.DraftStore
import org.matchat.core.matrix.MatrixSession
import org.matchat.core.model.InviteSummary
import org.matchat.core.model.MillisClock
import org.matchat.core.model.RoomId
import org.matchat.core.model.RoomSummary
import org.matchat.core.model.SyncState
import org.matchat.core.model.format.RelativeTime
import org.matchat.core.policy.PolicyProvider
import javax.inject.Inject

/**
 * S8 logic. All formatting and the loading/empty/offline decisions live here so
 * they are unit-testable against a [org.matchat.core.testing.FakeMatrixSession]
 * (AGENTS.md §6). Navigation is emitted as one-shot events the Fragment collects.
 */
@HiltViewModel
class RoomListViewModel @Inject constructor(
    private val session: MatrixSession,
    policyProvider: PolicyProvider,
    private val clock: MillisClock,
    private val draftStore: DraftStore,
) : ViewModel() {

    private val focusedIndex = MutableStateFlow(0)
    private val navChannel = Channel<RoomListNav>(Channel.BUFFERED)
    val navEvents: Flow<RoomListNav> = navChannel.receiveAsFlow()

    /** Everything [state] needs except [DraftStore.drafts] — its own type
     *  purely so the two combine steps below stay readable (Kotlin's
     *  fixed-arity `combine` tops out at 5 flows, and this room-related
     *  group already uses all 5; mirrors TimelineViewModel's own
     *  ComposeContext/pendingAttachment nesting for exactly this reason). */
    private data class RoomListContext(
        val rooms: List<RoomSummary>,
        val invites: List<InviteSummary>,
        val sync: SyncState,
        val allowDirectChat: Boolean,
        val focus: Int,
    )

    val state: StateFlow<RoomListState> =
        combine(
            combine(
                session.rooms,
                session.invites,
                session.syncState,
                policyProvider.policy,
                focusedIndex,
            ) { rooms, invites, sync, policy, focus ->
                RoomListContext(rooms, invites, sync, policy.allowDirectChat, focus)
            },
            draftStore.drafts,
        ) { ctx, drafts ->
            RoomListState(
                isLoading = ctx.sync == SyncState.SYNCING && ctx.rooms.isEmpty(),
                rooms = ctx.rooms.map { it.toRow(drafts[it.id.value]) },
                inviteBand = ctx.invites.toBand(),
                isOffline = ctx.sync == SyncState.OFFLINE || ctx.sync == SyncState.ERROR,
                focusedIndex = ctx.focus,
                newMessageEnabled = ctx.allowDirectChat,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), RoomListState())

    fun onAction(action: RoomListAction) {
        when (action) {
            is RoomListAction.RoomFocused -> focusedIndex.value = action.index
            is RoomListAction.OpenRoom -> emit(RoomListNav.Room(action.roomId))
            RoomListAction.OpenInvites -> emit(RoomListNav.Invites)
            RoomListAction.NewMessage -> emit(RoomListNav.NewChat)
            RoomListAction.OpenSettings -> emit(RoomListNav.Settings)
            RoomListAction.OpenHelp -> emit(RoomListNav.Help)
            RoomListAction.SignOut -> emit(RoomListNav.SignOut)
            RoomListAction.MarkAllRead -> Unit // handled by the SDK read-marker call (M2)
            RoomListAction.NextUnread -> jumpToNextUnread()
        }
    }

    private fun jumpToNextUnread() {
        val rooms = state.value.rooms
        val start = focusedIndex.value
        val next = (1..rooms.size).map { (start + it) % rooms.size }
            .firstOrNull { rooms[it].isUnread } ?: return
        focusedIndex.value = next
    }

    private fun emit(nav: RoomListNav) {
        viewModelScope.launch { navChannel.send(nav) }
    }

    /** [draft] is this room's live draft, if any (draft messages round) — a
     *  plain passthrough of the domain data; the Fragment/Adapter (which
     *  already own every `getString`/resource call in this module — the
     *  ViewModel has none) decide the actual "Draft: …" display text and
     *  styling from it, same boundary as everywhere else here. */
    private fun RoomSummary.toRow(draft: Draft?) = RoomRow(
        id = id,
        name = name,
        preview = lastMessage.orEmpty(),
        time = lastActivityEpochMs?.let { RelativeTime.roomListLabel(it, clock.now()) }.orEmpty(),
        unreadCount = unreadCount,
        avatarUrl = avatarUrl,
        draft = draft,
    )

    /** Download an avatar's bytes by its `mxc://` URI (Avatars round). */
    suspend fun loadAvatar(mxcUrl: String): ByteArray? = session.loadAvatar(mxcUrl)

    private fun List<InviteSummary>.toBand(): InviteBand? = if (isEmpty()) null else InviteBand(size)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** One-shot navigation intents from the room list. */
sealed interface RoomListNav {
    data class Room(val roomId: RoomId) : RoomListNav
    data object Invites : RoomListNav
    data object NewChat : RoomListNav
    data object Settings : RoomListNav
    data object Help : RoomListNav
    data object SignOut : RoomListNav
}

package org.matchat.feature.timeline

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.matchat.core.matrix.MatrixSession
import org.matchat.core.model.Membership
import org.matchat.core.model.RoomDetails
import org.matchat.core.model.RoomId
import org.matchat.core.model.RoomMemberSummary
import org.matchat.core.model.UserId
import javax.inject.Inject

/** S12 Room Info: shows name/topic/encryption/members and applies basic edits. */
@HiltViewModel
class RoomInfoViewModel @Inject constructor(
    private val session: MatrixSession,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val roomId = RoomId(requireNotNull(savedStateHandle["roomId"]))

    private val _state = MutableStateFlow(RoomInfoState())
    val state: StateFlow<RoomInfoState> = _state.asStateFlow()

    private val navChannel = Channel<RoomInfoNav>(Channel.BUFFERED)
    val navEvents: Flow<RoomInfoNav> = navChannel.receiveAsFlow()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            val details = session.roomDetails(roomId)
            val members = session.roomMembers(roomId)
            _state.update { it.copy(title = details?.name.orEmpty(), rows = rows(details, members)) }
        }
    }

    fun setName(name: String) = edit { session.setRoomName(roomId, name.trim()) }
    fun setTopic(topic: String) = edit { session.setRoomTopic(roomId, topic.trim()) }

    fun addMember(rawAddress: String) {
        val address = rawAddress.trim()
        if (!address.startsWith("@") || !address.contains(':')) {
            emit(RoomInfoNav.Toast(ToastKey.BAD_ADDRESS))
            return
        }
        edit { session.inviteMember(roomId, UserId(address)) }
    }

    fun removeMember(userId: UserId) = edit { session.removeMember(roomId, userId) }

    fun leave() {
        _state.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            session.leaveRoom(roomId)
            navChannel.send(RoomInfoNav.Left)
        }
    }

    /** Runs [op], then reloads so the UI reflects the server state. */
    private fun edit(op: suspend () -> Result<Unit>) {
        _state.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            val result = op()
            _state.update { it.copy(isBusy = false) }
            if (result.isFailure) emit(RoomInfoNav.Toast(ToastKey.EDIT_FAILED)) else reload()
        }
    }

    private fun emit(nav: RoomInfoNav) {
        viewModelScope.launch { navChannel.send(nav) }
    }

    private fun rows(details: RoomDetails?, members: List<RoomMemberSummary>): List<RoomInfoRow> {
        val rows = mutableListOf<RoomInfoRow>()
        rows += RoomInfoRow.Field(KEY_NAME, "Name", details?.name.orEmpty())
        rows += RoomInfoRow.Field(KEY_TOPIC, "Topic", details?.topic.orEmpty())
        rows += RoomInfoRow.Info("Encryption", if (details?.isEncrypted == true) "On" else "Off")
        rows += RoomInfoRow.Section("Members (${members.count { it.membership == Membership.JOINED }})")
        members.filter { it.membership == Membership.JOINED || it.membership == Membership.INVITED }
            .sortedBy { it.label.lowercase() }
            .forEach { m ->
                val sub = when {
                    m.isSelf -> "You"
                    m.membership == Membership.INVITED -> "Invited"
                    else -> m.userId.value
                }
                rows += RoomInfoRow.Member(m.userId, m.label, sub, m.isSelf)
            }
        rows += RoomInfoRow.Action(KEY_ADD, "Add member")
        rows += RoomInfoRow.Action(KEY_LEAVE, "Leave room")
        return rows
    }

    companion object {
        const val KEY_NAME = "name"
        const val KEY_TOPIC = "topic"
        const val KEY_ADD = "add"
        const val KEY_LEAVE = "leave"
    }
}

sealed interface RoomInfoNav {
    data object Left : RoomInfoNav
    data class Toast(val key: ToastKey) : RoomInfoNav
}

enum class ToastKey { BAD_ADDRESS, EDIT_FAILED }

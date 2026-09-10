package org.matchat.feature.timeline

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.matchat.core.matrix.MatrixSession
import org.matchat.core.model.RoomId
import org.matchat.core.model.UserId

/** A sender's profile (S11): name + address, with a Send-message action. */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val session: MatrixSession,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val userId = UserId(requireNotNull(savedStateHandle["userId"]))

    private val _state = MutableStateFlow(ProfileState(rows = baseRows(null)))
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private val navChannel = Channel<ProfileNav>(Channel.BUFFERED)
    val navEvents: Flow<ProfileNav> = navChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            val name = session.lookupProfile(userId).getOrNull()?.displayName
            _state.update { it.copy(title = name ?: userId.value, rows = baseRows(name)) }
        }
    }

    fun message() {
        if (_state.value.isBusy) return
        _state.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            session.startDirectChat(userId).fold(
                onSuccess = { navChannel.send(ProfileNav.OpenRoom(it)) },
                onFailure = {
                    _state.update { s -> s.copy(isBusy = false) }
                    navChannel.send(ProfileNav.Failed)
                },
            )
        }
    }

    private fun baseRows(displayName: String?): List<RoomInfoRow> = listOf(
        RoomInfoRow.Info("Name", displayName?.takeIf { it.isNotBlank() } ?: "—"),
        RoomInfoRow.Info("User ID", userId.value),
        RoomInfoRow.Action(ACTION_MESSAGE, "Send message"),
    )

    companion object {
        const val ACTION_MESSAGE = "message"
    }
}

data class ProfileState(
    val title: String = "",
    val rows: List<RoomInfoRow> = emptyList(),
    val isBusy: Boolean = false,
)

sealed interface ProfileNav {
    data class OpenRoom(val roomId: RoomId) : ProfileNav
    data object Failed : ProfileNav
}

package org.matchat.feature.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** S33: saving the offline setup page somewhere the user can take it to a
 *  computer. Every outcome is said on screen — never a silent no-op. */
@HiltViewModel
class SetupHelpViewModel internal constructor(
    private val store: SetupPageStore,
    private val io: CoroutineDispatcher,
) : ViewModel() {

    @Inject constructor(store: SetupPageStore) : this(store, Dispatchers.IO)

    private val _state = MutableStateFlow(SetupHelpState())
    val state: StateFlow<SetupHelpState> = _state.asStateFlow()

    private val navChannel = Channel<SetupHelpNav>(Channel.BUFFERED)
    val navEvents: Flow<SetupHelpNav> = navChannel.receiveAsFlow()

    fun onAction(action: SetupHelpAction) {
        viewModelScope.launch {
            when (action) {
                SetupHelpAction.SaveToPhone ->
                    if (store.needsStoragePermission) {
                        navChannel.send(SetupHelpNav.RequestStoragePermission)
                    } else {
                        saveToPhone()
                    }
                is SetupHelpAction.StoragePermissionResult ->
                    if (action.granted) saveToPhone() else show(SetupHelpMessage.NeedsStorage)
                SetupHelpAction.SaveToSdCard -> show(
                    when (val saved = withContext(io) { store.saveToSdCard() }) {
                        is SdCardSave.Saved -> SetupHelpMessage.Saved(saved.where)
                        SdCardSave.NoCard -> SetupHelpMessage.NoSdCard
                        SdCardSave.Failed -> SetupHelpMessage.SaveFailed
                    },
                )
                SetupHelpAction.ShareFailed -> show(SetupHelpMessage.NoShare)
            }
        }
    }

    private suspend fun saveToPhone() {
        val where = withContext(io) { store.saveToDownloads() }
        show(if (where != null) SetupHelpMessage.Saved(where) else SetupHelpMessage.SaveFailed)
    }

    private fun show(message: SetupHelpMessage) {
        _state.value = SetupHelpState(message)
    }
}

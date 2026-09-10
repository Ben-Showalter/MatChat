package org.matchat.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.matchat.core.ui.prefs.SILENT_NOTIFICATION_SOUND
import org.matchat.core.ui.prefs.UserPreferences

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
) : ViewModel() {

    val state: StateFlow<NotificationsState> =
        combineState().stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), NotificationsState())

    fun onAction(action: NotificationsAction) {
        viewModelScope.launch {
            when (action) {
                NotificationsAction.ToggleEnabled ->
                    userPreferences.setNotificationsEnabled(!userPreferences.notificationsEnabled.value)
                is NotificationsAction.SelectSound -> userPreferences.setNotificationSoundUri(action.uri)
            }
        }
    }

    /** The picker returns null for both "Silent" (EXTRA_RINGTONE_SHOW_SILENT)
     *  and "nothing picked" (the user backed out) — this is the one place
     *  that tells them apart, since only the Fragment knows which of those
     *  actually happened (a cancelled picker vs. a real pick). */
    fun ringtonePickResultToUri(pickedUri: Uri?, wasCancelled: Boolean): String? = when {
        wasCancelled -> userPreferences.notificationSoundUri.value // no-op, keep current
        pickedUri == null -> SILENT_NOTIFICATION_SOUND
        else -> pickedUri.toString()
    }

    /** The current sound's stored uri, for pre-selecting the picker. */
    fun currentSoundUri(): String? = userPreferences.notificationSoundUri.value

    private fun combineState() = combine(
        userPreferences.notificationsEnabled,
        userPreferences.notificationSoundUri,
    ) { enabled, soundUri ->
        NotificationsState(enabled = enabled, sound = soundChoiceFor(soundUri))
    }

    private fun soundChoiceFor(soundUri: String?): SoundChoice = when (soundUri) {
        null -> SoundChoice.Default
        SILENT_NOTIFICATION_SOUND -> SoundChoice.Silent
        else -> SoundChoice.Custom(soundUri)
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

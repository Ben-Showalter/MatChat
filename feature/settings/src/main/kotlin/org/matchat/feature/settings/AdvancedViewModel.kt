package org.matchat.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.matchat.core.ui.prefs.UserPreferences

@HiltViewModel
class AdvancedViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
) : ViewModel() {

    val state: StateFlow<AdvancedState> =
        userPreferences.softkeysSwapped
            .map(::AdvancedState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AdvancedState())

    fun onAction(action: AdvancedAction) {
        viewModelScope.launch {
            when (action) {
                AdvancedAction.ToggleSoftkeysSwapped ->
                    userPreferences.setSoftkeysSwapped(!userPreferences.softkeysSwapped.value)
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

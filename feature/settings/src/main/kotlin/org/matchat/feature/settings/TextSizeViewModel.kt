package org.matchat.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.matchat.core.ui.prefs.TextSizePreference
import org.matchat.core.ui.prefs.UserPreferences
import javax.inject.Inject

@HiltViewModel
class TextSizeViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
) : ViewModel() {

    val state: StateFlow<TextSizeState> =
        userPreferences.textSize
            .map(::TextSizeState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TextSizeState())

    fun onAction(action: TextSizeAction) {
        viewModelScope.launch {
            when (action) {
                TextSizeAction.SelectNormal -> userPreferences.setTextSize(TextSizePreference.NORMAL)
                TextSizeAction.SelectLarge -> userPreferences.setTextSize(TextSizePreference.LARGE)
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

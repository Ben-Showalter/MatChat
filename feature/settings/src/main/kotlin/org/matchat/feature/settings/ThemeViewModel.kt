package org.matchat.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.matchat.core.ui.prefs.ThemeMode
import org.matchat.core.ui.prefs.UserPreferences

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
) : ViewModel() {

    val state: StateFlow<ThemeState> =
        combine(userPreferences.themeMode, userPreferences.accentColor, ::ThemeState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ThemeState())

    fun onAction(action: ThemeAction) {
        viewModelScope.launch {
            when (action) {
                ThemeAction.SelectLight -> userPreferences.setThemeMode(ThemeMode.LIGHT)
                ThemeAction.SelectDark -> userPreferences.setThemeMode(ThemeMode.DARK)
                is ThemeAction.SelectAccent -> userPreferences.setAccentColor(action.color)
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

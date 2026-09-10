package org.matchat.core.testing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.matchat.core.ui.prefs.AccentColor
import org.matchat.core.ui.prefs.ThemeMode
import org.matchat.core.ui.prefs.UserPreferences

/** An in-memory [UserPreferences] for ViewModel tests. */
class FakeUserPreferences(
    initialThemeMode: ThemeMode = ThemeMode.LIGHT,
    initialAccentColor: AccentColor = AccentColor.GREEN,
    initialSoftkeysSwapped: Boolean = false,
) : UserPreferences {

    private val themeModeState = MutableStateFlow(initialThemeMode)
    override val themeMode: StateFlow<ThemeMode> = themeModeState

    private val accentColorState = MutableStateFlow(initialAccentColor)
    override val accentColor: StateFlow<AccentColor> = accentColorState

    private val softkeysSwappedState = MutableStateFlow(initialSoftkeysSwapped)
    override val softkeysSwapped: StateFlow<Boolean> = softkeysSwappedState

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeModeState.value = mode
    }

    override suspend fun setAccentColor(color: AccentColor) {
        accentColorState.value = color
    }

    override suspend fun setSoftkeysSwapped(swapped: Boolean) {
        softkeysSwappedState.value = swapped
    }
}

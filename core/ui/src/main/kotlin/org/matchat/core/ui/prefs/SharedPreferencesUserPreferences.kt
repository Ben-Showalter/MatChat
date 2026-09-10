package org.matchat.core.ui.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Plain [SharedPreferences]-backed [UserPreferences]. Falls back to LIGHT /
 *  GREEN for a value that's missing, or that no longer names an enum
 *  constant (an old build's preference on a downgrade). */
@Singleton
internal class SharedPreferencesUserPreferences @Inject constructor(
    @ApplicationContext context: Context,
) : UserPreferences {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val themeModeState = MutableStateFlow(readEnum(KEY_THEME_MODE, ThemeMode.LIGHT))
    override val themeMode: StateFlow<ThemeMode> = themeModeState

    private val accentColorState = MutableStateFlow(readEnum(KEY_ACCENT_COLOR, AccentColor.GREEN))
    override val accentColor: StateFlow<AccentColor> = accentColorState

    private val softkeysSwappedState = MutableStateFlow(prefs.getBoolean(KEY_SOFTKEYS_SWAPPED, false))
    override val softkeysSwapped: StateFlow<Boolean> = softkeysSwappedState

    override suspend fun setThemeMode(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME_MODE, mode.name) }
        themeModeState.value = mode
    }

    override suspend fun setAccentColor(color: AccentColor) {
        prefs.edit { putString(KEY_ACCENT_COLOR, color.name) }
        accentColorState.value = color
    }

    override suspend fun setSoftkeysSwapped(swapped: Boolean) {
        prefs.edit { putBoolean(KEY_SOFTKEYS_SWAPPED, swapped) }
        softkeysSwappedState.value = swapped
    }

    private inline fun <reified T : Enum<T>> readEnum(key: String, default: T): T =
        prefs.getString(key, null)?.let { stored ->
            enumValues<T>().firstOrNull { it.name == stored }
        } ?: default

    private companion object {
        const val PREFS_NAME = "user_preferences"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_ACCENT_COLOR = "accent_color"
        const val KEY_SOFTKEYS_SWAPPED = "softkeys_swapped"
    }
}

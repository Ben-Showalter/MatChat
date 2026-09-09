package org.matchat.core.ui.prefs

import kotlinx.coroutines.flow.StateFlow

/** Light or dark (Settings > Theme). */
enum class ThemeMode { LIGHT, DARK }

/**
 * The selectable accent colors (Settings > Theme > Accent color). Governs
 * only the focus bar and the system accent tint — never the fixed
 * "encrypted" green or the link color, which stay theme-driven (light/dark),
 * not user-driven, so they keep meaning what they mean regardless of taste.
 */
enum class AccentColor { GREEN, AMBER, BLUE, PLUM }

/**
 * How the app renders itself. A [StateFlow], not a value read once, so a
 * screen observing it updates live if it ever changes out from under it —
 * same shape as :core:policy's PolicyProvider. In practice the only writer
 * is the Theme settings screen itself, and MainActivity recreates on a
 * change (mid-session theme switching isn't attempted).
 */
interface UserPreferences {
    val themeMode: StateFlow<ThemeMode>
    val accentColor: StateFlow<AccentColor>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setAccentColor(color: AccentColor)
}

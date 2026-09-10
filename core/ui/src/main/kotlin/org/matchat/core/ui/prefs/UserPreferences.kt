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
enum class AccentColor {
    GREEN, AMBER, BLUE, PLUM,
    TEAL, CYAN, INDIGO, VIOLET, ORCHID, ROSE, RUST, OCHRE, OLIVE, FOREST, SLATE, WINE,
}

/** Settings > Text size (UX-SPEC §S16): scales text, avatars, and row heights
 *  together app-wide, via the same ?attr indirection + Activity.recreate()
 *  mechanism as [AccentColor] (Theme.MatChat.Size.{Normal,Large}, themes.xml).
 *  Also toggled by holding `*` (LogicalKey.STAR_HOLD), from any screen. */
enum class TextSizePreference { NORMAL, LARGE }

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
    val textSize: StateFlow<TextSizePreference>

    /** Settings > Advanced > "Swap Left/Right keys" (Phase 6, UI improvement
     *  plan) — for a device whose hardware softkeys are physically reversed.
     *  A narrow, explicit exception to "LEFT=Options/RIGHT=Back, always"
     *  (AGENTS.md §4, docs/adr/0007). Read directly (StateFlow.value, not
     *  collected) by KeyMap's one caller (MainActivity.dispatchKeyEvent) on
     *  every key press — swapping takes effect immediately, no recreate. */
    val softkeysSwapped: StateFlow<Boolean>

    /** Settings > Notifications — whether the incoming-message notification
     *  (raised by SyncForegroundService) fires at all. Read directly
     *  (.value) by the sync service on every unread-count increase, same as
     *  [softkeysSwapped]'s read shape. Does not affect the persistent
     *  "MatChat is running" sync notification, which is not user-optional
     *  (docs/adr/0004). */
    val notificationsEnabled: StateFlow<Boolean>

    /** The chosen message-notification sound, as a content URI string from
     *  RingtoneManager's picker; null means the system default sound. */
    val notificationSoundUri: StateFlow<String?>

    /** Bumped every time [notificationSoundUri] changes. Not shown in any
     *  UI — a NotificationChannel's sound is immutable once created (the
     *  same wall SyncForegroundService's own CHANNEL_ID = "matchat.sync.v2"
     *  already hit), so a new sound means a new channel id
     *  ("matchat.messages.s$version"), not mutating the old one. */
    val notificationChannelVersion: StateFlow<Int>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setAccentColor(color: AccentColor)
    suspend fun setTextSize(size: TextSizePreference)
    suspend fun setSoftkeysSwapped(swapped: Boolean)
    suspend fun setNotificationsEnabled(enabled: Boolean)

    /** Also bumps [notificationChannelVersion] so the next notification is
     *  posted on a fresh channel carrying the new sound. */
    suspend fun setNotificationSoundUri(uri: String?)
}

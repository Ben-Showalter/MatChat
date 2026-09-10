package org.matchat.feature.settings

/** Settings > Notifications: a toggle for whether the incoming-message
 *  notification fires at all, plus the sound it plays when it does. [sound]
 *  is Context-free (no ViewModel here touches Android APIs, same rule as
 *  every other screen this session) — resolving [SoundChoice.Custom]'s
 *  display title via RingtoneManager is the Fragment's job, in render(). */
data class NotificationsState(
    val enabled: Boolean = true,
    val sound: SoundChoice = SoundChoice.Default,
)

sealed interface SoundChoice {
    data object Default : SoundChoice
    data object Silent : SoundChoice
    data class Custom(val uri: String) : SoundChoice
}

sealed interface NotificationsAction {
    data object ToggleEnabled : NotificationsAction
    data class SelectSound(val uri: String?) : NotificationsAction
}

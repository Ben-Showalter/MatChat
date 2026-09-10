package org.matchat.feature.settings

/** Settings > Advanced (Phase 6, UI improvement plan; docs/adr/0007). One
 *  toggle today: swap the physical Left/Right softkeys for a device whose
 *  hardware is reversed. */
data class AdvancedState(
    val softkeysSwapped: Boolean = false,
)

sealed interface AdvancedAction {
    data object ToggleSoftkeysSwapped : AdvancedAction
}

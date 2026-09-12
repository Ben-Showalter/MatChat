package org.matchat.feature.settings

import org.matchat.core.ui.prefs.TextSizePreference

/** Settings > Text size (UX-SPEC §S16). Three states, shown as three inline
 *  focusable rows (not a MenuSheet — few enough states, same shape as
 *  Theme's Appearance section, unlike its 16-choice Accent color picker).
 *  Normal is the default; Small is the reduced option; Large is a step up
 *  again. */
data class TextSizeState(
    val size: TextSizePreference = TextSizePreference.NORMAL,
)

sealed interface TextSizeAction {
    data object SelectNormal : TextSizeAction
    data object SelectSmall : TextSizeAction
    data object SelectLarge : TextSizeAction
}

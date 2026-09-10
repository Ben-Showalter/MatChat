package org.matchat.feature.settings

import org.matchat.core.ui.prefs.TextSizePreference

/** Settings > Text size (UX-SPEC §S16). Two states, shown as two inline
 *  focusable rows (not a MenuSheet — only 2 states, same shape as Theme's
 *  Appearance section, unlike its 16-choice Accent color picker). */
data class TextSizeState(
    val size: TextSizePreference = TextSizePreference.NORMAL,
)

sealed interface TextSizeAction {
    data object SelectNormal : TextSizeAction
    data object SelectLarge : TextSizeAction
}

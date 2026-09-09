package org.matchat.core.ui.theme

import android.graphics.Color
import android.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.matchat.core.ui.R
import org.robolectric.RobolectricTestRunner

/** Regression coverage for the MenuSheet "invisible label" bug: [themeColor]
 *  must resolve a color-state-list role (colorTextOnFocus, colorTextMetaOnFocus
 *  — backed by a `<selector>`, not a plain `<color>`) to an actual opaque
 *  color, not the raw [android.util.TypedValue.data] left over from resolving
 *  a complex resource (AGENTS.md §6). */
@RunWith(RobolectricTestRunner::class)
class ThemeColorTest {

    private val themedContext =
        ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.Theme_MatChat_Light_Green)

    @Test
    fun `a plain color role resolves to its palette value`() {
        val color = themedContext.themeColor(R.attr.colorSurfaceBright)
        assertEquals(themedContext.getColor(R.color.palette_paper_bright), color)
    }

    @Test
    fun `colorTextOnFocus (a color-state-list role) resolves to an opaque color, not the bug's transparent value`() {
        val color = themedContext.themeColor(R.attr.colorTextOnFocus)
        // text_on_focus.xml unconditionally resolves to ?attr/colorTextPrimary.
        assertEquals(themedContext.themeColor(R.attr.colorTextPrimary), color)
        assertNotEquals(0, Color.alpha(color))
    }

    @Test
    fun `colorTextMetaOnFocus (a color-state-list role) resolves to an opaque color`() {
        val color = themedContext.themeColor(R.attr.colorTextMetaOnFocus)
        // text_meta_on_focus.xml unconditionally resolves to ?attr/colorTextSecondary.
        assertEquals(themedContext.themeColor(R.attr.colorTextSecondary), color)
        assertNotEquals(0, Color.alpha(color))
    }
}

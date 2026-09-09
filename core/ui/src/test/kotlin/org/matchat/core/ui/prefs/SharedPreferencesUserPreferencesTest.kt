package org.matchat.core.ui.prefs

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Read-after-write and defaults for the real SharedPreferences-backed impl
 *  (AGENTS.md §6). The fake's round-trip is covered separately in
 *  :core:testing, since it doesn't touch Android APIs. */
@RunWith(RobolectricTestRunner::class)
class SharedPreferencesUserPreferencesTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `defaults are light and green`() {
        val prefs = SharedPreferencesUserPreferences(context)
        assertEquals(ThemeMode.LIGHT, prefs.themeMode.value)
        assertEquals(AccentColor.GREEN, prefs.accentColor.value)
    }

    @Test
    fun `theme mode read-after-write, including a fresh instance`() = runTest {
        val prefs = SharedPreferencesUserPreferences(context)
        prefs.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, prefs.themeMode.value)

        val reloaded = SharedPreferencesUserPreferences(context)
        assertEquals(ThemeMode.DARK, reloaded.themeMode.value)
    }

    @Test
    fun `accent color read-after-write, including a fresh instance`() = runTest {
        val prefs = SharedPreferencesUserPreferences(context)
        prefs.setAccentColor(AccentColor.BLUE)
        assertEquals(AccentColor.BLUE, prefs.accentColor.value)

        val reloaded = SharedPreferencesUserPreferences(context)
        assertEquals(AccentColor.BLUE, reloaded.accentColor.value)
    }
}

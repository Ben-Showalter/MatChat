package org.matchat.core.ui.key

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit coverage for the key map. holdKey takes a raw keycode Int (inlined at
 * compile time, no Android runtime needed); map(event, swapped) takes a real
 * KeyEvent, so this class runs under Robolectric for that — same convention
 * as ThemeColorTest/SharedPreferencesUserPreferencesTest. Full dispatch of
 * live KeyEvents is exercised by the instrumented traversal suite
 * (PLAN.md §8.1).
 */
@RunWith(RobolectricTestRunner::class)
class KeyMapTest {
    @Test
    fun `hash and star hold map to their actions`() {
        assertEquals(LogicalKey.HASH_HOLD, KeyMap.holdKey(KeyEvent.KEYCODE_POUND))
        assertEquals(LogicalKey.STAR_HOLD, KeyMap.holdKey(KeyEvent.KEYCODE_STAR))
    }

    @Test
    fun `other codes are not hold actions`() {
        assertNull(KeyMap.holdKey(KeyEvent.KEYCODE_DPAD_CENTER))
    }

    // --- map(event, swapped) — Phase 6, UI improvement plan (docs/adr/0007) ---

    private fun down(keyCode: Int) = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)

    @Test
    fun `unswapped, LEFT-position codes map to SOFT_LEFT and RIGHT-position codes to SOFT_RIGHT`() {
        assertEquals(LogicalKey.SOFT_LEFT, KeyMap.map(down(KeyEvent.KEYCODE_SOFT_LEFT), swapped = false))
        assertEquals(LogicalKey.SOFT_LEFT, KeyMap.map(down(KeyEvent.KEYCODE_MENU), swapped = false))
        assertEquals(LogicalKey.SOFT_RIGHT, KeyMap.map(down(KeyEvent.KEYCODE_SOFT_RIGHT), swapped = false))
        assertEquals(LogicalKey.SOFT_RIGHT, KeyMap.map(down(KeyEvent.KEYCODE_BACK), swapped = false))
    }

    @Test
    fun `swapped, the two positional codes flip`() {
        assertEquals(LogicalKey.SOFT_RIGHT, KeyMap.map(down(KeyEvent.KEYCODE_SOFT_LEFT), swapped = true))
        assertEquals(LogicalKey.SOFT_RIGHT, KeyMap.map(down(KeyEvent.KEYCODE_MENU), swapped = true))
        assertEquals(LogicalKey.SOFT_LEFT, KeyMap.map(down(KeyEvent.KEYCODE_SOFT_RIGHT), swapped = true))
    }

    @Test
    fun `swapped, KEYCODE_BACK still means Back, never Options`() {
        // The confirmed bug this guards: a dedicated hardware Back key (distinct
        // from the two labeled positional softkeys) got swapped to Options along
        // with them. It must always resolve to SOFT_RIGHT (Back's LogicalKey).
        assertEquals(LogicalKey.SOFT_RIGHT, KeyMap.map(down(KeyEvent.KEYCODE_BACK), swapped = true))
        assertEquals(LogicalKey.SOFT_RIGHT, KeyMap.map(down(KeyEvent.KEYCODE_BACK), swapped = false))
    }

    @Test
    fun `swap never touches a non-softkey code`() {
        // Table test across every other mapped key: swapped must equal unswapped.
        // KEYCODE_BACK is deliberately not in this list — it's covered by its own
        // test above, and it's the one code that's exempt for a different reason
        // (always Back) than these (not a softkey at all).
        val codes = listOf(
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_CALL, KeyEvent.KEYCODE_ENDCALL,
            KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3,
            KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7,
            KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9,
        )
        codes.forEach { code ->
            assertEquals(
                "code $code should map the same regardless of swap",
                KeyMap.map(down(code), swapped = false),
                KeyMap.map(down(code), swapped = true),
            )
        }
    }

    @Test
    fun `an unmapped code stays null regardless of swap`() {
        assertNull(KeyMap.map(down(KeyEvent.KEYCODE_UNKNOWN), swapped = false))
        assertNull(KeyMap.map(down(KeyEvent.KEYCODE_UNKNOWN), swapped = true))
    }
}

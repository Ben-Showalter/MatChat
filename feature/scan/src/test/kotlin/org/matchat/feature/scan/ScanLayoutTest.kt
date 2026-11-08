package org.matchat.feature.scan

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ScanLayoutTest {

    @Test
    fun `a portrait phone fits a rotated 4x3 picture by width`() {
        // 640x480 turned upright is 480 wide x 640 tall: 240 wide -> 320 tall, area 240x250 -> height-limited.
        assertEquals(187 to 250, ScanLayout.fit(240, 250, 640, 480, rotated = true))
    }

    @Test
    fun `a landscape area fits an unrotated picture by height`() {
        assertEquals(213 to 160, ScanLayout.fit(320, 160, 640, 480, rotated = false))
    }

    @Test
    fun `unknown sizes fall back to the whole area`() {
        assertEquals(240 to 200, ScanLayout.fit(240, 200, 0, 0, rotated = true))
    }
}

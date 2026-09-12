package org.matchat.core.ui.media

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream

/** decodeAndCache round-crops real photo avatars — see class doc on
 *  [AvatarCache]. Needs a real Bitmap/Canvas, so this runs under Robolectric
 *  (same convention as KeyMapTest/ThemeColorTest). */
@RunWith(RobolectricTestRunner::class)
class AvatarCacheTest {

    @Test
    fun `a real photo is cropped to a circle, not left square`() {
        val square = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.RED)
        }
        val bitmap = AvatarCache.decodeAndCache("mxc://s/photo", square.toPng(), maxPx = 100)
        requireNotNull(bitmap)

        // A far corner sits outside the inscribed circle -> fully transparent.
        assertEquals(0, Color.alpha(bitmap.getPixel(0, 0)))
        // The center sits inside the circle -> keeps the source color.
        val center = bitmap.width / 2
        assertEquals(Color.RED, bitmap.getPixel(center, center))
    }

    private fun Bitmap.toPng(): ByteArray =
        ByteArrayOutputStream().also { compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
}

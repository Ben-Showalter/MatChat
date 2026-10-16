package org.matchat.core.ui.media

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream

/** decodeAndCache round-crops real photo avatars — see class doc on
 *  [AvatarCache]. Needs a real Bitmap/Canvas, so this runs under Robolectric
 *  (same convention as KeyMapTest/ThemeColorTest). */
// decodeAndCache really draws (Canvas.drawCircle with a shader), so it needs
// Robolectric's NATIVE graphics. The default legacy mode no-ops the draw, leaving
// the output fully transparent and failing the "centre keeps the colour"
// assertion. Scoped to this class; the module's logic-only Robolectric tests
// (KeyMapTest, ThemeColorTest) stay on the default mode.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
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

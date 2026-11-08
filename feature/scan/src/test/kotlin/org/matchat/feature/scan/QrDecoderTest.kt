package org.matchat.feature.scan

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** A QR code drawn into a camera-like luminance frame decodes; so does the
 *  same code light-on-dark (a phone screen in dark mode). */
class QrDecoderTest {

    private val text = "matchat:add?v=1&server=example.org&user=sam&pass=p%40ss"

    private fun frame(inverted: Boolean): Triple<ByteArray, Int, Int> {
        val size = 320
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val pixels = ByteArray(size * size) { i ->
            val dark = matrix.get(i % size, i / size) != inverted
            (if (dark) 20 else 235).toByte()
        }
        return Triple(pixels, size, size)
    }

    @Test
    fun `a QR code in the frame is read`() {
        val (pixels, w, h) = frame(inverted = false)
        assertEquals(text, QrDecoder.decode(pixels, w, h))
    }

    @Test
    fun `a light-on-dark code is read too`() {
        val (pixels, w, h) = frame(inverted = true)
        assertEquals(text, QrDecoder.decode(pixels, w, h))
    }

    @Test
    fun `a blank or short frame is nothing, not a crash`() {
        assertNull(QrDecoder.decode(ByteArray(100 * 100) { 120 }, 100, 100))
        assertNull(QrDecoder.decode(ByteArray(10), 100, 100))
    }
}

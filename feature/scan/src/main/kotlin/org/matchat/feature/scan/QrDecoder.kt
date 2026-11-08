package org.matchat.feature.scan

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

/**
 * Finds a QR code in one camera preview frame (NV21: the luminance plane comes
 * first, which is all ZXing needs). ZXing core, pure Java, offline — never ML
 * Kit, which needs Play services (docs/adr/0010). The frame is never stored.
 * Ported from BusyCall's QrDecoder.
 */
internal object QrDecoder {

    private val hints = mapOf(
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.CHARACTER_SET to "UTF-8",
    )

    /** The code's text, or null if there's no readable QR code in the frame. */
    fun decode(frame: ByteArray, width: Int, height: Int): String? {
        if (width <= 0 || height <= 0 || frame.size < width * height) return null
        val source = PlanarYUVLuminanceSource(frame, width, height, 0, 0, width, height, false)
        // Phone screens and some printers show the code light-on-dark: try it inverted too.
        return read(source) ?: read(source.invert())
    }

    private fun read(source: LuminanceSource): String? = try {
        QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source)), hints).text
    } catch (e: ReaderException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }
}

package org.matchat.feature.scan

/** Sizes the camera preview so its picture keeps its shape (never squashed).
 *  Pure, unit-tested. Ported from BusyCall's ScanLayout. */
internal object ScanLayout {

    /**
     * The largest size with the camera picture's aspect ratio that fits inside
     * the area, as (width, height). [rotated] is true when the picture is turned
     * 90° or 270° on screen (portrait phones), which swaps its width and height.
     * Falls back to the whole area when a size is unknown.
     */
    fun fit(areaW: Int, areaH: Int, previewW: Int, previewH: Int, rotated: Boolean): Pair<Int, Int> {
        if (areaW <= 0 || areaH <= 0 || previewW <= 0 || previewH <= 0) return areaW to areaH
        val picW = if (rotated) previewH else previewW
        val picH = if (rotated) previewW else previewH
        // Is the area relatively taller than the picture? Then its width limits the size.
        return if (areaW.toLong() * picH <= areaH.toLong() * picW) {
            areaW to (areaW.toLong() * picH / picW).toInt()
        } else {
            (areaH.toLong() * picW / picH).toInt() to areaH
        }
    }
}

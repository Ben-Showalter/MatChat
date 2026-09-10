package org.matchat.core.ui.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache

/**
 * Decode + in-memory cache for avatar bitmaps, shared by every feature that
 * shows an avatar (room list, timeline, Room Info) — features can't depend
 * on each other (AGENTS.md §2), so this lives in :core:ui instead. Callers
 * fetch bytes themselves (their own ViewModel -> MatrixSession.loadAvatar;
 * :core:ui cannot see :core:matrix) and hand them to [decodeAndCache]; the
 * same `mxc://` URL decodes once no matter how many rows show it, the way
 * feature/timeline's MediaFiles.decodeSampled does for inline images, just
 * cached (an avatar repeats across many rows; a message image doesn't).
 */
object AvatarCache {
    private val cache = LruCache<String, Bitmap>(MAX_ENTRIES)

    /** The already-decoded bitmap for [url], or null if it hasn't been
     *  fetched/decoded yet (or was evicted). */
    fun get(url: String): Bitmap? = cache.get(url)

    /** Decodes [bytes] with an inSampleSize so the result fits within
     *  [maxPx], caching under [url] before returning it. Returns the cached
     *  bitmap without re-decoding if [url] is already present. */
    fun decodeAndCache(url: String, bytes: ByteArray, maxPx: Int): Bitmap? {
        cache.get(url)?.let { return it }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        var w = bounds.outWidth
        var h = bounds.outHeight
        while (w / 2 >= maxPx || h / 2 >= maxPx) {
            sample *= 2
            w /= 2
            h /= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) }.getOrNull()
        if (bitmap != null) cache.put(url, bitmap)
        return bitmap
    }

    private const val MAX_ENTRIES = 200
}

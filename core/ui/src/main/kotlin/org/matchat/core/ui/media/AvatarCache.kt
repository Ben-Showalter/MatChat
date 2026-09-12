package org.matchat.core.ui.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
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
     *  [maxPx], round-crops it (avatars are always round, real photo or
     *  fallback alike — see [toCircular]), and caches the round result under
     *  [url] before returning it. Returns the cached bitmap without
     *  re-decoding if [url] is already present. */
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
        val decoded = runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) }.getOrNull()
        val bitmap = decoded?.let { toCircular(it) }
        if (bitmap != null) cache.put(url, bitmap)
        return bitmap
    }

    /** Crops [source] to a circle inscribed in its shorter side — a real
     *  avatar photo may be square (or any aspect ratio); every avatar in the
     *  app is round, fallback or photo alike, so this is applied once here
     *  rather than left to each ImageView to clip. Pixels outside the circle
     *  are fully transparent (ARGB_8888), matching how [fallback] already
     *  draws a bare circle rather than a square with rounded corners. */
    private fun toCircular(source: Bitmap): Bitmap {
        val size = minOf(source.width, source.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            // Center the source bitmap's longer dimension so the crop isn't
            // off-axis for a non-square photo (matches CENTER_CROP framing).
            val dx = (size - source.width) / 2f
            val dy = (size - source.height) / 2f
            val matrix = android.graphics.Matrix().apply { setTranslate(dx, dy) }
            setLocalMatrix(matrix)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)
        return output
    }

    /** The no-avatar fallback (colored circle + initial letter, AvatarFallback
     *  round) for [userId]/[name] — drawn once per user id, then cached, since
     *  the same user's fallback repeats across many rows just like a real
     *  decoded avatar does. Always drawn at one fixed size; every call site's
     *  ImageView scales it like any other bitmap, so there's no need to cache
     *  per-requested-size copies. */
    fun fallback(userId: String, name: String): Bitmap {
        cache.get(fallbackKey(userId))?.let { return it }
        val bitmap = Bitmap.createBitmap(FALLBACK_SIZE_PX, FALLBACK_SIZE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AvatarFallback.colorFor(userId) }
        val radius = FALLBACK_SIZE_PX / 2f
        canvas.drawCircle(radius, radius, radius, circlePaint)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = FALLBACK_SIZE_PX * 0.5f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val text = AvatarFallback.initial(name)
        val textY = radius - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(text, radius, textY, textPaint)
        cache.put(fallbackKey(userId), bitmap)
        return bitmap
    }

    private fun fallbackKey(userId: String) = "initial:$userId"

    private const val MAX_ENTRIES = 200
    private const val FALLBACK_SIZE_PX = 96
}

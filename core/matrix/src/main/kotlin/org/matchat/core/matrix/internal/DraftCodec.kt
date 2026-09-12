package org.matchat.core.matrix.internal

import org.json.JSONArray
import org.json.JSONObject
import org.matchat.core.matrix.Draft
import org.matchat.core.matrix.DraftAttachment
import org.matchat.core.model.MediaKind

/**
 * Encodes/decodes a [Draft] to/from the JSON string a single `SharedPreferences`
 * value holds. Same shape as [PinnedEventsContent] — a plain-`org.json` pure
 * object, no SDK call in here. Not unit-tested in this module for the same
 * reason [PinnedEventsContent.toJson] isn't (see that test class's own doc
 * comment): android.jar's `org.json` classes are stubs outside Robolectric,
 * which this module doesn't pull in, so a plain JUnit test calling into
 * `JSONObject`/`JSONArray` here would throw at runtime rather than assert
 * anything.
 */
internal object DraftCodec {
    private const val KEY_TEXT = "text"
    private const val KEY_ATTACHMENT = "attachment"
    private const val KEY_PATH = "path"
    private const val KEY_MIME_TYPE = "mimeType"
    private const val KEY_KIND = "kind"
    private const val KEY_DISPLAY_NAME = "displayName"
    private const val KEY_DURATION_MS = "durationMs"
    private const val KEY_WAVEFORM = "waveform"

    fun toJson(draft: Draft): String {
        val obj = JSONObject().put(KEY_TEXT, draft.text)
        draft.attachment?.let { a ->
            val attachmentJson = JSONObject()
                .put(KEY_PATH, a.path)
                .put(KEY_MIME_TYPE, a.mimeType)
                .put(KEY_KIND, a.kind.name)
                .put(KEY_DISPLAY_NAME, a.displayName)
            a.durationMs?.let { attachmentJson.put(KEY_DURATION_MS, it) }
            a.waveform?.let { attachmentJson.put(KEY_WAVEFORM, JSONArray(it)) }
            obj.put(KEY_ATTACHMENT, attachmentJson)
        }
        return obj.toString()
    }

    /** A corrupt or unrecognized-shape string (an old build's entry, on
     *  downgrade) is just treated as no draft — never a crash. */
    fun fromJson(json: String): Draft? = runCatching {
        val obj = JSONObject(json)
        val text = obj.optString(KEY_TEXT, "")
        val attachment = obj.optJSONObject(KEY_ATTACHMENT)?.let { a ->
            DraftAttachment(
                path = a.getString(KEY_PATH),
                mimeType = a.getString(KEY_MIME_TYPE),
                kind = MediaKind.valueOf(a.getString(KEY_KIND)),
                displayName = a.getString(KEY_DISPLAY_NAME),
                durationMs = if (a.has(KEY_DURATION_MS)) a.getLong(KEY_DURATION_MS) else null,
                waveform = a.optJSONArray(KEY_WAVEFORM)?.let { arr ->
                    List(arr.length()) { i -> arr.getDouble(i).toFloat() }
                },
            )
        }
        Draft(text, attachment)
    }.getOrNull()
}

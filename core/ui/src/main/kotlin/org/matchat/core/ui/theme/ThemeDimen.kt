package org.matchat.core.ui.theme

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes

/**
 * Resolves a `?attr/textSize*`/`?attr/avatarSize*`/`?attr/rowMinHeight*`
 * dimension attribute (attrs.xml) to its current pixel size, for code that
 * builds Views by hand (MenuSheet, TextPromptSheet, and the handful of
 * fragments with their own raw sp/px constants) — no layout to inflate, so
 * `?attr/...` doesn't resolve itself the way it does for an XML attribute.
 * Mirrors [themeColor]'s shape exactly, for a dimension instead of a color.
 */
fun Context.themeDimenPx(@AttrRes attrRes: Int): Float {
    val typedValue = TypedValue()
    theme.resolveAttribute(attrRes, typedValue, true)
    return typedValue.getDimension(resources.displayMetrics)
}

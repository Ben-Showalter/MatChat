package org.matchat.core.ui.theme

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes

/**
 * Resolves a `?attr/color*` theme attribute (attrs.xml) to its current color
 * int. Views inflated from XML resolve `?attr/...` automatically; code that
 * builds Views by hand (MenuSheet, TextPromptSheet — no layout to inflate)
 * must use this instead of `ContextCompat.getColor(context, R.color.foo)`,
 * which reads a fixed resource and ignores the active theme entirely.
 */
fun Context.themeColor(@AttrRes attrRes: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(attrRes, typedValue, true)
    return typedValue.data
}

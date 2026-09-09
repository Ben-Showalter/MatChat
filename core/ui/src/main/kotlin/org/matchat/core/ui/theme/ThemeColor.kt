package org.matchat.core.ui.theme

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat

/**
 * Resolves a `?attr/color*` theme attribute (attrs.xml) to its current color
 * int. Views inflated from XML resolve `?attr/...` automatically; code that
 * builds Views by hand (MenuSheet, TextPromptSheet — no layout to inflate)
 * must use this instead of `ContextCompat.getColor(context, R.color.foo)`,
 * which reads a fixed resource and ignores the active theme entirely.
 *
 * Some roles (e.g. colorTextOnFocus) point at a `<selector>` color-state-list
 * resource, not a plain `<color>` — for those, [TypedValue.data] after
 * [android.content.res.Resources.Theme.resolveAttribute] is not a color int
 * (it renders as invisible/transparent text, the bug this once was). Route
 * through [ContextCompat.getColor] whenever the attr resolved to an actual
 * resource, since it correctly flattens a color-state-list to its default
 * color as well as reading a plain color; fall back to the raw value only
 * for an attr with no backing resource (an inline value, not `@color/...`).
 */
fun Context.themeColor(@AttrRes attrRes: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(attrRes, typedValue, true)
    return if (typedValue.resourceId != 0) {
        ContextCompat.getColor(this, typedValue.resourceId)
    } else {
        typedValue.data
    }
}

package org.matchat.core.ui.menu

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.matchat.core.ui.R
import org.matchat.core.ui.theme.themeColor
import org.matchat.core.ui.theme.themeDimenPx

/** One row of a [MenuSheet]. [enabled] false renders greyed and is not focusable. */
data class MenuItem(
    val id: String,
    val label: CharSequence,
    val enabled: Boolean = true,
)

/**
 * The only menu construct in the app (ARCHITECTURE.md, UX-SPEC S11): a
 * bottom-anchored list of focusable rows, typically ~5, dismissed with
 * RIGHT/BACK. The menu *is* the options list, so the caller leaves its own
 * LEFT softkey blank while it is open. No touch-only dismiss — BACK always
 * closes it (AGENTS.md §9).
 *
 * The row list is wrapped in a height-capped ScrollView (Reactions round —
 * the 10-choice reaction picker doesn't fit a 320dp-tall screen at once);
 * a short menu's natural height stays under the cap, so this is invisible
 * for every existing ≤5-item menu — only a longer list actually scrolls.
 */
object MenuSheet {

    fun show(context: Context, items: List<MenuItem>, onSelect: (MenuItem) -> Unit): Dialog {
        // Confirmed bug: opening this from Options while compose_input (or any
        // EditText) has focus and the IME is showing left the menu invisible —
        // a plain Dialog can render behind an active IME window (a higher
        // z-order window type), so the menu was technically open, just hidden.
        // This has no text field of its own, so there's no reason to keep the
        // keyboard up while it's showing; hiding it first also resolves that.
        hideKeyboard(context)

        val list = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(context.themeColor(R.attr.colorSurfaceBright))
        }

        // A plain Dialog (not AppCompatDialog): the bottom-anchored, non-floating
        // menu theme is incompatible with AppCompat's decor requirements, and the
        // rows are plain views that need no AppCompat theming.
        val dialog = Dialog(context, R.style.Theme_MatChat_Menu)
        items.forEach { item ->
            list.addView(
                rowFor(context, item) {
                    onSelect(item)
                    dialog.dismiss()
                },
            )
        }
        val scroll = boundedScrollView(context).apply { addView(list) }
        dialog.setContentView(scroll)
        dialog.window?.apply {
            setGravity(Gravity.BOTTOM)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.setCancelable(true) // BACK dismisses; there is no touch-scrim dependence
        dialog.show()
        list.getChildAt(0)?.requestFocus()
        return dialog
    }

    /** A ScrollView that clamps its own measured height to a fraction of the
     *  screen instead of growing unbounded — plain ScrollView has no
     *  maxHeight attribute, so this overrides onMeasure to impose one. */
    private fun boundedScrollView(context: Context): ScrollView = object : ScrollView(context) {
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val maxHeight = (context.resources.displayMetrics.heightPixels * MAX_HEIGHT_FRACTION).toInt()
            val capped = View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST)
            super.onMeasure(widthMeasureSpec, capped)
        }
    }

    /** Best-effort: requires [context] to be (or wrap) the hosting Activity, which
     *  Fragment.requireContext() always is in practice. Silently no-ops otherwise
     *  or if nothing is currently focused — never worth crashing the menu over. */
    private fun hideKeyboard(context: Context) {
        val activity = context as? Activity ?: return
        val focused = activity.currentFocus ?: return
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(focused.windowToken, 0)
    }

    private fun rowFor(context: Context, item: MenuItem, onClick: () -> Unit): TextView = TextView(context).apply {
        text = item.label
        setTextSize(TypedValue.COMPLEX_UNIT_PX, context.themeDimenPx(R.attr.textSizeBody))
        setTextColor(context.themeColor(R.attr.colorTextOnFocus))
        minHeight = context.resources.getDimensionPixelSize(R.dimen.row_min_height_compact)
        gravity = Gravity.CENTER_VERTICAL
        val pad = context.resources.getDimensionPixelSize(R.dimen.content_pad)
        setPadding(pad, pad, pad, pad)
        isEnabled = item.enabled
        isFocusable = item.enabled
        isFocusableInTouchMode = false
        setBackgroundResource(R.drawable.focus_selector)
        if (item.enabled) setOnClickListener { onClick() }
    }

    private const val MAX_HEIGHT_FRACTION = 0.6 // leaves the title bar visible above it
}

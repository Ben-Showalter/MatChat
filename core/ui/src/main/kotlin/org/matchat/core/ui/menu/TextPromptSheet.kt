package org.matchat.core.ui.menu

import android.app.Dialog
import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import org.matchat.core.ui.R
import org.matchat.core.ui.focus.FocusEngine
import org.matchat.core.ui.theme.themeColor
import org.matchat.core.ui.theme.themeDimenPx

/**
 * The single text-entry construct (S12 room edits, message edit): a
 * bottom-anchored prompt with one focusable [EditText] and an OK row. CENTER/IME
 * "done" on the field confirms, as does the OK row; RIGHT/BACK cancels. Same plain
 * Dialog approach as [MenuSheet] (no AppCompat decor), so it works on a keypad.
 */
object TextPromptSheet {

    fun show(
        context: Context,
        title: CharSequence,
        initial: CharSequence = "",
        singleLine: Boolean = true,
        onConfirm: (String) -> Unit,
    ): Dialog {
        val pad = context.resources.getDimensionPixelSize(R.dimen.content_pad)
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(context.themeColor(R.attr.colorSurfaceBright))
            setPadding(pad, pad, pad, pad)
        }
        container.addView(
            TextView(context).apply {
                text = title
                setTextSize(TypedValue.COMPLEX_UNIT_PX, context.themeDimenPx(R.attr.textSizeLabel))
                setTextColor(context.themeColor(R.attr.colorTextSecondary))
            },
        )

        val field = EditText(context).apply {
            setText(initial)
            setSelection(text.length)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, context.themeDimenPx(R.attr.textSizeBody))
            setTextColor(context.themeColor(R.attr.colorTextPrimary))
            isSingleLine = singleLine
            imeOptions = EditorInfo.IME_ACTION_DONE
            isFocusableInTouchMode = true
        }
        container.addView(field)

        val dialog = Dialog(context, R.style.Theme_MatChat_Menu)
        val confirm = {
            onConfirm(field.text.toString())
            dialog.dismiss()
        }
        field.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                confirm()
                true
            } else {
                false
            }
        }

        val ok = TextView(context).apply {
            text = context.getString(R.string.prompt_ok)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, context.themeDimenPx(R.attr.textSizeBody))
            setTextColor(context.themeColor(R.attr.colorTextOnFocus))
            minHeight = context.resources.getDimensionPixelSize(R.dimen.row_min_height_compact)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, pad, 0, 0)
            isFocusable = true
            isFocusableInTouchMode = false
            setBackgroundResource(R.drawable.focus_selector)
            setOnClickListener { confirm() }
        }
        container.addView(ok)

        // Bug fix: a multi-line field's own ArrowKeyMovementMethod intercepts
        // DPAD_DOWN for in-text cursor movement before Android's focus search
        // ever gets a chance to move focus down to OK — from most cursor
        // positions the OK row was simply unreachable, so an edit had no way
        // to be submitted. This app's D-pad model treats DOWN as "move to the
        // next thing" everywhere else, never as in-field cursor navigation,
        // so unconditionally redirecting DOWN to OK (rather than only when
        // the movement method happens to decline it) is consistent, not a
        // narrow patch.
        field.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && event.action == KeyEvent.ACTION_DOWN) {
                ok.requestFocus()
                true
            } else {
                false
            }
        }

        dialog.setContentView(container)
        dialog.window?.apply {
            setGravity(Gravity.BOTTOM)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.setCancelable(true)
        dialog.show()
        FocusEngine.requestInitialFocus(field)
        return dialog
    }
}

package org.matchat.core.ui.menu

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import org.matchat.core.ui.R
import org.matchat.core.ui.theme.themeColor

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
                textSize = LABEL_SP
                setTextColor(context.themeColor(R.attr.colorTextSecondary))
            },
        )

        val field = EditText(context).apply {
            setText(initial)
            setSelection(text.length)
            textSize = BODY_SP
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
            if (actionId == EditorInfo.IME_ACTION_DONE) { confirm(); true } else false
        }

        container.addView(
            TextView(context).apply {
                text = context.getString(R.string.prompt_ok)
                textSize = BODY_SP
                setTextColor(context.themeColor(R.attr.colorTextOnFocus))
                minHeight = context.resources.getDimensionPixelSize(R.dimen.row_min_height_compact)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, pad, 0, 0)
                isFocusable = true
                isFocusableInTouchMode = false
                setBackgroundResource(R.drawable.focus_selector)
                setOnClickListener { confirm() }
            },
        )

        dialog.setContentView(container)
        dialog.window?.apply {
            setGravity(Gravity.BOTTOM)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.setCancelable(true)
        dialog.show()
        field.requestFocus()
        return dialog
    }

    private const val LABEL_SP = 14f
    private const val BODY_SP = 16f
}

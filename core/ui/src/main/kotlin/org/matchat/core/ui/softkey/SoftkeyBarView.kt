package org.matchat.core.ui.softkey

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import org.matchat.core.ui.R
import org.matchat.core.ui.databinding.ViewSoftkeyBarBinding
import org.matchat.core.ui.key.LogicalKey
import org.matchat.core.ui.theme.themeColor

/**
 * Renders the three softkey labels (UX-SPEC §1). A label may be empty (a screen
 * with no Options leaves LEFT blank) but the three cells always exist so the bar
 * never reflows.
 *
 * The cells are also tappable, dispatching the [LogicalKey] matching whatever
 * is currently displayed at that screen position (LEFT=Options, CENTRE=
 * activate, RIGHT=Back — or LEFT=Back/RIGHT=Options when [render]'s swapped
 * flag is set, Settings > Advanced, Phase 6 of the UI improvement plan: the
 * left/right cells' dispatched keys must track their mirrored labels, not
 * stay pinned to screen position, or tapping the cell that visibly reads
 * "Back" would fire Options — a confirmed bug during that phase). Real
 * feature phones have no touchscreen, so the taps are a no-op there and cost
 * nothing; on an emulator or a touch device they make the bar usable without
 * a D-pad. A blank cell is not tappable.
 */
class SoftkeyBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewSoftkeyBarBinding
    private var swapped = false

    /** Set by [SoftkeyFragment] to receive taps as logical keys. */
    var onKey: ((LogicalKey) -> Unit)? = null

    init {
        orientation = HORIZONTAL
        // Accent surface, not the inverse surface — matches the title bar
        // (view_chrome.xml) and the reference device's own SMS app (Phase 4
        // of the UI improvement plan); tracks the user's chosen accent
        // (Settings > Theme) automatically since it's an attr, not a fixed
        // color.
        setBackgroundColor(context.themeColor(R.attr.colorFocusAccent))
        binding = ViewSoftkeyBarBinding.inflate(android.view.LayoutInflater.from(context), this)
        isFocusable = false
        isFocusableInTouchMode = false
        // Reads `swapped` fresh on each tap (a var, not a value captured at
        // init time), so it always matches the labels render() last set.
        binding.softkeyLeft.setOnClickListener {
            onKey?.invoke(if (swapped) LogicalKey.SOFT_RIGHT else LogicalKey.SOFT_LEFT)
        }
        binding.softkeyCenter.setOnClickListener { onKey?.invoke(LogicalKey.CENTER) }
        binding.softkeyRight.setOnClickListener {
            onKey?.invoke(if (swapped) LogicalKey.SOFT_LEFT else LogicalKey.SOFT_RIGHT)
        }
    }

    fun render(left: CharSequence, center: CharSequence, right: CharSequence, swapped: Boolean = false) {
        this.swapped = swapped
        binding.softkeyLeft.text = left
        binding.softkeyCenter.text = center
        binding.softkeyRight.text = right
        // Only offer a tap target where there is a label.
        binding.softkeyLeft.isClickable = left.isNotEmpty()
        binding.softkeyCenter.isClickable = center.isNotEmpty()
        binding.softkeyRight.isClickable = right.isNotEmpty()
    }
}

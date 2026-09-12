package org.matchat.feature.timeline

import android.view.LayoutInflater
import android.view.View
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import org.junit.Rule
import org.junit.Test
import org.matchat.feature.timeline.databinding.ItemMessageBinding

/**
 * Screenshot coverage for the S9 message bubble at the reference viewport
 * (PLAN.md §8.2), mirroring RoomRowScreenshotTest. Rendered at 240x320 mdpi
 * portrait and 320x240 landscape (UX-SPEC S17), each at normal and largest
 * font scale, for both bubble sides (bindBubbleSide — the same function
 * TimelineAdapter binds, not a re-typed copy) and both focus states. Focus
 * is forced directly on the bubble's background Drawable via
 * android.R.attr.state_focused rather than View.requestFocus(), since the
 * latter depends on the test harness's touch-mode/window state — this row
 * isn't itself what's focusable (the outer row is; the bubble picks its
 * state up via duplicateParentState, AGENTS.md §4 note), so driving the
 * drawable state directly is the more reliable and more targeted check. A
 * diff is a review conversation; an unreviewed diff blocks.
 */
class MessageRowScreenshotTest {

    private val config = DeviceConfig(
        screenWidth = 240,
        screenHeight = 320,
        density = Density.MEDIUM,
        orientation = ScreenOrientation.PORTRAIT,
    )

    private val landscapeConfig = config.copy(
        screenWidth = 320,
        screenHeight = 240,
        orientation = ScreenOrientation.LANDSCAPE,
    )

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = config)

    private fun row(
        isOwn: Boolean,
        showSender: Boolean = false,
        focused: Boolean = false,
        body: String = "See you at six by the north gate.",
    ): View {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(paparazzi.context))
        binding.messageBody.text = body
        binding.messageTime.text = "3:42 PM ✓"
        if (showSender) {
            binding.messageSender.text = "Wayne"
            // Avatars round: the name (and its avatar) share one row,
            // messageSenderRow, gone by default in the layout — the TextView
            // itself no longer carries its own visibility.
            binding.messageSenderRow.visibility = View.VISIBLE
        }
        bindBubbleSide(binding.messageBubble, binding.messageTime, isOwn)
        if (focused) {
            binding.messageBubble.background.state = intArrayOf(android.R.attr.state_focused)
        }
        return binding.root
    }

    @Test
    fun messageRow_received() {
        paparazzi.snapshot(row(isOwn = false, showSender = true))
    }

    @Test
    fun messageRow_received_focused() {
        paparazzi.snapshot(row(isOwn = false, showSender = true, focused = true))
    }

    @Test
    fun messageRow_own() {
        paparazzi.snapshot(row(isOwn = true))
    }

    @Test
    fun messageRow_own_focused() {
        paparazzi.snapshot(row(isOwn = true, focused = true))
    }

    @Test
    fun messageRow_received_largestFont() {
        paparazzi.unsafeUpdateConfig(deviceConfig = config.copy(fontScale = 1.5f))
        paparazzi.snapshot(row(isOwn = false, showSender = true))
    }

    @Test
    fun messageRow_own_largestFont() {
        paparazzi.unsafeUpdateConfig(deviceConfig = config.copy(fontScale = 1.5f))
        paparazzi.snapshot(row(isOwn = true))
    }

    @Test
    fun messageRow_received_landscape() {
        paparazzi.unsafeUpdateConfig(deviceConfig = landscapeConfig)
        paparazzi.snapshot(row(isOwn = false, showSender = true))
    }

    @Test
    fun messageRow_own_landscape() {
        paparazzi.unsafeUpdateConfig(deviceConfig = landscapeConfig)
        paparazzi.snapshot(row(isOwn = true))
    }

    // Long-message round: every other case above uses the same one-line body,
    // leaving a real gap around a bubble many times taller than the viewport
    // (the actual on-device report — see TimelineAdapter's onRowKey). This
    // doesn't exercise the D-pad scroll-step behavior itself (no key-dispatch
    // harness exists in this repo), only that a long, multi-paragraph body
    // still inflates and renders at its full, unclamped height rather than
    // being silently truncated by some layout constraint.
    @Test
    fun messageRow_received_longBody() {
        paparazzi.snapshot(row(isOwn = false, showSender = true, body = LONG_BODY))
    }

    private companion object {
        val LONG_BODY = List(8) {
            "This is a long message meant to stretch well past the visible " +
                "list area, paragraph ${it + 1} of 8, so the bubble has to " +
                "grow far taller than the screen to hold it all."
        }.joinToString("\n\n")
    }
}

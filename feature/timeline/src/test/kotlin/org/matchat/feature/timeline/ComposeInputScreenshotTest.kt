package org.matchat.feature.timeline

import android.view.LayoutInflater
import android.view.View
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import org.junit.Rule
import org.junit.Test
import org.matchat.feature.timeline.databinding.FragmentTimelineBinding

/**
 * Regression guard for Phase 8 (UI improvement plan): compose_input's
 * inputType was missing textMultiLine, so it stayed single-line despite
 * maxLines already being correct — Android silently ignores maxLines on a
 * non-multiline EditText. Cap raised from 3 to 5 lines per explicit user
 * request, same day. Renders the whole S9 content band (not just the
 * EditText in isolation) so the strip's actual growth against the timeline
 * list above it is visible, at 1/3/5/6 lines of content and both font
 * scales.
 */
class ComposeInputScreenshotTest {

    private val config = DeviceConfig(
        screenWidth = 240,
        screenHeight = 320,
        density = Density.MEDIUM,
        orientation = ScreenOrientation.PORTRAIT,
    )

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = config)

    private fun screen(text: String): View {
        val binding = FragmentTimelineBinding.inflate(LayoutInflater.from(paparazzi.context))
        binding.composeInput.setText(text)
        return binding.root
    }

    @Test
    fun composeInput_oneLine() {
        paparazzi.snapshot(screen("On my way."))
    }

    @Test
    fun composeInput_threeLines() {
        paparazzi.snapshot(screen("On my way, should be there in about ten minutes or so."))
    }

    @Test
    fun composeInput_fiveLines_caps() {
        paparazzi.snapshot(
            screen(
                "On my way, should be there in about ten minutes or so, assuming the " +
                    "barn road isn't washed out again like last time — I'll text when I'm close.",
            ),
        )
    }

    @Test
    fun composeInput_sixLinesOfContent_stillCapsAtFive() {
        // More than fits at 5 lines: the box must not grow past its cap — the
        // timeline list above it keeps the remainder, scrolled by focus as usual.
        paparazzi.snapshot(
            screen(
                "On my way, should be there in about ten minutes or so, assuming the " +
                    "barn road isn't washed out again like last time — I'll text when I'm " +
                    "close, and let Ray know too since he's coming from the other direction.",
            ),
        )
    }

    @Test
    fun composeInput_fiveLines_largestFont() {
        paparazzi.unsafeUpdateConfig(deviceConfig = config.copy(fontScale = 1.5f))
        paparazzi.snapshot(screen("On my way, should be there in about ten minutes or so."))
    }
}

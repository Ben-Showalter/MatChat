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
 * maxLines="3" already being correct — Android silently ignores maxLines on
 * a non-multiline EditText. Renders the whole S9 content band (not just the
 * EditText in isolation) so the strip's actual growth against the timeline
 * list above it is visible, at 1/2/3 lines of content and both font scales.
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
    fun composeInput_twoLines() {
        paparazzi.snapshot(screen("On my way, should be there in about ten minutes or so."))
    }

    @Test
    fun composeInput_threeLines_caps_noMore() {
        paparazzi.snapshot(
            screen(
                "On my way, should be there in about ten minutes or so, " +
                    "assuming the barn road isn't washed out again like last time.",
            ),
        )
    }

    @Test
    fun composeInput_threeLines_largestFont() {
        paparazzi.unsafeUpdateConfig(deviceConfig = config.copy(fontScale = 1.5f))
        paparazzi.snapshot(screen("On my way, should be there in about ten minutes or so."))
    }
}

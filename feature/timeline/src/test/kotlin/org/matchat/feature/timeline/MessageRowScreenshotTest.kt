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
 * font scale, and for both bubble sides (bindBubbleSide — the same function
 * TimelineAdapter binds, not a re-typed copy of its logic). A diff is a
 * review conversation; an unreviewed diff blocks.
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

    private fun row(isOwn: Boolean, showSender: Boolean = false): View {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(paparazzi.context))
        binding.messageBody.text = "See you at six by the north gate."
        binding.messageTime.text = "3:42 PM ✓"
        if (showSender) {
            binding.messageSender.text = "Wayne"
            binding.messageSender.visibility = View.VISIBLE
        }
        bindBubbleSide(binding.messageBubble, binding.messageTime, isOwn)
        return binding.root
    }

    @Test
    fun messageRow_received() {
        paparazzi.snapshot(row(isOwn = false, showSender = true))
    }

    @Test
    fun messageRow_own() {
        paparazzi.snapshot(row(isOwn = true))
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
}

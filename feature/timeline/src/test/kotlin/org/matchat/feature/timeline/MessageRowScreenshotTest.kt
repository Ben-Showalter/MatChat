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
 * Screenshot coverage for the S9 message row at the reference viewport
 * (PLAN.md §8.2), mirroring RoomRowScreenshotTest. Rendered at 240x320 mdpi
 * portrait and 320x240 landscape (UX-SPEC S17), each at normal and largest
 * font scale, both with and without a visible sender name (own messages
 * never show one; other-sender rows do). A diff is a review conversation;
 * an unreviewed diff blocks.
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

    private fun row(showSender: Boolean): View {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(paparazzi.context))
        binding.messageBody.text = "See you at six by the north gate."
        binding.messageTime.text = "3:42 PM ✓"
        if (showSender) {
            binding.messageSender.text = "Wayne"
            binding.messageSender.visibility = View.VISIBLE
        }
        return binding.root
    }

    @Test
    fun messageRow_normal() {
        paparazzi.snapshot(row(showSender = true))
    }

    @Test
    fun messageRow_noSender_normal() {
        paparazzi.snapshot(row(showSender = false))
    }

    @Test
    fun messageRow_largestFont() {
        paparazzi.unsafeUpdateConfig(deviceConfig = config.copy(fontScale = 1.5f))
        paparazzi.snapshot(row(showSender = true))
    }

    @Test
    fun messageRow_landscape() {
        paparazzi.unsafeUpdateConfig(deviceConfig = landscapeConfig)
        paparazzi.snapshot(row(showSender = true))
    }

    @Test
    fun messageRow_landscape_largestFont() {
        paparazzi.unsafeUpdateConfig(deviceConfig = landscapeConfig.copy(fontScale = 1.5f))
        paparazzi.snapshot(row(showSender = true))
    }
}

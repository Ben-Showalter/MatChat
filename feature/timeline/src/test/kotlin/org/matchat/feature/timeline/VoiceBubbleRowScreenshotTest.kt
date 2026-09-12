package org.matchat.feature.timeline

import android.view.LayoutInflater
import android.view.View
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import org.junit.Rule
import org.junit.Test
import org.matchat.feature.timeline.databinding.ItemVoiceBubbleBinding

/**
 * Screenshot coverage for the Voice bubble round, mirroring
 * MessageRowScreenshotTest's own conventions (same reference viewport,
 * same bindBubbleSide the real adapter uses, same focus-via-drawable-state
 * approach). A diff is a review conversation; an unreviewed diff blocks.
 */
class VoiceBubbleRowScreenshotTest {

    private val config = DeviceConfig(
        screenWidth = 240,
        screenHeight = 320,
        density = Density.MEDIUM,
        orientation = ScreenOrientation.PORTRAIT,
    )

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = config)

    private val sampleWaveform = List(30) { i -> (i % 10) / 10f + 0.05f }
    private val flatWaveform = List(30) { 0.15f }

    private fun row(isOwn: Boolean, waveform: List<Float>, duration: String = "0:12"): View {
        val binding = ItemVoiceBubbleBinding.inflate(LayoutInflater.from(paparazzi.context))
        binding.voiceWaveform.setValues(waveform)
        binding.voiceDuration.text = duration
        binding.voiceTime.text = "3:42 PM"
        bindBubbleSide(binding.voiceBubble, binding.voiceTime, isOwn)
        return binding.root
    }

    @Test
    fun voiceBubble_received_realWaveform() {
        paparazzi.snapshot(row(isOwn = false, waveform = sampleWaveform))
    }

    @Test
    fun voiceBubble_own_realWaveform() {
        paparazzi.snapshot(row(isOwn = true, waveform = sampleWaveform))
    }

    @Test
    fun voiceBubble_flatWaveform() {
        // AUDIO (never carries real samples) or a VOICE message from a
        // client that omitted one — TimelineViewModel.FLAT_WAVEFORM.
        paparazzi.snapshot(row(isOwn = false, waveform = flatWaveform))
    }
}

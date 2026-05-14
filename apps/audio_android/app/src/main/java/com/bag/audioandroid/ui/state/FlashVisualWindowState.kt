package com.bag.audioandroid.ui.state

import com.bag.audioandroid.ui.screen.FlashSignalToneSegment

data class FlashVisualWindowSource(
    val timelineSegments: List<FlashSignalToneSegment>,
    val totalPcmSampleCount: Int,
) {
    init {
        require(totalPcmSampleCount >= 0) {
            "Flash visual source sample count must be non-negative."
        }
        require(timelineSegments.all { it.sampleCount > 0 }) {
            "Flash visual timeline segments must be positive."
        }
    }
}

data class FlashVisualWindowState(
    // Keep exact timeline segments for readout and semantic alignment.
    val segments: List<FlashSignalToneSegment> = emptyList(),
    // Canvas consumes only this budgeted list so frame cost stays bounded.
    val drawableSegments: List<FlashSignalToneSegment> = emptyList(),
    val startSample: Int = 0,
    val endSampleExclusive: Int = 0,
    val displayedSamples: Int = 0,
    val totalPcmSampleCount: Int = 0,
) {
    val available: Boolean
        get() = drawableSegments.isNotEmpty() && totalPcmSampleCount > 0

    fun isComfortablyInside(sample: Int): Boolean =
        available &&
            sample >= startSample &&
            sample < endSampleExclusive &&
            sample - startSample > RefreshMarginSamples &&
            endSampleExclusive - sample > RefreshMarginSamples

    companion object {
        private const val RefreshMarginSamples = 22_050
    }
}

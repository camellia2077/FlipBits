package com.bag.audioandroid.domain

import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption

data class GeneratedAudioMetadata(
    val version: Int = CURRENT_VERSION,
    val mode: TransportModeOption,
    val flashVoicingStyle: FlashVoicingStyleOption? = null,
    // ISO-8601 UTC timestamp for when this PCM payload was generated.
    val createdAtIsoUtc: String,
    val durationMs: Long,
    // Stored in metadata so saved-library screens can show the intended playback
    // sample rate without re-decoding the whole file body.
    val sampleRateHz: Int,
    val frameSamples: Int,
    val pcmSampleCount: Int,
    // UTF-8 payload size helps explain segmentation behavior and preserves the
    // original encode budget that produced this audio file.
    val payloadByteCount: Int,
    // Distinguishes user-typed text from catalog/sample-driven generations so
    // library surfaces can explain where a clip came from.
    val inputSourceKind: GeneratedAudioInputSourceKind,
    val segmentCount: Int = 1,
    val appVersion: String,
    val coreVersion: String,
    val segmentSampleCounts: List<Int> = emptyList(),
) {
    init {
        require(sampleRateHz > 0) { "sampleRateHz must be positive." }
        require(payloadByteCount >= 0) { "payloadByteCount must not be negative." }
        require(segmentCount > 0) { "segmentCount must be positive." }
        if (segmentCount > 1 || segmentSampleCounts.isNotEmpty()) {
            require(segmentSampleCounts.size == segmentCount) {
                "Segmented metadata must carry one sample-count boundary per segment."
            }
            require(segmentSampleCounts.all { it > 0 }) {
                "Segment sample counts must be positive."
            }
            require(segmentSampleCounts.sum() == pcmSampleCount) {
                "Segment sample counts must add up to the PCM sample count."
            }
        }
    }

    val isSegmented: Boolean
        get() = segmentCount > 1

    companion object {
        const val CURRENT_VERSION = 6
    }
}

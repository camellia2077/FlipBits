package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.domain.AudioVisualizationRegion
import com.bag.audioandroid.domain.AudioVisualizationTrack
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

internal data class WaveSliceModel(
    val startSample: Int,
    val endSample: Int,
    val peak: Float,
    val rms: Float,
    val brightness: Float,
    val coverage: Float,
    val region: AudioVisualizationRegion
)

internal fun buildNearFieldSlices(
    track: AudioVisualizationTrack,
    currentSample: Float,
    windowSampleCount: Int,
    targetSliceCount: Int
): List<WaveSliceModel> {
    if (track.frames.isEmpty()) {
        return emptyList()
    }

    val frames = track.frames
    val safeSliceCount = targetSliceCount.coerceAtLeast(1)
    val safeWindowSampleCount = windowSampleCount.coerceAtLeast(1)
    val pastWindowSamples = safeWindowSampleCount * VisualizationPlayheadAnchorRatio
    val windowStart = currentSample - pastWindowSamples
    val slotSampleWidth = safeWindowSampleCount.toFloat() / safeSliceCount.toFloat()
    val slices = ArrayList<WaveSliceModel>(safeSliceCount)

    var frameIndex = 0
    for (sliceIndex in 0 until safeSliceCount) {
        val slotStart = windowStart + slotSampleWidth * sliceIndex.toFloat()
        val slotEnd = slotStart + slotSampleWidth

        while (frameIndex < frames.size) {
            val frame = frames[frameIndex]
            val frameEnd = frame.sampleOffset + frame.sampleCount
            if (frameEnd > slotStart) {
                break
            }
            frameIndex += 1
        }

        val regionScores = mutableMapOf<AudioVisualizationRegion, Float>()
        var peak = 0f
        var rmsWeightedSum = 0f
        var brightnessWeightedSum = 0f
        var coveredSamples = 0f

        if (slotEnd > 0f && slotStart < track.totalSamples.toFloat()) {
            var scanIndex = frameIndex
            while (scanIndex < frames.size) {
                val frame = frames[scanIndex]
                val frameStart = frame.sampleOffset.toFloat()
                val frameEnd = (frame.sampleOffset + frame.sampleCount).toFloat()
                if (frameStart >= slotEnd) {
                    break
                }
                val overlapStart = max(slotStart, frameStart)
                val overlapEnd = min(slotEnd, frameEnd)
                val overlap = (overlapEnd - overlapStart).coerceAtLeast(0f)
                if (overlap > 0f) {
                    peak = max(peak, frame.peak)
                    rmsWeightedSum += frame.rms * overlap
                    brightnessWeightedSum += frame.brightness * overlap
                    coveredSamples += overlap
                    regionScores[frame.region] = (regionScores[frame.region] ?: 0f) + overlap
                }
                scanIndex += 1
            }
        }

        val dominantRegion = regionScores.maxByOrNull { it.value }?.key ?: AudioVisualizationRegion.Unknown
        val slotSpan = (slotEnd - slotStart).coerceAtLeast(1f)
        slices += WaveSliceModel(
            startSample = floor(slotStart.toDouble()).toInt(),
            endSample = ceil(slotEnd.toDouble()).toInt(),
            peak = peak.coerceIn(0f, 1f),
            rms = if (coveredSamples > 0f) (rmsWeightedSum / coveredSamples).coerceIn(0f, 1f) else 0f,
            brightness = if (coveredSamples > 0f) (brightnessWeightedSum / coveredSamples).coerceIn(0f, 1f) else 0f,
            coverage = (coveredSamples / slotSpan).coerceIn(0f, 1f),
            region = dominantRegion
        )
    }
    return slices
}

internal const val VisualizationPlayheadAnchorRatio = 0.40f
internal const val VisualizationMinSliceCount = 40
internal const val VisualizationMaxSliceCount = 72

package com.bag.audioandroid.audio

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt

internal data class RenderedPlaybackTimeline(
    val sourceStartSamples: Int,
    val sourceTotalSamples: Int,
    val renderedTotalSamples: Int,
    val sourceSamplesPerRenderedSample: Double,
    val segments: List<RenderedPlaybackTimelineSegment> = emptyList(),
) {
    fun sourceProgress(renderedSamples: Int): Int {
        if (segments.isNotEmpty()) {
            return sourceProgressForSegmentedTimeline(renderedSamples)
        }
        if (renderedTotalSamples <= 0) {
            return sourceStartSamples.coerceIn(0, sourceTotalSamples)
        }
        if (renderedSamples >= renderedTotalSamples) {
            return sourceTotalSamples
        }
        val sourceOffset = (renderedSamples.coerceAtLeast(0).toDouble() * sourceSamplesPerRenderedSample).roundToInt()
        return (sourceStartSamples + sourceOffset).coerceIn(0, sourceTotalSamples)
    }

    fun renderedPositionForSource(sourceSamples: Int): Int {
        if (segments.isNotEmpty()) {
            return renderedPositionForSegmentedTimeline(sourceSamples)
        }
        if (sourceSamples <= sourceStartSamples || sourceSamplesPerRenderedSample <= 0.0) {
            return 0
        }
        if (sourceSamples >= sourceTotalSamples) {
            return renderedTotalSamples
        }
        return ((sourceSamples - sourceStartSamples).toDouble() / sourceSamplesPerRenderedSample)
            .roundToInt()
            .coerceIn(0, renderedTotalSamples)
    }

    private fun sourceProgressForSegmentedTimeline(renderedSamples: Int): Int {
        if (renderedTotalSamples <= 0) {
            return sourceStartSamples.coerceIn(0, sourceTotalSamples)
        }
        if (renderedSamples >= renderedTotalSamples) {
            return sourceTotalSamples
        }
        val clampedRendered = renderedSamples.coerceAtLeast(0)
        val segment =
            segments.firstOrNull { clampedRendered >= it.renderedStartSamples && clampedRendered < it.renderedEndSamples }
                ?: segments.lastOrNull { clampedRendered >= it.renderedStartSamples }
                ?: return sourceStartSamples.coerceIn(0, sourceTotalSamples)
        if (segment.renderedSampleCount <= 0 || segment.sourceSampleCount <= 0) {
            return segment.sourceStartSamples.coerceIn(0, sourceTotalSamples)
        }
        val segmentRenderedOffset = clampedRendered - segment.renderedStartSamples
        val sourceOffset =
            (segmentRenderedOffset.toDouble() * segment.sourceSampleCount.toDouble() / segment.renderedSampleCount.toDouble())
                .roundToInt()
        return (segment.sourceStartSamples + sourceOffset).coerceIn(0, sourceTotalSamples)
    }

    private fun renderedPositionForSegmentedTimeline(sourceSamples: Int): Int {
        if (sourceSamples <= sourceStartSamples) {
            return 0
        }
        if (sourceSamples >= sourceTotalSamples) {
            return renderedTotalSamples
        }
        val segment =
            segments.firstOrNull { sourceSamples >= it.sourceStartSamples && sourceSamples < it.sourceEndSamples }
                ?: segments.lastOrNull { sourceSamples >= it.sourceStartSamples }
                ?: return 0
        if (segment.sourceSampleCount <= 0 || segment.renderedSampleCount <= 0) {
            return segment.renderedStartSamples.coerceIn(0, renderedTotalSamples)
        }
        val sourceOffset = sourceSamples - segment.sourceStartSamples
        return (
            segment.renderedStartSamples.toDouble() +
                sourceOffset.toDouble() * segment.renderedSampleCount.toDouble() / segment.sourceSampleCount.toDouble()
        ).roundToInt().coerceIn(0, renderedTotalSamples)
    }
}

internal data class RenderedPlaybackTimelineSegment(
    val sourceStartSamples: Int,
    val sourceSampleCount: Int,
    val renderedStartSamples: Int,
    val renderedSampleCount: Int,
) {
    val sourceEndSamples: Int
        get() = sourceStartSamples + sourceSampleCount

    val renderedEndSamples: Int
        get() = renderedStartSamples + renderedSampleCount
}

internal data class RenderedSpeedAdjustedPcm(
    val pcm: ShortArray,
    val timeline: RenderedPlaybackTimeline,
)

internal fun speedAdjustedRenderedSampleCount(
    sourceSampleCount: Int,
    playbackSpeed: Float,
): Int {
    if (sourceSampleCount <= 0) {
        return 0
    }
    val resolvedSpeed = coercePlaybackSpeed(playbackSpeed)
    return ceil(sourceSampleCount.toDouble() / resolvedSpeed.toDouble()).toInt().coerceAtLeast(1)
}

internal fun shouldStreamSpeedAdjustedPcm(
    sourceSampleCount: Int,
    playbackSpeed: Float,
): Boolean = speedAdjustedRenderedSampleCount(sourceSampleCount, playbackSpeed) > MaxPreRenderedSpeedAdjustedSamples

internal fun renderSpeedAdjustedPcm(
    sourcePcm: ShortArray,
    sourceStartSamples: Int,
    sourceTotalSamples: Int,
    playbackSpeed: Float,
    sampleRateHz: Int,
): RenderedSpeedAdjustedPcm {
    val sourceRemaining = (sourceTotalSamples - sourceStartSamples).coerceAtLeast(0)
    if (sourceRemaining <= 0 || sourcePcm.isEmpty()) {
        return RenderedSpeedAdjustedPcm(
            pcm = ShortArray(0),
            timeline =
                RenderedPlaybackTimeline(
                    sourceStartSamples = sourceStartSamples,
                    sourceTotalSamples = sourceTotalSamples,
                    renderedTotalSamples = 0,
                    sourceSamplesPerRenderedSample = 1.0,
                ),
        )
    }
    val resolvedSpeed = coercePlaybackSpeed(playbackSpeed)
    val renderedLength = speedAdjustedRenderedSampleCount(sourcePcm.size, resolvedSpeed)
    val renderedPcm =
        renderSpeedAdjustedPcmWithOverlapAdd(
            sourcePcm = sourcePcm,
            outputLength = renderedLength,
            playbackSpeed = resolvedSpeed,
            sampleRateHz = sampleRateHz,
        )
    val sourcePerRendered =
        if (renderedPcm.isNotEmpty()) {
            sourceRemaining.toDouble() / renderedPcm.size.toDouble()
        } else {
            1.0
        }
    return RenderedSpeedAdjustedPcm(
        pcm = renderedPcm,
        timeline =
            RenderedPlaybackTimeline(
                sourceStartSamples = sourceStartSamples,
                sourceTotalSamples = sourceTotalSamples,
                renderedTotalSamples = renderedPcm.size,
                sourceSamplesPerRenderedSample = sourcePerRendered,
            ),
    )
}

internal fun renderSpeedAdjustedPcmChunk(
    sourcePcm: ShortArray,
    outputStartSamples: Int,
    outputSampleCount: Int,
    playbackSpeed: Float,
    sampleRateHz: Int,
): ShortArray {
    if (outputSampleCount <= 0) {
        return ShortArray(0)
    }
    val renderedTotalSamples = speedAdjustedRenderedSampleCount(sourcePcm.size, playbackSpeed)
    val clampedStart = outputStartSamples.coerceIn(0, renderedTotalSamples)
    val clampedCount = outputSampleCount.coerceAtMost(renderedTotalSamples - clampedStart)
    if (clampedCount <= 0) {
        return ShortArray(0)
    }
    return renderSpeedAdjustedPcmWithOverlapAdd(
        sourcePcm = sourcePcm,
        outputStartSamples = clampedStart,
        outputLength = clampedCount,
        playbackSpeed = coercePlaybackSpeed(playbackSpeed),
        sampleRateHz = sampleRateHz,
    )
}

private fun renderSpeedAdjustedPcmWithOverlapAdd(
    sourcePcm: ShortArray,
    outputLength: Int,
    playbackSpeed: Float,
    sampleRateHz: Int,
    outputStartSamples: Int = 0,
): ShortArray {
    if (outputLength <= 0) {
        return ShortArray(0)
    }
    if (sourcePcm.size == 1) {
        return ShortArray(outputLength) { sourcePcm[0] }
    }
    val windowSize = speedAdjustedRenderWindowSize(sampleRateHz, sourcePcm.size)
    val synthesisHop = (windowSize / 2).coerceAtLeast(1)
    val analysisHop = (synthesisHop.toFloat() * playbackSpeed).roundToInt().coerceAtLeast(1)
    val output = FloatArray(outputLength)
    val weights = FloatArray(outputLength)
    val window = hannWindow(windowSize)
    val firstWindowIndex = Math.floorDiv(outputStartSamples - windowSize + 1, synthesisHop).coerceAtLeast(0)
    var windowNumber = firstWindowIndex
    var outputOffset = windowNumber * synthesisHop
    while (outputOffset < outputStartSamples + outputLength) {
        val sourceOffset = (windowNumber * analysisHop).coerceAtMost(sourcePcm.lastIndex)
        val localStart = (outputStartSamples - outputOffset).coerceAtLeast(0)
        val localEnd = (outputStartSamples + outputLength - outputOffset).coerceAtMost(windowSize)
        var windowIndex = localStart
        while (windowIndex < localEnd) {
            val targetIndex = outputOffset + windowIndex - outputStartSamples
            val sourceIndex = (sourceOffset + windowIndex).coerceAtMost(sourcePcm.lastIndex)
            val weight = window[windowIndex]
            output[targetIndex] += sourcePcm[sourceIndex].toFloat() * weight
            weights[targetIndex] += weight
            windowIndex += 1
        }
        windowNumber += 1
        outputOffset += synthesisHop
    }
    return ShortArray(outputLength) { index ->
        val weight = weights[index]
        val absoluteIndex = outputStartSamples + index
        val sample =
            if (weight > MinimumRenderWeight) {
                output[index] / weight
            } else {
                sourcePcm[(absoluteIndex.toDouble() * playbackSpeed.toDouble()).roundToInt().coerceIn(0, sourcePcm.lastIndex)]
                    .toFloat()
            }
        sample.roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
}

private fun speedAdjustedRenderWindowSize(
    sampleRateHz: Int,
    sourceSampleCount: Int,
): Int {
    val target = (sampleRateHz.coerceAtLeast(DefaultSampleRateHz).toDouble() * SpeedAdjustedRenderWindowSeconds).roundToInt()
    val evenTarget = if (target % 2 == 0) target else target + 1
    return evenTarget
        .coerceIn(MinSpeedAdjustedRenderWindowSamples, MaxSpeedAdjustedRenderWindowSamples)
        .coerceAtMost(sourceSampleCount.coerceAtLeast(2))
        .let { if (it % 2 == 0) it else (it - 1).coerceAtLeast(2) }
}

private fun hannWindow(size: Int): FloatArray {
    if (size <= 1) {
        return FloatArray(size) { 1f }
    }
    return FloatArray(size) { index ->
        (0.5 - 0.5 * cos((2.0 * PI * index.toDouble()) / (size - 1).toDouble())).toFloat()
    }
}

private const val SpeedAdjustedRenderWindowSeconds = 0.030
private const val MaxPreRenderedSpeedAdjustedSamples = 2_000_000
private const val MinSpeedAdjustedRenderWindowSamples = 256
private const val MaxSpeedAdjustedRenderWindowSamples = 2048
private const val DefaultSampleRateHz = 48_000
private const val MinimumRenderWeight = 0.000001f

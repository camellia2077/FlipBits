package com.bag.audioandroid.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

internal fun shouldUseFlashSpeedAdjustedPcm(
    playbackSpeed: Float,
    context: PlaybackRenderContext,
): Boolean =
    playbackSpeed < PlaybackSpeedNormal &&
        context.isFlashMode &&
        context.followData.followAvailable &&
        context.followData.binaryGroupTimeline.isNotEmpty()

internal fun renderFlashSpeedAdjustedPcm(
    sourcePcm: ShortArray,
    sourceStartSamples: Int,
    sourceTotalSamples: Int,
    playbackSpeed: Float,
    sampleRateHz: Int,
    context: PlaybackRenderContext,
): RenderedSpeedAdjustedPcm? {
    val timeline =
        buildFlashSpeedAdjustedTimeline(
            sourceStartSamples = sourceStartSamples,
            sourceTotalSamples = sourceTotalSamples,
            playbackSpeed = playbackSpeed,
            context = context,
        ) ?: return null
    val pcm =
        renderFlashSpeedAdjustedPcmChunk(
            sourcePcm = sourcePcm,
            sourceStartSamples = sourceStartSamples,
            sourceTotalSamples = sourceTotalSamples,
            outputStartSamples = 0,
            outputSampleCount = timeline.renderedTotalSamples,
            playbackSpeed = playbackSpeed,
            sampleRateHz = sampleRateHz,
            context = context,
        )
    return RenderedSpeedAdjustedPcm(pcm = pcm, timeline = timeline)
}

internal fun renderFlashSpeedAdjustedPcmChunk(
    sourcePcm: ShortArray,
    sourceStartSamples: Int,
    sourceTotalSamples: Int,
    outputStartSamples: Int,
    outputSampleCount: Int,
    playbackSpeed: Float,
    sampleRateHz: Int,
    context: PlaybackRenderContext,
): ShortArray {
    if (outputSampleCount <= 0 || sourcePcm.isEmpty()) {
        return ShortArray(0)
    }
    val timeline =
        buildFlashSpeedAdjustedTimeline(
            sourceStartSamples = sourceStartSamples,
            sourceTotalSamples = sourceTotalSamples,
            playbackSpeed = playbackSpeed,
            context = context,
        ) ?: return ShortArray(0)
    val outputStart = outputStartSamples.coerceIn(0, timeline.renderedTotalSamples)
    val outputEnd = (outputStart + outputSampleCount).coerceAtMost(timeline.renderedTotalSamples)
    if (outputEnd <= outputStart) {
        return ShortArray(0)
    }
    val output = ShortArray(outputEnd - outputStart)
    val fadeSamples = (sampleRateHz.toDouble() * FlashBoundaryFadeSeconds).roundToInt().coerceAtLeast(1)
    timeline.segments.forEachIndexed { index, segment ->
        if (segment.sourceSampleCount <= 0 || segment.renderedSampleCount <= 0) {
            return@forEachIndexed
        }
        val overlapStart = maxOf(outputStart, segment.renderedStartSamples)
        val overlapEnd = minOf(outputEnd, segment.renderedEndSamples)
        if (overlapEnd <= overlapStart) {
            return@forEachIndexed
        }
        val fadeIn = timeline.segments.getOrNull(index - 1)?.sourceSampleCount == 0
        val fadeOut = timeline.segments.getOrNull(index + 1)?.sourceSampleCount == 0
        var renderedIndex = overlapStart
        while (renderedIndex < overlapEnd) {
            val segmentOffset = renderedIndex - segment.renderedStartSamples
            val sourceIndex = segment.sourceStartSamples + segmentOffset - sourceStartSamples
            if (sourceIndex in sourcePcm.indices) {
                val gain =
                    flashBoundaryGain(
                        segmentOffset = segmentOffset,
                        segmentSamples = segment.renderedSampleCount,
                        fadeSamples = fadeSamples,
                        fadeIn = fadeIn,
                        fadeOut = fadeOut,
                    )
                output[renderedIndex - outputStart] =
                    (sourcePcm[sourceIndex].toDouble() * gain)
                        .roundToInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        .toShort()
            }
            renderedIndex += 1
        }
    }
    return output
}

internal fun buildFlashSpeedAdjustedTimeline(
    sourceStartSamples: Int,
    sourceTotalSamples: Int,
    playbackSpeed: Float,
    context: PlaybackRenderContext,
): RenderedPlaybackTimeline? {
    if (!shouldUseFlashSpeedAdjustedPcm(playbackSpeed, context)) {
        return null
    }
    val sourceStart = sourceStartSamples.coerceIn(0, sourceTotalSamples)
    val sourceRemaining = (sourceTotalSamples - sourceStart).coerceAtLeast(0)
    if (sourceRemaining <= 0) {
        return null
    }
    val renderedTotalSamples = speedAdjustedRenderedSampleCount(sourceRemaining, playbackSpeed)
    val extraSamples = (renderedTotalSamples - sourceRemaining).coerceAtLeast(0)
    // Flash slow playback preserves each binary group's original carrier PCM; the extra duration is inserted between groups.
    val clippedRanges =
        context.followData.binaryGroupTimeline.mapNotNull { entry ->
            val start = maxOf(entry.startSample, sourceStart)
            val end = minOf(entry.startSample + entry.sampleCount, sourceTotalSamples)
            if (end > start) {
                start to end
            } else {
                null
            }
        }
    val insertionRanges =
        clippedRanges.sortedBy { it.first }.fold(mutableListOf<Pair<Int, Int>>()) { ranges, range ->
            val previous = ranges.lastOrNull()
            if (previous != null && range.first < previous.second) {
                ranges[ranges.lastIndex] = previous.first to maxOf(previous.second, range.second)
            } else {
                ranges += range
            }
            ranges
        }
    if (insertionRanges.isEmpty()) {
        return null
    }

    val segments = ArrayList<RenderedPlaybackTimelineSegment>(insertionRanges.size * 2 + 2)
    var sourceCursor = sourceStart
    var renderedCursor = 0
    var remainingExtra = extraSamples
    insertionRanges.forEachIndexed { index, range ->
        val copyEnd = range.second.coerceAtLeast(sourceCursor)
        if (copyEnd > sourceCursor) {
            val copySamples = copyEnd - sourceCursor
            segments +=
                RenderedPlaybackTimelineSegment(
                    sourceStartSamples = sourceCursor,
                    sourceSampleCount = copySamples,
                    renderedStartSamples = renderedCursor,
                    renderedSampleCount = copySamples,
                )
            renderedCursor += copySamples
            sourceCursor = copyEnd
        }
        val insertionCount = insertionRanges.size - index
        val insertSamples =
            if (remainingExtra > 0 && insertionCount > 0) {
                (remainingExtra + insertionCount - 1) / insertionCount
            } else {
                0
            }
        if (insertSamples > 0) {
            segments +=
                RenderedPlaybackTimelineSegment(
                    sourceStartSamples = sourceCursor,
                    sourceSampleCount = 0,
                    renderedStartSamples = renderedCursor,
                    renderedSampleCount = insertSamples,
                )
            renderedCursor += insertSamples
            remainingExtra -= insertSamples
        }
    }
    if (sourceCursor < sourceTotalSamples) {
        val copySamples = sourceTotalSamples - sourceCursor
        segments +=
            RenderedPlaybackTimelineSegment(
                sourceStartSamples = sourceCursor,
                sourceSampleCount = copySamples,
                renderedStartSamples = renderedCursor,
                renderedSampleCount = copySamples,
            )
        renderedCursor += copySamples
    }
    return RenderedPlaybackTimeline(
        sourceStartSamples = sourceStart,
        sourceTotalSamples = sourceTotalSamples,
        renderedTotalSamples = renderedCursor,
        sourceSamplesPerRenderedSample = sourceRemaining.toDouble() / renderedCursor.coerceAtLeast(1).toDouble(),
        segments = segments,
    )
}

private fun flashBoundaryGain(
    segmentOffset: Int,
    segmentSamples: Int,
    fadeSamples: Int,
    fadeIn: Boolean,
    fadeOut: Boolean,
): Double {
    var gain = 1.0
    val boundedFadeSamples = fadeSamples.coerceAtMost(segmentSamples / 2).coerceAtLeast(1)
    if (fadeIn && segmentOffset < boundedFadeSamples) {
        val ratio = segmentOffset.toDouble() / boundedFadeSamples.toDouble()
        gain = minOf(gain, 0.5 - 0.5 * cos(PI * ratio))
    }
    val samplesFromEnd = segmentSamples - segmentOffset - 1
    if (fadeOut && samplesFromEnd < boundedFadeSamples) {
        val ratio = samplesFromEnd.toDouble() / boundedFadeSamples.toDouble()
        gain = minOf(gain, 0.5 - 0.5 * cos(PI * ratio))
    }
    return gain
}

private const val FlashBoundaryFadeSeconds = 0.003

package com.bag.audioandroid.audio

import com.bag.audioandroid.domain.PayloadFollowBinaryGroupTimelineEntry
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal fun canRenderMiniCwSpeedAdjustedPcm(context: PlaybackRenderContext): Boolean =
    context.isMiniMode &&
        context.frameSamples > 0 &&
        context.totalSamples > 0 &&
        context.followData.followAvailable &&
        context.followData.binaryGroupTimeline.any { it.sampleCount > 0 }

internal fun miniCwSpeedAdjustedRenderedSampleCount(
    sourceStartSamples: Int,
    sourceTotalSamples: Int,
    playbackSpeed: Float,
): Int {
    val sourceRemaining = (sourceTotalSamples - sourceStartSamples).coerceAtLeast(0)
    if (sourceRemaining <= 0) {
        return 0
    }
    val resolvedSpeed = coercePlaybackSpeed(playbackSpeed)
    return (sourceRemaining.toDouble() / resolvedSpeed.toDouble()).roundToInt().coerceAtLeast(1)
}

internal fun renderMiniCwSpeedAdjustedPcm(
    sourceStartSamples: Int,
    sourceTotalSamples: Int,
    playbackSpeed: Float,
    sampleRateHz: Int,
    context: PlaybackRenderContext,
): RenderedSpeedAdjustedPcm? {
    val sourceRemaining = (sourceTotalSamples - sourceStartSamples).coerceAtLeast(0)
    if (sourceRemaining <= 0 || !canRenderMiniCwSpeedAdjustedPcm(context)) {
        return null
    }
    val segments =
        buildMiniCwPlaybackSegments(
            sourceStartSamples = sourceStartSamples,
            sourceTotalSamples = sourceTotalSamples,
            playbackSpeed = playbackSpeed,
            toneTimeline = context.followData.binaryGroupTimeline,
        )
    if (segments.none { it.isTone }) {
        return null
    }
    val renderedLength = segments.sumOf { it.renderedSampleCount }
    if (renderedLength <= 0) {
        return null
    }
    return RenderedSpeedAdjustedPcm(
        pcm =
            renderMiniCwSpeedAdjustedPcmChunk(
                sourceStartSamples = sourceStartSamples,
                sourceTotalSamples = sourceTotalSamples,
                outputStartSamples = 0,
                outputSampleCount = renderedLength,
                playbackSpeed = playbackSpeed,
                sampleRateHz = sampleRateHz,
                context = context,
            ),
        timeline =
            RenderedPlaybackTimeline(
                sourceStartSamples = sourceStartSamples,
                sourceTotalSamples = sourceTotalSamples,
                renderedTotalSamples = renderedLength,
                sourceSamplesPerRenderedSample = sourceRemaining.toDouble() / renderedLength.toDouble(),
                segments = segments.map(MiniCwPlaybackSegment::toTimelineSegment),
            ),
    )
}

internal fun buildMiniCwSpeedAdjustedTimeline(
    sourceStartSamples: Int,
    sourceTotalSamples: Int,
    playbackSpeed: Float,
    context: PlaybackRenderContext,
): RenderedPlaybackTimeline? {
    val sourceRemaining = (sourceTotalSamples - sourceStartSamples).coerceAtLeast(0)
    if (sourceRemaining <= 0 || !canRenderMiniCwSpeedAdjustedPcm(context)) {
        return null
    }
    val segments =
        buildMiniCwPlaybackSegments(
            sourceStartSamples = sourceStartSamples,
            sourceTotalSamples = sourceTotalSamples,
            playbackSpeed = playbackSpeed,
            toneTimeline = context.followData.binaryGroupTimeline,
        )
    if (segments.none { it.isTone }) {
        return null
    }
    val renderedLength = segments.sumOf { it.renderedSampleCount }
    if (renderedLength <= 0) {
        return null
    }
    return RenderedPlaybackTimeline(
        sourceStartSamples = sourceStartSamples,
        sourceTotalSamples = sourceTotalSamples,
        renderedTotalSamples = renderedLength,
        sourceSamplesPerRenderedSample = sourceRemaining.toDouble() / renderedLength.toDouble(),
        segments = segments.map(MiniCwPlaybackSegment::toTimelineSegment),
    )
}

internal fun renderMiniCwSpeedAdjustedPcmChunk(
    sourceStartSamples: Int,
    sourceTotalSamples: Int,
    outputStartSamples: Int,
    outputSampleCount: Int,
    playbackSpeed: Float,
    sampleRateHz: Int,
    context: PlaybackRenderContext,
): ShortArray {
    if (outputSampleCount <= 0 || sampleRateHz <= 0 || !canRenderMiniCwSpeedAdjustedPcm(context)) {
        return ShortArray(0)
    }
    val segments =
        buildMiniCwPlaybackSegments(
            sourceStartSamples = sourceStartSamples,
            sourceTotalSamples = sourceTotalSamples,
            playbackSpeed = playbackSpeed,
            toneTimeline = context.followData.binaryGroupTimeline,
        )
    val renderedTotalSamples = segments.sumOf { it.renderedSampleCount }
    val clampedStart = outputStartSamples.coerceIn(0, renderedTotalSamples)
    val clampedCount = outputSampleCount.coerceAtMost(renderedTotalSamples - clampedStart)
    if (clampedCount <= 0) {
        return ShortArray(0)
    }
    val output = ShortArray(clampedCount)
    val outputEnd = clampedStart + clampedCount
    segments.forEach { segment ->
        val segmentRenderStart = maxOf(segment.renderedStartSamples, clampedStart)
        val segmentRenderEnd = minOf(segment.renderedEndSamples, outputEnd)
        if (segmentRenderStart >= segmentRenderEnd) {
            return@forEach
        }
        val outputOffset = segmentRenderStart - clampedStart
        val segmentOffset = segmentRenderStart - segment.renderedStartSamples
        val count = segmentRenderEnd - segmentRenderStart
        if (segment.isTone) {
            renderMiniCwToneSegment(
                output = output,
                outputOffset = outputOffset,
                sampleCount = count,
                segmentRenderedOffset = segmentOffset,
                renderedOffsetWithinTone = segment.renderedOffsetWithinTone,
                renderedToneSampleCount = segment.renderedToneSampleCount,
                sampleRateHz = sampleRateHz,
            )
        }
    }
    return output
}

private fun buildMiniCwPlaybackSegments(
    sourceStartSamples: Int,
    sourceTotalSamples: Int,
    playbackSpeed: Float,
    toneTimeline: List<PayloadFollowBinaryGroupTimelineEntry>,
): List<MiniCwPlaybackSegment> {
    // Mini CW speed changes regenerate the 700 Hz carrier for tone segments instead of stretching source PCM pitch.
    val sourceStart = sourceStartSamples.coerceIn(0, sourceTotalSamples)
    val sourceEnd = sourceTotalSamples.coerceAtLeast(sourceStart)
    if (sourceStart >= sourceEnd) {
        return emptyList()
    }
    val resolvedSpeed = coercePlaybackSpeed(playbackSpeed)
    val segments = ArrayList<MiniCwPlaybackSegment>()
    var sourceCursor = sourceStart
    var renderedCursor = 0
    toneTimeline
        .asSequence()
        .filter { it.sampleCount > 0 }
        .sortedBy { it.startSample }
        .forEach { tone ->
            val toneStart = tone.startSample.coerceAtLeast(0)
            val toneEnd = (tone.startSample + tone.sampleCount).coerceAtLeast(toneStart)
            val toneSampleCount = toneEnd - toneStart
            val clippedToneStart = toneStart.coerceIn(sourceStart, sourceEnd)
            val clippedToneEnd = toneEnd.coerceIn(sourceStart, sourceEnd)
            if (clippedToneEnd <= clippedToneStart) {
                return@forEach
            }
            if (clippedToneStart > sourceCursor) {
                val silence =
                    MiniCwPlaybackSegment(
                        sourceStartSamples = sourceCursor,
                        sourceSampleCount = clippedToneStart - sourceCursor,
                        renderedStartSamples = renderedCursor,
                        renderedSampleCount = renderedSampleCount(clippedToneStart - sourceCursor, resolvedSpeed),
                        isTone = false,
                        renderedOffsetWithinTone = 0,
                        renderedToneSampleCount = 0,
                    )
                segments += silence
                renderedCursor += silence.renderedSampleCount
            }
            val toneSegment =
                MiniCwPlaybackSegment(
                    sourceStartSamples = clippedToneStart,
                    sourceSampleCount = clippedToneEnd - clippedToneStart,
                    renderedStartSamples = renderedCursor,
                    renderedSampleCount = renderedSampleCount(clippedToneEnd - clippedToneStart, resolvedSpeed),
                    isTone = true,
                    renderedOffsetWithinTone = renderedSampleCount(clippedToneStart - toneStart, resolvedSpeed),
                    renderedToneSampleCount = renderedSampleCount(toneSampleCount, resolvedSpeed),
                )
            segments += toneSegment
            renderedCursor += toneSegment.renderedSampleCount
            sourceCursor = clippedToneEnd
        }
    if (sourceCursor < sourceEnd) {
        val trailingSilence =
            MiniCwPlaybackSegment(
                sourceStartSamples = sourceCursor,
                sourceSampleCount = sourceEnd - sourceCursor,
                renderedStartSamples = renderedCursor,
                renderedSampleCount = renderedSampleCount(sourceEnd - sourceCursor, resolvedSpeed),
                isTone = false,
                renderedOffsetWithinTone = 0,
                renderedToneSampleCount = 0,
            )
        segments += trailingSilence
    }
    return segments.filter { it.sourceSampleCount > 0 && it.renderedSampleCount > 0 }
}

private fun renderedSampleCount(
    sourceSampleCount: Int,
    playbackSpeed: Float,
): Int =
    if (sourceSampleCount <= 0) {
        0
    } else {
        (sourceSampleCount.toDouble() / playbackSpeed.toDouble()).roundToInt().coerceAtLeast(1)
    }

private fun renderMiniCwToneSegment(
    output: ShortArray,
    outputOffset: Int,
    sampleCount: Int,
    segmentRenderedOffset: Int,
    renderedOffsetWithinTone: Int,
    renderedToneSampleCount: Int,
    sampleRateHz: Int,
) {
    if (renderedToneSampleCount <= 0) {
        return
    }
    val phaseOffsetSamples = renderedOffsetWithinTone + segmentRenderedOffset
    var index = 0
    while (index < sampleCount) {
        val toneSampleIndex =
            (renderedOffsetWithinTone + segmentRenderedOffset + index)
                .coerceIn(0, renderedToneSampleCount - 1)
        val time = (phaseOffsetSamples + index).toDouble() / sampleRateHz.toDouble()
        val sample =
            MiniCwToneAmplitude *
                miniCwToneEnvelopeGain(
                    sampleIndex = toneSampleIndex,
                    sampleCount = renderedToneSampleCount,
                    sampleRateHz = sampleRateHz,
                ) *
                sin(2.0 * PI * MiniCwToneFrequencyHz * time)
        output[outputOffset + index] =
            sample
                .roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        index += 1
    }
}

private fun miniCwToneEnvelopeGain(
    sampleIndex: Int,
    sampleCount: Int,
    sampleRateHz: Int,
): Double {
    val rampSamples = miniCwToneEnvelopeRampSampleCount(sampleCount, sampleRateHz)
    if (rampSamples <= 0 || sampleCount <= 0) {
        return 1.0
    }
    var gain = 1.0
    if (sampleIndex < rampSamples) {
        val ratio = sampleIndex.toDouble() / rampSamples.toDouble()
        gain = minOf(gain, 0.5 - 0.5 * cos(PI * ratio))
    }
    val samplesFromEnd = sampleCount - sampleIndex - 1
    if (samplesFromEnd < rampSamples) {
        val ratio = samplesFromEnd.toDouble() / rampSamples.toDouble()
        gain = minOf(gain, 0.5 - 0.5 * cos(PI * ratio))
    }
    return gain
}

private fun miniCwToneEnvelopeRampSampleCount(
    sampleCount: Int,
    sampleRateHz: Int,
): Int {
    if (sampleCount < 2 || sampleRateHz <= 0) {
        return 0
    }
    return (sampleRateHz.toDouble() * MiniCwToneEnvelopeSeconds)
        .roundToInt()
        .coerceAtMost(sampleCount / 2)
}

private data class MiniCwPlaybackSegment(
    val sourceStartSamples: Int,
    val sourceSampleCount: Int,
    val renderedStartSamples: Int,
    val renderedSampleCount: Int,
    val isTone: Boolean,
    val renderedOffsetWithinTone: Int,
    val renderedToneSampleCount: Int,
) {
    val renderedEndSamples: Int
        get() = renderedStartSamples + renderedSampleCount

    fun toTimelineSegment(): RenderedPlaybackTimelineSegment =
        RenderedPlaybackTimelineSegment(
            sourceStartSamples = sourceStartSamples,
            sourceSampleCount = sourceSampleCount,
            renderedStartSamples = renderedStartSamples,
            renderedSampleCount = renderedSampleCount,
        )
}

private const val MiniCwToneFrequencyHz = 700.0
private const val MiniCwToneAmplitude = 0.75 * 32767.0
private const val MiniCwToneEnvelopeSeconds = 0.005

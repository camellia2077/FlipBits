package com.bag.audioandroid.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

internal fun playbackEdgeFadeSampleCount(sampleRateHz: Int): Int =
    if (sampleRateHz <= 0) {
        0
    } else {
        (sampleRateHz.toDouble() * PlaybackEdgeFadeSeconds).roundToInt().coerceAtLeast(1)
    }

internal fun applyPlaybackStartFadeInPlace(
    samples: ShortArray,
    absoluteStartSamples: Int,
    sampleRateHz: Int,
    sampleCount: Int = samples.size,
) {
    val fadeSamples = playbackEdgeFadeSampleCount(sampleRateHz)
    val clampedCount = sampleCount.coerceIn(0, samples.size)
    if (fadeSamples <= 0 || clampedCount <= 0 || absoluteStartSamples >= fadeSamples) {
        return
    }
    var index = 0
    while (index < clampedCount) {
        val absoluteSample = absoluteStartSamples + index
        if (absoluteSample >= fadeSamples) {
            return
        }
        val gain = raisedCosineFadeInGain(absoluteSample, fadeSamples)
        samples[index] =
            (samples[index].toDouble() * gain)
                .roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        index += 1
    }
}

internal fun playbackStartFadedChunk(
    source: ShortArray,
    sourceOffset: Int,
    sampleCount: Int,
    sampleRateHz: Int,
): ShortArray {
    val clampedOffset = sourceOffset.coerceIn(0, source.size)
    val clampedCount = sampleCount.coerceIn(0, source.size - clampedOffset)
    val chunk = source.copyOfRange(clampedOffset, clampedOffset + clampedCount)
    applyPlaybackStartFadeInPlace(
        samples = chunk,
        absoluteStartSamples = clampedOffset,
        sampleRateHz = sampleRateHz,
    )
    return chunk
}

private fun raisedCosineFadeInGain(
    sampleIndex: Int,
    fadeSamples: Int,
): Double {
    if (fadeSamples <= 0) {
        return 1.0
    }
    val ratio = sampleIndex.toDouble() / fadeSamples.toDouble()
    return 0.5 - 0.5 * cos(PI * ratio)
}

private const val PlaybackEdgeFadeSeconds = 0.012

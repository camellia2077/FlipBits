package com.bag.audioandroid.ui.screen

import android.util.Log
import kotlin.math.roundToInt

private const val FlashToneTraceTag = "FlashToneTrace"
private const val FlashToneTraceIntervalNanos = 1_000_000_000L
private var flashToneTraceLastLogNanos = 0L

internal fun recordToneTrace(
    isPlaying: Boolean,
    displayedSample: Float,
    sampleRateHz: Int,
    totalSamples: Int,
    buckets: List<ToneSpectrumBucket>,
    segments: List<FlashSignalToneSegment>,
    frequencyScale: ToneFrequencyScale,
    drawStats: ToneSpectrumDrawStats,
    activeThresholdBucketIndex: Float,
    bucketWidth: Float,
    source: String,
) {
    val now = System.nanoTime()
    if (now - flashToneTraceLastLogNanos < FlashToneTraceIntervalNanos) {
        return
    }
    flashToneTraceLastLogNanos = now
    val activeBuckets = buckets.filter { it.frequencyHz > 0f && it.amplitude >= FlashSignalSilenceThreshold }
    val zeroFrequencyBuckets = buckets.count { it.frequencyHz <= 0f }
    val averageFrequency = activeBuckets.map { it.frequencyHz }.averageOrZero()
    val averageStrength = activeBuckets.map { it.strength }.averageOrZero()
    val segmentCarriers = segments.mapNotNull { it.carrierFrequencyHz.takeIf { carrier -> carrier > 0f } }
    val uniqueSegmentCarrierCount = segmentCarriers.map { it.roundToInt() }.distinct().size
    val segmentCarrierMin = segmentCarriers.minOrNull()?.roundToInt() ?: 0
    val segmentCarrierMax = segmentCarriers.maxOrNull()?.roundToInt() ?: 0
    safeLogD(
        FlashToneTraceTag,
        "tone source=$source isPlaying=$isPlaying " +
            "displayMs=${displayedSample.samplesToMs(sampleRateHz)} " +
            "totalMs=${totalSamples.toFloat().samplesToMs(sampleRateHz)} " +
            "scale=${frequencyScale.minHz.roundToInt()}-${frequencyScale.maxHz.roundToInt()} " +
            "refs=${frequencyScale.references.joinToString("|") { it.label }} " +
            "buckets=${buckets.size} nonSilent=${activeBuckets.size} zeroHz=$zeroFrequencyBuckets " +
            "avgHz=${averageFrequency.roundToInt()} " +
            "drawValid=${drawStats.validBuckets} lineSegments=${drawStats.lineSegments} pointDraws=${drawStats.pointDraws} " +
            "freqRange=${drawStats.minFrequencyHz.roundToInt()}-${drawStats.maxFrequencyHz.roundToInt()} " +
            "firstLast=${drawStats.firstFrequencyHz.roundToInt()}-${drawStats.lastFrequencyHz.roundToInt()} " +
            "currentHz=${drawStats.currentFrequencyHz.roundToInt()} " +
            "segmentCarriers=$uniqueSegmentCarrierCount/$segmentCarrierMin-$segmentCarrierMax " +
            "strokePx=${drawStats.minStrokeWidthPx.formatOneDecimal()}-${drawStats.maxStrokeWidthPx.formatOneDecimal()} " +
            "avgStrength=${averageStrength.formatTwoDecimals()} " +
            "activeThreshold=${activeThresholdBucketIndex.formatOneDecimal()} bucketWidth=${bucketWidth.formatOneDecimal()}",
    )
}

private fun List<Float>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()

private fun Float.samplesToMs(sampleRateHz: Int): Int =
    if (sampleRateHz > 0) {
        (this * 1000f / sampleRateHz.toFloat()).roundToInt()
    } else {
        0
    }

private fun Float.formatOneDecimal(): String = "%.1f".format(java.util.Locale.US, this)

private fun Double.formatTwoDecimals(): String = "%.2f".format(java.util.Locale.US, this)

private fun safeLogD(
    tag: String,
    message: String,
) {
    try {
        Log.d(tag, message)
    } catch (_: RuntimeException) {
        // Plain JVM unit tests use the Android stub jar, where Log.d is not implemented.
    }
}

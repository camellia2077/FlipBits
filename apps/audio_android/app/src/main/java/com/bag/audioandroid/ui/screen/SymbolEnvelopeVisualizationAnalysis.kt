package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.domain.PayloadFollowBinaryGroupTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.UltraFrameSection
import com.bag.audioandroid.domain.UltraFrameSymbolTimelineEntry
import com.bag.audioandroid.ui.model.TransportModeOption
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

internal data class SymbolEnvelopeBucket(
    val upperEnergy: Float,
    val lowerEnergy: Float,
    val peakAmplitude: Float,
    val dominantLaneIndex: Int? = null,
    val dominantFrequencyHz: Int? = null,
    val ultraFrameSection: UltraFrameSection? = null,
    val isUltraPayloadSymbol: Boolean = false,
    val ultraNibbleValue: Int? = null,
)

internal data class UltraSymbolStepVisualizationState(
    val buckets: List<SymbolEnvelopeBucket>,
    val currentBucketIndex: Int,
    val currentBucket: SymbolEnvelopeBucket,
    val nextBucket: SymbolEnvelopeBucket?,
)

internal fun buildSymbolEnvelopeBuckets(
    pcm: ShortArray,
    sampleRateHz: Int,
    currentSample: Float,
    symbolSamples: Int,
    targetBucketCount: Int,
    transportMode: TransportModeOption,
): List<SymbolEnvelopeBucket> {
    if (pcm.isEmpty() || sampleRateHz <= 0 || symbolSamples <= 0) {
        return emptyList()
    }

    val safeBucketCount = targetBucketCount.coerceAtLeast(1)
    val bucketSampleWidth = (symbolSamples * SymbolEnvelopeBucketSymbolCount).toFloat().coerceAtLeast(1f)
    val safeWindowSampleCount = bucketSampleWidth * safeBucketCount.toFloat()
    val pastWindowSamples = safeWindowSampleCount * SymbolEnvelopePlayheadAnchorRatio
    val windowStart = currentSample - pastWindowSamples
    val rawBuckets = ArrayList<RawSymbolEnvelopeBucket>(safeBucketCount)

    repeat(safeBucketCount) { bucketIndex ->
        val bucketStart = windowStart + bucketSampleWidth * bucketIndex.toFloat()
        val bucketEnd = bucketStart + bucketSampleWidth
        val startIndex = floor(bucketStart.toDouble()).toInt().coerceAtLeast(0)
        val endIndexExclusive = ceil(bucketEnd.toDouble()).toInt().coerceAtMost(pcm.size)
        if (endIndexExclusive - startIndex < SymbolEnvelopeMinimumAnalysisSamples) {
            rawBuckets += RawSymbolEnvelopeBucket(0f, 0f, 0f, 0f)
        } else {
            val signalMetrics =
                signalMetrics(
                    pcm = pcm,
                    startIndex = startIndex,
                    endIndexExclusive = endIndexExclusive,
                )
            when (transportMode) {
                TransportModeOption.Pro -> {
                    val lowPower =
                        ProLowFreqsHz
                            .sumOf { freq ->
                                goertzelPower(
                                    pcm = pcm,
                                    startIndex = startIndex,
                                    endIndexExclusive = endIndexExclusive,
                                    sampleRateHz = sampleRateHz,
                                    targetFrequencyHz = freq,
                                ).toDouble()
                            }.toFloat()
                    val highPower =
                        ProHighFreqsHz
                            .sumOf { freq ->
                                goertzelPower(
                                    pcm = pcm,
                                    startIndex = startIndex,
                                    endIndexExclusive = endIndexExclusive,
                                    sampleRateHz = sampleRateHz,
                                    targetFrequencyHz = freq,
                                ).toDouble()
                            }.toFloat()
                    rawBuckets +=
                        RawSymbolEnvelopeBucket(
                            upperPower = highPower,
                            lowerPower = lowPower,
                            rmsAmplitude = signalMetrics.rmsAmplitude,
                            peakAmplitude = signalMetrics.peakAmplitude,
                        )
                }

                TransportModeOption.Ultra -> {
                    val dominantPower =
                        UltraFreqsHz.maxOf { freq ->
                            goertzelPower(
                                pcm = pcm,
                                startIndex = startIndex,
                                endIndexExclusive = endIndexExclusive,
                                sampleRateHz = sampleRateHz,
                                targetFrequencyHz = freq,
                            )
                        }
                    val dominantLaneIndex =
                        UltraFreqsHz.indices.maxByOrNull { laneIndex ->
                            goertzelPower(
                                pcm = pcm,
                                startIndex = startIndex,
                                endIndexExclusive = endIndexExclusive,
                                sampleRateHz = sampleRateHz,
                                targetFrequencyHz = UltraFreqsHz[laneIndex],
                            )
                        }
                    rawBuckets +=
                        RawSymbolEnvelopeBucket(
                            upperPower = dominantPower,
                            lowerPower = dominantPower,
                            rmsAmplitude = signalMetrics.rmsAmplitude,
                            peakAmplitude = signalMetrics.peakAmplitude,
                            dominantLaneIndex = dominantLaneIndex,
                            dominantFrequencyHz = dominantLaneIndex?.let { UltraFreqsHz[it].toInt() },
                        )
                }

                else -> rawBuckets += RawSymbolEnvelopeBucket(0f, 0f, 0f, 0f)
            }
        }
    }

    val maxUpperPower = rawBuckets.maxOfOrNull { it.upperPower }?.coerceAtLeast(1e-6f) ?: 1f
    val maxLowerPower = rawBuckets.maxOfOrNull { it.lowerPower }?.coerceAtLeast(1e-6f) ?: 1f
    val normalizedBuckets =
        rawBuckets.map { bucket ->
            val amplitudeGain =
                (
                    0.18f +
                        0.52f * bucket.rmsAmplitude.coerceIn(0f, 1f) +
                        0.30f * bucket.peakAmplitude.coerceIn(0f, 1f)
                ).coerceIn(0f, 1f)
            SymbolEnvelopeBucket(
                upperEnergy = sqrt((bucket.upperPower / maxUpperPower).coerceIn(0f, 1f)) * amplitudeGain,
                lowerEnergy = sqrt((bucket.lowerPower / maxLowerPower).coerceIn(0f, 1f)) * amplitudeGain,
                peakAmplitude = bucket.peakAmplitude.coerceIn(0f, 1f),
                dominantLaneIndex = bucket.dominantLaneIndex,
                dominantFrequencyHz = bucket.dominantFrequencyHz,
            )
        }
    return smoothEnvelopeBuckets(normalizedBuckets)
}

internal fun buildUltraSymbolStepVisualizationState(
    pcm: ShortArray,
    sampleRateHz: Int,
    currentSample: Float,
    symbolSamples: Int,
    targetBucketCount: Int,
): UltraSymbolStepVisualizationState? {
    val buckets =
        buildSymbolEnvelopeBuckets(
            pcm = pcm,
            sampleRateHz = sampleRateHz,
            currentSample = currentSample,
            symbolSamples = symbolSamples,
            targetBucketCount = targetBucketCount,
            transportMode = TransportModeOption.Ultra,
        )
    if (buckets.isEmpty()) {
        return null
    }
    val currentBucketIndex =
        (buckets.size * SymbolEnvelopePlayheadAnchorRatio)
            .toInt()
            .coerceIn(0, buckets.lastIndex)
    return UltraSymbolStepVisualizationState(
        buckets = buckets,
        currentBucketIndex = currentBucketIndex,
        currentBucket = buckets[currentBucketIndex],
        nextBucket = buckets.getOrNull(currentBucketIndex + 1),
    )
}

internal fun buildUltraSymbolStepVisualizationState(
    followData: PayloadFollowViewData,
    currentSample: Int,
    targetBucketCount: Int,
): UltraSymbolStepVisualizationState? {
    if (!followData.followAvailable) {
        return null
    }
    if (followData.ultraFrameTimeline.isNotEmpty()) {
        return buildUltraFrameSymbolStepVisualizationState(
            entries =
                followData.ultraFrameTimeline.sortedBy(
                    UltraFrameSymbolTimelineEntry::startSample,
                ),
            currentSample = currentSample,
            targetBucketCount = targetBucketCount,
        )
    }
    if (followData.binaryGroupTimeline.isEmpty()) {
        return null
    }
    return buildLegacyUltraPayloadSymbolStepVisualizationState(
        entries =
            followData.binaryGroupTimeline.sortedBy(
                PayloadFollowBinaryGroupTimelineEntry::startSample,
            ),
        currentSample = currentSample,
        targetBucketCount = targetBucketCount,
    )
}

private fun buildUltraFrameSymbolStepVisualizationState(
    entries: List<UltraFrameSymbolTimelineEntry>,
    currentSample: Int,
    targetBucketCount: Int,
): UltraSymbolStepVisualizationState? {
    val activeEntryIndex = activeUltraFrameEntryIndex(entries, currentSample)
    if (activeEntryIndex < 0) {
        return null
    }
    return buildUltraSymbolStepVisualizationStateFromWindow(
        entries = entries,
        activeEntryIndex = activeEntryIndex,
        targetBucketCount = targetBucketCount,
        toBucket = ::ultraFrameTimelineBucket,
    )
}

private fun buildLegacyUltraPayloadSymbolStepVisualizationState(
    entries: List<PayloadFollowBinaryGroupTimelineEntry>,
    currentSample: Int,
    targetBucketCount: Int,
): UltraSymbolStepVisualizationState? {
    val activeEntryIndex = activePayloadGroupEntryIndex(entries, currentSample)
    if (activeEntryIndex < 0) {
        return null
    }
    return buildUltraSymbolStepVisualizationStateFromWindow(
        entries = entries,
        activeEntryIndex = activeEntryIndex,
        targetBucketCount = targetBucketCount,
        toBucket = ::legacyUltraPayloadTimelineBucket,
    )
}

private fun <T> buildUltraSymbolStepVisualizationStateFromWindow(
    entries: List<T>,
    activeEntryIndex: Int,
    targetBucketCount: Int,
    toBucket: (T) -> SymbolEnvelopeBucket?,
): UltraSymbolStepVisualizationState? {
    if (entries.isEmpty()) {
        return null
    }
    val safeBucketCount = targetBucketCount.coerceAtLeast(1)
    val desiredCurrentBucketIndex =
        (safeBucketCount * SymbolEnvelopePlayheadAnchorRatio)
            .toInt()
            .coerceIn(0, safeBucketCount - 1)
    val maxStartIndex = (entries.size - safeBucketCount).coerceAtLeast(0)
    val windowStartIndex = (activeEntryIndex - desiredCurrentBucketIndex).coerceIn(0, maxStartIndex)
    val windowEndIndex = (windowStartIndex + safeBucketCount).coerceAtMost(entries.size)
    val windowEntries = entries.subList(windowStartIndex, windowEndIndex)
    val buckets = windowEntries.mapNotNull(toBucket)
    if (buckets.isEmpty()) {
        return null
    }
    val currentBucketIndex = (activeEntryIndex - windowStartIndex).coerceIn(0, buckets.lastIndex)
    return UltraSymbolStepVisualizationState(
        buckets = buckets,
        currentBucketIndex = currentBucketIndex,
        currentBucket = buckets[currentBucketIndex],
        nextBucket = buckets.getOrNull(currentBucketIndex + 1),
    )
}

private fun activePayloadGroupEntryIndex(
    entries: List<PayloadFollowBinaryGroupTimelineEntry>,
    currentSample: Int,
): Int {
    var low = 0
    var high = entries.lastIndex
    var previousIndex = -1
    while (low <= high) {
        val mid = (low + high).ushr(1)
        val entry = entries[mid]
        val end = entry.startSample + entry.sampleCount
        when {
            currentSample < entry.startSample -> high = mid - 1
            currentSample >= end -> {
                previousIndex = mid
                low = mid + 1
            }
            else -> return mid
        }
    }
    return previousIndex.coerceIn(-1, entries.lastIndex)
}

private fun activeUltraFrameEntryIndex(
    entries: List<UltraFrameSymbolTimelineEntry>,
    currentSample: Int,
): Int {
    var low = 0
    var high = entries.lastIndex
    var previousIndex = -1
    while (low <= high) {
        val mid = (low + high).ushr(1)
        val entry = entries[mid]
        val end = entry.startSample + entry.sampleCount
        when {
            currentSample < entry.startSample -> high = mid - 1
            currentSample >= end -> {
                previousIndex = mid
                low = mid + 1
            }
            else -> return mid
        }
    }
    return previousIndex.coerceIn(-1, entries.lastIndex)
}

private fun ultraFrameTimelineBucket(entry: UltraFrameSymbolTimelineEntry): SymbolEnvelopeBucket? {
    val frequencyHz = entry.carrierFrequencyHz.takeIf { it > 0f } ?: return null
    val laneIndex = nearestUltraFrequencyLane(frequencyHz)
    return SymbolEnvelopeBucket(
        upperEnergy = 0.88f,
        lowerEnergy = 0.88f,
        peakAmplitude = 1f,
        dominantLaneIndex = laneIndex,
        dominantFrequencyHz = frequencyHz.toInt(),
        ultraFrameSection = entry.section,
        isUltraPayloadSymbol = entry.isPayload,
        ultraNibbleValue = entry.nibbleValue,
    )
}

private fun legacyUltraPayloadTimelineBucket(entry: PayloadFollowBinaryGroupTimelineEntry): SymbolEnvelopeBucket? {
    val frequencyHz = entry.carrierFrequencyHz.takeIf { it > 0f } ?: return null
    val laneIndex = nearestUltraFrequencyLane(frequencyHz)
    return SymbolEnvelopeBucket(
        upperEnergy = 0.88f,
        lowerEnergy = 0.88f,
        peakAmplitude = 1f,
        dominantLaneIndex = laneIndex,
        dominantFrequencyHz = frequencyHz.toInt(),
        ultraFrameSection = UltraFrameSection.Payload,
        isUltraPayloadSymbol = true,
    )
}

private fun nearestUltraFrequencyLane(frequencyHz: Float): Int =
    UltraFreqsHz.indices.minByOrNull { index ->
        kotlin.math.abs(UltraFreqsHz[index] - frequencyHz.toDouble())
    } ?: 0

internal fun snapDisplayedSampleToSymbol(
    displayedSample: Int,
    symbolSamples: Int,
    totalSamples: Int,
): Int {
    if (symbolSamples <= 0 || totalSamples <= 0) {
        return 0
    }
    val clampedDisplayedSample = displayedSample.coerceIn(0, totalSamples)
    return ((clampedDisplayedSample / symbolSamples) * symbolSamples).coerceIn(0, totalSamples)
}

private data class RawSymbolEnvelopeBucket(
    val upperPower: Float,
    val lowerPower: Float,
    val rmsAmplitude: Float,
    val peakAmplitude: Float,
    val dominantLaneIndex: Int? = null,
    val dominantFrequencyHz: Int? = null,
)

private data class SignalMetrics(
    val rmsAmplitude: Float,
    val peakAmplitude: Float,
)

private fun signalMetrics(
    pcm: ShortArray,
    startIndex: Int,
    endIndexExclusive: Int,
): SignalMetrics {
    var peak = 0f
    var energySum = 0.0
    val sampleCount = (endIndexExclusive - startIndex).coerceAtLeast(1)
    for (index in startIndex until endIndexExclusive) {
        val normalized = (pcm[index].toFloat() / Short.MAX_VALUE.toFloat()).coerceIn(-1f, 1f)
        val absNormalized = abs(normalized)
        peak = max(peak, absNormalized)
        energySum += normalized.toDouble() * normalized.toDouble()
    }
    return SignalMetrics(
        rmsAmplitude = sqrt((energySum / sampleCount.toDouble()).coerceAtLeast(0.0)).toFloat().coerceIn(0f, 1f),
        peakAmplitude = peak.coerceIn(0f, 1f),
    )
}

private fun goertzelPower(
    pcm: ShortArray,
    startIndex: Int,
    endIndexExclusive: Int,
    sampleRateHz: Int,
    targetFrequencyHz: Double,
): Float {
    val sampleCount = endIndexExclusive - startIndex
    if (sampleCount < 2 || sampleRateHz <= 0) {
        return 0f
    }

    val omega = 2.0 * PI * targetFrequencyHz / sampleRateHz.toDouble()
    val coeff = 2.0 * cos(omega)
    var q0 = 0.0
    var q1 = 0.0
    var q2 = 0.0
    for (index in startIndex until endIndexExclusive) {
        val sample = pcm[index].toDouble() / Short.MAX_VALUE.toDouble()
        q0 = coeff * q1 - q2 + sample
        q2 = q1
        q1 = q0
    }
    return max(0.0, q1 * q1 + q2 * q2 - coeff * q1 * q2).toFloat()
}

private fun smoothEnvelopeBuckets(buckets: List<SymbolEnvelopeBucket>): List<SymbolEnvelopeBucket> {
    if (buckets.isEmpty()) {
        return emptyList()
    }

    val smoothed = ArrayList<SymbolEnvelopeBucket>(buckets.size)
    buckets.indices.forEach { index ->
        var weightSum = 0f
        var upperSum = 0f
        var lowerSum = 0f
        var peakSum = 0f
        for (neighbor in max(0, index - SymbolEnvelopeSmoothingRadius)..min(buckets.lastIndex, index + SymbolEnvelopeSmoothingRadius)) {
            val distance = abs(neighbor - index)
            val weight =
                when (distance) {
                    0 -> 0.5f
                    1 -> 0.3f
                    else -> 0.2f
                }
            val bucket = buckets[neighbor]
            weightSum += weight
            upperSum += bucket.upperEnergy * weight
            lowerSum += bucket.lowerEnergy * weight
            peakSum += bucket.peakAmplitude * weight
        }
        smoothed +=
            SymbolEnvelopeBucket(
                upperEnergy = (upperSum / weightSum).coerceIn(0f, 1f),
                lowerEnergy = (lowerSum / weightSum).coerceIn(0f, 1f),
                peakAmplitude = (peakSum / weightSum).coerceIn(0f, 1f),
                dominantLaneIndex = buckets[index].dominantLaneIndex,
                dominantFrequencyHz = buckets[index].dominantFrequencyHz,
                ultraFrameSection = buckets[index].ultraFrameSection,
                isUltraPayloadSymbol = buckets[index].isUltraPayloadSymbol,
                ultraNibbleValue = buckets[index].ultraNibbleValue,
            )
    }
    return smoothed
}

private val ProLowFreqsHz = listOf(697.0, 770.0, 852.0, 941.0)
private val ProHighFreqsHz = listOf(1209.0, 1336.0, 1477.0, 1633.0)
internal val UltraFreqsHz =
    listOf(
        1000.0,
        1140.0,
        1280.0,
        1420.0,
        1560.0,
        1700.0,
        1840.0,
        1980.0,
        2120.0,
        2260.0,
        2400.0,
        2540.0,
        2680.0,
        2820.0,
        2960.0,
        3100.0,
    )

internal const val SymbolEnvelopePlayheadAnchorRatio = 0.40f
private const val SymbolEnvelopeBucketSymbolCount = 2
private const val SymbolEnvelopeSmoothingRadius = 1
private const val SymbolEnvelopeMinimumAnalysisSamples = 96

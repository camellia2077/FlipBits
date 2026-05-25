package com.bag.audioandroid.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

internal data class FlashSignalAnalysisInputs(
    val analysisCache: FlashSignalAnalysisCache,
    val analysisDisplayedSamplePosition: Float,
    val followAnalysisSampleStep: Int,
)

private data class FlashSignalAnalysisCacheKey(
    val source: String,
    val currentSample: Int,
    val windowSampleCount: Int,
    val targetBucketCount: Int,
)

internal class FlashSignalAnalysisCache {
    private val bucketsByKey = LinkedHashMap<FlashSignalAnalysisCacheKey, List<FskEnergyBucket>>()

    fun pcmBuckets(
        currentSample: Float,
        windowSampleCount: Int,
        targetBucketCount: Int,
        build: () -> List<FskEnergyBucket>,
    ): List<FskEnergyBucket> =
        bucketsFor(
            FlashSignalAnalysisCacheKey(
                source = "pcm",
                currentSample = currentSample.toInt(),
                windowSampleCount = windowSampleCount,
                targetBucketCount = targetBucketCount,
            ),
            build,
        )

    fun followBuckets(
        currentSample: Float,
        windowSampleCount: Int,
        targetBucketCount: Int,
        build: () -> List<FskEnergyBucket>,
    ): List<FskEnergyBucket> =
        bucketsFor(
            FlashSignalAnalysisCacheKey(
                source = "follow",
                currentSample = currentSample.toInt(),
                windowSampleCount = windowSampleCount,
                targetBucketCount = targetBucketCount,
            ),
            build,
        )

    private fun bucketsFor(
        key: FlashSignalAnalysisCacheKey,
        build: () -> List<FskEnergyBucket>,
    ): List<FskEnergyBucket> {
        bucketsByKey[key]?.let { return it }
        val buckets = build()
        bucketsByKey[key] = buckets
        if (bucketsByKey.size > FlashSignalAnalysisCacheMaxEntries) {
            val eldestKey = bucketsByKey.keys.first()
            bucketsByKey.remove(eldestKey)
        }
        return buckets
    }
}

internal data class FlashSignalBucketFrame(
    val buckets: List<FskEnergyBucket>,
    val displayedSamplePosition: Float,
    val analysisDisplayedSamplePosition: Float,
) {
    companion object {
        val Empty =
            FlashSignalBucketFrame(
                buckets = emptyList(),
                displayedSamplePosition = 0f,
                analysisDisplayedSamplePosition = 0f,
            )
    }
}

@Composable
internal fun rememberFlashSignalAnalysisInputs(
    pcm: ShortArray,
    sampleRateHz: Int,
    bucketSource: FlashSignalBucketSource,
    displayedSamplePosition: Float,
    totalSamples: Int,
    followTimelineTotalSamples: Int,
): FlashSignalAnalysisInputs {
    val analysisCache = remember(pcm, sampleRateHz, bucketSource.stableCacheKey()) { FlashSignalAnalysisCache() }
    val analysisSampleStep =
        remember(sampleRateHz, totalSamples) {
            visualizationAnalysisSampleStep(sampleRateHz = sampleRateHz, totalSamples = totalSamples)
        }
    val analysisDisplayedSamplePosition =
        remember(displayedSamplePosition, analysisSampleStep, totalSamples) {
            quantizeVisualizationDisplayedSamples(
                displayedSamples = displayedSamplePosition,
                sampleStep = analysisSampleStep,
                totalSamples = totalSamples,
            )
        }
    val followAnalysisSampleStep =
        remember(sampleRateHz, followTimelineTotalSamples) {
            visualizationAnalysisSampleStep(sampleRateHz = sampleRateHz, totalSamples = followTimelineTotalSamples)
        }
    return remember(
        analysisCache,
        analysisDisplayedSamplePosition,
        followAnalysisSampleStep,
    ) {
        FlashSignalAnalysisInputs(
            analysisCache = analysisCache,
            analysisDisplayedSamplePosition = analysisDisplayedSamplePosition,
            followAnalysisSampleStep = followAnalysisSampleStep,
        )
    }
}

@Composable
internal fun rememberFlashSignalBucketFrame(
    pcm: ShortArray,
    sampleRateHz: Int,
    bucketSource: FlashSignalBucketSource,
    analysisCache: FlashSignalAnalysisCache,
    targetBucketCount: Int,
    windowSampleCount: Int,
    analysisDisplayedSamplePosition: Float,
    visualFollowAnalysisDisplayedSamplePosition: Float,
    displayedSamplePosition: Float,
    visualFollowDisplayedSamplePosition: Float,
    fixedTimelineFrame: FlashSignalFixedTimelineFrame?,
): FlashSignalBucketFrame =
    remember(
        pcm,
        sampleRateHz,
        bucketSource.stableCacheKey(),
        targetBucketCount,
        windowSampleCount,
        analysisDisplayedSamplePosition,
        visualFollowAnalysisDisplayedSamplePosition,
        fixedTimelineFrame,
    ) {
        if (fixedTimelineFrame != null) {
            return@remember FlashSignalBucketFrame.Empty
        }
        when (bucketSource) {
            is FlashSignalBucketSource.FollowTimeline ->
                buildFollowTimelineBucketFrame(
                    bucketSource = bucketSource,
                    analysisCache = analysisCache,
                    pcm = pcm,
                    sampleRateHz = sampleRateHz,
                    targetBucketCount = targetBucketCount,
                    windowSampleCount = windowSampleCount,
                    analysisDisplayedSamplePosition = analysisDisplayedSamplePosition,
                    visualFollowAnalysisDisplayedSamplePosition = visualFollowAnalysisDisplayedSamplePosition,
                    displayedSamplePosition = displayedSamplePosition,
                    visualFollowDisplayedSamplePosition = visualFollowDisplayedSamplePosition,
                )

            is FlashSignalBucketSource.Pcm ->
                FlashSignalBucketFrame(
                    buckets =
                        analysisCache.pcmBuckets(
                            currentSample = analysisDisplayedSamplePosition,
                            windowSampleCount = windowSampleCount,
                            targetBucketCount = targetBucketCount,
                        ) {
                            buildFskEnergyBuckets(
                                pcm = pcm,
                                sampleRateHz = sampleRateHz,
                                currentSample = analysisDisplayedSamplePosition,
                                windowSampleCount = windowSampleCount,
                                targetBucketCount = targetBucketCount,
                            )
                        },
                    displayedSamplePosition = displayedSamplePosition,
                    analysisDisplayedSamplePosition = analysisDisplayedSamplePosition,
                )
        }
    }

private fun buildFollowTimelineBucketFrame(
    bucketSource: FlashSignalBucketSource.FollowTimeline,
    analysisCache: FlashSignalAnalysisCache,
    pcm: ShortArray,
    sampleRateHz: Int,
    targetBucketCount: Int,
    windowSampleCount: Int,
    analysisDisplayedSamplePosition: Float,
    visualFollowAnalysisDisplayedSamplePosition: Float,
    displayedSamplePosition: Float,
    visualFollowDisplayedSamplePosition: Float,
): FlashSignalBucketFrame {
    val followBuckets =
        analysisCache.followBuckets(
            currentSample = visualFollowAnalysisDisplayedSamplePosition,
            windowSampleCount = windowSampleCount,
            targetBucketCount = targetBucketCount,
        ) {
            buildFskEnergyBucketsFromFollowData(
                followData = bucketSource.followData,
                currentSample = visualFollowAnalysisDisplayedSamplePosition,
                windowSampleCount = windowSampleCount,
                targetBucketCount = targetBucketCount,
            )
        }
    if (followBuckets.isNotEmpty()) {
        return FlashSignalBucketFrame(
            buckets = followBuckets,
            displayedSamplePosition = visualFollowDisplayedSamplePosition,
            analysisDisplayedSamplePosition = visualFollowAnalysisDisplayedSamplePosition,
        )
    }
    return FlashSignalBucketFrame(
        buckets =
            analysisCache.pcmBuckets(
                currentSample = analysisDisplayedSamplePosition,
                windowSampleCount = windowSampleCount,
                targetBucketCount = targetBucketCount,
            ) {
                buildFskEnergyBuckets(
                    pcm = pcm,
                    sampleRateHz = sampleRateHz,
                    currentSample = analysisDisplayedSamplePosition,
                    windowSampleCount = windowSampleCount,
                    targetBucketCount = targetBucketCount,
                )
            },
        displayedSamplePosition = displayedSamplePosition,
        analysisDisplayedSamplePosition = analysisDisplayedSamplePosition,
    )
}

private const val FlashSignalAnalysisCacheMaxEntries = 12

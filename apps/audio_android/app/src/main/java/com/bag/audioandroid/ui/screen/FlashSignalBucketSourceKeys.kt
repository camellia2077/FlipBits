package com.bag.audioandroid.ui.screen

internal data class FlashSignalBucketSourceCacheKey(
    val source: String,
    val identity: Int,
    val timelineSize: Int,
    val totalSamples: Int,
)

internal data class FlashSignalBucketSourceTimelineKey(
    val identity: Int,
    val timelineSize: Int,
    val totalSamples: Int,
)

internal fun FlashSignalBucketSource.stableCacheKey(): FlashSignalBucketSourceCacheKey =
    when (this) {
        is FlashSignalBucketSource.Pcm ->
            FlashSignalBucketSourceCacheKey(
                source = "pcm",
                identity = 0,
                timelineSize = 0,
                totalSamples = 0,
            )

        is FlashSignalBucketSource.FollowTimeline ->
            FlashSignalBucketSourceCacheKey(
                source = "follow",
                identity = System.identityHashCode(followData),
                timelineSize = followData.binaryGroupTimeline.size,
                totalSamples = followData.totalPcmSampleCount,
            )
    }

internal fun FlashSignalBucketSource.stableTimelineKey(): FlashSignalBucketSourceTimelineKey? =
    (this as? FlashSignalBucketSource.FollowTimeline)?.followData?.let { followData ->
        FlashSignalBucketSourceTimelineKey(
            identity = System.identityHashCode(followData),
            timelineSize = followData.binaryGroupTimeline.size,
            totalSamples = followData.totalPcmSampleCount,
        )
    }

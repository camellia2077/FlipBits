package com.bag.audioandroid.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

internal data class FlashSignalTimelineStrategy(
    val fallbackTimelineFrame: FlashSignalFixedTimelineFrame?,
    val fixedTimelineFrame: FlashSignalFixedTimelineFrame?,
    val usesFallbackTimeline: Boolean,
    val visualSegments: List<FlashSignalToneSegment>,
    val visualTotalSamples: Int,
)

@Composable
internal fun rememberFlashSignalTimelineStrategy(
    mode: FlashSignalVisualizationMode,
    bucketSource: FlashSignalBucketSource,
    windowedTimelineFrame: FlashSignalFixedTimelineFrame?,
    followTimelineTotalSamples: Int,
    isScrubbing: Boolean,
): FlashSignalTimelineStrategy {
    val hasWindowedTimelineFrame = windowedTimelineFrame != null && !isScrubbing
    val shouldUseTimelineFallback =
        remember(mode, hasWindowedTimelineFrame, bucketSource.stableTimelineKey(), isScrubbing) {
            shouldUseFallbackTimeline(
                mode = mode,
                hasWindowedTimelineFrame = hasWindowedTimelineFrame,
            )
        }
    val fallbackTimelineFrame =
        remember(bucketSource.stableTimelineKey(), shouldUseTimelineFallback) {
            if (!shouldUseTimelineFallback) {
                return@remember null
            }
            fallbackTimelineFrameOrNull(bucketSource)
        }
    val fixedTimelineFrame =
        selectedFixedTimelineFrame(
            windowedTimelineFrame = windowedTimelineFrame,
            fallbackTimelineFrame = fallbackTimelineFrame,
            isScrubbing = isScrubbing,
        )
    val usesFallbackTimeline = fallbackTimelineFrame != null && fixedTimelineFrame === fallbackTimelineFrame
    val visualSegments =
        flashVisualSegmentsForMode(
            fixedTimelineFrame = fixedTimelineFrame,
            mode = mode,
            usesFallbackTimeline = usesFallbackTimeline,
            isScrubbing = isScrubbing,
        )
    val visualTotalSamples = fixedTimelineFrame?.totalSamples ?: followTimelineTotalSamples
    return remember(
        fallbackTimelineFrame,
        fixedTimelineFrame,
        usesFallbackTimeline,
        visualSegments,
        visualTotalSamples,
    ) {
        FlashSignalTimelineStrategy(
            fallbackTimelineFrame = fallbackTimelineFrame,
            fixedTimelineFrame = fixedTimelineFrame,
            usesFallbackTimeline = usesFallbackTimeline,
            visualSegments = visualSegments,
            visualTotalSamples = visualTotalSamples,
        )
    }
}

private fun shouldUseFallbackTimeline(
    mode: FlashSignalVisualizationMode,
    hasWindowedTimelineFrame: Boolean,
): Boolean =
    when (mode) {
        FlashSignalVisualizationMode.Pulse -> !hasWindowedTimelineFrame
        FlashSignalVisualizationMode.Lanes,
        FlashSignalVisualizationMode.Pitch,
        FlashSignalVisualizationMode.Hz,
        -> true
    }

private fun fallbackTimelineFrameOrNull(bucketSource: FlashSignalBucketSource): FlashSignalFixedTimelineFrame? =
    (bucketSource as? FlashSignalBucketSource.FollowTimeline)
        ?.followData
        ?.toFixedTimelineFrameOrNull()

private fun selectedFixedTimelineFrame(
    windowedTimelineFrame: FlashSignalFixedTimelineFrame?,
    fallbackTimelineFrame: FlashSignalFixedTimelineFrame?,
    isScrubbing: Boolean,
): FlashSignalFixedTimelineFrame? =
    if (isScrubbing) {
        fallbackTimelineFrame ?: windowedTimelineFrame
    } else {
        windowedTimelineFrame ?: fallbackTimelineFrame
    }

internal fun flashVisualSegmentsForMode(
    fixedTimelineFrame: FlashSignalFixedTimelineFrame?,
    mode: FlashSignalVisualizationMode,
    usesFallbackTimeline: Boolean,
    isScrubbing: Boolean,
): List<FlashSignalToneSegment> {
    if (fixedTimelineFrame == null) {
        return emptyList()
    }
    val shouldPreserveExactBitGeometry =
        mode == FlashSignalVisualizationMode.Lanes ||
            mode == FlashSignalVisualizationMode.Hz ||
            (
                usesFallbackTimeline &&
                    isScrubbing &&
                    mode == FlashSignalVisualizationMode.Pitch
            )
    return if (shouldPreserveExactBitGeometry) {
        fixedTimelineFrame.segments
    } else {
        fixedTimelineFrame.drawableSegments.takeIf { it.isNotEmpty() } ?: fixedTimelineFrame.segments
    }
}

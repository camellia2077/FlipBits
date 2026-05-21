package com.bag.audioandroid.ui.screen

private const val FlashSignalMaxVisualBucketOffset = 3f

internal data class FlashSignalCanvasDecision(
    val fixedViewport: FlashSignalViewport?,
    val visualBucketCount: Int,
    val bucketWidth: Float,
    val activeThresholdBucketIndex: Float,
    val drawContent: FlashSignalDrawContent,
    val showPulseOverlay: Boolean,
    val showLaneBoundaryOverlay: Boolean,
    val showLaneSummaryOverlay: Boolean,
)

internal sealed interface FlashSignalDrawContent {
    data class LaneSegments(
        val viewport: FlashSignalViewport,
    ) : FlashSignalDrawContent

    data class LaneBuckets(
        val bucketOffset: Float,
    ) : FlashSignalDrawContent

    data class PitchSegments(
        val viewport: FlashSignalViewport,
    ) : FlashSignalDrawContent

    data class PitchBuckets(
        val bucketOffset: Float,
    ) : FlashSignalDrawContent

    data class HzSegments(
        val viewport: FlashSignalViewport,
    ) : FlashSignalDrawContent

    data class HzBuckets(
        val bucketOffset: Float,
    ) : FlashSignalDrawContent

    data object Pulse : FlashSignalDrawContent
}

private data class FlashSignalBucketMetrics(
    val visualBucketCount: Int,
    val bucketWidth: Float,
    val bucketOffset: Float,
    val activeThresholdBucketIndex: Float,
)

private data class FlashSignalOverlayFlags(
    val showPulseOverlay: Boolean,
    val showLaneBoundaryOverlay: Boolean,
    val showLaneSummaryOverlay: Boolean,
)

internal fun buildFlashSignalCanvasDecision(
    mode: FlashSignalVisualizationMode,
    renderState: FlashSignalVisualizerRenderState,
    runtimeState: FlashSignalCanvasRuntimeState,
    followDisplayedSamplePosition: Float,
    windowSampleCount: Int,
    innerWidth: Float,
    showPerfOverlay: Boolean,
): FlashSignalCanvasDecision {
    val fixedViewport =
        buildFlashSignalFixedViewport(
            renderState = renderState,
            followDisplayedSamplePosition = followDisplayedSamplePosition,
            windowSampleCount = windowSampleCount,
        )
    val bucketMetrics =
        buildFlashSignalBucketMetrics(
            mode = mode,
            renderState = renderState,
            windowSampleCount = windowSampleCount,
            innerWidth = innerWidth,
        )
    val drawContent =
        buildFlashSignalDrawContent(
            mode = mode,
            renderState = renderState,
            fixedViewport = fixedViewport,
            bucketOffset = bucketMetrics.bucketOffset,
        )
    val overlayFlags =
        buildFlashSignalOverlayFlags(
            drawContent = drawContent,
            runtimeState = runtimeState,
            showPerfOverlay = showPerfOverlay,
        )
    return FlashSignalCanvasDecision(
        fixedViewport = fixedViewport,
        visualBucketCount = bucketMetrics.visualBucketCount,
        bucketWidth = bucketMetrics.bucketWidth,
        activeThresholdBucketIndex = bucketMetrics.activeThresholdBucketIndex,
        drawContent = drawContent,
        showPulseOverlay = overlayFlags.showPulseOverlay,
        showLaneBoundaryOverlay = overlayFlags.showLaneBoundaryOverlay,
        showLaneSummaryOverlay = overlayFlags.showLaneSummaryOverlay,
    )
}

private fun buildFlashSignalFixedViewport(
    renderState: FlashSignalVisualizerRenderState,
    followDisplayedSamplePosition: Float,
    windowSampleCount: Int,
): FlashSignalViewport? =
    renderState.fixedTimelineFrame?.let {
        val windowStart = followDisplayedSamplePosition - windowSampleCount * FlashSignalPlayheadAnchorRatio
        FlashSignalViewport(
            startSample = windowStart,
            endSample = windowStart + windowSampleCount,
            playheadSample = followDisplayedSamplePosition,
        )
    }

private fun buildFlashSignalBucketMetrics(
    mode: FlashSignalVisualizationMode,
    renderState: FlashSignalVisualizerRenderState,
    windowSampleCount: Int,
    innerWidth: Float,
): FlashSignalBucketMetrics {
    val visualBucketCount =
        if (mode == FlashSignalVisualizationMode.Hz) {
            renderState.toneSpectrumBuckets.size.takeIf { it > 0 } ?: renderState.buckets.size
        } else {
            renderState.buckets.size
        }
    val bucketWidth = if (visualBucketCount > 0) innerWidth / visualBucketCount.toFloat() else 1f
    val analysisBucketSampleWidth =
        if (visualBucketCount > 0) {
            windowSampleCount.toFloat() / visualBucketCount.toFloat()
        } else {
            1f
        }
    val bucketOffset =
        if (renderState.buckets.isNotEmpty() && mode != FlashSignalVisualizationMode.Hz) {
            (
                (renderState.bucketFrame.displayedSamplePosition - renderState.bucketFrame.analysisDisplayedSamplePosition) /
                    analysisBucketSampleWidth
            ).coerceIn(-FlashSignalMaxVisualBucketOffset, FlashSignalMaxVisualBucketOffset)
        } else {
            0f
        }
    val scanHeadBucketIndex =
        if (visualBucketCount > 0) {
            (visualBucketCount * FlashSignalPlayheadAnchorRatio).coerceIn(0f, (visualBucketCount - 1).toFloat())
        } else {
            0f
        }
    val activeThresholdBucketIndex =
        if (visualBucketCount > 0) {
            (scanHeadBucketIndex + bucketOffset).coerceIn(0f, (visualBucketCount - 1).toFloat())
        } else {
            0f
        }
    return FlashSignalBucketMetrics(
        visualBucketCount = visualBucketCount,
        bucketWidth = bucketWidth,
        bucketOffset = bucketOffset,
        activeThresholdBucketIndex = activeThresholdBucketIndex,
    )
}

private fun buildFlashSignalDrawContent(
    mode: FlashSignalVisualizationMode,
    renderState: FlashSignalVisualizerRenderState,
    fixedViewport: FlashSignalViewport?,
    bucketOffset: Float,
): FlashSignalDrawContent =
    when (mode) {
        FlashSignalVisualizationMode.Lanes ->
            if (renderState.fixedTimelineFrame != null && fixedViewport != null) {
                FlashSignalDrawContent.LaneSegments(fixedViewport)
            } else {
                FlashSignalDrawContent.LaneBuckets(bucketOffset)
            }

        FlashSignalVisualizationMode.Pitch ->
            if (renderState.fixedTimelineFrame != null && fixedViewport != null) {
                FlashSignalDrawContent.PitchSegments(fixedViewport)
            } else {
                FlashSignalDrawContent.PitchBuckets(bucketOffset)
            }

        FlashSignalVisualizationMode.Hz ->
            if (renderState.fixedTimelineFrame != null && fixedViewport != null) {
                FlashSignalDrawContent.HzSegments(fixedViewport)
            } else {
                FlashSignalDrawContent.HzBuckets(bucketOffset)
            }

        FlashSignalVisualizationMode.Pulse -> FlashSignalDrawContent.Pulse
    }

private fun buildFlashSignalOverlayFlags(
    drawContent: FlashSignalDrawContent,
    runtimeState: FlashSignalCanvasRuntimeState,
    showPerfOverlay: Boolean,
): FlashSignalOverlayFlags =
    FlashSignalOverlayFlags(
        showPulseOverlay = drawContent == FlashSignalDrawContent.Pulse && runtimeState.pulseTapeState != null,
        showLaneBoundaryOverlay =
            showPerfOverlay &&
                drawContent is FlashSignalDrawContent.LaneSegments &&
                runtimeState.laneActiveBitState != null,
        showLaneSummaryOverlay = showPerfOverlay && drawContent is FlashSignalDrawContent.LaneSegments,
    )

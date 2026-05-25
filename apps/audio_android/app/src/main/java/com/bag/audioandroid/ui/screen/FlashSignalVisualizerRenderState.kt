package com.bag.audioandroid.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.state.FlashVisualWindowState

internal data class FlashSignalVisualizerRenderState(
    val buckets: List<FskEnergyBucket>,
    val toneSpectrumBuckets: List<ToneSpectrumBucket>,
    val toneFrequencyScale: ToneFrequencyScale,
    val toneCarrierLayout: ToneCarrierLayout,
    val bucketFrame: FlashSignalBucketFrame,
    val fixedTimelineFrame: FlashSignalFixedTimelineFrame?,
    val visualSegments: List<FlashSignalToneSegment>,
    val playbackSampleState: FlashVisualPlaybackSampleState,
    val followData: PayloadFollowViewData?,
    val bitReadoutSource: FlashBitReadoutSource?,
    val bitReadoutFrame: FlashBitReadoutFrame?,
    val bitReadoutSample: Float,
    val activeWindowBucketCount: Int,
    val primitiveEstimate: Int,
    val usesFallbackTimeline: Boolean,
    val enableViewportEdgeFade: Boolean,
    val traceWindowSamples: Int,
    val traceWindowStartSample: Int,
    val traceWindowEndSample: Int,
    val totalSamples: Int,
)

private data class FlashSignalRenderContext(
    val visualizerModel: FlashSignalVisualizerModel,
    val totalSamples: Int,
    val followTimelineSource: FlashSignalBucketSource.FollowTimeline?,
    val followTimelineTotalSamples: Int,
    val displayedSamplePosition: Float,
    val followDisplayedSamplePosition: Float,
    val windowedTimelineFrame: FlashSignalFixedTimelineFrame?,
)

@Composable
internal fun rememberFlashSignalVisualizerRenderState(
    input: FlashSignalVisualizationInput,
    isPlaying: Boolean,
    mode: FlashSignalVisualizationMode,
    flashVoicingStyle: FlashVoicingStyleOption?,
    flashVisualWindow: FlashVisualWindowState,
    sharedPlaybackSampleState: FlashVisualPlaybackSampleState?,
    playbackSpeed: Float,
    isScrubbing: Boolean,
    targetBucketCount: Int,
    windowSampleCount: Int,
): FlashSignalVisualizerRenderState {
    val pcm = input.pcm
    val sampleRateHz = input.sampleRateHz
    val renderContext =
        rememberFlashSignalRenderContext(
            input = input,
            flashVisualWindow = flashVisualWindow,
        )
    val analysisInputs =
        rememberFlashSignalAnalysisInputs(
            pcm = pcm,
            sampleRateHz = sampleRateHz,
            bucketSource = input.bucketSource,
            displayedSamplePosition = renderContext.displayedSamplePosition,
            totalSamples = renderContext.totalSamples,
            followTimelineTotalSamples = renderContext.followTimelineTotalSamples,
        )
    val visualStyle =
        rememberFlashSignalVisualStyleContext(
            flashVoicingStyle = flashVoicingStyle,
        )
    val timelineStrategy =
        rememberFlashSignalTimelineStrategy(
            mode = mode,
            bucketSource = input.bucketSource,
            windowedTimelineFrame = renderContext.windowedTimelineFrame,
            followTimelineTotalSamples = renderContext.followTimelineTotalSamples,
            isScrubbing = isScrubbing,
        )
    val followData = renderContext.followTimelineSource?.followData
    val playbackReadoutState =
        rememberFlashSignalPlaybackReadoutState(
            sharedPlaybackSampleState = sharedPlaybackSampleState,
            displayedSamplePosition = renderContext.displayedSamplePosition,
            followDisplayedSamplePosition = renderContext.followDisplayedSamplePosition,
            isPlaying = isPlaying,
            isScrubbing = isScrubbing,
            playbackSpeed = playbackSpeed,
            sampleRateHz = sampleRateHz,
            visualTotalSamples = timelineStrategy.visualTotalSamples,
            followAnalysisSampleStep = analysisInputs.followAnalysisSampleStep,
            followTimelineTotalSamples = renderContext.followTimelineTotalSamples,
            followData = followData,
        )
    val bucketFrame =
        rememberFlashSignalBucketFrame(
            pcm = pcm,
            sampleRateHz = sampleRateHz,
            bucketSource = input.bucketSource,
            analysisCache = analysisInputs.analysisCache,
            targetBucketCount = targetBucketCount,
            windowSampleCount = windowSampleCount,
            analysisDisplayedSamplePosition = analysisInputs.analysisDisplayedSamplePosition,
            visualFollowAnalysisDisplayedSamplePosition = playbackReadoutState.visualFollowAnalysisDisplayedSamplePosition,
            displayedSamplePosition = renderContext.displayedSamplePosition,
            visualFollowDisplayedSamplePosition = playbackReadoutState.visualFollowDisplayedSamplePosition,
            fixedTimelineFrame = timelineStrategy.fixedTimelineFrame,
        )
    val toneSpectrumBuckets =
        rememberFlashSignalToneSpectrumBuckets(
            pcm = pcm,
            sampleRateHz = sampleRateHz,
            targetBucketCount = targetBucketCount,
            windowSampleCount = windowSampleCount,
            visualFollowAnalysisDisplayedSamplePosition = playbackReadoutState.visualFollowAnalysisDisplayedSamplePosition,
            toneFrequencyScale = visualStyle.toneFrequencyScale,
            fixedTimelineFrame = timelineStrategy.fixedTimelineFrame,
            mode = mode,
        )
    val primitiveEstimate =
        rememberFlashSignalPrimitiveEstimate(
            mode = mode,
            visualSegments = timelineStrategy.visualSegments,
            toneSpectrumBuckets = toneSpectrumBuckets,
            bucketFrame = bucketFrame,
            fixedTimelineFrame = timelineStrategy.fixedTimelineFrame,
        )
    return remember(
        bucketFrame,
        toneSpectrumBuckets,
        visualStyle.toneFrequencyScale,
        visualStyle.toneCarrierLayout,
        timelineStrategy.fixedTimelineFrame,
        timelineStrategy.visualSegments,
        playbackReadoutState.playbackSampleState,
        followData,
        playbackReadoutState.bitReadoutSource,
        playbackReadoutState.bitReadoutFrame,
        playbackReadoutState.visualFollowDisplayedSamplePosition,
        visualStyle.activeWindowBucketCount,
        primitiveEstimate,
        timelineStrategy.usesFallbackTimeline,
        flashVoicingStyle,
        flashVisualWindow,
        timelineStrategy.visualTotalSamples,
    ) {
        buildFlashSignalVisualizerRenderState(
            bucketFrame = bucketFrame,
            toneSpectrumBuckets = toneSpectrumBuckets,
            visualStyle = visualStyle,
            timelineStrategy = timelineStrategy,
            playbackReadoutState = playbackReadoutState,
            followData = followData,
            primitiveEstimate = primitiveEstimate,
            flashVoicingStyle = flashVoicingStyle,
            flashVisualWindow = flashVisualWindow,
        )
    }
}

private fun buildFlashSignalVisualizerRenderState(
    bucketFrame: FlashSignalBucketFrame,
    toneSpectrumBuckets: List<ToneSpectrumBucket>,
    visualStyle: FlashSignalVisualStyleContext,
    timelineStrategy: FlashSignalTimelineStrategy,
    playbackReadoutState: FlashSignalPlaybackReadoutState,
    followData: PayloadFollowViewData?,
    primitiveEstimate: Int,
    flashVoicingStyle: FlashVoicingStyleOption?,
    flashVisualWindow: FlashVisualWindowState,
): FlashSignalVisualizerRenderState =
    FlashSignalVisualizerRenderState(
        buckets = bucketFrame.buckets,
        toneSpectrumBuckets = toneSpectrumBuckets,
        toneFrequencyScale = visualStyle.toneFrequencyScale,
        toneCarrierLayout = visualStyle.toneCarrierLayout,
        bucketFrame = bucketFrame,
        fixedTimelineFrame = timelineStrategy.fixedTimelineFrame,
        visualSegments = timelineStrategy.visualSegments,
        playbackSampleState = playbackReadoutState.playbackSampleState,
        followData = followData,
        bitReadoutSource = playbackReadoutState.bitReadoutSource,
        bitReadoutFrame = playbackReadoutState.bitReadoutFrame,
        bitReadoutSample = playbackReadoutState.visualFollowDisplayedSamplePosition,
        activeWindowBucketCount = visualStyle.activeWindowBucketCount,
        primitiveEstimate = primitiveEstimate,
        usesFallbackTimeline = timelineStrategy.usesFallbackTimeline,
        enableViewportEdgeFade = flashVoicingStyle != FlashVoicingStyleOption.Litany,
        traceWindowSamples = flashVisualWindow.endSampleExclusive - flashVisualWindow.startSample,
        traceWindowStartSample = flashVisualWindow.startSample,
        traceWindowEndSample = flashVisualWindow.endSampleExclusive,
        totalSamples = timelineStrategy.visualTotalSamples,
    )

@Composable
private fun rememberFlashSignalRenderContext(
    input: FlashSignalVisualizationInput,
    flashVisualWindow: FlashVisualWindowState,
): FlashSignalRenderContext {
    val visualizerModel =
        rememberFlashSignalVisualizerModel(
            input = input,
            flashVisualWindow = flashVisualWindow,
        )
    return remember(visualizerModel) {
        FlashSignalRenderContext(
            visualizerModel = visualizerModel,
            totalSamples = visualizerModel.totalSamples,
            followTimelineSource = visualizerModel.followTimelineSource,
            followTimelineTotalSamples = visualizerModel.followTimelineTotalSamples,
            displayedSamplePosition = visualizerModel.displayedSamplePosition,
            followDisplayedSamplePosition = visualizerModel.followDisplayedSamplePosition,
            windowedTimelineFrame = visualizerModel.windowedTimelineFrame,
        )
    }
}

@Composable
private fun rememberFlashSignalPrimitiveEstimate(
    mode: FlashSignalVisualizationMode,
    visualSegments: List<FlashSignalToneSegment>,
    toneSpectrumBuckets: List<ToneSpectrumBucket>,
    bucketFrame: FlashSignalBucketFrame,
    fixedTimelineFrame: FlashSignalFixedTimelineFrame?,
): Int =
    remember(mode, visualSegments, toneSpectrumBuckets, bucketFrame, fixedTimelineFrame) {
        flashVisualPrimitiveEstimate(
            mode = mode,
            drawableSegments = visualSegments.size,
            buckets =
                if (mode == FlashSignalVisualizationMode.Hz && fixedTimelineFrame == null) {
                    toneSpectrumBuckets.size
                } else {
                    bucketFrame.buckets.size
                },
            hasFixedTimeline = fixedTimelineFrame != null,
        )
    }

@Composable
private fun rememberFlashSignalToneSpectrumBuckets(
    pcm: ShortArray,
    sampleRateHz: Int,
    targetBucketCount: Int,
    windowSampleCount: Int,
    visualFollowAnalysisDisplayedSamplePosition: Float,
    toneFrequencyScale: ToneFrequencyScale,
    fixedTimelineFrame: FlashSignalFixedTimelineFrame?,
    mode: FlashSignalVisualizationMode,
): List<ToneSpectrumBucket> =
    remember(
        pcm,
        sampleRateHz,
        targetBucketCount,
        windowSampleCount,
        visualFollowAnalysisDisplayedSamplePosition,
        toneFrequencyScale,
        fixedTimelineFrame,
        mode,
    ) {
        if (mode != FlashSignalVisualizationMode.Hz || fixedTimelineFrame != null) {
            emptyList()
        } else {
            buildToneSpectrumBuckets(
                pcm = pcm,
                sampleRateHz = sampleRateHz,
                currentSample = visualFollowAnalysisDisplayedSamplePosition,
                windowSampleCount = windowSampleCount,
                targetBucketCount = targetBucketCount,
                frequencyScale = toneFrequencyScale,
            )
        }
    }

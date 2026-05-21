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

private data class FlashSignalTimelineStrategy(
    val fallbackTimelineFrame: FlashSignalFixedTimelineFrame?,
    val fixedTimelineFrame: FlashSignalFixedTimelineFrame?,
    val usesFallbackTimeline: Boolean,
    val visualSegments: List<FlashSignalToneSegment>,
    val visualTotalSamples: Int,
)

private data class FlashSignalPlaybackReadoutState(
    val playbackSampleState: FlashVisualPlaybackSampleState,
    val visualFollowDisplayedSamplePosition: Float,
    val visualFollowAnalysisDisplayedSamplePosition: Float,
    val bitReadoutSource: FlashBitReadoutSource?,
    val bitReadoutFrame: FlashBitReadoutFrame?,
)

private data class FlashSignalBitReadoutState(
    val source: FlashBitReadoutSource?,
    val frame: FlashBitReadoutFrame?,
)

private data class FlashSignalAnalysisInputs(
    val analysisCache: FlashSignalAnalysisCache,
    val analysisDisplayedSamplePosition: Float,
    val followAnalysisSampleStep: Int,
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
private fun rememberFlashSignalAnalysisInputs(
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
private fun rememberFlashSignalPlaybackReadoutState(
    sharedPlaybackSampleState: FlashVisualPlaybackSampleState?,
    displayedSamplePosition: Float,
    followDisplayedSamplePosition: Float,
    isPlaying: Boolean,
    isScrubbing: Boolean,
    playbackSpeed: Float,
    sampleRateHz: Int,
    visualTotalSamples: Int,
    followAnalysisSampleStep: Int,
    followTimelineTotalSamples: Int,
    followData: PayloadFollowViewData?,
): FlashSignalPlaybackReadoutState {
    val playbackSampleState =
        sharedPlaybackSampleState
            ?: rememberFlashVisualPlaybackSampleState(
                rawSample =
                    flashVisualPlaybackRawSample(
                        displayedSamplePosition = displayedSamplePosition,
                        followDisplayedSamplePosition = followDisplayedSamplePosition,
                        isScrubbing = isScrubbing,
                    ),
                isPlaying = isPlaying && !isScrubbing,
                snapWhenNotPlaying = isScrubbing,
                isScrubbing = isScrubbing,
                playbackSpeed = playbackSpeed,
                sampleRateHz = sampleRateHz,
                totalSamples = visualTotalSamples,
            )
    val visualFollowDisplayedSamplePosition = playbackSampleState.displayedSample
    val visualFollowAnalysisDisplayedSamplePosition =
        rememberVisualFollowAnalysisSample(
            visualFollowDisplayedSamplePosition = visualFollowDisplayedSamplePosition,
            followAnalysisSampleStep = followAnalysisSampleStep,
            followTimelineTotalSamples = followTimelineTotalSamples,
        )
    val bitReadoutState =
        rememberFlashSignalBitReadoutState(
            followData = followData,
            visualFollowDisplayedSamplePosition = visualFollowDisplayedSamplePosition,
        )
    return remember(
        playbackSampleState,
        visualFollowDisplayedSamplePosition,
        visualFollowAnalysisDisplayedSamplePosition,
        bitReadoutState,
    ) {
        FlashSignalPlaybackReadoutState(
            playbackSampleState = playbackSampleState,
            visualFollowDisplayedSamplePosition = visualFollowDisplayedSamplePosition,
            visualFollowAnalysisDisplayedSamplePosition = visualFollowAnalysisDisplayedSamplePosition,
            bitReadoutSource = bitReadoutState.source,
            bitReadoutFrame = bitReadoutState.frame,
        )
    }
}

private fun flashVisualPlaybackRawSample(
    displayedSamplePosition: Float,
    followDisplayedSamplePosition: Float,
    isScrubbing: Boolean,
): Float =
    if (isScrubbing) {
        displayedSamplePosition
    } else {
        followDisplayedSamplePosition
    }

@Composable
private fun rememberVisualFollowAnalysisSample(
    visualFollowDisplayedSamplePosition: Float,
    followAnalysisSampleStep: Int,
    followTimelineTotalSamples: Int,
): Float =
    remember(visualFollowDisplayedSamplePosition, followAnalysisSampleStep, followTimelineTotalSamples) {
        quantizeVisualizationDisplayedSamples(
            displayedSamples = visualFollowDisplayedSamplePosition,
            sampleStep = followAnalysisSampleStep,
            totalSamples = followTimelineTotalSamples,
        )
    }

@Composable
private fun rememberFlashSignalBitReadoutState(
    followData: PayloadFollowViewData?,
    visualFollowDisplayedSamplePosition: Float,
): FlashSignalBitReadoutState {
    val source =
        remember(followData) {
            followData?.toFlashBitReadoutSource()
        }
    val frame =
        remember(source, visualFollowDisplayedSamplePosition) {
            source?.let { source ->
                flashBitReadoutFrame(
                    source = source,
                    sample = visualFollowDisplayedSamplePosition,
                )
            }
        }
    return remember(source, frame) {
        FlashSignalBitReadoutState(
            source = source,
            frame = frame,
        )
    }
}

@Composable
private fun rememberFlashSignalTimelineStrategy(
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

@Composable
private fun rememberFlashSignalBucketFrame(
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

private data class FlashSignalBucketSourceCacheKey(
    val source: String,
    val identity: Int,
    val timelineSize: Int,
    val totalSamples: Int,
)

private data class FlashSignalBucketSourceTimelineKey(
    val identity: Int,
    val timelineSize: Int,
    val totalSamples: Int,
)

private fun FlashSignalBucketSource.stableCacheKey(): FlashSignalBucketSourceCacheKey =
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

private fun FlashSignalBucketSource.stableTimelineKey(): FlashSignalBucketSourceTimelineKey? =
    (this as? FlashSignalBucketSource.FollowTimeline)?.followData?.let { followData ->
        FlashSignalBucketSourceTimelineKey(
            identity = System.identityHashCode(followData),
            timelineSize = followData.binaryGroupTimeline.size,
            totalSamples = followData.totalPcmSampleCount,
        )
    }

private data class FlashSignalAnalysisCacheKey(
    val source: String,
    val currentSample: Int,
    val windowSampleCount: Int,
    val targetBucketCount: Int,
)

private class FlashSignalAnalysisCache {
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

private const val FlashSignalAnalysisCacheMaxEntries = 12

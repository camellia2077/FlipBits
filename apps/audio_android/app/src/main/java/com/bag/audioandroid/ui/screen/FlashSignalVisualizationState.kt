package com.bag.audioandroid.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.bag.audioandroid.domain.PayloadFollowBinaryGroupTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.state.FlashVisualWindowState
import kotlin.math.abs

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
    val shouldUseTimelineFallback: Boolean,
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

private data class FlashSignalVisualStyleContext(
    val activeWindowBucketCount: Int,
    val toneFrequencyScale: ToneFrequencyScale,
    val toneCarrierLayout: ToneCarrierLayout,
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
    }
}

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
private fun rememberFlashSignalVisualStyleContext(flashVoicingStyle: FlashVoicingStyleOption?): FlashSignalVisualStyleContext {
    val activeWindowBucketCount =
        remember(flashVoicingStyle) {
            flashSignalActiveWindowBucketCount(flashVoicingStyle)
        }
    val toneFrequencyScale =
        remember(flashVoicingStyle) {
            toneFrequencyScaleForStyle(flashVoicingStyle)
        }
    val toneCarrierLayout =
        remember(flashVoicingStyle) {
            toneCarrierLayoutForStyle(flashVoicingStyle)
        }
    return remember(
        activeWindowBucketCount,
        toneFrequencyScale,
        toneCarrierLayout,
    ) {
        FlashSignalVisualStyleContext(
            activeWindowBucketCount = activeWindowBucketCount,
            toneFrequencyScale = toneFrequencyScale,
            toneCarrierLayout = toneCarrierLayout,
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
                rawSample = if (isScrubbing) displayedSamplePosition else followDisplayedSamplePosition,
                isPlaying = isPlaying && !isScrubbing,
                snapWhenNotPlaying = isScrubbing,
                playbackSpeed = playbackSpeed,
                sampleRateHz = sampleRateHz,
                totalSamples = visualTotalSamples,
            )
    val visualFollowDisplayedSamplePosition = playbackSampleState.displayedSample
    val visualFollowAnalysisDisplayedSamplePosition =
        remember(visualFollowDisplayedSamplePosition, followAnalysisSampleStep, followTimelineTotalSamples) {
            quantizeVisualizationDisplayedSamples(
                displayedSamples = visualFollowDisplayedSamplePosition,
                sampleStep = followAnalysisSampleStep,
                totalSamples = followTimelineTotalSamples,
            )
        }
    val bitReadoutSource =
        remember(followData) {
            followData?.toFlashBitReadoutSource()
        }
    val bitReadoutFrame =
        bitReadoutSource?.let { source ->
            flashBitReadoutFrame(
                source = source,
                sample = visualFollowDisplayedSamplePosition,
            )
        }
    return remember(
        playbackSampleState,
        visualFollowDisplayedSamplePosition,
        visualFollowAnalysisDisplayedSamplePosition,
        bitReadoutSource,
        bitReadoutFrame,
    ) {
        FlashSignalPlaybackReadoutState(
            playbackSampleState = playbackSampleState,
            visualFollowDisplayedSamplePosition = visualFollowDisplayedSamplePosition,
            visualFollowAnalysisDisplayedSamplePosition = visualFollowAnalysisDisplayedSamplePosition,
            bitReadoutSource = bitReadoutSource,
            bitReadoutFrame = bitReadoutFrame,
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
            when (mode) {
                FlashSignalVisualizationMode.Pulse -> !hasWindowedTimelineFrame
                FlashSignalVisualizationMode.Lanes,
                FlashSignalVisualizationMode.Pitch,
                -> true
                FlashSignalVisualizationMode.Hz -> true
            }
        }
    val fallbackTimelineFrame =
        remember(bucketSource.stableTimelineKey(), shouldUseTimelineFallback) {
            if (!shouldUseTimelineFallback) {
                return@remember null
            }
            (bucketSource as? FlashSignalBucketSource.FollowTimeline)
                ?.followData
                ?.toFixedTimelineFrameOrNull()
        }
    val fixedTimelineFrame =
        if (isScrubbing) {
            fallbackTimelineFrame ?: windowedTimelineFrame
        } else {
            windowedTimelineFrame ?: fallbackTimelineFrame
        }
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
        shouldUseTimelineFallback,
        fallbackTimelineFrame,
        fixedTimelineFrame,
        usesFallbackTimeline,
        visualSegments,
        visualTotalSamples,
    ) {
        FlashSignalTimelineStrategy(
            shouldUseTimelineFallback = shouldUseTimelineFallback,
            fallbackTimelineFrame = fallbackTimelineFrame,
            fixedTimelineFrame = fixedTimelineFrame,
            usesFallbackTimeline = usesFallbackTimeline,
            visualSegments = visualSegments,
            visualTotalSamples = visualTotalSamples,
        )
    }
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

internal data class FlashSignalCanvasRuntimeState(
    val pulseTapeState: FlashPulseTapeState?,
    val laneActiveBitState: FlashLaneActiveBitState?,
    val tokenAlignmentState: FlashTokenAlignmentState?,
    val telemetryState: FlashSignalCanvasTelemetryState,
)

internal data class FlashSignalCanvasTelemetryState(
    val currentReadoutBit: Int?,
    val currentReadoutBitValue: Char?,
    val revealedBitOffset: Int,
    val currentVisualBit: Int?,
    val currentRawBit: Int?,
)

@Composable
internal fun rememberFlashSignalCanvasRuntimeState(
    followDisplayedSamplePosition: Float,
    rawSample: Float,
    followData: PayloadFollowViewData?,
    bitReadoutSource: FlashBitReadoutSource?,
    bitReadoutFrame: FlashBitReadoutFrame?,
): FlashSignalCanvasRuntimeState {
    val pulseTapeState =
        remember(bitReadoutSource, followDisplayedSamplePosition) {
            bitReadoutSource?.let { source ->
                flashPulseTapeState(
                    source = source,
                    sample = followDisplayedSamplePosition,
                )
            }
        }
    val laneActiveBitState =
        remember(bitReadoutSource, followDisplayedSamplePosition) {
            bitReadoutSource?.let { source ->
                flashLaneActiveBitState(
                    entries = source.entries,
                    bitByOffset = source.bitByOffset,
                    sample = followDisplayedSamplePosition,
                )
            }
        }
    val tokenAlignmentState =
        remember(followData, followDisplayedSamplePosition) {
            followData?.let { data ->
                flashTokenAlignmentState(
                    followData = data,
                    displayedSamples = followDisplayedSamplePosition.toInt(),
                )
            }
        }
    val telemetryState =
        remember(bitReadoutFrame, bitReadoutSource, followDisplayedSamplePosition, rawSample) {
            val currentReadoutBit = bitReadoutFrame?.currentBitOffset
            FlashSignalCanvasTelemetryState(
                currentReadoutBit = currentReadoutBit,
                currentReadoutBitValue = currentReadoutBit?.let { bitReadoutSource?.bitByOffset?.get(it) },
                revealedBitOffset = bitReadoutFrame?.revealedBitOffset ?: -1,
                currentVisualBit = bitReadoutSource?.currentBitOffsetAtSample(followDisplayedSamplePosition),
                currentRawBit = bitReadoutSource?.currentBitOffsetAtSample(rawSample),
            )
        }
    return remember(
        pulseTapeState,
        laneActiveBitState,
        tokenAlignmentState,
        telemetryState,
    ) {
        FlashSignalCanvasRuntimeState(
            pulseTapeState = pulseTapeState,
            laneActiveBitState = laneActiveBitState,
            tokenAlignmentState = tokenAlignmentState,
            telemetryState = telemetryState,
        )
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

internal data class FlashVisualPlaybackSampleState(
    val rawSample: Float,
    val displayedSample: Float,
)

@Composable
internal fun rememberFlashVisualPlaybackSampleState(
    rawSample: Float,
    isPlaying: Boolean,
    snapWhenNotPlaying: Boolean = false,
    holdVisualPositionOnPause: Boolean = false,
    playbackSpeed: Float,
    sampleRateHz: Int,
    totalSamples: Int,
): FlashVisualPlaybackSampleState {
    var visualSample by remember { mutableFloatStateOf(rawSample) }
    var previousIsPlaying by remember { mutableStateOf(isPlaying) }
    var pausedHoldRawSample by remember { mutableStateOf<Float?>(null) }
    var pausedHoldDisplayedSample by remember { mutableStateOf<Float?>(null) }
    val clampedRawSample = rawSample.coerceIn(0f, totalSamples.coerceAtLeast(1).toFloat())
    val safeSpeed = playbackSpeed.coerceIn(0.1f, 4f)
    val latestAnchorSample by rememberUpdatedState(rawSample)
    val latestTotalSamples by rememberUpdatedState(totalSamples)
    if (!isPlaying || sampleRateHz <= 0 || totalSamples <= 0) {
        val justPaused = previousIsPlaying && !isPlaying
        val holdRawSample = pausedHoldRawSample
        val holdDisplayedSample = pausedHoldDisplayedSample
        val pausedHoldCanApply =
            holdVisualPositionOnPause &&
                (
                    justPaused ||
                        (
                            holdRawSample != null &&
                                holdDisplayedSample != null &&
                                abs(clampedRawSample - holdRawSample) <= 0.5f
                        )
                )
        if (pausedHoldCanApply) {
            val displayedSample =
                (
                    if (justPaused) {
                        visualSample
                    } else {
                        holdDisplayedSample ?: visualSample
                    }
                ).coerceIn(0f, totalSamples.coerceAtLeast(1).toFloat())
            SideEffect {
                visualSample = displayedSample
                pausedHoldRawSample = clampedRawSample
                pausedHoldDisplayedSample = displayedSample
                previousIsPlaying = false
            }
            return FlashVisualPlaybackSampleState(
                rawSample = rawSample,
                displayedSample = displayedSample,
            )
        }
        val shouldSnapToRaw =
            snapWhenNotPlaying ||
                sampleRateHz <= 0 ||
                abs(clampedRawSample - visualSample) > sampleRateHz * PauseSnapDriftThresholdSeconds
        val displayedSample =
            if (shouldSnapToRaw) {
                clampedRawSample
            } else {
                visualSample.coerceIn(0f, totalSamples.coerceAtLeast(1).toFloat())
            }
        if (shouldSnapToRaw) {
            SideEffect {
                visualSample = clampedRawSample
                pausedHoldDisplayedSample = null
                pausedHoldRawSample = null
                previousIsPlaying = false
            }
        } else {
            SideEffect {
                pausedHoldDisplayedSample = null
                pausedHoldRawSample = null
                previousIsPlaying = false
            }
        }
        return FlashVisualPlaybackSampleState(
            rawSample = rawSample,
            displayedSample = displayedSample,
        )
    }
    LaunchedEffect(safeSpeed, sampleRateHz, totalSamples) {
        previousIsPlaying = true
        pausedHoldRawSample = null
        pausedHoldDisplayedSample = null
        FlashVisualPerfTrace.recordSmoothReset(
            anchorSample = latestAnchorSample,
            previousSmoothSample = visualSample,
            sampleRateHz = sampleRateHz,
        )
        val maxSample = latestTotalSamples.toFloat()
        val initialAnchor = latestAnchorSample.coerceIn(0f, maxSample)
        val forwardAnchorThreshold = sampleRateHz * PlaybackResumeForwardAnchorThresholdSeconds
        val shouldSnapBackwardToAnchor = initialAnchor + 0.5f < visualSample
        val shouldSnapForwardToAnchor = initialAnchor - visualSample > forwardAnchorThreshold
        visualSample =
            if (shouldSnapBackwardToAnchor || shouldSnapForwardToAnchor) {
                initialAnchor
            } else {
                visualSample.coerceIn(0f, maxSample)
            }
        var frameAnchorNanos = withFrameNanos { it }
        var frameAnchorSample = visualSample
        while (true) {
            val frameNanos = withFrameNanos { it }
            val elapsedSeconds = (frameNanos - frameAnchorNanos).toDouble() / 1_000_000_000.0
            val currentMaxSample = latestTotalSamples.toFloat()
            val anchor = latestAnchorSample.coerceIn(0f, currentMaxSample)
            val nextSample =
                frameAnchorSample +
                    (elapsedSeconds * sampleRateHz.toDouble() * safeSpeed.toDouble()).toFloat()
            val predictedSample = nextSample.coerceIn(0f, currentMaxSample)
            visualSample =
                if (abs(anchor - predictedSample) > sampleRateHz * 0.35f) {
                    frameAnchorNanos = frameNanos
                    frameAnchorSample = anchor
                    anchor
                } else {
                    predictedSample
                }
            if (visualSample >= currentMaxSample) {
                frameAnchorNanos = frameNanos
                frameAnchorSample = visualSample
            }
        }
    }
    return FlashVisualPlaybackSampleState(
        rawSample = rawSample,
        displayedSample = visualSample,
    )
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

internal data class FlashBitReadoutFrame(
    val currentGroupStartIndex: Int,
    val currentBitOffset: Int?,
    val revealedBitOffset: Int,
    val previousCells: List<FlashBitReadoutCell>,
    val currentCells: List<FlashBitReadoutCell>,
)

internal data class FlashBitReadoutCell(
    val bit: Char?,
    val isCurrent: Boolean,
)

internal data class FlashPulseTapeState(
    val cells: List<FlashPulseCellState>,
    val currentBitProgress: Float,
)

internal data class FlashPulseCellState(
    val bit: Char?,
    val isActive: Boolean,
    val isRevealed: Boolean,
)

internal data class FlashBitReadoutSource(
    val entries: List<PayloadFollowBinaryGroupTimelineEntry>,
    val bitByOffset: Map<Int, Char>,
)

internal fun PayloadFollowViewData.toFlashBitReadoutSource(): FlashBitReadoutSource? {
    if (!followAvailable || binaryGroupTimeline.isEmpty() || binaryTokens.isEmpty()) {
        return null
    }
    return FlashBitReadoutSource(
        entries = binaryGroupTimeline,
        bitByOffset = binaryBitsByOffset(),
    )
}

internal fun flashBitReadoutFrame(
    followData: PayloadFollowViewData,
    sample: Float,
): FlashBitReadoutFrame? {
    val source = followData.toFlashBitReadoutSource() ?: return null
    return flashBitReadoutFrame(source = source, sample = sample)
}

internal fun flashBitReadoutFrame(
    source: FlashBitReadoutSource,
    sample: Float,
): FlashBitReadoutFrame? {
    if (source.entries.isEmpty() || source.bitByOffset.isEmpty()) {
        return null
    }
    val playbackState = flashTimelinePlaybackState(entries = source.entries, sample = sample)
    val currentGroupStartIndex = (playbackState.revealedBitOffset.coerceAtLeast(0) / FlashBitReadoutGroupSize) * FlashBitReadoutGroupSize
    val previousGroupStartIndex = currentGroupStartIndex - FlashBitReadoutGroupSize
    return FlashBitReadoutFrame(
        currentGroupStartIndex = currentGroupStartIndex,
        currentBitOffset = playbackState.currentBitOffset,
        revealedBitOffset = playbackState.revealedBitOffset,
        previousCells =
            buildFlashBitReadoutCells(
                bitByOffset = source.bitByOffset,
                groupStartIndex = previousGroupStartIndex,
                revealThroughIndex = previousGroupStartIndex + FlashBitReadoutGroupSize - 1,
                currentBitOffset = null,
            ),
        currentCells =
            buildFlashBitReadoutCells(
                bitByOffset = source.bitByOffset,
                groupStartIndex = currentGroupStartIndex,
                revealThroughIndex = playbackState.revealedBitOffset,
                currentBitOffset = playbackState.currentBitOffset,
            ),
    )
}

internal fun flashPulseTapeState(
    source: FlashBitReadoutSource,
    sample: Float,
): FlashPulseTapeState? {
    if (source.entries.isEmpty() || source.bitByOffset.isEmpty()) {
        return null
    }
    val playbackState = flashTimelinePlaybackState(entries = source.entries, sample = sample)
    val anchorBitOffset = playbackState.currentBitOffset ?: playbackState.revealedBitOffset.takeIf { it >= 0 } ?: return null
    val halfWindow = FlashPulseVisibleCellCount / 2
    return FlashPulseTapeState(
        cells =
            List(FlashPulseVisibleCellCount) { index ->
                val bitOffset = anchorBitOffset + index - halfWindow
                FlashPulseCellState(
                    bit = source.bitByOffset[bitOffset],
                    isActive = playbackState.currentBitOffset == bitOffset,
                    isRevealed = bitOffset <= playbackState.revealedBitOffset,
                )
            },
        currentBitProgress = playbackState.currentBitProgress,
    )
}

private fun buildFlashBitReadoutCells(
    bitByOffset: Map<Int, Char>,
    groupStartIndex: Int,
    revealThroughIndex: Int,
    currentBitOffset: Int?,
): List<FlashBitReadoutCell> =
    List(FlashBitReadoutGroupSize) { slot ->
        val bitOffset = groupStartIndex + slot
        FlashBitReadoutCell(
            bit =
                if (bitOffset >= 0 && bitOffset <= revealThroughIndex) {
                    bitByOffset[bitOffset]
                } else {
                    null
                },
            isCurrent = currentBitOffset == bitOffset,
        )
    }

private data class FlashTimelinePlaybackState(
    val currentBitOffset: Int?,
    val revealedBitOffset: Int,
    val currentBitProgress: Float,
)

private fun flashTimelinePlaybackState(
    entries: List<PayloadFollowBinaryGroupTimelineEntry>,
    sample: Float,
): FlashTimelinePlaybackState {
    var low = 0
    var high = entries.lastIndex
    var previousRevealedBitOffset = -1
    while (low <= high) {
        val mid = (low + high) ushr 1
        val entry = entries[mid]
        val entryEndSample = entry.startSample + entry.sampleCount
        when {
            sample < entry.startSample -> high = mid - 1
            sample >= entryEndSample -> {
                previousRevealedBitOffset = entry.lastBitOffset
                low = mid + 1
            }
            else -> {
                val bitProgress = entry.bitProgressAtSample(sample)
                val currentBitOffset =
                    entry.bitOffset + bitProgress.toInt().coerceIn(0, entry.bitCount.coerceAtLeast(1) - 1)
                return FlashTimelinePlaybackState(
                    currentBitOffset = currentBitOffset,
                    revealedBitOffset = currentBitOffset,
                    currentBitProgress = bitProgress - bitProgress.toInt(),
                )
            }
        }
    }
    return FlashTimelinePlaybackState(
        currentBitOffset = null,
        revealedBitOffset = previousRevealedBitOffset,
        currentBitProgress = 0f,
    )
}

internal fun FlashBitReadoutSource.currentBitOffsetAtSample(sample: Float): Int? =
    flashTimelinePlaybackState(entries = entries, sample = sample).currentBitOffset

internal fun FlashBitReadoutFrame.currentBitsText(): String = currentCells.joinToString(separator = "") { it.bit?.toString() ?: "_" }

internal fun FlashBitReadoutFrame.previousBitsText(): String = previousCells.joinToString(separator = "") { it.bit?.toString() ?: "_" }

private val PayloadFollowBinaryGroupTimelineEntry.lastBitOffset: Int
    get() = bitOffset + bitCount - 1

internal fun PayloadFollowBinaryGroupTimelineEntry.bitProgressAtSample(sample: Float): Float {
    if (bitCount <= 1 || sampleCount <= 0) {
        return 0f
    }
    val progress = ((sample - startSample.toFloat()) / sampleCount.toFloat()).coerceIn(0f, 0.9999f)
    return progress * bitCount.toFloat()
}

private fun PayloadFollowViewData.binaryBitsByOffset(): Map<Int, Char> {
    val bitsByOffset = LinkedHashMap<Int, Char>()
    binaryGroupTimeline.forEach { entry ->
        val bits = binaryTokens.getOrNull(entry.groupIndex).orEmpty().filter { it == '0' || it == '1' }
        repeat(entry.bitCount.coerceAtLeast(0)) { bitIndex ->
            val tokenBitIndex =
                if (bits.length == entry.bitCount) {
                    bitIndex
                } else {
                    (entry.bitOffset + bitIndex).floorMod(bits.length)
                }
            bits.getOrNull(tokenBitIndex)?.let { bit ->
                bitsByOffset[entry.bitOffset + bitIndex] = bit
            }
        }
    }
    return bitsByOffset
}

private fun Int.floorMod(divisor: Int): Int =
    if (divisor <= 0) {
        0
    } else {
        ((this % divisor) + divisor) % divisor
    }

private const val PlaybackResumeForwardAnchorThresholdSeconds = 0.08f

internal fun flashVisualPrimitiveEstimate(
    mode: FlashSignalVisualizationMode,
    drawableSegments: Int,
    buckets: Int,
    hasFixedTimeline: Boolean,
): Int =
    if (hasFixedTimeline) {
        when (mode) {
            FlashSignalVisualizationMode.Lanes -> drawableSegments * 2
            FlashSignalVisualizationMode.Pitch -> drawableSegments
            FlashSignalVisualizationMode.Hz -> drawableSegments
            FlashSignalVisualizationMode.Pulse -> FlashPulseVisibleCellCount
        }
    } else {
        when (mode) {
            FlashSignalVisualizationMode.Lanes -> buckets * 2
            FlashSignalVisualizationMode.Pitch -> buckets * 2
            FlashSignalVisualizationMode.Hz -> buckets
            FlashSignalVisualizationMode.Pulse -> FlashPulseVisibleCellCount
        }
    }

private const val FlashSignalAnalysisCacheMaxEntries = 12
private const val FlashBitReadoutGroupSize = 8
private const val PauseSnapDriftThresholdSeconds = 0.35f
internal const val FlashPulseVisibleCellCount = 13

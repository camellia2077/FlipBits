package com.bag.audioandroid.ui.screen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bag.audioandroid.BuildConfig
import com.bag.audioandroid.domain.PayloadFollowBinaryGroupTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.state.FlashVisualWindowState
import com.bag.audioandroid.ui.theme.appThemeVisualTokens
import kotlin.math.ceil
import kotlin.math.roundToInt

// Controls only the Flash Visual viewport density. Playback progress and audio
// timeline semantics stay based on absolute samples.
private const val FlashSignalViewportSeconds = 0.80f

@Composable
internal fun AudioFlashSignalVisualizer(
    input: FlashSignalVisualizationInput,
    isPlaying: Boolean,
    mode: FlashSignalVisualizationMode,
    flashVoicingStyle: FlashVoicingStyleOption?,
    flashVisualWindow: FlashVisualWindowState = FlashVisualWindowState(),
    playbackSpeed: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val pcm = input.pcm
    val sampleRateHz = input.sampleRateHz
    if (pcm.isEmpty() || sampleRateHz <= 0) {
        return
    }

    val visualTokens = appThemeVisualTokens()
    val density = LocalDensity.current
    val totalSamples = pcm.size.coerceAtLeast(1)
    val clampedDisplayedSamples = input.pcmDisplayedSamples.coerceIn(0, totalSamples)
    val followTimelineSource = input.bucketSource as? FlashSignalBucketSource.FollowTimeline
    val followTimelineTotalSamples =
        followTimelineSource
            ?.followData
            ?.totalPcmSampleCount
            ?.coerceAtLeast(followTimelineSource.displayedSamples)
            ?.coerceAtLeast(1)
            ?: 1
    val clampedFollowDisplayedSamples =
        followTimelineSource
            ?.displayedSamples
            ?.coerceIn(0, followTimelineTotalSamples)
            ?: clampedDisplayedSamples
    val displayedSamplePosition = clampedDisplayedSamples.toFloat()
    val followDisplayedSamplePosition = clampedFollowDisplayedSamples.toFloat()
    // The generated Flash path supplies a pre-windowed, budgeted timeline so Canvas
    // does not rebuild or scan the full follow timeline during long playback.
    val windowedTimelineFrame =
        remember(flashVisualWindow) {
            flashVisualWindow
                .takeIf { it.available }
                ?.let { window ->
                    FlashSignalFixedTimelineFrame(
                        segments = window.segments,
                        drawableSegments = window.drawableSegments,
                        totalSamples = window.totalPcmSampleCount.coerceAtLeast(1),
                    )
                }
        }
    val analysisCache = remember(pcm, sampleRateHz, input.bucketSource.stableCacheKey()) { FlashSignalAnalysisCache() }
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
    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .height(if (BuildConfig.DEBUG) 230.dp else 170.dp),
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val targetBucketCount =
            remember(widthPx) {
                val bucketSpacingPx = with(density) { 6.dp.toPx() }
                ceil((widthPx / bucketSpacingPx).toDouble())
                    .toInt()
                    .coerceIn(FlashSignalMinBucketCount, FlashSignalMaxBucketCount)
            }
        val windowSampleCount =
            remember(sampleRateHz) {
                (sampleRateHz.coerceAtLeast(1) * FlashSignalViewportSeconds)
                    .roundToInt()
                    .coerceAtLeast(1)
            }
        val activeWindowBucketCount =
            remember(flashVoicingStyle) {
                flashSignalActiveWindowBucketCount(flashVoicingStyle)
            }
        val hasWindowedTimelineFrame = windowedTimelineFrame != null
        val fallbackTimelineFrame =
            remember(input.bucketSource.stableTimelineKey(), hasWindowedTimelineFrame) {
                if (hasWindowedTimelineFrame) {
                    return@remember null
                }
                (input.bucketSource as? FlashSignalBucketSource.FollowTimeline)
                    ?.followData
                    ?.let { followData ->
                        val segments = buildFlashSignalToneSegments(followData)
                        if (segments.isNotEmpty()) {
                            FlashSignalFixedTimelineFrame(
                                segments = segments,
                                drawableSegments = segments,
                                totalSamples = followData.totalPcmSampleCount.coerceAtLeast(1),
                            )
                        } else {
                            null
                        }
                    }
            }
        val fixedTimelineFrame = windowedTimelineFrame ?: fallbackTimelineFrame
        val visualSegments = fixedTimelineFrame?.segments.orEmpty()
        val usesFallbackTimeline = windowedTimelineFrame == null && fallbackTimelineFrame != null
        val visualTotalSamples = fixedTimelineFrame?.totalSamples ?: followTimelineTotalSamples
        val playbackSampleState =
            rememberFlashVisualPlaybackSampleState(
                rawSample = followDisplayedSamplePosition,
                isPlaying = isPlaying,
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
        val followData = followTimelineSource?.followData
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
        val bucketFrame =
            remember(
                pcm,
                sampleRateHz,
                input.bucketSource.stableCacheKey(),
                targetBucketCount,
                windowSampleCount,
                analysisDisplayedSamplePosition,
                visualFollowAnalysisDisplayedSamplePosition,
                fixedTimelineFrame,
            ) {
                if (fixedTimelineFrame != null) {
                    return@remember FlashSignalBucketFrame.Empty
                }
                when (val bucketSource = input.bucketSource) {
                    is FlashSignalBucketSource.FollowTimeline -> {
                        val followBuckets =
                            analysisCache
                                .followBuckets(
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
                            FlashSignalBucketFrame(
                                buckets = followBuckets,
                                displayedSamplePosition = visualFollowDisplayedSamplePosition,
                                analysisDisplayedSamplePosition = visualFollowAnalysisDisplayedSamplePosition,
                            )
                        } else {
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
        val buckets = bucketFrame.buckets
        val traceWindowStartSample = flashVisualWindow.startSample
        val traceWindowEndSample = flashVisualWindow.endSampleExclusive
        val primitiveEstimate =
            flashVisualPrimitiveEstimate(
                mode = mode,
                drawableSegments = visualSegments.size,
                buckets = buckets.size,
                hasFixedTimeline = fixedTimelineFrame != null,
            )
        val traceWindowSamples = flashVisualWindow.endSampleExclusive - flashVisualWindow.startSample
        val activeToneColor = MaterialTheme.colorScheme.primary
        val inactiveToneColor = visualTokens.visualizationInactiveToneColor
        val glowColor = MaterialTheme.colorScheme.onPrimaryContainer
        val baseBackground = visualTokens.visualizationBaseBackgroundColor
        val centerLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
        FlashVisualPerfTrace.recordCompose(
            mode = mode,
            isPlaying = isPlaying,
            displayedSample = visualFollowDisplayedSamplePosition,
            drawableSegments = visualSegments.size,
            exactSegments = fixedTimelineFrame?.segments?.size ?: 0,
            primitiveEstimate = primitiveEstimate,
            buckets = buckets.size,
            hasFixedTimeline = fixedTimelineFrame != null,
            usesFallbackTimeline = usesFallbackTimeline,
            hasBitReadout = bitReadoutFrame != null,
            windowSamples = traceWindowSamples,
            totalSamples = visualTotalSamples,
            windowStartSample = traceWindowStartSample,
            windowEndSample = traceWindowEndSample,
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FlashSignalCanvas(
                mode = mode,
                isPlaying = isPlaying,
                buckets = buckets,
                bucketFrame = bucketFrame,
                fixedTimelineFrame = fixedTimelineFrame,
                visualSegments = visualSegments,
                playbackSampleState = playbackSampleState,
                sampleRateHz = sampleRateHz,
                windowSampleCount = windowSampleCount,
                activeWindowBucketCount = activeWindowBucketCount,
                activeToneColor = activeToneColor,
                inactiveToneColor = inactiveToneColor,
                glowColor = glowColor,
                baseBackground = baseBackground,
                centerLineColor = centerLineColor,
                followData = followData,
                bitReadoutSource = bitReadoutSource,
                bitReadoutFrame = bitReadoutFrame,
                bitReadoutSample = visualFollowDisplayedSamplePosition,
                primitiveEstimate = primitiveEstimate,
                usesFallbackTimeline = usesFallbackTimeline,
                hasBitReadout = bitReadoutFrame != null,
                enableViewportEdgeFade = flashVoicingStyle != FlashVoicingStyleOption.Litany,
                traceWindowSamples = traceWindowSamples,
                traceWindowStartSample = traceWindowStartSample,
                traceWindowEndSample = traceWindowEndSample,
                totalSamples = visualTotalSamples,
                modifier = Modifier.fillMaxWidth(),
            )
            if (BuildConfig.DEBUG) {
                FlashVisualFpsOverlay(
                    snapshot = FlashVisualPerfTrace.snapshot(),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                )
            }
            if (bitReadoutFrame != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    FlashBitReadoutRow(
                        cells = bitReadoutFrame.previousCells,
                        activeToneColor = activeToneColor,
                        baseBackground = baseBackground,
                        isPreviousRow = true,
                    )
                    FlashBitReadoutRow(
                        cells = bitReadoutFrame.currentCells,
                        activeToneColor = activeToneColor,
                        baseBackground = baseBackground,
                        isPreviousRow = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun FlashSignalCanvas(
    mode: FlashSignalVisualizationMode,
    isPlaying: Boolean,
    buckets: List<FskEnergyBucket>,
    bucketFrame: FlashSignalBucketFrame,
    fixedTimelineFrame: FlashSignalFixedTimelineFrame?,
    visualSegments: List<FlashSignalToneSegment>,
    playbackSampleState: FlashVisualPlaybackSampleState,
    sampleRateHz: Int,
    windowSampleCount: Int,
    activeWindowBucketCount: Int,
    activeToneColor: Color,
    inactiveToneColor: Color,
    glowColor: Color,
    baseBackground: Color,
    centerLineColor: Color,
    followData: PayloadFollowViewData?,
    bitReadoutSource: FlashBitReadoutSource?,
    bitReadoutFrame: FlashBitReadoutFrame?,
    bitReadoutSample: Float,
    primitiveEstimate: Int,
    usesFallbackTimeline: Boolean,
    hasBitReadout: Boolean,
    enableViewportEdgeFade: Boolean,
    traceWindowSamples: Int,
    traceWindowStartSample: Int,
    traceWindowEndSample: Int,
    totalSamples: Int,
    modifier: Modifier = Modifier,
) {
    val followDisplayedSamplePosition = playbackSampleState.displayedSample
    val visualTransition = rememberInfiniteTransition(label = "flashSignalCanvas")
    val glowPulseAnimated by visualTransition.animateFloat(
        initialValue = 0.70f,
        targetValue = 1.0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "flashSignalGlowPulse",
    )
    val sweepAnimated by visualTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 3200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "flashSignalSweep",
    )
    val glowPulse = if (isPlaying) glowPulseAnimated else 0.82f
    val sweepPhase = if (isPlaying) sweepAnimated else 0.24f
    val ambientBrush =
        Brush.horizontalGradient(
            colors =
                listOf(
                    inactiveToneColor.copy(alpha = 0.10f + 0.02f * glowPulse),
                    activeToneColor.copy(alpha = 0.12f + 0.02f * sweepPhase),
                    inactiveToneColor.copy(alpha = 0.10f + 0.02f * glowPulse),
                ),
        )

    Box(modifier = modifier.height(112.dp)) {
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(112.dp),
        ) {
            if (buckets.isEmpty() && fixedTimelineFrame == null) {
                return@Canvas
            }

            val corner = CornerRadius(24f, 24f)
            val leftPadding = 12.dp.toPx()
            val rightPadding = 12.dp.toPx()
            val topPadding = 12.dp.toPx()
            val bottomPadding = 12.dp.toPx()
            val innerWidth = (size.width - leftPadding - rightPadding).coerceAtLeast(1f)
            val innerHeight = (size.height - topPadding - bottomPadding).coerceAtLeast(1f)
            val bucketWidth = if (buckets.isNotEmpty()) innerWidth / buckets.size.toFloat() else 1f
            val analysisBucketSampleWidth = if (buckets.isNotEmpty()) windowSampleCount.toFloat() / buckets.size.toFloat() else 1f
            val bucketOffset =
                if (buckets.isNotEmpty()) {
                    ((bucketFrame.displayedSamplePosition - bucketFrame.analysisDisplayedSamplePosition) / analysisBucketSampleWidth)
                        .coerceIn(-FlashSignalMaxVisualBucketOffset, FlashSignalMaxVisualBucketOffset)
                } else {
                    0f
                }
            val scanHeadBucketIndex =
                if (buckets.isNotEmpty()) {
                    (buckets.size * FlashSignalPlayheadAnchorRatio).coerceIn(0f, buckets.lastIndex.toFloat())
                } else {
                    0f
                }
            val activeThresholdBucketIndex =
                if (buckets.isNotEmpty()) {
                    (scanHeadBucketIndex + bucketOffset).coerceIn(0f, buckets.lastIndex.toFloat())
                } else {
                    0f
                }
            val playheadX = leftPadding + innerWidth * FlashSignalPlayheadAnchorRatio
            val fixedViewport =
                fixedTimelineFrame?.let {
                    // Follow timeline rendering is fixed in absolute sample space; playback only
                    // moves the viewport, so short Flash bits do not change shape while playing.
                    val windowStart = followDisplayedSamplePosition - windowSampleCount * FlashSignalPlayheadAnchorRatio
                    FlashSignalViewport(
                        startSample = windowStart,
                        endSample = windowStart + windowSampleCount,
                        playheadSample = followDisplayedSamplePosition,
                    )
                }
            val currentReadoutBit = bitReadoutFrame?.currentBitOffset
            val revealedBitOffset = bitReadoutFrame?.revealedBitOffset ?: -1
            val currentVisualBit = bitReadoutSource?.bitOffsetAtSample(followDisplayedSamplePosition)
            val currentRawBit = bitReadoutSource?.bitOffsetAtSample(playbackSampleState.rawSample)
            if (bitReadoutFrame != null && followData != null) {
                FlashVisualPerfTrace.recordBitReadout(
                    readoutSample = bitReadoutSample,
                    currentBitOffset = currentReadoutBit,
                    revealedBitOffset = revealedBitOffset,
                    groupStart = bitReadoutFrame.currentGroupStartIndex,
                    previousBits = bitReadoutFrame.previousBitsText(),
                    currentBits = bitReadoutFrame.currentBitsText(),
                    visualBitOffset = currentVisualBit,
                    rawBitOffset = currentRawBit,
                )
            }
            followData?.let { data ->
                FlashAlignmentPerfTrace.recordLyrics(
                    isPlaying = isPlaying,
                    sample = followDisplayedSamplePosition.toInt(),
                    state =
                        flashAlignmentLyricsState(
                            followData = data,
                            displayedSamples = followDisplayedSamplePosition.toInt(),
                        ),
                )
            }
            FlashAlignmentPerfTrace.recordVisual(
                mode = mode,
                isPlaying = isPlaying,
                smoothSample = followDisplayedSamplePosition,
                rawSample = playbackSampleState.rawSample,
                readoutSample = bitReadoutSample,
                readoutBit = currentReadoutBit,
                revealedBit = revealedBitOffset,
                visualBit = currentVisualBit,
                rawBit = currentRawBit,
                usesFallbackTimeline = usesFallbackTimeline,
                hasBitReadout = bitReadoutFrame != null,
            )
            FlashVisualPerfTrace.recordMotion(
                rawSample = playbackSampleState.rawSample,
                smoothSample = followDisplayedSamplePosition,
                sampleRateHz = sampleRateHz,
                viewportWidthPx = innerWidth,
                viewportSamples = windowSampleCount,
                windowStartSample = traceWindowStartSample,
                viewportStartSample = fixedViewport?.startSample ?: 0f,
            )
            val visibleSegmentCount =
                fixedViewport
                    ?.let { viewport -> visualSegments.count { segment -> segment.overlaps(viewport) } }
                    ?: 0
            val visiblePrimitiveEstimate =
                flashVisualPrimitiveEstimate(
                    mode = mode,
                    drawableSegments = visibleSegmentCount,
                    buckets = buckets.size,
                    hasFixedTimeline = fixedTimelineFrame != null,
                )
            val drawStartNanos = System.nanoTime()

            drawRoundRect(
                color = baseBackground,
                size = size,
                cornerRadius = corner,
            )
            drawRoundRect(
                brush = ambientBrush,
                size = size,
                cornerRadius = corner,
            )

            when (mode) {
                FlashSignalVisualizationMode.ToneTracks ->
                    if (fixedTimelineFrame != null && fixedViewport != null) {
                        drawToneTrackSegments(
                            segments = visualSegments,
                            viewport = fixedViewport,
                            leftPadding = leftPadding,
                            topPadding = topPadding,
                            innerWidth = innerWidth,
                            innerHeight = innerHeight,
                            activeToneColor = activeToneColor,
                            inactiveToneColor = inactiveToneColor,
                            centerLineColor = centerLineColor,
                            glowPulse = glowPulse,
                            enableViewportEdgeFade = enableViewportEdgeFade,
                        )
                    } else {
                        drawToneTracks(
                            buckets = buckets,
                            activeThresholdBucketIndex = activeThresholdBucketIndex,
                            activeWindowBucketCount = activeWindowBucketCount,
                            bucketOffset = bucketOffset,
                            leftPadding = leftPadding,
                            topPadding = topPadding,
                            innerWidth = innerWidth,
                            innerHeight = innerHeight,
                            bucketWidth = bucketWidth,
                            activeToneColor = activeToneColor,
                            inactiveToneColor = inactiveToneColor,
                            centerLineColor = centerLineColor,
                            glowPulse = glowPulse,
                        )
                    }

                FlashSignalVisualizationMode.ToneEnergy ->
                    if (fixedTimelineFrame != null && fixedViewport != null) {
                        drawToneEnergySegments(
                            segments = visualSegments,
                            viewport = fixedViewport,
                            leftPadding = leftPadding,
                            topPadding = topPadding,
                            innerWidth = innerWidth,
                            innerHeight = innerHeight,
                            activeToneColor = activeToneColor,
                            inactiveToneColor = inactiveToneColor,
                            centerLineColor = centerLineColor,
                            glowPulse = glowPulse,
                            enableViewportEdgeFade = enableViewportEdgeFade,
                        )
                    } else {
                        drawToneEnergy(
                            buckets = buckets,
                            activeThresholdBucketIndex = activeThresholdBucketIndex,
                            activeWindowBucketCount = activeWindowBucketCount,
                            bucketOffset = bucketOffset,
                            leftPadding = leftPadding,
                            topPadding = topPadding,
                            innerWidth = innerWidth,
                            innerHeight = innerHeight,
                            bucketWidth = bucketWidth,
                            activeToneColor = activeToneColor,
                            inactiveToneColor = inactiveToneColor,
                            centerLineColor = centerLineColor,
                            glowPulse = glowPulse,
                        )
                    }

                FlashSignalVisualizationMode.PitchLadder ->
                    if (fixedTimelineFrame != null && fixedViewport != null) {
                        drawPitchLadderSegments(
                            segments = visualSegments,
                            viewport = fixedViewport,
                            leftPadding = leftPadding,
                            topPadding = topPadding,
                            innerWidth = innerWidth,
                            innerHeight = innerHeight,
                            activeToneColor = activeToneColor,
                            inactiveToneColor = inactiveToneColor,
                            centerLineColor = centerLineColor,
                            glowPulse = glowPulse,
                            enableViewportEdgeFade = enableViewportEdgeFade,
                        )
                    } else {
                        drawPitchLadder(
                            buckets = buckets,
                            activeThresholdBucketIndex = activeThresholdBucketIndex,
                            activeWindowBucketCount = activeWindowBucketCount,
                            bucketOffset = bucketOffset,
                            leftPadding = leftPadding,
                            topPadding = topPadding,
                            innerWidth = innerWidth,
                            innerHeight = innerHeight,
                            bucketWidth = bucketWidth,
                            activeToneColor = activeToneColor,
                            inactiveToneColor = inactiveToneColor,
                            centerLineColor = centerLineColor,
                            glowPulse = glowPulse,
                        )
                    }
            }

            drawLine(
                color = glowColor.copy(alpha = 0.80f),
                start = Offset(playheadX, topPadding),
                end = Offset(playheadX, size.height - bottomPadding),
                strokeWidth = 2.dp.toPx(),
            )
            val drawDurationNanos = System.nanoTime() - drawStartNanos
            FlashVisualPerfTrace.recordDraw(
                mode = mode,
                isPlaying = isPlaying,
                displayedSample = followDisplayedSamplePosition,
                drawableSegments = visualSegments.size,
                exactSegments = fixedTimelineFrame?.segments?.size ?: 0,
                primitiveEstimate = primitiveEstimate,
                visibleSegments = visibleSegmentCount,
                visiblePrimitiveEstimate = visiblePrimitiveEstimate,
                drawDurationNanos = drawDurationNanos,
                buckets = buckets.size,
                hasFixedTimeline = fixedTimelineFrame != null,
                usesFallbackTimeline = usesFallbackTimeline,
                hasBitReadout = hasBitReadout,
                windowSamples = traceWindowSamples,
                totalSamples = totalSamples,
                windowStartSample = traceWindowStartSample,
                windowEndSample = traceWindowEndSample,
            )
        }
    }
}

@Composable
private fun FlashVisualFpsOverlay(
    snapshot: FlashVisualPerfSnapshot,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.White,
        contentColor = Color.Black,
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text =
                "FPS ${snapshot.drawFps.toInt()}  " +
                    "avg ${"%.1f".format(snapshot.drawAvgMs)}ms  " +
                    "max ${"%.1f".format(snapshot.drawMaxMs)}ms\n" +
                    "raw ${"%.1f".format(snapshot.rawUpdatesPerSecond)}/s  " +
                    "step ${"%.1f".format(snapshot.rawStepAvgMs)}ms  " +
                    "max ${"%.1f".format(snapshot.rawStepMaxMs)}ms\n" +
                    "smooth ${"%.1f".format(snapshot.smoothStepAvgMs)}ms  " +
                    "err ${"%.1f".format(snapshot.visualErrorMs)}ms  " +
                    "px ${"%.2f".format(snapshot.visualPxStepAvg)}/${"%.2f".format(snapshot.visualPxStepMax)}\n" +
                    "jump ${"%.1f".format(snapshot.anchorJumpMaxMs)}ms  " +
                    "reset ${snapshot.smoothResetCount}  " +
                    "vp ${"%.1f".format(snapshot.viewportStartStepMaxMs)}ms",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun FlashBitReadoutRow(
    cells: List<FlashBitReadoutCell>,
    activeToneColor: androidx.compose.ui.graphics.Color,
    baseBackground: androidx.compose.ui.graphics.Color,
    isPreviousRow: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        cells.forEach { cell ->
            val cellBackground =
                when {
                    cell.isCurrent -> activeToneColor.copy(alpha = 0.18f)
                    cell.bit != null && isPreviousRow -> baseBackground.copy(alpha = 0.40f)
                    cell.bit != null -> baseBackground.copy(alpha = 0.52f)
                    else -> baseBackground.copy(alpha = 0.24f)
                }
            val cellColor =
                when {
                    cell.isCurrent -> activeToneColor.copy(alpha = 0.94f)
                    cell.bit != null && isPreviousRow -> activeToneColor.copy(alpha = 0.48f)
                    cell.bit != null -> activeToneColor.copy(alpha = 0.72f)
                    else -> activeToneColor.copy(alpha = 0f)
                }
            Text(
                text = cell.bit?.toString().orEmpty(),
                modifier =
                    Modifier
                        .weight(1f)
                        .height(24.dp)
                        .background(
                            color = cellBackground,
                            shape = RoundedCornerShape(4.dp),
                        ).padding(vertical = 1.dp),
                color = cellColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 17.sp,
                fontWeight = if (cell.isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private const val FlashSignalMinBucketCount = 56
private const val FlashSignalMaxBucketCount = 124
private const val FlashSignalMaxVisualBucketOffset = 3f

private data class FlashSignalBucketFrame(
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

private data class FlashSignalFixedTimelineFrame(
    val segments: List<FlashSignalToneSegment>,
    val drawableSegments: List<FlashSignalToneSegment>,
    val totalSamples: Int,
)

private fun flashVisualPrimitiveEstimate(
    mode: FlashSignalVisualizationMode,
    drawableSegments: Int,
    buckets: Int,
    hasFixedTimeline: Boolean,
): Int =
    if (hasFixedTimeline) {
        when (mode) {
            FlashSignalVisualizationMode.ToneTracks -> drawableSegments * 2
            FlashSignalVisualizationMode.ToneEnergy -> drawableSegments * 2
            FlashSignalVisualizationMode.PitchLadder -> drawableSegments
        }
    } else {
        when (mode) {
            FlashSignalVisualizationMode.ToneTracks -> buckets * 2
            FlashSignalVisualizationMode.ToneEnergy -> buckets * 2
            FlashSignalVisualizationMode.PitchLadder -> buckets * 2
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

private data class FlashVisualPlaybackSampleState(
    val rawSample: Float,
    val displayedSample: Float,
)

@Composable
private fun rememberFlashVisualPlaybackSampleState(
    rawSample: Float,
    isPlaying: Boolean,
    playbackSpeed: Float,
    sampleRateHz: Int,
    totalSamples: Int,
): FlashVisualPlaybackSampleState {
    var visualSample by remember { mutableFloatStateOf(rawSample) }
    val safeSpeed = playbackSpeed.coerceIn(0.1f, 4f)
    val latestAnchorSample by rememberUpdatedState(rawSample)
    val latestTotalSamples by rememberUpdatedState(totalSamples)
    if (!isPlaying || sampleRateHz <= 0 || totalSamples <= 0) {
        return FlashVisualPlaybackSampleState(
            rawSample = rawSample,
            displayedSample = visualSample.coerceIn(0f, totalSamples.coerceAtLeast(1).toFloat()),
        )
    }
    LaunchedEffect(safeSpeed, sampleRateHz, totalSamples) {
        FlashVisualPerfTrace.recordSmoothReset(
            anchorSample = latestAnchorSample,
            previousSmoothSample = visualSample,
            sampleRateHz = sampleRateHz,
        )
        val maxSample = latestTotalSamples.toFloat()
        visualSample = visualSample.coerceIn(0f, maxSample)
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
            // Raw playback position arrives in coarse jumps. Keep a continuous
            // frame-driven clock and only re-anchor when drift is large enough
            // that the visual timeline would otherwise detach from playback.
            visualSample =
                if (kotlin.math.abs(anchor - predictedSample) > sampleRateHz * 0.35f) {
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

private data class FlashBitReadoutSource(
    val entries: List<PayloadFollowBinaryGroupTimelineEntry>,
    val bitByOffset: Map<Int, Char>,
)

private fun PayloadFollowViewData.toFlashBitReadoutSource(): FlashBitReadoutSource? {
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

private fun flashBitReadoutFrame(
    source: FlashBitReadoutSource,
    sample: Float,
): FlashBitReadoutFrame? {
    if (source.entries.isEmpty() || source.bitByOffset.isEmpty()) {
        return null
    }
    val playbackIndex = flashTimelinePlaybackIndex(entries = source.entries, sample = sample)
    val currentGroupStartIndex = (playbackIndex.revealedBitOffset.coerceAtLeast(0) / FlashBitReadoutGroupSize) * FlashBitReadoutGroupSize
    val previousGroupStartIndex = currentGroupStartIndex - FlashBitReadoutGroupSize
    return FlashBitReadoutFrame(
        currentGroupStartIndex = currentGroupStartIndex,
        currentBitOffset = playbackIndex.currentBitOffset,
        revealedBitOffset = playbackIndex.revealedBitOffset,
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
                revealThroughIndex = playbackIndex.revealedBitOffset,
                currentBitOffset = playbackIndex.currentBitOffset,
            ),
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

private data class FlashTimelinePlaybackIndex(
    val currentBitOffset: Int?,
    val revealedBitOffset: Int,
)

private fun flashTimelinePlaybackIndex(
    entries: List<PayloadFollowBinaryGroupTimelineEntry>,
    sample: Float,
): FlashTimelinePlaybackIndex {
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
                val currentBitOffset = entry.bitOffsetAtSample(sample)
                return FlashTimelinePlaybackIndex(currentBitOffset = currentBitOffset, revealedBitOffset = currentBitOffset)
            }
        }
    }
    return FlashTimelinePlaybackIndex(currentBitOffset = null, revealedBitOffset = previousRevealedBitOffset)
}

private fun FlashBitReadoutSource.bitOffsetAtSample(sample: Float): Int? =
    flashTimelinePlaybackIndex(entries = entries, sample = sample).currentBitOffset

internal fun FlashBitReadoutFrame.currentBitsText(): String = currentCells.joinToString(separator = "") { it.bit?.toString() ?: "_" }

internal fun FlashBitReadoutFrame.previousBitsText(): String = previousCells.joinToString(separator = "") { it.bit?.toString() ?: "_" }

private val PayloadFollowBinaryGroupTimelineEntry.lastBitOffset: Int
    get() = bitOffset + bitCount - 1

private fun PayloadFollowBinaryGroupTimelineEntry.bitOffsetAtSample(sample: Float): Int {
    if (bitCount <= 1 || sampleCount <= 0) {
        return bitOffset
    }
    val progress = ((sample - startSample.toFloat()) / sampleCount.toFloat()).coerceIn(0f, 0.9999f)
    return bitOffset + (progress * bitCount.toFloat()).toInt().coerceIn(0, bitCount - 1)
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

private const val FlashBitReadoutGroupSize = 8

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

private data class FlashSignalAnalysisCacheKey(
    val source: String,
    val currentSample: Int,
    val windowSampleCount: Int,
    val targetBucketCount: Int,
)

private const val FlashSignalAnalysisCacheMaxEntries = 12

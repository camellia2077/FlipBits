package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.domain.PayloadFollowBinaryGroupTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.state.FlashVisualWindowState
import com.bag.audioandroid.ui.theme.appThemeVisualTokens

@Composable
internal fun AudioFlashSignalVisualizer(
    input: FlashSignalVisualizationInput,
    isPlaying: Boolean,
    mode: FlashSignalVisualizationMode,
    flashVoicingStyle: FlashVoicingStyleOption?,
    flashVisualWindow: FlashVisualWindowState = FlashVisualWindowState(),
    sharedPlaybackSampleState: FlashVisualPlaybackSampleState? = null,
    showPerfOverlay: Boolean = false,
    playbackSpeed: Float = 1f,
    isScrubbing: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val pcm = input.pcm
    val sampleRateHz = input.sampleRateHz
    if (pcm.isEmpty() || sampleRateHz <= 0) {
        return
    }

    val visualTokens = appThemeVisualTokens()
    SideEffect {
        FlashVisualPerfTrace.setEnabled(showPerfOverlay)
    }
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .height(if (showPerfOverlay) 230.dp else 170.dp),
    ) {
        val sceneState =
            rememberFlashSignalVisualizerSceneState(
                density = density,
                maxWidth = maxWidth,
                sampleRateHz = sampleRateHz,
                input = input,
                isPlaying = isPlaying,
                mode = mode,
                flashVoicingStyle = flashVoicingStyle,
                flashVisualWindow = flashVisualWindow,
                sharedPlaybackSampleState = sharedPlaybackSampleState,
                playbackSpeed = playbackSpeed,
                isScrubbing = isScrubbing,
                visualTokens = visualTokens,
                colorScheme = MaterialTheme.colorScheme,
            )
        recordFlashSignalVisualizerComposeTrace(
            mode = mode,
            isPlaying = isPlaying,
            renderState = sceneState.renderState,
        )
        FlashSignalVisualizerContent(
            mode = mode,
            isPlaying = isPlaying,
            sampleRateHz = sampleRateHz,
            showPerfOverlay = showPerfOverlay,
            sceneState = sceneState,
        )
    }
}

@Composable
private fun FlashSignalVisualizerContent(
    mode: FlashSignalVisualizationMode,
    isPlaying: Boolean,
    sampleRateHz: Int,
    showPerfOverlay: Boolean,
    sceneState: FlashSignalVisualizerSceneState,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FlashSignalCanvas(
            mode = mode,
            isPlaying = isPlaying,
            renderState = sceneState.renderState,
            sampleRateHz = sampleRateHz,
            windowSampleCount = sceneState.layoutModel.windowSampleCount,
            activeToneColor = sceneState.visualStyle.activeToneColor,
            inactiveToneColor = sceneState.visualStyle.inactiveToneColor,
            glowColor = sceneState.visualStyle.glowColor,
            baseBackground = sceneState.visualStyle.baseBackground,
            centerLineColor = sceneState.visualStyle.centerLineColor,
            pulseGuideColor = sceneState.visualStyle.pulseGuideColor,
            referenceLabelColor = sceneState.visualStyle.referenceLabelColor,
            showPerfOverlay = showPerfOverlay,
            modifier = Modifier.fillMaxWidth(),
        )
        if (showPerfOverlay) {
            FlashVisualFpsOverlay(
                snapshot = FlashVisualPerfTrace.snapshot(),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
            )
        }
        FlashSignalBitReadoutSection(
            bitReadoutFrame = sceneState.renderState.bitReadoutFrame,
            activeToneColor = sceneState.visualStyle.activeToneColor,
            baseBackground = sceneState.visualStyle.baseBackground,
        )
    }
}

private fun recordFlashSignalVisualizerComposeTrace(
    mode: FlashSignalVisualizationMode,
    isPlaying: Boolean,
    renderState: FlashSignalVisualizerRenderState,
) {
    FlashVisualPerfTrace.recordCompose(
        mode = mode,
        isPlaying = isPlaying,
        displayedSample = renderState.playbackSampleState.displayedSample,
        drawableSegments = renderState.visualSegments.size,
        exactSegments = renderState.fixedTimelineFrame?.segments?.size ?: 0,
        primitiveEstimate = renderState.primitiveEstimate,
        buckets = renderState.buckets.size,
        hasFixedTimeline = renderState.fixedTimelineFrame != null,
        usesFallbackTimeline = renderState.usesFallbackTimeline,
        hasBitReadout = renderState.bitReadoutFrame != null,
        windowSamples = renderState.traceWindowSamples,
        totalSamples = renderState.totalSamples,
        windowStartSample = renderState.traceWindowStartSample,
        windowEndSample = renderState.traceWindowEndSample,
    )
}

@Composable
private fun FlashSignalBitReadoutSection(
    bitReadoutFrame: FlashBitReadoutFrame?,
    activeToneColor: Color,
    baseBackground: Color,
) {
    if (bitReadoutFrame == null) {
        return
    }
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

@Composable
private fun FlashSignalCanvas(
    mode: FlashSignalVisualizationMode,
    isPlaying: Boolean,
    renderState: FlashSignalVisualizerRenderState,
    sampleRateHz: Int,
    windowSampleCount: Int,
    activeToneColor: Color,
    inactiveToneColor: Color,
    glowColor: Color,
    baseBackground: Color,
    centerLineColor: Color,
    pulseGuideColor: Color,
    referenceLabelColor: Color,
    showPerfOverlay: Boolean,
    modifier: Modifier = Modifier,
) {
    val canvasVisualState =
        rememberFlashSignalCanvasVisualState(
            isPlaying = isPlaying,
            renderState = renderState,
            activeToneColor = activeToneColor,
            inactiveToneColor = inactiveToneColor,
        )
    val canvasDecisionState = remember { mutableStateOf<FlashSignalCanvasDecision?>(null) }

    Box(modifier = modifier.height(112.dp)) {
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(112.dp),
        ) {
            if (renderState.buckets.isEmpty() && renderState.fixedTimelineFrame == null && renderState.toneSpectrumBuckets.isEmpty()) {
                return@Canvas
            }

            val corner = CornerRadius(24f, 24f)
            val leftPadding = 12.dp.toPx()
            val rightPadding = 12.dp.toPx()
            val topPadding = 12.dp.toPx()
            val bottomPadding = 12.dp.toPx()
            val innerWidth = (size.width - leftPadding - rightPadding).coerceAtLeast(1f)
            val innerHeight = (size.height - topPadding - bottomPadding).coerceAtLeast(1f)
            val canvasDecision =
                buildFlashSignalCanvasDecision(
                    mode = mode,
                    renderState = renderState,
                    runtimeState = canvasVisualState.runtimeState,
                    followDisplayedSamplePosition = canvasVisualState.followDisplayedSamplePosition,
                    windowSampleCount = windowSampleCount,
                    innerWidth = innerWidth,
                    showPerfOverlay = showPerfOverlay,
                )
            canvasDecisionState.value = canvasDecision
            recordFlashSignalCanvasTelemetry(
                mode = mode,
                isPlaying = isPlaying,
                renderState = renderState,
                runtimeState = canvasVisualState.runtimeState,
                followDisplayedSamplePosition = canvasVisualState.followDisplayedSamplePosition,
                sampleRateHz = sampleRateHz,
                windowSampleCount = windowSampleCount,
                viewportStartSample = canvasDecision.fixedViewport?.startSample ?: 0f,
                viewportWidthPx = innerWidth,
            )
            val drawMetrics =
                buildFlashSignalDrawMetrics(
                    mode = mode,
                    renderState = renderState,
                    fixedViewport = canvasDecision.fixedViewport,
                )
            val drawStartNanos = System.nanoTime()

            drawRoundRect(
                color = baseBackground,
                size = size,
                cornerRadius = corner,
            )
            drawRoundRect(
                brush = canvasVisualState.ambientBrush,
                size = size,
                cornerRadius = corner,
            )
            drawFlashSignalCanvasContent(
                canvasDecision = canvasDecision,
                renderState = renderState,
                isPlaying = isPlaying,
                displayedSample = canvasVisualState.followDisplayedSamplePosition,
                sampleRateHz = sampleRateHz,
                activeToneColor = activeToneColor,
                inactiveToneColor = inactiveToneColor,
                centerLineColor = centerLineColor,
                referenceLabelColor = referenceLabelColor,
                glowPulse = canvasVisualState.glowPulse,
                leftPadding = leftPadding,
                topPadding = topPadding,
                innerWidth = innerWidth,
                innerHeight = innerHeight,
                showPerfOverlay = showPerfOverlay,
            )
            val drawDurationNanos = System.nanoTime() - drawStartNanos
            FlashVisualPerfTrace.recordDraw(
                mode = mode,
                isPlaying = isPlaying,
                displayedSample = canvasVisualState.followDisplayedSamplePosition,
                drawableSegments = renderState.visualSegments.size,
                exactSegments = renderState.fixedTimelineFrame?.segments?.size ?: 0,
                primitiveEstimate = renderState.primitiveEstimate,
                visibleSegments = drawMetrics.visibleSegmentCount,
                visiblePrimitiveEstimate = drawMetrics.visiblePrimitiveEstimate,
                drawDurationNanos = drawDurationNanos,
                buckets = drawMetrics.visibleBucketCount,
                hasFixedTimeline = renderState.fixedTimelineFrame != null,
                usesFallbackTimeline = renderState.usesFallbackTimeline,
                hasBitReadout = renderState.bitReadoutFrame != null,
                windowSamples = renderState.traceWindowSamples,
                totalSamples = renderState.totalSamples,
                windowStartSample = renderState.traceWindowStartSample,
                windowEndSample = renderState.traceWindowEndSample,
            )
        }
        val canvasDecision = canvasDecisionState.value
        FlashSignalCanvasOverlays(
            mode = mode,
            canvasDecision = canvasDecision,
            runtimeState = canvasVisualState.runtimeState,
            activeToneColor = activeToneColor,
            inactiveToneColor = inactiveToneColor,
            pulseGuideColor = pulseGuideColor,
            glowColor = glowColor,
        )
    }
}

private fun recordFlashSignalCanvasTelemetry(
    mode: FlashSignalVisualizationMode,
    isPlaying: Boolean,
    renderState: FlashSignalVisualizerRenderState,
    runtimeState: FlashSignalCanvasRuntimeState,
    followDisplayedSamplePosition: Float,
    sampleRateHz: Int,
    windowSampleCount: Int,
    viewportStartSample: Float,
    viewportWidthPx: Float,
) {
    val telemetryState = runtimeState.telemetryState
    if (renderState.bitReadoutFrame != null && renderState.followData != null) {
        FlashVisualPerfTrace.recordBitReadout(
            readoutSample = renderState.bitReadoutSample,
            currentBitOffset = telemetryState.currentReadoutBit,
            revealedBitOffset = telemetryState.revealedBitOffset,
            groupStart = renderState.bitReadoutFrame.currentGroupStartIndex,
            previousBits = renderState.bitReadoutFrame.previousBitsText(),
            currentBits = renderState.bitReadoutFrame.currentBitsText(),
            visualBitOffset = telemetryState.currentVisualBit,
            rawBitOffset = telemetryState.currentRawBit,
        )
    }
    renderState.followData?.let { data ->
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
        rawSample = renderState.playbackSampleState.rawSample,
        readoutSample = renderState.bitReadoutSample,
        readoutBit = telemetryState.currentReadoutBit,
        readoutBitValue = telemetryState.currentReadoutBitValue,
        revealedBit = telemetryState.revealedBitOffset,
        visualBit = telemetryState.currentVisualBit,
        rawBit = telemetryState.currentRawBit,
        visualBitValue = runtimeState.laneActiveBitState?.bitValue,
        usesFallbackTimeline = renderState.usesFallbackTimeline,
        hasBitReadout = renderState.bitReadoutFrame != null,
    )
    PlaybackStartupTrace.recordFlashVisual(
        mode = mode,
        isPlaying = isPlaying,
        sampleRateHz = sampleRateHz,
        rawSample = renderState.playbackSampleState.rawSample,
        smoothSample = followDisplayedSamplePosition,
        viewportStartSample = viewportStartSample,
        windowStartSample = renderState.traceWindowStartSample,
        windowEndSample = renderState.traceWindowEndSample,
        totalSamples = renderState.totalSamples,
        readoutBit = telemetryState.currentReadoutBit,
        visualBit = telemetryState.currentVisualBit,
        rawBit = telemetryState.currentRawBit,
        usesFallbackTimeline = renderState.usesFallbackTimeline,
        hasBitReadout = renderState.bitReadoutFrame != null,
    )
    FlashVisualPerfTrace.recordMotion(
        rawSample = renderState.playbackSampleState.rawSample,
        smoothSample = followDisplayedSamplePosition,
        sampleRateHz = sampleRateHz,
        viewportWidthPx = viewportWidthPx,
        viewportSamples = windowSampleCount,
        windowStartSample = renderState.traceWindowStartSample,
        viewportStartSample = viewportStartSample,
    )
}

private fun DrawScope.drawFlashSignalCanvasContent(
    canvasDecision: FlashSignalCanvasDecision,
    renderState: FlashSignalVisualizerRenderState,
    isPlaying: Boolean,
    displayedSample: Float,
    sampleRateHz: Int,
    activeToneColor: Color,
    inactiveToneColor: Color,
    centerLineColor: Color,
    referenceLabelColor: Color,
    glowPulse: Float,
    leftPadding: Float,
    topPadding: Float,
    innerWidth: Float,
    innerHeight: Float,
    showPerfOverlay: Boolean,
) {
    when (val drawContent = canvasDecision.drawContent) {
        is FlashSignalDrawContent.LaneSegments ->
            drawBitCellSegments(
                segments = renderState.visualSegments,
                viewport = drawContent.viewport,
                leftPadding = leftPadding,
                topPadding = topPadding,
                innerWidth = innerWidth,
                innerHeight = innerHeight,
                activeToneColor = activeToneColor,
                inactiveToneColor = inactiveToneColor,
                centerLineColor = centerLineColor,
                glowPulse = glowPulse,
                enableViewportEdgeFade = renderState.enableViewportEdgeFade,
            )

        is FlashSignalDrawContent.LaneBuckets ->
            drawBitCells(
                buckets = renderState.buckets,
                activeThresholdBucketIndex = canvasDecision.activeThresholdBucketIndex,
                activeWindowBucketCount = renderState.activeWindowBucketCount,
                bucketOffset = drawContent.bucketOffset,
                leftPadding = leftPadding,
                topPadding = topPadding,
                innerWidth = innerWidth,
                innerHeight = innerHeight,
                bucketWidth = canvasDecision.bucketWidth,
                activeToneColor = activeToneColor,
                inactiveToneColor = inactiveToneColor,
                centerLineColor = centerLineColor,
                glowPulse = glowPulse,
            )

        is FlashSignalDrawContent.PitchSegments ->
            drawPitchSegments(
                segments = renderState.visualSegments,
                viewport = drawContent.viewport,
                leftPadding = leftPadding,
                topPadding = topPadding,
                innerWidth = innerWidth,
                innerHeight = innerHeight,
                activeToneColor = activeToneColor,
                inactiveToneColor = inactiveToneColor,
                centerLineColor = centerLineColor,
                glowPulse = glowPulse,
                enableViewportEdgeFade = renderState.enableViewportEdgeFade,
            )

        is FlashSignalDrawContent.PitchBuckets ->
            drawPitch(
                buckets = renderState.buckets,
                activeThresholdBucketIndex = canvasDecision.activeThresholdBucketIndex,
                activeWindowBucketCount = renderState.activeWindowBucketCount,
                bucketOffset = drawContent.bucketOffset,
                leftPadding = leftPadding,
                topPadding = topPadding,
                innerWidth = innerWidth,
                innerHeight = innerHeight,
                bucketWidth = canvasDecision.bucketWidth,
                activeToneColor = activeToneColor,
                inactiveToneColor = inactiveToneColor,
                centerLineColor = centerLineColor,
                glowPulse = glowPulse,
            )

        is FlashSignalDrawContent.HzSegments,
        is FlashSignalDrawContent.HzBuckets,
        ->
            drawFlashHzContent(
                drawContent = drawContent,
                canvasDecision = canvasDecision,
                renderState = renderState,
                isPlaying = isPlaying,
                displayedSample = displayedSample,
                sampleRateHz = sampleRateHz,
                activeToneColor = activeToneColor,
                inactiveToneColor = inactiveToneColor,
                centerLineColor = centerLineColor,
                referenceLabelColor = referenceLabelColor,
                glowPulse = glowPulse,
                leftPadding = leftPadding,
                topPadding = topPadding,
                innerWidth = innerWidth,
                innerHeight = innerHeight,
                showPerfOverlay = showPerfOverlay,
            )

        FlashSignalDrawContent.Pulse -> Unit
    }
}

private fun DrawScope.drawFlashHzContent(
    drawContent: FlashSignalDrawContent,
    canvasDecision: FlashSignalCanvasDecision,
    renderState: FlashSignalVisualizerRenderState,
    isPlaying: Boolean,
    displayedSample: Float,
    sampleRateHz: Int,
    activeToneColor: Color,
    inactiveToneColor: Color,
    centerLineColor: Color,
    referenceLabelColor: Color,
    glowPulse: Float,
    leftPadding: Float,
    topPadding: Float,
    innerWidth: Float,
    innerHeight: Float,
    showPerfOverlay: Boolean,
) {
    val toneDrawStats =
        if (drawContent is FlashSignalDrawContent.HzSegments) {
            drawToneTimelineSegments(
                segments = renderState.visualSegments,
                viewport = drawContent.viewport,
                frequencyScale = renderState.toneFrequencyScale,
                carrierLayout = renderState.toneCarrierLayout,
                leftPadding = leftPadding,
                topPadding = topPadding,
                innerWidth = innerWidth,
                innerHeight = innerHeight,
                activeToneColor = activeToneColor,
                inactiveToneColor = inactiveToneColor,
                centerLineColor = centerLineColor,
                referenceLabelColor = referenceLabelColor,
                glowPulse = glowPulse,
            )
        } else {
            drawContent as FlashSignalDrawContent.HzBuckets
            drawToneSpectrum(
                buckets = renderState.toneSpectrumBuckets,
                frequencyScale = renderState.toneFrequencyScale,
                activeThresholdBucketIndex = canvasDecision.activeThresholdBucketIndex,
                bucketOffset = drawContent.bucketOffset,
                leftPadding = leftPadding,
                topPadding = topPadding,
                innerWidth = innerWidth,
                innerHeight = innerHeight,
                bucketWidth = canvasDecision.bucketWidth,
                activeToneColor = activeToneColor,
                inactiveToneColor = inactiveToneColor,
                centerLineColor = centerLineColor,
                referenceLabelColor = referenceLabelColor,
                glowPulse = glowPulse,
            )
        }
    if (showPerfOverlay) {
        recordToneTrace(
            isPlaying = isPlaying,
            displayedSample = displayedSample,
            sampleRateHz = sampleRateHz,
            totalSamples = renderState.totalSamples,
            buckets = renderState.toneSpectrumBuckets,
            segments = renderState.visualSegments,
            frequencyScale = renderState.toneFrequencyScale,
            drawStats = toneDrawStats,
            activeThresholdBucketIndex = canvasDecision.activeThresholdBucketIndex,
            bucketWidth = canvasDecision.bucketWidth,
            source = if (renderState.fixedTimelineFrame != null) "layout" else "spectrum",
        )
    }
}

internal data class FlashLaneActiveBitState(
    val bitOffset: Int,
    val bitValue: Char?,
    val startSample: Float,
    val endSample: Float,
)

internal fun flashLaneActiveBitState(
    entries: List<PayloadFollowBinaryGroupTimelineEntry>,
    bitByOffset: Map<Int, Char>,
    sample: Float,
): FlashLaneActiveBitState? {
    val activeEntry =
        entries.firstOrNull { entry ->
            sample >= entry.startSample && sample < entry.startSample + entry.sampleCount
        } ?: return null
    if (activeEntry.bitCount <= 0 || activeEntry.sampleCount <= 0) {
        return null
    }
    val bitProgress = activeEntry.bitProgressAtSample(sample)
    val bitIndexWithinEntry = bitProgress.toInt().coerceIn(0, activeEntry.bitCount - 1)
    val bitOffset = activeEntry.bitOffset + bitIndexWithinEntry
    val bitSampleWidth = activeEntry.sampleCount.toFloat() / activeEntry.bitCount.toFloat()
    val bitStartSample = activeEntry.startSample + bitSampleWidth * bitIndexWithinEntry.toFloat()
    val bitEndSample = bitStartSample + bitSampleWidth
    return FlashLaneActiveBitState(
        bitOffset = bitOffset,
        bitValue = bitByOffset[bitOffset],
        startSample = bitStartSample,
        endSample = bitEndSample,
    )
}

internal data class FlashTokenAlignmentState(
    val activeTokenIndex: Int,
    val activeByteIndexWithinToken: Int,
    val activeBitIndexWithinByte: Int,
    val globalBitOffset: Int,
    val currentBitValue: Char?,
    val byteHex: String,
    val byteBinary: String,
)

internal fun flashTokenAlignmentState(
    followData: PayloadFollowViewData,
    displayedSamples: Int,
): FlashTokenAlignmentState {
    val rawDisplayUnitsByToken = followData.textRawDisplayUnits.groupBy { it.tokenIndex }
    val activeTokenIndex = activeTextTimelineIndex(followData.textTokenTimeline, displayedSamples)
    val activeByteIndexWithinToken =
        activeByteIndexWithinToken(
            activeTextIndex = activeTokenIndex,
            displayedSamples = displayedSamples,
            rawDisplayUnitsByToken = rawDisplayUnitsByToken,
        )
    val activeBitPosition =
        activeBitPositionWithinByte(
            activeTextIndex = activeTokenIndex,
            activeByteIndexWithinToken = activeByteIndexWithinToken,
            displayedSamples = displayedSamples,
            followData = followData,
            rawDisplayUnitsByToken = rawDisplayUnitsByToken,
        )
    val activeUnit =
        rawDisplayUnitsByToken[activeTokenIndex]
            .orEmpty()
            .firstOrNull { it.byteIndexWithinToken == activeByteIndexWithinToken }
    val globalBitOffset =
        activeUnit
            ?.takeIf { activeBitPosition.bitIndexWithinByte >= 0 }
            ?.let { it.byteOffset * 8 + activeBitPosition.bitIndexWithinByte }
            ?: -1
    val byteBinary = activeUnit?.binaryText?.filter { it == '0' || it == '1' }.orEmpty()
    return FlashTokenAlignmentState(
        activeTokenIndex = activeTokenIndex,
        activeByteIndexWithinToken = activeByteIndexWithinToken,
        activeBitIndexWithinByte = activeBitPosition.bitIndexWithinByte,
        globalBitOffset = globalBitOffset,
        currentBitValue = byteBinary.getOrNull(activeBitPosition.bitIndexWithinByte),
        byteHex = activeUnit?.hexText.orEmpty(),
        byteBinary = byteBinary,
    )
}

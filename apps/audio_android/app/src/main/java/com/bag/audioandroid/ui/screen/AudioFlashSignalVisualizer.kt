package com.bag.audioandroid.ui.screen

import android.util.Log
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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bag.audioandroid.domain.PayloadFollowBinaryGroupTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.state.FlashVisualWindowState
import com.bag.audioandroid.ui.theme.AppThemeVisualTokens
import com.bag.audioandroid.ui.theme.appThemeVisualTokens
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

// Controls only the Flash Visual viewport density. Playback progress and audio
// timeline semantics stay based on absolute samples.
private const val FlashSignalViewportSeconds = 0.80f
private const val FlashToneTraceTag = "FlashToneTrace"
private const val FlashToneTraceIntervalNanos = 1_000_000_000L
private var flashToneTraceLastLogNanos = 0L

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
private fun rememberFlashSignalVisualizerSceneState(
    density: androidx.compose.ui.unit.Density,
    maxWidth: Dp,
    sampleRateHz: Int,
    input: FlashSignalVisualizationInput,
    isPlaying: Boolean,
    mode: FlashSignalVisualizationMode,
    flashVoicingStyle: FlashVoicingStyleOption?,
    flashVisualWindow: FlashVisualWindowState,
    sharedPlaybackSampleState: FlashVisualPlaybackSampleState?,
    playbackSpeed: Float,
    isScrubbing: Boolean,
    visualTokens: AppThemeVisualTokens,
    colorScheme: ColorScheme,
): FlashSignalVisualizerSceneState {
    val layoutModel =
        rememberFlashSignalVisualizerLayoutModel(
            density = density,
            maxWidth = maxWidth,
            sampleRateHz = sampleRateHz,
        )
    val renderState =
        rememberFlashSignalVisualizerRenderState(
            input = input,
            isPlaying = isPlaying,
            mode = mode,
            flashVoicingStyle = flashVoicingStyle,
            flashVisualWindow = flashVisualWindow,
            sharedPlaybackSampleState = sharedPlaybackSampleState,
            playbackSpeed = playbackSpeed,
            isScrubbing = isScrubbing,
            targetBucketCount = layoutModel.targetBucketCount,
            windowSampleCount = layoutModel.windowSampleCount,
        )
    val visualStyle =
        rememberFlashSignalVisualStyle(
            colorScheme = colorScheme,
            visualTokens = visualTokens,
        )
    return remember(layoutModel, renderState, visualStyle) {
        FlashSignalVisualizerSceneState(
            layoutModel = layoutModel,
            renderState = renderState,
            visualStyle = visualStyle,
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

@Composable
private fun rememberFlashSignalVisualizerLayoutModel(
    density: androidx.compose.ui.unit.Density,
    maxWidth: Dp,
    sampleRateHz: Int,
): FlashSignalVisualizerLayoutModel {
    val widthPx = with(density) { maxWidth.toPx() }
    val targetBucketCount =
        remember(widthPx, density) {
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
    return remember(targetBucketCount, windowSampleCount) {
        FlashSignalVisualizerLayoutModel(
            targetBucketCount = targetBucketCount,
            windowSampleCount = windowSampleCount,
        )
    }
}

@Composable
private fun rememberFlashSignalVisualStyle(
    colorScheme: ColorScheme,
    visualTokens: AppThemeVisualTokens,
): FlashSignalVisualStyle =
    remember(visualTokens, colorScheme) {
        FlashSignalVisualStyle(
            activeToneColor = colorScheme.primary,
            inactiveToneColor = visualTokens.visualizationInactiveToneColor,
            glowColor = colorScheme.onPrimaryContainer,
            baseBackground = visualTokens.visualizationBaseBackgroundColor,
            centerLineColor = flashSignalOutlineColor(visualTokens.subtleOutlineColor),
            pulseGuideColor = flashSignalOutlineColor(visualTokens.subtleOutlineColor),
            // low/high Hz reference labels sit on the mini player surface, so they should
            // contrast with that background instead of following primary/secondary slots.
            referenceLabelColor = colorScheme.onSurface,
        )
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

private data class FlashSignalVisualizerLayoutModel(
    val targetBucketCount: Int,
    val windowSampleCount: Int,
)

private data class FlashSignalVisualStyle(
    val activeToneColor: Color,
    val inactiveToneColor: Color,
    val glowColor: Color,
    val baseBackground: Color,
    val centerLineColor: Color,
    val pulseGuideColor: Color,
    val referenceLabelColor: Color,
)

private data class FlashSignalVisualizerSceneState(
    val layoutModel: FlashSignalVisualizerLayoutModel,
    val renderState: FlashSignalVisualizerRenderState,
    val visualStyle: FlashSignalVisualStyle,
)

private data class FlashSignalCanvasVisualState(
    val followDisplayedSamplePosition: Float,
    val runtimeState: FlashSignalCanvasRuntimeState,
    val glowPulse: Float,
    val ambientBrush: Brush,
)

private data class FlashSignalDrawMetrics(
    val visibleSegmentCount: Int,
    val visiblePrimitiveEstimate: Int,
    val visibleBucketCount: Int,
)

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

@Composable
private fun rememberFlashSignalCanvasVisualState(
    isPlaying: Boolean,
    renderState: FlashSignalVisualizerRenderState,
    activeToneColor: Color,
    inactiveToneColor: Color,
): FlashSignalCanvasVisualState {
    val followDisplayedSamplePosition = renderState.playbackSampleState.displayedSample
    val runtimeState =
        rememberFlashSignalCanvasRuntimeState(
            followDisplayedSamplePosition = followDisplayedSamplePosition,
            rawSample = renderState.playbackSampleState.rawSample,
            followData = renderState.followData,
            bitReadoutSource = renderState.bitReadoutSource,
            bitReadoutFrame = renderState.bitReadoutFrame,
        )
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
        remember(activeToneColor, inactiveToneColor, glowPulse, sweepPhase) {
            Brush.horizontalGradient(
                colors =
                    listOf(
                        inactiveToneColor.copy(alpha = 0.10f + 0.02f * glowPulse),
                        activeToneColor.copy(alpha = 0.12f + 0.02f * sweepPhase),
                        inactiveToneColor.copy(alpha = 0.10f + 0.02f * glowPulse),
                    ),
            )
        }
    return remember(
        followDisplayedSamplePosition,
        runtimeState,
        glowPulse,
        ambientBrush,
    ) {
        FlashSignalCanvasVisualState(
            followDisplayedSamplePosition = followDisplayedSamplePosition,
            runtimeState = runtimeState,
            glowPulse = glowPulse,
            ambientBrush = ambientBrush,
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

private fun buildFlashSignalDrawMetrics(
    mode: FlashSignalVisualizationMode,
    renderState: FlashSignalVisualizerRenderState,
    fixedViewport: FlashSignalViewport?,
): FlashSignalDrawMetrics {
    val visibleSegmentCount =
        fixedViewport
            ?.let { viewport -> renderState.visualSegments.count { segment -> segment.overlaps(viewport) } }
            ?: 0
    val visiblePrimitiveEstimate =
        if (mode == FlashSignalVisualizationMode.Hz) {
            visibleSegmentCount.takeIf { renderState.fixedTimelineFrame != null } ?: renderState.toneSpectrumBuckets.size
        } else {
            flashVisualPrimitiveEstimate(
                mode = mode,
                drawableSegments = visibleSegmentCount,
                buckets = renderState.buckets.size,
                hasFixedTimeline = renderState.fixedTimelineFrame != null,
            )
        }
    val visibleBucketCount =
        if (mode == FlashSignalVisualizationMode.Hz) {
            visibleSegmentCount.takeIf { renderState.fixedTimelineFrame != null } ?: renderState.toneSpectrumBuckets.size
        } else {
            renderState.buckets.size
        }
    return FlashSignalDrawMetrics(
        visibleSegmentCount = visibleSegmentCount,
        visiblePrimitiveEstimate = visiblePrimitiveEstimate,
        visibleBucketCount = visibleBucketCount,
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

@Composable
private fun BoxScope.FlashSignalCanvasOverlays(
    mode: FlashSignalVisualizationMode,
    canvasDecision: FlashSignalCanvasDecision?,
    runtimeState: FlashSignalCanvasRuntimeState,
    activeToneColor: Color,
    inactiveToneColor: Color,
    pulseGuideColor: Color,
    glowColor: Color,
) {
    if (canvasDecision?.showPulseOverlay == true) {
        FlashPulseTapeOverlay(
            tapeState = runtimeState.pulseTapeState!!,
            activeToneColor = activeToneColor,
            inactiveToneColor = inactiveToneColor,
            guideColor = pulseGuideColor,
            modifier = Modifier.fillMaxSize(),
        )
    }
    if (canvasDecision?.showLaneBoundaryOverlay == true) {
        FlashLaneBitBoundaryOverlay(
            viewport = canvasDecision.fixedViewport!!,
            activeBit = runtimeState.laneActiveBitState!!,
            layout = flashVisualPlayheadLayout(mode),
            color = glowColor,
            modifier =
                Modifier
                    .fillMaxSize()
                    .testTag("flash-visual-lanes-alignment-overlay"),
        )
    }
    FlashVisualPlayheadOverlay(
        layout = flashVisualPlayheadLayout(mode),
        color = glowColor,
        modifier = Modifier.fillMaxSize(),
    )
    if (canvasDecision?.showLaneSummaryOverlay == true) {
        FlashLaneAlignmentSummaryOverlay(
            laneActiveBit = runtimeState.laneActiveBitState,
            readoutBitOffset = runtimeState.telemetryState.currentReadoutBit ?: -1,
            readoutBitValue = runtimeState.telemetryState.currentReadoutBitValue,
            tokenState = runtimeState.tokenAlignmentState,
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 14.dp, end = 16.dp)
                    .testTag("flash-visual-lanes-alignment-summary"),
        )
    }
}

private fun recordToneTrace(
    isPlaying: Boolean,
    displayedSample: Float,
    sampleRateHz: Int,
    totalSamples: Int,
    buckets: List<ToneSpectrumBucket>,
    segments: List<FlashSignalToneSegment>,
    frequencyScale: ToneFrequencyScale,
    drawStats: ToneSpectrumDrawStats,
    activeThresholdBucketIndex: Float,
    bucketWidth: Float,
    source: String,
) {
    val now = System.nanoTime()
    if (now - flashToneTraceLastLogNanos < FlashToneTraceIntervalNanos) {
        return
    }
    flashToneTraceLastLogNanos = now
    val activeBuckets = buckets.filter { it.frequencyHz > 0f && it.amplitude >= FlashSignalSilenceThreshold }
    val zeroFrequencyBuckets = buckets.count { it.frequencyHz <= 0f }
    val averageFrequency = activeBuckets.map { it.frequencyHz }.averageOrZero()
    val averageStrength = activeBuckets.map { it.strength }.averageOrZero()
    val segmentCarriers = segments.mapNotNull { it.carrierFrequencyHz.takeIf { carrier -> carrier > 0f } }
    val uniqueSegmentCarrierCount = segmentCarriers.map { it.roundToInt() }.distinct().size
    val segmentCarrierMin = segmentCarriers.minOrNull()?.roundToInt() ?: 0
    val segmentCarrierMax = segmentCarriers.maxOrNull()?.roundToInt() ?: 0
    safeLogD(
        FlashToneTraceTag,
        "tone source=$source isPlaying=$isPlaying " +
            "displayMs=${displayedSample.samplesToMs(sampleRateHz)} " +
            "totalMs=${totalSamples.toFloat().samplesToMs(sampleRateHz)} " +
            "scale=${frequencyScale.minHz.roundToInt()}-${frequencyScale.maxHz.roundToInt()} " +
            "refs=${frequencyScale.references.joinToString("|") { it.label }} " +
            "buckets=${buckets.size} nonSilent=${activeBuckets.size} zeroHz=$zeroFrequencyBuckets " +
            "avgHz=${averageFrequency.roundToInt()} " +
            "drawValid=${drawStats.validBuckets} lineSegments=${drawStats.lineSegments} pointDraws=${drawStats.pointDraws} " +
            "freqRange=${drawStats.minFrequencyHz.roundToInt()}-${drawStats.maxFrequencyHz.roundToInt()} " +
            "firstLast=${drawStats.firstFrequencyHz.roundToInt()}-${drawStats.lastFrequencyHz.roundToInt()} " +
            "currentHz=${drawStats.currentFrequencyHz.roundToInt()} " +
            "segmentCarriers=$uniqueSegmentCarrierCount/$segmentCarrierMin-$segmentCarrierMax " +
            "strokePx=${drawStats.minStrokeWidthPx.formatOneDecimal()}-${drawStats.maxStrokeWidthPx.formatOneDecimal()} " +
            "avgStrength=${averageStrength.formatTwoDecimals()} " +
            "activeThreshold=${activeThresholdBucketIndex.formatOneDecimal()} bucketWidth=${bucketWidth.formatOneDecimal()}",
    )
}

private fun List<Float>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()

private fun Float.samplesToMs(sampleRateHz: Int): Int =
    if (sampleRateHz > 0) {
        (this * 1000f / sampleRateHz.toFloat()).roundToInt()
    } else {
        0
    }

private fun Float.formatOneDecimal(): String = "%.1f".format(java.util.Locale.US, this)

private fun Double.formatTwoDecimals(): String = "%.2f".format(java.util.Locale.US, this)

private fun flashSignalOutlineColor(color: Color): Color = color.copy(alpha = FlashSignalOutlineAlpha)

private fun safeLogD(
    tag: String,
    message: String,
) {
    try {
        Log.d(tag, message)
    } catch (_: RuntimeException) {
        // Plain JVM unit tests use the Android stub jar, where Log.d is not implemented.
    }
}

private data class FlashSignalCanvasDecision(
    val fixedViewport: FlashSignalViewport?,
    val visualBucketCount: Int,
    val bucketWidth: Float,
    val activeThresholdBucketIndex: Float,
    val drawContent: FlashSignalDrawContent,
    val showPulseOverlay: Boolean,
    val showLaneBoundaryOverlay: Boolean,
    val showLaneSummaryOverlay: Boolean,
)

private sealed interface FlashSignalDrawContent {
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

private fun buildFlashSignalCanvasDecision(
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

@Composable
private fun FlashLaneBitBoundaryOverlay(
    viewport: FlashSignalViewport,
    activeBit: FlashLaneActiveBitState,
    layout: FlashVisualPlayheadLayout,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val leftPadding = layout.leadingPadding.toPx()
        val rightPadding = layout.trailingPadding.toPx()
        val topPadding = layout.topPadding.toPx()
        val bottomPadding = layout.bottomPadding.toPx()
        val innerWidth = (size.width - leftPadding - rightPadding).coerceAtLeast(1f)
        val startX =
            flashOverlaySampleToViewportX(
                sample = activeBit.startSample,
                viewport = viewport,
                leftPadding = leftPadding,
                innerWidth = innerWidth,
            ).coerceIn(leftPadding, size.width - rightPadding)
        val endX =
            flashOverlaySampleToViewportX(
                sample = activeBit.endSample,
                viewport = viewport,
                leftPadding = leftPadding,
                innerWidth = innerWidth,
            ).coerceIn(leftPadding, size.width - rightPadding)
        val playheadX =
            flashVisualPlayheadX(
                totalWidthPx = size.width,
                leftPaddingPx = leftPadding,
                rightPaddingPx = rightPadding,
            )
        val touchThreshold = 1.dp.toPx()
        val boundaryColor =
            if (abs(playheadX - startX) <= touchThreshold) {
                color.copy(alpha = 0.94f)
            } else {
                color.copy(alpha = 0.42f)
            }
        val overlayHeight = size.height - topPadding - bottomPadding

        drawLine(
            color = boundaryColor,
            start = Offset(startX, topPadding),
            end = Offset(startX, size.height - bottomPadding),
            strokeWidth = 1.dp.toPx(),
        )
        drawLine(
            color = color.copy(alpha = 0.22f),
            start = Offset(endX, topPadding),
            end = Offset(endX, size.height - bottomPadding),
            strokeWidth = 1.dp.toPx(),
        )
        drawRect(
            color = color.copy(alpha = 0.10f),
            topLeft = Offset(startX, topPadding),
            size = Size((endX - startX).coerceAtLeast(1f), overlayHeight),
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

@Composable
private fun FlashLaneAlignmentSummaryOverlay(
    laneActiveBit: FlashLaneActiveBitState?,
    readoutBitOffset: Int,
    readoutBitValue: Char?,
    tokenState: FlashTokenAlignmentState?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.88f),
        contentColor = Color.Black,
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text =
                buildString {
                    append("lane ")
                    append(bitLabel(laneActiveBit?.bitOffset ?: -1, laneActiveBit?.bitValue))
                    append("  row ")
                    append(bitLabel(readoutBitOffset, readoutBitValue))
                    append("  token ")
                    append(bitLabel(tokenState?.globalBitOffset ?: -1, tokenState?.currentBitValue))
                    tokenState?.takeIf { it.byteHex.isNotBlank() || it.byteBinary.isNotBlank() }?.let { state ->
                        append('\n')
                        append("hex ")
                        append(state.byteHex.ifBlank { "_" })
                        append("  bin ")
                        append(state.byteBinary.ifBlank { "_" })
                    }
                },
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

private fun bitLabel(
    bitOffset: Int,
    bitValue: Char?,
): String =
    if (bitOffset < 0) {
        "_"
    } else {
        "$bitOffset:${bitValue ?: '_'}"
    }

private fun flashOverlaySampleToViewportX(
    sample: Float,
    viewport: FlashSignalViewport,
    leftPadding: Float,
    innerWidth: Float,
): Float = leftPadding + ((sample - viewport.startSample) / viewport.sampleCount) * innerWidth

@Composable
private fun FlashPulseTapeOverlay(
    tapeState: FlashPulseTapeState,
    activeToneColor: Color,
    inactiveToneColor: Color,
    guideColor: Color,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier =
            modifier
                .padding(
                    horizontal = FlashPulseOverlayHorizontalPadding,
                    vertical = FlashPulseOverlayVerticalPadding,
                ).clipToBounds()
                .testTag("flash-visual-pulse-tape"),
    ) {
        val density = LocalDensity.current
        LaunchedEffect(maxWidth, maxHeight) {
            val overlayWidthDp = maxWidth.value
            val overlayHeightDp = maxHeight.value
            val upperTrackCenterDp = overlayHeightDp * 0.22f
            val lowerTrackCenterDp = overlayHeightDp * 0.78f
            Log.d(
                "PlaybackPulseLayout",
                "overlayWidthDp=${"%.1f".format(overlayWidthDp)} overlayHeightDp=${"%.1f".format(overlayHeightDp)} " +
                    "waveWidthDp=${"%.1f".format(overlayWidthDp)} upperTrackCenterDp=${"%.1f".format(upperTrackCenterDp)} " +
                    "lowerTrackCenterDp=${"%.1f".format(lowerTrackCenterDp)} visibleSegments=$FlashPulseVisibleCellCount",
            )
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeDp = with(density) { 3.dp.toPx() }
            val activeStrokeDp = with(density) { 5.dp.toPx() }
            val overlayHorizontalPaddingPx = with(density) { FlashPulseOverlayHorizontalPadding.toPx() }
            val upperCenterY = size.height * 0.22f
            val lowerCenterY = size.height * 0.78f
            val segmentWidth = size.width / FlashPulseVisibleCellCount.coerceAtLeast(1)
            val fullCardWidth = size.width + overlayHorizontalPaddingPx * 2f
            val anchorX =
                flashVisualPlayheadX(
                    totalWidthPx = fullCardWidth,
                    leftPaddingPx = 12.dp.toPx(),
                    rightPaddingPx = 12.dp.toPx(),
                ) - overlayHorizontalPaddingPx
            val activeIndex = FlashPulseVisibleCellCount / 2
            val inactiveStroke = strokeDp * 1.15f
            val activeStroke = activeStrokeDp * 1.2f

            drawLine(
                color = guideColor,
                start = Offset(0f, upperCenterY),
                end = Offset(size.width, upperCenterY),
                strokeWidth = 1.dp.toPx(),
            )
            drawLine(
                color = guideColor,
                start = Offset(0f, lowerCenterY),
                end = Offset(size.width, lowerCenterY),
                strokeWidth = 1.dp.toPx(),
            )

            tapeState.cells.forEachIndexed { index, cell ->
                val xStart =
                    anchorX +
                        (index - activeIndex - tapeState.currentBitProgress) * segmentWidth
                val xEnd = xStart + segmentWidth
                val y =
                    when (cell.bit) {
                        '1' -> upperCenterY
                        '0' -> lowerCenterY
                        else -> null
                    } ?: return@forEachIndexed
                val color =
                    when {
                        cell.isActive -> activeToneColor.copy(alpha = 0.92f)
                        cell.isRevealed -> inactiveToneColor.copy(alpha = 0.54f)
                        else -> inactiveToneColor.copy(alpha = 0.24f)
                    }
                if (cell.isActive) {
                    drawLine(
                        color = activeToneColor.copy(alpha = 0.10f),
                        start = Offset(xStart, y),
                        end = Offset(xEnd, y),
                        strokeWidth = 8.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Butt,
                    )
                }
                drawLine(
                    color = color,
                    start = Offset(xStart, y),
                    end = Offset(xEnd, y),
                    strokeWidth = if (cell.isActive) activeStroke else inactiveStroke,
                    cap = if (cell.isActive) androidx.compose.ui.graphics.StrokeCap.Butt else androidx.compose.ui.graphics.StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun FlashVisualPlayheadOverlay(
    layout: FlashVisualPlayheadLayout,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val leftPadding = layout.leadingPadding.toPx()
        val rightPadding = layout.trailingPadding.toPx()
        val topPadding = layout.topPadding.toPx()
        val bottomPadding = layout.bottomPadding.toPx()
        val playheadX =
            flashVisualPlayheadX(
                totalWidthPx = size.width,
                leftPaddingPx = leftPadding,
                rightPaddingPx = rightPadding,
            )

        drawLine(
            color = color.copy(alpha = 0.80f),
            start = Offset(playheadX, topPadding),
            end = Offset(playheadX, size.height - bottomPadding),
            strokeWidth = 2.dp.toPx(),
        )
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
private const val FlashSignalOutlineAlpha = 0.20f
private val FlashPulseOverlayHorizontalPadding = 18.dp
private val FlashPulseOverlayVerticalPadding = 18.dp

internal data class FlashVisualPlayheadLayout(
    val leadingPadding: Dp,
    val trailingPadding: Dp,
    val topPadding: Dp,
    val bottomPadding: Dp,
)

internal fun flashVisualPlayheadLayout(mode: FlashSignalVisualizationMode): FlashVisualPlayheadLayout =
    FlashVisualPlayheadLayout(
        leadingPadding = 12.dp,
        trailingPadding = 12.dp,
        topPadding = 12.dp,
        bottomPadding = 12.dp,
    )

internal fun flashVisualPlayheadX(
    totalWidthPx: Float,
    leftPaddingPx: Float,
    rightPaddingPx: Float,
): Float {
    val innerWidth = (totalWidthPx - leftPaddingPx - rightPaddingPx).coerceAtLeast(1f)
    return leftPaddingPx + innerWidth * FlashSignalPlayheadAnchorRatio
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

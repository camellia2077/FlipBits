package com.bag.audioandroid.ui.screen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

internal data class FlashSignalCanvasVisualState(
    val followDisplayedSamplePosition: Float,
    val runtimeState: FlashSignalCanvasRuntimeState,
    val glowPulse: Float,
    val ambientBrush: Brush,
)

internal data class FlashSignalDrawMetrics(
    val visibleSegmentCount: Int,
    val visiblePrimitiveEstimate: Int,
    val visibleBucketCount: Int,
)

@Composable
internal fun rememberFlashSignalCanvasVisualState(
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

internal fun buildFlashSignalDrawMetrics(
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

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
import kotlin.math.abs

internal data class FlashVisualPlaybackSampleState(
    val rawSample: Float,
    val displayedSample: Float,
)

private data class FlashVisualPausedSampleDecision(
    val displayedSample: Float,
    val holdPausedVisual: Boolean,
    val snapToRaw: Boolean,
)

private class FlashVisualPlaybackFrameClock(
    var anchorNanos: Long,
    var anchorSample: Float,
    var waitingForInitialPlaybackAdvance: Boolean,
) {
    fun snapTo(
        frameNanos: Long,
        sample: Float,
    ) {
        anchorNanos = frameNanos
        anchorSample = sample
    }

    fun predictedSample(
        frameNanos: Long,
        sampleRateHz: Int,
        playbackSpeed: Float,
        maxSample: Float,
    ): Float {
        val elapsedSeconds = (frameNanos - anchorNanos).toDouble() / 1_000_000_000.0
        return (
            anchorSample +
                (elapsedSeconds * sampleRateHz.toDouble() * playbackSpeed.toDouble()).toFloat()
        ).coerceIn(0f, maxSample)
    }
}

@Composable
internal fun rememberFlashVisualPlaybackSampleState(
    rawSample: Float,
    isPlaying: Boolean,
    snapWhenNotPlaying: Boolean = false,
    holdVisualPositionOnPause: Boolean = false,
    isScrubbing: Boolean = false,
    playbackSpeed: Float,
    sampleRateHz: Int,
    totalSamples: Int,
): FlashVisualPlaybackSampleState {
    var visualSample by remember { mutableFloatStateOf(rawSample) }
    var previousIsPlaying by remember { mutableStateOf(isPlaying) }
    var pausedHoldDisplayedSample by remember { mutableStateOf<Float?>(null) }
    val clampedRawSample = rawSample.coerceIn(0f, totalSamples.coerceAtLeast(1).toFloat())
    val safeSpeed = playbackSpeed.coerceIn(0.1f, 4f)
    val latestAnchorSample by rememberUpdatedState(rawSample)
    val latestTotalSamples by rememberUpdatedState(totalSamples)
    if (!isPlaying || sampleRateHz <= 0 || totalSamples <= 0) {
        val justPaused = previousIsPlaying && !isPlaying
        val pausedDecision =
            flashVisualPausedSampleDecision(
                clampedRawSample = clampedRawSample,
                visualSample = visualSample,
                pausedHoldDisplayedSample = pausedHoldDisplayedSample,
                justPaused = justPaused,
                snapWhenNotPlaying = snapWhenNotPlaying,
                holdVisualPositionOnPause = holdVisualPositionOnPause,
                isScrubbing = isScrubbing,
                sampleRateHz = sampleRateHz,
                totalSamples = totalSamples,
            )
        if (pausedDecision.holdPausedVisual) {
            SideEffect {
                visualSample = pausedDecision.displayedSample
                pausedHoldDisplayedSample = pausedDecision.displayedSample
                previousIsPlaying = false
            }
        } else {
            SideEffect {
                if (pausedDecision.snapToRaw) {
                    visualSample = clampedRawSample
                }
                pausedHoldDisplayedSample = null
                previousIsPlaying = false
            }
        }
        return FlashVisualPlaybackSampleState(
            rawSample = rawSample,
            displayedSample = pausedDecision.displayedSample,
        )
    }
    LaunchedEffect(safeSpeed, sampleRateHz, totalSamples) {
        previousIsPlaying = true
        pausedHoldDisplayedSample = null
        FlashVisualPerfTrace.recordSmoothReset(
            anchorSample = latestAnchorSample,
            previousSmoothSample = visualSample,
            sampleRateHz = sampleRateHz,
        )
        val maxSample = latestTotalSamples.toFloat()
        val initialAnchor = latestAnchorSample.coerceIn(0f, maxSample)
        val hardAnchorThreshold = sampleRateHz * PlaybackHardAnchorThresholdSeconds
        visualSample = initialPlaybackVisualSample(initialAnchor, visualSample, maxSample)
        val frameClock =
            FlashVisualPlaybackFrameClock(
                anchorNanos = withFrameNanos { it },
                anchorSample = visualSample,
                waitingForInitialPlaybackAdvance = initialAnchor <= PlaybackStartHoldRawSampleThresholdSamples,
            )
        while (true) {
            val frameNanos = withFrameNanos { it }
            val currentMaxSample = latestTotalSamples.toFloat()
            val anchor = latestAnchorSample.coerceIn(0f, currentMaxSample)
            visualSample =
                nextPlayingVisualSample(
                    frameClock = frameClock,
                    frameNanos = frameNanos,
                    anchor = anchor,
                    sampleRateHz = sampleRateHz,
                    playbackSpeed = safeSpeed,
                    maxSample = currentMaxSample,
                    hardAnchorThreshold = hardAnchorThreshold,
                )
            if (visualSample >= currentMaxSample) {
                frameClock.snapTo(frameNanos, visualSample)
            }
        }
    }
    return FlashVisualPlaybackSampleState(
        rawSample = rawSample,
        displayedSample = visualSample,
    )
}

private fun flashVisualPausedSampleDecision(
    clampedRawSample: Float,
    visualSample: Float,
    pausedHoldDisplayedSample: Float?,
    justPaused: Boolean,
    snapWhenNotPlaying: Boolean,
    holdVisualPositionOnPause: Boolean,
    isScrubbing: Boolean,
    sampleRateHz: Int,
    totalSamples: Int,
): FlashVisualPausedSampleDecision {
    val maxSample = totalSamples.coerceAtLeast(1).toFloat()
    val canHoldPausedVisual =
        !isScrubbing &&
            (holdVisualPositionOnPause || !snapWhenNotPlaying) &&
            sampleRateHz > 0 &&
            totalSamples > 0
    val shouldHoldPausedVisual =
        canHoldPausedVisual &&
            (justPaused || pausedHoldDisplayedSample != null)
    if (shouldHoldPausedVisual) {
        // Pause freezes the presentation sample exactly where the visual was drawn.
        // Later raw callbacks may arrive, but they must not move the paused visual.
        val displayedSample =
            (
                if (justPaused) {
                    visualSample
                } else {
                    pausedHoldDisplayedSample ?: visualSample
                }
            ).coerceIn(0f, maxSample)
        return FlashVisualPausedSampleDecision(
            displayedSample = displayedSample,
            holdPausedVisual = true,
            snapToRaw = false,
        )
    }
    val shouldSnapToRaw =
        snapWhenNotPlaying ||
            sampleRateHz <= 0 ||
            totalSamples <= 0 ||
            abs(clampedRawSample - visualSample) > sampleRateHz * PauseSnapDriftThresholdSeconds
    return FlashVisualPausedSampleDecision(
        displayedSample =
            if (shouldSnapToRaw) {
                clampedRawSample
            } else {
                visualSample.coerceIn(0f, maxSample)
            },
        holdPausedVisual = false,
        snapToRaw = shouldSnapToRaw,
    )
}

private fun initialPlaybackVisualSample(
    initialAnchor: Float,
    visualSample: Float,
    maxSample: Float,
): Float =
    if (initialAnchor + 0.5f < visualSample) {
        initialAnchor
    } else {
        visualSample.coerceIn(0f, maxSample)
    }

private fun nextPlayingVisualSample(
    frameClock: FlashVisualPlaybackFrameClock,
    frameNanos: Long,
    anchor: Float,
    sampleRateHz: Int,
    playbackSpeed: Float,
    maxSample: Float,
    hardAnchorThreshold: Float,
): Float {
    if (frameClock.waitingForInitialPlaybackAdvance) {
        frameClock.snapTo(frameNanos, anchor)
        if (anchor > PlaybackStartHoldRawSampleThresholdSamples) {
            frameClock.waitingForInitialPlaybackAdvance = false
        }
        return anchor
    }
    val predictedSample =
        frameClock.predictedSample(
            frameNanos = frameNanos,
            sampleRateHz = sampleRateHz,
            playbackSpeed = playbackSpeed,
            maxSample = maxSample,
        )
    val anchorDelta = anchor - predictedSample
    return when {
        anchorDelta < -hardAnchorThreshold -> {
            frameClock.snapTo(frameNanos, anchor)
            anchor
        }
        anchorDelta > sampleRateHz * PlaybackCatchUpDeadbandSeconds -> {
            val catchUpStep =
                minOf(
                    anchorDelta * PlaybackCatchUpFraction,
                    sampleRateHz * PlaybackCatchUpMaxStepSeconds,
                )
            val correctedSample = (predictedSample + catchUpStep).coerceAtMost(anchor)
            frameClock.snapTo(frameNanos, correctedSample)
            correctedSample
        }
        else -> predictedSample
    }
}

private const val PlaybackStartHoldRawSampleThresholdSamples = 0.5f
private const val PlaybackHardAnchorThresholdSeconds = 0.35f
private const val PlaybackCatchUpDeadbandSeconds = 0.02f
private const val PlaybackCatchUpFraction = 0.22f
private const val PlaybackCatchUpMaxStepSeconds = 0.030f
private const val PauseSnapDriftThresholdSeconds = 0.35f

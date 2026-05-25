package com.bag.audioandroid.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.bag.audioandroid.domain.PayloadFollowViewData

internal data class FlashSignalPlaybackReadoutState(
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

@Composable
internal fun rememberFlashSignalPlaybackReadoutState(
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

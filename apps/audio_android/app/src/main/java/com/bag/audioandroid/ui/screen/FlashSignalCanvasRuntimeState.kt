package com.bag.audioandroid.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.bag.audioandroid.domain.PayloadFollowViewData

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

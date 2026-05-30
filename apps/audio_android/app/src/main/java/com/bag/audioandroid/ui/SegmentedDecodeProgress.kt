package com.bag.audioandroid.ui

import com.bag.audioandroid.domain.AudioDecodePhase
import com.bag.audioandroid.domain.DecodeOperationSnapshot
import com.bag.audioandroid.domain.DecodeOperationState
import com.bag.audioandroid.domain.DecodeProgressUpdate

internal const val SEGMENTED_DECODE_AGGREGATE_WORK_UNITS = 1_000_000L

internal fun aggregateSegmentedDecodeProgress0To1(
    segmentIndex: Int,
    segmentCount: Int,
    segmentProgress0To1: Float,
): Float {
    if (segmentCount <= 0) {
        return 0f
    }
    return ((segmentIndex.toFloat() + segmentProgress0To1.coerceIn(0f, 1f)) / segmentCount.toFloat())
        .coerceIn(0f, 1f)
}

internal fun aggregateSegmentedDecodeWorkUnits(
    segmentIndex: Int,
    segmentCount: Int,
    segmentProgress0To1: Float,
): Long =
    (aggregateSegmentedDecodeProgress0To1(segmentIndex, segmentCount, segmentProgress0To1) * SEGMENTED_DECODE_AGGREGATE_WORK_UNITS)
        .toLong()

internal fun segmentedDecodeProgressUpdate(
    segmentIndex: Int,
    segmentCount: Int,
    segmentProgress0To1: Float,
    phase: AudioDecodePhase = AudioDecodePhase.DecodingPayload,
): DecodeProgressUpdate {
    val aggregateProgress =
        aggregateSegmentedDecodeProgress0To1(
            segmentIndex = segmentIndex,
            segmentCount = segmentCount,
            segmentProgress0To1 = segmentProgress0To1,
        )
    return DecodeProgressUpdate(
        phase = phase,
        progress0To1 = aggregateProgress,
        snapshot =
            DecodeOperationSnapshot.Initial.copy(
                state = DecodeOperationState.Running,
                phase = phase,
                overallProgress0To1 = aggregateProgress,
                phaseProgress0To1 = aggregateProgress,
                completedWorkUnits =
                    aggregateSegmentedDecodeWorkUnits(
                        segmentIndex = segmentIndex,
                        segmentCount = segmentCount,
                        segmentProgress0To1 = segmentProgress0To1,
                    ),
                totalWorkUnits = SEGMENTED_DECODE_AGGREGATE_WORK_UNITS,
                phaseCompletedWorkUnits =
                    aggregateSegmentedDecodeWorkUnits(
                        segmentIndex = segmentIndex,
                        segmentCount = segmentCount,
                        segmentProgress0To1 = segmentProgress0To1,
                    ),
                phaseTotalWorkUnits = SEGMENTED_DECODE_AGGREGATE_WORK_UNITS,
            ),
    )
}

package com.bag.audioandroid.ui

import com.bag.audioandroid.ui.state.QueueSheetValue
import kotlin.math.abs

internal data class QueueSheetAnchorPositions(
    val hiddenHeightPx: Float,
    val peekHeightPx: Float,
    val halfHeightPx: Float,
    val expandedHeightPx: Float,
) {
    fun visibleHeightFor(value: QueueSheetValue): Float =
        when (value) {
            QueueSheetValue.Hidden -> hiddenHeightPx
            QueueSheetValue.Peek -> peekHeightPx
            QueueSheetValue.Half -> halfHeightPx
            QueueSheetValue.Expanded -> expandedHeightPx
        }

    fun nearestValue(visibleHeightPx: Float): QueueSheetValue {
        val candidates =
            listOf(
                QueueSheetValue.Hidden to hiddenHeightPx,
                QueueSheetValue.Peek to peekHeightPx,
                QueueSheetValue.Half to halfHeightPx,
                QueueSheetValue.Expanded to expandedHeightPx,
            )
        return candidates.minBy { (_, anchorHeight) -> abs(anchorHeight - visibleHeightPx) }.first
    }
}

internal object PlayerGestureController {
    private const val QueueVelocityThresholdPxPerSec = 1800f

    fun settleQueueValue(
        currentVisibleHeightPx: Float,
        currentValue: QueueSheetValue,
        anchors: QueueSheetAnchorPositions,
        velocityPxPerSec: Float,
    ): QueueSheetValue {
        if (velocityPxPerSec <= -QueueVelocityThresholdPxPerSec) {
            return when (currentValue) {
                QueueSheetValue.Hidden -> QueueSheetValue.Peek
                QueueSheetValue.Peek -> QueueSheetValue.Half
                QueueSheetValue.Half -> QueueSheetValue.Expanded
                QueueSheetValue.Expanded -> QueueSheetValue.Expanded
            }
        }
        if (velocityPxPerSec >= QueueVelocityThresholdPxPerSec) {
            return when (currentValue) {
                QueueSheetValue.Hidden -> QueueSheetValue.Hidden
                QueueSheetValue.Peek -> QueueSheetValue.Hidden
                QueueSheetValue.Half -> QueueSheetValue.Hidden
                QueueSheetValue.Expanded -> QueueSheetValue.Half
            }
        }
        return anchors.nearestValue(currentVisibleHeightPx)
    }
}

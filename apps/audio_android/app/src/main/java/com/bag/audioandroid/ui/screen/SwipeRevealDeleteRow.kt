package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlin.math.max

@Stable
internal class SwipeRevealDeleteState(
    private val revealThresholdFraction: Float = SwipeRevealOpenThresholdFraction,
    private val deleteThresholdFraction: Float = SwipeRevealDeleteThresholdFraction,
) {
    var offsetPx by mutableFloatStateOf(0f)
        private set

    var rowWidthPx by mutableFloatStateOf(0f)
        private set

    var actionLaneWidthPx by mutableFloatStateOf(0f)
        private set

    var revealedOffsetPx by mutableFloatStateOf(0f)
        private set

    var rowHeightPx by mutableFloatStateOf(0f)
        private set

    val isRevealed: Boolean
        get() = offsetPx <= -actionLaneWidthPx && actionLaneWidthPx > 0f

    fun updateGeometry(
        rowWidthPx: Float,
        actionLaneWidthPx: Float,
        revealedOffsetPx: Float,
    ) {
        this.rowWidthPx = rowWidthPx
        this.actionLaneWidthPx = actionLaneWidthPx
        this.revealedOffsetPx = revealedOffsetPx
        offsetPx = offsetPx.coerceIn(-rowWidthPx, 0f)
    }

    fun dragBy(deltaPx: Float) {
        offsetPx = (offsetPx + deltaPx).coerceIn(-rowWidthPx, 0f)
    }

    fun updateRowHeight(heightPx: Float) {
        rowHeightPx = heightPx
    }

    fun settle(onDelete: () -> Unit) {
        val draggedDistancePx = abs(offsetPx)
        val revealThresholdPx = actionLaneWidthPx * revealThresholdFraction
        val directDeleteThresholdPx = max(rowWidthPx * deleteThresholdFraction, actionLaneWidthPx * 1.75f)
        when {
            draggedDistancePx >= directDeleteThresholdPx -> {
                reset()
                onDelete()
            }
            draggedDistancePx >= revealThresholdPx -> {
                offsetPx = -revealedOffsetPx
            }
            else -> {
                reset()
            }
        }
    }

    fun reset() {
        offsetPx = 0f
    }
}

@Composable
internal fun rememberSwipeRevealDeleteState(): SwipeRevealDeleteState = remember { SwipeRevealDeleteState() }

@Composable
internal fun SwipeRevealDeleteRow(
    enabled: Boolean,
    state: SwipeRevealDeleteState,
    deleteLabel: String,
    onDelete: () -> Unit,
    actionLaneWidthDp: Dp = 96.dp,
    revealedOffsetDp: Dp = actionLaneWidthDp,
    actionBackgroundOverdrawDp: Dp = 12.dp,
    actionVerticalPaddingDp: Dp = 0.dp,
    actionContainerColor: Color = MaterialTheme.colorScheme.error,
    actionContentColor: Color = MaterialTheme.colorScheme.onError,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    if (!enabled) {
        content(modifier)
        return
    }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth(),
    ) {
        val density = LocalDensity.current
        val rowWidthPx = with(density) { maxWidth.toPx() }
        val actionLaneWidthPx = with(density) { actionLaneWidthDp.toPx() }
        val revealedOffsetPx = with(density) { revealedOffsetDp.toPx() }
        val actionBackgroundOverdrawPx = with(density) { actionBackgroundOverdrawDp.toPx() }
        val exposedActionWidthDp =
            with(density) {
                val exposedWidthPx = abs(state.offsetPx).coerceIn(0f, rowWidthPx)
                val overdrawPx =
                    if (exposedWidthPx > 0f) {
                        actionBackgroundOverdrawPx
                    } else {
                        0f
                    }
                (exposedWidthPx + overdrawPx)
                    .coerceIn(0f, rowWidthPx)
                    .toDp()
            }
        val rowHeightDp = with(density) { state.rowHeightPx.toDp() }
        state.updateGeometry(
            rowWidthPx = rowWidthPx,
            actionLaneWidthPx = actionLaneWidthPx,
            revealedOffsetPx = revealedOffsetPx,
        )

        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .then(
                        if (state.rowHeightPx > 0f) {
                            Modifier.height(rowHeightDp)
                        } else {
                            Modifier.fillMaxHeight()
                        },
                    ).width(exposedActionWidthDp)
                    .background(actionContainerColor),
        )

        content(
            Modifier
                .onSizeChanged { size -> state.updateRowHeight(size.height.toFloat()) }
                .absoluteOffset(x = with(density) { state.offsetPx.toDp() })
                .draggable(
                    orientation = Orientation.Horizontal,
                    state =
                        rememberDraggableState { delta ->
                            state.dragBy(delta)
                        },
                    onDragStopped = {
                        state.settle(onDelete)
                    },
                ),
        )

        if (state.isRevealed) {
            TextButton(
                onClick = {
                    state.reset()
                    onDelete()
                },
                modifier =
                    Modifier
                        .zIndex(1f)
                        .align(Alignment.CenterEnd)
                        .width(actionLaneWidthDp)
                        .then(
                            if (state.rowHeightPx > 0f) {
                                Modifier.height(rowHeightDp)
                            } else {
                                Modifier.fillMaxHeight()
                            },
                        ).padding(vertical = actionVerticalPaddingDp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = deleteLabel,
                    tint = actionContentColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private const val SwipeRevealOpenThresholdFraction = 0.45f
private const val SwipeRevealDeleteThresholdFraction = 0.6f

package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.ui.theme.AppThemeAccentTokens

@Composable
internal fun rememberReorderDragState(
    itemIds: List<String>,
    estimatedRowHeightPx: Float,
    rowSpacingPx: Float,
    thresholdFraction: Float,
): ReorderDragState =
    remember(itemIds, estimatedRowHeightPx, rowSpacingPx, thresholdFraction) {
        ReorderDragState(
            estimatedRowHeightPx = estimatedRowHeightPx,
            rowSpacingPx = rowSpacingPx,
            thresholdFraction = thresholdFraction,
        )
    }

internal class ReorderDragState(
    private val estimatedRowHeightPx: Float,
    private val rowSpacingPx: Float,
    private val thresholdFraction: Float,
) {
    private val itemHeights = mutableStateMapOf<String, Float>()

    var draggedItemId by mutableStateOf<String?>(null)
        private set

    var draggedOffsetY by mutableFloatStateOf(0f)
        private set

    var draggedStartIndex by mutableStateOf<Int?>(null)
        private set

    var draggedTargetIndex by mutableStateOf<Int?>(null)
        private set

    val previewSlot: Int?
        get() {
            val startIndex = draggedStartIndex
            val targetIndex = draggedTargetIndex
            return if (startIndex != null && targetIndex != null && startIndex != targetIndex) {
                previewSlotForTargetIndex(startIndex = startIndex, targetIndex = targetIndex)
            } else {
                null
            }
        }

    fun onItemMeasured(
        itemId: String,
        heightPx: Float,
    ) {
        itemHeights[itemId] = heightPx
    }

    fun startDrag(
        itemId: String,
        itemIds: List<String>,
    ) {
        val startIndex = itemIds.indexOfFirst { it == itemId }
        draggedItemId = itemId
        draggedOffsetY = 0f
        draggedStartIndex = startIndex.takeIf { it >= 0 }
        draggedTargetIndex = startIndex.takeIf { it >= 0 }
    }

    fun updateDrag(
        fallbackItemId: String,
        itemIds: List<String>,
        dragDeltaY: Float,
    ) {
        draggedOffsetY += dragDeltaY
        val activeItemId = draggedItemId ?: fallbackItemId
        val startIndex = draggedStartIndex ?: itemIds.indexOfFirst { it == activeItemId }
        if (startIndex < 0) {
            return
        }
        draggedTargetIndex =
            resolveReorderTargetIndex(
                itemIds = itemIds,
                itemHeights = itemHeights,
                startIndex = startIndex,
                dragOffsetY = draggedOffsetY,
                estimatedRowHeightPx = estimatedRowHeightPx,
                rowSpacingPx = rowSpacingPx,
                thresholdFraction = thresholdFraction,
            )
    }

    fun endDrag(onMove: ((Int, Int) -> Unit)?) {
        val startIndex = draggedStartIndex
        val targetIndex = draggedTargetIndex
        if (startIndex != null && targetIndex != null && startIndex != targetIndex) {
            onMove?.invoke(startIndex, targetIndex)
        }
        reset()
    }

    fun cancelDrag() {
        reset()
    }

    fun dragOffsetFor(itemId: String): Float = if (draggedItemId == itemId) draggedOffsetY else 0f

    fun zIndexFor(itemId: String): Float = if (draggedItemId == itemId) 2f else 0f

    fun shouldShowPreviewBefore(index: Int): Boolean = draggedItemId != null && previewSlot == index

    fun shouldShowTrailingPreview(itemCount: Int): Boolean = draggedItemId != null && previewSlot == itemCount

    private fun reset() {
        draggedItemId = null
        draggedOffsetY = 0f
        draggedStartIndex = null
        draggedTargetIndex = null
    }
}

internal fun Modifier.reorderableLongPressDrag(
    enabled: Boolean,
    itemId: String,
    itemIds: List<String>,
    dragState: ReorderDragState,
    hapticFeedback: HapticFeedback,
    onMove: ((Int, Int) -> Unit)?,
): Modifier =
    if (!enabled) {
        this
    } else {
        pointerInput(itemId, itemIds) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    dragState.startDrag(
                        itemId = itemId,
                        itemIds = itemIds,
                    )
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onDragEnd = {
                    dragState.endDrag(onMove)
                },
                onDragCancel = {
                    dragState.cancelDrag()
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    dragState.updateDrag(
                        fallbackItemId = itemId,
                        itemIds = itemIds,
                        dragDeltaY = dragAmount.y,
                    )
                },
            )
        }
    }

@Composable
internal fun ReorderDropIndicator(accentTokens: AppThemeAccentTokens) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(accentTokens.selectionBorderAccentTint.copy(alpha = 0.92f)),
    )
}

@Composable
internal fun ReorderTrailingDropIndicator(
    accentTokens: AppThemeAccentTokens,
    visible: Boolean,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (visible) {
                        accentTokens.selectionBorderAccentTint.copy(alpha = 0.16f)
                    } else {
                        Color.Transparent
                    },
                ).border(
                    width = if (visible) 1.5.dp else 0.dp,
                    color = accentTokens.selectionBorderAccentTint.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(12.dp),
                ),
    )
}

private fun resolveReorderTargetIndex(
    itemIds: List<String>,
    itemHeights: Map<String, Float>,
    startIndex: Int,
    dragOffsetY: Float,
    estimatedRowHeightPx: Float,
    rowSpacingPx: Float,
    thresholdFraction: Float,
): Int {
    if (startIndex !in itemIds.indices) {
        return startIndex
    }

    var targetIndex = startIndex
    val clampedThresholdFraction = thresholdFraction.coerceIn(0f, 1f)

    if (dragOffsetY > 0f) {
        var remainingOffset = dragOffsetY
        while (targetIndex < itemIds.lastIndex) {
            val nextIndex = targetIndex + 1
            val nextHeight = itemHeights[itemIds[nextIndex]] ?: estimatedRowHeightPx
            val threshold = nextHeight * clampedThresholdFraction + rowSpacingPx
            if (remainingOffset < threshold) {
                break
            }
            remainingOffset -= threshold
            targetIndex = nextIndex
        }
        return targetIndex
    }

    if (dragOffsetY < 0f) {
        var remainingOffset = -dragOffsetY
        while (targetIndex > 0) {
            val previousIndex = targetIndex - 1
            val previousHeight = itemHeights[itemIds[previousIndex]] ?: estimatedRowHeightPx
            val threshold = previousHeight * clampedThresholdFraction + rowSpacingPx
            if (remainingOffset < threshold) {
                break
            }
            remainingOffset -= threshold
            targetIndex = previousIndex
        }
    }

    return targetIndex
}

private fun previewSlotForTargetIndex(
    startIndex: Int,
    targetIndex: Int,
): Int = if (targetIndex > startIndex) targetIndex + 1 else targetIndex

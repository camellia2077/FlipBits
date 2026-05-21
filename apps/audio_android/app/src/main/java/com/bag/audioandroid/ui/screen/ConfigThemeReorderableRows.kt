package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

internal data class ConfigThemeSwipeDeleteSpec(
    val enabled: Boolean,
    val deleteLabel: String,
    val actionLaneWidthDp: Dp,
    val actionContainerColor: Color,
    val actionContentColor: Color,
    val onDelete: () -> Unit,
)

@Composable
internal fun <T> ConfigThemeReorderableSwipeRows(
    items: List<T>,
    itemId: (T) -> String,
    rowSpacing: Dp,
    estimatedRowHeightPx: Float,
    rowSpacingPx: Float,
    reorderThresholdFraction: Float,
    reorderEnabled: Boolean,
    onMove: ((Int, Int) -> Unit)?,
    deleteSpec: @Composable (T) -> ConfigThemeSwipeDeleteSpec?,
    showDropIndicator: (Int) -> Boolean,
    dropIndicator: @Composable () -> Unit,
    rowContent: @Composable (item: T, rowModifier: Modifier, dragOffsetY: Float) -> Unit,
) {
    val itemIds = items.map(itemId)
    val dragState =
        rememberReorderDragState(
            itemIds = itemIds,
            estimatedRowHeightPx = estimatedRowHeightPx,
            rowSpacingPx = rowSpacingPx,
            thresholdFraction = reorderThresholdFraction,
        )
    val currentItems by rememberUpdatedState(items)
    val currentOnMove by rememberUpdatedState(onMove)
    val hapticFeedback = LocalHapticFeedback.current

    Column(verticalArrangement = Arrangement.spacedBy(rowSpacing)) {
        items.forEachIndexed { index, item ->
            val id = itemId(item)
            key(id) {
                Box(
                    modifier = Modifier.zIndex(dragState.zIndexFor(id)),
                ) {
                    val swipeDeleteState = rememberSwipeRevealDeleteState()
                    val rowModifier =
                        Modifier
                            .onGloballyPositioned { coordinates ->
                                dragState.onItemMeasured(
                                    itemId = id,
                                    heightPx = coordinates.size.height.toFloat(),
                                )
                            }.then(
                                if (reorderEnabled && currentOnMove != null && items.size > 1) {
                                    Modifier.reorderableLongPressDrag(
                                        enabled = true,
                                        itemId = id,
                                        itemIds = currentItems.map(itemId),
                                        dragState = dragState,
                                        hapticFeedback = hapticFeedback,
                                        onMove = currentOnMove,
                                    )
                                } else {
                                    Modifier
                                },
                            )
                    val delete = deleteSpec(item)
                    SwipeRevealDeleteRow(
                        enabled = delete?.enabled == true,
                        state = swipeDeleteState,
                        deleteLabel = delete?.deleteLabel.orEmpty(),
                        onDelete = { delete?.onDelete?.invoke() },
                        actionLaneWidthDp = delete?.actionLaneWidthDp ?: 0.dp,
                        revealedOffsetDp = delete?.actionLaneWidthDp ?: 0.dp,
                        actionContainerColor = delete?.actionContainerColor ?: Color.Transparent,
                        actionContentColor = delete?.actionContentColor ?: Color.Transparent,
                    ) { swipeModifier ->
                        rowContent(
                            item,
                            rowModifier.then(swipeModifier),
                            dragState.dragOffsetFor(id),
                        )
                    }
                    if (showDropIndicator(index) && dragState.shouldShowPreviewBefore(index)) {
                        dropIndicator()
                    }
                }
            }
        }
    }
}

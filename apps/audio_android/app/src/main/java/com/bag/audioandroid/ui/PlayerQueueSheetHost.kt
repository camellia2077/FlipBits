package com.bag.audioandroid.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.ui.model.SavedAudioModeFilter
import com.bag.audioandroid.ui.screen.SavedAudioPickerSheet
import com.bag.audioandroid.ui.state.QueueSheetValue
import kotlinx.coroutines.launch

@Composable
internal fun PlayerQueueSheetHost(
    savedAudioItems: List<SavedAudioItem>,
    savedAudioFilter: SavedAudioModeFilter,
    currentSavedAudioItemId: String?,
    initialQueueValue: QueueSheetValue,
    onCloseQueue: () -> Unit,
    onQueueValueChanged: (QueueSheetValue) -> Unit,
    onSavedAudioFilterChange: (SavedAudioModeFilter) -> Unit,
    onSavedAudioSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val scope = rememberCoroutineScope()
        val density = LocalDensity.current
        val containerHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val anchors =
            with(density) {
                QueueSheetAnchorPositions(
                    hiddenHeightPx = 0f,
                    peekHeightPx = minOf(140.dp.toPx(), containerHeightPx * 0.22f),
                    halfHeightPx = minOf(420.dp.toPx(), containerHeightPx * 0.58f),
                    expandedHeightPx = minOf(720.dp.toPx(), containerHeightPx * 0.86f),
                )
            }
        val visibleHeightPx = remember { Animatable(anchors.visibleHeightFor(initialQueueValue)) }
        LaunchedEffect(initialQueueValue, anchors.hiddenHeightPx, anchors.peekHeightPx, anchors.halfHeightPx, anchors.expandedHeightPx) {
            visibleHeightPx.animateTo(anchors.visibleHeightFor(initialQueueValue), spring())
        }

        SavedAudioPickerSheet(
            savedAudioItems = savedAudioItems,
            selectedFilter = savedAudioFilter,
            onFilterSelected = onSavedAudioFilterChange,
            onSavedAudioSelected = onSavedAudioSelected,
            currentItemId = currentSavedAudioItemId,
            maxListHeight = with(density) { (visibleHeightPx.value - 112.dp.toPx()).coerceAtLeast(160f).toDp() },
            sheetHeight = with(density) { visibleHeightPx.value.coerceAtLeast(220f).toDp() },
            onCollapseRequested = onCloseQueue,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .pointerInput(initialQueueValue, anchors.expandedHeightPx) {
                        val velocityTracker = VelocityTracker()
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                val nextHeight =
                                    (visibleHeightPx.value - dragAmount).coerceIn(
                                        anchors.hiddenHeightPx,
                                        anchors.expandedHeightPx,
                                    )
                                scope.launch {
                                    visibleHeightPx.snapTo(nextHeight)
                                }
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                change.consume()
                            },
                            onDragEnd = {
                                val velocity = velocityTracker.calculateVelocity().y
                                val settledValue =
                                    PlayerGestureController.settleQueueValue(
                                        currentVisibleHeightPx = visibleHeightPx.value,
                                        currentValue = initialQueueValue,
                                        anchors = anchors,
                                        velocityPxPerSec = velocity,
                                    )
                                onQueueValueChanged(settledValue)
                                if (settledValue == QueueSheetValue.Hidden) {
                                    onCloseQueue()
                                }
                                scope.launch {
                                    visibleHeightPx.animateTo(anchors.visibleHeightFor(settledValue), spring())
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    visibleHeightPx.animateTo(anchors.visibleHeightFor(initialQueueValue), spring())
                                }
                            },
                        )
                    },
        )
    }
}

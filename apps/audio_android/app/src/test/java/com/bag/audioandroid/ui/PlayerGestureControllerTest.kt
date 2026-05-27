package com.bag.audioandroid.ui

import com.bag.audioandroid.ui.state.QueueSheetValue
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerGestureControllerTest {
    @Test
    fun `queue settles to nearest anchor without fling`() {
        val anchors =
            QueueSheetAnchorPositions(
                hiddenHeightPx = 0f,
                peekHeightPx = 100f,
                halfHeightPx = 300f,
                expandedHeightPx = 600f,
            )

        assertEquals(
            QueueSheetValue.Half,
            PlayerGestureController.settleQueueValue(
                currentVisibleHeightPx = 280f,
                currentValue = QueueSheetValue.Peek,
                anchors = anchors,
                velocityPxPerSec = 0f,
            ),
        )
    }
}

package com.bag.audioandroid.ui.screen

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

private val FlashPulseOverlayHorizontalPadding = 18.dp
private val FlashPulseOverlayVerticalPadding = 18.dp

@Composable
internal fun FlashPulseTapeOverlay(
    tapeState: FlashPulseTapeState,
    activeToneColor: Color,
    inactiveToneColor: Color,
    guideColor: Color,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier =
            modifier
                .padding(
                    horizontal = FlashPulseOverlayHorizontalPadding,
                    vertical = FlashPulseOverlayVerticalPadding,
                ).clipToBounds()
                .testTag("flash-visual-pulse-tape"),
    ) {
        val density = LocalDensity.current
        LaunchedEffect(maxWidth, maxHeight) {
            val overlayWidthDp = maxWidth.value
            val overlayHeightDp = maxHeight.value
            val upperTrackCenterDp = overlayHeightDp * 0.22f
            val lowerTrackCenterDp = overlayHeightDp * 0.78f
            Log.d(
                "PlaybackPulseLayout",
                "overlayWidthDp=${"%.1f".format(overlayWidthDp)} overlayHeightDp=${"%.1f".format(overlayHeightDp)} " +
                    "waveWidthDp=${"%.1f".format(overlayWidthDp)} upperTrackCenterDp=${"%.1f".format(upperTrackCenterDp)} " +
                    "lowerTrackCenterDp=${"%.1f".format(lowerTrackCenterDp)} visibleSegments=$FlashPulseVisibleCellCount",
            )
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeDp = with(density) { 3.dp.toPx() }
            val activeStrokeDp = with(density) { 5.dp.toPx() }
            val overlayHorizontalPaddingPx = with(density) { FlashPulseOverlayHorizontalPadding.toPx() }
            val upperCenterY = size.height * 0.22f
            val lowerCenterY = size.height * 0.78f
            val segmentWidth = size.width / FlashPulseVisibleCellCount.coerceAtLeast(1)
            val fullCardWidth = size.width + overlayHorizontalPaddingPx * 2f
            val anchorX =
                flashVisualPlayheadX(
                    totalWidthPx = fullCardWidth,
                    leftPaddingPx = 12.dp.toPx(),
                    rightPaddingPx = 12.dp.toPx(),
                ) - overlayHorizontalPaddingPx
            val activeIndex = FlashPulseVisibleCellCount / 2
            val inactiveStroke = strokeDp * 1.15f
            val activeStroke = activeStrokeDp * 1.2f

            drawLine(
                color = guideColor,
                start = Offset(0f, upperCenterY),
                end = Offset(size.width, upperCenterY),
                strokeWidth = 1.dp.toPx(),
            )
            drawLine(
                color = guideColor,
                start = Offset(0f, lowerCenterY),
                end = Offset(size.width, lowerCenterY),
                strokeWidth = 1.dp.toPx(),
            )

            tapeState.cells.forEachIndexed { index, cell ->
                val xStart =
                    anchorX +
                        (index - activeIndex - tapeState.currentBitProgress) * segmentWidth
                val xEnd = xStart + segmentWidth
                val y =
                    when (cell.bit) {
                        '1' -> upperCenterY
                        '0' -> lowerCenterY
                        else -> null
                    } ?: return@forEachIndexed
                val color =
                    when {
                        cell.isActive -> activeToneColor.copy(alpha = 0.92f)
                        cell.isRevealed -> inactiveToneColor.copy(alpha = 0.54f)
                        else -> inactiveToneColor.copy(alpha = 0.24f)
                    }
                if (cell.isActive) {
                    drawLine(
                        color = activeToneColor.copy(alpha = 0.10f),
                        start = Offset(xStart, y),
                        end = Offset(xEnd, y),
                        strokeWidth = 8.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Butt,
                    )
                }
                drawLine(
                    color = color,
                    start = Offset(xStart, y),
                    end = Offset(xEnd, y),
                    strokeWidth = if (cell.isActive) activeStroke else inactiveStroke,
                    cap = if (cell.isActive) androidx.compose.ui.graphics.StrokeCap.Butt else androidx.compose.ui.graphics.StrokeCap.Round,
                )
            }
        }
    }
}

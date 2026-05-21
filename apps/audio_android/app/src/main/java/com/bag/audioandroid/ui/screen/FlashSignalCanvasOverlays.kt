package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
internal fun BoxScope.FlashSignalCanvasOverlays(
    mode: FlashSignalVisualizationMode,
    canvasDecision: FlashSignalCanvasDecision?,
    runtimeState: FlashSignalCanvasRuntimeState,
    activeToneColor: Color,
    inactiveToneColor: Color,
    pulseGuideColor: Color,
    glowColor: Color,
) {
    if (canvasDecision?.showPulseOverlay == true) {
        FlashPulseTapeOverlay(
            tapeState = runtimeState.pulseTapeState!!,
            activeToneColor = activeToneColor,
            inactiveToneColor = inactiveToneColor,
            guideColor = pulseGuideColor,
            modifier = Modifier.fillMaxSize(),
        )
    }
    if (canvasDecision?.showLaneBoundaryOverlay == true) {
        FlashLaneBitBoundaryOverlay(
            viewport = canvasDecision.fixedViewport!!,
            activeBit = runtimeState.laneActiveBitState!!,
            layout = flashVisualPlayheadLayout(mode),
            color = glowColor,
            modifier =
                Modifier
                    .fillMaxSize()
                    .testTag("flash-visual-lanes-alignment-overlay"),
        )
    }
    FlashVisualPlayheadOverlay(
        layout = flashVisualPlayheadLayout(mode),
        color = glowColor,
        modifier = Modifier.fillMaxSize(),
    )
    if (canvasDecision?.showLaneSummaryOverlay == true) {
        FlashLaneAlignmentSummaryOverlay(
            laneActiveBit = runtimeState.laneActiveBitState,
            readoutBitOffset = runtimeState.telemetryState.currentReadoutBit ?: -1,
            readoutBitValue = runtimeState.telemetryState.currentReadoutBitValue,
            tokenState = runtimeState.tokenAlignmentState,
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 14.dp, end = 16.dp)
                    .testTag("flash-visual-lanes-alignment-summary"),
        )
    }
}

@Composable
private fun FlashLaneBitBoundaryOverlay(
    viewport: FlashSignalViewport,
    activeBit: FlashLaneActiveBitState,
    layout: FlashVisualPlayheadLayout,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val leftPadding = layout.leadingPadding.toPx()
        val rightPadding = layout.trailingPadding.toPx()
        val topPadding = layout.topPadding.toPx()
        val bottomPadding = layout.bottomPadding.toPx()
        val innerWidth = (size.width - leftPadding - rightPadding).coerceAtLeast(1f)
        val startX =
            flashOverlaySampleToViewportX(
                sample = activeBit.startSample,
                viewport = viewport,
                leftPadding = leftPadding,
                innerWidth = innerWidth,
            ).coerceIn(leftPadding, size.width - rightPadding)
        val endX =
            flashOverlaySampleToViewportX(
                sample = activeBit.endSample,
                viewport = viewport,
                leftPadding = leftPadding,
                innerWidth = innerWidth,
            ).coerceIn(leftPadding, size.width - rightPadding)
        val playheadX =
            flashVisualPlayheadX(
                totalWidthPx = size.width,
                leftPaddingPx = leftPadding,
                rightPaddingPx = rightPadding,
            )
        val touchThreshold = 1.dp.toPx()
        val boundaryColor =
            if (abs(playheadX - startX) <= touchThreshold) {
                color.copy(alpha = 0.94f)
            } else {
                color.copy(alpha = 0.42f)
            }
        val overlayHeight = size.height - topPadding - bottomPadding

        drawLine(
            color = boundaryColor,
            start = Offset(startX, topPadding),
            end = Offset(startX, size.height - bottomPadding),
            strokeWidth = 1.dp.toPx(),
        )
        drawLine(
            color = color.copy(alpha = 0.22f),
            start = Offset(endX, topPadding),
            end = Offset(endX, size.height - bottomPadding),
            strokeWidth = 1.dp.toPx(),
        )
        drawRect(
            color = color.copy(alpha = 0.10f),
            topLeft = Offset(startX, topPadding),
            size = Size((endX - startX).coerceAtLeast(1f), overlayHeight),
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

@Composable
private fun FlashLaneAlignmentSummaryOverlay(
    laneActiveBit: FlashLaneActiveBitState?,
    readoutBitOffset: Int,
    readoutBitValue: Char?,
    tokenState: FlashTokenAlignmentState?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.88f),
        contentColor = Color.Black,
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text =
                buildString {
                    append("lane ")
                    append(bitLabel(laneActiveBit?.bitOffset ?: -1, laneActiveBit?.bitValue))
                    append("  row ")
                    append(bitLabel(readoutBitOffset, readoutBitValue))
                    append("  token ")
                    append(bitLabel(tokenState?.globalBitOffset ?: -1, tokenState?.currentBitValue))
                    tokenState?.takeIf { it.byteHex.isNotBlank() || it.byteBinary.isNotBlank() }?.let { state ->
                        append('\n')
                        append("hex ")
                        append(state.byteHex.ifBlank { "_" })
                        append("  bin ")
                        append(state.byteBinary.ifBlank { "_" })
                    }
                },
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

private fun bitLabel(
    bitOffset: Int,
    bitValue: Char?,
): String =
    if (bitOffset < 0) {
        "_"
    } else {
        "$bitOffset:${bitValue ?: '_'}"
    }

private fun flashOverlaySampleToViewportX(
    sample: Float,
    viewport: FlashSignalViewport,
    leftPadding: Float,
    innerWidth: Float,
): Float = leftPadding + ((sample - viewport.startSample) / viewport.sampleCount) * innerWidth

@Composable
private fun FlashVisualPlayheadOverlay(
    layout: FlashVisualPlayheadLayout,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val leftPadding = layout.leadingPadding.toPx()
        val rightPadding = layout.trailingPadding.toPx()
        val topPadding = layout.topPadding.toPx()
        val bottomPadding = layout.bottomPadding.toPx()
        val playheadX =
            flashVisualPlayheadX(
                totalWidthPx = size.width,
                leftPaddingPx = leftPadding,
                rightPaddingPx = rightPadding,
            )

        drawLine(
            color = color.copy(alpha = 0.80f),
            start = Offset(playheadX, topPadding),
            end = Offset(playheadX, size.height - bottomPadding),
            strokeWidth = 2.dp.toPx(),
        )
    }
}

internal data class FlashVisualPlayheadLayout(
    val leadingPadding: Dp,
    val trailingPadding: Dp,
    val topPadding: Dp,
    val bottomPadding: Dp,
)

internal fun flashVisualPlayheadLayout(mode: FlashSignalVisualizationMode): FlashVisualPlayheadLayout =
    FlashVisualPlayheadLayout(
        leadingPadding = 12.dp,
        trailingPadding = 12.dp,
        topPadding = 12.dp,
        bottomPadding = 12.dp,
    )

internal fun flashVisualPlayheadX(
    totalWidthPx: Float,
    leftPaddingPx: Float,
    rightPaddingPx: Float,
): Float {
    val innerWidth = (totalWidthPx - leftPaddingPx - rightPaddingPx).coerceAtLeast(1f)
    return leftPaddingPx + innerWidth * FlashSignalPlayheadAnchorRatio
}

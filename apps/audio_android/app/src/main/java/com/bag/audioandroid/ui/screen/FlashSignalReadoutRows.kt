package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun FlashVisualFpsOverlay(
    snapshot: FlashVisualPerfSnapshot,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.White,
        contentColor = Color.Black,
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text =
                "FPS ${snapshot.drawFps.toInt()}  " +
                    "avg ${"%.1f".format(snapshot.drawAvgMs)}ms  " +
                    "max ${"%.1f".format(snapshot.drawMaxMs)}ms\n" +
                    "raw ${"%.1f".format(snapshot.rawUpdatesPerSecond)}/s  " +
                    "step ${"%.1f".format(snapshot.rawStepAvgMs)}ms  " +
                    "max ${"%.1f".format(snapshot.rawStepMaxMs)}ms\n" +
                    "smooth ${"%.1f".format(snapshot.smoothStepAvgMs)}ms  " +
                    "err ${"%.1f".format(snapshot.visualErrorMs)}ms  " +
                    "px ${"%.2f".format(snapshot.visualPxStepAvg)}/${"%.2f".format(snapshot.visualPxStepMax)}\n" +
                    "jump ${"%.1f".format(snapshot.anchorJumpMaxMs)}ms  " +
                    "reset ${snapshot.smoothResetCount}  " +
                    "vp ${"%.1f".format(snapshot.viewportStartStepMaxMs)}ms",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
internal fun FlashBitReadoutRow(
    cells: List<FlashBitReadoutCell>,
    activeToneColor: Color,
    baseBackground: Color,
    isPreviousRow: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        cells.forEach { cell ->
            val cellBackground =
                when {
                    cell.isCurrent -> activeToneColor.copy(alpha = 0.18f)
                    cell.bit != null && isPreviousRow -> baseBackground.copy(alpha = 0.40f)
                    cell.bit != null -> baseBackground.copy(alpha = 0.52f)
                    else -> baseBackground.copy(alpha = 0.24f)
                }
            val cellColor =
                when {
                    cell.isCurrent -> activeToneColor.copy(alpha = 0.94f)
                    cell.bit != null && isPreviousRow -> activeToneColor.copy(alpha = 0.48f)
                    cell.bit != null -> activeToneColor.copy(alpha = 0.72f)
                    else -> activeToneColor.copy(alpha = 0f)
                }
            Text(
                text = cell.bit?.toString().orEmpty(),
                modifier =
                    Modifier
                        .weight(1f)
                        .height(24.dp)
                        .background(
                            color = cellBackground,
                            shape = RoundedCornerShape(4.dp),
                        ).padding(vertical = 1.dp),
                color = cellColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 17.sp,
                fontWeight = if (cell.isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

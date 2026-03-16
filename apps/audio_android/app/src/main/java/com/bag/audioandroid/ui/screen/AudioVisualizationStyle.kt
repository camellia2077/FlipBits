package com.bag.audioandroid.ui.screen

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.bag.audioandroid.domain.AudioVisualizationRegion
import kotlin.math.max

internal data class VisualizationRegionPalette(
    val payloadColor: Color,
    val leadingShellColor: Color,
    val trailingShellColor: Color,
    val unknownColor: Color
)

internal fun VisualizationRegionPalette.colorFor(region: AudioVisualizationRegion): Color = when (region) {
    AudioVisualizationRegion.LeadingShell -> leadingShellColor
    AudioVisualizationRegion.Payload -> payloadColor
    AudioVisualizationRegion.TrailingShell -> trailingShellColor
    AudioVisualizationRegion.Unknown -> unknownColor
}

internal fun DrawScope.drawWindowRegionWash(
    slices: List<WaveSliceModel>,
    windowStart: Float,
    windowSampleCount: Float,
    leftPadding: Float,
    innerWidth: Float,
    topPadding: Float,
    innerHeight: Float,
    regionPalette: VisualizationRegionPalette
) {
    val safeWindowSampleCount = windowSampleCount.coerceAtLeast(1f)
    slices.forEach { slice ->
        if (slice.coverage <= 0f) {
            return@forEach
        }
        val startRatio = ((slice.startSample - windowStart) / safeWindowSampleCount).coerceIn(0f, 1f)
        val endRatio = ((slice.endSample - windowStart) / safeWindowSampleCount).coerceIn(0f, 1f)
        val widthRatio = (endRatio - startRatio).coerceAtLeast(0f)
        if (widthRatio <= 0f) {
            return@forEach
        }
        drawRect(
            color = regionPalette.colorFor(slice.region).copy(alpha = 0.04f * slice.coverage),
            topLeft = Offset(leftPadding + innerWidth * startRatio, topPadding),
            size = Size(
                width = max(1f, innerWidth * widthRatio),
                height = innerHeight
            )
        )
    }
}

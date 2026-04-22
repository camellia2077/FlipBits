package com.bag.audioandroid.ui.screen

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import com.bag.audioandroid.ui.theme.AudioEncodeGlyphColors
import kotlin.math.floor

internal const val IconViewportSize = 256f
private const val SplitX = 128f
private const val OuterGearRotationDegrees = 15f
private const val GearStrokeWidth = 3f
private const val CoreRingRadius = 40f
private const val CoreRingStrokeWidth = 3.5f
private const val CoreRingDashCount = 8
private const val CoreRingHalfSegmentCount = CoreRingDashCount * 2
private const val CoreRingDashSpacingDegrees = 360f / CoreRingDashCount
private const val CoreRingFullDashSweepDegrees = CoreRingDashSpacingDegrees * 0.5f
private const val CoreRingHalfDashSweepDegrees = CoreRingFullDashSweepDegrees / 2f
private const val CoreRingStartAngle = -90f

private val GlyphCenter = Offset(128f, 128f)

internal fun DrawScope.drawAudioEncodeGlyph(
    outerGearPath: Path,
    innerGearPath: Path,
    outerRotation: Float,
    innerRotation: Float,
    encodeProgress: Float,
    isEncodingBusy: Boolean,
    glyphColors: AudioEncodeGlyphColors,
) {
    drawSplitOuterGear(
        outerGearPath = outerGearPath,
        rotationDegrees = outerRotation,
        glyphColors = glyphColors,
    )
    drawSplitInnerGear(
        innerGearPath = innerGearPath,
        rotationDegrees = innerRotation,
        glyphColors = glyphColors,
    )
    drawOuterGearOutline(
        outerGearPath = outerGearPath,
        rotationDegrees = outerRotation,
        glyphColors = glyphColors,
    )
    drawInnerGearOutline(
        innerGearPath = innerGearPath,
        rotationDegrees = innerRotation,
        glyphColors = glyphColors,
    )
    drawSplitCoreRing(
        encodeProgress = encodeProgress,
        isEncodingBusy = isEncodingBusy,
        glyphColors = glyphColors,
    )
}

private fun DrawScope.drawSplitOuterGear(
    outerGearPath: Path,
    rotationDegrees: Float,
    glyphColors: AudioEncodeGlyphColors,
) {
    clipRect(
        left = 0f,
        top = 0f,
        right = SplitX,
        bottom = IconViewportSize,
    ) {
        rotate(
            degrees = OuterGearRotationDegrees + rotationDegrees,
            pivot = GlyphCenter,
        ) {
            drawPath(
                path = outerGearPath,
                color = glyphColors.secondarySplit,
            )
        }
    }
    clipRect(
        left = SplitX,
        top = 0f,
        right = IconViewportSize,
        bottom = IconViewportSize,
    ) {
        rotate(
            degrees = OuterGearRotationDegrees + rotationDegrees,
            pivot = GlyphCenter,
        ) {
            drawPath(
                path = outerGearPath,
                color = glyphColors.primarySplit,
            )
        }
    }
}

private fun DrawScope.drawSplitInnerGear(
    innerGearPath: Path,
    rotationDegrees: Float,
    glyphColors: AudioEncodeGlyphColors,
) {
    clipRect(
        left = 0f,
        top = 0f,
        right = SplitX,
        bottom = IconViewportSize,
    ) {
        rotate(degrees = rotationDegrees, pivot = GlyphCenter) {
            drawPath(
                path = innerGearPath,
                color = glyphColors.primarySplit,
            )
        }
    }
    clipRect(
        left = SplitX,
        top = 0f,
        right = IconViewportSize,
        bottom = IconViewportSize,
    ) {
        rotate(degrees = rotationDegrees, pivot = GlyphCenter) {
            drawPath(
                path = innerGearPath,
                color = glyphColors.secondarySplit,
            )
        }
    }
}

private fun DrawScope.drawOuterGearOutline(
    outerGearPath: Path,
    rotationDegrees: Float,
    glyphColors: AudioEncodeGlyphColors,
) {
    rotate(
        degrees = OuterGearRotationDegrees + rotationDegrees,
        pivot = GlyphCenter,
    ) {
        drawPath(
            path = outerGearPath,
            color = glyphColors.outline,
            style =
                Stroke(
                    width = GearStrokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
        )
    }
}

private fun DrawScope.drawInnerGearOutline(
    innerGearPath: Path,
    rotationDegrees: Float,
    glyphColors: AudioEncodeGlyphColors,
) {
    rotate(degrees = rotationDegrees, pivot = GlyphCenter) {
        drawPath(
            path = innerGearPath,
            color = glyphColors.outline,
            style =
                Stroke(
                    width = GearStrokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
        )
    }
}

private fun DrawScope.drawSplitCoreRing(
    encodeProgress: Float,
    isEncodingBusy: Boolean,
    glyphColors: AudioEncodeGlyphColors,
) {
    val arcBounds =
        Size(
            width = CoreRingRadius * 2f,
            height = CoreRingRadius * 2f,
        )
    val arcTopLeft =
        Offset(
            x = GlyphCenter.x - CoreRingRadius,
            y = GlyphCenter.y - CoreRingRadius,
        )
    val segmentStyle =
        Stroke(
            width = CoreRingStrokeWidth,
            cap = StrokeCap.Butt,
        )
    val visibleSegmentCount =
        if (isEncodingBusy) {
            floor((encodeProgress.coerceIn(0f, 1f) * CoreRingHalfSegmentCount).toDouble()).toInt()
        } else {
            CoreRingHalfSegmentCount
        }

    if (visibleSegmentCount <= 0) {
        return
    }

    repeat(visibleSegmentCount) { index ->
        // Treat the 12 o'clock dash as the ritual start marker: busy-state progress reveals its
        // right half first and only closes the left half at the very end of the full cycle.
        val halfSegmentIndex =
            when (index) {
                0 -> 1
                CoreRingHalfSegmentCount - 1 -> 0
                else -> index + 1
            }
        val dashIndex = halfSegmentIndex / 2
        val dashCenterAngle = CoreRingStartAngle + (dashIndex * CoreRingDashSpacingDegrees)
        // The 12 o'clock dash intentionally straddles the left/right split line, so busy-state
        // progress reveals each dash in two halves and starts with the top-right half first.
        val startAngle =
            if (halfSegmentIndex % 2 == 0) {
                dashCenterAngle - CoreRingHalfDashSweepDegrees
            } else {
                dashCenterAngle
            }
        val sweepAngle = CoreRingHalfDashSweepDegrees
        val midAngleRadians = Math.toRadians((startAngle + (sweepAngle / 2f)).toDouble())
        // Keep the ring's split-color rule anchored to the screen left/right halves so it
        // matches the SVG design even while the gears animate independently.
        val sampleX = GlyphCenter.x + (CoreRingRadius * kotlin.math.cos(midAngleRadians).toFloat())
        val segmentColor = if (sampleX < SplitX) glyphColors.secondarySplit else glyphColors.primarySplit
        drawArc(
            color = segmentColor,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcBounds,
            style = segmentStyle,
        )
    }
}

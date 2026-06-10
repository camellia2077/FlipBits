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

internal const val IconViewportSize = 256f
private const val SplitX = 128f
private const val OuterGearRotationDegrees = 15f
private const val GearStrokeWidth = 3f
private const val CoreRingRadius = 40f
private const val CoreRingStrokeWidth = 3.5f
private const val CoreRingDashCount = 8
private const val CoreRingDashSpacingDegrees = 360f / CoreRingDashCount
private const val CoreRingFullDashSweepDegrees = CoreRingDashSpacingDegrees * 0.5f
private const val CoreRingStartAngle = -90f

private val GlyphCenter = Offset(128f, 128f)
private val CoreRingDashParts = buildCoreRingDashParts()

internal fun DrawScope.drawAudioEncodeGlyph(
    outerGearPath: Path,
    innerGearPath: Path,
    outerRotation: Float,
    innerRotation: Float,
    encodeProgress: Float,
    isEncodingBusy: Boolean,
    showIdleCoreRing: Boolean,
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
        showIdleCoreRing = showIdleCoreRing,
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
    showIdleCoreRing: Boolean,
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
    if (!isEncodingBusy) {
        if (!showIdleCoreRing) {
            return
        }
        CoreRingDashParts.forEach { part ->
            drawCoreRingSegment(
                part = part,
                sweepFraction = 1f,
                arcTopLeft = arcTopLeft,
                arcBounds = arcBounds,
                segmentStyle = segmentStyle,
                glyphColors = glyphColors,
            )
        }
        return
    }

    val normalizedProgress = encodeProgress.coerceIn(0f, 1f)
    if (normalizedProgress <= 0f) {
        return
    }

    val scaledProgress = normalizedProgress * CoreRingDashParts.size.toFloat()
    val completedPartCount = scaledProgress.toInt().coerceIn(0, CoreRingDashParts.size)
    val partialPartFraction = (scaledProgress - completedPartCount.toFloat()).coerceIn(0f, 1f)

    repeat(completedPartCount) { partIndex ->
        drawCoreRingSegment(
            part = CoreRingDashParts[partIndex],
            sweepFraction = 1f,
            arcTopLeft = arcTopLeft,
            arcBounds = arcBounds,
            segmentStyle = segmentStyle,
            glyphColors = glyphColors,
        )
    }
    if (completedPartCount < CoreRingDashParts.size && partialPartFraction > 0f) {
        drawCoreRingSegment(
            part = CoreRingDashParts[completedPartCount],
            sweepFraction = partialPartFraction,
            arcTopLeft = arcTopLeft,
            arcBounds = arcBounds,
            segmentStyle = segmentStyle,
            glyphColors = glyphColors,
        )
    }
}

private fun DrawScope.drawCoreRingSegment(
    part: CoreRingDashPart,
    sweepFraction: Float,
    arcTopLeft: Offset,
    arcBounds: Size,
    segmentStyle: Stroke,
    glyphColors: AudioEncodeGlyphColors,
) {
    if (sweepFraction <= 0f) {
        return
    }

    val clampedSweepFraction = sweepFraction.coerceIn(0f, 1f)
    if (clampedSweepFraction <= 0f) {
        return
    }

    drawCoreRingArcPart(
        startAngle = part.startAngle,
        sweepAngle = part.sweepAngle * clampedSweepFraction,
        arcTopLeft = arcTopLeft,
        arcBounds = arcBounds,
        segmentStyle = segmentStyle,
        glyphColors = glyphColors,
    )
}

private fun DrawScope.drawCoreRingArcPart(
    startAngle: Float,
    sweepAngle: Float,
    arcTopLeft: Offset,
    arcBounds: Size,
    segmentStyle: Stroke,
    glyphColors: AudioEncodeGlyphColors,
) {
    if (sweepAngle <= 0f) {
        return
    }

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

private data class CoreRingDashPart(
    val startAngle: Float,
    val sweepAngle: Float,
)

private fun buildCoreRingDashParts(): List<CoreRingDashPart> {
    val halfSweep = CoreRingFullDashSweepDegrees / 2f
    val parts = mutableListOf<CoreRingDashPart>()
    parts +=
        CoreRingDashPart(
            startAngle = CoreRingStartAngle,
            sweepAngle = halfSweep,
        )
    for (dashIndex in 1 until CoreRingDashCount) {
        val centerAngle = CoreRingStartAngle + (dashIndex * CoreRingDashSpacingDegrees)
        parts +=
            CoreRingDashPart(
                startAngle = centerAngle - halfSweep,
                sweepAngle = halfSweep,
            )
        parts +=
            CoreRingDashPart(
                startAngle = centerAngle,
                sweepAngle = halfSweep,
            )
    }
    parts +=
        CoreRingDashPart(
            startAngle = CoreRingStartAngle - halfSweep,
            sweepAngle = halfSweep,
        )
    return parts
}

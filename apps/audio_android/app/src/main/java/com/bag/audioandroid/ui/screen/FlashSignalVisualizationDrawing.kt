package com.bag.audioandroid.ui.screen

import android.graphics.Paint
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

internal data class ToneSpectrumDrawStats(
    val validBuckets: Int,
    val lineSegments: Int,
    val pointDraws: Int,
    val minFrequencyHz: Float,
    val maxFrequencyHz: Float,
    val firstFrequencyHz: Float,
    val lastFrequencyHz: Float,
    val currentFrequencyHz: Float,
    val minStrokeWidthPx: Float,
    val maxStrokeWidthPx: Float,
) {
    companion object {
        val Empty =
            ToneSpectrumDrawStats(
                validBuckets = 0,
                lineSegments = 0,
                pointDraws = 0,
                minFrequencyHz = 0f,
                maxFrequencyHz = 0f,
                firstFrequencyHz = 0f,
                lastFrequencyHz = 0f,
                currentFrequencyHz = 0f,
                minStrokeWidthPx = 0f,
                maxStrokeWidthPx = 0f,
            )
    }
}

internal data class FlashSignalViewport(
    val startSample: Float,
    val endSample: Float,
    val playheadSample: Float,
) {
    val sampleCount: Float = (endSample - startSample).coerceAtLeast(1f)
}

internal fun DrawScope.drawToneTrackSegments(
    segments: List<FlashSignalToneSegment>,
    viewport: FlashSignalViewport,
    leftPadding: Float,
    topPadding: Float,
    innerWidth: Float,
    innerHeight: Float,
    activeToneColor: Color,
    inactiveToneColor: Color,
    centerLineColor: Color,
    glowPulse: Float,
    enableViewportEdgeFade: Boolean = true,
) {
    val laneGap = 10.dp.toPx()
    val laneHeight = (innerHeight - laneGap).coerceAtLeast(1f) / 2f
    val upperTop = topPadding
    val lowerTop = topPadding + laneHeight + laneGap
    val edgeFadeSamples = if (enableViewportEdgeFade) edgeFadeSampleCount(segments, viewport) else 0f

    drawLine(
        color = centerLineColor,
        start = Offset(leftPadding, upperTop + laneHeight + laneGap / 2f),
        end = Offset(leftPadding + innerWidth, upperTop + laneHeight + laneGap / 2f),
        strokeWidth = 1.dp.toPx(),
    )
    clipFlashViewport(leftPadding, topPadding, innerWidth, innerHeight) {
        segments.forEach { segment ->
            if (!segment.overlaps(viewport)) {
                return@forEach
            }
            val startX = sampleToViewportX(segment.startSample.toFloat(), viewport, leftPadding, innerWidth)
            val endX = sampleToViewportX(segment.endSample.toFloat(), viewport, leftPadding, innerWidth)
            val drawBounds = segmentVisualBounds(segment, viewport, startX, endX, minVisibleWidth = 1.6f)
            val segmentWidth = (drawBounds.endX - drawBounds.startX).coerceAtLeast(1.6f)
            val top =
                when (segment.tone) {
                    FskDominantTone.High -> upperTop
                    FskDominantTone.Low -> lowerTop
                    FskDominantTone.Unknown -> return@forEach
                }
            drawMicroBarsAcrossSegment(
                segment = segment,
                viewport = viewport,
                startX = drawBounds.startX,
                endX = drawBounds.endX,
                minBarHeight = laneHeight * 0.48f,
                maxBarHeight = laneHeight * 0.82f,
                centerY = top + laneHeight / 2f,
                activeToneColor = activeToneColor,
                inactiveToneColor = inactiveToneColor,
                glowPulse = glowPulse,
                minVisualWidth = segmentWidth,
                edgeFadeSamples = edgeFadeSamples,
            )
        }
    }
}

internal fun DrawScope.drawBitCellSegments(
    segments: List<FlashSignalToneSegment>,
    viewport: FlashSignalViewport,
    leftPadding: Float,
    topPadding: Float,
    innerWidth: Float,
    innerHeight: Float,
    activeToneColor: Color,
    inactiveToneColor: Color,
    centerLineColor: Color,
    glowPulse: Float,
    enableViewportEdgeFade: Boolean = true,
) {
    val centerY = topPadding + innerHeight / 2f
    val laneGap = 8.dp.toPx()
    val laneHeight = (innerHeight - laneGap).coerceAtLeast(1f) / 2f
    val upperTop = topPadding + laneHeight * 0.14f
    val lowerTop = centerY + laneGap / 2f + laneHeight * 0.14f
    val contentHeight = laneHeight * 0.72f

    drawLine(
        color = centerLineColor,
        start = Offset(leftPadding, centerY),
        end = Offset(leftPadding + innerWidth, centerY),
        strokeWidth = 1.dp.toPx(),
    )
    clipFlashViewport(leftPadding, topPadding, innerWidth, innerHeight) {
        segments.forEach { segment ->
            if (!segment.overlaps(viewport)) {
                return@forEach
            }
            val startX = sampleToViewportX(segment.startSample.toFloat(), viewport, leftPadding, innerWidth)
            val endX = sampleToViewportX(segment.endSample.toFloat(), viewport, leftPadding, innerWidth)
            val drawBounds = segmentVisualBounds(segment, viewport, startX, endX, minVisibleWidth = 1.2f)
            val rectStartX = drawBounds.startX.coerceAtLeast(leftPadding)
            val rectEndX = drawBounds.endX.coerceAtMost(leftPadding + innerWidth)
            val rectWidth = rectEndX - rectStartX
            if (rectWidth <= 0f) {
                return@forEach
            }
            val isCurrentBit =
                viewport.playheadSample >= segment.startSample.toFloat() &&
                    viewport.playheadSample < segment.endSample.toFloat()
            val isPastBit = segment.endSample.toFloat() <= viewport.playheadSample
            val top =
                when (segment.tone) {
                    FskDominantTone.High -> upperTop
                    FskDominantTone.Low -> lowerTop
                    FskDominantTone.Unknown -> return@forEach
                }
            drawLaneSegmentProgress(
                rectStartX = rectStartX,
                rectEndX = rectEndX,
                top = top,
                height = contentHeight,
                splitX =
                    sampleToViewportX(
                        viewport.playheadSample,
                        viewport,
                        leftPadding,
                        innerWidth,
                    ),
                isCurrent = isCurrentBit,
                isPast = isPastBit,
                activeToneColor = activeToneColor,
                inactiveToneColor = inactiveToneColor,
            )
        }
    }
}

internal fun DrawScope.drawPitchSegments(
    segments: List<FlashSignalToneSegment>,
    viewport: FlashSignalViewport,
    leftPadding: Float,
    topPadding: Float,
    innerWidth: Float,
    innerHeight: Float,
    activeToneColor: Color,
    inactiveToneColor: Color,
    centerLineColor: Color,
    glowPulse: Float,
    enableViewportEdgeFade: Boolean = true,
) {
    val highY = topPadding + innerHeight * 0.12f
    val lowY = topPadding + innerHeight * 0.88f
    val activeStrokeWidth = 4.8.dp.toPx()
    val inactiveStrokeWidth = 3.6.dp.toPx()
    val edgeFadeSamples = if (enableViewportEdgeFade) edgeFadeSampleCount(segments, viewport) else 0f

    drawLine(
        color = centerLineColor.copy(alpha = 0.20f),
        start = Offset(leftPadding, highY),
        end = Offset(leftPadding + innerWidth, highY),
        strokeWidth = 1.dp.toPx(),
    )
    drawLine(
        color = centerLineColor.copy(alpha = 0.20f),
        start = Offset(leftPadding, lowY),
        end = Offset(leftPadding + innerWidth, lowY),
        strokeWidth = 1.dp.toPx(),
    )

    clipFlashViewport(leftPadding, topPadding, innerWidth, innerHeight) {
        segments.forEach { segment ->
            if (!segment.overlaps(viewport)) {
                return@forEach
            }
            val y =
                when (segment.tone) {
                    FskDominantTone.High -> highY
                    FskDominantTone.Low -> lowY
                    FskDominantTone.Unknown -> return@forEach
                }
            val startX = sampleToViewportX(segment.startSample.toFloat(), viewport, leftPadding, innerWidth)
            val endX = sampleToViewportX(segment.endSample.toFloat(), viewport, leftPadding, innerWidth)
            drawPitchSegmentedBar(
                segment = segment,
                viewport = viewport,
                startX = startX,
                endX = endX,
                y = y,
                activeBarHeight = activeStrokeWidth,
                inactiveBarHeight = inactiveStrokeWidth,
                activeToneColor = activeToneColor,
                inactiveToneColor = inactiveToneColor,
                glowPulse = glowPulse,
                edgeFadeSamples = edgeFadeSamples,
            )
        }
    }
}

internal fun DrawScope.drawLanes(
    buckets: List<FskEnergyBucket>,
    activeThresholdBucketIndex: Float,
    activeWindowBucketCount: Int,
    bucketOffset: Float,
    leftPadding: Float,
    topPadding: Float,
    innerWidth: Float,
    innerHeight: Float,
    bucketWidth: Float,
    activeToneColor: Color,
    inactiveToneColor: Color,
    centerLineColor: Color,
    glowPulse: Float,
) {
    val laneGap = 10.dp.toPx()
    val laneHeight = (innerHeight - laneGap).coerceAtLeast(1f) / 2f
    val upperTop = topPadding
    val lowerTop = topPadding + laneHeight + laneGap

    drawLine(
        color = centerLineColor,
        start = Offset(leftPadding, upperTop + laneHeight + laneGap / 2f),
        end = Offset(leftPadding + innerWidth, upperTop + laneHeight + laneGap / 2f),
        strokeWidth = 1.dp.toPx(),
    )
    buckets.forEachIndexed { index, bucket ->
        val isActiveBucket = index.toFloat() <= activeThresholdBucketIndex
        val x = leftPadding + bucketWidth * (index.toFloat() - bucketOffset)
        val contentWidth = (bucketWidth - 2.dp.toPx()).coerceAtLeast(2.4f)
        val heldHighStrength = bucket.heldToneStrength(buckets, index, FskDominantTone.High)
        val heldLowStrength = bucket.heldToneStrength(buckets, index, FskDominantTone.Low)
        val highAlpha =
            when (bucket.dominantTone) {
                FskDominantTone.High -> 0.30f + bucket.highStrength * (0.54f + 0.08f * glowPulse)
                FskDominantTone.Unknown -> 0.06f + bucket.highStrength * 0.20f
                FskDominantTone.Low -> 0.08f + bucket.highStrength * 0.18f
            }
        val lowAlpha =
            when (bucket.dominantTone) {
                FskDominantTone.Low -> 0.30f + bucket.lowStrength * (0.54f + 0.08f * glowPulse)
                FskDominantTone.Unknown -> 0.06f + bucket.lowStrength * 0.20f
                FskDominantTone.High -> 0.08f + bucket.lowStrength * 0.18f
            }
        val highHoldAlpha = if (heldHighStrength > bucket.highStrength) heldHighStrength * FlashToneHoldAlphaBoost else 0f
        val lowHoldAlpha = if (heldLowStrength > bucket.lowStrength) heldLowStrength * FlashToneHoldAlphaBoost else 0f
        val highHeight = laneHeight * (0.24f + 0.66f * heldHighStrength)
        val lowHeight = laneHeight * (0.24f + 0.66f * heldLowStrength)
        val highColor =
            toneBucketColor(
                isActive = isActiveBucket && bucket.dominantTone == FskDominantTone.High,
                activeColor = activeToneColor,
                inactiveColor = inactiveToneColor,
                alpha = highAlpha + highHoldAlpha,
                strength = heldHighStrength,
            )
        val lowColor =
            toneBucketColor(
                isActive = isActiveBucket && bucket.dominantTone == FskDominantTone.Low,
                activeColor = activeToneColor,
                inactiveColor = inactiveToneColor,
                alpha = lowAlpha + lowHoldAlpha,
                strength = heldLowStrength,
            )

        drawRoundRect(
            color = highColor,
            topLeft = Offset(x + (bucketWidth - contentWidth) / 2f, upperTop + (laneHeight - highHeight) / 2f),
            size = Size(contentWidth, highHeight),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
        )
        drawRoundRect(
            color = lowColor,
            topLeft = Offset(x + (bucketWidth - contentWidth) / 2f, lowerTop + (laneHeight - lowHeight) / 2f),
            size = Size(contentWidth, lowHeight),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
        )
    }
}

internal fun DrawScope.drawBitCells(
    buckets: List<FskEnergyBucket>,
    activeThresholdBucketIndex: Float,
    activeWindowBucketCount: Int,
    bucketOffset: Float,
    leftPadding: Float,
    topPadding: Float,
    innerWidth: Float,
    innerHeight: Float,
    bucketWidth: Float,
    activeToneColor: Color,
    inactiveToneColor: Color,
    centerLineColor: Color,
    glowPulse: Float,
) {
    val centerY = topPadding + innerHeight / 2f
    val laneGap = 8.dp.toPx()
    val laneHeight = (innerHeight - laneGap).coerceAtLeast(1f) / 2f
    val upperTop = topPadding + laneHeight * 0.14f
    val lowerTop = centerY + laneGap / 2f + laneHeight * 0.14f
    val contentHeight = laneHeight * 0.72f
    val currentBucketIndex = activeThresholdBucketIndex.toInt().coerceIn(0, buckets.lastIndex.coerceAtLeast(0))

    drawLine(
        color = centerLineColor,
        start = Offset(leftPadding, centerY),
        end = Offset(leftPadding + innerWidth, centerY),
        strokeWidth = 1.dp.toPx(),
    )
    buckets.forEachIndexed { index, bucket ->
        if (bucket.dominantTone == FskDominantTone.Unknown) {
            return@forEachIndexed
        }
        val isCurrentBucket = index == currentBucketIndex
        val isPastBucket = index < currentBucketIndex
        val x = leftPadding + bucketWidth * (index.toFloat() - bucketOffset)
        val contentWidth = bucketWidth.coerceAtLeast(1.2f)
        val top =
            when (bucket.dominantTone) {
                FskDominantTone.High -> upperTop
                FskDominantTone.Low -> lowerTop
                FskDominantTone.Unknown -> return@forEachIndexed
            }
        drawRect(
            color =
                laneRectColor(
                    isCurrent = isCurrentBucket,
                    isPast = isPastBucket,
                    activeToneColor = activeToneColor,
                    inactiveToneColor = inactiveToneColor,
                ),
            topLeft = Offset(x, top),
            size = Size(contentWidth, contentHeight),
        )
    }
}

private fun laneRectColor(
    isCurrent: Boolean,
    isPast: Boolean,
    activeToneColor: Color,
    inactiveToneColor: Color,
): Color =
    when {
        isCurrent -> activeToneColor.copy(alpha = 0.96f)
        isPast -> activeToneColor.copy(alpha = 0.52f)
        else -> inactiveToneColor.copy(alpha = 0.22f)
    }

private fun DrawScope.drawLaneSegmentProgress(
    rectStartX: Float,
    rectEndX: Float,
    top: Float,
    height: Float,
    splitX: Float,
    isCurrent: Boolean,
    isPast: Boolean,
    activeToneColor: Color,
    inactiveToneColor: Color,
) {
    val activeColor =
        laneRectColor(
            isCurrent = isCurrent,
            isPast = isPast,
            activeToneColor = activeToneColor,
            inactiveToneColor = inactiveToneColor,
        )
    val inactiveColor =
        laneRectColor(
            isCurrent = false,
            isPast = false,
            activeToneColor = activeToneColor,
            inactiveToneColor = inactiveToneColor,
        )
    when {
        isPast -> {
            drawRect(
                color = activeColor,
                topLeft = Offset(rectStartX, top),
                size = Size(rectEndX - rectStartX, height),
            )
        }
        isCurrent -> {
            val activeEndX = splitX.coerceIn(rectStartX, rectEndX)
            if (activeEndX > rectStartX) {
                drawRect(
                    color = activeColor,
                    topLeft = Offset(rectStartX, top),
                    size = Size(activeEndX - rectStartX, height),
                )
            }
            if (activeEndX < rectEndX) {
                drawRect(
                    color = inactiveColor,
                    topLeft = Offset(activeEndX, top),
                    size = Size(rectEndX - activeEndX, height),
                )
            }
        }
        else -> {
            drawRect(
                color = inactiveColor,
                topLeft = Offset(rectStartX, top),
                size = Size(rectEndX - rectStartX, height),
            )
        }
    }
}

internal fun DrawScope.drawPitch(
    buckets: List<FskEnergyBucket>,
    activeThresholdBucketIndex: Float,
    activeWindowBucketCount: Int,
    bucketOffset: Float,
    leftPadding: Float,
    topPadding: Float,
    innerWidth: Float,
    innerHeight: Float,
    bucketWidth: Float,
    activeToneColor: Color,
    inactiveToneColor: Color,
    centerLineColor: Color,
    glowPulse: Float,
) {
    val highY = topPadding + innerHeight * 0.12f
    val lowY = topPadding + innerHeight * 0.88f
    val strokeWidth = 3.dp.toPx()
    val pointRadius = (bucketWidth * 0.34f).coerceIn(1.6f, 4.8f)

    drawLine(
        color = centerLineColor.copy(alpha = 0.20f),
        start = Offset(leftPadding, highY),
        end = Offset(leftPadding + innerWidth, highY),
        strokeWidth = 1.dp.toPx(),
    )
    drawLine(
        color = centerLineColor.copy(alpha = 0.20f),
        start = Offset(leftPadding, lowY),
        end = Offset(leftPadding + innerWidth, lowY),
        strokeWidth = 1.dp.toPx(),
    )

    fun yForTone(bucket: FskEnergyBucket): Float? =
        when (bucket.dominantTone) {
            FskDominantTone.High -> highY
            FskDominantTone.Low -> lowY
            FskDominantTone.Unknown -> null
        }

    val pastPath = Path()
    val futurePath = Path()
    var pastStarted = false
    var futureStarted = false
    var pastSegmentOpen = false
    var futureSegmentOpen = false
    buckets.forEachIndexed { index, bucket ->
        val y = yForTone(bucket)
        if (y == null) {
            if (index.toFloat() <= activeThresholdBucketIndex) {
                pastSegmentOpen = false
            } else {
                futureSegmentOpen = false
            }
            return@forEachIndexed
        }

        val segmentStartX = leftPadding + bucketWidth * (index.toFloat() - bucketOffset)
        val segmentEndX = segmentStartX + bucketWidth
        val isPastBucket = index.toFloat() <= activeThresholdBucketIndex
        val targetPath = if (isPastBucket) pastPath else futurePath
        val segmentOpen = if (isPastBucket) pastSegmentOpen else futureSegmentOpen
        if (!segmentOpen) {
            targetPath.moveTo(segmentStartX, y)
            if (isPastBucket) {
                pastStarted = true
                pastSegmentOpen = true
            } else {
                futureStarted = true
                futureSegmentOpen = true
            }
        } else {
            targetPath.lineTo(segmentStartX, y)
        }
        targetPath.lineTo(segmentEndX, y)
    }

    if (futureStarted) {
        drawPath(
            path = futurePath,
            color = inactiveToneColor.copy(alpha = 0.42f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
    if (pastStarted) {
        drawPath(
            path = pastPath,
            color = activeToneColor.copy(alpha = 0.72f + 0.16f * glowPulse),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }

    buckets.forEachIndexed { index, bucket ->
        val y = yForTone(bucket) ?: return@forEachIndexed
        val isActiveBucket = index.toFloat() <= activeThresholdBucketIndex
        val centerX = leftPadding + bucketWidth * (index.toFloat() - bucketOffset + 0.5f)
        val color =
            if (isActiveBucket) {
                activeToneColor.copy(alpha = 0.72f + 0.16f * glowPulse)
            } else {
                inactiveToneColor.copy(alpha = 0.34f + 0.18f * bucket.confidence)
            }
        drawCircle(
            color = color,
            radius = pointRadius,
            center = Offset(centerX, y),
        )
    }
}

internal fun DrawScope.drawToneSpectrum(
    buckets: List<ToneSpectrumBucket>,
    frequencyScale: ToneFrequencyScale,
    activeThresholdBucketIndex: Float,
    bucketOffset: Float,
    leftPadding: Float,
    topPadding: Float,
    innerWidth: Float,
    innerHeight: Float,
    bucketWidth: Float,
    activeToneColor: Color,
    inactiveToneColor: Color,
    centerLineColor: Color,
    referenceLabelColor: Color,
    glowPulse: Float,
): ToneSpectrumDrawStats {
    if (buckets.isEmpty()) {
        return ToneSpectrumDrawStats.Empty
    }
    val minHz = frequencyScale.minHz
    val maxHz = frequencyScale.maxHz
    drawToneFrequencyReferences(
        frequencyScale = frequencyScale,
        leftPadding = leftPadding,
        topPadding = topPadding,
        innerWidth = innerWidth,
        innerHeight = innerHeight,
        activeToneColor = activeToneColor,
        centerLineColor = centerLineColor,
        referenceLabelColor = referenceLabelColor,
    )

    var validBuckets = 0
    var lineSegments = 0
    var minFrequency = Float.POSITIVE_INFINITY
    var maxFrequency = 0f
    var firstFrequency = 0f
    var lastFrequency = 0f
    var minStrokeWidth = Float.POSITIVE_INFINITY
    var maxStrokeWidth = 0f
    var previousPoint: Offset? = null
    buckets.forEachIndexed { index, bucket ->
        val x = leftPadding + bucketWidth * (index.toFloat() - bucketOffset + 0.5f)
        if (x < leftPadding - bucketWidth || x > leftPadding + innerWidth + bucketWidth) {
            return@forEachIndexed
        }
        if (bucket.frequencyHz <= 0f || bucket.amplitude < FlashSignalSilenceThreshold) {
            previousPoint = null
            return@forEachIndexed
        }
        validBuckets += 1
        minFrequency = minOf(minFrequency, bucket.frequencyHz)
        maxFrequency = maxOf(maxFrequency, bucket.frequencyHz)
        if (firstFrequency <= 0f) {
            firstFrequency = bucket.frequencyHz
        }
        lastFrequency = bucket.frequencyHz
        val y = toneSpectrumY(bucket.frequencyHz, minHz, maxHz, topPadding, innerHeight)
        val isActive = index.toFloat() <= activeThresholdBucketIndex
        val strength = bucket.strength.coerceIn(0f, 1f)
        val color =
            if (isActive) {
                activeToneColor.copy(alpha = (0.66f + 0.22f * strength + 0.06f * glowPulse).coerceIn(0f, 1f))
            } else {
                inactiveToneColor.copy(alpha = (0.38f + 0.24f * strength).coerceIn(0f, 0.72f))
            }
        val point = Offset(x, y)
        previousPoint?.let { previous ->
            val strokeWidth = 1.35.dp.toPx()
            lineSegments += 1
            minStrokeWidth = minOf(minStrokeWidth, strokeWidth)
            maxStrokeWidth = maxOf(maxStrokeWidth, strokeWidth)
            drawLine(
                color = color.copy(alpha = color.alpha * 0.72f),
                start = previous,
                end = point,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Butt,
            )
        }
        previousPoint = point
    }
    return ToneSpectrumDrawStats(
        validBuckets = validBuckets,
        lineSegments = lineSegments,
        pointDraws = 0,
        minFrequencyHz = if (validBuckets > 0) minFrequency else 0f,
        maxFrequencyHz = if (validBuckets > 0) maxFrequency else 0f,
        firstFrequencyHz = firstFrequency,
        lastFrequencyHz = lastFrequency,
        currentFrequencyHz = 0f,
        minStrokeWidthPx = if (lineSegments > 0) minStrokeWidth else 0f,
        maxStrokeWidthPx = if (lineSegments > 0) maxStrokeWidth else 0f,
    )
}

internal fun DrawScope.drawToneTimelineSegments(
    segments: List<FlashSignalToneSegment>,
    viewport: FlashSignalViewport,
    frequencyScale: ToneFrequencyScale,
    carrierLayout: ToneCarrierLayout,
    leftPadding: Float,
    topPadding: Float,
    innerWidth: Float,
    innerHeight: Float,
    activeToneColor: Color,
    inactiveToneColor: Color,
    centerLineColor: Color,
    referenceLabelColor: Color,
    glowPulse: Float,
): ToneSpectrumDrawStats {
    if (segments.isEmpty()) {
        return ToneSpectrumDrawStats.Empty
    }
    drawToneFrequencyReferences(
        frequencyScale = frequencyScale,
        leftPadding = leftPadding,
        topPadding = topPadding,
        innerWidth = innerWidth,
        innerHeight = innerHeight,
        activeToneColor = activeToneColor,
        centerLineColor = centerLineColor,
        referenceLabelColor = referenceLabelColor,
    )

    var validSegments = 0
    var minFrequency = Float.POSITIVE_INFINITY
    var maxFrequency = 0f
    var firstFrequency = 0f
    var lastFrequency = 0f
    val pulseStrokeWidth = 2.6.dp.toPx()
    val railStrokeWidth = 1.dp.toPx()
    val splitX = sampleToViewportX(viewport.playheadSample, viewport, leftPadding, innerWidth)
    val fallbackLowY =
        toneSpectrumY(
            frequencyHz = carrierLayout.lowHz,
            minHz = frequencyScale.minHz,
            maxHz = frequencyScale.maxHz,
            topPadding = topPadding,
            innerHeight = innerHeight,
        )
    val fallbackHighY =
        toneSpectrumY(
            frequencyHz = carrierLayout.highHz,
            minHz = frequencyScale.minHz,
            maxHz = frequencyScale.maxHz,
            topPadding = topPadding,
            innerHeight = innerHeight,
        )
    var currentFrequency = 0f
    clipFlashViewport(
        leftPadding = leftPadding,
        topPadding = topPadding,
        innerWidth = innerWidth,
        innerHeight = innerHeight,
    ) {
        segments.forEach { segment ->
            if (!segment.overlaps(viewport)) {
                return@forEach
            }
            val frequency =
                segment.carrierFrequencyHz.takeIf { it > 0f }
                    ?: when (segment.tone) {
                        FskDominantTone.Low -> carrierLayout.lowHz
                        FskDominantTone.High -> carrierLayout.highHz
                        FskDominantTone.Unknown -> return@forEach
                    }
            val fallbackY =
                when (segment.tone) {
                    FskDominantTone.Low -> fallbackLowY
                    FskDominantTone.High -> fallbackHighY
                    FskDominantTone.Unknown -> return@forEach
                }
            val startX = sampleToViewportX(segment.startSample.toFloat(), viewport, leftPadding, innerWidth)
            val endX = sampleToViewportX(segment.endSample.toFloat(), viewport, leftPadding, innerWidth)
            val drawBounds = segmentVisualBounds(segment, viewport, startX, endX, minVisibleWidth = 1.2f)
            val rectStartX = drawBounds.startX.coerceAtLeast(leftPadding)
            val rectEndX = drawBounds.endX.coerceAtMost(leftPadding + innerWidth)
            if (rectEndX <= rectStartX) {
                return@forEach
            }
            val isCurrent =
                viewport.playheadSample >= segment.startSample.toFloat() &&
                    viewport.playheadSample < segment.endSample.toFloat()
            if (isCurrent) {
                currentFrequency = frequency
            }
            validSegments += 1
            minFrequency = minOf(minFrequency, frequency)
            maxFrequency = maxOf(maxFrequency, frequency)
            if (firstFrequency <= 0f) {
                firstFrequency = frequency
            }
            lastFrequency = frequency
            val y =
                if (segment.carrierFrequencyHz > 0f) {
                    toneSpectrumY(
                        frequencyHz = frequency,
                        minHz = frequencyScale.minHz,
                        maxHz = frequencyScale.maxHz,
                        topPadding = topPadding,
                        innerHeight = innerHeight,
                    )
                } else {
                    fallbackY
                }
            drawToneTimelineProgressLine(
                startX = rectStartX,
                endX = rectEndX,
                y = y,
                splitX = splitX,
                isPast = viewport.playheadSample >= segment.endSample.toFloat(),
                isCurrent = isCurrent,
                activeToneColor = activeToneColor.copy(alpha = (0.78f + 0.10f * glowPulse).coerceIn(0f, 1f)),
                inactiveToneColor = inactiveToneColor.copy(alpha = 0.54f),
                strokeWidth = pulseStrokeWidth,
            )
        }
    }
    drawToneCurrentFrequencyHud(
        frequencyHz = currentFrequency,
        maxFrequencyHz = frequencyScale.maxHz,
        leftPadding = leftPadding,
        topPadding = topPadding,
        innerWidth = innerWidth,
        activeToneColor = activeToneColor,
        inactiveToneColor = inactiveToneColor,
    )
    return ToneSpectrumDrawStats(
        validBuckets = validSegments,
        lineSegments = validSegments,
        pointDraws = 0,
        minFrequencyHz = if (validSegments > 0) minFrequency else 0f,
        maxFrequencyHz = if (validSegments > 0) maxFrequency else 0f,
        firstFrequencyHz = firstFrequency,
        lastFrequencyHz = lastFrequency,
        currentFrequencyHz = currentFrequency,
        minStrokeWidthPx = if (validSegments > 0) pulseStrokeWidth else 0f,
        maxStrokeWidthPx = if (validSegments > 0) pulseStrokeWidth else 0f,
    )
}

private fun DrawScope.drawToneCurrentFrequencyHud(
    frequencyHz: Float,
    maxFrequencyHz: Float,
    leftPadding: Float,
    topPadding: Float,
    innerWidth: Float,
    activeToneColor: Color,
    inactiveToneColor: Color,
) {
    val valueLabel = if (frequencyHz > 0f) frequencyHz.toInt().toString() else "--"
    val fixedWidthValueLabel = maxFrequencyHz.roundToInt().coerceAtLeast(0).toString()
    val unitLabel = " Hz"
    val textPaint =
        Paint().apply {
            isAntiAlias = true
            textSize = 11.dp.toPx()
            color = activeToneColor.copy(alpha = 0.94f).toArgb()
        }
    val horizontalPadding = 6.dp.toPx()
    val verticalPadding = 4.dp.toPx()
    val valueWidth = maxOf(textPaint.measureText(valueLabel), textPaint.measureText(fixedWidthValueLabel))
    val unitWidth = textPaint.measureText(unitLabel)
    val textWidth = valueWidth + unitWidth
    val textHeight = textPaint.textSize
    val boxWidth = textWidth + horizontalPadding * 2f
    val boxHeight = textHeight + verticalPadding * 2f
    val boxLeft = (leftPadding + innerWidth - boxWidth - 6.dp.toPx()).coerceAtLeast(leftPadding)
    val boxTop = topPadding - 10.dp.toPx()
    drawRoundRect(
        color = activeToneColor.copy(alpha = 0.18f),
        topLeft = Offset(boxLeft, boxTop),
        size = Size(boxWidth, boxHeight),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
    )
    drawRoundRect(
        color = activeToneColor.copy(alpha = 0.10f),
        topLeft = Offset(boxLeft, boxTop),
        size = Size(boxWidth, boxHeight),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
    )
    drawIntoCanvas { canvas ->
        val textBaseline = boxTop + verticalPadding + textHeight * 0.82f
        val unitX = boxLeft + horizontalPadding + valueWidth
        canvas.nativeCanvas.drawText(
            valueLabel,
            boxLeft + horizontalPadding,
            textBaseline,
            textPaint,
        )
        canvas.nativeCanvas.drawText(
            unitLabel,
            unitX,
            textBaseline,
            textPaint,
        )
    }
}

private fun DrawScope.drawToneTimelineProgressLine(
    startX: Float,
    endX: Float,
    y: Float,
    splitX: Float,
    isPast: Boolean,
    isCurrent: Boolean,
    activeToneColor: Color,
    inactiveToneColor: Color,
    strokeWidth: Float,
) {
    when {
        isPast -> {
            drawLine(
                color = activeToneColor,
                start = Offset(startX, y),
                end = Offset(endX, y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Butt,
            )
        }
        isCurrent -> {
            val activeEndX = splitX.coerceIn(startX, endX)
            if (activeEndX > startX) {
                drawLine(
                    color = activeToneColor,
                    start = Offset(startX, y),
                    end = Offset(activeEndX, y),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Butt,
                )
            }
            if (activeEndX < endX) {
                drawLine(
                    color = inactiveToneColor,
                    start = Offset(activeEndX, y),
                    end = Offset(endX, y),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Butt,
                )
            }
        }
        else -> {
            drawLine(
                color = inactiveToneColor,
                start = Offset(startX, y),
                end = Offset(endX, y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Butt,
            )
        }
    }
}

private fun DrawScope.drawToneFrequencyReferences(
    frequencyScale: ToneFrequencyScale,
    leftPadding: Float,
    topPadding: Float,
    innerWidth: Float,
    innerHeight: Float,
    activeToneColor: Color,
    centerLineColor: Color,
    referenceLabelColor: Color,
) {
    val lineColor = centerLineColor
    val bandColor = activeToneColor.copy(alpha = 0.10f)
    frequencyScale.references.forEach { reference ->
        if (reference.isBand) {
            val topY =
                toneSpectrumY(
                    frequencyHz = reference.endHz,
                    minHz = frequencyScale.minHz,
                    maxHz = frequencyScale.maxHz,
                    topPadding = topPadding,
                    innerHeight = innerHeight,
                )
            val bottomY =
                toneSpectrumY(
                    frequencyHz = reference.startHz,
                    minHz = frequencyScale.minHz,
                    maxHz = frequencyScale.maxHz,
                    topPadding = topPadding,
                    innerHeight = innerHeight,
                )
            drawRect(
                color = bandColor,
                topLeft = Offset(leftPadding, topY),
                size = Size(innerWidth, (bottomY - topY).coerceAtLeast(1f)),
            )
            val labelY =
                if (topY < topPadding + innerHeight / 2f) {
                    topY - 6.dp.toPx()
                } else {
                    topY + 32.dp.toPx()
                }
            drawToneReferenceLabel(
                label = reference.label,
                x = leftPadding + 6.dp.toPx(),
                y = labelY,
                color = referenceLabelColor,
            )
        } else {
            val y =
                toneSpectrumY(
                    frequencyHz = reference.startHz,
                    minHz = frequencyScale.minHz,
                    maxHz = frequencyScale.maxHz,
                    topPadding = topPadding,
                    innerHeight = innerHeight,
                )
            val labelY =
                if (y < topPadding + innerHeight / 2f) {
                    y - 8.dp.toPx()
                } else {
                    y + 12.dp.toPx()
                }
            drawLine(
                color = lineColor,
                start = Offset(leftPadding, y),
                end = Offset(leftPadding + innerWidth, y),
                strokeWidth = 1.dp.toPx(),
            )
            drawToneReferenceLabel(
                label = reference.label,
                x = leftPadding + 6.dp.toPx(),
                y = labelY,
                color = referenceLabelColor,
            )
        }
    }
}

private fun DrawScope.drawToneReferenceLabel(
    label: String,
    x: Float,
    y: Float,
    color: Color,
) {
    val textPaint =
        Paint().apply {
            isAntiAlias = true
            textSize = 10.dp.toPx()
            this.color = color.toArgb()
        }
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText(label, x, y, textPaint)
    }
}

private fun toneSpectrumY(
    frequencyHz: Float,
    minHz: Float,
    maxHz: Float,
    topPadding: Float,
    innerHeight: Float,
): Float {
    val normalized = ((frequencyHz - minHz) / (maxHz - minHz).coerceAtLeast(1f)).coerceIn(0f, 1f)
    return topPadding + innerHeight * (1f - normalized)
}

private fun toneBucketColor(
    isActive: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    alpha: Float,
    strength: Float,
): Color =
    if (isActive) {
        activeColor.copy(alpha = alpha.coerceIn(0f, 1f))
    } else {
        inactiveColor.copy(
            alpha = (0.18f + 0.28f * strength + 0.12f * alpha.coerceIn(0f, 1f)).coerceIn(0f, 0.55f),
        )
    }

private data class SegmentVisualBounds(
    val startX: Float,
    val endX: Float,
)

private fun DrawScope.segmentVisualBounds(
    segment: FlashSignalToneSegment,
    viewport: FlashSignalViewport,
    startX: Float,
    endX: Float,
    minVisibleWidth: Float,
): SegmentVisualBounds {
    val availableWidth = endX - startX
    if (availableWidth <= minVisibleWidth) {
        return SegmentVisualBounds(startX, endX)
    }
    // The timeline stays exact: sample boundaries still map to startX/endX.
    // This inset only changes paint geometry so adjacent same-tone bits read as
    // separate symbols without moving the playhead or changing durations.
    val halfGap = FlashBitSegmentGapDp.dp.toPx() / 2f
    val drawStart = if (segment.startSample.toFloat() >= viewport.startSample) startX + halfGap else startX
    val drawEnd = if (segment.endSample.toFloat() <= viewport.endSample) endX - halfGap else endX
    return if (drawEnd - drawStart >= minVisibleWidth) {
        SegmentVisualBounds(drawStart, drawEnd)
    } else {
        SegmentVisualBounds(startX, endX)
    }
}

private inline fun DrawScope.clipFlashViewport(
    leftPadding: Float,
    topPadding: Float,
    innerWidth: Float,
    innerHeight: Float,
    block: DrawScope.() -> Unit,
) {
    clipRect(
        left = leftPadding,
        top = topPadding,
        right = leftPadding + innerWidth,
        bottom = topPadding + innerHeight,
        block = block,
    )
}

private fun DrawScope.drawMicroBarsAcrossSegment(
    segment: FlashSignalToneSegment,
    viewport: FlashSignalViewport,
    startX: Float,
    endX: Float,
    minBarHeight: Float,
    maxBarHeight: Float,
    centerY: Float,
    activeToneColor: Color,
    inactiveToneColor: Color,
    glowPulse: Float,
    minVisualWidth: Float,
    edgeFadeSamples: Float,
) {
    val visualWidth = (endX - startX).coerceAtLeast(minVisualWidth)
    val barWidth = FlashSegmentMicroBarWidthDp.dp.toPx()
    val barFractions = segmentPulseFractions(visualWidth, FlashSegmentPulseSpacingDp.dp.toPx())
    barFractions.forEachIndexed { barIndex, fraction ->
        val x = startX + visualWidth * fraction
        val sample = segment.startSample.toFloat() + segment.sampleCount.toFloat() * fraction
        val isActive = sample <= viewport.playheadSample
        val edgeGlow = activeEdgeGlow(sample, viewport, segment.sampleCount)
        val texture = deterministicSignalTexture(segment, barIndex)
        val envelope = bitSegmentEnvelope(barIndex, barFractions.size)
        val height = minBarHeight + (maxBarHeight - minBarHeight) * (0.34f + 0.66f * texture * envelope)
        val color =
            flashSegmentColor(
                isActive = isActive,
                activeColor = activeToneColor,
                inactiveColor = inactiveToneColor,
                confidence = 0.78f + 0.22f * texture,
                glowPulse = glowPulse,
                edgeGlow = edgeGlow,
                alphaMultiplier = viewportEdgeFadeMultiplier(sample, viewport, edgeFadeSamples),
            )
        drawRoundRect(
            color = color,
            topLeft = Offset(x - barWidth / 2f, centerY - height / 2f),
            size = Size(barWidth, height),
            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
        )
    }
}

private fun segmentPulseFractions(
    visualWidth: Float,
    spacingPx: Float,
): FloatArray {
    val safeSpacingPx = spacingPx.coerceAtLeast(1f)
    val pulseCount = (visualWidth / safeSpacingPx).toInt().coerceAtLeast(2)
    val occupiedWidth = safeSpacingPx * (pulseCount - 1).toFloat()
    val firstX = (visualWidth - occupiedWidth).coerceAtLeast(0f) / 2f
    return FloatArray(pulseCount) { index ->
        ((firstX + safeSpacingPx * index.toFloat()) / visualWidth.coerceAtLeast(1f))
            .coerceIn(0f, 1f)
    }
}

private fun DrawScope.drawPitchSegmentedBar(
    segment: FlashSignalToneSegment,
    viewport: FlashSignalViewport,
    startX: Float,
    endX: Float,
    y: Float,
    activeBarHeight: Float,
    inactiveBarHeight: Float,
    activeToneColor: Color,
    inactiveToneColor: Color,
    glowPulse: Float,
    edgeFadeSamples: Float,
) {
    val availableWidth = endX - startX
    if (availableWidth <= 0f) {
        return
    }

    val gapInset = FlashPitchSegmentGapDp.dp.toPx() / 2f
    val minVisibleWidth = FlashPitchSegmentMinVisibleWidthDp.dp.toPx()
    val canApplyGap = availableWidth > gapInset * 2f + minVisibleWidth
    val drawStartX = if (canApplyGap) startX + gapInset else startX
    val drawEndX = if (canApplyGap) endX - gapInset else endX
    val visualWidth = (drawEndX - drawStartX).coerceAtLeast(1f)
    val visibleStartSample = maxOf(segment.startSample.toFloat(), viewport.startSample)
    val visibleEndSample = minOf(segment.endSample.toFloat(), viewport.endSample)
    val centerSample = (visibleStartSample + visibleEndSample) / 2f
    val edgeGlow = activeEdgeGlow(centerSample, viewport, segment.sampleCount)
    val isActive = centerSample <= viewport.playheadSample
    val barHeight = if (isActive) activeBarHeight else inactiveBarHeight
    val color =
        flashSegmentColor(
            isActive = isActive,
            activeColor = activeToneColor,
            inactiveColor = inactiveToneColor,
            confidence = 1f,
            glowPulse = glowPulse,
            edgeGlow = edgeGlow,
            alphaMultiplier = viewportEdgeFadeMultiplier(centerSample, viewport, edgeFadeSamples),
        )
    // Draw Pitch as real segmented bars. Round line caps visually bridge short
    // gaps, so the bar geometry owns the cut between adjacent low/high symbols.
    drawRoundRect(
        color = color,
        topLeft = Offset(drawStartX, y - barHeight / 2f),
        size = Size(visualWidth, barHeight),
        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
    )
}

private fun sampleToViewportX(
    sample: Float,
    viewport: FlashSignalViewport,
    leftPadding: Float,
    innerWidth: Float,
): Float =
    leftPadding +
        ((sample - viewport.startSample) / viewport.sampleCount) * innerWidth

internal fun FlashSignalToneSegment.overlaps(viewport: FlashSignalViewport): Boolean =
    endSample.toFloat() >= viewport.startSample &&
        startSample.toFloat() <= viewport.endSample

private fun flashSegmentColor(
    isActive: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    confidence: Float,
    glowPulse: Float,
    edgeGlow: Float = 0f,
    alphaMultiplier: Float = 1f,
): Color =
    if (isActive) {
        activeColor.copy(alpha = ((0.58f + 0.20f * glowPulse + 0.20f * edgeGlow) * alphaMultiplier).coerceIn(0f, 1f))
    } else {
        inactiveColor.copy(
            alpha =
                ((0.26f + 0.18f * confidence.coerceIn(0f, 1f) + 0.16f * edgeGlow) * alphaMultiplier)
                    .coerceIn(0f, 0.76f),
        )
    }

private fun edgeFadeSampleCount(
    segments: List<FlashSignalToneSegment>,
    viewport: FlashSignalViewport,
): Float {
    var visibleCount = 0
    var sampleTotal = 0f
    segments.forEach { segment ->
        if (segment.overlaps(viewport)) {
            visibleCount += 1
            sampleTotal += segment.sampleCount.coerceAtLeast(1)
        }
    }
    if (visibleCount <= 0) {
        return 0f
    }
    // Fade roughly two visible bits at each viewport edge. This only changes
    // visual emphasis; sample positions and segment widths remain exact.
    return (sampleTotal / visibleCount.toFloat()) * 2f
}

private fun viewportEdgeFadeMultiplier(
    sample: Float,
    viewport: FlashSignalViewport,
    edgeFadeSamples: Float,
): Float {
    if (edgeFadeSamples <= 0f) {
        return 1f
    }
    val distanceToEdge = minOf(sample - viewport.startSample, viewport.endSample - sample)
    val progress = (distanceToEdge / edgeFadeSamples).coerceIn(0f, 1f)
    return FlashViewportEdgeMinAlpha + (1f - FlashViewportEdgeMinAlpha) * progress
}

private fun activeEdgeGlow(
    sample: Float,
    viewport: FlashSignalViewport,
    segmentSampleCount: Int,
): Float {
    val glowWindow = maxOf(segmentSampleCount.toFloat() * 0.42f, viewport.sampleCount * 0.018f)
    return (1f - kotlin.math.abs(sample - viewport.playheadSample) / glowWindow)
        .coerceIn(0f, 1f)
}

private fun deterministicSignalTexture(
    segment: FlashSignalToneSegment,
    index: Int,
): Float {
    val seed =
        segment.startSample * 31 +
            segment.endSample * 17 +
            index * 13 +
            if (segment.tone == FskDominantTone.High) 7 else 3
    val folded = ((seed xor (seed ushr 7)) and 0xF).toFloat() / 15f
    return 0.58f + 0.42f * folded
}

private fun bitSegmentEnvelope(
    index: Int,
    count: Int,
): Float =
    if (count <= 2) {
        if (index == 0) 1.12f else 0.96f
    } else {
        when (index) {
            0, 2 -> 1.18f
            1 -> 0.54f
            else -> 0.84f
        }
    }

private fun FskEnergyBucket.heldToneStrength(
    buckets: List<FskEnergyBucket>,
    index: Int,
    tone: FskDominantTone,
): Float {
    val ownStrength = strengthForTone(tone)
    var heldStrength = ownStrength
    for (offset in FlashToneHoldDecayByBucketOffset.indices) {
        val previousBucket = buckets.getOrNull(index - offset - 1) ?: break
        if (previousBucket.dominantTone == tone) {
            heldStrength =
                maxOf(
                    heldStrength,
                    previousBucket.strengthForTone(tone) * FlashToneHoldDecayByBucketOffset[offset],
                )
        }
    }
    return heldStrength.coerceIn(0f, 1f)
}

private fun FskEnergyBucket.strengthForTone(tone: FskDominantTone): Float =
    when (tone) {
        FskDominantTone.High -> highStrength
        FskDominantTone.Low -> lowStrength
        FskDominantTone.Unknown -> 0f
    }

private val FlashToneHoldDecayByBucketOffset = floatArrayOf(0.52f, 0.26f)
private const val FlashToneHoldAlphaBoost = 0.18f
private const val FlashViewportEdgeMinAlpha = 0.52f
private const val FlashSegmentMicroBarWidthDp = 2
private const val FlashSegmentPulseSpacingDp = 8
private const val FlashBitSegmentGapDp = 1.6f
private const val FlashPitchSegmentGapDp = 3
private const val FlashPitchSegmentMinVisibleWidthDp = 2

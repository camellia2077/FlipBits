package com.bag.audioandroid.ui.screen

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

internal fun DrawScope.drawToneTracks(
    buckets: List<FskEnergyBucket>,
    scanHeadBucketIndex: Int,
    activeWindowBucketCount: Int,
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
        val isActiveBucket = index <= scanHeadBucketIndex
        val x = leftPadding + bucketWidth * index.toFloat()
        val contentWidth = (bucketWidth - 1.dp.toPx()).coerceAtLeast(1.6f)
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
        val highHeight = laneHeight * (0.24f + 0.66f * bucket.highStrength)
        val lowHeight = laneHeight * (0.24f + 0.66f * bucket.lowStrength)
        val highColor =
            toneBucketColor(
                isActive = isActiveBucket && bucket.dominantTone == FskDominantTone.High,
                activeColor = activeToneColor,
                inactiveColor = inactiveToneColor,
                alpha = highAlpha,
                strength = bucket.highStrength,
            )
        val lowColor =
            toneBucketColor(
                isActive = isActiveBucket && bucket.dominantTone == FskDominantTone.Low,
                activeColor = activeToneColor,
                inactiveColor = inactiveToneColor,
                alpha = lowAlpha,
                strength = bucket.lowStrength,
            )

        drawRoundRect(
            color = highColor,
            topLeft = Offset(x, upperTop + (laneHeight - highHeight) / 2f),
            size = Size(contentWidth, highHeight),
            cornerRadius = CornerRadius(contentWidth / 2f, contentWidth / 2f),
        )
        drawRoundRect(
            color = lowColor,
            topLeft = Offset(x, lowerTop + (laneHeight - lowHeight) / 2f),
            size = Size(contentWidth, lowHeight),
            cornerRadius = CornerRadius(contentWidth / 2f, contentWidth / 2f),
        )
    }
}

internal fun DrawScope.drawToneEnergy(
    buckets: List<FskEnergyBucket>,
    scanHeadBucketIndex: Int,
    activeWindowBucketCount: Int,
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
    val upperGap = 3.dp.toPx()
    val lowerGap = 3.dp.toPx()
    val maxEnergyHeight = innerHeight * 0.42f
    val strokeWidth = (bucketWidth * 0.58f).coerceAtLeast(1.8f)

    drawLine(
        color = centerLineColor,
        start = Offset(leftPadding, centerY),
        end = Offset(leftPadding + innerWidth, centerY),
        strokeWidth = 1.dp.toPx(),
    )
    buckets.forEachIndexed { index, bucket ->
        val isActiveBucket = index <= scanHeadBucketIndex
        val x = leftPadding + bucketWidth * index.toFloat() + bucketWidth / 2f
        val highHeight = maxEnergyHeight * bucket.highStrength
        val lowHeight = maxEnergyHeight * bucket.lowStrength
        val highAlpha =
            if (bucket.dominantTone == FskDominantTone.High) {
                0.42f + 0.42f * glowPulse + bucket.confidence * 0.12f
            } else {
                0.18f + bucket.highStrength * 0.24f
            }
        val lowAlpha =
            if (bucket.dominantTone == FskDominantTone.Low) {
                0.42f + 0.42f * glowPulse + bucket.confidence * 0.12f
            } else {
                0.18f + bucket.lowStrength * 0.24f
            }
        val highColor =
            toneBucketColor(
                isActive = isActiveBucket && bucket.dominantTone == FskDominantTone.High,
                activeColor = activeToneColor,
                inactiveColor = inactiveToneColor,
                alpha = highAlpha,
                strength = bucket.highStrength,
            )
        val lowColor =
            toneBucketColor(
                isActive = isActiveBucket && bucket.dominantTone == FskDominantTone.Low,
                activeColor = activeToneColor,
                inactiveColor = inactiveToneColor,
                alpha = lowAlpha,
                strength = bucket.lowStrength,
            )

        drawLine(
            color = highColor,
            start = Offset(x, centerY - upperGap),
            end = Offset(x, centerY - upperGap - highHeight),
            strokeWidth = strokeWidth,
        )
        drawLine(
            color = lowColor,
            start = Offset(x, centerY + lowerGap),
            end = Offset(x, centerY + lowerGap + lowHeight),
            strokeWidth = strokeWidth,
        )
    }
}

internal fun DrawScope.drawPitchLadder(
    buckets: List<FskEnergyBucket>,
    scanHeadBucketIndex: Int,
    activeWindowBucketCount: Int,
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
        color = centerLineColor.copy(alpha = 0.48f),
        start = Offset(leftPadding, highY),
        end = Offset(leftPadding + innerWidth, highY),
        strokeWidth = 1.dp.toPx(),
    )
    drawLine(
        color = centerLineColor.copy(alpha = 0.48f),
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
            if (index <= scanHeadBucketIndex) {
                pastSegmentOpen = false
            } else {
                futureSegmentOpen = false
            }
            return@forEachIndexed
        }

        val segmentStartX = leftPadding + bucketWidth * index.toFloat()
        val segmentEndX = segmentStartX + bucketWidth
        val isPastBucket = index <= scanHeadBucketIndex
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
        val isActiveBucket = index <= scanHeadBucketIndex
        val centerX = leftPadding + bucketWidth * (index.toFloat() + 0.5f)
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

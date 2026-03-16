package com.bag.audioandroid.ui.screen

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import com.bag.audioandroid.domain.AudioVisualizationRegion
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

internal data class WavePointModel(
    val x: Float,
    val y: Float,
    val region: AudioVisualizationRegion
)

internal data class WavePathSegment(
    val region: AudioVisualizationRegion,
    val path: Path
)

internal fun buildWavePoints(
    slices: List<WaveSliceModel>,
    sampleRateHz: Int,
    leftPadding: Float,
    innerWidth: Float,
    centerY: Float,
    maxAmplitudePx: Float,
    motionPhase: Float
): List<WavePointModel> {
    if (slices.isEmpty()) {
        return emptyList()
    }

    val safeSampleRate = sampleRateHz.coerceAtLeast(1)
    val pointCount = (slices.size * VisualizationPointsPerSlice)
        .coerceIn(VisualizationMinWavePointCount, VisualizationMaxWavePointCount)
    val points = ArrayList<WavePointModel>(pointCount)

    for (index in 0 until pointCount) {
        val positionRatio = if (pointCount == 1) 0f else index.toFloat() / (pointCount - 1).toFloat()
        val slicePosition = positionRatio * slices.lastIndex.coerceAtLeast(0).toFloat()
        val lowerIndex = floor(slicePosition.toDouble()).toInt().coerceIn(0, slices.lastIndex)
        val upperIndex = min(lowerIndex + 1, slices.lastIndex)
        val localT = (slicePosition - lowerIndex.toFloat()).coerceIn(0f, 1f)
        val lowerSlice = slices[lowerIndex]
        val upperSlice = slices[upperIndex]

        val peak = lerp(lowerSlice.peak, upperSlice.peak, localT)
        val rms = lerp(lowerSlice.rms, upperSlice.rms, localT)
        val brightness = lerp(lowerSlice.brightness, upperSlice.brightness, localT)
        val coverage = lerp(lowerSlice.coverage, upperSlice.coverage, localT)
        val samplePosition = lerp(
            lowerSlice.startSample.toFloat(),
            upperSlice.endSample.toFloat(),
            localT
        )
        val seconds = samplePosition / safeSampleRate.toFloat()

        val macroAmplitude = (0.08f + rms * 0.54f + peak * 0.24f).coerceIn(0f, 1f) * coverage
        val detailIntensity = (0.08f + brightness * 0.28f + peak * 0.08f).coerceIn(0f, 0.44f)
        val sharpness = (0.06f + peak * 0.24f + brightness * 0.28f).coerceIn(0f, 0.60f)
        val primaryFrequency = 1.2f + brightness * 3.2f + peak * 0.7f
        val detailFrequency = 3.6f + brightness * 9.0f
        val tertiaryFrequency = 0.8f + rms * 1.6f

        val primaryOscillation = sinF(VisualizationTwoPi * (seconds * primaryFrequency + motionPhase * 0.72f))
        val detailOscillation = sinF(
            VisualizationTwoPi * (seconds * detailFrequency - motionPhase * 1.18f + peak * 0.12f)
        )
        val breathingOscillation = sinF(VisualizationTwoPi * (seconds * tertiaryFrequency + motionPhase * 0.18f))
        val compositeWave = primaryOscillation * (0.88f - detailIntensity * 0.34f) +
            detailOscillation * detailIntensity +
            breathingOscillation * (0.04f + rms * 0.05f)
        val shapedWave = shapeWave(compositeWave.coerceIn(-1.2f, 1.2f), sharpness)
        val amplitudePx = maxAmplitudePx * macroAmplitude.coerceAtLeast(0f)
        val region = if (localT < 0.5f) lowerSlice.region else upperSlice.region

        points += WavePointModel(
            x = leftPadding + innerWidth * positionRatio,
            y = centerY - amplitudePx * shapedWave,
            region = region
        )
    }
    return points
}

internal fun buildWaveSegments(points: List<WavePointModel>): List<WavePathSegment> {
    if (points.size < 2) {
        return emptyList()
    }

    val segments = mutableListOf<WavePathSegment>()
    var currentRegion = points.first().region
    val currentPoints = mutableListOf(points.first())

    for (index in 1 until points.size) {
        val previousPoint = points[index - 1]
        val point = points[index]
        if (point.region != currentRegion) {
            currentPoints += point
            segments += WavePathSegment(
                region = currentRegion,
                path = buildSmoothPath(currentPoints)
            )
            currentRegion = point.region
            currentPoints.clear()
            currentPoints += previousPoint
            currentPoints += point
        } else {
            currentPoints += point
        }
    }

    if (currentPoints.size >= 2) {
        segments += WavePathSegment(
            region = currentRegion,
            path = buildSmoothPath(currentPoints)
        )
    }
    return segments
}

internal fun buildSmoothPath(points: List<WavePointModel>): Path {
    val path = Path()
    if (points.isEmpty()) {
        return path
    }

    path.moveTo(points.first().x, points.first().y)
    if (points.size == 1) {
        return path
    }
    if (points.size == 2) {
        path.lineTo(points.last().x, points.last().y)
        return path
    }

    for (index in 1 until points.lastIndex) {
        val current = points[index]
        val next = points[index + 1]
        val midpointX = (current.x + next.x) / 2f
        val midpointY = (current.y + next.y) / 2f
        path.quadraticTo(current.x, current.y, midpointX, midpointY)
    }
    path.lineTo(points.last().x, points.last().y)
    return path
}

internal fun buildFocusPath(
    points: List<WavePointModel>,
    playheadX: Float,
    spanWidth: Float
): Path? {
    val halfSpan = spanWidth / 2f
    val focusPoints = points.filter { point ->
        point.x in (playheadX - halfSpan)..(playheadX + halfSpan)
    }
    return if (focusPoints.size >= 2) {
        buildSmoothPath(focusPoints)
    } else {
        null
    }
}

internal fun pointNearestToX(
    points: List<WavePointModel>,
    targetX: Float
): Offset {
    val nearestPoint = points.minByOrNull { point -> abs(point.x - targetX) } ?: points.first()
    return Offset(nearestPoint.x, nearestPoint.y)
}

internal fun lerp(start: Float, end: Float, fraction: Float): Float =
    start + (end - start) * fraction

private fun shapeWave(value: Float, sharpness: Float): Float {
    val magnitude = abs(value).coerceAtMost(1f)
    val exponent = (1f - sharpness.coerceIn(0f, 0.85f)).coerceAtLeast(0.2f)
    return sign(value) * magnitude.pow(exponent)
}

private fun sinF(value: Float): Float =
    sin(value.toDouble()).toFloat()

internal const val VisualizationFocusSpanRatio = 0.18f

private const val VisualizationPointsPerSlice = 5
private const val VisualizationMinWavePointCount = 160
private const val VisualizationMaxWavePointCount = 260
private const val VisualizationTwoPi = (PI * 2.0).toFloat()

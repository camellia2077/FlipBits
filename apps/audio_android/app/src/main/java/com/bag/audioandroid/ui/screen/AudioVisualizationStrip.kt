package com.bag.audioandroid.ui.screen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.domain.AudioVisualizationFrame
import com.bag.audioandroid.domain.AudioVisualizationRegion
import com.bag.audioandroid.domain.AudioVisualizationTrack
import kotlin.math.ceil

@Composable
internal fun AudioVisualizationStrip(
    track: AudioVisualizationTrack,
    currentFrame: AudioVisualizationFrame?,
    displayedSamples: Int,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val totalSamples = track.totalSamples.coerceAtLeast(1)
    val visualTransition = rememberInfiniteTransition(label = "visualizationOscilloscope")
    val motionPhaseAnimated by visualTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "visualizationMotionPhase"
    )
    val glowPulseAnimated by visualTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "visualizationGlowPulse"
    )

    val clampedDisplayedSamples = displayedSamples.coerceIn(0, totalSamples)
    val animatedDisplayedSamples by animateFloatAsState(
        targetValue = clampedDisplayedSamples.toFloat(),
        animationSpec = tween(durationMillis = if (isPlaying) 120 else 0, easing = FastOutSlowInEasing),
        label = "visualizationDisplayedSamples"
    )
    val motionPhase = if (isPlaying) motionPhaseAnimated else 0.18f
    val glowPulse = if (isPlaying) glowPulseAnimated else 0.84f
    val focusGlowBoost = when (currentFrame?.region ?: AudioVisualizationRegion.Unknown) {
        AudioVisualizationRegion.LeadingShell -> 1.08f
        AudioVisualizationRegion.TrailingShell -> 1.08f
        AudioVisualizationRegion.Payload -> 1.0f
        AudioVisualizationRegion.Unknown -> 0.94f
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val targetSliceCount = remember(widthPx) {
            val targetSpacingPx = with(density) { 10.dp.toPx() }
            ceil((widthPx / targetSpacingPx).toDouble())
                .toInt()
                .coerceIn(VisualizationMinSliceCount, VisualizationMaxSliceCount)
        }
        val windowSampleCount = remember(track.sampleRateHz) {
            track.sampleRateHz.coerceAtLeast(1)
        }
        val slices = remember(track, targetSliceCount, windowSampleCount, animatedDisplayedSamples) {
            buildNearFieldSlices(
                track = track,
                currentSample = animatedDisplayedSamples,
                windowSampleCount = windowSampleCount,
                targetSliceCount = targetSliceCount
            )
        }

        val playheadColor = MaterialTheme.colorScheme.onPrimaryContainer
        val regionPalette = VisualizationRegionPalette(
            payloadColor = MaterialTheme.colorScheme.primary,
            leadingShellColor = MaterialTheme.colorScheme.secondary,
            trailingShellColor = MaterialTheme.colorScheme.tertiary,
            unknownColor = MaterialTheme.colorScheme.outline
        )
        val baseBackground = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f)
        val centerLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
        val surfaceTintColor = MaterialTheme.colorScheme.surfaceTint
        val ambientBrush = Brush.horizontalGradient(
            colors = listOf(
                regionPalette.leadingShellColor.copy(alpha = 0.11f),
                regionPalette.payloadColor.copy(alpha = 0.15f),
                regionPalette.trailingShellColor.copy(alpha = 0.11f)
            )
        )
        val windowStart = animatedDisplayedSamples - windowSampleCount * VisualizationPlayheadAnchorRatio

        Canvas(modifier = Modifier.fillMaxWidth().height(76.dp)) {
            if (slices.isEmpty()) {
                return@Canvas
            }

            val corner = CornerRadius(24f, 24f)
            val leftPadding = 12.dp.toPx()
            val rightPadding = 12.dp.toPx()
            val topPadding = 10.dp.toPx()
            val bottomPadding = 10.dp.toPx()
            val innerWidth = (size.width - leftPadding - rightPadding).coerceAtLeast(1f)
            val innerHeight = (size.height - topPadding - bottomPadding).coerceAtLeast(1f)
            val centerY = topPadding + innerHeight / 2f
            val maxAmplitudePx = innerHeight * 0.44f
            val playheadX = leftPadding + innerWidth * VisualizationPlayheadAnchorRatio

            val points = buildWavePoints(
                slices = slices,
                sampleRateHz = track.sampleRateHz.coerceAtLeast(1),
                leftPadding = leftPadding,
                innerWidth = innerWidth,
                centerY = centerY,
                maxAmplitudePx = maxAmplitudePx,
                motionPhase = motionPhase
            )
            if (points.size < 2) {
                return@Canvas
            }

            val ghostPoints = points.map { point ->
                point.copy(y = lerp(centerY, point.y, 0.82f) + 3.dp.toPx())
            }
            val waveSegments = buildWaveSegments(points)
            val ghostSegments = buildWaveSegments(ghostPoints)
            val baseTracePath = buildSmoothPath(points)
            val playheadPoint = pointNearestToX(points, playheadX)

            drawRoundRect(
                color = baseBackground,
                size = size,
                cornerRadius = corner
            )
            drawRoundRect(
                brush = ambientBrush,
                size = size,
                cornerRadius = corner
            )
            drawWindowRegionWash(
                slices = slices,
                windowStart = windowStart,
                windowSampleCount = windowSampleCount.toFloat(),
                leftPadding = leftPadding,
                innerWidth = innerWidth,
                topPadding = topPadding,
                innerHeight = innerHeight,
                regionPalette = regionPalette
            )

            drawLine(
                color = centerLineColor,
                start = Offset(leftPadding, centerY),
                end = Offset(size.width - rightPadding, centerY),
                strokeWidth = 1.dp.toPx()
            )
            drawPath(
                path = baseTracePath,
                color = surfaceTintColor.copy(alpha = 0.18f),
                style = Stroke(
                    width = 6.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            ghostSegments.forEach { segment ->
                drawPath(
                    path = segment.path,
                    color = regionPalette.colorFor(segment.region).copy(alpha = 0.16f + 0.06f * glowPulse),
                    style = Stroke(
                        width = 7.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            waveSegments.forEach { segment ->
                val segmentColor = regionPalette.colorFor(segment.region)
                drawPath(
                    path = segment.path,
                    color = segmentColor.copy(alpha = 0.22f + 0.10f * glowPulse),
                    style = Stroke(
                        width = 4.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
                drawPath(
                    path = segment.path,
                    color = segmentColor.copy(alpha = 0.92f),
                    style = Stroke(
                        width = 2.2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            buildFocusPath(points, playheadX, innerWidth * VisualizationFocusSpanRatio)?.let { focusPath ->
                drawPath(
                    path = focusPath,
                    color = playheadColor.copy(alpha = (0.30f + 0.18f * glowPulse) * focusGlowBoost),
                    style = Stroke(
                        width = 3.6.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            drawLine(
                color = playheadColor.copy(alpha = if (isPlaying) 0.80f else 0.56f),
                start = Offset(playheadX, topPadding),
                end = Offset(playheadX, size.height - bottomPadding),
                strokeWidth = 2.dp.toPx()
            )
            drawCircle(
                color = playheadColor.copy(alpha = 0.12f * glowPulse * focusGlowBoost),
                radius = 12.dp.toPx(),
                center = playheadPoint
            )
            drawCircle(
                color = playheadColor.copy(alpha = 0.24f * glowPulse * focusGlowBoost),
                radius = 6.dp.toPx(),
                center = playheadPoint
            )
            drawCircle(
                color = playheadColor.copy(alpha = 0.96f),
                radius = 2.6.dp.toPx(),
                center = playheadPoint
            )
            drawCircle(
                color = playheadColor.copy(alpha = (0.52f + glowPulse * 0.18f).coerceAtMost(0.82f)),
                radius = 1.2.dp.toPx(),
                center = Offset(playheadX, centerY)
            )
        }
    }
}

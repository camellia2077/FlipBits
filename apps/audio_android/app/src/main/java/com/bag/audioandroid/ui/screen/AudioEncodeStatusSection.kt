package com.bag.audioandroid.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioEncodePhase
import com.bag.audioandroid.ui.theme.AudioEncodeGlyphColors
import com.bag.audioandroid.ui.theme.audioEncodeGlyphColors
import kotlin.math.floor

@Composable
internal fun AudioEncodeStatusSection(
    encodeProgress: Float?,
    encodePhase: AudioEncodePhase?,
    isEncodingBusy: Boolean,
    modifier: Modifier = Modifier,
) {
    val glyphColors = audioEncodeGlyphColors()
    val clampedProgress = (encodeProgress ?: 0f).coerceIn(0f, 1f)
    var isGlyphExpanded by rememberSaveable { mutableStateOf(false) }
    val glyphScale by animateFloatAsState(
        targetValue = if (isGlyphExpanded) ExpandedGlyphScale else 1f,
        animationSpec =
            spring(
                dampingRatio = 0.82f,
                stiffness = 420f,
            ),
        label = "audioEncodeGlyphScale",
    )
    val busyElapsedMillis by produceState(
        initialValue = 0L,
        key1 = isEncodingBusy,
    ) {
        if (!isEncodingBusy) {
            value = 0L
            return@produceState
        }

        val startedAtNanos = withFrameNanos { it }
        while (true) {
            value =
                withFrameNanos { frameTimeNanos ->
                    ((frameTimeNanos - startedAtNanos) / 1_000_000L).coerceAtLeast(0L)
                }
        }
    }
    val outerRotation =
        if (isEncodingBusy) {
            ((busyElapsedMillis % OuterRotationDurationMs).toFloat() / OuterRotationDurationMs) * 360f
        } else {
            0f
        }
    // Start the outer gear first, then let the inner gear catch up later so the glyph
    // reads like a heavy mechanism waking up instead of two spinners starting in sync.
    val innerRotation =
        if (isEncodingBusy && busyElapsedMillis > InnerRotationDelayMs) {
            -(
                ((busyElapsedMillis - InnerRotationDelayMs) % InnerRotationDurationMs).toFloat() /
                    InnerRotationDurationMs
            ) * 360f
        } else {
            0f
        }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            AudioEncodeGlyph(
                modifier =
                    Modifier.clickable {
                        isGlyphExpanded = !isGlyphExpanded
                    },
                outerRotation = if (isEncodingBusy) outerRotation else 0f,
                innerRotation = if (isEncodingBusy) innerRotation else 0f,
                encodeProgress = clampedProgress,
                isEncodingBusy = isEncodingBusy,
                glyphScale = glyphScale,
                glyphColors = glyphColors,
            )
        }
        if (isEncodingBusy) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                encodePhase?.let { phase ->
                    Text(
                        text = stringResource(phase.labelResId),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LinearProgressIndicator(
                    progress = { clampedProgress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun AudioEncodeGlyph(
    outerRotation: Float,
    innerRotation: Float,
    encodeProgress: Float,
    isEncodingBusy: Boolean,
    glyphScale: Float,
    glyphColors: AudioEncodeGlyphColors,
    modifier: Modifier = Modifier,
) {
    val outerGearPath =
        remember {
            PathParser().parsePathString(OuterGearPathData).toPath()
        }
    val innerGearPath =
        remember {
            PathParser().parsePathString(InnerGearPathData).toPath()
        }

    Canvas(
        modifier = modifier.size((108.dp * glyphScale)),
    ) {
        val iconScale = size.minDimension / IconViewportSize
        val iconWidth = IconViewportSize * iconScale
        val iconHeight = IconViewportSize * iconScale
        val left = (size.width - iconWidth) / 2f
        val top = (size.height - iconHeight) / 2f

        translate(left = left, top = top) {
            scale(scale = iconScale, pivot = Offset.Zero) {
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
        }
    }
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

private val AudioEncodePhase.labelResId: Int
    get() =
        when (this) {
            AudioEncodePhase.PreparingInput -> R.string.audio_encode_phase_preparing_input_label
            AudioEncodePhase.RenderingPcm -> R.string.audio_encode_phase_rendering_pcm_label
            AudioEncodePhase.Postprocessing -> R.string.audio_encode_phase_postprocessing_label
            AudioEncodePhase.Finalizing -> R.string.audio_encode_phase_finalizing_label
        }

private const val IconViewportSize = 256f
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
private const val OuterRotationDurationMs = 9800L
private const val InnerRotationDurationMs = 8200L
private const val InnerRotationDelayMs = 900L
private const val ExpandedGlyphScale = 1.75f

private val GlyphCenter = Offset(128f, 128f)
private const val OuterGearPathData =
    "M 242.02 112.99 L 242.02 143.01 L 236.33 147.10 A 110 110 0 0 1 231.37 165.62 " +
        "L 234.25 172.01 L 219.24 198.01 L 212.26 198.71 A 110 110 0 0 1 198.71 212.26 " +
        "L 198.01 219.24 L 172.01 234.25 L 165.62 231.37 A 110 110 0 0 1 147.10 236.33 " +
        "L 143.01 242.02 L 112.99 242.02 L 108.90 236.33 A 110 110 0 0 1 90.38 231.37 " +
        "L 83.99 234.25 L 57.99 219.24 L 57.29 212.26 A 110 110 0 0 1 43.74 198.71 " +
        "L 36.76 198.01 L 21.75 172.01 L 24.63 165.62 A 110 110 0 0 1 19.67 147.10 " +
        "L 13.98 143.01 L 13.98 112.99 L 19.67 108.90 A 110 110 0 0 1 24.63 90.38 " +
        "L 21.75 83.99 L 36.76 57.99 L 43.74 57.29 A 110 110 0 0 1 57.29 43.74 " +
        "L 57.99 36.76 L 83.99 21.75 L 90.38 24.63 A 110 110 0 0 1 108.90 19.67 " +
        "L 112.99 13.98 L 143.01 13.98 L 147.10 19.67 A 110 110 0 0 1 165.62 24.63 " +
        "L 172.01 21.75 L 198.01 36.76 L 198.71 43.74 A 110 110 0 0 1 212.26 57.29 " +
        "L 219.24 57.99 L 234.25 83.99 L 231.37 90.38 A 110 110 0 0 1 236.33 108.90 Z"

private const val InnerGearPathData =
    "M 198.13 114.05 L 198.13 141.95 L 192.56 145.30 A 66.84 66.84 0 0 1 185.88 161.42 " +
        "L 187.45 167.72 L 167.72 187.45 L 161.42 185.88 A 66.84 66.84 0 0 1 145.30 192.56 " +
        "L 141.95 198.13 L 114.05 198.13 L 110.70 192.56 A 66.84 66.84 0 0 1 94.58 185.88 " +
        "L 88.28 187.45 L 68.55 167.72 L 70.12 161.42 A 66.84 66.84 0 0 1 63.44 145.30 " +
        "L 57.87 141.95 L 57.87 114.05 L 63.44 110.70 A 66.84 66.84 0 0 1 70.12 94.58 " +
        "L 68.55 88.28 L 88.28 68.55 L 94.58 70.12 A 66.84 66.84 0 0 1 110.70 63.44 " +
        "L 114.05 57.87 L 141.95 57.87 L 145.30 63.44 A 66.84 66.84 0 0 1 161.42 70.12 " +
        "L 167.72 68.55 L 187.45 88.28 L 185.88 94.58 A 66.84 66.84 0 0 1 192.56 110.70 Z"

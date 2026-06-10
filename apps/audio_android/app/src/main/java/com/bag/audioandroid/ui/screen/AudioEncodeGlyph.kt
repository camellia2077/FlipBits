package com.bag.audioandroid.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.ui.theme.AudioEncodeGlyphColors
import com.bag.audioandroid.ui.theme.audioEncodeGlyphColors

@Composable
internal fun AudioEncodeGlyph(
    encodeProgress: Float,
    isEncodingBusy: Boolean,
    modifier: Modifier = Modifier,
    baseSize: androidx.compose.ui.unit.Dp = 108.dp,
    showCropGuide: Boolean = false,
    cropGuideColor: Color = Color.Unspecified,
    cropGuideBackgroundColor: Color = Color.Unspecified,
    showIdleCoreRing: Boolean = true,
    glyphColorsOverride: AudioEncodeGlyphColors? = null,
) {
    val glyphColors = glyphColorsOverride ?: audioEncodeGlyphColors()
    val normalizedProgress = encodeProgress.coerceIn(0f, 1f)
    val isInnerRotationActive = isEncodingBusy && normalizedProgress >= InnerRotationStartProgressThreshold
    val isOuterRotationActive = isEncodingBusy && normalizedProgress >= OuterRotationStartProgressThreshold
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
    val innerRotationElapsedMillis by produceState(
        initialValue = 0L,
        key1 = isEncodingBusy,
        key2 = isInnerRotationActive,
    ) {
        if (!isEncodingBusy || !isInnerRotationActive) {
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
    val outerRotationElapsedMillis by produceState(
        initialValue = 0L,
        key1 = isEncodingBusy,
        key2 = isOuterRotationActive,
    ) {
        if (!isEncodingBusy || !isOuterRotationActive) {
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
        if (isOuterRotationActive) {
            (
                (outerRotationElapsedMillis % OuterRotationDurationMs).toFloat() /
                    OuterRotationDurationMs
            ) * 360f
        } else {
            0f
        }
    val innerRotation =
        if (isInnerRotationActive) {
            -(
                (innerRotationElapsedMillis % InnerRotationDurationMs).toFloat() /
                    InnerRotationDurationMs
            ) * 360f
        } else {
            0f
        }
    val outerGearPath =
        remember {
            PathParser().parsePathString(OuterGearPathData).toPath()
        }
    val innerGearPath =
        remember {
            PathParser().parsePathString(InnerGearPathData).toPath()
        }

    Canvas(
        modifier =
            modifier
                .clickable { isGlyphExpanded = !isGlyphExpanded }
                .size((baseSize * glyphScale)),
    ) {
        if (showCropGuide) {
            val guideInset = size.minDimension * CropGuideInsetFraction
            val guideSide = size.minDimension - (guideInset * 2f)
            if (cropGuideBackgroundColor != Color.Unspecified) {
                drawRect(
                    color = cropGuideBackgroundColor,
                    topLeft = Offset(guideInset, guideInset),
                    size = Size(guideSide, guideSide),
                )
            }
        }

        if (showCropGuide && cropGuideColor != Color.Unspecified) {
            val guideInset = size.minDimension * CropGuideInsetFraction
            val guideSide = size.minDimension - (guideInset * 2f)
            drawRect(
                color = cropGuideColor,
                topLeft = Offset(guideInset, guideInset),
                size = Size(guideSide, guideSide),
                style = Stroke(width = CropGuideStrokeWidth.toPx()),
            )
        }

        val iconScale = size.minDimension / IconViewportSize
        val iconWidth = IconViewportSize * iconScale
        val iconHeight = IconViewportSize * iconScale
        val left = (size.width - iconWidth) / 2f
        val top = (size.height - iconHeight) / 2f

        translate(left = left, top = top) {
            scale(scale = iconScale, pivot = Offset.Zero) {
                drawAudioEncodeGlyph(
                    outerGearPath = outerGearPath,
                    innerGearPath = innerGearPath,
                    outerRotation = if (isEncodingBusy) outerRotation else 0f,
                    innerRotation = if (isEncodingBusy) innerRotation else 0f,
                    encodeProgress = encodeProgress,
                    isEncodingBusy = isEncodingBusy,
                    showIdleCoreRing = showIdleCoreRing,
                    glyphColors = glyphColors,
                )
            }
        }
    }
}

private const val OuterRotationDurationMs = 9800L
private const val InnerRotationDurationMs = 8200L
private const val ExpandedGlyphScale = 1.75f
private const val CropGuideInsetFraction = 0.01f
private val CropGuideStrokeWidth = 2.dp
private const val InnerRotationStartProgressThreshold = 1f / 8f
private const val OuterRotationStartProgressThreshold = 2f / 8f

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

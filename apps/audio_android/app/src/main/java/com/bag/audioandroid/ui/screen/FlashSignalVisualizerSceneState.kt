package com.bag.audioandroid.ui.screen

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.state.FlashVisualWindowState
import com.bag.audioandroid.ui.theme.AppThemeVisualTokens
import kotlin.math.ceil
import kotlin.math.roundToInt

// Controls only the Flash Visual viewport density. Playback progress and audio
// timeline semantics stay based on absolute samples.
private const val FlashSignalViewportSeconds = 0.80f
private const val FlashSignalMinBucketCount = 56
private const val FlashSignalMaxBucketCount = 124
private const val FlashSignalOutlineAlpha = 0.20f

internal data class FlashSignalVisualizerLayoutModel(
    val targetBucketCount: Int,
    val windowSampleCount: Int,
)

internal data class FlashSignalVisualStyle(
    val activeToneColor: Color,
    val inactiveToneColor: Color,
    val glowColor: Color,
    val baseBackground: Color,
    val centerLineColor: Color,
    val pulseGuideColor: Color,
    val referenceLabelColor: Color,
)

internal data class FlashSignalVisualizerSceneState(
    val layoutModel: FlashSignalVisualizerLayoutModel,
    val renderState: FlashSignalVisualizerRenderState,
    val visualStyle: FlashSignalVisualStyle,
)

@Composable
internal fun rememberFlashSignalVisualizerSceneState(
    density: Density,
    maxWidth: Dp,
    sampleRateHz: Int,
    input: FlashSignalVisualizationInput,
    isPlaying: Boolean,
    mode: FlashSignalVisualizationMode,
    flashVoicingStyle: FlashVoicingStyleOption?,
    flashVisualWindow: FlashVisualWindowState,
    sharedPlaybackSampleState: FlashVisualPlaybackSampleState?,
    playbackSpeed: Float,
    isScrubbing: Boolean,
    visualTokens: AppThemeVisualTokens,
    colorScheme: ColorScheme,
): FlashSignalVisualizerSceneState {
    val layoutModel =
        rememberFlashSignalVisualizerLayoutModel(
            density = density,
            maxWidth = maxWidth,
            sampleRateHz = sampleRateHz,
        )
    val renderState =
        rememberFlashSignalVisualizerRenderState(
            input = input,
            isPlaying = isPlaying,
            mode = mode,
            flashVoicingStyle = flashVoicingStyle,
            flashVisualWindow = flashVisualWindow,
            sharedPlaybackSampleState = sharedPlaybackSampleState,
            playbackSpeed = playbackSpeed,
            isScrubbing = isScrubbing,
            targetBucketCount = layoutModel.targetBucketCount,
            windowSampleCount = layoutModel.windowSampleCount,
        )
    val visualStyle =
        rememberFlashSignalVisualStyle(
            colorScheme = colorScheme,
            visualTokens = visualTokens,
        )
    return remember(layoutModel, renderState, visualStyle) {
        FlashSignalVisualizerSceneState(
            layoutModel = layoutModel,
            renderState = renderState,
            visualStyle = visualStyle,
        )
    }
}

@Composable
private fun rememberFlashSignalVisualizerLayoutModel(
    density: Density,
    maxWidth: Dp,
    sampleRateHz: Int,
): FlashSignalVisualizerLayoutModel {
    val widthPx = with(density) { maxWidth.toPx() }
    val targetBucketCount =
        remember(widthPx, density) {
            val bucketSpacingPx = with(density) { 6.dp.toPx() }
            ceil((widthPx / bucketSpacingPx).toDouble())
                .toInt()
                .coerceIn(FlashSignalMinBucketCount, FlashSignalMaxBucketCount)
        }
    val windowSampleCount =
        remember(sampleRateHz) {
            (sampleRateHz.coerceAtLeast(1) * FlashSignalViewportSeconds)
                .roundToInt()
                .coerceAtLeast(1)
        }
    return remember(targetBucketCount, windowSampleCount) {
        FlashSignalVisualizerLayoutModel(
            targetBucketCount = targetBucketCount,
            windowSampleCount = windowSampleCount,
        )
    }
}

@Composable
private fun rememberFlashSignalVisualStyle(
    colorScheme: ColorScheme,
    visualTokens: AppThemeVisualTokens,
): FlashSignalVisualStyle =
    remember(visualTokens, colorScheme) {
        FlashSignalVisualStyle(
            activeToneColor = colorScheme.primary,
            inactiveToneColor = visualTokens.visualizationInactiveToneColor,
            glowColor = colorScheme.onPrimaryContainer,
            baseBackground = visualTokens.visualizationBaseBackgroundColor,
            centerLineColor = flashSignalOutlineColor(visualTokens.subtleOutlineColor),
            pulseGuideColor = flashSignalOutlineColor(visualTokens.subtleOutlineColor),
            // low/high Hz reference labels sit on the mini player surface, so they should
            // contrast with that background instead of following primary/secondary slots.
            referenceLabelColor = colorScheme.onSurface,
        )
    }

private fun flashSignalOutlineColor(color: Color): Color = color.copy(alpha = FlashSignalOutlineAlpha)

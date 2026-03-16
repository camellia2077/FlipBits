package com.bag.audioandroid.ui.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.domain.AudioVisualizationFrame
import com.bag.audioandroid.domain.AudioVisualizationTrack
import kotlin.math.roundToInt

@Composable
internal fun AudioPlaybackProgressSection(
    displayedSamples: Int,
    totalSamples: Int,
    isScrubbing: Boolean,
    visualizationTrack: AudioVisualizationTrack?,
    currentVisualizationFrame: AudioVisualizationFrame?,
    displayedTime: String,
    totalTime: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onScrubStarted: () -> Unit,
    onScrubChanged: (Int) -> Unit,
    onScrubFinished: () -> Unit
) {
    val sliderUpperBound = totalSamples.coerceAtLeast(1)
    var userScrubbing by remember { mutableStateOf(false) }
    val clampedDisplayedSamples = displayedSamples.coerceIn(0, sliderUpperBound)
    val shouldAnimateSlider = isPlaying && !isScrubbing && !userScrubbing && totalSamples > 0
    val animatedSliderValue by animateFloatAsState(
        targetValue = clampedDisplayedSamples.toFloat(),
        animationSpec = if (shouldAnimateSlider) {
            tween(
                durationMillis = PlaybackProgressAnimationDurationMs,
                easing = LinearEasing
            )
        } else {
            snap()
        },
        label = "audioPlaybackProgress"
    )

    LaunchedEffect(isScrubbing) {
        if (!isScrubbing) {
            userScrubbing = false
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        visualizationTrack?.takeIf { it.frames.isNotEmpty() }?.let { track ->
            AudioVisualizationStrip(
                track = track,
                currentFrame = currentVisualizationFrame,
                displayedSamples = displayedSamples,
                isPlaying = isPlaying,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Slider(
            value = if (userScrubbing || isScrubbing) {
                clampedDisplayedSamples.toFloat()
            } else {
                animatedSliderValue
            },
            onValueChange = { rawValue ->
                if (totalSamples <= 0) {
                    return@Slider
                }
                if (!userScrubbing) {
                    onScrubStarted()
                    userScrubbing = true
                }
                onScrubChanged(rawValue.roundToInt())
            },
            onValueChangeFinished = {
                if (userScrubbing) {
                    onScrubFinished()
                }
                userScrubbing = false
            },
            enabled = totalSamples > 0,
            valueRange = 0f..sliderUpperBound.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayedTime,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = totalTime,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private const val PlaybackProgressAnimationDurationMs = 90

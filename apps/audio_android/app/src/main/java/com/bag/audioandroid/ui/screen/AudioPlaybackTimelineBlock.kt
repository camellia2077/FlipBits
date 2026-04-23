package com.bag.audioandroid.ui.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
internal fun AudioPlaybackTimelineBlock(
    displayedSamples: Int,
    totalSamples: Int,
    isScrubbing: Boolean,
    displayedTime: String,
    totalTime: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onScrubStarted: () -> Unit,
    onScrubChanged: (Int) -> Unit,
    onScrubFinished: () -> Unit,
) {
    val sliderUpperBound = totalSamples.coerceAtLeast(1)
    var userScrubbing by remember { mutableStateOf(false) }
    val clampedDisplayedSamples = displayedSamples.coerceIn(0, sliderUpperBound)
    val shouldAnimateSlider = isPlaying && !isScrubbing && !userScrubbing && totalSamples > 0
    val animatedSliderValue by animateFloatAsState(
        targetValue = clampedDisplayedSamples.toFloat(),
        animationSpec =
            if (shouldAnimateSlider) {
                tween(
                    durationMillis = PlaybackProgressAnimationDurationMs,
                    easing = LinearEasing,
                )
            } else {
                snap()
            },
        label = "audioPlaybackProgress",
    )

    LaunchedEffect(isScrubbing) {
        if (!isScrubbing) {
            userScrubbing = false
        }
    }

    PlaybackTimelineSection(
        displayedTime = displayedTime,
        totalTime = totalTime,
        totalSamples = totalSamples,
        sliderUpperBound = sliderUpperBound,
        isScrubbing = isScrubbing,
        userScrubbing = userScrubbing,
        clampedDisplayedSamples = clampedDisplayedSamples,
        animatedSliderValue = animatedSliderValue,
        onScrubStarted = {
            onScrubStarted()
            userScrubbing = true
        },
        onScrubChanged = onScrubChanged,
        onScrubFinished = {
            onScrubFinished()
            userScrubbing = false
        },
        onScrubCancelled = {
            userScrubbing = false
        },
        modifier = modifier,
    )
}

private const val PlaybackProgressAnimationDurationMs = 90

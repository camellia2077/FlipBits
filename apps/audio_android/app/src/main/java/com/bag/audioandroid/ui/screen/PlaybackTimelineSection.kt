package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.ui.playbackLyricsAccentTextColor
import com.bag.audioandroid.ui.theme.appThemeVisualTokens
import kotlin.math.roundToInt

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun PlaybackTimelineSection(
    displayedTime: String,
    totalTime: String,
    totalSamples: Int,
    sliderUpperBound: Int,
    isScrubbing: Boolean,
    userScrubbing: Boolean,
    clampedDisplayedSamples: Int,
    animatedSliderValue: Float,
    onScrubStarted: () -> Unit,
    onScrubChanged: (Int) -> Unit,
    onScrubFinished: () -> Unit,
    onScrubCancelled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lyricsAccentTextColor = playbackLyricsAccentTextColor()
    val visualTokens = appThemeVisualTokens()
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("playback-timeline-section"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = displayedTime,
                style = MaterialTheme.typography.bodySmall,
                color = lyricsAccentTextColor,
            )
            Text(
                text = totalTime,
                style = MaterialTheme.typography.bodySmall,
                color = lyricsAccentTextColor,
            )
        }
        Slider(
            value =
                if (userScrubbing || isScrubbing) {
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
                }
                onScrubChanged(rawValue.roundToInt())
            },
            onValueChangeFinished = {
                if (userScrubbing) {
                    onScrubFinished()
                } else {
                    onScrubCancelled()
                }
            },
            enabled = totalSamples > 0,
            valueRange = 0f..sliderUpperBound.toFloat(),
            interactionSource = interactionSource,
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    colors =
                        SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                        ),
                    enabled = totalSamples > 0,
                )
            },
            track = { sliderState ->
                // Material 3's default track leaves a thumb gap that shows the sheet/background
                // color through the active segment. Force a seamless single-color progress bar.
                SliderDefaults.Track(
                    sliderState = sliderState,
                    colors =
                        SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = visualTokens.timelineInactiveTrackColor,
                        ),
                    enabled = totalSamples > 0,
                    thumbTrackGapSize = 0.dp,
                    trackInsideCornerSize = 0.dp,
                )
            },
            colors =
                SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = visualTokens.timelineInactiveTrackColor,
                ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

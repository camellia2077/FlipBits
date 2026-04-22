package com.bag.audioandroid.ui.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.playerSegmentedButtonColors
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import kotlin.math.roundToInt

@Composable
internal fun AudioPlaybackProgressSection(
    displayedSamples: Int,
    totalSamples: Int,
    isScrubbing: Boolean,
    waveformPcm: ShortArray,
    sampleRateHz: Int,
    isFlashMode: Boolean,
    flashVoicingStyle: FlashVoicingStyleOption?,
    followData: PayloadFollowViewData,
    displayedTime: String,
    totalTime: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onScrubStarted: () -> Unit,
    onScrubChanged: (Int) -> Unit,
    onScrubFinished: () -> Unit,
    onLyricsRequested: () -> Unit = {},
    initialDisplayMode: PlaybackDisplayMode = PlaybackDisplayMode.Visual,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("playback-progress-section"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AudioPlaybackDisplayBlock(
            displayedSamples = displayedSamples,
            waveformPcm = waveformPcm,
            sampleRateHz = sampleRateHz,
            isFlashMode = isFlashMode,
            flashVoicingStyle = flashVoicingStyle,
            followData = followData,
            isPlaying = isPlaying,
            onLyricsRequested = onLyricsRequested,
            initialDisplayMode = initialDisplayMode,
        )
        AudioPlaybackTimelineBlock(
            displayedSamples = displayedSamples,
            totalSamples = totalSamples,
            isScrubbing = isScrubbing,
            displayedTime = displayedTime,
            totalTime = totalTime,
            isPlaying = isPlaying,
            onScrubStarted = onScrubStarted,
            onScrubChanged = onScrubChanged,
            onScrubFinished = onScrubFinished,
        )
    }
}

@Composable
internal fun AudioPlaybackDisplayBlock(
    displayedSamples: Int,
    waveformPcm: ShortArray,
    sampleRateHz: Int,
    isFlashMode: Boolean,
    flashVoicingStyle: FlashVoicingStyleOption?,
    followData: PayloadFollowViewData,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onLyricsRequested: () -> Unit = {},
    initialDisplayMode: PlaybackDisplayMode = PlaybackDisplayMode.Visual,
) {
    var playbackDisplayModeName by rememberSaveable { mutableStateOf(initialDisplayMode.name) }
    var flashVisualizationModeName by rememberSaveable(isFlashMode) {
        mutableStateOf(FlashSignalVisualizationMode.ToneTracks.name)
    }
    val playbackDisplayMode =
        remember(playbackDisplayModeName) {
            PlaybackDisplayMode.entries.firstOrNull { it.name == playbackDisplayModeName } ?: PlaybackDisplayMode.Visual
        }

    PlaybackDisplaySection(
        followData = followData,
        displayedSamples = displayedSamples,
        waveformPcm = waveformPcm,
        sampleRateHz = sampleRateHz,
        isFlashMode = isFlashMode,
        flashVoicingStyle = flashVoicingStyle,
        isPlaying = isPlaying,
        playbackDisplayMode = playbackDisplayMode,
        flashVisualizationModeName = flashVisualizationModeName,
        onDisplayModeSelected = { option ->
            playbackDisplayModeName = option.name
            if (option == PlaybackDisplayMode.Lyrics) {
                onLyricsRequested()
            }
        },
        onFlashVisualizationModeSelected = { selectedMode ->
            flashVisualizationModeName = selectedMode.name
        },
        modifier = modifier,
    )
}

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

@Composable
internal fun PlaybackDisplaySection(
    displayedSamples: Int,
    waveformPcm: ShortArray,
    sampleRateHz: Int,
    isFlashMode: Boolean,
    flashVoicingStyle: FlashVoicingStyleOption?,
    followData: PayloadFollowViewData,
    isPlaying: Boolean,
    playbackDisplayMode: PlaybackDisplayMode,
    flashVisualizationModeName: String,
    onDisplayModeSelected: (PlaybackDisplayMode) -> Unit,
    onFlashVisualizationModeSelected: (FlashSignalVisualizationMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("playback-display-section"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag("playback-display-switcher"),
        ) {
            PlaybackDisplayMode.entries.forEachIndexed { index, option ->
                val optionLabel = stringResource(option.titleResId)
                SegmentedButton(
                    selected = playbackDisplayMode == option,
                    onClick = { onDisplayModeSelected(option) },
                    modifier =
                        Modifier
                            .testTag("playback-display-${option.name.lowercase()}")
                            .semantics { contentDescription = optionLabel },
                    shape =
                        SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = PlaybackDisplayMode.entries.size,
                        ),
                    colors = playerSegmentedButtonColors(),
                    label = { Text(text = optionLabel) },
                )
            }
        }
        if (playbackDisplayMode == PlaybackDisplayMode.Visual) {
            if (waveformPcm.isNotEmpty()) {
                if (isFlashMode) {
                    val flashVisualizationMode =
                        FlashSignalVisualizationMode.values().firstOrNull { mode ->
                            mode.name == flashVisualizationModeName
                        } ?: FlashSignalVisualizationMode.ToneTracks
                    FlashSignalVisualizationModeSwitcher(
                        selectedMode = flashVisualizationMode,
                        onModeSelected = onFlashVisualizationModeSelected,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    AudioFlashSignalVisualizer(
                        pcm = waveformPcm,
                        sampleRateHz = sampleRateHz,
                        displayedSamples = displayedSamples,
                        isPlaying = isPlaying,
                        mode = flashVisualizationMode,
                        flashVoicingStyle = flashVoicingStyle,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    AudioPcmWaveform(
                        pcm = waveformPcm,
                        sampleRateHz = sampleRateHz,
                        displayedSamples = displayedSamples,
                        isPlaying = isPlaying,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else {
            // The playback area mirrors a music player: visual mode works like album art,
            // while lyrics mode hands off to the formal line-timeline lyric page.
            PlaybackDataFollowSection(
                followData = followData,
                displayedSamples = displayedSamples,
            )
        }
        PlaybackTokenContextTape(
            followData = followData,
            displayedSamples = displayedSamples,
        )
    }
}

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
            )
            Text(
                text = totalTime,
                style = MaterialTheme.typography.bodySmall,
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
                            inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer,
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
                    inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private const val PlaybackProgressAnimationDurationMs = 90

internal enum class PlaybackDisplayMode(
    val titleResId: Int,
) {
    Visual(R.string.audio_playback_view_visual),
    Lyrics(R.string.audio_playback_view_lyrics),
}

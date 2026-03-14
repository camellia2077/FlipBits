package com.bag.audioandroid.ui.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.PlaybackSequenceMode
import kotlin.math.roundToInt

@Composable
internal fun AudioPlaybackCard(
    statusText: String,
    playbackSampleCount: Int,
    displayedSamples: Int,
    totalSamples: Int,
    isScrubbing: Boolean,
    displayedTime: String,
    totalTime: String,
    isPlaying: Boolean,
    playbackSequenceMode: PlaybackSequenceMode,
    onTogglePlayback: () -> Unit,
    onSkipToPreviousTrack: () -> Unit,
    onSkipToNextTrack: () -> Unit,
    onPlaybackSequenceModeSelected: (PlaybackSequenceMode) -> Unit,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    onExportAudio: () -> Unit,
    onShareSavedAudio: (() -> Unit)?,
    onOpenSavedAudioSheet: () -> Unit,
    onScrubStarted: () -> Unit,
    onScrubChanged: (Int) -> Unit,
    onScrubFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sliderUpperBound = totalSamples.coerceAtLeast(1)
    var userScrubbing by remember { mutableStateOf(false) }
    val clampedDisplayedSamples = displayedSamples.coerceIn(0, sliderUpperBound)
    val shouldAnimateSlider = isPlaying && !isScrubbing && !userScrubbing && totalSamples > 0
    val animatedSliderValue by animateFloatAsState(
        targetValue = clampedDisplayedSamples.toFloat(),
        animationSpec = if (shouldAnimateSlider) {
            tween(
                durationMillis = PLAYBACK_PROGRESS_ANIMATION_DURATION_MS,
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

    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.audio_status, statusText),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onExportAudio) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = stringResource(R.string.audio_action_export)
                    )
                }
                onShareSavedAudio?.let { shareAction ->
                    IconButton(onClick = shareAction) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = stringResource(R.string.library_action_share)
                        )
                    }
                }
                IconButton(onClick = onOpenSavedAudioSheet) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = stringResource(R.string.audio_action_open_saved_audio_list)
                    )
                }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlaybackModeIconButton(
                    selected = playbackSequenceMode == PlaybackSequenceMode.Shuffle,
                    onClick = { onPlaybackSequenceModeSelected(PlaybackSequenceMode.Shuffle) },
                    contentDescription = stringResource(R.string.audio_action_shuffle_playback)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = null
                    )
                }
                PlaybackModeIconButton(
                    selected = playbackSequenceMode == PlaybackSequenceMode.RepeatOne,
                    onClick = { onPlaybackSequenceModeSelected(PlaybackSequenceMode.RepeatOne) },
                    contentDescription = stringResource(R.string.audio_action_repeat_one)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RepeatOne,
                        contentDescription = null
                    )
                }
                PlaybackModeIconButton(
                    selected = playbackSequenceMode == PlaybackSequenceMode.RepeatList,
                    onClick = { onPlaybackSequenceModeSelected(PlaybackSequenceMode.RepeatList) },
                    contentDescription = stringResource(R.string.audio_action_repeat_list)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Repeat,
                        contentDescription = null
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onSkipToPreviousTrack,
                    enabled = canSkipPrevious,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        modifier = Modifier.size(28.dp),
                        contentDescription = stringResource(R.string.audio_action_previous_track)
                    )
                }
                FilledIconButton(
                    onClick = onTogglePlayback,
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        modifier = Modifier.size(48.dp),
                        contentDescription = stringResource(
                            if (isPlaying) {
                                R.string.audio_action_pause
                            } else {
                                R.string.audio_action_play
                            }
                        )
                    )
                }
                IconButton(
                    onClick = onSkipToNextTrack,
                    enabled = canSkipNext,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        modifier = Modifier.size(28.dp),
                        contentDescription = stringResource(R.string.audio_action_next_track)
                    )
                }
            }
            Text(
                stringResource(R.string.audio_sample_count, playbackSampleCount),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private const val PLAYBACK_PROGRESS_ANIMATION_DURATION_MS = 90

@Composable
private fun PlaybackModeIconButton(
    selected: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    icon: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .padding(2.dp)
                .semantics { this.contentDescription = contentDescription },
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
    }
}

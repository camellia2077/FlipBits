package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioVisualizationFrame
import com.bag.audioandroid.domain.AudioVisualizationTrack
import com.bag.audioandroid.ui.model.PlaybackSequenceMode

@Composable
internal fun AudioPlaybackCard(
    statusText: String,
    playbackSampleCount: Int,
    displayedSamples: Int,
    totalSamples: Int,
    isScrubbing: Boolean,
    visualizationTrack: AudioVisualizationTrack?,
    currentVisualizationFrame: AudioVisualizationFrame?,
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
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AudioPlaybackHeaderRow(
                statusText = statusText,
                onExportAudio = onExportAudio,
                onShareSavedAudio = onShareSavedAudio,
                onOpenSavedAudioSheet = onOpenSavedAudioSheet
            )
            AudioPlaybackProgressSection(
                displayedSamples = displayedSamples,
                totalSamples = totalSamples,
                isScrubbing = isScrubbing,
                visualizationTrack = visualizationTrack,
                currentVisualizationFrame = currentVisualizationFrame,
                displayedTime = displayedTime,
                totalTime = totalTime,
                isPlaying = isPlaying,
                onScrubStarted = onScrubStarted,
                onScrubChanged = onScrubChanged,
                onScrubFinished = onScrubFinished
            )
            AudioPlaybackSequenceModeRow(
                playbackSequenceMode = playbackSequenceMode,
                onPlaybackSequenceModeSelected = onPlaybackSequenceModeSelected
            )
            AudioPlaybackTransportControls(
                isPlaying = isPlaying,
                canSkipPrevious = canSkipPrevious,
                canSkipNext = canSkipNext,
                onTogglePlayback = onTogglePlayback,
                onSkipToPreviousTrack = onSkipToPreviousTrack,
                onSkipToNextTrack = onSkipToNextTrack
            )
            Text(
                text = stringResource(R.string.audio_sample_count, playbackSampleCount),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

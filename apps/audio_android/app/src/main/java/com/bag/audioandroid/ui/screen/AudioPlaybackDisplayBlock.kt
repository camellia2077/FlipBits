package com.bag.audioandroid.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption

@Composable
internal fun AudioPlaybackDisplayBlock(
    displayedSamples: Int,
    waveformPcm: ShortArray,
    sampleRateHz: Int,
    isFlashMode: Boolean,
    flashVoicingStyle: FlashVoicingStyleOption?,
    followData: PayloadFollowViewData,
    isPlaying: Boolean,
    displaySectionState: PlaybackDisplaySectionState,
    modifier: Modifier = Modifier,
) {
    PlaybackDisplaySection(
        followData = followData,
        displayedSamples = displayedSamples,
        waveformPcm = waveformPcm,
        sampleRateHz = sampleRateHz,
        isFlashMode = isFlashMode,
        flashVoicingStyle = flashVoicingStyle,
        isPlaying = isPlaying,
        playbackDisplayMode = displaySectionState.playbackDisplayMode,
        flashVisualizationModeName = displaySectionState.flashVisualizationModeName,
        onDisplayModeSelected = displaySectionState.onDisplayModeSelected,
        onFlashVisualizationModeSelected = displaySectionState.onFlashVisualizationModeSelected,
        modifier = modifier,
    )
}

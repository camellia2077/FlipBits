package com.bag.audioandroid.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.state.FlashVisualWindowState

@Composable
internal fun AudioPlaybackDisplayBlock(
    displayedSamples: Int,
    visualDisplayedSamples: Int = displayedSamples,
    waveformPcm: ShortArray,
    isWaveformPreview: Boolean = false,
    sampleRateHz: Int,
    transportMode: TransportModeOption?,
    frameSamples: Int,
    isFlashMode: Boolean,
    flashVoicingStyle: FlashVoicingStyleOption?,
    followData: PayloadFollowViewData,
    flashVisualWindow: FlashVisualWindowState = FlashVisualWindowState(),
    isPlaying: Boolean,
    displaySectionState: PlaybackDisplaySectionState,
    modifier: Modifier = Modifier,
    onSeekToSample: (Int) -> Unit = {},
) {
    PlaybackDisplaySection(
        followData = followData,
        flashVisualWindow = flashVisualWindow,
        displayedSamples = displayedSamples,
        visualDisplayedSamples = visualDisplayedSamples,
        waveformPcm = waveformPcm,
        isWaveformPreview = isWaveformPreview,
        sampleRateHz = sampleRateHz,
        transportMode = transportMode,
        frameSamples = frameSamples,
        isFlashMode = isFlashMode,
        flashVoicingStyle = flashVoicingStyle,
        isPlaying = isPlaying,
        playbackDisplayMode = displaySectionState.playbackDisplayMode,
        flashVisualizationModeName = displaySectionState.flashVisualizationModeName,
        onDisplayModeSelected = displaySectionState.onDisplayModeSelected,
        onFlashVisualizationModeSelected = displaySectionState.onFlashVisualizationModeSelected,
        onSeekToSample = onSeekToSample,
        modifier = modifier,
    )
}

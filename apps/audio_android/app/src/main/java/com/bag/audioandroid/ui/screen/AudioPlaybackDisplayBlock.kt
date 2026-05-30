package com.bag.audioandroid.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.domain.BagDecodeContentCodes
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
    decodedTextStatusCode: Int = BagDecodeContentCodes.STATUS_UNAVAILABLE,
    playbackDetailsSource: String = "unknown",
    flashVisualWindow: FlashVisualWindowState = FlashVisualWindowState(),
    isPlaying: Boolean,
    isScrubbing: Boolean = false,
    isFlashVisualPerfOverlayEnabled: Boolean = false,
    playbackSpeed: Float = 1f,
    displaySectionState: PlaybackDisplaySectionState,
    initialFollowViewMode: PlaybackFollowViewMode = PlaybackFollowViewMode.Binary,
    extraLyricsRecoveryHeight: Dp = 0.dp,
    applyLyricsPreviewBonusLine: Boolean = false,
    onOpenLyricsNavigator: () -> Unit = {},
    modifier: Modifier = Modifier,
    onSeekToSample: (Int) -> Unit = {},
) {
    PlaybackDisplaySection(
        followData = followData,
        playbackDetailsSource = playbackDetailsSource,
        flashVisualWindow = flashVisualWindow,
        isFlashVisualPerfOverlayEnabled = isFlashVisualPerfOverlayEnabled,
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
        decodedTextStatusCode = decodedTextStatusCode,
        isScrubbing = isScrubbing,
        playbackSpeed = playbackSpeed,
        playbackDisplayMode = displaySectionState.playbackDisplayMode,
        flashVisualizationModeName = displaySectionState.flashVisualizationModeName,
        morseVisualizationModeName = displaySectionState.morseVisualizationModeName,
        initialFollowViewMode = initialFollowViewMode,
        lyricsExpanded = displaySectionState.lyricsExpanded,
        extraLyricsRecoveryHeight = extraLyricsRecoveryHeight,
        applyLyricsPreviewBonusLine = applyLyricsPreviewBonusLine,
        onOpenLyricsNavigator = onOpenLyricsNavigator,
        onDisplayModeSelected = displaySectionState.onDisplayModeSelected,
        onFlashVisualizationModeSelected = displaySectionState.onFlashVisualizationModeSelected,
        onMorseVisualizationModeSelected = displaySectionState.onMorseVisualizationModeSelected,
        onLyricsExpandedChanged = displaySectionState.onLyricsExpandedChanged,
        onSeekToSample = onSeekToSample,
        modifier = modifier,
    )
}

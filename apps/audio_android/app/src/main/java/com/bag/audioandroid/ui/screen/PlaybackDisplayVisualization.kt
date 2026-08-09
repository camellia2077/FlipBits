package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.state.FlashVisualWindowState

@Composable
internal fun PlaybackVisualizationContent(
    waveformPcm: ShortArray,
    sampleRateHz: Int,
    visualDisplayedSamples: Int,
    displayedSamples: Int,
    frameSamples: Int,
    isPlaying: Boolean,
    flashVisualizationModeName: String,
    morseVisualizationModeName: String,
    flashVoicingStyle: FlashVoicingStyleOption?,
    flashVisualWindow: FlashVisualWindowState,
    isFlashVisualPerfOverlayEnabled: Boolean,
    playbackSpeed: Float,
    isScrubbing: Boolean,
    followData: PayloadFollowViewData,
    visualizationRoute: PlaybackVisualizationRoute,
    sharedFlashPlaybackSampleState: FlashVisualPlaybackSampleState?,
    onFlashVisualizationModeSelected: (FlashSignalVisualizationMode) -> Unit,
    onMorseVisualizationModeSelected: (MiniMorseVisualizationMode) -> Unit,
) {
    if (waveformPcm.isEmpty()) {
        return
    }
    when (val route = visualizationRoute) {
        PlaybackVisualizationRoute.PcmWaveform ->
            AudioPcmWaveform(
                pcm = waveformPcm,
                sampleRateHz = sampleRateHz,
                displayedSamples = visualDisplayedSamples,
                isPlaying = isPlaying,
                modifier = Modifier.fillMaxWidth(),
            )

        is PlaybackVisualizationRoute.SymbolEnvelope ->
            AudioSymbolEnvelopeVisualizer(
                pcm = waveformPcm,
                sampleRateHz = sampleRateHz,
                displayedSamples = visualDisplayedSamples,
                isPlaying = isPlaying,
                transportMode = route.transportMode,
                frameSamples = frameSamples,
                modifier = Modifier.fillMaxWidth(),
            )

        is PlaybackVisualizationRoute.FlashSignal -> {
            val flashVisualizationMode = flashVisualizationModeFromName(flashVisualizationModeName)
            FlashSignalVisualizationModeSwitcher(
                selectedMode = flashVisualizationMode,
                onModeSelected = onFlashVisualizationModeSelected,
                modifier = Modifier.fillMaxWidth(),
            )
            AudioFlashSignalVisualizer(
                input = route.input,
                isPlaying = isPlaying,
                mode = flashVisualizationMode,
                flashVoicingStyle = flashVoicingStyle,
                flashVisualWindow = flashVisualWindow,
                sharedPlaybackSampleState = sharedFlashPlaybackSampleState,
                showPerfOverlay = isFlashVisualPerfOverlayEnabled,
                playbackSpeed = playbackSpeed,
                isScrubbing = isScrubbing,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        PlaybackVisualizationRoute.ProCompact ->
            ProCompactVisualizer(
                followData = followData,
                displayedSamples = displayedSamples,
                frameSamples = frameSamples,
                modifier = Modifier.fillMaxWidth(),
            )

        PlaybackVisualizationRoute.ProExplanation ->
            ProEncodingExplanationVisualizer(
                followData = followData,
                displayedSamples = displayedSamples,
                frameSamples = frameSamples,
                modifier = Modifier.fillMaxWidth(),
            )

        PlaybackVisualizationRoute.UltraStep ->
            UltraSymbolStepVisualizer(
                displayedSamples = visualDisplayedSamples,
                followData = followData,
                modifier = Modifier.fillMaxWidth(),
            )

        PlaybackVisualizationRoute.MorseTimeline ->
            run {
                val morseVisualizationMode = miniMorseVisualizationModeFromName(morseVisualizationModeName)
                MiniMorseVisualizationModeSwitcher(
                    selectedMode = morseVisualizationMode,
                    onModeSelected = onMorseVisualizationModeSelected,
                    modifier = Modifier.fillMaxWidth(),
                )
                when (morseVisualizationMode) {
                    MiniMorseVisualizationMode.Vertical ->
                        MorseTimelineVisualizer(
                            followData = followData,
                            displayedSamples = displayedSamples,
                            frameSamples = frameSamples,
                            sampleRateHz = sampleRateHz,
                            isPlaying = isPlaying,
                            playbackSpeed = playbackSpeed,
                            isScrubbing = isScrubbing,
                            showPerfOverlay = isFlashVisualPerfOverlayEnabled,
                            modifier = Modifier.fillMaxWidth(),
                        )

                    MiniMorseVisualizationMode.Horizontal ->
                        MorseHorizontalPlaybackVisualizer(
                            followData = followData,
                            displayedSamples = displayedSamples,
                            frameSamples = frameSamples,
                            sampleRateHz = sampleRateHz,
                            isPlaying = isPlaying,
                            playbackSpeed = playbackSpeed,
                            isScrubbing = isScrubbing,
                            showPerfOverlay = isFlashVisualPerfOverlayEnabled,
                            modifier = Modifier.fillMaxWidth(),
                        )
                }
            }
    }
}

@Composable
internal fun rememberMixFlashPlaybackSampleState(
    playbackDisplayMode: PlaybackDisplayMode,
    visualizationRoute: PlaybackVisualizationRoute,
    displayedSamples: Int,
    isPlaying: Boolean,
    isScrubbing: Boolean,
    playbackSpeed: Float,
    sampleRateHz: Int,
): FlashVisualPlaybackSampleState? {
    val followTimelineSource = playbackMixFollowTimelineSourceOrNull(visualizationRoute)
    val totalSamples =
        followTimelineSource
            ?.followData
            ?.totalPcmSampleCount
            ?.coerceAtLeast(displayedSamples)
            ?.coerceAtLeast(1)
            ?: return null
    if (playbackDisplayMode != PlaybackDisplayMode.Mix) {
        return null
    }
    if (isScrubbing) {
        return null
    }
    return rememberFlashVisualPlaybackSampleState(
        rawSample = followTimelineSource.displayedSamples.toFloat(),
        isPlaying = isPlaying,
        playbackSpeed = playbackSpeed,
        sampleRateHz = sampleRateHz,
        totalSamples = totalSamples,
    )
}

internal fun playbackMixFollowTimelineSourceOrNull(
    visualizationRoute: PlaybackVisualizationRoute,
): FlashSignalBucketSource.FollowTimeline? =
    (visualizationRoute as? PlaybackVisualizationRoute.FlashSignal)
        ?.input
        ?.bucketSource as? FlashSignalBucketSource.FollowTimeline

internal fun flashVisualizationModeFromName(flashVisualizationModeName: String): FlashSignalVisualizationMode =
    FlashSignalVisualizationMode.entries.firstOrNull { mode ->
        mode.name == flashVisualizationModeName
    } ?: FlashSignalVisualizationMode.Lanes

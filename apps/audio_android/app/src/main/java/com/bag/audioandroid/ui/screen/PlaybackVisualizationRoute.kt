package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.model.TransportModeOption

internal sealed interface PlaybackVisualizationRoute {
    data object PcmWaveform : PlaybackVisualizationRoute
    data class SymbolEnvelope(
        val transportMode: TransportModeOption,
    ) : PlaybackVisualizationRoute
    data class FlashSignal(
        val input: FlashSignalVisualizationInput,
    ) : PlaybackVisualizationRoute
    data object ProExplanation : PlaybackVisualizationRoute
    data object UltraStep : PlaybackVisualizationRoute
}

internal fun resolvePlaybackVisualizationRoute(
    transportMode: TransportModeOption?,
    isFlashMode: Boolean,
    waveformPcm: ShortArray,
    isWaveformPreview: Boolean,
    sampleRateHz: Int,
    visualDisplayedSamples: Int,
    displayedSamples: Int,
    followData: PayloadFollowViewData,
): PlaybackVisualizationRoute {
    val useFlashFollowTimelineBuckets = followData.canDriveFlashSignalBuckets()
    val useTimelineLimitedVisualization =
        isWaveformPreview || waveformPcm.size >= LONG_AUDIO_VISUALIZATION_SAMPLE_THRESHOLD
    val useLightweightVisualization =
        useTimelineLimitedVisualization && !followData.followAvailable
    val useFlashLightweightVisualization =
        useTimelineLimitedVisualization && !useFlashFollowTimelineBuckets

    return when (transportMode) {
        TransportModeOption.Flash ->
            if (useFlashLightweightVisualization) {
                PlaybackVisualizationRoute.PcmWaveform
            } else {
                PlaybackVisualizationRoute.FlashSignal(
                    input =
                        flashSignalVisualizationInput(
                            pcm = waveformPcm,
                            sampleRateHz = sampleRateHz,
                            visualDisplayedSamples = visualDisplayedSamples,
                            followDisplayedSamples = displayedSamples,
                            followData = followData,
                            useFollowTimelineBuckets = useFlashFollowTimelineBuckets,
                        ),
                )
            }

        TransportModeOption.Pro, TransportModeOption.Ultra ->
            if (useLightweightVisualization) {
                PlaybackVisualizationRoute.SymbolEnvelope(transportMode = transportMode)
            } else if (transportMode == TransportModeOption.Pro) {
                PlaybackVisualizationRoute.ProExplanation
            } else {
                PlaybackVisualizationRoute.UltraStep
            }

        null ->
            if (isFlashMode && !useFlashLightweightVisualization) {
                PlaybackVisualizationRoute.FlashSignal(
                    input =
                        flashSignalVisualizationInput(
                            pcm = waveformPcm,
                            sampleRateHz = sampleRateHz,
                            visualDisplayedSamples = visualDisplayedSamples,
                            followDisplayedSamples = displayedSamples,
                            followData = followData,
                            useFollowTimelineBuckets = useFlashFollowTimelineBuckets,
                        ),
                )
            } else {
                PlaybackVisualizationRoute.PcmWaveform
            }
    }
}

private fun PayloadFollowViewData.canDriveFlashSignalBuckets(): Boolean =
    // Flash visualizations need one binary token per timeline group so the
    // follow timeline can drive low/high buckets without falling back to PCM
    // frequency analysis for long or preview-only audio.
    followAvailable &&
        binaryGroupTimeline.isNotEmpty() &&
        binaryTokens.size >= binaryGroupTimeline.size

private const val LONG_AUDIO_VISUALIZATION_SAMPLE_THRESHOLD = 44100 * 120

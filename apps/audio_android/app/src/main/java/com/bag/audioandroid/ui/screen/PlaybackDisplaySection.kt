package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.BagDecodeContentCodes
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.PlaybackScrubDiagTrace
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.MorseSpeedOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.playerSegmentedButtonColors
import com.bag.audioandroid.ui.state.FlashVisualWindowState

@Composable
internal fun PlaybackDisplaySection(
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
    isScrubbing: Boolean,
    isFlashVisualPerfOverlayEnabled: Boolean = false,
    playbackSpeed: Float = 1f,
    playbackDisplayMode: PlaybackDisplayMode,
    flashVisualizationModeName: String,
    morseVisualizationModeName: String = MiniMorseVisualizationMode.Horizontal.name,
    initialFollowViewMode: PlaybackFollowViewMode = PlaybackFollowViewMode.Binary,
    lyricsExpanded: Boolean,
    extraLyricsRecoveryHeight: Dp = 0.dp,
    applyLyricsPreviewBonusLine: Boolean = false,
    onOpenLyricsNavigator: () -> Unit = {},
    onDisplayModeSelected: (PlaybackDisplayMode) -> Unit,
    onFlashVisualizationModeSelected: (FlashSignalVisualizationMode) -> Unit,
    onMorseVisualizationModeSelected: (MiniMorseVisualizationMode) -> Unit = {},
    onLyricsExpandedChanged: (Boolean) -> Unit,
    onSeekToSample: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var tokenStripHeightDp by remember(playbackDisplayMode, transportMode) { mutableStateOf<Float?>(null) }
    var stableTokenStripHeightDp by remember(playbackDisplayMode, transportMode) { mutableStateOf<Float?>(null) }
    val renderPolicy = rememberPlaybackDisplayRenderPolicy(playbackDisplayMode)
    val layoutModel =
        rememberPlaybackDisplayLayoutModel(
            transportMode = transportMode,
            isFlashMode = isFlashMode,
            waveformPcm = waveformPcm,
            isWaveformPreview = isWaveformPreview,
            sampleRateHz = sampleRateHz,
            visualDisplayedSamples = visualDisplayedSamples,
            displayedSamples = displayedSamples,
            followData = followData,
        )
    val sharedFlashPlaybackSampleState =
        rememberMixFlashPlaybackSampleState(
            playbackDisplayMode = playbackDisplayMode,
            visualizationRoute = layoutModel.visualizationRoute,
            displayedSamples = displayedSamples,
            isPlaying = isPlaying,
            isScrubbing = isScrubbing,
            playbackSpeed = playbackSpeed,
            sampleRateHz = sampleRateHz,
        )
    val followSectionDisplayedSamples =
        playbackFollowSectionDisplayedSamples(
            playbackDisplayMode = playbackDisplayMode,
            displayedSamples = displayedSamples,
            visualDisplayedSamples = visualDisplayedSamples,
            sharedFlashPlaybackSampleState = sharedFlashPlaybackSampleState,
        )
    if (transportMode == TransportModeOption.Mini) {
        MiniAlignmentPerfTrace.record(
            followData = followData,
            isPlaying = isPlaying,
            visualSample = displayedSamples,
            lyricsSample = visualDisplayedSamples,
            frameSamples = frameSamples,
            speed = MorseSpeedOption.fromFrameSamples(frameSamples).id,
        )
    }
    SideEffect {
        PlaybackScrubDiagTrace.display(
            displayMode = playbackDisplayMode,
            visualizationRoute = layoutModel.visualizationRoute,
            displayedSamples = displayedSamples,
            visualDisplayedSamples = visualDisplayedSamples,
            followSectionDisplayedSamples = followSectionDisplayedSamples,
            isPlaying = isPlaying,
            isScrubbing = isScrubbing,
            playbackSpeed = playbackSpeed,
            activeTokenIndex =
                activeTextTimelineIndex(
                    entries = followData.textTokenTimeline,
                    displayedSamples = displayedSamples,
                ),
        )
    }
    val resolvedTokenStripHeightDp =
        if (transportMode?.supportsSharedTokenPage() == true) {
            stableTokenStripHeightDp ?: tokenStripHeightDp
        } else {
            tokenStripHeightDp
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("playback-display-section"),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag("playback-display-switcher"),
        ) {
            layoutModel.displayModeOptions.forEachIndexed { index, option ->
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
                            count = layoutModel.displayModeOptions.size,
                        ),
                    colors = playerSegmentedButtonColors(),
                    label = { Text(text = optionLabel) },
                )
            }
        }
        Spacer(modifier = Modifier.height(renderPolicy.topSpacing))
        if (renderPolicy.showsVisualization) {
            PlaybackVisualizationContent(
                waveformPcm = waveformPcm,
                sampleRateHz = sampleRateHz,
                visualDisplayedSamples = visualDisplayedSamples,
                displayedSamples = displayedSamples,
                frameSamples = frameSamples,
                isPlaying = isPlaying,
                flashVisualizationModeName = flashVisualizationModeName,
                morseVisualizationModeName = morseVisualizationModeName,
                flashVoicingStyle = flashVoicingStyle,
                flashVisualWindow = flashVisualWindow,
                isFlashVisualPerfOverlayEnabled = isFlashVisualPerfOverlayEnabled,
                playbackSpeed = playbackSpeed,
                isScrubbing = isScrubbing,
                followData = followData,
                visualizationRoute = layoutModel.visualizationRoute,
                sharedFlashPlaybackSampleState = sharedFlashPlaybackSampleState,
                onFlashVisualizationModeSelected = onFlashVisualizationModeSelected,
                onMorseVisualizationModeSelected = onMorseVisualizationModeSelected,
            )
            enhancedVisualStatusTextResId(
                playbackDisplayMode = playbackDisplayMode,
                transportMode = transportMode,
                visualizationRoute = layoutModel.visualizationRoute,
                followData = followData,
                decodedTextStatusCode = decodedTextStatusCode,
            )?.let { statusTextResId ->
                Text(
                    text = stringResource(statusTextResId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        if (renderPolicy.showsMixSpacer) {
            Spacer(modifier = Modifier.height(14.dp))
        }
        if (renderPolicy.showsFollowSection) {
            // The playback area mirrors a music player: visual mode works like album art,
            // while lyrics mode hands off to the formal line-timeline lyric page.
            PlaybackDataFollowSection(
                followData = followData,
                displayedSamples = followSectionDisplayedSamples,
                isPlaying = isPlaying,
                transportMode = transportMode,
                decodedTextStatusCode = decodedTextStatusCode,
                playbackDetailsSource = playbackDetailsSource,
                initialAnnotationMode = initialFollowViewMode,
                contentSpacing = renderPolicy.followContentSpacing,
                onTokenStripHeightDpChanged = { heightDp ->
                    tokenStripHeightDp = heightDp
                    stableTokenStripHeightDp =
                        when (val currentStable = stableTokenStripHeightDp) {
                            null -> heightDp
                            else -> maxOf(currentStable, heightDp)
                        }
                },
                onSeekToSample = onSeekToSample,
            )
        }
        renderPolicy.bottomSpacing?.let { bottomSpacing ->
            Spacer(modifier = Modifier.height(bottomSpacing))
        }
        if (renderPolicy.showsExpandableLyrics) {
            ExpandablePlaybackLyricsSection(
                followData = followData,
                displayedSamples = displayedSamples,
                sampleRateHz = sampleRateHz,
                isPlaying = isPlaying,
                isScrubbing = isScrubbing,
                transportMode = transportMode,
                playbackDisplayMode = playbackDisplayMode,
                lyricsExpanded = lyricsExpanded,
                tokenStripHeightDp = resolvedTokenStripHeightDp,
                extraLyricsRecoveryHeight = extraLyricsRecoveryHeight,
                applyLyricsPreviewBonusLine = applyLyricsPreviewBonusLine,
                onOpenLyricsNavigator = onOpenLyricsNavigator,
                onLyricsExpandedChanged = onLyricsExpandedChanged,
                onSeekToSample = onSeekToSample,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

internal fun playbackFollowSectionDisplayedSamples(
    playbackDisplayMode: PlaybackDisplayMode,
    displayedSamples: Int,
    visualDisplayedSamples: Int,
    sharedFlashPlaybackSampleState: FlashVisualPlaybackSampleState? = null,
): Int =
    if (playbackDisplayMode == PlaybackDisplayMode.Mix) {
        sharedFlashPlaybackSampleState?.displayedSample?.toInt() ?: visualDisplayedSamples
    } else {
        displayedSamples
    }

private fun enhancedVisualStatusTextResId(
    playbackDisplayMode: PlaybackDisplayMode,
    transportMode: TransportModeOption?,
    visualizationRoute: PlaybackVisualizationRoute,
    followData: PayloadFollowViewData,
    decodedTextStatusCode: Int,
): Int? {
    if (playbackDisplayMode != PlaybackDisplayMode.Visual) {
        return null
    }
    val isEnhancedVisualMissing =
        when (transportMode) {
            TransportModeOption.Flash ->
                visualizationRoute is PlaybackVisualizationRoute.PcmWaveform &&
                    (!followData.followAvailable || followData.binaryGroupTimeline.isEmpty())

            TransportModeOption.Pro,
            TransportModeOption.Ultra,
            ->
                visualizationRoute is PlaybackVisualizationRoute.SymbolEnvelope &&
                    !followData.followAvailable

            TransportModeOption.Mini ->
                visualizationRoute is PlaybackVisualizationRoute.PcmWaveform &&
                    (!followData.followAvailable || followData.binaryGroupTimeline.isEmpty())

            null -> false
        }
    if (!isEnhancedVisualMissing) {
        return null
    }
    return if (decodedTextStatusCode.isDecodeFailureStatus()) {
        R.string.audio_follow_visuals_failed
    } else {
        R.string.audio_follow_unavailable
    }
}

private fun Int.isDecodeFailureStatus(): Boolean =
    this != BagDecodeContentCodes.STATUS_UNAVAILABLE &&
        this != BagDecodeContentCodes.STATUS_OK &&
        this != BagDecodeContentCodes.STATUS_BUFFER_TOO_SMALL

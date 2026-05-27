package com.bag.audioandroid.ui.screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.BuildConfig
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.MorseSpeedOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.playerSegmentedButtonColors
import com.bag.audioandroid.ui.state.FlashVisualWindowState
import com.bag.audioandroid.ui.theme.appThemeVisualTokens
import kotlinx.coroutines.launch
import kotlin.math.abs

private data class PlaybackDisplayRenderPolicy(
    val topSpacing: Dp,
    val showsVisualization: Boolean,
    val showsMixSpacer: Boolean,
    val showsFollowSection: Boolean,
    val followContentSpacing: Dp,
    val showsExpandableLyrics: Boolean,
    val bottomSpacing: Dp?,
)

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

@Composable
private fun rememberPlaybackDisplayRenderPolicy(playbackDisplayMode: PlaybackDisplayMode): PlaybackDisplayRenderPolicy =
    remember(playbackDisplayMode) {
        when (playbackDisplayMode) {
            PlaybackDisplayMode.Visual ->
                PlaybackDisplayRenderPolicy(
                    topSpacing = 10.dp,
                    showsVisualization = true,
                    showsMixSpacer = false,
                    showsFollowSection = false,
                    followContentSpacing = 10.dp,
                    showsExpandableLyrics = true,
                    bottomSpacing = 10.dp,
                )

            PlaybackDisplayMode.Mix ->
                PlaybackDisplayRenderPolicy(
                    topSpacing = 10.dp,
                    showsVisualization = true,
                    showsMixSpacer = true,
                    showsFollowSection = true,
                    followContentSpacing = 6.dp,
                    showsExpandableLyrics = false,
                    bottomSpacing = null,
                )

            PlaybackDisplayMode.Lyrics ->
                PlaybackDisplayRenderPolicy(
                    topSpacing = 6.dp,
                    showsVisualization = false,
                    showsMixSpacer = false,
                    showsFollowSection = true,
                    followContentSpacing = 10.dp,
                    showsExpandableLyrics = true,
                    bottomSpacing = 6.dp,
                )
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

@Composable
private fun rememberMixFlashPlaybackSampleState(
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

@Composable
private fun PlaybackVisualizationContent(
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

private fun playbackMixFollowTimelineSourceOrNull(
    visualizationRoute: PlaybackVisualizationRoute,
): FlashSignalBucketSource.FollowTimeline? =
    (visualizationRoute as? PlaybackVisualizationRoute.FlashSignal)
        ?.input
        ?.bucketSource as? FlashSignalBucketSource.FollowTimeline

private fun flashVisualizationModeFromName(flashVisualizationModeName: String): FlashSignalVisualizationMode =
    FlashSignalVisualizationMode.entries.firstOrNull { mode ->
        mode.name == flashVisualizationModeName
    } ?: FlashSignalVisualizationMode.Lanes

@Composable
private fun ExpandablePlaybackLyricsSection(
    followData: PayloadFollowViewData,
    displayedSamples: Int,
    sampleRateHz: Int,
    isPlaying: Boolean,
    isScrubbing: Boolean,
    transportMode: TransportModeOption?,
    playbackDisplayMode: PlaybackDisplayMode,
    lyricsExpanded: Boolean,
    tokenStripHeightDp: Float?,
    extraLyricsRecoveryHeight: Dp,
    applyLyricsPreviewBonusLine: Boolean,
    onOpenLyricsNavigator: () -> Unit,
    onLyricsExpandedChanged: (Boolean) -> Unit,
    onSeekToSample: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lyricsLayoutModel =
        rememberPlaybackLyricsLayoutModel(
            followData = followData,
            displayedSamples = displayedSamples,
            transportMode = transportMode,
            playbackDisplayMode = playbackDisplayMode,
            tokenStripHeightDp = tokenStripHeightDp,
            extraLyricsRecoveryHeight = extraLyricsRecoveryHeight,
            applyLyricsPreviewBonusLine = applyLyricsPreviewBonusLine,
            lyricsExpanded = true,
        )
    val selectionListState =
        rememberLazyListState(
            initialFirstVisibleItemIndex =
                (lyricsLayoutModel.activeLineIndex).coerceIn(
                    minimumValue = 0,
                    maximumValue = lyricsLayoutModel.displayLineRanges.lastIndex.coerceAtLeast(0),
                ),
        )
    var isSelectingLyricsLine by remember { mutableStateOf(false) }
    var selectedLineStartSample by remember { mutableStateOf<Int?>(null) }
    val latestSelectedLineStartSample by rememberUpdatedState(selectedLineStartSample)
    val latestOnSeekToSample by rememberUpdatedState(onSeekToSample)
    val lyricsSelectionScope = rememberCoroutineScope()
    LaunchedEffect(lyricsLayoutModel.activeLineIndex, isSelectingLyricsLine, lyricsLayoutModel.displayLineRanges.size) {
        if (!isSelectingLyricsLine && lyricsLayoutModel.activeLineIndex >= 0) {
            selectionListState.scrollToItem(lyricsLayoutModel.activeLineIndex)
        }
    }
    SideEffect {
        debugPlayerLyricsCapacity(
            "lyricsLayout",
            "transport=${transportMode?.wireName ?: "unknown"} " +
                "displayMode=${playbackDisplayMode.name.lowercase()} selectable=true " +
                "tokenStripHeightDp=${tokenStripHeightDp ?: -1f} " +
                "extraRecoveryDp=${extraLyricsRecoveryHeight.value} " +
                "effectiveExtraRecoveryDp=${lyricsLayoutModel.effectiveExtraLyricsRecoveryHeight.value} " +
                "bonusLine=$applyLyricsPreviewBonusLine " +
                "compactVisibleLineCount=${lyricsLayoutModel.compactVisibleLineCount} " +
                "displayLineCount=${lyricsLayoutModel.displayLineRanges.size} " +
                "prefersWrapped=${lyricsLayoutModel.prefersWrappedLines}",
        )
    }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("playback-lyrics-section"),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .pointerInput(lyricsLayoutModel.displayLineRanges) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                isSelectingLyricsLine = true
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                lyricsSelectionScope.launch {
                                    selectionListState.scrollBy(-dragAmount)
                                }
                            },
                            onDragEnd = {
                                latestSelectedLineStartSample?.let(latestOnSeekToSample)
                                isSelectingLyricsLine = false
                            },
                            onDragCancel = {
                                isSelectingLyricsLine = false
                            },
                        )
                    },
        ) {
            if (isSelectingLyricsLine) {
                PlaybackLyricsFullList(
                    followData = followData,
                    displayLineRanges = lyricsLayoutModel.displayLineRanges,
                    activeLineIndex = lyricsLayoutModel.activeLineIndex,
                    sampleRateHz = sampleRateHz,
                    onSeekToSample = onSeekToSample,
                    extraHeight = lyricsLayoutModel.effectiveExtraLyricsRecoveryHeight,
                    listState = selectionListState,
                    autoSeekOnScrollStop = false,
                    selectionFadeInactiveLines = true,
                    onSelectedSampleChanged = { selectedLineStartSample = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                PlaybackTokenContextTape(
                    followData = followData,
                    displayedSamples = displayedSamples,
                    isPlaying = isPlaying,
                    isScrubbing = isScrubbing,
                    visibleLineCount = lyricsLayoutModel.compactVisibleLineCount,
                    extraContainerHeight = lyricsLayoutModel.effectiveExtraLyricsRecoveryHeight,
                    layoutMeasurementSource =
                        when (playbackDisplayMode) {
                            PlaybackDisplayMode.Visual -> PlaybackLyricsLayoutMeasurementSource.VisualPreview
                            PlaybackDisplayMode.Mix -> PlaybackLyricsLayoutMeasurementSource.VisualPreview
                            PlaybackDisplayMode.Lyrics -> PlaybackLyricsLayoutMeasurementSource.LyricsPreview
                        },
                    onSeekToSample = onSeekToSample,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (followData.textFollowAvailable && followData.textTokens.isNotEmpty()) {
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = onOpenLyricsNavigator,
                modifier =
                    Modifier
                        .padding(top = 4.dp)
                        .size(36.dp)
                        .testTag("playback-lyrics-open-navigator")
                        .semantics {
                            contentDescription = "Open full lyrics"
                        },
            ) {
                Icon(
                    imageVector = Icons.Rounded.OpenInFull,
                    contentDescription = stringResource(R.string.audio_lyrics_navigator_open),
                )
            }
        }
    }
}

private fun debugPlayerLyricsCapacity(
    label: String,
    message: String,
) {
    if (!BuildConfig.DEBUG) {
        return
    }
    try {
        Log.d("PlayerLyricsCapacity", "$label $message")
    } catch (_: RuntimeException) {
    }
}

@Composable
internal fun PlaybackLyricsFullList(
    followData: PayloadFollowViewData,
    displayLineRanges: List<DisplayTokenLineRange>,
    activeLineIndex: Int,
    sampleRateHz: Int,
    onSeekToSample: (Int) -> Unit,
    extraHeight: Dp = 0.dp,
    useFixedHeight: Boolean = true,
    listState: LazyListState? = null,
    autoSeekOnScrollStop: Boolean = true,
    showSelectionGuideOnlyWhileScrolling: Boolean = false,
    selectionFadeInactiveLines: Boolean = false,
    onSelectedSampleChanged: (Int?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(displayLineRanges.size) {
        Log.d("MiniAutomation", "lyricsFullListRendered lineCount=${displayLineRanges.size}")
    }
    val lineStartSamples =
        remember(followData.textTokenTimeline) {
            followData.textTokenTimeline
                .groupBy { it.tokenIndex }
                .mapValues { (_, entries) -> entries.minOfOrNull { it.startSample } }
        }
    val lineItems =
        remember(followData, displayLineRanges, lineStartSamples) {
            displayLineRanges.map { lineRange ->
                val lineText =
                    resolveFullLyricsLineText(
                        followData = followData,
                        lineRange = lineRange,
                    )
                        ?: resolveContinuousViewportLineForRange(
                            followData = followData,
                            tokenRange = lineRange.tokenRange,
                        ).text
                PlaybackLyricLineItem(
                    text = lineText,
                    startSample =
                        lineStartSamples[lineRange.tokenBeginIndex]
                            ?.takeIf { it >= 0 },
                )
            }
        }
    val resolvedListState =
        listState ?: rememberLazyListState(
            initialFirstVisibleItemIndex =
                (activeLineIndex - 2).coerceIn(
                    minimumValue = 0,
                    maximumValue = displayLineRanges.lastIndex.coerceAtLeast(0),
                ),
        )
    val selectedLineIndex by remember(resolvedListState, lineItems, activeLineIndex) {
        derivedStateOf {
            centeredVisibleLineIndex(
                listState = resolvedListState,
                fallbackIndex = activeLineIndex,
                lineCount = lineItems.size,
            )
        }
    }
    val selectedStartSample = lineItems.getOrNull(selectedLineIndex)?.startSample
    val latestSelectedStartSample by rememberUpdatedState(selectedStartSample)
    val latestOnSeekToSample by rememberUpdatedState(onSeekToSample)
    var userScrollSelectionActive by remember { mutableStateOf(false) }
    var isSelectionGuideVisible by remember { mutableStateOf(!showSelectionGuideOnlyWhileScrolling) }
    LaunchedEffect(selectedStartSample) {
        onSelectedSampleChanged(selectedStartSample)
    }
    LaunchedEffect(resolvedListState, autoSeekOnScrollStop, showSelectionGuideOnlyWhileScrolling) {
        snapshotFlow { resolvedListState.isScrollInProgress }
            .collect { isScrollInProgress ->
                isSelectionGuideVisible = !showSelectionGuideOnlyWhileScrolling || isScrollInProgress
                if (isScrollInProgress) {
                    userScrollSelectionActive = true
                } else if (userScrollSelectionActive) {
                    userScrollSelectionActive = false
                    val targetSample = latestSelectedStartSample
                    if (autoSeekOnScrollStop && targetSample != null) {
                        latestOnSeekToSample(targetSample)
                    }
                }
            }
    }
    LaunchedEffect(showSelectionGuideOnlyWhileScrolling) {
        if (!showSelectionGuideOnlyWhileScrolling) {
            isSelectionGuideVisible = true
        }
    }
    val selectorColor = playbackLyricsSelectionLineColor()
    BoxWithConstraints(
        modifier =
            modifier
                .then(
                    if (useFixedHeight) {
                        Modifier.height(280.dp + extraHeight)
                    } else {
                        Modifier.fillMaxHeight()
                    },
                ).testTag("playback-lyrics-full-list"),
    ) {
        val selectorPadding = ((maxHeight - PlaybackLyricsSelectionTargetHeight) / 2).coerceAtLeast(0.dp)
        LazyColumn(
            state = resolvedListState,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
            contentPadding = PaddingValues(vertical = selectorPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(lineItems) { index, lineItem ->
                val isActive = index == activeLineIndex
                val isSelected = index == selectedLineIndex
                Text(
                    text = lineItem.text,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .then(
                                if (lineItem.startSample != null) {
                                    Modifier.clickable { onSeekToSample(lineItem.startSample) }
                                } else {
                                    Modifier
                                },
                            ).testTag(if (isActive) "playback-lyrics-full-line-active" else "playback-lyrics-full-line"),
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            fontWeight =
                                when {
                                    isActive || isSelected -> FontWeight.SemiBold
                                    else -> FontWeight.Normal
                                },
                        ),
                    color =
                        when {
                            selectionFadeInactiveLines && isSelected -> MaterialTheme.colorScheme.onSurface
                            selectionFadeInactiveLines ->
                                MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = PlaybackLyricsSelectionInactiveLineAlpha,
                                )
                            isActive -> MaterialTheme.colorScheme.primary
                            isSelected -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
        }
        if (isSelectionGuideVisible) {
            PlaybackLyricsSelectionGuide(
                selectedStartSample = selectedStartSample,
                sampleRateHz = sampleRateHz,
                lineColor = selectorColor,
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(),
            )
        }
    }
}

private data class PlaybackLyricLineItem(
    val text: String,
    val startSample: Int?,
)

private val PlaybackLyricsSelectionTargetHeight = 48.dp
private val PlaybackLyricsSelectionLineWidth = 132.dp

private fun centeredVisibleLineIndex(
    listState: LazyListState,
    fallbackIndex: Int,
    lineCount: Int,
): Int {
    if (lineCount <= 0) {
        return 0
    }
    val layoutInfo = listState.layoutInfo
    val centerOffset = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    val centeredItem =
        layoutInfo.visibleItemsInfo.minByOrNull { itemInfo ->
            abs((itemInfo.offset + itemInfo.size / 2) - centerOffset)
        }
    return centeredItem
        ?.index
        ?.coerceIn(0, lineCount - 1)
        ?: fallbackIndex.coerceIn(0, lineCount - 1)
}

@Composable
private fun playbackLyricsSelectionLineColor(): Color {
    val visualTokens = appThemeVisualTokens()
    val outlineColor =
        visualTokens.subtleOutlineColor
            .takeIf { it.isSpecified }
            ?: MaterialTheme.colorScheme.outline
    return outlineColor.copy(alpha = PlaybackLyricsSelectionLineAlpha)
}

private const val PlaybackLyricsSelectionLineAlpha = 0.42f
private const val PlaybackLyricsSelectionInactiveLineAlpha = 0.42f

@Composable
private fun PlaybackLyricsSelectionGuide(
    selectedStartSample: Int?,
    sampleRateHz: Int,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    val selectedTime =
        formatDurationMillis(
            samplesToMillis(
                samples = selectedStartSample ?: 0,
                sampleRateHz = sampleRateHz,
            ),
        )
    Row(
        modifier =
            modifier
                .height(PlaybackLyricsSelectionTargetHeight)
                .testTag("playback-lyrics-selection-guide"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1f, fill = false)
                    .width(PlaybackLyricsSelectionLineWidth)
                    .height(1.dp)
                    .background(lineColor)
                    .testTag("playback-lyrics-selection-line"),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = selectedTime,
            modifier =
                Modifier
                    .width(48.dp)
                    .testTag("playback-lyrics-selection-time"),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun resolveFullLyricsLineText(
    followData: PayloadFollowViewData,
    lineRange: DisplayTokenLineRange,
): String? =
    if (lineRange.coversFullSourceLine) {
        followData.lyricLines.getOrNull(lineRange.sourceLineIndex)
    } else {
        null
    }

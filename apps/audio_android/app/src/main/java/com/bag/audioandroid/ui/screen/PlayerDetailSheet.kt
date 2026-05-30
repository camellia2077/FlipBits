package com.bag.audioandroid.ui.screen

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.BuildConfig
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.BagDecodeContentCodes
import com.bag.audioandroid.domain.DecodeOperationSnapshot
import com.bag.audioandroid.domain.DecodedPayloadViewData
import com.bag.audioandroid.domain.FlashSignalInfo
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.domain.WavAudioInfo
import com.bag.audioandroid.ui.LyricsNavigatorReadingModel
import com.bag.audioandroid.ui.PlayerChromeColors
import com.bag.audioandroid.ui.PlayerDetailBottomActions
import com.bag.audioandroid.ui.buildLyricsNavigatorReadingModel
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.MiniPlayerUiModel
import com.bag.audioandroid.ui.model.PlaybackSequenceMode
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.playerChromeColors
import com.bag.audioandroid.ui.resolveSeekSampleForReadingLine
import com.bag.audioandroid.ui.state.FlashVisualWindowState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
internal fun PlayerDetailSheetContent(
    miniPlayerModel: MiniPlayerUiModel,
    displayedSamples: Int,
    waveformDisplayedSamples: Int = displayedSamples,
    totalSamples: Int,
    isScrubbing: Boolean,
    waveformPcm: ShortArray,
    isWaveformPreview: Boolean = false,
    sampleRateHz: Int,
    frameSamples: Int = 2205,
    wavAudioInfo: WavAudioInfo = WavAudioInfo.Empty,
    flashSignalInfo: FlashSignalInfo = FlashSignalInfo.Empty,
    displayedTime: String,
    totalTime: String,
    isPlaying: Boolean,
    playbackSequenceMode: PlaybackSequenceMode,
    playbackSpeed: Float,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    canExportGeneratedAudio: Boolean,
    followData: PayloadFollowViewData,
    playbackDetailsSource: String = "unknown",
    decodedPayload: DecodedPayloadViewData = DecodedPayloadViewData.Empty,
    lyricsNavigatorReadingModel: LyricsNavigatorReadingModel? = null,
    flashVisualWindow: FlashVisualWindowState = FlashVisualWindowState(),
    savedAudioItem: SavedAudioItem?,
    showSavedAudioDecodeLoadingNotice: Boolean = false,
    savedAudioDecodeProgressSnapshot: DecodeOperationSnapshot? = null,
    isFlashVisualPerfOverlayEnabled: Boolean = false,
    onTogglePlayback: () -> Unit,
    onSkipToPreviousTrack: () -> Unit,
    onSkipToNextTrack: () -> Unit,
    onPlaybackSequenceModeSelected: (PlaybackSequenceMode) -> Unit,
    onPlaybackSpeedSelected: (Float) -> Unit,
    onExportGeneratedAudio: () -> Unit = {},
    onExportGeneratedAudioToDocument: () -> Unit = {},
    onShareSavedAudio: (() -> Unit)? = null,
    onOpenSavedAudioSheet: () -> Unit = {},
    bottomActions: PlayerDetailBottomActions? = null,
    onScrubStarted: () -> Unit,
    onScrubChanged: (Int) -> Unit,
    onScrubFinished: () -> Unit,
    onLyricsRequested: () -> Unit = {},
    onPlaybackDisplayModeSelected: (PlaybackDisplayMode) -> Unit = {},
    debugExpandLyricsRequestId: Long? = null,
    onDebugExpandLyricsHandled: (Long) -> Unit = {},
    debugPlaybackDisplayModeRequest: DebugPlaybackDisplayModeRequest? = null,
    onDebugPlaybackDisplayModeHandled: (Long) -> Unit = {},
    debugMorseVisualizationModeRequest: DebugMorseVisualizationModeRequest? = null,
    onDebugMorseVisualizationModeHandled: (Long) -> Unit = {},
    onSeekToSample: (Int) -> Unit = { targetSamples ->
        onScrubStarted()
        onScrubChanged(targetSamples)
        onScrubFinished()
    },
    initialDisplayMode: PlaybackDisplayMode = PlaybackDisplayMode.Lyrics,
    initialFollowViewMode: PlaybackFollowViewMode = PlaybackFollowViewMode.Binary,
    initialFlashVisualizationMode: FlashSignalVisualizationMode? = null,
    initialMorseVisualizationMode: MiniMorseVisualizationMode = MiniMorseVisualizationMode.Horizontal,
    onLyricsNavigatorVisibilityChanged: (Boolean) -> Unit = {},
    topContentPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val resolvedBottomActions =
        bottomActions ?: PlayerDetailBottomActions(
            onOpenSavedAudioSheet = onOpenSavedAudioSheet,
            onSaveToLibrary = if (canExportGeneratedAudio) onExportGeneratedAudio else null,
            isAlreadySavedToLibrary = savedAudioItem != null,
        )
    val density = LocalDensity.current
    var rootSlice by remember { mutableStateOf<PlaybackVerticalSlice?>(null) }
    var scrollSlice by remember { mutableStateOf<PlaybackVerticalSlice?>(null) }
    var displaySlice by remember { mutableStateOf<PlaybackVerticalSlice?>(null) }
    var bottomDockSlice by remember { mutableStateOf<PlaybackVerticalSlice?>(null) }
    val displaySectionState =
        rememberPlaybackDisplaySectionState(
            isFlashMode = miniPlayerModel.isFlashMode,
            onLyricsRequested = onLyricsRequested,
            initialDisplayMode = initialDisplayMode,
            initialFlashVisualizationMode = initialFlashVisualizationMode ?: FlashSignalVisualizationMode.Lanes,
            initialMorseVisualizationMode = initialMorseVisualizationMode,
            onDisplayModeSelected = onPlaybackDisplayModeSelected,
        )
    var isLyricsNavigatorVisible by remember { mutableStateOf(false) }
    BackHandler(enabled = isLyricsNavigatorVisible) {
        isLyricsNavigatorVisible = false
    }
    LaunchedEffect(isLyricsNavigatorVisible) {
        onLyricsNavigatorVisibilityChanged(isLyricsNavigatorVisible)
    }
    val layoutPolicyState =
        rememberPlayerDetailLayoutPolicyState(
            transportMode = miniPlayerModel.transportMode,
            playbackDisplayMode = displaySectionState.playbackDisplayMode,
            displaySlice = displaySlice,
            bottomDockSlice = bottomDockSlice,
            density = density,
        )
    LaunchedEffect(debugExpandLyricsRequestId) {
        val requestId = debugExpandLyricsRequestId ?: return@LaunchedEffect
        displaySectionState.onLyricsExpandedChanged(true)
        android.util.Log.d(playerDetailAutomationTag(miniPlayerModel.transportMode), "lyricsExpanded requestId=$requestId expanded=true")
        onDebugExpandLyricsHandled(requestId)
    }
    LaunchedEffect(debugPlaybackDisplayModeRequest) {
        val request = debugPlaybackDisplayModeRequest ?: return@LaunchedEffect
        displaySectionState.onDisplayModeSelected(request.mode)
        android.util.Log.d(
            playerDetailAutomationTag(miniPlayerModel.transportMode),
            "displayModeApplied requestId=${request.requestId} mode=${request.mode.name.lowercase()}",
        )
        onDebugPlaybackDisplayModeHandled(request.requestId)
    }
    LaunchedEffect(debugMorseVisualizationModeRequest) {
        val request = debugMorseVisualizationModeRequest ?: return@LaunchedEffect
        displaySectionState.onMorseVisualizationModeSelected(request.mode)
        android.util.Log.d(
            playerDetailAutomationTag(miniPlayerModel.transportMode),
            "morseVisualizationModeApplied requestId=${request.requestId} mode=${request.mode.name.lowercase()}",
        )
        onDebugMorseVisualizationModeHandled(request.requestId)
    }
    LaunchedEffect(
        miniPlayerModel.transportMode,
        displaySectionState.playbackDisplayMode,
        rootSlice,
        scrollSlice,
        displaySlice,
        bottomDockSlice,
    ) {
        val root = rootSlice ?: return@LaunchedEffect
        val scroll = scrollSlice ?: return@LaunchedEffect
        val display = displaySlice ?: return@LaunchedEffect
        val dock = bottomDockSlice ?: return@LaunchedEffect
        val gapToScrollBottomPx = (scroll.bottomPx - display.bottomPx).coerceAtLeast(0)
        val gapToDockTopPx = (dock.topPx - display.bottomPx).coerceAtLeast(0)
        android.util.Log.d(
            "PlaybackVerticalLayout",
            "transport=${miniPlayerModel.transportMode.wireName} displayMode=${displaySectionState.playbackDisplayMode.name.lowercase()} " +
                "rootHeightDp=${root.heightDp(density)} scrollHeightDp=${scroll.heightDp(density)} " +
                "displayHeightDp=${display.heightDp(density)} bottomDockHeightDp=${dock.heightDp(density)} " +
                "displayBottomToScrollBottomDp=${pxToDpString(gapToScrollBottomPx, density)} " +
                "displayBottomToDockTopDp=${pxToDpString(gapToDockTopPx, density)} " +
                "scrollTopDp=${scroll.topDp(density)} displayTopDp=${display.topDp(density)} dockTopDp=${dock.topDp(density)}",
        )
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    rootSlice = coordinates.toPlaybackVerticalSlice()
                }.testTag("player-detail-sheet-content"),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (isLyricsNavigatorVisible) {
                    LyricsNavigatorScaffold(
                        followData = followData,
                        navigatorReadingModel = lyricsNavigatorReadingModel,
                        displayedSamples = displayedSamples,
                        totalSamples = totalSamples,
                        displayedTime = displayedTime,
                        totalTime = totalTime,
                        isPlaying = isPlaying,
                        isScrubbing = isScrubbing,
                        playbackSequenceMode = playbackSequenceMode,
                        playbackSpeed = playbackSpeed,
                        canSkipPrevious = canSkipPrevious,
                        canSkipNext = canSkipNext,
                        transportMode = miniPlayerModel.transportMode,
                        durationMs = miniPlayerModel.durationMs,
                        sampleRateHz = sampleRateHz,
                        frameSamples = frameSamples,
                        wavAudioInfo = wavAudioInfo,
                        onBack = { isLyricsNavigatorVisible = false },
                        onTogglePlayback = onTogglePlayback,
                        onSkipToPreviousTrack = onSkipToPreviousTrack,
                        onSkipToNextTrack = onSkipToNextTrack,
                        onPlaybackSequenceModeSelected = onPlaybackSequenceModeSelected,
                        onPlaybackSpeedSelected = onPlaybackSpeedSelected,
                        onScrubStarted = onScrubStarted,
                        onScrubChanged = onScrubChanged,
                        onScrubFinished = onScrubFinished,
                        onSeekToSample = onSeekToSample,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    PlayerDetailScrollContent(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        displayedSamples = displayedSamples,
                        waveformDisplayedSamples = waveformDisplayedSamples,
                        waveformPcm = waveformPcm,
                        isWaveformPreview = isWaveformPreview,
                        sampleRateHz = sampleRateHz,
                        frameSamples = frameSamples,
                        transportMode = miniPlayerModel.transportMode,
                        isFlashMode = miniPlayerModel.isFlashMode,
                        flashVoicingStyle = miniPlayerModel.flashVoicingStyle,
                        followData = followData,
                        playbackDetailsSource = playbackDetailsSource,
                        decodedTextStatusCode = decodedPayload.textDecodeStatusCode,
                        flashVisualWindow = flashVisualWindow,
                        isPlaying = isPlaying,
                        isScrubbing = isScrubbing,
                        isFlashVisualPerfOverlayEnabled = isFlashVisualPerfOverlayEnabled,
                        playbackSpeed = playbackSpeed,
                        displaySectionState = displaySectionState,
                        initialFollowViewMode = initialFollowViewMode,
                        savedAudioItem = savedAudioItem,
                        showSavedAudioDecodeLoadingNotice = showSavedAudioDecodeLoadingNotice,
                        savedAudioDecodeProgressSnapshot = savedAudioDecodeProgressSnapshot,
                        topContentPadding = topContentPadding,
                        extraLyricsRecoveryHeight = layoutPolicyState.extraLyricsRecoveryHeight,
                        applyLyricsPreviewBonusLine = layoutPolicyState.applyLyricsPreviewBonusLine,
                        onOpenLyricsNavigator = { isLyricsNavigatorVisible = true },
                        onLayoutMeasured = { slice -> displaySlice = slice },
                        onSeekToSample = onSeekToSample,
                        onContainerMeasured = { slice -> scrollSlice = slice },
                    )
                    PlayerDetailBottomDock(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    bottomDockSlice = coordinates.toPlaybackVerticalSlice()
                                },
                        displayedSamples = displayedSamples,
                        totalSamples = totalSamples,
                        isScrubbing = isScrubbing,
                        displayedTime = displayedTime,
                        totalTime = totalTime,
                        isPlaying = isPlaying,
                        playbackSequenceMode = playbackSequenceMode,
                        playbackSpeed = playbackSpeed,
                        canSkipPrevious = canSkipPrevious,
                        canSkipNext = canSkipNext,
                        transportMode = miniPlayerModel.transportMode,
                        durationMs = miniPlayerModel.durationMs,
                        sampleRateHz = sampleRateHz,
                        frameSamples = frameSamples,
                        wavAudioInfo = wavAudioInfo,
                        flashSignalInfo = flashSignalInfo,
                        flashVoicingStyle = miniPlayerModel.flashVoicingStyle,
                        savedAudioItem = savedAudioItem,
                        onTogglePlayback = onTogglePlayback,
                        onSkipToPreviousTrack = onSkipToPreviousTrack,
                        onSkipToNextTrack = onSkipToNextTrack,
                        onPlaybackSequenceModeSelected = onPlaybackSequenceModeSelected,
                        onPlaybackSpeedSelected = onPlaybackSpeedSelected,
                        bottomActions = resolvedBottomActions,
                        onScrubStarted = onScrubStarted,
                        onScrubChanged = onScrubChanged,
                        onScrubFinished = onScrubFinished,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerDetailScrollContent(
    displayedSamples: Int,
    waveformDisplayedSamples: Int,
    waveformPcm: ShortArray,
    isWaveformPreview: Boolean,
    sampleRateHz: Int,
    frameSamples: Int,
    transportMode: TransportModeOption,
    isFlashMode: Boolean,
    flashVoicingStyle: FlashVoicingStyleOption?,
    followData: PayloadFollowViewData,
    playbackDetailsSource: String,
    decodedTextStatusCode: Int = BagDecodeContentCodes.STATUS_UNAVAILABLE,
    flashVisualWindow: FlashVisualWindowState,
    isPlaying: Boolean,
    isScrubbing: Boolean,
    isFlashVisualPerfOverlayEnabled: Boolean,
    playbackSpeed: Float,
    displaySectionState: PlaybackDisplaySectionState,
    initialFollowViewMode: PlaybackFollowViewMode,
    savedAudioItem: SavedAudioItem?,
    showSavedAudioDecodeLoadingNotice: Boolean,
    savedAudioDecodeProgressSnapshot: DecodeOperationSnapshot?,
    topContentPadding: Dp,
    extraLyricsRecoveryHeight: Dp,
    applyLyricsPreviewBonusLine: Boolean,
    onOpenLyricsNavigator: () -> Unit,
    onContainerMeasured: (PlaybackVerticalSlice) -> Unit,
    onLayoutMeasured: (PlaybackVerticalSlice) -> Unit,
    onSeekToSample: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .onGloballyPositioned { coordinates ->
                    onContainerMeasured(coordinates.toPlaybackVerticalSlice())
                }.padding(
                    start = PlayerDetailHorizontalPadding,
                    top = topContentPadding + 8.dp,
                    end = PlayerDetailHorizontalPadding,
                    bottom = 8.dp,
                ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (savedAudioItem != null && showSavedAudioDecodeLoadingNotice) {
            SavedAudioDecodeLoadingNotice(savedAudioDecodeProgressSnapshot)
        }
        AudioPlaybackDisplayBlock(
            displayedSamples = displayedSamples,
            visualDisplayedSamples = waveformDisplayedSamples,
            waveformPcm = waveformPcm,
            isWaveformPreview = isWaveformPreview,
            sampleRateHz = sampleRateHz,
            transportMode = transportMode,
            frameSamples = frameSamples,
            isFlashMode = isFlashMode,
            flashVoicingStyle = flashVoicingStyle,
            followData = followData,
            playbackDetailsSource = playbackDetailsSource,
            decodedTextStatusCode = decodedTextStatusCode,
            flashVisualWindow = flashVisualWindow,
            isPlaying = isPlaying,
            isScrubbing = isScrubbing,
            isFlashVisualPerfOverlayEnabled = isFlashVisualPerfOverlayEnabled,
            playbackSpeed = playbackSpeed,
            displaySectionState = displaySectionState,
            initialFollowViewMode = initialFollowViewMode,
            extraLyricsRecoveryHeight = extraLyricsRecoveryHeight,
            applyLyricsPreviewBonusLine = applyLyricsPreviewBonusLine,
            onOpenLyricsNavigator = onOpenLyricsNavigator,
            modifier =
                Modifier.onGloballyPositioned { coordinates ->
                    onLayoutMeasured(coordinates.toPlaybackVerticalSlice())
                },
            onSeekToSample = onSeekToSample,
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
internal fun LyricsNavigatorScaffold(
    followData: PayloadFollowViewData,
    navigatorReadingModel: LyricsNavigatorReadingModel? = null,
    displayedSamples: Int,
    totalSamples: Int,
    displayedTime: String,
    totalTime: String,
    isPlaying: Boolean,
    isScrubbing: Boolean,
    playbackSequenceMode: PlaybackSequenceMode,
    playbackSpeed: Float,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    transportMode: TransportModeOption,
    durationMs: Long,
    sampleRateHz: Int,
    frameSamples: Int,
    wavAudioInfo: WavAudioInfo,
    onBack: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSkipToPreviousTrack: () -> Unit,
    onSkipToNextTrack: () -> Unit,
    onPlaybackSequenceModeSelected: (PlaybackSequenceMode) -> Unit,
    onPlaybackSpeedSelected: (Float) -> Unit,
    onScrubStarted: () -> Unit,
    onScrubChanged: (Int) -> Unit,
    onScrubFinished: () -> Unit,
    onSeekToSample: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val readingModel =
        remember(
            navigatorReadingModel,
            followData.textCharacters,
            followData.lyricLines,
            followData.lineTokenRanges,
            followData.textTokenTimeline,
            followData.textTokens,
        ) {
            navigatorReadingModel ?: buildLyricsNavigatorReadingModel(followData)
        }
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = PlayerDetailHorizontalPadding, vertical = 16.dp)
                .testTag("lyrics-navigator-screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("lyrics-navigator-back"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.audio_lyrics_navigator_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.audio_lyrics_navigator_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = if (isPlaying) "$displayedTime / $totalTime" else totalTime,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 1.dp,
        ) {
            LyricsNavigatorReadingText(
                readingModel = readingModel,
                displayedSamples = displayedSamples,
                sampleRateHz = sampleRateHz,
                onSeekToSample = onSeekToSample,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 16.dp),
            )
        }
        LyricsNavigatorBottomDock(
            modifier = Modifier.fillMaxWidth(),
            displayedSamples = displayedSamples,
            totalSamples = totalSamples,
            isScrubbing = isScrubbing,
            displayedTime = displayedTime,
            totalTime = totalTime,
            isPlaying = isPlaying,
            canSkipPrevious = canSkipPrevious,
            canSkipNext = canSkipNext,
            transportMode = transportMode,
            durationMs = durationMs,
            sampleRateHz = sampleRateHz,
            frameSamples = frameSamples,
            wavAudioInfo = wavAudioInfo,
            onTogglePlayback = onTogglePlayback,
            onSkipToPreviousTrack = onSkipToPreviousTrack,
            onSkipToNextTrack = onSkipToNextTrack,
            onScrubStarted = onScrubStarted,
            onScrubChanged = onScrubChanged,
            onScrubFinished = onScrubFinished,
        )
    }
}

@Composable
private fun LyricsNavigatorReadingText(
    readingModel: LyricsNavigatorReadingModel,
    displayedSamples: Int,
    sampleRateHz: Int,
    onSeekToSample: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseStyle = MaterialTheme.typography.bodyMedium
    val formattedModel = remember(readingModel) { formatLyricsNavigatorReadingModelForDisplay(readingModel) }
    val textMeasurer = rememberTextMeasurer()
    val latestOnSeekToSample by rememberUpdatedState(onSeekToSample)
    val scope = rememberCoroutineScope()
    BoxWithConstraints(
        modifier = modifier.testTag("lyrics-navigator-reading-text"),
    ) {
        val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
        val lines =
            remember(formattedModel, widthPx, baseStyle, textMeasurer) {
                if (widthPx <= 0) {
                    emptyList()
                } else {
                    buildLyricsNavigatorDisplayLines(
                        readingModel = formattedModel,
                        textMeasurer = textMeasurer,
                        style = baseStyle,
                        maxWidthPx = widthPx,
                    )
                }
            }
        val activeTokenRange =
            remember(formattedModel, displayedSamples) {
                resolveActiveTokenCharacterRange(
                    readingModel = formattedModel,
                    displayedSamples = displayedSamples,
                )
            }
        val listState =
            rememberLazyListState(
                initialFirstVisibleItemIndex =
                    resolveNavigatorInitialLineIndex(lines).coerceAtLeast(0),
            )
        var isSelectingLine by remember { mutableStateOf(false) }
        LaunchedEffect(listState) {
            snapshotFlow { listState.isScrollInProgress }
                .map { scrolling -> scrolling }
                .distinctUntilChanged()
                .collectLatest { scrolling ->
                    isSelectingLine = scrolling
                }
        }
        val selectedLineIndex by remember(listState, lines) {
            derivedStateOf {
                centeredNavigatorLineIndex(
                    listState = listState,
                    fallbackIndex = 0,
                    lineCount = lines.size,
                )
            }
        }
        val selectedLine = lines.getOrNull(selectedLineIndex)
        val selectedSample = selectedLine?.startSample
        val selectorPadding =
            ((maxHeight - LyricsNavigatorSelectionTargetHeight) / 2).coerceAtLeast(0.dp)
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(lines) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                isSelectingLine = true
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                scope.launch {
                                    listState.scrollBy(-dragAmount)
                                }
                            },
                            onDragEnd = {
                                selectedSample?.let(latestOnSeekToSample)
                                isSelectingLine = false
                            },
                            onDragCancel = {
                                isSelectingLine = false
                            },
                        )
                    },
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = selectorPadding),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(lines) { index, line ->
                    val selectedDistance = abs(index - selectedLineIndex)
                    val isSelected = index == selectedLineIndex
                    val lineColor =
                        when (selectedDistance) {
                            0 -> MaterialTheme.colorScheme.onSurface
                            1 -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f)
                            2 -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.56f)
                        }
                    Text(
                        text =
                            buildLyricsNavigatorLineAnnotatedText(
                                line = line,
                                activeTokenRange = activeTokenRange,
                                activeTokenColor = MaterialTheme.colorScheme.secondary,
                            ),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable(enabled = line.startSample != null) {
                                    scope.launch {
                                        listState.animateScrollToItem(index)
                                        line.startSample?.let(latestOnSeekToSample)
                                    }
                                },
                        style =
                            baseStyle.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            ),
                        color = lineColor,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
            if (isSelectingLine) {
                LyricsNavigatorSelectionGuide(
                    selectedStartSample = selectedSample,
                    sampleRateHz = sampleRateHz,
                    lineColor = lyricsNavigatorSelectionLineColor(),
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(),
                )
            }
        }
    }
}

private data class LyricsNavigatorDisplayLine(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
    val startSample: Int?,
)

private val LyricsNavigatorSelectionTargetHeight = 48.dp
private val LyricsNavigatorSelectionLineWidth = 132.dp

private fun buildLyricsNavigatorDisplayLines(
    readingModel: LyricsNavigatorReadingModel,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    style: androidx.compose.ui.text.TextStyle,
    maxWidthPx: Int,
): List<LyricsNavigatorDisplayLine> {
    val layout =
        textMeasurer.measure(
            text = AnnotatedString(readingModel.text),
            style = style,
            constraints = Constraints(maxWidth = maxWidthPx),
        )
    val lines = ArrayList<LyricsNavigatorDisplayLine>(layout.lineCount)
    repeat(layout.lineCount) { lineIndex ->
        val lineStart = layout.getLineStart(lineIndex)
        val lineEnd = layout.getLineEnd(lineIndex, visibleEnd = true)
        if (lineStart >= lineEnd) {
            return@repeat
        }
        val text = readingModel.text.substring(lineStart, lineEnd).trimEnd('\n', '\r')
        if (text.isEmpty()) {
            return@repeat
        }
        lines +=
            LyricsNavigatorDisplayLine(
                text = text,
                startOffset = lineStart,
                endOffset = lineEnd,
                startSample =
                    resolveSeekSampleForReadingLine(
                        text = readingModel.text,
                        sampleAtOffset = readingModel.sampleAtOffset,
                        lineStart = lineStart,
                        lineEnd = lineEnd,
                    ),
            )
    }
    return lines
}

internal fun formatLyricsNavigatorReadingModelForDisplay(readingModel: LyricsNavigatorReadingModel): LyricsNavigatorReadingModel {
    if (readingModel.text.isEmpty()) {
        return readingModel
    }
    val textBuilder = StringBuilder(readingModel.text.length + 16)
    val samples = ArrayList<Int>(readingModel.sampleAtOffset.size + 16)
    var runLength = 0
    readingModel.text.forEachIndexed { index, ch ->
        textBuilder.append(ch)
        samples += readingModel.sampleAtOffset.getOrElse(index) { -1 }
        when {
            ch == '\n' || ch == '\r' -> runLength = 0
            ch.isWhitespace() -> Unit
            else -> runLength += 1
        }
        val nextChar = readingModel.text.getOrNull(index + 1)
        val shouldBreak =
            when {
                nextChar == null -> false
                nextChar == '\n' || nextChar == '\r' -> false
                ch in StrongPunctuationBreaks -> true
                ch in SoftPunctuationBreaks && runLength >= SoftBreakMinRunLength -> true
                else -> false
            }
        if (shouldBreak) {
            textBuilder.append('\n')
            samples += -1
            runLength = 0
        }
    }
    return LyricsNavigatorReadingModel(
        text = textBuilder.toString(),
        sampleAtOffset = samples.toIntArray(),
    )
}

private fun centeredNavigatorLineIndex(
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
    return centeredItem?.index?.coerceIn(0, lineCount - 1) ?: fallbackIndex.coerceIn(0, lineCount - 1)
}

private fun resolveNavigatorInitialLineIndex(lines: List<LyricsNavigatorDisplayLine>): Int =
    lines.indexOfFirst { it.startSample != null }.coerceAtLeast(0)

private fun resolveActiveTokenCharacterRange(
    readingModel: LyricsNavigatorReadingModel,
    displayedSamples: Int,
): IntRange? {
    if (readingModel.text.isEmpty() || readingModel.sampleAtOffset.isEmpty()) {
        return null
    }
    val activeOffset =
        readingModel.sampleAtOffset.indexOfLast { sample ->
            sample >= 0 && sample <= displayedSamples
        }
    if (activeOffset < 0 || activeOffset >= readingModel.text.length) {
        return null
    }
    val activeChar = readingModel.text[activeOffset]
    if (activeChar.isWhitespace() || activeChar == '\n' || activeChar == '\r') {
        return null
    }
    var start = activeOffset
    while (start > 0) {
        val previous = readingModel.text[start - 1]
        val previousSample = readingModel.sampleAtOffset[start - 1]
        if (previous.isWhitespace() || previous == '\n' || previous == '\r' || previousSample < 0) {
            break
        }
        start -= 1
    }
    var end = activeOffset
    while (end + 1 < readingModel.text.length) {
        val next = readingModel.text[end + 1]
        val nextSample = readingModel.sampleAtOffset[end + 1]
        if (next.isWhitespace() || next == '\n' || next == '\r' || nextSample < 0) {
            break
        }
        end += 1
    }
    return start..end
}

private fun buildLyricsNavigatorLineAnnotatedText(
    line: LyricsNavigatorDisplayLine,
    activeTokenRange: IntRange?,
    activeTokenColor: Color,
): AnnotatedString {
    val overlap =
        activeTokenRange?.let { range ->
            val start = maxOf(line.startOffset, range.first)
            val endExclusive = minOf(line.endOffset, range.last + 1)
            if (start < endExclusive) start until endExclusive else null
        }
    return buildAnnotatedString {
        if (overlap == null) {
            append(line.text)
            return@buildAnnotatedString
        }
        val localStart = (overlap.first - line.startOffset).coerceAtLeast(0)
        val localEndExclusive = (overlap.last - line.startOffset + 1).coerceAtMost(line.text.length)
        append(line.text.substring(0, localStart))
        withStyle(SpanStyle(color = activeTokenColor)) {
            append(line.text.substring(localStart, localEndExclusive))
        }
        append(line.text.substring(localEndExclusive))
    }
}

@Composable
private fun lyricsNavigatorSelectionLineColor(): Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)

@Composable
private fun LyricsNavigatorSelectionGuide(
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
        modifier = modifier.height(LyricsNavigatorSelectionTargetHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(lineColor),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = selectedTime,
            modifier = Modifier.width(48.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val StrongPunctuationBreaks = setOf('。', '！', '？', '；', '：', '.', '!', '?', ';', ':')
private val SoftPunctuationBreaks = setOf('，', '、', ',')
private const val SoftBreakMinRunLength = 12

@Composable
private fun LyricsNavigatorBottomDock(
    displayedSamples: Int,
    totalSamples: Int,
    isScrubbing: Boolean,
    displayedTime: String,
    totalTime: String,
    isPlaying: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    transportMode: TransportModeOption,
    durationMs: Long,
    sampleRateHz: Int,
    frameSamples: Int,
    wavAudioInfo: WavAudioInfo,
    onTogglePlayback: () -> Unit,
    onSkipToPreviousTrack: () -> Unit,
    onSkipToNextTrack: () -> Unit,
    onScrubStarted: () -> Unit,
    onScrubChanged: (Int) -> Unit,
    onScrubFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playerColors = playerChromeColors()
    Column(
        modifier =
            modifier.padding(
                top = 8.dp,
                bottom = 0.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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
        LyricsNavigatorPrimaryControlsRow(
            isPlaying = isPlaying,
            canSkipPrevious = canSkipPrevious,
            canSkipNext = canSkipNext,
            onTogglePlayback = onTogglePlayback,
            onSkipToPreviousTrack = onSkipToPreviousTrack,
            onSkipToNextTrack = onSkipToNextTrack,
            colors = playerColors,
        )
    }
}

@Composable
private fun LyricsNavigatorPrimaryControlsRow(
    isPlaying: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    onTogglePlayback: () -> Unit,
    onSkipToPreviousTrack: () -> Unit,
    onSkipToNextTrack: () -> Unit,
    colors: PlayerChromeColors,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp, androidx.compose.ui.Alignment.CenterHorizontally),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        LyricsNavigatorIconButton(
            onClick = onSkipToPreviousTrack,
            enabled = canSkipPrevious,
            contentDescription = stringResource(R.string.audio_action_previous_track),
            contentColor = colors.neutralAction,
            disabledContentColor = colors.disabledAction,
            icon = Icons.Rounded.SkipPrevious,
            iconSize = 34.dp,
        )
        LyricsNavigatorIconButton(
            onClick = onTogglePlayback,
            contentDescription =
                stringResource(
                    if (isPlaying) {
                        R.string.audio_action_pause
                    } else {
                        R.string.audio_action_play
                    },
                ),
            contentColor = colors.accent,
            icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            buttonSize = 56.dp,
            iconSize = 40.dp,
        )
        LyricsNavigatorIconButton(
            onClick = onSkipToNextTrack,
            enabled = canSkipNext,
            contentDescription = stringResource(R.string.audio_action_next_track),
            contentColor = colors.neutralAction,
            disabledContentColor = colors.disabledAction,
            icon = Icons.Rounded.SkipNext,
            iconSize = 34.dp,
        )
    }
}

@Composable
private fun LyricsNavigatorIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    disabledContentColor: androidx.compose.ui.graphics.Color = contentColor.copy(alpha = 0.38f),
    buttonSize: Dp = 40.dp,
    iconSize: Dp = 28.dp,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier
                .size(buttonSize)
                .semantics { this.contentDescription = contentDescription },
        colors =
            IconButtonDefaults.iconButtonColors(
                contentColor = contentColor,
                disabledContentColor = disabledContentColor,
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun SavedAudioDecodeLoadingNotice(progressSnapshot: DecodeOperationSnapshot?) {
    val progress = progressSnapshot?.overallProgress0To1?.coerceIn(0f, 1f) ?: 0f
    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                text = stringResource(R.string.audio_saved_audio_decode_loading_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.audio_saved_audio_decode_loading_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlayerDetailBottomDock(
    displayedSamples: Int,
    totalSamples: Int,
    isScrubbing: Boolean,
    displayedTime: String,
    totalTime: String,
    isPlaying: Boolean,
    playbackSequenceMode: PlaybackSequenceMode,
    playbackSpeed: Float,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    transportMode: TransportModeOption,
    durationMs: Long,
    sampleRateHz: Int,
    frameSamples: Int,
    wavAudioInfo: WavAudioInfo,
    flashSignalInfo: FlashSignalInfo,
    flashVoicingStyle: FlashVoicingStyleOption?,
    savedAudioItem: SavedAudioItem?,
    onTogglePlayback: () -> Unit,
    onSkipToPreviousTrack: () -> Unit,
    onSkipToNextTrack: () -> Unit,
    onPlaybackSequenceModeSelected: (PlaybackSequenceMode) -> Unit,
    onPlaybackSpeedSelected: (Float) -> Unit,
    bottomActions: PlayerDetailBottomActions,
    onScrubStarted: () -> Unit,
    onScrubChanged: (Int) -> Unit,
    onScrubFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    Column(
        modifier =
            modifier
                .padding(
                    start = PlayerDetailHorizontalPadding,
                    top = 8.dp,
                    end = PlayerDetailHorizontalPadding,
                    bottom = 0.dp,
                ).onGloballyPositioned { coordinates ->
                    debugPlayerBottomDockLayout(
                        "dock",
                        "heightDp=${coordinates.toPlaybackVerticalSlice().heightDp(density)} " +
                            "paddingTopDp=8.0 paddingBottomDp=0.0 " +
                            "horizontalPaddingDp=${PlayerDetailHorizontalPadding.value} " +
                            "columnSpacingDp=8.0",
                    )
                },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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
        AudioPlaybackTransportControls(
            isPlaying = isPlaying,
            playbackSequenceMode = playbackSequenceMode,
            playbackSpeed = playbackSpeed,
            canSkipPrevious = canSkipPrevious,
            canSkipNext = canSkipNext,
            transportMode = transportMode,
            durationMs = durationMs,
            totalSamples = totalSamples,
            sampleRateHz = sampleRateHz,
            frameSamples = frameSamples,
            wavAudioInfo = wavAudioInfo,
            flashSignalInfo = flashSignalInfo,
            flashVoicingStyle = flashVoicingStyle,
            savedAudioItem = savedAudioItem,
            onTogglePlayback = onTogglePlayback,
            onSkipToPreviousTrack = onSkipToPreviousTrack,
            onSkipToNextTrack = onSkipToNextTrack,
            onPlaybackSequenceModeSelected = onPlaybackSequenceModeSelected,
            onPlaybackSpeedSelected = onPlaybackSpeedSelected,
            bottomActions = bottomActions,
        )
    }
}

private fun debugPlayerBottomDockLayout(
    label: String,
    message: String,
) {
    if (!BuildConfig.DEBUG) {
        return
    }
    try {
        android.util.Log.d("PlayerBottomDockLayout", "$label $message")
    } catch (_: RuntimeException) {
    }
}

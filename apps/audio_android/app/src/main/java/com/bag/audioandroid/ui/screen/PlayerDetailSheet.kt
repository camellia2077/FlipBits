package com.bag.audioandroid.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.BuildConfig
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.FlashSignalInfo
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.domain.WavAudioInfo
import com.bag.audioandroid.ui.PlayerChromeColors
import com.bag.audioandroid.ui.PlayerDetailBottomActions
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.MiniPlayerUiModel
import com.bag.audioandroid.ui.model.PlaybackSequenceMode
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.playerChromeColors
import com.bag.audioandroid.ui.state.FlashVisualWindowState

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
    flashVisualWindow: FlashVisualWindowState = FlashVisualWindowState(),
    savedAudioItem: SavedAudioItem?,
    showSavedAudioDecodeLoadingNotice: Boolean = false,
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
                        flashVisualWindow = flashVisualWindow,
                        isPlaying = isPlaying,
                        isScrubbing = isScrubbing,
                        isFlashVisualPerfOverlayEnabled = isFlashVisualPerfOverlayEnabled,
                        playbackSpeed = playbackSpeed,
                        displaySectionState = displaySectionState,
                        initialFollowViewMode = initialFollowViewMode,
                        savedAudioItem = savedAudioItem,
                        showSavedAudioDecodeLoadingNotice = showSavedAudioDecodeLoadingNotice,
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
    flashVisualWindow: FlashVisualWindowState,
    isPlaying: Boolean,
    isScrubbing: Boolean,
    isFlashVisualPerfOverlayEnabled: Boolean,
    playbackSpeed: Float,
    displaySectionState: PlaybackDisplaySectionState,
    initialFollowViewMode: PlaybackFollowViewMode,
    savedAudioItem: SavedAudioItem?,
    showSavedAudioDecodeLoadingNotice: Boolean,
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
            SavedAudioDecodeLoadingNotice()
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
    val layoutModel =
        rememberPlaybackLyricsLayoutModel(
            followData = followData,
            displayedSamples = displayedSamples,
            transportMode = null,
            playbackDisplayMode = PlaybackDisplayMode.Lyrics,
            tokenStripHeightDp = null,
            extraLyricsRecoveryHeight = 0.dp,
            applyLyricsPreviewBonusLine = false,
            lyricsExpanded = true,
        )
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
            PlaybackLyricsFullList(
                followData = followData,
                displayLineRanges = layoutModel.displayLineRanges,
                activeLineIndex = layoutModel.activeLineIndex,
                sampleRateHz = sampleRateHz,
                onSeekToSample = onSeekToSample,
                useFixedHeight = false,
                showSelectionGuideOnlyWhileScrolling = true,
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
private fun SavedAudioDecodeLoadingNotice() {
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

package com.bag.audioandroid.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.DecodeOperationSnapshot
import com.bag.audioandroid.domain.DecodedPayloadViewData
import com.bag.audioandroid.domain.FlashSignalInfo
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.domain.WavAudioInfo
import com.bag.audioandroid.ui.model.MiniPlayerUiModel
import com.bag.audioandroid.ui.model.PlaybackSequenceMode
import com.bag.audioandroid.ui.model.SavedAudioModeFilter
import com.bag.audioandroid.ui.screen.DebugMorseVisualizationModeRequest
import com.bag.audioandroid.ui.screen.DebugPlaybackDisplayModeRequest
import com.bag.audioandroid.ui.screen.FlashSignalVisualizationMode
import com.bag.audioandroid.ui.screen.PlaybackDisplayMode
import com.bag.audioandroid.ui.screen.PlaybackFollowViewMode
import com.bag.audioandroid.ui.screen.PlayerDetailSheetContent
import com.bag.audioandroid.ui.state.FlashVisualWindowState
import com.bag.audioandroid.ui.state.QueueSheetValue
import com.bag.audioandroid.ui.theme.appThemeVisualTokens
import com.bag.audioandroid.ui.utilityActionIconButtonColors

@Composable
internal fun PlayerSurfaceHost(
    miniPlayerModel: MiniPlayerUiModel,
    topBarActions: PlayerDetailTopBarActions,
    bottomActions: PlayerDetailBottomActions,
    displayedSamples: Int,
    waveformDisplayedSamples: Int,
    totalSamples: Int,
    isScrubbing: Boolean,
    waveformPcm: ShortArray,
    isWaveformPreview: Boolean,
    sampleRateHz: Int,
    frameSamples: Int,
    wavAudioInfo: WavAudioInfo,
    flashSignalInfo: FlashSignalInfo,
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
    lyricsNavigatorReadingModel: LyricsNavigatorReadingModel?,
    flashVisualWindow: FlashVisualWindowState,
    savedAudioItem: SavedAudioItem?,
    showSavedAudioDecodeLoadingNotice: Boolean,
    savedAudioDecodeProgressSnapshot: DecodeOperationSnapshot? = null,
    isFlashVisualPerfOverlayEnabled: Boolean,
    showQueueSheet: Boolean,
    queueSheetValue: QueueSheetValue,
    savedAudioItems: List<SavedAudioItem>,
    savedAudioFilter: SavedAudioModeFilter,
    currentSavedAudioItemId: String?,
    playerDetailSnackbarHostState: SnackbarHostState,
    onTogglePlayback: () -> Unit,
    onSkipToPreviousTrack: () -> Unit,
    onSkipToNextTrack: () -> Unit,
    onPlaybackSequenceModeSelected: (PlaybackSequenceMode) -> Unit,
    onPlaybackSpeedSelected: (Float) -> Unit,
    onCloseSavedAudioSheet: () -> Unit,
    onQueueValueChanged: (QueueSheetValue) -> Unit,
    onSavedAudioFilterChange: (SavedAudioModeFilter) -> Unit,
    onSavedAudioSelected: (String) -> Unit,
    onScrubStarted: () -> Unit,
    onScrubChanged: (Int) -> Unit,
    onScrubFinished: () -> Unit,
    onLyricsRequested: () -> Unit,
    onPlaybackDisplayModeSelected: (PlaybackDisplayMode) -> Unit,
    debugExpandLyricsRequestId: Long?,
    onDebugExpandLyricsHandled: (Long) -> Unit,
    debugPlaybackDisplayModeRequest: DebugPlaybackDisplayModeRequest?,
    onDebugPlaybackDisplayModeHandled: (Long) -> Unit,
    debugMorseVisualizationModeRequest: DebugMorseVisualizationModeRequest?,
    onDebugMorseVisualizationModeHandled: (Long) -> Unit,
    initialFollowViewMode: PlaybackFollowViewMode,
    initialFlashVisualizationMode: FlashSignalVisualizationMode?,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        var isLyricsNavigatorVisible by remember { mutableStateOf(false) }
        BackHandler(enabled = !isLyricsNavigatorVisible) {
            if (showQueueSheet) {
                onCloseSavedAudioSheet()
            } else {
                topBarActions.onCollapse()
            }
        }
        val collapseChromeContentPadding = 52.dp
        val showExpandedPlayerChrome = !isLyricsNavigatorVisible
        val detailTopContentPadding =
            if (showExpandedPlayerChrome) {
                collapseChromeContentPadding
            } else {
                0.dp
            }
        Surface(
            color = appThemeVisualTokens().modalContainerColor,
            contentColor = appThemeVisualTokens().modalContentColor,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier =
                Modifier
                    .fillMaxSize(),
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = {
                    SnackbarHost(hostState = playerDetailSnackbarHostState)
                },
            ) { sheetInnerPadding ->
                Box(modifier = Modifier.fillMaxSize()) {
                    PlayerDetailSheetContent(
                        miniPlayerModel = miniPlayerModel,
                        displayedSamples = displayedSamples,
                        waveformDisplayedSamples = waveformDisplayedSamples,
                        totalSamples = totalSamples,
                        isScrubbing = isScrubbing,
                        waveformPcm = waveformPcm,
                        isWaveformPreview = isWaveformPreview,
                        sampleRateHz = sampleRateHz,
                        frameSamples = frameSamples,
                        wavAudioInfo = wavAudioInfo,
                        flashSignalInfo = flashSignalInfo,
                        displayedTime = displayedTime,
                        totalTime = totalTime,
                        isPlaying = isPlaying,
                        playbackSequenceMode = playbackSequenceMode,
                        playbackSpeed = playbackSpeed,
                        canSkipPrevious = canSkipPrevious,
                        canSkipNext = canSkipNext,
                        canExportGeneratedAudio = canExportGeneratedAudio,
                        followData = followData,
                        playbackDetailsSource = playbackDetailsSource,
                        decodedPayload = decodedPayload,
                        lyricsNavigatorReadingModel = lyricsNavigatorReadingModel,
                        flashVisualWindow = flashVisualWindow,
                        savedAudioItem = savedAudioItem,
                        showSavedAudioDecodeLoadingNotice = showSavedAudioDecodeLoadingNotice,
                        savedAudioDecodeProgressSnapshot = savedAudioDecodeProgressSnapshot,
                        isFlashVisualPerfOverlayEnabled = isFlashVisualPerfOverlayEnabled,
                        onTogglePlayback = onTogglePlayback,
                        onSkipToPreviousTrack = onSkipToPreviousTrack,
                        onSkipToNextTrack = onSkipToNextTrack,
                        onPlaybackSequenceModeSelected = onPlaybackSequenceModeSelected,
                        onPlaybackSpeedSelected = onPlaybackSpeedSelected,
                        bottomActions = bottomActions,
                        onScrubStarted = onScrubStarted,
                        onScrubChanged = onScrubChanged,
                        onScrubFinished = onScrubFinished,
                        onLyricsRequested = onLyricsRequested,
                        onPlaybackDisplayModeSelected = onPlaybackDisplayModeSelected,
                        debugExpandLyricsRequestId = debugExpandLyricsRequestId,
                        onDebugExpandLyricsHandled = onDebugExpandLyricsHandled,
                        debugPlaybackDisplayModeRequest = debugPlaybackDisplayModeRequest,
                        onDebugPlaybackDisplayModeHandled = onDebugPlaybackDisplayModeHandled,
                        debugMorseVisualizationModeRequest = debugMorseVisualizationModeRequest,
                        onDebugMorseVisualizationModeHandled = onDebugMorseVisualizationModeHandled,
                        initialFollowViewMode = initialFollowViewMode,
                        initialFlashVisualizationMode = initialFlashVisualizationMode,
                        onLyricsNavigatorVisibilityChanged = { isLyricsNavigatorVisible = it },
                        topContentPadding = detailTopContentPadding,
                        modifier = Modifier.padding(sheetInnerPadding),
                    )
                    if (showExpandedPlayerChrome) {
                        Box(
                            modifier =
                                Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(start = 8.dp, top = 4.dp, end = 8.dp),
                        ) {
                            IconButton(
                                onClick = topBarActions.onCollapse,
                                colors = utilityActionIconButtonColors(),
                                modifier = Modifier.align(Alignment.CenterStart),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = stringResource(R.string.audio_action_collapse_player_detail),
                                )
                            }
                            Text(
                                text = stringResource(topBarActions.modeLabelResId),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.align(Alignment.Center),
                            )
                            Row(
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.align(Alignment.CenterEnd),
                            ) {
                                topBarActions.onShareAudio?.let { shareAudio ->
                                    IconButton(
                                        onClick = shareAudio,
                                        colors = utilityActionIconButtonColors(),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Share,
                                            contentDescription = stringResource(R.string.library_action_share),
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                topBarActions.onDownloadToDevice?.let { downloadToDevice ->
                                    IconButton(
                                        onClick = downloadToDevice,
                                        colors = utilityActionIconButtonColors(),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.SaveAlt,
                                            contentDescription = stringResource(R.string.audio_action_export_to_file),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (showQueueSheet) {
                        PlayerQueueSheetHost(
                            savedAudioItems = savedAudioItems,
                            savedAudioFilter = savedAudioFilter,
                            currentSavedAudioItemId = currentSavedAudioItemId,
                            initialQueueValue = queueSheetValue,
                            onCloseQueue = onCloseSavedAudioSheet,
                            onQueueValueChanged = onQueueValueChanged,
                            onSavedAudioFilterChange = onSavedAudioFilterChange,
                            onSavedAudioSelected = onSavedAudioSelected,
                        )
                    }
                }
            }
        }
    }
}

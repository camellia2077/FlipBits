package com.bag.audioandroid.ui

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.BuildConfig
import com.bag.audioandroid.ui.model.AppTab
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.MiniPlayerSource
import com.bag.audioandroid.ui.model.SavedAudioModeFilter
import com.bag.audioandroid.ui.model.asString
import com.bag.audioandroid.ui.screen.AudioTabScreen
import com.bag.audioandroid.ui.screen.ConfigTabScreen
import com.bag.audioandroid.ui.screen.DebugMorseVisualizationModeRequest
import com.bag.audioandroid.ui.screen.DebugPlaybackDisplayModeRequest
import com.bag.audioandroid.ui.screen.LibraryTabScreen
import com.bag.audioandroid.ui.screen.MiniPlayerBar
import com.bag.audioandroid.ui.screen.PlaybackFollowViewMode
import com.bag.audioandroid.ui.screen.SavedAudioPickerSheet
import com.bag.audioandroid.ui.screen.formatDurationMillis
import com.bag.audioandroid.ui.screen.samplesToMillis
import com.bag.audioandroid.ui.screen.toEncodeProgressDisplayModel
import com.bag.audioandroid.ui.state.AudioAppUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun AudioAndroidMainScaffold(
    uiState: AudioAppUiState,
    savedAudioFilter: SavedAudioModeFilter,
    onSavedAudioFilterChange: (SavedAudioModeFilter) -> Unit,
    accentTokens: com.bag.audioandroid.ui.theme.AppThemeAccentTokens,
    materialPalettes: List<com.bag.audioandroid.ui.model.PaletteOption>,
    factionThemes: List<com.bag.audioandroid.ui.model.FactionThemeOption>,
    onImportAudio: () -> Unit,
    viewModel: AudioAndroidViewModel,
    debugScenario: FlashDebugScenario? = null,
    debugExpandLyricsRequestId: Long? = null,
    onDebugExpandLyricsHandled: (Long) -> Unit = {},
    debugPlaybackDisplayModeRequest: DebugPlaybackDisplayModeRequest? = null,
    onDebugPlaybackDisplayModeHandled: (Long) -> Unit = {},
    debugMorseVisualizationModeRequest: DebugMorseVisualizationModeRequest? = null,
    onDebugMorseVisualizationModeHandled: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val currentSession = uiState.currentSession
    val currentPlayback = uiState.currentPlayback
    val miniPlayerModel = uiState.miniPlayerModel
    val snackbarHostState = remember { SnackbarHostState() }
    val playerDetailSnackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage = uiState.snackbarMessage
    val snackbarText = snackbarMessage?.text?.asString().orEmpty()
    val displayedTime =
        formatDurationMillis(
            samplesToMillis(currentPlayback.displayedSamples, currentPlayback.sampleRateHz),
        )
    val totalTime =
        formatDurationMillis(
            samplesToMillis(currentPlayback.totalSamples, currentPlayback.sampleRateHz),
        )
    val playbackVisualData = uiState.currentPlaybackVisualData
    val waveformPcm = playbackVisualData.samples
    val waveformDisplayedSamples = playbackVisualData.displayedSamplesFor(currentPlayback.displayedSamples)
    val navigationBarColors = navigationBarItemColors(uiState)
    val demoTouchStrokeColor =
        if (uiState.selectedThemeStyle == com.bag.audioandroid.ui.model.ThemeStyleOption.FactionTheme) {
            uiState.activeFactionTheme.secondaryColor
        } else {
            androidx.compose.material3.MaterialTheme.colorScheme.primary
        }
    val demoTouchFillColor =
        if (uiState.selectedThemeStyle == com.bag.audioandroid.ui.model.ThemeStyleOption.FactionTheme) {
            val primary = uiState.activeFactionTheme.primaryColor
            if (primary.luminance() < 0.5f) {
                lerp(Color.White, primary, 0.15f)
            } else {
                lerp(Color.Black, primary, 0.15f)
            }
        } else {
            val materialBg = androidx.compose.material3.MaterialTheme.colorScheme.background
            if (materialBg.luminance() < 0.5f) {
                lerp(Color.White, materialBg, 0.12f)
            } else {
                lerp(Color.Black, materialBg, 0.12f)
            }
        }

    LaunchedEffect(snackbarMessage?.id) {
        val message = snackbarMessage ?: return@LaunchedEffect
        val hostState =
            if (uiState.isExpandedPlayerVisible) {
                playerDetailSnackbarHostState
            } else {
                snackbarHostState
            }
        val customDurationMillis = message.durationMillis
        if (customDurationMillis != null) {
            hostState.currentSnackbarData?.dismiss()
            val dismissJob =
                launch {
                    delay(customDurationMillis)
                    hostState.currentSnackbarData?.dismiss()
                }
            hostState.showSnackbar(
                message = snackbarText,
                duration = SnackbarDuration.Indefinite,
            )
            dismissJob.cancel()
        } else {
            hostState.showSnackbar(snackbarText)
        }
        viewModel.onSnackbarMessageShown(message.id)
    }

    LaunchedEffect(
        uiState.isExpandedPlayerVisible,
        uiState.currentPlaybackSource,
        uiState.selectedSavedAudio?.item?.itemId,
        uiState.selectedSavedAudio?.isLoadingContent,
        uiState.selectedSavedAudio?.needsDecodedContent,
        uiState.selectedSavedAudio?.isDecodingContent,
        uiState.currentPlaybackDecodedPayload?.textDecodeStatusCode,
        uiState.currentPlaybackFollowData.followAvailable,
        uiState.currentPlaybackFollowData.textFollowAvailable,
        uiState.currentPlaybackFollowData.textTokens.size,
        uiState.currentPlaybackFollowData.binaryGroupTimeline.size,
    ) {
        try {
            Log.e(
                "SavedAudioDecodeProgress",
                "mainScaffoldDecodeEffect expanded=${uiState.isExpandedPlayerVisible} source=${uiState.currentPlaybackSource} " +
                    "textStatus=${uiState.currentPlaybackDecodedPayload?.textDecodeStatusCode} " +
                    "followAvailable=${uiState.currentPlaybackFollowData.followAvailable} " +
                    "textFollowAvailable=${uiState.currentPlaybackFollowData.textFollowAvailable} " +
                    "textTokens=${uiState.currentPlaybackFollowData.textTokens.size} " +
                    "binaryGroups=${uiState.currentPlaybackFollowData.binaryGroupTimeline.size} " +
                    "selectedSaved=${uiState.selectedSavedAudio?.item?.itemId.orEmpty()} " +
                    "loading=${uiState.selectedSavedAudio?.isLoadingContent} decoding=${uiState.selectedSavedAudio?.isDecodingContent} " +
                    "needsDecoded=${uiState.selectedSavedAudio?.needsDecodedContent}",
            )
        } catch (_: RuntimeException) {
            // Plain JVM unit tests use the Android stub jar, where Log.e is not implemented.
        }
        if (uiState.isExpandedPlayerVisible && uiState.currentPlaybackSource is AudioPlaybackSource.Saved) {
            viewModel.ensureCurrentPlaybackDecodedForLyrics()
        }
    }

    LaunchedEffect(
        uiState.isQueueVisible,
        uiState.isExpandedPlayerVisible,
        uiState.miniPlayerModel,
        uiState.currentPlaybackSource,
    ) {
        debugMiniPlayerOverlayLog(
            "state",
            "surface=${uiState.playerShellState.surface.current} queue=${uiState.playerShellState.queue.current} " +
                "savedSheet=${uiState.isQueueVisible} detailSheet=${uiState.isExpandedPlayerVisible} " +
                "miniPlayer=${uiState.miniPlayerModel != null} source=${uiState.currentPlaybackSource}",
        )
    }

    PlayerScaffold(
        modifier = modifier,
        isDemoModeEnabled = uiState.isDemoModeEnabled,
        demoTouchFillColor = demoTouchFillColor,
        demoTouchStrokeColor = demoTouchStrokeColor,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        playerShellOverlay =
            if (uiState.isDockQueueVisible || uiState.isExpandedPlayerVisible) {
                { overlayPadding ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        if (uiState.isDockQueueVisible) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .padding(bottom = overlayPadding.dockOverlayBottomPadding)
                                        .onGloballyPositioned { coordinates ->
                                            val position = coordinates.boundsInRoot().topLeft
                                            debugMiniPlayerOverlayLog(
                                                "dismissLayer",
                                                "x=${position.x.toInt()} y=${position.y.toInt()} " +
                                                    "w=${coordinates.size.width} h=${coordinates.size.height} " +
                                                    "bottomPadding=${overlayPadding.dockOverlayBottomPadding.value}",
                                            )
                                        }.clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = viewModel::onCloseSavedAudioSheet,
                                        ),
                            )
                            SavedAudioPickerSheet(
                                savedAudioItems = uiState.savedAudioItems,
                                selectedFilter = savedAudioFilter,
                                onFilterSelected = onSavedAudioFilterChange,
                                onSavedAudioSelected = viewModel::onShellSavedAudioSelected,
                                currentItemId = uiState.currentSavedAudioItem?.itemId,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            start = 12.dp,
                                            end = 12.dp,
                                            bottom = overlayPadding.dockOverlayBottomPadding,
                                        ).onGloballyPositioned { coordinates ->
                                            val position = coordinates.boundsInRoot().topLeft
                                            debugMiniPlayerOverlayLog(
                                                "savedSheetCard",
                                                "x=${position.x.toInt()} y=${position.y.toInt()} " +
                                                    "w=${coordinates.size.width} h=${coordinates.size.height} " +
                                                    "bottomPadding=${overlayPadding.dockOverlayBottomPadding.value}",
                                            )
                                        },
                            )
                        }

                        if (uiState.isExpandedPlayerVisible) {
                            miniPlayerModel?.let { model ->
                                val isGeneratedPlayback =
                                    model.source == MiniPlayerSource.Generated &&
                                        uiState.currentPlaybackSource is AudioPlaybackSource.Generated
                                val isSavedPlayback =
                                    model.source == MiniPlayerSource.Saved &&
                                        uiState.currentPlaybackSource is AudioPlaybackSource.Saved
                                PlayerSurfaceHost(
                                    miniPlayerModel = model,
                                    topBarActions =
                                        PlayerDetailTopBarActions(
                                            modeLabelResId = model.transportMode.labelResId,
                                            onCollapse = viewModel::onClosePlayerDetailSheet,
                                            onShareAudio =
                                                when {
                                                    isSavedPlayback -> viewModel::onShareCurrentSavedAudio
                                                    isGeneratedPlayback -> viewModel::onShareCurrentGeneratedAudio
                                                    else -> null
                                                },
                                            onDownloadToDevice =
                                                when {
                                                    isSavedPlayback -> viewModel::onRequestExportCurrentSavedAudioToDocument
                                                    isGeneratedPlayback -> viewModel::onRequestExportGeneratedAudioToDocument
                                                    else -> null
                                                },
                                        ),
                                    bottomActions =
                                        PlayerDetailBottomActions(
                                            onOpenSavedAudioSheet = viewModel::onOpenSavedAudioSheetFromPlayerDetail,
                                            onSaveToLibrary =
                                                if (isGeneratedPlayback) {
                                                    viewModel::onExportAudio
                                                } else {
                                                    null
                                                },
                                            isAlreadySavedToLibrary = isSavedPlayback,
                                        ),
                                    displayedSamples = currentPlayback.displayedSamples,
                                    waveformDisplayedSamples = waveformDisplayedSamples,
                                    totalSamples = currentPlayback.totalSamples,
                                    isScrubbing = currentPlayback.isScrubbing,
                                    waveformPcm = waveformPcm,
                                    isWaveformPreview = playbackVisualData.isPreview,
                                    sampleRateHz = currentPlayback.sampleRateHz,
                                    frameSamples = uiState.currentPlaybackFrameSamples,
                                    wavAudioInfo = uiState.currentPlaybackWavAudioInfo,
                                    flashSignalInfo = uiState.currentPlaybackFlashSignalInfo,
                                    displayedTime = displayedTime,
                                    totalTime = totalTime,
                                    isPlaying = currentPlayback.isPlaying,
                                    playbackSequenceMode = uiState.playbackSequenceMode,
                                    playbackSpeed = uiState.currentPlaybackSpeed,
                                    canSkipPrevious = uiState.canSkipPrevious,
                                    canSkipNext = uiState.canSkipNext,
                                    canExportGeneratedAudio = isGeneratedPlayback,
                                    followData = uiState.currentPlaybackFollowData,
                                    playbackDetailsSource = uiState.currentPlaybackDetailsSourceWireName,
                                    decodedPayload = uiState.audioTabDecodedPayload,
                                    lyricsNavigatorReadingModel = uiState.currentPlaybackLyricsNavigatorReadingModel,
                                    flashVisualWindow = uiState.currentPlaybackFlashVisualWindow,
                                    savedAudioItem = uiState.currentSavedAudioItem,
                                    showSavedAudioDecodeLoadingNotice = uiState.showCurrentSavedAudioDecodeLoadingNotice,
                                    savedAudioDecodeProgressSnapshot = uiState.currentSavedAudioDecodeProgressSnapshot,
                                    isFlashVisualPerfOverlayEnabled = uiState.isFlashVisualPerfOverlayEnabled,
                                    showQueueSheet = uiState.isExpandedQueueVisible,
                                    queueSheetValue = uiState.playerShellState.queue.current,
                                    savedAudioItems = uiState.savedAudioItems,
                                    savedAudioFilter = savedAudioFilter,
                                    currentSavedAudioItemId = uiState.currentSavedAudioItem?.itemId,
                                    playerDetailSnackbarHostState = playerDetailSnackbarHostState,
                                    onTogglePlayback = viewModel::onTogglePlayback,
                                    onSkipToPreviousTrack = viewModel::onSkipToPreviousTrack,
                                    onSkipToNextTrack = viewModel::onSkipToNextTrack,
                                    onPlaybackSequenceModeSelected = viewModel::onPlaybackSequenceModeSelected,
                                    onPlaybackSpeedSelected = viewModel::onPlaybackSpeedSelected,
                                    onCloseSavedAudioSheet = viewModel::onCloseSavedAudioSheet,
                                    onQueueValueChanged = viewModel::onQueueSheetValueChanged,
                                    onSavedAudioFilterChange = onSavedAudioFilterChange,
                                    onSavedAudioSelected = viewModel::onShellSavedAudioSelected,
                                    onScrubStarted = viewModel::onScrubStarted,
                                    onScrubChanged = viewModel::onScrubChanged,
                                    onScrubFinished = viewModel::onScrubFinished,
                                    onLyricsRequested = viewModel::ensureCurrentPlaybackDecodedForLyrics,
                                    onPlaybackDisplayModeSelected = { mode ->
                                        viewModel.onPlaybackDisplayModeSelected(
                                            mode == com.bag.audioandroid.ui.screen.PlaybackDisplayMode.Lyrics,
                                        )
                                    },
                                    debugExpandLyricsRequestId = debugExpandLyricsRequestId,
                                    onDebugExpandLyricsHandled = onDebugExpandLyricsHandled,
                                    debugPlaybackDisplayModeRequest = debugPlaybackDisplayModeRequest,
                                    onDebugPlaybackDisplayModeHandled = onDebugPlaybackDisplayModeHandled,
                                    debugMorseVisualizationModeRequest = debugMorseVisualizationModeRequest,
                                    onDebugMorseVisualizationModeHandled = onDebugMorseVisualizationModeHandled,
                                    initialFollowViewMode = debugScenario?.followViewMode ?: PlaybackFollowViewMode.Binary,
                                    initialFlashVisualizationMode = debugScenario?.visualMode,
                                )
                            }
                        }
                    }
                }
            } else {
                null
            },
        bottomBar = {
            AudioAndroidBottomBar(
                uiState = uiState,
                navigationBarColors = navigationBarColors,
                onTabSelected = viewModel::onTabSelected,
            )
        },
        miniPlayer =
            uiState.miniPlayerModel?.takeIf { !uiState.isExpandedPlayerVisible }?.let { model ->
                {
                    MiniPlayerBar(
                        model = model,
                        isPlaying = uiState.currentPlayback.isPlaying,
                        onTogglePlayback = viewModel::onTogglePlayback,
                        onOpenSavedAudioSheet = viewModel::onOpenSavedAudioSheetFromDock,
                        onOpenDetails = viewModel::onOpenPlayerDetailSheet,
                        // Keep the mini-player on the same dock container color as the tab bar.
                        // The two pieces should read as one bottom playback system that adapts
                        // automatically when the user switches brand faction themes in config.
                        containerColor = playerDockContainerColor(uiState),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
    ) { contentPadding ->
        when (uiState.selectedTab) {
            AppTab.Config ->
                ConfigTabScreen(
                    selectedLanguage = uiState.selectedLanguage,
                    onLanguageSelected = viewModel::onLanguageSelected,
                    isLanguageExpanded = uiState.isConfigLanguageExpanded,
                    onLanguageExpandedChanged = viewModel::onConfigLanguageExpandedChanged,
                    selectedThemeStyle = uiState.selectedThemeStyle,
                    onThemeStyleSelected = viewModel::onThemeStyleSelected,
                    selectedFactionTheme = uiState.activeFactionTheme,
                    onFactionThemeSelected = viewModel::onFactionThemeSelected,
                    customFactionThemes = uiState.customFactionThemes,
                    onCustomFactionThemeSaved = viewModel::onCustomFactionThemeSaved,
                    onCustomFactionThemeDeleted = viewModel::onCustomFactionThemeDeleted,
                    onCustomFactionThemesImported = viewModel::onCustomFactionThemesImported,
                    onCustomFactionThemesReordered = viewModel::onCustomFactionThemesReordered,
                    customMaterialThemePresets = uiState.customMaterialThemePresets,
                    customMaterialThemeSettings = uiState.customMaterialThemeSettings,
                    onCustomMaterialThemeSaved = viewModel::onCustomMaterialThemeSaved,
                    onCustomMaterialThemeDeleted = viewModel::onCustomMaterialThemeDeleted,
                    onCustomMaterialThemesImported = viewModel::onCustomMaterialThemesImported,
                    onCustomMaterialThemesReordered = viewModel::onCustomMaterialThemesReordered,
                    onCreateCustomMaterialTheme = viewModel::onCreateCustomMaterialTheme,
                    customFactionThemePresets = uiState.customFactionThemePresets,
                    selectedThemeMode = uiState.selectedThemeMode,
                    onThemeModeSelected = viewModel::onThemeModeSelected,
                    isThemeAppearanceExpanded = uiState.isConfigThemeAppearanceExpanded,
                    onThemeAppearanceExpandedChanged = viewModel::onConfigThemeAppearanceExpandedChanged,
                    isCustomMaterialThemeExpanded = uiState.isConfigCustomMaterialThemeExpanded,
                    onCustomMaterialThemeExpandedChanged = viewModel::onConfigCustomMaterialThemeExpandedChanged,
                    isBuiltInMaterialPalettesExpanded = uiState.isConfigBuiltInMaterialPalettesExpanded,
                    onBuiltInMaterialPalettesExpandedChanged = viewModel::onConfigBuiltInMaterialPalettesExpandedChanged,
                    isMaterialRedsPaletteExpanded = uiState.isConfigMaterialRedsPaletteExpanded,
                    onMaterialRedsPaletteExpandedChanged = viewModel::onConfigMaterialRedsPaletteExpandedChanged,
                    isMaterialOrangesPaletteExpanded = uiState.isConfigMaterialOrangesPaletteExpanded,
                    onMaterialOrangesPaletteExpandedChanged = viewModel::onConfigMaterialOrangesPaletteExpandedChanged,
                    isMaterialYellowsPaletteExpanded = uiState.isConfigMaterialYellowsPaletteExpanded,
                    onMaterialYellowsPaletteExpandedChanged = viewModel::onConfigMaterialYellowsPaletteExpandedChanged,
                    isMaterialGreensPaletteExpanded = uiState.isConfigMaterialGreensPaletteExpanded,
                    onMaterialGreensPaletteExpandedChanged = viewModel::onConfigMaterialGreensPaletteExpandedChanged,
                    isMaterialBluesPaletteExpanded = uiState.isConfigMaterialBluesPaletteExpanded,
                    onMaterialBluesPaletteExpandedChanged = viewModel::onConfigMaterialBluesPaletteExpandedChanged,
                    isMaterialPurplesPaletteExpanded = uiState.isConfigMaterialPurplesPaletteExpanded,
                    onMaterialPurplesPaletteExpandedChanged = viewModel::onConfigMaterialPurplesPaletteExpandedChanged,
                    isMaterialNeutralsPaletteExpanded = uiState.isConfigMaterialNeutralsPaletteExpanded,
                    onMaterialNeutralsPaletteExpandedChanged = viewModel::onConfigMaterialNeutralsPaletteExpandedChanged,
                    isCustomFactionThemeExpanded = uiState.isConfigCustomFactionThemeExpanded,
                    onCustomFactionThemeExpandedChanged = viewModel::onConfigCustomFactionThemeExpandedChanged,
                    isSampleTextExpanded = uiState.isConfigSampleTextExpanded,
                    onSampleTextExpandedChanged = viewModel::onConfigSampleTextExpandedChanged,
                    isSacredMachineFactionThemeExpanded = uiState.isConfigSacredMachineFactionThemeExpanded,
                    onSacredMachineFactionThemeExpandedChanged = viewModel::onConfigSacredMachineFactionThemeExpandedChanged,
                    isAncientDynastyFactionThemeExpanded = uiState.isConfigAncientDynastyFactionThemeExpanded,
                    onAncientDynastyFactionThemeExpandedChanged = viewModel::onConfigAncientDynastyFactionThemeExpandedChanged,
                    isImmortalRotFactionThemeExpanded = uiState.isConfigImmortalRotFactionThemeExpanded,
                    onImmortalRotFactionThemeExpandedChanged = viewModel::onConfigImmortalRotFactionThemeExpandedChanged,
                    isScarletCarnageFactionThemeExpanded = uiState.isConfigScarletCarnageFactionThemeExpanded,
                    onScarletCarnageFactionThemeExpandedChanged = viewModel::onConfigScarletCarnageFactionThemeExpandedChanged,
                    isExquisiteFallFactionThemeExpanded = uiState.isConfigExquisiteFallFactionThemeExpanded,
                    onExquisiteFallFactionThemeExpandedChanged = viewModel::onConfigExquisiteFallFactionThemeExpandedChanged,
                    isLabyrinthOfMutabilityFactionThemeExpanded = uiState.isConfigLabyrinthOfMutabilityFactionThemeExpanded,
                    onLabyrinthOfMutabilityFactionThemeExpandedChanged =
                        viewModel::onConfigLabyrinthOfMutabilityFactionThemeExpandedChanged,
                    isDebugExpanded = uiState.isConfigDebugExpanded,
                    onDebugExpandedChanged = viewModel::onConfigDebugExpandedChanged,
                    isDemoModeEnabled = uiState.isDemoModeEnabled,
                    onDemoModeEnabledChange = viewModel::onDemoModeEnabledChanged,
                    isDualThemeAnimationEnabled = uiState.isDualThemeAnimationEnabled,
                    onDualThemeAnimationEnabledChange = viewModel::onDualThemeAnimationEnabledChanged,
                    isSampleAutoFillEnabled = uiState.isSampleAutoFillEnabled,
                    onSampleAutoFillEnabledChange = viewModel::onSampleAutoFillEnabledChanged,
                    isSampleDecorationEnabled = uiState.isSampleDecorationEnabled,
                    onSampleDecorationEnabledChange = viewModel::onSampleDecorationEnabledChanged,
                    isSavedAudioPlaybackDataStorageEnabled = uiState.isSavedAudioPlaybackDataStorageEnabled,
                    onSavedAudioPlaybackDataStorageEnabledChange = viewModel::onSavedAudioPlaybackDataStorageEnabledChanged,
                    isFlashVisualPerfOverlayEnabled = uiState.isFlashVisualPerfOverlayEnabled,
                    onFlashVisualPerfOverlayEnabledChange = viewModel::onFlashVisualPerfOverlayEnabledChanged,
                    selectedPalette = uiState.selectedPalette,
                    onPaletteSelected = viewModel::onPaletteSelected,
                    materialPalettes = materialPalettes,
                    factionThemes = factionThemes,
                    accentTokens = accentTokens,
                    onOpenAboutPage = viewModel::onOpenAboutPage,
                    contentPadding = contentPadding,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                )

            AppTab.Audio ->
                AudioTabScreen(
                    selectedThemeStyle = uiState.selectedThemeStyle,
                    transportMode = uiState.transportMode,
                    isCodecBusy = uiState.audioTabCodecBusy,
                    encodeProgressDisplay = uiState.currentSession.encodeOperationSnapshot.toEncodeProgressDisplayModel(),
                    isEncodeCancelling = uiState.currentSession.isEncodeCancelling,
                    onTransportModeSelected = viewModel::onTransportModeSelected,
                    isFlashVoicingEnabled = uiState.isFlashVoicingEnabled,
                    selectedFlashVoicingStyle = uiState.selectedFlashVoicingStyle,
                    onFlashVoicingStyleSelected = viewModel::onFlashVoicingStyleSelected,
                    selectedMorseSpeed = uiState.selectedMorseSpeed,
                    onMorseSpeedSelected = viewModel::onMorseSpeedSelected,
                    isSampleAutoFillEnabled = uiState.isSampleAutoFillEnabled,
                    sampleInputLength = uiState.selectedSampleInputLength,
                    onSampleInputLengthSelected = viewModel::onSampleInputLengthSelected,
                    inputText = currentSession.inputText,
                    inputPlaceholderText = viewModel.currentPlaceholderText(uiState.transportMode),
                    onInputTextChange = viewModel::onInputTextChange,
                    onRandomizeSampleInput = viewModel::onRandomizeSampleInput,
                    decodedPayload = uiState.audioTabDecodedPayload,
                    onEncode = viewModel::onEncode,
                    onCancelEncode = viewModel::onCancelEncode,
                    onDecode = viewModel::onDecode,
                    onClear = viewModel::onClear,
                    onClearResult = viewModel::onClearResult,
                    contentPadding = contentPadding,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                )

            AppTab.Library ->
                LibraryTabScreen(
                    savedAudioItems = uiState.savedAudioItems,
                    decodedSavedAudioItemIds = uiState.decodedSavedAudioItemIds,
                    currentSavedAudioItem = uiState.currentSavedAudioItem,
                    savedAudioFolders = uiState.savedAudioFolders,
                    savedAudioFolderAssignments = uiState.savedAudioFolderAssignments,
                    librarySelection = uiState.librarySelection,
                    statusText = uiState.libraryStatusText,
                    onImportAudio = onImportAudio,
                    onSelectSavedAudio = viewModel::onSavedAudioSelected,
                    onEnterLibrarySelection = viewModel::onEnterLibrarySelection,
                    onToggleLibrarySelection = viewModel::onToggleLibrarySelection,
                    onSelectAllLibraryItems = viewModel::onSelectAllLibraryItems,
                    onDeleteSelectedSavedAudio = viewModel::onDeleteSelectedSavedAudio,
                    onClearLibrarySelection = viewModel::onClearLibrarySelection,
                    onDeleteSavedAudio = viewModel::onDeleteSavedAudio,
                    onRenameSavedAudio = viewModel::onRenameSavedAudio,
                    onCreateSavedAudioFolder = viewModel::onCreateSavedAudioFolder,
                    onRenameSavedAudioFolder = viewModel::onRenameSavedAudioFolder,
                    onDeleteSavedAudioFolder = viewModel::onDeleteSavedAudioFolder,
                    onMoveSavedAudioToFolder = viewModel::onMoveSavedAudioToFolder,
                    onShareSavedAudio = viewModel::onShareSavedAudio,
                    onExportSavedAudioToDocument = viewModel::onRequestExportSavedAudioToDocument,
                    onClearSavedAudioDecodeData = viewModel::onClearSavedAudioDecodeData,
                    contentPadding = contentPadding,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                )
        }
    }
}

private fun debugMiniPlayerOverlayLog(
    label: String,
    message: String,
) {
    if (!BuildConfig.DEBUG) {
        return
    }
    try {
        Log.d("MiniPlayerOverlayDiag", "$label $message")
    } catch (_: RuntimeException) {
    }
}

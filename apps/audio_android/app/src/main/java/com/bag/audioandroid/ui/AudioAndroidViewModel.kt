package com.bag.audioandroid.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bag.audioandroid.BuildConfig
import com.bag.audioandroid.R
import com.bag.audioandroid.audio.AudioPlaybackCoordinator
import com.bag.audioandroid.audio.VoicePreviewPlayer
import com.bag.audioandroid.data.AppSettingsRepository
import com.bag.audioandroid.data.SampleInputTextProvider
import com.bag.audioandroid.domain.AudioCodecGateway
import com.bag.audioandroid.domain.AudioIoGateway
import com.bag.audioandroid.domain.GeneratedAudioCacheGateway
import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.domain.SavedAudioDecodeCacheGateway
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.domain.SavedAudioRepository
import com.bag.audioandroid.domain.VoiceAudioFileGateway
import com.bag.audioandroid.domain.VoiceFxGateway
import com.bag.audioandroid.domain.VoiceLiveGateway
import com.bag.audioandroid.domain.VoiceRecordingGateway
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.AppTab
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.CustomFactionThemeSettings
import com.bag.audioandroid.ui.model.FactionThemeOption
import com.bag.audioandroid.ui.model.MorseSpeedOption
import com.bag.audioandroid.ui.model.PaletteOption
import com.bag.audioandroid.ui.model.PlaybackSequenceMode
import com.bag.audioandroid.ui.model.PlaybackSpeedOption
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.ThemeModeOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.model.VoiceTrackModeOption
import com.bag.audioandroid.ui.model.defaultPreset
import com.bag.audioandroid.ui.screen.DebugMorseVisualizationModeRequest
import com.bag.audioandroid.ui.screen.DebugPlaybackDisplayModeRequest
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.AudioDocumentExportSource
import com.bag.audioandroid.ui.state.DefaultVoiceTrackMode
import com.bag.audioandroid.ui.state.QueueSheetValue
import com.bag.audioandroid.ui.state.VoiceSessionState
import com.bag.audioandroid.ui.theme.DefaultCustomMaterialPaletteSettings
import com.bag.audioandroid.ui.theme.DefaultFactionTheme
import com.bag.audioandroid.ui.theme.DefaultMaterialPalette
import com.bag.audioandroid.ui.theme.FactionThemes
import com.bag.audioandroid.ui.theme.customFactionTheme
import com.bag.audioandroid.ui.theme.customMaterialPalette
import com.bag.audioandroid.ui.theme.isCustomFactionThemeOptionId
import com.bag.audioandroid.util.safeDebugLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Suppress("LargeClass")
class AudioAndroidViewModel(
    audioCodecGateway: AudioCodecGateway,
    audioIoGateway: AudioIoGateway,
    private val sampleInputTextProvider: SampleInputTextProvider,
    appSettingsRepository: AppSettingsRepository,
    playbackRuntimeGateway: PlaybackRuntimeGateway,
    voiceFxGateway: VoiceFxGateway,
    voiceRecordingGateway: VoiceRecordingGateway,
    voiceLiveGateway: VoiceLiveGateway,
    voiceAudioFileGateway: VoiceAudioFileGateway,
    savedAudioRepository: SavedAudioRepository,
    private val generatedAudioCacheGateway: GeneratedAudioCacheGateway,
    private val savedAudioDecodeCacheGateway: SavedAudioDecodeCacheGateway,
) : ViewModel() {
    private val uiStateFlow = MutableStateFlow(loadInitialUiState(appSettingsRepository))
    val uiState: StateFlow<AudioAppUiState> = uiStateFlow
    private val debugExpandLyricsRequestIdFlow = MutableStateFlow<Long?>(null)
    val debugExpandLyricsRequestId: StateFlow<Long?> = debugExpandLyricsRequestIdFlow
    private val debugPlaybackDisplayModeRequestFlow = MutableStateFlow<DebugPlaybackDisplayModeRequest?>(null)
    val debugPlaybackDisplayModeRequest: StateFlow<DebugPlaybackDisplayModeRequest?> = debugPlaybackDisplayModeRequestFlow
    private val debugMorseVisualizationModeRequestFlow = MutableStateFlow<DebugMorseVisualizationModeRequest?>(null)
    val debugMorseVisualizationModeRequest: StateFlow<DebugMorseVisualizationModeRequest?> = debugMorseVisualizationModeRequestFlow

    private val uiTextMapper = BagUiTextMapper()
    private val sampleInputSessionUpdater = SampleInputSessionUpdater(sampleInputTextProvider)
    private val sessionStateStore = AudioSessionStateStore(uiStateFlow)
    private val playbackCoordinator = AudioPlaybackCoordinator()
    private val playbackSourceCoordinator = PlaybackSourceCoordinator(SAMPLE_RATE_HZ)
    private val playbackSessionReducer = PlaybackSessionReducer(playbackRuntimeGateway, SAMPLE_RATE_HZ)
    private val playbackSequenceNavigator = PlaybackSequenceNavigator()
    private val voicePreviewPlayer = VoicePreviewPlayer()
    private val followDataWindowActions =
        FollowDataWindowActions(
            uiState = uiStateFlow,
            scope = viewModelScope,
            audioCodecGateway = audioCodecGateway,
            sessionStateStore = sessionStateStore,
            sampleRateHz = SAMPLE_RATE_HZ,
            frameSamples = FRAME_SAMPLES,
            workerDispatcher = Dispatchers.IO,
        )
    private val flashVisualWindowActions =
        FlashVisualWindowActions(
            uiState = uiStateFlow,
            scope = viewModelScope,
            sessionStateStore = sessionStateStore,
            workerDispatcher = Dispatchers.IO,
        )
    private val playbackActions =
        AudioAndroidPlaybackActions(
            uiState = uiStateFlow,
            scope = viewModelScope,
            sessionStateStore = sessionStateStore,
            playbackCoordinator = playbackCoordinator,
            playbackRuntimeGateway = playbackRuntimeGateway,
            playbackSourceCoordinator = playbackSourceCoordinator,
            playbackSessionReducer = playbackSessionReducer,
            sampleRateHz = SAMPLE_RATE_HZ,
            onPlaybackCompleted = ::handlePlaybackCompleted,
            followDataWindowActions = followDataWindowActions,
            flashVisualWindowActions = flashVisualWindowActions,
        )
    private val libraryActions =
        AudioAndroidLibraryActions(
            uiState = uiStateFlow,
            scope = viewModelScope,
            sessionStateStore = sessionStateStore,
            playbackRuntimeGateway = playbackRuntimeGateway,
            savedAudioRepository = savedAudioRepository,
            stopPlayback = playbackActions::stopPlayback,
            playCurrentFromStart = playbackActions::playCurrentFromStart,
            generatedAudioCacheGateway = generatedAudioCacheGateway,
            savedAudioDecodeCacheGateway = savedAudioDecodeCacheGateway,
            workerDispatcher = Dispatchers.IO,
        )
    private val documentExportActions =
        AudioDocumentExportActions(
            uiState = uiStateFlow,
            scope = viewModelScope,
            sessionStateStore = sessionStateStore,
            savedAudioRepository = savedAudioRepository,
            sampleRateHz = SAMPLE_RATE_HZ,
            workerDispatcher = Dispatchers.IO,
        )
    private val sessionActions =
        AudioAndroidSessionActions(
            uiState = uiStateFlow,
            scope = viewModelScope,
            audioCodecGateway = audioCodecGateway,
            audioIoGateway = audioIoGateway,
            sampleInputTextProvider = sampleInputTextProvider,
            sessionStateStore = sessionStateStore,
            uiTextMapper = uiTextMapper,
            playbackRuntimeGateway = playbackRuntimeGateway,
            savedAudioRepository = savedAudioRepository,
            sampleRateHz = SAMPLE_RATE_HZ,
            frameSamples = FRAME_SAMPLES,
            stopPlayback = playbackActions::stopPlayback,
            refreshSavedAudioItems = libraryActions::refreshSavedAudioItems,
            workerDispatcher = Dispatchers.IO,
            generatedAudioCacheGateway = generatedAudioCacheGateway,
            savedAudioDecodeCacheGateway = savedAudioDecodeCacheGateway,
            followDataWindowActions = followDataWindowActions,
        )
    private val navigationActions =
        AudioAndroidNavigationActions(
            uiState = uiStateFlow,
            appSettingsRepository = appSettingsRepository,
            scope = viewModelScope,
        )
    private val voiceSessionActions =
        VoiceSessionActions(
            uiState = uiStateFlow,
            scope = viewModelScope,
            voiceFxGateway = voiceFxGateway,
            voiceRecordingGateway = voiceRecordingGateway,
            voiceLiveGateway = voiceLiveGateway,
            voiceAudioFileGateway = voiceAudioFileGateway,
            voicePreviewPlayer = voicePreviewPlayer,
            savedAudioRepository = savedAudioRepository,
            stopGlobalPlayback = playbackActions::stopPlayback,
            onPersistWorkflowModeSelected = { mode ->
                viewModelScope.launch {
                    appSettingsRepository.setSelectedVoiceWorkflowModeId(mode.id)
                }
            },
            onPersistTrackModeSelected = { mode ->
                viewModelScope.launch {
                    appSettingsRepository.setSelectedVoiceTrackModeId(mode.id)
                }
            },
            onPersistInputSourceSelected = { source ->
                viewModelScope.launch {
                    appSettingsRepository.setSelectedVoiceInputSourceId(source.id)
                }
            },
            workerDispatcher = Dispatchers.IO,
        )
    private val preferencesActions =
        AudioAndroidPreferencesActions(
            uiState = uiStateFlow,
            sampleInputSessionUpdater = sampleInputSessionUpdater,
            appSettingsRepository = appSettingsRepository,
            scope = viewModelScope,
        )
    private val preferencesBindings =
        AudioAndroidPreferencesBindings(
            uiState = uiStateFlow,
            sampleInputSessionUpdater = sampleInputSessionUpdater,
            appSettingsRepository = appSettingsRepository,
            scope = viewModelScope,
        )
    private val debugScenarioActions =
        AudioDebugScenarioActions(
            uiState = uiStateFlow,
            scope = viewModelScope,
            sampleInputTextProvider = sampleInputTextProvider,
            savedAudioRepository = savedAudioRepository,
            generatedAudioCacheGateway = generatedAudioCacheGateway,
            sessionStateStore = sessionStateStore,
            onTransportModeSelected = ::onTransportModeSelected,
            onFlashVoicingStyleSelected = ::onFlashVoicingStyleSelected,
            onMorseSpeedSelected = ::onMorseSpeedSelected,
            onLanguageSelected = ::onLanguageSelected,
            onDemoModeEnabledChanged = ::onDemoModeEnabledChanged,
            onFlashVisualPerfOverlayEnabledChanged = ::onFlashVisualPerfOverlayEnabledChanged,
            onInputTextChange = ::onInputTextChange,
            onEncode = ::onEncode,
            onPlaybackSpeedSelected = ::onPlaybackSpeedSelected,
            onShellSavedAudioSelected = ::onShellSavedAudioSelected,
            onDecode = ::onDecode,
            onOpenPlayerDetailSheet = ::onOpenPlayerDetailSheet,
            onTabSelected = ::onTabSelected,
            onThemeStyleSelected = ::onThemeStyleSelected,
            onCustomMaterialThemeSaved = ::onCustomMaterialThemeSaved,
            onCustomMaterialThemesImported = ::onCustomMaterialThemesImported,
        )

    init {
        // Cold-start rule: never retain generated audio cache across launches.
        // Storage footprint is a product constraint, not a best-effort cleanup.
        generatedAudioCacheGateway.pruneCachedFiles()
        savedAudioDecodeCacheGateway.prune(savedAudioRepository.listSavedAudio().map { it.itemId }.toSet())
        val coreVersion = audioCodecGateway.getCoreVersion()
        val selectedLanguage =
            AppLanguageOption.fromLanguageTags(
                AppCompatDelegate.getApplicationLocales().toLanguageTags(),
            )
        val initialSampleAutoFillEnabled = runBlocking { appSettingsRepository.isSampleAutoFillEnabled.first() }
        val initialSampleDecorationEnabled = runBlocking { appSettingsRepository.isSampleDecorationEnabled.first() }
        val initialSavedAudioPlaybackDataStorageEnabled =
            runBlocking { appSettingsRepository.isSavedAudioPlaybackDataStorageEnabled.first() }
        uiStateFlow.update {
            it.copy(
                selectedLanguage = selectedLanguage,
                presentationVersion = BuildConfig.VERSION_NAME.ifBlank { "" },
                coreVersion = coreVersion.ifBlank { "" },
                isSampleAutoFillEnabled = initialSampleAutoFillEnabled,
                isSampleDecorationEnabled = initialSampleDecorationEnabled,
                isSavedAudioPlaybackDataStorageEnabled = initialSavedAudioPlaybackDataStorageEnabled,
                sessions =
                    sampleInputSessionUpdater.initialize(
                        sessions = it.sessions,
                        language = selectedLanguage,
                        flavor = it.currentSampleFlavor,
                        length = it.selectedSampleInputLength,
                        isSampleAutoFillEnabled = initialSampleAutoFillEnabled,
                        isDecorationEnabled = initialSampleDecorationEnabled,
                    ),
                currentPlaybackSource = AudioPlaybackSource.Generated(it.transportMode),
            )
        }
        preferencesBindings.startObserving()
    }

    fun onTabSelected(tab: AppTab) {
        val previousTab = uiStateFlow.value.selectedTab
        if (previousTab == AppTab.Voice && tab != AppTab.Voice) {
            voiceSessionActions.onLeaveVoiceTab()
        }
        navigationActions.onTabSelected(tab, libraryActions::refreshSavedAudioItems)
    }

    fun onLanguageSelected(language: AppLanguageOption) {
        preferencesActions.onLanguageSelected(language)
    }

    fun onOpenAboutPage() {
        navigationActions.onOpenAboutPage()
    }

    fun onCloseAboutPage() {
        navigationActions.onCloseAboutPage()
    }

    fun onOpenLicensesPage() {
        navigationActions.onOpenLicensesPage()
    }

    fun onCloseLicensesPage() {
        navigationActions.onCloseLicensesPage()
    }

    fun onPaletteSelected(palette: PaletteOption) {
        preferencesActions.onPaletteSelected(palette)
    }

    fun onThemeModeSelected(themeMode: ThemeModeOption) {
        preferencesActions.onThemeModeSelected(themeMode)
    }

    fun onMaterialDarkThemeActiveChanged(isDarkTheme: Boolean) {
        preferencesActions.onMaterialDarkThemeActiveChanged(isDarkTheme)
    }

    fun onThemeStyleSelected(themeStyle: ThemeStyleOption) {
        preferencesActions.onThemeStyleSelected(themeStyle)
    }

    fun onFactionThemeSelected(factionTheme: FactionThemeOption) {
        preferencesActions.onFactionThemeSelected(factionTheme)
    }

    fun onCustomFactionThemeSaved(
        settings: CustomFactionThemeSettings,
        replacePresetId: String?,
    ) {
        preferencesActions.onCustomFactionThemeSaved(settings, replacePresetId)
    }

    fun onCustomFactionThemeDeleted(presetId: String) {
        preferencesActions.onCustomFactionThemeDeleted(presetId)
    }

    fun onCustomMaterialThemeSaved(
        settings: CustomFactionThemeSettings,
        replacePresetId: String? = null,
    ) {
        preferencesActions.onCustomMaterialThemeSaved(settings, replacePresetId)
    }

    fun onCustomMaterialThemeDeleted(presetId: String) {
        preferencesActions.onCustomMaterialThemeDeleted(presetId)
    }

    fun onCreateCustomMaterialTheme() {
        preferencesActions.onCreateCustomMaterialTheme()
    }

    fun onCustomMaterialThemesImported(settings: List<CustomFactionThemeSettings>) {
        preferencesActions.onCustomMaterialThemesImported(settings)
    }

    fun onCustomMaterialThemesReordered(
        fromIndex: Int,
        toIndex: Int,
    ) {
        preferencesActions.onCustomMaterialThemesReordered(fromIndex, toIndex)
    }

    fun onCustomFactionThemesImported(settings: List<CustomFactionThemeSettings>) {
        preferencesActions.onCustomFactionThemesImported(settings)
    }

    fun onCustomFactionThemesReordered(
        fromIndex: Int,
        toIndex: Int,
    ) {
        preferencesActions.onCustomFactionThemesReordered(fromIndex, toIndex)
    }

    fun onConfigLanguageExpandedChanged(expanded: Boolean) {
        preferencesActions.onConfigLanguageExpandedChanged(expanded)
    }

    fun onConfigThemeAppearanceExpandedChanged(expanded: Boolean) {
        preferencesActions.onConfigThemeAppearanceExpandedChanged(expanded)
    }

    fun onConfigCustomMaterialThemeExpandedChanged(expanded: Boolean) {
        preferencesActions.onConfigCustomMaterialThemeExpandedChanged(expanded)
    }

    fun onConfigBuiltInMaterialPalettesExpandedChanged(expanded: Boolean) {
        preferencesActions.onConfigBuiltInMaterialPalettesExpandedChanged(expanded)
    }

    fun onConfigMaterialRedsPaletteExpandedChanged(expanded: Boolean) {
        preferencesActions.onConfigMaterialRedsPaletteExpandedChanged(expanded)
    }

    fun onConfigMaterialOrangesPaletteExpandedChanged(expanded: Boolean) {
        preferencesActions.onConfigMaterialOrangesPaletteExpandedChanged(expanded)
    }

    fun onConfigMaterialYellowsPaletteExpandedChanged(expanded: Boolean) {
        preferencesActions.onConfigMaterialYellowsPaletteExpandedChanged(expanded)
    }

    fun onConfigMaterialGreensPaletteExpandedChanged(expanded: Boolean) {
        preferencesActions.onConfigMaterialGreensPaletteExpandedChanged(expanded)
    }

    fun onConfigMaterialBluesPaletteExpandedChanged(expanded: Boolean) {
        preferencesActions.onConfigMaterialBluesPaletteExpandedChanged(expanded)
    }

    fun onConfigMaterialPurplesPaletteExpandedChanged(expanded: Boolean) {
        preferencesActions.onConfigMaterialPurplesPaletteExpandedChanged(expanded)
    }

    fun onConfigMaterialNeutralsPaletteExpandedChanged(expanded: Boolean) {
        preferencesActions.onConfigMaterialNeutralsPaletteExpandedChanged(expanded)
    }

    fun onConfigCustomFactionThemeExpandedChanged(expanded: Boolean) {
        preferencesActions.onConfigCustomFactionThemeExpandedChanged(expanded)
    }

    fun onConfigSampleTextExpandedChanged(expanded: Boolean) {
        preferencesActions.onConfigSampleTextExpandedChanged(expanded)
    }

    fun onConfigSacredMachineFactionThemeExpandedChanged(expanded: Boolean) {
        preferencesActions.onConfigSacredMachineFactionThemeExpandedChanged(expanded)
    }

    fun onConfigAncientDynastyFactionThemeExpandedChanged(expanded: Boolean) {
        preferencesActions.onConfigAncientDynastyFactionThemeExpandedChanged(expanded)
    }

    fun onConfigImmortalRotFactionThemeExpandedChanged(expanded: Boolean) {
        preferencesActions.onConfigImmortalRotFactionThemeExpandedChanged(expanded)
    }

    fun onConfigScarletCarnageFactionThemeExpandedChanged(expanded: Boolean) {
        preferencesActions.onConfigScarletCarnageFactionThemeExpandedChanged(expanded)
    }

    fun onConfigExquisiteFallFactionThemeExpandedChanged(expanded: Boolean) {
        preferencesActions.onConfigExquisiteFallFactionThemeExpandedChanged(expanded)
    }

    fun onConfigLabyrinthOfMutabilityFactionThemeExpandedChanged(expanded: Boolean) {
        preferencesActions.onConfigLabyrinthOfMutabilityFactionThemeExpandedChanged(expanded)
    }

    fun onConfigDebugExpandedChanged(expanded: Boolean) {
        preferencesActions.onConfigDebugExpandedChanged(expanded)
    }

    fun onFlashVoicingStyleSelected(style: com.bag.audioandroid.ui.model.FlashVoicingStyleOption) {
        preferencesActions.onFlashVoicingStyleSelected(style)
    }

    fun onMorseSpeedSelected(speed: MorseSpeedOption) {
        preferencesActions.onMorseSpeedStyleSelected(speed)
    }

    fun onSampleInputLengthSelected(length: SampleInputLengthOption) {
        preferencesActions.onSampleInputLengthSelected(length)
    }

    fun onDemoModeEnabledChanged(enabled: Boolean) {
        preferencesActions.onDemoModeEnabledChanged(enabled)
    }

    fun onDualThemeAnimationEnabledChanged(enabled: Boolean) {
        preferencesActions.onDualThemeAnimationEnabledChanged(enabled)
    }

    fun onSampleDecorationEnabledChanged(enabled: Boolean) {
        preferencesActions.onSampleDecorationEnabledChanged(enabled)
    }

    fun onSampleAutoFillEnabledChanged(enabled: Boolean) {
        preferencesActions.onSampleAutoFillEnabledChanged(enabled)
    }

    fun onSavedAudioPlaybackDataStorageEnabledChanged(enabled: Boolean) {
        preferencesActions.onSavedAudioPlaybackDataStorageEnabledChanged(enabled)
    }

    fun onFlashVisualPerfOverlayEnabledChanged(enabled: Boolean) {
        preferencesActions.onFlashVisualPerfOverlayEnabledChanged(enabled)
    }

    fun onOpenPlayerDetailSheet() {
        playbackActions.onPlaybackSpeedSelected(PlaybackSpeedOption.default.speed)
        navigationActions.onOpenPlayerDetailSheet()
    }

    fun onClosePlayerDetailSheet() {
        navigationActions.onClosePlayerDetailSheet()
    }

    fun onSnackbarMessageShown(messageId: Long) {
        navigationActions.onSnackbarMessageShown(messageId)
    }

    fun onInputTextChange(value: String) {
        sessionActions.onInputTextChange(value)
    }

    fun startFlashDebugScenario(scenario: FlashDebugScenario) {
        debugPlaybackDisplayModeRequestFlow.value =
            DebugPlaybackDisplayModeRequest(
                requestId = scenario.requestId,
                mode = scenario.displayMode,
            )
        debugScenarioActions.startFlashDebugScenario(scenario)
    }

    fun startMiniDebugScenario(scenario: MiniDebugScenario) {
        debugExpandLyricsRequestIdFlow.value = scenario.requestId.takeIf { scenario.expandLyrics }
        debugPlaybackDisplayModeRequestFlow.value =
            DebugPlaybackDisplayModeRequest(
                requestId = scenario.requestId,
                mode = scenario.displayMode,
            )
        debugMorseVisualizationModeRequestFlow.value =
            DebugMorseVisualizationModeRequest(
                requestId = scenario.requestId,
                mode = scenario.morseVisualizationMode,
            )
        debugScenarioActions.startMiniDebugScenario(scenario)
    }

    fun onDebugExpandLyricsHandled(requestId: Long) {
        if (debugExpandLyricsRequestIdFlow.value == requestId) {
            debugExpandLyricsRequestIdFlow.value = null
        }
    }

    fun onDebugPlaybackDisplayModeHandled(requestId: Long) {
        if (debugPlaybackDisplayModeRequestFlow.value?.requestId == requestId) {
            debugPlaybackDisplayModeRequestFlow.value = null
        }
    }

    fun onDebugMorseVisualizationModeHandled(requestId: Long) {
        if (debugMorseVisualizationModeRequestFlow.value?.requestId == requestId) {
            debugMorseVisualizationModeRequestFlow.value = null
        }
    }

    fun startEncodeProgressDebugScenario(scenario: EncodeProgressDebugScenario) {
        debugScenarioActions.startEncodeProgressDebugScenario(scenario)
    }

    fun startSavedAudioDebugScenario(scenario: SavedAudioDebugScenario) {
        debugScenarioActions.startSavedAudioDebugScenario(scenario)
    }

    fun startAppTabDebugScenario(scenario: AppTabDebugScenario) {
        debugScenarioActions.startAppTabDebugScenario(scenario)
    }

    fun startSettingsImportDebugScenario(scenario: SettingsImportDebugScenario) {
        debugScenarioActions.startSettingsImportDebugScenario(scenario)
    }

    fun onRandomizeSampleInput(length: SampleInputLengthOption) {
        sessionActions.onRandomizeSampleInput(length)
    }

    fun currentPlaceholderText(mode: TransportModeOption): String {
        val state = uiStateFlow.value
        val currentSession = state.sessions.getValue(mode)
        if (currentSession.sampleInputId == null) {
            return ""
        }
        return sampleInputTextProvider
            .defaultSample(
                mode = mode,
                language = state.selectedLanguage,
                flavor = state.currentSampleFlavor,
            ).text
    }

    fun onTransportModeSelected(mode: TransportModeOption) {
        sessionActions.onTransportModeSelected(mode)
        preferencesActions.onTransportModeSelected(mode)
    }

    fun onEncode() {
        if (uiStateFlow.value.currentSession.inputText
                .isEmpty()
        ) {
            navigationActions.showSnackbar(
                UiText.Resource(R.string.audio_input_required_or_randomize),
            )
            return
        }
        sessionActions.onEncode()
    }

    fun onCancelEncode() {
        sessionActions.onCancelEncode()
    }

    fun onTogglePlayback() {
        voiceSessionActions.stopPreviewForExternalPlayback()
        playbackActions.onTogglePlayback()
    }

    fun pauseCurrentPlaybackIfPlaying(): Boolean = playbackActions.pauseCurrentPlaybackIfPlaying()

    fun stopPlayback() {
        voiceSessionActions.stopPreviewForExternalPlayback()
        playbackActions.stopPlayback()
    }

    fun onPlaybackSequenceModeSelected(mode: PlaybackSequenceMode) {
        preferencesActions.onPlaybackSequenceModeSelected(mode)
    }

    fun onPlaybackSpeedSelected(playbackSpeed: Float) {
        playbackActions.onPlaybackSpeedSelected(playbackSpeed)
    }

    fun onPlaybackDisplayModeSelected(showLyrics: Boolean) {
        // Playback display mode is currently local UI state in the sheet.
        // Keep this callback for scaffold API compatibility.
        @Suppress("UNUSED_VARIABLE")
        val ignored = showLyrics
    }

    fun onSkipToPreviousTrack() {
        voiceSessionActions.stopPreviewForExternalPlayback()
        safeDebugLog(
            SavedAudioPlaybackDiagTag,
            "skipPrevious requested currentSource=${uiStateFlow.value.currentPlaybackSource} " +
                "selectedSaved=${uiStateFlow.value.selectedSavedAudio?.item?.itemId.orEmpty()}",
        )
        skipToAdjacentSavedTrack { state, currentSource ->
            playbackSequenceNavigator.previousSavedSource(
                savedAudioItems = state.savedAudioItems,
                currentSource = currentSource,
            )
        }
    }

    fun onSkipToNextTrack() {
        voiceSessionActions.stopPreviewForExternalPlayback()
        safeDebugLog(
            SavedAudioPlaybackDiagTag,
            "skipNext requested currentSource=${uiStateFlow.value.currentPlaybackSource} " +
                "selectedSaved=${uiStateFlow.value.selectedSavedAudio?.item?.itemId.orEmpty()}",
        )
        skipToAdjacentSavedTrack { state, currentSource ->
            playbackSequenceNavigator.nextSavedSource(
                savedAudioItems = state.savedAudioItems,
                currentSource = currentSource,
            )
        }
    }

    fun onScrubStarted() {
        playbackActions.onScrubStarted()
    }

    fun onScrubChanged(targetSamples: Int) {
        playbackActions.onScrubChanged(targetSamples)
    }

    fun onScrubFinished() {
        playbackActions.onScrubFinished()
    }

    fun onScrubCanceled() {
        playbackActions.onScrubCanceled()
    }

    fun onDecode() {
        sessionActions.onDecode()
    }

    fun ensureCurrentPlaybackDecodedForLyrics() {
        sessionActions.ensureCurrentPlaybackDecodedForLyrics()
    }

    fun onClear() {
        sessionActions.onClear()
    }

    fun onClearResult() {
        sessionActions.onClearResult()
    }

    fun onExportAudio() {
        sessionActions.onExportAudio()
    }

    fun onShareCurrentGeneratedAudio() {
        sessionActions.onShareCurrentGeneratedAudio()
    }

    fun onRequestExportGeneratedAudioToDocument() {
        documentExportActions.onRequestGeneratedAudioExportToDocument()
    }

    fun onRequestExportCurrentSavedAudioToDocument() {
        uiStateFlow.value.currentSavedAudioItem?.let(documentExportActions::onRequestSavedAudioExportToDocument)
    }

    fun onDocumentExportPicked(uriString: String?) {
        when (uiStateFlow.value.pendingDocumentExportRequest?.source) {
            AudioDocumentExportSource.Voice -> voiceSessionActions.onDocumentExportPicked(uriString)
            else -> documentExportActions.onDocumentExportPicked(uriString)
        }
    }

    override fun onCleared() {
        voiceSessionActions.release()
        playbackActions.release()
        uiStateFlow.value.sessions.values.forEach { session ->
            generatedAudioCacheGateway.deleteCachedFile(session.generatedPcmFilePath)
        }
        generatedAudioCacheGateway.deleteCachedFile(uiStateFlow.value.selectedSavedAudio?.pcmFilePath)
        super.onCleared()
    }

    fun onOpenSavedAudioSheetFromDock() {
        sessionActions.onOpenSavedAudioSheetFromDock()
    }

    fun onOpenSavedAudioSheetFromPlayerDetail() {
        sessionActions.onOpenSavedAudioSheetFromPlayerDetail()
    }

    fun onCloseSavedAudioSheet() {
        sessionActions.onCloseSavedAudioSheet()
    }

    fun onQueueSheetValueChanged(value: QueueSheetValue) {
        navigationActions.onQueueSheetValueChanged(value)
    }

    fun onSavedAudioSelected(itemId: String) {
        voiceSessionActions.stopPreviewForExternalPlayback()
        libraryActions.onSavedAudioSelected(itemId)
    }

    fun onImportAudio(uriString: String) {
        libraryActions.onImportAudio(uriString)
    }

    fun onShellSavedAudioSelected(itemId: String) {
        voiceSessionActions.stopPreviewForExternalPlayback()
        libraryActions.onShellSavedAudioSelected(itemId)
    }

    fun onVoiceRecordPermissionChanged(granted: Boolean) {
        voiceSessionActions.onRecordPermissionChanged(granted)
    }

    fun onStartVoiceRecording() {
        voiceSessionActions.onStartRecording()
    }

    fun onStopVoiceRecording() {
        voiceSessionActions.onStopRecording()
    }

    fun onVoiceInputSourceSelected(source: com.bag.audioandroid.ui.model.VoiceInputSourceOption) {
        voiceSessionActions.onInputSourceSelected(source)
    }

    fun onVoiceRecordProcessingModeSelected(mode: com.bag.audioandroid.ui.model.VoiceRecordProcessingModeOption) {
        voiceSessionActions.onRecordProcessingModeSelected(mode)
    }

    fun onVoiceWorkflowModeSelected(mode: com.bag.audioandroid.ui.model.VoiceWorkflowModeOption) {
        voiceSessionActions.onWorkflowModeSelected(mode)
    }

    fun onVoiceTrackModeSelected(mode: com.bag.audioandroid.ui.model.VoiceTrackModeOption) {
        voiceSessionActions.onTrackModeSelected(mode)
    }

    fun onImportVoiceAudio(uriString: String) {
        voiceSessionActions.onImportAudio(uriString)
    }

    fun onProcessVoiceRecording() {
        voiceSessionActions.onProcessRecording()
    }

    fun onStartLiveVoice() {
        voiceSessionActions.onStartLive()
    }

    fun onStopLiveVoice() {
        voiceSessionActions.onStopLive()
    }

    fun onToggleVoicePreview() {
        voiceSessionActions.onTogglePreview()
    }

    fun onToggleVoicePreviewTrack(track: com.bag.audioandroid.ui.model.VoicePreviewTrackOption) {
        voiceSessionActions.onTogglePreviewTrack(track)
    }

    fun onVoicePreviewTrackSeek(
        track: com.bag.audioandroid.ui.model.VoicePreviewTrackOption,
        targetSamples: Int,
    ) {
        voiceSessionActions.onPreviewTrackSeek(track, targetSamples)
    }

    fun onRequestExportVoiceAudioToDocument() {
        voiceSessionActions.onRequestExportProcessedAudioToDocument()
    }

    fun onShareVoiceOutput() {
        voiceSessionActions.onShareProcessedAudio()
    }

    fun onClearVoice() {
        voiceSessionActions.onClear()
    }

    fun onVoicePresetSelected(preset: com.bag.audioandroid.ui.model.VoiceFxPresetOption) {
        voiceSessionActions.onPresetSelected(preset)
    }

    fun onVoiceSubvoiceStyleSelected(style: com.bag.audioandroid.ui.model.VoiceFxSubvoiceStyleOption) {
        voiceSessionActions.onSubvoiceStyleSelected(style)
    }

    fun onEnterLibrarySelection(itemId: String) {
        libraryActions.onEnterLibrarySelection(itemId)
    }

    fun onToggleLibrarySelection(itemId: String) {
        libraryActions.onToggleLibrarySelection(itemId)
    }

    fun onSelectAllLibraryItems(itemIds: Collection<String>) {
        libraryActions.onSelectAllLibraryItems(itemIds)
    }

    fun onClearLibrarySelection() {
        libraryActions.onClearLibrarySelection()
    }

    fun onDeleteSelectedSavedAudio() {
        libraryActions.onDeleteSelectedSavedAudio()
    }

    fun onDeleteSavedAudio(itemId: String) {
        libraryActions.onDeleteSavedAudio(itemId)
    }

    fun onRenameSavedAudio(
        itemId: String,
        newBaseName: String,
    ) {
        libraryActions.onRenameSavedAudio(itemId, newBaseName)
    }

    fun onShareCurrentSavedAudio() {
        libraryActions.onShareCurrentSavedAudio()
    }

    fun onShareSavedAudio(item: SavedAudioItem) {
        libraryActions.onShareSavedAudio(item)
    }

    fun onClearSavedAudioDecodeData(itemId: String) {
        libraryActions.onClearSavedAudioDecodeData(itemId)
    }

    fun onRequestExportSavedAudioToDocument(item: SavedAudioItem) {
        documentExportActions.onRequestSavedAudioExportToDocument(item)
    }

    fun onCreateSavedAudioFolder(name: String) {
        libraryActions.onCreateSavedAudioFolder(name)
    }

    fun onRenameSavedAudioFolder(
        folderId: String,
        name: String,
    ) {
        libraryActions.onRenameSavedAudioFolder(folderId, name)
    }

    fun onDeleteSavedAudioFolder(folderId: String) {
        libraryActions.onDeleteSavedAudioFolder(folderId)
    }

    fun onMoveSavedAudioToFolder(
        itemIds: Collection<String>,
        folderId: String?,
    ) {
        libraryActions.onMoveSavedAudioToFolder(itemIds, folderId)
    }

    private fun handlePlaybackCompleted(source: AudioPlaybackSource): Boolean {
        val nextSource = playbackSequenceNavigator.nextSourceForCompletion(uiStateFlow.value, source) ?: return false
        return when (nextSource) {
            is AudioPlaybackSource.Generated -> playbackActions.playCurrentFromStart()
            is AudioPlaybackSource.Saved -> {
                if (nextSource.itemId != uiStateFlow.value.currentSavedAudioItem?.itemId &&
                    !libraryActions.prepareSavedAudioSelectionForPlayback(
                        itemId = nextSource.itemId,
                    )
                ) {
                    return false
                }
                if (nextSource.itemId == uiStateFlow.value.currentSavedAudioItem?.itemId) {
                    playbackActions.playCurrentFromStart()
                } else {
                    true
                }
            }
        }
    }

    private fun skipToAdjacentSavedTrack(resolveTarget: (AudioAppUiState, AudioPlaybackSource) -> AudioPlaybackSource?) {
        val currentState = uiStateFlow.value
        val currentSource = currentState.currentPlaybackSource
        val targetSource = resolveTarget(currentState, currentSource) as? AudioPlaybackSource.Saved ?: return
        safeDebugLog(
            SavedAudioPlaybackDiagTag,
            "skipResolved from=$currentSource to=$targetSource selectedSaved=${currentState.selectedSavedAudio?.item?.itemId.orEmpty()}",
        )
        if (!libraryActions.prepareSavedAudioSelectionForPlayback(targetSource.itemId)) {
            safeDebugLog(
                SavedAudioPlaybackDiagTag,
                "skipPrepareFailed target=$targetSource currentSource=${uiStateFlow.value.currentPlaybackSource}",
            )
            return
        }
    }

    private companion object {
        const val SAMPLE_RATE_HZ = 44100
        const val FRAME_SAMPLES = 2205
        const val SavedAudioPlaybackDiagTag = "SavedAudioDiag"
    }
}

private fun loadInitialUiState(appSettingsRepository: AppSettingsRepository): AudioAppUiState =
    runBlocking {
        val customFactionThemePresets =
            appSettingsRepository.customFactionThemePresets.first().ifEmpty {
                listOf(com.bag.audioandroid.ui.model.DefaultCustomFactionThemeSettings)
            }
        val customMaterialThemePresets =
            appSettingsRepository.customMaterialThemePresets.first().ifEmpty {
                listOf(DefaultCustomMaterialPaletteSettings)
            }
        val selectedThemeMode = ThemeModeOption.fromId(appSettingsRepository.selectedThemeModeId.first())
        val selectedThemeStyle =
            inferPersistedThemeStyle(
                themeStyleId = appSettingsRepository.selectedThemeStyleId.first(),
                paletteId = appSettingsRepository.selectedPaletteId.first(),
                lightPaletteId = appSettingsRepository.selectedMaterialPaletteIdLight.first(),
                darkPaletteId = appSettingsRepository.selectedMaterialPaletteIdDark.first(),
            )
        val selectedFactionThemeId = appSettingsRepository.selectedFactionThemeId.first()
        val selectedFactionTheme =
            if (selectedFactionThemeId != null && isCustomFactionThemeOptionId(selectedFactionThemeId)) {
                customFactionThemePresets
                    .map(::customFactionTheme)
                    .firstOrNull { it.id == selectedFactionThemeId }
                    ?: customFactionTheme(customFactionThemePresets.first())
            } else {
                FactionThemes.firstOrNull { it.id == selectedFactionThemeId } ?: DefaultFactionTheme
            }
        val selectedMaterialPaletteIdLight = appSettingsRepository.selectedMaterialPaletteIdLight.first()
        val selectedMaterialPaletteIdDark = appSettingsRepository.selectedMaterialPaletteIdDark.first()
        val isMaterialDarkThemeActive = selectedThemeMode == ThemeModeOption.Dark
        val selectedSampleInputLength =
            SampleInputLengthOption.fromId(appSettingsRepository.selectedSampleInputLengthId.first())
                ?: SampleInputLengthOption.Short
        val selectedTransportMode =
            TransportModeOption.fromWireName(appSettingsRepository.selectedTransportModeId.first())
                ?: TransportModeOption.Flash
        val selectedVoiceTrackMode =
            VoiceTrackModeOption.fromIdOrNull(appSettingsRepository.selectedVoiceTrackModeId.first())
                ?: DefaultVoiceTrackMode
        val selectedPaletteId =
            if (isMaterialDarkThemeActive) {
                selectedMaterialPaletteIdDark ?: selectedMaterialPaletteIdLight ?: appSettingsRepository.selectedPaletteId.first()
            } else {
                selectedMaterialPaletteIdLight ?: selectedMaterialPaletteIdDark ?: appSettingsRepository.selectedPaletteId.first()
            }
        val selectedPalette =
            if (selectedPaletteId != null) {
                customMaterialThemePresets
                    .map(::customMaterialPalette)
                    .firstOrNull { it.id == selectedPaletteId }
                    ?: BrandNewOrBuiltInMaterialPalette(selectedPaletteId)
            } else {
                DefaultMaterialPalette
            }

        AudioAppUiState(
            selectedPalette = selectedPalette,
            selectedMaterialPaletteIdLight = selectedMaterialPaletteIdLight,
            selectedMaterialPaletteIdDark = selectedMaterialPaletteIdDark,
            customMaterialThemePresets = customMaterialThemePresets,
            selectedFactionTheme = selectedFactionTheme,
            customFactionThemePresets = customFactionThemePresets,
            selectedThemeStyle = selectedThemeStyle,
            selectedThemeMode = selectedThemeMode,
            isMaterialDarkThemeActive = isMaterialDarkThemeActive,
            selectedSampleInputLength = selectedSampleInputLength,
            transportMode = selectedTransportMode,
            currentPlaybackSource = AudioPlaybackSource.Generated(selectedTransportMode),
            voiceSession = defaultInitialVoiceSessionState(selectedVoiceTrackMode),
        )
    }

private fun defaultInitialVoiceSessionState(selectedTrackMode: VoiceTrackModeOption): VoiceSessionState =
    VoiceSessionState(
        selectedTrackMode = selectedTrackMode,
        selectedPreset = selectedTrackMode.defaultPreset(),
    )

private fun BrandNewOrBuiltInMaterialPalette(selectedPaletteId: String): PaletteOption =
    com.bag.audioandroid.ui.theme.MaterialPalettes
        .firstOrNull { it.id == selectedPaletteId } ?: DefaultMaterialPalette

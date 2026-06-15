package com.bag.audioandroid.ui.state

import com.bag.audioandroid.R
import com.bag.audioandroid.domain.DecodeOperationSnapshot
import com.bag.audioandroid.domain.DecodedPayloadViewData
import com.bag.audioandroid.domain.FlashSignalInfo
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.SavedAudioFolder
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.domain.WavAudioInfo
import com.bag.audioandroid.ui.LyricsNavigatorReadingModel
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.AppTab
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.CustomFactionThemeSettings
import com.bag.audioandroid.ui.model.DefaultCustomFactionThemeSettings
import com.bag.audioandroid.ui.model.FactionThemeOption
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.MiniPlayerLeadingIcon
import com.bag.audioandroid.ui.model.MiniPlayerSource
import com.bag.audioandroid.ui.model.MiniPlayerUiModel
import com.bag.audioandroid.ui.model.MorseSpeedOption
import com.bag.audioandroid.ui.model.PaletteOption
import com.bag.audioandroid.ui.model.PlaybackSequenceMode
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.SavedAudioModeFilter
import com.bag.audioandroid.ui.model.ThemeModeOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.screen.LONG_AUDIO_VISUALIZATION_SAMPLE_THRESHOLD
import com.bag.audioandroid.ui.screen.formatDurationMillis
import com.bag.audioandroid.ui.theme.DefaultCustomMaterialPaletteSettings
import com.bag.audioandroid.ui.theme.DefaultFactionTheme
import com.bag.audioandroid.ui.theme.DefaultMaterialPalette
import com.bag.audioandroid.ui.theme.MaterialPalettes
import com.bag.audioandroid.ui.theme.customFactionTheme
import com.bag.audioandroid.ui.theme.customMaterialPalette
import com.bag.audioandroid.ui.theme.isCustomFactionThemeOptionId
import com.bag.audioandroid.ui.theme.isCustomMaterialPaletteId

data class AudioAppUiState(
    val selectedTab: AppTab = AppTab.Data,
    val selectedLanguage: AppLanguageOption = AppLanguageOption.FollowSystem,
    val showAboutPage: Boolean = false,
    val showLicensesPage: Boolean = false,
    val presentationVersion: String = "",
    val coreVersion: String = "",
    val selectedPalette: PaletteOption = DefaultMaterialPalette,
    val selectedMaterialPaletteIdLight: String? = null,
    val selectedMaterialPaletteIdDark: String? = null,
    val customMaterialThemePresets: List<CustomFactionThemeSettings> = listOf(DefaultCustomMaterialPaletteSettings),
    val selectedFactionTheme: FactionThemeOption = DefaultFactionTheme,
    val customFactionThemePresets: List<CustomFactionThemeSettings> = listOf(DefaultCustomFactionThemeSettings),
    val selectedThemeStyle: ThemeStyleOption = ThemeStyleOption.FactionTheme,
    val selectedThemeMode: ThemeModeOption = ThemeModeOption.FollowSystem,
    val isMaterialDarkThemeActive: Boolean = false,
    val isDemoModeEnabled: Boolean = false,
    val isSampleAutoFillEnabled: Boolean = true,
    val isSampleDecorationEnabled: Boolean = true,
    val isSavedAudioPlaybackDataStorageEnabled: Boolean = true,
    val isFlashVisualPerfOverlayEnabled: Boolean = false,
    val isConfigLanguageExpanded: Boolean = true,
    val isConfigThemeAppearanceExpanded: Boolean = true,
    val isConfigCustomMaterialThemeExpanded: Boolean = true,
    val isConfigBuiltInMaterialPalettesExpanded: Boolean = true,
    val isConfigMaterialRedsPaletteExpanded: Boolean = true,
    val isConfigMaterialOrangesPaletteExpanded: Boolean = true,
    val isConfigMaterialYellowsPaletteExpanded: Boolean = true,
    val isConfigMaterialGreensPaletteExpanded: Boolean = true,
    val isConfigMaterialBluesPaletteExpanded: Boolean = true,
    val isConfigMaterialPurplesPaletteExpanded: Boolean = true,
    val isConfigMaterialNeutralsPaletteExpanded: Boolean = true,
    val isConfigSampleTextExpanded: Boolean = false,
    val isConfigCustomFactionThemeExpanded: Boolean = false,
    val isConfigSacredMachineFactionThemeExpanded: Boolean = false,
    val isConfigAncientDynastyFactionThemeExpanded: Boolean = false,
    val isConfigImmortalRotFactionThemeExpanded: Boolean = false,
    val isConfigScarletCarnageFactionThemeExpanded: Boolean = false,
    val isConfigExquisiteFallFactionThemeExpanded: Boolean = false,
    val isConfigLabyrinthOfMutabilityFactionThemeExpanded: Boolean = false,
    val isConfigDebugExpanded: Boolean = false,
    val isFlashVoicingEnabled: Boolean = true,
    val isDualThemeAnimationEnabled: Boolean = false,
    val selectedFlashVoicingStyle: FlashVoicingStyleOption = FlashVoicingStyleOption.Standard,
    val selectedMorseSpeed: MorseSpeedOption = MorseSpeedOption.default,
    val selectedSampleInputLength: SampleInputLengthOption = SampleInputLengthOption.Short,
    val transportMode: TransportModeOption = TransportModeOption.Flash,
    val sessions: Map<TransportModeOption, ModeAudioSessionState> = defaultModeSessions(),
    val voiceSession: VoiceSessionState = VoiceSessionState(),
    val currentPlaybackSource: AudioPlaybackSource = AudioPlaybackSource.Generated(TransportModeOption.Flash),
    val playbackSequenceMode: PlaybackSequenceMode = PlaybackSequenceMode.Normal,
    val selectedSavedAudio: SavedAudioPlaybackSelection? = null,
    val savedAudioItems: List<SavedAudioItem> = emptyList(),
    val decodedSavedAudioItemIds: Set<String> = emptySet(),
    val savedAudioFolders: List<SavedAudioFolder> = emptyList(),
    val savedAudioFolderAssignments: Map<String, String> = emptyMap(),
    val librarySelection: LibrarySelectionUiState = LibrarySelectionUiState(),
    val playerShellState: PlayerShellState = PlayerShellState(),
    val libraryStatusText: UiText = UiText.Empty,
    val snackbarMessage: SnackbarMessage? = null,
    val pendingDocumentExportRequest: PendingAudioDocumentExportRequest? = null,
) {
    val isExpandedPlayerVisible: Boolean
        get() = playerShellState.isExpandedPlayerVisible

    val isQueueVisible: Boolean
        get() = playerShellState.isQueueVisible

    val isDockQueueVisible: Boolean
        get() = playerShellState.isDockQueueVisible

    val isExpandedQueueVisible: Boolean
        get() = playerShellState.isExpandedQueueVisible

    val customFactionThemes: List<FactionThemeOption>
        get() = customFactionThemePresets.map(::customFactionTheme)

    val customMaterialThemeSettings: CustomFactionThemeSettings
        get() =
            customMaterialThemePresets
                .firstOrNull { customMaterialPalette(it).id == selectedPalette.id }
                ?: customMaterialThemePresets.first()

    val activeFactionTheme: FactionThemeOption
        get() =
            if (isCustomFactionThemeOptionId(selectedFactionTheme.id)) {
                customFactionThemes.firstOrNull { it.id == selectedFactionTheme.id } ?: customFactionThemes.first()
            } else {
                selectedFactionTheme
            }

    val currentSession: ModeAudioSessionState
        get() = sessions.getValue(transportMode)

    val currentPlayback: PlaybackUiState
        get() =
            when (val source = currentPlaybackSource) {
                is AudioPlaybackSource.Generated -> sessions.getValue(source.mode).playback
                is AudioPlaybackSource.Saved ->
                    selectedSavedAudio
                        ?.takeIf { it.item.itemId == source.itemId }
                        ?.playback
                        ?: PlaybackUiState()
            }

    val currentPlaybackSpeed: Float
        get() =
            when (val source = currentPlaybackSource) {
                is AudioPlaybackSource.Generated -> sessions.getValue(source.mode).playbackSpeed
                is AudioPlaybackSource.Saved ->
                    selectedSavedAudio
                        ?.takeIf { it.item.itemId == source.itemId }
                        ?.playbackSpeed
                        ?: com.bag.audioandroid.ui.model.PlaybackSpeedOption.default.speed
            }

    val currentPlaybackSampleCount: Int
        get() =
            when (val source = currentPlaybackSource) {
                is AudioPlaybackSource.Generated -> {
                    val session = sessions.getValue(source.mode)
                    session.generatedAudioMetadata?.pcmSampleCount?.takeIf { it > 0 }
                        ?: session.generatedPcm.size
                }
                is AudioPlaybackSource.Saved ->
                    selectedSavedAudio
                        ?.takeIf { it.item.itemId == source.itemId }
                        ?.let { saved ->
                            saved.metadata?.pcmSampleCount?.takeIf { it > 0 } ?: saved.pcm.size
                        }
                        ?: 0
            }

    val currentPlaybackVisualData: PlaybackPcmVisualData
        get() =
            when (val source = currentPlaybackSource) {
                is AudioPlaybackSource.Generated -> {
                    val session = sessions.getValue(source.mode)
                    val totalSamples =
                        session.generatedAudioMetadata?.pcmSampleCount?.takeIf { it > 0 }
                            ?: session.generatedPcm.size
                    val shouldUseWaveformPreview =
                        source.mode == TransportModeOption.Flash &&
                            session.generatedWaveformPcm.isNotEmpty() &&
                            totalSamples >= LONG_AUDIO_VISUALIZATION_SAMPLE_THRESHOLD
                    if (shouldUseWaveformPreview) {
                        PlaybackPcmVisualData(
                            samples = session.generatedWaveformPcm,
                            kind = PlaybackPcmVisualKind.WaveformPreview,
                            totalSamples = totalSamples,
                        )
                    } else if (session.generatedPcm.isNotEmpty()) {
                        PlaybackPcmVisualData(
                            samples = session.generatedPcm,
                            kind = PlaybackPcmVisualKind.FullPcm,
                            totalSamples = totalSamples,
                        )
                    } else {
                        PlaybackPcmVisualData(
                            samples = session.generatedWaveformPcm,
                            kind =
                                if (session.generatedWaveformPcm.isNotEmpty()) {
                                    PlaybackPcmVisualKind.WaveformPreview
                                } else {
                                    PlaybackPcmVisualKind.Empty
                                },
                            totalSamples = totalSamples,
                        )
                    }
                }
                is AudioPlaybackSource.Saved ->
                    selectedSavedAudio
                        ?.takeIf { it.item.itemId == source.itemId }
                        ?.let { saved ->
                            val totalSamples = saved.metadata?.pcmSampleCount?.takeIf { it > 0 } ?: saved.pcm.size
                            if (saved.pcm.isNotEmpty()) {
                                PlaybackPcmVisualData(
                                    samples = saved.pcm,
                                    kind = PlaybackPcmVisualKind.FullPcm,
                                    totalSamples = totalSamples,
                                )
                            } else {
                                PlaybackPcmVisualData(
                                    samples = saved.waveformPcm,
                                    kind =
                                        if (saved.waveformPcm.isNotEmpty()) {
                                            PlaybackPcmVisualKind.WaveformPreview
                                        } else {
                                            PlaybackPcmVisualKind.Empty
                                        },
                                    totalSamples = totalSamples,
                                )
                            }
                        }
                        ?: PlaybackPcmVisualData()
            }

    val currentPlaybackPcm: ShortArray
        get() = currentPlaybackVisualData.samples

    val currentPlaybackTransportMode: TransportModeOption?
        get() =
            when (val source = currentPlaybackSource) {
                is AudioPlaybackSource.Generated -> source.mode
                is AudioPlaybackSource.Saved -> currentSavedAudioItem?.modeWireName?.let(TransportModeOption::fromWireName)
            }

    val currentPlaybackFrameSamples: Int
        get() =
            when (val source = currentPlaybackSource) {
                is AudioPlaybackSource.Generated ->
                    sessions.getValue(source.mode).generatedAudioMetadata?.frameSamples ?: 2205
                is AudioPlaybackSource.Saved ->
                    currentSavedAudioSelection
                        ?.metadata
                        ?.frameSamples
                        ?: 2205
            }

    val currentSavedAudioSelection: SavedAudioPlaybackSelection?
        get() =
            (currentPlaybackSource as? AudioPlaybackSource.Saved)
                ?.let { source ->
                    selectedSavedAudio?.takeIf { it.item.itemId == source.itemId }
                }

    val currentSavedAudioItem: SavedAudioItem?
        get() = currentSavedAudioSelection?.item

    val currentPlaybackDetailsSourceWireName: String
        get() =
            when (currentPlaybackSource) {
                is AudioPlaybackSource.Generated -> "generated-session"
                is AudioPlaybackSource.Saved ->
                    currentSavedAudioSelection
                        ?.playbackDetailsSource
                        ?.wireName
                        ?: "saved-unknown"
            }

    val showCurrentSavedAudioDecodeLoadingNotice: Boolean
        get() =
            currentSavedAudioSelection
                ?.let { it.needsDecodedContent || it.isDecodingContent }
                ?: false

    val currentSavedAudioDecodeProgressSnapshot: DecodeOperationSnapshot?
        get() = currentSavedAudioSelection?.decodeOperationSnapshot

    val currentPlaybackDecodedPayload: DecodedPayloadViewData?
        get() =
            when (val source = currentPlaybackSource) {
                is AudioPlaybackSource.Generated -> sessions.getValue(source.mode).decodedPayload
                is AudioPlaybackSource.Saved -> currentSavedAudioSelection?.decodedPayload
            }

    val audioTabDecodedPayload: DecodedPayloadViewData
        get() = currentPlaybackDecodedPayload ?: DecodedPayloadViewData.Empty

    val audioTabCodecBusy: Boolean
        get() =
            currentSession.isCodecBusy ||
                when (val source = currentPlaybackSource) {
                    is AudioPlaybackSource.Generated -> sessions.getValue(source.mode).isCodecBusy
                    is AudioPlaybackSource.Saved -> currentSession.isCodecBusy
                }

    val audioTabDecodeSourceLabel: UiText
        get() =
            when (val source = currentPlaybackSource) {
                is AudioPlaybackSource.Generated ->
                    UiText.Resource(
                        R.string.audio_decode_source_generated,
                        listOf(source.mode.fixedEnglishLabel),
                    )
                is AudioPlaybackSource.Saved ->
                    UiText.Resource(
                        R.string.audio_decode_source_saved,
                        listOf(currentSavedAudioItem?.displayName.orEmpty()),
                    )
            }

    val audioTabDecodeBusyReason: UiText
        get() =
            if (!audioTabCodecBusy) {
                UiText.Empty
            } else {
                when (currentPlaybackSource) {
                    is AudioPlaybackSource.Generated ->
                        UiText.Resource(R.string.audio_decode_busy_reason_generated)
                    is AudioPlaybackSource.Saved ->
                        UiText.Resource(R.string.audio_decode_busy_reason_saved)
                }
            }

    val currentPlaybackFollowData: PayloadFollowViewData
        get() =
            when (val source = currentPlaybackSource) {
                is AudioPlaybackSource.Generated -> sessions.getValue(source.mode).followData
                is AudioPlaybackSource.Saved -> currentSavedAudioSelection?.followData ?: PayloadFollowViewData.Empty
            }

    val currentPlaybackLyricsNavigatorReadingModel: LyricsNavigatorReadingModel?
        get() =
            when (val source = currentPlaybackSource) {
                is AudioPlaybackSource.Generated -> sessions.getValue(source.mode).lyricsNavigatorReadingModel
                is AudioPlaybackSource.Saved -> null
            }

    val currentPlaybackFlashVisualWindow: FlashVisualWindowState
        get() =
            when (val source = currentPlaybackSource) {
                is AudioPlaybackSource.Generated -> sessions.getValue(source.mode).flashVisualWindow
                is AudioPlaybackSource.Saved -> FlashVisualWindowState()
            }

    val currentPlaybackFlashSignalInfo: FlashSignalInfo
        get() =
            when (val source = currentPlaybackSource) {
                is AudioPlaybackSource.Generated -> sessions.getValue(source.mode).generatedFlashSignalInfo
                is AudioPlaybackSource.Saved -> currentSavedAudioSelection?.flashSignalInfo ?: FlashSignalInfo.Empty
            }

    val currentPlaybackWavAudioInfo: WavAudioInfo
        get() =
            when (val source = currentPlaybackSource) {
                is AudioPlaybackSource.Generated -> sessions.getValue(source.mode).generatedWavAudioInfo
                is AudioPlaybackSource.Saved -> currentSavedAudioSelection?.wavAudioInfo ?: WavAudioInfo.Empty
            }

    val miniPlayerModel: MiniPlayerUiModel?
        get() =
            when (val source = currentPlaybackSource) {
                is AudioPlaybackSource.Generated -> {
                    val session = sessions.getValue(source.mode)
                    val pcmSampleCount =
                        session.generatedAudioMetadata?.pcmSampleCount?.takeIf { it > 0 }
                            ?: session.generatedPcm.size
                    if (pcmSampleCount <= 0 || (session.generatedPcm.isEmpty() && session.generatedPcmFilePath == null)) {
                        null
                    } else {
                        val durationMs =
                            samplesToDurationMillis(
                                sampleCount = currentPlayback.totalSamples.takeIf { it > 0 } ?: pcmSampleCount,
                                sampleRateHz = currentPlayback.sampleRateHz,
                            )
                        MiniPlayerUiModel(
                            title =
                                UiText.Resource(
                                    R.string.audio_mini_player_generated_title,
                                    listOf(UiText.Resource(source.mode.labelResId)),
                                ),
                            subtitle = generatedMiniPlayerSubtitle(source.mode, session.generatedFlashVoicingStyle, durationMs),
                            leadingIcon = MiniPlayerLeadingIcon.Generated,
                            durationMs = durationMs,
                            transportMode = source.mode,
                            isFlashMode = source.mode == TransportModeOption.Flash,
                            flashVoicingStyle = session.generatedFlashVoicingStyle,
                            source = MiniPlayerSource.Generated,
                        )
                    }
                }

                is AudioPlaybackSource.Saved ->
                    currentSavedAudioItem?.let { item ->
                        MiniPlayerUiModel(
                            title = UiText.Plain(item.displayName),
                            subtitle =
                                savedMiniPlayerSubtitle(
                                    modeWireName = item.modeWireName,
                                    flashVoicingStyle = item.flashVoicingStyle,
                                    durationMs = item.durationMs,
                                ),
                            leadingIcon = MiniPlayerLeadingIcon.Saved,
                            durationMs = item.durationMs,
                            transportMode =
                                TransportModeOption.fromWireName(item.modeWireName) ?: TransportModeOption.Flash,
                            isFlashMode = item.modeWireName == TransportModeOption.Flash.wireName,
                            flashVoicingStyle = item.flashVoicingStyle,
                            source = MiniPlayerSource.Saved,
                        )
                    }
            }

    val canSkipPrevious: Boolean
        get() {
            val currentItemId = currentSavedAudioItem?.itemId ?: return false
            val currentIndex = savedAudioItems.indexOfFirst { it.itemId == currentItemId }
            return currentIndex > 0
        }

    val canSkipNext: Boolean
        get() {
            val currentItemId = currentSavedAudioItem?.itemId ?: return false
            val currentIndex = savedAudioItems.indexOfFirst { it.itemId == currentItemId }
            return currentIndex >= 0 && currentIndex < savedAudioItems.lastIndex
        }
}

private fun defaultModeSessions(): Map<TransportModeOption, ModeAudioSessionState> =
    TransportModeOption.entries.associateWith { ModeAudioSessionState() }

private fun samplesToDurationMillis(
    sampleCount: Int,
    sampleRateHz: Int,
): Long {
    if (sampleCount <= 0 || sampleRateHz <= 0) {
        return 0L
    }
    return (sampleCount.toLong() * 1000L) / sampleRateHz.toLong()
}

private fun generatedMiniPlayerSubtitle(
    mode: TransportModeOption,
    flashVoicingStyle: FlashVoicingStyleOption?,
    durationMs: Long,
): UiText {
    val durationText = formatDurationMillis(durationMs)
    return if (mode == TransportModeOption.Flash && flashVoicingStyle != null) {
        UiText.Resource(
            R.string.audio_mini_player_generated_flash_subtitle,
            listOf(UiText.Resource(flashVoicingStyle.labelResId), durationText),
        )
    } else {
        UiText.Resource(
            R.string.audio_mini_player_duration_only,
            listOf(durationText),
        )
    }
}

private fun savedMiniPlayerSubtitle(
    modeWireName: String,
    flashVoicingStyle: FlashVoicingStyleOption?,
    durationMs: Long,
): UiText {
    val isFlashMode = modeWireName == TransportModeOption.Flash.wireName
    val modeLabel =
        if (isFlashMode && flashVoicingStyle != null) {
            UiText.Resource(flashVoicingStyle.labelResId)
        } else {
            UiText.Resource(SavedAudioModeFilter.labelResIdForModeWireName(modeWireName))
        }
    return UiText.Resource(
        if (isFlashMode && flashVoicingStyle != null) {
            R.string.audio_mini_player_generated_flash_subtitle
        } else {
            R.string.audio_mini_player_saved_subtitle
        },
        listOf(modeLabel, formatDurationMillis(durationMs)),
    )
}

internal fun AudioAppUiState.materialPaletteIdForMode(isDarkTheme: Boolean): String? =
    if (isDarkTheme) {
        selectedMaterialPaletteIdDark ?: selectedMaterialPaletteIdLight ?: selectedPalette.id
    } else {
        selectedMaterialPaletteIdLight ?: selectedMaterialPaletteIdDark ?: selectedPalette.id
    }

internal fun AudioAppUiState.withMaterialDarkThemeActive(isDarkTheme: Boolean): AudioAppUiState =
    if (isMaterialDarkThemeActive == isDarkTheme) {
        this
    } else {
        copy(
            isMaterialDarkThemeActive = isDarkTheme,
            selectedPalette = resolveMaterialPaletteById(materialPaletteIdForMode(isDarkTheme)),
        )
    }

internal fun AudioAppUiState.withSelectedThemeMode(themeMode: ThemeModeOption): AudioAppUiState {
    val resolvedDarkTheme =
        when (themeMode) {
            ThemeModeOption.Light -> false
            ThemeModeOption.Dark -> true
            ThemeModeOption.FollowSystem -> isMaterialDarkThemeActive
        }
    return copy(selectedThemeMode = themeMode).withMaterialDarkThemeActive(resolvedDarkTheme)
}

internal fun AudioAppUiState.resolveMaterialPaletteById(paletteId: String?): PaletteOption =
    if (paletteId != null && isCustomMaterialPaletteId(paletteId)) {
        customMaterialThemePresets
            .map(::customMaterialPalette)
            .firstOrNull { it.id == paletteId }
            ?: customMaterialPalette(customMaterialThemePresets.first())
    } else {
        MaterialPalettes.firstOrNull { it.id == paletteId } ?: DefaultMaterialPalette
    }

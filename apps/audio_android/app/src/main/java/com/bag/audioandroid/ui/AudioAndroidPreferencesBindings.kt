package com.bag.audioandroid.ui

import com.bag.audioandroid.data.AppSettingsRepository
import com.bag.audioandroid.ui.model.AppTab
import com.bag.audioandroid.ui.model.CustomFactionThemeSettings
import com.bag.audioandroid.ui.model.DefaultCustomFactionThemeSettings
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.MorseSpeedOption
import com.bag.audioandroid.ui.model.PlaybackSequenceMode
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.ThemeModeOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.model.VoiceInputSourceOption
import com.bag.audioandroid.ui.model.VoiceTrackModeOption
import com.bag.audioandroid.ui.model.VoiceWorkflowModeOption
import com.bag.audioandroid.ui.model.defaultPreset
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.materialPaletteIdForMode
import com.bag.audioandroid.ui.state.resolveMaterialPaletteById
import com.bag.audioandroid.ui.theme.DefaultCustomMaterialPaletteSettings
import com.bag.audioandroid.ui.theme.DefaultFactionTheme
import com.bag.audioandroid.ui.theme.DefaultMaterialPalette
import com.bag.audioandroid.ui.theme.FactionThemes
import com.bag.audioandroid.ui.theme.MaterialPalettes
import com.bag.audioandroid.ui.theme.customFactionTheme
import com.bag.audioandroid.ui.theme.isCustomFactionThemeOptionId
import com.bag.audioandroid.ui.theme.isCustomMaterialPaletteId
import com.bag.audioandroid.ui.theme.normalizeCustomMaterialThemeSettings
import com.bag.audioandroid.ui.theme.normalizeFactionThemeHex
import com.bag.audioandroid.ui.theme.normalizeFactionThemeHexOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Suppress("LargeClass")
internal class AudioAndroidPreferencesBindings(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val sampleInputSessionUpdater: SampleInputSessionUpdater,
    private val appSettingsRepository: AppSettingsRepository,
    private val scope: CoroutineScope,
) {
    fun startObserving() {
        observeSelectedTab()
        observeSelectedVoiceWorkflowMode()
        observeSelectedVoiceTrackMode()
        observeSelectedVoiceInputSource()
        observeSelectedPalette()
        observeSelectedThemeMode()
        observeSelectedMaterialPaletteIdLight()
        observeSelectedMaterialPaletteIdDark()
        observeSelectedThemeStyle()
        observeCustomMaterialThemeSettings()
        observeCustomFactionThemeSettings()
        observeSelectedFactionTheme()
        observeSelectedFlashVoicingStyle()
        observeSelectedMorseSpeedStyle()
        observeSelectedSampleInputLength()
        observeFlashVoicingEnabled()
        observeSelectedPlaybackSequenceMode()
        observeConfigLanguageExpanded()
        observeConfigThemeAppearanceExpanded()
        observeConfigCustomMaterialThemeExpanded()
        observeConfigBuiltInMaterialPalettesExpanded()
        observeConfigMaterialRedsPaletteExpanded()
        observeConfigMaterialOrangesPaletteExpanded()
        observeConfigMaterialYellowsPaletteExpanded()
        observeConfigMaterialGreensPaletteExpanded()
        observeConfigMaterialBluesPaletteExpanded()
        observeConfigMaterialPurplesPaletteExpanded()
        observeConfigMaterialNeutralsPaletteExpanded()
        observeConfigCustomFactionThemeExpanded()
        observeConfigSampleTextExpanded()
        observeConfigSacredMachineFactionThemeExpanded()
        observeConfigAncientDynastyFactionThemeExpanded()
        observeConfigImmortalRotFactionThemeExpanded()
        observeConfigScarletCarnageFactionThemeExpanded()
        observeConfigExquisiteFallFactionThemeExpanded()
        observeConfigLabyrinthOfMutabilityFactionThemeExpanded()
        observeConfigDebugExpanded()
        observeDemoModeEnabled()
        observeDualThemeAnimationEnabled()
        observeSampleAutoFillEnabled()
        observeSampleDecorationEnabled()
        observeSavedAudioPlaybackDataStorageEnabled()
        observeFlashVisualPerfOverlayEnabled()
    }

    private fun observeSelectedTab() {
        scope.launch {
            appSettingsRepository.selectedTabId
                .distinctUntilChanged()
                .collect { tabId ->
                    if (tabId == null) {
                        return@collect
                    }
                    val selectedTab = AppTab.fromAutomationId(tabId)
                    uiState.update { state ->
                        if (state.selectedTab == selectedTab) {
                            state
                        } else {
                            state.copy(selectedTab = selectedTab)
                        }
                    }
                }
        }
    }

    private fun observeSelectedVoiceWorkflowMode() {
        scope.launch {
            appSettingsRepository.selectedVoiceWorkflowModeId
                .distinctUntilChanged()
                .collect { modeId ->
                    if (modeId == null) {
                        return@collect
                    }
                    val selectedMode = VoiceWorkflowModeOption.fromId(modeId)
                    uiState.update { state ->
                        if (state.voiceSession.selectedWorkflowMode == selectedMode) {
                            state
                        } else {
                            state.copy(
                                voiceSession = state.voiceSession.copy(selectedWorkflowMode = selectedMode),
                            )
                        }
                    }
                }
        }
    }

    private fun observeSelectedVoiceInputSource() {
        scope.launch {
            appSettingsRepository.selectedVoiceInputSourceId
                .distinctUntilChanged()
                .collect { sourceId ->
                    if (sourceId == null) {
                        return@collect
                    }
                    val selectedSource = VoiceInputSourceOption.fromId(sourceId)
                    uiState.update { state ->
                        if (state.voiceSession.selectedInputSource == selectedSource) {
                            state
                        } else {
                            state.copy(
                                voiceSession = state.voiceSession.copy(selectedInputSource = selectedSource),
                            )
                        }
                    }
                }
        }
    }

    private fun observeSelectedVoiceTrackMode() {
        scope.launch {
            appSettingsRepository.selectedVoiceTrackModeId
                .distinctUntilChanged()
                .collect { modeId ->
                    if (modeId == null) {
                        return@collect
                    }
                    val selectedMode = VoiceTrackModeOption.fromId(modeId)
                    uiState.update { state ->
                        if (state.voiceSession.selectedTrackMode == selectedMode) {
                            state
                        } else {
                            val nextPreset =
                                if (state.voiceSession.selectedPreset.trackMode == selectedMode) {
                                    state.voiceSession.selectedPreset
                                } else {
                                    selectedMode.defaultPreset()
                                }
                            state.copy(
                                voiceSession =
                                    state.voiceSession.copy(
                                        selectedTrackMode = selectedMode,
                                        selectedPreset = nextPreset,
                                    ),
                            )
                        }
                    }
                }
        }
    }

    private fun observeSelectedPalette() {
        scope.launch {
            appSettingsRepository.selectedPaletteId
                .distinctUntilChanged()
                .collect { paletteId ->
                    uiState.update { state ->
                        val resolvedPalette =
                            state.resolveMaterialPaletteById(
                                state.materialPaletteIdForMode(state.isMaterialDarkThemeActive) ?: paletteId,
                            )
                        if (state.selectedPalette.id == resolvedPalette.id &&
                            state.selectedPalette.lightScheme == resolvedPalette.lightScheme &&
                            state.selectedPalette.darkScheme == resolvedPalette.darkScheme
                        ) {
                            state
                        } else {
                            state.copy(selectedPalette = resolvedPalette)
                        }
                    }
                }
        }
    }

    private fun observeCustomMaterialThemeSettings() {
        scope.launch {
            appSettingsRepository.customMaterialThemePresets
                .map { presets ->
                    val normalizedPresets = presets.map(::normalizeCustomMaterialThemeSettings)
                    if (normalizedPresets.isEmpty()) {
                        listOf(DefaultCustomMaterialPaletteSettings)
                    } else {
                        normalizedPresets
                    }
                }.distinctUntilChanged()
                .collect { presets ->
                    uiState.update { state ->
                        val selectedCustomPalette =
                            state.resolveMaterialPaletteById(
                                state.materialPaletteIdForMode(state.isMaterialDarkThemeActive),
                            )
                        state.copy(
                            customMaterialThemePresets = presets,
                            selectedPalette = selectedCustomPalette,
                        )
                    }
                }
        }
    }

    private fun observeSelectedThemeMode() {
        scope.launch {
            appSettingsRepository.selectedThemeModeId
                .distinctUntilChanged()
                .collect { themeModeId ->
                    val themeMode = ThemeModeOption.fromId(themeModeId)
                    uiState.update { state ->
                        if (state.selectedThemeMode == themeMode) {
                            state
                        } else {
                            state.copy(selectedThemeMode = themeMode)
                        }
                    }
                }
        }
    }

    private fun observeSelectedThemeStyle() {
        scope.launch {
            combine(
                appSettingsRepository.selectedThemeStyleId,
                appSettingsRepository.selectedPaletteId,
                appSettingsRepository.selectedMaterialPaletteIdLight,
                appSettingsRepository.selectedMaterialPaletteIdDark,
            ) { themeStyleId, paletteId, lightPaletteId, darkPaletteId ->
                inferPersistedThemeStyle(
                    themeStyleId = themeStyleId,
                    paletteId = paletteId,
                    lightPaletteId = lightPaletteId,
                    darkPaletteId = darkPaletteId,
                )
            }.distinctUntilChanged()
                .collect { themeStyle ->
                    uiState.update { state ->
                        state.withSelectedThemeStyle(themeStyle, sampleInputSessionUpdater)
                    }
                }
        }
    }

    private fun observeSelectedFactionTheme() {
        scope.launch {
            appSettingsRepository.selectedFactionThemeId
                .distinctUntilChanged()
                .collect { factionThemeId ->
                    uiState.update { state ->
                        val factionTheme =
                            if (factionThemeId != null && isCustomFactionThemeOptionId(factionThemeId)) {
                                state.customFactionThemes.firstOrNull { it.id == factionThemeId } ?: state.customFactionThemes.first()
                            } else {
                                FactionThemes.firstOrNull { it.id == factionThemeId } ?: DefaultFactionTheme
                            }
                        state.withSelectedFactionTheme(factionTheme, sampleInputSessionUpdater)
                    }
                }
        }
    }

    private fun observeCustomFactionThemeSettings() {
        scope.launch {
            appSettingsRepository.customFactionThemePresets
                .map { presets ->
                    val normalizedPresets =
                        presets.map { settings ->
                            CustomFactionThemeSettings(
                                presetId = settings.presetId,
                                displayName = settings.displayName.trim().ifBlank { DefaultCustomFactionThemeSettings.displayName },
                                primaryHex =
                                    normalizeFactionThemeHex(settings.primaryHex)
                                        ?: DefaultCustomFactionThemeSettings.primaryHex,
                                secondaryHex =
                                    normalizeFactionThemeHex(settings.secondaryHex)
                                        ?: DefaultCustomFactionThemeSettings.secondaryHex,
                                outlineHexOrNull =
                                    settings.outlineHexOrNull?.let { outlineHex ->
                                        normalizeFactionThemeHexOrNull(outlineHex) ?: DefaultCustomFactionThemeSettings.outlineHexOrNull
                                    },
                            )
                        }
                    if (normalizedPresets.isEmpty()) {
                        listOf(DefaultCustomFactionThemeSettings)
                    } else {
                        normalizedPresets
                    }
                }.distinctUntilChanged()
                .collect { presets ->
                    uiState.update { state ->
                        val presetThemes = presets.map(::customFactionTheme)
                        val selectedFactionTheme =
                            if (isCustomFactionThemeOptionId(state.selectedFactionTheme.id)) {
                                presetThemes.firstOrNull { it.id == state.selectedFactionTheme.id } ?: presetThemes.first()
                            } else {
                                state.selectedFactionTheme
                            }
                        state.copy(
                            customFactionThemePresets = presets,
                            selectedFactionTheme = selectedFactionTheme,
                        )
                    }
                }
        }
    }

    private fun observeSelectedFlashVoicingStyle() {
        scope.launch {
            appSettingsRepository.selectedFlashVoicingStyleId
                .distinctUntilChanged()
                .collect { styleId ->
                    val style = FlashVoicingStyleOption.fromId(styleId)
                    uiState.update { state ->
                        if (state.selectedFlashVoicingStyle == style) {
                            state
                        } else {
                            state.copy(selectedFlashVoicingStyle = style)
                        }
                    }
                }
        }
    }

    private fun observeFlashVoicingEnabled() {
        scope.launch {
            appSettingsRepository.isFlashVoicingEnabled
                .distinctUntilChanged()
                .collect { enabled ->
                    uiState.update { state ->
                        if (state.isFlashVoicingEnabled == enabled) {
                            state
                        } else {
                            state.copy(isFlashVoicingEnabled = enabled)
                        }
                    }
                }
        }
    }

    private fun observeSelectedPlaybackSequenceMode() {
        scope.launch {
            appSettingsRepository.selectedPlaybackSequenceModeId
                .distinctUntilChanged()
                .collect { modeId ->
                    val mode = PlaybackSequenceMode.fromId(modeId)
                    uiState.update { state ->
                        if (state.playbackSequenceMode == mode) {
                            state
                        } else {
                            state.copy(playbackSequenceMode = mode)
                        }
                    }
                }
        }
    }

    private fun observeConfigLanguageExpanded() {
        scope.launch {
            appSettingsRepository.isConfigLanguageExpanded
                .distinctUntilChanged()
                .collect { expanded ->
                    uiState.update { state ->
                        if (state.isConfigLanguageExpanded == expanded) state else state.copy(isConfigLanguageExpanded = expanded)
                    }
                }
        }
    }

    private fun observeConfigThemeAppearanceExpanded() {
        scope.launch {
            appSettingsRepository.isConfigThemeAppearanceExpanded
                .distinctUntilChanged()
                .collect { expanded ->
                    uiState.update { state ->
                        if (state.isConfigThemeAppearanceExpanded == expanded) {
                            state
                        } else {
                            state.copy(isConfigThemeAppearanceExpanded = expanded)
                        }
                    }
                }
        }
    }

    private fun observeSelectedMorseSpeedStyle() {
        scope.launch {
            appSettingsRepository.selectedMorseSpeedStyleId
                .distinctUntilChanged()
                .collect { styleId ->
                    val style = MorseSpeedOption.fromId(styleId)
                    uiState.update { state ->
                        if (state.selectedMorseSpeed == style) {
                            state
                        } else {
                            state.copy(selectedMorseSpeed = style)
                        }
                    }
                }
        }
    }

    private fun observeSelectedSampleInputLength() {
        scope.launch {
            appSettingsRepository.selectedSampleInputLengthId
                .distinctUntilChanged()
                .collect { lengthId ->
                    val length = SampleInputLengthOption.fromId(lengthId) ?: SampleInputLengthOption.Short
                    uiState.update { state ->
                        if (state.selectedSampleInputLength == length) {
                            state
                        } else {
                            state.copy(selectedSampleInputLength = length)
                        }
                    }
                }
        }
    }

    private fun observeSelectedMaterialPaletteIdLight() {
        scope.launch {
            appSettingsRepository.selectedMaterialPaletteIdLight
                .distinctUntilChanged()
                .collect { paletteId ->
                    uiState.update { state ->
                        state.copy(
                            selectedMaterialPaletteIdLight = paletteId,
                            selectedPalette =
                                if (!state.isMaterialDarkThemeActive && paletteId != null) {
                                    state.resolveMaterialPaletteById(paletteId)
                                } else {
                                    state.selectedPalette
                                },
                        )
                    }
                }
        }
    }

    private fun observeSelectedMaterialPaletteIdDark() {
        scope.launch {
            appSettingsRepository.selectedMaterialPaletteIdDark
                .distinctUntilChanged()
                .collect { paletteId ->
                    uiState.update { state ->
                        state.copy(
                            selectedMaterialPaletteIdDark = paletteId,
                            selectedPalette =
                                if (state.isMaterialDarkThemeActive && paletteId != null) {
                                    state.resolveMaterialPaletteById(paletteId)
                                } else {
                                    state.selectedPalette
                                },
                        )
                    }
                }
        }
    }

    private fun observeConfigCustomMaterialThemeExpanded() {
        scope.launch {
            appSettingsRepository.isConfigCustomMaterialThemeExpanded
                .distinctUntilChanged()
                .collect { expanded ->
                    uiState.update { state ->
                        if (state.isConfigCustomMaterialThemeExpanded == expanded) {
                            state
                        } else {
                            state.copy(isConfigCustomMaterialThemeExpanded = expanded)
                        }
                    }
                }
        }
    }

    private fun observeConfigBuiltInMaterialPalettesExpanded() {
        scope.launch {
            appSettingsRepository.isConfigBuiltInMaterialPalettesExpanded
                .distinctUntilChanged()
                .collect { expanded ->
                    uiState.update { state ->
                        if (state.isConfigBuiltInMaterialPalettesExpanded == expanded) {
                            state
                        } else {
                            state.copy(isConfigBuiltInMaterialPalettesExpanded = expanded)
                        }
                    }
                }
        }
    }

    private fun observeConfigMaterialRedsPaletteExpanded() {
        scope.launch {
            appSettingsRepository.isConfigMaterialRedsPaletteExpanded
                .distinctUntilChanged()
                .collect { expanded ->
                    uiState.update { state ->
                        if (state.isConfigMaterialRedsPaletteExpanded == expanded) {
                            state
                        } else {
                            state.copy(isConfigMaterialRedsPaletteExpanded = expanded)
                        }
                    }
                }
        }
    }

    private fun observeConfigMaterialOrangesPaletteExpanded() {
        scope.launch {
            appSettingsRepository.isConfigMaterialOrangesPaletteExpanded
                .distinctUntilChanged()
                .collect { expanded ->
                    uiState.update { state ->
                        if (state.isConfigMaterialOrangesPaletteExpanded == expanded) {
                            state
                        } else {
                            state.copy(isConfigMaterialOrangesPaletteExpanded = expanded)
                        }
                    }
                }
        }
    }

    private fun observeConfigMaterialYellowsPaletteExpanded() {
        scope.launch {
            appSettingsRepository.isConfigMaterialYellowsPaletteExpanded
                .distinctUntilChanged()
                .collect { expanded ->
                    uiState.update { state ->
                        if (state.isConfigMaterialYellowsPaletteExpanded == expanded) {
                            state
                        } else {
                            state.copy(isConfigMaterialYellowsPaletteExpanded = expanded)
                        }
                    }
                }
        }
    }

    private fun observeConfigMaterialGreensPaletteExpanded() {
        scope.launch {
            appSettingsRepository.isConfigMaterialGreensPaletteExpanded
                .distinctUntilChanged()
                .collect { expanded ->
                    uiState.update { state ->
                        if (state.isConfigMaterialGreensPaletteExpanded == expanded) {
                            state
                        } else {
                            state.copy(isConfigMaterialGreensPaletteExpanded = expanded)
                        }
                    }
                }
        }
    }

    private fun observeConfigMaterialBluesPaletteExpanded() {
        scope.launch {
            appSettingsRepository.isConfigMaterialBluesPaletteExpanded
                .distinctUntilChanged()
                .collect { expanded ->
                    uiState.update { state ->
                        if (state.isConfigMaterialBluesPaletteExpanded == expanded) {
                            state
                        } else {
                            state.copy(isConfigMaterialBluesPaletteExpanded = expanded)
                        }
                    }
                }
        }
    }

    private fun observeConfigMaterialPurplesPaletteExpanded() {
        scope.launch {
            appSettingsRepository.isConfigMaterialPurplesPaletteExpanded
                .distinctUntilChanged()
                .collect { expanded ->
                    uiState.update { state ->
                        if (state.isConfigMaterialPurplesPaletteExpanded == expanded) {
                            state
                        } else {
                            state.copy(isConfigMaterialPurplesPaletteExpanded = expanded)
                        }
                    }
                }
        }
    }

    private fun observeConfigMaterialNeutralsPaletteExpanded() {
        scope.launch {
            appSettingsRepository.isConfigMaterialNeutralsPaletteExpanded
                .distinctUntilChanged()
                .collect { expanded ->
                    uiState.update { state ->
                        if (state.isConfigMaterialNeutralsPaletteExpanded == expanded) {
                            state
                        } else {
                            state.copy(isConfigMaterialNeutralsPaletteExpanded = expanded)
                        }
                    }
                }
        }
    }

    private fun observeConfigCustomFactionThemeExpanded() {
        scope.launch {
            appSettingsRepository.isConfigCustomFactionThemeExpanded
                .distinctUntilChanged()
                .collect { expanded ->
                    uiState.update { state ->
                        if (state.isConfigCustomFactionThemeExpanded == expanded) {
                            state
                        } else {
                            state.copy(isConfigCustomFactionThemeExpanded = expanded)
                        }
                    }
                }
        }
    }

    private fun observeConfigSampleTextExpanded() {
        scope.launch {
            appSettingsRepository.isConfigSampleTextExpanded
                .distinctUntilChanged()
                .collect { expanded ->
                    uiState.update { state ->
                        if (state.isConfigSampleTextExpanded == expanded) {
                            state
                        } else {
                            state.copy(isConfigSampleTextExpanded = expanded)
                        }
                    }
                }
        }
    }

    private fun observeConfigSacredMachineFactionThemeExpanded() {
        scope.launch {
            appSettingsRepository.isConfigSacredMachineFactionThemeExpanded
                .distinctUntilChanged()
                .collect { expanded ->
                    uiState.update { state ->
                        if (state.isConfigSacredMachineFactionThemeExpanded ==
                            expanded
                        ) {
                            state
                        } else {
                            state.copy(isConfigSacredMachineFactionThemeExpanded = expanded)
                        }
                    }
                }
        }
    }

    private fun observeConfigAncientDynastyFactionThemeExpanded() {
        scope.launch {
            appSettingsRepository.isConfigAncientDynastyFactionThemeExpanded
                .distinctUntilChanged()
                .collect { expanded ->
                    uiState.update { state ->
                        if (state.isConfigAncientDynastyFactionThemeExpanded ==
                            expanded
                        ) {
                            state
                        } else {
                            state.copy(isConfigAncientDynastyFactionThemeExpanded = expanded)
                        }
                    }
                }
        }
    }

    private fun observeConfigImmortalRotFactionThemeExpanded() {
        scope.launch {
            appSettingsRepository.isConfigImmortalRotFactionThemeExpanded
                .distinctUntilChanged()
                .collect { expanded ->
                    uiState.update { state ->
                        if (state.isConfigImmortalRotFactionThemeExpanded ==
                            expanded
                        ) {
                            state
                        } else {
                            state.copy(isConfigImmortalRotFactionThemeExpanded = expanded)
                        }
                    }
                }
        }
    }

    private fun observeConfigScarletCarnageFactionThemeExpanded() {
        scope.launch {
            appSettingsRepository.isConfigScarletCarnageFactionThemeExpanded
                .distinctUntilChanged()
                .collect { expanded ->
                    uiState.update { state ->
                        if (state.isConfigScarletCarnageFactionThemeExpanded ==
                            expanded
                        ) {
                            state
                        } else {
                            state.copy(isConfigScarletCarnageFactionThemeExpanded = expanded)
                        }
                    }
                }
        }
    }

    private fun observeConfigExquisiteFallFactionThemeExpanded() {
        scope.launch {
            appSettingsRepository.isConfigExquisiteFallFactionThemeExpanded
                .distinctUntilChanged()
                .collect { expanded ->
                    uiState.update { state ->
                        if (state.isConfigExquisiteFallFactionThemeExpanded ==
                            expanded
                        ) {
                            state
                        } else {
                            state.copy(isConfigExquisiteFallFactionThemeExpanded = expanded)
                        }
                    }
                }
        }
    }

    private fun observeConfigLabyrinthOfMutabilityFactionThemeExpanded() {
        scope.launch {
            appSettingsRepository.isConfigLabyrinthOfMutabilityFactionThemeExpanded
                .distinctUntilChanged()
                .collect { expanded ->
                    uiState.update { state ->
                        if (state.isConfigLabyrinthOfMutabilityFactionThemeExpanded ==
                            expanded
                        ) {
                            state
                        } else {
                            state.copy(isConfigLabyrinthOfMutabilityFactionThemeExpanded = expanded)
                        }
                    }
                }
        }
    }

    private fun observeConfigDebugExpanded() {
        scope.launch {
            appSettingsRepository.isConfigDebugExpanded
                .distinctUntilChanged()
                .collect { expanded ->
                    uiState.update { state ->
                        if (state.isConfigDebugExpanded == expanded) state else state.copy(isConfigDebugExpanded = expanded)
                    }
                }
        }
    }

    private fun observeDemoModeEnabled() {
        scope.launch {
            appSettingsRepository.isDemoModeEnabled
                .distinctUntilChanged()
                .collect { enabled ->
                    uiState.update { state ->
                        if (state.isDemoModeEnabled == enabled) {
                            state
                        } else {
                            state.copy(isDemoModeEnabled = enabled)
                        }
                    }
                }
        }
    }

    private fun observeDualThemeAnimationEnabled() {
        scope.launch {
            appSettingsRepository.isDualThemeAnimationEnabled
                .distinctUntilChanged()
                .collect { enabled ->
                    uiState.update { state ->
                        if (state.isDualThemeAnimationEnabled == enabled) {
                            state
                        } else {
                            state.copy(isDualThemeAnimationEnabled = enabled)
                        }
                    }
                }
        }
    }

    private fun observeSampleDecorationEnabled() {
        scope.launch {
            appSettingsRepository.isSampleDecorationEnabled
                .distinctUntilChanged()
                .collect { enabled ->
                    uiState.update { state ->
                        if (state.isSampleDecorationEnabled == enabled) {
                            state
                        } else {
                            state.copy(isSampleDecorationEnabled = enabled)
                        }
                    }
                }
        }
    }

    private fun observeSampleAutoFillEnabled() {
        scope.launch {
            appSettingsRepository.isSampleAutoFillEnabled
                .distinctUntilChanged()
                .collect { enabled ->
                    uiState.update { state ->
                        if (state.isSampleAutoFillEnabled == enabled) {
                            state
                        } else {
                            state.copy(isSampleAutoFillEnabled = enabled)
                        }
                    }
                }
        }
    }

    private fun observeSavedAudioPlaybackDataStorageEnabled() {
        scope.launch {
            appSettingsRepository.isSavedAudioPlaybackDataStorageEnabled
                .distinctUntilChanged()
                .collect { enabled ->
                    uiState.update { state ->
                        if (state.isSavedAudioPlaybackDataStorageEnabled == enabled) {
                            state
                        } else {
                            state.copy(isSavedAudioPlaybackDataStorageEnabled = enabled)
                        }
                    }
                }
        }
    }

    private fun observeFlashVisualPerfOverlayEnabled() {
        scope.launch {
            appSettingsRepository.isFlashVisualPerfOverlayEnabled
                .distinctUntilChanged()
                .collect { enabled ->
                    uiState.update { state ->
                        if (state.isFlashVisualPerfOverlayEnabled == enabled) {
                            state
                        } else {
                            state.copy(isFlashVisualPerfOverlayEnabled = enabled)
                        }
                    }
                }
        }
    }
}

internal fun inferPersistedThemeStyle(
    themeStyleId: String?,
    paletteId: String?,
    lightPaletteId: String?,
    darkPaletteId: String?,
): ThemeStyleOption {
    if (themeStyleId != null) {
        return ThemeStyleOption.fromId(themeStyleId)
    }

    val persistedMaterialPaletteIds = listOfNotNull(paletteId, lightPaletteId, darkPaletteId)
    val hasCustomMaterialPalette = persistedMaterialPaletteIds.any(::isCustomMaterialPaletteId)
    val hasNonDefaultBuiltInMaterialPalette =
        persistedMaterialPaletteIds.any { persistedId ->
            persistedId != DefaultMaterialPalette.id && MaterialPalettes.any { palette -> palette.id == persistedId }
        }

    return if (hasCustomMaterialPalette || hasNonDefaultBuiltInMaterialPalette) {
        ThemeStyleOption.Material
    } else {
        ThemeStyleOption.FactionTheme
    }
}

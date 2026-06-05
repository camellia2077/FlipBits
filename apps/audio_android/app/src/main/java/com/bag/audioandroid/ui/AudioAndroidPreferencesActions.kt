package com.bag.audioandroid.ui

import androidx.appcompat.app.AppCompatDelegate
import com.bag.audioandroid.data.AppSettingsRepository
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.CustomFactionThemeSettings
import com.bag.audioandroid.ui.model.CustomThemeImportMode
import com.bag.audioandroid.ui.model.DefaultCustomFactionThemePresetId
import com.bag.audioandroid.ui.model.FactionThemeOption
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.MorseSpeedOption
import com.bag.audioandroid.ui.model.PaletteOption
import com.bag.audioandroid.ui.model.PlaybackSequenceMode
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.ThemeModeOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.findDuplicateImportedThemePresetId
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.withMaterialDarkThemeActive
import com.bag.audioandroid.ui.state.withSelectedThemeMode
import com.bag.audioandroid.ui.theme.DefaultCustomMaterialPaletteSettings
import com.bag.audioandroid.ui.theme.MaterialPalettes
import com.bag.audioandroid.ui.theme.customFactionTheme
import com.bag.audioandroid.ui.theme.customFactionThemeOptionId
import com.bag.audioandroid.ui.theme.customMaterialPalette
import com.bag.audioandroid.ui.theme.customMaterialPaletteId
import com.bag.audioandroid.ui.theme.normalizeCustomMaterialThemeSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

internal class AudioAndroidPreferencesActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val sampleInputSessionUpdater: SampleInputSessionUpdater,
    private val appSettingsRepository: AppSettingsRepository,
    private val scope: CoroutineScope,
) {
    private fun persistSelectedMaterialThemeState(state: AudioAppUiState) {
        scope.launch {
            appSettingsRepository.setSelectedThemeStyleId(state.selectedThemeStyle.id)
            appSettingsRepository.setSelectedPaletteId(state.selectedPalette.id)
            appSettingsRepository.setSelectedMaterialPaletteIdLight(
                state.selectedMaterialPaletteIdLight ?: state.selectedPalette.id,
            )
            appSettingsRepository.setSelectedMaterialPaletteIdDark(
                state.selectedMaterialPaletteIdDark ?: state.selectedPalette.id,
            )
        }
    }

    fun onLanguageSelected(language: AppLanguageOption) {
        val previousLanguage = uiState.value.selectedLanguage
        if (previousLanguage == language) {
            return
        }
        uiState.update { state ->
            state.copy(
                selectedLanguage = language,
                sessions =
                    sampleInputSessionUpdater.refreshForLanguageChange(
                        state.sessions,
                        language,
                        state.currentSampleFlavor,
                        state.isSampleAutoFillEnabled,
                    ),
            )
        }
        AppCompatDelegate.setApplicationLocales(language.toLocaleList())
    }

    fun onPaletteSelected(palette: PaletteOption) {
        uiState.update { state ->
            state.withSelectedMaterialPaletteTheme(palette)
        }
        val updatedState = uiState.value
        persistSelectedMaterialThemeState(updatedState)
    }

    fun onThemeModeSelected(themeMode: ThemeModeOption) {
        uiState.update { state -> state.withSelectedThemeMode(themeMode) }
        val updatedState = uiState.value
        scope.launch {
            appSettingsRepository.setSelectedThemeModeId(themeMode.id)
        }
        persistSelectedMaterialThemeState(updatedState)
    }

    fun onMaterialDarkThemeActiveChanged(isDarkTheme: Boolean) {
        uiState.update { state -> state.withMaterialDarkThemeActive(isDarkTheme) }
    }

    fun onThemeStyleSelected(themeStyle: ThemeStyleOption) {
        uiState.update { state ->
            if (themeStyle == ThemeStyleOption.Material) {
                state.withSelectedMaterialThemeStyle(sampleInputSessionUpdater)
            } else {
                state.withSelectedThemeStyle(themeStyle, sampleInputSessionUpdater)
            }
        }
        val updatedState = uiState.value
        if (themeStyle == ThemeStyleOption.Material) {
            persistSelectedMaterialThemeState(updatedState)
        } else {
            scope.launch {
                appSettingsRepository.setSelectedThemeStyleId(themeStyle.id)
            }
        }
    }

    fun onFactionThemeSelected(factionTheme: FactionThemeOption) {
        uiState.update { state -> state.withSelectedFactionTheme(factionTheme, sampleInputSessionUpdater) }
        scope.launch {
            appSettingsRepository.setSelectedFactionThemeId(factionTheme.id)
        }
    }

    fun onCustomFactionThemeSaved(
        settings: CustomFactionThemeSettings,
        replacePresetId: String?,
    ) {
        uiState.update { state ->
            val normalizedSettings =
                settings.copy(
                    presetId = replacePresetId ?: UUID.randomUUID().toString(),
                    displayName = settings.displayName.trim(),
                )
            val updatedPresets =
                if (replacePresetId == null) {
                    listOf(normalizedSettings) + state.customFactionThemePresets
                } else {
                    state.customFactionThemePresets.map { preset ->
                        if (preset.presetId == replacePresetId) {
                            normalizedSettings
                        } else {
                            preset
                        }
                    }
                }
            val updatedState = state.copy(customFactionThemePresets = updatedPresets)
            updatedState.withSelectedFactionTheme(customFactionTheme(normalizedSettings), sampleInputSessionUpdater)
        }
        scope.launch {
            appSettingsRepository.setCustomFactionThemePresets(uiState.value.customFactionThemePresets)
            appSettingsRepository.setSelectedFactionThemeId(uiState.value.activeFactionTheme.id)
        }
    }

    fun onCustomMaterialThemeSaved(
        settings: CustomFactionThemeSettings,
        replacePresetId: String? = null,
    ) {
        uiState.update { state -> state.withSavedCustomMaterialTheme(settings, replacePresetId) }
        val updatedState = uiState.value
        scope.launch {
            appSettingsRepository.setCustomMaterialThemePresets(updatedState.customMaterialThemePresets)
        }
        persistSelectedMaterialThemeState(updatedState)
    }

    fun onCreateCustomMaterialTheme() {
        val nextIndex = uiState.value.customMaterialThemePresets.size + 1
        val settings =
            normalizeCustomMaterialThemeSettings(
                DefaultCustomMaterialPaletteSettings.copy(
                    presetId = UUID.randomUUID().toString(),
                    displayName = "Custom $nextIndex",
                ),
            )
        uiState.update { state ->
            state
                .copy(customMaterialThemePresets = listOf(settings) + state.customMaterialThemePresets)
                .withSelectedMaterialPaletteTheme(customMaterialPalette(settings))
        }
        val updatedState = uiState.value
        scope.launch {
            appSettingsRepository.setCustomMaterialThemePresets(updatedState.customMaterialThemePresets)
        }
        persistSelectedMaterialThemeState(updatedState)
    }

    fun onCustomMaterialThemeDeleted(presetId: String) {
        uiState.update { state ->
            val remainingPresets = state.customMaterialThemePresets.filterNot { it.presetId == presetId }
            if (remainingPresets.isEmpty()) {
                return@update state
            }

            val deletedSelectedPalette = customMaterialPaletteId(presetId) == state.selectedPalette.id
            val nextSelectedPalette =
                if (deletedSelectedPalette) {
                    customMaterialPalette(remainingPresets.first())
                } else {
                    state.selectedPalette
                }
            state.copy(
                customMaterialThemePresets = remainingPresets,
                selectedMaterialPaletteIdLight =
                    if (state.selectedMaterialPaletteIdLight == customMaterialPaletteId(presetId)) {
                        nextSelectedPalette.id
                    } else {
                        state.selectedMaterialPaletteIdLight
                    },
                selectedMaterialPaletteIdDark =
                    if (state.selectedMaterialPaletteIdDark == customMaterialPaletteId(presetId)) {
                        nextSelectedPalette.id
                    } else {
                        state.selectedMaterialPaletteIdDark
                    },
                selectedPalette = nextSelectedPalette,
            )
        }
        val updatedState = uiState.value
        scope.launch {
            appSettingsRepository.setCustomMaterialThemePresets(updatedState.customMaterialThemePresets)
        }
        persistSelectedMaterialThemeState(updatedState)
    }

    fun onCustomMaterialThemesImported(settings: List<CustomFactionThemeSettings>) {
        if (settings.isEmpty()) {
            return
        }
        uiState.update { state -> state.withImportedCustomMaterialThemes(settings) }
        val updatedState = uiState.value
        scope.launch {
            appSettingsRepository.setCustomMaterialThemePresets(updatedState.customMaterialThemePresets)
        }
        persistSelectedMaterialThemeState(updatedState)
    }

    fun onCustomMaterialThemesReordered(
        fromIndex: Int,
        toIndex: Int,
    ) {
        uiState.update { state ->
            val presets = state.customMaterialThemePresets
            if (fromIndex !in presets.indices || toIndex !in presets.indices || fromIndex == toIndex) {
                return@update state
            }
            val reordered = presets.toMutableList()
            val moved = reordered.removeAt(fromIndex)
            reordered.add(toIndex, moved)
            state.copy(customMaterialThemePresets = reordered)
        }
        scope.launch {
            appSettingsRepository.setCustomMaterialThemePresets(uiState.value.customMaterialThemePresets)
        }
    }

    fun onCustomFactionThemeDeleted(presetId: String) {
        uiState.update { state ->
            val remainingPresets = state.customFactionThemePresets.filterNot { it.presetId == presetId }
            if (remainingPresets.isEmpty()) {
                return@update state
            }

            val updatedState = state.copy(customFactionThemePresets = remainingPresets)
            val deletedActiveCustomTheme = state.selectedFactionTheme.id == customFactionThemeOptionId(presetId)

            if (deletedActiveCustomTheme) {
                updatedState.withSelectedFactionTheme(customFactionTheme(remainingPresets.first()), sampleInputSessionUpdater)
            } else {
                updatedState
            }
        }
        scope.launch {
            appSettingsRepository.setCustomFactionThemePresets(uiState.value.customFactionThemePresets)
            appSettingsRepository.setSelectedFactionThemeId(uiState.value.activeFactionTheme.id)
        }
    }

    fun onCustomFactionThemesImported(settings: List<CustomFactionThemeSettings>) {
        if (settings.isEmpty()) {
            return
        }
        uiState.update { state -> state.withImportedCustomFactionThemes(settings) }
        scope.launch {
            appSettingsRepository.setCustomFactionThemePresets(uiState.value.customFactionThemePresets)
        }
    }

    fun onCustomFactionThemesReordered(
        fromIndex: Int,
        toIndex: Int,
    ) {
        uiState.update { state ->
            val presets = state.customFactionThemePresets
            if (fromIndex !in presets.indices || toIndex !in presets.indices || fromIndex == toIndex) {
                return@update state
            }
            val reordered = presets.toMutableList()
            val moved = reordered.removeAt(fromIndex)
            reordered.add(toIndex, moved)
            state.copy(customFactionThemePresets = reordered)
        }
        scope.launch {
            appSettingsRepository.setCustomFactionThemePresets(uiState.value.customFactionThemePresets)
        }
    }

    fun onFlashVoicingStyleSelected(style: FlashVoicingStyleOption) {
        val isEnhancedStyle = style != FlashVoicingStyleOption.Standard
        uiState.update {
            it.copy(
                selectedFlashVoicingStyle = style,
                isFlashVoicingEnabled = isEnhancedStyle,
            )
        }
        scope.launch {
            appSettingsRepository.setSelectedFlashVoicingStyleId(style.id)
            appSettingsRepository.setFlashVoicingEnabled(isEnhancedStyle)
        }
    }

    fun onPlaybackSequenceModeSelected(mode: PlaybackSequenceMode) {
        uiState.update { it.copy(playbackSequenceMode = mode) }
        scope.launch {
            appSettingsRepository.setSelectedPlaybackSequenceModeId(mode.id)
        }
    }

    fun onConfigLanguageExpandedChanged(expanded: Boolean) {
        uiState.update { it.copy(isConfigLanguageExpanded = expanded) }
        scope.launch {
            appSettingsRepository.setConfigLanguageExpanded(expanded)
        }
    }

    fun onConfigThemeAppearanceExpandedChanged(expanded: Boolean) {
        uiState.update { it.copy(isConfigThemeAppearanceExpanded = expanded) }
        scope.launch {
            appSettingsRepository.setConfigThemeAppearanceExpanded(expanded)
        }
    }

    fun onConfigCustomMaterialThemeExpandedChanged(expanded: Boolean) {
        uiState.update { it.copy(isConfigCustomMaterialThemeExpanded = expanded) }
        scope.launch {
            appSettingsRepository.setConfigCustomMaterialThemeExpanded(expanded)
        }
    }

    fun onConfigBuiltInMaterialPalettesExpandedChanged(expanded: Boolean) {
        uiState.update { it.copy(isConfigBuiltInMaterialPalettesExpanded = expanded) }
        scope.launch {
            appSettingsRepository.setConfigBuiltInMaterialPalettesExpanded(expanded)
        }
    }

    fun onConfigMaterialRedsPaletteExpandedChanged(expanded: Boolean) {
        uiState.update { it.copy(isConfigMaterialRedsPaletteExpanded = expanded) }
        scope.launch {
            appSettingsRepository.setConfigMaterialRedsPaletteExpanded(expanded)
        }
    }

    fun onConfigMaterialOrangesPaletteExpandedChanged(expanded: Boolean) {
        uiState.update { it.copy(isConfigMaterialOrangesPaletteExpanded = expanded) }
        scope.launch {
            appSettingsRepository.setConfigMaterialOrangesPaletteExpanded(expanded)
        }
    }

    fun onConfigMaterialYellowsPaletteExpandedChanged(expanded: Boolean) {
        uiState.update { it.copy(isConfigMaterialYellowsPaletteExpanded = expanded) }
        scope.launch {
            appSettingsRepository.setConfigMaterialYellowsPaletteExpanded(expanded)
        }
    }

    fun onConfigMaterialGreensPaletteExpandedChanged(expanded: Boolean) {
        uiState.update { it.copy(isConfigMaterialGreensPaletteExpanded = expanded) }
        scope.launch {
            appSettingsRepository.setConfigMaterialGreensPaletteExpanded(expanded)
        }
    }

    fun onConfigMaterialBluesPaletteExpandedChanged(expanded: Boolean) {
        uiState.update { it.copy(isConfigMaterialBluesPaletteExpanded = expanded) }
        scope.launch {
            appSettingsRepository.setConfigMaterialBluesPaletteExpanded(expanded)
        }
    }

    fun onConfigMaterialPurplesPaletteExpandedChanged(expanded: Boolean) {
        uiState.update { it.copy(isConfigMaterialPurplesPaletteExpanded = expanded) }
        scope.launch {
            appSettingsRepository.setConfigMaterialPurplesPaletteExpanded(expanded)
        }
    }

    fun onConfigMaterialNeutralsPaletteExpandedChanged(expanded: Boolean) {
        uiState.update { it.copy(isConfigMaterialNeutralsPaletteExpanded = expanded) }
        scope.launch {
            appSettingsRepository.setConfigMaterialNeutralsPaletteExpanded(expanded)
        }
    }

    fun onMorseSpeedStyleSelected(speed: MorseSpeedOption) {
        uiState.update { it.copy(selectedMorseSpeed = speed) }
        scope.launch {
            appSettingsRepository.setSelectedMorseSpeedStyleId(speed.id)
        }
    }

    fun onTransportModeSelected(mode: TransportModeOption) {
        scope.launch {
            appSettingsRepository.setSelectedTransportModeId(mode.wireName)
        }
    }

    fun onSampleInputLengthSelected(length: SampleInputLengthOption) {
        uiState.update { state ->
            if (state.selectedSampleInputLength == length) {
                state
            } else {
                state.copy(selectedSampleInputLength = length)
            }
        }
        scope.launch {
            appSettingsRepository.setSelectedSampleInputLengthId(length.id)
        }
    }

    fun onConfigCustomFactionThemeExpandedChanged(expanded: Boolean) {
        uiState.update { it.copy(isConfigCustomFactionThemeExpanded = expanded) }
        scope.launch {
            appSettingsRepository.setConfigCustomFactionThemeExpanded(expanded)
        }
    }

    fun onConfigSampleTextExpandedChanged(expanded: Boolean) {
        uiState.update { it.copy(isConfigSampleTextExpanded = expanded) }
        scope.launch {
            appSettingsRepository.setConfigSampleTextExpanded(expanded)
        }
    }

    fun onConfigSacredMachineFactionThemeExpandedChanged(expanded: Boolean) {
        uiState.update { it.copy(isConfigSacredMachineFactionThemeExpanded = expanded) }
        scope.launch {
            appSettingsRepository.setConfigSacredMachineFactionThemeExpanded(expanded)
        }
    }

    fun onConfigAncientDynastyFactionThemeExpandedChanged(expanded: Boolean) {
        uiState.update { it.copy(isConfigAncientDynastyFactionThemeExpanded = expanded) }
        scope.launch {
            appSettingsRepository.setConfigAncientDynastyFactionThemeExpanded(expanded)
        }
    }

    fun onConfigImmortalRotFactionThemeExpandedChanged(expanded: Boolean) {
        uiState.update { it.copy(isConfigImmortalRotFactionThemeExpanded = expanded) }
        scope.launch {
            appSettingsRepository.setConfigImmortalRotFactionThemeExpanded(expanded)
        }
    }

    fun onConfigScarletCarnageFactionThemeExpandedChanged(expanded: Boolean) {
        uiState.update { it.copy(isConfigScarletCarnageFactionThemeExpanded = expanded) }
        scope.launch {
            appSettingsRepository.setConfigScarletCarnageFactionThemeExpanded(expanded)
        }
    }

    fun onConfigExquisiteFallFactionThemeExpandedChanged(expanded: Boolean) {
        uiState.update { it.copy(isConfigExquisiteFallFactionThemeExpanded = expanded) }
        scope.launch {
            appSettingsRepository.setConfigExquisiteFallFactionThemeExpanded(expanded)
        }
    }

    fun onConfigLabyrinthOfMutabilityFactionThemeExpandedChanged(expanded: Boolean) {
        uiState.update { it.copy(isConfigLabyrinthOfMutabilityFactionThemeExpanded = expanded) }
        scope.launch {
            appSettingsRepository.setConfigLabyrinthOfMutabilityFactionThemeExpanded(expanded)
        }
    }

    fun onConfigDebugExpandedChanged(expanded: Boolean) {
        uiState.update { it.copy(isConfigDebugExpanded = expanded) }
        scope.launch {
            appSettingsRepository.setConfigDebugExpanded(expanded)
        }
    }

    fun onDemoModeEnabledChanged(enabled: Boolean) {
        uiState.update { it.copy(isDemoModeEnabled = enabled) }
        scope.launch {
            appSettingsRepository.setDemoModeEnabled(enabled)
        }
    }

    fun onSampleDecorationEnabledChanged(enabled: Boolean) {
        uiState.update { state ->
            state.withSampleDecoration(isDecorationEnabled = enabled)
        }
        scope.launch {
            appSettingsRepository.setSampleDecorationEnabled(enabled)
        }
    }

    fun onSampleAutoFillEnabledChanged(enabled: Boolean) {
        uiState.update { state ->
            val updatedSessions =
                if (enabled) {
                    sampleInputSessionUpdater.initialize(
                        sessions = state.sessions,
                        language = state.selectedLanguage,
                        flavor = state.currentSampleFlavor,
                        length = state.selectedSampleInputLength,
                        isSampleAutoFillEnabled = true,
                        isDecorationEnabled = state.isSampleDecorationEnabled,
                    )
                } else {
                    sampleInputSessionUpdater.clearAutoFilledSessions(state.sessions)
                }
            state.copy(
                isSampleAutoFillEnabled = enabled,
                sessions = updatedSessions,
            )
        }
        scope.launch {
            appSettingsRepository.setSampleAutoFillEnabled(enabled)
        }
    }

    fun onSavedAudioPlaybackDataStorageEnabledChanged(enabled: Boolean) {
        uiState.update { it.copy(isSavedAudioPlaybackDataStorageEnabled = enabled) }
        scope.launch {
            appSettingsRepository.setSavedAudioPlaybackDataStorageEnabled(enabled)
        }
    }

    fun onFlashVisualPerfOverlayEnabledChanged(enabled: Boolean) {
        uiState.update { it.copy(isFlashVisualPerfOverlayEnabled = enabled) }
        scope.launch {
            appSettingsRepository.setFlashVisualPerfOverlayEnabled(enabled)
        }
    }
}

internal fun AudioAppUiState.withSampleDecoration(isDecorationEnabled: Boolean): AudioAppUiState {
    val updatedSession =
        applySampleEmojiDecoration(
            session = currentSession,
            mode = transportMode,
            flavor = currentSampleFlavor,
            isDecorationEnabled = isDecorationEnabled,
        )
    return copy(
        isSampleDecorationEnabled = isDecorationEnabled,
        sessions = sessions + (transportMode to updatedSession),
    )
}

internal fun AudioAppUiState.withSelectedMaterialPaletteTheme(
    palette: PaletteOption,
    isDarkTheme: Boolean = isMaterialDarkThemeActive,
): AudioAppUiState =
    copy(selectedThemeStyle = ThemeStyleOption.Material).withSelectedMaterialPalette(
        palette = palette,
        isDarkTheme = isDarkTheme,
    )

internal fun AudioAppUiState.withSelectedMaterialPalette(
    palette: PaletteOption,
    isDarkTheme: Boolean = isMaterialDarkThemeActive,
): AudioAppUiState =
    copy(
        selectedPalette = palette,
        selectedMaterialPaletteIdLight =
            if (isDarkTheme) {
                selectedMaterialPaletteIdLight
            } else {
                palette.id
            },
        selectedMaterialPaletteIdDark =
            if (isDarkTheme) {
                palette.id
            } else {
                selectedMaterialPaletteIdDark
            },
    )

internal fun AudioAppUiState.withSelectedMaterialThemeStyle(sampleInputSessionUpdater: SampleInputSessionUpdater): AudioAppUiState {
    val materialStyleState = withSelectedThemeStyle(ThemeStyleOption.Material, sampleInputSessionUpdater)
    val hasPersistedMaterialPalette =
        selectedMaterialPaletteIdLight != null || selectedMaterialPaletteIdDark != null
    if (hasPersistedMaterialPalette) {
        return materialStyleState
    }
    val defaultStonePalette =
        MaterialPalettes.firstOrNull { it.id == DefaultFirstMaterialPaletteId } ?: materialStyleState.selectedPalette
    return materialStyleState.withSelectedMaterialPalette(defaultStonePalette)
}

internal fun AudioAppUiState.withSavedCustomMaterialTheme(
    settings: CustomFactionThemeSettings,
    replacePresetId: String? = null,
): AudioAppUiState {
    val normalizedSettings =
        normalizeCustomMaterialThemeSettings(
            settings.copy(
                presetId = replacePresetId ?: settings.presetId.ifBlank { UUID.randomUUID().toString() },
            ),
        )
    val updatedPresets =
        if (replacePresetId == null) {
            listOf(normalizedSettings) + customMaterialThemePresets
        } else {
            customMaterialThemePresets.map { preset ->
                if (preset.presetId == replacePresetId) {
                    normalizedSettings
                } else {
                    preset
                }
            }
        }
    val palette = customMaterialPalette(normalizedSettings)
    return copy(
        selectedThemeStyle = ThemeStyleOption.Material,
        customMaterialThemePresets = updatedPresets,
        selectedPalette = palette,
        selectedMaterialPaletteIdLight =
            if (isMaterialDarkThemeActive) {
                selectedMaterialPaletteIdLight
            } else {
                palette.id
            },
        selectedMaterialPaletteIdDark =
            if (isMaterialDarkThemeActive) {
                palette.id
            } else {
                selectedMaterialPaletteIdDark
            },
    )
}

internal fun AudioAppUiState.withImportedCustomMaterialThemes(settings: List<CustomFactionThemeSettings>): AudioAppUiState {
    var updatedPresets = customMaterialThemePresets
    val importedResolved = mutableListOf<CustomFactionThemeSettings>()
    val insertedPresetIdsInImportOrder = mutableListOf<String>()
    settings.forEach { setting ->
        val normalizedCandidate =
            normalizeCustomMaterialThemeSettings(
                setting.copy(
                    presetId = importedPresetIdOrNew(setting.presetId),
                    displayName = setting.displayName.trim(),
                ),
            )
        val duplicatePresetId =
            findDuplicateImportedThemePresetId(
                existing = updatedPresets,
                imported = normalizedCandidate,
                mode = CustomThemeImportMode.Material,
            )
        val resolved =
            normalizedCandidate.copy(
                presetId = duplicatePresetId ?: normalizedCandidate.presetId.ifBlank { UUID.randomUUID().toString() },
            )
        updatedPresets =
            if (duplicatePresetId == null) {
                // Keep batch-imported presets at the top, but preserve the same
                // order as the shared/exported config text instead of reversing
                // the batch through repeated head insertion.
                insertedPresetIdsInImportOrder += resolved.presetId
                listOf(resolved) + updatedPresets
            } else {
                updatedPresets.map { preset ->
                    if (preset.presetId == duplicatePresetId) {
                        resolved
                    } else {
                        preset
                    }
                }
            }
        importedResolved += resolved
    }
    val orderedInsertedPresets =
        insertedPresetIdsInImportOrder.mapNotNull { insertedPresetId ->
            updatedPresets.firstOrNull { it.presetId == insertedPresetId }
        }
    val remainingPresets =
        updatedPresets.filterNot { preset -> preset.presetId in insertedPresetIdsInImportOrder }
    val updatedState =
        copy(
            customMaterialThemePresets = orderedInsertedPresets + remainingPresets,
        )
    return if (importedResolved.size == 1) {
        updatedState.withSavedCustomMaterialTheme(importedResolved.single(), importedResolved.single().presetId)
    } else {
        updatedState
    }
}

internal fun AudioAppUiState.withImportedCustomFactionThemes(settings: List<CustomFactionThemeSettings>): AudioAppUiState {
    var updatedPresets = customFactionThemePresets
    val insertedPresetIdsInImportOrder = mutableListOf<String>()
    settings.forEach { setting ->
        val normalizedCandidate =
            setting.copy(
                presetId = importedPresetIdOrNew(setting.presetId),
                displayName = setting.displayName.trim(),
            )
        val duplicatePresetId =
            findDuplicateImportedThemePresetId(
                existing = updatedPresets,
                imported = normalizedCandidate,
                mode = CustomThemeImportMode.DualTone,
            )
        val resolved =
            normalizedCandidate.copy(
                presetId = duplicatePresetId ?: normalizedCandidate.presetId.ifBlank { UUID.randomUUID().toString() },
            )
        updatedPresets =
            if (duplicatePresetId == null) {
                // Match imported/exported order at the top of the list while
                // still treating batch import as a stack push above older presets.
                insertedPresetIdsInImportOrder += resolved.presetId
                listOf(resolved) + updatedPresets
            } else {
                updatedPresets.map { preset ->
                    if (preset.presetId == duplicatePresetId) {
                        resolved
                    } else {
                        preset
                    }
                }
            }
    }
    val orderedInsertedPresets =
        insertedPresetIdsInImportOrder.mapNotNull { insertedPresetId ->
            updatedPresets.firstOrNull { it.presetId == insertedPresetId }
        }
    val remainingPresets =
        updatedPresets.filterNot { preset -> preset.presetId in insertedPresetIdsInImportOrder }
    return copy(customFactionThemePresets = orderedInsertedPresets + remainingPresets)
}

private const val DefaultFirstMaterialPaletteId = "stone"

private fun importedPresetIdOrNew(presetId: String): String =
    presetId
        .takeUnless {
            it.isBlank() || it == DefaultCustomFactionThemePresetId
        } ?: UUID.randomUUID().toString()

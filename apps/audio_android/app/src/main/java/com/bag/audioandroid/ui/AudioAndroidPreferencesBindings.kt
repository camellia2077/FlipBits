package com.bag.audioandroid.ui

import com.bag.audioandroid.data.AppSettingsRepository
import com.bag.audioandroid.ui.model.CustomBrandThemeSettings
import com.bag.audioandroid.ui.model.DefaultCustomBrandThemeSettings
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.MorseSpeedOption
import com.bag.audioandroid.ui.model.PlaybackSequenceMode
import com.bag.audioandroid.ui.model.ThemeModeOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.materialPaletteIdForMode
import com.bag.audioandroid.ui.state.resolveMaterialPaletteById
import com.bag.audioandroid.ui.theme.BrandDualToneThemes
import com.bag.audioandroid.ui.theme.DefaultBrandTheme
import com.bag.audioandroid.ui.theme.DefaultCustomMaterialPaletteSettings
import com.bag.audioandroid.ui.theme.DefaultMaterialPalette
import com.bag.audioandroid.ui.theme.MaterialPalettes
import com.bag.audioandroid.ui.theme.customBrandTheme
import com.bag.audioandroid.ui.theme.isCustomBrandThemeOptionId
import com.bag.audioandroid.ui.theme.isCustomMaterialPaletteId
import com.bag.audioandroid.ui.theme.normalizeBrandThemeHex
import com.bag.audioandroid.ui.theme.normalizeBrandThemeHexOrNull
import com.bag.audioandroid.ui.theme.normalizeCustomMaterialThemeSettings
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
        observeSelectedPalette()
        observeSelectedThemeMode()
        observeSelectedMaterialPaletteIdLight()
        observeSelectedMaterialPaletteIdDark()
        observeSelectedThemeStyle()
        observeCustomMaterialThemeSettings()
        observeCustomBrandThemeSettings()
        observeSelectedBrandTheme()
        observeSelectedFlashVoicingStyle()
        observeSelectedMorseSpeedStyle()
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
        observeConfigCustomBrandThemeExpanded()
        observeConfigSampleTextExpanded()
        observeConfigSacredMachineBrandThemeExpanded()
        observeConfigAncientDynastyBrandThemeExpanded()
        observeConfigImmortalRotBrandThemeExpanded()
        observeConfigScarletCarnageBrandThemeExpanded()
        observeConfigExquisiteFallBrandThemeExpanded()
        observeConfigLabyrinthOfMutabilityBrandThemeExpanded()
        observeConfigDebugExpanded()
        observeDemoModeEnabled()
        observeSampleAutoFillEnabled()
        observeSampleDecorationEnabled()
        observeFlashVisualPerfOverlayEnabled()
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

    private fun observeSelectedBrandTheme() {
        scope.launch {
            appSettingsRepository.selectedBrandThemeId
                .distinctUntilChanged()
                .collect { brandThemeId ->
                    uiState.update { state ->
                        val brandTheme =
                            if (brandThemeId != null && isCustomBrandThemeOptionId(brandThemeId)) {
                                state.customBrandThemes.firstOrNull { it.id == brandThemeId } ?: state.customBrandThemes.first()
                            } else {
                                BrandDualToneThemes.firstOrNull { it.id == brandThemeId } ?: DefaultBrandTheme
                            }
                        state.withSelectedBrandTheme(brandTheme, sampleInputSessionUpdater)
                    }
                }
        }
    }

    private fun observeCustomBrandThemeSettings() {
        scope.launch {
            appSettingsRepository.customBrandThemePresets
                .map { presets ->
                    val normalizedPresets =
                        presets.map { settings ->
                            CustomBrandThemeSettings(
                                presetId = settings.presetId,
                                displayName = settings.displayName.trim().ifBlank { DefaultCustomBrandThemeSettings.displayName },
                                primaryHex =
                                    normalizeBrandThemeHex(settings.primaryHex)
                                        ?: DefaultCustomBrandThemeSettings.primaryHex,
                                secondaryHex =
                                    normalizeBrandThemeHex(settings.secondaryHex)
                                        ?: DefaultCustomBrandThemeSettings.secondaryHex,
                                outlineHexOrNull =
                                    settings.outlineHexOrNull?.let { outlineHex ->
                                        normalizeBrandThemeHexOrNull(outlineHex) ?: DefaultCustomBrandThemeSettings.outlineHexOrNull
                                    },
                            )
                        }
                    if (normalizedPresets.isEmpty()) {
                        listOf(DefaultCustomBrandThemeSettings)
                    } else {
                        normalizedPresets
                    }
                }.distinctUntilChanged()
                .collect { presets ->
                    uiState.update { state ->
                        val presetThemes = presets.map(::customBrandTheme)
                        val selectedBrandTheme =
                            if (isCustomBrandThemeOptionId(state.selectedBrandTheme.id)) {
                                presetThemes.firstOrNull { it.id == state.selectedBrandTheme.id } ?: presetThemes.first()
                            } else {
                                state.selectedBrandTheme
                            }
                        state.copy(
                            customBrandThemePresets = presets,
                            selectedBrandTheme = selectedBrandTheme,
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

    private fun observeConfigCustomBrandThemeExpanded() {
        scope.launch {
            appSettingsRepository.isConfigCustomBrandThemeExpanded
                .distinctUntilChanged()
                .collect { expanded ->
                    uiState.update { state ->
                        if (state.isConfigCustomBrandThemeExpanded == expanded) {
                            state
                        } else {
                            state.copy(isConfigCustomBrandThemeExpanded = expanded)
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

    private fun observeConfigSacredMachineBrandThemeExpanded() {
        scope.launch {
            appSettingsRepository.isConfigSacredMachineBrandThemeExpanded
                .distinctUntilChanged()
                .collect { expanded ->
                    uiState.update { state ->
                        if (state.isConfigSacredMachineBrandThemeExpanded ==
                            expanded
                        ) {
                            state
                        } else {
                            state.copy(isConfigSacredMachineBrandThemeExpanded = expanded)
                        }
                    }
                }
        }
    }

    private fun observeConfigAncientDynastyBrandThemeExpanded() {
        scope.launch {
            appSettingsRepository.isConfigAncientDynastyBrandThemeExpanded
                .distinctUntilChanged()
                .collect { expanded ->
                    uiState.update { state ->
                        if (state.isConfigAncientDynastyBrandThemeExpanded ==
                            expanded
                        ) {
                            state
                        } else {
                            state.copy(isConfigAncientDynastyBrandThemeExpanded = expanded)
                        }
                    }
                }
        }
    }

    private fun observeConfigImmortalRotBrandThemeExpanded() {
        scope.launch {
            appSettingsRepository.isConfigImmortalRotBrandThemeExpanded
                .distinctUntilChanged()
                .collect { expanded ->
                    uiState.update { state ->
                        if (state.isConfigImmortalRotBrandThemeExpanded ==
                            expanded
                        ) {
                            state
                        } else {
                            state.copy(isConfigImmortalRotBrandThemeExpanded = expanded)
                        }
                    }
                }
        }
    }

    private fun observeConfigScarletCarnageBrandThemeExpanded() {
        scope.launch {
            appSettingsRepository.isConfigScarletCarnageBrandThemeExpanded
                .distinctUntilChanged()
                .collect { expanded ->
                    uiState.update { state ->
                        if (state.isConfigScarletCarnageBrandThemeExpanded ==
                            expanded
                        ) {
                            state
                        } else {
                            state.copy(isConfigScarletCarnageBrandThemeExpanded = expanded)
                        }
                    }
                }
        }
    }

    private fun observeConfigExquisiteFallBrandThemeExpanded() {
        scope.launch {
            appSettingsRepository.isConfigExquisiteFallBrandThemeExpanded
                .distinctUntilChanged()
                .collect { expanded ->
                    uiState.update { state ->
                        if (state.isConfigExquisiteFallBrandThemeExpanded ==
                            expanded
                        ) {
                            state
                        } else {
                            state.copy(isConfigExquisiteFallBrandThemeExpanded = expanded)
                        }
                    }
                }
        }
    }

    private fun observeConfigLabyrinthOfMutabilityBrandThemeExpanded() {
        scope.launch {
            appSettingsRepository.isConfigLabyrinthOfMutabilityBrandThemeExpanded
                .distinctUntilChanged()
                .collect { expanded ->
                    uiState.update { state ->
                        if (state.isConfigLabyrinthOfMutabilityBrandThemeExpanded ==
                            expanded
                        ) {
                            state
                        } else {
                            state.copy(isConfigLabyrinthOfMutabilityBrandThemeExpanded = expanded)
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
        ThemeStyleOption.BrandDualTone
    }
}

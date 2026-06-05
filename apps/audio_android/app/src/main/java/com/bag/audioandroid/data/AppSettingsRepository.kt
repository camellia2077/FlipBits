package com.bag.audioandroid.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bag.audioandroid.ui.model.CustomFactionThemeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

class AppSettingsRepository(
    private val appContext: Context,
) {
    val selectedPaletteId: Flow<String?> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.SelectedPaletteId] }

    val selectedFlashVoicingStyleId: Flow<String?> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.SelectedFlashVoicingStyleId] }

    val selectedMorseSpeedStyleId: Flow<String?> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.SelectedMorseSpeedStyleId] }

    val selectedTransportModeId: Flow<String?> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.SelectedTransportModeId] }

    val selectedSampleInputLengthId: Flow<String?> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.SelectedSampleInputLengthId] }

    val isFlashVoicingEnabled: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.FlashVoicingEnabled] ?: true }

    val selectedThemeModeId: Flow<String?> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.SelectedThemeModeId] }

    val selectedMaterialPaletteIdLight: Flow<String?> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.SelectedMaterialPaletteIdLight] }

    val selectedMaterialPaletteIdDark: Flow<String?> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.SelectedMaterialPaletteIdDark] }

    val selectedThemeStyleId: Flow<String?> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.SelectedThemeStyleId] }

    val selectedFactionThemeId: Flow<String?> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.SelectedFactionThemeId] }

    val customFactionThemePresets: Flow<List<CustomFactionThemeSettings>> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences ->
                CustomFactionThemeSettingsStore.decode(preferences[Keys.CustomFactionThemePresetsJson])
            }

    val customMaterialThemePresets: Flow<List<CustomFactionThemeSettings>> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> CustomFactionThemeSettingsStore.decode(preferences[Keys.CustomMaterialThemeJson]) }

    val selectedPlaybackSequenceModeId: Flow<String?> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.SelectedPlaybackSequenceModeId] }

    val isConfigLanguageExpanded: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.ConfigLanguageExpanded] ?: true }

    val isConfigThemeAppearanceExpanded: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.ConfigThemeAppearanceExpanded] ?: true }

    val isConfigCustomMaterialThemeExpanded: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.ConfigCustomMaterialThemeExpanded] ?: true }

    val isConfigBuiltInMaterialPalettesExpanded: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.ConfigBuiltInMaterialPalettesExpanded] ?: true }

    val isConfigMaterialRedsPaletteExpanded: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.ConfigMaterialRedsPaletteExpanded] ?: true }

    val isConfigMaterialOrangesPaletteExpanded: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.ConfigMaterialOrangesPaletteExpanded] ?: true }

    val isConfigMaterialYellowsPaletteExpanded: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.ConfigMaterialYellowsPaletteExpanded] ?: true }

    val isConfigMaterialGreensPaletteExpanded: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.ConfigMaterialGreensPaletteExpanded] ?: true }

    val isConfigMaterialBluesPaletteExpanded: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.ConfigMaterialBluesPaletteExpanded] ?: true }

    val isConfigMaterialPurplesPaletteExpanded: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.ConfigMaterialPurplesPaletteExpanded] ?: true }

    val isConfigMaterialNeutralsPaletteExpanded: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.ConfigMaterialNeutralsPaletteExpanded] ?: true }

    val isConfigCustomFactionThemeExpanded: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.ConfigCustomFactionThemeExpanded] ?: false }

    val isConfigSampleTextExpanded: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.ConfigSampleTextExpanded] ?: false }

    val isConfigSacredMachineFactionThemeExpanded: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.ConfigSacredMachineFactionThemeExpanded] ?: false }

    val isConfigAncientDynastyFactionThemeExpanded: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.ConfigAncientDynastyFactionThemeExpanded] ?: false }

    val isConfigImmortalRotFactionThemeExpanded: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.ConfigImmortalRotFactionThemeExpanded] ?: false }

    val isConfigScarletCarnageFactionThemeExpanded: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.ConfigScarletCarnageFactionThemeExpanded] ?: false }

    val isConfigExquisiteFallFactionThemeExpanded: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.ConfigExquisiteFallFactionThemeExpanded] ?: false }

    val isConfigLabyrinthOfMutabilityFactionThemeExpanded: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.ConfigLabyrinthOfMutabilityFactionThemeExpanded] ?: false }

    val isConfigDebugExpanded: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.ConfigDebugExpanded] ?: false }

    val isDemoModeEnabled: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.DemoModeEnabled] ?: false }

    val isSampleDecorationEnabled: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.SampleDecorationEnabled] ?: true }

    val isSampleAutoFillEnabled: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.SampleAutoFillEnabled] ?: true }

    val isSavedAudioPlaybackDataStorageEnabled: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.SavedAudioPlaybackDataStorageEnabled] ?: true }

    val isFlashVisualPerfOverlayEnabled: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.FlashVisualPerfOverlayEnabled] ?: false }

    suspend fun setSelectedPaletteId(paletteId: String) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.SelectedPaletteId] = paletteId
        }
    }

    suspend fun setSelectedFlashVoicingStyleId(styleId: String) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.SelectedFlashVoicingStyleId] = styleId
        }
    }

    suspend fun setSelectedMorseSpeedStyleId(styleId: String) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.SelectedMorseSpeedStyleId] = styleId
        }
    }

    suspend fun setSelectedTransportModeId(modeId: String) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.SelectedTransportModeId] = modeId
        }
    }

    suspend fun setSelectedSampleInputLengthId(lengthId: String) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.SelectedSampleInputLengthId] = lengthId
        }
    }

    suspend fun setFlashVoicingEnabled(enabled: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.FlashVoicingEnabled] = enabled
        }
    }

    suspend fun setSelectedThemeModeId(themeModeId: String) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.SelectedThemeModeId] = themeModeId
        }
    }

    suspend fun setSelectedMaterialPaletteIdLight(paletteId: String) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.SelectedMaterialPaletteIdLight] = paletteId
        }
    }

    suspend fun setSelectedMaterialPaletteIdDark(paletteId: String) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.SelectedMaterialPaletteIdDark] = paletteId
        }
    }

    suspend fun setSelectedThemeStyleId(themeStyleId: String) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.SelectedThemeStyleId] = themeStyleId
        }
    }

    suspend fun setSelectedFactionThemeId(factionThemeId: String) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.SelectedFactionThemeId] = factionThemeId
        }
    }

    suspend fun setCustomFactionThemePresets(settings: List<CustomFactionThemeSettings>) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.CustomFactionThemePresetsJson] = CustomFactionThemeSettingsStore.encode(settings)
        }
    }

    suspend fun setCustomMaterialThemePresets(settings: List<CustomFactionThemeSettings>) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.CustomMaterialThemeJson] = CustomFactionThemeSettingsStore.encode(settings)
        }
    }

    suspend fun setSelectedPlaybackSequenceModeId(playbackSequenceModeId: String) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.SelectedPlaybackSequenceModeId] = playbackSequenceModeId
        }
    }

    suspend fun setConfigLanguageExpanded(expanded: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.ConfigLanguageExpanded] = expanded
        }
    }

    suspend fun setConfigThemeAppearanceExpanded(expanded: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.ConfigThemeAppearanceExpanded] = expanded
        }
    }

    suspend fun setConfigCustomMaterialThemeExpanded(expanded: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.ConfigCustomMaterialThemeExpanded] = expanded
        }
    }

    suspend fun setConfigBuiltInMaterialPalettesExpanded(expanded: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.ConfigBuiltInMaterialPalettesExpanded] = expanded
        }
    }

    suspend fun setConfigMaterialRedsPaletteExpanded(expanded: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.ConfigMaterialRedsPaletteExpanded] = expanded
        }
    }

    suspend fun setConfigMaterialOrangesPaletteExpanded(expanded: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.ConfigMaterialOrangesPaletteExpanded] = expanded
        }
    }

    suspend fun setConfigMaterialYellowsPaletteExpanded(expanded: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.ConfigMaterialYellowsPaletteExpanded] = expanded
        }
    }

    suspend fun setConfigMaterialGreensPaletteExpanded(expanded: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.ConfigMaterialGreensPaletteExpanded] = expanded
        }
    }

    suspend fun setConfigMaterialBluesPaletteExpanded(expanded: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.ConfigMaterialBluesPaletteExpanded] = expanded
        }
    }

    suspend fun setConfigMaterialPurplesPaletteExpanded(expanded: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.ConfigMaterialPurplesPaletteExpanded] = expanded
        }
    }

    suspend fun setConfigMaterialNeutralsPaletteExpanded(expanded: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.ConfigMaterialNeutralsPaletteExpanded] = expanded
        }
    }

    suspend fun setConfigCustomFactionThemeExpanded(expanded: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.ConfigCustomFactionThemeExpanded] = expanded
        }
    }

    suspend fun setConfigSampleTextExpanded(expanded: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.ConfigSampleTextExpanded] = expanded
        }
    }

    suspend fun setConfigSacredMachineFactionThemeExpanded(expanded: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.ConfigSacredMachineFactionThemeExpanded] = expanded
        }
    }

    suspend fun setConfigAncientDynastyFactionThemeExpanded(expanded: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.ConfigAncientDynastyFactionThemeExpanded] = expanded
        }
    }

    suspend fun setConfigImmortalRotFactionThemeExpanded(expanded: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.ConfigImmortalRotFactionThemeExpanded] = expanded
        }
    }

    suspend fun setConfigScarletCarnageFactionThemeExpanded(expanded: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.ConfigScarletCarnageFactionThemeExpanded] = expanded
        }
    }

    suspend fun setConfigExquisiteFallFactionThemeExpanded(expanded: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.ConfigExquisiteFallFactionThemeExpanded] = expanded
        }
    }

    suspend fun setConfigLabyrinthOfMutabilityFactionThemeExpanded(expanded: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.ConfigLabyrinthOfMutabilityFactionThemeExpanded] = expanded
        }
    }

    suspend fun setConfigDebugExpanded(expanded: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.ConfigDebugExpanded] = expanded
        }
    }

    suspend fun setDemoModeEnabled(enabled: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.DemoModeEnabled] = enabled
        }
    }

    suspend fun setSampleDecorationEnabled(enabled: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.SampleDecorationEnabled] = enabled
        }
    }

    suspend fun setSampleAutoFillEnabled(enabled: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.SampleAutoFillEnabled] = enabled
        }
    }

    suspend fun setSavedAudioPlaybackDataStorageEnabled(enabled: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.SavedAudioPlaybackDataStorageEnabled] = enabled
        }
    }

    suspend fun setFlashVisualPerfOverlayEnabled(enabled: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.FlashVisualPerfOverlayEnabled] = enabled
        }
    }

    private object Keys {
        val SelectedPaletteId: Preferences.Key<String> = stringPreferencesKey("palette_id")
        val SelectedFlashVoicingStyleId: Preferences.Key<String> = stringPreferencesKey("flash_voicing_style")
        val SelectedMorseSpeedStyleId: Preferences.Key<String> = stringPreferencesKey("mini_speed_style")
        val SelectedTransportModeId: Preferences.Key<String> = stringPreferencesKey("transport_mode")
        val SelectedSampleInputLengthId: Preferences.Key<String> = stringPreferencesKey("sample_input_length")
        val FlashVoicingEnabled: Preferences.Key<Boolean> = booleanPreferencesKey("flash_voicing_enabled")
        val SelectedThemeModeId: Preferences.Key<String> = stringPreferencesKey("theme_mode")
        val SelectedMaterialPaletteIdLight: Preferences.Key<String> = stringPreferencesKey("material_palette_id_light")
        val SelectedMaterialPaletteIdDark: Preferences.Key<String> = stringPreferencesKey("material_palette_id_dark")
        val SelectedThemeStyleId: Preferences.Key<String> = stringPreferencesKey("theme_style")
        val SelectedFactionThemeId: Preferences.Key<String> = stringPreferencesKey("faction_theme_id")
        val CustomFactionThemePresetsJson: Preferences.Key<String> = stringPreferencesKey("custom_faction_presets_json")
        val CustomMaterialThemeJson: Preferences.Key<String> = stringPreferencesKey("custom_material_theme_json")
        val SelectedPlaybackSequenceModeId: Preferences.Key<String> = stringPreferencesKey("playback_sequence_mode")
        val ConfigLanguageExpanded: Preferences.Key<Boolean> = booleanPreferencesKey("config_language_expanded")
        val ConfigThemeAppearanceExpanded: Preferences.Key<Boolean> = booleanPreferencesKey("config_theme_appearance_expanded")
        val ConfigCustomMaterialThemeExpanded: Preferences.Key<Boolean> = booleanPreferencesKey("config_custom_material_theme_expanded")
        val ConfigBuiltInMaterialPalettesExpanded: Preferences.Key<Boolean> =
            booleanPreferencesKey("config_built_in_material_palettes_expanded")
        val ConfigMaterialRedsPaletteExpanded: Preferences.Key<Boolean> = booleanPreferencesKey("config_material_reds_palette_expanded")
        val ConfigMaterialOrangesPaletteExpanded: Preferences.Key<Boolean> =
            booleanPreferencesKey("config_material_oranges_palette_expanded")
        val ConfigMaterialYellowsPaletteExpanded: Preferences.Key<Boolean> =
            booleanPreferencesKey("config_material_yellows_palette_expanded")
        val ConfigMaterialGreensPaletteExpanded: Preferences.Key<Boolean> = booleanPreferencesKey("config_material_greens_palette_expanded")
        val ConfigMaterialBluesPaletteExpanded: Preferences.Key<Boolean> = booleanPreferencesKey("config_material_blues_palette_expanded")
        val ConfigMaterialPurplesPaletteExpanded: Preferences.Key<Boolean> =
            booleanPreferencesKey("config_material_purples_palette_expanded")
        val ConfigMaterialNeutralsPaletteExpanded: Preferences.Key<Boolean> =
            booleanPreferencesKey("config_material_neutrals_palette_expanded")
        val ConfigCustomFactionThemeExpanded: Preferences.Key<Boolean> = booleanPreferencesKey("config_custom_faction_theme_expanded")
        val ConfigSampleTextExpanded: Preferences.Key<Boolean> = booleanPreferencesKey("config_sample_text_expanded")
        val ConfigSacredMachineFactionThemeExpanded: Preferences.Key<Boolean> =
            booleanPreferencesKey("config_sacred_machine_faction_theme_expanded")
        val ConfigAncientDynastyFactionThemeExpanded: Preferences.Key<Boolean> =
            booleanPreferencesKey("config_ancient_dynasty_faction_theme_expanded")
        val ConfigImmortalRotFactionThemeExpanded: Preferences.Key<Boolean> =
            booleanPreferencesKey("config_immortal_rot_faction_theme_expanded")
        val ConfigScarletCarnageFactionThemeExpanded: Preferences.Key<Boolean> =
            booleanPreferencesKey("config_scarlet_carnage_faction_theme_expanded")
        val ConfigExquisiteFallFactionThemeExpanded: Preferences.Key<Boolean> =
            booleanPreferencesKey("config_exquisite_fall_faction_theme_expanded")
        val ConfigLabyrinthOfMutabilityFactionThemeExpanded: Preferences.Key<Boolean> =
            booleanPreferencesKey("config_labyrinth_of_mutability_faction_theme_expanded")
        val ConfigDebugExpanded: Preferences.Key<Boolean> = booleanPreferencesKey("config_debug_expanded")
        val DemoModeEnabled: Preferences.Key<Boolean> = booleanPreferencesKey("demo_mode_enabled")
        val SampleAutoFillEnabled: Preferences.Key<Boolean> = booleanPreferencesKey("sample_auto_fill_enabled")
        val SampleDecorationEnabled: Preferences.Key<Boolean> = booleanPreferencesKey("sample_decoration_enabled")
        val SavedAudioPlaybackDataStorageEnabled: Preferences.Key<Boolean> =
            booleanPreferencesKey("saved_audio_playback_data_storage_enabled")
        val FlashVisualPerfOverlayEnabled: Preferences.Key<Boolean> = booleanPreferencesKey("visual_fps_overlay_enabled")
    }
}

package com.bag.audioandroid.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bag.audioandroid.ui.model.CustomBrandThemeSettings
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

    val selectedBrandThemeId: Flow<String?> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.SelectedBrandThemeId] }

    val customBrandThemePresets: Flow<List<CustomBrandThemeSettings>> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences ->
                CustomBrandThemeSettingsStore.decode(preferences[Keys.CustomBrandThemePresetsJson])
            }

    val customMaterialThemePresets: Flow<List<CustomBrandThemeSettings>> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> CustomBrandThemeSettingsStore.decode(preferences[Keys.CustomMaterialThemeJson]) }

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

    val isConfigCustomBrandThemeExpanded: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.ConfigCustomBrandThemeExpanded] ?: false }

    val isConfigSampleTextExpanded: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.ConfigSampleTextExpanded] ?: false }

    val isConfigSacredMachineBrandThemeExpanded: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.ConfigSacredMachineBrandThemeExpanded] ?: false }

    val isConfigAncientDynastyBrandThemeExpanded: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.ConfigAncientDynastyBrandThemeExpanded] ?: false }

    val isConfigImmortalRotBrandThemeExpanded: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.ConfigImmortalRotBrandThemeExpanded] ?: false }

    val isConfigScarletCarnageBrandThemeExpanded: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.ConfigScarletCarnageBrandThemeExpanded] ?: false }

    val isConfigExquisiteFallBrandThemeExpanded: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.ConfigExquisiteFallBrandThemeExpanded] ?: false }

    val isConfigLabyrinthOfMutabilityBrandThemeExpanded: Flow<Boolean> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences -> preferences[Keys.ConfigLabyrinthOfMutabilityBrandThemeExpanded] ?: false }

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

    suspend fun setSelectedBrandThemeId(brandThemeId: String) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.SelectedBrandThemeId] = brandThemeId
        }
    }

    suspend fun setCustomBrandThemePresets(settings: List<CustomBrandThemeSettings>) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.CustomBrandThemePresetsJson] = CustomBrandThemeSettingsStore.encode(settings)
        }
    }

    suspend fun setCustomMaterialThemePresets(settings: List<CustomBrandThemeSettings>) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.CustomMaterialThemeJson] = CustomBrandThemeSettingsStore.encode(settings)
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

    suspend fun setConfigCustomBrandThemeExpanded(expanded: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.ConfigCustomBrandThemeExpanded] = expanded
        }
    }

    suspend fun setConfigSampleTextExpanded(expanded: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.ConfigSampleTextExpanded] = expanded
        }
    }

    suspend fun setConfigSacredMachineBrandThemeExpanded(expanded: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.ConfigSacredMachineBrandThemeExpanded] = expanded
        }
    }

    suspend fun setConfigAncientDynastyBrandThemeExpanded(expanded: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.ConfigAncientDynastyBrandThemeExpanded] = expanded
        }
    }

    suspend fun setConfigImmortalRotBrandThemeExpanded(expanded: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.ConfigImmortalRotBrandThemeExpanded] = expanded
        }
    }

    suspend fun setConfigScarletCarnageBrandThemeExpanded(expanded: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.ConfigScarletCarnageBrandThemeExpanded] = expanded
        }
    }

    suspend fun setConfigExquisiteFallBrandThemeExpanded(expanded: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.ConfigExquisiteFallBrandThemeExpanded] = expanded
        }
    }

    suspend fun setConfigLabyrinthOfMutabilityBrandThemeExpanded(expanded: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.ConfigLabyrinthOfMutabilityBrandThemeExpanded] = expanded
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

    suspend fun setFlashVisualPerfOverlayEnabled(enabled: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.FlashVisualPerfOverlayEnabled] = enabled
        }
    }

    private object Keys {
        val SelectedPaletteId: Preferences.Key<String> = stringPreferencesKey("palette_id")
        val SelectedFlashVoicingStyleId: Preferences.Key<String> = stringPreferencesKey("flash_voicing_style")
        val SelectedMorseSpeedStyleId: Preferences.Key<String> = stringPreferencesKey("mini_speed_style")
        val FlashVoicingEnabled: Preferences.Key<Boolean> = booleanPreferencesKey("flash_voicing_enabled")
        val SelectedThemeModeId: Preferences.Key<String> = stringPreferencesKey("theme_mode")
        val SelectedMaterialPaletteIdLight: Preferences.Key<String> = stringPreferencesKey("material_palette_id_light")
        val SelectedMaterialPaletteIdDark: Preferences.Key<String> = stringPreferencesKey("material_palette_id_dark")
        val SelectedThemeStyleId: Preferences.Key<String> = stringPreferencesKey("theme_style")
        val SelectedBrandThemeId: Preferences.Key<String> = stringPreferencesKey("brand_theme_id")
        val CustomBrandThemePresetsJson: Preferences.Key<String> = stringPreferencesKey("custom_brand_presets_json")
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
        val ConfigCustomBrandThemeExpanded: Preferences.Key<Boolean> = booleanPreferencesKey("config_custom_brand_theme_expanded")
        val ConfigSampleTextExpanded: Preferences.Key<Boolean> = booleanPreferencesKey("config_sample_text_expanded")
        val ConfigSacredMachineBrandThemeExpanded: Preferences.Key<Boolean> =
            booleanPreferencesKey("config_sacred_machine_brand_theme_expanded")
        val ConfigAncientDynastyBrandThemeExpanded: Preferences.Key<Boolean> =
            booleanPreferencesKey("config_ancient_dynasty_brand_theme_expanded")
        val ConfigImmortalRotBrandThemeExpanded: Preferences.Key<Boolean> =
            booleanPreferencesKey("config_immortal_rot_brand_theme_expanded")
        val ConfigScarletCarnageBrandThemeExpanded: Preferences.Key<Boolean> =
            booleanPreferencesKey("config_scarlet_carnage_brand_theme_expanded")
        val ConfigExquisiteFallBrandThemeExpanded: Preferences.Key<Boolean> =
            booleanPreferencesKey("config_exquisite_fall_brand_theme_expanded")
        val ConfigLabyrinthOfMutabilityBrandThemeExpanded: Preferences.Key<Boolean> =
            booleanPreferencesKey("config_labyrinth_of_mutability_brand_theme_expanded")
        val ConfigDebugExpanded: Preferences.Key<Boolean> = booleanPreferencesKey("config_debug_expanded")
        val DemoModeEnabled: Preferences.Key<Boolean> = booleanPreferencesKey("demo_mode_enabled")
        val SampleAutoFillEnabled: Preferences.Key<Boolean> = booleanPreferencesKey("sample_auto_fill_enabled")
        val SampleDecorationEnabled: Preferences.Key<Boolean> = booleanPreferencesKey("sample_decoration_enabled")
        val FlashVisualPerfOverlayEnabled: Preferences.Key<Boolean> = booleanPreferencesKey("visual_fps_overlay_enabled")
    }
}

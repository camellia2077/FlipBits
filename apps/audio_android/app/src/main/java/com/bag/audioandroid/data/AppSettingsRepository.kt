package com.bag.audioandroid.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

class AppSettingsRepository(
    private val appContext: Context
) {
    val selectedPaletteId: Flow<String?> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences -> preferences[Keys.SelectedPaletteId] }

    val selectedFlashVoicingStyleId: Flow<String?> =
        appContext.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences -> preferences[Keys.SelectedFlashVoicingStyleId] }

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

    private object Keys {
        val SelectedPaletteId: Preferences.Key<String> = stringPreferencesKey("palette_id")
        val SelectedFlashVoicingStyleId: Preferences.Key<String> = stringPreferencesKey("flash_voicing_style")
    }
}

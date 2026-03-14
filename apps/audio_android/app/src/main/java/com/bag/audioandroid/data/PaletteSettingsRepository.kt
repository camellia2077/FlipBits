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

private val Context.paletteSettingsDataStore by preferencesDataStore(name = "palette_settings")

class PaletteSettingsRepository(
    private val appContext: Context
) {
    val selectedPaletteId: Flow<String?> =
        appContext.paletteSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences -> preferences[Keys.SelectedPaletteId] }

    suspend fun setSelectedPaletteId(paletteId: String) {
        appContext.paletteSettingsDataStore.edit { preferences ->
            preferences[Keys.SelectedPaletteId] = paletteId
        }
    }

    private object Keys {
        val SelectedPaletteId: Preferences.Key<String> = stringPreferencesKey("palette_id")
    }
}

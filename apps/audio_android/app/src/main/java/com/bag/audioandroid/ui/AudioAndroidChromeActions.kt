package com.bag.audioandroid.ui

import androidx.appcompat.app.AppCompatDelegate
import com.bag.audioandroid.data.PaletteSettingsRepository
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.AppTab
import com.bag.audioandroid.ui.model.PaletteOption
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.LibrarySelectionUiState
import com.bag.audioandroid.ui.theme.MaterialPalettes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class AudioAndroidChromeActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val sampleInputSessionUpdater: SampleInputSessionUpdater,
    private val paletteSettingsRepository: PaletteSettingsRepository,
    private val scope: CoroutineScope
) {
    fun onTabSelected(
        tab: AppTab,
        refreshSavedAudioItems: () -> Unit
    ) {
        if (tab == AppTab.Library) {
            refreshSavedAudioItems()
        }
        uiState.update { state ->
            state.copy(
                selectedTab = tab,
                librarySelection = if (tab == AppTab.Library) {
                    state.librarySelection
                } else {
                    LibrarySelectionUiState()
                }
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
                sessions = sampleInputSessionUpdater.refreshForLanguageChange(
                    state.sessions,
                    previousLanguage,
                    language
                )
            )
        }
        AppCompatDelegate.setApplicationLocales(language.toLocaleList())
    }

    fun onOpenAboutPage() {
        uiState.update { it.copy(showAboutPage = true) }
    }

    fun onCloseAboutPage() {
        uiState.update { it.copy(showAboutPage = false) }
    }

    fun onOpenLicensesPage() {
        uiState.update { it.copy(showLicensesPage = true, showAboutPage = false) }
    }

    fun onCloseLicensesPage() {
        uiState.update { it.copy(showLicensesPage = false, showAboutPage = true) }
    }

    fun onPaletteSelected(palette: PaletteOption) {
        uiState.update { it.copy(selectedPalette = palette) }
        scope.launch {
            paletteSettingsRepository.setSelectedPaletteId(palette.id)
        }
    }

    fun observeSelectedPalette() {
        scope.launch {
            paletteSettingsRepository.selectedPaletteId
                .distinctUntilChanged()
                .collect { paletteId ->
                    val palette = MaterialPalettes.firstOrNull { it.id == paletteId } ?: MaterialPalettes.first()
                    uiState.update { state ->
                        if (state.selectedPalette.id == palette.id) {
                            state
                        } else {
                            state.copy(selectedPalette = palette)
                        }
                    }
                }
        }
    }
}

package com.bag.audioandroid.ui

import com.bag.audioandroid.ui.model.FactionThemeOption
import com.bag.audioandroid.ui.model.SampleFlavor
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.model.effectiveSampleFlavor
import com.bag.audioandroid.ui.state.AudioAppUiState

internal val AudioAppUiState.currentSampleFlavor: SampleFlavor
    get() = effectiveSampleFlavor(selectedThemeStyle, activeFactionTheme)

internal fun AudioAppUiState.withSelectedFactionTheme(
    factionTheme: FactionThemeOption,
    sampleInputSessionUpdater: SampleInputSessionUpdater,
): AudioAppUiState {
    if (selectedFactionTheme.id == factionTheme.id) {
        return this
    }
    val newFlavor = effectiveSampleFlavor(selectedThemeStyle, factionTheme)
    val updatedSessions =
        if (currentSampleFlavor != newFlavor) {
            sampleInputSessionUpdater.refreshForFlavorChange(
                sessions = sessions,
                language = selectedLanguage,
                newFlavor = newFlavor,
                isSampleAutoFillEnabled = isSampleAutoFillEnabled,
            )
        } else {
            sessions
        }
    return copy(
        selectedFactionTheme = factionTheme,
        sessions = updatedSessions,
    )
}

internal fun AudioAppUiState.withSelectedThemeStyle(
    themeStyle: ThemeStyleOption,
    sampleInputSessionUpdater: SampleInputSessionUpdater,
): AudioAppUiState {
    if (selectedThemeStyle == themeStyle) {
        return this
    }
    val newFlavor = effectiveSampleFlavor(themeStyle, activeFactionTheme)
    val updatedSessions =
        if (currentSampleFlavor != newFlavor) {
            sampleInputSessionUpdater.refreshForFlavorChange(
                sessions = sessions,
                language = selectedLanguage,
                newFlavor = newFlavor,
                isSampleAutoFillEnabled = isSampleAutoFillEnabled,
            )
        } else {
            sessions
        }
    return copy(
        selectedThemeStyle = themeStyle,
        sessions = updatedSessions,
    )
}

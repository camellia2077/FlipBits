package com.bag.audioandroid.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.bag.audioandroid.ui.model.SavedAudioModeFilter
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.screen.AboutScreen
import com.bag.audioandroid.ui.screen.DebugMorseVisualizationModeRequest
import com.bag.audioandroid.ui.screen.DebugPlaybackDisplayModeRequest
import com.bag.audioandroid.ui.screen.OpenSourceLicensesScreen
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.theme.BrandDualToneThemes
import com.bag.audioandroid.ui.theme.LocalAppThemeAccentTokens
import com.bag.audioandroid.ui.theme.LocalAppThemeVisualTokens
import com.bag.audioandroid.ui.theme.LocalAudioEncodeGlyphColors
import com.bag.audioandroid.ui.theme.MaterialPalettes
import com.bag.audioandroid.ui.theme.materialAccentTokens
import com.bag.audioandroid.ui.theme.materialThemeVisualTokens
import com.bag.audioandroid.ui.theme.rememberAnimatedBrandThemeSnapshot
import com.bag.audioandroid.ui.theme.rememberAnimatedMaterialColorScheme
import com.bag.audioandroid.ui.theme.rememberAnimatedThemeSnapshot

@Composable
internal fun AudioAndroidAppShell(
    uiState: AudioAppUiState,
    savedAudioFilter: SavedAudioModeFilter,
    onSavedAudioFilterChange: (SavedAudioModeFilter) -> Unit,
    debugScenario: FlashDebugScenario? = null,
    debugExpandLyricsRequestId: Long? = null,
    onDebugExpandLyricsHandled: (Long) -> Unit = {},
    debugPlaybackDisplayModeRequest: DebugPlaybackDisplayModeRequest? = null,
    onDebugPlaybackDisplayModeHandled: (Long) -> Unit = {},
    debugMorseVisualizationModeRequest: DebugMorseVisualizationModeRequest? = null,
    onDebugMorseVisualizationModeHandled: (Long) -> Unit = {},
    onImportAudio: () -> Unit,
    viewModel: AudioAndroidViewModel,
    modifier: Modifier = Modifier,
) {
    val shouldUseDarkTheme = shouldUseDarkTheme(uiState.selectedThemeMode)
    LaunchedEffect(shouldUseDarkTheme) {
        viewModel.onMaterialDarkThemeActiveChanged(shouldUseDarkTheme)
    }
    val animatedBrandThemeSnapshot = rememberAnimatedBrandThemeSnapshot(uiState.activeBrandTheme)
    val materialColorScheme =
        if (shouldUseDarkTheme) {
            uiState.selectedPalette.darkScheme
        } else {
            uiState.selectedPalette.lightScheme
        }
    val animatedMaterialColorScheme = rememberAnimatedMaterialColorScheme(materialColorScheme)
    val colorScheme =
        when (uiState.selectedThemeStyle) {
            // Dual-tone brand themes are curated, fixed looks. They can be either light or dark
            // on their own, so they do not follow the app's Material light/dark mode toggle.
            ThemeStyleOption.BrandDualTone -> animatedBrandThemeSnapshot.colorScheme
            ThemeStyleOption.Material -> animatedMaterialColorScheme
        }
    val targetAccentTokens =
        when (uiState.selectedThemeStyle) {
            ThemeStyleOption.BrandDualTone -> animatedBrandThemeSnapshot.accentTokens
            ThemeStyleOption.Material -> materialAccentTokens(colorScheme.primary)
        }
    val targetVisualTokens =
        when (uiState.selectedThemeStyle) {
            ThemeStyleOption.BrandDualTone -> animatedBrandThemeSnapshot.visualTokens
            ThemeStyleOption.Material -> materialThemeVisualTokens(colorScheme)
        }
    val animatedThemeSnapshot =
        rememberAnimatedThemeSnapshot(
            targetColorScheme = colorScheme,
            targetAccentTokens = targetAccentTokens,
            targetVisualTokens = targetVisualTokens,
        )

    MaterialTheme(colorScheme = animatedThemeSnapshot.colorScheme) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalAppThemeAccentTokens provides animatedThemeSnapshot.accentTokens,
            LocalAppThemeVisualTokens provides animatedThemeSnapshot.visualTokens,
            LocalAudioEncodeGlyphColors provides animatedThemeSnapshot.visualTokens.audioEncodeGlyphColors,
        ) {
            when {
                uiState.showLicensesPage -> {
                    OpenSourceLicensesScreen(onBack = viewModel::onCloseLicensesPage)
                }

                uiState.showAboutPage -> {
                    AboutScreen(
                        onBack = viewModel::onCloseAboutPage,
                        onOpenLicensesPage = viewModel::onOpenLicensesPage,
                        presentationVersion = uiState.presentationVersion,
                        coreVersion = uiState.coreVersion,
                    )
                }

                else -> {
                    AudioAndroidMainScaffold(
                        uiState = uiState,
                        savedAudioFilter = savedAudioFilter,
                        onSavedAudioFilterChange = onSavedAudioFilterChange,
                        accentTokens = animatedThemeSnapshot.accentTokens,
                        materialPalettes = MaterialPalettes,
                        brandThemes = BrandDualToneThemes,
                        viewModel = viewModel,
                        debugScenario = debugScenario,
                        debugExpandLyricsRequestId = debugExpandLyricsRequestId,
                        onDebugExpandLyricsHandled = onDebugExpandLyricsHandled,
                        debugPlaybackDisplayModeRequest = debugPlaybackDisplayModeRequest,
                        onDebugPlaybackDisplayModeHandled = onDebugPlaybackDisplayModeHandled,
                        debugMorseVisualizationModeRequest = debugMorseVisualizationModeRequest,
                        onDebugMorseVisualizationModeHandled = onDebugMorseVisualizationModeHandled,
                        onImportAudio = onImportAudio,
                        modifier = modifier,
                    )
                }
            }
        }
    }
}

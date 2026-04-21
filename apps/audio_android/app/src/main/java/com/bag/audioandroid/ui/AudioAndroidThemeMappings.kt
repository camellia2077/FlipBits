package com.bag.audioandroid.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.lerp
import com.bag.audioandroid.ui.model.ThemeModeOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.state.AudioAppUiState

private const val LightDualToneNavUnselectedMix = 0.4f

@Composable
internal fun navigationBarItemColors(uiState: AudioAppUiState): NavigationBarItemColors =
    when (uiState.selectedThemeStyle) {
        ThemeStyleOption.BrandDualTone -> {
            // Light dual-tone themes need a corrected unselected foreground so the brighter
            // paired color does not disappear into the navigation container. Dark dual-tone
            // themes already separate strongly enough, so they keep the raw paired color.
            val unselectedDualToneForeground =
                if (uiState.selectedBrandTheme.isDarkTheme) {
                    uiState.selectedBrandTheme.secondaryColor
                } else {
                    lerp(
                        uiState.selectedBrandTheme.secondaryColor,
                        uiState.selectedBrandTheme.colorScheme.onPrimaryContainer,
                        LightDualToneNavUnselectedMix,
                    )
                }
            NavigationBarItemDefaults.colors(
                // Dual-tone navigation keeps selected/unselected states on the original
                // paired colors instead of relying on Material's derived alpha variants.
                selectedIconColor =
                    if (uiState.selectedBrandTheme.isDarkTheme) {
                        uiState.selectedBrandTheme.primaryColor
                    } else {
                        uiState.selectedBrandTheme.secondaryColor
                    },
                selectedTextColor =
                    if (uiState.selectedBrandTheme.isDarkTheme) {
                        uiState.selectedBrandTheme.secondaryColor
                    } else {
                        uiState.selectedBrandTheme.primaryColor
                    },
                indicatorColor =
                    if (uiState.selectedBrandTheme.isDarkTheme) {
                        uiState.selectedBrandTheme.secondaryColor
                    } else {
                        uiState.selectedBrandTheme.primaryColor
                    },
                unselectedIconColor = unselectedDualToneForeground,
                unselectedTextColor = unselectedDualToneForeground,
            )
        }

        ThemeStyleOption.Material ->
            NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                unselectedTextColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
            )
    }

@Composable
internal fun shouldUseDarkTheme(selectedThemeMode: ThemeModeOption): Boolean {
    val systemDarkTheme = isSystemInDarkTheme()
    return when (selectedThemeMode) {
        ThemeModeOption.FollowSystem -> systemDarkTheme
        ThemeModeOption.Light -> false
        ThemeModeOption.Dark -> true
    }
}

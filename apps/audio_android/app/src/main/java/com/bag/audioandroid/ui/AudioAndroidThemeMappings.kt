package com.bag.audioandroid.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.bag.audioandroid.ui.model.ThemeModeOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.theme.appThemeAccentTokens
import com.bag.audioandroid.ui.theme.appThemeVisualTokens

private const val LightDualToneNavUnselectedMix = 0.4f

@Immutable
internal data class PlayerChromeColors(
    val accent: Color,
    val onAccent: Color,
    val neutralAction: Color,
    val mutedAction: Color,
    val disabledAction: Color,
    val annotationChipContainer: Color,
    val annotationChipContent: Color,
)

/**
 * Returns the container color for the "Dock System" (Mini Player + Bottom Navigation).
 *
 * DESIGN DECISION:
 * The mini-player and bottom tabs form a single visual dock. They MUST share the same 
 * container color so they read as part of the same base layer across every theme.
 * Modification here will affect both components simultaneously.
 */
@Composable
internal fun playerDockContainerColor(uiState: AudioAppUiState): Color =
    appThemeVisualTokens().dockContainerColor

@Composable
internal fun playerSegmentedButtonColors() =
    SegmentedButtonDefaults.colors(
        // Keep player control switchers on one shared color recipe so Visual/Lyrics,
        // FSK lanes/Tone energy, and other playback toggles stay visually aligned.
        activeContainerColor = MaterialTheme.colorScheme.primary,
        activeContentColor = MaterialTheme.colorScheme.onPrimary,
        activeBorderColor = MaterialTheme.colorScheme.primary,
        inactiveContainerColor = appThemeVisualTokens().segmentedInactiveContainerColor,
        inactiveContentColor = appThemeVisualTokens().segmentedInactiveContentColor,
        inactiveBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
    )

@Composable
internal fun appSegmentedButtonColors() =
    SegmentedButtonDefaults.colors(
        // Non-player segmented controls should still read as stateful accent chrome.
        // Keep the inactive border on a softened primary lane instead of fading back to
        // outlineVariant so Audio and Saved switchers stay visually consistent.
        activeContainerColor = MaterialTheme.colorScheme.primary,
        activeContentColor = MaterialTheme.colorScheme.onPrimary,
        activeBorderColor = MaterialTheme.colorScheme.primary,
        inactiveContainerColor = appThemeVisualTokens().segmentedInactiveContainerColor,
        inactiveContentColor = appThemeVisualTokens().segmentedInactiveContentColor,
        inactiveBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
    )

@Composable
internal fun audioInputTextFieldColors(selectedThemeStyle: ThemeStyleOption): TextFieldColors {
    val visualTokens = appThemeVisualTokens()
    return when (selectedThemeStyle) {
        ThemeStyleOption.BrandDualTone -> {
            OutlinedTextFieldDefaults.colors(
                focusedContainerColor = visualTokens.inputContainerColor,
                unfocusedContainerColor = visualTokens.inputContainerColor,
                disabledContainerColor = visualTokens.inputContainerColor,
                errorContainerColor = visualTokens.inputContainerColor,
                focusedBorderColor = visualTokens.inputFocusedBorderColor,
                unfocusedBorderColor = visualTokens.inputUnfocusedBorderColor,
                disabledBorderColor = visualTokens.inputUnfocusedBorderColor,
            )
        }

        ThemeStyleOption.Material -> OutlinedTextFieldDefaults.colors()
    }
}

@Composable
internal fun playerChromeColors(): PlayerChromeColors =
    PlayerChromeColors(
        accent = MaterialTheme.colorScheme.primary,
        onAccent = MaterialTheme.colorScheme.onPrimary,
        neutralAction = MaterialTheme.colorScheme.onSurfaceVariant,
        mutedAction = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledAction = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f),
        annotationChipContainer = appThemeVisualTokens().annotationChipContainerColor,
        annotationChipContent = appThemeVisualTokens().annotationChipContentColor,
    )

@Composable
internal fun utilityActionIconButtonColors(): IconButtonColors {
    val actionTint = appThemeAccentTokens().selectionLabelAccentTint
    return IconButtonDefaults.iconButtonColors(
        // Utility action icons across Settings / Audio / Saved should all follow the same
        // accent lane instead of each screen inventing its own hard-coded tint behavior.
        contentColor = actionTint,
        disabledContentColor = actionTint.copy(alpha = 0.38f),
    )
}

@Composable
internal fun playbackLyricsAccentTextColor(): Color =
    appThemeAccentTokens().selectionLabelAccentTint

@Composable
internal fun navigationBarItemColors(uiState: AudioAppUiState): NavigationBarItemColors =
    when (uiState.selectedThemeStyle) {
        ThemeStyleOption.BrandDualTone -> {
            // Light dual-tone themes need a corrected unselected foreground so the brighter
            // paired color does not disappear into the navigation container. Dark dual-tone
            // themes already separate strongly enough, so they keep the raw paired color.
            val brandTheme = uiState.activeBrandTheme
            val unselectedDualToneForeground =
                if (brandTheme.isDarkTheme) {
                    brandTheme.accentColor
                } else {
                    lerp(
                        brandTheme.accentColor,
                        brandTheme.colorScheme.onPrimaryContainer,
                        LightDualToneNavUnselectedMix,
                    )
                }
            NavigationBarItemDefaults.colors(
                // Dual-tone navigation keeps selected/unselected states on the original
                // paired colors instead of relying on Material's derived alpha variants.
                selectedIconColor = brandTheme.backgroundColor,
                selectedTextColor = brandTheme.accentColor,
                indicatorColor = brandTheme.accentColor,
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

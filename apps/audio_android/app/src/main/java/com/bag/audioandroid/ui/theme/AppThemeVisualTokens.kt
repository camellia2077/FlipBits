package com.bag.audioandroid.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.bag.audioandroid.ui.model.BrandThemeOption
import com.bag.audioandroid.ui.model.ThemeStyleOption

@Immutable
data class AppThemeVisualTokens(
    val themeStyle: ThemeStyleOption,
    val dockContainerColor: Color,
    val modalContainerColor: Color,
    val modalContentColor: Color,
    val segmentedInactiveContainerColor: Color,
    val segmentedInactiveContentColor: Color,
    val selectionSelectedContainerColor: Color,
    val selectionUnselectedContainerColor: Color,
    val groupContainerColor: Color,
    val supportSurfaceColor: Color,
    val supportStrongSurfaceColor: Color,
    val inputContainerColor: Color,
    val inputContentColor: Color,
    val inputFocusedBorderColor: Color,
    val inputUnfocusedBorderColor: Color,
    val actionContainerColor: Color,
    val actionDisabledContainerColor: Color,
    val actionContentColor: Color,
    val actionDisabledContentColor: Color,
    val miniPlayerLeadingContainerColor: Color,
    val miniPlayerLeadingContentColor: Color,
    val annotationChipContainerColor: Color,
    val annotationChipContentColor: Color,
    val followTokenContainerColor: Color,
    val subtleOutlineColor: Color,
    val timelineInactiveTrackColor: Color,
    val visualizationBaseBackgroundColor: Color,
    val visualizationInactiveToneColor: Color,
)

internal val DefaultAppThemeVisualTokens =
    AppThemeVisualTokens(
        themeStyle = ThemeStyleOption.Material,
        dockContainerColor = Color.Unspecified,
        modalContainerColor = Color.Unspecified,
        modalContentColor = Color.Unspecified,
        segmentedInactiveContainerColor = Color.Unspecified,
        segmentedInactiveContentColor = Color.Unspecified,
        selectionSelectedContainerColor = Color.Unspecified,
        selectionUnselectedContainerColor = Color.Unspecified,
        groupContainerColor = Color.Unspecified,
        supportSurfaceColor = Color.Unspecified,
        supportStrongSurfaceColor = Color.Unspecified,
        inputContainerColor = Color.Unspecified,
        inputContentColor = Color.Unspecified,
        inputFocusedBorderColor = Color.Unspecified,
        inputUnfocusedBorderColor = Color.Unspecified,
        actionContainerColor = Color.Unspecified,
        actionDisabledContainerColor = Color.Unspecified,
        actionContentColor = Color.Unspecified,
        actionDisabledContentColor = Color.Unspecified,
        miniPlayerLeadingContainerColor = Color.Unspecified,
        miniPlayerLeadingContentColor = Color.Unspecified,
        annotationChipContainerColor = Color.Unspecified,
        annotationChipContentColor = Color.Unspecified,
        followTokenContainerColor = Color.Unspecified,
        subtleOutlineColor = Color.Unspecified,
        timelineInactiveTrackColor = Color.Unspecified,
        visualizationBaseBackgroundColor = Color.Unspecified,
        visualizationInactiveToneColor = Color.Unspecified,
    )

internal val DefaultAppThemeAccentTokens =
    AppThemeAccentTokens(
        disclosureAccentTint = Color.Unspecified,
        actionAccentTint = Color.Unspecified,
        selectionLabelAccentTint = Color.Unspecified,
        selectionBorderAccentTint = Color.Unspecified,
    )

val LocalAppThemeVisualTokens = staticCompositionLocalOf { DefaultAppThemeVisualTokens }

@Composable
@ReadOnlyComposable
fun appThemeVisualTokens(): AppThemeVisualTokens = LocalAppThemeVisualTokens.current

fun materialThemeVisualTokens(colorScheme: ColorScheme): AppThemeVisualTokens =
    AppThemeVisualTokens(
        themeStyle = ThemeStyleOption.Material,
        dockContainerColor = colorScheme.surface,
        modalContainerColor = colorScheme.primaryContainer,
        modalContentColor = colorScheme.onPrimaryContainer,
        segmentedInactiveContainerColor = colorScheme.primaryContainer,
        segmentedInactiveContentColor = colorScheme.onPrimaryContainer,
        selectionSelectedContainerColor = colorScheme.secondaryContainer.copy(alpha = 0.12f),
        selectionUnselectedContainerColor = colorScheme.surfaceVariant.copy(alpha = 0.28f),
        groupContainerColor = colorScheme.surfaceVariant.copy(alpha = 0.24f),
        supportSurfaceColor = colorScheme.surfaceVariant.copy(alpha = 0.72f),
        supportStrongSurfaceColor = colorScheme.primaryContainer,
        inputContainerColor = colorScheme.surface,
        inputContentColor = colorScheme.onSurface,
        inputFocusedBorderColor = colorScheme.primary,
        inputUnfocusedBorderColor = colorScheme.outline,
        actionContainerColor = colorScheme.primaryContainer,
        actionDisabledContainerColor = colorScheme.surfaceVariant,
        actionContentColor = colorScheme.onPrimaryContainer,
        actionDisabledContentColor = colorScheme.onSurfaceVariant,
        miniPlayerLeadingContainerColor = colorScheme.primaryContainer,
        miniPlayerLeadingContentColor = colorScheme.onPrimaryContainer,
        annotationChipContainerColor = colorScheme.surfaceVariant,
        annotationChipContentColor = colorScheme.onSurfaceVariant,
        followTokenContainerColor = colorScheme.surfaceVariant.copy(alpha = 0.28f),
        subtleOutlineColor = colorScheme.outlineVariant,
        timelineInactiveTrackColor = colorScheme.primaryContainer,
        visualizationBaseBackgroundColor = colorScheme.primaryContainer.copy(alpha = 0.24f),
        visualizationInactiveToneColor = colorScheme.outlineVariant,
    )

fun brandThemeVisualTokens(
    theme: BrandThemeOption,
    accentTokens: AppThemeAccentTokens,
): AppThemeVisualTokens {
    val base = theme.backgroundColor
    val accent = theme.accentColor
    val onSurface = theme.colorScheme.onSurface
    val isDarkTheme = theme.backgroundColor.luminance() < 0.5f

    // "Ancient Alloy" philosophy: Use a recessed shadow derived from the background
    // for structural elements instead of a decorative third color.
    val depthShadow = blend(base, Color.Black, if (isDarkTheme) 0.62f else 0.28f)

    return AppThemeVisualTokens(
        themeStyle = ThemeStyleOption.BrandDualTone,
        // Keep the dock on the main color lane with just enough accent tint to separate it from
        // the page background. This avoids falling back to Material's primaryContainer semantics.
        dockContainerColor = blend(base, accent, 0.10f),
        modalContainerColor = base,
        modalContentColor = onSurface,
        segmentedInactiveContainerColor = Color.Transparent,
        segmentedInactiveContentColor = onSurface,
        // Selected and idle rows should stay on the main color lane; only text/border switch to
        // the accent lane. This avoids turning every settings row into a tinted accent chip.
        selectionSelectedContainerColor = blend(base, accent, 0.08f),
        selectionUnselectedContainerColor = Color.Transparent,
        groupContainerColor = blend(base, accent, 0.06f),
        supportSurfaceColor = blend(base, onSurface, 0.07f),
        supportStrongSurfaceColor = blend(base, accent, 0.12f),
        inputContainerColor = blend(base, accent, 0.08f),
        inputContentColor = onSurface,
        inputFocusedBorderColor = accentTokens.selectionBorderAccentTint,
        inputUnfocusedBorderColor = accentTokens.selectionBorderAccentTint.copy(alpha = 0.42f),
        actionContainerColor = blend(base, accent, 0.12f),
        actionDisabledContainerColor = blend(base, onSurface, 0.08f),
        actionContentColor = accentTokens.disclosureAccentTint,
        actionDisabledContentColor = onSurface.copy(alpha = 0.38f),
        miniPlayerLeadingContainerColor = blend(base, accent, 0.14f),
        miniPlayerLeadingContentColor = accentTokens.selectionLabelAccentTint,
        annotationChipContainerColor = blend(base, accent, 0.10f),
        annotationChipContentColor = onSurface,
        followTokenContainerColor = blend(base, accent, 0.06f),
        subtleOutlineColor = depthShadow.copy(alpha = 0.52f),
        timelineInactiveTrackColor = blend(base, accent, 0.10f),
        visualizationBaseBackgroundColor = blend(base, accent, 0.10f),
        // Inactive waveform/tone rails now always use the recessed shadow
        // to match the Ancient Alloy philosophy of depth.
        visualizationInactiveToneColor = depthShadow,
    )
}

private fun blend(
    from: Color,
    to: Color,
    ratio: Float,
): Color = lerp(from, to, ratio.coerceIn(0f, 1f))

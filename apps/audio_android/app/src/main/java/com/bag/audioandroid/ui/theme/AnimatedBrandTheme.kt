package com.bag.audioandroid.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.bag.audioandroid.ui.model.BrandThemeOption

private const val BrandThemeBackgroundDurationMs = 200
private const val BrandThemeBackgroundLargeShiftDurationMs = 240
private const val BrandThemeStructureDurationMs = 180
private const val BrandThemeAccentDurationMs = 150
private const val MaterialThemeColorDurationMs = 180
private const val LargePrimaryShiftLuminanceDelta = 0.25f
private const val LargePrimaryShiftChannelDelta = 0.45f

@Immutable
data class AnimatedBrandThemeSnapshot(
    val colorScheme: ColorScheme,
    val accentTokens: AppThemeAccentTokens,
    val visualTokens: AppThemeVisualTokens,
)

@Immutable
data class AnimatedThemeSnapshot(
    val colorScheme: ColorScheme,
    val accentTokens: AppThemeAccentTokens,
    val visualTokens: AppThemeVisualTokens,
)

@Composable
fun rememberAnimatedMaterialColorScheme(target: ColorScheme): ColorScheme {
    val colorSpec =
        tween<Color>(
            durationMillis = MaterialThemeColorDurationMs,
            easing = FastOutSlowInEasing,
        )
    return target.copy(
        primary = animateThemeColor(target.primary, colorSpec, "materialPrimary"),
        onPrimary = animateThemeColor(target.onPrimary, colorSpec, "materialOnPrimary"),
        primaryContainer = animateThemeColor(target.primaryContainer, colorSpec, "materialPrimaryContainer"),
        onPrimaryContainer =
            animateThemeColor(
                target.onPrimaryContainer,
                colorSpec,
                "materialOnPrimaryContainer",
            ),
        secondary = animateThemeColor(target.secondary, colorSpec, "materialSecondary"),
        onSecondary = animateThemeColor(target.onSecondary, colorSpec, "materialOnSecondary"),
        secondaryContainer = animateThemeColor(target.secondaryContainer, colorSpec, "materialSecondaryContainer"),
        onSecondaryContainer =
            animateThemeColor(
                target.onSecondaryContainer,
                colorSpec,
                "materialOnSecondaryContainer",
            ),
        tertiary = animateThemeColor(target.tertiary, colorSpec, "materialTertiary"),
        onTertiary = animateThemeColor(target.onTertiary, colorSpec, "materialOnTertiary"),
        tertiaryContainer = animateThemeColor(target.tertiaryContainer, colorSpec, "materialTertiaryContainer"),
        onTertiaryContainer =
            animateThemeColor(
                target.onTertiaryContainer,
                colorSpec,
                "materialOnTertiaryContainer",
            ),
        background = animateThemeColor(target.background, colorSpec, "materialBackground"),
        onBackground = animateThemeColor(target.onBackground, colorSpec, "materialOnBackground"),
        surface = animateThemeColor(target.surface, colorSpec, "materialSurface"),
        onSurface = animateThemeColor(target.onSurface, colorSpec, "materialOnSurface"),
        surfaceVariant = animateThemeColor(target.surfaceVariant, colorSpec, "materialSurfaceVariant"),
        onSurfaceVariant =
            animateThemeColor(
                target.onSurfaceVariant,
                colorSpec,
                "materialOnSurfaceVariant",
            ),
        outline = animateThemeColor(target.outline, colorSpec, "materialOutline"),
        outlineVariant = animateThemeColor(target.outlineVariant, colorSpec, "materialOutlineVariant"),
    )
}

@Composable
fun rememberAnimatedThemeSnapshot(
    targetColorScheme: ColorScheme,
    targetAccentTokens: AppThemeAccentTokens,
    targetVisualTokens: AppThemeVisualTokens,
): AnimatedThemeSnapshot {
    val colorSpec =
        tween<Color>(
            durationMillis = MaterialThemeColorDurationMs,
            easing = FastOutSlowInEasing,
        )
    val animatedColorScheme =
        targetColorScheme.copy(
            primary = animateThemeColor(targetColorScheme.primary, colorSpec, "themePrimary"),
            onPrimary = animateThemeColor(targetColorScheme.onPrimary, colorSpec, "themeOnPrimary"),
            primaryContainer = animateThemeColor(targetColorScheme.primaryContainer, colorSpec, "themePrimaryContainer"),
            onPrimaryContainer =
                animateThemeColor(
                    targetColorScheme.onPrimaryContainer,
                    colorSpec,
                    "themeOnPrimaryContainer",
                ),
            secondary = animateThemeColor(targetColorScheme.secondary, colorSpec, "themeSecondary"),
            onSecondary = animateThemeColor(targetColorScheme.onSecondary, colorSpec, "themeOnSecondary"),
            secondaryContainer =
                animateThemeColor(
                    targetColorScheme.secondaryContainer,
                    colorSpec,
                    "themeSecondaryContainer",
                ),
            onSecondaryContainer =
                animateThemeColor(
                    targetColorScheme.onSecondaryContainer,
                    colorSpec,
                    "themeOnSecondaryContainer",
                ),
            tertiary = animateThemeColor(targetColorScheme.tertiary, colorSpec, "themeTertiary"),
            onTertiary = animateThemeColor(targetColorScheme.onTertiary, colorSpec, "themeOnTertiary"),
            tertiaryContainer =
                animateThemeColor(
                    targetColorScheme.tertiaryContainer,
                    colorSpec,
                    "themeTertiaryContainer",
                ),
            onTertiaryContainer =
                animateThemeColor(
                    targetColorScheme.onTertiaryContainer,
                    colorSpec,
                    "themeOnTertiaryContainer",
                ),
            background = animateThemeColor(targetColorScheme.background, colorSpec, "themeBackground"),
            onBackground = animateThemeColor(targetColorScheme.onBackground, colorSpec, "themeOnBackground"),
            surface = animateThemeColor(targetColorScheme.surface, colorSpec, "themeSurface"),
            onSurface = animateThemeColor(targetColorScheme.onSurface, colorSpec, "themeOnSurface"),
            surfaceVariant = animateThemeColor(targetColorScheme.surfaceVariant, colorSpec, "themeSurfaceVariant"),
            onSurfaceVariant =
                animateThemeColor(
                    targetColorScheme.onSurfaceVariant,
                    colorSpec,
                    "themeOnSurfaceVariant",
                ),
            outline = animateThemeColor(targetColorScheme.outline, colorSpec, "themeOutline"),
            outlineVariant =
                animateThemeColor(
                    targetColorScheme.outlineVariant,
                    colorSpec,
                    "themeOutlineVariant",
                ),
        )
    val animatedAccentTokens =
        targetAccentTokens.copy(
            disclosureAccentTint =
                animateThemeColor(targetAccentTokens.disclosureAccentTint, colorSpec, "themeDisclosureAccentTint"),
            actionAccentTint =
                animateThemeColor(targetAccentTokens.actionAccentTint, colorSpec, "themeActionAccentTint"),
            selectionLabelAccentTint =
                animateThemeColor(
                    targetAccentTokens.selectionLabelAccentTint,
                    colorSpec,
                    "themeSelectionLabelAccentTint",
                ),
            selectionBorderAccentTint =
                animateThemeColor(
                    targetAccentTokens.selectionBorderAccentTint,
                    colorSpec,
                    "themeSelectionBorderAccentTint",
                ),
        )
    val animatedVisualTokens =
        targetVisualTokens.copy(
            audioEncodeGlyphColors =
                targetVisualTokens.audioEncodeGlyphColors.copy(
                    primarySplit =
                        animateThemeColor(
                            targetVisualTokens.audioEncodeGlyphColors.primarySplit,
                            colorSpec,
                            "themeGlyphPrimarySplit",
                        ),
                    secondarySplit =
                        animateThemeColor(
                            targetVisualTokens.audioEncodeGlyphColors.secondarySplit,
                            colorSpec,
                            "themeGlyphSecondarySplit",
                        ),
                    outline =
                        animateThemeColor(
                            targetVisualTokens.audioEncodeGlyphColors.outline,
                            colorSpec,
                            "themeGlyphOutline",
                        ),
                ),
            dockContainerColor = animateThemeColor(targetVisualTokens.dockContainerColor, colorSpec, "themeDockContainerColor"),
            modalContainerColor = animateThemeColor(targetVisualTokens.modalContainerColor, colorSpec, "themeModalContainerColor"),
            modalContentColor = animateThemeColor(targetVisualTokens.modalContentColor, colorSpec, "themeModalContentColor"),
            segmentedInactiveContainerColor =
                animateThemeColor(
                    targetVisualTokens.segmentedInactiveContainerColor,
                    colorSpec,
                    "themeSegmentedInactiveContainerColor",
                ),
            segmentedInactiveContentColor =
                animateThemeColor(
                    targetVisualTokens.segmentedInactiveContentColor,
                    colorSpec,
                    "themeSegmentedInactiveContentColor",
                ),
            selectionSelectedContainerColor =
                animateThemeColor(
                    targetVisualTokens.selectionSelectedContainerColor,
                    colorSpec,
                    "themeSelectionSelectedContainerColor",
                ),
            selectionUnselectedContainerColor =
                animateThemeColor(
                    targetVisualTokens.selectionUnselectedContainerColor,
                    colorSpec,
                    "themeSelectionUnselectedContainerColor",
                ),
            groupContainerColor = animateThemeColor(targetVisualTokens.groupContainerColor, colorSpec, "themeGroupContainerColor"),
            supportSurfaceColor = animateThemeColor(targetVisualTokens.supportSurfaceColor, colorSpec, "themeSupportSurfaceColor"),
            supportStrongSurfaceColor =
                animateThemeColor(
                    targetVisualTokens.supportStrongSurfaceColor,
                    colorSpec,
                    "themeSupportStrongSurfaceColor",
                ),
            inputContainerColor = animateThemeColor(targetVisualTokens.inputContainerColor, colorSpec, "themeInputContainerColor"),
            inputContentColor = animateThemeColor(targetVisualTokens.inputContentColor, colorSpec, "themeInputContentColor"),
            inputFocusedBorderColor =
                animateThemeColor(
                    targetVisualTokens.inputFocusedBorderColor,
                    colorSpec,
                    "themeInputFocusedBorderColor",
                ),
            inputUnfocusedBorderColor =
                animateThemeColor(
                    targetVisualTokens.inputUnfocusedBorderColor,
                    colorSpec,
                    "themeInputUnfocusedBorderColor",
                ),
            actionContainerColor = animateThemeColor(targetVisualTokens.actionContainerColor, colorSpec, "themeActionContainerColor"),
            actionDisabledContainerColor =
                animateThemeColor(
                    targetVisualTokens.actionDisabledContainerColor,
                    colorSpec,
                    "themeActionDisabledContainerColor",
                ),
            actionContentColor = animateThemeColor(targetVisualTokens.actionContentColor, colorSpec, "themeActionContentColor"),
            actionDisabledContentColor =
                animateThemeColor(
                    targetVisualTokens.actionDisabledContentColor,
                    colorSpec,
                    "themeActionDisabledContentColor",
                ),
            miniPlayerLeadingContainerColor =
                animateThemeColor(
                    targetVisualTokens.miniPlayerLeadingContainerColor,
                    colorSpec,
                    "themeMiniPlayerLeadingContainerColor",
                ),
            miniPlayerLeadingContentColor =
                animateThemeColor(
                    targetVisualTokens.miniPlayerLeadingContentColor,
                    colorSpec,
                    "themeMiniPlayerLeadingContentColor",
                ),
            annotationChipContainerColor =
                animateThemeColor(
                    targetVisualTokens.annotationChipContainerColor,
                    colorSpec,
                    "themeAnnotationChipContainerColor",
                ),
            annotationChipContentColor =
                animateThemeColor(
                    targetVisualTokens.annotationChipContentColor,
                    colorSpec,
                    "themeAnnotationChipContentColor",
                ),
            followTokenContainerColor =
                animateThemeColor(
                    targetVisualTokens.followTokenContainerColor,
                    colorSpec,
                    "themeFollowTokenContainerColor",
                ),
            subtleOutlineColor = animateThemeColor(targetVisualTokens.subtleOutlineColor, colorSpec, "themeSubtleOutlineColor"),
            timelineInactiveTrackColor =
                animateThemeColor(
                    targetVisualTokens.timelineInactiveTrackColor,
                    colorSpec,
                    "themeTimelineInactiveTrackColor",
                ),
            visualizationBaseBackgroundColor =
                animateThemeColor(
                    targetVisualTokens.visualizationBaseBackgroundColor,
                    colorSpec,
                    "themeVisualizationBaseBackgroundColor",
                ),
            visualizationInactiveToneColor =
                animateThemeColor(
                    targetVisualTokens.visualizationInactiveToneColor,
                    colorSpec,
                    "themeVisualizationInactiveToneColor",
                ),
        )
    return AnimatedThemeSnapshot(
        colorScheme = animatedColorScheme,
        accentTokens = animatedAccentTokens,
        visualTokens = animatedVisualTokens,
    )
}

@Composable
fun rememberAnimatedBrandThemeSnapshot(theme: BrandThemeOption): AnimatedBrandThemeSnapshot {
    val previousThemeState = remember { mutableStateOf(theme) }
    val previousTheme = previousThemeState.value
    val isLargePrimaryShift =
        theme.id != previousTheme.id && isLargePrimaryShift(previousTheme.primaryColor, theme.primaryColor)

    LaunchedEffect(theme.id) {
        previousThemeState.value = theme
    }

    val backgroundSpec =
        tween<Color>(
            durationMillis =
                if (isLargePrimaryShift) {
                    BrandThemeBackgroundLargeShiftDurationMs
                } else {
                    BrandThemeBackgroundDurationMs
                },
            easing = FastOutSlowInEasing,
        )
    val structureSpec =
        tween<Color>(
            durationMillis = BrandThemeStructureDurationMs,
            easing = FastOutSlowInEasing,
        )
    val accentSpec =
        tween<Color>(
            durationMillis = BrandThemeAccentDurationMs,
            easing = FastOutSlowInEasing,
        )

    val targetAccentTokens = brandAccentTokens(theme)
    val targetVisualTokens = brandThemeVisualTokens(theme, targetAccentTokens)
    val targetColorScheme = theme.colorScheme

    val animatedColorScheme =
        targetColorScheme.copy(
            primary = animateThemeColor(targetColorScheme.primary, accentSpec, "brandPrimary"),
            onPrimary = animateThemeColor(targetColorScheme.onPrimary, structureSpec, "brandOnPrimary"),
            primaryContainer =
                animateThemeColor(
                    targetColorScheme.primaryContainer,
                    backgroundSpec,
                    "brandPrimaryContainer",
                ),
            onPrimaryContainer =
                animateThemeColor(
                    targetColorScheme.onPrimaryContainer,
                    structureSpec,
                    "brandOnPrimaryContainer",
                ),
            secondary = animateThemeColor(targetColorScheme.secondary, accentSpec, "brandSecondary"),
            onSecondary = animateThemeColor(targetColorScheme.onSecondary, structureSpec, "brandOnSecondary"),
            secondaryContainer =
                animateThemeColor(
                    targetColorScheme.secondaryContainer,
                    backgroundSpec,
                    "brandSecondaryContainer",
                ),
            onSecondaryContainer =
                animateThemeColor(
                    targetColorScheme.onSecondaryContainer,
                    structureSpec,
                    "brandOnSecondaryContainer",
                ),
            tertiary = animateThemeColor(targetColorScheme.tertiary, accentSpec, "brandTertiary"),
            onTertiary = animateThemeColor(targetColorScheme.onTertiary, structureSpec, "brandOnTertiary"),
            tertiaryContainer =
                animateThemeColor(
                    targetColorScheme.tertiaryContainer,
                    backgroundSpec,
                    "brandTertiaryContainer",
                ),
            onTertiaryContainer =
                animateThemeColor(
                    targetColorScheme.onTertiaryContainer,
                    structureSpec,
                    "brandOnTertiaryContainer",
                ),
            background = animateThemeColor(targetColorScheme.background, backgroundSpec, "brandBackground"),
            onBackground = animateThemeColor(targetColorScheme.onBackground, structureSpec, "brandOnBackground"),
            surface = animateThemeColor(targetColorScheme.surface, backgroundSpec, "brandSurface"),
            onSurface = animateThemeColor(targetColorScheme.onSurface, structureSpec, "brandOnSurface"),
            surfaceVariant =
                animateThemeColor(
                    targetColorScheme.surfaceVariant,
                    backgroundSpec,
                    "brandSurfaceVariant",
                ),
            onSurfaceVariant =
                animateThemeColor(
                    targetColorScheme.onSurfaceVariant,
                    structureSpec,
                    "brandOnSurfaceVariant",
                ),
            outline = animateThemeColor(targetColorScheme.outline, accentSpec, "brandOutline"),
            outlineVariant =
                animateThemeColor(
                    targetColorScheme.outlineVariant,
                    accentSpec,
                    "brandOutlineVariant",
                ),
        )

    val animatedAccentTokens =
        targetAccentTokens.copy(
            disclosureAccentTint =
                animateThemeColor(
                    targetAccentTokens.disclosureAccentTint,
                    accentSpec,
                    "brandDisclosureAccentTint",
                ),
            actionAccentTint =
                animateThemeColor(
                    targetAccentTokens.actionAccentTint,
                    accentSpec,
                    "brandActionAccentTint",
                ),
            selectionLabelAccentTint =
                animateThemeColor(
                    targetAccentTokens.selectionLabelAccentTint,
                    accentSpec,
                    "brandSelectionLabelAccentTint",
                ),
            selectionBorderAccentTint =
                animateThemeColor(
                    targetAccentTokens.selectionBorderAccentTint,
                    accentSpec,
                    "brandSelectionBorderAccentTint",
                ),
        )

    val animatedVisualTokens =
        targetVisualTokens.copy(
            audioEncodeGlyphColors =
                targetVisualTokens.audioEncodeGlyphColors.copy(
                    primarySplit =
                        animateThemeColor(
                            targetVisualTokens.audioEncodeGlyphColors.primarySplit,
                            accentSpec,
                            "brandGlyphPrimarySplit",
                        ),
                    secondarySplit =
                        animateThemeColor(
                            targetVisualTokens.audioEncodeGlyphColors.secondarySplit,
                            backgroundSpec,
                            "brandGlyphSecondarySplit",
                        ),
                    outline =
                        animateThemeColor(
                            targetVisualTokens.audioEncodeGlyphColors.outline,
                            accentSpec,
                            "brandGlyphOutline",
                        ),
                ),
            dockContainerColor =
                animateThemeColor(
                    targetVisualTokens.dockContainerColor,
                    backgroundSpec,
                    "brandDockContainerColor",
                ),
            modalContainerColor =
                animateThemeColor(
                    targetVisualTokens.modalContainerColor,
                    backgroundSpec,
                    "brandModalContainerColor",
                ),
            modalContentColor =
                animateThemeColor(
                    targetVisualTokens.modalContentColor,
                    structureSpec,
                    "brandModalContentColor",
                ),
            segmentedInactiveContainerColor =
                animateThemeColor(
                    targetVisualTokens.segmentedInactiveContainerColor,
                    backgroundSpec,
                    "brandSegmentedInactiveContainerColor",
                ),
            segmentedInactiveContentColor =
                animateThemeColor(
                    targetVisualTokens.segmentedInactiveContentColor,
                    structureSpec,
                    "brandSegmentedInactiveContentColor",
                ),
            selectionSelectedContainerColor =
                animateThemeColor(
                    targetVisualTokens.selectionSelectedContainerColor,
                    backgroundSpec,
                    "brandSelectionSelectedContainerColor",
                ),
            selectionUnselectedContainerColor =
                animateThemeColor(
                    targetVisualTokens.selectionUnselectedContainerColor,
                    backgroundSpec,
                    "brandSelectionUnselectedContainerColor",
                ),
            groupContainerColor =
                animateThemeColor(
                    targetVisualTokens.groupContainerColor,
                    backgroundSpec,
                    "brandGroupContainerColor",
                ),
            supportSurfaceColor =
                animateThemeColor(
                    targetVisualTokens.supportSurfaceColor,
                    backgroundSpec,
                    "brandSupportSurfaceColor",
                ),
            supportStrongSurfaceColor =
                animateThemeColor(
                    targetVisualTokens.supportStrongSurfaceColor,
                    backgroundSpec,
                    "brandSupportStrongSurfaceColor",
                ),
            inputContainerColor =
                animateThemeColor(
                    targetVisualTokens.inputContainerColor,
                    backgroundSpec,
                    "brandInputContainerColor",
                ),
            inputContentColor =
                animateThemeColor(
                    targetVisualTokens.inputContentColor,
                    structureSpec,
                    "brandInputContentColor",
                ),
            inputFocusedBorderColor =
                animateThemeColor(
                    targetVisualTokens.inputFocusedBorderColor,
                    accentSpec,
                    "brandInputFocusedBorderColor",
                ),
            inputUnfocusedBorderColor =
                animateThemeColor(
                    targetVisualTokens.inputUnfocusedBorderColor,
                    accentSpec,
                    "brandInputUnfocusedBorderColor",
                ),
            actionContainerColor =
                animateThemeColor(
                    targetVisualTokens.actionContainerColor,
                    backgroundSpec,
                    "brandActionContainerColor",
                ),
            actionDisabledContainerColor =
                animateThemeColor(
                    targetVisualTokens.actionDisabledContainerColor,
                    structureSpec,
                    "brandActionDisabledContainerColor",
                ),
            actionContentColor =
                animateThemeColor(
                    targetVisualTokens.actionContentColor,
                    accentSpec,
                    "brandActionContentColor",
                ),
            actionDisabledContentColor =
                animateThemeColor(
                    targetVisualTokens.actionDisabledContentColor,
                    structureSpec,
                    "brandActionDisabledContentColor",
                ),
            miniPlayerLeadingContainerColor =
                animateThemeColor(
                    targetVisualTokens.miniPlayerLeadingContainerColor,
                    backgroundSpec,
                    "brandMiniPlayerLeadingContainerColor",
                ),
            miniPlayerLeadingContentColor =
                animateThemeColor(
                    targetVisualTokens.miniPlayerLeadingContentColor,
                    accentSpec,
                    "brandMiniPlayerLeadingContentColor",
                ),
            annotationChipContainerColor =
                animateThemeColor(
                    targetVisualTokens.annotationChipContainerColor,
                    backgroundSpec,
                    "brandAnnotationChipContainerColor",
                ),
            annotationChipContentColor =
                animateThemeColor(
                    targetVisualTokens.annotationChipContentColor,
                    structureSpec,
                    "brandAnnotationChipContentColor",
                ),
            followTokenContainerColor =
                animateThemeColor(
                    targetVisualTokens.followTokenContainerColor,
                    backgroundSpec,
                    "brandFollowTokenContainerColor",
                ),
            subtleOutlineColor =
                animateThemeColor(
                    targetVisualTokens.subtleOutlineColor,
                    accentSpec,
                    "brandSubtleOutlineColor",
                ),
            timelineInactiveTrackColor =
                animateThemeColor(
                    targetVisualTokens.timelineInactiveTrackColor,
                    backgroundSpec,
                    "brandTimelineInactiveTrackColor",
                ),
            visualizationBaseBackgroundColor =
                animateThemeColor(
                    targetVisualTokens.visualizationBaseBackgroundColor,
                    backgroundSpec,
                    "brandVisualizationBaseBackgroundColor",
                ),
            visualizationInactiveToneColor =
                animateThemeColor(
                    targetVisualTokens.visualizationInactiveToneColor,
                    structureSpec,
                    "brandVisualizationInactiveToneColor",
                ),
        )

    return AnimatedBrandThemeSnapshot(
        colorScheme = animatedColorScheme,
        accentTokens = animatedAccentTokens,
        visualTokens = animatedVisualTokens,
    )
}

@Composable
private fun animateThemeColor(
    targetValue: Color,
    animationSpec: AnimationSpec<Color>,
    label: String,
): Color {
    val animatedColor by
        animateColorAsState(
            targetValue = targetValue,
            animationSpec = animationSpec,
            label = label,
        )
    return animatedColor
}

private fun isLargePrimaryShift(
    from: Color,
    to: Color,
): Boolean {
    val luminanceDelta = kotlin.math.abs(from.luminance() - to.luminance())
    val channelDelta =
        maxOf(
            kotlin.math.abs(from.red - to.red),
            kotlin.math.abs(from.green - to.green),
            kotlin.math.abs(from.blue - to.blue),
        )
    return luminanceDelta >= LargePrimaryShiftLuminanceDelta || channelDelta >= LargePrimaryShiftChannelDelta
}

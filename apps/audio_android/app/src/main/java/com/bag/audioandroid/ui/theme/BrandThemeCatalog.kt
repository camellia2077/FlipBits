package com.bag.audioandroid.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.BrandThemeOption

private val BrandInkLight = Color(0xFF241B18)
private val BrandInkDark = Color(0xFFF1E8E1)

val BrandDualToneThemes: List<BrandThemeOption> =
    listOf(
        // Each group is ordered from the most stable / approachable theme to the strongest one.
        // Neighboring entries are also staggered to avoid stacking near-identical hues together.
        brandTheme(
            id = "mars_relic",
            groupTitleResId = R.string.config_dual_tone_group_sacred_machine,
            titleResId = R.string.brand_theme_mars_relic_title,
            descriptionResId = R.string.brand_theme_mars_relic_description,
            accessibilityLabelResId = R.string.brand_theme_mars_relic_accessibility,
            primaryColor = Color(0xFF9E1B1B),
            secondaryColor = Color(0xFFE8E2D0),
            isDarkTheme = false,
        ),
        brandTheme(
            id = "scarlet_guard",
            groupTitleResId = R.string.config_dual_tone_group_sacred_machine,
            titleResId = R.string.brand_theme_scarlet_guard_title,
            descriptionResId = R.string.brand_theme_scarlet_guard_description,
            accessibilityLabelResId = R.string.brand_theme_scarlet_guard_accessibility,
            primaryColor = Color(0xFF8B0000),
            secondaryColor = Color(0xFFE0E0E0),
            isDarkTheme = false,
        ),
        brandTheme(
            id = "black_crimson_rite",
            groupTitleResId = R.string.config_dual_tone_group_sacred_machine,
            titleResId = R.string.brand_theme_black_crimson_rite_title,
            descriptionResId = R.string.brand_theme_black_crimson_rite_description,
            accessibilityLabelResId = R.string.brand_theme_black_crimson_rite_accessibility,
            primaryColor = Color(0xFF211A1A),
            secondaryColor = Color(0xFFCE1126),
            isDarkTheme = true,
        ),
        brandTheme(
            id = "ancient_alloy",
            groupTitleResId = R.string.config_dual_tone_group_ancient_dynasty,
            titleResId = R.string.brand_theme_ancient_alloy_title,
            descriptionResId = R.string.brand_theme_ancient_alloy_description,
            accessibilityLabelResId = R.string.brand_theme_ancient_alloy_accessibility,
            primaryColor = Color(0xFF1E1B1A),
            secondaryColor = Color(0xFF00FFCC),
            isDarkTheme = true,
        ),
        brandTheme(
            id = "dynasty_revival",
            groupTitleResId = R.string.config_dual_tone_group_ancient_dynasty,
            titleResId = R.string.brand_theme_dynasty_revival_title,
            descriptionResId = R.string.brand_theme_dynasty_revival_description,
            accessibilityLabelResId = R.string.brand_theme_dynasty_revival_accessibility,
            primaryColor = Color(0xFF161A1D),
            secondaryColor = Color(0xFF14FF00),
            isDarkTheme = true,
        ),
        brandTheme(
            id = "sepulcher_cyan",
            groupTitleResId = R.string.config_dual_tone_group_ancient_dynasty,
            titleResId = R.string.brand_theme_sepulcher_cyan_title,
            descriptionResId = R.string.brand_theme_sepulcher_cyan_description,
            accessibilityLabelResId = R.string.brand_theme_sepulcher_cyan_accessibility,
            primaryColor = Color(0xFF181C1F),
            secondaryColor = Color(0xFF00E5B8),
            isDarkTheme = true,
        ),
        brandTheme(
            id = "tomb_sigil",
            groupTitleResId = R.string.config_dual_tone_group_ancient_dynasty,
            titleResId = R.string.brand_theme_tomb_sigil_title,
            descriptionResId = R.string.brand_theme_tomb_sigil_description,
            accessibilityLabelResId = R.string.brand_theme_tomb_sigil_accessibility,
            primaryColor = Color(0xFF1A1A18),
            secondaryColor = Color(0xFF14FF00),
            isDarkTheme = true,
        ),
    )

val DefaultBrandTheme: BrandThemeOption
    get() = BrandDualToneThemes.first()

private fun brandTheme(
    id: String,
    groupTitleResId: Int,
    titleResId: Int,
    descriptionResId: Int,
    accessibilityLabelResId: Int,
    primaryColor: Color,
    secondaryColor: Color,
    isDarkTheme: Boolean,
): BrandThemeOption {
    val background = if (isDarkTheme) primaryColor else secondaryColor
    val surface = if (isDarkTheme) primaryColor.copy(alpha = 0.94f) else secondaryColor.copy(alpha = 0.94f)
    val onBackground = if (isDarkTheme) BrandInkDark else BrandInkLight
    val primary = if (isDarkTheme) secondaryColor else primaryColor
    val onPrimary = if (isDarkTheme) primaryColor else secondaryColor
    val primaryContainer = blend(background, secondaryColor, if (isDarkTheme) 0.22f else 0.14f)
    val secondaryContainer = blend(background, primary, if (isDarkTheme) 0.30f else 0.18f)
    val surfaceVariant = blend(background, secondaryColor, if (isDarkTheme) 0.16f else 0.10f)
    val outline = blend(primary, onBackground, if (isDarkTheme) 0.48f else 0.58f)
    val outlineVariant = blend(surfaceVariant, onBackground, if (isDarkTheme) 0.22f else 0.18f)

    val colorScheme =
        if (isDarkTheme) {
            darkColorScheme(
                primary = primary,
                onPrimary = onPrimary,
                primaryContainer = primaryContainer,
                onPrimaryContainer = BrandInkDark,
                secondary = secondaryColor,
                onSecondary = primaryColor,
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = BrandInkDark,
                tertiary = secondaryColor,
                onTertiary = primaryColor,
                tertiaryContainer = secondaryContainer,
                onTertiaryContainer = BrandInkDark,
                background = background,
                onBackground = onBackground,
                surface = surface,
                onSurface = onBackground,
                surfaceVariant = surfaceVariant,
                onSurfaceVariant = blend(onBackground, secondaryColor, 0.22f),
                outline = outline,
                outlineVariant = outlineVariant,
            )
        } else {
            lightColorScheme(
                primary = primary,
                onPrimary = onPrimary,
                primaryContainer = primaryContainer,
                onPrimaryContainer = BrandInkLight,
                secondary = blend(primary, background, 0.20f),
                onSecondary = secondaryColor,
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = BrandInkLight,
                tertiary = blend(primary, background, 0.34f),
                onTertiary = secondaryColor,
                tertiaryContainer = blend(secondaryColor, primaryColor, 0.08f),
                onTertiaryContainer = BrandInkLight,
                background = background,
                onBackground = onBackground,
                surface = surface,
                onSurface = onBackground,
                surfaceVariant = surfaceVariant,
                onSurfaceVariant = blend(onBackground, primaryColor, 0.36f),
                outline = outline,
                outlineVariant = outlineVariant,
            )
        }

    return BrandThemeOption(
        id = id,
        groupTitleResId = groupTitleResId,
        titleResId = titleResId,
        descriptionResId = descriptionResId,
        accessibilityLabelResId = accessibilityLabelResId,
        isDarkTheme = isDarkTheme,
        primaryColor = primaryColor,
        secondaryColor = secondaryColor,
        colorScheme = colorScheme,
    )
}

private fun blend(
    from: Color,
    to: Color,
    ratio: Float,
): Color = lerp(from, to, ratio.coerceIn(0f, 1f))

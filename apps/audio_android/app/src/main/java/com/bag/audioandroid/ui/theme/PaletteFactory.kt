package com.bag.audioandroid.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.CustomBrandThemeSettings
import com.bag.audioandroid.ui.model.PaletteFamily
import com.bag.audioandroid.ui.model.PaletteOption

const val CustomMaterialPaletteId = "custom_material"
private const val CustomMaterialPaletteIdSeparator = "::"

private data class MaterialSingleColorSeed(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
)

internal fun vividPalette(
    id: String,
    family: PaletteFamily,
    titleResId: Int,
    primary: Color,
    primaryContainer: Color,
    secondary: Color,
    tertiary: Color,
    background: Color,
    dark: DarkPaletteSeed,
    onPrimaryContainer: Color = Color(0xFF201A1B),
): PaletteOption {
    val lightSecondaryContainer = lerp(primaryContainer, secondary, 0.14f)
    val lightTertiaryContainer = lerp(primaryContainer, tertiary, 0.14f)
    val darkPrimaryContainer = lerp(dark.primary, dark.background, 0.68f)
    val darkSecondaryContainer = lerp(dark.secondary, dark.background, 0.72f)
    val darkTertiaryContainer = lerp(dark.tertiary, dark.background, 0.72f)
    val darkSurfaceVariant = lerp(dark.primary, dark.surface, 0.82f)
    val darkOutline = lerp(dark.surface, Color.White, 0.44f)

    return PaletteOption(
        id = id,
        family = family,
        titleResId = titleResId,
        previewColor = primary,
        lightScheme =
            lightColorScheme(
                primary = primary,
                onPrimary = Color.White,
                primaryContainer = primaryContainer,
                onPrimaryContainer = onPrimaryContainer,
                secondary = secondary,
                onSecondary = Color.White,
                secondaryContainer = lightSecondaryContainer,
                onSecondaryContainer = onPrimaryContainer,
                tertiary = tertiary,
                onTertiary = Color.White,
                tertiaryContainer = lightTertiaryContainer,
                onTertiaryContainer = onPrimaryContainer,
                background = background,
                surface = background,
            ),
        darkScheme =
            darkColorScheme(
                primary = dark.primary,
                onPrimary = Color(0xFF101418),
                primaryContainer = darkPrimaryContainer,
                onPrimaryContainer = lerp(primaryContainer, Color.White, 0.06f),
                secondary = dark.secondary,
                onSecondary = Color(0xFF101418),
                secondaryContainer = darkSecondaryContainer,
                onSecondaryContainer = Color(0xFFF2EAF0),
                tertiary = dark.tertiary,
                onTertiary = Color(0xFF101418),
                tertiaryContainer = darkTertiaryContainer,
                onTertiaryContainer = Color(0xFFF5EBEF),
                background = dark.background,
                onBackground = Color(0xFFEAE2E7),
                surface = dark.surface,
                onSurface = Color(0xFFEAE2E7),
                surfaceVariant = darkSurfaceVariant,
                onSurfaceVariant = Color(0xFFD3C5CC),
                outline = darkOutline,
            ),
    )
}

internal data class DarkPaletteSeed(
    val background: Color,
    val surface: Color,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
)

internal fun darkSeed(
    background: Color,
    surface: Color,
    primary: Color,
    secondary: Color,
    tertiary: Color,
): DarkPaletteSeed =
    DarkPaletteSeed(
        background = background,
        surface = surface,
        primary = primary,
        secondary = secondary,
        tertiary = tertiary,
    )

fun customMaterialPalette(settings: CustomBrandThemeSettings): PaletteOption {
    val primary = brandThemeColorOrNull(settings.primaryHex) ?: Color(0xFF6750A4)
    val seed = materialSingleColorSeed(primary)
    val secondary = seed.secondary
    val tertiary = seed.tertiary
    val lightBackground =
        if (primary.luminance() < 0.45f) {
            lerp(primary, Color.White, 0.92f)
        } else {
            lerp(primary, Color.White, 0.84f)
        }
    val lightPrimaryContainer = lerp(primary, Color.White, 0.68f)
    val darkBackground = lerp(primary, Color.Black, 0.84f)
    val darkSurface = lerp(darkBackground, secondary, 0.10f)
    val darkPrimary =
        if (primary.luminance() < 0.50f) {
            lerp(primary, Color.White, 0.34f)
        } else {
            lerp(primary, Color.Black, 0.16f)
        }
    val darkSecondary =
        if (secondary.luminance() < 0.50f) {
            lerp(secondary, Color.White, 0.28f)
        } else {
            lerp(secondary, Color.Black, 0.10f)
        }
    val darkTertiary =
        if (tertiary.luminance() < 0.50f) {
            lerp(tertiary, Color.White, 0.26f)
        } else {
            lerp(tertiary, Color.Black, 0.10f)
        }

    return vividPalette(
        id = customMaterialPaletteId(settings.presetId),
        family = PaletteFamily.Custom,
        titleResId = R.string.palette_family_custom,
        primary = primary,
        primaryContainer = lightPrimaryContainer,
        secondary = secondary,
        tertiary = tertiary,
        background = lightBackground,
        dark =
            darkSeed(
                background = darkBackground,
                surface = darkSurface,
                primary = darkPrimary,
                secondary = darkSecondary,
                tertiary = darkTertiary,
            ),
    )
}

fun customMaterialPaletteId(presetId: String): String =
    if (presetId == DefaultCustomMaterialPaletteSettings.presetId) {
        CustomMaterialPaletteId
    } else {
        "$CustomMaterialPaletteId$CustomMaterialPaletteIdSeparator$presetId"
    }

fun isCustomMaterialPaletteId(paletteId: String): Boolean =
    paletteId == CustomMaterialPaletteId || paletteId.startsWith("$CustomMaterialPaletteId$CustomMaterialPaletteIdSeparator")

fun normalizeCustomMaterialThemeSettings(settings: CustomBrandThemeSettings): CustomBrandThemeSettings {
    val normalizedPrimary = normalizeBrandThemeHex(settings.primaryHex) ?: DefaultCustomMaterialPaletteSettings.primaryHex
    val seed = materialSingleColorSeed(parseMaterialHexColor(normalizedPrimary))
    return settings.copy(
        presetId = settings.presetId.ifBlank { DefaultCustomMaterialPaletteSettings.presetId },
        displayName = settings.displayName.ifBlank { DefaultCustomMaterialPaletteSettings.displayName },
        primaryHex = normalizedPrimary,
        secondaryHex = materialColorToHexString(seed.secondary),
        outlineHexOrNull = materialColorToHexString(seed.tertiary),
    )
}

private fun materialSingleColorSeed(primary: Color): MaterialSingleColorSeed {
    val secondary =
        if (primary.luminance() < 0.5f) {
            lerp(primary, Color.White, 0.22f)
        } else {
            lerp(primary, Color.Black, 0.18f)
        }
    val tertiary =
        if (primary.luminance() < 0.5f) {
            lerp(primary, Color.White, 0.36f)
        } else {
            lerp(primary, Color.Black, 0.30f)
        }
    return MaterialSingleColorSeed(
        primary = primary,
        secondary = secondary,
        tertiary = tertiary,
    )
}

private fun parseMaterialHexColor(hex: String): Color {
    val rgb = hex.removePrefix("#").toLong(radix = 16)
    return Color(0xFF000000 or rgb)
}

private fun materialColorToHexString(color: Color): String {
    val red = (color.red * 255f).toInt().coerceIn(0, 255)
    val green = (color.green * 255f).toInt().coerceIn(0, 255)
    val blue = (color.blue * 255f).toInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", red, green, blue)
}

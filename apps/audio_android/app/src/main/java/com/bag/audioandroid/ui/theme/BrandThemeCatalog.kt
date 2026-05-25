package com.bag.audioandroid.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.BrandThemeOption
import com.bag.audioandroid.ui.model.CustomBrandThemeSettings
import com.bag.audioandroid.ui.model.DefaultCustomBrandThemePresetId
import com.bag.audioandroid.ui.model.SampleFlavor

private val BrandInkLight = Color(0xFF241B18)
private val BrandInkDark = Color(0xFFF1E8E1)

const val CustomBrandThemeId = "custom_dual_tone"
private const val CustomBrandThemeIdSeparator = "::"

val BrandDualToneThemes: List<BrandThemeOption> =
    listOf(
        // Each group is ordered from the most stable / approachable theme to the strongest one.
        // Neighboring entries are also staggered to avoid stacking near-identical hues together.
        buildBrandThemeOption(
            id = "mars_relic",
            groupTitleResId = R.string.config_dual_tone_group_sacred_machine,
            titleResId = R.string.brand_theme_mars_relic_title,
            descriptionResId = R.string.brand_theme_mars_relic_description,
            accessibilityLabelResId = R.string.brand_theme_mars_relic_accessibility,
            sampleFlavor = SampleFlavor.SacredMachine,
            primaryColor = Color(0xFFE8E2D0),
            secondaryColor = Color(0xFF9E1B1B),
            outlineColor = Color(0xFFC5A059),
        ),
        buildBrandThemeOption(
            id = "scarlet_guard",
            groupTitleResId = R.string.config_dual_tone_group_sacred_machine,
            titleResId = R.string.brand_theme_scarlet_guard_title,
            descriptionResId = R.string.brand_theme_scarlet_guard_description,
            accessibilityLabelResId = R.string.brand_theme_scarlet_guard_accessibility,
            sampleFlavor = SampleFlavor.SacredMachine,
            primaryColor = Color(0xFFE0E0E0),
            secondaryColor = Color(0xFFD64011),
            outlineColor = Color(0xFF3B444B),
        ),
        buildBrandThemeOption(
            id = "black_crimson_rite",
            groupTitleResId = R.string.config_dual_tone_group_sacred_machine,
            titleResId = R.string.brand_theme_black_crimson_rite_title,
            descriptionResId = R.string.brand_theme_black_crimson_rite_description,
            accessibilityLabelResId = R.string.brand_theme_black_crimson_rite_accessibility,
            sampleFlavor = SampleFlavor.SacredMachine,
            primaryColor = Color(0xFF4A2B2F),
            secondaryColor = Color(0xFFDC143C),
            outlineColor = Color(0xFFB2C9AB),
        ),
        buildBrandThemeOption(
            id = "blood_soaked_ivory",
            groupTitleResId = R.string.config_dual_tone_group_scarlet_carnage,
            titleResId = R.string.brand_theme_scarlet_carnage_title,
            descriptionResId = R.string.brand_theme_scarlet_carnage_description,
            accessibilityLabelResId = R.string.brand_theme_scarlet_carnage_accessibility,
            sampleFlavor = SampleFlavor.ScarletCarnage,
            primaryColor = Color(0xFF520606),
            secondaryColor = Color(0xFF8A5A00),
            outlineColor = Color(0xFFD4C4A8),
        ),
        buildBrandThemeOption(
            id = "brass_forge",
            groupTitleResId = R.string.config_dual_tone_group_scarlet_carnage,
            titleResId = R.string.brand_theme_brass_forge_title,
            descriptionResId = R.string.brand_theme_brass_forge_description,
            accessibilityLabelResId = R.string.brand_theme_brass_forge_accessibility,
            sampleFlavor = SampleFlavor.ScarletCarnage,
            primaryColor = Color(0xFF2B0505),
            secondaryColor = Color(0xFFCD7F32),
            outlineColor = Color(0xFFFF4500),
        ),
        buildBrandThemeOption(
            id = "fires_of_fate",
            groupTitleResId = R.string.config_dual_tone_group_labyrinth_of_mutability,
            titleResId = R.string.brand_theme_fires_of_fate_title,
            descriptionResId = R.string.brand_theme_fires_of_fate_description,
            accessibilityLabelResId = R.string.brand_theme_fires_of_fate_accessibility,
            sampleFlavor = SampleFlavor.LabyrinthOfMutability,
            primaryColor = Color(0xFF0C3C7E),
            secondaryColor = Color(0xFFFF007F),
            outlineColor = Color(0xFFCCFF00),
        ),
        buildBrandThemeOption(
            id = "arcane_abyss",
            groupTitleResId = R.string.config_dual_tone_group_labyrinth_of_mutability,
            titleResId = R.string.brand_theme_labyrinth_of_mutability_title,
            descriptionResId = R.string.brand_theme_labyrinth_of_mutability_description,
            accessibilityLabelResId = R.string.brand_theme_labyrinth_of_mutability_accessibility,
            sampleFlavor = SampleFlavor.LabyrinthOfMutability,
            primaryColor = Color(0xFF002133),
            secondaryColor = Color(0xFF00E5FF),
            outlineColor = Color(0xFFC399FF),
        ),
        buildBrandThemeOption(
            id = "ecstatic_rapture",
            groupTitleResId = R.string.config_dual_tone_group_exquisite_fall,
            titleResId = R.string.brand_theme_ecstatic_rapture_title,
            descriptionResId = R.string.brand_theme_ecstatic_rapture_description,
            accessibilityLabelResId = R.string.brand_theme_ecstatic_rapture_accessibility,
            sampleFlavor = SampleFlavor.ExquisiteFall,
            primaryColor = Color(0xFF661B64),
            secondaryColor = Color(0xFFFFB7D5),
            outlineColor = Color(0xFFFFD700),
        ),
        buildBrandThemeOption(
            id = "velvet_nightmare",
            groupTitleResId = R.string.config_dual_tone_group_exquisite_fall,
            titleResId = R.string.brand_theme_exquisite_fall_title,
            descriptionResId = R.string.brand_theme_exquisite_fall_description,
            accessibilityLabelResId = R.string.brand_theme_exquisite_fall_accessibility,
            sampleFlavor = SampleFlavor.ExquisiteFall,
            primaryColor = Color(0xFF26053A),
            secondaryColor = Color(0xFFFF3399),
            outlineColor = Color(0xFFECB1AC),
        ),
        buildBrandThemeOption(
            id = "toxic_effluence",
            groupTitleResId = R.string.config_dual_tone_group_immortal_rot,
            titleResId = R.string.brand_theme_immortal_rot_title,
            descriptionResId = R.string.brand_theme_immortal_rot_description,
            accessibilityLabelResId = R.string.brand_theme_immortal_rot_accessibility,
            sampleFlavor = SampleFlavor.ImmortalRot,
            primaryColor = Color(0xFF35522C),
            secondaryColor = Color(0xFFE4D00A),
            outlineColor = Color(0xFF8CFF1A),
        ),
        buildBrandThemeOption(
            id = "plague_mire",
            groupTitleResId = R.string.config_dual_tone_group_immortal_rot,
            titleResId = R.string.brand_theme_plague_mire_title,
            descriptionResId = R.string.brand_theme_plague_mire_description,
            accessibilityLabelResId = R.string.brand_theme_plague_mire_accessibility,
            sampleFlavor = SampleFlavor.ImmortalRot,
            primaryColor = Color(0xFF1E2A18),
            secondaryColor = Color(0xFF9ACD32),
            outlineColor = Color(0xFFFF8C00),
        ),
        // The dark dual-tone dynasty set is separated by material character instead of
        // tiny near-black shifts: alloy leans warm metallic, revival leans jade-cold,
        // sepulcher leans graphite-cyan, and tomb sigil leans earthen tomb black.
        buildBrandThemeOption(
            id = "dynasty_revival",
            groupTitleResId = R.string.config_dual_tone_group_ancient_dynasty,
            titleResId = R.string.brand_theme_dynasty_revival_title,
            descriptionResId = R.string.brand_theme_dynasty_revival_description,
            accessibilityLabelResId = R.string.brand_theme_dynasty_revival_accessibility,
            sampleFlavor = SampleFlavor.AncientDynasty,
            // Separate the two green dynasty themes on purpose: dynasty revival stays in
            // a colder jade/bronze lane, while tomb sigil keeps the harsher tomb glow.
            primaryColor = Color(0xFF31443E),
            secondaryColor = Color(0xFF00D68F),
            outlineColor = Color(0xFFA0B3AC),
        ),
        buildBrandThemeOption(
            id = "sepulcher_cyan",
            groupTitleResId = R.string.config_dual_tone_group_ancient_dynasty,
            titleResId = R.string.brand_theme_sepulcher_cyan_title,
            descriptionResId = R.string.brand_theme_sepulcher_cyan_description,
            accessibilityLabelResId = R.string.brand_theme_sepulcher_cyan_accessibility,
            sampleFlavor = SampleFlavor.AncientDynasty,
            primaryColor = Color(0xFF2B404A),
            secondaryColor = Color(0xFF00E5B8),
            outlineColor = Color(0xFF81A1B1),
        ),
        buildBrandThemeOption(
            id = "ancient_alloy",
            groupTitleResId = R.string.config_dual_tone_group_ancient_dynasty,
            titleResId = R.string.brand_theme_ancient_alloy_title,
            descriptionResId = R.string.brand_theme_ancient_alloy_description,
            accessibilityLabelResId = R.string.brand_theme_ancient_alloy_accessibility,
            sampleFlavor = SampleFlavor.AncientDynasty,
            primaryColor = Color(0xFF423B33),
            secondaryColor = Color(0xFF00FFCC),
            outlineColor = Color(0xFFB59367),
        ),
        buildBrandThemeOption(
            id = "tomb_sigil",
            groupTitleResId = R.string.config_dual_tone_group_ancient_dynasty,
            titleResId = R.string.brand_theme_tomb_sigil_title,
            descriptionResId = R.string.brand_theme_tomb_sigil_description,
            accessibilityLabelResId = R.string.brand_theme_tomb_sigil_accessibility,
            sampleFlavor = SampleFlavor.AncientDynasty,
            primaryColor = Color(0xFF4D3C2B),
            secondaryColor = Color(0xFFA6FF00),
            outlineColor = Color(0xFFC7B39B),
        ),
    )

val DefaultBrandTheme: BrandThemeOption
    get() = BrandDualToneThemes.first()

fun customBrandTheme(settings: CustomBrandThemeSettings): BrandThemeOption {
    val normalizedPrimary = normalizeBrandThemeHex(settings.primaryHex) ?: DefaultCustomPrimaryHex
    val normalizedSecondary = normalizeBrandThemeHex(settings.secondaryHex) ?: DefaultCustomSecondaryHex
    val normalizedOutline = normalizeBrandThemeHexOrNull(settings.outlineHexOrNull)
    val primaryColor = parseBrandThemeColor(normalizedPrimary)
    val secondaryColor = parseBrandThemeColor(normalizedSecondary)
    return buildBrandThemeOption(
        id = customBrandThemeOptionId(settings.presetId),
        groupTitleResId = R.string.config_dual_tone_group_custom,
        titleResId = R.string.brand_theme_custom_title,
        descriptionResId = R.string.brand_theme_custom_description,
        accessibilityLabelResId = R.string.brand_theme_custom_accessibility,
        titleOverride = settings.displayName,
        sampleFlavor = SampleFlavor.SacredMachine,
        primaryColor = primaryColor,
        secondaryColor = secondaryColor,
        outlineColor = normalizedOutline?.let(::parseBrandThemeColor) ?: secondaryColor,
    )
}

fun normalizeBrandThemeHex(value: String?): String? {
    val trimmed = value?.trim()?.uppercase().orEmpty()
    val normalized =
        if (trimmed.startsWith("#")) {
            trimmed
        } else {
            "#$trimmed"
        }
    if (!BrandThemeHexRegex.matches(normalized)) {
        return null
    }
    return normalized
}

fun normalizeBrandThemeHexOrNull(value: String?): String? {
    val trimmed = value?.trim().orEmpty()
    if (trimmed.isBlank()) {
        return null
    }
    return normalizeBrandThemeHex(trimmed)
}

fun brandThemeColorOrNull(value: String?): Color? = normalizeBrandThemeHexOrNull(value)?.let(::parseBrandThemeColor)

fun customBrandThemeOptionId(presetId: String): String =
    if (presetId == DefaultCustomBrandThemePresetId) {
        CustomBrandThemeId
    } else {
        "$CustomBrandThemeId$CustomBrandThemeIdSeparator$presetId"
    }

fun isCustomBrandThemeOptionId(optionId: String): Boolean =
    optionId == CustomBrandThemeId || optionId.startsWith("$CustomBrandThemeId$CustomBrandThemeIdSeparator")

private fun parseBrandThemeColor(hex: String): Color {
    val normalized = normalizeBrandThemeHex(hex) ?: error("Invalid brand theme color: $hex")
    val rgb = normalized.drop(1).toLong(radix = 16)
    return Color(0xFF000000 or rgb)
}

fun buildBrandThemeOption(
    id: String,
    groupTitleResId: Int,
    titleResId: Int,
    descriptionResId: Int,
    accessibilityLabelResId: Int,
    titleOverride: String? = null,
    descriptionOverride: String? = null,
    accessibilityLabelOverride: String? = null,
    sampleFlavor: SampleFlavor,
    primaryColor: Color,
    secondaryColor: Color,
    outlineColor: Color = secondaryColor,
): BrandThemeOption {
    val isDarkTheme = primaryColor.luminance() < DarkThemeLuminanceThreshold
    // Call sites pass colors by visual responsibility, not by Material slot name:
    // primaryColor is the dominant page/surface color, and secondaryColor is the
    // strong action/selection color. Do not swap them based on light/dark mode.
    val background = primaryColor
    // Keep surface fully opaque. Floating UI such as the mini-player relies on
    // MaterialTheme.colorScheme.surface for readable text; adding alpha here makes
    // docked cards look translucent even when the component requests a solid color.
    val surface = primaryColor
    val onBackground = if (isDarkTheme) BrandInkDark else BrandInkLight
    val primary = secondaryColor
    val onPrimary = primaryColor
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
                onSecondary = primaryColor,
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = BrandInkLight,
                tertiary = blend(primary, background, 0.34f),
                onTertiary = primaryColor,
                tertiaryContainer = blend(primaryColor, secondaryColor, 0.08f),
                onTertiaryContainer = BrandInkLight,
                background = background,
                onBackground = onBackground,
                surface = surface,
                onSurface = onBackground,
                surfaceVariant = surfaceVariant,
                onSurfaceVariant = blend(onBackground, secondaryColor, 0.36f),
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
        titleOverride = titleOverride,
        descriptionOverride = descriptionOverride,
        accessibilityLabelOverride = accessibilityLabelOverride,
        sampleFlavor = sampleFlavor,
        primaryColor = primaryColor,
        secondaryColor = secondaryColor,
        outlineColor = outlineColor,
        colorScheme = colorScheme,
    )
}

private fun blend(
    from: Color,
    to: Color,
    ratio: Float,
): Color = lerp(from, to, ratio.coerceIn(0f, 1f))

private val BrandThemeHexRegex = Regex("^#[0-9A-F]{6}$")

private const val DefaultCustomPrimaryHex = "#E8E2D0"
private const val DefaultCustomSecondaryHex = "#9E1B1B"
private const val DarkThemeLuminanceThreshold = 0.5f

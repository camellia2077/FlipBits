package com.bag.audioandroid.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.CustomFactionThemeSettings
import com.bag.audioandroid.ui.model.DefaultCustomFactionThemePresetId
import com.bag.audioandroid.ui.model.FactionThemeOption
import com.bag.audioandroid.ui.model.SampleFlavor

private val FactionInkLight = Color(0xFF241B18)
private val FactionInkDark = Color(0xFFF1E8E1)
private val FactionThemeHexRegex = Regex("^#[0-9A-F]{6}$")
private val SecondaryTextFallbackBlendSteps =
    listOf(0.10f, 0.18f, 0.26f, 0.34f, 0.42f, 0.50f, 0.62f, 0.74f, 0.86f, 1f)
private const val SecondaryTextContrastThreshold = 4.5f
private const val DefaultCustomPrimaryHex = "#E8E2D0"
private const val DefaultCustomSecondaryHex = "#9E1B1B"
private const val DarkThemeLuminanceThreshold = 0.5f

const val CustomFactionThemeId = "custom_faction_theme"
private const val CustomFactionThemeIdSeparator = "::"

val FactionThemes: List<FactionThemeOption> =
    listOf(
        // Each group is ordered from the most stable / approachable theme to the strongest one.
        // Neighboring entries are also staggered to avoid stacking near-identical hues together.
        buildFactionThemeOption(
            id = "mars_relic",
            groupTitleResId = R.string.config_faction_theme_group_sacred_machine,
            titleResId = R.string.faction_theme_mars_relic_title,
            descriptionResId = R.string.faction_theme_mars_relic_description,
            accessibilityLabelResId = R.string.faction_theme_mars_relic_accessibility,
            sampleFlavor = SampleFlavor.SacredMachine,
            primaryColor = Color(0xFFE8E2D0),
            secondaryColor = Color(0xFF9E1B1B),
            outlineColor = Color(0xFFC78C25),
        ),
        buildFactionThemeOption(
            id = "scarlet_guard",
            groupTitleResId = R.string.config_faction_theme_group_sacred_machine,
            titleResId = R.string.faction_theme_scarlet_guard_title,
            descriptionResId = R.string.faction_theme_scarlet_guard_description,
            accessibilityLabelResId = R.string.faction_theme_scarlet_guard_accessibility,
            sampleFlavor = SampleFlavor.SacredMachine,
            primaryColor = Color(0xFFE0E0E0),
            secondaryColor = Color(0xFFD64011),
            outlineColor = Color(0xFF3B444B),
        ),
        buildFactionThemeOption(
            id = "black_crimson_rite",
            groupTitleResId = R.string.config_faction_theme_group_sacred_machine,
            titleResId = R.string.faction_theme_black_crimson_rite_title,
            descriptionResId = R.string.faction_theme_black_crimson_rite_description,
            accessibilityLabelResId = R.string.faction_theme_black_crimson_rite_accessibility,
            sampleFlavor = SampleFlavor.SacredMachine,
            primaryColor = Color(0xFF1A1A1A),
            secondaryColor = Color(0xFFD52112),
            outlineColor = Color(0xFF00FFFF),
        ),
        buildFactionThemeOption(
            id = "xeno_code",
            groupTitleResId = R.string.config_faction_theme_group_sacred_machine,
            titleResId = R.string.faction_theme_xeno_code_title,
            descriptionResId = R.string.faction_theme_xeno_code_description,
            accessibilityLabelResId = R.string.faction_theme_xeno_code_accessibility,
            sampleFlavor = SampleFlavor.SacredMachine,
            primaryColor = Color(0xFF1B241D),
            secondaryColor = Color(0xFF00FF7F),
            outlineColor = Color(0xFFFFFFFF),
        ),
        buildFactionThemeOption(
            id = "blood_soaked_ivory",
            groupTitleResId = R.string.config_faction_theme_group_scarlet_carnage,
            titleResId = R.string.faction_theme_scarlet_carnage_title,
            descriptionResId = R.string.faction_theme_scarlet_carnage_description,
            accessibilityLabelResId = R.string.faction_theme_scarlet_carnage_accessibility,
            sampleFlavor = SampleFlavor.ScarletCarnage,
            primaryColor = Color(0xFF520606),
            secondaryColor = Color(0xFF8A5A00),
            outlineColor = Color(0xFFD4C4A8),
        ),
        buildFactionThemeOption(
            id = "brass_forge",
            groupTitleResId = R.string.config_faction_theme_group_scarlet_carnage,
            titleResId = R.string.faction_theme_brass_forge_title,
            descriptionResId = R.string.faction_theme_brass_forge_description,
            accessibilityLabelResId = R.string.faction_theme_brass_forge_accessibility,
            sampleFlavor = SampleFlavor.ScarletCarnage,
            primaryColor = Color(0xFF2B0505),
            secondaryColor = Color(0xFFCD7F32),
            outlineColor = Color(0xFFFF4500),
        ),
        buildFactionThemeOption(
            id = "fires_of_fate",
            groupTitleResId = R.string.config_faction_theme_group_labyrinth_of_mutability,
            titleResId = R.string.faction_theme_fires_of_fate_title,
            descriptionResId = R.string.faction_theme_fires_of_fate_description,
            accessibilityLabelResId = R.string.faction_theme_fires_of_fate_accessibility,
            sampleFlavor = SampleFlavor.LabyrinthOfMutability,
            primaryColor = Color(0xFF0C3C7E),
            secondaryColor = Color(0xFFFF007F),
            outlineColor = Color(0xFFCCFF00),
        ),
        buildFactionThemeOption(
            id = "arcane_abyss",
            groupTitleResId = R.string.config_faction_theme_group_labyrinth_of_mutability,
            titleResId = R.string.faction_theme_labyrinth_of_mutability_title,
            descriptionResId = R.string.faction_theme_labyrinth_of_mutability_description,
            accessibilityLabelResId = R.string.faction_theme_labyrinth_of_mutability_accessibility,
            sampleFlavor = SampleFlavor.LabyrinthOfMutability,
            primaryColor = Color(0xFF002133),
            secondaryColor = Color(0xFF00E5FF),
            outlineColor = Color(0xFFC399FF),
        ),
        buildFactionThemeOption(
            id = "ecstatic_rapture",
            groupTitleResId = R.string.config_faction_theme_group_exquisite_fall,
            titleResId = R.string.faction_theme_ecstatic_rapture_title,
            descriptionResId = R.string.faction_theme_ecstatic_rapture_description,
            accessibilityLabelResId = R.string.faction_theme_ecstatic_rapture_accessibility,
            sampleFlavor = SampleFlavor.ExquisiteFall,
            primaryColor = Color(0xFF661B64),
            secondaryColor = Color(0xFFFFB7D5),
            outlineColor = Color(0xFFFFD700),
        ),
        buildFactionThemeOption(
            id = "velvet_nightmare",
            groupTitleResId = R.string.config_faction_theme_group_exquisite_fall,
            titleResId = R.string.faction_theme_exquisite_fall_title,
            descriptionResId = R.string.faction_theme_exquisite_fall_description,
            accessibilityLabelResId = R.string.faction_theme_exquisite_fall_accessibility,
            sampleFlavor = SampleFlavor.ExquisiteFall,
            primaryColor = Color(0xFF26053A),
            secondaryColor = Color(0xFFFF3399),
            outlineColor = Color(0xFFECB1AC),
        ),
        buildFactionThemeOption(
            id = "toxic_effluence",
            groupTitleResId = R.string.config_faction_theme_group_immortal_rot,
            titleResId = R.string.faction_theme_immortal_rot_title,
            descriptionResId = R.string.faction_theme_immortal_rot_description,
            accessibilityLabelResId = R.string.faction_theme_immortal_rot_accessibility,
            sampleFlavor = SampleFlavor.ImmortalRot,
            primaryColor = Color(0xFF35522C),
            secondaryColor = Color(0xFFE4D00A),
            outlineColor = Color(0xFF8CFF1A),
        ),
        buildFactionThemeOption(
            id = "plague_mire",
            groupTitleResId = R.string.config_faction_theme_group_immortal_rot,
            titleResId = R.string.faction_theme_plague_mire_title,
            descriptionResId = R.string.faction_theme_plague_mire_description,
            accessibilityLabelResId = R.string.faction_theme_plague_mire_accessibility,
            sampleFlavor = SampleFlavor.ImmortalRot,
            primaryColor = Color(0xFF1E2A18),
            secondaryColor = Color(0xFF9ACD32),
            outlineColor = Color(0xFFFF8C00),
        ),
        // The dynasty set stays ordered from steadier authority to harsher contrast.
        buildFactionThemeOption(
            id = "dynasty_revival",
            groupTitleResId = R.string.config_faction_theme_group_ancient_dynasty,
            titleResId = R.string.faction_theme_dynasty_revival_title,
            descriptionResId = R.string.faction_theme_dynasty_revival_description,
            accessibilityLabelResId = R.string.faction_theme_dynasty_revival_accessibility,
            sampleFlavor = SampleFlavor.AncientDynasty,
            primaryColor = Color(0xFF31443E),
            secondaryColor = Color(0xFF00FF7F),
            outlineColor = Color(0xFFA0B3AC),
        ),
        buildFactionThemeOption(
            id = "sepulcher_cyan",
            groupTitleResId = R.string.config_faction_theme_group_ancient_dynasty,
            titleResId = R.string.faction_theme_sepulcher_cyan_title,
            descriptionResId = R.string.faction_theme_sepulcher_cyan_description,
            accessibilityLabelResId = R.string.faction_theme_sepulcher_cyan_accessibility,
            sampleFlavor = SampleFlavor.AncientDynasty,
            primaryColor = Color(0xFFAC8073),
            secondaryColor = Color(0xFF00FF7F),
            outlineColor = Color(0xFFEAE6DF),
        ),
        buildFactionThemeOption(
            id = "tomb_sigil",
            groupTitleResId = R.string.config_faction_theme_group_ancient_dynasty,
            titleResId = R.string.faction_theme_tomb_sigil_title,
            descriptionResId = R.string.faction_theme_tomb_sigil_description,
            accessibilityLabelResId = R.string.faction_theme_tomb_sigil_accessibility,
            sampleFlavor = SampleFlavor.AncientDynasty,
            primaryColor = Color(0xFFBB7F31),
            secondaryColor = Color(0xFF00C9C7),
            outlineColor = Color(0xFF9AA4A8),
        ),
        buildFactionThemeOption(
            id = "ancient_alloy",
            groupTitleResId = R.string.config_faction_theme_group_ancient_dynasty,
            titleResId = R.string.faction_theme_ancient_alloy_title,
            descriptionResId = R.string.faction_theme_ancient_alloy_description,
            accessibilityLabelResId = R.string.faction_theme_ancient_alloy_accessibility,
            sampleFlavor = SampleFlavor.AncientDynasty,
            primaryColor = Color(0xFF60742F),
            secondaryColor = Color(0xFFFF8C00),
            outlineColor = Color(0xFFA1A29D),
        ),
        buildFactionThemeOption(
            id = "void_fluctuation",
            groupTitleResId = R.string.config_faction_theme_group_ancient_dynasty,
            titleResId = R.string.faction_theme_void_fluctuation_title,
            descriptionResId = R.string.faction_theme_void_fluctuation_description,
            accessibilityLabelResId = R.string.faction_theme_void_fluctuation_accessibility,
            sampleFlavor = SampleFlavor.AncientDynasty,
            primaryColor = Color(0xFF2E465A),
            secondaryColor = Color(0xFF00E5FF),
            outlineColor = Color(0xFF56738A),
        ),
        buildFactionThemeOption(
            id = "crimson_decree",
            groupTitleResId = R.string.config_faction_theme_group_ancient_dynasty,
            titleResId = R.string.faction_theme_crimson_decree_title,
            descriptionResId = R.string.faction_theme_crimson_decree_description,
            accessibilityLabelResId = R.string.faction_theme_crimson_decree_accessibility,
            sampleFlavor = SampleFlavor.AncientDynasty,
            primaryColor = Color(0xFF616B70),
            secondaryColor = Color(0xFF861D21),
            outlineColor = Color(0xFFFF7800),
        ),
    )

val DefaultFactionTheme: FactionThemeOption
    get() = FactionThemes.first()

fun customFactionTheme(settings: CustomFactionThemeSettings): FactionThemeOption {
    val normalizedPrimary = normalizeFactionThemeHex(settings.primaryHex) ?: DefaultCustomPrimaryHex
    val normalizedSecondary = normalizeFactionThemeHex(settings.secondaryHex) ?: DefaultCustomSecondaryHex
    val normalizedOutline = normalizeFactionThemeHexOrNull(settings.outlineHexOrNull)
    val primaryColor = parseFactionThemeColor(normalizedPrimary)
    val secondaryColor = parseFactionThemeColor(normalizedSecondary)
    return buildFactionThemeOption(
        id = customFactionThemeOptionId(settings.presetId),
        groupTitleResId = R.string.config_faction_theme_group_custom,
        titleResId = R.string.faction_theme_custom_title,
        descriptionResId = R.string.faction_theme_custom_description,
        accessibilityLabelResId = R.string.faction_theme_custom_accessibility,
        titleOverride = settings.displayName,
        sampleFlavor = SampleFlavor.SacredMachine,
        primaryColor = primaryColor,
        secondaryColor = secondaryColor,
        outlineColor = normalizedOutline?.let(::parseFactionThemeColor) ?: secondaryColor,
    )
}

fun normalizeFactionThemeHex(value: String?): String? {
    val trimmed = value?.trim()?.uppercase().orEmpty()
    val normalized =
        if (trimmed.startsWith("#")) {
            trimmed
        } else {
            "#$trimmed"
        }
    if (!FactionThemeHexRegex.matches(normalized)) {
        return null
    }
    return normalized
}

fun normalizeFactionThemeHexOrNull(value: String?): String? {
    val trimmed = value?.trim().orEmpty()
    if (trimmed.isBlank()) {
        return null
    }
    return normalizeFactionThemeHex(trimmed)
}

fun factionThemeColorOrNull(value: String?): Color? = normalizeFactionThemeHexOrNull(value)?.let(::parseFactionThemeColor)

fun customFactionThemeOptionId(presetId: String): String =
    if (presetId == DefaultCustomFactionThemePresetId) {
        CustomFactionThemeId
    } else {
        "$CustomFactionThemeId$CustomFactionThemeIdSeparator$presetId"
    }

fun isCustomFactionThemeOptionId(optionId: String): Boolean =
    optionId == CustomFactionThemeId || optionId.startsWith("$CustomFactionThemeId$CustomFactionThemeIdSeparator")

private fun parseFactionThemeColor(hex: String): Color {
    val normalized = normalizeFactionThemeHex(hex) ?: error("Invalid faction theme color: $hex")
    val rgb = normalized.drop(1).toLong(radix = 16)
    return Color(0xFF000000 or rgb)
}

fun buildFactionThemeOption(
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
): FactionThemeOption {
    val isDarkTheme = primaryColor.luminance() < DarkThemeLuminanceThreshold
    // Call sites pass colors by visual responsibility, not by Material slot name:
    // primaryColor is the dominant page/surface color, and secondaryColor is the
    // strong action/selection color. Do not swap them based on light/dark mode.
    val background = primaryColor
    // Keep surface fully opaque. Floating UI such as the mini-player relies on
    // MaterialTheme.colorScheme.surface for readable text; adding alpha here makes
    // docked cards look translucent even when the component requests a solid color.
    val surface = primaryColor
    val onBackground = if (isDarkTheme) FactionInkDark else FactionInkLight
    val primary = secondaryColor
    val onPrimary = primaryColor
    val primaryContainer = blend(background, secondaryColor, if (isDarkTheme) 0.22f else 0.14f)
    val secondaryContainer = blend(background, primary, if (isDarkTheme) 0.30f else 0.18f)
    val surfaceVariant = blend(background, secondaryColor, if (isDarkTheme) 0.16f else 0.10f)
    val outline = blend(primary, onBackground, if (isDarkTheme) 0.48f else 0.58f)
    val outlineVariant = blend(surfaceVariant, onBackground, if (isDarkTheme) 0.22f else 0.18f)
    val onSurfaceVariant =
        outlineFirstSecondaryTextColor(
            background = background,
            outline = outlineColor,
            fallbackText = onBackground,
        )

    val colorScheme =
        if (isDarkTheme) {
            darkColorScheme(
                primary = primary,
                onPrimary = onPrimary,
                primaryContainer = primaryContainer,
                onPrimaryContainer = FactionInkDark,
                secondary = secondaryColor,
                onSecondary = primaryColor,
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = FactionInkDark,
                tertiary = secondaryColor,
                onTertiary = primaryColor,
                tertiaryContainer = secondaryContainer,
                onTertiaryContainer = FactionInkDark,
                background = background,
                onBackground = onBackground,
                surface = surface,
                onSurface = onBackground,
                surfaceVariant = surfaceVariant,
                onSurfaceVariant = onSurfaceVariant,
                outline = outline,
                outlineVariant = outlineVariant,
            )
        } else {
            lightColorScheme(
                primary = primary,
                onPrimary = onPrimary,
                primaryContainer = primaryContainer,
                onPrimaryContainer = FactionInkLight,
                secondary = blend(primary, background, 0.20f),
                onSecondary = primaryColor,
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = FactionInkLight,
                tertiary = blend(primary, background, 0.34f),
                onTertiary = primaryColor,
                tertiaryContainer = blend(primaryColor, secondaryColor, 0.08f),
                onTertiaryContainer = FactionInkLight,
                background = background,
                onBackground = onBackground,
                surface = surface,
                onSurface = onBackground,
                surfaceVariant = surfaceVariant,
                onSurfaceVariant = onSurfaceVariant,
                outline = outline,
                outlineVariant = outlineVariant,
            )
        }

    return FactionThemeOption(
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

private fun outlineFirstSecondaryTextColor(
    background: Color,
    outline: Color,
    fallbackText: Color,
): Color {
    if (contrastRatio(background, outline) >= SecondaryTextContrastThreshold) {
        return outline
    }
    for (ratio in SecondaryTextFallbackBlendSteps) {
        val candidate = blend(outline, fallbackText, ratio)
        if (contrastRatio(background, candidate) >= SecondaryTextContrastThreshold) {
            return candidate
        }
    }
    return fallbackText
}

private fun contrastRatio(
    first: Color,
    second: Color,
): Float {
    val firstL = first.luminance()
    val secondL = second.luminance()
    val lighter = maxOf(firstL, secondL)
    val darker = minOf(firstL, secondL)
    return (lighter + 0.05f) / (darker + 0.05f)
}

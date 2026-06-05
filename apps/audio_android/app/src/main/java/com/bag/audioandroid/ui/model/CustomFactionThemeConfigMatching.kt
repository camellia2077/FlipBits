package com.bag.audioandroid.ui.model

import com.bag.audioandroid.ui.theme.normalizeFactionThemeHex

fun CustomFactionThemeSettings.hasSameConfigAs(other: CustomFactionThemeSettings): Boolean =
    displayName.trim() == other.displayName.trim() &&
        primaryHex.equals(other.primaryHex, ignoreCase = true) &&
        secondaryHex.equals(other.secondaryHex, ignoreCase = true) &&
        (outlineHexOrNull ?: "").equals(other.outlineHexOrNull ?: "", ignoreCase = true)

fun CustomFactionThemeSettings.hasSameMaterialImportConfigAs(other: CustomFactionThemeSettings): Boolean {
    val normalizedCurrentPrimary = normalizeFactionThemeHex(primaryHex)
    val normalizedOtherPrimary = normalizeFactionThemeHex(other.primaryHex)
    return displayName.trim() == other.displayName.trim() &&
        normalizedCurrentPrimary != null &&
        normalizedCurrentPrimary.equals(normalizedOtherPrimary, ignoreCase = true)
}

fun findDuplicateImportedThemePresetId(
    existing: List<CustomFactionThemeSettings>,
    imported: CustomFactionThemeSettings,
    mode: CustomThemeImportMode,
): String? =
    existing
        .firstOrNull { preset ->
            when (mode) {
                CustomThemeImportMode.DualTone -> preset.hasSameConfigAs(imported)
                CustomThemeImportMode.Material -> preset.hasSameMaterialImportConfigAs(imported)
            }
        }?.presetId

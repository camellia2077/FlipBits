package com.bag.audioandroid.ui.model

import com.bag.audioandroid.ui.theme.normalizeBrandThemeHex

fun CustomBrandThemeSettings.hasSameConfigAs(other: CustomBrandThemeSettings): Boolean =
    displayName.trim() == other.displayName.trim() &&
        primaryHex.equals(other.primaryHex, ignoreCase = true) &&
        secondaryHex.equals(other.secondaryHex, ignoreCase = true) &&
        (outlineHexOrNull ?: "").equals(other.outlineHexOrNull ?: "", ignoreCase = true)

fun CustomBrandThemeSettings.hasSameMaterialImportConfigAs(other: CustomBrandThemeSettings): Boolean {
    val normalizedCurrentPrimary = normalizeBrandThemeHex(primaryHex)
    val normalizedOtherPrimary = normalizeBrandThemeHex(other.primaryHex)
    return displayName.trim() == other.displayName.trim() &&
        normalizedCurrentPrimary != null &&
        normalizedCurrentPrimary.equals(normalizedOtherPrimary, ignoreCase = true)
}

fun findDuplicateImportedThemePresetId(
    existing: List<CustomBrandThemeSettings>,
    imported: CustomBrandThemeSettings,
    mode: CustomThemeImportMode,
): String? =
    existing
        .firstOrNull { preset ->
            when (mode) {
                CustomThemeImportMode.DualTone -> preset.hasSameConfigAs(imported)
                CustomThemeImportMode.Material -> preset.hasSameMaterialImportConfigAs(imported)
            }
        }?.presetId

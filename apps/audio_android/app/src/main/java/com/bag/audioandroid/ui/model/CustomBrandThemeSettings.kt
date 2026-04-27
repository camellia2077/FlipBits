package com.bag.audioandroid.ui.model

data class CustomBrandThemeSettings(
    val presetId: String = DefaultCustomBrandThemePresetId,
    val displayName: String = DefaultCustomBrandThemeDisplayName,
    val backgroundHex: String = "#E8E2D0",
    val accentHex: String = "#9E1B1B",
    val outlineHexOrNull: String? = "#C5A059",
)

val DefaultCustomBrandThemeSettings = CustomBrandThemeSettings()

const val DefaultCustomBrandThemePresetId = "default"
const val DefaultCustomBrandThemeDisplayName = "Custom 1"

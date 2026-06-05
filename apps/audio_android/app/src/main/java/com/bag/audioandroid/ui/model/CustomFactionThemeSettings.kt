package com.bag.audioandroid.ui.model

data class CustomFactionThemeSettings(
    val presetId: String = DefaultCustomFactionThemePresetId,
    val displayName: String = DefaultCustomFactionThemeDisplayName,
    val primaryHex: String = "#E8E2D0",
    val secondaryHex: String = "#9E1B1B",
    val outlineHexOrNull: String? = "#C78C25",
)

val DefaultCustomFactionThemeSettings = CustomFactionThemeSettings()

const val DefaultCustomFactionThemePresetId = "default"
const val DefaultCustomFactionThemeDisplayName = "Custom 1"

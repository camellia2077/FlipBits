package com.bag.audioandroid.ui.theme

import com.bag.audioandroid.ui.model.PaletteOption

val DefaultCustomMaterialPaletteSettings =
    com.bag.audioandroid.ui.model.CustomBrandThemeSettings(
        presetId = "material_custom",
        displayName = "Custom 1",
        primaryHex = "#6750A4",
        secondaryHex = "#8A74C0",
        outlineHexOrNull = "#A38BD6",
    )

val MaterialPalettes: List<PaletteOption> =
    buildList {
        add(customMaterialPalette(DefaultCustomMaterialPaletteSettings))
        addAll(materialRedsPalettes)
        addAll(materialOrangePalettes)
        addAll(materialYellowPalettes)
        addAll(materialGreenPalettes)
        addAll(materialBluePalettes)
        addAll(materialPurplePalettes)
        addAll(materialNeutralPalettes)
    }

val DefaultMaterialPalette: PaletteOption =
    MaterialPalettes.firstOrNull { it.id == "stone" } ?: MaterialPalettes.first()

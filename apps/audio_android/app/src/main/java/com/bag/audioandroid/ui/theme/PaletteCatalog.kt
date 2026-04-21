package com.bag.audioandroid.ui.theme

import com.bag.audioandroid.ui.model.PaletteOption

val MaterialPalettes: List<PaletteOption> =
    buildList {
        addAll(materialRedsPinksPalettes)
        addAll(materialOrangePalettes)
        addAll(materialYellowPalettes)
        addAll(materialGreenPalettes)
        addAll(materialCyansBluesPalettes)
        addAll(materialPurplesMagentasPalettes)
        addAll(materialNeutralPalettes)
    }

val DefaultMaterialPalette: PaletteOption =
    MaterialPalettes.firstOrNull { it.id == "mars_relic" } ?: MaterialPalettes.first()

package com.bag.audioandroid.ui

import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.theme.DefaultMaterialPalette
import com.bag.audioandroid.ui.theme.MaterialPalettes
import com.bag.audioandroid.ui.theme.customMaterialPaletteId
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioAndroidPreferencesBindingsTest {
    @Test
    fun `legacy persisted non default material palette restores material theme style`() {
        val nonDefaultMaterialPalette = MaterialPalettes.first { it.id != DefaultMaterialPalette.id }

        val restored =
            inferPersistedThemeStyle(
                themeStyleId = null,
                paletteId = nonDefaultMaterialPalette.id,
                lightPaletteId = nonDefaultMaterialPalette.id,
                darkPaletteId = nonDefaultMaterialPalette.id,
            )

        assertEquals(ThemeStyleOption.Material, restored)
    }

    @Test
    fun `legacy persisted custom material palette restores material theme style`() {
        val restored =
            inferPersistedThemeStyle(
                themeStyleId = null,
                paletteId = customMaterialPaletteId("keyboard"),
                lightPaletteId = customMaterialPaletteId("keyboard"),
                darkPaletteId = null,
            )

        assertEquals(ThemeStyleOption.Material, restored)
    }

    @Test
    fun `explicit persisted theme style still wins`() {
        val restored =
            inferPersistedThemeStyle(
                themeStyleId = ThemeStyleOption.BrandDualTone.id,
                paletteId = customMaterialPaletteId("keyboard"),
                lightPaletteId = customMaterialPaletteId("keyboard"),
                darkPaletteId = customMaterialPaletteId("keyboard"),
            )

        assertEquals(ThemeStyleOption.BrandDualTone, restored)
    }
}

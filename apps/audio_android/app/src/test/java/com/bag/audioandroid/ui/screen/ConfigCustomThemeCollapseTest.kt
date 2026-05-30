package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.ui.theme.BrandDualToneThemes
import com.bag.audioandroid.ui.theme.MaterialPalettes
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfigCustomThemeCollapseTest {
    @Test
    fun `collapsed custom material palettes expose only selected palette`() {
        val options = MaterialPalettes.take(3)
        val selected = options[1]

        val visible =
            visibleCustomMaterialPaletteOptions(
                options = options,
                selectedPaletteId = selected.id,
                expanded = false,
            )

        assertEquals(listOf(selected.id), visible.map { it.id })
    }

    @Test
    fun `expanded custom material palettes expose all palettes`() {
        val options = MaterialPalettes.take(3)

        val visible =
            visibleCustomMaterialPaletteOptions(
                options = options,
                selectedPaletteId = options[1].id,
                expanded = true,
            )

        assertEquals(options.map { it.id }, visible.map { it.id })
    }

    @Test
    fun `collapsed brand theme section exposes only selected theme`() {
        val options = BrandDualToneThemes.take(3)
        val selected = options[1]

        val visible =
            visibleBrandThemeOptions(
                options = options,
                selectedBrandThemeId = selected.id,
                expanded = false,
            )

        assertEquals(listOf(selected.id), visible.map { it.id })
    }

    @Test
    fun `expanded brand theme section exposes all themes`() {
        val options = BrandDualToneThemes.take(3)

        val visible =
            visibleBrandThemeOptions(
                options = options,
                selectedBrandThemeId = options[1].id,
                expanded = true,
            )

        assertEquals(options.map { it.id }, visible.map { it.id })
    }

    @Test
    fun `collapsed brand theme section exposes no theme when selection is outside group`() {
        val options = BrandDualToneThemes.take(3)

        val visible =
            visibleBrandThemeOptions(
                options = options,
                selectedBrandThemeId = "outside",
                expanded = false,
            )

        assertEquals(emptyList<String>(), visible.map { it.id })
    }

    @Test
    fun `collapsed custom material palettes expose no palette when selection is outside group`() {
        val options = MaterialPalettes.take(3)

        val visible =
            visibleCustomMaterialPaletteOptions(
                options = options,
                selectedPaletteId = "outside",
                expanded = false,
            )

        assertEquals(emptyList<String>(), visible.map { it.id })
    }
}

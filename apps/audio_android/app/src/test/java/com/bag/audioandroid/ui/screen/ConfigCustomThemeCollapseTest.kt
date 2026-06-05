package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.ui.theme.FactionThemes
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
    fun `collapsed faction theme section exposes only selected theme`() {
        val options = FactionThemes.take(3)
        val selected = options[1]

        val visible =
            visibleFactionThemeOptions(
                options = options,
                selectedFactionThemeId = selected.id,
                expanded = false,
            )

        assertEquals(listOf(selected.id), visible.map { it.id })
    }

    @Test
    fun `expanded faction theme section exposes all themes`() {
        val options = FactionThemes.take(3)

        val visible =
            visibleFactionThemeOptions(
                options = options,
                selectedFactionThemeId = options[1].id,
                expanded = true,
            )

        assertEquals(options.map { it.id }, visible.map { it.id })
    }

    @Test
    fun `collapsed faction theme section exposes no theme when selection is outside group`() {
        val options = FactionThemes.take(3)

        val visible =
            visibleFactionThemeOptions(
                options = options,
                selectedFactionThemeId = "outside",
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

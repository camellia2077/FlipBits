package com.bag.audioandroid.ui

import com.bag.audioandroid.data.SampleInput
import com.bag.audioandroid.data.SampleInputTextProvider
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.CustomBrandThemeSettings
import com.bag.audioandroid.ui.model.SampleFlavor
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.theme.DefaultMaterialPalette
import com.bag.audioandroid.ui.theme.MaterialPalettes
import com.bag.audioandroid.ui.theme.customMaterialPalette
import com.bag.audioandroid.ui.theme.customMaterialPaletteId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AudioAndroidPreferencesActionsMaterialTest {
    private val sampleInputSessionUpdater = SampleInputSessionUpdater(LocalPreferencesActionsSampleInputTextProvider())

    @Test
    fun `first switch to material defaults to stone palette`() {
        val state = AudioAppUiState(selectedThemeStyle = ThemeStyleOption.BrandDualTone)

        val updated = state.withSelectedMaterialThemeStyle(sampleInputSessionUpdater)

        assertEquals(ThemeStyleOption.Material, updated.selectedThemeStyle)
        assertEquals("stone", updated.selectedPalette.id)
        assertEquals("stone", updated.selectedMaterialPaletteIdLight)
    }

    @Test
    fun `switching to material keeps persisted material palette selection`() {
        val persistedPalette = MaterialPalettes.first { it.id == "slate" }
        val state =
            AudioAppUiState(
                selectedThemeStyle = ThemeStyleOption.BrandDualTone,
                selectedPalette = persistedPalette,
                selectedMaterialPaletteIdLight = persistedPalette.id,
            )

        val updated = state.withSelectedMaterialThemeStyle(sampleInputSessionUpdater)

        assertEquals(ThemeStyleOption.Material, updated.selectedThemeStyle)
        assertEquals("slate", updated.selectedPalette.id)
        assertEquals("slate", updated.selectedMaterialPaletteIdLight)
    }

    @Test
    fun `selecting built in material palette switches theme style to material`() {
        val state = AudioAppUiState(selectedThemeStyle = ThemeStyleOption.BrandDualTone)

        val updated = state.withSelectedMaterialPaletteTheme(DefaultMaterialPalette)

        assertEquals(ThemeStyleOption.Material, updated.selectedThemeStyle)
        assertEquals(DefaultMaterialPalette.id, updated.selectedPalette.id)
        assertEquals(DefaultMaterialPalette.id, updated.selectedMaterialPaletteIdLight)
    }

    @Test
    fun `saving custom material with null replace id pushes a new preset to the top`() {
        val existing = customMaterialSettings(presetId = "keyboard", displayName = "Keyboard", primaryHex = "#2D005F")
        val state =
            AudioAppUiState(
                customMaterialThemePresets = listOf(existing),
                selectedPalette = customMaterialPalette(existing),
            )

        val added =
            state.withSavedCustomMaterialTheme(
                settings = customMaterialSettings(presetId = "", displayName = "Paper", primaryHex = "#E5E9F0"),
                replacePresetId = null,
            )

        assertEquals(2, added.customMaterialThemePresets.size)
        assertEquals("Paper", added.customMaterialThemePresets.first().displayName)
        assertEquals("Keyboard", added.customMaterialThemePresets.last().displayName)
        assertNotEquals(existing.presetId, added.customMaterialThemePresets.first().presetId)
        assertEquals(ThemeStyleOption.Material, added.selectedThemeStyle)
        assertEquals(
            customMaterialPaletteId(added.customMaterialThemePresets.first().presetId),
            added.selectedPalette.id,
        )
    }

    @Test
    fun `saving custom material with replace id updates the existing preset`() {
        val existing = customMaterialSettings(presetId = "keyboard", displayName = "Keyboard", primaryHex = "#2D005F")
        val state =
            AudioAppUiState(
                customMaterialThemePresets = listOf(existing),
                selectedPalette = customMaterialPalette(existing),
            )

        val replaced =
            state.withSavedCustomMaterialTheme(
                settings = customMaterialSettings(presetId = "", displayName = "Night Keyboard", primaryHex = "#111827"),
                replacePresetId = existing.presetId,
            )

        assertEquals(1, replaced.customMaterialThemePresets.size)
        assertEquals(existing.presetId, replaced.customMaterialThemePresets.single().presetId)
        assertEquals("Night Keyboard", replaced.customMaterialThemePresets.single().displayName)
        assertEquals("#111827", replaced.customMaterialThemePresets.single().primaryHex)
        assertEquals(
            customMaterialPaletteId(existing.presetId),
            replaced.selectedPalette.id,
        )
    }

    @Test
    fun `importing duplicate custom material overwrites matching preset`() {
        val existing = customMaterialSettings(presetId = "keyboard", displayName = "Keyboard", primaryHex = "#2D005F")
        val state =
            AudioAppUiState(
                customMaterialThemePresets = listOf(existing),
                selectedPalette = customMaterialPalette(existing),
            )

        val imported =
            state.withImportedCustomMaterialThemes(
                listOf(customMaterialSettings(presetId = "", displayName = "Keyboard", primaryHex = "#2D005F")),
            )

        assertEquals(1, imported.customMaterialThemePresets.size)
        assertEquals("keyboard", imported.customMaterialThemePresets.single().presetId)
        assertEquals("#2D005F", imported.customMaterialThemePresets.single().primaryHex)
        assertEquals(customMaterialPaletteId("keyboard"), imported.selectedPalette.id)
    }

    @Test
    fun `importing duplicate custom brand overwrites matching preset`() {
        val existing =
            CustomBrandThemeSettings(
                presetId = "vigilu",
                displayName = "Vigilu",
                primaryHex = "#E5E9F0",
                secondaryHex = "#4C566A",
                outlineHexOrNull = "#2E3440",
            )
        val state = AudioAppUiState(customBrandThemePresets = listOf(existing))

        val imported =
            state.withImportedCustomBrandThemes(
                listOf(
                    CustomBrandThemeSettings(
                        presetId = "",
                        displayName = "Vigilu",
                        primaryHex = "#E5E9F0",
                        secondaryHex = "#4C566A",
                        outlineHexOrNull = "#2E3440",
                    ),
                ),
            )

        assertEquals(1, imported.customBrandThemePresets.size)
        assertEquals("vigilu", imported.customBrandThemePresets.single().presetId)
        assertEquals("#E5E9F0", imported.customBrandThemePresets.single().primaryHex)
        assertEquals("#4C566A", imported.customBrandThemePresets.single().secondaryHex)
    }

    @Test
    fun `importing same name dual tone with different colors adds a new preset`() {
        val existing =
            CustomBrandThemeSettings(
                presetId = "vigilu",
                displayName = "Vigilu",
                primaryHex = "#E5E9F0",
                secondaryHex = "#4C566A",
                outlineHexOrNull = "#2E3440",
            )
        val state = AudioAppUiState(customBrandThemePresets = listOf(existing))

        val imported =
            state.withImportedCustomBrandThemes(
                listOf(
                    CustomBrandThemeSettings(
                        presetId = "",
                        displayName = "Vigilu",
                        primaryHex = "#101014",
                        secondaryHex = "#78D6FF",
                        outlineHexOrNull = "#303846",
                    ),
                ),
            )

        assertEquals(2, imported.customBrandThemePresets.size)
        assertEquals(listOf("Vigilu", "Vigilu"), imported.customBrandThemePresets.map { it.displayName })
        assertEquals("#101014", imported.customBrandThemePresets.first().primaryHex)
        assertEquals("#E5E9F0", imported.customBrandThemePresets.last().primaryHex)
    }

    @Test
    fun `batch importing material themes generates unique preset ids for default template imports`() {
        val state = AudioAppUiState(customMaterialThemePresets = emptyList())

        val imported =
            state.withImportedCustomMaterialThemes(
                listOf(
                    customMaterialSettings(presetId = "default", displayName = "Paper", primaryHex = "#E5E9F0"),
                    customMaterialSettings(presetId = "default", displayName = "Night", primaryHex = "#111827"),
                ),
            )

        assertEquals(2, imported.customMaterialThemePresets.size)
        assertNotEquals(
            imported.customMaterialThemePresets[0].presetId,
            imported.customMaterialThemePresets[1].presetId,
        )
        assertNotEquals("default", imported.customMaterialThemePresets[0].presetId)
        assertNotEquals("default", imported.customMaterialThemePresets[1].presetId)
    }

    @Test
    fun `importing material themes keeps shared batch order at the top`() {
        val existing = customMaterialSettings(presetId = "existing", displayName = "Existing", primaryHex = "#2D005F")
        val state = AudioAppUiState(customMaterialThemePresets = listOf(existing))

        val imported =
            state.withImportedCustomMaterialThemes(
                listOf(
                    customMaterialSettings(presetId = "", displayName = "Paper", primaryHex = "#E5E9F0"),
                    customMaterialSettings(presetId = "", displayName = "Night", primaryHex = "#111827"),
                ),
            )

        assertEquals(listOf("Paper", "Night", "Existing"), imported.customMaterialThemePresets.map { it.displayName })
    }

    @Test
    fun `importing same name material with different primary adds a new preset`() {
        val existing = customMaterialSettings(presetId = "keyboard", displayName = "Keyboard", primaryHex = "#2D005F")
        val state = AudioAppUiState(customMaterialThemePresets = listOf(existing))

        val imported =
            state.withImportedCustomMaterialThemes(
                listOf(
                    customMaterialSettings(
                        presetId = "",
                        displayName = "Keyboard",
                        primaryHex = "#111827",
                    ),
                ),
            )

        assertEquals(2, imported.customMaterialThemePresets.size)
        assertEquals(listOf("Keyboard", "Keyboard"), imported.customMaterialThemePresets.map { it.displayName })
        assertEquals("#111827", imported.customMaterialThemePresets.first().primaryHex)
        assertEquals("#2D005F", imported.customMaterialThemePresets.last().primaryHex)
    }

    @Test
    fun `batch importing dual tone themes generates unique preset ids for default template imports`() {
        val state = AudioAppUiState(customBrandThemePresets = emptyList())

        val imported =
            state.withImportedCustomBrandThemes(
                listOf(
                    CustomBrandThemeSettings(
                        presetId = "default",
                        displayName = "Ash",
                        primaryHex = "#101014",
                        secondaryHex = "#78D6FF",
                        outlineHexOrNull = "#303846",
                    ),
                    CustomBrandThemeSettings(
                        presetId = "default",
                        displayName = "Brass",
                        primaryHex = "#E8E2D0",
                        secondaryHex = "#9E1B1B",
                        outlineHexOrNull = "#C78C25",
                    ),
                ),
            )

        assertEquals(2, imported.customBrandThemePresets.size)
        assertNotEquals(
            imported.customBrandThemePresets[0].presetId,
            imported.customBrandThemePresets[1].presetId,
        )
        assertNotEquals("default", imported.customBrandThemePresets[0].presetId)
        assertNotEquals("default", imported.customBrandThemePresets[1].presetId)
    }

    @Test
    fun `importing dual tone themes keeps shared batch order at the top`() {
        val existing =
            CustomBrandThemeSettings(
                presetId = "existing",
                displayName = "Existing",
                primaryHex = "#E8E2D0",
                secondaryHex = "#9E1B1B",
                outlineHexOrNull = "#C78C25",
            )
        val state = AudioAppUiState(customBrandThemePresets = listOf(existing))

        val imported =
            state.withImportedCustomBrandThemes(
                listOf(
                    CustomBrandThemeSettings(
                        presetId = "",
                        displayName = "Ash",
                        primaryHex = "#101014",
                        secondaryHex = "#78D6FF",
                        outlineHexOrNull = "#303846",
                    ),
                    CustomBrandThemeSettings(
                        presetId = "",
                        displayName = "Brass",
                        primaryHex = "#E8E2D0",
                        secondaryHex = "#9E1B1B",
                        outlineHexOrNull = "#C78C25",
                    ),
                ),
            )

        assertEquals(listOf("Ash", "Brass", "Existing"), imported.customBrandThemePresets.map { it.displayName })
    }

    private fun customMaterialSettings(
        presetId: String,
        displayName: String,
        primaryHex: String,
    ) = CustomBrandThemeSettings(
        presetId = presetId,
        displayName = displayName,
        primaryHex = primaryHex,
        secondaryHex = primaryHex,
        outlineHexOrNull = primaryHex,
    )
}

private class LocalPreferencesActionsSampleInputTextProvider : SampleInputTextProvider {
    override fun defaultSample(
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
    ): SampleInput = SampleInput(id = "${mode.wireName}-${flavor.name}", text = "sample")

    override fun sampleIds(
        mode: TransportModeOption,
        flavor: SampleFlavor,
        length: SampleInputLengthOption,
    ): List<String> = listOf("${mode.wireName}-${flavor.name}")

    override fun sampleById(
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
        sampleId: String,
    ): SampleInput? = SampleInput(id = sampleId, text = "sample")
}

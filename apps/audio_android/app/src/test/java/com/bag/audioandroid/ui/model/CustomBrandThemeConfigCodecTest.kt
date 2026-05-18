package com.bag.audioandroid.ui.model

import com.bag.audioandroid.ui.theme.normalizeCustomMaterialThemeSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomBrandThemeConfigCodecTest {
    @Test
    fun `serializes single custom theme config`() {
        val text =
            CustomBrandThemeSettings(
                displayName = "Test Theme",
                primaryHex = "#101014",
                secondaryHex = "#78D6FF",
                outlineHexOrNull = "#303846",
            ).toConfigText()

        assertEquals(
            """
            name=Test Theme
            primary=#101014
            secondary=#78D6FF
            outline=#303846
            """.trimIndent(),
            text,
        )
    }

    @Test
    fun `serializes single custom material theme config`() {
        val text =
            normalizeCustomMaterialThemeSettings(
                CustomBrandThemeSettings(
                    displayName = "Paper",
                    primaryHex = "#E5E9F0",
                    secondaryHex = "#111111",
                    outlineHexOrNull = "#222222",
                ),
            ).toMaterialConfigText()

        assertEquals(
            """
            name=Paper
            primary=#E5E9F0
            """.trimIndent(),
            text,
        )
    }

    @Test
    fun `parses single custom theme config`() {
        val result =
            parseCustomBrandThemeImportText(
                """
                NAME = Imported

                Primary = 101014
                SECONDARY = #78d6ff
                outline =
                """.trimIndent(),
            )

        require(result is CustomBrandThemeImportParseResult.Valid)
        assertEquals(1, result.settings.size)
        assertEquals("Imported", result.settings.single().displayName)
        assertEquals("#101014", result.settings.single().primaryHex)
        assertEquals("#78D6FF", result.settings.single().secondaryHex)
        assertEquals(null, result.settings.single().outlineHexOrNull)
    }

    @Test
    fun `rejects invalid config`() {
        val result =
            parseCustomBrandThemeImportText(
                """
                name=Broken
                primary=nope
                secondary=#78D6FF
                """.trimIndent(),
            )

        assertEquals(
            CustomBrandThemeImportParseResult.Invalid(
                CustomThemeImportError.InvalidHex(1, "primary", "nope"),
            ),
            result,
        )
    }

    @Test
    fun `serializes batch custom theme config`() {
        val text =
            listOf(
                CustomBrandThemeSettings(
                    displayName = "First",
                    primaryHex = "#101014",
                    secondaryHex = "#78D6FF",
                    outlineHexOrNull = null,
                ),
                CustomBrandThemeSettings(
                    displayName = "Second",
                    primaryHex = "#1A100C",
                    secondaryHex = "#FFAA55",
                    outlineHexOrNull = "#553011",
                ),
            ).toBatchConfigText()

        assertEquals(
            """
            name=First
            primary=#101014
            secondary=#78D6FF
            outline=

            name=Second
            primary=#1A100C
            secondary=#FFAA55
            outline=#553011
            """.trimIndent(),
            text,
        )
    }

    @Test
    fun `parses batch custom theme config`() {
        val result =
            parseCustomBrandThemeImportText(
                """
                name=First
                primary=#101014
                secondary=#78D6FF
                outline=

                name=Second
                primary=1a100c
                secondary=ffaa55
                outline=553011
                """.trimIndent(),
            )

        require(result is CustomBrandThemeImportParseResult.Valid)
        assertEquals(2, result.settings.size)
        assertEquals("First", result.settings[0].displayName)
        assertEquals(null, result.settings[0].outlineHexOrNull)
        assertEquals("#1A100C", result.settings[1].primaryHex)
        assertEquals("#FFAA55", result.settings[1].secondaryHex)
        assertEquals("#553011", result.settings[1].outlineHexOrNull)
    }

    @Test
    fun `rejects empty batch custom theme config`() {
        val result =
            parseCustomBrandThemeImportText(
                """
                
                """.trimIndent(),
            )

        assertEquals(
            CustomBrandThemeImportParseResult.Invalid(CustomThemeImportError.EmptyInput),
            result,
        )
    }

    @Test
    fun `rejects batch with invalid block`() {
        val result =
            parseCustomBrandThemeImportText(
                """
                name=Broken
                primary=nope
                secondary=#78D6FF
                outline=
                """.trimIndent(),
            )

        assertEquals(
            CustomBrandThemeImportParseResult.Invalid(
                CustomThemeImportError.InvalidHex(1, "primary", "nope"),
            ),
            result,
        )
    }

    @Test
    fun `reports missing required field in dual tone import`() {
        val result =
            parseCustomBrandThemeImportText(
                """
                name=Broken
                primary=#101014
                outline=#FFFFFF
                """.trimIndent(),
            )

        assertEquals(
            CustomBrandThemeImportParseResult.Invalid(
                CustomThemeImportError.MissingField(1, "secondary"),
            ),
            result,
        )
    }

    @Test
    fun `reports malformed import line`() {
        val result =
            parseCustomBrandThemeImportText(
                """
                name=Broken
                primary:#101014
                secondary=#78D6FF
                """.trimIndent(),
            )

        assertEquals(
            CustomBrandThemeImportParseResult.Invalid(
                CustomThemeImportError.MalformedLine(2),
            ),
            result,
        )
    }

    @Test
    fun `parses batch custom material theme config`() {
        val result =
            parseCustomMaterialThemeImportText(
                """
                name=keyboard
                primary=#2D005F

                name=paper
                primary=E5E9F0
                """.trimIndent(),
            )

        require(result is CustomBrandThemeImportParseResult.Valid)
        assertEquals(2, result.settings.size)
        assertEquals("keyboard", result.settings[0].displayName)
        assertEquals("#2D005F", result.settings[0].primaryHex)
        assertEquals("paper", result.settings[1].displayName)
        assertEquals("#E5E9F0", result.settings[1].primaryHex)
    }

    @Test
    fun `reports unknown field in material import`() {
        val result =
            parseCustomMaterialThemeImportText(
                """
                name=keyboard
                primary=#2D005F
                tertiary=#FFFFFF
                """.trimIndent(),
            )

        assertEquals(
            CustomBrandThemeImportParseResult.Invalid(
                CustomThemeImportError.UnknownField(1, "tertiary"),
            ),
            result,
        )
    }

    @Test
    fun `reports dual tone text pasted into material import`() {
        val result =
            parseCustomMaterialThemeImportText(
                """
                name=Vigilu
                primary=#E5E9F0
                secondary=#4C566A
                outline=#2E3440
                """.trimIndent(),
            )

        assertEquals(
            CustomBrandThemeImportParseResult.Invalid(
                CustomThemeImportError.WrongImportMode(
                    blockIndex = 1,
                    expectedMode = CustomThemeImportMode.Material,
                    detectedMode = CustomThemeImportMode.DualTone,
                ),
            ),
            result,
        )
    }

    @Test
    fun `reports material text pasted into dual tone import`() {
        val result =
            parseCustomBrandThemeImportText(
                """
                name=keyboard
                primary=#2D005F
                """.trimIndent(),
            )

        assertEquals(
            CustomBrandThemeImportParseResult.Invalid(
                CustomThemeImportError.WrongImportMode(
                    blockIndex = 1,
                    expectedMode = CustomThemeImportMode.DualTone,
                    detectedMode = CustomThemeImportMode.Material,
                ),
            ),
            result,
        )
    }

    @Test
    fun `reports invalid material hex`() {
        val result =
            parseCustomMaterialThemeImportText(
                """
                name=keyboard
                primary=#xyzxyz
                """.trimIndent(),
            )

        assertEquals(
            CustomBrandThemeImportParseResult.Invalid(
                CustomThemeImportError.InvalidHex(1, "primary", "#xyzxyz"),
            ),
            result,
        )
    }

    @Test
    fun `material export then import matches existing preset for duplicate confirm`() {
        val existing =
            normalizeCustomMaterialThemeSettings(
                CustomBrandThemeSettings(
                    presetId = "keyboard",
                    displayName = "Keyboard",
                    primaryHex = "#2D005F",
                    secondaryHex = "",
                    outlineHexOrNull = null,
                ),
            )
        val exported = listOf(existing).toMaterialBatchConfigText()
        val parsed = parseCustomMaterialThemeImportText(exported)

        require(parsed is CustomBrandThemeImportParseResult.Valid)
        assertEquals(
            "keyboard",
            findDuplicateImportedThemePresetId(
                existing = listOf(existing),
                imported = parsed.settings.single(),
                mode = CustomThemeImportMode.Material,
            ),
        )
    }

    @Test
    fun `matches duplicate theme by user visible fields`() {
        val first =
            CustomBrandThemeSettings(
                presetId = "a",
                displayName = "Same",
                primaryHex = "#101014",
                secondaryHex = "#78D6FF",
                outlineHexOrNull = null,
            )
        val second =
            CustomBrandThemeSettings(
                presetId = "b",
                displayName = "Same",
                primaryHex = "#101014",
                secondaryHex = "#78d6ff",
                outlineHexOrNull = "",
            )

        assertTrue(first.hasSameConfigAs(second))
    }
}

package com.bag.audioandroid.ui.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomBrandThemeConfigCodecTest {
    @Test
    fun `serializes single custom theme config`() {
        val text =
            CustomBrandThemeSettings(
                displayName = "Test Theme",
                backgroundHex = "#101014",
                accentHex = "#78D6FF",
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
        assertEquals("#101014", result.settings.single().backgroundHex)
        assertEquals("#78D6FF", result.settings.single().accentHex)
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

        assertTrue(result is CustomBrandThemeImportParseResult.Invalid)
    }

    @Test
    fun `serializes batch custom theme config`() {
        val text =
            listOf(
                CustomBrandThemeSettings(
                    displayName = "First",
                    backgroundHex = "#101014",
                    accentHex = "#78D6FF",
                    outlineHexOrNull = null,
                ),
                CustomBrandThemeSettings(
                    displayName = "Second",
                    backgroundHex = "#1A100C",
                    accentHex = "#FFAA55",
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
        assertEquals("#1A100C", result.settings[1].backgroundHex)
        assertEquals("#FFAA55", result.settings[1].accentHex)
        assertEquals("#553011", result.settings[1].outlineHexOrNull)
    }

    @Test
    fun `rejects empty batch custom theme config`() {
        val result =
            parseCustomBrandThemeImportText(
                """
                
                """.trimIndent(),
            )

        assertTrue(result is CustomBrandThemeImportParseResult.Invalid)
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

        assertTrue(result is CustomBrandThemeImportParseResult.Invalid)
    }

    @Test
    fun `matches duplicate theme by user visible fields`() {
        val first =
            CustomBrandThemeSettings(
                presetId = "a",
                displayName = "Same",
                backgroundHex = "#101014",
                accentHex = "#78D6FF",
                outlineHexOrNull = null,
            )
        val second =
            CustomBrandThemeSettings(
                presetId = "b",
                displayName = "Same",
                backgroundHex = "#101014",
                accentHex = "#78d6ff",
                outlineHexOrNull = "",
            )

        assertTrue(first.hasSameConfigAs(second))
    }
}

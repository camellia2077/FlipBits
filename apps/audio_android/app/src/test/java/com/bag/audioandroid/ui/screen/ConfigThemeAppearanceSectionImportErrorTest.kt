package com.bag.audioandroid.ui.screen

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bag.audioandroid.ui.model.CustomBrandThemeSettings
import com.bag.audioandroid.ui.model.CustomThemeImportError
import com.bag.audioandroid.ui.model.CustomThemeImportMode
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConfigThemeAppearanceSectionImportErrorTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `batch dual tone import preview treats fully matching configs as duplicates`() {
        val existing =
            CustomBrandThemeSettings(
                presetId = "custom1",
                displayName = "custom1",
                primaryHex = "#E5E9F0",
                secondaryHex = "#4C566A",
                outlineHexOrNull = "#2E3440",
            )
        val duplicate =
            CustomBrandThemeSettings(
                displayName = "custom1",
                primaryHex = "#E5E9F0",
                secondaryHex = "#4C566A",
                outlineHexOrNull = "#2E3440",
            )
        val newTheme =
            CustomBrandThemeSettings(
                displayName = "custom2",
                primaryHex = "#101014",
                secondaryHex = "#78D6FF",
                outlineHexOrNull = "#303846",
            )

        val preview =
            buildCustomBrandThemeBatchImportPreview(
                existing = listOf(existing),
                imported = listOf(duplicate, newTheme),
                mode = DuplicateImportMode.Brand,
            )

        assertEquals(2, preview.totalCount)
        assertEquals(1, preview.duplicateCount)
        assertEquals(1, preview.newCount)
        assertEquals(listOf(duplicate, newTheme), preview.importedSettings)
    }

    @Test
    fun `batch dual tone import preview treats same name with different colors as new`() {
        val existing =
            CustomBrandThemeSettings(
                presetId = "custom1",
                displayName = "custom1",
                primaryHex = "#E5E9F0",
                secondaryHex = "#4C566A",
                outlineHexOrNull = "#2E3440",
            )
        val imported =
            CustomBrandThemeSettings(
                displayName = "custom1",
                primaryHex = "#3D0101",
                secondaryHex = "#FF9500",
                outlineHexOrNull = "#FFCC00",
            )

        val preview =
            buildCustomBrandThemeBatchImportPreview(
                existing = listOf(existing),
                imported = listOf(imported),
                mode = DuplicateImportMode.Brand,
            )

        assertEquals(1, preview.totalCount)
        assertEquals(0, preview.duplicateCount)
        assertEquals(1, preview.newCount)
    }

    @Test
    fun `batch material import preview treats matching name and primary as duplicates`() {
        val existing =
            CustomBrandThemeSettings(
                presetId = "custom1",
                displayName = "custom1",
                primaryHex = "#E5E9F0",
            )
        val imported =
            CustomBrandThemeSettings(
                displayName = "custom1",
                primaryHex = "#E5E9F0",
            )

        val preview =
            buildCustomBrandThemeBatchImportPreview(
                existing = listOf(existing),
                imported = listOf(imported),
                mode = DuplicateImportMode.Material,
            )

        assertEquals(1, preview.totalCount)
        assertEquals(1, preview.duplicateCount)
        assertEquals(0, preview.newCount)
    }

    @Test
    fun `batch material import preview treats same name with different primary as new`() {
        val existing =
            CustomBrandThemeSettings(
                presetId = "custom1",
                displayName = "custom1",
                primaryHex = "#E5E9F0",
            )
        val imported =
            CustomBrandThemeSettings(
                displayName = "custom1",
                primaryHex = "#3D0101",
            )

        val preview =
            buildCustomBrandThemeBatchImportPreview(
                existing = listOf(existing),
                imported = listOf(imported),
                mode = DuplicateImportMode.Material,
            )

        assertEquals(1, preview.totalCount)
        assertEquals(0, preview.duplicateCount)
        assertEquals(1, preview.newCount)
    }

    @Test
    fun `formats malformed line import error`() {
        assertEquals(
            "Line 3 must use key=value.",
            formatCustomThemeImportErrorMessage(
                context,
                CustomThemeImportError.MalformedLine(lineNumber = 3),
            ),
        )
    }

    @Test
    fun `formats missing field import error`() {
        assertEquals(
            "Group 2 is missing secondary.",
            formatCustomThemeImportErrorMessage(
                context,
                CustomThemeImportError.MissingField(blockIndex = 2, field = "secondary"),
            ),
        )
    }

    @Test
    fun `formats invalid hex import error`() {
        assertEquals(
            "Group 1 has an invalid primary hex color: #xyzxyz.",
            formatCustomThemeImportErrorMessage(
                context,
                CustomThemeImportError.InvalidHex(
                    blockIndex = 1,
                    field = "primary",
                    value = "#xyzxyz",
                ),
            ),
        )
    }

    @Test
    fun `formats unknown field import error`() {
        assertEquals(
            "Group 1 has an unsupported field: tertiary.",
            formatCustomThemeImportErrorMessage(
                context,
                CustomThemeImportError.UnknownField(blockIndex = 1, field = "tertiary"),
            ),
        )
    }

    @Test
    fun `formats wrong target dual tone import error`() {
        assertEquals(
            "Group 1 uses custom dual-tone fields. Import it from custom dual-tone instead.",
            formatCustomThemeImportErrorMessage(
                context,
                CustomThemeImportError.WrongImportMode(
                    blockIndex = 1,
                    expectedMode = CustomThemeImportMode.Material,
                    detectedMode = CustomThemeImportMode.DualTone,
                ),
            ),
        )
    }

    @Test
    fun `formats wrong target material import error`() {
        assertEquals(
            "Group 1 uses custom color fields. Import it from custom colors instead.",
            formatCustomThemeImportErrorMessage(
                context,
                CustomThemeImportError.WrongImportMode(
                    blockIndex = 1,
                    expectedMode = CustomThemeImportMode.DualTone,
                    detectedMode = CustomThemeImportMode.Material,
                ),
            ),
        )
    }

    @Test
    fun `formats duplicate field import error with group wording`() {
        assertEquals(
            "Group 3 repeats primary.",
            formatCustomThemeImportErrorMessage(
                context,
                CustomThemeImportError.DuplicateField(blockIndex = 3, field = "primary"),
            ),
        )
    }
}

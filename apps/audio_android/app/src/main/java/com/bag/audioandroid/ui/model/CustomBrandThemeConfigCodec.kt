package com.bag.audioandroid.ui.model

import com.bag.audioandroid.ui.theme.normalizeBrandThemeHex
import com.bag.audioandroid.ui.theme.normalizeBrandThemeHexOrNull

fun CustomBrandThemeSettings.toConfigText(): String = listOf(this).toBatchConfigText()

fun CustomBrandThemeSettings.toMaterialConfigText(): String = listOf(this).toMaterialBatchConfigText()

fun List<CustomBrandThemeSettings>.toBatchConfigText(): String =
    joinToString(separator = "\n\n") { settings ->
        buildString { appendThemeBlock(settings) }.trimEnd()
    }

fun List<CustomBrandThemeSettings>.toMaterialBatchConfigText(): String =
    joinToString(separator = "\n\n") { settings ->
        buildString {
            appendLine("name=${settings.displayName.trim()}")
            appendLine("primary=${settings.primaryHex.toExportHex()}")
        }.trimEnd()
    }

private fun StringBuilder.appendThemeBlock(settings: CustomBrandThemeSettings) {
    appendLine("name=${settings.displayName.trim()}")
    appendLine("primary=${settings.primaryHex.toExportHex()}")
    appendLine("secondary=${settings.secondaryHex.toExportHex()}")
    appendLine("outline=${settings.outlineHexOrNull?.toExportHex().orEmpty()}")
}

fun parseCustomBrandThemeImportText(text: String): CustomBrandThemeImportParseResult =
    parseCustomThemeImportText(text, CustomThemeImportMode.DualTone)

fun parseCustomMaterialThemeImportText(text: String): CustomBrandThemeImportParseResult =
    parseCustomThemeImportText(text, CustomThemeImportMode.Material)

enum class CustomThemeImportMode(
    val allowedFields: Set<String>,
    val requiredFields: Set<String>,
) {
    DualTone(
        allowedFields = setOf("name", "primary", "secondary", "outline"),
        requiredFields = setOf("name", "primary", "secondary"),
    ),
    Material(
        allowedFields = setOf("name", "primary"),
        requiredFields = setOf("name", "primary"),
    ),
}

private fun parseCustomThemeImportText(
    text: String,
    mode: CustomThemeImportMode,
): CustomBrandThemeImportParseResult {
    val rawLines =
        text
            .lineSequence()
            .mapIndexed { index, line -> IndexedImportLine(index + 1, line.trim()) }
            .toList()
    val themes = mutableListOf<CustomBrandThemeSettings>()
    val block = linkedMapOf<String, String>()
    var sawContent = false

    fun currentBlockIndex(): Int = themes.size + 1

    fun flushBlock(): CustomBrandThemeImportParseResult.Invalid? {
        if (block.isEmpty()) {
            return null
        }
        val name = block["name"]?.trim().orEmpty()
        if (name.isBlank()) {
            return CustomBrandThemeImportParseResult.Invalid(
                CustomThemeImportError.MissingField(currentBlockIndex(), "name"),
            )
        }
        mode.requiredFields.forEach { field ->
            if (block[field].isNullOrBlank()) {
                if (
                    mode == CustomThemeImportMode.DualTone &&
                    field == "secondary" &&
                    block["primary"]?.isNotBlank() == true &&
                    block["outline"].isNullOrBlank()
                ) {
                    return CustomBrandThemeImportParseResult.Invalid(
                        CustomThemeImportError.WrongImportMode(
                            blockIndex = currentBlockIndex(),
                            expectedMode = mode,
                            detectedMode = CustomThemeImportMode.Material,
                        ),
                    )
                }
                return CustomBrandThemeImportParseResult.Invalid(
                    CustomThemeImportError.MissingField(currentBlockIndex(), field),
                )
            }
        }
        if (
            mode == CustomThemeImportMode.Material &&
            (block["secondary"]?.isNotBlank() == true || block["outline"]?.isNotBlank() == true)
        ) {
            return CustomBrandThemeImportParseResult.Invalid(
                CustomThemeImportError.WrongImportMode(
                    blockIndex = currentBlockIndex(),
                    expectedMode = mode,
                    detectedMode = CustomThemeImportMode.DualTone,
                ),
            )
        }
        val primary = normalizeBrandThemeHex(block["primary"].orEmpty())
        if (primary == null) {
            return CustomBrandThemeImportParseResult.Invalid(
                CustomThemeImportError.InvalidHex(
                    blockIndex = currentBlockIndex(),
                    field = "primary",
                    value = block["primary"].orEmpty(),
                ),
            )
        }
        val secondary =
            if (mode == CustomThemeImportMode.DualTone) {
                normalizeBrandThemeHex(block["secondary"].orEmpty()) ?: return CustomBrandThemeImportParseResult.Invalid(
                    CustomThemeImportError.InvalidHex(
                        blockIndex = currentBlockIndex(),
                        field = "secondary",
                        value = block["secondary"].orEmpty(),
                    ),
                )
            } else {
                ""
            }
        val outlineRaw = block["outline"].orEmpty()
        val outline = normalizeBrandThemeHexOrNull(outlineRaw)
        if (outlineRaw.isNotBlank() && outline == null) {
            return CustomBrandThemeImportParseResult.Invalid(
                CustomThemeImportError.InvalidHex(
                    blockIndex = currentBlockIndex(),
                    field = "outline",
                    value = outlineRaw,
                ),
            )
        }
        themes +=
            CustomBrandThemeSettings(
                displayName = name,
                primaryHex = primary,
                secondaryHex = secondary,
                outlineHexOrNull = outline,
            )
        block.clear()
        return null
    }

    rawLines.forEach { line ->
        if (line.value.isEmpty() || line.value.startsWith("#")) {
            return@forEach
        }
        sawContent = true
        val separatorIndex = line.value.indexOf('=')
        if (separatorIndex <= 0) {
            return CustomBrandThemeImportParseResult.Invalid(
                CustomThemeImportError.MalformedLine(line.number),
            )
        }
        val key =
            line.value
                .substring(0, separatorIndex)
                .trim()
                .lowercase()
        val value = line.value.substring(separatorIndex + 1).trim()
        if (key == "name" && block.isNotEmpty()) {
            val flushError = flushBlock()
            if (flushError != null) {
                return flushError
            }
        }
        if (
            mode == CustomThemeImportMode.Material &&
            (key == "secondary" || key == "outline")
        ) {
            return CustomBrandThemeImportParseResult.Invalid(
                CustomThemeImportError.WrongImportMode(
                    blockIndex = currentBlockIndex(),
                    expectedMode = mode,
                    detectedMode = CustomThemeImportMode.DualTone,
                ),
            )
        }
        if (key !in mode.allowedFields) {
            return CustomBrandThemeImportParseResult.Invalid(
                CustomThemeImportError.UnknownField(currentBlockIndex(), key),
            )
        }
        if (block.containsKey(key)) {
            return CustomBrandThemeImportParseResult.Invalid(
                CustomThemeImportError.DuplicateField(currentBlockIndex(), key),
            )
        }
        block[key] = value
    }
    val flushError = flushBlock()
    if (flushError != null) {
        return flushError
    }
    if (!sawContent || themes.isEmpty()) {
        return CustomBrandThemeImportParseResult.Invalid(CustomThemeImportError.EmptyInput)
    }
    return CustomBrandThemeImportParseResult.Valid(themes)
}

private fun String.toExportHex(): String = normalizeBrandThemeHex(this) ?: this

fun parseCustomBrandThemeConfig(text: String): CustomBrandThemeConfigParseResult {
    val result = parseCustomBrandThemeImportText(text)
    return if (result is CustomBrandThemeImportParseResult.Valid && result.settings.size == 1) {
        CustomBrandThemeConfigParseResult.Valid(result.settings.single())
    } else {
        CustomBrandThemeConfigParseResult.Invalid
    }
}

fun CustomBrandThemeSettings.hasSameConfigAs(other: CustomBrandThemeSettings): Boolean =
    displayName.trim() == other.displayName.trim() &&
        primaryHex.equals(other.primaryHex, ignoreCase = true) &&
        secondaryHex.equals(other.secondaryHex, ignoreCase = true) &&
        (outlineHexOrNull ?: "").equals(other.outlineHexOrNull ?: "", ignoreCase = true)

fun CustomBrandThemeSettings.hasSameMaterialImportConfigAs(other: CustomBrandThemeSettings): Boolean {
    val normalizedCurrentPrimary = normalizeBrandThemeHex(primaryHex)
    val normalizedOtherPrimary = normalizeBrandThemeHex(other.primaryHex)
    return displayName.trim() == other.displayName.trim() &&
        normalizedCurrentPrimary != null &&
        normalizedCurrentPrimary.equals(normalizedOtherPrimary, ignoreCase = true)
}

private fun CustomBrandThemeSettings.matchesImportedPresetTarget(other: CustomBrandThemeSettings): Boolean =
    displayName.trim().equals(other.displayName.trim(), ignoreCase = true)

fun findDuplicateImportedThemePresetId(
    existing: List<CustomBrandThemeSettings>,
    imported: CustomBrandThemeSettings,
    mode: CustomThemeImportMode,
): String? =
    existing
        .firstOrNull { preset ->
            when (mode) {
                CustomThemeImportMode.DualTone -> preset.matchesImportedPresetTarget(imported)
                CustomThemeImportMode.Material -> preset.matchesImportedPresetTarget(imported)
            }
        }?.presetId

sealed interface CustomBrandThemeConfigParseResult {
    data class Valid(
        val settings: CustomBrandThemeSettings,
    ) : CustomBrandThemeConfigParseResult

    data object Invalid : CustomBrandThemeConfigParseResult
}

sealed interface CustomBrandThemeImportParseResult {
    data class Valid(
        val settings: List<CustomBrandThemeSettings>,
    ) : CustomBrandThemeImportParseResult

    data class Invalid(
        val error: CustomThemeImportError,
    ) : CustomBrandThemeImportParseResult
}

data class IndexedImportLine(
    val number: Int,
    val value: String,
)

sealed interface CustomThemeImportError {
    data object EmptyInput : CustomThemeImportError

    data class MalformedLine(
        val lineNumber: Int,
    ) : CustomThemeImportError

    data class UnknownField(
        val blockIndex: Int,
        val field: String,
    ) : CustomThemeImportError

    data class DuplicateField(
        val blockIndex: Int,
        val field: String,
    ) : CustomThemeImportError

    data class MissingField(
        val blockIndex: Int,
        val field: String,
    ) : CustomThemeImportError

    data class InvalidHex(
        val blockIndex: Int,
        val field: String,
        val value: String,
    ) : CustomThemeImportError

    data class WrongImportMode(
        val blockIndex: Int,
        val expectedMode: CustomThemeImportMode,
        val detectedMode: CustomThemeImportMode,
    ) : CustomThemeImportError
}

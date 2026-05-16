package com.bag.audioandroid.ui.model

import com.bag.audioandroid.ui.theme.normalizeBrandThemeHex
import com.bag.audioandroid.ui.theme.normalizeBrandThemeHexOrNull

fun CustomBrandThemeSettings.toConfigText(): String = listOf(this).toBatchConfigText()

fun List<CustomBrandThemeSettings>.toBatchConfigText(): String =
    joinToString(separator = "\n\n") { settings ->
        buildString { appendThemeBlock(settings) }.trimEnd()
    }

private fun StringBuilder.appendThemeBlock(settings: CustomBrandThemeSettings) {
    appendLine("name=${settings.displayName.trim()}")
    appendLine("primary=${settings.primaryHex.toExportHex()}")
    appendLine("secondary=${settings.secondaryHex.toExportHex()}")
    appendLine("outline=${settings.outlineHexOrNull?.toExportHex().orEmpty()}")
}

private fun parseCustomBrandThemeBlock(values: Map<String, String>): CustomBrandThemeConfigParseResult {
    val name = values["name"]?.trim().orEmpty()
    val primary = normalizeBrandThemeHex(values["primary"].orEmpty())
    val secondary = normalizeBrandThemeHex(values["secondary"].orEmpty())
    val outlineRaw = values["outline"].orEmpty()
    val outline = normalizeBrandThemeHexOrNull(outlineRaw)
    val hasInvalidRequiredFields = name.isBlank() || primary == null || secondary == null
    val hasInvalidOutline = outlineRaw.isNotBlank() && outline == null
    if (hasInvalidRequiredFields || hasInvalidOutline) {
        return CustomBrandThemeConfigParseResult.Invalid
    }
    return CustomBrandThemeConfigParseResult.Valid(
        CustomBrandThemeSettings(
            displayName = name,
            primaryHex = primary,
            secondaryHex = secondary,
            outlineHexOrNull = outline,
        ),
    )
}

fun parseCustomBrandThemeImportText(text: String): CustomBrandThemeImportParseResult {
    val rawLines =
        text
            .lineSequence()
            .map { it.trim() }
            .toList()
    val themes = mutableListOf<CustomBrandThemeSettings>()
    val block = linkedMapOf<String, String>()

    fun flushBlock(): Boolean {
        if (block.isEmpty()) {
            return true
        }
        val result = parseCustomBrandThemeBlock(block)
        if (result !is CustomBrandThemeConfigParseResult.Valid) {
            return false
        }
        themes += result.settings
        block.clear()
        return true
    }

    rawLines.forEach { line ->
        if (line.isEmpty() || line.startsWith("# ") || line == "#") {
            return@forEach
        }
        val separatorIndex = line.indexOf('=')
        if (separatorIndex <= 0) {
            return@forEach
        }
        val key = line.substring(0, separatorIndex).trim().lowercase()
        val value = line.substring(separatorIndex + 1).trim()
        if (key == "name" && block.isNotEmpty()) {
            if (!flushBlock()) {
                return CustomBrandThemeImportParseResult.Invalid
            }
        }
        when (key) {
            "name", "primary", "secondary", "outline" -> block[key] = value
        }
    }
    if (!flushBlock() || themes.isEmpty()) {
        return CustomBrandThemeImportParseResult.Invalid
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

    data object Invalid : CustomBrandThemeImportParseResult
}

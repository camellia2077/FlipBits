package com.bag.audioandroid.ui.model

import com.bag.audioandroid.ui.theme.normalizeBrandThemeHex
import com.bag.audioandroid.ui.theme.normalizeBrandThemeHexOrNull

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
    val scanner = CustomThemeImportScanner(text)
    val groupedFields = mutableListOf<LinkedHashMap<String, String>>()
    val currentGroup = linkedMapOf<String, String>()

    fun currentGroupIndex(): Int = groupedFields.size + 1

    fun addField(
        key: String,
        value: String,
    ): CustomBrandThemeImportParseResult.Invalid? {
        if (key == "name" && currentGroup.isNotEmpty()) {
            groupedFields += LinkedHashMap(currentGroup)
            currentGroup.clear()
        }
        if (
            mode == CustomThemeImportMode.Material &&
            (key == "secondary" || key == "outline")
        ) {
            return CustomBrandThemeImportParseResult.Invalid(
                CustomThemeImportError.WrongImportMode(
                    blockIndex = currentGroupIndex(),
                    expectedMode = mode,
                    detectedMode = CustomThemeImportMode.DualTone,
                ),
            )
        }
        if (key !in mode.allowedFields) {
            return CustomBrandThemeImportParseResult.Invalid(
                CustomThemeImportError.UnknownField(currentGroupIndex(), key),
            )
        }
        if (currentGroup.containsKey(key)) {
            return CustomBrandThemeImportParseResult.Invalid(
                CustomThemeImportError.DuplicateField(currentGroupIndex(), key),
            )
        }
        currentGroup[key] = value
        return null
    }

    scanner
        .scan(
            onFields = { fields ->
                var fieldError: CustomBrandThemeImportParseResult.Invalid? = null
                fields.forEach { (key, value) ->
                    val error = addField(key, value)
                    if (error != null) {
                        fieldError = error
                        return@forEach
                    }
                }
                if (fieldError != null) {
                    return@scan fieldError
                }
                if (fields.size > 1) {
                    groupedFields += LinkedHashMap(currentGroup)
                    currentGroup.clear()
                }
                null
            },
            currentGroupIndex = ::currentGroupIndex,
        )?.let { return it }

    if (currentGroup.isNotEmpty()) {
        groupedFields += LinkedHashMap(currentGroup)
    }
    if (!scanner.sawContent || groupedFields.isEmpty()) {
        return CustomBrandThemeImportParseResult.Invalid(CustomThemeImportError.EmptyInput)
    }
    return validateImportedThemeGroups(groupedFields, mode)
}

private fun validateImportedThemeGroups(
    groups: List<Map<String, String>>,
    mode: CustomThemeImportMode,
): CustomBrandThemeImportParseResult {
    val themes = mutableListOf<CustomBrandThemeSettings>()
    groups.forEachIndexed { index, group ->
        val blockIndex = index + 1
        validateThemeGroup(group, mode, blockIndex)?.let { return it }
        val primary = normalizeBrandThemeHex(group["primary"].orEmpty())!!
        val secondary =
            if (mode == CustomThemeImportMode.DualTone) {
                normalizeBrandThemeHex(group["secondary"].orEmpty())!!
            } else {
                ""
            }
        val outline = normalizeBrandThemeHexOrNull(group["outline"].orEmpty())
        themes +=
            CustomBrandThemeSettings(
                displayName = group["name"]!!.trim(),
                primaryHex = primary,
                secondaryHex = secondary,
                outlineHexOrNull = outline,
            )
    }
    return CustomBrandThemeImportParseResult.Valid(themes)
}

private fun validateThemeGroup(
    group: Map<String, String>,
    mode: CustomThemeImportMode,
    blockIndex: Int,
): CustomBrandThemeImportParseResult.Invalid? {
    val name = group["name"]?.trim().orEmpty()
    if (name.isBlank()) {
        return CustomBrandThemeImportParseResult.Invalid(
            CustomThemeImportError.MissingField(blockIndex, "name"),
        )
    }
    mode.requiredFields.forEach { field ->
        if (group[field].isNullOrBlank()) {
            if (
                mode == CustomThemeImportMode.DualTone &&
                field == "secondary" &&
                group["primary"]?.isNotBlank() == true &&
                group["outline"].isNullOrBlank()
            ) {
                return CustomBrandThemeImportParseResult.Invalid(
                    CustomThemeImportError.WrongImportMode(
                        blockIndex = blockIndex,
                        expectedMode = mode,
                        detectedMode = CustomThemeImportMode.Material,
                    ),
                )
            }
            return CustomBrandThemeImportParseResult.Invalid(
                CustomThemeImportError.MissingField(blockIndex, field),
            )
        }
    }
    if (
        mode == CustomThemeImportMode.Material &&
        (group["secondary"]?.isNotBlank() == true || group["outline"]?.isNotBlank() == true)
    ) {
        return CustomBrandThemeImportParseResult.Invalid(
            CustomThemeImportError.WrongImportMode(
                blockIndex = blockIndex,
                expectedMode = mode,
                detectedMode = CustomThemeImportMode.DualTone,
            ),
        )
    }
    validateHexField(group, blockIndex, "primary", allowBlank = false)?.let { return it }
    if (mode == CustomThemeImportMode.DualTone) {
        validateHexField(group, blockIndex, "secondary", allowBlank = false)?.let { return it }
    }
    validateHexField(group, blockIndex, "outline", allowBlank = true)?.let { return it }
    return null
}

private fun validateHexField(
    group: Map<String, String>,
    blockIndex: Int,
    field: String,
    allowBlank: Boolean,
): CustomBrandThemeImportParseResult.Invalid? {
    val value = group[field].orEmpty()
    if (allowBlank && value.isBlank()) {
        return null
    }
    val normalized =
        if (allowBlank) {
            normalizeBrandThemeHexOrNull(value)
        } else {
            normalizeBrandThemeHex(value)
        }
    if (normalized == null) {
        return CustomBrandThemeImportParseResult.Invalid(
            CustomThemeImportError.InvalidHex(
                blockIndex = blockIndex,
                field = field,
                value = value,
            ),
        )
    }
    return null
}

private class CustomThemeImportScanner(
    text: String,
) {
    private val rawLines =
        text
            .lineSequence()
            .mapIndexed { index, line -> IndexedImportLine(index + 1, line.trim()) }
            .toList()

    var sawContent: Boolean = false
        private set

    fun scan(
        onFields: (List<Pair<String, String>>) -> CustomBrandThemeImportParseResult.Invalid?,
        currentGroupIndex: () -> Int,
    ): CustomBrandThemeImportParseResult.Invalid? {
        rawLines.forEach { line ->
            if (line.value.isEmpty() || line.value.startsWith("#")) {
                return@forEach
            }
            sawContent = true
            val parsedFields = parseImportFields(line.value)
            if (parsedFields == null) {
                return parseLineFailure(line, currentGroupIndex())
            }
            onFields(parsedFields)?.let { return it }
        }
        return null
    }

    private fun parseLineFailure(
        line: IndexedImportLine,
        blockIndex: Int,
    ): CustomBrandThemeImportParseResult.Invalid {
        val unknownKey =
            Regex("""([A-Za-z]+)\s*=""")
                .find(line.value)
                ?.groupValues
                ?.get(1)
                ?.lowercase()
        return if (unknownKey != null) {
            CustomBrandThemeImportParseResult.Invalid(
                CustomThemeImportError.UnknownField(blockIndex, unknownKey),
            )
        } else {
            CustomBrandThemeImportParseResult.Invalid(
                CustomThemeImportError.MalformedLine(line.number),
            )
        }
    }
}

private data class IndexedImportLine(
    val number: Int,
    val value: String,
)

private val KnownImportKeys = setOf("name", "primary", "secondary", "outline")
private val ImportFieldPattern =
    Regex("""(name|primary|secondary|outline)\s*=""", RegexOption.IGNORE_CASE)

private fun parseImportFields(line: String): List<Pair<String, String>>? {
    val matches = ImportFieldPattern.findAll(line).toList()
    if (matches.isEmpty() || matches.first().range.first != 0) {
        return null
    }
    return matches.mapIndexed { index, match ->
        val key = match.groupValues[1].trim().lowercase()
        val valueStart = match.range.last + 1
        val nextMatchStart = matches.getOrNull(index + 1)?.range?.first ?: line.length
        val valueEndExclusive = findCompactNextKeyStart(line, valueStart, nextMatchStart)
        key to line.substring(valueStart, valueEndExclusive).trim()
    }
}

private fun findCompactNextKeyStart(
    line: String,
    valueStart: Int,
    fallbackEndExclusive: Int,
): Int {
    var candidateStart = valueStart
    while (candidateStart < fallbackEndExclusive) {
        val separatorIndex = line.indexOf('=', candidateStart)
        if (separatorIndex == -1 || separatorIndex >= fallbackEndExclusive) {
            break
        }
        val keyStart = separatorIndex - 1
        if (keyStart < valueStart) {
            candidateStart = separatorIndex + 1
            continue
        }
        var scanStart = keyStart
        while (scanStart >= valueStart && line[scanStart].isLetter()) {
            scanStart--
        }
        val possibleKeyStart = scanStart + 1
        val possibleKey = line.substring(possibleKeyStart, separatorIndex).lowercase()
        if (possibleKey in KnownImportKeys && possibleKeyStart > valueStart) {
            return possibleKeyStart
        }
        candidateStart = separatorIndex + 1
    }
    return fallbackEndExclusive
}

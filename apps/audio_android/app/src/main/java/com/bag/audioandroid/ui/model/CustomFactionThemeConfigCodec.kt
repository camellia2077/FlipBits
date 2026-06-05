package com.bag.audioandroid.ui.model

fun parseCustomFactionThemeConfig(text: String): CustomFactionThemeConfigParseResult {
    val result = parseCustomFactionThemeImportText(text)
    return if (result is CustomFactionThemeImportParseResult.Valid && result.settings.size == 1) {
        CustomFactionThemeConfigParseResult.Valid(result.settings.single())
    } else {
        CustomFactionThemeConfigParseResult.Invalid
    }
}

sealed interface CustomFactionThemeConfigParseResult {
    data class Valid(
        val settings: CustomFactionThemeSettings,
    ) : CustomFactionThemeConfigParseResult

    data object Invalid : CustomFactionThemeConfigParseResult
}

sealed interface CustomFactionThemeImportParseResult {
    data class Valid(
        val settings: List<CustomFactionThemeSettings>,
    ) : CustomFactionThemeImportParseResult

    data class Invalid(
        val error: CustomThemeImportError,
    ) : CustomFactionThemeImportParseResult
}

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

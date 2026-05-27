package com.bag.audioandroid.ui.model

fun parseCustomBrandThemeConfig(text: String): CustomBrandThemeConfigParseResult {
    val result = parseCustomBrandThemeImportText(text)
    return if (result is CustomBrandThemeImportParseResult.Valid && result.settings.size == 1) {
        CustomBrandThemeConfigParseResult.Valid(result.settings.single())
    } else {
        CustomBrandThemeConfigParseResult.Invalid
    }
}

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

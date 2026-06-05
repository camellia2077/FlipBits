package com.bag.audioandroid.ui.model

import com.bag.audioandroid.ui.theme.normalizeFactionThemeHex

fun CustomFactionThemeSettings.toConfigText(): String = listOf(this).toBatchConfigText()

fun CustomFactionThemeSettings.toMaterialConfigText(): String = listOf(this).toMaterialBatchConfigText()

fun List<CustomFactionThemeSettings>.toBatchConfigText(): String =
    joinToString(separator = "\n\n") { settings -> settings.toMultiLineDualToneConfig() }

fun List<FactionThemeExportEntry>.toFactionThemeExportBatchConfigText(): String =
    joinToString(separator = "\n\n") { settings -> settings.toMultiLineDualToneConfig() }

fun List<CustomFactionThemeSettings>.toMaterialBatchConfigText(): String =
    joinToString(separator = "\n\n") { settings -> settings.toMultiLineMaterialConfig() }

data class FactionThemeExportEntry(
    val displayName: String,
    val primaryHex: String,
    val secondaryHex: String,
    val outlineHexOrNull: String?,
)

private fun CustomFactionThemeSettings.toMultiLineDualToneConfig(): String =
    """
    name=${displayName.trim()}
    primary=${primaryHex.toExportHex()} secondary=${secondaryHex.toExportHex()} outline=${outlineHexOrNull?.toExportHex().orEmpty()}
    """.trimIndent()

private fun CustomFactionThemeSettings.toMultiLineMaterialConfig(): String =
    """
    name=${displayName.trim()}
    primary=${primaryHex.toExportHex()}
    """.trimIndent()

private fun FactionThemeExportEntry.toMultiLineDualToneConfig(): String =
    """
    name=${displayName.trim()}
    primary=${primaryHex.toExportHex()} secondary=${secondaryHex.toExportHex()} outline=${outlineHexOrNull?.toExportHex().orEmpty()}
    """.trimIndent()

private fun String.toExportHex(): String = normalizeFactionThemeHex(this) ?: this

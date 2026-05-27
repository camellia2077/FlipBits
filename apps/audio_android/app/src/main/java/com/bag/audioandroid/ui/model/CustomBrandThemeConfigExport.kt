package com.bag.audioandroid.ui.model

import com.bag.audioandroid.ui.theme.normalizeBrandThemeHex

fun CustomBrandThemeSettings.toConfigText(): String = listOf(this).toBatchConfigText()

fun CustomBrandThemeSettings.toMaterialConfigText(): String = listOf(this).toMaterialBatchConfigText()

fun List<CustomBrandThemeSettings>.toBatchConfigText(): String =
    joinToString(separator = "\n\n") { settings -> settings.toMultiLineDualToneConfig() }

fun List<BrandThemeExportEntry>.toBrandThemeExportBatchConfigText(): String =
    joinToString(separator = "\n\n") { settings -> settings.toMultiLineDualToneConfig() }

fun List<CustomBrandThemeSettings>.toMaterialBatchConfigText(): String =
    joinToString(separator = "\n\n") { settings -> settings.toMultiLineMaterialConfig() }

data class BrandThemeExportEntry(
    val displayName: String,
    val primaryHex: String,
    val secondaryHex: String,
    val outlineHexOrNull: String?,
)

private fun CustomBrandThemeSettings.toMultiLineDualToneConfig(): String =
    """
    name=${displayName.trim()}
    primary=${primaryHex.toExportHex()} secondary=${secondaryHex.toExportHex()} outline=${outlineHexOrNull?.toExportHex().orEmpty()}
    """.trimIndent()

private fun CustomBrandThemeSettings.toMultiLineMaterialConfig(): String =
    """
    name=${displayName.trim()}
    primary=${primaryHex.toExportHex()}
    """.trimIndent()

private fun BrandThemeExportEntry.toMultiLineDualToneConfig(): String =
    """
    name=${displayName.trim()}
    primary=${primaryHex.toExportHex()} secondary=${secondaryHex.toExportHex()} outline=${outlineHexOrNull?.toExportHex().orEmpty()}
    """.trimIndent()

private fun String.toExportHex(): String = normalizeBrandThemeHex(this) ?: this

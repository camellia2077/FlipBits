package com.bag.audioandroid.data

import com.bag.audioandroid.ui.model.CustomBrandThemeSettings
import org.json.JSONArray
import org.json.JSONObject

internal object CustomBrandThemeSettingsStore {
    private const val KeyPresetId = "preset_id"
    private const val KeyDisplayName = "display_name"
    private const val KeyBackgroundHex = "background_hex"
    private const val KeyAccentHex = "accent_hex"
    private const val KeyOutlineHex = "outline_hex"

    fun encode(settings: List<CustomBrandThemeSettings>): String =
        JSONArray().apply {
            settings.forEach { item ->
                put(
                    JSONObject()
                        .put(KeyPresetId, item.presetId)
                        .put(KeyDisplayName, item.displayName)
                        .put(KeyBackgroundHex, item.backgroundHex)
                        .put(KeyAccentHex, item.accentHex)
                        .put(KeyOutlineHex, item.outlineHexOrNull),
                )
            }
        }.toString()

    fun decode(rawValue: String?): List<CustomBrandThemeSettings> {
        if (rawValue.isNullOrBlank()) {
            return emptyList()
        }
        return runCatching {
            val array = JSONArray(rawValue)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val presetId = item.optString(KeyPresetId).trim()
                    val displayName = item.optString(KeyDisplayName).trim()
                    val backgroundHex = item.optString(KeyBackgroundHex).trim()
                    val accentHex = item.optString(KeyAccentHex).trim()
                    val outlineHex =
                        item
                            .optString(KeyOutlineHex)
                            .trim()
                            .ifBlank { null }
                    if (presetId.isEmpty() || displayName.isEmpty() || backgroundHex.isEmpty() || accentHex.isEmpty()) {
                        continue
                    }
                    add(
                        CustomBrandThemeSettings(
                            presetId = presetId,
                            displayName = displayName,
                            backgroundHex = backgroundHex,
                            accentHex = accentHex,
                            outlineHexOrNull = outlineHex,
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}

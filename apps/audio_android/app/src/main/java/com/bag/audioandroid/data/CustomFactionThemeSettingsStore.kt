package com.bag.audioandroid.data

import com.bag.audioandroid.ui.model.CustomFactionThemeSettings
import org.json.JSONArray
import org.json.JSONObject

internal object CustomFactionThemeSettingsStore {
    private const val KeyPresetId = "preset_id"
    private const val KeyDisplayName = "display_name"
    private const val KeyPrimaryHex = "primary_hex"
    private const val KeySecondaryHex = "secondary_hex"
    private const val KeyOutlineHex = "outline_hex"

    fun encode(settings: List<CustomFactionThemeSettings>): String =
        JSONArray()
            .apply {
                settings.forEach { item ->
                    put(
                        JSONObject()
                            .put(KeyPresetId, item.presetId)
                            .put(KeyDisplayName, item.displayName)
                            .put(KeyPrimaryHex, item.primaryHex)
                            .put(KeySecondaryHex, item.secondaryHex)
                            .put(KeyOutlineHex, item.outlineHexOrNull),
                    )
                }
            }.toString()

    fun decode(rawValue: String?): List<CustomFactionThemeSettings> {
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
                    val primaryHex = item.optString(KeyPrimaryHex).trim()
                    val secondaryHex = item.optString(KeySecondaryHex).trim()
                    val outlineHex =
                        item
                            .optString(KeyOutlineHex)
                            .trim()
                            .ifBlank { null }
                    if (presetId.isEmpty() || displayName.isEmpty() || primaryHex.isEmpty() || secondaryHex.isEmpty()) {
                        continue
                    }
                    add(
                        CustomFactionThemeSettings(
                            presetId = presetId,
                            displayName = displayName,
                            primaryHex = primaryHex,
                            secondaryHex = secondaryHex,
                            outlineHexOrNull = outlineHex,
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}

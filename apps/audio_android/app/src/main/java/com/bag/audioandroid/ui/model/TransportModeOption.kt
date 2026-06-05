package com.bag.audioandroid.ui.model

import androidx.annotation.StringRes
import com.bag.audioandroid.R

enum class TransportModeOption(
    val nativeValue: Int,
    val wireName: String,
    val fixedEnglishLabel: String,
    @param:StringRes val labelResId: Int,
    @param:StringRes val charsetHintResId: Int,
    @param:StringRes val exampleTextResId: Int,
) {
    Mini(
        nativeValue = 0,
        wireName = "mini",
        fixedEnglishLabel = "Mini",
        labelResId = R.string.transport_mode_mini_label,
        charsetHintResId = R.string.audio_transport_mini_hint,
        exampleTextResId = R.string.audio_transport_pro_example,
    ),
    Flash(
        nativeValue = 1,
        wireName = "flash",
        fixedEnglishLabel = "Flash",
        labelResId = R.string.transport_mode_flash_label,
        charsetHintResId = R.string.audio_transport_flash_hint,
        exampleTextResId = R.string.audio_transport_flash_example,
    ),
    Pro(
        nativeValue = 2,
        wireName = "pro",
        fixedEnglishLabel = "Pro",
        labelResId = R.string.transport_mode_pro_label,
        charsetHintResId = R.string.audio_transport_pro_hint,
        exampleTextResId = R.string.audio_transport_pro_example,
    ),
    Ultra(
        nativeValue = 3,
        wireName = "ultra",
        fixedEnglishLabel = "Ultra",
        labelResId = R.string.transport_mode_ultra_label,
        charsetHintResId = R.string.audio_transport_ultra_hint,
        exampleTextResId = R.string.audio_transport_ultra_example,
    ),
    ;

    companion object {
        fun fromWireName(wireName: String?): TransportModeOption? {
            val normalized = wireName?.trim().orEmpty()
            if (normalized.isEmpty()) {
                return null
            }
            return entries.firstOrNull { option ->
                option.wireName == normalized.lowercase() ||
                    option.fixedEnglishLabel.equals(normalized, ignoreCase = true)
            }
        }
    }
}

package com.bag.audioandroid.ui.model

import androidx.annotation.StringRes
import com.bag.audioandroid.R

enum class FlashVoicingStyleOption(
    val id: String,
    val signalProfileValue: Int,
    val voicingFlavorValue: Int,
    @param:StringRes val labelResId: Int,
    @param:StringRes val descriptionResId: Int,
) {
    CodedBurst(
        id = "coded_burst",
        signalProfileValue = 0,
        voicingFlavorValue = 0,
        labelResId = R.string.config_flash_style_coded_burst_label,
        descriptionResId = R.string.config_flash_style_coded_burst_description,
    ),
    RitualChant(
        id = "ritual_chant",
        signalProfileValue = 1,
        voicingFlavorValue = 1,
        labelResId = R.string.config_flash_style_ritual_chant_label,
        descriptionResId = R.string.config_flash_style_ritual_chant_description,
    ),
    DeepRitual(
        id = "deep_ritual",
        signalProfileValue = 2,
        voicingFlavorValue = 2,
        labelResId = R.string.config_flash_style_deep_ritual_label,
        descriptionResId = R.string.config_flash_style_deep_ritual_description,
    ),
    ;

    companion object {
        fun fromId(id: String?): FlashVoicingStyleOption = entries.firstOrNull { it.id == id } ?: CodedBurst
    }
}

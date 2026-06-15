package com.bag.audioandroid.ui.model

import androidx.annotation.StringRes
import com.bag.audioandroid.R

enum class VoiceFxSubvoiceStyleOption(
    val id: String,
    val nativeValue: Int,
    @param:StringRes val labelResId: Int,
    @param:StringRes val descriptionResId: Int,
) {
    Standard(
        id = "standard",
        nativeValue = 0,
        labelResId = R.string.config_flash_style_standard_label,
        descriptionResId = R.string.config_flash_style_standard_description,
    ),
    Litany(
        id = "litany",
        nativeValue = 1,
        labelResId = R.string.config_flash_style_litany_label,
        descriptionResId = R.string.config_flash_style_litany_description,
    ),
    Hostility(
        id = "hostility",
        nativeValue = 3,
        labelResId = R.string.config_flash_style_hostility_label,
        descriptionResId = R.string.config_flash_style_hostility_description,
    ),
    Collapse(
        id = "collapse",
        nativeValue = 4,
        labelResId = R.string.config_flash_style_collapse_label,
        descriptionResId = R.string.config_flash_style_collapse_description,
    ),
    Zeal(
        id = "zeal",
        nativeValue = 5,
        labelResId = R.string.config_flash_style_zeal_label,
        descriptionResId = R.string.config_flash_style_zeal_description,
    ),
    Void(
        id = "void",
        nativeValue = 6,
        labelResId = R.string.config_flash_style_void_label,
        descriptionResId = R.string.config_flash_style_void_description,
    ),
    ;

    companion object {
        fun fromId(id: String?): VoiceFxSubvoiceStyleOption = entries.firstOrNull { it.id == id } ?: Standard
    }
}

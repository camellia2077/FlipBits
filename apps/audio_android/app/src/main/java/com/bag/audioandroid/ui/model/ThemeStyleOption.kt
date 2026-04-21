package com.bag.audioandroid.ui.model

import androidx.annotation.StringRes
import com.bag.audioandroid.R

enum class ThemeStyleOption(
    val id: String,
    @param:StringRes val labelResId: Int,
) {
    Material(
        id = "material",
        labelResId = R.string.config_theme_style_material,
    ),
    BrandDualTone(
        id = "brand_dual_tone",
        labelResId = R.string.config_theme_style_brand_dual_tone,
    ),
    ;

    companion object {
        fun fromId(id: String?): ThemeStyleOption = entries.firstOrNull { it.id == id } ?: BrandDualTone
    }
}

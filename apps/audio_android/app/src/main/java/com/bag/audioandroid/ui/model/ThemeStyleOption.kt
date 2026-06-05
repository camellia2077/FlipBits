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
    FactionTheme(
        id = "faction_theme",
        labelResId = R.string.config_theme_style_faction_theme,
    ),
    ;

    companion object {
        fun fromId(id: String?): ThemeStyleOption = entries.firstOrNull { it.id == id } ?: FactionTheme
    }
}

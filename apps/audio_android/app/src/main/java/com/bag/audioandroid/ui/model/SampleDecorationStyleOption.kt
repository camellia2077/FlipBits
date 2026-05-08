package com.bag.audioandroid.ui.model

import androidx.annotation.StringRes
import com.bag.audioandroid.R

enum class SampleDecorationStyleOption(
    val id: String,
    @param:StringRes val labelResId: Int,
) {
    None(
        id = "none",
        labelResId = R.string.config_sample_decoration_style_none,
    ),
    Emoji(
        id = "emoji",
        labelResId = R.string.config_sample_decoration_style_emoji,
    ),
    ;

    companion object {
        fun fromId(id: String?): SampleDecorationStyleOption = entries.firstOrNull { it.id == id } ?: Emoji
    }
}

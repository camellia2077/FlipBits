package com.bag.audioandroid.ui.model

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

data class PaletteOption(
    val id: String,
    @param:StringRes val titleResId: Int,
    val previewColor: Color,
    val scheme: ColorScheme
)

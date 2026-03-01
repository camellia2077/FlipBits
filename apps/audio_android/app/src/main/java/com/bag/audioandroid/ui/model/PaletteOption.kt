package com.bag.audioandroid.ui.model

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

data class PaletteOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val previewColor: Color,
    val scheme: ColorScheme
)

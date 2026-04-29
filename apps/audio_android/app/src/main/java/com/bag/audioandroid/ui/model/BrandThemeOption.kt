package com.bag.audioandroid.ui.model

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

data class BrandThemeOption(
    val id: String,
    @param:StringRes val groupTitleResId: Int,
    @param:StringRes val titleResId: Int,
    @param:StringRes val descriptionResId: Int,
    @param:StringRes val accessibilityLabelResId: Int,
    val titleOverride: String? = null,
    val descriptionOverride: String? = null,
    val accessibilityLabelOverride: String? = null,
    val sampleFlavor: SampleFlavor,
    val backgroundColor: Color,
    val accentColor: Color,
    val outlineColor: Color,
    val colorScheme: ColorScheme,
) {
    val isDarkTheme: Boolean
        get() = backgroundColor.luminance() < 0.5f

    val primaryColor: Color
        get() = backgroundColor

    val secondaryColor: Color
        get() = accentColor
}

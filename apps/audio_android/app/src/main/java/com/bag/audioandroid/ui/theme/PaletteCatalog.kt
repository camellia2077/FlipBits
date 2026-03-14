package com.bag.audioandroid.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.PaletteOption

val MaterialPalettes = listOf(
    vividPalette(
        id = "ruby",
        titleResId = R.string.palette_ruby_title,
        primary = Color(0xFFC62828),
        primaryContainer = Color(0xFFFFDAD6),
        onPrimaryContainer = Color(0xFF410002),
        secondary = Color(0xFF8B4A47),
        tertiary = Color(0xFF7A5A12),
        background = Color(0xFFFFF8F7)
    ),
    vividPalette(
        id = "orange",
        titleResId = R.string.palette_orange_title,
        primary = Color(0xFFD95F02),
        primaryContainer = Color(0xFFFFDBC8),
        onPrimaryContainer = Color(0xFF341100),
        secondary = Color(0xFF81523C),
        tertiary = Color(0xFF715C2D),
        background = Color(0xFFFFF8F4)
    ),
    vividPalette(
        id = "amber",
        titleResId = R.string.palette_amber_title,
        primary = Color(0xFFA96800),
        primaryContainer = Color(0xFFFFDFAC),
        onPrimaryContainer = Color(0xFF321D00),
        secondary = Color(0xFF755A2F),
        tertiary = Color(0xFF5C6400),
        background = Color(0xFFFFF9F1)
    ),
    vividPalette(
        id = "lime",
        titleResId = R.string.palette_lime_title,
        primary = Color(0xFF5E8E00),
        primaryContainer = Color(0xFFD9F5A0),
        onPrimaryContainer = Color(0xFF192A00),
        secondary = Color(0xFF58653F),
        tertiary = Color(0xFF3A6D59),
        background = Color(0xFFFBFCEA)
    ),
    vividPalette(
        id = "emerald",
        titleResId = R.string.palette_emerald_title,
        primary = Color(0xFF008A24),
        primaryContainer = Color(0xFF9EF2A8),
        onPrimaryContainer = Color(0xFF002107),
        secondary = Color(0xFF486548),
        tertiary = Color(0xFF1F6D68),
        background = Color(0xFFF4FCF4)
    ),
    vividPalette(
        id = "teal",
        titleResId = R.string.palette_teal_title,
        primary = Color(0xFF00839B),
        primaryContainer = Color(0xFFABEEFF),
        onPrimaryContainer = Color(0xFF001F26),
        secondary = Color(0xFF496368),
        tertiary = Color(0xFF4F6090),
        background = Color(0xFFF2FCFF)
    ),
    vividPalette(
        id = "ocean",
        titleResId = R.string.palette_ocean_title,
        primary = Color(0xFF005CE6),
        primaryContainer = Color(0xFFDCE2FF),
        onPrimaryContainer = Color(0xFF001944),
        secondary = Color(0xFF505E7A),
        tertiary = Color(0xFF6B5778),
        background = Color(0xFFF7F9FF)
    ),
    vividPalette(
        id = "indigo",
        titleResId = R.string.palette_indigo_title,
        primary = Color(0xFF3D4FD4),
        primaryContainer = Color(0xFFDFE0FF),
        onPrimaryContainer = Color(0xFF00135F),
        secondary = Color(0xFF5A5E7A),
        tertiary = Color(0xFF7A536A),
        background = Color(0xFFF8F8FF)
    ),
    vividPalette(
        id = "violet",
        titleResId = R.string.palette_violet_title,
        primary = Color(0xFF7A2CF5),
        primaryContainer = Color(0xFFE9DCFF),
        onPrimaryContainer = Color(0xFF280055),
        secondary = Color(0xFF695A80),
        tertiary = Color(0xFF8A4E78),
        background = Color(0xFFFCF7FF)
    ),
    vividPalette(
        id = "magenta",
        titleResId = R.string.palette_magenta_title,
        primary = Color(0xFFC2188F),
        primaryContainer = Color(0xFFFFD7F0),
        onPrimaryContainer = Color(0xFF43002D),
        secondary = Color(0xFF82526E),
        tertiary = Color(0xFF8D4D4D),
        background = Color(0xFFFFF7FB)
    )
)

private fun vividPalette(
    id: String,
    titleResId: Int,
    primary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    secondary: Color,
    tertiary: Color,
    background: Color
): PaletteOption =
    PaletteOption(
        id = id,
        titleResId = titleResId,
        previewColor = primary,
        scheme = lightColorScheme(
            primary = primary,
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            tertiary = tertiary,
            background = background,
            surface = background
        )
    )

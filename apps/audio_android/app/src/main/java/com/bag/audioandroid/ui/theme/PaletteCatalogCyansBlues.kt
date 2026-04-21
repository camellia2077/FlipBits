package com.bag.audioandroid.ui.theme

import androidx.compose.ui.graphics.Color
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.PaletteFamily
import com.bag.audioandroid.ui.model.PaletteOption

internal val materialCyansBluesPalettes: List<PaletteOption> =
    listOf(
        vividPalette(
            id = "teal",
            family = PaletteFamily.CyansBlues,
            titleResId = R.string.palette_teal_title,
            primary = Color(0xFF00839B),
            primaryContainer = Color(0xFFABEEFF),
            secondary = Color(0xFF496368),
            tertiary = Color(0xFF4F6090),
            background = Color(0xFFF2FCFF),
            dark =
                darkSeed(
                    background = Color(0xFF0C1518),
                    surface = Color(0xFF152026),
                    primary = Color(0xFF6ADCF8),
                    secondary = Color(0xFFB4D0D5),
                    tertiary = Color(0xFFABC2F0),
                ),
        ),
        vividPalette(
            id = "cyan",
            family = PaletteFamily.CyansBlues,
            titleResId = R.string.palette_cyan_title,
            primary = Color(0xFF00A7C2),
            primaryContainer = Color(0xFFB8F4FF),
            secondary = Color(0xFF4B6870),
            tertiary = Color(0xFF506A97),
            background = Color(0xFFF1FCFF),
            dark =
                darkSeed(
                    background = Color(0xFF0A1418),
                    surface = Color(0xFF132026),
                    primary = Color(0xFF76E9FF),
                    secondary = Color(0xFFB7D6DC),
                    tertiary = Color(0xFFACC6F5),
                ),
        ),
        vividPalette(
            id = "sky",
            family = PaletteFamily.CyansBlues,
            titleResId = R.string.palette_sky_title,
            primary = Color(0xFF1A7BFF),
            primaryContainer = Color(0xFFD8E7FF),
            secondary = Color(0xFF51617D),
            tertiary = Color(0xFF6A5C8C),
            background = Color(0xFFF5F9FF),
            dark =
                darkSeed(
                    background = Color(0xFF0C131A),
                    surface = Color(0xFF14202A),
                    primary = Color(0xFF8AB8FF),
                    secondary = Color(0xFFC1CEE3),
                    tertiary = Color(0xFFD0B8FF),
                ),
        ),
        vividPalette(
            id = "ocean",
            family = PaletteFamily.CyansBlues,
            titleResId = R.string.palette_ocean_title,
            primary = Color(0xFF005CE6),
            primaryContainer = Color(0xFFDCE2FF),
            secondary = Color(0xFF505E7A),
            tertiary = Color(0xFF6B5778),
            background = Color(0xFFF7F9FF),
            dark =
                darkSeed(
                    background = Color(0xFF0B121A),
                    surface = Color(0xFF131C27),
                    primary = Color(0xFF8AB4FF),
                    secondary = Color(0xFFBECAE0),
                    tertiary = Color(0xFFD3B4DA),
                ),
        ),
        vividPalette(
            id = "cobalt",
            family = PaletteFamily.CyansBlues,
            titleResId = R.string.palette_cobalt_title,
            primary = Color(0xFF2563EB),
            primaryContainer = Color(0xFFDCE4FF),
            secondary = Color(0xFF56627D),
            tertiary = Color(0xFF6A5E90),
            background = Color(0xFFF6F9FF),
            dark =
                darkSeed(
                    background = Color(0xFF0C111B),
                    surface = Color(0xFF141B29),
                    primary = Color(0xFF92B3FF),
                    secondary = Color(0xFFC1CAE3),
                    tertiary = Color(0xFFD1C1FF),
                ),
        ),
        vividPalette(
            id = "indigo",
            family = PaletteFamily.CyansBlues,
            titleResId = R.string.palette_indigo_title,
            primary = Color(0xFF3D4FD4),
            primaryContainer = Color(0xFFDFE0FF),
            secondary = Color(0xFF5A5E7A),
            tertiary = Color(0xFF7A536A),
            background = Color(0xFFF8F8FF),
            dark =
                darkSeed(
                    background = Color(0xFF0F111A),
                    surface = Color(0xFF181A27),
                    primary = Color(0xFFB2BAFF),
                    secondary = Color(0xFFC8CAE0),
                    tertiary = Color(0xFFE1B4CC),
                ),
        ),
    )

package com.bag.audioandroid.ui.theme

import androidx.compose.ui.graphics.Color
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.PaletteFamily
import com.bag.audioandroid.ui.model.PaletteOption

internal val materialOrangePalettes: List<PaletteOption> =
    listOf(
        vividPalette(
            id = "apricot",
            family = PaletteFamily.Oranges,
            titleResId = R.string.palette_apricot_title,
            primary = Color(0xFFFF9E66),
            primaryContainer = Color(0xFFFFDDCB),
            secondary = Color(0xFF8D6656),
            tertiary = Color(0xFF8B6741),
            background = Color(0xFFFFF8F4),
            dark =
                darkSeed(
                    background = Color(0xFF1A120E),
                    surface = Color(0xFF261A15),
                    primary = Color(0xFFFFC09B),
                    secondary = Color(0xFFE7C0B0),
                    tertiary = Color(0xFFF1D39A),
                ),
        ),
        vividPalette(
            id = "coral",
            family = PaletteFamily.Oranges,
            titleResId = R.string.palette_coral_title,
            primary = Color(0xFFFF835C),
            primaryContainer = Color(0xFFFFDCCF),
            secondary = Color(0xFF985F4F),
            tertiary = Color(0xFF8B6640),
            background = Color(0xFFFFF8F6),
            dark =
                darkSeed(
                    background = Color(0xFF1A110E),
                    surface = Color(0xFF261915),
                    primary = Color(0xFFFFB79E),
                    secondary = Color(0xFFE8BEB3),
                    tertiary = Color(0xFFF2D29B),
                ),
        ),
        vividPalette(
            id = "persimmon",
            family = PaletteFamily.Oranges,
            titleResId = R.string.palette_persimmon_title,
            primary = Color(0xFFF26A1B),
            primaryContainer = Color(0xFFFFD9C3),
            secondary = Color(0xFF905D46),
            tertiary = Color(0xFF896139),
            background = Color(0xFFFFF8F4),
            dark =
                darkSeed(
                    background = Color(0xFF19110D),
                    surface = Color(0xFF251915),
                    primary = Color(0xFFFFB085),
                    secondary = Color(0xFFE4BFB1),
                    tertiary = Color(0xFFF0D092),
                ),
        ),
        vividPalette(
            id = "terracotta",
            family = PaletteFamily.Oranges,
            titleResId = R.string.palette_terracotta_title,
            primary = Color(0xFFD6723F),
            primaryContainer = Color(0xFFFFDCD0),
            secondary = Color(0xFF8C604D),
            tertiary = Color(0xFF85613E),
            background = Color(0xFFFFF8F5),
            dark =
                darkSeed(
                    background = Color(0xFF17120F),
                    surface = Color(0xFF231B17),
                    primary = Color(0xFFF3B08C),
                    secondary = Color(0xFFDDBFB1),
                    tertiary = Color(0xFFE8D1A0),
                ),
        ),
        vividPalette(
            id = "orange",
            family = PaletteFamily.Oranges,
            titleResId = R.string.palette_orange_title,
            primary = Color(0xFFF57C00),
            primaryContainer = Color(0xFFFFDCB5),
            secondary = Color(0xFF8C6140),
            tertiary = Color(0xFF876337),
            background = Color(0xFFFFF8F2),
            dark =
                darkSeed(
                    background = Color(0xFF18120D),
                    surface = Color(0xFF241A14),
                    primary = Color(0xFFFFB973),
                    secondary = Color(0xFFE4BF9F),
                    tertiary = Color(0xFFF0D08D),
                ),
        ),
        vividPalette(
            id = "tangerine",
            family = PaletteFamily.Oranges,
            titleResId = R.string.palette_tangerine_title,
            primary = Color(0xFFFF8F00),
            primaryContainer = Color(0xFFFFE0A8),
            secondary = Color(0xFF906338),
            tertiary = Color(0xFF886436),
            background = Color(0xFFFFF9F3),
            dark =
                darkSeed(
                    background = Color(0xFF19130C),
                    surface = Color(0xFF261C12),
                    primary = Color(0xFFFFC775),
                    secondary = Color(0xFFE9C6A2),
                    tertiary = Color(0xFFF2D38F),
                ),
        ),
    )

package com.bag.audioandroid.ui.theme

import androidx.compose.ui.graphics.Color
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.PaletteFamily
import com.bag.audioandroid.ui.model.PaletteOption

internal val materialOrangePalettes: List<PaletteOption> =
    listOf(
        vividPalette(
            id = "orange",
            family = PaletteFamily.Oranges,
            titleResId = R.string.palette_orange_title,
            primary = Color(0xFFD95F02),
            primaryContainer = Color(0xFFFFDBC8),
            secondary = Color(0xFF81523C),
            tertiary = Color(0xFF715C2D),
            background = Color(0xFFFFF8F4),
            dark =
                darkSeed(
                    background = Color(0xFF18120E),
                    surface = Color(0xFF241A15),
                    primary = Color(0xFFFFA55D),
                    secondary = Color(0xFFE1B79D),
                    tertiary = Color(0xFFF0CE82),
                ),
        ),
        vividPalette(
            id = "coral",
            family = PaletteFamily.Oranges,
            titleResId = R.string.palette_coral_title,
            primary = Color(0xFFFF6F61),
            primaryContainer = Color(0xFFFFDAD3),
            secondary = Color(0xFF9C5E58),
            tertiary = Color(0xFF8A6648),
            background = Color(0xFFFFF8F6),
            dark =
                darkSeed(
                    background = Color(0xFF1A110F),
                    surface = Color(0xFF261915),
                    primary = Color(0xFFFFAA9F),
                    secondary = Color(0xFFE5BBB4),
                    tertiary = Color(0xFFF0D09A),
                ),
        ),
        vividPalette(
            id = "tangerine",
            family = PaletteFamily.Oranges,
            titleResId = R.string.palette_tangerine_title,
            primary = Color(0xFFEB7A00),
            primaryContainer = Color(0xFFFFE0B2),
            secondary = Color(0xFF8A633A),
            tertiary = Color(0xFF73603D),
            background = Color(0xFFFFF9F3),
            dark =
                darkSeed(
                    background = Color(0xFF19130C),
                    surface = Color(0xFF261C13),
                    primary = Color(0xFFFFC16D),
                    secondary = Color(0xFFE7C29E),
                    tertiary = Color(0xFFF0D08E),
                ),
        ),
        vividPalette(
            id = "apricot",
            family = PaletteFamily.Oranges,
            titleResId = R.string.palette_apricot_title,
            primary = Color(0xFFFF8A65),
            primaryContainer = Color(0xFFFFDDCF),
            secondary = Color(0xFFA16659),
            tertiary = Color(0xFF8A6A49),
            background = Color(0xFFFFF8F5),
            dark =
                darkSeed(
                    background = Color(0xFF1A120F),
                    surface = Color(0xFF261A16),
                    primary = Color(0xFFFFB49E),
                    secondary = Color(0xFFE6C0B7),
                    tertiary = Color(0xFFF0D5A1),
                ),
        ),
        vividPalette(
            id = "terracotta",
            family = PaletteFamily.Oranges,
            titleResId = R.string.palette_terracotta_title,
            primary = Color(0xFFBF5B3D),
            primaryContainer = Color(0xFFFFDBCF),
            secondary = Color(0xFF86604F),
            tertiary = Color(0xFF7B6241),
            background = Color(0xFFFFF8F5),
            dark =
                darkSeed(
                    background = Color(0xFF17120F),
                    surface = Color(0xFF231B17),
                    primary = Color(0xFFE7AD92),
                    secondary = Color(0xFFDABFB3),
                    tertiary = Color(0xFFE5D1A1),
                ),
        ),
        vividPalette(
            id = "persimmon",
            family = PaletteFamily.Oranges,
            titleResId = R.string.palette_persimmon_title,
            primary = Color(0xFFE65100),
            primaryContainer = Color(0xFFFFDBCC),
            secondary = Color(0xFF8A5C4C),
            tertiary = Color(0xFF7E653A),
            background = Color(0xFFFFF8F4),
            dark =
                darkSeed(
                    background = Color(0xFF19110D),
                    surface = Color(0xFF251915),
                    primary = Color(0xFFFFAB7C),
                    secondary = Color(0xFFE3BEB0),
                    tertiary = Color(0xFFEECF91),
                ),
        ),
    )

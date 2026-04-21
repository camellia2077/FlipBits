package com.bag.audioandroid.ui.theme

import androidx.compose.ui.graphics.Color
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.PaletteFamily
import com.bag.audioandroid.ui.model.PaletteOption

internal val materialPurplesMagentasPalettes: List<PaletteOption> =
    listOf(
        vividPalette(
            id = "violet",
            family = PaletteFamily.PurplesMagentas,
            titleResId = R.string.palette_violet_title,
            primary = Color(0xFF7A2CF5),
            primaryContainer = Color(0xFFE9DCFF),
            secondary = Color(0xFF695A80),
            tertiary = Color(0xFF8A4E78),
            background = Color(0xFFFCF7FF),
            dark =
                darkSeed(
                    background = Color(0xFF13101A),
                    surface = Color(0xFF1E1827),
                    primary = Color(0xFFD2B5FF),
                    secondary = Color(0xFFD4C4EA),
                    tertiary = Color(0xFFF0B6DE),
                ),
        ),
        vividPalette(
            id = "plum",
            family = PaletteFamily.PurplesMagentas,
            titleResId = R.string.palette_plum_title,
            primary = Color(0xFF8E44AD),
            primaryContainer = Color(0xFFF0DBFF),
            secondary = Color(0xFF776084),
            tertiary = Color(0xFFA15C6C),
            background = Color(0xFFFDF7FF),
            dark =
                darkSeed(
                    background = Color(0xFF141018),
                    surface = Color(0xFF201925),
                    primary = Color(0xFFE0B8FF),
                    secondary = Color(0xFFD8C7E4),
                    tertiary = Color(0xFFF0BDCA),
                ),
        ),
        vividPalette(
            id = "magenta",
            family = PaletteFamily.PurplesMagentas,
            titleResId = R.string.palette_magenta_title,
            primary = Color(0xFFC2188F),
            primaryContainer = Color(0xFFFFD7F0),
            secondary = Color(0xFF82526E),
            tertiary = Color(0xFF8D4D4D),
            background = Color(0xFFFFF7FB),
            dark =
                darkSeed(
                    background = Color(0xFF170F15),
                    surface = Color(0xFF23171E),
                    primary = Color(0xFFFFA8E3),
                    secondary = Color(0xFFE0BCD2),
                    tertiary = Color(0xFFF0B9B4),
                ),
        ),
        vividPalette(
            id = "fuchsia",
            family = PaletteFamily.PurplesMagentas,
            titleResId = R.string.palette_fuchsia_title,
            primary = Color(0xFFE11D8D),
            primaryContainer = Color(0xFFFFD8EC),
            secondary = Color(0xFF92566F),
            tertiary = Color(0xFF92545C),
            background = Color(0xFFFFF7FB),
            dark =
                darkSeed(
                    background = Color(0xFF180F15),
                    surface = Color(0xFF24161D),
                    primary = Color(0xFFFFA9DD),
                    secondary = Color(0xFFE3BDD0),
                    tertiary = Color(0xFFF0C0BE),
                ),
        ),
        vividPalette(
            id = "orchid",
            family = PaletteFamily.PurplesMagentas,
            titleResId = R.string.palette_orchid_title,
            primary = Color(0xFF9C27B0),
            primaryContainer = Color(0xFFF5D8FF),
            secondary = Color(0xFF7E5B84),
            tertiary = Color(0xFFA45A70),
            background = Color(0xFFFFF7FF),
            dark =
                darkSeed(
                    background = Color(0xFF161019),
                    surface = Color(0xFF221925),
                    primary = Color(0xFFE8B3FF),
                    secondary = Color(0xFFDEC7E2),
                    tertiary = Color(0xFFF1BCD0),
                ),
        ),
        vividPalette(
            id = "lilac",
            family = PaletteFamily.PurplesMagentas,
            titleResId = R.string.palette_lilac_title,
            primary = Color(0xFFAA7CFF),
            primaryContainer = Color(0xFFEEDDFF),
            secondary = Color(0xFF75608A),
            tertiary = Color(0xFF9A6086),
            background = Color(0xFFFDFAFF),
            dark =
                darkSeed(
                    background = Color(0xFF14111A),
                    surface = Color(0xFF201927),
                    primary = Color(0xFFDFC2FF),
                    secondary = Color(0xFFD8CAE5),
                    tertiary = Color(0xFFF0C3DD),
                ),
        ),
    )

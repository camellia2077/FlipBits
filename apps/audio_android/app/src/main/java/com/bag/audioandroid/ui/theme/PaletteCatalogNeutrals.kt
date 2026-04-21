package com.bag.audioandroid.ui.theme

import androidx.compose.ui.graphics.Color
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.PaletteFamily
import com.bag.audioandroid.ui.model.PaletteOption

internal val materialNeutralPalettes: List<PaletteOption> =
    listOf(
        vividPalette(
            id = "mocha",
            family = PaletteFamily.Neutrals,
            titleResId = R.string.palette_mocha_title,
            primary = Color(0xFF8B5E3C),
            primaryContainer = Color(0xFFFFDCC2),
            secondary = Color(0xFF786252),
            tertiary = Color(0xFF8A5748),
            background = Color(0xFFFFF8F4),
            dark =
                darkSeed(
                    background = Color(0xFF16120F),
                    surface = Color(0xFF211A16),
                    primary = Color(0xFFE0B995),
                    secondary = Color(0xFFD1C0B1),
                    tertiary = Color(0xFFE6B7A9),
                ),
        ),
        vividPalette(
            id = "slate",
            family = PaletteFamily.Neutrals,
            titleResId = R.string.palette_slate_title,
            primary = Color(0xFF4B5563),
            primaryContainer = Color(0xFFDDE4EE),
            secondary = Color(0xFF657080),
            tertiary = Color(0xFF6E6383),
            background = Color(0xFFF7F8FB),
            dark =
                darkSeed(
                    background = Color(0xFF111317),
                    surface = Color(0xFF191C21),
                    primary = Color(0xFFB8C2D1),
                    secondary = Color(0xFFC8CFDB),
                    tertiary = Color(0xFFD3C5EA),
                ),
        ),
        vividPalette(
            id = "sand",
            family = PaletteFamily.Neutrals,
            titleResId = R.string.palette_sand_title,
            primary = Color(0xFF9C7B5B),
            primaryContainer = Color(0xFFF8DDC8),
            secondary = Color(0xFF7E6855),
            tertiary = Color(0xFF8B6248),
            background = Color(0xFFFFF9F5),
            dark =
                darkSeed(
                    background = Color(0xFF17120F),
                    surface = Color(0xFF221B17),
                    primary = Color(0xFFE7C5A5),
                    secondary = Color(0xFFD7C6B8),
                    tertiary = Color(0xFFE7BFAE),
                ),
        ),
        vividPalette(
            id = "stone",
            family = PaletteFamily.Neutrals,
            titleResId = R.string.palette_stone_title,
            primary = Color(0xFF7A6F63),
            primaryContainer = Color(0xFFE7DED6),
            secondary = Color(0xFF85776C),
            tertiary = Color(0xFF7E6B7F),
            background = Color(0xFFFAF8F6),
            dark =
                darkSeed(
                    background = Color(0xFF121110),
                    surface = Color(0xFF1C1A19),
                    primary = Color(0xFFCDC1B5),
                    secondary = Color(0xFFD4C7BD),
                    tertiary = Color(0xFFD8C4D8),
                ),
        ),
        vividPalette(
            id = "graphite",
            family = PaletteFamily.Neutrals,
            titleResId = R.string.palette_graphite_title,
            primary = Color(0xFF374151),
            primaryContainer = Color(0xFFD9E2F1),
            secondary = Color(0xFF5E6978),
            tertiary = Color(0xFF6F647F),
            background = Color(0xFFF7F8FB),
            dark =
                darkSeed(
                    background = Color(0xFF101317),
                    surface = Color(0xFF171C22),
                    primary = Color(0xFFAFBBCB),
                    secondary = Color(0xFFC5CEDA),
                    tertiary = Color(0xFFD2C7E3),
                ),
        ),
        vividPalette(
            id = "ivory",
            family = PaletteFamily.Neutrals,
            titleResId = R.string.palette_ivory_title,
            primary = Color(0xFFA89B8C),
            primaryContainer = Color(0xFFF4E6D7),
            secondary = Color(0xFF8B7C6F),
            tertiary = Color(0xFF907566),
            background = Color(0xFFFFFCF8),
            dark =
                darkSeed(
                    background = Color(0xFF151311),
                    surface = Color(0xFF1F1B18),
                    primary = Color(0xFFE1D1C1),
                    secondary = Color(0xFFD8CCC2),
                    tertiary = Color(0xFFE2CDC6),
                ),
        ),
    )

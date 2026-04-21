package com.bag.audioandroid.ui.theme

import androidx.compose.ui.graphics.Color
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.PaletteFamily
import com.bag.audioandroid.ui.model.PaletteOption

internal val materialYellowPalettes: List<PaletteOption> =
    listOf(
        vividPalette(
            id = "amber",
            family = PaletteFamily.Yellows,
            titleResId = R.string.palette_amber_title,
            primary = Color(0xFFA96800),
            primaryContainer = Color(0xFFFFDFAC),
            secondary = Color(0xFF755A2F),
            tertiary = Color(0xFF5C6400),
            background = Color(0xFFFFF9F1),
            dark =
                darkSeed(
                    background = Color(0xFF18140C),
                    surface = Color(0xFF241C12),
                    primary = Color(0xFFFFB95A),
                    secondary = Color(0xFFE2C38E),
                    tertiary = Color(0xFFD4DE7A),
                ),
        ),
        vividPalette(
            id = "gold",
            family = PaletteFamily.Yellows,
            titleResId = R.string.palette_gold_title,
            primary = Color(0xFFC08A00),
            primaryContainer = Color(0xFFFFE6B8),
            secondary = Color(0xFF816330),
            tertiary = Color(0xFF5E650A),
            background = Color(0xFFFFFAF2),
            dark =
                darkSeed(
                    background = Color(0xFF19150C),
                    surface = Color(0xFF261E12),
                    primary = Color(0xFFFFC867),
                    secondary = Color(0xFFE6CA96),
                    tertiary = Color(0xFFD7E585),
                ),
        ),
        vividPalette(
            id = "sunflower",
            family = PaletteFamily.Yellows,
            titleResId = R.string.palette_sunflower_title,
            primary = Color(0xFF8C7A00),
            primaryContainer = Color(0xFFF8E287),
            secondary = Color(0xFF716538),
            tertiary = Color(0xFF5C6800),
            background = Color(0xFFFFFBEA),
            dark =
                darkSeed(
                    background = Color(0xFF17140B),
                    surface = Color(0xFF231C12),
                    primary = Color(0xFFE7D067),
                    secondary = Color(0xFFD8CCA0),
                    tertiary = Color(0xFFCFD978),
                ),
        ),
        vividPalette(
            id = "lemon",
            family = PaletteFamily.Yellows,
            titleResId = R.string.palette_lemon_title,
            primary = Color(0xFF9E9100),
            primaryContainer = Color(0xFFFFF0A6),
            secondary = Color(0xFF746A35),
            tertiary = Color(0xFF606800),
            background = Color(0xFFFFFDEA),
            dark =
                darkSeed(
                    background = Color(0xFF17150B),
                    surface = Color(0xFF241E12),
                    primary = Color(0xFFF4E46F),
                    secondary = Color(0xFFDCD29A),
                    tertiary = Color(0xFFD8E27C),
                ),
        ),
        vividPalette(
            id = "honey",
            family = PaletteFamily.Yellows,
            titleResId = R.string.palette_honey_title,
            primary = Color(0xFFB37A00),
            primaryContainer = Color(0xFFFFDF9A),
            secondary = Color(0xFF7C5E2F),
            tertiary = Color(0xFF696300),
            background = Color(0xFFFFF9EE),
            dark =
                darkSeed(
                    background = Color(0xFF18130B),
                    surface = Color(0xFF241C12),
                    primary = Color(0xFFFFC56B),
                    secondary = Color(0xFFE2C493),
                    tertiary = Color(0xFFD4DD7C),
                ),
        ),
        vividPalette(
            id = "marigold",
            family = PaletteFamily.Yellows,
            titleResId = R.string.palette_marigold_title,
            primary = Color(0xFFC79200),
            primaryContainer = Color(0xFFFFEBB0),
            secondary = Color(0xFF866530),
            tertiary = Color(0xFF6D6700),
            background = Color(0xFFFFFAF1),
            dark =
                darkSeed(
                    background = Color(0xFF18140B),
                    surface = Color(0xFF251D12),
                    primary = Color(0xFFFFD070),
                    secondary = Color(0xFFE6CC9C),
                    tertiary = Color(0xFFDBE484),
                ),
        ),
    )

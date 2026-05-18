package com.bag.audioandroid.ui.theme

import androidx.compose.ui.graphics.Color
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.PaletteFamily
import com.bag.audioandroid.ui.model.PaletteOption

internal val materialYellowPalettes: List<PaletteOption> =
    listOf(
        vividPalette(
            id = "lemon",
            family = PaletteFamily.Yellows,
            titleResId = R.string.palette_lemon_title,
            primary = Color(0xFFAAA000),
            primaryContainer = Color(0xFFFFF3A1),
            secondary = Color(0xFF756C27),
            tertiary = Color(0xFF586A00),
            background = Color(0xFFFFFDEE),
            dark =
                darkSeed(
                    background = Color(0xFF17150A),
                    surface = Color(0xFF241E11),
                    primary = Color(0xFFFFEE78),
                    secondary = Color(0xFFDDD394),
                    tertiary = Color(0xFFD5E27C),
                ),
        ),
        vividPalette(
            id = "sunflower",
            family = PaletteFamily.Yellows,
            titleResId = R.string.palette_sunflower_title,
            primary = Color(0xFFC3A100),
            primaryContainer = Color(0xFFFFEFA6),
            secondary = Color(0xFF7C6928),
            tertiary = Color(0xFF5D6E00),
            background = Color(0xFFFFFCEC),
            dark =
                darkSeed(
                    background = Color(0xFF17150B),
                    surface = Color(0xFF241D11),
                    primary = Color(0xFFFFE67D),
                    secondary = Color(0xFFE1D59B),
                    tertiary = Color(0xFFD7E481),
                ),
        ),
        vividPalette(
            id = "honey",
            family = PaletteFamily.Yellows,
            titleResId = R.string.palette_honey_title,
            primary = Color(0xFFE39A00),
            primaryContainer = Color(0xFFFFE0A0),
            secondary = Color(0xFF8A632E),
            tertiary = Color(0xFF6E6900),
            background = Color(0xFFFFF9ED),
            dark =
                darkSeed(
                    background = Color(0xFF18130B),
                    surface = Color(0xFF241C12),
                    primary = Color(0xFFFFD176),
                    secondary = Color(0xFFE6C996),
                    tertiary = Color(0xFFD9E181),
                ),
        ),
        vividPalette(
            id = "amber",
            family = PaletteFamily.Yellows,
            titleResId = R.string.palette_amber_title,
            primary = Color(0xFFD69800),
            primaryContainer = Color(0xFFFFE59C),
            secondary = Color(0xFF86652B),
            tertiary = Color(0xFF636B00),
            background = Color(0xFFFFFAEA),
            dark =
                darkSeed(
                    background = Color(0xFF18140A),
                    surface = Color(0xFF251D11),
                    primary = Color(0xFFFFDC74),
                    secondary = Color(0xFFE3D09A),
                    tertiary = Color(0xFFD8E07E),
                ),
        ),
        vividPalette(
            id = "gold",
            family = PaletteFamily.Yellows,
            titleResId = R.string.palette_gold_title,
            primary = Color(0xFFE0A800),
            primaryContainer = Color(0xFFFFE8A8),
            secondary = Color(0xFF8A6730),
            tertiary = Color(0xFF686C00),
            background = Color(0xFFFFFBED),
            dark =
                darkSeed(
                    background = Color(0xFF18140B),
                    surface = Color(0xFF251D12),
                    primary = Color(0xFFFFDE7B),
                    secondary = Color(0xFFE5D3A0),
                    tertiary = Color(0xFFDBE582),
                ),
        ),
        vividPalette(
            id = "marigold",
            family = PaletteFamily.Yellows,
            titleResId = R.string.palette_marigold_title,
            primary = Color(0xFFF0A11A),
            primaryContainer = Color(0xFFFFE4AC),
            secondary = Color(0xFF8D6534),
            tertiary = Color(0xFF746900),
            background = Color(0xFFFFFAF0),
            dark =
                darkSeed(
                    background = Color(0xFF18140B),
                    surface = Color(0xFF251D12),
                    primary = Color(0xFFFFD07B),
                    secondary = Color(0xFFE8CD9E),
                    tertiary = Color(0xFFDDE585),
                ),
        ),
    )

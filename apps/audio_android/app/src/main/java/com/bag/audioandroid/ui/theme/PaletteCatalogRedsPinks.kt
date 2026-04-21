package com.bag.audioandroid.ui.theme

import androidx.compose.ui.graphics.Color
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.PaletteFamily
import com.bag.audioandroid.ui.model.PaletteOption

internal val materialRedsPinksPalettes: List<PaletteOption> =
    listOf(
        vividPalette(
            id = "mars_relic",
            family = PaletteFamily.RedsPinks,
            titleResId = R.string.palette_mars_relic_title,
            primary = Color(0xFFA12828),
            primaryContainer = Color(0xFFFFDAD4),
            secondary = Color(0xFF8E5D57),
            tertiary = Color(0xFF836629),
            background = Color(0xFFFFF8F6),
            dark =
                darkSeed(
                    background = Color(0xFF1B1111),
                    surface = Color(0xFF261818),
                    primary = Color(0xFFFFB4A9),
                    secondary = Color(0xFFE3BDB7),
                    tertiary = Color(0xFFF5C878),
                ),
        ),
        vividPalette(
            id = "ruby",
            family = PaletteFamily.RedsPinks,
            titleResId = R.string.palette_ruby_title,
            primary = Color(0xFFC62828),
            primaryContainer = Color(0xFFFFDAD6),
            secondary = Color(0xFF8B4A47),
            tertiary = Color(0xFF7A5A12),
            background = Color(0xFFFFF8F7),
            dark =
                darkSeed(
                    background = Color(0xFF171112),
                    surface = Color(0xFF22181A),
                    primary = Color(0xFFFF8A80),
                    secondary = Color(0xFFE0B4B0),
                    tertiary = Color(0xFFF3C97A),
                ),
        ),
        vividPalette(
            id = "crimson",
            family = PaletteFamily.RedsPinks,
            titleResId = R.string.palette_crimson_title,
            primary = Color(0xFFB71C45),
            primaryContainer = Color(0xFFFFD9E2),
            secondary = Color(0xFF97546A),
            tertiary = Color(0xFF8F5B2D),
            background = Color(0xFFFFF7F9),
            dark =
                darkSeed(
                    background = Color(0xFF170F14),
                    surface = Color(0xFF21161D),
                    primary = Color(0xFFFF84A2),
                    secondary = Color(0xFFE0B1C2),
                    tertiary = Color(0xFFF2C47B),
                ),
        ),
        vividPalette(
            id = "rose",
            family = PaletteFamily.RedsPinks,
            titleResId = R.string.palette_rose_title,
            primary = Color(0xFFE05A77),
            primaryContainer = Color(0xFFFFD9E0),
            secondary = Color(0xFFA95F74),
            tertiary = Color(0xFF9B6A36),
            background = Color(0xFFFFF8FA),
            dark =
                darkSeed(
                    background = Color(0xFF1A1115),
                    surface = Color(0xFF25171C),
                    primary = Color(0xFFFF9BB1),
                    secondary = Color(0xFFE7B9C5),
                    tertiary = Color(0xFFF3C97C),
                ),
        ),
        vividPalette(
            id = "sakura",
            family = PaletteFamily.RedsPinks,
            titleResId = R.string.palette_sakura_title,
            primary = Color(0xFFF06292),
            primaryContainer = Color(0xFFFFD9E6),
            secondary = Color(0xFFB36A83),
            tertiary = Color(0xFF8D6A4A),
            background = Color(0xFFFFF8FB),
            dark =
                darkSeed(
                    background = Color(0xFF191116),
                    surface = Color(0xFF24181E),
                    primary = Color(0xFFFFA8C4),
                    secondary = Color(0xFFE9BED0),
                    tertiary = Color(0xFFE9CC9E),
                ),
        ),
        vividPalette(
            id = "raspberry",
            family = PaletteFamily.RedsPinks,
            titleResId = R.string.palette_raspberry_title,
            primary = Color(0xFFAD1457),
            primaryContainer = Color(0xFFFFD8E6),
            secondary = Color(0xFF8A5568),
            tertiary = Color(0xFF9A5B4C),
            background = Color(0xFFFFF7F9),
            dark =
                darkSeed(
                    background = Color(0xFF170F13),
                    surface = Color(0xFF23161C),
                    primary = Color(0xFFFF9BC0),
                    secondary = Color(0xFFE2B8C8),
                    tertiary = Color(0xFFF0C0AF),
                ),
        ),
        vividPalette(
            id = "blush",
            family = PaletteFamily.RedsPinks,
            titleResId = R.string.palette_blush_title,
            primary = Color(0xFFF48FB1),
            primaryContainer = Color(0xFFFFD8E5),
            secondary = Color(0xFFB56A7D),
            tertiary = Color(0xFF9A7050),
            background = Color(0xFFFFF8FB),
            dark =
                darkSeed(
                    background = Color(0xFF191116),
                    surface = Color(0xFF25181E),
                    primary = Color(0xFFFFB0C8),
                    secondary = Color(0xFFE8C1CD),
                    tertiary = Color(0xFFEBCDA6),
                ),
        ),
    )

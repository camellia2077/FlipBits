package com.bag.audioandroid.ui.theme

import androidx.compose.ui.graphics.Color
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.PaletteFamily
import com.bag.audioandroid.ui.model.PaletteOption

internal val materialGreenPalettes: List<PaletteOption> =
    listOf(
        vividPalette(
            id = "lime",
            family = PaletteFamily.Greens,
            titleResId = R.string.palette_lime_title,
            primary = Color(0xFF5E8E00),
            primaryContainer = Color(0xFFD9F5A0),
            secondary = Color(0xFF58653F),
            tertiary = Color(0xFF3A6D59),
            background = Color(0xFFFBFCEA),
            dark =
                darkSeed(
                    background = Color(0xFF12150C),
                    surface = Color(0xFF1B2012),
                    primary = Color(0xFFC0E86D),
                    secondary = Color(0xFFC7D5A7),
                    tertiary = Color(0xFF9BDCC7),
                ),
        ),
        vividPalette(
            id = "olive",
            family = PaletteFamily.Greens,
            titleResId = R.string.palette_olive_title,
            primary = Color(0xFF6B7A12),
            primaryContainer = Color(0xFFE2F1AA),
            secondary = Color(0xFF626847),
            tertiary = Color(0xFF4A6C44),
            background = Color(0xFFFBFCEF),
            dark =
                darkSeed(
                    background = Color(0xFF13150E),
                    surface = Color(0xFF1D2016),
                    primary = Color(0xFFD1E27D),
                    secondary = Color(0xFFD0D8AE),
                    tertiary = Color(0xFFB5D69C),
                ),
        ),
        vividPalette(
            id = "emerald",
            family = PaletteFamily.Greens,
            titleResId = R.string.palette_emerald_title,
            primary = Color(0xFF008A24),
            primaryContainer = Color(0xFF9EF2A8),
            secondary = Color(0xFF486548),
            tertiary = Color(0xFF1F6D68),
            background = Color(0xFFF4FCF4),
            dark =
                darkSeed(
                    background = Color(0xFF0D1510),
                    surface = Color(0xFF152018),
                    primary = Color(0xFF7EEA8F),
                    secondary = Color(0xFFB9D6BA),
                    tertiary = Color(0xFF8EDCD4),
                ),
        ),
        vividPalette(
            id = "mint",
            family = PaletteFamily.Greens,
            titleResId = R.string.palette_mint_title,
            primary = Color(0xFF1AAE8E),
            primaryContainer = Color(0xFFB7F2E6),
            secondary = Color(0xFF47756B),
            tertiary = Color(0xFF4A6691),
            background = Color(0xFFF3FCFA),
            dark =
                darkSeed(
                    background = Color(0xFF0D1615),
                    surface = Color(0xFF15211F),
                    primary = Color(0xFF6DE6CA),
                    secondary = Color(0xFFB5D8D2),
                    tertiary = Color(0xFFA9C8F0),
                ),
        ),
        vividPalette(
            id = "jade",
            family = PaletteFamily.Greens,
            titleResId = R.string.palette_jade_title,
            primary = Color(0xFF00A86B),
            primaryContainer = Color(0xFFAAF3D2),
            secondary = Color(0xFF467368),
            tertiary = Color(0xFF466B8C),
            background = Color(0xFFF2FCF8),
            dark =
                darkSeed(
                    background = Color(0xFF0D1613),
                    surface = Color(0xFF15211D),
                    primary = Color(0xFF6EE6B1),
                    secondary = Color(0xFFB5D8CF),
                    tertiary = Color(0xFFA8C8EA),
                ),
        ),
        vividPalette(
            id = "forest",
            family = PaletteFamily.Greens,
            titleResId = R.string.palette_forest_title,
            primary = Color(0xFF2E7D32),
            primaryContainer = Color(0xFFCAEFCF),
            secondary = Color(0xFF566650),
            tertiary = Color(0xFF2E6A5A),
            background = Color(0xFFF5FBF4),
            dark =
                darkSeed(
                    background = Color(0xFF0E150F),
                    surface = Color(0xFF172019),
                    primary = Color(0xFF8FD98E),
                    secondary = Color(0xFFC0D4BE),
                    tertiary = Color(0xFF95D6C7),
                ),
        ),
    )

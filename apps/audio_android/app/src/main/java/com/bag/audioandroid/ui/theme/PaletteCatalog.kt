package com.bag.audioandroid.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.bag.audioandroid.ui.model.PaletteOption

val MaterialPalettes = listOf(
    PaletteOption(
        id = "ocean",
        title = "Ocean Blue",
        subtitle = "蓝色主调，清晰稳定",
        previewColor = Color(0xFF0061A4),
        scheme = lightColorScheme(
            primary = Color(0xFF0061A4),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFD1E4FF),
            onPrimaryContainer = Color(0xFF001D36),
            secondary = Color(0xFF535F70),
            tertiary = Color(0xFF6B5778),
            background = Color(0xFFF7F9FF),
            surface = Color(0xFFF7F9FF)
        )
    ),
    PaletteOption(
        id = "teal",
        title = "Teal Cyan",
        subtitle = "青色主调，科技感",
        previewColor = Color(0xFF006A6A),
        scheme = lightColorScheme(
            primary = Color(0xFF006A6A),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFF6FF7F6),
            onPrimaryContainer = Color(0xFF002020),
            secondary = Color(0xFF4A6363),
            tertiary = Color(0xFF4D5F7A),
            background = Color(0xFFF4FBFA),
            surface = Color(0xFFF4FBFA)
        )
    ),
    PaletteOption(
        id = "emerald",
        title = "Emerald Green",
        subtitle = "绿色主调，偏自然",
        previewColor = Color(0xFF006E1C),
        scheme = lightColorScheme(
            primary = Color(0xFF006E1C),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFF95F990),
            onPrimaryContainer = Color(0xFF002204),
            secondary = Color(0xFF52634F),
            tertiary = Color(0xFF38656A),
            background = Color(0xFFF6FCF2),
            surface = Color(0xFFF6FCF2)
        )
    ),
    PaletteOption(
        id = "amber",
        title = "Amber Orange",
        subtitle = "橙色主调，活跃明亮",
        previewColor = Color(0xFF8A4B00),
        scheme = lightColorScheme(
            primary = Color(0xFF8A4B00),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFDCC2),
            onPrimaryContainer = Color(0xFF2D1600),
            secondary = Color(0xFF725A42),
            tertiary = Color(0xFF58633B),
            background = Color(0xFFFFF8F4),
            surface = Color(0xFFFFF8F4)
        )
    ),
    PaletteOption(
        id = "ruby",
        title = "Ruby Red",
        subtitle = "红色主调，强调感强",
        previewColor = Color(0xFFB3261E),
        scheme = lightColorScheme(
            primary = Color(0xFFB3261E),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFDAD6),
            onPrimaryContainer = Color(0xFF410002),
            secondary = Color(0xFF775653),
            tertiary = Color(0xFF705C2E),
            background = Color(0xFFFFF8F7),
            surface = Color(0xFFFFF8F7)
        )
    ),
    PaletteOption(
        id = "indigo",
        title = "Indigo",
        subtitle = "靛蓝主调，偏商务",
        previewColor = Color(0xFF4355B9),
        scheme = lightColorScheme(
            primary = Color(0xFF4355B9),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFDEE1FF),
            onPrimaryContainer = Color(0xFF001257),
            secondary = Color(0xFF5C5D72),
            tertiary = Color(0xFF78536A),
            background = Color(0xFFF9F8FF),
            surface = Color(0xFFF9F8FF)
        )
    ),
    PaletteOption(
        id = "slate",
        title = "Slate Gray",
        subtitle = "中性灰蓝，克制稳重",
        previewColor = Color(0xFF4A5C67),
        scheme = lightColorScheme(
            primary = Color(0xFF4A5C67),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFCDE6F5),
            onPrimaryContainer = Color(0xFF051E29),
            secondary = Color(0xFF56606A),
            tertiary = Color(0xFF6E5867),
            background = Color(0xFFF8F9FB),
            surface = Color(0xFFF8F9FB)
        )
    ),
    PaletteOption(
        id = "earth",
        title = "Earth Brown",
        subtitle = "棕色主调，温暖质感",
        previewColor = Color(0xFF7A5633),
        scheme = lightColorScheme(
            primary = Color(0xFF7A5633),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFDCC1),
            onPrimaryContainer = Color(0xFF2C1700),
            secondary = Color(0xFF6B5B4B),
            tertiary = Color(0xFF52643A),
            background = Color(0xFFFFF8F4),
            surface = Color(0xFFFFF8F4)
        )
    )
)

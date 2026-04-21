package com.bag.audioandroid.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.BrandThemeOption

@Immutable
data class AudioEncodeGlyphColors(
    val primarySplit: Color,
    val secondarySplit: Color,
    val outline: Color,
)

private val DefaultAudioEncodeGlyphColors =
    AudioEncodeGlyphColors(
        primarySplit = Color(0xFF9E1B1B),
        secondarySplit = Color(0xFFE8E2D0),
        outline = Color(0xFFC5A059),
    )

val LocalAudioEncodeGlyphColors = staticCompositionLocalOf { DefaultAudioEncodeGlyphColors }

@Composable
@ReadOnlyComposable
fun audioEncodeGlyphColors(): AudioEncodeGlyphColors = LocalAudioEncodeGlyphColors.current

fun audioEncodeGlyphColorsForBrandTheme(theme: BrandThemeOption): AudioEncodeGlyphColors {
    // Keep the brass outline fixed as the brand metal accent. Dark dual-tone themes are still
    // mostly held on the established Mars Relic glyph colors, except black_crimson_rite which
    // gets a dedicated dark-red metal base so the icon reads as a ritual machine instead of a
    // near-black silhouette.
    if (theme.id == "black_crimson_rite") {
        return AudioEncodeGlyphColors(
            primarySplit = Color(0xFFCE1126),
            secondarySplit = Color(0xFF5A181C),
            outline = DefaultAudioEncodeGlyphColors.outline,
        )
    }
    if (theme.groupTitleResId == R.string.config_dual_tone_group_ancient_dynasty) {
        // Dynasty glyphs use a visible metal base plus a restrained energy accent so the icon
        // still reads like an ancient machine glyph instead of a neon HUD element.
        return AudioEncodeGlyphColors(
            primarySplit = lerp(theme.secondaryColor, theme.colorScheme.onSurface, 0.34f),
            secondarySplit = lerp(theme.primaryColor, theme.colorScheme.onSurface, 0.18f),
            outline = DefaultAudioEncodeGlyphColors.outline,
        )
    }
    if (theme.isDarkTheme) {
        return DefaultAudioEncodeGlyphColors
    }
    return AudioEncodeGlyphColors(
        primarySplit = theme.primaryColor,
        secondarySplit = theme.secondaryColor,
        outline = DefaultAudioEncodeGlyphColors.outline,
    )
}

fun defaultAudioEncodeGlyphColors(): AudioEncodeGlyphColors = DefaultAudioEncodeGlyphColors

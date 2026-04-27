package com.bag.audioandroid.ui.theme

import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

data class RandomCustomBrandThemeColors(
    val backgroundHex: String,
    val accentHex: String,
    val outlineHex: String,
)

fun randomCustomBrandThemeColors(random: Random = Random.Default): RandomCustomBrandThemeColors {
    // Keep the first pass intentionally simple: randomize in HSV so colors stay
    // vivid enough to read, force primary/accent apart, and derive outline from
    // their mix so the gear edge feels related instead of like a third random actor.
    repeat(12) {
        val backgroundHue = random.nextInt(360).toFloat()
        val background =
            hsvColor(
                hue = backgroundHue,
                saturation = random.nextFloat().lerp(0.35f, 0.72f),
                value = random.nextFloat().lerp(0.42f, 0.76f),
            )

        val accentHueOffset = random.nextFloat().lerp(80f, 160f)
        val accentHue =
            if (random.nextBoolean()) {
                (backgroundHue + accentHueOffset) % 360f
            } else {
                (backgroundHue - accentHueOffset + 360f) % 360f
            }
        val accent =
            hsvColor(
                hue = accentHue,
                saturation = random.nextFloat().lerp(0.50f, 0.90f),
                value = random.nextFloat().lerp(0.56f, 0.92f),
            )

        if (!colorsLookSeparated(background, accent)) {
            return@repeat
        }

        val mixed = lerp(background, accent, 0.5f)
        val outline =
            if ((background.luminance() + accent.luminance()) / 2f > 0.45f) {
                lerp(mixed, Color.Black, 0.35f)
            } else {
                lerp(mixed, Color.White, 0.22f)
            }

        return RandomCustomBrandThemeColors(
            backgroundHex = background.toHexString(),
            accentHex = accent.toHexString(),
            outlineHex = outline.toHexString(),
        )
    }

    val fallbackBackground = Color(0xFF3A4B57)
    val fallbackAccent = Color(0xFFD9912B)
    val fallbackOutline = Color(0xFF9FB4C4)
    return RandomCustomBrandThemeColors(
        backgroundHex = fallbackBackground.toHexString(),
        accentHex = fallbackAccent.toHexString(),
        outlineHex = fallbackOutline.toHexString(),
    )
}

private fun colorsLookSeparated(
    background: Color,
    accent: Color,
): Boolean {
    val luminanceDelta = abs(background.luminance() - accent.luminance())
    val channelDistance =
        abs(background.red - accent.red) +
            abs(background.green - accent.green) +
            abs(background.blue - accent.blue)
    return channelDistance >= 0.40f && luminanceDelta >= 0.08f
}

private fun hsvColor(
    hue: Float,
    saturation: Float,
    value: Float,
): Color = Color(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value)))

private fun Float.lerp(
    min: Float,
    max: Float,
): Float = min + (max - min) * this

private fun Color.toHexString(): String {
    val red = (this.red * 255f).roundToInt().coerceIn(0, 255)
    val green = (this.green * 255f).roundToInt().coerceIn(0, 255)
    val blue = (this.blue * 255f).roundToInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", red, green, blue)
}

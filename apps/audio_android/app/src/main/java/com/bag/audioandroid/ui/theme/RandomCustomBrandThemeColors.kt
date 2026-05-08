package com.bag.audioandroid.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random
import android.graphics.Color as AndroidColor

data class RandomCustomBrandThemeColors(
    val backgroundHex: String,
    val accentHex: String,
    val outlineHex: String,
)

fun randomCustomBrandThemeColors(random: Random = Random.Default): RandomCustomBrandThemeColors {
    // Keep the first pass intentionally simple: randomize in HSV so colors stay
    // vivid enough to read, force primary/accent apart, and derive outline from
    // their mix so the gear edge feels related instead of like a third random actor.
    repeat(16) {
        val backgroundHue = random.nextInt(360).toFloat()
        val isLightMode = random.nextBoolean()

        val background: Color
        val accent: Color

        if (isLightMode) {
            // Light Profile: Low saturation, very high brightness for background
            // High saturation, medium brightness for accent (to ensure contrast)
            background =
                hsvColor(
                    hue = backgroundHue,
                    saturation = random.nextFloat().lerp(0.04f, 0.16f),
                    value = random.nextFloat().lerp(0.88f, 0.98f),
                )
            val accentHueOffset = random.nextFloat().lerp(60f, 180f)
            val accentHue = (backgroundHue + accentHueOffset) % 360f
            accent =
                hsvColor(
                    hue = accentHue,
                    saturation = random.nextFloat().lerp(0.65f, 0.95f),
                    value = random.nextFloat().lerp(0.35f, 0.60f),
                )
        } else {
            // Dark Profile: Medium saturation, low brightness for background
            // Medium-high saturation, high brightness for accent
            background =
                hsvColor(
                    hue = backgroundHue,
                    saturation = random.nextFloat().lerp(0.20f, 0.50f),
                    value = random.nextFloat().lerp(0.12f, 0.28f),
                )
            val accentHueOffset = random.nextFloat().lerp(60f, 180f)
            val accentHue = (backgroundHue + accentHueOffset) % 360f
            accent =
                hsvColor(
                    hue = accentHue,
                    saturation = random.nextFloat().lerp(0.55f, 0.95f),
                    value = random.nextFloat().lerp(0.75f, 0.98f),
                )
        }

        if (!colorsLookSeparated(background, accent)) {
            return@repeat
        }

        val outline = chooseOutlineColor(background = background, accent = accent)

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

private fun chooseOutlineColor(
    background: Color,
    accent: Color,
): Color {
    val mixed = lerp(background, accent, 0.5f)
    val darkCandidate = lerp(mixed, Color.Black, 0.38f)
    val lightCandidate = lerp(mixed, Color.White, 0.26f)

    val bestCandidate =
        if (outlineMinContrast(darkCandidate, background, accent) >= outlineMinContrast(lightCandidate, background, accent)) {
            darkCandidate
        } else {
            lightCandidate
        }

    val desaturated = desaturateColor(bestCandidate, amount = 0.18f)
    val targetMinContrast = 1.55f
    val adjusted = nudgeOutlineContrast(desaturated, background, accent, targetMinContrast)
    val finalCandidate =
        if (outlineMinContrast(adjusted, background, accent) >= targetMinContrast) {
            adjusted
        } else {
            fallbackOutlineColor(background, accent)
        }

    return finalCandidate
}

private fun outlineMinContrast(
    outline: Color,
    background: Color,
    accent: Color,
): Float = minOf(contrastRatio(outline, background), contrastRatio(outline, accent))

private fun contrastRatio(
    first: Color,
    second: Color,
): Float {
    val firstL = first.luminance()
    val secondL = second.luminance()
    val lighter = maxOf(firstL, secondL)
    val darker = minOf(firstL, secondL)
    return ((lighter + 0.05f) / (darker + 0.05f))
}

private fun desaturateColor(
    color: Color,
    amount: Float,
): Color {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(color.toArgb(), hsv)
    hsv[1] = (hsv[1] * (1f - amount)).coerceIn(0f, 1f)
    return Color(AndroidColor.HSVToColor(hsv))
}

private fun nudgeOutlineContrast(
    outline: Color,
    background: Color,
    accent: Color,
    targetMinContrast: Float,
): Color {
    var current = outline
    repeat(8) {
        val minContrast = outlineMinContrast(current, background, accent)
        if (minContrast >= targetMinContrast) {
            return current
        }
        val towardBlack = lerp(current, Color.Black, 0.14f)
        val towardWhite = lerp(current, Color.White, 0.14f)
        current =
            if (outlineMinContrast(towardBlack, background, accent) >= outlineMinContrast(towardWhite, background, accent)) {
                towardBlack
            } else {
                towardWhite
            }
    }
    return current
}

private fun fallbackOutlineColor(
    background: Color,
    accent: Color,
): Color {
    val darkFallback = Color(0xFF2A2F35)
    val lightFallback = Color(0xFFC7D0DA)
    return if (outlineMinContrast(darkFallback, background, accent) >= outlineMinContrast(lightFallback, background, accent)) {
        darkFallback
    } else {
        lightFallback
    }
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

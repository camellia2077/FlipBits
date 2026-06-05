package com.bag.audioandroid.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random
import android.graphics.Color as AndroidColor

data class RandomCustomFactionThemeColors(
    val primaryHex: String,
    val secondaryHex: String,
    val outlineHex: String,
)

enum class RandomCustomMaterialProfile {
    Light,
    Dark,
}

fun randomCustomFactionThemeColors(random: Random = Random.Default): RandomCustomFactionThemeColors {
    // Keep the first pass intentionally simple: randomize in HSV so colors stay
    // vivid enough to read, force primary/accent apart, and derive outline from
    // their mix so the gear edge feels related instead of like a third random actor.
    repeat(16) {
        val primaryHue = random.nextInt(360).toFloat()
        val isLightMode = random.nextBoolean()

        val primary: Color
        val secondary: Color

        if (isLightMode) {
            // Light Profile: Low saturation, very high brightness for primary
            // High saturation, medium brightness for secondary (to ensure contrast)
            primary =
                hsvColor(
                    hue = primaryHue,
                    saturation = random.nextFloat().lerp(0.04f, 0.16f),
                    value = random.nextFloat().lerp(0.88f, 0.98f),
                )
            val secondaryHueOffset = random.nextFloat().lerp(60f, 180f)
            val secondaryHue = (primaryHue + secondaryHueOffset) % 360f
            secondary =
                hsvColor(
                    hue = secondaryHue,
                    saturation = random.nextFloat().lerp(0.65f, 0.95f),
                    value = random.nextFloat().lerp(0.35f, 0.60f),
                )
        } else {
            // Dark Profile: Medium saturation, low brightness for primary
            // Medium-high saturation, high brightness for secondary
            primary =
                hsvColor(
                    hue = primaryHue,
                    saturation = random.nextFloat().lerp(0.20f, 0.50f),
                    value = random.nextFloat().lerp(0.12f, 0.28f),
                )
            val secondaryHueOffset = random.nextFloat().lerp(60f, 180f)
            val secondaryHue = (primaryHue + secondaryHueOffset) % 360f
            secondary =
                hsvColor(
                    hue = secondaryHue,
                    saturation = random.nextFloat().lerp(0.55f, 0.95f),
                    value = random.nextFloat().lerp(0.75f, 0.98f),
                )
        }

        if (!colorsLookSeparated(primary, secondary)) {
            return@repeat
        }

        val outline = chooseOutlineColor(primary = primary, secondary = secondary)

        return RandomCustomFactionThemeColors(
            primaryHex = primary.toHexString(),
            secondaryHex = secondary.toHexString(),
            outlineHex = outline.toHexString(),
        )
    }

    val fallbackPrimary = Color(0xFF3A4B57)
    val fallbackSecondary = Color(0xFFD9912B)
    val fallbackOutline = Color(0xFF9FB4C4)
    return RandomCustomFactionThemeColors(
        primaryHex = fallbackPrimary.toHexString(),
        secondaryHex = fallbackSecondary.toHexString(),
        outlineHex = fallbackOutline.toHexString(),
    )
}

fun randomCustomMaterialPrimaryHex(
    profile: RandomCustomMaterialProfile,
    random: Random = Random.Default,
): String {
    repeat(24) {
        val hue = random.nextInt(360).toFloat()
        val candidate =
            when (profile) {
                RandomCustomMaterialProfile.Light ->
                    hsvColor(
                        hue = hue,
                        saturation = random.nextFloat().lerp(0.48f, 0.88f),
                        value = random.nextFloat().lerp(0.42f, 0.68f),
                    )
                RandomCustomMaterialProfile.Dark ->
                    hsvColor(
                        hue = hue,
                        saturation = random.nextFloat().lerp(0.40f, 0.82f),
                        value = random.nextFloat().lerp(0.58f, 0.82f),
                    )
            }
        val luminance = candidate.luminance()
        val acceptable =
            when (profile) {
                RandomCustomMaterialProfile.Light -> luminance in 0.12f..0.46f
                RandomCustomMaterialProfile.Dark -> luminance in 0.24f..0.64f
            }
        if (acceptable) {
            return candidate.toHexString()
        }
    }

    val fallback =
        when (profile) {
            RandomCustomMaterialProfile.Light -> Color(0xFFB3261E)
            RandomCustomMaterialProfile.Dark -> Color(0xFFFF8A80)
        }
    return fallback.toHexString()
}

private fun chooseOutlineColor(
    primary: Color,
    secondary: Color,
): Color {
    val mixed = lerp(primary, secondary, 0.5f)
    val darkCandidate = lerp(mixed, Color.Black, 0.38f)
    val lightCandidate = lerp(mixed, Color.White, 0.26f)

    val bestCandidate =
        if (outlineMinContrast(darkCandidate, primary, secondary) >= outlineMinContrast(lightCandidate, primary, secondary)) {
            darkCandidate
        } else {
            lightCandidate
        }

    val desaturated = desaturateColor(bestCandidate, amount = 0.18f)
    val targetMinContrast = 1.55f
    val adjusted = nudgeOutlineContrast(desaturated, primary, secondary, targetMinContrast)
    val finalCandidate =
        if (outlineMinContrast(adjusted, primary, secondary) >= targetMinContrast) {
            adjusted
        } else {
            fallbackOutlineColor(primary, secondary)
        }

    return finalCandidate
}

private fun outlineMinContrast(
    outline: Color,
    primary: Color,
    secondary: Color,
): Float = minOf(contrastRatio(outline, primary), contrastRatio(outline, secondary))

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
    primary: Color,
    secondary: Color,
    targetMinContrast: Float,
): Color {
    var current = outline
    repeat(8) {
        val minContrast = outlineMinContrast(current, primary, secondary)
        if (minContrast >= targetMinContrast) {
            return current
        }
        val towardBlack = lerp(current, Color.Black, 0.14f)
        val towardWhite = lerp(current, Color.White, 0.14f)
        current =
            if (outlineMinContrast(towardBlack, primary, secondary) >= outlineMinContrast(towardWhite, primary, secondary)) {
                towardBlack
            } else {
                towardWhite
            }
    }
    return current
}

private fun fallbackOutlineColor(
    primary: Color,
    secondary: Color,
): Color {
    val darkFallback = Color(0xFF2A2F35)
    val lightFallback = Color(0xFFC7D0DA)
    return if (outlineMinContrast(darkFallback, primary, secondary) >= outlineMinContrast(lightFallback, primary, secondary)) {
        darkFallback
    } else {
        lightFallback
    }
}

private fun colorsLookSeparated(
    primary: Color,
    secondary: Color,
): Boolean {
    val luminanceDelta = abs(primary.luminance() - secondary.luminance())
    val channelDistance =
        abs(primary.red - secondary.red) +
            abs(primary.green - secondary.green) +
            abs(primary.blue - secondary.blue)
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

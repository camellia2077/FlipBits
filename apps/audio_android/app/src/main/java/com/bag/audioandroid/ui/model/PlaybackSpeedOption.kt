package com.bag.audioandroid.ui.model

import java.util.Locale

internal enum class PlaybackSpeedOption(
    val speed: Float,
    val label: String,
) {
    Quarter(0.25f, "0.25x"),
    Half(0.5f, "0.5x"),
    Normal(1.0f, "1.0x"),
    Double(2.0f, "2.0x"),
    ;

    companion object {
        val default: PlaybackSpeedOption = Normal

        fun fromSpeed(speed: Float): PlaybackSpeedOption = entries.minBy { option -> kotlin.math.abs(option.speed - speed) }

        fun nextSpeed(currentSpeed: Float): Float {
            val currentIndex = entries.indexOf(fromSpeed(currentSpeed))
            val nextIndex = (currentIndex + 1) % entries.size
            return entries[nextIndex].speed
        }

        fun labelFor(speed: Float): String = fromSpeed(speed).label

        fun format(speed: Float): String =
            String.format(Locale.US, "%.2fx", speed)
                .replace(".00x", ".0x")
                .replace(Regex("(\\.\\d)0x$"), "$1x")
    }
}

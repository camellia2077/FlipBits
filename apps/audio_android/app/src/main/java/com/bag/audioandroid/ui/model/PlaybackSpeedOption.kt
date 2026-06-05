package com.bag.audioandroid.ui.model

import java.util.Locale

internal enum class PlaybackSpeedOption(
    val speed: Float,
) {
    Tenth(0.1f),
    Quarter(0.25f),
    Half(0.5f),
    ThreeQuarter(0.75f),
    Normal(1.0f),
    OneAndHalf(1.5f),
    Double(2.0f),
    Quadruple(4.0f),
    ;

    companion object {
        val default: PlaybackSpeedOption = Normal

        val speeds: List<Float> = entries.map { it.speed }
        val slowerSpeeds: List<PlaybackSpeedOption> = entries.filter { it.speed <= 1.0f }
        val fasterSpeeds: List<PlaybackSpeedOption> = entries.filter { it.speed >= 1.0f }

        fun speedsForMode(mode: TransportModeOption): List<Float> = optionsForMode(mode).map { it.speed }

        fun coerceSpeedForMode(
            speed: Float,
            mode: TransportModeOption,
        ): Float = optionsForMode(mode).minBy { option -> kotlin.math.abs(option.speed - speed) }.speed

        fun slowerSpeedsForMode(mode: TransportModeOption): List<PlaybackSpeedOption> = optionsForMode(mode).filter { it.speed <= 1.0f }

        fun fasterSpeedsForMode(mode: TransportModeOption): List<PlaybackSpeedOption> = optionsForMode(mode).filter { it.speed >= 1.0f }

        fun fromSpeed(speed: Float): PlaybackSpeedOption = entries.minBy { option -> kotlin.math.abs(option.speed - speed) }

        fun nextSpeed(currentSpeed: Float): Float {
            val currentIndex = entries.indexOf(fromSpeed(currentSpeed))
            val nextIndex = (currentIndex + 1) % entries.size
            return entries[nextIndex].speed
        }

        fun nextSpeed(
            currentSpeed: Float,
            mode: TransportModeOption,
        ): Float {
            val options = optionsForMode(mode)
            val currentOption = options.minBy { option -> kotlin.math.abs(option.speed - currentSpeed) }
            val currentIndex = options.indexOf(currentOption)
            val nextIndex = (currentIndex + 1) % options.size
            return options[nextIndex].speed
        }

        fun sliderPosition(speed: Float): Float = entries.indexOf(fromSpeed(speed)).toFloat()

        fun speedAtSliderPosition(position: Float): Float {
            val index = position.toInt().coerceIn(0, entries.lastIndex)
            return entries[index].speed
        }

        fun format(speed: Float): String =
            String
                .format(Locale.US, "%.2fx", speed)
                .replace(".00x", ".0x")
                .replace(Regex("(\\.\\d)0x$"), "$1x")

        private fun optionsForMode(mode: TransportModeOption): List<PlaybackSpeedOption> =
            when (mode) {
                TransportModeOption.Flash ->
                    listOf(
                        Tenth,
                        Quarter,
                        Half,
                        ThreeQuarter,
                        Normal,
                        OneAndHalf,
                        Double,
                        Quadruple,
                    )
                TransportModeOption.Mini,
                TransportModeOption.Pro,
                TransportModeOption.Ultra,
                -> entries.toList()
            }
    }
}

package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.R

enum class MiniMorseVisualizationMode(
    val labelResId: Int,
) {
    Vertical(R.string.audio_morse_visualizer_mode_vertical),
    Horizontal(R.string.audio_morse_visualizer_mode_horizontal),
}

internal fun miniMorseVisualizationModeFromName(name: String): MiniMorseVisualizationMode =
    MiniMorseVisualizationMode.entries.firstOrNull { mode ->
        mode.name == name
    } ?: MiniMorseVisualizationMode.Horizontal

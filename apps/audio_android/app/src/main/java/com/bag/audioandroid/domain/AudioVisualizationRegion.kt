package com.bag.audioandroid.domain

enum class AudioVisualizationRegion {
    Unknown,
    LeadingShell,
    Payload,
    TrailingShell;

    companion object {
        fun fromNativeValue(value: Int): AudioVisualizationRegion = when (value) {
            1 -> LeadingShell
            2 -> Payload
            3 -> TrailingShell
            else -> Unknown
        }
    }
}

package com.bag.audioandroid.domain

data class AudioVisualizationFrame(
    val sampleOffset: Int,
    val sampleCount: Int,
    val rms: Float,
    val peak: Float,
    val brightness: Float,
    val region: AudioVisualizationRegion
)

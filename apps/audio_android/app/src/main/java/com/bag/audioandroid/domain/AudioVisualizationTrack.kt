package com.bag.audioandroid.domain

data class AudioVisualizationTrack(
    val frames: List<AudioVisualizationFrame>,
    val totalSamples: Int,
    val sampleRateHz: Int,
    val frameStrideSamples: Int
)

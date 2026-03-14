package com.bag.audioandroid.ui.model

sealed interface AudioPlaybackSource {
    data class Generated(
        val mode: TransportModeOption
    ) : AudioPlaybackSource

    data class Saved(
        val itemId: String
    ) : AudioPlaybackSource
}

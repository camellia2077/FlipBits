package com.bag.audioandroid.ui.state

data class PlaybackUiState(
    val phase: PlaybackPhase = PlaybackPhase.Idle,
    val playedSamples: Int = 0,
    val totalSamples: Int = 0,
    val sampleRateHz: Int = 0,
    val isScrubbing: Boolean = false,
    val scrubPreviewSamples: Int = 0,
    val resumeAfterScrub: Boolean = false
) {
    val displayedSamples: Int
        get() = if (isScrubbing) scrubPreviewSamples else playedSamples

    val isPlaying: Boolean
        get() = phase == PlaybackPhase.Playing

    val isPaused: Boolean
        get() = phase == PlaybackPhase.Paused
}

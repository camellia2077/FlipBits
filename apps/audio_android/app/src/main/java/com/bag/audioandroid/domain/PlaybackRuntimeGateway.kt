package com.bag.audioandroid.domain

import com.bag.audioandroid.ui.state.PlaybackUiState

interface PlaybackRuntimeGateway {
    fun cleared(): PlaybackUiState

    fun load(
        totalSamples: Int,
        sampleRateHz: Int,
    ): PlaybackUiState

    fun playStarted(state: PlaybackUiState): PlaybackUiState

    fun paused(state: PlaybackUiState): PlaybackUiState

    fun resumed(state: PlaybackUiState): PlaybackUiState

    fun progress(
        state: PlaybackUiState,
        playedSamples: Int,
    ): PlaybackUiState

    fun scrubStarted(state: PlaybackUiState): PlaybackUiState

    fun scrubChanged(
        state: PlaybackUiState,
        targetSamples: Int,
    ): PlaybackUiState

    fun scrubCommitted(state: PlaybackUiState): PlaybackUiState

    fun scrubCanceled(state: PlaybackUiState): PlaybackUiState

    fun stopped(state: PlaybackUiState): PlaybackUiState

    fun completed(state: PlaybackUiState): PlaybackUiState

    fun failed(state: PlaybackUiState): PlaybackUiState

    fun clampSamples(
        totalSamples: Int,
        sampleIndex: Int,
    ): Int

    fun fractionToSamples(
        totalSamples: Int,
        fraction: Float,
    ): Int

    fun progressFraction(state: PlaybackUiState): Float

    fun elapsedMs(state: PlaybackUiState): Long

    fun totalMs(state: PlaybackUiState): Long
}

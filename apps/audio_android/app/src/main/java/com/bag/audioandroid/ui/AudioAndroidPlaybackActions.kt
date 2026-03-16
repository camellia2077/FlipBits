package com.bag.audioandroid.ui

import com.bag.audioandroid.audio.AudioPlaybackCoordinator
import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.state.AudioAppUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

internal class AudioAndroidPlaybackActions(
    uiState: MutableStateFlow<AudioAppUiState>,
    scope: CoroutineScope,
    sessionStateStore: AudioSessionStateStore,
    playbackCoordinator: AudioPlaybackCoordinator,
    playbackRuntimeGateway: PlaybackRuntimeGateway,
    playbackSourceCoordinator: PlaybackSourceCoordinator,
    playbackSessionReducer: PlaybackSessionReducer,
    sampleRateHz: Int,
    onPlaybackCompleted: (AudioPlaybackSource) -> Boolean
) {
    private val playbackUiStateSync = AudioPlaybackUiStateSync(
        uiState = uiState,
        sessionStateStore = sessionStateStore,
        playbackRuntimeGateway = playbackRuntimeGateway,
        playbackSourceCoordinator = playbackSourceCoordinator,
        playbackSessionReducer = playbackSessionReducer,
        sampleRateHz = sampleRateHz
    )
    private val commandActions = AudioPlaybackCommandActions(
        uiState = uiState,
        scope = scope,
        playbackCoordinator = playbackCoordinator,
        playbackRuntimeGateway = playbackRuntimeGateway,
        playbackSourceCoordinator = playbackSourceCoordinator,
        playbackUiStateSync = playbackUiStateSync,
        onPlaybackCompleted = onPlaybackCompleted
    )
    private val scrubActions = AudioPlaybackScrubActions(
        uiState = uiState,
        playbackCoordinator = playbackCoordinator,
        playbackRuntimeGateway = playbackRuntimeGateway,
        playbackSourceCoordinator = playbackSourceCoordinator,
        playbackUiStateSync = playbackUiStateSync,
        startPlaybackFromTarget = commandActions::startPlaybackFromTarget
    )

    fun onTogglePlayback() {
        commandActions.onTogglePlayback()
    }

    fun onScrubStarted() {
        scrubActions.onScrubStarted()
    }

    fun onScrubChanged(targetSamples: Int) {
        scrubActions.onScrubChanged(targetSamples)
    }

    fun onScrubFinished() {
        scrubActions.onScrubFinished()
    }

    fun onScrubCanceled() {
        scrubActions.onScrubCanceled()
    }

    fun stopPlayback() {
        commandActions.stopPlayback()
    }

    fun release() {
        commandActions.release()
    }

    fun playCurrentFromStart(): Boolean =
        commandActions.playCurrentFromStart()
}

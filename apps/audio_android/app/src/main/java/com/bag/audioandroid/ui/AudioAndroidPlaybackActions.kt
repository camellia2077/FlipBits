package com.bag.audioandroid.ui

import com.bag.audioandroid.audio.AudioPlaybackCoordinator
import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.PlaybackSpeedOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.util.safeDebugLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class AudioAndroidPlaybackActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    scope: CoroutineScope,
    private val sessionStateStore: AudioSessionStateStore,
    private val playbackCoordinator: AudioPlaybackCoordinator,
    playbackRuntimeGateway: PlaybackRuntimeGateway,
    private val playbackSourceCoordinator: PlaybackSourceCoordinator,
    playbackSessionReducer: PlaybackSessionReducer,
    sampleRateHz: Int,
    onPlaybackCompleted: (AudioPlaybackSource) -> Boolean,
    followDataWindowActions: FollowDataWindowActions? = null,
    flashVisualWindowActions: FlashVisualWindowActions? = null,
) {
    private val playbackUiStateSync =
        AudioPlaybackUiStateSync(
            uiState = uiState,
            sessionStateStore = sessionStateStore,
            playbackRuntimeGateway = playbackRuntimeGateway,
            playbackSourceCoordinator = playbackSourceCoordinator,
            playbackSessionReducer = playbackSessionReducer,
            sampleRateHz = sampleRateHz,
            followDataWindowActions = followDataWindowActions,
            flashVisualWindowActions = flashVisualWindowActions,
        )
    private val commandActions =
        AudioPlaybackCommandActions(
            uiState = uiState,
            scope = scope,
            playbackCoordinator = playbackCoordinator,
            playbackRuntimeGateway = playbackRuntimeGateway,
            playbackSourceCoordinator = playbackSourceCoordinator,
            playbackUiStateSync = playbackUiStateSync,
            onPlaybackCompleted = onPlaybackCompleted,
        )
    private val scrubActions =
        AudioPlaybackScrubActions(
            uiState = uiState,
            playbackCoordinator = playbackCoordinator,
            playbackRuntimeGateway = playbackRuntimeGateway,
            playbackSourceCoordinator = playbackSourceCoordinator,
            playbackUiStateSync = playbackUiStateSync,
            followDataWindowActions = followDataWindowActions,
            flashVisualWindowActions = flashVisualWindowActions,
            startPlaybackFromTarget = commandActions::startPlaybackFromTarget,
        )

    fun onTogglePlayback() {
        commandActions.onTogglePlayback()
    }

    fun pauseCurrentPlaybackIfPlaying(): Boolean = commandActions.pauseCurrentPlaybackIfPlaying()

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

    fun playCurrentFromStart(): Boolean = commandActions.playCurrentFromStart()

    fun onPlaybackSpeedSelected(playbackSpeed: Float) {
        val current = uiState.value
        val source = current.currentPlaybackSource
        val sourceKey = playbackSourceCoordinator.sourceKey(source)
        val currentPlayback = current.currentPlayback
        val transportMode =
            current.currentPlaybackTransportMode
                ?: when (source) {
                    is AudioPlaybackSource.Generated -> source.mode
                    is AudioPlaybackSource.Saved -> TransportModeOption.Flash
                }
        val resolvedPlaybackSpeed = PlaybackSpeedOption.coerceSpeedForMode(playbackSpeed, transportMode)
        safeDebugLog(
            PlaybackSpeedDiagTag,
            "select source=$sourceKey requested=$playbackSpeed resolved=$resolvedPlaybackSpeed previous=${current.currentPlaybackSpeed} " +
                "phase=${currentPlayback.phase} playing=${currentPlayback.isPlaying} " +
                "sample=${currentPlayback.displayedSamples}/${currentPlayback.totalSamples}",
        )
        when (source) {
            is AudioPlaybackSource.Generated ->
                sessionStateStore.updateSession(source.mode) {
                    it.copy(playbackSpeed = resolvedPlaybackSpeed)
                }

            is AudioPlaybackSource.Saved ->
                uiState.update { state ->
                    val selectedSavedAudio =
                        state.selectedSavedAudio
                            ?.takeIf { it.item.itemId == source.itemId }
                            ?: return@update state
                    state.copy(
                        selectedSavedAudio = selectedSavedAudio.copy(playbackSpeed = resolvedPlaybackSpeed),
                    )
                }
        }
        val updated = uiState.value
        safeDebugLog(
            PlaybackSpeedDiagTag,
            "stateApplied source=$sourceKey requested=$playbackSpeed resolved=$resolvedPlaybackSpeed " +
                "stored=${updated.currentPlaybackSpeed} " +
                "currentSource=${playbackSourceCoordinator.sourceKey(updated.currentPlaybackSource)}",
        )
        val applied =
            playbackCoordinator.setPlaybackSpeed(
                sourceKey = sourceKey,
                playbackSpeed = resolvedPlaybackSpeed,
            )
        val restarted =
            if (!applied) {
                commandActions.restartActivePlaybackForSpeedChange(
                    source = source,
                    wasPlaying = currentPlayback.isPlaying,
                )
            } else {
                false
            }
        safeDebugLog(
            PlaybackSpeedDiagTag,
            "coordinatorApplied source=$sourceKey requested=$playbackSpeed resolved=$resolvedPlaybackSpeed applied=$applied restarted=$restarted",
        )
    }

    private companion object {
        const val PlaybackSpeedDiagTag = "PlaybackSpeedDiag"
    }
}

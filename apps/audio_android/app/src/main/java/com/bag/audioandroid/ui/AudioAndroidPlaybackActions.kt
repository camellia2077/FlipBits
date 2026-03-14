package com.bag.audioandroid.ui

import com.bag.audioandroid.R
import com.bag.audioandroid.audio.AudioPlaybackCoordinator
import com.bag.audioandroid.audio.PlaybackResult
import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.PlaybackUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class AudioAndroidPlaybackActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val scope: CoroutineScope,
    private val sessionStateStore: AudioSessionStateStore,
    private val playbackCoordinator: AudioPlaybackCoordinator,
    private val playbackRuntimeGateway: PlaybackRuntimeGateway,
    private val playbackSourceCoordinator: PlaybackSourceCoordinator,
    private val playbackSessionReducer: PlaybackSessionReducer,
    private val sampleRateHz: Int,
    private val onPlaybackCompleted: (AudioPlaybackSource) -> Boolean
) {
    fun onTogglePlayback() {
        val current = uiState.value
        val playbackTarget = playbackSourceCoordinator.resolveTarget(current) ?: run {
            sessionStateStore.updateCurrentSession {
                it.copy(statusText = UiText.Resource(R.string.status_no_audio_for_mode))
            }
            return
        }
        val sourceKey = playbackSourceCoordinator.sourceKey(playbackTarget.source)

        if (playbackCoordinator.hasActivePlaybackForOtherSource(sourceKey)) {
            stopPlayback()
        }
        val playback = playbackTarget.playback

        when {
            playback.isPlaying -> {
                playbackCoordinator.pausePlayback(sourceKey)
                updatePlaybackState(playbackTarget.source) {
                    playbackRuntimeGateway.paused(it)
                }
                setCurrentStatusText(UiText.Resource(R.string.status_playback_paused))
            }

            playback.isPaused && playbackCoordinator.hasActivePlaybackForSource(sourceKey) -> {
                if (playbackCoordinator.resumePlayback(sourceKey)) {
                    updatePlaybackState(playbackTarget.source) {
                        playbackRuntimeGateway.resumed(it)
                    }
                    setCurrentStatusText(playbackStatusPlaying(playbackTarget.source))
                } else {
                    startPlayback(playbackTarget)
                }
            }

            else -> startPlayback(playbackTarget)
        }
    }

    fun onScrubStarted() {
        val current = uiState.value
        val playbackTarget = playbackSourceCoordinator.resolveTarget(current) ?: return
        if (playbackTarget.playback.totalSamples <= 0) {
            return
        }

        val sourceKey = playbackSourceCoordinator.sourceKey(playbackTarget.source)
        val started = playbackRuntimeGateway.scrubStarted(playbackTarget.playback)
        if (started.resumeAfterScrub) {
            playbackCoordinator.beginScrub(sourceKey)
        }
        updatePlaybackState(playbackTarget.source) { started }
        setCurrentStatusText(UiText.Resource(R.string.status_playback_paused))
    }

    fun onScrubChanged(targetSamples: Int) {
        val current = uiState.value
        val playbackTarget = playbackSourceCoordinator.resolveTarget(current) ?: return
        if (playbackTarget.playback.totalSamples <= 0) {
            return
        }

        val clampedTarget = playbackRuntimeGateway.clampSamples(playbackTarget.playback.totalSamples, targetSamples)
        playbackCoordinator.updateScrub(playbackSourceCoordinator.sourceKey(playbackTarget.source), clampedTarget)
        updatePlaybackState(playbackTarget.source) {
            playbackRuntimeGateway.scrubChanged(it, clampedTarget)
        }
    }

    fun onScrubFinished() {
        val current = uiState.value
        val playbackTarget = playbackSourceCoordinator.resolveTarget(current) ?: return
        val playback = playbackTarget.playback
        if (!playback.isScrubbing) {
            return
        }

        val sourceKey = playbackSourceCoordinator.sourceKey(playbackTarget.source)
        val shouldResume = playback.resumeAfterScrub && playbackCoordinator.hasActivePlaybackForSource(sourceKey)
        val committed = playbackRuntimeGateway.scrubCommitted(playback)
        if (playbackCoordinator.hasActivePlaybackForSource(sourceKey)) {
            playbackCoordinator.commitScrub(sourceKey, committed.playedSamples, shouldResume)
        }
        val nextPlayback = if (shouldResume) {
            playbackRuntimeGateway.resumed(committed)
        } else {
            committed
        }
        updatePlaybackState(playbackTarget.source) { nextPlayback }
        setCurrentStatusText(
            if (shouldResume) {
                playbackStatusPlaying(playbackTarget.source)
            } else {
                UiText.Resource(R.string.status_playback_paused)
            }
        )
    }

    fun onScrubCanceled() {
        val current = uiState.value
        val playbackTarget = playbackSourceCoordinator.resolveTarget(current) ?: return
        val playback = playbackTarget.playback
        if (!playback.isScrubbing) {
            return
        }

        val sourceKey = playbackSourceCoordinator.sourceKey(playbackTarget.source)
        val canceled = playbackRuntimeGateway.scrubCanceled(playback)
        if (playbackCoordinator.hasActivePlaybackForSource(sourceKey)) {
            playbackCoordinator.cancelScrub(sourceKey, canceled.isPlaying)
        }
        updatePlaybackState(playbackTarget.source) { canceled }
        setCurrentStatusText(
            if (canceled.isPlaying) {
                playbackStatusPlaying(playbackTarget.source)
            } else {
                UiText.Resource(R.string.status_playback_paused)
            }
        )
    }

    fun stopPlayback() {
        playbackCoordinator.stopPlayback { sourceKey ->
            playbackSourceCoordinator.sourceForKey(uiState.value, sourceKey)?.let(::applyPlaybackStopped)
        }
    }

    fun release() {
        playbackCoordinator.release()
    }

    fun playCurrentFromStart(): Boolean {
        val current = uiState.value
        val playbackTarget = playbackSourceCoordinator.resolveTarget(current) ?: return false
        val sourceKey = playbackSourceCoordinator.sourceKey(playbackTarget.source)
        if (playbackCoordinator.hasActivePlaybackForOtherSource(sourceKey)) {
            stopPlayback()
        }
        startPlayback(playbackTarget, restartFromBeginning = true)
        return true
    }

    private fun startPlayback(
        target: PlaybackSourceCoordinator.PlaybackTarget,
        restartFromBeginning: Boolean = false
    ) {
        val sourceKey = playbackSourceCoordinator.sourceKey(target.source)
        val basePlayback = if (restartFromBeginning) {
            playbackRuntimeGateway.load(target.pcm.size, target.sampleRateHz)
        } else {
            target.playback
        }
        val startedPlayback = playbackRuntimeGateway.playStarted(basePlayback)
        playbackCoordinator.startPlayback(
            scope = scope,
            sourceKey = sourceKey,
            pcm = target.pcm,
            sampleRateHz = target.sampleRateHz,
            startSampleIndex = startedPlayback.playedSamples,
            onStarted = {
                updatePlaybackState(target.source) { startedPlayback }
                setCurrentStatusText(playbackStatusPlaying(target.source))
            },
            onProgressChanged = { playedSamples, totalSamples ->
                applyPlaybackProgress(target.source, playedSamples, totalSamples)
            },
            onFinished = { _, result ->
                when (result) {
                    PlaybackResult.Completed -> {
                        if (!onPlaybackCompleted(target.source)) {
                            applyPlaybackCompleted(target.source)
                        }
                    }
                    PlaybackResult.Stopped -> applyPlaybackStopped(target.source)
                }
            },
            onFailed = ::applyPlaybackFailed
        )
    }

    private fun updatePlaybackState(
        source: AudioPlaybackSource,
        transform: (PlaybackUiState) -> PlaybackUiState
    ) {
        when (source) {
            is AudioPlaybackSource.Generated -> sessionStateStore.updateSession(source.mode) {
                it.copy(playback = transform(it.playback))
            }

            is AudioPlaybackSource.Saved -> uiState.update { state ->
                val selectedSavedAudio = state.selectedSavedAudio
                    ?.takeIf { it.item.itemId == source.itemId }
                    ?: return@update state
                state.copy(selectedSavedAudio = selectedSavedAudio.copy(playback = transform(selectedSavedAudio.playback)))
            }
        }
    }

    private fun setCurrentStatusText(statusText: UiText) {
        sessionStateStore.updateCurrentSession {
            it.copy(statusText = statusText)
        }
    }

    private fun playbackStatusPlaying(source: AudioPlaybackSource): UiText =
        when (source) {
            is AudioPlaybackSource.Generated -> playbackSessionReducer.playingStatus(source.mode)
            is AudioPlaybackSource.Saved -> UiText.Resource(
                R.string.status_playing_saved_audio,
                listOf(uiState.value.selectedSavedAudio?.item?.displayName.orEmpty())
            )
        }

    private fun applyPlaybackProgress(source: AudioPlaybackSource, playedSamples: Int, totalSamples: Int) {
        updatePlaybackState(source) { currentPlayback ->
            val playbackBase = if (currentPlayback.totalSamples == 0 && totalSamples > 0) {
                val resolvedSampleRateHz = when (source) {
                    is AudioPlaybackSource.Generated -> sampleRateHz
                    is AudioPlaybackSource.Saved -> uiState.value.selectedSavedAudio?.sampleRateHz ?: sampleRateHz
                }
                playbackRuntimeGateway.load(totalSamples, resolvedSampleRateHz)
            } else {
                currentPlayback
            }
            playbackRuntimeGateway.progress(playbackBase, playedSamples)
        }
    }

    private fun applyPlaybackCompleted(source: AudioPlaybackSource) {
        updatePlaybackState(source) { playbackRuntimeGateway.completed(it) }
        setCurrentStatusText(UiText.Resource(R.string.status_playback_completed))
    }

    private fun applyPlaybackFailed(sourceKey: String) {
        val source = playbackSourceCoordinator.sourceForKey(uiState.value, sourceKey) ?: return
        updatePlaybackState(source) { playbackRuntimeGateway.failed(it) }
        setCurrentStatusText(UiText.Resource(R.string.status_playback_failed))
    }

    private fun applyPlaybackStopped(source: AudioPlaybackSource) {
        updatePlaybackState(source) { playbackRuntimeGateway.stopped(it) }
        setCurrentStatusText(UiText.Resource(R.string.status_playback_stopped))
    }
}

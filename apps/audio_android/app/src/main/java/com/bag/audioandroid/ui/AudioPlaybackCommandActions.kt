package com.bag.audioandroid.ui

import android.util.Log
import com.bag.audioandroid.BuildConfig
import com.bag.audioandroid.R
import com.bag.audioandroid.audio.AudioPlaybackCoordinator
import com.bag.audioandroid.audio.PlaybackRenderContext
import com.bag.audioandroid.audio.PlaybackResult
import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

internal class AudioPlaybackCommandActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val scope: CoroutineScope,
    private val playbackCoordinator: AudioPlaybackCoordinator,
    private val playbackRuntimeGateway: PlaybackRuntimeGateway,
    private val playbackSourceCoordinator: PlaybackSourceCoordinator,
    private val playbackUiStateSync: AudioPlaybackUiStateSync,
    private val onPlaybackCompleted: (AudioPlaybackSource) -> Boolean,
) {
    fun onTogglePlayback() {
        val current = uiState.value
        val playbackTarget =
            playbackSourceCoordinator.resolveTarget(current) ?: run {
                safeLogD(
                    PLAYBACK_AUTOMATION_TAG,
                    "toggle:noTarget source=${current.currentPlaybackSource.debugSourceId()} " +
                        "currentMode=${current.transportMode.wireName} " +
                        "currentSamples=${current.currentPlaybackSampleCount} " +
                        "miniPlayer=${current.miniPlayerModel != null}",
                )
                playbackUiStateSync.setCurrentStatusText(UiText.Resource(R.string.status_no_audio_for_mode))
                return
            }
        val sourceKey = playbackSourceCoordinator.sourceKey(playbackTarget.source)

        if (playbackCoordinator.hasActivePlaybackForOtherSource(sourceKey)) {
            safeLogD(
                PLAYBACK_AUTOMATION_TAG,
                "toggle:stopOther target=$sourceKey",
            )
            stopPlayback()
        }
        val playback = playbackTarget.playback

        when {
            playback.isPlaying -> {
                safeLogD(
                    PLAYBACK_AUTOMATION_TAG,
                    "toggle:pause source=$sourceKey played=${playback.playedSamples} total=${playback.totalSamples}",
                )
                playbackCoordinator.pausePlayback(sourceKey)
                playbackUiStateSync.updatePlaybackState(playbackTarget.source) {
                    playbackRuntimeGateway.paused(it)
                }
                playbackUiStateSync.setCurrentStatusText(UiText.Resource(R.string.status_playback_paused))
            }

            playback.isPaused && playbackCoordinator.hasActivePlaybackForSource(sourceKey) -> {
                safeLogD(
                    PLAYBACK_AUTOMATION_TAG,
                    "toggle:resume source=$sourceKey played=${playback.playedSamples} total=${playback.totalSamples}",
                )
                if (playbackCoordinator.resumePlayback(sourceKey)) {
                    playbackUiStateSync.updatePlaybackState(playbackTarget.source) {
                        playbackRuntimeGateway.resumed(it)
                    }
                    playbackUiStateSync.setCurrentStatusText(
                        playbackUiStateSync.playbackStatusPlaying(playbackTarget.source),
                    )
                } else {
                    startPlayback(playbackTarget)
                }
            }

            else -> startPlayback(playbackTarget)
        }
    }

    fun pauseCurrentPlaybackIfPlaying(): Boolean {
        val current = uiState.value
        val playbackTarget = playbackSourceCoordinator.resolveTarget(current) ?: return false
        val playback = playbackTarget.playback
        if (!playback.isPlaying) {
            return false
        }
        val sourceKey = playbackSourceCoordinator.sourceKey(playbackTarget.source)
        safeLogD(
            PLAYBACK_AUTOMATION_TAG,
            "pauseOnly source=$sourceKey played=${playback.playedSamples} total=${playback.totalSamples}",
        )
        playbackCoordinator.pausePlayback(sourceKey)
        playbackUiStateSync.updatePlaybackState(playbackTarget.source) {
            playbackRuntimeGateway.paused(it)
        }
        playbackUiStateSync.setCurrentStatusText(UiText.Resource(R.string.status_playback_paused))
        return true
    }

    fun stopPlayback() {
        val currentState = uiState.value
        safeLogD(
            PLAYBACK_AUTOMATION_TAG,
            "stopRequested currentSource=${currentState.currentPlaybackSource.debugSourceId()} " +
                "selectedSaved=${currentState.selectedSavedAudio?.item?.itemId.orEmpty()}",
        )
        playbackCoordinator.stopPlayback { sourceKey ->
            safeLogD(
                PLAYBACK_AUTOMATION_TAG,
                "stopApplied source=$sourceKey currentSource=${uiState.value.currentPlaybackSource.debugSourceId()}",
            )
            playbackSourceCoordinator.sourceForKey(uiState.value, sourceKey)?.let(playbackUiStateSync::applyPlaybackStopped)
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

    fun startPlaybackFromTarget(target: PlaybackSourceCoordinator.PlaybackTarget) {
        startPlayback(target)
    }

    fun restartActivePlaybackForSpeedChange(
        source: AudioPlaybackSource,
        wasPlaying: Boolean,
    ): Boolean {
        val sourceKey = playbackSourceCoordinator.sourceKey(source)
        if (!playbackCoordinator.hasActivePlaybackForSource(sourceKey)) {
            return false
        }
        val target = playbackSourceCoordinator.resolveTarget(uiState.value) ?: return false
        safeLogD(
            PLAYBACK_SPEED_DIAG_TAG,
            "restartRequested source=$sourceKey wasPlaying=$wasPlaying speed=${target.playbackSpeed} " +
                "played=${target.playback.playedSamples}/${target.totalSamples} phase=${target.playback.phase} " +
                "fileBacked=${!target.pcmFilePath.isNullOrBlank()} mode=${target.transportMode?.wireName ?: "_"}",
        )
        val restarted =
            if (wasPlaying) {
                startPlayback(target, handoffFromActivePlayback = true)
                true
            } else {
                playbackCoordinator.stopPlayback()
                true
            }
        safeLogD(
            PLAYBACK_SPEED_DIAG_TAG,
            "restartApplied source=$sourceKey wasPlaying=$wasPlaying restarted=$restarted speed=${target.playbackSpeed} " +
                "played=${target.playback.playedSamples}/${target.totalSamples}",
        )
        return restarted
    }

    private fun startPlayback(
        target: PlaybackSourceCoordinator.PlaybackTarget,
        restartFromBeginning: Boolean = false,
        handoffFromActivePlayback: Boolean = false,
    ) {
        val sourceKey = playbackSourceCoordinator.sourceKey(target.source)
        val basePlayback =
            if (restartFromBeginning) {
                playbackRuntimeGateway.load(target.totalSamples, target.sampleRateHz)
            } else {
                target.playback
            }
        val startedPlayback = playbackRuntimeGateway.playStarted(basePlayback)
        safeLogD(
            PLAYBACK_AUTOMATION_TAG,
            "start source=$sourceKey total=${target.totalSamples} inMemory=${target.pcm.size} " +
                "fileBacked=${!target.pcmFilePath.isNullOrBlank()} sampleRate=${target.sampleRateHz} " +
                "start=${startedPlayback.playedSamples} restart=$restartFromBeginning " +
                "savedSelection=${uiState.value.selectedSavedAudio?.item?.itemId.orEmpty()}",
        )
        playbackCoordinator.startPlayback(
            scope = scope,
            sourceKey = sourceKey,
            pcm = target.pcm,
            pcmFilePath = target.pcmFilePath,
            totalSamples = target.totalSamples,
            sampleRateHz = target.sampleRateHz,
            playbackSpeed = target.playbackSpeed,
            startSampleIndex = startedPlayback.playedSamples,
            renderContext =
                PlaybackRenderContext(
                    isMiniMode = target.transportMode == TransportModeOption.Mini,
                    isFlashMode = target.transportMode == TransportModeOption.Flash,
                    frameSamples = target.frameSamples,
                    followData = target.followData,
                    totalSamples = target.totalSamples,
                ),
            handoffFromActivePlayback = handoffFromActivePlayback,
            onStarted = {
                playbackUiStateSync.updatePlaybackState(target.source) { startedPlayback }
                playbackUiStateSync.setCurrentStatusText(playbackUiStateSync.playbackStatusPlaying(target.source))
            },
            onProgressChanged = { playedSamples, totalSamples ->
                playbackUiStateSync.applyPlaybackProgress(target.source, playedSamples, totalSamples)
            },
            onFinished = { _, result ->
                safeLogD(
                    PLAYBACK_AUTOMATION_TAG,
                    "finished source=$sourceKey result=$result currentSource=${uiState.value.currentPlaybackSource.debugSourceId()} " +
                        "selectedSaved=${uiState.value.selectedSavedAudio?.item?.itemId.orEmpty()}",
                )
                when (result) {
                    PlaybackResult.Completed -> {
                        if (!onPlaybackCompleted(target.source)) {
                            playbackUiStateSync.applyPlaybackCompleted(target.source)
                        }
                    }

                    PlaybackResult.Stopped -> playbackUiStateSync.applyPlaybackStopped(target.source)
                }
            },
            onFailed = { failedSourceKey ->
                safeLogD(
                    PLAYBACK_AUTOMATION_TAG,
                    "failed source=$failedSourceKey currentSource=${uiState.value.currentPlaybackSource.debugSourceId()} " +
                        "selectedSaved=${uiState.value.selectedSavedAudio?.item?.itemId.orEmpty()}",
                )
                playbackUiStateSync.applyPlaybackFailed(failedSourceKey)
            },
        )
    }
}

private const val PLAYBACK_AUTOMATION_TAG = "PlaybackAutomation"
private const val PLAYBACK_SPEED_DIAG_TAG = "PlaybackSpeedDiag"

private fun AudioPlaybackSource.debugSourceId(): String =
    when (this) {
        is AudioPlaybackSource.Generated -> "generated:${mode.wireName}"
        is AudioPlaybackSource.Saved -> "saved:$itemId"
    }

private fun safeLogD(
    tag: String,
    message: String,
) {
    if (!BuildConfig.DEBUG) {
        return
    }
    try {
        Log.d(tag, message)
    } catch (_: RuntimeException) {
        // Plain JVM unit tests use the Android stub jar, where Log.d is not implemented.
    }
}

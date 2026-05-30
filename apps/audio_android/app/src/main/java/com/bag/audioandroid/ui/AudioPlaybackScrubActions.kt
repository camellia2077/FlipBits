package com.bag.audioandroid.ui

import com.bag.audioandroid.R
import com.bag.audioandroid.audio.AudioPlaybackCoordinator
import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
import kotlinx.coroutines.flow.MutableStateFlow

internal class AudioPlaybackScrubActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val playbackCoordinator: AudioPlaybackCoordinator,
    private val playbackRuntimeGateway: PlaybackRuntimeGateway,
    private val playbackSourceCoordinator: PlaybackSourceCoordinator,
    private val playbackUiStateSync: AudioPlaybackUiStateSync,
    private val followDataWindowActions: FollowDataWindowActions? = null,
    private val flashVisualWindowActions: FlashVisualWindowActions? = null,
    private val startPlaybackFromTarget: (PlaybackSourceCoordinator.PlaybackTarget) -> Unit,
) {
    fun onScrubStarted() {
        val current = uiState.value
        val playbackTarget = playbackSourceCoordinator.resolveTarget(current) ?: return
        if (playbackTarget.playback.totalSamples <= 0) {
            return
        }

        val sourceKey = playbackSourceCoordinator.sourceKey(playbackTarget.source)
        PlaybackScrubDiagTrace.event(
            name = "scrubStart",
            source = playbackTarget.source,
            playback = playbackTarget.playback,
            detail = "active=${playbackCoordinator.hasActivePlaybackForSource(sourceKey)}",
        )
        val started = playbackRuntimeGateway.scrubStarted(playbackTarget.playback)
        if (started.resumeAfterScrub) {
            playbackCoordinator.beginScrub(sourceKey)
        }
        playbackUiStateSync.updatePlaybackState(playbackTarget.source) { started }
        PlaybackScrubDiagTrace.event(
            name = "scrubStarted",
            source = playbackTarget.source,
            playback = started,
            detail = "active=${playbackCoordinator.hasActivePlaybackForSource(sourceKey)}",
        )
        playbackUiStateSync.setCurrentStatusText(UiText.Resource(R.string.status_playback_paused))
    }

    fun onScrubChanged(targetSamples: Int) {
        val current = uiState.value
        val playbackTarget = playbackSourceCoordinator.resolveTarget(current) ?: return
        if (playbackTarget.playback.totalSamples <= 0) {
            return
        }

        val clampedTarget = playbackRuntimeGateway.clampSamples(playbackTarget.playback.totalSamples, targetSamples)
        PlaybackScrubDiagTrace.event(
            name = "scrubChange",
            source = playbackTarget.source,
            playback = playbackTarget.playback,
            detail = "target=$targetSamples clamped=$clampedTarget",
        )
        playbackCoordinator.updateScrub(playbackSourceCoordinator.sourceKey(playbackTarget.source), clampedTarget)
        playbackUiStateSync.updatePlaybackState(playbackTarget.source) {
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
        val hasActivePlayback = playbackCoordinator.hasActivePlaybackForSource(sourceKey)
        val shouldResume = playback.resumeAfterScrub && hasActivePlayback
        val committed = playbackRuntimeGateway.scrubCommitted(playback)
        PlaybackScrubDiagTrace.event(
            name = "scrubFinish",
            source = playbackTarget.source,
            playback = playback,
            detail = "active=$hasActivePlayback shouldResume=$shouldResume committedPlayed=${committed.playedSamples}",
        )
        if (hasActivePlayback) {
            playbackCoordinator.stopPlayback()
        }
        if (shouldResume) {
            PlaybackScrubDiagTrace.event(
                name = "scrubResume",
                source = playbackTarget.source,
                playback = committed,
                detail = "sourceKey=$sourceKey",
            )
            startPlaybackFromTarget(playbackTarget.copy(playback = committed))
            return
        }
        playbackUiStateSync.updatePlaybackState(playbackTarget.source) { committed }
        val generatedSource = playbackTarget.source as? com.bag.audioandroid.ui.model.AudioPlaybackSource.Generated
        if (generatedSource != null) {
            followDataWindowActions?.ensureCurrentWindow(generatedSource.mode, committed.playedSamples)
            flashVisualWindowActions?.ensureCurrentWindow(generatedSource.mode, committed.playedSamples)
        }
        playbackUiStateSync.setCurrentStatusText(
            UiText.Resource(R.string.status_playback_paused),
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
        PlaybackScrubDiagTrace.event(
            name = "scrubCancel",
            source = playbackTarget.source,
            playback = playback,
            detail = "active=${playbackCoordinator.hasActivePlaybackForSource(sourceKey)}",
        )
        val canceled = playbackRuntimeGateway.scrubCanceled(playback)
        if (playbackCoordinator.hasActivePlaybackForSource(sourceKey)) {
            playbackCoordinator.cancelScrub(sourceKey, canceled.isPlaying)
        }
        playbackUiStateSync.updatePlaybackState(playbackTarget.source) { canceled }
        PlaybackScrubDiagTrace.event(
            name = "scrubCanceled",
            source = playbackTarget.source,
            playback = canceled,
            detail = "sourceKey=$sourceKey",
        )
        playbackUiStateSync.setCurrentStatusText(
            if (canceled.isPlaying) {
                playbackUiStateSync.playbackStatusPlaying(playbackTarget.source)
            } else {
                UiText.Resource(R.string.status_playback_paused)
            },
        )
    }
}

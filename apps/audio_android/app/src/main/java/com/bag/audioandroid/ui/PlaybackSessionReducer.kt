package com.bag.audioandroid.ui

import com.bag.audioandroid.R
import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.ModeAudioSessionState
import com.bag.audioandroid.ui.state.PlaybackUiState

class PlaybackSessionReducer(
    private val playbackRuntimeGateway: PlaybackRuntimeGateway,
    private val sampleRateHz: Int
) {
    fun started(
        session: ModeAudioSessionState,
        mode: TransportModeOption,
        playback: PlaybackUiState
    ): ModeAudioSessionState =
        session.copy(
            statusText = playingStatus(mode),
            playback = playback
        )

    fun progress(
        session: ModeAudioSessionState,
        playedSamples: Int,
        totalSamples: Int
    ): ModeAudioSessionState {
        val playbackBase = if (session.playback.totalSamples == 0 && totalSamples > 0) {
            playbackRuntimeGateway.load(totalSamples, sampleRateHz)
        } else {
            session.playback
        }
        return session.copy(
            playback = playbackRuntimeGateway.progress(playbackBase, playedSamples)
        )
    }

    fun paused(session: ModeAudioSessionState): ModeAudioSessionState =
        session.copy(
            statusText = UiText.Resource(R.string.status_playback_paused),
            playback = playbackRuntimeGateway.paused(session.playback)
        )

    fun resumed(
        session: ModeAudioSessionState,
        mode: TransportModeOption
    ): ModeAudioSessionState =
        session.copy(
            statusText = playingStatus(mode),
            playback = playbackRuntimeGateway.resumed(session.playback)
        )

    fun completed(session: ModeAudioSessionState): ModeAudioSessionState =
        session.copy(
            statusText = UiText.Resource(R.string.status_playback_completed),
            playback = playbackRuntimeGateway.completed(session.playback)
        )

    fun failed(session: ModeAudioSessionState): ModeAudioSessionState =
        session.copy(
            statusText = UiText.Resource(R.string.status_playback_failed),
            playback = playbackRuntimeGateway.failed(session.playback)
        )

    fun stopped(session: ModeAudioSessionState): ModeAudioSessionState =
        session.copy(
            statusText = UiText.Resource(R.string.status_playback_stopped),
            playback = playbackRuntimeGateway.stopped(session.playback)
        )

    fun playingStatus(mode: TransportModeOption): UiText =
        UiText.Resource(R.string.status_playing_mode_audio, listOf(mode.wireName))
}

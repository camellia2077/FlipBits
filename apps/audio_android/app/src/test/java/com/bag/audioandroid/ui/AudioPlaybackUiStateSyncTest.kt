package com.bag.audioandroid.ui

import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.ModeAudioSessionState
import com.bag.audioandroid.ui.state.PlaybackPhase
import com.bag.audioandroid.ui.state.PlaybackUiState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AudioPlaybackUiStateSyncTest {
    @Test
    fun `progress callback after pause does not advance generated playback`() {
        val gateway = LocalPlaybackRuntimeGateway()
        val source = AudioPlaybackSource.Generated(TransportModeOption.Flash)
        val initialPlayback =
            PlaybackUiState(
                phase = PlaybackPhase.Paused,
                playedSamples = 4_096,
                totalSamples = 44_100,
                sampleRateHz = 44_100,
            )
        val uiState =
            MutableStateFlow(
                AudioAppUiState(
                    currentPlaybackSource = source,
                    sessions =
                        mapOf(
                            TransportModeOption.Flash to ModeAudioSessionState(playback = initialPlayback),
                            TransportModeOption.Mini to ModeAudioSessionState(),
                            TransportModeOption.Pro to ModeAudioSessionState(),
                            TransportModeOption.Ultra to ModeAudioSessionState(),
                        ),
                ),
            )
        val sync =
            AudioPlaybackUiStateSync(
                uiState = uiState,
                sessionStateStore = AudioSessionStateStore(uiState),
                playbackRuntimeGateway = gateway,
                playbackSourceCoordinator = PlaybackSourceCoordinator(generatedSampleRateHz = 44_100),
                playbackSessionReducer =
                    PlaybackSessionReducer(
                        playbackRuntimeGateway = gateway,
                        sampleRateHz = 44_100,
                    ),
                sampleRateHz = 44_100,
            )

        sync.applyPlaybackProgress(source, playedSamples = 8_192, totalSamples = 44_100)

        assertEquals(
            4_096,
            uiState.value.sessions
                .getValue(TransportModeOption.Flash)
                .playback
                .playedSamples,
        )
        assertFalse(gateway.progressCalled)
    }
}

private class LocalPlaybackRuntimeGateway : PlaybackRuntimeGateway {
    var progressCalled = false

    override fun cleared(): PlaybackUiState = PlaybackUiState()

    override fun load(
        totalSamples: Int,
        sampleRateHz: Int,
    ): PlaybackUiState = PlaybackUiState(totalSamples = totalSamples, sampleRateHz = sampleRateHz)

    override fun playStarted(state: PlaybackUiState): PlaybackUiState = state.copy(phase = PlaybackPhase.Playing)

    override fun paused(state: PlaybackUiState): PlaybackUiState = state.copy(phase = PlaybackPhase.Paused)

    override fun resumed(state: PlaybackUiState): PlaybackUiState = state.copy(phase = PlaybackPhase.Playing)

    override fun progress(
        state: PlaybackUiState,
        playedSamples: Int,
    ): PlaybackUiState {
        progressCalled = true
        return state.copy(playedSamples = playedSamples)
    }

    override fun scrubStarted(state: PlaybackUiState): PlaybackUiState = state

    override fun scrubChanged(
        state: PlaybackUiState,
        targetSamples: Int,
    ): PlaybackUiState = state

    override fun scrubCommitted(state: PlaybackUiState): PlaybackUiState = state

    override fun scrubCanceled(state: PlaybackUiState): PlaybackUiState = state

    override fun stopped(state: PlaybackUiState): PlaybackUiState = state.copy(phase = PlaybackPhase.Stopped, playedSamples = 0)

    override fun completed(state: PlaybackUiState): PlaybackUiState = state.copy(phase = PlaybackPhase.Completed)

    override fun failed(state: PlaybackUiState): PlaybackUiState = state.copy(phase = PlaybackPhase.Failed)

    override fun clampSamples(
        totalSamples: Int,
        sampleIndex: Int,
    ): Int = sampleIndex.coerceIn(0, totalSamples)

    override fun fractionToSamples(
        totalSamples: Int,
        fraction: Float,
    ): Int = (totalSamples * fraction).toInt()

    override fun progressFraction(state: PlaybackUiState): Float = 0f

    override fun elapsedMs(state: PlaybackUiState): Long = 0L

    override fun totalMs(state: PlaybackUiState): Long = 0L
}

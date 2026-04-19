package com.bag.audioandroid.data

import com.bag.audioandroid.NativePlaybackRuntimeBridge
import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.ui.state.PlaybackPhase
import com.bag.audioandroid.ui.state.PlaybackUiState

private const val PLAYBACK_STATE_FIELD_COUNT = 7
private const val PHASE_INDEX = 0
private const val PLAYED_SAMPLES_INDEX = 1
private const val TOTAL_SAMPLES_INDEX = 2
private const val SAMPLE_RATE_INDEX = 3
private const val IS_SCRUBBING_INDEX = 4
private const val SCRUB_TARGET_INDEX = 5
private const val RESUME_AFTER_SCRUB_INDEX = 6

class NativePlaybackRuntimeGateway : PlaybackRuntimeGateway {
    override fun cleared(): PlaybackUiState = decodeState(NativePlaybackRuntimeBridge.nativeClearedState())

    override fun load(
        totalSamples: Int,
        sampleRateHz: Int,
    ): PlaybackUiState = decodeState(NativePlaybackRuntimeBridge.nativeLoadState(totalSamples, sampleRateHz))

    override fun playStarted(state: PlaybackUiState): PlaybackUiState =
        decodeState(NativePlaybackRuntimeBridge.nativePlayStarted(encodeState(state)))

    override fun paused(state: PlaybackUiState): PlaybackUiState = decodeState(NativePlaybackRuntimeBridge.nativePaused(encodeState(state)))

    override fun resumed(state: PlaybackUiState): PlaybackUiState =
        decodeState(NativePlaybackRuntimeBridge.nativeResumed(encodeState(state)))

    override fun progress(
        state: PlaybackUiState,
        playedSamples: Int,
    ): PlaybackUiState = decodeState(NativePlaybackRuntimeBridge.nativeProgress(encodeState(state), playedSamples))

    override fun scrubStarted(state: PlaybackUiState): PlaybackUiState =
        decodeState(NativePlaybackRuntimeBridge.nativeScrubStarted(encodeState(state)))

    override fun scrubChanged(
        state: PlaybackUiState,
        targetSamples: Int,
    ): PlaybackUiState = decodeState(NativePlaybackRuntimeBridge.nativeScrubChanged(encodeState(state), targetSamples))

    override fun scrubCommitted(state: PlaybackUiState): PlaybackUiState =
        decodeState(NativePlaybackRuntimeBridge.nativeScrubCommitted(encodeState(state)))

    override fun scrubCanceled(state: PlaybackUiState): PlaybackUiState =
        decodeState(NativePlaybackRuntimeBridge.nativeScrubCanceled(encodeState(state)))

    override fun stopped(state: PlaybackUiState): PlaybackUiState =
        decodeState(NativePlaybackRuntimeBridge.nativeStopped(encodeState(state)))

    override fun completed(state: PlaybackUiState): PlaybackUiState =
        decodeState(NativePlaybackRuntimeBridge.nativeCompleted(encodeState(state)))

    override fun failed(state: PlaybackUiState): PlaybackUiState = decodeState(NativePlaybackRuntimeBridge.nativeFailed(encodeState(state)))

    override fun clampSamples(
        totalSamples: Int,
        sampleIndex: Int,
    ): Int = NativePlaybackRuntimeBridge.nativeClampSamples(totalSamples, sampleIndex)

    override fun fractionToSamples(
        totalSamples: Int,
        fraction: Float,
    ): Int = NativePlaybackRuntimeBridge.nativeFractionToSamples(totalSamples, fraction)

    override fun progressFraction(state: PlaybackUiState): Float = NativePlaybackRuntimeBridge.nativeProgressFraction(encodeState(state))

    override fun elapsedMs(state: PlaybackUiState): Long = NativePlaybackRuntimeBridge.nativeElapsedMs(encodeState(state))

    override fun totalMs(state: PlaybackUiState): Long = NativePlaybackRuntimeBridge.nativeTotalMs(encodeState(state))

    private fun encodeState(state: PlaybackUiState): IntArray =
        intArrayOf(
            state.phase.nativeValue,
            state.playedSamples,
            state.totalSamples,
            state.sampleRateHz,
            if (state.isScrubbing) 1 else 0,
            state.scrubPreviewSamples,
            if (state.resumeAfterScrub) 1 else 0,
        )

    private fun decodeState(rawState: IntArray): PlaybackUiState {
        val values =
            if (rawState.size >= PLAYBACK_STATE_FIELD_COUNT) {
                rawState
            } else {
                IntArray(PLAYBACK_STATE_FIELD_COUNT).also { rawState.copyInto(it) }
            }
        return PlaybackUiState(
            phase = PlaybackPhase.fromNativeValue(values[PHASE_INDEX]),
            playedSamples = values[PLAYED_SAMPLES_INDEX],
            totalSamples = values[TOTAL_SAMPLES_INDEX],
            sampleRateHz = values[SAMPLE_RATE_INDEX],
            isScrubbing = values[IS_SCRUBBING_INDEX] != 0,
            scrubPreviewSamples = values[SCRUB_TARGET_INDEX],
            resumeAfterScrub = values[RESUME_AFTER_SCRUB_INDEX] != 0,
        )
    }
}

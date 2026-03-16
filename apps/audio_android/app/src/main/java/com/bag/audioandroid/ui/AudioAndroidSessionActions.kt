package com.bag.audioandroid.ui

import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioCodecGateway
import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.domain.SavedAudioRepository
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
import kotlinx.coroutines.flow.MutableStateFlow

internal class AudioAndroidSessionActions(
    uiState: MutableStateFlow<AudioAppUiState>,
    audioCodecGateway: AudioCodecGateway,
    private val sessionStateStore: AudioSessionStateStore,
    uiTextMapper: BagUiTextMapper,
    playbackRuntimeGateway: PlaybackRuntimeGateway,
    savedAudioRepository: SavedAudioRepository,
    sampleRateHz: Int,
    frameSamples: Int,
    stopPlayback: () -> Unit,
    refreshSavedAudioItems: () -> Unit
) {
    private val editingActions = AudioSessionEditingActions(
        uiState = uiState,
        sessionStateStore = sessionStateStore,
        stopPlayback = stopPlayback,
        refreshSavedAudioItems = refreshSavedAudioItems
    )
    private val codecActions = AudioSessionCodecActions(
        uiState = uiState,
        audioCodecGateway = audioCodecGateway,
        sessionStateStore = sessionStateStore,
        uiTextMapper = uiTextMapper,
        playbackRuntimeGateway = playbackRuntimeGateway,
        sampleRateHz = sampleRateHz,
        frameSamples = frameSamples,
        stopPlayback = stopPlayback
    )
    private val exportActions = AudioSessionExportActions(
        uiState = uiState,
        sessionStateStore = sessionStateStore,
        savedAudioRepository = savedAudioRepository,
        sampleRateHz = sampleRateHz,
        refreshSavedAudioItems = refreshSavedAudioItems
    )

    fun onInputTextChange(value: String) {
        editingActions.onInputTextChange(value)
    }

    fun onTransportModeSelected(mode: TransportModeOption) {
        editingActions.onTransportModeSelected(mode)
    }

    fun onEncode() {
        codecActions.onEncode()
    }

    fun onDecode() {
        codecActions.onDecode()
    }

    fun onClear() {
        sessionStateStore.updateCurrentSession {
            it.copy(
                inputText = "",
                statusText = if (it.generatedPcm.isEmpty()) {
                    UiText.Resource(R.string.status_ready_to_encode)
                } else {
                    it.statusText
                }
            )
        }
    }

    fun onClearResult() {
        sessionStateStore.updateCurrentSession {
            it.copy(
                resultText = "",
                statusText = UiText.Resource(R.string.status_result_cleared)
            )
        }
    }

    fun onExportAudio() {
        exportActions.onExportAudio()
    }

    fun onOpenSavedAudioSheet() {
        editingActions.onOpenSavedAudioSheet()
    }

    fun onCloseSavedAudioSheet() {
        editingActions.onCloseSavedAudioSheet()
    }
}

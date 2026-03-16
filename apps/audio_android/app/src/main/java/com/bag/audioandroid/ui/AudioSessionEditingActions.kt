package com.bag.audioandroid.ui

import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.state.AudioAppUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class AudioSessionEditingActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val sessionStateStore: AudioSessionStateStore,
    private val stopPlayback: () -> Unit,
    private val refreshSavedAudioItems: () -> Unit
) {
    fun onInputTextChange(value: String) {
        sessionStateStore.updateCurrentSession { it.copy(inputText = value) }
    }

    fun onTransportModeSelected(mode: TransportModeOption) {
        val currentState = uiState.value
        if (currentState.transportMode == mode &&
            currentState.currentPlaybackSource == AudioPlaybackSource.Generated(mode)
        ) {
            return
        }
        stopPlayback()
        uiState.update {
            it.copy(
                transportMode = mode,
                currentPlaybackSource = AudioPlaybackSource.Generated(mode),
                showSavedAudioSheet = false
            )
        }
    }

    fun onOpenSavedAudioSheet() {
        refreshSavedAudioItems()
        uiState.update { it.copy(showSavedAudioSheet = true) }
    }

    fun onCloseSavedAudioSheet() {
        uiState.update { it.copy(showSavedAudioSheet = false) }
    }
}

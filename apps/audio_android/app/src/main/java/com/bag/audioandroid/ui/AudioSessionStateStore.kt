package com.bag.audioandroid.ui

import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.ModeAudioSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class AudioSessionStateStore(
    private val uiState: MutableStateFlow<AudioAppUiState>,
) {
    fun updateCurrentSession(transform: (ModeAudioSessionState) -> ModeAudioSessionState) {
        updateSession(uiState.value.transportMode, transform)
    }

    fun updateSession(
        mode: TransportModeOption,
        transform: (ModeAudioSessionState) -> ModeAudioSessionState,
    ) {
        uiState.update { state ->
            val updatedSessions = state.sessions.toMutableMap()
            updatedSessions[mode] = transform(state.sessions.getValue(mode))
            state.copy(sessions = updatedSessions)
        }
    }
}

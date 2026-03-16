package com.bag.audioandroid.ui

import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioExportResult
import com.bag.audioandroid.domain.SavedAudioRepository
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
import kotlinx.coroutines.flow.MutableStateFlow

internal class AudioSessionExportActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val sessionStateStore: AudioSessionStateStore,
    private val savedAudioRepository: SavedAudioRepository,
    private val sampleRateHz: Int,
    private val refreshSavedAudioItems: () -> Unit
) {
    fun onExportAudio() {
        val current = uiState.value
        val session = current.currentSession
        if (session.generatedPcm.isEmpty()) {
            sessionStateStore.updateCurrentSession {
                it.copy(statusText = UiText.Resource(R.string.status_no_audio_for_mode))
            }
            return
        }

        when (
            val result = savedAudioRepository.exportGeneratedAudio(
                mode = current.transportMode,
                inputText = session.inputText,
                pcm = session.generatedPcm,
                sampleRateHz = sampleRateHz
            )
        ) {
            is AudioExportResult.Success -> sessionStateStore.updateCurrentSession {
                it.copy(
                    statusText = UiText.Resource(
                        R.string.status_audio_saved,
                        listOf(result.displayName)
                    )
                )
            }.also { refreshSavedAudioItems() }

            AudioExportResult.Failed -> sessionStateStore.updateCurrentSession {
                it.copy(statusText = UiText.Resource(R.string.status_audio_save_failed))
            }
        }
    }
}

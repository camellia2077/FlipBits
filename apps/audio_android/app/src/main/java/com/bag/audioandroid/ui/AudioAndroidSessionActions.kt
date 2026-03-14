package com.bag.audioandroid.ui

import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioCodecGateway
import com.bag.audioandroid.domain.AudioExportResult
import com.bag.audioandroid.domain.BagApiCodes
import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.domain.SavedAudioRepository
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class AudioAndroidSessionActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val audioCodecGateway: AudioCodecGateway,
    private val sessionStateStore: AudioSessionStateStore,
    private val uiTextMapper: BagUiTextMapper,
    private val playbackRuntimeGateway: PlaybackRuntimeGateway,
    private val savedAudioRepository: SavedAudioRepository,
    private val sampleRateHz: Int,
    private val frameSamples: Int,
    private val stopPlayback: () -> Unit,
    private val refreshSavedAudioItems: () -> Unit
) {
    fun onInputTextChange(value: String) {
        sessionStateStore.updateCurrentSession { it.copy(inputText = value) }
    }

    fun onTransportModeSelected(mode: com.bag.audioandroid.ui.model.TransportModeOption) {
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

    fun onEncode() {
        stopPlayback()
        val current = uiState.value
        val session = current.currentSession
        val validationIssue = audioCodecGateway.validateEncodeRequest(
            session.inputText,
            sampleRateHz,
            frameSamples,
            current.transportMode.nativeValue
        )
        if (validationIssue != BagApiCodes.VALIDATION_OK) {
            sessionStateStore.updateCurrentSession {
                it.copy(
                    generatedPcm = shortArrayOf(),
                    resultText = "",
                    statusText = uiTextMapper.validationIssue(validationIssue),
                    playback = playbackRuntimeGateway.cleared()
                )
            }
            return
        }

        val pcm = audioCodecGateway.encodeTextToPcm(
            session.inputText,
            sampleRateHz,
            frameSamples,
            current.transportMode.nativeValue
        )
        val status = if (pcm.isEmpty()) {
            if (session.inputText.isBlank()) {
                UiText.Resource(R.string.status_input_empty)
            } else {
                uiTextMapper.errorCode(BagApiCodes.ERROR_INTERNAL)
            }
        } else {
            UiText.Resource(
                R.string.status_mode_audio_generated,
                listOf(current.transportMode.wireName, pcm.size)
            )
        }
        sessionStateStore.updateCurrentSession {
            it.copy(
                generatedPcm = pcm,
                resultText = "",
                statusText = status,
                playback = if (pcm.isEmpty()) {
                    playbackRuntimeGateway.cleared()
                } else {
                    playbackRuntimeGateway.load(pcm.size, sampleRateHz)
                }
            )
        }
        uiState.update {
            it.copy(currentPlaybackSource = AudioPlaybackSource.Generated(current.transportMode))
        }
    }

    fun onDecode() {
        val current = uiState.value
        val session = current.currentSession
        if (session.generatedPcm.isEmpty()) {
            sessionStateStore.updateCurrentSession {
                it.copy(statusText = UiText.Resource(R.string.status_no_audio_for_mode))
            }
            return
        }

        val validationIssue = audioCodecGateway.validateDecodeConfig(
            sampleRateHz,
            frameSamples,
            current.transportMode.nativeValue
        )
        if (validationIssue != BagApiCodes.VALIDATION_OK) {
            sessionStateStore.updateCurrentSession {
                it.copy(statusText = uiTextMapper.validationIssue(validationIssue))
            }
            return
        }

        val decoded = audioCodecGateway.decodeGeneratedPcm(
            session.generatedPcm,
            sampleRateHz,
            frameSamples,
            current.transportMode.nativeValue
        )
        val status = if (decoded.isEmpty()) {
            uiTextMapper.errorCode(BagApiCodes.ERROR_INTERNAL)
        } else {
            UiText.Resource(
                R.string.status_mode_decode_completed,
                listOf(current.transportMode.wireName)
            )
        }
        sessionStateStore.updateCurrentSession {
            it.copy(
                resultText = decoded,
                statusText = status
            )
        }
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

    fun onOpenSavedAudioSheet() {
        refreshSavedAudioItems()
        uiState.update {
            it.copy(showSavedAudioSheet = true)
        }
    }

    fun onCloseSavedAudioSheet() {
        uiState.update { it.copy(showSavedAudioSheet = false) }
    }
}
